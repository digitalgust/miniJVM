#ifndef MINI_JVM_HEAPDUMP_HPROF_H
#define MINI_JVM_HEAPDUMP_HPROF_H

#include "garbage.h"

#ifdef __cplusplus
extern "C" {
#endif

int hprof_dump_heap(Runtime *runtime, const char *path, int flags);

int hprof_write_heap(GcCollector *collector, const char *path);

int hprof_dump_heap_marked(GcCollector *collector, const char *path, int flags);

#ifdef __cplusplus
}
#endif

#endif
