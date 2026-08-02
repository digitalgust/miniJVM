//
// Created by root on 10/30/19.
//

#ifndef MINI_JVM_JIT_H
#define MINI_JVM_JIT_H
#ifdef __cplusplus
extern "C" {

#endif

#include "jvm.h"
#include "jvm_util.h"

#if __JVM_OS_IOS__ || __JVM_OS_CYGWIN__ || __JVM_ARCH_32__
#define JIT_ENABLE 0
#else
#define JIT_ENABLE 01
#endif

#define JIT_COMPILE_EXEC_COUNT 5000
#define JIT_DEBUG 0

/* JIT opt: v5=INLINE_STATIC v6=INLINE_SAFEPOINT */
#define JIT_OPT_FUSION 1
#define JIT_OPT_FIELD 1
#define JIT_OPT_FUSION_EXT 1   /* idiv/irem local fusion */
#define JIT_OPT_FUSION_CMP 1   /* iload+iload+if_icmplt loop-head fusion */
#define JIT_OPT_INLINE_SAFEPOINT 1
#define JIT_OPT_LAZY_PC 1       /* write runtime->pc only at safepoints/callouts */
#define JIT_OPT_TOS_CACHE 1     /* keep short expression windows in registers */
#define JIT_OPT_HOT_LOCALS 1    /* keep two verified int-only locals in saved regs */
#define JIT_OPT_INLINE_GETTER_SETTER 1 /* guarded invokevirtual/direct invokespecial accessor inline */
#define JIT_OPT_INLINE_STATIC 1 /* branch-free short static int expression inline */

#define SLJIT_CONFIG_AUTO 1
#define JIT_CODE_DUMP 0

enum {
    LOCAL_METHOD = 0,
    LOCAL_RUNTIME,
    LOCAL_STACK_SP,
    LOCAL_RUNTIME_PC,
    LOCAL_STACK,
    LOCAL_THREADINFO,
    LOCAL_R0, //for save_ip_sp
    LOCAL_R2, //for check_suspend
    LOCAL_INLINE_STATIC_BASE,
#if JIT_OPT_INLINE_STATIC
    LOCAL_INLINE_STATIC_END = LOCAL_INLINE_STATIC_BASE + 30,
    LOCAL_COUNT = LOCAL_INLINE_STATIC_END,
#else
    LOCAL_COUNT = LOCAL_INLINE_STATIC_BASE,
#endif
};

enum {
    JIT_GEN_UNKNOW = 0,
    JIT_GEN_COMPILING,
    JIT_GEN_ERROR,
    JIT_GEN_SUCCESS,
};


typedef void (*thread_suspend_check_func)();

struct _SwitchTable {
    struct _SwitchTable *next;
    s32 size; //table length
    struct V2PTable {
        s32 value; //for value
        s32 bc_pos; //for get label addr
        __refer jump_ptr;
    } *table;
};

void jit_init(CodeAttribute *ca);

void jit_destroy(Jit *jit);

void construct_jit(MethodInfo *method, Runtime *runtime);

s32 jit_invoke_from_jit(MethodInfo *method, Runtime *runtime);

void jit_set_exception_jump_addr(Runtime *runtime, CodeAttribute *ca, s32 index);

#ifdef __cplusplus
}
#endif
#endif //MINI_JVM_JIT_H
