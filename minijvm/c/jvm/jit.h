//
// Created by root on 10/30/19.
//

#ifndef MINI_JVM_JIT_H
#define MINI_JVM_JIT_H


#include "sljitLir.h"

#define JIT_ENABLE 01
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
    SwitchTable *next;
    s32 size;//table length
    struct V2PTable {
        s32 value; //for value
        s32 bc_pos;//for get label addr
        sljit_uw jump_ptr;
    } *table;
};


static inline SwitchTable *switchtable_create(Jit *jit, s32 size) {
    SwitchTable *st = jvm_calloc(sizeof(SwitchTable));
    st->size = size;
    st->next = jit->switchtable;
    jit->switchtable = st;
    st->table = jvm_calloc(sizeof(struct V2PTable) * size);
    return st;
}

static inline void jit_init(CodeAttribute *ca) {
    Jit *jit = &ca->jit;
    s32 count = ca->exception_table_length;
    if (count) {
        jit->exception_handle_jump_ptr = jvm_calloc(sizeof(__refer) * count);
    }
}

static inline void jit_destory(Jit *jit) {

    while (jit->switchtable) {
        SwitchTable *tmp = jit->switchtable->next;
        jvm_free(jit->switchtable);
        jit->switchtable = tmp;
    }

    if (jit->exception_handle_jump_ptr) {
        jvm_free(jit->exception_handle_jump_ptr);
        jit->exception_handle_jump_ptr = NULL;
    }

    if (jit->func) {
        sljit_free_code(jit->func);
    }
}

void construct_jit(MethodInfo *method, Runtime *runtime);

s32 gen_jit_bytecode_func(struct sljit_compiler *C, MethodInfo *method, Runtime *runtime);


#endif //MINI_JVM_JIT_H
