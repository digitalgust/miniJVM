//
// Created by Gust on 2017/11/15 0015.
//


#ifndef MINI_JVM_SPINLOCK_H
#define MINI_JVM_SPINLOCK_H

//======================= spinlock =============================

#include "tinycthread.h"

typedef int spinlock_t;

//int spin_init(volatile spinlock_t *lock, int pshared);
//
//int spin_destroy(spinlock_t *lock);
//
//int spin_lock(volatile spinlock_t *lock);
//
//int spin_lock_count(volatile spinlock_t *lock, int count);
//
//int spin_trylock(volatile spinlock_t *lock);
//
//int spin_unlock(volatile spinlock_t *lock);


#if defined( __JVM_OS_VS__ )
static inline int __sync_bool_compare_and_swap(volatile int *lock,int comparand, int exchange) {
    //if *lock == comparand then *lock = exchange  and return old *lock value
    if(InterlockedCompareExchange(lock, exchange, comparand) == comparand)return 1;
    else return 0;
}
#endif

static inline int spin_init(volatile spinlock_t *lock, int pshared) {
//    __asm__ __volatile__ ("":: : "memory");
    *lock = 0;
    return 0;
}

static inline int spin_destroy(spinlock_t *lock) {
    return 0;
}


static inline int spin_lock_count(volatile spinlock_t *lock, int count) {
    while (1) {
        int i;
        for (i = 0; i < count; i++) {
            // if *lock == 0,then *lock = 1  ,  return true else return false
            if (__sync_bool_compare_and_swap(lock, 0, 1)) {
                return 0;
            }
        }
        thrd_yield();
    }
}

static inline int spin_lock(volatile spinlock_t *lock) {
    return spin_lock_count(lock, 100);
}

static inline int spin_trylock(volatile spinlock_t *lock) {
    if (__sync_bool_compare_and_swap(lock, 0, 1)) {
        return 0;
    }
    return 1;
}

static inline int spin_unlock(volatile spinlock_t *lock) {
    //__asm__ __volatile__ ("":: : "memory");
    *lock = 0;
    return 0;
}



#endif //MINI_JVM_SPINLOCK_H
