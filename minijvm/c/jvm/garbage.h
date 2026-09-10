

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
    GcObjectLink *header, *tmp_header, *tmp_tailer; //external registration nodes
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

    // Recycled node slab for GcObjectLink allocation (managed accounting).
    struct {
        GcObjectLink *free_list;
        GcObjectLink **chunks;
        s32 chunk_count;
        s32 chunk_cap;
        s32 live_links;
        spinlock_t lock;
    } link_slab;

    // Classic (malloc backend) pending re-mark queue: objects that ran
    // finalize() or were enqueued as weak references this cycle and must
    // survive the sweep (replaces MemoryBlock.tmp_next).
    ArrayList *classic_pending;

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

/* Registers ref with the collector via an external GcObjectLink.
 * Returns 0 on success; -1 means the link slab could not grow and the
 * caller must recycle the not-yet-published object and fail with OOM. */
void gc_obj_reg(Runtime *runtime, __refer ref);

void gc_move_objs_thread_2_gc(Runtime *runtime);

/* Returns a thread's cached free GcObjectLink nodes to the slab (thread exit). */
void gc_link_cache_flush(GcCollector *collector, JavaThreadInfo *ti);

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
