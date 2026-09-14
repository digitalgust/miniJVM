

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
    // Registered objects (classes always; every Java object on the malloc
    // backend). Threads append to their own objs_array and hand them to
    // objs_stage at thread boundaries; the GC splices stage into objs_array
    // once the world is stopped — after that only the GC thread mutates it
    // (the classic finalize/sweep walks run post-resume), so the walks are
    // race-free. Contiguous pointer arrays walk with hardware prefetch and
    // compact in place, unlike the old external link chain.
    ArrayList *objs_array;
    ArrayList *objs_stage;
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

    // Classic (malloc backend) pending re-mark queue: objects that ran
    // finalize() or were enqueued as weak references this cycle and must
    // survive the sweep (replaces MemoryBlock.tmp_next).
    ArrayList *classic_pending;

    //Immix side lists (immix backend only): registration at creation turns
    //the per-cycle finalize/weak/capture decisions into O(list) work instead
    //of full-heap enumerations. Compacted during the cycle.
    ArrayList *side_weakrefs;   //instances with GCFLAG_WEAKREFERENCE
    ArrayList *side_finalizable;//instances whose class has finalizeMethod
    ArrayList *side_capturable; //JLOADER or JTHREAD instances (dead capture)

    // Immix (block backend) integration state. immix_heap is NULL when the
    // malloc backend is active and the classic linked-list collector owns
    // object storage.
    struct ImmixHeap *immix_heap;
    ArrayList *immix_pending_finalize; //finalizable objects kept alive this cycle
    ArrayList *immix_pending_enqueue;  //weak references to enqueue after resume
    ArrayList *immix_pending_runtimes; //dead jthread runtimes to destroy after resume
    ArrayList *immix_pending_loaders;  //dead classloaders to destroy after resume
    volatile s32 gc_request;     //async collection request from a mutator
    s64 immix_java_tracked;      //java-heap bytes already in the tracked total (synced per cycle)
    ImmixCollectionReason gc_request_reason;
    size_t gc_requested_bytes;
    volatile s64 gc_gen;         //incremented after every completed cycle
    s64 trim_last_ms;            //wall clock of the last immix trim (cooldown)

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

void gc_make_room(MiniJVM *jvm);

void gc_resume(GcCollector *collector);

MemoryBlock *gc_is_alive(GcCollector *collector, __refer obj);

void gc_obj_hold(GcCollector *collector, __refer ref);

void gc_obj_release(GcCollector *collector, __refer ref);

/* Registers ref with the collector by appending it to the thread's
 * objs_array; the GC splices that array into the global one at pause. */
void gc_obj_reg(Runtime *runtime, __refer ref);

void gc_move_objs_thread_2_gc(Runtime *runtime);

/* Immix side-list registration (no-op on the malloc backend). */
void gc_side_register_instance(Runtime *runtime, Instance *ins);
void gc_side_register_jthread_for_jvm(MiniJVM *jvm, Instance *ins);

void gc_dump_runtime(GcCollector *collector);

s64 gc_sum_heap(GcCollector *collector);

/* Non-zero when Java objects live in the Immix block backend. */
s32 gc_backend_is_immix(MiniJVM *jvm);

/*
 * Unified Java object storage entry. Returns zeroed memory sized insSize,
 * backed either by the Immix block backend or jvm_calloc.
 */
void *gc_obj_alloc(Runtime *runtime, s32 insSize, ImmixObjectKind kind);

/* Iterates every Java heap object (Immix blocks/LOS plus the class list). */
typedef s32 (*GcHeapObjectIter)(MemoryBlock *mb, void *data);

s32 gc_iterate_heap_objects(GcCollector *collector, GcHeapObjectIter iter, void *data);

/* System memory pressure entry for embedders (Immix backend). */
void gc_notify_memory_pressure(MiniJVM *jvm, s32 pressure_level);


#ifdef __cplusplus
}
#endif

#endif //_GARBAGE_H
