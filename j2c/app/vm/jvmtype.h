//
// Created by Gust on 2020/5/8.
//

#ifndef CCJVM_JVMTYPE_H
#define CCJVM_JVMTYPE_H

#include <unistd.h>
#include <stdlib.h>

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
//define something for Windows (32-bit and 64-bit, this part is common)
#ifdef _WIN64
//define something for Windows (64-bit only)
#else
//define something for Windows (32-bit only)
#endif
#if defined(_MSC_VER)
#define __JVM_OS_VS__ 1
#endif
#if defined(__MINGW_H) || defined(__MINGW32_MAJOR_VERSION)
#define __JVM_OS_MINGW__ 1
#endif
#ifdef _CYGWIN_CONFIG_H
#define __JVM_OS_CYGWIN__ 1
#endif
#elif __APPLE__
#define __JVM_OS_MAC__ 1

#include <TargetConditionals.h>

#if TARGET_IPHONE_SIMULATOR
// iOS Simulator
#define __JVM_OS_IOS__ 1
#elif TARGET_OS_IPHONE
// iOS device
#define __JVM_OS_IOS__ 1
#elif TARGET_OS_MAC
// Other kinds of Mac OS
#else
#   error "Unknown Apple platform"
#endif
#elif __linux__
// linux
#define __JVM_OS_LINUX__ 1
#elif __unix__ // all unices not caught above
    // Unix
#elif defined(_POSIX_VERSION)
    // POSIX
#else
#   error "Unknown compiler"
#endif


typedef unsigned char u8;
typedef signed char s8;
typedef char c8;
typedef unsigned short int u16;
typedef signed short int s16;
typedef unsigned int u32;
typedef signed int s32;
typedef float f32;
typedef double f64;
typedef unsigned long long int u64;
typedef signed long long int s64;
typedef void *__refer;
typedef void *__returnaddress;


//void *jvm_calloc(u32 size);
//
//void *jvm_malloc(u32 size);
//
//void jvm_free(__refer ptr);
//
//void *jvm_realloc(__refer ptr, u32 size);



static inline void *jvm_calloc(u32 size) {
    return calloc(size, 1);
}

static inline void *jvm_malloc(u32 size) {
    return malloc(size);
}

static inline void jvm_free(__refer ptr) {
    free(ptr);
}

static inline void *jvm_realloc(__refer pPtr, u32 size) {
    return realloc(pPtr, size);

}


#endif //CCJVM_JVMTYPE_H
