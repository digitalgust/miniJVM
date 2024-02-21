//
// Created by gust on 10/30/19.
//


#include "jit.h"

#if JIT_ENABLE

#include "sljitLir.h"

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

//------------------------  note ----------------------------

//   This jit implementation is dependend on SLJIT (github https://github.com/zherczeg/sljit)

//-----------------------------------------------------------

#define REGISTER_SP SLJIT_S0
#define REGISTER_LOCALVAR SLJIT_S1
#define REGISTER_IP SLJIT_S2

#undef VOID
//------------------------  declare ----------------------------

static thread_suspend_check_func check_suspend;

/**
 * Generate jit code for exception check , throw , and handle
 * if src1 == src2 then throw Exception and handle it,
 *
 * @param C
 * @param src1
 * @param srcw1
 * @param src2
 * @param srcw2
 * @param throw_type     !=-1 then throw a new exception
 * @param stack_adjust   !=0 if stack need to adjust
 */
void _gen_exception_check_throw_handle(struct sljit_compiler *C, sljit_s32 cmp, sljit_s32 src1, sljit_sw srcw1, sljit_s32 src2, sljit_sw srcw2, s32 throw_type, s32 stack_adjust);

void _gen_exception_handle(struct sljit_compiler *C);

void _gen_exception_new(struct sljit_compiler *C, s32 exception_type);

SwitchTable *switchtable_create(Jit *jit, s32 size);

s32 gen_jit_bytecode_func(struct sljit_compiler *C, MethodInfo *method, Runtime *runtime);

void _gen_jump_to_suspend_check(struct sljit_compiler *C, int offset);

void _gen_save_sp_ip(struct sljit_compiler *C);
//------------------------  jit util ----------------------------

static void FAILE(s32 cond, c8 *text) {
    if (cond) {
        printf("compile error: %s\n", text);
    }
}

static void CHECK(struct sljit_compiler *compiler) {
    if (sljit_get_compiler_error(compiler) != SLJIT_ERR_COMPILED) {
        printf("Compiler error: %d\n", sljit_get_compiler_error(compiler));
        sljit_free_compiler(compiler);
    }
}

static void print_reg(s64 a, s64 b, s64 c) {
    printf("R0=%lld[%llx] , R1=%lld[%llx] , R2=%lld[%llx]\n", a, a, b, b, c, c);
}

static void print_freg(f32 a, f32 b, f32 c) {
    printf("FR0=%f , FR1=%f , FR2=%f\n", a, b, c);
}

static void print_dreg(f64 a, f64 b, f64 c) {
    printf("FR0=%lf , FR1=%lf , FR2=%lf\n", a, b, c);
}

static void print_stack(s64 a, s64 b, s64 c) {
    //printf("S0=[%llx] , S1=[%llx] , S2=[%llx]\n", a, b, c);
    Runtime *runtime = (__refer) (intptr_t) b;
    CodeAttribute *ca = runtime->method->converted_code;
    s32 offset = (s32) (c - (s64) (intptr_t) ca->code);
    s32 size = (s32) (runtime->stack->sp - runtime->stack->store);
    printf("[%d]====", size);
    s32 i, imax;
    s32 MAX = 10;
    imax = size > MAX ? MAX : size;
    for (i = 0; i < imax; i++) {
        StackEntry *e = runtime->stack->sp - 1 - i;
        s64 v1 = e->lvalue;
        s64 v2 = (s64) (intptr_t) e->rvalue;
        printf("[%llx]%llx   ", v1, v2);
    }
    if (size > imax) printf("  >>");
    printf("\n");
    printf("%d %s\n", offset, INST_NAME[ca->bytecode_for_jit[offset]]);
}

static void print_callstack(Runtime *runtime) {
    Utf8String *ustr = utf8_create();
    getRuntimeStack(runtime, ustr);
    jvm_printf("error :\n %s\n", utf8_cstr(ustr));
    utf8_destory(ustr);
}

static void _debug_gen_print_reg(struct sljit_compiler *C) {
    //save r0,r1,r2
    static sljit_sw a, b, c;
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &a, SLJIT_R0, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &b, SLJIT_R1, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &c, SLJIT_R2, 0);

    //sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP), 0);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3V(W, W, W), SLJIT_IMM, SLJIT_FUNC_ADDR(print_reg));

    //restore r0,r1,r2
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) &a);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R1, 0, SLJIT_MEM0(), (sljit_sw) &b);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R2, 0, SLJIT_MEM0(), (sljit_sw) &c);
}

static void _debug_gen_print_freg(struct sljit_compiler *C) {
    //save fr0,fr1,fr2
    static sljit_f32 a, b, c;
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_MEM0(), (sljit_sw) &a, SLJIT_FR0, 0);
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_MEM0(), (sljit_sw) &b, SLJIT_FR1, 0);
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_MEM0(), (sljit_sw) &c, SLJIT_FR2, 0);

    //sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP), 0);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3V(F32, F32, F32), SLJIT_IMM, SLJIT_FUNC_ADDR(print_freg));

    //restore fr0,fr1,fr2
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_FR0, 0, SLJIT_MEM0(), (sljit_sw) &a);
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_FR1, 0, SLJIT_MEM0(), (sljit_sw) &b);
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_FR2, 0, SLJIT_MEM0(), (sljit_sw) &c);
}

static void _debug_gen_print_dreg(struct sljit_compiler *C) {
    //save fr0,fr1,fr2
    static sljit_f64 a, b, c;
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_MEM0(), (sljit_sw) &a, SLJIT_FR0, 0);
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_MEM0(), (sljit_sw) &b, SLJIT_FR1, 0);
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_MEM0(), (sljit_sw) &c, SLJIT_FR2, 0);

    //sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP), 0);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3V(F64, F64, F64), SLJIT_IMM, SLJIT_FUNC_ADDR(print_dreg));

    //restore fr0,fr1,fr2
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_FR0, 0, SLJIT_MEM0(), (sljit_sw) &a);
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_FR1, 0, SLJIT_MEM0(), (sljit_sw) &b);
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_FR2, 0, SLJIT_MEM0(), (sljit_sw) &c);
}

static void _debug_gen_print_stack(struct sljit_compiler *C) {
    //save r0,r1,r2
    static sljit_sw a, b, c;
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &a, SLJIT_R0, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &b, SLJIT_R1, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &c, SLJIT_R2, 0);

    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, REGISTER_SP, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R2, 0, REGISTER_IP, 0);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3V(W, W, W), SLJIT_IMM, SLJIT_FUNC_ADDR(print_stack));

    //restore r0,r1,r2
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) &a);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R1, 0, SLJIT_MEM0(), (sljit_sw) &b);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R2, 0, SLJIT_MEM0(), (sljit_sw) &c);

}

static void _debug_gen_print_callstack(struct sljit_compiler *C) {
    //save r0,r1,r2
    static sljit_sw a, b, c;
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &a, SLJIT_R0, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &b, SLJIT_R1, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) &c, SLJIT_R2, 0);

    _gen_save_sp_ip(C);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS1V(P), SLJIT_IMM, SLJIT_FUNC_ADDR(print_callstack));

    //restore r0,r1,r2
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) &a);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R1, 0, SLJIT_MEM0(), (sljit_sw) &b);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R2, 0, SLJIT_MEM0(), (sljit_sw) &c);

}

static void dump_code(void *code, sljit_uw len) {

    FILE *fp = fopen("/tmp/slj_dump", "wb");
    if (!fp)
        return;
    fwrite(code, len, 1, fp);
    fclose(fp);

#if __JVM_ARCH_64__
#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    system("D:\\mingw64\\bin\\objdump.exe -b binary -m l1om -D d:/tmp/slj_dump");
#elif __JVM_OS_MAC__
    system("/usr/local/Cellar/binutils/2.34/bin/objdump -b binary -m l1om -D /tmp/slj_dump");
#else
    system("objdump -b binary -m l1om -D /tmp/slj_dump");
#endif
#elif __JVM_ARCH_32__
#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    system("D:\\msys64\\mingw32\\bin\\objdump -b binary -m i386 -D d:/tmp/slj_dump");
#else
    system("objdump -b binary -m i386 -D /tmp/slj_dump");
#endif
#endif
}


//------------------------  tool ----------------------------

void _gen_ip_modify_imm(struct sljit_compiler *C, s32 count) {
    //ip
    sljit_emit_op2(C, SLJIT_ADD, REGISTER_IP, 0, REGISTER_IP, 0, SLJIT_IMM, count);
}

void _gen_ip_modify_reg(struct sljit_compiler *C, sljit_s32 src, sljit_s32 srcw) {
    //ip
    sljit_emit_op2(C, SLJIT_ADD, REGISTER_IP, 0, REGISTER_IP, 0, src, srcw);
}

void _gen_save_sp_ip(struct sljit_compiler *C) {
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0, SLJIT_R0, 0);
    //stack->sp = S2
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R0), 0, REGISTER_SP, 0);

    //runtime->pc = ip;
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME_PC);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R0), 0, REGISTER_IP, 0);

    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0);
}

void _gen_load_sp_ip(struct sljit_compiler *C) {
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0, SLJIT_R0, 0);

    //stack->sp = S2
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP);
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_SP, 0, SLJIT_MEM1(SLJIT_R0), 0);

    //runtime->pc = ip;
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME_PC);
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_IP, 0, SLJIT_MEM1(SLJIT_R0), 0);

    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0);

}

void _gen_stack_size_modify(struct sljit_compiler *C, s32 offset) {
    //sp += offset ;
    sljit_emit_op2(C, SLJIT_ADD, REGISTER_SP, 0, REGISTER_SP, 0, SLJIT_IMM, sizeof(StackEntry) * offset);
}

//------------------------  stack peek ----------------------------


void _gen_stack_set_int(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->ivalue = v
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, ivalue), src, srcw);
}

void _gen_stack_set_long(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->ivalue = v
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, lvalue), src, srcw);
}

void _gen_stack_set_ref(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->ivalue = value
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, rvalue), src, srcw);
}

void _gen_stack_set_ra(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->ivalue = value
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, rvalue), src, srcw);
}

void _gen_stack_set_float(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->fvalue = v
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, fvalue), src, srcw);
}

void _gen_stack_set_double(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->dvalue = v
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, dvalue), src, srcw);
}

void _gen_stack_set_entry(struct sljit_compiler *C, s32 offset, sljit_s32 val_src, sljit_sw val_srcw, sljit_s32 type_src, sljit_sw type_srcw) {
    //sp[offset]->ivalue = v
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, lvalue), val_src, val_srcw);
}

void _gen_stack_peek_int(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->ivalue
    sljit_emit_op1(C, SLJIT_MOV_S32, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, ivalue));
}

void _gen_stack_peek_long(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->lvalue
    sljit_emit_op1(C, SLJIT_MOV, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, lvalue));
}

void _gen_stack_peek_ref(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst = sp[offset]->rvalue
    sljit_emit_op1(C, SLJIT_MOV, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, rvalue));
}

void _gen_stack_peek_float(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->fvalue
    sljit_emit_fop1(C, SLJIT_MOV_F32, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, fvalue));
}

void _gen_stack_peek_double(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->dvalue
    sljit_emit_fop1(C, SLJIT_MOV_F64, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, dvalue));
}


void _gen_stack_peek_entry(struct sljit_compiler *C, s32 offset, sljit_s32 val_dst, sljit_sw val_dstw, sljit_s32 r_dst, sljit_sw r_dstw) {
    //val_dst=sp[offset]->lvalue
    sljit_emit_op1(C, SLJIT_MOV, val_dst, val_dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, lvalue));
    //rval_dst=sp[offset]->rvalue
    sljit_emit_op1(C, SLJIT_MOV, r_dst, r_dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + SLJIT_OFFSETOF(StackEntry, rvalue));
}
//-------------------------  push pop  ---------------------------

void _gen_stack_push_int(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_int(stack, v);
    _gen_stack_set_int(C, 0, src, srcw);
    _gen_stack_size_modify(C, 1);
}

void _gen_stack_push_float(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_int(stack, v);
    _gen_stack_set_float(C, 0, src, srcw);
    _gen_stack_size_modify(C, 1);
}

void _gen_stack_push_long(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_long(stack, v);
    _gen_stack_set_long(C, 0, src, srcw);
    _gen_stack_size_modify(C, 2);
}

void _gen_stack_push_double(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_long(stack, v);
    _gen_stack_set_double(C, 0, src, srcw);
    _gen_stack_size_modify(C, 2);
}

void _gen_stack_push_ref(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_ref(stack, v);
    _gen_stack_set_ref(C, 0, src, srcw);
    _gen_stack_size_modify(C, 1);
}

void _gen_stack_push_entry(struct sljit_compiler *C, sljit_s32 val_src, sljit_sw val_srcw, sljit_s32 type_src, sljit_sw type_srcw) {
    //push_entry(stack, v);
    _gen_stack_set_entry(C, 0, val_src, val_srcw, type_src, type_srcw);
    _gen_stack_size_modify(C, 1);
}

void _gen_stack_push_ra(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_ra(stack, v);
    _gen_stack_set_ra(C, 0, src, srcw);
    _gen_stack_size_modify(C, 1);
}

void _gen_stack_pop_int(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_int(stack);
    _gen_stack_size_modify(C, -1);
    _gen_stack_peek_int(C, 0, dst, dstw);
}

void _gen_stack_pop_float(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_int(stack);
    _gen_stack_size_modify(C, -1);
    _gen_stack_peek_float(C, 0, dst, dstw);
}

void _gen_stack_pop_long(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_long(stack);
    _gen_stack_size_modify(C, -2);
    _gen_stack_peek_long(C, 0, dst, dstw);
}

void _gen_stack_pop_double(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_long(stack);
    _gen_stack_size_modify(C, -2);
    _gen_stack_peek_double(C, 0, dst, dstw);
}

void _gen_stack_pop_ref(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_ref(stack);
    _gen_stack_size_modify(C, -1);
    _gen_stack_peek_ref(C, 0, dst, dstw);
}

void _gen_stack_pop_entry(struct sljit_compiler *C, sljit_s32 val_dst, sljit_sw val_dstw, sljit_s32 type_dst, sljit_sw type_dstw) {
    //dst = pop_ref(stack);
    _gen_stack_size_modify(C, -1);
    _gen_stack_peek_entry(C, 0, val_dst, val_dstw, type_dst, type_dstw);
}

//------------------------------  local var  ----------------------

void _gen_local_get_int(struct sljit_compiler *C, s32 index, sljit_s32 dst, sljit_sw dstw) {
    //dst=localvar[index].ivalue
    sljit_emit_op1(C, SLJIT_MOV_S32, dst, dstw, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(StackEntry) * index + SLJIT_OFFSETOF(LocalVarItem, ivalue));
}

void _gen_local_get_ref(struct sljit_compiler *C, s32 index, sljit_s32 dst, sljit_sw dstw) {
    //dst=localvar[index].rvalue
    sljit_emit_op1(C, SLJIT_MOV_P, dst, dstw, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(StackEntry) * index + SLJIT_OFFSETOF(LocalVarItem, rvalue));
}

void _gen_local_get_long(struct sljit_compiler *C, s32 index, sljit_s32 dst, sljit_sw dstw) {
    //dst=localvar[index].lvalue
    sljit_emit_op1(C, SLJIT_MOV, dst, dstw, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, lvalue));
}

void _gen_local_set_int(struct sljit_compiler *C, s32 index, sljit_s32 src, sljit_sw srcw) {
    //localvar[index].ivalue = src
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, lvalue), src, srcw);
}

void _gen_local_set_ref(struct sljit_compiler *C, s32 index, sljit_s32 src, sljit_sw srcw) {
    //localvar[index].rvalue = src
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, rvalue), src, srcw);
}

void _gen_local_set_long(struct sljit_compiler *C, s32 index, sljit_s32 src, sljit_sw srcw) {
    //localvar[index].lvalue = src
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, lvalue), src, srcw);
}

//------------------------------  load store  ----------------------


void _gen_i_f_load(struct sljit_compiler *C, s32 index) {
    //push_int(stack, runtime->localvar[index].ivalue);

    _gen_local_get_int(C, index, SLJIT_R0, 0);
    _gen_stack_push_int(C, SLJIT_R0, 0);
}

void _gen_i_f_store(struct sljit_compiler *C, s32 index) {
    //s32 v = pop_int(stack);
    //localvar_setInt(runtime->localvar, index, v);

    _gen_stack_pop_int(C, SLJIT_R0, 0);
    _gen_local_set_int(C, index, SLJIT_R0, 0);
}

void _gen_a_load(struct sljit_compiler *C, s32 index) {
    //push_ref(stack, runtime->localvar[index].rvalue);

    _gen_local_get_ref(C, index, SLJIT_R0, 0);
    _gen_stack_push_ref(C, SLJIT_R0, 0);

}

void _gen_a_store(struct sljit_compiler *C, s32 index) {
    //__refer v = pop_int(stack);
    //localvar_setRefer(runtime->localvar, index, v);
    _gen_stack_size_modify(C, -1);
    //
    //MUST process  returnaddress  , so can't : _gen_local_set_ref(C, index, SLJIT_R0, 0);
    //localvar[index].rvalue = src
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, rvalue), SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * 0 + SLJIT_OFFSETOF(StackEntry, rvalue));
}

void _gen_l_d_load(struct sljit_compiler *C, s32 index) {
    //push_long(stack, runtime->localvar[index].lvalue);

    _gen_local_get_long(C, index, SLJIT_R0, 0);
    _gen_stack_push_long(C, SLJIT_R0, 0);
}

void _gen_l_d_store(struct sljit_compiler *C, s32 index) {
    //s64 v = pop_long(stack);
    //localvar_setLong(runtime->localvar, i, v);

    _gen_stack_pop_long(C, SLJIT_R0, 0);
    _gen_local_set_long(C, index, SLJIT_R0, 0);
}


void _gen_arr_load(struct sljit_compiler *C, s32 datatype) {
    // =====================================================================
    //    s32 index = pop_int(stack);
    //    Instance *arr = (Instance *) pop_ref(stack);
    //    ret = _jarray_check_exception(arr, index, runtime);
    //    if (!ret) {
    //        s32 s = *((s32 *) (arr->arr_body) + index);
    //        push_int(stack, s);
    //        ip++;
    //    } else {
    //        goto label_exception_handle;
    //    }
    // =====================================================================
    _gen_stack_size_modify(C, -2);
    _gen_save_sp_ip(C);

    _gen_stack_peek_ref(C, 0, SLJIT_R0, 0);
    _gen_stack_peek_int(C, 1, SLJIT_R1, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, 32, P), SLJIT_IMM, SLJIT_FUNC_ADDR(_jarray_check_exception));
    _gen_load_sp_ip(C);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, RUNTIME_STATUS_EXCEPTION, -1, 0);


    _gen_stack_peek_ref(C, 0, SLJIT_R0, 0);
    _gen_stack_peek_int(C, 1, SLJIT_R1, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(Instance, arr_body));
    switch (datatype) {
        case DATATYPE_BOOLEAN:
        case DATATYPE_BYTE: {
            sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_R0, 0, SLJIT_MEM2(SLJIT_R2, SLJIT_R1), 0);
            _gen_stack_push_int(C, SLJIT_R0, 0);
            break;
        }
        case DATATYPE_SHORT: {
            sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_R0, 0, SLJIT_MEM2(SLJIT_R2, SLJIT_R1), 1);
            _gen_stack_push_int(C, SLJIT_R0, 0);
            break;
        }
        case DATATYPE_JCHAR: {
            sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R0, 0, SLJIT_MEM2(SLJIT_R2, SLJIT_R1), 1);
            _gen_stack_push_int(C, SLJIT_R0, 0);
            break;
        }
        case DATATYPE_FLOAT:
        case DATATYPE_INT: {
            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R0, 0, SLJIT_MEM2(SLJIT_R2, SLJIT_R1), 2);
            _gen_stack_push_int(C, SLJIT_R0, 0);
            break;
        }
        case DATATYPE_LONG:
        case DATATYPE_DOUBLE: {
            sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM2(SLJIT_R2, SLJIT_R1), 3);
            _gen_stack_push_long(C, SLJIT_R0, 0);
            break;
        }
        default: {
            sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM2(SLJIT_R2, SLJIT_R1), SLJIT_POINTER_SHIFT);
            _gen_stack_push_ref(C, SLJIT_R0, 0);
            break;
        }
    }
}

void _gen_arr_store(struct sljit_compiler *C, s32 datatype) {
    // =====================================================================
    //    s32 i = pop_int(stack);
    //    s32 index = pop_int(stack);
    //    Instance *jarr = (Instance *) pop_ref(stack);
    //    ret = _jarray_check_exception(jarr, index, runtime);
    //    if (!ret) {
    //        *(((s32 *) jarr->arr_body) + index) = i;
    //        ip++;
    //    } else {
    //        goto label_exception_handle;
    //    }
    // =====================================================================

    s32 slots;
    if (datatype == DATATYPE_LONG || datatype == DATATYPE_DOUBLE) {
        slots = 2;
    } else {
        slots = 1;
    }
    _gen_stack_size_modify(C, -2 - slots);


    _gen_save_sp_ip(C);
    _gen_stack_peek_ref(C, 0, SLJIT_R0, 0);//arr
    _gen_stack_peek_int(C, 1, SLJIT_R1, 0);//index
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, 32, P), SLJIT_IMM, SLJIT_FUNC_ADDR(_jarray_check_exception));
    _gen_load_sp_ip(C);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, RUNTIME_STATUS_EXCEPTION, -1, 0);

    _gen_stack_peek_ref(C, 0, SLJIT_R1, 0);//arr
    _gen_stack_peek_int(C, 1, SLJIT_R0, 0);//index
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Instance, arr_body));
    switch (datatype) {
        case DATATYPE_BOOLEAN:
        case DATATYPE_BYTE: {
            _gen_stack_peek_int(C, 2, SLJIT_R1, 0);
            sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_MEM2(SLJIT_R2, SLJIT_R0), 0, SLJIT_R1, 0);
            break;
        }
        case DATATYPE_SHORT: {
            _gen_stack_peek_int(C, 2, SLJIT_R1, 0);
            sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_MEM2(SLJIT_R2, SLJIT_R0), 1, SLJIT_R1, 0);
            break;
        }
        case DATATYPE_JCHAR: {
            _gen_stack_peek_int(C, 2, SLJIT_R1, 0);
            sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_MEM2(SLJIT_R2, SLJIT_R0), 1, SLJIT_R1, 0);
            break;
        }
        case DATATYPE_FLOAT:
        case DATATYPE_INT: {
            _gen_stack_peek_int(C, 2, SLJIT_R1, 0);
            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM2(SLJIT_R2, SLJIT_R0), 2, SLJIT_R1, 0);
            break;
        }
        case DATATYPE_LONG:
        case DATATYPE_DOUBLE: {
            _gen_stack_peek_long(C, 2, SLJIT_R1, 0);
            sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM2(SLJIT_R2, SLJIT_R0), 3, SLJIT_R1, 0);
            break;
        }
        default: {
            _gen_stack_peek_ref(C, 2, SLJIT_R1, 0);
            sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM2(SLJIT_R2, SLJIT_R0), SLJIT_POINTER_SHIFT, SLJIT_R1, 0);
            break;
        }
    }
}

//------------------------------  arithmetic  ----------------------

void _gen_div_0_exception_check(struct sljit_compiler *C, sljit_s32 op) {
    // =====================================================================
    //    if (!value1) {
    //        _arrithmetic_throw_exception(stack, runtime);
    //        ret = RUNTIME_STATUS_EXCEPTION;
    //        goto label_exception_handle;
    //    }
    // =====================================================================
    if (op == SLJIT_DIV_UW || op == SLJIT_DIV_SW || op == SLJIT_DIVMOD_UW || op == SLJIT_DIVMOD_SW
        || op == SLJIT_DIV_U32 || op == SLJIT_DIV_S32 || op == SLJIT_DIVMOD_U32 || op == SLJIT_DIVMOD_S32
            ) {
        _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
        _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_ARRITHMETIC, -2);
    }
}

void _gen_arith_int_2op(struct sljit_compiler *C, sljit_s32 op) {
    _gen_div_0_exception_check(C, op);

    _gen_stack_peek_int(C, -1, SLJIT_R1, 0);
    _gen_stack_peek_int(C, -2, SLJIT_R0, 0);
    //MUST mask shift value as bit length-1
    if (op == SLJIT_SHL32 || op == SLJIT_ASHR32 || op == SLJIT_LSHR32) {
        sljit_emit_op2(C, SLJIT_AND, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_IMM, 0x1f);
    }
    //R0=R0+R1
    if (op == SLJIT_DIV_UW || op == SLJIT_DIV_SW || op == SLJIT_DIVMOD_UW || op == SLJIT_DIVMOD_SW
        || op == SLJIT_DIV_U32 || op == SLJIT_DIV_S32 || op == SLJIT_DIVMOD_U32 || op == SLJIT_DIVMOD_S32
            ) {
        //check if div 0

        sljit_emit_op0(C, op);
    } else {
        sljit_emit_op2(C, op, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R1, 0);
    }
    if (op == SLJIT_DIVMOD_UW || op == SLJIT_DIVMOD_SW || op == SLJIT_DIVMOD_U32 || op == SLJIT_DIVMOD_S32) {
        _gen_stack_set_int(C, -2, SLJIT_R1, 0);
    } else {
        _gen_stack_set_int(C, -2, SLJIT_R0, 0);
    }
    _gen_stack_size_modify(C, -1);
}

void _gen_arith_float_2op(struct sljit_compiler *C, sljit_s32 op) {

    _gen_stack_peek_float(C, -1, SLJIT_FR1, 0);
    _gen_stack_peek_float(C, -2, SLJIT_FR0, 0);
    //R0=R0+R1
    sljit_emit_fop2(C, op, SLJIT_FR0, 0, SLJIT_FR0, 0, SLJIT_FR1, 0);
    _gen_stack_set_float(C, -2, SLJIT_FR0, 0);
    _gen_stack_size_modify(C, -1);
}

void _gen_arith_long_2op(struct sljit_compiler *C, sljit_s32 op) {

    _gen_stack_peek_long(C, -2, SLJIT_R1, 0);
    _gen_stack_peek_long(C, -4, SLJIT_R0, 0);
    //MUST mask shift value as bit length-1
    if (op == SLJIT_SHL || op == SLJIT_ASHR || op == SLJIT_LSHR) {
        sljit_emit_op2(C, SLJIT_AND, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_IMM, 0x3f);
    }
    if (op == SLJIT_DIV_UW || op == SLJIT_DIV_SW || op == SLJIT_DIVMOD_UW || op == SLJIT_DIVMOD_SW
        || op == SLJIT_DIV_U32 || op == SLJIT_DIV_S32 || op == SLJIT_DIVMOD_U32 || op == SLJIT_DIVMOD_S32
            ) {
        sljit_emit_op0(C, op);
    } else {
        //R0=R0+R1
        sljit_emit_op2(C, op, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R1, 0);
    }
    if (op == SLJIT_DIVMOD_UW || op == SLJIT_DIVMOD_SW || op == SLJIT_DIVMOD_U32 || op == SLJIT_DIVMOD_S32) {
        _gen_stack_set_long(C, -4, SLJIT_R1, 0);
    } else {
        _gen_stack_set_long(C, -4, SLJIT_R0, 0);
    }
    _gen_stack_size_modify(C, -2);
}

void _gen_arith_double_2op(struct sljit_compiler *C, sljit_s32 op) {

    _gen_stack_peek_double(C, -2, SLJIT_FR1, 0);
    _gen_stack_peek_double(C, -4, SLJIT_FR0, 0);
    //R0=R0+R1
    sljit_emit_fop2(C, op, SLJIT_FR0, 0, SLJIT_FR0, 0, SLJIT_FR1, 0);
    _gen_stack_set_double(C, -4, SLJIT_FR0, 0);
    _gen_stack_size_modify(C, -2);
}

//------------------------------  cmp  ----------------------

void _gen_icmp_op1(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 type) {
    s32 offset = *((s16 *) (ip + 1));
    s32 jumpto = code_idx + offset;
    struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
    }

    _gen_stack_pop_int(C, SLJIT_R0, 0);

    struct sljit_jump *jump_true, *jump_out, *jump_away;
    struct sljit_label *label_out, *label_true;
    jump_true = sljit_emit_cmp(C, type, SLJIT_R0, 0, SLJIT_IMM, 0);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {// if R0 vs. 0 true
        _gen_jump_to_suspend_check(C, offset);
        _gen_ip_modify_imm(C, offset);
        jump_away = sljit_emit_jump(C, SLJIT_JUMP);
        pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_out, label_out);
    sljit_set_label(jump_true, label_true);
}

void _gen_icmp_op2(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 test_type) {
    s32 offset = *((s16 *) (ip + 1));
    s32 jumpto = code_idx + offset;
    struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
    }

    _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
    _gen_stack_peek_int(C, -2, SLJIT_R1, 0);
    _gen_stack_size_modify(C, -2);

    sljit_s32 flag_set = 0;
    switch (test_type) {
        case SLJIT_SIG_GREATER:
            flag_set = SLJIT_SET_SIG_GREATER;
            break;
        case SLJIT_SIG_GREATER_EQUAL:
            flag_set = SLJIT_SET_SIG_GREATER_EQUAL;
            break;
        case SLJIT_SIG_LESS:
            flag_set = SLJIT_SET_SIG_LESS;
            break;
        case SLJIT_SIG_LESS_EQUAL:
            flag_set = SLJIT_SET_SIG_LESS_EQUAL;
            break;
    }
    //flag_set : SLJIT_SET_SIG_LESS
    sljit_emit_op2u(C, SLJIT_SUB | flag_set, SLJIT_R1, 0, SLJIT_R0, 0);

    sljit_emit_op_flags(C, SLJIT_MOV, SLJIT_R2, 0, test_type);

    struct sljit_jump *jump_if_true, *jump_out, *jump_away;
    struct sljit_label *label_out, *label_true;
    jump_if_true = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {
        _gen_jump_to_suspend_check(C, offset);
        _gen_ip_modify_imm(C, offset);
        jump_away = sljit_emit_jump(C, SLJIT_JUMP);
        pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_if_true, label_true);
    sljit_set_label(jump_out, label_out);
}

void _gen_cmp_reg2(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 reg1, sljit_s32 reg2, sljit_s32 type) {
    s32 offset = *((s16 *) (ip + 1));
    s32 jumpto = code_idx + offset;
    struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
    }

    struct sljit_jump *jump_out, *jump_if_true;
    struct sljit_label *label_true, *label_out;
    //flag_type : SLJIT_SET_SIG_LESS
    sljit_emit_op2(C, SLJIT_SUB, reg1, 0, reg1, 0, reg2, 0);

    //type  = SLJIT_EQUAL ...
    //if R0 == 0 then jump to equ_0
    jump_if_true = sljit_emit_cmp(C, type, reg1, 0, SLJIT_IMM, 0);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {
        _gen_jump_to_suspend_check(C, offset);
        _gen_ip_modify_imm(C, offset);
        struct sljit_jump *jump_away = sljit_emit_jump(C, SLJIT_JUMP);
        pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, jumpto);
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_if_true, label_true);
    sljit_set_label(jump_out, label_out);
}


void _gen_goto(struct sljit_compiler *C, MethodInfo *method, s32 code_idx, s32 offset) {
    _gen_jump_to_suspend_check(C, offset);
    _gen_ip_modify_imm(C, offset);

    s32 jumpto = code_idx + offset;
    struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
    }

    struct sljit_jump *jump_away = sljit_emit_jump(C, SLJIT_JUMP);
    pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, jumpto);
}

void _gen_parilist_get(struct sljit_compiler *C, Pairlist *list) {
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_IMM, (sljit_sw) list);
    //r0=list->count
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Pairlist, count));
    //r1=list->ptr
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Pairlist, ptr));
    //R0=list->count * sizeof(Pair)   //end ptr
    sljit_emit_op2(C, SLJIT_MUL, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, sizeof(Pair));
    //max count ptr
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R1, 0);

    struct sljit_jump *jump_to_loop, *jump_to_not_equal, *jump_to_end_loop1;
    struct sljit_label *label_not_equal, *label_end_loop;
    //for
    struct sljit_label *lable_loop = sljit_emit_label(C);
    //if equal
    struct sljit_jump *jump_to_end_loop = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_R0, 0);
    //body
    {
        jump_to_not_equal = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Pair, left), SLJIT_R2, 0);
        {//found left
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Pair, right));
            sljit_emit_return(C, SLJIT_MOV_P, SLJIT_R2, 0);
            jump_to_end_loop1 = sljit_emit_jump(C, SLJIT_JUMP);
        }
        label_not_equal = sljit_emit_label(C);
        //ptr++
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R1, 0, SLJIT_R0, 0, SLJIT_IMM, sizeof(Pair));
        //
        jump_to_loop = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_end_loop = sljit_emit_label(C);
    //
    sljit_set_label(jump_to_not_equal, label_not_equal);
    sljit_set_label(jump_to_loop, lable_loop);
    sljit_set_label(jump_to_end_loop, label_end_loop);
    sljit_set_label(jump_to_end_loop1, label_end_loop);
}
//
//void _gen_invokevirtual(struct sljit_compiler *C, ConstantMethodRef *cmr) {
//
//    //Instance *ins = (stack->sp - 1 - cmr->para_slots)->rvalue;//getInstanceInStack(cmr, stack);
//    _gen_stack_peek_ref(C, -1 - cmr->para_slots, SLJIT_R2, 0);
//
//    //if instance == 0 then jump to equ_0
//    struct sljit_jump *jump_if_ins_not_null = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);
//
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK);
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
//    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2V(P,P), SLJIT_IMM, SLJIT_FUNC_ADDR(_null_throw_exception));
//    struct sljit_jump *jump_to_exception_handle = sljit_emit_jump(C, SLJIT_JUMP);
//
//    //R0=stack,R1=runtime
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK);
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
//    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32,P,P), SLJIT_IMM, SLJIT_FUNC_ADDR(exception_handle));
//    struct sljit_jump *throw_2_parent_jump = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0);
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * 3);
//    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_IP, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(Runtime, pc));
//    struct sljit_jump *jump_to_exception_handle_success = sljit_emit_jump(C, SLJIT_JUMP);
//
//    sljit_set_label(throw_2_parent_jump, sljit_emit_label(C));
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_RETURN_REG, 0, SLJIT_R0, 0);
//    sljit_emit_return(C, SLJIT_MOV, SLJIT_RETURN_REG, 0);
//
//    struct sljit_label *label_ins_not_null = sljit_emit_label(C);
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) cmr->virtual_methods);
//    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(MemoryBlock, clazz));
//    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(P,P,P), SLJIT_IMM, SLJIT_FUNC_ADDR(pairlist_get));
//    struct sljit_jump *method_found_jump = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0);
//
//    struct sljit_label *label_method_found = sljit_emit_label(C);
//
//    struct sljit_label *label_method_not_found = sljit_emit_label(C);
//
//    struct sljit_label *out = sljit_emit_label(C);
//    sljit_set_label(jump_if_ins_not_null, label_ins_not_null);
//    sljit_set_label(jump_to_exception_handle_success, out);
//    sljit_set_label(method_found_jump, out);
//
//}

void _gen_exception_new(struct sljit_compiler *C, s32 exception_type) {
    _gen_save_sp_ip(C);
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R0, 0, SLJIT_IMM, exception_type);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(W, 32, P), SLJIT_IMM, SLJIT_FUNC_ADDR(exception_create));
    _gen_load_sp_ip(C);
    _gen_stack_push_ref(C, SLJIT_RETURN_REG, 0);
}

void _gen_exception_handle(struct sljit_compiler *C) {
    _gen_save_sp_ip(C);
    //R0=stack,R1=runtime
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(exception_handle));
    _gen_load_sp_ip(C);

    struct sljit_jump *jump_found_handle, *jump_out, *jump_away;
    struct sljit_label *label_out, *label_found_handle;
    jump_found_handle = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, 0);
    {
        //_debug_gen_print_reg(C);
        sljit_emit_return(C, SLJIT_MOV, SLJIT_IMM, RUNTIME_STATUS_EXCEPTION);
    }
    label_found_handle = sljit_emit_label(C);
    {// if R0 vs. 0 true
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Runtime, method));
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(MethodInfo, converted_code));
        sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_IP, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(CodeAttribute, code));
        sljit_emit_op2(C, SLJIT_ADD, REGISTER_IP, 0, REGISTER_IP, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Runtime, jit_exception_bc_pos));
        _gen_load_sp_ip(C);
        sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Runtime, jit_exception_jump_ptr));
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_found_handle, label_found_handle);
}

/**
 *    if src1=src then throw exception(type) and handle
 *    dont throw when throw_type = -1
 *
 *
 * @param C
 * @param src1
 * @param srcw1
 * @param src2
 * @param srcw2
 * @param throw_type
 */
void _gen_exception_check_throw_handle(struct sljit_compiler *C, sljit_s32 cmp, sljit_s32 src1, sljit_sw srcw1, sljit_s32 src2, sljit_sw srcw2, s32 throw_type, s32 stack_adjust) {

    struct sljit_jump *jump_true, *jump_out;
    struct sljit_label *label_out, *label_true;
    jump_true = sljit_emit_cmp(C, cmp, src1, srcw1, src2, srcw2);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {// if R0 vs. 0 true
        if (stack_adjust) {
            _gen_stack_size_modify(C, stack_adjust);
        }
        if (throw_type != -1) {
            _gen_exception_new(C, throw_type);
        }
        //_debug_gen_print_reg(C);
        _gen_exception_handle(C);
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_out, label_out);
    sljit_set_label(jump_true, label_true);

}

void _gen_jdwp(struct sljit_compiler *C) {
//    JavaThreadInfo *threadInfo = runtime->threadInfo;
//    if (jdwp_enable) {
//        //breakpoint
//        if (method->breakpoint) {
//            jdwp_check_breakpoint(runtime);
//        }
//        //debug step
//        if (threadInfo->jdwp_step.active) {//单步状态
//            threadInfo->jdwp_step.bytecode_count++;
//            jdwp_check_debug_step(runtime);
//
//        }
//    }
}

void _gen_jump_to_suspend_check(struct sljit_compiler *C, int offset) {
    if (offset < 0)
        sljit_emit_ijump(C, SLJIT_FAST_CALL, SLJIT_IMM, SLJIT_FUNC_ADDR(check_suspend));
}

//------------------------------  inst impl  ----------------------

s32 multiarray(Runtime *runtime, Utf8String *desc, s32 count) {
    RuntimeStack *stack = runtime->stack;
#ifdef __JVM_OS_VS__
    s32 dim[32];
#else
    s32 dim[count];
#endif
    int i;
    for (i = 0; i < count; i++)
        dim[i] = pop_int(stack);

    Instance *arr = jarray_multi_create(runtime, dim, count, desc, 0);

    if (!arr) {
        return RUNTIME_STATUS_EXCEPTION;
    } else {
        push_ref(stack, (__refer) arr);
    }
    return RUNTIME_STATUS_NORMAL;
}


s32 invokevirtual(Runtime *runtime, s32 idx) {
    if (utf8_equals_c(runtime->method->_this_class->name, "org/mini/json/JsonParser")
        && utf8_equals_c(runtime->method->name, "map2obj")) {
        s32 debug = 1;
    }
    s32 ret = 0;
    ConstantMethodRef *cmr = class_get_constant_method_ref(runtime->clazz, idx);
    RuntimeStack *stack = runtime->stack;
    Instance *ins = getInstanceInStack(cmr, stack);
    if (!ins) {
        _null_throw_exception(stack, runtime);
        return RUNTIME_STATUS_EXCEPTION;
    } else {
        MethodInfo *m = (MethodInfo *) pairlist_get(cmr->virtual_methods, ins->mb.clazz);
        if (!m) {
            m = find_instance_methodInfo_by_name(ins, cmr->name, cmr->descriptor, runtime);
            spin_lock(&runtime->jvm->lock_cloader);
            {
                pairlist_put(cmr->virtual_methods, ins->mb.clazz, m);//放入缓存，以便下次直接调用
            }
            spin_unlock(&runtime->jvm->lock_cloader);
        }

        if (!m) {
            _nosuchmethod_check_exception(utf8_cstr(cmr->name), stack, runtime);
            return RUNTIME_STATUS_EXCEPTION;
        } else {
            ret = execute_method_impl(m, runtime);
            if (ret) {
                return ret;
            }
        }
    }
    return RUNTIME_STATUS_NORMAL;
}


static float frem(float value1, float value2) {
    return value2 - ((int) (value2 / value1) * value1);
}

static double drem_1(double value1, double value2) {
    return value2 - ((s64) (value2 / value1) * value1);
}


static s32 fcmp(u8 bytecode, float value1, float value2) {
    if (isnan(value1) || isnan(value2)) {
        if (bytecode == op_fcmpg) {
            return 1;
        } else {
            return -1;
        }
    }
    return value2 == value1 ? 0 : (value2 > value1 ? 1 : -1);
}

static s32 dcmp(u8 bytecode, double value1, double value2) {
    if (isnan(value1) || isnan(value2)) {
        if (bytecode == op_dcmpg) {
            return 1;
        } else {
            return -1;
        }
    }
    return value2 == value1 ? 0 : (value2 > value1 ? 1 : -1);
}

static s32 instanceof(JClass *other, Instance *ins, Runtime *runtime) {
    s32 checkok = 0;
    if (!ins) {
    } else if (ins->mb.type & (MEM_TYPE_INS | MEM_TYPE_ARR)) {
        if (instance_of(ins, other)) {
            checkok = 1;
        }
    }
    return checkok;
}


//-----------------------------------------------------------------
//------------------------------  gen jit impl  ----------------------
//-----------------------------------------------------------------

void gen_jit_suspend_check_func() {
    struct sljit_compiler *C = sljit_create_compiler(NULL, NULL);
    sljit_set_context(C, 0, 0, 3, 3, 3, 0, LOCAL_COUNT * sizeof(sljit_sw));

    sljit_emit_op_dst(C, SLJIT_FAST_ENTER, SLJIT_R2, 0);

    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_THREADINFO);
    sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(JavaThreadInfo, suspend_count));

    struct sljit_jump *jump_suspended, *jump_out, *jump_to_interrupted, *jump_not_interrupted;
    struct sljit_label *label_out, *label_suspended, *label_not_interrupted, *label_interrupted;
    jump_suspended = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_suspended = sljit_emit_label(C);
    {
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R2, SLJIT_R2, 0);
        _gen_save_sp_ip(C);
        sljit_emit_op1(C, SLJIT_MOV_U8, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(JavaThreadInfo, is_interrupt));
        jump_to_interrupted = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0);
        {
            jump_not_interrupted = sljit_emit_jump(C, SLJIT_JUMP);
        }
        label_interrupted = sljit_emit_label(C);
        {
            //set R2 to label_interrupt_handle address ,
            // that address saved in method->ca->interrupt_handle_jump_ptr
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_METHOD);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(MethodInfo, converted_code));
            sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, SLJIT_OFFSETOF(CodeAttribute, jit));
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(Jit, interrupt_handle_jump_ptr));
            sljit_emit_op_src(C, SLJIT_FAST_RETURN, SLJIT_R2, 0);
        }
        label_not_interrupted = sljit_emit_label(C);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
        sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS1(32, P), SLJIT_IMM, SLJIT_FUNC_ADDR(check_suspend_and_pause));

        _gen_load_sp_ip(C);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R2);

    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_out, label_out);
    sljit_set_label(jump_suspended, label_suspended);
    sljit_set_label(jump_to_interrupted, label_interrupted);
    sljit_set_label(jump_not_interrupted, label_not_interrupted);

    sljit_emit_op_src(C, SLJIT_FAST_RETURN, SLJIT_R2, 0);


    check_suspend = sljit_generate_code(C);
    sljit_uw len = sljit_get_generated_code_size(C);
    sljit_free_compiler(C);
    //dump_code(check_suspend, len);
}

s32 gen_jit_bytecode_func(struct sljit_compiler *C, MethodInfo *method, Runtime *runtime) {
#if JIT_DEBUG
    if (
//            (utf8_equals_c(method->_this_class->name, "java/lang/ClassLoader")
//             && utf8_equals_c(method->descriptor, "(Ljava/lang/String;Z)Ljava/lang/Class;")
//             && utf8_equals_c(method->name, "loadClass"))
//            ||
            (utf8_equals_c(method->_this_class->name, "com/ebsee/shl/main/GamePanel")
             && utf8_equals_c(method->descriptor, "(J)V")
             && utf8_equals_c(method->name, "paint_title"))
            ) {
        int debug = 1;

    } else {
        return JIT_GEN_ERROR;
    }

    //    if (utf8_equals_c(method->_this_class->name, "org/mini/gui/GContainer")&&utf8_equals_c(method->name, "drawObj")) {
    //        int debug = 1;
    //        return JIT_GEN_ERROR;
    //    } else {
    //    }
#endif


    CodeAttribute *ca = method->converted_code;
    u8 *ip = ca->bytecode_for_jit;
    u8 *end = ca->code_length + ip;
    s32 i;

    {// exception pc need label
        ExceptionTable *e = ca->exception_table;
        for (i = 0; i < ca->exception_table_length; i++) {
            s32 pos = (e + i)->handler_pc;
            pairlist_putl(method->pos_2_label, pos, -1);// save label pos in list
        }
    }
    JClass *clazz = method->_this_class;

    void *genfunc;

    /* Start a context(function entry), have 2 arguments, discuss later */
    sljit_emit_enter(C, 0, SLJIT_ARGS2(W, P, P), 3, 3, 3, 0, LOCAL_COUNT * sizeof(sljit_sw));

    /* SLJIT_SP is the init address of local var */
    //arr[LOCAL_METHOD]= (S0)MethodInfo *method
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_METHOD, SLJIT_S0, 0);
    //arr[LOCAL_RUNTIME]= (S1)Runtime *runtime
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME, SLJIT_S1, 0);


    //S2 = method->converted_code->code;
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S0), SLJIT_OFFSETOF(MethodInfo, converted_code));
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_IP, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(CodeAttribute, code));

    //S0=runtime->stack->sp
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, stack));
    //arr[LOCAL_STACK]= runtime->stack->sp
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK, SLJIT_R0, 0);
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, SLJIT_OFFSETOF(RuntimeStack, sp));
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_SP, 0, SLJIT_MEM1(SLJIT_R0), 0);
    //arr[LOCAL_STACK_SP]= runtime->stack->sp
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP, SLJIT_R0, 0);
    //arr[LOCAL_RUNTIME_PC]= runtime->pc
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_S1, 0, SLJIT_IMM, SLJIT_OFFSETOF(Runtime, pc));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME_PC, SLJIT_R0, 0);
    //arr[LOCAL_THREADINFO_SUSPEND]= runtime->threadInfo
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, thrd_info));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_THREADINFO, SLJIT_R0, 0);
    //S1=runtime->localvar
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_LOCALVAR, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, localvar));


    _gen_jump_to_suspend_check(C, -1);
    //S0=sp , S1=localvar , S2=ip
#if JIT_DEBUG
    //_debug_gen_print_callstack(C);
    //_debug_gen_print_stack(C);
    sljit_emit_op0(C, SLJIT_NOP);
#endif
    while (ip < end) {
        u8 cur_inst = *ip;
        s32 code_idx = (s32) (ip - ca->bytecode_for_jit);

        //generate label
        if (pairlist_getl(method->pos_2_label, code_idx)) {
            struct sljit_label *label = sljit_emit_label(C);
            pairlist_putl(method->pos_2_label, code_idx, (intptr_t) label);
        }
        switch (cur_inst) {
            case op_nop: {
                sljit_emit_op0(C, SLJIT_NOP);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_aconst_null: {
                //push_ref(stack, 0);
                _gen_stack_push_ref(C, SLJIT_IMM, (sljit_sw) NULL);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_iconst_m1:
            case op_iconst_0:
            case op_iconst_1:
            case op_iconst_2:
            case op_iconst_3:
            case op_iconst_4:
            case op_iconst_5: {
                //push_int(stack, i);
                _gen_stack_push_int(C, SLJIT_IMM, cur_inst - op_iconst_0);
                _gen_ip_modify_imm(C, 1);

                ip++;
                break;
            }
            case op_lconst_0:
            case op_lconst_1: {
                //push_long(stack, value);
                _gen_stack_push_long(C, SLJIT_IMM, cur_inst - op_lconst_0);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fconst_0:
            case op_fconst_1:
            case op_fconst_2: {
                // push_float(stack, value);
                Int2Float i2f;
                i2f.f = (f32) (cur_inst - op_fconst_0);
                _gen_stack_push_int(C, SLJIT_IMM, i2f.i);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dconst_0:
            case op_dconst_1: {
                Long2Double l2d;
                l2d.d = cur_inst - op_dconst_0;
                _gen_stack_push_long(C, SLJIT_IMM, l2d.l);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_bipush: {
                //push_int(stack, v);
                s8 v = (s8) ip[1];
                _gen_stack_push_int(C, SLJIT_IMM, v);
                _gen_ip_modify_imm(C, 2);
                ip += 2;
                break;
            }
            case op_sipush: {
                // push_int(stack, i);
                _gen_stack_push_int(C, SLJIT_IMM, *((s16 *) (ip + 1)));
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }


            case op_ldc:
            case op_ldc_w: {
                u16 index = 0;
                if (cur_inst == op_ldc) {
                    index = ip[1];
                } else {
                    index = *((u16 *) (ip + 1));
                }
                ConstantItem *item = class_get_constant_item(clazz, index);
                switch (item->tag) {
                    case CONSTANT_INTEGER:
                    case CONSTANT_FLOAT: {
                        s32 v = class_get_constant_integer(clazz, index);
                        //printf("ldc %d %f\n", v, *((f32 *) &v));
                        _gen_stack_push_int(C, SLJIT_IMM, v);
                        break;
                    }
                    case CONSTANT_STRING_REF: {
                        ConstantUTF8 *cutf = class_get_constant_utf8(clazz, class_get_constant_stringref(clazz, index)->stringIndex);
                        //push_ref(stack, (__refer) cutf->jstr);
                        _gen_stack_push_ref(C, SLJIT_IMM, (sljit_sw) cutf->jstr);
                        break;
                    }
                    case CONSTANT_CLASS: {
                        JClass *cl = classes_load_get(clazz->jloader, class_get_constant_classref(clazz, index)->name, runtime);
                        if (!cl->ins_class) {
                            cl->ins_class = insOfJavaLangClass_create_get(runtime, cl);
                        }
                        //push_ref(stack, cl->ins_class);
                        _gen_stack_push_ref(C, SLJIT_IMM, (sljit_sw) cl->ins_class);
                        break;
                    }
                    default: {
                        jvm_printf("ldc: something not implemention \n");
                    }
                }

                if (cur_inst == op_ldc) {
                    _gen_ip_modify_imm(C, 2);
                    ip += 2;
                } else {
                    _gen_ip_modify_imm(C, 3);
                    ip += 3;
                }

                break;
            }

            case op_ldc2_w: {
                //push_long(stack, value);
                s64 value = class_get_constant_long(clazz, *((u16 *) (ip + 1)));//long or double
                _gen_stack_push_long(C, SLJIT_IMM, value);
                _gen_ip_modify_imm(C, 3);
                ip += 3;

                break;
            }


            case op_iload:
            case op_fload: {
                s32 index = (u8) ip[1];
                _gen_i_f_load(C, index);
                _gen_ip_modify_imm(C, 2);
                ip += 2;
                break;
            }

            case op_aload: {
                s32 index = (u8) ip[1];
                _gen_a_load(C, index);
                _gen_ip_modify_imm(C, 2);

                ip += 2;
                break;
            }
            case op_lload:
            case op_dload: {
                //push_long(stack, runtime->localvar[index].lvalue);
                s32 index = (u8) ip[1];
                _gen_l_d_load(C, index);
                _gen_ip_modify_imm(C, 2);
                ip += 2;
                break;
            }

            case op_iload_0:
            case op_iload_1:
            case op_iload_2:
            case op_iload_3: {
                _gen_i_f_load(C, cur_inst - op_iload_0);
                _gen_ip_modify_imm(C, 1);

                ip++;
                break;
            }
            case op_lload_0:
            case op_lload_1:
            case op_lload_2:
            case op_lload_3: {
                _gen_l_d_load(C, cur_inst - op_lload_0);
                _gen_ip_modify_imm(C, 1);

                ip++;
                break;
            }
            case op_fload_0:
            case op_fload_1:
            case op_fload_2:
            case op_fload_3: {
                _gen_i_f_load(C, cur_inst - op_fload_0);
                _gen_ip_modify_imm(C, 1);

                ip++;
                break;
            }
            case op_dload_0:
            case op_dload_1:
            case op_dload_2:
            case op_dload_3: {
                _gen_l_d_load(C, cur_inst - op_dload_0);
                _gen_ip_modify_imm(C, 1);

                ip++;
                break;
            }
            case op_aload_0:
            case op_aload_1:
            case op_aload_2:
            case op_aload_3: {
                _gen_a_load(C, cur_inst - op_aload_0);
                _gen_ip_modify_imm(C, 1);

                ip++;
                break;
            }
            case op_iaload:
            case op_faload: {
                _gen_arr_load(C, DATATYPE_INT);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_laload:
            case op_daload: {
                _gen_arr_load(C, DATATYPE_LONG);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_aaload: {
                _gen_arr_load(C, DATATYPE_REFERENCE);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_baload: {
                _gen_arr_load(C, DATATYPE_BYTE);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_caload: {
                _gen_arr_load(C, DATATYPE_JCHAR);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_saload: {
                _gen_arr_load(C, DATATYPE_SHORT);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_istore:
            case op_fstore: {
                s32 index = (u8) ip[1];
                _gen_i_f_store(C, index);
                _gen_ip_modify_imm(C, 2);
                ip += 2;
                break;
            }
            case op_astore: {
                s32 index = (u8) ip[1];
                _gen_a_store(C, index);
                _gen_ip_modify_imm(C, 2);
                ip += 2;
                break;
            }
            case op_lstore:
            case op_dstore: {
                s32 index = (u8) ip[1];
                _gen_l_d_store(C, index);
                _gen_ip_modify_imm(C, 2);
                ip += 2;
                break;
            }
            case op_istore_0:
            case op_istore_1:
            case op_istore_2:
            case op_istore_3: {
                _gen_i_f_store(C, cur_inst - op_istore_0);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lstore_0:
            case op_lstore_1:
            case op_lstore_2:
            case op_lstore_3: {
                _gen_l_d_store(C, cur_inst - op_lstore_0);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fstore_0:
            case op_fstore_1:
            case op_fstore_2:
            case op_fstore_3: {
                _gen_i_f_store(C, cur_inst - op_fstore_0);

//                sljit_emit_fop1(C, SLJIT_CONV_S32_FROM_F32, SLJIT_FR0, 0, SLJIT_R0, 0);
                //sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_FR0, 0, SLJIT_MEM1(REGISTER_SP), 0);
//                _debug_gen_print_freg(C);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dstore_0:
            case op_dstore_1:
            case op_dstore_2:
            case op_dstore_3: {
                _gen_l_d_store(C, cur_inst - op_dstore_0);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_astore_0:
            case op_astore_1:
            case op_astore_2:
            case op_astore_3: {
                _gen_a_store(C, cur_inst - op_astore_0);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fastore:
            case op_iastore: {
                _gen_arr_store(C, DATATYPE_INT);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dastore:
            case op_lastore: {
                _gen_arr_store(C, DATATYPE_LONG);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_aastore: {
                _gen_arr_store(C, DATATYPE_REFERENCE);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_bastore: {
                _gen_arr_store(C, DATATYPE_BYTE);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_castore: {
                _gen_arr_store(C, DATATYPE_JCHAR);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_sastore: {
                _gen_arr_store(C, DATATYPE_SHORT);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_pop: {
                _gen_stack_size_modify(C, -1);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_pop2: {
                _gen_stack_size_modify(C, -2);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dup: {
                //add1
                _gen_stack_size_modify(C, 1);
                //-2  ==>  -1
                _gen_stack_peek_entry(C, -2, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dup_x1: {
                //add 1
                _gen_stack_size_modify(C, 1);
                //-2   ==>  -1
                _gen_stack_peek_entry(C, -2, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-3   ==>  -2
                _gen_stack_peek_entry(C, -3, SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-1   ==>  -3
                _gen_stack_peek_entry(C, -1, SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dup_x2: {
                //add 1
                _gen_stack_size_modify(C, 1);
                //-2   ==>  -1
                _gen_stack_peek_entry(C, -2, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-3   ==>  -2
                _gen_stack_peek_entry(C, -3, SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-4   ==>  -3
                _gen_stack_peek_entry(C, -4, SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-1   ==>  -4
                _gen_stack_peek_entry(C, -1, SLJIT_MEM1(REGISTER_SP), -4 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -4 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dup2: {
                //add2
                _gen_stack_size_modify(C, 2);
                //-4  ==>  -2
                _gen_stack_peek_entry(C, -4, SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-3  ==>  -1
                _gen_stack_peek_entry(C, -3, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dup2_x1: {
                //add 2
                _gen_stack_size_modify(C, 2);
                //-3   ==>  -1
                _gen_stack_peek_entry(C, -3, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-4   ==>  -2
                _gen_stack_peek_entry(C, -4, SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-5   ==>  -3
                _gen_stack_peek_entry(C, -5, SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-2   ==>  -5
                _gen_stack_peek_entry(C, -2, SLJIT_MEM1(REGISTER_SP), -5 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -5 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-1   ==>  -4
                _gen_stack_peek_entry(C, -1, SLJIT_MEM1(REGISTER_SP), -4 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -4 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dup2_x2: {
                //add 2
                _gen_stack_size_modify(C, 2);
                //-3   ==>  -1
                _gen_stack_peek_entry(C, -3, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-4   ==>  -2
                _gen_stack_peek_entry(C, -4, SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-5   ==>  -3
                _gen_stack_peek_entry(C, -5, SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -3 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-6   ==>  -4
                _gen_stack_peek_entry(C, -6, SLJIT_MEM1(REGISTER_SP), -4 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -4 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-2   ==>  -6
                _gen_stack_peek_entry(C, -2, SLJIT_MEM1(REGISTER_SP), -6 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -6 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-1   ==>  -5
                _gen_stack_peek_entry(C, -1, SLJIT_MEM1(REGISTER_SP), -5 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -5 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_swap: {
                //-2   ==>  0
                _gen_stack_peek_entry(C, -2, SLJIT_MEM1(REGISTER_SP), 0 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), 0 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //-1   ==>  -2
                _gen_stack_peek_entry(C, -1, SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -2 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));
                //0   ==>  -1
                _gen_stack_peek_entry(C, 0, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_iadd: {
                _gen_arith_int_2op(C, SLJIT_ADD);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ladd: {
                _gen_arith_long_2op(C, SLJIT_ADD);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fadd: {
                _gen_arith_float_2op(C, SLJIT_ADD_F32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dadd: {
                _gen_arith_double_2op(C, SLJIT_ADD_F64);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_isub: {
                _gen_arith_int_2op(C, SLJIT_SUB);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lsub: {
                _gen_arith_long_2op(C, SLJIT_SUB);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fsub: {
                _gen_arith_float_2op(C, SLJIT_SUB_F32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dsub: {
                _gen_arith_double_2op(C, SLJIT_SUB_F64);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_imul: {
                _gen_arith_int_2op(C, SLJIT_MUL);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lmul: {
                _gen_arith_long_2op(C, SLJIT_MUL);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fmul: {
                _gen_arith_float_2op(C, SLJIT_MUL_F32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dmul: {
                _gen_arith_double_2op(C, SLJIT_MUL_F64);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_idiv: {
                _gen_arith_int_2op(C, SLJIT_DIV_S32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ldiv: {
                _gen_arith_long_2op(C, SLJIT_DIV_SW);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fdiv: {
                _gen_arith_float_2op(C, SLJIT_DIV_F32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ddiv: {
                _gen_arith_double_2op(C, SLJIT_DIV_F64);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_irem: {
                _gen_arith_int_2op(C, SLJIT_DIVMOD_S32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lrem: {
                _gen_arith_long_2op(C, SLJIT_DIVMOD_SW);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_frem: {
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                _gen_stack_peek_float(C, -2, SLJIT_FR1, 0);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(F32, F32, F32), SLJIT_IMM, SLJIT_FUNC_ADDR(frem));
                _gen_stack_set_float(C, -2, SLJIT_FR0, 0);
                _gen_stack_size_modify(C, -1);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_drem: {
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                _gen_stack_peek_double(C, -4, SLJIT_FR1, 0);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(F64, F64, F64), SLJIT_IMM, SLJIT_FUNC_ADDR(drem_1));
                _gen_stack_set_double(C, -4, SLJIT_FR0, 0);
                _gen_stack_size_modify(C, -2);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ineg: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                sljit_emit_op2(C, SLJIT_SUB32, SLJIT_R0, 0, SLJIT_IMM, 0, SLJIT_R0, 0);
                _gen_stack_set_int(C, -1, SLJIT_R0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lneg: {
                _gen_stack_peek_long(C, -2, SLJIT_R0, 0);
                sljit_emit_op2(C, SLJIT_SUB, SLJIT_R0, 0, SLJIT_IMM, 0, SLJIT_R0, 0);
                _gen_stack_set_long(C, -2, SLJIT_R0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fneg: {
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_NEG_F32, SLJIT_FR0, 0, SLJIT_FR0, 0);
                _gen_stack_set_float(C, -1, SLJIT_FR0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dneg: {
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_NEG_F64, SLJIT_FR0, 0, SLJIT_FR0, 0);
                _gen_stack_set_double(C, -2, SLJIT_FR0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ishl: {
                _gen_arith_int_2op(C, SLJIT_SHL32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lshl: {
                _gen_stack_peek_int(C, -1, SLJIT_R1, 0);
                _gen_stack_peek_long(C, -3, SLJIT_R0, 0);
                //R0=R0+R1
                sljit_emit_op2(C, SLJIT_AND, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_IMM, 0x3f);
                sljit_emit_op2(C, SLJIT_SHL, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R1, 0);
                _gen_stack_set_long(C, -3, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ishr: {
                _gen_arith_int_2op(C, SLJIT_ASHR32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lshr: {
                _gen_stack_peek_int(C, -1, SLJIT_R1, 0);
                _gen_stack_peek_long(C, -3, SLJIT_R0, 0);
                //R0=R0+R1
                sljit_emit_op2(C, SLJIT_AND, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_IMM, 0x3f);
                sljit_emit_op2(C, SLJIT_ASHR, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R1, 0);
                _gen_stack_set_long(C, -3, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_iushr: {
                _gen_arith_int_2op(C, SLJIT_LSHR32);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lushr: {
                _gen_stack_peek_int(C, -1, SLJIT_R1, 0);
                _gen_stack_peek_long(C, -3, SLJIT_R0, 0);
                //R0=R0+R1
                sljit_emit_op2(C, SLJIT_AND, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_IMM, 0x3f);
                sljit_emit_op2(C, SLJIT_LSHR, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R1, 0);
                _gen_stack_set_long(C, -3, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_iand: {
                _gen_arith_int_2op(C, SLJIT_AND);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_land: {
                _gen_arith_long_2op(C, SLJIT_AND);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ior: {
                _gen_arith_int_2op(C, SLJIT_OR);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lor: {
                _gen_arith_long_2op(C, SLJIT_OR);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ixor: {
                _gen_arith_int_2op(C, SLJIT_XOR);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lxor: {
                _gen_arith_long_2op(C, SLJIT_XOR);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }

            case op_iinc: {
                _gen_local_get_int(C, (u8) ip[1], SLJIT_R0, 0);
                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, (s8) ip[2]);
                _gen_local_set_int(C, (u8) ip[1], SLJIT_R0, 0);

                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_i2l: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                _gen_stack_set_long(C, -1, SLJIT_R0, 0);
                _gen_stack_size_modify(C, 1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_i2f: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_F32_FROM_S32, SLJIT_FR0, 0, SLJIT_R0, 0);
                _gen_stack_set_float(C, -1, SLJIT_FR0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_i2d: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_F64_FROM_S32, SLJIT_FR0, 0, SLJIT_R0, 0);
                _gen_stack_set_double(C, -1, SLJIT_FR0, 0);
                _gen_stack_size_modify(C, 1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_l2i: {
                _gen_stack_peek_long(C, -2, SLJIT_R0, 0);
                _gen_stack_set_int(C, -2, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_l2f: {
                _gen_stack_peek_long(C, -2, SLJIT_R0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_F32_FROM_SW, SLJIT_FR0, 0, SLJIT_R0, 0);
                _gen_stack_set_float(C, -2, SLJIT_FR0, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_l2d: {
                _gen_stack_peek_long(C, -2, SLJIT_R0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_F64_FROM_SW, SLJIT_FR0, 0, SLJIT_R0, 0);
                _gen_stack_set_double(C, -2, SLJIT_FR0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_f2i: {
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_S32_FROM_F32, SLJIT_R0, 0, SLJIT_FR0, 0);
                _gen_stack_set_int(C, -1, SLJIT_R0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_f2l: {
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_SW_FROM_F32, SLJIT_R0, 0, SLJIT_FR0, 0);
                _gen_stack_set_long(C, -1, SLJIT_R0, 0);
                _gen_stack_size_modify(C, 1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_f2d: {
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_F64_FROM_F32, SLJIT_FR1, 0, SLJIT_FR0, 0);
                _gen_stack_set_double(C, -1, SLJIT_FR1, 0);
                _gen_stack_size_modify(C, 1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_d2i: {
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_S32_FROM_F64, SLJIT_R0, 0, SLJIT_FR0, 0);
                _gen_stack_set_int(C, -2, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_d2l: {
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_SW_FROM_F64, SLJIT_R0, 0, SLJIT_FR0, 0);
                _gen_stack_set_long(C, -2, SLJIT_R0, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_d2f: {
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                sljit_emit_fop1(C, SLJIT_CONV_F32_FROM_F64, SLJIT_FR1, 0, SLJIT_FR0, 0);
                _gen_stack_set_float(C, -2, SLJIT_FR1, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_i2b: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_R1, 0, SLJIT_R0, 0);
                _gen_stack_set_int(C, -1, SLJIT_R1, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_i2c: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R1, 0, SLJIT_R0, 0);
                _gen_stack_set_int(C, -1, SLJIT_R1, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_i2s: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_R1, 0, SLJIT_R0, 0);
                _gen_stack_set_int(C, -1, SLJIT_R1, 0);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_lcmp: {
                // =====================================================================
                //                s64 value1 = pop_long(stack);
                //                s64 value2 = pop_long(stack);
                //                s32 result = value2 == value1 ? 0 : (value2 > value1 ? 1 : -1);
                //                push_int(stack, result);
                // =====================================================================
                _gen_stack_peek_long(C, -2, SLJIT_R0, 0);
                _gen_stack_peek_long(C, -4, SLJIT_R1, 0);
//
//                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(S32,W,W), SLJIT_IMM, SLJIT_FUNC_ADDR(lcmp));
//                _gen_stack_set_int(C, -4, SLJIT_RETURN_REG, 0);
//                _gen_stack_size_modify(C, -3);

                sljit_emit_op2(C, SLJIT_XOR, SLJIT_R2, 0, SLJIT_R2, 0, SLJIT_R2, 0);
                sljit_emit_op2u(C, SLJIT_SUB | SLJIT_SET_SIG_GREATER, SLJIT_R0, 0, SLJIT_R1, 0);
                sljit_emit_select(C, SLJIT_SIG_GREATER, SLJIT_R2, SLJIT_IMM, -1, SLJIT_R2);
                sljit_emit_op2u(C, SLJIT_SUB | SLJIT_SET_SIG_LESS, SLJIT_R0, 0, SLJIT_R1, 0);
                sljit_emit_select(C, SLJIT_SIG_LESS, SLJIT_R2, SLJIT_IMM, 1, SLJIT_R2);
                sljit_emit_op2u(C, SLJIT_SUB | SLJIT_SET_Z, SLJIT_R0, 0, SLJIT_R1, 0);
                sljit_emit_select(C, SLJIT_EQUAL, SLJIT_R2, SLJIT_IMM, 0, SLJIT_R2);
                _gen_stack_set_int(C, -4, SLJIT_R2, 0);
                _gen_stack_size_modify(C, -3);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_fcmpl:
            case op_fcmpg: {
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                _gen_stack_peek_float(C, -2, SLJIT_FR1, 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_IMM, cur_inst);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, 32, F32, F32), SLJIT_IMM, SLJIT_FUNC_ADDR(fcmp));
                _gen_stack_set_int(C, -2, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -1);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_dcmpl:
            case op_dcmpg: {
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                _gen_stack_peek_double(C, -4, SLJIT_FR1, 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_IMM, cur_inst);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, 32, F64, F64), SLJIT_IMM, SLJIT_FUNC_ADDR(dcmp));
                _gen_stack_set_int(C, -4, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -3);
                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_ifeq: {
                _gen_icmp_op1(C, method, ip, code_idx, SLJIT_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_ifne: {
                _gen_icmp_op1(C, method, ip, code_idx, SLJIT_NOT_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_iflt: {
                _gen_icmp_op1(C, method, ip, code_idx, SLJIT_SIG_LESS);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_ifge: {
                _gen_icmp_op1(C, method, ip, code_idx, SLJIT_SIG_GREATER_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_ifgt: {
                _gen_icmp_op1(C, method, ip, code_idx, SLJIT_SIG_GREATER);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_ifle: {
                _gen_icmp_op1(C, method, ip, code_idx, SLJIT_SIG_LESS_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_icmpeq: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                _gen_stack_peek_int(C, -2, SLJIT_R1, 0);
                _gen_stack_size_modify(C, -2);
                _gen_cmp_reg2(C, method, ip, code_idx, SLJIT_R0, SLJIT_R1, SLJIT_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_icmpne: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                _gen_stack_peek_int(C, -2, SLJIT_R1, 0);
                _gen_stack_size_modify(C, -2);
                _gen_cmp_reg2(C, method, ip, code_idx, SLJIT_R0, SLJIT_R1, SLJIT_NOT_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_icmplt: {
                _gen_icmp_op2(C, method, ip, code_idx, SLJIT_SIG_LESS);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_icmpge: {
                _gen_icmp_op2(C, method, ip, code_idx, SLJIT_SIG_GREATER_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_icmpgt: {
                _gen_icmp_op2(C, method, ip, code_idx, SLJIT_SIG_GREATER);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_icmple: {
                _gen_icmp_op2(C, method, ip, code_idx, SLJIT_SIG_LESS_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_acmpeq: {
                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_stack_peek_ref(C, -2, SLJIT_R1, 0);
                _gen_stack_size_modify(C, -2);
                _gen_cmp_reg2(C, method, ip, code_idx, SLJIT_R0, SLJIT_R1, SLJIT_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_if_acmpne: {
                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_stack_peek_ref(C, -2, SLJIT_R1, 0);
                _gen_stack_size_modify(C, -2);
                _gen_cmp_reg2(C, method, ip, code_idx, SLJIT_R0, SLJIT_R1, SLJIT_NOT_EQUAL);
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_goto: {
                s32 offset = *((s16 *) (ip + 1));

                _gen_goto(C, method, code_idx, offset);
                ip += 3;
                break;
            }
            case op_jsr: {
                //s32 offset = *((s16 *) (ip + 1));
                //s32 jumpto = code_idx + offset;

                return JIT_GEN_ERROR;
                ip += 3;
                break;
            }

            case op_ret: {
                //__returnaddress addr = localvar_getRefer(runtime->localvar, (u8) ip[1]);

                return JIT_GEN_ERROR;

                //_gen_ip_modify_imm(C, 1);
                ip += 2;
                break;
            }


            case op_tableswitch: {
                s32 pos = 0;
                pos = (s32) (4 - ((((u64) (intptr_t) ip) - (u64) (intptr_t) (ca->bytecode_for_jit)) % 4));//4 byte对齐


                s32 default_offset = *((s32 *) (ip + pos));
                pos += 4;
                s32 low = *((s32 *) (ip + pos));
                pos += 4;
                s32 high = *((s32 *) (ip + pos));
                pos += 4;

                SwitchTable *st = switchtable_create(&ca->jit, high - low + 1);
                s32 i = low;
                for (; i <= high; i++) {
                    s32 offset = (*((s32 *) (ip + pos)));
                    st->table[i - low].bc_pos = code_idx + offset;
                    pos += 4;
                }
                // =====================================================================
                //                int val = pop_int(stack);// pop an int from the stack
                //                int offset = 0;
                //                if (val < low || val > high) {  // if its less than <low> or greater than <high>,
                //                    offset = default_offset;              // branch to default
                //                } else {                        // otherwise
                //                    pos += (val - low) * 4;
                //
                //                    offset = *((s32 *) (ip + pos));     // branch to entry in table
                //                }
                // =====================================================================
                sljit_emit_op2(C, SLJIT_XOR, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R0, 0);
                sljit_emit_op2(C, SLJIT_XOR, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_R1, 0);
                _gen_stack_pop_int(C, SLJIT_R0, 0);
                sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R1, 0, SLJIT_IMM, (sljit_s32) low);

                struct sljit_jump *jump_if_less_low, *jump_if_greater_high;
                struct sljit_label *label_out, *label_default;
                jump_if_less_low = sljit_emit_cmp(C, SLJIT_SIG_LESS, SLJIT_R0, 0, SLJIT_R1, 0);
                {
                    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R1, 0, SLJIT_IMM, (sljit_s32) high);
                    jump_if_greater_high = sljit_emit_cmp(C, SLJIT_SIG_GREATER, SLJIT_R0, 0, SLJIT_R1, 0);
                    {
                        sljit_emit_op2(C, SLJIT_SUB, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) low);
                        sljit_emit_op2(C, SLJIT_MUL, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) sizeof(struct V2PTable));
                        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) st->table);
                        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(struct V2PTable, bc_pos));
                        sljit_emit_op2(C, SLJIT_SUB, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_IMM, code_idx);
                        _gen_ip_modify_reg(C, SLJIT_R1, 0);
                        sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(struct V2PTable, jump_ptr));
                    }
                }
                label_default = sljit_emit_label(C);
                {
                    _gen_ip_modify_imm(C, default_offset);
                    struct sljit_jump *jump_away = sljit_emit_jump(C, SLJIT_JUMP);
                    pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + default_offset);
                }
                label_out = sljit_emit_label(C);
                //
                sljit_set_label(jump_if_less_low, label_default);
                sljit_set_label(jump_if_greater_high, label_default);


                ip += pos;

                break;
            }

            case op_lookupswitch: {
                s32 pos = 0;
                pos = (s32) (4 - ((((u64) (intptr_t) ip) - (u64) (intptr_t) (ca->bytecode_for_jit)) % 4));//4 byte对齐

                s32 default_offset = *((s32 *) (ip + pos));
                pos += 4;
                s32 n = *((s32 *) (ip + pos));
                pos += 4;
                s32 i, key;

                SwitchTable *st = switchtable_create(&ca->jit, n);
                for (i = 0; i < n; i++) {

                    st->table[i].value = *((s32 *) (ip + pos));
                    pos += 4;
                    st->table[i].bc_pos = code_idx + (*((s32 *) (ip + pos)));
                    pos += 4;
                }

                // =====================================================================
                //       int val = pop_int(stack);// pop an int from the stack
                //       int offset = default_offset;
                //       for (i = 0; i < n; i++) {
                //
                //           key = *((s32 *) (ip + pos));
                //           pos += 4;
                //           if (key == val) {
                //               offset = *((s32 *) (ip + pos));
                //               break;
                //           } else {
                //               pos += 4;
                //           }
                //       }
                // =====================================================================

                sljit_emit_op2(C, SLJIT_XOR, SLJIT_R2, 0, SLJIT_R2, 0, SLJIT_R2, 0);
                _gen_stack_pop_int(C, SLJIT_R2, 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R1, 0, SLJIT_IMM, (sljit_sw) st->table);
                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R1, 0, SLJIT_IMM, (sljit_sw)
                                                                                          sizeof(struct V2PTable) * n);

                struct sljit_jump *jump_to_loop, *jump_to_not_equal;
                struct sljit_label *label_not_equal, *label_end_loop;
                //for
                struct sljit_label *lable_loop = sljit_emit_label(C);
                //if equal
                struct sljit_jump *jump_to_end_loop = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_R0, 0);
                //body
                {
                    jump_to_not_equal = sljit_emit_cmp(C, SLJIT_NOT_EQUAL | SLJIT_32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(struct V2PTable, value));
                    {//found left
                        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(struct V2PTable, bc_pos));
                        sljit_emit_op2(C, SLJIT_SUB, SLJIT_R2, 0, SLJIT_R2, 0, SLJIT_IMM, code_idx);
                        _gen_ip_modify_reg(C, SLJIT_R2, 0);
                        sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(struct V2PTable, jump_ptr));
                    }
                    label_not_equal = sljit_emit_label(C);
                    //ptr++
                    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R1, 0, SLJIT_R1, 0, SLJIT_IMM, sizeof(struct V2PTable));
                    //
                    jump_to_loop = sljit_emit_jump(C, SLJIT_JUMP);
                }
                label_end_loop = sljit_emit_label(C);
                //jump to default
                {
                    _gen_ip_modify_imm(C, default_offset);
                    struct sljit_jump *jump_away = sljit_emit_jump(C, SLJIT_JUMP);
                    pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + default_offset);
                }
                //
                sljit_set_label(jump_to_not_equal, label_not_equal);
                sljit_set_label(jump_to_loop, lable_loop);
                sljit_set_label(jump_to_end_loop, label_end_loop);

                ip += pos;
                break;
            }

            case op_lreturn:
            case op_dreturn:
            case op_ireturn:
            case op_freturn:
            case op_areturn:
            case op_return: {
                _gen_save_sp_ip(C);
                sljit_emit_return(C, SLJIT_MOV, SLJIT_IMM, RUNTIME_STATUS_NORMAL);
                ip++;
                break;
            }

            case op_getstatic: {
                u16 idx = *((u16 *) (ip + 1));
                FieldInfo *fi = class_get_constant_fieldref(clazz, idx)->fieldInfo;

                if (!fi) {
                    ConstantFieldRef *cfr = class_get_constant_fieldref(clazz, idx);
                    fi = find_fieldInfo_by_fieldref(clazz, cfr->item.index, runtime);
                    cfr->fieldInfo = fi;
                    if (!fi) {
                        return JIT_GEN_ERROR;
                    }
                }
                if (fi->_this_class->status < CLASS_STATUS_CLINITED) {
                    class_clinit(fi->_this_class, runtime);
                }

                c8 *ptr = getStaticFieldPtr(fi);
                if (fi->isrefer) {
                    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                    _gen_stack_push_ref(C, SLJIT_R0, 0);
                } else {
                    // check variable type to determine s64/s32/f64/f32
                    s32 data_bytes = fi->datatype_bytes;
                    switch (data_bytes) {
                        case 4: {
                            //sp->rvalue = *((s32 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            _gen_stack_push_int(C, SLJIT_R0, 0);
                            break;
                        }
                        case 1: {
                            //sp->rvalue = *((s8 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            _gen_stack_push_int(C, SLJIT_R0, 0);

                            break;
                        }
                        case 8: {
                            //sp->rvalue = *((s64 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            _gen_stack_push_long(C, SLJIT_R0, 0);
                            break;
                        }
                        case 2: {
                            if (fi->datatype_idx == DATATYPE_JCHAR) {
                                //sp->rvalue = *((u16 *)ptr)
                                sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                                _gen_stack_push_int(C, SLJIT_R0, 0);

                            } else {
                                //sp->rvalue = *((s16 *)ptr)
                                sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                                _gen_stack_push_int(C, SLJIT_R0, 0);
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }

                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_putstatic: {
                u16 idx = *((u16 *) (ip + 1));
                FieldInfo *fi = class_get_constant_fieldref(clazz, idx)->fieldInfo;
                if (!fi) {
                    ConstantFieldRef *cfr = class_get_constant_fieldref(clazz, idx);
                    fi = find_fieldInfo_by_fieldref(clazz, cfr->item.index, runtime);
                    cfr->fieldInfo = fi;
                    if (!fi) {
                        return JIT_GEN_ERROR;
                    }
                }
                if (fi->_this_class->status < CLASS_STATUS_CLINITED) {
                    class_clinit(fi->_this_class, runtime);
                }
                c8 *ptr = getStaticFieldPtr(fi);

                if (fi->isrefer) {
                    _gen_stack_pop_ref(C, SLJIT_R0, 0);
                    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM0(), (sljit_sw) ptr, SLJIT_R0, 0);
                } else {
                    // check variable type to determine s64/s32/f64/f32
                    s32 data_bytes = fi->datatype_bytes;
                    switch (data_bytes) {
                        case 4: {
                            _gen_stack_pop_int(C, SLJIT_R0, 0);
                            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM0(), (sljit_sw) ptr, SLJIT_R0, 0);
                            break;
                        }
                        case 1: {
                            _gen_stack_pop_int(C, SLJIT_R0, 0);
                            sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_MEM0(), (sljit_sw) ptr, SLJIT_R0, 0);
                            break;
                        }
                        case 8: {
                            _gen_stack_pop_long(C, SLJIT_R0, 0);
                            sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM0(), (sljit_sw) ptr, SLJIT_R0, 0);
                            break;
                        }
                        case 2: {
                            _gen_stack_pop_int(C, SLJIT_R0, 0);
                            sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_MEM0(), (sljit_sw) ptr, SLJIT_R0, 0);
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
                //ip
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_getfield: {
                u16 idx = *((u16 *) (ip + 1));
                FieldInfo *fi = class_get_constant_fieldref(clazz, idx)->fieldInfo;
                if (!fi) {
                    ConstantFieldRef *cfr = class_get_constant_fieldref(clazz, idx);
                    fi = find_fieldInfo_by_fieldref(clazz, cfr->item.index, runtime);
                    cfr->fieldInfo = fi;
                    if (!fi) {
                        return JIT_GEN_ERROR;
                    }
                }
                if (fi->_this_class->status < CLASS_STATUS_CLINITED) {
                    class_clinit(fi->_this_class, runtime);
                }

                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, RUNTIME_STATUS_EXCEPTION, JVM_EXCEPTION_NULLPOINTER, -1);

                //&(ins->obj_fields[fi->offset_instance]);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(Instance, obj_fields));
                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R2, 0, SLJIT_IMM, fi->offset_instance);


                if (fi->isrefer) {
                    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), 0);
                    _gen_stack_set_ref(C, -1, SLJIT_R0, 0);
                } else {
                    // check variable type to determine s64/s32/f64/f32
                    s32 data_bytes = fi->datatype_bytes;
                    switch (data_bytes) {
                        case 4: {
                            //sp->rvalue = *((s32 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            _gen_stack_set_int(C, -1, SLJIT_R0, 0);
                            break;
                        }
                        case 1: {
                            //sp->rvalue = *((s8 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            _gen_stack_set_int(C, -1, SLJIT_R0, 0);

                            break;
                        }
                        case 8: {
                            //sp->rvalue = *((s64 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            _gen_stack_set_long(C, -1, SLJIT_R0, 0);
                            _gen_stack_size_modify(C, 1);
                            break;
                        }
                        case 2: {
                            if (fi->datatype_idx == DATATYPE_JCHAR) {
                                //sp->rvalue = *((u16 *)ptr)
                                sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), 0);
                                _gen_stack_set_int(C, -1, SLJIT_R0, 0);

                            } else {
                                //sp->rvalue = *((s16 *)ptr)
                                sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), 0);
                                _gen_stack_set_int(C, -1, SLJIT_R0, 0);
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
                //ip
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }
            case op_putfield: {
                u16 idx = *((u16 *) (ip + 1));
                FieldInfo *fi = class_get_constant_fieldref(clazz, idx)->fieldInfo;
                if (!fi) {
                    ConstantFieldRef *cfr = class_get_constant_fieldref(clazz, idx);
                    fi = find_fieldInfo_by_fieldref(clazz, cfr->item.index, runtime);
                    cfr->fieldInfo = fi;
                    if (!fi) {
                        return JIT_GEN_ERROR;
                    }
                }
                if (fi->_this_class->status < CLASS_STATUS_CLINITED) {
                    class_clinit(fi->_this_class, runtime);
                }
                s32 stack_size;

                if (fi->isrefer) {
                    _gen_stack_peek_ref(C, -1, SLJIT_R1, 0);
                    stack_size = 2;
                } else {
                    // check variable type to determine s64/s32/f64/f32
                    s32 data_bytes = fi->datatype_bytes;
                    switch (data_bytes) {
                        case 1:
                        case 2:
                        case 4: {
                            _gen_stack_peek_int(C, -1, SLJIT_R1, 0);
                            stack_size = 2;
                            break;
                        }
                        case 8: {
                            _gen_stack_peek_long(C, -2, SLJIT_R1, 0);
                            stack_size = 3;
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }

                _gen_stack_peek_ref(C, -stack_size, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, RUNTIME_STATUS_EXCEPTION, JVM_EXCEPTION_NULLPOINTER, -stack_size);


                //&(ins->obj_fields[fi->offset_instance]);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(Instance, obj_fields));
                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R2, 0, SLJIT_IMM, fi->offset_instance);

                if (fi->isrefer) {
                    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), 0, SLJIT_R1, 0);
                    _gen_stack_size_modify(C, -2);
                } else {
                    // check variable type to determine s64/s32/f64/f32
                    s32 data_bytes = fi->datatype_bytes;
                    switch (data_bytes) {
                        case 4: {
                            //sp->rvalue = *((s32 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(SLJIT_R2), 0, SLJIT_R1, 0);
                            _gen_stack_size_modify(C, -2);
                            break;
                        }
                        case 1: {
                            //sp->rvalue = *((s8 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_MEM1(SLJIT_R2), 0, SLJIT_R1, 0);
                            _gen_stack_size_modify(C, -2);
                            break;
                        }
                        case 8: {
                            //sp->rvalue = *((s64 *)ptr)
                            sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), 0, SLJIT_R1, 0);
                            _gen_stack_size_modify(C, -3);
                            break;
                        }
                        case 2: {
                            sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_MEM1(SLJIT_R2), 0, SLJIT_R1, 0);
                            _gen_stack_size_modify(C, -2);
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }

                //ip
                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }

            case op_invokevirtual:
            case op_invokeinterface: {
                _gen_save_sp_ip(C);

                //此cmr所描述的方法，对于不同的实例，有不同的method
                //s32 _gen_invokevirtual(Runtime *runtime, u16 idx)
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R1, 0, SLJIT_IMM, *((u16 *) (ip + 1)));
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, 32), SLJIT_IMM, SLJIT_FUNC_ADDR(invokevirtual));
                _gen_load_sp_ip(C);
                _gen_exception_check_throw_handle(C, SLJIT_NOT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, RUNTIME_STATUS_NORMAL, -1, 0);

                if (cur_inst == op_invokevirtual) {
                    _gen_ip_modify_imm(C, 3);
                    ip += 3;
                } else {
                    _gen_ip_modify_imm(C, 5);
                    ip += 5;
                }
                break;
            }


            case op_invokespecial:
            case op_invokestatic: {
                _gen_save_sp_ip(C);

                ConstantMethodRef *cmr = class_get_constant_method_ref(clazz, *((u16 *) (ip + 1)));
                MethodInfo *m = cmr->methodInfo;

                //R0 = method
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) m);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(execute_method_impl));
                _gen_load_sp_ip(C);
                _gen_exception_check_throw_handle(C, SLJIT_NOT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, RUNTIME_STATUS_NORMAL, -1, 0);

                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }

            case op_invokedynamic: {
                _gen_save_sp_ip(C);

                s32 idx = *((u16 *) (ip + 1));

                ConstantInvokeDynamic *cid = class_get_invoke_dynamic(clazz, idx);
                BootstrapMethod *bootMethod = &clazz->bootstrapMethodAttr->bootstrap_methods[cid->bootstrap_method_attr_index];//Boot

                if (bootMethod->make == NULL) {
                    s32 ret = invokedynamic_prepare(runtime, bootMethod, cid);
                    if (ret) {
                        return JIT_GEN_ERROR;
                    }
                }
                MethodInfo *m = bootMethod->make;
                //R0 = method
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) m);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(execute_method_impl));
                _gen_load_sp_ip(C);
                _gen_exception_check_throw_handle(C, SLJIT_NOT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, RUNTIME_STATUS_NORMAL, -1, 0);

                _gen_ip_modify_imm(C, 5);
                ip += 5;
                break;
            }


            case op_new: {


                s32 idx = *((u16 *) (ip + 1));

                ConstantClassRef *ccf = class_get_constant_classref(clazz, idx);
                if (!ccf->clazz) {
                    Utf8String *clsName = class_get_utf8_string(clazz, ccf->stringIndex);
                    ccf->clazz = classes_load_get(clazz->jloader, clsName, runtime);
                }
                JClass *other = ccf->clazz;
                Instance *ins = NULL;
                if (other) {
                    // =====================================================================
                    //                    ins = instance_create(runtime, other);
                    //                    push_ref(stack, (__refer) ins);
                    // =====================================================================
                    _gen_save_sp_ip(C);
                    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_IMM, (sljit_sw) other);
                    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(P, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(instance_create));
                    _gen_load_sp_ip(C);
                    _gen_stack_push_ref(C, SLJIT_RETURN_REG, 0);
                } else {
                    return JIT_GEN_ERROR;
                }

                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }


            case op_newarray: {
                _gen_save_sp_ip(C);

                s32 typeIdx = ip[1];
                // =====================================================================
                //                s32 count = pop_int(stack);
                //                Instance *arr = jarray_create_by_type_index(runtime, count, typeIdx);
                // =====================================================================
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                _gen_stack_peek_int(C, -1, SLJIT_R1, 0);
                sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_IMM, typeIdx);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(P, P, 32, 32), SLJIT_IMM, SLJIT_FUNC_ADDR(jarray_create_by_type_index));
                _gen_load_sp_ip(C);
                _gen_stack_set_ref(C, -1, SLJIT_RETURN_REG, 0);

                _gen_ip_modify_imm(C, 2);
                ip += 2;
                break;
            }

            case op_anewarray: {
                _gen_save_sp_ip(C);

                s32 idx = *((u16 *) (ip + 1));
                JClass *arr_class = pairlist_get(clazz->arr_class_type, (__refer) (intptr_t) idx);

                if (!arr_class) {//cache to speed
                    arr_class = array_class_get_by_name(runtime, runtime->clazz->jloader, class_get_utf8_string(clazz, idx));
                    spin_lock(&runtime->jvm->lock_cloader);
                    {
                        pairlist_put(clazz->arr_class_type, (__refer) (intptr_t) idx, arr_class);
                    }
                    spin_unlock(&runtime->jvm->lock_cloader);
                }

                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                _gen_stack_peek_int(C, -1, SLJIT_R1, 0);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_IMM, (sljit_sw) arr_class);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(P, P, 32, P), SLJIT_IMM, SLJIT_FUNC_ADDR(jarray_create_by_class));
                _gen_load_sp_ip(C);
                _gen_stack_set_ref(C, -1, SLJIT_RETURN_REG, 0);

                _gen_ip_modify_imm(C, 3);
                ip += 3;

                break;
            }

            case op_arraylength: {
                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, -1);

                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_stack_set_int(C, -1, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(Instance, arr_length));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }


            case op_athrow: {

                _gen_exception_handle(C);

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }

            case op_checkcast: {
                _gen_save_sp_ip(C);

                s32 typeIdx = *((u16 *) (ip + 1));
                // =====================================================================
                //                Instance *ins = (Instance *) pop_ref(stack);
                //                if (!checkcast(runtime, ins, typeIdx)) {
                //                    _checkcast_throw_exception(stack, runtime);
                //                    ret = RUNTIME_STATUS_EXCEPTION;
                //                    goto label_exception_handle;
                //                } else {
                //                    push_ref(stack, (__refer) ins);
                //                    ip += 3;
                //                }
                // =====================================================================
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                _gen_stack_peek_ref(C, -1, SLJIT_R1, 0);
                sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_IMM, typeIdx);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, P, 32), SLJIT_IMM, SLJIT_FUNC_ADDR(checkcast));
                _gen_load_sp_ip(C);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, 0, JVM_EXCEPTION_CLASSCAST, -1);

                ip += 3;
                break;
            }


            case op_instanceof: {
                _gen_save_sp_ip(C);

                s32 typeIdx = *((u16 *) (ip + 1));
                JClass *other = getClassByConstantClassRef(clazz, typeIdx, runtime);

                // =====================================================================
                //                Instance *ins = (Instance *) pop_ref(stack);
                //                s32 checkok = 0;
                //                if (!ins) {
                //                } else if (ins->mb.type & (MEM_TYPE_INS | MEM_TYPE_ARR)) {
                //                    if (instance_of(getClassByConstantClassRef(clazz, typeIdx, runtime), ins, runtime)) {
                //                        checkok = 1;
                //                    }
                //                }
                //                push_int(stack, checkok);
                // =====================================================================
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) other);
                _gen_stack_peek_ref(C, -1, SLJIT_R1, 0);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(instanceof));
                _gen_load_sp_ip(C);
                _gen_stack_set_int(C, -1, SLJIT_RETURN_REG, 0);

                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }

            case op_monitorenter: {

                // =====================================================================
                //                Instance *ins = (Instance *) pop_ref(stack);
                //                jthread_lock(&ins->mb, runtime);
                // =====================================================================

                _gen_stack_pop_ref(C, SLJIT_R0, 0);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(jthread_lock));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }
            case op_monitorexit: {
                // =====================================================================
                //                Instance *ins = (Instance *) pop_ref(stack);
                //                jthread_unlock(&ins->mb, runtime);
                // =====================================================================

                _gen_stack_pop_ref(C, SLJIT_R0, 0);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(jthread_unlock));

                _gen_ip_modify_imm(C, 1);
                ip++;
                break;
            }

            case op_wide: {
                _gen_ip_modify_imm(C, 1);
                ip++;

                cur_inst = *ip;
                switch (cur_inst) {
                    case op_iload:
                    case op_fload: {
                        _gen_i_f_load(C, *((u16 *) (ip + 1)));

                        _gen_ip_modify_imm(C, 3);
                        ip += 3;
                        break;
                    }
                    case op_aload: {
                        _gen_a_load(C, *((u16 *) (ip + 1)));

                        _gen_ip_modify_imm(C, 3);
                        ip += 3;
                        break;
                    }
                    case op_lload:
                    case op_dload: {
                        _gen_l_d_load(C, *((u16 *) (ip + 1)));

                        _gen_ip_modify_imm(C, 3);
                        ip += 3;
                        break;
                    }
                    case op_istore:
                    case op_fstore: {
                        _gen_i_f_store(C, *((u16 *) (ip + 1)));

                        _gen_ip_modify_imm(C, 3);
                        ip += 3;
                        break;
                    }
                    case op_astore: {
                        _gen_a_store(C, *((u16 *) (ip + 1)));

                        _gen_ip_modify_imm(C, 3);
                        ip += 3;
                        break;
                    }
                    case op_lstore:
                    case op_dstore: {
                        _gen_l_d_store(C, *((u16 *) (ip + 1)));

                        _gen_ip_modify_imm(C, 3);
                        ip += 3;
                        break;
                    }
                    case op_ret: {
                        //__refer addr = localvar_getRefer(runtime->localvar, *((u16 *) (ip + 1)));
                        // ip = (u8 *) addr;

                        _gen_local_get_ref(C, *((u16 *) (ip + 1)), SLJIT_R0, 0);
                        sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_R0, 0);

                        return JIT_GEN_ERROR;

                        _gen_ip_modify_imm(C, 3);
                        ip += 3;
                        break;
                    }
                    case op_iinc    : {
                        s32 idx = *((u16 *) (ip + 1));
                        s32 v = *((s16 *) (ip + 3));
                        //runtime->localvar[*((u16 *) (ip + 1))].ivalue += *((s16 *) (ip + 3));
                        _gen_local_get_int(C, idx, SLJIT_R0, 0);
                        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, v);
                        _gen_local_set_int(C, idx, SLJIT_R0, 0);

                        _gen_ip_modify_imm(C, 5);
                        ip += 5;
                        break;
                    }
                    default:
                        jvm_printf("instruct wide %x not found\n", cur_inst);
                }
                break;
            }

            case op_multianewarray: {
                //data type index
                Utf8String *desc = class_get_utf8_string(clazz, *((u16 *) (ip + 1)));
                //array dim
                s32 count = (u8) ip[3];

//                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK);
//                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R1, 0, SLJIT_R0, 0, SLJIT_IMM, SLJIT_OFFSETOF(RuntimeStack, multi_arr_dim));
//                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R1, 0, SLJIT_IMM, sizeof(s32) * count);
//
//                struct sljit_jump *jump_to_loop, *jump_to_end_loop;
//                struct sljit_label *lable_loop, *label_end_loop;
//                //for loop
//                lable_loop = sljit_emit_label(C);
//                {//loop body
//                    //if equal then break loop
//                    jump_to_end_loop = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_R2, 0);
//
//                    //multi_arr_dim[i]=pop_int()
//                    _gen_stack_pop_int(C, SLJIT_MEM1(SLJIT_R1), 0);
//                    //ptr++
//                    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R1, 0, SLJIT_R0, 0, SLJIT_IMM, sizeof(s32));
//                    //
//                    jump_to_loop = sljit_emit_jump(C, SLJIT_JUMP);
//                }
//                label_end_loop = sljit_emit_label(C);
//                //
//                sljit_set_label(jump_to_loop, lable_loop);
//                sljit_set_label(jump_to_end_loop, label_end_loop);
//
//
//                _gen_stack_pop_ref(C, SLJIT_R0, 0);
//                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
//                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2V(W,W), SLJIT_IMM, SLJIT_FUNC_ADDR(jarray_multi_create));

                _gen_save_sp_ip(C);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_IMM, (sljit_sw) desc);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_IMM, count);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, P, 32), SLJIT_IMM, SLJIT_FUNC_ADDR(multiarray));
                _gen_load_sp_ip(C);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0, SLJIT_IMM, RUNTIME_STATUS_EXCEPTION, JVM_EXCEPTION_NULLPOINTER, 0);

                _gen_ip_modify_imm(C, 4);
                ip += 4;
                break;
            }


            case op_ifnull:
            case op_ifnonnull: {
                // =====================================================================
                //                __refer ref = pop_ref(stack);
                //                if (!ref) {
                //
                //                    ip += *((s16 *) (ip + 1));
                //                } else {
                //                    ip += 3;
                //                }
                // =====================================================================

                s32 offset = *((s16 *) (ip + 1));
                _gen_stack_pop_ref(C, SLJIT_R0, 0);

                struct sljit_jump *jump_if_true, *jump_out, *jump_away;
                struct sljit_label *label_out, *label_true;
                //if R0 == 0 then jump to equ_0
                jump_if_true = sljit_emit_cmp(C, cur_inst == op_ifnull ? SLJIT_EQUAL : SLJIT_NOT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0);
                {
                    jump_out = sljit_emit_jump(C, SLJIT_JUMP);
                }
                label_true = sljit_emit_label(C);
                {
                    _gen_jump_to_suspend_check(C, offset);
                    _gen_ip_modify_imm(C, offset);
                    jump_away = sljit_emit_jump(C, SLJIT_JUMP);
                    pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
                }
                label_out = sljit_emit_label(C);
                //
                sljit_set_label(jump_if_true, label_true);
                sljit_set_label(jump_out, label_out);

                _gen_ip_modify_imm(C, 3);
                ip += 3;
                break;
            }

            case op_breakpoint: {

                _gen_ip_modify_imm(C, 1);
                ip += 1;
                break;
            }


            case op_goto_w: {
                s32 offset = *((s32 *) (ip + 1));
                _gen_ip_modify_imm(C, code_idx + offset);
                _gen_goto(C, method, code_idx, offset);

                _gen_ip_modify_imm(C, 5);
                ip += 5;
                break;
            }

            case op_jsr_w: {
                // =====================================================================
                //                s32 branchoffset = *((s32 *) (ip + 1));
                //                push_ra(stack, (__refer) (ip + 3));
                // =====================================================================
                return JIT_GEN_ERROR;
                ip += 5;
                break;
            }
            default: {
                jvm_printf("jit instruct %x not found\n", cur_inst);
            }
        }
        //garbage stop world detect
        //sljit_emit_ijump(C, SLJIT_FAST_CALL, SLJIT_IMM, SLJIT_FUNC_ADDR(check_suspend));

#if JIT_DEBUG
        _gen_save_sp_ip(C);
        _debug_gen_print_stack(C);
        sljit_emit_op0(C, SLJIT_NOP);
#endif
    }//end while

    //interrupt detected,then return
    struct sljit_label *label_interrupt_handle = sljit_emit_label(C);
    {
        _gen_save_sp_ip(C);
        sljit_emit_op1(C, SLJIT_MOV, SLJIT_RETURN_REG, 0, SLJIT_IMM, RUNTIME_STATUS_INTERRUPT);
        sljit_emit_return(C, SLJIT_MOV, SLJIT_RETURN_REG, 0);
    }
    //

    //process jump to label
    for (i = 0; i < method->jump_2_pos->count; i++) {
        Pair p = pairlist_get_pair(method->jump_2_pos, i);
        struct sljit_jump *jump = (__refer) (intptr_t) p.leftl;
        struct sljit_label *label = (__refer) (intptr_t) pairlist_getl(method->pos_2_label, p.rightl);
        if (!label) {
            jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), (s32) (intptr_t) p.rightl);
        } else {
            sljit_set_label(jump, label);
        }
    }

    //Generate machine code
    genfunc = sljit_generate_code(C);
    if (sljit_get_compiler_error(C) != SLJIT_ERR_COMPILED) {
        return JIT_GEN_ERROR;
    }

    //save interrupt jump address
    ca->jit.interrupt_handle_jump_ptr = (__refer) sljit_get_label_addr(label_interrupt_handle);

    //process switch table jump
    SwitchTable *st1 = ca->jit.switchtable;
    while (st1) {
        struct V2PTable *v2p = st1->table;
        s32 i, imax;
        for (i = 0, imax = st1->size; i < imax; i++) {
            s32 pos = v2p[i].bc_pos;
            struct sljit_label *label = (__refer) (intptr_t) pairlist_getl(method->pos_2_label, pos);
            if (!label) {
                jvm_printf("switch label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), (s32) (intptr_t) pos);
            } else {
                v2p[i].jump_ptr = (__refer) sljit_get_label_addr(label);
            }
        }
        st1 = st1->next;
    }

    //process exception jump ptr
    {
        ExceptionTable *e = ca->exception_table;
        for (i = 0; i < ca->exception_table_length; i++) {
            s32 pos = (e + i)->handler_pc;
            struct sljit_label *label = (__refer) (intptr_t) pairlist_getl(method->pos_2_label, pos);
            if (!label) {
                jvm_printf("exception label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), (s32) (intptr_t) pos);
            } else {
                ca->jit.ex_jump_table[i].bc_pos = pos;
                ca->jit.ex_jump_table[i].exception_handle_jump_ptr = (__refer) sljit_get_label_addr(label);
            }
        }
    }

    ca->jit.len = (s32) sljit_get_generated_code_size(C);

    //Execute code
    ca->jit.func = (jit_func) genfunc;
#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("jit compile method %s.%s%s ,func length:%d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), utf8_cstr(method->descriptor), ca->jit.len);
#endif

    return JIT_GEN_SUCCESS;
}


void construct_jit(MethodInfo *method, Runtime *runtime) {

//    printf(" %s reg %d, %d\n", sljit_get_platform_name(), SLJIT_NUMBER_OF_SCRATCH_REGISTERS, SLJIT_NUMBER_OF_SAVED_REGISTERS);
//    printf("address offset :%llx\n", (s64) (intptr_t) SLJIT_OFFSETOF(Instance, obj_fields));
//    printf("size of sljit_sw :%d\n", (s32) sizeof(sljit_sw));
    CodeAttribute *ca = method->converted_code;

    if (!check_suspend) {
        gen_jit_suspend_check_func();
    }
    if (runtime->jvm->jdwp_enable) {
        ca->jit.state = JIT_GEN_ERROR;
        return;
    }
    /* Create a SLJIT compiler */
    struct sljit_compiler *C = sljit_create_compiler(NULL, NULL);
    ca->jit.state = JIT_GEN_COMPILING;
    ca->jit.state = gen_jit_bytecode_func(C, method, runtime);

    if (ca->jit.state == JIT_GEN_SUCCESS) {
        int debug = 1;
    }
#if(JIT_CODE_DUMP)
    if (utf8_equals_c(runtime->method->_this_class->name, "org/mini/json/JsonParser")
        && utf8_equals_c(runtime->method->name, "<init>")) {
        if (ca->jit.state == JIT_GEN_SUCCESS)dump_code(ca->jit.func, ca->jit.len);
    }
#endif
    sljit_free_compiler(C);
}

SwitchTable *switchtable_create(Jit *jit, s32 size) {
    SwitchTable *st = jvm_calloc(sizeof(SwitchTable));
    st->size = size;
    st->next = jit->switchtable;
    jit->switchtable = st;
    st->table = jvm_calloc(sizeof(struct V2PTable) * size);
    return st;
}

void jit_init(CodeAttribute *ca) {
    Jit *jit = &ca->jit;
    s32 count = ca->exception_table_length;
    if (count) {
        jit->ex_jump_table = jvm_calloc(sizeof(struct _ExceptionJumpTable) * count);
    }
}

void jit_destory(Jit *jit) {

    while (jit->switchtable) {
        SwitchTable *tmp = jit->switchtable->next;
        jvm_free(jit->switchtable);
        jit->switchtable = tmp;
    }

    if (jit->ex_jump_table) {
        jvm_free(jit->ex_jump_table);
        jit->ex_jump_table = NULL;
    }

    if (jit->func) {
        sljit_free_code(jit->func, NULL);
    }
}

void jit_set_exception_jump_addr(Runtime *runtime, CodeAttribute *ca, s32 index) {
    if (ca->jit.ex_jump_table) {
        runtime->jit_exception_bc_pos = ca->jit.ex_jump_table[index].bc_pos;
        runtime->jit_exception_jump_ptr = ca->jit.ex_jump_table[index].exception_handle_jump_ptr;
    }
}

#else

void jit_init(CodeAttribute *ca) {
}

void jit_destory(Jit *jit) {
}

void jit_set_exception_jump_addr(Runtime *runtime, CodeAttribute *ca, s32 index) {
}

void construct_jit(MethodInfo *method, Runtime *runtime) {
}

#endif