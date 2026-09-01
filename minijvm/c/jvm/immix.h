//
// miniJVM Immix heap architecture.
//
// Phase A provides an allocation-backend boundary while retaining the current
// per-object allocator and collector. Phase B will implement a non-moving
// Immix block/line heap for Java instances and arrays. The exact root scanner,
// Java reference semantics, finalization, weak references and class unloading
// remain owned by garbage.c.
//

#ifndef MINI_JVM_IMMIX_H
#define MINI_JVM_IMMIX_H

#include <stddef.h>

#include "jvm.h"

#ifdef __cplusplus
extern "C" {
#endif

#define IMMIX_API_VERSION 2u

#define IMMIX_DEFAULT_BLOCK_SIZE (32u * 1024u)
#define IMMIX_DEFAULT_LINE_SIZE 256u
#define IMMIX_DEFAULT_OBJECT_ALIGNMENT 8u
#define IMMIX_DEFAULT_LARGE_OBJECT_THRESHOLD (8u * 1024u)
#define IMMIX_DEFAULT_CHUNK_SIZE (1024u * 1024u)
#define IMMIX_DEFAULT_GC_TRIGGER_PERCENT 75u
#define IMMIX_DEFAULT_DEBT_GROWTH_PERCENT 50u
#define IMMIX_DEFAULT_MIN_ALLOCATION_DEBT (4u * 1024u * 1024u)
#define IMMIX_DEFAULT_EMERGENCY_RESERVE (1024u * 1024u)

typedef struct ImmixHeap ImmixHeap;
typedef struct ImmixMutator ImmixMutator;
typedef struct ImmixChunk ImmixChunk;
typedef struct ImmixBlock ImmixBlock;

typedef enum ImmixResult {
    IMMIX_OK = 0,
    IMMIX_ERR_INVALID_ARGUMENT = -1,
    IMMIX_ERR_OUT_OF_MEMORY = -2,
    IMMIX_ERR_UNSUPPORTED = -3,
    IMMIX_ERR_INVALID_STATE = -4,
    IMMIX_ERR_NOT_IMPLEMENTED = -5,
    IMMIX_ERR_VISITOR_STOPPED = -6
} ImmixResult;

/*
 * MALLOC is the Phase-A compatibility backend. It keeps the current
 * jvm_malloc/jvm_free behavior and does not own object enumeration or sweep.
 * BLOCK is the Phase-B non-moving Immix backend. It is deliberately disabled
 * until its block allocator and collector are implemented and verified.
 */
typedef enum ImmixBackend {
    IMMIX_BACKEND_MALLOC = 0,
    IMMIX_BACKEND_BLOCK = 1
} ImmixBackend;

typedef enum ImmixObjectKind {
    IMMIX_OBJECT_INSTANCE = 0,
    IMMIX_OBJECT_ARRAY = 1,
    IMMIX_OBJECT_VM_METADATA = 2,
    IMMIX_OBJECT_LARGE = 3
} ImmixObjectKind;

typedef enum ImmixBlockState {
    IMMIX_BLOCK_UNUSED = 0,
    IMMIX_BLOCK_FREE = 1,
    IMMIX_BLOCK_ACTIVE = 2,
    IMMIX_BLOCK_RECYCLABLE = 3,
    IMMIX_BLOCK_FULL = 4,
    IMMIX_BLOCK_COLLECTING = 5,
    IMMIX_BLOCK_DECOMMITTED = 6
} ImmixBlockState;

typedef enum ImmixCollectionReason {
    IMMIX_GC_ALLOCATION_DEBT = 0,
    IMMIX_GC_HEAP_LIMIT = 1,
    IMMIX_GC_EXPLICIT = 2,
    IMMIX_GC_MEMORY_PRESSURE = 3,
    IMMIX_GC_SHUTDOWN = 4
} ImmixCollectionReason;

typedef enum ImmixMemoryPressure {
    IMMIX_MEMORY_PRESSURE_NONE = 0,
    IMMIX_MEMORY_PRESSURE_WARNING = 1,
    IMMIX_MEMORY_PRESSURE_CRITICAL = 2
} ImmixMemoryPressure;

/*
 * The callbacks intentionally describe virtual-memory operations rather than
 * mmap/VirtualAlloc. Mobile and desktop ports can provide platform-specific
 * implementations without putting platform branches in the allocator core.
 */
typedef struct ImmixPlatformOps {
    void *context;
    void *(*reserve)(void *context, size_t size, size_t alignment);
    ImmixResult (*commit)(void *context, void *address, size_t size);
    ImmixResult (*decommit)(void *context, void *address, size_t size);
    void (*release)(void *context, void *address, size_t size);
} ImmixPlatformOps;

/*
 * The allocator requests a synchronous collection through this boundary on a
 * block-allocation slow path. The VM owns safepoints and exception delivery;
 * returning an error lets the allocation caller raise OutOfMemoryError.
 */
typedef struct ImmixVmOps {
    void *context;
    ImmixResult (*request_collection)(void *context,
                                      JavaThreadInfo *requesting_thread,
                                      ImmixCollectionReason reason,
                                      size_t requested_bytes);
} ImmixVmOps;

typedef struct ImmixConfig {
    u32 api_version;
    ImmixBackend backend;

    size_t heap_limit;
    size_t initial_chunk_size;
    size_t growth_chunk_size;
    size_t allocation_debt_limit;
    /* Block storage reserved for the GC thread. Address space/metadata are
     * created eagerly, while object pages are committed only when used. */
    size_t emergency_reserve_size;

    u32 block_size;
    u32 line_size;
    u32 object_alignment;
    u32 large_object_threshold;
    u32 gc_trigger_percent;
    /* Next GC target = max(allocation_debt_limit, live * debt_growth_percent / 100). */
    u32 debt_growth_percent;

    u8 zero_on_allocate;
    u8 enable_decommit;
    u8 enable_validation;
    u8 reserved;
} ImmixConfig;

typedef struct ImmixStats {
    u64 requested_bytes;
    u64 live_bytes;
    u64 committed_bytes;
    u64 reserved_bytes;
    u64 metadata_bytes;
    u64 allocation_debt;
    u64 allocation_count;
    u64 live_object_count;
    u64 large_object_bytes;
    u64 large_object_count;
    u64 collection_count;
    u64 reclaimed_bytes;
    u64 reclaimed_object_count;
    u64 failed_allocation_count;

    u32 chunk_count;
    u32 free_block_count;
    u32 recyclable_block_count;
    u32 active_block_count;
    u32 full_block_count;
    u32 decommitted_block_count;
} ImmixStats;

/* Return zero to continue visiting and non-zero to stop. */
typedef s32 (*ImmixObjectVisitor)(void *context,
                                  MemoryBlock *object,
                                  size_t object_size,
                                  ImmixObjectKind kind);

/*
 * garbage.c must finish finalizable-object retention and weak-reference
 * processing before this predicate is used by the Phase-B sweep.
 */
typedef s32 (*ImmixIsObjectLive)(void *context, MemoryBlock *object);

/*
 * Releases monitors/native peers/class-loader side data for a dead object.
 * It must not free the object's storage; storage belongs to Immix.
 */
typedef void (*ImmixBeforeReclaim)(void *context, MemoryBlock *object);

typedef struct ImmixSweepOps {
    void *context;
    ImmixIsObjectLive is_live;
    ImmixBeforeReclaim before_reclaim;
} ImmixSweepOps;

/* Configuration and lifecycle. */
void immix_config_init(ImmixConfig *config);

ImmixResult immix_heap_create(MiniJVM *jvm,
                              const ImmixConfig *config,
                              const ImmixPlatformOps *platform,
                              const ImmixVmOps *vm_ops,
                              ImmixHeap **out_heap);

void immix_heap_destroy(ImmixHeap *heap);

const ImmixConfig *immix_heap_config(const ImmixHeap *heap);

/* Per-Java-thread allocation context. */
ImmixResult immix_mutator_attach(ImmixHeap *heap,
                                 JavaThreadInfo *thread_info,
                                 ImmixMutator **out_mutator);

void immix_mutator_flush(ImmixMutator *mutator);

void immix_mutator_detach(ImmixMutator *mutator);

/*
 * Publishes every attached mutator's cursor/debt into the heap. Called by the
 * collector after the world is stopped; the sweep relies on the published
 * boundaries to keep per-thread bump regions from being reallocated.
 */
void immix_flush_all_mutators(ImmixHeap *heap);

/* Object storage. All returned storage is suitably aligned for Java objects. */
void *immix_alloc(ImmixMutator *mutator,
                  size_t size,
                  ImmixObjectKind kind);

void *immix_alloc_slow(ImmixMutator *mutator,
                       size_t size,
                       ImmixObjectKind kind);

void *immix_alloc_large(ImmixMutator *mutator,
                        size_t size,
                        ImmixObjectKind kind);

/*
 * Phase A frees the individual allocation. Phase B only performs object-side
 * cleanup elsewhere and reclaims storage during sweep; calling this for a
 * Phase-B object will therefore be rejected.
 */
ImmixResult immix_release(ImmixHeap *heap, void *object, size_t size);

/* Stop-the-world collection boundary used by garbage.c. */
ImmixResult immix_collection_begin(ImmixHeap *heap,
                                   ImmixCollectionReason reason,
                                   u8 mark_epoch);

ImmixResult immix_collection_mark(ImmixHeap *heap,
                                  MemoryBlock *object,
                                  size_t object_size);

ImmixResult immix_collection_sweep(ImmixHeap *heap,
                                   const ImmixSweepOps *ops);

ImmixResult immix_collection_end(ImmixHeap *heap);

void immix_collection_abort(ImmixHeap *heap);

/* Heap enumeration and ownership queries. */
ImmixResult immix_visit_objects(ImmixHeap *heap,
                                ImmixObjectVisitor visitor,
                                void *context);

s32 immix_contains(const ImmixHeap *heap, const void *address);

s32 immix_is_large_object(const ImmixHeap *heap, const void *object);

/*
 * Non-zero when address is the exact start of an allocated (not yet swept)
 * object. Used by the VM to answer liveness queries for raw pointers.
 */
s32 immix_is_object_start(const ImmixHeap *heap, const void *address);

/* Triggering, pressure response, accounting and diagnostics. */
s32 immix_should_collect(const ImmixHeap *heap, size_t incoming_bytes);

void immix_notify_memory_pressure(ImmixHeap *heap,
                                  ImmixMemoryPressure pressure);

ImmixResult immix_trim(ImmixHeap *heap, size_t target_committed_bytes);

void immix_get_stats(const ImmixHeap *heap, ImmixStats *out_stats);

ImmixResult immix_verify(const ImmixHeap *heap);

const c8 *immix_result_string(ImmixResult result);

/*
 * Reserved for the later Sticky-Immix phase. It is a no-op in Phases A/B, but
 * defining the boundary now avoids another public API redesign when card
 * marking is introduced.
 */
void immix_write_barrier_post(ImmixHeap *heap,
                              MemoryBlock *owner,
                              __refer *slot,
                              __refer value);

void immix_write_barrier_range(ImmixHeap *heap,
                               MemoryBlock *owner,
                               __refer *first_slot,
                               size_t slot_count);

#ifdef __cplusplus
}
#endif

#endif // MINI_JVM_IMMIX_H
