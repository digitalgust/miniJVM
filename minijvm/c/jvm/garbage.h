

#ifndef _GARBAGE_H
#define _GARBAGE_H

#include "../utils/hashtable.h"
#include "../utils/hashset.h"
#include "../utils/linkedlist.h"
#include "jvm.h"
#include "jvm_util.h"


#ifdef __cplusplus
extern "C" {
#endif


//回收线程


//每个线程一个回收站，线程多了就是灾难
struct _GcCollectorType {
    MiniJVM *jvm;
    //
    Hashset *objs_holder; //法外之地，防回收的持有器，放入其中的对象及其引用的其他对象不会被回收
    MemoryBlock *header, *tmp_header, *tmp_tailer;
    s64 obj_count;
    s64 obj_heap_size;
    s64 lastgc;//last gc at mills
    Runtime *runtime;
    //

    //
    thrd_t garbage_thread;//垃圾回收线程

    spinlock_t lock;
    //
    ArrayList *runtime_refer_copy;
    //
    u8 _garbage_thread_status;
    u8 mark_cnt;
    u8 isgc;
    s16 exit_flag;
    s16 exit_code;
};

enum {
    GARBAGE_THREAD_NORMAL,
    GARBAGE_THREAD_PAUSE,
    GARBAGE_THREAD_STOP,
    GARBAGE_THREAD_DEAD,
};


//其他函数

s32 gc_create(MiniJVM *jvm);

void gc_destory(MiniJVM *jvm);

void gc_stop(GcCollector *collector);

void gc_pause(GcCollector *collector);

void gc_resume(GcCollector *collector);

MemoryBlock *gc_is_alive(GcCollector *collector, __refer obj);

void gc_obj_hold(GcCollector *collector, __refer ref);

void gc_obj_release(GcCollector *collector, __refer ref);

void gc_obj_reg(Runtime *runtime, __refer ref);

void gc_move_objs_thread_2_gc(Runtime *runtime);

void gc_dump_runtime(GcCollector *collector);

s64 gc_sum_heap(GcCollector *collector);


#ifdef __cplusplus
}
#endif

#endif //_GARBAGE_H
