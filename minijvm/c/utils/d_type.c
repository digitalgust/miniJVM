//
// Created by Gust on 2018/6/22.
//
#include "d_type.h"

#include <stdarg.h>


#if __JVM_OS_ANDROID__

#include <android/log.h>

#define LOG_TAG "MINIJVM"
#endif

FILE *logfile = NULL;
static s64 last_flush = 0;

extern s64 currentTimeMillis();


void open_log() {
#if _JVM_DEBUG_LOG_TO_FILE
    if (!logfile) {
        logfile = fopen("./jvmlog.txt", "wb+");
    }
#endif
}

void close_log() {
#if _JVM_DEBUG_LOG_TO_FILE
    if (logfile) {
        fclose(logfile);
        logfile = NULL;
        last_flush = 0;
    }
#endif
}

s32 jvm_init_mem_alloc() {
    open_log();
#if __JVM_PRI_ALLOC__
    pri_alloc_init();
#endif
    return 0;
}

s32 jvm_destroy_mem_alloc() {
#if __JVM_PRI_ALLOC__
    pri_alloc_destroy();
#endif
    close_log();
    return 0;
}

s32 jvm_printf(const c8 *format, ...) {
    va_list vp;
    va_start(vp, format);
    s32 result = 0;
#if _JVM_DEBUG_LOG_TO_FILE
    if (logfile) {
        result = vfprintf(logfile, format, vp);
        fflush(logfile);
        if (currentTimeMillis() - last_flush > 1000) {
            fflush(logfile);
            last_flush = currentTimeMillis();
        }
    }
#else
#ifdef __JVM_OS_ANDROID__
    static c8 buf[1024];
    static u32 buf_pos = 0, buf_writable_len = sizeof(buf) - 1;
    s32 w = vsnprintf(buf + buf_pos, sizeof(buf) - buf_pos - 1, format, vp); //maybe some bytes lost
    buf_pos += (u32) w;
    buf[buf_pos] = 0;
    if ((buf_pos > 0 && memchr(buf, '\n', buf_pos) != NULL) // if '\n' in buf, print buf and clear buf
        || buf_pos == buf_writable_len) {
        // or buf is full, print buf and clear buf
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s", buf);
        buf_pos = 0;
    }
    result = strlen(buf);
#else
    result = vfprintf(stderr, format, vp);
    fflush(stderr);
#endif
#endif
    va_end(vp);
    return result;
}


//===============================================================================
#if __JVM_PRI_ALLOC__


jvm_allocator_t g_jvm_allocator = {0};

static s64 pri_alloc_atomic_load64(volatile s64 *value) {
    return ATOMIC_ADD64(value, 0);
}

static void pri_alloc_atomic_store64(volatile s64 *value, s64 desired) {
    s64 current;
    do {
        current = pri_alloc_atomic_load64(value);
    } while (current != desired && !ATOMIC_CAS64(value, current, desired));
}

static s32 pri_alloc_atomic_load32(volatile s32 *value) {
    return ATOMIC_ADD(value, 0);
}

static void pri_alloc_atomic_store32(volatile s32 *value, s32 desired) {
    s32 current;
    do {
        current = pri_alloc_atomic_load32(value);
    } while (current != desired && !ATOMIC_CAS(value, current, desired));
}

static void pri_alloc_update_peak(s64 current) {
    s64 peak = pri_alloc_atomic_load64(&g_jvm_allocator.peak_allocated);
    while (current > peak) {
        if (ATOMIC_CAS64(&g_jvm_allocator.peak_allocated, peak, current)) return;
        peak = pri_alloc_atomic_load64(&g_jvm_allocator.peak_allocated);
    }
}

static void pri_alloc_latch_gc_if_needed(s64 current) {
    s64 limit = pri_alloc_atomic_load64(&g_jvm_allocator.pool_size);
    s64 trigger;
    if (limit <= 0) return;
    trigger = limit - limit / 5; /* 80%, avoids current * 100 overflow */
    if (current >= trigger) {
        pri_alloc_atomic_store32(&g_jvm_allocator.need_gc, 1);
    }
}

u64 pri_alloc_get_live_bytes(void) {
    s64 value = pri_alloc_atomic_load64(&g_jvm_allocator.allocated);
    return value > 0 ? (u64) value : 0;
}

u64 pri_alloc_get_peak_bytes(void) {
    s64 value = pri_alloc_atomic_load64(&g_jvm_allocator.peak_allocated);
    return value > 0 ? (u64) value : 0;
}

u64 pri_alloc_get_limit(void) {
    s64 value = pri_alloc_atomic_load64(&g_jvm_allocator.pool_size);
    return value > 0 ? (u64) value : 0;
}

u64 pri_alloc_get_max_ceiling(void) {
    s64 value = pri_alloc_atomic_load64(&g_jvm_allocator.max_pool_size);
    return value > 0 ? (u64) value : 0;
}

s32 pri_alloc_should_gc(void) {
    return pri_alloc_atomic_load32(&g_jvm_allocator.need_gc) != 0;
}

s32 pri_alloc_would_exceed(size_t incoming_bytes) {
    u64 live = pri_alloc_get_live_bytes();
    u64 limit = pri_alloc_get_limit();
    if (limit == 0) return 0;
    return live >= limit || (u64) incoming_bytes >= limit - live;
}

void pri_alloc_recalculate_gc_request(void) {
    u64 live = pri_alloc_get_live_bytes();
    u64 limit = pri_alloc_get_limit();
    u64 trigger = limit ? limit - limit / 5u : UINT64_MAX;
    pri_alloc_atomic_store32(&g_jvm_allocator.need_gc,
                             limit != 0 && live >= trigger ? 1 : 0);
}

void pri_alloc_clear_gc_request(void) {
    pri_alloc_atomic_store32(&g_jvm_allocator.need_gc, 0);
}

void pri_alloc_account_allocation(size_t size) {
    s64 current;
    if (size == 0 || size > (size_t) INT64_MAX) return;
    current = ATOMIC_ADD64(&g_jvm_allocator.allocated, (s64) size);
    if (current < 0) {
        ATOMIC_ADD64(&g_jvm_allocator.accounting_errors, 1);
        pri_alloc_atomic_store64(&g_jvm_allocator.allocated, INT64_MAX);
        current = INT64_MAX;
    }
    pri_alloc_update_peak(current);
    pri_alloc_latch_gc_if_needed(current);
}

void pri_alloc_account_free(size_t size) {
    s64 old_value;
    s64 new_value;
    if (size == 0 || size > (size_t) INT64_MAX) return;
    for (;;) {
        old_value = pri_alloc_atomic_load64(&g_jvm_allocator.allocated);
        if (old_value < 0 || (u64) old_value < (u64) size) {
            new_value = 0;
        } else {
            new_value = old_value - (s64) size;
        }
        if (ATOMIC_CAS64(&g_jvm_allocator.allocated, old_value, new_value)) break;
    }
    if (old_value < 0 || (u64) old_value < (u64) size) {
        ATOMIC_ADD64(&g_jvm_allocator.accounting_errors, 1);
    }
    /* need_gc stays latched until a completed GC clears it. */
}

void pri_alloc_account_resize(size_t old_size, size_t new_size) {
    if (new_size >= old_size) {
        pri_alloc_account_allocation(new_size - old_size);
    } else {
        pri_alloc_account_free(old_size - new_size);
    }
}


static void mi_output_to_jvm(const char *msg, void *arg) {
    (void) arg;
    if (msg) jvm_printf("%s", msg);
}

void pri_alloc_print_debug_info() {
    size_t elapsed_msecs = 0, user_msecs = 0, system_msecs = 0;
    size_t current_rss = 0, peak_rss = 0;
    size_t current_commit = 0, peak_commit = 0;
    size_t page_faults = 0;

    mi_process_info(&elapsed_msecs, &user_msecs, &system_msecs,
                    &current_rss, &peak_rss,
                    &current_commit, &peak_commit, &page_faults);

    jvm_printf("mimalloc version:%d\n", mi_version());
    jvm_printf("mimalloc process: rss=%zu peak_rss=%zu commit=%zu peak_commit=%zu page_faults=%zu\n",
               current_rss, peak_rss, current_commit, peak_commit, page_faults);
    jvm_printf("jvm tracked: allocated=%llu peak=%llu soft_limit=%llu max_limit=%llu need_gc=%d errors=%llu\n",
               (unsigned long long) pri_alloc_get_live_bytes(),
               (unsigned long long) pri_alloc_get_peak_bytes(),
               (unsigned long long) pri_alloc_get_limit(),
               (unsigned long long) pri_alloc_get_max_ceiling(),
               pri_alloc_should_gc(),
               (unsigned long long) pri_alloc_atomic_load64(&g_jvm_allocator.accounting_errors));

    mi_stats_print(NULL);
    fflush(stderr);
    fflush(stdout);
}

s32 pri_alloc_init() {
    mi_process_init();
    memset(&g_jvm_allocator, 0, sizeof(g_jvm_allocator));
    return 0;
}

s32 pri_alloc_set_max_size(size_t size) {
    if (size > (size_t) INT64_MAX) return -1;
    pri_alloc_atomic_store64(&g_jvm_allocator.pool_size, (s64) size);
    pri_alloc_recalculate_gc_request();
    return 0;
}

s32 pri_alloc_set_max_ceiling(size_t size) {
    if (size > (size_t) INT64_MAX) return -1;
    pri_alloc_atomic_store64(&g_jvm_allocator.max_pool_size, (s64) size);
    return 0;
}

s32 pri_alloc_destroy() {
    s64 errors = pri_alloc_atomic_load64(&g_jvm_allocator.accounting_errors);
    if (errors != 0) {
        jvm_printf("[WARN] allocator accounting recovered from %lld error(s)\n", errors);
    }
    mi_process_done();
    memset(&g_jvm_allocator, 0, sizeof(g_jvm_allocator));
    return 0;
}

#endif
