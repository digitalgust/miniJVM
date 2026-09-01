//
// miniJVM Immix heap: Phase-A malloc backend and Phase-B non-moving block
// backend.
//
// Phase B layout: the heap grows in aligned chunks that are tiled into
// block_size blocks; each block is split into line_size lines. Mutators bump
// allocate inside holes; object start positions are tracked in a side bitmap
// and live data is tracked with per-line mark bytes. Objects never move, so
// raw Instance*/arr_body pointers in JNI and JIT code stay valid.
//
// Deviations from the original plan in minijvm-memory-footprint-analysis.md
// are listed in that document, section 16.
//

#include "immix.h"

#include <limits.h>
#include <string.h>

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#else
#include <sys/mman.h>
#include <unistd.h>
#endif

#define IMMIX_HEAP_MAGIC 0x494d4d58u /* IMMX */

#define IMMIX_LINE_FREE 0u
#define IMMIX_LINE_LIVE 1u

/* Publish per-mutator debt at least every 256 KiB; publishing costs the
 * global heap lock, so it must stay far below the debt trigger floor. */
#define IMMIX_DEBT_PUBLISH_BYTES (256u * 1024u)
#define IMMIX_LOS_BUCKET_COUNT 256u

typedef struct ImmixBlockList {
    ImmixBlock *head;
    ImmixBlock *tail;
    u32 count;
} ImmixBlockList;

typedef struct ImmixLargeObject {
    struct ImmixLargeObject *next;
    struct ImmixLargeObject *hash_next;
    void *address;     /* aligned object address */
    void *reservation; /* raw reservation base, required for release */
    size_t size;
    size_t reserved_size;
    u8 marked_epoch;
    u8 kind;
} ImmixLargeObject;

/*
 * Side metadata lives outside the Java object area (plain jvm_calloc), so a
 * decommitted block keeps its maps and can be recommitted cheaply.
 */
struct ImmixBlock {
    ImmixBlock *next;
    ImmixChunk *chunk;
    u8 *start;
    u8 *line_marks;    /* per line: 1 when a live object covers the line */
    u8 *line_state;    /* per line: LIVE/FREE as of last sweep or release */
    u8 *object_starts; /* bitmap over block offsets at object_alignment */
    u8 *pub_cursor;    /* published bump cursor while the owner is parked */
    u8 *pub_limit;
    u32 index_in_chunk;
    u32 live_bytes;
    u32 live_object_count;
    u32 free_line_count;
    u16 best_hole_line;  /* largest free run, cached for O(1) acquisition filter */
    u16 best_hole_lines;
    ImmixBlockState state;
    u8 ever_used;      /* 0 -> object pages are still OS zero, may skip memset */
    u8 decommitted;    /* 1 -> pages returned to the OS, needs recommit */
    u8 reserved[2];
};

struct ImmixChunk {
    ImmixChunk *next;
    void *reservation; /* raw reservation base returned by the platform */
    u8 *block_base;    /* block-aligned start of the usable region */
    size_t reserved_size;
    size_t committed_size;
    u32 block_count;
    ImmixBlock *blocks;
    u8 *metadata;
    size_t metadata_size;
    u8 emergency;
};

struct ImmixHeap {
    u32 magic;
    MiniJVM *jvm;
    ImmixConfig config;
    ImmixPlatformOps platform;
    ImmixVmOps vm_ops;
    spinlock_t lock;

    ImmixChunk *chunks;
    ImmixChunk *chunk_lookup_hint;
    ImmixLargeObject *large_objects;
    ImmixLargeObject *large_object_buckets[IMMIX_LOS_BUCKET_COUNT];
    ImmixBlockList free_blocks;
    ImmixBlockList recyclable_blocks;
    ImmixBlockList active_blocks; /* bookkeeping count only; owned blocks are unlinked */
    ImmixBlockList full_blocks;
    ImmixBlockList collecting_blocks;

    ImmixMutator *mutators; /* registry of attached mutators */
    u32 lines_per_block;
    u32 start_bitmap_bytes;
    u64 managed_capacity_bytes; /* usable block bytes + live LOS storage */

    ImmixStats stats;
    ImmixCollectionReason collection_reason;
    ImmixMemoryPressure memory_pressure;
    u8 collecting;
    u8 mark_epoch;
};

struct ImmixMutator {
    ImmixHeap *heap;
    JavaThreadInfo *thread_info;
    struct ImmixMutator *next;
    ImmixBlock *block;
    u8 *cursor;
    u8 *limit;
    u8 *hole_start;
    u64 allocated_bytes;
    u64 local_debt;
    u64 local_count;
};

/* Phase-B backend boundary. */
static ImmixResult immix_block_backend_initialize(ImmixHeap *heap);
static void immix_block_backend_shutdown(ImmixHeap *heap);
static ImmixResult immix_chunk_create(ImmixHeap *heap, size_t chunk_bytes,
                                      s32 emergency);
static ImmixResult immix_block_mutator_initialize(ImmixMutator *mutator);
static void immix_block_mutator_flush(ImmixMutator *mutator);
static void immix_block_mutator_release(ImmixMutator *mutator);
static void *immix_block_allocate(ImmixMutator *mutator,
                                  size_t size,
                                  ImmixObjectKind kind,
                                  s32 slow_path);
static ImmixResult immix_block_mark_object(ImmixHeap *heap,
                                           MemoryBlock *object,
                                           size_t object_size);
static ImmixResult immix_block_object_size(const ImmixHeap *heap,
                                           const ImmixBlock *block,
                                           const u8 *address,
                                           size_t raw_size,
                                           size_t *out_aligned_size);
static ImmixResult immix_block_mark_range(ImmixHeap *heap,
                                          ImmixBlock *block,
                                          const u8 *address,
                                          size_t aligned_size);
static ImmixResult immix_block_sweep(ImmixHeap *heap, const ImmixSweepOps *ops);
static ImmixResult immix_block_visit_objects(ImmixHeap *heap,
                                             ImmixObjectVisitor visitor,
                                             void *context);
static s32 immix_block_contains(const ImmixHeap *heap, const void *address);
static s32 immix_block_is_large_object(const ImmixHeap *heap, const void *object);
static ImmixResult immix_block_trim(ImmixHeap *heap, size_t target_committed_bytes);

/* ------------------------------------------------------------------------- */
/* Platform defaults                                                         */
/* ------------------------------------------------------------------------- */

static void *immix_platform_reserve_default(void *context, size_t size, size_t alignment) {
#if defined(_WIN32) || defined(_WIN64)
    (void) context;
    (void) alignment;
    if (size == 0) return NULL;
    return VirtualAlloc(NULL, size, MEM_RESERVE, PAGE_NOACCESS);
#else
    void *result;
    int flags = MAP_PRIVATE | MAP_ANONYMOUS;
    (void) context;
    (void) alignment;
    if (size == 0) return NULL;
#ifdef MAP_NORESERVE
    flags |= MAP_NORESERVE;
#endif
    result = mmap(NULL, size, PROT_NONE, flags, -1, 0);
    return result == MAP_FAILED ? NULL : result;
#endif
}

static ImmixResult immix_platform_commit_default(void *context, void *address, size_t size) {
    if (!address || size == 0) return IMMIX_ERR_INVALID_ARGUMENT;
#if defined(_WIN32) || defined(_WIN64)
    (void) context;
    if (!VirtualAlloc(address, size, MEM_COMMIT, PAGE_READWRITE)) {
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    return IMMIX_OK;
#else
    (void) context;
    if (mprotect(address, size, PROT_READ | PROT_WRITE) != 0) {
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    return IMMIX_OK;
#endif
}

static ImmixResult immix_platform_decommit_default(void *context, void *address, size_t size) {
    if (!address || size == 0) return IMMIX_ERR_INVALID_ARGUMENT;
#if defined(_WIN32) || defined(_WIN64)
    (void) context;
    if (!VirtualFree(address, size, MEM_DECOMMIT)) {
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    return IMMIX_OK;
#else
    /* Discards physical pages; the next touch sees zeroed memory. */
    (void) context;
    if (madvise(address, size, MADV_DONTNEED) != 0) {
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    return IMMIX_OK;
#endif
}

static void immix_platform_release_default(void *context, void *address, size_t size) {
    if (!address) return;
#if defined(_WIN32) || defined(_WIN64)
    (void) size;
    (void) context;
    VirtualFree(address, 0, MEM_RELEASE);
#else
    (void) context;
    munmap(address, size);
#endif
}

static ImmixPlatformOps immix_default_platform_ops(void) {
    ImmixPlatformOps ops;
    memset(&ops, 0, sizeof(ops));
    ops.reserve = immix_platform_reserve_default;
    ops.commit = immix_platform_commit_default;
    ops.decommit = immix_platform_decommit_default;
    ops.release = immix_platform_release_default;
    return ops;
}

/* ------------------------------------------------------------------------- */
/* Small helpers                                                             */
/* ------------------------------------------------------------------------- */

static s32 immix_is_power_of_two(size_t value) {
    return value != 0 && (value & (value - 1)) == 0;
}

static size_t immix_align_up(size_t value, size_t alignment) {
    size_t mask;

    if (!immix_is_power_of_two(alignment)) return 0;
    mask = alignment - 1;
    if (value > SIZE_MAX - mask) return 0;
    return (value + mask) & ~mask;
}

static s32 immix_heap_is_valid(const ImmixHeap *heap) {
    return heap != NULL && heap->magic == IMMIX_HEAP_MAGIC;
}

static u32 immix_line_of(const ImmixHeap *heap, const u8 *block_start, const u8 *address) {
    return (u32) ((size_t) (address - block_start) / heap->config.line_size);
}

static const u8 *immix_line_address(const ImmixHeap *heap, const ImmixBlock *block, u32 line) {
    return block->start + (size_t) line * heap->config.line_size;
}

static u32 immix_start_index_of(const ImmixHeap *heap, const ImmixBlock *block,
                                const u8 *address) {
    return (u32) ((size_t) (address - block->start) / heap->config.object_alignment);
}

static u32 immix_ctz8(u8 value) {
    u32 count = 0;
    while ((value & 1u) == 0u) {
        value >>= 1;
        count++;
    }
    return count;
}

/* Finds the next set object-start bit without testing every alignment slot. */
static s32 immix_next_start_bit(const ImmixHeap *heap,
                                const ImmixBlock *block,
                                u32 from_bit,
                                u32 *out_bit) {
    u32 byte_index = from_bit >> 3;
    u32 bit_in_byte = from_bit & 7u;

    while (byte_index < heap->start_bitmap_bytes) {
        u8 value = block->object_starts[byte_index];
        if (bit_in_byte != 0) {
            value &= (u8) (0xffu << bit_in_byte);
        }
        if (value != 0) {
            *out_bit = (byte_index << 3) + immix_ctz8(value);
            return 1;
        }
        byte_index++;
        bit_in_byte = 0;
    }
    return 0;
}

static void immix_start_bit_set(const ImmixHeap *heap, ImmixBlock *block, const u8 *address) {
    u32 index = immix_start_index_of(heap, block, address);
    block->object_starts[index >> 3] |= (u8) (1u << (index & 7));
}

static void immix_start_bit_clear(const ImmixHeap *heap, ImmixBlock *block, const u8 *address) {
    u32 index = immix_start_index_of(heap, block, address);
    block->object_starts[index >> 3] &= (u8) ~(1u << (index & 7));
}

static s32 immix_start_bit_test(const ImmixHeap *heap, const ImmixBlock *block,
                                const u8 *address) {
    u32 index = immix_start_index_of(heap, block, address);
    return (block->object_starts[index >> 3] >> (index & 7)) & 1;
}

static ImmixBlock *immix_block_of_address(ImmixHeap *heap, const void *address) {
    ImmixChunk *chunk;
    uintptr_t addr = (uintptr_t) address;

    if (!address) return NULL;
    chunk = heap->chunk_lookup_hint;
    if (chunk) {
        uintptr_t base = (uintptr_t) chunk->block_base;
        uintptr_t end = base + (size_t) chunk->block_count * heap->config.block_size;
        if (addr >= base && addr < end) {
            return &chunk->blocks[(addr - base) / heap->config.block_size];
        }
    }
    for (chunk = heap->chunks; chunk; chunk = chunk->next) {
        uintptr_t base = (uintptr_t) chunk->block_base;
        uintptr_t end = base + (size_t) chunk->block_count * heap->config.block_size;
        if (addr >= base && addr < end) {
            heap->chunk_lookup_hint = chunk;
            return &chunk->blocks[(addr - base) / heap->config.block_size];
        }
    }
    return NULL;
}

static u32 immix_los_bucket_of(const void *address) {
    uintptr_t value = (uintptr_t) address;
    value ^= value >> 17;
    value ^= value >> 9;
    return (u32) value & (IMMIX_LOS_BUCKET_COUNT - 1u);
}

static ImmixLargeObject *immix_los_record_of(ImmixHeap *heap, const void *address) {
    ImmixLargeObject *los;
    u32 bucket;

    if (!address) return NULL;
    bucket = immix_los_bucket_of(address);
    for (los = heap->large_object_buckets[bucket]; los; los = los->hash_next) {
        if (los->address == address) return los;
    }
    return NULL;
}

static void immix_los_hash_insert(ImmixHeap *heap, ImmixLargeObject *los) {
    u32 bucket = immix_los_bucket_of(los->address);
    los->hash_next = heap->large_object_buckets[bucket];
    heap->large_object_buckets[bucket] = los;
}

static void immix_los_hash_remove(ImmixHeap *heap, ImmixLargeObject *los) {
    u32 bucket = immix_los_bucket_of(los->address);
    ImmixLargeObject **link = &heap->large_object_buckets[bucket];
    while (*link) {
        if (*link == los) {
            *link = los->hash_next;
            los->hash_next = NULL;
            return;
        }
        link = &(*link)->hash_next;
    }
}

/* ------------------------------------------------------------------------- */
/* Block lists (all mutations happen under heap->lock)                       */
/* ------------------------------------------------------------------------- */

static void immix_block_list_push(ImmixBlockList *list, ImmixBlock *block) {
    block->next = NULL;
    if (list->tail) {
        list->tail->next = block;
    } else {
        list->head = block;
    }
    list->tail = block;
    list->count++;
}

static ImmixBlock *immix_block_list_pop(ImmixBlockList *list) {
    ImmixBlock *block = list->head;
    if (!block) return NULL;
    list->head = block->next;
    if (!list->head) list->tail = NULL;
    block->next = NULL;
    list->count--;
    return block;
}

static void immix_block_list_unlink(ImmixBlockList *list, ImmixBlock *block) {
    ImmixBlock *cursor = list->head;
    ImmixBlock *prev = NULL;

    while (cursor) {
        if (cursor == block) {
            if (prev) prev->next = cursor->next;
            else list->head = cursor->next;
            if (list->tail == block) {
                list->tail = prev;
                if (!list->tail) list->head = NULL;
            }
            block->next = NULL;
            list->count--;
            return;
        }
        prev = cursor;
        cursor = cursor->next;
    }
}

/* ------------------------------------------------------------------------- */
/* Statistics helpers                                                        */
/* ------------------------------------------------------------------------- */

static void immix_publish_mutator_stats(ImmixHeap *heap, ImmixMutator *mutator) {
    if (mutator->local_debt == 0 && mutator->local_count == 0) return;
    spin_lock(&heap->lock);
    heap->stats.requested_bytes += mutator->local_debt;
    heap->stats.live_bytes += mutator->local_debt;
    heap->stats.allocation_debt += mutator->local_debt;
    heap->stats.allocation_count += mutator->local_count;
    heap->stats.live_object_count += mutator->local_count;
    mutator->allocated_bytes += mutator->local_debt;
    mutator->local_debt = 0;
    mutator->local_count = 0;
    spin_unlock(&heap->lock);
}

static void immix_record_fast_allocation(ImmixMutator *mutator, size_t aligned_size) {
    mutator->local_debt += (u64) aligned_size;
    mutator->local_count++;
    if (mutator->local_debt >= IMMIX_DEBT_PUBLISH_BYTES) {
        immix_publish_mutator_stats(mutator->heap, mutator);
    }
}

static void immix_stats_record_allocation_failure(ImmixHeap *heap) {
    spin_lock(&heap->lock);
    heap->stats.failed_allocation_count++;
    spin_unlock(&heap->lock);
}

static void immix_stats_record_release(ImmixHeap *heap, size_t size) {
    u64 released = (u64) size;
    u64 requested_released = released;

    spin_lock(&heap->lock);
    if (released > heap->stats.live_bytes) released = heap->stats.live_bytes;
    if (requested_released > heap->stats.requested_bytes) {
        requested_released = heap->stats.requested_bytes;
    }
    heap->stats.live_bytes -= released;
    heap->stats.requested_bytes -= requested_released;
    heap->stats.reclaimed_bytes += released;
    if (heap->stats.live_object_count > 0) heap->stats.live_object_count--;
    spin_unlock(&heap->lock);
}

/* ------------------------------------------------------------------------- */
/* Malloc backend (Phase A)                                                  */
/* ------------------------------------------------------------------------- */

static void *immix_malloc_allocate(ImmixMutator *mutator,
                                   size_t size,
                                   ImmixObjectKind kind) {
    ImmixHeap *heap = mutator->heap;
    void *result;
    size_t aligned_size;
    s32 is_large;

    aligned_size = immix_align_up(size, heap->config.object_alignment);
    if (aligned_size == 0 || aligned_size > UINT_MAX) {
        immix_stats_record_allocation_failure(heap);
        return NULL;
    }

    result = heap->config.zero_on_allocate
             ? jvm_calloc((u32) aligned_size)
             : jvm_malloc((u32) aligned_size);
    if (!result) {
        immix_stats_record_allocation_failure(heap);
        return NULL;
    }

    is_large = kind == IMMIX_OBJECT_LARGE ||
               aligned_size >= heap->config.large_object_threshold;
    spin_lock(&heap->lock);
    heap->stats.requested_bytes += (u64) aligned_size;
    heap->stats.live_bytes += (u64) aligned_size;
    heap->stats.allocation_debt += (u64) aligned_size;
    heap->stats.allocation_count++;
    heap->stats.live_object_count++;
    if (is_large) {
        heap->stats.large_object_bytes += (u64) aligned_size;
        heap->stats.large_object_count++;
    }
    spin_unlock(&heap->lock);
    mutator->allocated_bytes += (u64) aligned_size;
    return result;
}

/* ------------------------------------------------------------------------- */
/* Public configuration / lifecycle                                          */
/* ------------------------------------------------------------------------- */

void immix_config_init(ImmixConfig *config) {
    if (!config) return;

    memset(config, 0, sizeof(ImmixConfig));
    config->api_version = IMMIX_API_VERSION;
    config->backend = IMMIX_BACKEND_MALLOC;
    config->initial_chunk_size = IMMIX_DEFAULT_CHUNK_SIZE;
    config->growth_chunk_size = IMMIX_DEFAULT_CHUNK_SIZE;
    config->allocation_debt_limit = IMMIX_DEFAULT_MIN_ALLOCATION_DEBT;
    config->emergency_reserve_size = IMMIX_DEFAULT_EMERGENCY_RESERVE;
    config->block_size = IMMIX_DEFAULT_BLOCK_SIZE;
    config->line_size = IMMIX_DEFAULT_LINE_SIZE;
    config->object_alignment = IMMIX_DEFAULT_OBJECT_ALIGNMENT;
    config->large_object_threshold = IMMIX_DEFAULT_LARGE_OBJECT_THRESHOLD;
    config->gc_trigger_percent = IMMIX_DEFAULT_GC_TRIGGER_PERCENT;
    config->debt_growth_percent = IMMIX_DEFAULT_DEBT_GROWTH_PERCENT;
    config->zero_on_allocate = 1;
    config->enable_decommit = 1;
}

static ImmixResult immix_validate_config(const ImmixConfig *config) {
    if (!config || config->api_version != IMMIX_API_VERSION) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (config->backend != IMMIX_BACKEND_MALLOC &&
        config->backend != IMMIX_BACKEND_BLOCK) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (!immix_is_power_of_two(config->block_size) ||
        !immix_is_power_of_two(config->line_size) ||
        !immix_is_power_of_two(config->object_alignment)) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (config->line_size >= config->block_size ||
        config->block_size % config->line_size != 0 ||
        config->object_alignment > config->line_size) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (config->large_object_threshold == 0 ||
        config->large_object_threshold >= config->block_size) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (config->gc_trigger_percent == 0 ||
        config->gc_trigger_percent >= 100) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (config->heap_limit != 0 &&
        config->heap_limit < config->block_size) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (config->initial_chunk_size < config->block_size ||
        config->initial_chunk_size % config->block_size != 0 ||
        config->growth_chunk_size < config->block_size ||
        config->growth_chunk_size % config->block_size != 0) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    return IMMIX_OK;
}

ImmixResult immix_heap_create(MiniJVM *jvm,
                              const ImmixConfig *config,
                              const ImmixPlatformOps *platform,
                              const ImmixVmOps *vm_ops,
                              ImmixHeap **out_heap) {
    ImmixConfig local_config;
    ImmixHeap *heap;
    ImmixResult result;

    if (!jvm || !out_heap) return IMMIX_ERR_INVALID_ARGUMENT;
    *out_heap = NULL;

    if (config) {
        local_config = *config;
    } else {
        immix_config_init(&local_config);
    }

    result = immix_validate_config(&local_config);
    if (result != IMMIX_OK) return result;

    /* Keep the default 1 MiB reserve on ordinary mobile/desktop heaps, but
     * do not let it dominate deliberately tiny test/embedded heaps. */
    if (local_config.backend == IMMIX_BACKEND_BLOCK) {
        if (local_config.heap_limit == 0 ||
            local_config.emergency_reserve_size == 0) {
            local_config.emergency_reserve_size = 0;
        } else {
            size_t reserve_cap = local_config.heap_limit / 16u;
            reserve_cap -= reserve_cap % local_config.block_size;
            local_config.emergency_reserve_size -=
                    local_config.emergency_reserve_size % local_config.block_size;
            if (local_config.emergency_reserve_size > reserve_cap) {
                local_config.emergency_reserve_size = reserve_cap;
            }
        }
    } else {
        local_config.emergency_reserve_size = 0;
    }

    heap = (ImmixHeap *) jvm_calloc((u32) sizeof(ImmixHeap));
    if (!heap) return IMMIX_ERR_OUT_OF_MEMORY;

    heap->magic = IMMIX_HEAP_MAGIC;
    heap->jvm = jvm;
    heap->config = local_config;
    heap->platform = platform ? *platform : immix_default_platform_ops();
    if (vm_ops) heap->vm_ops = *vm_ops;
    spin_init(&heap->lock, 0);
    heap->lines_per_block = local_config.block_size / local_config.line_size;
    heap->start_bitmap_bytes =
            (local_config.block_size / local_config.object_alignment + 7) / 8;

    if (heap->config.backend == IMMIX_BACKEND_BLOCK) {
        if (!heap->platform.reserve || !heap->platform.commit ||
            !heap->platform.decommit || !heap->platform.release) {
            result = IMMIX_ERR_INVALID_ARGUMENT;
        } else {
            result = immix_block_backend_initialize(heap);
        }
        if (result != IMMIX_OK) {
            heap->magic = 0;
            spin_destroy(&heap->lock);
            jvm_free(heap);
            return result;
        }
    }

    *out_heap = heap;
    return IMMIX_OK;
}

void immix_heap_destroy(ImmixHeap *heap) {
    if (!immix_heap_is_valid(heap)) return;

    if (heap->config.backend == IMMIX_BACKEND_BLOCK) {
        immix_block_backend_shutdown(heap);
    }
    heap->magic = 0;
    spin_destroy(&heap->lock);
    jvm_free(heap);
}

const ImmixConfig *immix_heap_config(const ImmixHeap *heap) {
    return immix_heap_is_valid(heap) ? &heap->config : NULL;
}

/* ------------------------------------------------------------------------- */
/* Mutator lifecycle                                                         */
/* ------------------------------------------------------------------------- */

ImmixResult immix_mutator_attach(ImmixHeap *heap,
                                 JavaThreadInfo *thread_info,
                                 ImmixMutator **out_mutator) {
    ImmixMutator *mutator;
    ImmixResult result;

    if (!immix_heap_is_valid(heap) || !thread_info || !out_mutator) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    *out_mutator = NULL;

    mutator = (ImmixMutator *) jvm_calloc((u32) sizeof(ImmixMutator));
    if (!mutator) return IMMIX_ERR_OUT_OF_MEMORY;
    mutator->heap = heap;
    mutator->thread_info = thread_info;

    if (heap->config.backend == IMMIX_BACKEND_BLOCK) {
        result = immix_block_mutator_initialize(mutator);
        if (result != IMMIX_OK) {
            jvm_free(mutator);
            return result;
        }
    }

    spin_lock(&heap->lock);
    mutator->next = heap->mutators;
    heap->mutators = mutator;
    spin_unlock(&heap->lock);

    *out_mutator = mutator;
    return IMMIX_OK;
}

void immix_mutator_flush(ImmixMutator *mutator) {
    if (!mutator || !immix_heap_is_valid(mutator->heap)) return;
    immix_publish_mutator_stats(mutator->heap, mutator);
    if (mutator->heap->config.backend == IMMIX_BACKEND_BLOCK) {
        immix_block_mutator_flush(mutator);
    }
}

void immix_flush_all_mutators(ImmixHeap *heap) {
    ImmixMutator *mutator;

    if (!immix_heap_is_valid(heap)) return;
    /* Only called from the collector with the world stopped: no mutator can
     * attach, detach or advance a cursor while the registry is walked. */
    for (mutator = heap->mutators; mutator; mutator = mutator->next) {
        immix_mutator_flush(mutator);
    }
}

void immix_mutator_detach(ImmixMutator *mutator) {
    ImmixHeap *heap;
    ImmixMutator *cursor;

    if (!mutator) return;
    heap = mutator->heap;
    if (!immix_heap_is_valid(heap)) return;

    immix_publish_mutator_stats(heap, mutator);
    if (heap->config.backend == IMMIX_BACKEND_BLOCK) {
        immix_block_mutator_release(mutator);
    }

    spin_lock(&heap->lock);
    if (heap->mutators == mutator) {
        heap->mutators = mutator->next;
    } else {
        for (cursor = heap->mutators; cursor; cursor = cursor->next) {
            if (cursor->next == mutator) {
                cursor->next = mutator->next;
                break;
            }
        }
    }
    spin_unlock(&heap->lock);

    mutator->heap = NULL;
    mutator->thread_info = NULL;
    jvm_free(mutator);
}

/* ------------------------------------------------------------------------- */
/* Public allocation entry points                                            */
/* ------------------------------------------------------------------------- */

void *immix_alloc(ImmixMutator *mutator,
                  size_t size,
                  ImmixObjectKind kind) {
    ImmixHeap *heap;

    if (!mutator || size == 0 || !immix_heap_is_valid(mutator->heap)) {
        return NULL;
    }
    heap = mutator->heap;

    if (size >= heap->config.large_object_threshold ||
        kind == IMMIX_OBJECT_LARGE) {
        if (heap->config.backend == IMMIX_BACKEND_MALLOC) {
            return immix_malloc_allocate(mutator, size, kind);
        }
        return immix_block_allocate(mutator, size, kind, 0);
    }
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) {
        return immix_malloc_allocate(mutator, size, kind);
    }
    return immix_block_allocate(mutator, size, kind, 0);
}

void *immix_alloc_slow(ImmixMutator *mutator,
                       size_t size,
                       ImmixObjectKind kind) {
    if (!mutator || size == 0 || !immix_heap_is_valid(mutator->heap)) {
        return NULL;
    }
    if (mutator->heap->config.backend == IMMIX_BACKEND_MALLOC) {
        return immix_malloc_allocate(mutator, size, kind);
    }
    return immix_block_allocate(mutator, size, kind, 1);
}

void *immix_alloc_large(ImmixMutator *mutator,
                        size_t size,
                        ImmixObjectKind kind) {
    if (!mutator || size == 0 || !immix_heap_is_valid(mutator->heap)) {
        return NULL;
    }
    if (mutator->heap->config.backend == IMMIX_BACKEND_MALLOC) {
        return immix_malloc_allocate(mutator, size, IMMIX_OBJECT_LARGE);
    }
    return immix_block_allocate(mutator, size, kind, 1);
}

ImmixResult immix_release(ImmixHeap *heap, void *object, size_t size) {
    size_t aligned_size;

    if (!immix_heap_is_valid(heap) || !object || size == 0) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (heap->config.backend != IMMIX_BACKEND_MALLOC) {
        return IMMIX_ERR_INVALID_STATE;
    }

    aligned_size = immix_align_up(size, heap->config.object_alignment);
    if (aligned_size == 0) return IMMIX_ERR_INVALID_ARGUMENT;
    jvm_free(object);
    immix_stats_record_release(heap, aligned_size);
    return IMMIX_OK;
}

/* ------------------------------------------------------------------------- */
/* Collection boundary                                                       */
/* ------------------------------------------------------------------------- */

ImmixResult immix_collection_begin(ImmixHeap *heap,
                                   ImmixCollectionReason reason,
                                   u8 mark_epoch) {
    ImmixChunk *chunk;
    ImmixLargeObject *los;

    if (!immix_heap_is_valid(heap) || mark_epoch == 0) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }

    spin_lock(&heap->lock);
    if (heap->collecting) {
        spin_unlock(&heap->lock);
        return IMMIX_ERR_INVALID_STATE;
    }
    heap->collecting = 1;
    heap->collection_reason = reason;
    heap->mark_epoch = mark_epoch;
    heap->stats.collection_count++;
    /* MemoryBlock::garbage_mark is only 8-bit and wraps. A LOS record that
     * survived the same epoch 255 cycles ago must not look marked now. */
    for (los = heap->large_objects; los; los = los->next) {
        los->marked_epoch = 0;
    }
    for (chunk = heap->chunks; chunk; chunk = chunk->next) {
        u32 i;
        for (i = 0; i < chunk->block_count; i++) {
            ImmixBlock *b = &chunk->blocks[i];
            if (b->state != IMMIX_BLOCK_DECOMMITTED && b->line_marks) {
                memset(b->line_marks, 0, heap->lines_per_block);
            }
        }
    }
    spin_unlock(&heap->lock);
    return IMMIX_OK;
}

ImmixResult immix_collection_mark(ImmixHeap *heap,
                                  MemoryBlock *object,
                                  size_t object_size) {
    if (!immix_heap_is_valid(heap) || !object || object_size == 0) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (!heap->collecting) return IMMIX_ERR_INVALID_STATE;

    /* Malloc backend keeps marks in MemoryBlock::garbage_mark as before. */
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) return IMMIX_OK;
    return immix_block_mark_object(heap, object, object_size);
}

ImmixResult immix_collection_sweep(ImmixHeap *heap,
                                   const ImmixSweepOps *ops) {
    if (!immix_heap_is_valid(heap) || !ops || !ops->is_live) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (!heap->collecting) return IMMIX_ERR_INVALID_STATE;

    if (heap->config.backend == IMMIX_BACKEND_MALLOC) {
        return IMMIX_ERR_UNSUPPORTED;
    }
    return immix_block_sweep(heap, ops);
}

ImmixResult immix_collection_end(ImmixHeap *heap) {
    if (!immix_heap_is_valid(heap)) return IMMIX_ERR_INVALID_ARGUMENT;

    spin_lock(&heap->lock);
    if (!heap->collecting) {
        spin_unlock(&heap->lock);
        return IMMIX_ERR_INVALID_STATE;
    }
    heap->collecting = 0;
    heap->memory_pressure = IMMIX_MEMORY_PRESSURE_NONE;
    heap->stats.allocation_debt = 0;
    spin_unlock(&heap->lock);
    return IMMIX_OK;
}

void immix_collection_abort(ImmixHeap *heap) {
    if (!immix_heap_is_valid(heap)) return;
    spin_lock(&heap->lock);
    heap->collecting = 0;
    spin_unlock(&heap->lock);
}

/* ------------------------------------------------------------------------- */
/* Enumeration / queries / triggers / stats                                  */
/* ------------------------------------------------------------------------- */

ImmixResult immix_visit_objects(ImmixHeap *heap,
                                ImmixObjectVisitor visitor,
                                void *context) {
    if (!immix_heap_is_valid(heap) || !visitor) {
        return IMMIX_ERR_INVALID_ARGUMENT;
    }
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) {
        return IMMIX_ERR_UNSUPPORTED;
    }
    return immix_block_visit_objects(heap, visitor, context);
}

s32 immix_contains(const ImmixHeap *heap, const void *address) {
    if (!immix_heap_is_valid(heap) || !address) return 0;
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) return 0;
    return immix_block_contains(heap, address);
}

s32 immix_is_large_object(const ImmixHeap *heap, const void *object) {
    if (!immix_heap_is_valid(heap) || !object) return 0;
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) return 0;
    return immix_block_is_large_object(heap, object);
}

s32 immix_is_object_start(const ImmixHeap *heap, const void *address) {
    ImmixHeap *mutable_heap;
    ImmixBlock *block;

    if (!immix_heap_is_valid(heap) || !address) return 0;
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) return 0;
    mutable_heap = (ImmixHeap *) heap;
    block = immix_block_of_address(mutable_heap, address);
    if (!block) return 0;
    if (block->state == IMMIX_BLOCK_DECOMMITTED) return 0;
    return immix_start_bit_test(heap, block, (const u8 *) address);
}

s32 immix_should_collect(const ImmixHeap *heap, size_t incoming_bytes) {
    ImmixHeap *mutable_heap;
    ImmixStats stats;
    ImmixMemoryPressure pressure;
    u64 projected;
    u64 projected_debt;
    u64 debt_target;
    u64 heap_limit;
    u64 trigger;

    if (!immix_heap_is_valid(heap)) return 0;
    mutable_heap = (ImmixHeap *) heap;
    spin_lock(&mutable_heap->lock);
    stats = heap->stats;
    pressure = heap->memory_pressure;
    spin_unlock(&mutable_heap->lock);
    if (pressure != IMMIX_MEMORY_PRESSURE_NONE) return 1;

    /* Debt target: floor plus a growth factor over the live set. */
    debt_target = (u64) heap->config.allocation_debt_limit;
    if (heap->config.debt_growth_percent > 0) {
        u64 growth = (stats.live_bytes / 100u) * heap->config.debt_growth_percent;
        if (growth > debt_target) debt_target = growth;
    }
    if (debt_target > 0) {
        projected_debt = stats.allocation_debt;
        if ((u64) incoming_bytes > UINT64_MAX - projected_debt) return 1;
        if (projected_debt + (u64) incoming_bytes >= debt_target) return 1;
    }

    projected = stats.live_bytes;
    if ((u64) incoming_bytes > UINT64_MAX - projected) return 1;
    projected += (u64) incoming_bytes;

    heap_limit = (u64) heap->config.heap_limit;
    if (heap_limit != 0) {
        if (projected >= heap_limit) return 1;
        trigger = (heap_limit / 100u) * heap->config.gc_trigger_percent;
        trigger += ((heap_limit % 100u) * heap->config.gc_trigger_percent) / 100u;
        if (projected >= trigger) return 1;
    }
    return 0;
}

void immix_notify_memory_pressure(ImmixHeap *heap,
                                  ImmixMemoryPressure pressure) {
    if (!immix_heap_is_valid(heap)) return;
    if (pressure < IMMIX_MEMORY_PRESSURE_NONE ||
        pressure > IMMIX_MEMORY_PRESSURE_CRITICAL) return;

    spin_lock(&heap->lock);
    if (pressure > heap->memory_pressure ||
        pressure == IMMIX_MEMORY_PRESSURE_NONE) {
        heap->memory_pressure = pressure;
    }
    spin_unlock(&heap->lock);
}

ImmixResult immix_trim(ImmixHeap *heap, size_t target_committed_bytes) {
    if (!immix_heap_is_valid(heap)) return IMMIX_ERR_INVALID_ARGUMENT;
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) {
        return IMMIX_ERR_UNSUPPORTED;
    }
    return immix_block_trim(heap, target_committed_bytes);
}

void immix_get_stats(const ImmixHeap *heap, ImmixStats *out_stats) {
    ImmixHeap *mutable_heap;
    ImmixChunk *chunk;
    ImmixMutator *mutator;
    u32 decommitted = 0;
    u32 chunk_count = 0;
    u32 active = 0;

    if (!out_stats) return;
    memset(out_stats, 0, sizeof(ImmixStats));
    if (!immix_heap_is_valid(heap)) return;

    mutable_heap = (ImmixHeap *) heap;
    spin_lock(&mutable_heap->lock);
    for (mutator = heap->mutators; mutator; mutator = mutator->next) {
        if (mutator->block) active++;
    }
    for (chunk = heap->chunks; chunk; chunk = chunk->next) {
        u32 i;
        chunk_count++;
        for (i = 0; i < chunk->block_count; i++) {
            if (chunk->blocks[i].state == IMMIX_BLOCK_DECOMMITTED) decommitted++;
        }
    }
    *out_stats = heap->stats;
    out_stats->chunk_count = chunk_count;
    out_stats->free_block_count = heap->free_blocks.count;
    out_stats->recyclable_block_count = heap->recyclable_blocks.count;
    out_stats->active_block_count = active;
    out_stats->full_block_count = heap->full_blocks.count;
    out_stats->decommitted_block_count = decommitted;
    spin_unlock(&mutable_heap->lock);
}

ImmixResult immix_verify(const ImmixHeap *heap) {
    ImmixChunk *chunk;

    if (!immix_heap_is_valid(heap)) return IMMIX_ERR_INVALID_ARGUMENT;
    if (immix_validate_config(&heap->config) != IMMIX_OK) {
        return IMMIX_ERR_INVALID_STATE;
    }
    if (heap->config.backend == IMMIX_BACKEND_MALLOC) return IMMIX_OK;

    /* Block-level invariants: object starts are aligned, in bounds, ordered. */
    for (chunk = heap->chunks; chunk; chunk = chunk->next) {
        u32 i;
        for (i = 0; i < chunk->block_count; i++) {
            ImmixBlock *b = &chunk->blocks[i];
            const u8 *prev_end = b->start;
            u32 bit = 0;
            if (b->state == IMMIX_BLOCK_DECOMMITTED) continue;
            while (immix_next_start_bit(heap, b, bit, &bit)) {
                const u8 *addr = b->start +
                                 (size_t) bit * heap->config.object_alignment;
                MemoryBlock *mb = (MemoryBlock *) addr;
                size_t size;
                if (mb->heap_size <= 0 ||
                    immix_block_object_size(heap, b, addr,
                                            (size_t) mb->heap_size,
                                            &size) != IMMIX_OK) {
                    jvm_printf("[IMMIX] verify: invalid object header %p block=%p\n",
                               addr, b->start);
                    return IMMIX_ERR_INVALID_STATE;
                }
                if (addr < prev_end) {
                    jvm_printf("[IMMIX] verify: overlapping start %p < %p block=%p\n",
                               addr, prev_end, b->start);
                    return IMMIX_ERR_INVALID_STATE;
                }
                prev_end = addr + size;
                bit++;
            }
        }
    }
    return IMMIX_OK;
}

const c8 *immix_result_string(ImmixResult result) {
    switch (result) {
        case IMMIX_OK:
            return "ok";
        case IMMIX_ERR_INVALID_ARGUMENT:
            return "invalid argument";
        case IMMIX_ERR_OUT_OF_MEMORY:
            return "out of memory";
        case IMMIX_ERR_UNSUPPORTED:
            return "unsupported";
        case IMMIX_ERR_INVALID_STATE:
            return "invalid state";
        case IMMIX_ERR_NOT_IMPLEMENTED:
            return "not implemented";
        case IMMIX_ERR_VISITOR_STOPPED:
            return "visitor stopped";
        default:
            return "unknown immix result";
    }
}

void immix_write_barrier_post(ImmixHeap *heap,
                              MemoryBlock *owner,
                              __refer *slot,
                              __refer value) {
    /* Full-heap collector; Sticky Immix will dirty a card here. */
    (void) heap;
    (void) owner;
    (void) slot;
    (void) value;
}

void immix_write_barrier_range(ImmixHeap *heap,
                               MemoryBlock *owner,
                               __refer *first_slot,
                               size_t slot_count) {
    (void) heap;
    (void) owner;
    (void) first_slot;
    (void) slot_count;
}

/* ------------------------------------------------------------------------- */
/* Phase-B block backend                                                     */
/* ------------------------------------------------------------------------- */

static ImmixResult immix_block_backend_initialize(ImmixHeap *heap) {
    /*
     * Reserve only virtual address space and side metadata here. Physical
     * pages are still committed one block at a time when the GC thread first
     * needs the reserve.
     */
    ImmixResult result = IMMIX_OK;
    if (heap->config.emergency_reserve_size != 0) {
        spin_lock(&heap->lock);
        result = immix_chunk_create(heap,
                                    heap->config.emergency_reserve_size, 1);
        spin_unlock(&heap->lock);
    }
    return result;
}

static void immix_block_backend_shutdown(ImmixHeap *heap) {
    ImmixChunk *chunk = heap->chunks;
    ImmixLargeObject *los = heap->large_objects;

    while (los) {
        ImmixLargeObject *next = los->next;
        heap->platform.release(heap->platform.context, los->reservation,
                               los->reserved_size);
        jvm_free(los);
        los = next;
    }
    heap->large_objects = NULL;
    memset(heap->large_object_buckets, 0, sizeof(heap->large_object_buckets));

    while (chunk) {
        ImmixChunk *next = chunk->next;
        heap->platform.release(heap->platform.context, chunk->reservation,
                               chunk->reserved_size);
        if (chunk->metadata) jvm_free(chunk->metadata);
        if (chunk->blocks) jvm_free(chunk->blocks);
        jvm_free(chunk);
        chunk = next;
    }
    heap->chunks = NULL;
    heap->chunk_lookup_hint = NULL;
    heap->managed_capacity_bytes = 0;
    memset(&heap->free_blocks, 0, sizeof(ImmixBlockList));
    memset(&heap->recyclable_blocks, 0, sizeof(ImmixBlockList));
    memset(&heap->active_blocks, 0, sizeof(ImmixBlockList));
    memset(&heap->full_blocks, 0, sizeof(ImmixBlockList));
    memset(&heap->collecting_blocks, 0, sizeof(ImmixBlockList));
}

static ImmixResult immix_chunk_create(ImmixHeap *heap, size_t chunk_bytes,
                                      s32 emergency) {
    ImmixChunk *chunk;
    void *reservation;
    uintptr_t aligned_base;
    size_t reserve_total;
    size_t usable;
    size_t metadata_per_block;
    size_t metadata_size;
    u32 block_count;
    u32 i;

    /* Caller holds heap->lock (only the allocation slow path grows the heap). */
    if (heap->config.heap_limit != 0) {
        size_t allowed;
        if (heap->managed_capacity_bytes >= (u64) heap->config.heap_limit) {
            return IMMIX_ERR_OUT_OF_MEMORY;
        }
        allowed = (size_t) ((u64) heap->config.heap_limit -
                            heap->managed_capacity_bytes);
        allowed = allowed - (allowed % heap->config.block_size);
        if (allowed < heap->config.block_size) return IMMIX_ERR_OUT_OF_MEMORY;
        if (chunk_bytes > allowed) chunk_bytes = allowed;
    }
    chunk_bytes -= chunk_bytes % heap->config.block_size;
    if (chunk_bytes < heap->config.block_size ||
        chunk_bytes > SIZE_MAX - (heap->config.block_size - 1u) ||
        chunk_bytes / heap->config.block_size > UINT32_MAX) {
        return IMMIX_ERR_OUT_OF_MEMORY;
    }

    chunk = (ImmixChunk *) jvm_calloc((u32) sizeof(ImmixChunk));
    if (!chunk) return IMMIX_ERR_OUT_OF_MEMORY;

    /* Alignment slack is address space only and must never become an extra block. */
    reserve_total = chunk_bytes + heap->config.block_size - 1u;
    reservation = heap->platform.reserve(heap->platform.context, reserve_total,
                                         heap->config.block_size);
    if (!reservation) {
        jvm_free(chunk);
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    aligned_base = immix_align_up((uintptr_t) reservation, heap->config.block_size);
    block_count = (u32) (chunk_bytes / heap->config.block_size);
    usable = chunk_bytes;
    if (aligned_base == 0 || block_count == 0 ||
        aligned_base > (uintptr_t) reservation + reserve_total - usable) {
        heap->platform.release(heap->platform.context, reservation, reserve_total);
        jvm_free(chunk);
        return IMMIX_ERR_OUT_OF_MEMORY;
    }

    if ((size_t) block_count > UINT_MAX / sizeof(ImmixBlock)) {
        heap->platform.release(heap->platform.context, reservation, reserve_total);
        jvm_free(chunk);
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    chunk->blocks = (ImmixBlock *) jvm_calloc((u32) ((size_t) block_count *
                                                      sizeof(ImmixBlock)));
    if (!chunk->blocks) {
        heap->platform.release(heap->platform.context, reservation, reserve_total);
        jvm_free(chunk);
        return IMMIX_ERR_OUT_OF_MEMORY;
    }

    metadata_per_block = (size_t) heap->lines_per_block * 2u +
                         heap->start_bitmap_bytes;
    if ((size_t) block_count > SIZE_MAX / metadata_per_block ||
        (metadata_size = (size_t) block_count * metadata_per_block) > UINT_MAX) {
        heap->platform.release(heap->platform.context, reservation, reserve_total);
        jvm_free(chunk->blocks);
        jvm_free(chunk);
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    chunk->metadata = (u8 *) jvm_calloc((u32) metadata_size);
    if (!chunk->metadata) {
        heap->platform.release(heap->platform.context, reservation, reserve_total);
        jvm_free(chunk->blocks);
        jvm_free(chunk);
        return IMMIX_ERR_OUT_OF_MEMORY;
    }
    chunk->metadata_size = metadata_size;
    chunk->emergency = emergency ? 1 : 0;

    chunk->reservation = reservation;
    chunk->block_base = (u8 *) aligned_base;
    chunk->reserved_size = reserve_total;
    chunk->committed_size = 0;
    chunk->block_count = block_count;

    for (i = 0; i < block_count; i++) {
        ImmixBlock *b = &chunk->blocks[i];
        u32 lines = heap->lines_per_block;
        u8 *metadata = chunk->metadata + (size_t) i * metadata_per_block;
        b->chunk = chunk;
        b->index_in_chunk = i;
        b->start = chunk->block_base + (size_t) i * heap->config.block_size;
        b->line_marks = metadata;
        b->line_state = metadata + lines;
        b->object_starts = metadata + (size_t) lines * 2u;
    }

    for (i = 0; i < block_count; i++) {
        ImmixBlock *b = &chunk->blocks[i];
        /* Reserve a chunk for compact metadata/address management, but commit
         * Java pages one block at a time on first acquisition. On Windows this
         * changes the physical commit granularity from 1 MiB to 32 KiB. */
        b->state = IMMIX_BLOCK_DECOMMITTED;
        b->decommitted = 1;
        b->free_line_count = heap->lines_per_block;
        b->best_hole_line = 0;
        b->best_hole_lines = (u16) heap->lines_per_block;
        immix_block_list_push(&heap->free_blocks, b);
    }
    chunk->next = heap->chunks;
    heap->chunks = chunk;
    heap->stats.reserved_bytes += reserve_total;
    heap->stats.metadata_bytes += metadata_size;
    heap->managed_capacity_bytes += usable;
    return IMMIX_OK;
}

/* Return completely empty chunks to the OS when LOS needs hard-limit room.
 * Without this transfer, an old but empty block chunk permanently consumes
 * the heap capacity and can cause a false OOME for a later large object. */
static void immix_release_empty_chunks_for(ImmixHeap *heap, size_t incoming_bytes) {
    ImmixChunk **link = &heap->chunks;

    while (*link && heap->config.heap_limit != 0 &&
           (heap->managed_capacity_bytes > (u64) heap->config.heap_limit ||
            (u64) incoming_bytes > (u64) heap->config.heap_limit -
                                    heap->managed_capacity_bytes)) {
        ImmixChunk *chunk = *link;
        size_t usable = (size_t) chunk->block_count * heap->config.block_size;
        u32 i;
        s32 empty = 1;

        /* The GC reserve must not be converted into ordinary LOS capacity. */
        if (chunk->emergency) {
            link = &chunk->next;
            continue;
        }

        for (i = 0; i < chunk->block_count; i++) {
            ImmixBlockState state = chunk->blocks[i].state;
            if (state != IMMIX_BLOCK_FREE && state != IMMIX_BLOCK_DECOMMITTED) {
                empty = 0;
                break;
            }
        }
        if (!empty) {
            link = &chunk->next;
            continue;
        }

        for (i = 0; i < chunk->block_count; i++) {
            immix_block_list_unlink(&heap->free_blocks, &chunk->blocks[i]);
        }
        *link = chunk->next;
        if (heap->chunk_lookup_hint == chunk) heap->chunk_lookup_hint = NULL;
        heap->stats.reserved_bytes -= (u64) chunk->reserved_size;
        heap->stats.committed_bytes -= (u64) chunk->committed_size;
        heap->stats.metadata_bytes -= (u64) chunk->metadata_size;
        heap->managed_capacity_bytes -= (u64) usable;
        heap->platform.release(heap->platform.context, chunk->reservation,
                               chunk->reserved_size);
        jvm_free(chunk->metadata);
        jvm_free(chunk->blocks);
        jvm_free(chunk);
    }
}

/*
 * Finds a run of FREE lines of at least need_bytes, starting at or after
 * from_line. Hole bounds are line aligned.
 */
static s32 immix_find_hole(ImmixHeap *heap, ImmixBlock *block,
                           u32 from_line, size_t need_bytes,
                           u32 *out_first_line, u32 *out_last_line) {
    u32 lines = heap->lines_per_block;
    u32 need_lines = (u32) ((need_bytes + heap->config.line_size - 1) /
                            heap->config.line_size);
    u32 run_start = 0;
    s32 in_run = 0;
    u32 line = from_line;

    for (; line < lines; line++) {
        if (block->line_state[line] == IMMIX_LINE_FREE) {
            if (!in_run) {
                in_run = 1;
                run_start = line;
            }
            if (line - run_start + 1 >= need_lines) {
                *out_first_line = run_start;
                *out_last_line = line;
                return 1;
            }
        } else {
            in_run = 0;
        }
    }
    return 0;
}

static u32 immix_count_free_lines(ImmixHeap *heap, ImmixBlock *block) {
    u32 count = 0;
    u32 line;
    for (line = 0; line < heap->lines_per_block; line++) {
        if (block->line_state[line] == IMMIX_LINE_FREE) count++;
    }
    return count;
}

/* Caches the largest FREE line run so acquisition can filter blocks in O(1). */
static void immix_block_update_best_hole(ImmixHeap *heap, ImmixBlock *block) {
    u32 lines = heap->lines_per_block;
    u32 run_start = 0;
    s32 in_run = 0;
    u32 line;

    block->best_hole_lines = 0;
    block->best_hole_line = 0;
    for (line = 0; line < lines; line++) {
        if (block->line_state[line] == IMMIX_LINE_FREE) {
            if (!in_run) {
                in_run = 1;
                run_start = line;
            }
            if (line - run_start + 1 > block->best_hole_lines) {
                block->best_hole_lines = (u16) (line - run_start + 1);
                block->best_hole_line = (u16) run_start;
            }
        } else {
            in_run = 0;
        }
    }
}

/* Marks the allocated prefix of the mutator's hole LIVE; the tail stays FREE. */
static void immix_block_publish_prefix(ImmixHeap *heap, ImmixMutator *mutator) {
    ImmixBlock *block = mutator->block;

    if (!block) return;
    if (mutator->cursor > mutator->hole_start) {
        u32 first = immix_line_of(heap, block->start, mutator->hole_start);
        u32 last = immix_line_of(heap, block->start, mutator->cursor - 1);
        u32 line;
        for (line = first; line <= last; line++) {
            block->line_state[line] = IMMIX_LINE_LIVE;
        }
    }
}

static void immix_block_classify_released(ImmixHeap *heap, ImmixBlock *block) {
    block->free_line_count = immix_count_free_lines(heap, block);
    immix_block_update_best_hole(heap, block);
    if (block->free_line_count == 0) {
        block->state = IMMIX_BLOCK_FULL;
        immix_block_list_push(&heap->full_blocks, block);
    } else if (block->free_line_count == heap->lines_per_block) {
        block->state = IMMIX_BLOCK_FREE;
        immix_block_list_push(&heap->free_blocks, block);
    } else {
        block->state = IMMIX_BLOCK_RECYCLABLE;
        immix_block_list_push(&heap->recyclable_blocks, block);
    }
}

/* Gives the current block back to the heap lists. Caller holds heap->lock. */
static void immix_block_release_current(ImmixHeap *heap, ImmixMutator *mutator) {
    ImmixBlock *block = mutator->block;

    if (!block) return;
    immix_block_publish_prefix(heap, mutator);
    block->pub_cursor = NULL;
    block->pub_limit = NULL;
    immix_block_classify_released(heap, block);
    mutator->block = NULL;
    mutator->cursor = NULL;
    mutator->limit = NULL;
    mutator->hole_start = NULL;
}

/* Pops a recyclable or free block with a hole of at least aligned_size. */
static s32 immix_block_try_acquire(ImmixHeap *heap, ImmixMutator *mutator,
                                   size_t aligned_size) {
    ImmixBlock *scan;
    u32 first_line = 0, last_line = 0;
    s32 gc_thread = mutator->thread_info &&
                    mutator->thread_info->type == THREAD_TYPE_GC;

    /* 1. Recyclable block whose cached largest hole can hold the request. */
    scan = heap->recyclable_blocks.head;
    while (scan) {
        ImmixBlock *next = scan->next;
        if ((gc_thread || !scan->chunk->emergency) &&
            (size_t) scan->best_hole_lines * heap->config.line_size >= aligned_size &&
            immix_find_hole(heap, scan, 0, aligned_size, &first_line, &last_line)) {
            immix_block_list_unlink(&heap->recyclable_blocks, scan);
            scan->state = IMMIX_BLOCK_ACTIVE;
            mutator->block = scan;
            mutator->hole_start = (u8 *) immix_line_address(heap, scan, first_line);
            mutator->cursor = mutator->hole_start;
            mutator->limit = (u8 *) immix_line_address(heap, scan, last_line + 1);
            return 1;
        }
        scan = next;
    }

    /* 2. Free block (whole-block hole), recommitting if it was trimmed. */
    scan = heap->free_blocks.head;
    while (scan && !gc_thread && scan->chunk->emergency) {
        scan = scan->next;
    }
    if (!scan) return 0;
    immix_block_list_unlink(&heap->free_blocks, scan);
    if (scan->decommitted) {
        if (heap->platform.commit(heap->platform.context, scan->start,
                                  heap->config.block_size) != IMMIX_OK) {
            immix_block_list_push(&heap->free_blocks, scan);
            return 0;
        }
        scan->decommitted = 0;
        scan->ever_used = 0;
        heap->stats.committed_bytes += heap->config.block_size;
        scan->chunk->committed_size += heap->config.block_size;
    }
    memset(scan->line_state, IMMIX_LINE_FREE, heap->lines_per_block);
    memset(scan->object_starts, 0, heap->start_bitmap_bytes);
    scan->state = IMMIX_BLOCK_ACTIVE;
    mutator->block = scan;
    mutator->hole_start = scan->start;
    mutator->cursor = scan->start;
    mutator->limit = scan->start + heap->config.block_size;
    return 1;
}

static void *immix_block_allocate_large(ImmixMutator *mutator,
                                        size_t aligned_size,
                                        ImmixObjectKind kind) {
    ImmixHeap *heap = mutator->heap;
    ImmixLargeObject *los;
    void *address;
    uintptr_t aligned;
    size_t reserve_total;

    if (aligned_size > SIZE_MAX - (heap->config.object_alignment - 1u)) {
        immix_stats_record_allocation_failure(heap);
        return NULL;
    }
    reserve_total = aligned_size + heap->config.object_alignment - 1u;

    /* Claim hard-limit capacity before doing an OS reservation. */
    spin_lock(&heap->lock);
    immix_release_empty_chunks_for(heap, aligned_size);
    if (heap->config.heap_limit != 0 &&
        (heap->managed_capacity_bytes > (u64) heap->config.heap_limit ||
         (u64) aligned_size > (u64) heap->config.heap_limit -
                               heap->managed_capacity_bytes)) {
        spin_unlock(&heap->lock);
        immix_stats_record_allocation_failure(heap);
        return NULL;
    }
    heap->managed_capacity_bytes += (u64) aligned_size;
    spin_unlock(&heap->lock);

    los = (ImmixLargeObject *) jvm_calloc((u32) sizeof(ImmixLargeObject));
    if (!los) {
        spin_lock(&heap->lock);
        heap->managed_capacity_bytes -= (u64) aligned_size;
        spin_unlock(&heap->lock);
        return NULL;
    }

    address = heap->platform.reserve(heap->platform.context, reserve_total,
                                     heap->config.object_alignment);
    if (!address) {
        jvm_free(los);
        spin_lock(&heap->lock);
        heap->managed_capacity_bytes -= (u64) aligned_size;
        spin_unlock(&heap->lock);
        immix_stats_record_allocation_failure(heap);
        return NULL;
    }
    aligned = immix_align_up((uintptr_t) address, heap->config.object_alignment);
    if (heap->platform.commit(heap->platform.context, (void *) aligned,
                              aligned_size) != IMMIX_OK) {
        heap->platform.release(heap->platform.context, address, reserve_total);
        jvm_free(los);
        spin_lock(&heap->lock);
        heap->managed_capacity_bytes -= (u64) aligned_size;
        spin_unlock(&heap->lock);
        immix_stats_record_allocation_failure(heap);
        return NULL;
    }
    los->address = (void *) aligned;
    los->reservation = address;
    los->size = aligned_size;
    los->reserved_size = reserve_total;
    los->marked_epoch = 0;
    los->kind = (u8) kind;
    if (heap->config.zero_on_allocate) {
        memset(los->address, 0, aligned_size);
    }

    spin_lock(&heap->lock);
    los->next = heap->large_objects;
    heap->large_objects = los;
    immix_los_hash_insert(heap, los);
    heap->stats.requested_bytes += (u64) aligned_size;
    heap->stats.live_bytes += (u64) aligned_size;
    heap->stats.allocation_debt += (u64) aligned_size;
    heap->stats.allocation_count++;
    heap->stats.live_object_count++;
    heap->stats.large_object_bytes += (u64) aligned_size;
    heap->stats.large_object_count++;
    heap->stats.committed_bytes += (u64) aligned_size;
    heap->stats.reserved_bytes += (u64) reserve_total;
    mutator->allocated_bytes += (u64) aligned_size;
    spin_unlock(&heap->lock);
    return los->address;
}

static void *immix_block_bump(ImmixMutator *mutator, ImmixBlock *block,
                              size_t aligned_size) {
    ImmixHeap *heap = mutator->heap;
    u8 *cursor = mutator->cursor;

    mutator->cursor = cursor + aligned_size;
    immix_start_bit_set(heap, block, cursor);
    if (heap->config.zero_on_allocate && block->ever_used) {
        memset(cursor, 0, aligned_size);
    }
    block->ever_used = 1;
    immix_record_fast_allocation(mutator, aligned_size);
    return cursor;
}

static ImmixResult immix_block_request_collection(ImmixMutator *mutator,
                                                  size_t requested_bytes) {
    ImmixHeap *heap = mutator->heap;
    ImmixCollectionReason reason;
    u64 live_bytes;

    if (!heap->vm_ops.request_collection) return IMMIX_ERR_OUT_OF_MEMORY;
    spin_lock(&heap->lock);
    live_bytes = heap->stats.live_bytes;
    spin_unlock(&heap->lock);
    if (heap->config.heap_limit != 0 &&
        (live_bytes >= (u64) heap->config.heap_limit ||
         (u64) requested_bytes >= (u64) heap->config.heap_limit - live_bytes)) {
        reason = IMMIX_GC_HEAP_LIMIT;
    } else {
        reason = IMMIX_GC_ALLOCATION_DEBT;
    }
    return heap->vm_ops.request_collection(heap->vm_ops.context,
                                           mutator->thread_info, reason,
                                           requested_bytes);
}

static void *immix_block_allocate(ImmixMutator *mutator,
                                  size_t size,
                                  ImmixObjectKind kind,
                                  s32 slow_path) {
    ImmixHeap *heap = mutator->heap;
    size_t aligned_size;
    s32 gc_retried = 0;

    aligned_size = immix_align_up(size, heap->config.object_alignment);
    if (aligned_size == 0) return NULL;

    if (kind == IMMIX_OBJECT_LARGE ||
        (aligned_size >= heap->config.large_object_threshold &&
         (!(mutator->thread_info &&
            mutator->thread_info->type == THREAD_TYPE_GC) ||
          aligned_size >= heap->config.block_size))) {
        void *large = immix_block_allocate_large(mutator, aligned_size, kind);
        if (!large && slow_path &&
            immix_block_request_collection(mutator, aligned_size) == IMMIX_OK) {
            large = immix_block_allocate_large(mutator, aligned_size, kind);
        }
        return large;
    }

    /* Fast path: bump inside the current hole. */
    if (mutator->cursor &&
        mutator->cursor + aligned_size <= mutator->limit) {
        return immix_block_bump(mutator, mutator->block, aligned_size);
    }
    if (!slow_path) return NULL;

    immix_publish_mutator_stats(heap, mutator);

    for (;;) {
        u32 first_line = 0, last_line = 0;

        spin_lock(&heap->lock);

        /* Continue in another hole of the current block. The old hole's
         * allocated prefix must become LIVE or a later re-acquisition of this
         * block would hand the same lines out again. */
        if (mutator->block) {
            u32 from_line = immix_line_of(heap, mutator->block->start, mutator->limit);
            if (immix_find_hole(heap, mutator->block, from_line, aligned_size,
                                &first_line, &last_line)) {
                immix_block_publish_prefix(heap, mutator);
                mutator->hole_start = (u8 *) immix_line_address(heap, mutator->block,
                                                                first_line);
                mutator->cursor = mutator->hole_start;
                mutator->limit = (u8 *) immix_line_address(heap, mutator->block,
                                                           last_line + 1);
                spin_unlock(&heap->lock);
                return immix_block_bump(mutator, mutator->block, aligned_size);
            }
            immix_block_release_current(heap, mutator);
        }

        /* Acquire a recyclable or free block. */
        if (immix_block_try_acquire(heap, mutator, aligned_size)) {
            spin_unlock(&heap->lock);
            return immix_block_bump(mutator, mutator->block, aligned_size);
        }

        /* Grow by one bounded chunk. */
        if (immix_chunk_create(heap, heap->config.growth_chunk_size, 0) == IMMIX_OK &&
            immix_block_try_acquire(heap, mutator, aligned_size)) {
            spin_unlock(&heap->lock);
            return immix_block_bump(mutator, mutator->block, aligned_size);
        }

        spin_unlock(&heap->lock);

        /* Ask the VM for a collection, then retry exactly once. */
        if (!gc_retried) {
            gc_retried = 1;
            if (immix_block_request_collection(mutator, aligned_size) == IMMIX_OK) {
                continue;
            }
        }
        jvm_printf("[IMMIX] alloc failed size=%llu live=%llu committed=%llu limit=%llu\n",
                   (unsigned long long) aligned_size,
                   (unsigned long long) heap->stats.live_bytes,
                   (unsigned long long) heap->stats.committed_bytes,
                   (unsigned long long) heap->config.heap_limit);
        immix_stats_record_allocation_failure(heap);
        return NULL;
    }
}

static ImmixResult immix_block_mutator_initialize(ImmixMutator *mutator) {
    /* No block is acquired until the first allocation needs one. */
    mutator->block = NULL;
    mutator->cursor = NULL;
    mutator->limit = NULL;
    mutator->hole_start = NULL;
    return IMMIX_OK;
}

/*
 * STW flush: publish the cursor so the sweep can protect the owner's unused
 * hole tail from being handed to another mutator. The owner keeps the block.
 */
static void immix_block_mutator_flush(ImmixMutator *mutator) {
    if (mutator->block) {
        mutator->block->pub_cursor = mutator->cursor;
        mutator->block->pub_limit = mutator->limit;
    }
}

/* Detach-time release: the whole block, including the unused tail, is returned. */
static void immix_block_mutator_release(ImmixMutator *mutator) {
    if (!mutator->block) return;
    spin_lock(&mutator->heap->lock);
    immix_block_release_current(mutator->heap, mutator);
    spin_unlock(&mutator->heap->lock);
}

static ImmixResult immix_block_object_size(const ImmixHeap *heap,
                                           const ImmixBlock *block,
                                           const u8 *address,
                                           size_t raw_size,
                                           size_t *out_aligned_size) {
    size_t offset;
    size_t aligned_size;

    if (!block || !address || !out_aligned_size ||
        address < block->start ||
        address >= block->start + heap->config.block_size ||
        ((uintptr_t) address & (heap->config.object_alignment - 1u)) != 0 ||
        raw_size < sizeof(MemoryBlock)) {
        return IMMIX_ERR_INVALID_STATE;
    }
    offset = (size_t) (address - block->start);
    aligned_size = immix_align_up(raw_size, heap->config.object_alignment);
    if (aligned_size == 0 || aligned_size > heap->config.block_size - offset) {
        return IMMIX_ERR_INVALID_STATE;
    }
    *out_aligned_size = aligned_size;
    return IMMIX_OK;
}

static ImmixResult immix_block_mark_range(ImmixHeap *heap,
                                          ImmixBlock *block,
                                          const u8 *address,
                                          size_t aligned_size) {
    u32 first_line;
    u32 last_line;
    u32 line;

    if (immix_block_object_size(heap, block, address, aligned_size,
                                &aligned_size) != IMMIX_OK) {
        return IMMIX_ERR_INVALID_STATE;
    }
    first_line = immix_line_of(heap, block->start, address);
    last_line = immix_line_of(heap, block->start,
                              address + aligned_size - 1u);
    if (last_line >= heap->lines_per_block) return IMMIX_ERR_INVALID_STATE;
    for (line = first_line; line <= last_line; line++) {
        block->line_marks[line] = IMMIX_LINE_LIVE;
    }
    return IMMIX_OK;
}

static ImmixResult immix_block_mark_object(ImmixHeap *heap,
                                           MemoryBlock *object,
                                           size_t object_size) {
    ImmixBlock *block;
    ImmixLargeObject *los;
    size_t aligned_size;

    /* Most objects are in blocks; avoid an LOS lookup on every mark. */
    block = immix_block_of_address(heap, object);
    if (!block) {
        los = immix_los_record_of(heap, object);
        if (los) {
            los->marked_epoch = heap->mark_epoch;
        }
        return IMMIX_OK; /* not an Immix object (e.g. a JClass) */
    }

    if (heap->config.enable_validation &&
        !immix_start_bit_test(heap, block, (const u8 *) object)) {
        return IMMIX_ERR_INVALID_STATE;
    }
    if (immix_block_object_size(heap, block, (const u8 *) object,
                                object_size, &aligned_size) != IMMIX_OK) {
        return IMMIX_ERR_INVALID_STATE;
    }
    return immix_block_mark_range(heap, block, (const u8 *) object,
                                  aligned_size);
}

static ImmixResult immix_block_sweep(ImmixHeap *heap, const ImmixSweepOps *ops) {
    ImmixChunk *chunk;
    ImmixLargeObject *los, *los_prev;
    u64 live_bytes = 0;
    u64 live_objects = 0;

    spin_lock(&heap->lock);

    /* Blocks: recompute line states, reclaim dead objects, classify. */
    memset(&heap->free_blocks, 0, sizeof(ImmixBlockList));
    memset(&heap->recyclable_blocks, 0, sizeof(ImmixBlockList));
    memset(&heap->full_blocks, 0, sizeof(ImmixBlockList));

    for (chunk = heap->chunks; chunk; chunk = chunk->next) {
        u32 i;
        for (i = 0; i < chunk->block_count; i++) {
            ImmixBlock *b = &chunk->blocks[i];
            u32 line;
            u32 bit;
            u32 block_live_bytes = 0;
            u32 block_live_objects = 0;

            /* Keep trimmed blocks acquirable after the block lists are rebuilt. */
            if (b->state == IMMIX_BLOCK_DECOMMITTED) {
                immix_block_list_push(&heap->free_blocks, b);
                continue;
            }

            /* Enumerate object starts and reclaim dead objects. */
            bit = 0;
            while (immix_next_start_bit(heap, b, bit, &bit)) {
                u8 *addr = b->start + (size_t) bit * heap->config.object_alignment;
                MemoryBlock *mb = (MemoryBlock *) addr;
                size_t size;
                if (mb->heap_size <= 0 ||
                    immix_block_object_size(heap, b, addr,
                                            (size_t) mb->heap_size,
                                            &size) != IMMIX_OK) {
                    spin_unlock(&heap->lock);
                    return IMMIX_ERR_INVALID_STATE;
                }
                if (ops->is_live(ops->context, mb)) {
                    if (immix_block_mark_range(heap, b, addr, size) != IMMIX_OK) {
                        spin_unlock(&heap->lock);
                        return IMMIX_ERR_INVALID_STATE;
                    }
                    block_live_bytes += (u32) size;
                    block_live_objects++;
                } else {
                    if (ops->before_reclaim) {
                        ops->before_reclaim(ops->context, mb);
                    }
                    immix_start_bit_clear(heap, b, addr);
                    heap->stats.reclaimed_bytes += (u64) size;
                    heap->stats.reclaimed_object_count++;
                }
                bit++;
            }

            /* Recompute line_state after the live predicate has been checked. */
            for (line = 0; line < heap->lines_per_block; line++) {
                b->line_state[line] = b->line_marks[line]
                                      ? IMMIX_LINE_LIVE : IMMIX_LINE_FREE;
            }

            /* The parked owner's unused hole tail stays reserved. */
            if (b->state == IMMIX_BLOCK_ACTIVE && b->pub_cursor && b->pub_limit &&
                b->pub_limit > b->pub_cursor) {
                u32 tail_first = immix_line_of(heap, b->start, b->pub_cursor);
                u32 tail_last = immix_line_of(heap, b->start, b->pub_limit - 1);
                for (line = tail_first; line <= tail_last; line++) {
                    b->line_state[line] = IMMIX_LINE_LIVE;
                }
            }

            b->live_bytes = block_live_bytes;
            b->live_object_count = block_live_objects;
            live_bytes += block_live_bytes;
            live_objects += block_live_objects;
            b->free_line_count = immix_count_free_lines(heap, b);

            if (b->state == IMMIX_BLOCK_ACTIVE) continue; /* owner keeps it */
            immix_block_update_best_hole(heap, b);

            if (b->free_line_count == 0) {
                b->state = IMMIX_BLOCK_FULL;
                immix_block_list_push(&heap->full_blocks, b);
            } else if (b->free_line_count == heap->lines_per_block) {
                b->state = IMMIX_BLOCK_FREE;
                immix_block_list_push(&heap->free_blocks, b);
            } else {
                b->state = IMMIX_BLOCK_RECYCLABLE;
                immix_block_list_push(&heap->recyclable_blocks, b);
            }
        }
    }

    /* Large objects: release dead reservations, keep live ones. */
    los_prev = NULL;
    los = heap->large_objects;
    while (los) {
        ImmixLargeObject *next = los->next;
        if (los->marked_epoch == heap->mark_epoch) {
            live_bytes += los->size;
            live_objects++;
            los_prev = los;
        } else {
            if (ops->before_reclaim) {
                ops->before_reclaim(ops->context, (MemoryBlock *) los->address);
            }
            if (los_prev) los_prev->next = next;
            else heap->large_objects = next;
            immix_los_hash_remove(heap, los);
            heap->platform.release(heap->platform.context, los->reservation,
                                   los->reserved_size);
            heap->stats.committed_bytes -= (u64) los->size;
            heap->stats.reserved_bytes -= (u64) los->reserved_size;
            heap->stats.large_object_count--;
            heap->stats.large_object_bytes -= (u64) los->size;
            heap->stats.reclaimed_bytes += (u64) los->size;
            heap->stats.reclaimed_object_count++;
            heap->managed_capacity_bytes -= (u64) los->size;
            jvm_free(los);
        }
        los = next;
    }

    heap->stats.live_bytes = live_bytes;
    heap->stats.requested_bytes = live_bytes;
    heap->stats.live_object_count = live_objects;

    /* A surviving owner should only conservatively publish allocations made
     * after this collection, not the already-swept prefix from older cycles. */
    {
        ImmixMutator *mutator;
        for (mutator = heap->mutators; mutator; mutator = mutator->next) {
            if (mutator->block && mutator->cursor) {
                mutator->hole_start = mutator->cursor;
            }
        }
    }
    spin_unlock(&heap->lock);
    return IMMIX_OK;
}

static ImmixResult immix_block_visit_objects(ImmixHeap *heap,
                                             ImmixObjectVisitor visitor,
                                             void *context) {
    ImmixChunk *chunk;
    ImmixLargeObject *los;

    for (chunk = heap->chunks; chunk; chunk = chunk->next) {
        u32 i;
        for (i = 0; i < chunk->block_count; i++) {
            ImmixBlock *b = &chunk->blocks[i];
            u32 bit = 0;
            if (b->state == IMMIX_BLOCK_DECOMMITTED) continue;
            while (immix_next_start_bit(heap, b, bit, &bit)) {
                u8 *addr = b->start + (size_t) bit * heap->config.object_alignment;
                MemoryBlock *mb = (MemoryBlock *) addr;
                ImmixObjectKind kind = mb->type == MEM_TYPE_ARR
                                       ? IMMIX_OBJECT_ARRAY
                                       : IMMIX_OBJECT_INSTANCE;
                size_t size;
                if (mb->heap_size <= 0 ||
                    immix_block_object_size(heap, b, addr,
                                            (size_t) mb->heap_size,
                                            &size) != IMMIX_OK) {
                    return IMMIX_ERR_INVALID_STATE;
                }
                if (visitor(context, mb, size, kind) != 0) {
                    return IMMIX_ERR_VISITOR_STOPPED;
                }
                bit++;
            }
        }
    }
    for (los = heap->large_objects; los; los = los->next) {
        if (visitor(context, (MemoryBlock *) los->address, los->size,
                    (ImmixObjectKind) los->kind) != 0) {
            return IMMIX_ERR_VISITOR_STOPPED;
        }
    }
    return IMMIX_OK;
}

static s32 immix_block_contains(const ImmixHeap *heap, const void *address) {
    return immix_block_of_address((ImmixHeap *) heap, address) != NULL;
}

static s32 immix_block_is_large_object(const ImmixHeap *heap, const void *object) {
    return immix_los_record_of((ImmixHeap *) heap, object) != NULL;
}

static ImmixResult immix_block_trim(ImmixHeap *heap, size_t target_committed_bytes) {
    ImmixResult result = IMMIX_OK;
    ImmixBlock *scan;

    if (!heap->config.enable_decommit) return IMMIX_ERR_UNSUPPORTED;

    spin_lock(&heap->lock);
    /*
     * Decommitted blocks stay in the free list; trim only flips their flag and
     * returns the pages. Acquisition recommits on demand.
     */
    for (scan = heap->free_blocks.head;
         scan && heap->stats.committed_bytes > target_committed_bytes;
         scan = scan->next) {
        if (scan->decommitted) continue;
        if (heap->platform.decommit(heap->platform.context, scan->start,
                                    heap->config.block_size) != IMMIX_OK) {
            result = IMMIX_ERR_OUT_OF_MEMORY;
            break;
        }
        scan->decommitted = 1;
        scan->state = IMMIX_BLOCK_DECOMMITTED;
        heap->stats.committed_bytes -= heap->config.block_size;
        scan->chunk->committed_size -= heap->config.block_size;
    }
    spin_unlock(&heap->lock);
    return result;
}
