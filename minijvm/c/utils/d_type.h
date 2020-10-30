//
// Created by gust on 2017/9/25.
//

#ifndef MINI_JVM_JVM_TYPE_H
#define MINI_JVM_JVM_TYPE_H

#include "sys/types.h"
#include "stdlib.h"
#include "stdint.h"
#include <stdio.h>



// x86   x64 ...
#define __JVM_LITTLE_ENDIAN__ 1
//
#define __JVM_BIG_ENDIAN__ 0



#if INTPTR_MAX == INT32_MAX
    #define __JVM_ARCH_32__ 1
#elif INTPTR_MAX == INT64_MAX
    #define __JVM_ARCH_64__ 1
#else
    #error "Environment not 32 or 64-bit."
#endif


#if defined(WIN32) || defined(_WIN32) || defined(__WIN32) || defined(WIN64) || defined(_WIN64) || defined(__WIN64) || defined(__WIN32__) || defined(__NT__)

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
   #if defined(__CYGWIN__) || defined(__CYGWIN32__)
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
#elif defined(_POSIX_VERSION)
    // POSIX
    #define __JVM_OS_CYGWIN__ 1
#elif __unix__ // all unices not caught above
    // Unix
#else
#   error "Unknown compiler"
#endif



//compile
#define __C99
//libary option : -lpthread -lws2_32


//#define __DEBUG

#ifdef __cplusplus
extern "C" {
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




//======================= memory manage =============================


static inline void *jvm_calloc(u32 size) {
    return calloc(size, 1);
}

static inline void *jvm_malloc(u32 size) {
    return malloc(size);
}

static inline void jvm_free(void *ptr) {
    free(ptr);
}

static inline void *jvm_realloc(void *pPtr, u32 size) {
    return realloc(pPtr, size);

}


#ifdef __cplusplus
}
#endif


#endif //MINI_JVM_JVM_TYPE_H
