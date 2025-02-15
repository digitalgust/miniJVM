//
// Created by Gust on 2017/11/15 0015.
//

#ifndef MINI_JVM_SPINLOCK_H
#define MINI_JVM_SPINLOCK_H
#ifdef __cplusplus
extern "C"
{
#endif
//======================= Spinlock =============================
// This spinlock implementation provides basic lock and unlock functionalities
// with support for reentrant locking. Reentrant locking allows the same thread
// to acquire the lock multiple times without causing a deadlock. The lock is
// owned by the thread that acquires it, and the ownership is tracked to ensure
// that only the owning thread can release the lock. This implementation is
// designed to be lightweight and efficient, suitable for scenarios where
// low-overhead synchronization is required. The spinlock also includes adaptive
// spinning and timeout features to enhance performance and responsiveness in
// high-contention environments.

#include "tinycthread.h"
#include "d_type.h"  // Include d_type.h for type definitions

#define MAX_SPIN 1024

struct _SpinLock {
    volatile s32 lock;
    volatile s32 count;
    volatile thrd_t owner;
};
typedef struct _SpinLock spinlock_t;

// Platform specific atomic operations
#if defined(_MSC_VER)
// Windows/MSVC
#include <windows.h>
#define ATOMIC_CAS(ptr, oldv, newv) (InterlockedCompareExchange((volatile LONG *)(ptr), (newv), (oldv)) == (oldv))
#define ATOMIC_INC(ptr) InterlockedIncrement((volatile LONG *)(ptr))
#define ATOMIC_DEC(ptr) InterlockedDecrement((volatile LONG *)(ptr))
#define ATOMIC_ADD(ptr, val) InterlockedAdd((volatile LONG *)(ptr), (val))
#define ATOMIC_SUB(ptr, val) InterlockedAdd((volatile LONG *)(ptr), -(val))
#define MEMORY_BARRIER() MemoryBarrier()
#elif defined(__GNUC__)
// GCC/Clang
#define ATOMIC_CAS(ptr, old, newv) __sync_bool_compare_and_swap(ptr, old, newv)
#define ATOMIC_INC(ptr) __sync_add_and_fetch(ptr, 1)
#define ATOMIC_DEC(ptr) __sync_sub_and_fetch(ptr, 1)
#define ATOMIC_ADD(ptr, val) __sync_add_and_fetch(ptr, val)
#define ATOMIC_SUB(ptr, val) __sync_sub_and_fetch(ptr, val)
#define MEMORY_BARRIER() __sync_synchronize()
#else
// Generic implementation using mutex for unsupported platforms
#include <pthread.h>
static pthread_mutex_t atomic_mutex = PTHREAD_MUTEX_INITIALIZER;

static inline int atomic_cas(volatile s32 *ptr, s32 old_val, s32 new_val)
{
    int result;
    pthread_mutex_lock(&atomic_mutex);
    if (*ptr == old_val)
    {
        *ptr = new_val;
        result = 1;
    }
    else
    {
        result = 0;
    }
    pthread_mutex_unlock(&atomic_mutex);
    return result;
}

static inline s32 atomic_inc(volatile s32 *ptr)
{
    s32 result;
    pthread_mutex_lock(&atomic_mutex);
    *ptr += 1;
    result = *ptr;
    pthread_mutex_unlock(&atomic_mutex);
    return result;
}

static inline s32 atomic_dec(volatile s32 *ptr)
{
    s32 result;
    pthread_mutex_lock(&atomic_mutex);
    *ptr -= 1;
    result = *ptr;
    pthread_mutex_unlock(&atomic_mutex);
    return result;
}

static inline s32 atomic_add(volatile s32 *ptr, s32 val)
{
    s32 result;
    pthread_mutex_lock(&atomic_mutex);
    *ptr += val;
    result = *ptr;
    pthread_mutex_unlock(&atomic_mutex);
    return result;
}

static inline s32 atomic_sub(volatile s32 *ptr, s32 val)
{
    s32 result;
    pthread_mutex_lock(&atomic_mutex);
    *ptr -= val;
    result = *ptr;
    pthread_mutex_unlock(&atomic_mutex);
    return result;
}

#define ATOMIC_CAS(ptr, old, new) atomic_cas(ptr, old, new)
#define ATOMIC_INC(ptr) atomic_inc(ptr)
#define ATOMIC_DEC(ptr) atomic_dec(ptr)
#define ATOMIC_ADD(ptr, val) atomic_add(ptr, val)
#define ATOMIC_SUB(ptr, val) atomic_sub(ptr, val)
#define MEMORY_BARRIER()               \
    pthread_mutex_lock(&atomic_mutex); \
    pthread_mutex_unlock(&atomic_mutex)
#endif

#ifndef ETIMEDOUT
#define ETIMEDOUT 110 // POSIX 超时错误码
#endif

static inline int spin_init(volatile spinlock_t *lock, s32 pshared) {
    lock->lock = 0;
    lock->count = 0;
    lock->owner = 0;
    return 0;
}

static inline int spin_destroy(spinlock_t *lock) {
    return 0;
}

static inline int spin_lock_count(volatile spinlock_t *lock, s32 count) {
    for (;;) {
        if (thrd_equal(lock->owner, thrd_current())) {
            ATOMIC_INC(&lock->count);
            return 0;
        }
        if (ATOMIC_CAS(&lock->lock, 0, 1)) {
            lock->owner = thrd_current();
            lock->count = 1;
            return 0;
        }
    }
}

static inline int spin_lock(volatile spinlock_t *lock) {
    return spin_lock_count(lock, 100);
}

static inline int spin_trylock(volatile spinlock_t *lock) {
    if (thrd_equal(lock->owner, thrd_current())) {
        ATOMIC_INC(&lock->count);
        return 0;
    }
    if (ATOMIC_CAS(&lock->lock, 0, 1)) {
        lock->owner = thrd_current();
        lock->count = 1;
        return 0;
    }
    return 1;
}

static inline int spin_unlock(volatile spinlock_t *lock) {
    if (!thrd_equal(lock->owner, thrd_current())) {
        return 1;
    }
    MEMORY_BARRIER();
    ATOMIC_DEC(&lock->count);
    if (lock->count == 0) {
        lock->owner = 0;
        ATOMIC_CAS(&lock->lock, 1, 0);
    }
    return 0;
}

static inline int spin_lock_adaptive(volatile spinlock_t *lock) {
    int spin = 4; // 初始自旋次数
    for (;;) {
        for (int i = 0; i < spin; i++) {
            if (spin_trylock(lock))
                return 0;
        }
        // 自旋失败，指数退避
        spin = spin < MAX_SPIN ? spin * 2 : MAX_SPIN; // min(spin * 2, MAX_SPIN);
        thrd_yield();                                 // 或者使用 nanosleep
    }
}

static inline s64 current_timestamp() {
    struct timespec ts;
    timespec_get(&ts, TIME_UTC);
    return (s64) ts.tv_sec * 1000LL + ts.tv_nsec / 1000000LL;
}

static inline int spin_lock_timeout(volatile spinlock_t *lock, int timeout_ms) {
    s64 start = current_timestamp();
    while ((current_timestamp() - start) < timeout_ms) {
        if (spin_trylock(lock))
            return 0;
        thrd_yield();
    }
    return ETIMEDOUT;
}

#ifdef __cplusplus
};
#endif

#endif // MINI_JVM_SPINLOCK_H
