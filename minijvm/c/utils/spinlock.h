//
// Created by Gust on 2017/11/15 0015.
//


#ifndef MINI_JVM_SPINLOCK_H
#define MINI_JVM_SPINLOCK_H
#ifdef __cplusplus
extern "C" {
#endif
//======================= spinlock =============================
//      reenter spinlock

#include "tinycthread.h"


struct _SpinLock {
    volatile s32 lock;
    volatile s32 count;
    volatile thrd_t owner;
};
typedef struct _SpinLock spinlock_t;


#if defined( __JVM_OS_VS__ )
static inline s64 __sync_bool_compare_and_swap64(volatile s64* lock, s64 comparand, s64 exchange) {
    //if *lock == comparand then *lock = exchange  and return old *lock value
    if (InterlockedCompareExchange64(lock, exchange, comparand) == comparand)return 1;
    else return 0;
}
static inline s32 __sync_bool_compare_and_swap(volatile s32 *lock,int comparand, s32 exchange) {
    //if *lock == comparand then *lock = exchange  and return old *lock value
    if(InterlockedCompareExchange(lock, exchange, comparand) == comparand)return 1;
    else return 0;
}
#else
#define  __sync_bool_compare_and_swap64 __sync_bool_compare_and_swap
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
    while (1) {
        int i;
        for (i = 0; i < count; i++) {
            // if *lock == 0,then *lock = 1  ,  return true else return false
            if (thrd_equal(lock->owner, thrd_current())) {
                lock->count++;
                return 0;
            }
            if (__sync_bool_compare_and_swap(&lock->lock, 0, 1)) {
                lock->owner = thrd_current();
                lock->count = 1;
                return 0;
            }
        }
        thrd_yield();
    }
}

static inline int spin_lock(volatile spinlock_t *lock) {
    return spin_lock_count(lock, 100);
}

//static inline int spin_trylock(volatile spinlock_t *lock) {
//    if (lock->owner == thrd_current()) {
//        lock->count++;
//        return 0;
//    }
//    if (__sync_bool_compare_and_swap(&lock->lock, 0, 1)) {
//        lock->owner = thrd_current();
//        lock->count++;
//        return 0;
//    }
//    return 1;
//}

static inline int spin_unlock(volatile spinlock_t *lock) {
    if (!thrd_equal(lock->owner, thrd_current())) {
        return 1;
    }
    lock->count--;
    if (lock->count == 0) {
        lock->owner = 0;
        __sync_bool_compare_and_swap(&lock->lock, 1, 0);
    }
    return 0;
}

#ifdef __cplusplus
};
#endif

#endif //MINI_JVM_SPINLOCK_H
