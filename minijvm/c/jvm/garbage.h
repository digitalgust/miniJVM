

#ifndef _GARBAGE_H
#define _GARBAGE_H

#include "../utils/hashtable.h"
#include "../utils/hashset.h"
#include "../utils/linkedlist.h"
#include "jvm.h"
#include "jvm_util.h"
#include "immix.h"


#ifdef __cplusplus
extern "C" {
#endif


// GC thread entities


// Each thread has its own garbage bin; too many threads would be a disaster.
struct _GcCollectorType {
    MiniJVM *jvm;

    // A lawless zone, a holder that prevents garbage collection.
    // Objects placed in it and other objects they reference will not be collected.
    Hashset *objs_holder;
    MemoryBlock *header, *tmp_header, *tmp_tailer;
    s64 obj_count;
    s64 obj_heap_size;
    s64 jit_heap_size;
    s64 lastgc;//last gc at mills
    Runtime *runtime;
    //

    //
    thrd_t garbage_thread;// Garbage collection thread
    Hashtable *objs_2_count;

    spinlock_t lock;
    //
    ArrayList *runtime_refer_copy;
    //

    // Immix (block backend) integration state. immix_heap is NULL when the
    // malloc backend is active and the classic linked-list collector owns
    // object storage.
    struct ImmixHeap *immix_heap;
    ArrayList *immix_pending_finalize; //finalizable objects kept alive this cycle
    ArrayList *immix_pending_enqueue;  //weak references to enqueue after resume
    ArrayList *immix_pending_runtimes; //dead jthread runtimes to destroy after resume
    ArrayList *immix_pending_loaders;  //dead classloaders to destroy after resume
    volatile s32 gc_request;     //async collection request from a mutator
    volatile s64 gc_gen;         //incremented after every completed cycle

    u8 _garbage_thread_status;
    u8 mark_cnt;
    volatile u8 isgc;
    volatile u8 isworldstoped;
    volatile u8 dump_flag;
    Utf8String *dump_path;
    s32 dump_flags;
    s32 dump_rc;
    s16 exit_flag;
    s16 exit_code;
    volatile s64 stw_total_ns;
};

enum {
    GARBAGE_THREAD_NORMAL,
    GARBAGE_THREAD_PAUSE,
    GARBAGE_THREAD_STOP,
    GARBAGE_THREAD_DEAD,
};


// API

s32 gc_create(MiniJVM *jvm);

void gc_destroy(MiniJVM *jvm);

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

/* Non-zero when Java objects live in the Immix block backend. */
s32 gc_backend_is_immix(MiniJVM *jvm);

/*
 * Unified Java object storage entry. Returns zeroed memory sized insSize,
 * backed either by the Immix block backend or jvm_calloc.
 */
void *gc_obj_alloc(Runtime *runtime, s32 insSize);

/* Iterates every Java heap object (Immix blocks/LOS plus the class list). */
typedef s32 (*GcHeapObjectIter)(MemoryBlock *mb, void *data);

void gc_iterate_heap_objects(GcCollector *collector, GcHeapObjectIter iter, void *data);

/* System memory pressure entry for embedders (Immix backend). */
void gc_notify_memory_pressure(MiniJVM *jvm, s32 pressure_level);


#ifdef __cplusplus
}
#endif

#endif //_GARBAGE_H
