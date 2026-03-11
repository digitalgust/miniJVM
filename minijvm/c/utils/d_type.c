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
    jvm_printf("jvm tracked: allocated=%zu heap_limit=%zu need_gc=%d\n",
               g_jvm_allocator.allocated,
               (g_jvm_allocator.pool_size),
               g_jvm_allocator.need_gc);

    mi_stats_print(NULL);
    fflush(stderr);
    fflush(stdout);
}

s32 pri_alloc_init() {
    mi_process_init();
    g_jvm_allocator.allocated = 0;
    g_jvm_allocator.need_gc = 0;
    return 0;
}

s32 pri_alloc_set_max_size(size_t size) {
    g_jvm_allocator.pool_size = size;
    return 0;
}

s32 pri_alloc_destroy() {
    mi_process_done();
    spin_destroy(&g_jvm_allocator.m_lock);
    memset(&g_jvm_allocator, 0, sizeof(g_jvm_allocator));
    return 0;
}

#endif
