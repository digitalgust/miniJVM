

#ifndef _GARBAGE_H
#define _GARBAGE_H

#include "jvm.h"


#ifdef __cplusplus
extern "C" {
#endif



//回收线程






enum {
    GARBAGE_THREAD_NORMAL,
    GARBAGE_THREAD_PAUSE,
    GARBAGE_THREAD_STOP,
    GARBAGE_THREAD_DEAD,
};

s32 _collect_thread_run(void *para);

s32 garbage_thread_trylock();

void garbage_thread_lock();

void garbage_thread_unlock();

void garbage_collection_stop();

void garbage_collection_pause();

void garbage_collection_resume();

void garbage_thread_wait();

void garbage_thread_timedwait(s64 ms);

void garbage_thread_notify();

void garbage_thread_notifyall();

//其他函数

GcCollector *garbage_collector_create();

void garbage_collector_destory(GcCollector *);

void garbage_start();

s64 garbage_collect();

InstProp *gc_is_alive(__refer obj);

void gc_refer_hold(__refer ref);

void gc_refer_release(__refer ref);

void gc_refer_reg(JThreadRuntime *runtime, __refer ref);

void gc_move_refer_thread_2_gc(JThreadRuntime *runtime);

void garbage_dump_runtime();

void instance_hold_to_thread(JThreadRuntime *runtime, __refer ref);

void instance_release_from_thread(JThreadRuntime *runtime, __refer ref);

#ifdef __cplusplus
}
#endif

#endif //_GARBAGE_H
