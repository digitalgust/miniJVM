/* Benchmark-only GNU ld wrapper; production runtime sources are unchanged. */
#include <stdlib.h>
#include "garbage.h"

GcCollector *__real_garbage_collector_create(void);

GcCollector *__wrap_garbage_collector_create(void) {
    GcCollector *collector = __real_garbage_collector_create();
    const char *value = getenv("J2C_BENCH_HEAP_BYTES");
    if (value) {
        char *end;
        unsigned long long bytes = strtoull(value, &end, 10);
        if (end != value && !*end && bytes > 0 && bytes <= INT64_MAX) {
            collector->max_heap_size = (s64) bytes;
        }
    }
    return collector;
}
