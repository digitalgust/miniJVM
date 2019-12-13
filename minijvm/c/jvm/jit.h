//
// Created by root on 10/30/19.
//

#ifndef MINI_JVM_JIT_H
#define MINI_JVM_JIT_H
#include "jvm.h"
#include "jvm_util.h"

#if __JVM_OS_IOS__ || __JVM_ARCH_32__
    #define JIT_ENABLE 0
#else
    #define JIT_ENABLE 01
#endif

#define JIT_COMPILE_EXEC_COUNT 500
#define JIT_DEBUG 0

enum {
    LOCAL_METHOD = 0,
    LOCAL_RUNTIME,
    LOCAL_STACK_SP,
    LOCAL_RUNTIME_PC,
    LOCAL_STACK,
    LOCAL_R0,//for save_ip_sp
    LOCAL_R1,//
    LOCAL_R2,//for check_suspend
    LOCAL_COUNT,
};

enum {
    JIT_GEN_UNKNOW = 0,
    JIT_GEN_COMPILING,
    JIT_GEN_ERROR,
    JIT_GEN_SUCCESS,
};


typedef void (*thread_suspend_check_func)();

struct _SwitchTable {
    struct _SwitchTable  *next;
    s32 size;//table length
    struct V2PTable {
        s32 value; //for value
        s32 bc_pos;//for get label addr
        __refer jump_ptr;
    } *table;
};

void jit_init(CodeAttribute *ca) ;

void jit_destory(Jit *jit);

void construct_jit(MethodInfo *method, Runtime *runtime);

void jit_set_exception_jump_addr(Runtime* runtime, CodeAttribute *ca, s32 index);

#endif //MINI_JVM_JIT_H
