//
// Created by gust on 10/30/19.
//


#include "jit.h"

#if JIT_ENABLE
//#pragma message("jit compiled")
#include "sljitLir.h"

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include "garbage.h"

//------------------------  note ----------------------------

//   This jit implementation is dependend on SLJIT (github https://github.com/zherczeg/sljit)

//-----------------------------------------------------------

#define REGISTER_SP SLJIT_S0
#define REGISTER_LOCALVAR SLJIT_S1
#define REGISTER_HOT_LOCAL0 SLJIT_S2
#define REGISTER_HOT_LOCAL1 SLJIT_S3

#define JIT_SCRATCH_REGS 6
#define JIT_SAVED_REGS 4

/* ---- dual top-of-stack register cache ----
 * One cached VALUE: the top 1..2 numeric values of the VM operand stack
 * live in registers (R3/R4 for int/long, FR3/FR4 for float/double)
 * instead of their stack slots. The logical SP is REGISTER_SP plus
 * sp_pending slots; stack accesses compensate for this deferred delta. */
typedef struct {
    u8 datatype; /* DATATYPE_INT / LONG / FLOAT / DOUBLE */
    u8 slots; /* StackEntry slots covered: 1 or 2 */
    u8 is_imm; /* int/long compile-time constant, not yet in register */
    sljit_sw imm; /* sign extended value while is_imm */
    sljit_s32 reg; /* R3/R4 (int,long) or FR3/FR4 (float,double) holding the value */
} TosValue;

typedef struct {
    s32 count; /* cached values, 0..2; v[0] deeper, v[1] = stack top */
    TosValue v[2];
} TosCache;

typedef struct {
    const u8 *jit_code;
    const u8 *runtime_code;
    const u8 *current_ip;
    s32 hot_local[2];
    TosCache tos;
    s32 sp_pending; /* stack slots REGISTER_SP lags behind the logical
                         depth: pushes/pops only bump this, one folded
                         add is emitted by _gen_sp_apply() before the
                         register is read, published or jumps */
} JitGenContext;

#if defined(_MSC_VER)
#define JIT_THREAD_LOCAL __declspec(thread)
#elif defined(__GNUC__) || defined(__clang__)
#define JIT_THREAD_LOCAL __thread
#else
#define JIT_THREAD_LOCAL _Thread_local
#endif

/*
 * Compilation can be nested (class initialization performed while resolving a
 * constant may compile another method), and different VM threads may compile
 * concurrently.  Keep generation-only state in TLS and restore it in
 * construct_jit().
 */
static JIT_THREAD_LOCAL JitGenContext *jit_gen_context;

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

void _gen_invoke_status_dispatch(struct sljit_compiler *C);

SwitchTable *switchtable_create(Jit *jit, s32 size);

s32 gen_jit_bytecode_func(struct sljit_compiler *C, MethodInfo *method, Runtime *runtime);

void _gen_jump_to_suspend_check(struct sljit_compiler *C, const u8 *branch_ip, s32 offset);

void _gen_save_sp_ip(struct sljit_compiler *C);

static void _gen_flush_hot_locals(struct sljit_compiler *C);

//------------------------  jit util ----------------------------

static void FAILE(s32 cond, c8 *text) {
    if (cond) {
        printf("compile error: %s\n", text);
    }
}

static s32 CHECK(struct sljit_compiler *compiler) {
    if (sljit_get_compiler_error(compiler) != SLJIT_ERR_COMPILED) {
        printf("Compiler error: %d\n", sljit_get_compiler_error(compiler));
        return -1; // return err status but not release
    }
    return 0; // sc
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
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Runtime, pc));
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
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS1V(P), SLJIT_IMM, SLJIT_FUNC_ADDR(print_runtime_stack));

    //restore r0,r1,r2
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM0(), (sljit_sw) &a);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R1, 0, SLJIT_MEM0(), (sljit_sw) &b);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_R2, 0, SLJIT_MEM0(), (sljit_sw) &c);
}

static void dump_code(void *code, sljit_uw len) {
    FILE *fp = fopen("/tmp/slj_dump", "wb");
    if (!fp)
        return;

    size_t written = fwrite(code, len, 1, fp);
    if (written != 1) {
        printf("Warning: Failed to write complete dump data\n");
    }
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

static void _gen_tos_flush(struct sljit_compiler *C);

static void _gen_tos_flush_values(struct sljit_compiler *C);

static void _gen_sp_apply(struct sljit_compiler *C);

static sljit_s32 _tos_is_float(u8 datatype);

static s32 _tos_slots(u8 datatype);

static s32 _gen_tos_reserve(struct sljit_compiler *C, u8 datatype);

static void _gen_tos_materialize(struct sljit_compiler *C, s32 idx);

static const u8 *_jit_runtime_pc(const u8 *jit_ip) {
    JitGenContext *ctx = jit_gen_context;
    if (!ctx || !jit_ip) {
        return NULL;
    }
    return ctx->runtime_code + (jit_ip - ctx->jit_code);
}

static void _gen_save_sp_pc_at(struct sljit_compiler *C, const u8 *jit_ip) {
    /* Publish a fully materialized frame. Conditional callers must
     * normalize SP/cache state before splitting their runtime paths. */
    _gen_tos_flush_values(C);
    _gen_sp_apply(C);

    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0, SLJIT_R0, 0);
    _gen_flush_hot_locals(C);

    // Publish the VM stack before a callout or safepoint so GC sees every root.
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R0), 0, REGISTER_SP, 0);

    // PC is a compile-time constant.  Do not maintain it after every bytecode.
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME_PC);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R0), 0, SLJIT_IMM, (sljit_sw) _jit_runtime_pc(jit_ip));

    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0);
}

void _gen_save_sp_ip(struct sljit_compiler *C) {
    _gen_save_sp_pc_at(C, jit_gen_context ? jit_gen_context->current_ip : NULL);
}

void _gen_load_sp_ip(struct sljit_compiler *C) {
    /* The runtime supplies the complete logical SP, not the old base. */
    if (jit_gen_context) {
        jit_gen_context->sp_pending = 0;
    }
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0, SLJIT_R0, 0);

    // A callout can change the VM stack pointer.
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP);
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_SP, 0, SLJIT_MEM1(SLJIT_R0), 0);

    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_R0);
}

void _gen_stack_size_modify(struct sljit_compiler *C, s32 offset) {
    //sp += offset ;  (deferred: folds into sp_pending, applied once later)
    if (jit_gen_context) {
        jit_gen_context->sp_pending += offset;
    } else {
        sljit_emit_op2(C, SLJIT_ADD, REGISTER_SP, 0, REGISTER_SP, 0, SLJIT_IMM, sizeof(StackEntry) * offset);
    }
}

/* fold sp_pending into the real register; SP-relative access helpers
 * compensate their offsets instead, this only runs before the register
 * value itself is consumed (publish, reload, hand-written addressing) */
static void _gen_sp_apply(struct sljit_compiler *C) {
    JitGenContext *ctx = jit_gen_context;
    if (!ctx || ctx->sp_pending == 0) {
        return;
    }
    sljit_emit_op2(C, SLJIT_ADD, REGISTER_SP, 0, REGISTER_SP, 0,
                   SLJIT_IMM, sizeof(StackEntry) * ctx->sp_pending);
    ctx->sp_pending = 0;
}

/* byte compensation for SP-relative slot addressing */
static s32 _tos_sp_adj_bytes(void) {
    return jit_gen_context ? jit_gen_context->sp_pending * (s32) sizeof(StackEntry) : 0;
}

//------------------------  stack peek ----------------------------


void _gen_stack_set_int(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->ivalue = v
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, ivalue), src, srcw);
}

void _gen_stack_set_long(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->ivalue = v
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, lvalue), src, srcw);
}

void _gen_stack_set_ref(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->ivalue = value
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, rvalue), src, srcw);
}

void _gen_stack_set_float(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->fvalue = v
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, fvalue), src, srcw);
}

void _gen_stack_set_double(struct sljit_compiler *C, s32 offset, sljit_s32 src, sljit_sw srcw) {
    //sp[offset]->dvalue = v
    sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, dvalue), src, srcw);
}

void _gen_stack_peek_int(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->ivalue
    sljit_emit_op1(C, SLJIT_MOV_S32, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, ivalue));
}

void _gen_stack_peek_long(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->lvalue
    sljit_emit_op1(C, SLJIT_MOV, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, lvalue));
}

void _gen_stack_peek_ref(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst = sp[offset]->rvalue
    sljit_emit_op1(C, SLJIT_MOV, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, rvalue));
}

void _gen_stack_peek_float(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->fvalue
    sljit_emit_fop1(C, SLJIT_MOV_F32, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, fvalue));
}

void _gen_stack_peek_double(struct sljit_compiler *C, s32 offset, sljit_s32 dst, sljit_sw dstw) {
    //dst=sp[offset]->dvalue
    sljit_emit_fop1(C, SLJIT_MOV_F64, dst, dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, dvalue));
}


void _gen_stack_peek_entry(struct sljit_compiler *C, s32 offset, sljit_s32 val_dst, sljit_sw val_dstw, sljit_s32 r_dst, sljit_sw r_dstw) {
    /* dup/swap pass logical SP-relative destinations as well as sources. */
    if (val_dst == SLJIT_MEM1(REGISTER_SP)) val_dstw += _tos_sp_adj_bytes();
    if (r_dst == SLJIT_MEM1(REGISTER_SP)) r_dstw += _tos_sp_adj_bytes();
    //val_dst=sp[offset]->lvalue
    sljit_emit_op1(C, SLJIT_MOV, val_dst, val_dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, lvalue));
    //rval_dst=sp[offset]->rvalue
    sljit_emit_op1(C, SLJIT_MOV, r_dst, r_dstw, SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * offset + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, rvalue));
}

//-------------------------  push pop  ---------------------------

void _gen_stack_push_int(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_int(stack, v);
    _gen_stack_set_int(C, 0, src, srcw);
    _gen_stack_size_modify(C, 1);
}

void _gen_stack_push_long(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_long(stack, v);
    _gen_stack_set_long(C, 0, src, srcw);
    _gen_stack_size_modify(C, 2);
}

void _gen_stack_push_ref(struct sljit_compiler *C, sljit_s32 src, sljit_sw srcw) {
    //push_ref(stack, v);
    _gen_stack_set_ref(C, 0, src, srcw);
    _gen_stack_size_modify(C, 1);
}

void _gen_stack_pop_int(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_int(stack);
    _gen_stack_size_modify(C, -1);
    _gen_stack_peek_int(C, 0, dst, dstw);
}

void _gen_stack_pop_long(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_long(stack);
    _gen_stack_size_modify(C, -2);
    _gen_stack_peek_long(C, 0, dst, dstw);
}

void _gen_stack_pop_ref(struct sljit_compiler *C, sljit_s32 dst, sljit_sw dstw) {
    //dst = pop_ref(stack);
    _gen_stack_size_modify(C, -1);
    _gen_stack_peek_ref(C, 0, dst, dstw);
}

//------------------------------  local var  ----------------------

void _gen_local_get_int(struct sljit_compiler *C, s32 index, sljit_s32 dst, sljit_sw dstw) {
    if (jit_gen_context) {
        if (index == jit_gen_context->hot_local[0]) {
            sljit_emit_op1(C, SLJIT_MOV_S32, dst, dstw, REGISTER_HOT_LOCAL0, 0);
            return;
        }
        if (index == jit_gen_context->hot_local[1]) {
            sljit_emit_op1(C, SLJIT_MOV_S32, dst, dstw, REGISTER_HOT_LOCAL1, 0);
            return;
        }
    }
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
    if (jit_gen_context) {
        if (index == jit_gen_context->hot_local[0]) {
            sljit_emit_op1(C, SLJIT_MOV_S32, REGISTER_HOT_LOCAL0, 0, src, srcw);
            return;
        }
        if (index == jit_gen_context->hot_local[1]) {
            sljit_emit_op1(C, SLJIT_MOV_S32, REGISTER_HOT_LOCAL1, 0, src, srcw);
            return;
        }
    }
    //localvar[index].ivalue = src
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, lvalue), src, srcw);
}

static void _gen_flush_hot_locals(struct sljit_compiler *C) {
    if (!jit_gen_context) {
        return;
    }
    if (jit_gen_context->hot_local[0] >= 0) {
        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(REGISTER_LOCALVAR),
                       sizeof(LocalVarItem) * jit_gen_context->hot_local[0] + SLJIT_OFFSETOF(LocalVarItem, ivalue),
                       REGISTER_HOT_LOCAL0, 0);
    }
    if (jit_gen_context->hot_local[1] >= 0) {
        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(REGISTER_LOCALVAR),
                       sizeof(LocalVarItem) * jit_gen_context->hot_local[1] + SLJIT_OFFSETOF(LocalVarItem, ivalue),
                       REGISTER_HOT_LOCAL1, 0);
    }
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
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(REGISTER_LOCALVAR), sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, rvalue), SLJIT_MEM1(REGISTER_SP), sizeof(StackEntry) * 0 + _tos_sp_adj_bytes() + SLJIT_OFFSETOF(StackEntry, rvalue));
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
    //    if (!arr) throw NullPointerException;
    //    else if (index < 0 || index >= jarray_length(arr)) throw ArrayIndexOutOfBoundsException;
    //    else push arr[index];
    //    index comes straight from the cache when possible, the loaded
    //    element becomes a cached value (numeric types only)
    // =====================================================================
    JitGenContext *ctx = jit_gen_context;
    sljit_s32 idx_reg = SLJIT_R1;

    if (ctx && ctx->tos.count > 0 && ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_INT) {
        s32 idx = ctx->tos.count - 1;
        _gen_tos_materialize(C, idx);
        idx_reg = ctx->tos.v[idx].reg;
        ctx->tos.count = idx;
        _gen_stack_size_modify(C, -1);
    } else {
        _gen_tos_flush(C);
        _gen_stack_size_modify(C, -1);
        _gen_stack_peek_int(C, 0, SLJIT_R1, 0);
    }

    /* the throw blocks flush at compile time: deeper cached values must
     * reach their slots on the normal path as well (consistency) */
    _gen_tos_flush(C);

    /* R0 = arr (top of the memory stack), index already in idx_reg */
    _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0,
                                      SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, -1);
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0), (sljit_sw) JVM_ARRAY_LENGTH_OFFSET);
    _gen_exception_check_throw_handle(C, SLJIT_GREATER_EQUAL, idx_reg, 0, SLJIT_R2, 0,
                                      JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, -1);

    /* pop arr; elements are inline at arr + JVM_ARRAY_BODY_OFFSET */
    _gen_stack_size_modify(C, -1);
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) JVM_ARRAY_BODY_OFFSET);

    if (datatype == DATATYPE_REFERENCE) {
        sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), SLJIT_POINTER_SHIFT);
        _gen_stack_push_ref(C, SLJIT_R0, 0);
    } else {
        u8 cache_dt = (datatype == DATATYPE_LONG || datatype == DATATYPE_DOUBLE
                       || datatype == DATATYPE_FLOAT)
                          ? datatype
                          : DATATYPE_INT;
        s32 idx2 = _gen_tos_reserve(C, cache_dt);
        sljit_s32 dst = jit_gen_context->tos.v[idx2].reg;
        switch (datatype) {
            case DATATYPE_BOOLEAN:
            case DATATYPE_BYTE:
                sljit_emit_op1(C, SLJIT_MOV_S8, dst, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), 0);
                break;
            case DATATYPE_SHORT:
                sljit_emit_op1(C, SLJIT_MOV_S16, dst, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), 1);
                break;
            case DATATYPE_JCHAR:
                sljit_emit_op1(C, SLJIT_MOV_U16, dst, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), 1);
                break;
            case DATATYPE_FLOAT:
                sljit_emit_fop1(C, SLJIT_MOV_F32, dst, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), 2);
                break;
            case DATATYPE_INT:
                sljit_emit_op1(C, SLJIT_MOV_S32, dst, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), 2);
                break;
            default: /* DATATYPE_LONG / DOUBLE */
                if (datatype == DATATYPE_DOUBLE) {
                    sljit_emit_fop1(C, SLJIT_MOV_F64, dst, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), 3);
                } else {
                    sljit_emit_op1(C, SLJIT_MOV, dst, 0, SLJIT_MEM2(SLJIT_R2, idx_reg), 3);
                }
                break;
        }
        _gen_stack_size_modify(C, _tos_slots(datatype));
    }
}

void _gen_arr_store(struct sljit_compiler *C, s32 datatype) {
    // =====================================================================
    //    s32 i = pop_int(stack);
    //    s32 index = pop_int(stack);
    //    Instance *jarr = (Instance *) pop_ref(stack);
    //    if (!jarr) throw NullPointerException;
    //    else if (index < 0 || index >= jarray_length(jarr)) throw ArrayIndexOutOfBoundsException;
    //    else jarr[index] = i;
    //    index and the numeric value come straight from the cache when
    //    possible; reference stores keep the legacy memory path
    // =====================================================================
    JitGenContext *ctx = jit_gen_context;
    s32 slots = (datatype == DATATYPE_LONG || datatype == DATATYPE_DOUBLE) ? 2 : 1;
    sljit_s32 idx_reg = SLJIT_R1;
    sljit_s32 val_reg = SLJIT_R5;
    sljit_s32 val_freg = 0;
    u8 val_dt = DATATYPE_INT;

    if (datatype == DATATYPE_REFERENCE) {
        /* the store-check helper calls out: everything through memory */
        _gen_tos_flush(C);
        _gen_stack_size_modify(C, -3);

        _gen_stack_peek_ref(C, 0, SLJIT_R0, 0); //arr (popped area)
        _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0,
                                          SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, 0);
        _gen_stack_peek_int(C, 1, SLJIT_R1, 0); //index
        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0), (sljit_sw) JVM_ARRAY_LENGTH_OFFSET);
        _gen_exception_check_throw_handle(C, SLJIT_GREATER_EQUAL, SLJIT_R1, 0, SLJIT_R2, 0,
                                          JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, 0);

        _gen_save_sp_ip(C);
        _gen_stack_peek_ref(C, 0, SLJIT_R0, 0); //arr
        _gen_stack_peek_ref(C, 2, SLJIT_R1, 0); //value
        sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM,
                         SLJIT_FUNC_ADDR(jarray_reference_store_check));
        _gen_load_sp_ip(C);
        _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0,
                                          SLJIT_IMM, 0, JVM_EXCEPTION_ARRAYSTORE, 0);

        _gen_stack_peek_ref(C, 0, SLJIT_R1, 0); //arr
        _gen_stack_peek_int(C, 1, SLJIT_R0, 0); //index
        //elements are inline: body = arr + JVM_ARRAY_BODY_OFFSET
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R1, 0, SLJIT_IMM, (sljit_sw) JVM_ARRAY_BODY_OFFSET);
        _gen_stack_peek_ref(C, 2, SLJIT_R1, 0);
        sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM2(SLJIT_R2, SLJIT_R0), SLJIT_POINTER_SHIFT, SLJIT_R1, 0);
        return;
    }

    /* value (stack top): cached when its type matches the element */
    if (ctx && ctx->tos.count > 0
        && ((datatype == DATATYPE_INT && (ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_INT
                                          || ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_FLOAT))
            || (datatype == DATATYPE_LONG && (ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_LONG
                                              || ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_DOUBLE))
            || (datatype != DATATYPE_INT && datatype != DATATYPE_LONG
                && ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_INT))) {
        s32 idx = ctx->tos.count - 1;
        _gen_tos_materialize(C, idx);
        val_dt = ctx->tos.v[idx].datatype;
        if (_tos_is_float(val_dt)) {
            val_freg = ctx->tos.v[idx].reg;
        } else {
            val_reg = ctx->tos.v[idx].reg;
        }
        ctx->tos.count = idx;
        _gen_stack_size_modify(C, -slots);
    } else {
        /* defensive: an unmatched cached top must still reach its slot
         * before the memory path reads the stack */
        _gen_tos_flush(C);
        if (datatype == DATATYPE_DOUBLE) {
            _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
            val_freg = SLJIT_FR0;
            val_dt = DATATYPE_DOUBLE;
        } else if (datatype == DATATYPE_LONG) {
            _gen_stack_peek_long(C, -2, val_reg, 0);
            val_dt = DATATYPE_LONG;
        } else {
            _gen_stack_peek_int(C, -1, val_reg, 0);
        }
        _gen_stack_size_modify(C, -slots);
    }

    /* index */
    if (ctx && ctx->tos.count > 0 && ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_INT) {
        s32 idx = ctx->tos.count - 1;
        _gen_tos_materialize(C, idx);
        idx_reg = ctx->tos.v[idx].reg;
        ctx->tos.count = idx;
        _gen_stack_size_modify(C, -1);
    } else {
        _gen_tos_flush(C);
        _gen_stack_size_modify(C, -1);
        _gen_stack_peek_int(C, 0, SLJIT_R1, 0);
    }

    /* the throw blocks flush at compile time: keep both paths consistent */
    _gen_tos_flush(C);

    /* R0 = arr (top of the memory stack), index in idx_reg, value kept */
    _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0,
                                      SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, -1);
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0), (sljit_sw) JVM_ARRAY_LENGTH_OFFSET);
    _gen_exception_check_throw_handle(C, SLJIT_GREATER_EQUAL, idx_reg, 0, SLJIT_R2, 0,
                                      JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, -1);

    _gen_stack_size_modify(C, -1);
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) JVM_ARRAY_BODY_OFFSET);

    if (val_freg) {
        if (val_dt == DATATYPE_FLOAT) {
            sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_MEM2(SLJIT_R2, idx_reg), 2, val_freg, 0);
        } else {
            sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_MEM2(SLJIT_R2, idx_reg), 3, val_freg, 0);
        }
        return;
    }
    switch (datatype) {
        case DATATYPE_BOOLEAN:
        case DATATYPE_BYTE:
            sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_MEM2(SLJIT_R2, idx_reg), 0, val_reg, 0);
            break;
        case DATATYPE_SHORT:
            sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_MEM2(SLJIT_R2, idx_reg), 1, val_reg, 0);
            break;
        case DATATYPE_JCHAR:
            sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_MEM2(SLJIT_R2, idx_reg), 1, val_reg, 0);
            break;
        case DATATYPE_FLOAT:
        case DATATYPE_INT:
            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM2(SLJIT_R2, idx_reg), 2, val_reg, 0);
            break;
        default: /* DATATYPE_LONG / DOUBLE */
            sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM2(SLJIT_R2, idx_reg), 3, val_reg, 0);
            break;
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

void _gen_arith_long_2op(struct sljit_compiler *C, sljit_s32 op) {
    // 首先检查除零错误（对长整型）
    if (op == SLJIT_DIV_UW || op == SLJIT_DIV_SW || op == SLJIT_DIVMOD_UW || op == SLJIT_DIVMOD_SW) {
        _gen_stack_peek_long(C, -2, SLJIT_R0, 0);
        _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_ARRITHMETIC, -4);
    }

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

//------------------------------  cmp  ----------------------

void _gen_icmp_op1(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 type) {
    s32 offset = *((s16 *) (ip + 1));
    s32 jumpto = code_idx + offset;
    struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
    }

    _gen_stack_pop_int(C, SLJIT_R0, 0);
    /* the taken path must land with the physical SP already aligned */
    _gen_sp_apply(C);

    struct sljit_jump *jump_true, *jump_out, *jump_away;
    struct sljit_label *label_out, *label_true;
    jump_true = sljit_emit_cmp(C, type, SLJIT_R0, 0, SLJIT_IMM, 0);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {
        // if R0 vs. 0 true
        _gen_jump_to_suspend_check(C, ip, offset);
        jump_away = sljit_emit_jump(C, SLJIT_JUMP);
        pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_out, label_out);
    sljit_set_label(jump_true, label_true);
}

static void _gen_icmp_op2_regs(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 test_type) {
    s32 offset = *((s16 *) (ip + 1));
    s32 jumpto = code_idx + offset;
    struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
    }

    /* R0=value2(top), R1=value1(deeper): direct conditional jump */
    struct sljit_jump *jump_if_true, *jump_out, *jump_away;
    struct sljit_label *label_out, *label_true;
    jump_if_true = sljit_emit_cmp(C, test_type, SLJIT_R1, 0, SLJIT_R0, 0);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {
        _gen_jump_to_suspend_check(C, ip, offset);
        jump_away = sljit_emit_jump(C, SLJIT_JUMP);
        pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
    }
    label_out = sljit_emit_label(C);
    sljit_set_label(jump_if_true, label_true);
    sljit_set_label(jump_out, label_out);
}

void _gen_icmp_op2(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 test_type) {
    _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
    _gen_stack_peek_int(C, -2, SLJIT_R1, 0);
    _gen_stack_size_modify(C, -2);
    _gen_sp_apply(C);
    _gen_icmp_op2_regs(C, method, ip, code_idx, test_type);
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
        _gen_jump_to_suspend_check(C, ip, offset);
        struct sljit_jump *jump_away = sljit_emit_jump(C, SLJIT_JUMP);
        pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, jumpto);
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_if_true, label_true);
    sljit_set_label(jump_out, label_out);
}


void _gen_goto(struct sljit_compiler *C, MethodInfo *method, s32 code_idx, s32 offset) {
    _gen_tos_flush(C);
    const u8 *branch_ip = jit_gen_context ? jit_gen_context->jit_code + code_idx : NULL;
    _gen_jump_to_suspend_check(C, branch_ip, offset);

    s32 jumpto = code_idx + offset;
    struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
    }

    struct sljit_jump *jump_away = sljit_emit_jump(C, SLJIT_JUMP);
    pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, jumpto);
}

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
    {
        // if R0 vs. 0 true
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
        _gen_load_sp_ip(C);
        sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(Runtime, jit_exception_jump_ptr));
    }
    label_out = sljit_emit_label(C);
    //
    sljit_set_label(jump_found_handle, label_found_handle);
}

/*
 * Unified dispatch for a helper/callee status sitting in RETURN_REG.
 * NORMAL is the fall-through (next bytecode).  EXCEPTION enters this
 * method's handler lookup (the exception reference is on the shared
 * stack, caller PC already published).  ERROR/INTERRUPT carry no
 * exception reference and no catch block may see them: propagate the
 * status unchanged to this method's own caller.
 */
void _gen_invoke_status_dispatch(struct sljit_compiler *C) {
    struct sljit_jump *jump_nonnormal, *jump_exception, *jump_normal;
    struct sljit_label *label_nonnormal, *label_exception, *label_normal;

    jump_nonnormal = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_RETURN_REG, 0,
                                    SLJIT_IMM, RUNTIME_STATUS_NORMAL);
    jump_normal = sljit_emit_jump(C, SLJIT_JUMP);

    label_nonnormal = sljit_emit_label(C);
    jump_exception = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0,
                                    SLJIT_IMM, RUNTIME_STATUS_EXCEPTION);
    /* fall through: ERROR / INTERRUPT */
    sljit_emit_return(C, SLJIT_MOV, SLJIT_RETURN_REG, 0);

    label_exception = sljit_emit_label(C);
    _gen_exception_handle(C);

    label_normal = sljit_emit_label(C);
    sljit_set_label(jump_nonnormal, label_nonnormal);
    sljit_set_label(jump_exception, label_exception);
    sljit_set_label(jump_normal, label_normal);
}

/**
 *    if src1=src2 then throw exception(type) and handle
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
    /* Both paths start at the same physical SP. Materializing only inside
     * the throw block would lose the incoming delta on the normal path. */
    _gen_tos_flush(C);
    struct sljit_jump *jump_true, *jump_out;
    struct sljit_label *label_out, *label_true;
    jump_true = sljit_emit_cmp(C, cmp, src1, srcw1, src2, srcw2);
    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {
        // if R0 vs. 0 true
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


void _gen_jump_to_suspend_check(struct sljit_compiler *C, const u8 *branch_ip, s32 offset) {
    if (offset >= 0) {
        return;
    }
    /* The no-suspend path skips publication, so normalize before the test. */
    _gen_tos_flush(C);
    {
        struct sljit_jump *jump_skip;
        struct sljit_label *label_skip;

        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_THREADINFO);
        sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(JavaThreadInfo, suspend_count));
        jump_skip = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0);
        {
            const u8 *safepoint_ip = NULL;
            if (jit_gen_context && branch_ip) {
                safepoint_ip = (offset == -1 && branch_ip == jit_gen_context->jit_code)
                                   ? branch_ip
                                   : branch_ip + offset;
            }
            _gen_save_sp_pc_at(C, safepoint_ip);
            sljit_emit_ijump(C, SLJIT_FAST_CALL, SLJIT_IMM, SLJIT_FUNC_ADDR(check_suspend));
        }
        label_skip = sljit_emit_label(C);
        sljit_set_label(jump_skip, label_skip);
    }
}

//------------------------------  inst impl  ----------------------

s32 multiarray(Runtime *runtime, Utf8String *desc, s32 count) {
    RuntimeStack *stack = runtime->stack;
    // 使用固定大小数组并添加边界检查以提高安全性
#define MAX_ARRAY_DIMENSIONS 32
    s32 dim[MAX_ARRAY_DIMENSIONS];

    // 添加维度数量的边界检查
    if (count > MAX_ARRAY_DIMENSIONS || count <= 0) {
        // 应该抛出适当的异常
        return RUNTIME_STATUS_EXCEPTION;
    }

    s32 i;
    s32 has_negative_dimension = 0;
    for (i = 0; i < count; i++) {
        dim[i] = pop_int(stack);
        if (dim[i] < 0) has_negative_dimension = 1;
    }

    if (has_negative_dimension) {
        push_ref(stack, exception_create(JVM_EXCEPTION_NEGATIVEARRAYSIZE, runtime));
        return RUNTIME_STATUS_EXCEPTION;
    }

    Instance *arr = jarray_multi_create(runtime, dim, count, desc, 0);

    if (!arr) {
        return exception_throw_out_of_memory(runtime);
    } else {
        push_ref(stack, (__refer) arr);
    }
    return RUNTIME_STATUS_NORMAL;
}


//------------------------  jit peephole fusion ----------------------------

static s32 _jit_match_iload(const u8 *ip, const u8 *end, s32 *out_idx, s32 *out_len) {
    if (ip >= end) return 0;
    u8 op = *ip;
    if (op >= op_iload_0 && op <= op_iload_3) {
        *out_idx = (s32) (op - op_iload_0);
        *out_len = 1;
        return 1;
    }
    if (op == op_iload && ip + 1 < end) {
        *out_idx = (u8) ip[1];
        *out_len = 2;
        return 1;
    }
    return 0;
}

static s32 _jit_match_istore(const u8 *ip, const u8 *end, s32 *out_idx, s32 *out_len) {
    if (ip >= end) return 0;
    u8 op = *ip;
    if (op >= op_istore_0 && op <= op_istore_3) {
        *out_idx = (s32) (op - op_istore_0);
        *out_len = 1;
        return 1;
    }
    if (op == op_istore && ip + 1 < end) {
        *out_idx = (u8) ip[1];
        *out_len = 2;
        return 1;
    }
    return 0;
}

//------------------------  jit peephole fusion ----------------------------

static s32 _jit_fusion_range_safe(MethodInfo *method, CodeAttribute *ca, s32 code_idx, s32 len) {
    s32 i;
    if (len <= 0) {
        return 0;
    }
    for (i = code_idx + 1; i < code_idx + len; i++) {
        if (pairlist_getl(method->pos_2_label, i)) {
            return 0;
        }
    }
    ExceptionTable *et = ca->exception_table;
    for (i = 0; i < ca->exception_table_length; i++) {
        u16 start = et[i].start_pc;
        u16 end = et[i].end_pc;
        u16 handler = et[i].handler_pc;
        s32 fused_end = code_idx + len;
        if ((s32) handler >= code_idx && (s32) handler < fused_end) {
            return 0;
        }
        if (code_idx < (s32) start && fused_end > (s32) start) {
            return 0;
        }
        if (code_idx < (s32) end && fused_end > (s32) end) {
            return 0;
        }
    }
    return 1;
}

static void _gen_local_get_float(struct sljit_compiler *C, s32 index, sljit_s32 dst, sljit_sw dstw) {
    sljit_emit_fop1(C, SLJIT_MOV_F32, dst, dstw, SLJIT_MEM1(REGISTER_LOCALVAR),
                    sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, fvalue));
}

static void _gen_local_set_float(struct sljit_compiler *C, s32 index, sljit_s32 src, sljit_sw srcw) {
    sljit_emit_fop1(C, SLJIT_MOV_F32, SLJIT_MEM1(REGISTER_LOCALVAR),
                    sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, fvalue), src, srcw);
}

//------------------------  TOS register cache  ----------------------------
/*
 * Straight-line, basic-block-local caching of the top two numeric stack
 * values.  Invariants:
 *  - Logical SP = REGISTER_SP + sp_pending * sizeof(StackEntry). Pushes
 *    and pops adjust the delta; stack helpers compensate their addresses.
 *    Control-flow boundaries apply the delta before paths split or merge.
 *  - The cached slots themselves hold garbage; they are materialized by
 *    _gen_tos_flush() at every label position (branch targets, exception
 *    handlers, fall-through after branches), at every opcode outside the
 *    cache-aware set (gate in gen_jit_bytecode_func) and inside
 *    _gen_save_sp_pc_at before anything publishes the frame.
 *  - References are never cached: the collector scans stack slots.
 *  - Int values in registers are kept sign-extended (32 bit canonical)
 *    so signed compares and conversions see the Java value.
 */

static sljit_s32 _tos_is_float(u8 datatype) {
    return datatype == DATATYPE_FLOAT || datatype == DATATYPE_DOUBLE;
}

static s32 _tos_slots(u8 datatype) {
    return (datatype == DATATYPE_LONG || datatype == DATATYPE_DOUBLE) ? 2 : 1;
}

/* first free register of the given bank among the cached values;
 * exclude_idx (>= 0) marks an entry whose register is being reassigned
 * right now — e.g. an int -> float conversion still carries its old GP
 * register number, which shares encoding with FR3/FR4 and must not be
 * counted as a float-bank occupation */
static sljit_s32 _tos_pick_reg(const TosCache *t, s32 float_bank, s32 exclude_idx) {
    sljit_s32 first = float_bank ? SLJIT_FR3 : SLJIT_R3;
    sljit_s32 second = float_bank ? SLJIT_FR4 : SLJIT_R4;
    s32 i;
    for (i = 0; i < t->count; i++) {
        if (i == exclude_idx) {
            continue;
        }
        if (_tos_is_float(t->v[i].datatype) == float_bank && t->v[i].reg == first) {
            return second;
        }
    }
    return first;
}

/* write one cached value into its stack slot; `above` = slots of the
 * cached values sitting on top of it */
static void _gen_tos_store_one(struct sljit_compiler *C, const TosValue *v, s32 above) {
    if (v->is_imm) {
        if (v->datatype == DATATYPE_LONG) {
            _gen_stack_set_long(C, -(above + 2), SLJIT_IMM, v->imm);
        } else {
            _gen_stack_set_int(C, -(above + 1), SLJIT_IMM, v->imm);
        }
        return;
    }
    if (v->datatype == DATATYPE_FLOAT) {
        _gen_stack_set_float(C, -(above + 1), v->reg, 0);
    } else if (v->datatype == DATATYPE_DOUBLE) {
        _gen_stack_set_double(C, -(above + 2), v->reg, 0);
    } else if (v->datatype == DATATYPE_LONG) {
        _gen_stack_set_long(C, -(above + 2), v->reg, 0);
    } else {
        _gen_stack_set_int(C, -(above + 1), v->reg, 0);
    }
}

/* write cached values into their (compensated) slots; SP stays behind */
static void _gen_tos_flush_values(struct sljit_compiler *C) {
    JitGenContext *ctx = jit_gen_context;
    s32 above = 0;
    s32 i;
    if (!ctx) {
        return;
    }
    for (i = ctx->tos.count - 1; i >= 0; i--) {
        _gen_tos_store_one(C, &ctx->tos.v[i], above);
        above += ctx->tos.v[i].slots;
    }
    ctx->tos.count = 0;
}

static void _gen_tos_flush(struct sljit_compiler *C) {
    /* full materialization: slots written AND the register catches up,
     * so hand-written SP addressing after a flush (non cache-aware
     * emitters) always sees the logical stack */
    _gen_tos_flush_values(C);
    _gen_sp_apply(C);
}

/* load an immediate cached value into its register */
static void _gen_tos_materialize(struct sljit_compiler *C, s32 idx) {
    JitGenContext *ctx = jit_gen_context;
    TosValue *v = &ctx->tos.v[idx];
    if (!v->is_imm) {
        return;
    }
    sljit_emit_op1(C, v->datatype == DATATYPE_LONG ? SLJIT_MOV : SLJIT_MOV_S32,
                   v->reg, 0, SLJIT_IMM, v->imm);
    v->is_imm = 0;
}

/* make room for one more cached value (evicts the deepest when full) and
 * register the new slot; caller loads the register / sets is_imm */
static s32 _gen_tos_reserve(struct sljit_compiler *C, u8 datatype) {
    JitGenContext *ctx = jit_gen_context;
    s32 idx;
    if (ctx->tos.count == 2) {
        _gen_tos_store_one(C, &ctx->tos.v[0], ctx->tos.v[1].slots);
        ctx->tos.v[0] = ctx->tos.v[1];
        ctx->tos.count = 1;
    }
    idx = ctx->tos.count;
    ctx->tos.v[idx].datatype = datatype;
    ctx->tos.v[idx].slots = _tos_slots(datatype);
    ctx->tos.v[idx].is_imm = 0;
    ctx->tos.v[idx].imm = 0;
    ctx->tos.v[idx].reg = _tos_pick_reg(&ctx->tos, _tos_is_float(datatype), -1);
    ctx->tos.count = idx + 1;
    return idx;
}

static void _gen_tos_push_imm(struct sljit_compiler *C, u8 datatype, sljit_sw value) {
    s32 idx = _gen_tos_reserve(C, datatype);
    jit_gen_context->tos.v[idx].is_imm = 1;
    jit_gen_context->tos.v[idx].imm = value;
    _gen_stack_size_modify(C, _tos_slots(datatype));
}

static void _gen_tos_push_fconst32(struct sljit_compiler *C, f32 value) {
    s32 idx = _gen_tos_reserve(C, DATATYPE_FLOAT);
    sljit_emit_fset32(C, jit_gen_context->tos.v[idx].reg, value);
    _gen_stack_size_modify(C, 1);
}

static void _gen_tos_push_fconst64(struct sljit_compiler *C, f64 value) {
    s32 idx = _gen_tos_reserve(C, DATATYPE_DOUBLE);
    sljit_emit_fset64(C, jit_gen_context->tos.v[idx].reg, value);
    _gen_stack_size_modify(C, 2);
}

static void _gen_tos_load_local(struct sljit_compiler *C, u8 datatype, s32 index) {
    s32 idx = _gen_tos_reserve(C, datatype);
    sljit_s32 reg = jit_gen_context->tos.v[idx].reg;
    switch (datatype) {
        case DATATYPE_INT:
            _gen_local_get_int(C, index, reg, 0);
            break;
        case DATATYPE_LONG:
            _gen_local_get_long(C, index, reg, 0);
            break;
        case DATATYPE_FLOAT:
            _gen_local_get_float(C, index, reg, 0);
            break;
        default: /* DATATYPE_DOUBLE: 8 byte value shares the lvalue slot */
            sljit_emit_fop1(C, SLJIT_MOV_F64, reg, 0, SLJIT_MEM1(REGISTER_LOCALVAR),
                            sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, lvalue));
            break;
    }
    _gen_stack_size_modify(C, _tos_slots(datatype));
}

static void _tos_peek_load(struct sljit_compiler *C, u8 datatype, s32 offset, sljit_s32 reg) {
    switch (datatype) {
        case DATATYPE_INT:
            _gen_stack_peek_int(C, offset, reg, 0);
            break;
        case DATATYPE_LONG:
            _gen_stack_peek_long(C, offset, reg, 0);
            break;
        case DATATYPE_FLOAT:
            _gen_stack_peek_float(C, offset, reg, 0);
            break;
        default:
            _gen_stack_peek_double(C, offset, reg, 0);
            break;
    }
}

/*
 * Prepare the two operands of a binary op.  dt1/dt2 are the datatypes of
 * value1 (deeper, gives the result type) and value2 (top); they differ
 * only for the long shifts, whose count operand is an int.  On return
 * the operands are in src1/src2, dst holds the surviving cache slot v[0]
 * register and the result must be produced as dst = src1 op src2.
 * SP drops by the value2 footprint (pop 2 push 1).
 */
static void _gen_tos_binary_operands(struct sljit_compiler *C, u8 dt1, u8 dt2,
                                     sljit_s32 *out_dst, sljit_s32 *out_src1, sljit_s32 *out_src2,
                                     sljit_sw *out_src2w) {
    JitGenContext *ctx = jit_gen_context;
    s32 w1 = _tos_slots(dt1);
    s32 w2 = _tos_slots(dt2);
    s32 k = ctx->tos.count;
    sljit_s32 dst, src1, src2;
    sljit_sw src2w = 0;

    if (k >= 2) {
        _gen_tos_materialize(C, 0);
        dst = src1 = ctx->tos.v[0].reg;
        if (ctx->tos.v[1].is_imm && !_tos_is_float(dt2)) {
            src2 = SLJIT_IMM;
            src2w = ctx->tos.v[1].imm;
        } else {
            _gen_tos_materialize(C, 1);
            src2 = ctx->tos.v[1].reg;
        }
        ctx->tos.count = 1;
        ctx->tos.v[0].datatype = dt1;
        ctx->tos.v[0].slots = w1;
        ctx->tos.v[0].reg = dst;
    } else if (k == 1) {
        if (ctx->tos.v[0].is_imm && !_tos_is_float(dt2)) {
            src2 = SLJIT_IMM;
            src2w = ctx->tos.v[0].imm;
            dst = src1 = _tos_is_float(dt1) ? SLJIT_FR3 : SLJIT_R3;
            _tos_peek_load(C, dt1, -(w1 + w2), src1);
            ctx->tos.v[0].datatype = dt1;
            ctx->tos.v[0].slots = w1;
            ctx->tos.v[0].is_imm = 0;
            ctx->tos.v[0].reg = dst;
            _gen_stack_size_modify(C, -w2);
            *out_dst = dst;
            *out_src1 = src1;
            *out_src2 = src2;
            *out_src2w = src2w;
            return;
        }
        _gen_tos_materialize(C, 0);
        dst = src2 = ctx->tos.v[0].reg;
        src1 = _tos_pick_reg(&ctx->tos, _tos_is_float(dt1), -1);
        _tos_peek_load(C, dt1, -(w1 + w2), src1);
        /* the result takes value1's type and footprint (e.g. a long
         * shift whose count operand is an int cached on top) */
        ctx->tos.v[0].datatype = dt1;
        ctx->tos.v[0].slots = w1;
        ctx->tos.v[0].reg = dst;
    } else {
        dst = src1 = _tos_is_float(dt1) ? SLJIT_FR3 : SLJIT_R3;
        src2 = _tos_is_float(dt2) ? SLJIT_FR4 : SLJIT_R4;
        _tos_peek_load(C, dt2, -w2, src2);
        _tos_peek_load(C, dt1, -(w1 + w2), src1);
        ctx->tos.v[0].datatype = dt1;
        ctx->tos.v[0].slots = w1;
        ctx->tos.v[0].is_imm = 0;
        ctx->tos.v[0].reg = dst;
        ctx->tos.count = 1;
    }
    _gen_stack_size_modify(C, -w2);
    *out_dst = dst;
    *out_src1 = src1;
    *out_src2 = src2;
    *out_src2w = src2w;
}

static void _gen_tos_arith_2op(struct sljit_compiler *C, u8 dt1, u8 dt2, sljit_s32 op) {
    sljit_s32 dst, src1, src2;
    sljit_sw src2w;
    _gen_tos_binary_operands(C, dt1, dt2, &dst, &src1, &src2, &src2w);
    if (op == SLJIT_SHL || op == SLJIT_ASHR || op == SLJIT_LSHR
        || op == SLJIT_SHL32 || op == SLJIT_ASHR32 || op == SLJIT_LSHR32) {
        if (src2 == SLJIT_IMM) {
            src2w &= (dt1 == DATATYPE_LONG) ? 0x3f : 0x1f;
        } else {
            sljit_emit_op2(C, SLJIT_AND, src2, 0, src2, 0, SLJIT_IMM,
                           dt1 == DATATYPE_LONG ? 0x3f : 0x1f);
        }
    }
    if (_tos_is_float(dt1)) {
        sljit_emit_fop2(C, op, dst, 0, src1, 0, src2, 0);
    } else {
        sljit_emit_op2(C, op, dst, 0, src1, 0, src2, src2w);
        if (dt1 == DATATYPE_INT) {
            /* keep the Java 32 bit value sign extended in the register */
            sljit_emit_op1(C, SLJIT_MOV_S32, dst, 0, dst, 0);
        }
    }
}

/*
 * Consume the cached top for a unary op and re-enter it as dst_dt.
 * SP is adjusted for the width change (e.g. i2l grows by one slot).
 * Returns the cache slot index; *src_reg_out receives the register
 * holding the (already canonical, for ints) input value, and the result
 * must be produced into ctx->tos.v[idx].reg — the same register for
 * same-band conversions, a float register for int -> float.
 */
static s32 _gen_tos_prepare_unary(struct sljit_compiler *C, u8 src_dt, u8 dst_dt, sljit_s32 *src_reg_out) {
    JitGenContext *ctx = jit_gen_context;
    s32 idx;
    sljit_s32 src_reg;
    if (ctx->tos.count > 0) {
        idx = ctx->tos.count - 1;
        _gen_tos_materialize(C, idx);
        src_reg = ctx->tos.v[idx].reg;
        ctx->tos.v[idx].datatype = dst_dt;
        ctx->tos.v[idx].slots = _tos_slots(dst_dt);
        if (_tos_is_float(dst_dt) != _tos_is_float(src_dt)) {
            /* the entry's old GP number still sits in .reg: exclude it
             * from the float-bank occupation test while re-picking */
            ctx->tos.v[idx].reg = _tos_pick_reg(&ctx->tos, _tos_is_float(dst_dt), idx);
        }
    } else {
        idx = _gen_tos_reserve(C, dst_dt);
        if (_tos_is_float(dst_dt) == _tos_is_float(src_dt)) {
            /* same register band: load in place, ops transform v.reg */
            src_reg = ctx->tos.v[idx].reg;
        } else {
            src_reg = _tos_is_float(src_dt) ? SLJIT_FR3 : SLJIT_R3;
        }
        _tos_peek_load(C, src_dt, -_tos_slots(src_dt), src_reg);
    }
    _gen_stack_size_modify(C, _tos_slots(dst_dt) - _tos_slots(src_dt));
    *src_reg_out = src_reg;
    return idx;
}

/* istore/lstore/fstore/dstore straight out of the cache when the type
 * matches; returns 0 when the caller must flush and use the memory path */
static s32 _gen_tos_try_store_local(struct sljit_compiler *C, u8 datatype, s32 index) {
    JitGenContext *ctx = jit_gen_context;
    TosValue *v;
    s32 idx;
    if (!ctx || ctx->tos.count == 0) {
        return 0;
    }
    idx = ctx->tos.count - 1;
    v = &ctx->tos.v[idx];
    if (v->datatype != datatype) {
        return 0;
    }
    if (v->is_imm) {
        if (datatype == DATATYPE_LONG) {
            _gen_local_set_long(C, index, SLJIT_IMM, v->imm);
        } else {
            _gen_local_set_int(C, index, SLJIT_IMM, v->imm);
        }
    } else {
        switch (datatype) {
            case DATATYPE_INT:
                _gen_local_set_int(C, index, v->reg, 0);
                break;
            case DATATYPE_LONG:
                _gen_local_set_long(C, index, v->reg, 0);
                break;
            case DATATYPE_FLOAT:
                _gen_local_set_float(C, index, v->reg, 0);
                break;
            default: /* DATATYPE_DOUBLE */
                sljit_emit_fop1(C, SLJIT_MOV_F64, SLJIT_MEM1(REGISTER_LOCALVAR),
                                sizeof(LocalVarItem) * index + SLJIT_OFFSETOF(LocalVarItem, lvalue),
                                v->reg, 0);
                break;
        }
    }
    ctx->tos.count = idx;
    _gen_stack_size_modify(C, -_tos_slots(datatype));
    return 1;
}

/* if<cond> comparing the cached int top against zero; falls back to the
 * memory emitter when the top is not a cached int */
static void _gen_tos_if1(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 type) {
    JitGenContext *ctx = jit_gen_context;
    if (ctx && ctx->tos.count > 0 && ctx->tos.v[ctx->tos.count - 1].datatype == DATATYPE_INT) {
        s32 offset = *((s16 *) (ip + 1));
        s32 jumpto = code_idx + offset;
        struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
        struct sljit_jump *jump_true, *jump_out, *jump_away;
        struct sljit_label *label_out, *label_true;
        sljit_s32 reg;

        if (!label) {
            jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
        }
        reg = ctx->tos.v[ctx->tos.count - 1].reg;
        _gen_tos_materialize(C, ctx->tos.count - 1);
        ctx->tos.count--;
        _gen_stack_size_modify(C, -1);
        /* both the branch target and the fall-through label are reached
         * with an empty cache: deeper values must be in their slots
         * before the jump */
        _gen_tos_flush(C);

        jump_true = sljit_emit_cmp(C, type, reg, 0, SLJIT_IMM, 0);
        {
            jump_out = sljit_emit_jump(C, SLJIT_JUMP);
        }
        label_true = sljit_emit_label(C);
        {
            // if reg vs. 0 true
            _gen_jump_to_suspend_check(C, ip, offset);
            jump_away = sljit_emit_jump(C, SLJIT_JUMP);
            pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
        }
        label_out = sljit_emit_label(C);
        //
        sljit_set_label(jump_out, label_out);
        sljit_set_label(jump_true, label_true);
    } else {
        _gen_tos_flush(C);
        _gen_icmp_op1(C, method, ip, code_idx, type);
    }
}

/* if_icmp<cond> on two cached ints; falls back to the memory emitter */
static void _gen_tos_if2(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, sljit_s32 test_type) {
    JitGenContext *ctx = jit_gen_context;
    if (ctx && ctx->tos.count == 2
        && ctx->tos.v[0].datatype == DATATYPE_INT && ctx->tos.v[1].datatype == DATATYPE_INT) {
        s32 offset = *((s16 *) (ip + 1));
        s32 jumpto = code_idx + offset;
        struct sljit_label *label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
        struct sljit_jump *jump_if_true, *jump_out, *jump_away;
        struct sljit_label *label_out, *label_true;

        if (!label) {
            jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
        }
        {
            sljit_s32 r1 = ctx->tos.v[0].reg;
            sljit_s32 r2;
            sljit_sw r2w = 0;
            _gen_tos_materialize(C, 0);
            if (ctx->tos.v[1].is_imm) {
                r2 = SLJIT_IMM;
                r2w = ctx->tos.v[1].imm;
            } else {
                _gen_tos_materialize(C, 1);
                r2 = ctx->tos.v[1].reg;
            }
            ctx->tos.count = 0;
            _gen_stack_size_modify(C, -2);
            _gen_tos_flush(C);

            jump_if_true = sljit_emit_cmp(C, test_type, r1, 0, r2, r2w);
            {
                jump_out = sljit_emit_jump(C, SLJIT_JUMP);
            }
            label_true = sljit_emit_label(C);
            {
                _gen_jump_to_suspend_check(C, ip, offset);
                jump_away = sljit_emit_jump(C, SLJIT_JUMP);
                pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
            }
            label_out = sljit_emit_label(C);
            sljit_set_label(jump_if_true, label_true);
            sljit_set_label(jump_out, label_out);
            (void) label;
        }
    } else {
        _gen_tos_flush(C);
        _gen_icmp_op2(C, method, ip, code_idx, test_type);
    }
}

/*
 * Fuse lcmp/fcmpl/fcmpg/dcmpl/dcmpg followed by if<cond> into a direct
 * compare-and-jump on the original operands: skips the -1/0/1 result
 * value and, for floats, the C helper call.  ip points at the compare
 * opcode, ip[1..3] must be the if.  Returns 1 when both bytecodes were
 * consumed.  Callers run outside the cache-aware set, so the operands
 * are read from memory with an empty cache.
 */
static s32 _gen_fused_cmp_if(struct sljit_compiler *C, MethodInfo *method, u8 *ip, s32 code_idx, const u8 *end, u8 cmp_op) {
    u8 if_op;
    s32 nan_negative; /* cmpl: NaN counts as -1, cmpg: as +1 */
    s32 is_float;
    s32 offset, jumpto;
    s32 test_type;
    struct sljit_label *label;
    struct sljit_jump *jump_if_true, *jump_out, *jump_away;
    struct sljit_label *label_out, *label_true;

    if (ip + 4 > end) {
        return 0;
    }
    if_op = ip[1];
    if (if_op < op_ifeq || if_op > op_ifle) {
        return 0;
    }
    /* nothing may branch into the middle of the pair */
    if (pairlist_getl(method->pos_2_label, code_idx + 1)) {
        return 0;
    }

    is_float = (cmp_op != op_lcmp);
    nan_negative = (cmp_op == op_fcmpl || cmp_op == op_dcmpl);

    if (!is_float) {
        switch (if_op) {
            case op_ifeq: test_type = SLJIT_EQUAL;
                break;
            case op_ifne: test_type = SLJIT_NOT_EQUAL;
                break;
            case op_iflt: test_type = SLJIT_SIG_LESS;
                break;
            case op_ifge: test_type = SLJIT_SIG_GREATER_EQUAL;
                break;
            case op_ifgt: test_type = SLJIT_SIG_GREATER;
                break;
            default: test_type = SLJIT_SIG_LESS_EQUAL;
                break; /* op_ifle */
        }
    } else {
        /* JVM cmp result: v1<v2 -> -1, equal -> 0, v1>v2 -> +1, NaN -> -1/+1 */
        switch (if_op) {
            case op_ifeq: test_type = SLJIT_ORDERED_EQUAL;
                break;
            case op_ifne: test_type = SLJIT_UNORDERED_OR_NOT_EQUAL;
                break;
            case op_iflt: test_type = nan_negative ? SLJIT_UNORDERED_OR_LESS : SLJIT_ORDERED_LESS;
                break;
            case op_ifge: test_type = nan_negative ? SLJIT_ORDERED_GREATER_EQUAL : SLJIT_UNORDERED_OR_GREATER_EQUAL;
                break;
            case op_ifgt: test_type = nan_negative ? SLJIT_ORDERED_GREATER : SLJIT_UNORDERED_OR_GREATER;
                break;
            default: test_type = nan_negative ? SLJIT_UNORDERED_OR_LESS_EQUAL : SLJIT_ORDERED_LESS_EQUAL;
                break; /* op_ifle */
        }
    }

    offset = *((s16 *) (ip + 2));
    /* JVM branch offsets are relative to the ifxx opcode itself */
    jumpto = (code_idx + 1) + offset;
    label = (__refer) pairlist_getl(method->pos_2_label, jumpto);
    if (!label) {
        jvm_printf("label not found %s.%s pc: %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), code_idx);
        return 0;
    }

    if (!is_float) {
        /* R0 = value2 (top), R1 = value1 (deeper) */
        _gen_stack_peek_long(C, -2, SLJIT_R0, 0);
        _gen_stack_peek_long(C, -4, SLJIT_R1, 0);
        _gen_stack_size_modify(C, -4);
        _gen_sp_apply(C);
        jump_if_true = sljit_emit_cmp(C, test_type, SLJIT_R1, 0, SLJIT_R0, 0);
    } else if (cmp_op == op_fcmpl || cmp_op == op_fcmpg) {
        /* FR0 = value1, FR1 = value2 */
        _gen_stack_peek_float(C, -2, SLJIT_FR0, 0);
        _gen_stack_peek_float(C, -1, SLJIT_FR1, 0);
        _gen_stack_size_modify(C, -2);
        _gen_sp_apply(C);
        jump_if_true = sljit_emit_fcmp(C, test_type | SLJIT_32, SLJIT_FR0, 0, SLJIT_FR1, 0);
    } else {
        _gen_stack_peek_double(C, -4, SLJIT_FR0, 0);
        _gen_stack_peek_double(C, -2, SLJIT_FR1, 0);
        _gen_stack_size_modify(C, -4);
        _gen_sp_apply(C);
        jump_if_true = sljit_emit_fcmp(C, test_type, SLJIT_FR0, 0, SLJIT_FR1, 0);
    }

    {
        jump_out = sljit_emit_jump(C, SLJIT_JUMP);
    }
    label_true = sljit_emit_label(C);
    {
        /* branch_ip and pc offset follow the ifxx instruction, like the
         * non-fused emitters (current instruction = ifxx position) */
        _gen_jump_to_suspend_check(C, ip + 1, offset);
        jump_away = sljit_emit_jump(C, SLJIT_JUMP);
        pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, jumpto);
    }
    label_out = sljit_emit_label(C);
    sljit_set_label(jump_if_true, label_true);
    sljit_set_label(jump_out, label_out);
    return 1;
}

static s32 _jit_try_emit_i2local_idiv_store(struct sljit_compiler *C, MethodInfo *method, CodeAttribute *ca, s32 code_idx, const u8 *ip, const u8 *end, s32 *consumed) {
    s32 idx_a, idx_b, idx_c, len_a, len_b, len_c;
    const u8 *p = ip;
    if (!_jit_match_iload(p, end, &idx_a, &len_a)) return 0;
    p += len_a;
    if (!_jit_match_iload(p, end, &idx_b, &len_b)) return 0;
    p += len_b;
    if (p >= end || *p != op_idiv) return 0;
    p += 1;
    if (!_jit_match_istore(p, end, &idx_c, &len_c)) return 0;

    *consumed = len_a + len_b + 1 + len_c;
    if (!_jit_fusion_range_safe(method, ca, code_idx, *consumed)) return 0;

    /* this peephole starts at an iload, so the TOS cache may hold live
     * values; the div-by-zero cold block flushes them at COMPILE time
     * via _gen_save_sp_pc_at, and those stores would only execute on
     * the throw path.  Flush here so the normal path sees them too. */
    _gen_tos_flush(C);

    _gen_local_get_int(C, idx_a, SLJIT_R0, 0);
    _gen_local_get_int(C, idx_b, SLJIT_R1, 0);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0, JVM_EXCEPTION_ARRITHMETIC, 0);
    sljit_emit_op0(C, SLJIT_DIV_S32);
    _gen_local_set_int(C, idx_c, SLJIT_R0, 0);

    return 1;
}

static s32 _jit_try_emit_i2local_irem_store(struct sljit_compiler *C, MethodInfo *method, CodeAttribute *ca, s32 code_idx, const u8 *ip, const u8 *end, s32 *consumed) {
    s32 idx_a, idx_b, idx_c, len_a, len_b, len_c;
    const u8 *p = ip;
    if (!_jit_match_iload(p, end, &idx_a, &len_a)) return 0;
    p += len_a;
    if (!_jit_match_iload(p, end, &idx_b, &len_b)) return 0;
    p += len_b;
    if (p >= end || *p != op_irem) return 0;
    p += 1;
    if (!_jit_match_istore(p, end, &idx_c, &len_c)) return 0;

    *consumed = len_a + len_b + 1 + len_c;
    if (!_jit_fusion_range_safe(method, ca, code_idx, *consumed)) return 0;

    /* same cold-block flush hazard as the idiv fusion above */
    _gen_tos_flush(C);

    _gen_local_get_int(C, idx_a, SLJIT_R0, 0);
    _gen_local_get_int(C, idx_b, SLJIT_R1, 0);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0, JVM_EXCEPTION_ARRITHMETIC, 0);
    sljit_emit_op0(C, SLJIT_DIVMOD_S32);
    _gen_local_set_int(C, idx_c, SLJIT_R1, 0);

    return 1;
}

/*
 * The dual TOS register cache covers all constant/load/arithmetic/branch
 * sequences, so the only remaining peephole is the div/rem local fusion
 * (its divide-by-zero throw needs the operands on the stack).
 */
static s32 _jit_try_emit_fusion_peephole(struct sljit_compiler *C, MethodInfo *method, CodeAttribute *ca, s32 code_idx, const u8 *ip, const u8 *end, s32 *consumed) {
    if (_jit_try_emit_i2local_idiv_store(C, method, ca, code_idx, ip, end, consumed)) {
        return 1;
    }
    if (_jit_try_emit_i2local_irem_store(C, method, ca, code_idx, ip, end, consumed)) {
        return 1;
    }
    return 0;
}

static FieldInfo *_jit_compile_resolve_field(JClass *clazz, Runtime *runtime, u16 idx) {
    ConstantFieldRef *cfr = class_get_constant_fieldref(clazz, idx);
    FieldInfo *fi = cfr->fieldInfo;
    if (!fi) {
        fi = find_fieldInfo_by_fieldref(clazz, cfr->item.index, runtime);
        if (fi) {
            cfr->fieldInfo = fi;
        }
    }
    return fi;
}

static void _jit_emit_field_ptr(struct sljit_compiler *C, sljit_s32 this_reg, FieldInfo *fi) {
    //fields are inline: address = this + JVM_OBJECT_BODY_OFFSET + offset_instance
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, this_reg, 0,
                   SLJIT_IMM, (sljit_sw) (JVM_OBJECT_BODY_OFFSET + fi->offset_instance));
}

static void _jit_emit_load_instance_field(struct sljit_compiler *C, FieldInfo *fi, sljit_s32 this_reg, sljit_s32 dst_reg) {
    _jit_emit_field_ptr(C, this_reg, fi);
    if (fi->isrefer) {
        sljit_emit_op1(C, SLJIT_MOV_P, dst_reg, 0, SLJIT_MEM1(SLJIT_R2), 0);
    } else {
        switch (fi->datatype_bytes) {
            case 1:
                sljit_emit_op1(C, SLJIT_MOV_S8, dst_reg, 0, SLJIT_MEM1(SLJIT_R2), 0);
                break;
            case 2:
                if (fi->datatype_idx == DATATYPE_JCHAR) {
                    sljit_emit_op1(C, SLJIT_MOV_U16, dst_reg, 0, SLJIT_MEM1(SLJIT_R2), 0);
                } else {
                    sljit_emit_op1(C, SLJIT_MOV_S16, dst_reg, 0, SLJIT_MEM1(SLJIT_R2), 0);
                }
                break;
            case 4:
                sljit_emit_op1(C, SLJIT_MOV_S32, dst_reg, 0, SLJIT_MEM1(SLJIT_R2), 0);
                break;
            case 8:
                sljit_emit_op1(C, SLJIT_MOV, dst_reg, 0, SLJIT_MEM1(SLJIT_R2), 0);
                break;
            default:
                break;
        }
    }
}

static void _jit_emit_store_instance_field(struct sljit_compiler *C, FieldInfo *fi, sljit_s32 this_reg, sljit_s32 val_reg) {
    _jit_emit_field_ptr(C, this_reg, fi);
    if (fi->isrefer) {
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), 0, val_reg, 0);
    } else {
        switch (fi->datatype_bytes) {
            case 1:
                sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_MEM1(SLJIT_R2), 0, val_reg, 0);
                break;
            case 2:
                if (fi->datatype_idx == DATATYPE_JCHAR) {
                    sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_MEM1(SLJIT_R2), 0, val_reg, 0);
                } else {
                    sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_MEM1(SLJIT_R2), 0, val_reg, 0);
                }
                break;
            case 4:
                sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(SLJIT_R2), 0, val_reg, 0);
                break;
            case 8:
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), 0, val_reg, 0);
                break;
            default:
                break;
        }
    }
}

static void _jit_emit_stack_set_field_value(struct sljit_compiler *C, FieldInfo *fi, s32 stack_off, sljit_s32 val_reg) {
    if (fi->isrefer) {
        _gen_stack_set_ref(C, stack_off, val_reg, 0);
    } else if (fi->datatype_bytes == 8) {
        _gen_stack_set_long(C, stack_off, val_reg, 0);
        _gen_stack_size_modify(C, 1);
    } else {
        _gen_stack_set_int(C, stack_off, val_reg, 0);
    }
}

static void _jit_emit_stack_peek_field_value(struct sljit_compiler *C, FieldInfo *fi, s32 stack_off, sljit_s32 dst_reg) {
    if (fi->isrefer) {
        _gen_stack_peek_ref(C, stack_off, dst_reg, 0);
    } else if (fi->datatype_bytes == 8) {
        _gen_stack_peek_long(C, stack_off, dst_reg, 0);
    } else {
        _gen_stack_peek_int(C, stack_off, dst_reg, 0);
    }
}

static void _jit_emit_push_field_value(struct sljit_compiler *C, FieldInfo *fi, sljit_s32 val_reg) {
    if (fi->isrefer) {
        _gen_stack_push_ref(C, val_reg, 0);
    } else if (fi->datatype_bytes == 8) {
        _gen_stack_push_long(C, val_reg, 0);
    } else {
        _gen_stack_push_int(C, val_reg, 0);
    }
}

static void _jit_emit_local_get_by_field(struct sljit_compiler *C, s32 index, FieldInfo *fi, sljit_s32 dst_reg) {
    if (fi->isrefer) {
        _gen_local_get_ref(C, index, dst_reg, 0);
    } else if (fi->datatype_bytes == 8) {
        _gen_local_get_long(C, index, dst_reg, 0);
    } else {
        _gen_local_get_int(C, index, dst_reg, 0);
    }
}

static s32 _jit_getter_return_matches_field(u8 ret_op, FieldInfo *fi) {
    if (fi->isrefer) {
        return ret_op == op_areturn;
    }
    switch (fi->datatype_bytes) {
        case 8:
            return ret_op == op_lreturn || ret_op == op_dreturn;
        case 4:
            return ret_op == op_ireturn || ret_op == op_freturn;
        case 1:
        case 2:
            return ret_op == op_ireturn;
        default:
            return 0;
    }
}

static s32 _jit_setter_load_matches_field(u8 load_op, FieldInfo *fi) {
    if (fi->isrefer) {
        return load_op == op_aload_1;
    }
    switch (fi->datatype_bytes) {
        case 8:
            if (fi->datatype_idx == DATATYPE_LONG) {
                return load_op == op_lload_1;
            }
            if (fi->datatype_idx == DATATYPE_DOUBLE) {
                return load_op == op_dload_1;
            }
            return load_op == op_lload_1 || load_op == op_dload_1;
        case 4:
            if (fi->datatype_idx == DATATYPE_FLOAT) {
                return load_op == op_fload_1;
            }
            return load_op == op_iload_1 || load_op == op_fload_1;
        case 1:
        case 2:
            return load_op == op_iload_1;
        default:
            return 0;
    }
}

static s32 _jit_try_emit_getfield_ireturn(struct sljit_compiler *C, MethodInfo *method, JClass *clazz, Runtime *runtime, s32 code_idx, const u8 *ip, const u8 *end, s32 *consumed) {
    FieldInfo *fi;
    u16 idx;
    CodeAttribute *ca = method->converted_code;
    if (code_idx != 0 || ca->code_length != 5) {
        return 0;
    }
    if (ip + 4 >= end || ip[0] != op_aload_0 || ip[1] != op_getfield) {
        return 0;
    }
    idx = *((u16 *) (ip + 2));
    fi = _jit_compile_resolve_field(clazz, runtime, idx);
    if (!fi || !_jit_getter_return_matches_field(ip[4], fi)) {
        return 0;
    }
    if (fi->_this_class->status < CLASS_STATUS_CLINITED) {
        class_clinit(fi->_this_class, runtime);
    }
    _gen_local_get_ref(C, 0, SLJIT_R0, 0);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, 0);
    _jit_emit_load_instance_field(C, fi, SLJIT_R0, SLJIT_R0);
    _jit_emit_push_field_value(C, fi, SLJIT_R0);
    _gen_save_sp_ip(C);
    sljit_emit_return(C, SLJIT_MOV, SLJIT_IMM, RUNTIME_STATUS_NORMAL);
    *consumed = 5;
    return 1;
}

static s32 _jit_try_emit_putfield_return(struct sljit_compiler *C, MethodInfo *method, JClass *clazz, Runtime *runtime, s32 code_idx, const u8 *ip, const u8 *end, s32 *consumed) {
    FieldInfo *fi;
    u16 idx;
    CodeAttribute *ca = method->converted_code;
    if (code_idx != 0 || ca->code_length != 6) {
        return 0;
    }
    if (ip + 6 > end || ip[0] != op_aload_0 || ip[2] != op_putfield || ip[5] != op_return) {
        return 0;
    }
    idx = *((u16 *) (ip + 3));
    fi = _jit_compile_resolve_field(clazz, runtime, idx);
    if (!fi || !_jit_setter_load_matches_field(ip[1], fi)) {
        return 0;
    }
    if (fi->_this_class->status < CLASS_STATUS_CLINITED) {
        class_clinit(fi->_this_class, runtime);
    }
    _gen_local_get_ref(C, 0, SLJIT_R0, 0);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, 0);
    _jit_emit_local_get_by_field(C, 1, fi, SLJIT_R1);
    _jit_emit_store_instance_field(C, fi, SLJIT_R0, SLJIT_R1);
    _gen_save_sp_ip(C);
    sljit_emit_return(C, SLJIT_MOV, SLJIT_IMM, RUNTIME_STATUS_NORMAL);
    *consumed = 6;
    return 1;
}

static s32 invokevirtual(Runtime *runtime, s32 idx, s32 opcode) {
    /* The bytecode kind is explicit, including when an interface reference
     * resolves to an inherited declaration or a public Object method. */
    s32 ret = 0;
    ConstantMethodRef *cmr = class_get_constant_method_ref(runtime->clazz, idx);
    RuntimeStack *stack = runtime->stack;
    Instance *ins = getInstanceInStack(cmr, stack);
    if (!ins) {
        _null_throw_exception(stack, runtime);
        return RUNTIME_STATUS_EXCEPTION;
    } else {
        MethodInfo *m = NULL;

        s32 derr = select_dispatch_target(runtime, cmr, ins, (u8) opcode, &m);
        if (derr) {
            push_ref(stack, exception_create_dispatch(derr, runtime));
            return RUNTIME_STATUS_EXCEPTION;
        }
        ret = execute_method_impl(m, runtime);
        if (ret) {
            return ret;
        }
    }
    return RUNTIME_STATUS_NORMAL;
}

enum {
    JIT_ACCESSOR_NONE = 0,
    JIT_ACCESSOR_GETTER,
    JIT_ACCESSOR_SETTER,
};

typedef struct {
    MethodInfo *method;
    FieldInfo *field;
    s32 kind;
} JitAccessorInfo;

/*
 * Re-validate the accessor body when compiling its caller.  is_getter/is_setter
 * is intentionally only a cheap class-load hint; synchronized methods and
 * volatile fields carry semantics which a plain field load/store cannot replace.
 */
static s32 _jit_resolve_accessor(MethodInfo *method, Runtime *runtime, JitAccessorInfo *accessor) {
    CodeAttribute *ca;
    const u8 *code;
    FieldInfo *fi;
    u16 field_idx;

    if (!method || method->is_static || method->is_sync
        || (method->access_flags & ACC_SYNCHRONIZED)
        || !method->converted_code) {
        return 0;
    }

    ca = method->converted_code;
    code = ca->bytecode_for_jit;
    if (!code) {
        return 0;
    }

    if (method->is_getter
        && ca->code_length == 5
        && method->para_slots == 1
        && code[0] == op_aload_0
        && code[1] == op_getfield) {
        field_idx = *((const u16 *) (code + 2));
        fi = _jit_compile_resolve_field(method->_this_class, runtime, field_idx);
        if (!fi || (fi->access_flags & ACC_STATIC) || fi->isvolatile
            || !_jit_getter_return_matches_field(code[4], fi)) {
            return 0;
        }
        accessor->method = method;
        accessor->field = fi;
        accessor->kind = JIT_ACCESSOR_GETTER;
        return 1;
    }

    if (method->is_setter
        && ca->code_length == 6
        && method->para_count_with_this == 2
        && code[0] == op_aload_0
        && code[2] == op_putfield
        && code[5] == op_return) {
        field_idx = *((const u16 *) (code + 3));
        fi = _jit_compile_resolve_field(method->_this_class, runtime, field_idx);
        if (!fi || (fi->access_flags & ACC_STATIC) || fi->isvolatile
            || !_jit_setter_load_matches_field(code[1], fi)) {
            return 0;
        }
        accessor->method = method;
        accessor->field = fi;
        accessor->kind = JIT_ACCESSOR_SETTER;
        return 1;
    }

    return 0;
}

static void _jit_emit_accessor_fast_path(struct sljit_compiler *C, ConstantMethodRef *cmr,
                                         const JitAccessorInfo *accessor) {
    s32 receiver_offset = -1 - cmr->para_slots;

    /* R0 still contains the receiver after the dispatch guard. */
    if (accessor->kind == JIT_ACCESSOR_GETTER) {
        _jit_emit_load_instance_field(C, accessor->field, SLJIT_R0, SLJIT_R1);
        _jit_emit_stack_set_field_value(C, accessor->field, receiver_offset, SLJIT_R1);
    } else {
        s32 value_offset = !accessor->field->isrefer && accessor->field->datatype_bytes == 8 ? -2 : -1;
        _jit_emit_stack_peek_field_value(C, accessor->field, value_offset, SLJIT_R1);
        _jit_emit_store_instance_field(C, accessor->field, SLJIT_R0, SLJIT_R1);
        _gen_stack_size_modify(C, receiver_offset);
    }
}

/*
 * Emit an accessor at its call site.
 *
 * invokevirtual is guarded by the actual vtable MethodInfo pointer.  Subclasses
 * which inherit the accessor take the fast path; every override, including a
 * different trivial accessor, falls back with the operand stack untouched.
 *
 * invokespecial is statically bound and therefore needs no dispatch guard.
 */
static s32 _jit_try_emit_accessor_invoke(struct sljit_compiler *C, JClass *clazz,
                                         Runtime *runtime, u16 idx, s32 is_virtual) {
    ConstantMethodRef *cmr = class_get_constant_method_ref(clazz, idx);
    JitAccessorInfo accessor;
    s32 receiver_offset;
    struct sljit_jump *jump_slow = NULL;
    struct sljit_jump *jump_slow_no_vtable = NULL;
    struct sljit_jump *jump_done;
    struct sljit_label *label_slow;
    struct sljit_label *label_done;

    if (!cmr || !_jit_resolve_accessor(cmr->methodInfo, runtime, &accessor)) {
        return 0;
    }
    if (is_virtual && accessor.method->_vtable_index < 0) {
        return 0;
    }

    if (accessor.field->_this_class->status < CLASS_STATUS_CLINITED) {
        class_clinit(accessor.field->_this_class, runtime);
    }

    receiver_offset = -1 - cmr->para_slots;
    _gen_stack_peek_ref(C, receiver_offset, SLJIT_R0, 0);
    _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0,
                                      SLJIT_IMM, 0,
                                      JVM_EXCEPTION_NULLPOINTER, 0);

    if (!is_virtual) {
        _jit_emit_accessor_fast_path(C, cmr, &accessor);
        return 1;
    }

    /* R1 = receiver class, then its vtable. */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(Instance, mb) + SLJIT_OFFSETOF(MemoryBlock, clazz));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(JClass, vtable));
    jump_slow_no_vtable = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1),
                   sizeof(MethodInfo *) * accessor.method->_vtable_index);
    jump_slow = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R2, 0,
                               SLJIT_IMM, (sljit_sw) accessor.method);

    _jit_emit_accessor_fast_path(C, cmr, &accessor);
    /* Only the fast path performed these stack changes. Commit them here,
     * before emitting the slow path with its untouched input stack. */
    _gen_sp_apply(C);
    jump_done = sljit_emit_jump(C, SLJIT_JUMP);

    label_slow = sljit_emit_label(C);
    _gen_save_sp_ip(C);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0,
                   SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R1, 0, SLJIT_IMM, idx);
    sljit_emit_op1(C, SLJIT_MOV32, SLJIT_R2, 0, SLJIT_IMM, op_invokevirtual);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, 32, 32),
                     SLJIT_IMM, SLJIT_FUNC_ADDR(invokevirtual));
    _gen_load_sp_ip(C);
    _gen_invoke_status_dispatch(C);

    label_done = sljit_emit_label(C);
    sljit_set_label(jump_slow_no_vtable, label_slow);
    sljit_set_label(jump_slow, label_slow);
    sljit_set_label(jump_done, label_done);
    return 1;
}


/* ---------------------- JIT -> JIT direct call ----------------------
 *
 * The fast path reuses a pooled Runtime, links it into the frame chain,
 * publishes the callee frame on the shared stack, and calls the callee's
 * released machine-code entry directly.  Every guard failure falls back to
 * the generic path; the caller must have run _gen_save_sp_ip() exactly once
 * before, so both paths enter with the same flushed TOS / zero sp_pending /
 * published SP+PC state.
 *
 * Two target modes:
 *   target_dynamic == 0: statically bound target (invokestatic /
 *   invokespecial) - the MethodInfo* is baked, lmax / need / clazz / pc are
 *   compile-time constants.
 *   target_dynamic == 1: runtime-resolved target (invokevirtual /
 *   invokeinterface) arriving in SLJIT_R2 from the inline vtable/itable
 *   lookup.  Nothing about the target can be baked: converted_code,
 *   direct_entry, clinit state and the frame geometry are all guarded or
 *   computed at run time.  Slow path is the invokevirtual() C helper.
 */

/* Bound the emitted interface-row walk; larger closures use the C helper. */
#define JIT_ITABLE_SCAN_MAX 64

static s32 _jit_direct_call_target_ok(MethodInfo *m) {
    if (!m || !m->converted_code || m->is_native
        || m->is_sync || (m->access_flags & ACC_SYNCHRONIZED)) {
        return 0;
    }
    return 1;
}

/*
 * Emits guarded fast path + generic fallback.  Returns 1 when the
 * sequence was emitted, 0 when the caller should use the plain generic
 * path instead (the fast path was filtered out at compile time).
 */
static s32 _jit_emit_direct_invoke(struct sljit_compiler *C, MethodInfo *m,
                                   ConstantMethodRef *cmr, Runtime *compile_runtime,
                                   s32 is_special, s32 target_dynamic, s32 opcode, u16 slow_idx) {
    CodeAttribute *ca = m->converted_code;
    s32 arg_slots = m->para_slots; /* includes `this` for instance methods */
    s32 lmax = 0, need_bytes = 0;
    s32 slot_shift = 0;
    /* Includes table checks and the shared direct-call guards. */
    struct sljit_jump *to_slow[20];
    s32 slow_count = 0, i;
    struct sljit_jump *jump_fast_done;
    struct sljit_label *label_slow, *label_fast_done;

    if (ca) {
        /* baked geometry is only meaningful for a declared target with a
         * body; the dynamic path reads the runtime target's own values */
        lmax = ca->max_locals > arg_slots ? ca->max_locals : arg_slots;
        need_bytes = (lmax + ca->max_stack - arg_slots) * (s32) sizeof(StackEntry);
    }
    while ((1 << slot_shift) < (s32) sizeof(StackEntry)) {
        slot_shift++;
    }

    if (target_dynamic) {
        s32 derr, kind;
        /* Resolve before emitting anything: a failed attempt must not leave
         * half a fast path (including an NPE check) in the caller. */
        spin_lock(&compile_runtime->jvm->lock_cloader);
        derr = resolve_dispatch_plan(compile_runtime, cmr, (u8) opcode);
        spin_unlock(&compile_runtime->jvm->lock_cloader);
        if (derr) return 0;
        kind = dispatch_kind_load(cmr);
        if (opcode == op_invokeinterface
            && (!(cmr->symbolic_owner->cff.access_flags & ACC_INTERFACE)
                || kind != DISP_ITABLE)) {
            /* Object-method interface references require a separate
             * membership/public check, supplied by the common selector. */
            return 0;
        }
        /* ---- inline receiver NPE + target lookup ----
         * Leaves R2 = actual MethodInfo* (every miss jumps to the generic
         * slow path below).
         *
         * invokevirtual (plan resolved at JIT compile time):
         *   DISP_VTABLE  -> 3 loads + 2 compares, no cache at all
         *   DISP_ITABLE  -> compile-time link to default method, emit table
         * invokeinterface: immutable symbolic-interface row + slot */
        struct sljit_jump *jump_loop, *jump_hit;
        struct sljit_label *label_loop, *label_hit;

        /* receiver = args slot 0: [SP - A*16 + rvalue_off] */
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(REGISTER_SP),
                       (sljit_sw) (SLJIT_OFFSETOF(StackEntry, rvalue)
                                   - (s64) arg_slots * (s64) sizeof(StackEntry)));
        _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0,
                                          JVM_EXCEPTION_NULLPOINTER, 0);
        /* R1 = receiver class */
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(Instance, mb) + SLJIT_OFFSETOF(MemoryBlock, clazz));

        /* The acquired plan never changes; bake owner/slot into the code. */
        if (kind == DISP_VTABLE) {
            /* vtable slot fetch: 3 loads + 2 compares */
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(JClass, vtable));
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);
            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(JClass, vtable_length));
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_SIG_LESS_EQUAL, SLJIT_R3, 0,
                                                   SLJIT_IMM, (sljit_sw) cmr->disp_slot);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R2),
                           sizeof(MethodInfo *) * cmr->disp_slot);
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);
        } else {
            /* R5 walks interfaces, R2 walks the matching entry in parallel. */
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(JClass, itable));
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R4, 0, SLJIT_IMM, 0);
            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(JClass, itable_length));
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_SIG_LESS_EQUAL, SLJIT_R3, 0,
                                                   SLJIT_IMM, 0);
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_SIG_GREATER, SLJIT_R3, 0,
                                                   SLJIT_IMM, JIT_ITABLE_SCAN_MAX);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R5, 0, SLJIT_MEM1(SLJIT_R4),
                           SLJIT_OFFSETOF(Itable, interfaces));
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R4),
                           SLJIT_OFFSETOF(Itable, entries));
            label_loop = sljit_emit_label(C);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R5), 0);
            jump_hit = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R4, 0,
                                      SLJIT_IMM, (sljit_sw) cmr->disp_owner);
            sljit_emit_op2(C, SLJIT_ADD, SLJIT_R5, 0, SLJIT_R5, 0,
                           SLJIT_IMM, (sljit_sw) sizeof(JClass *));
            sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R2, 0,
                           SLJIT_IMM, (sljit_sw) sizeof(ItableEntry));
            sljit_emit_op2(C, SLJIT_SUB, SLJIT_R3, 0, SLJIT_R3, 0, SLJIT_IMM, 1);
            jump_loop = sljit_emit_cmp(C, SLJIT_SIG_GREATER, SLJIT_R3, 0, SLJIT_IMM, 0);
            /* exhausted: fall through to the generic path */
            to_slow[slow_count++] = sljit_emit_jump(C, SLJIT_JUMP);

            label_hit = sljit_emit_label(C);
            sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R2),
                           SLJIT_OFFSETOF(ItableEntry, slot_count));
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_SIG_LESS_EQUAL, SLJIT_R3, 0,
                                                   SLJIT_IMM, cmr->disp_slot);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R2),
                           SLJIT_OFFSETOF(ItableEntry, status));
            sljit_emit_op1(C, SLJIT_MOV_U8, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R4), cmr->disp_slot);
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R4, 0,
                                                   SLJIT_IMM, ISLOT_OK);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R2),
                           SLJIT_OFFSETOF(ItableEntry, methods));
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R2),
                           sizeof(MethodInfo *) * cmr->disp_slot);
            to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);

            sljit_set_label(jump_loop, label_loop);
            sljit_set_label(jump_hit, label_hit);
        }

        /* R2 = actual target; park it, every later stage reloads it from
         * the native slot */
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_TARGET, SLJIT_R2, 0);
    }

    /* -------- read-only guards (no side effect before the last one) -------- */

    /* 1. released entry, acquire-ordered read.  x86-64 is TSO: an aligned
     * pointer load is already acquire, and an emitted fence costs a full
     * mfence per call.  Weakly-ordered backends take the barrier.
     * A non-NULL direct_entry implies JIT'd + published + non-sync +
     * non-native (see _jit_publish_direct_entry). */
    if (target_dynamic) {
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_TARGET);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(MethodInfo, converted_code));
        /* Cache immutable target metadata in scratch slots that are not
         * otherwise live until the child is linked and the call returns. */
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_CHILD, SLJIT_R1, 0);
        to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1),
                       SLJIT_OFFSETOF(CodeAttribute, jit) + SLJIT_OFFSETOF(struct _Jit, direct_entry));
    } else {
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) m);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(MethodInfo, converted_code));
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(CodeAttribute, jit) + SLJIT_OFFSETOF(struct _Jit, direct_entry));
    }
#if !(defined(SLJIT_CONFIG_X86_64) && SLJIT_CONFIG_X86_64)
    if (sljit_has_cpu_feature(SLJIT_HAS_MEMORY_BARRIER)) {
        /* acquire side of the release-publish in _jit_publish_direct_entry */
        sljit_emit_op0(C, SLJIT_MEMORY_BARRIER);
    }
#endif
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_ENTRY, SLJIT_R2, 0);

    /* 2. target class fully initialized (pending clinit goes the generic
     *    path, which runs class_clinit with the correct no_pause state) */
    if (target_dynamic) {
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(MethodInfo, _this_class));
        sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R3),
                       SLJIT_OFFSETOF(JClass, status));
        to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R0, 0,
                                               SLJIT_IMM, CLASS_STATUS_CLINITED);
    } else {
        sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_R0, 0, SLJIT_MEM0(),
                       (sljit_sw) ((c8 *) m->_this_class + SLJIT_OFFSETOF(JClass, status)));
        to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R0, 0,
                                               SLJIT_IMM, CLASS_STATUS_CLINITED);
    }

    /* 3. thread not stopping: ERROR must come from the callee's own
     *    entry/loop safepoints, never be swallowed by a fresh frame */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_THREADINFO);
    sljit_emit_op1(C, SLJIT_MOV_U8, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(JavaThreadInfo, is_stop));
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0);

    /* 4. runtime pool not empty (empty pool: let the generic path allocate,
     *    the returned frame re-enters the pool for the next hit) */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(JavaThreadInfo, top_runtime));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header));
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);
    /* keep R1 = top_runtime, R2 = child for the commit below */

    /* 5. shared stack capacity: base + (max_locals + max_stack) slots
     *    must stay inside store[0..max_size), including the locals reserve
     *    localvar_init adds between the args and the callee operand stack */
    if (target_dynamic) {
        struct sljit_jump *jump_lmax_ready;
        struct sljit_label *label_lmax_ready;

        /* need = (max(max_locals, arg_slots) + max_stack - arg_slots) * 16,
         * all read from the runtime target's CodeAttribute */
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_CHILD);
        sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(CodeAttribute, max_stack));
        sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(CodeAttribute, max_locals));
        jump_lmax_ready = sljit_emit_cmp(C, SLJIT_SIG_GREATER_EQUAL, SLJIT_R4, 0,
                                         SLJIT_IMM, (sljit_sw) arg_slots);
        sljit_emit_op1(C, SLJIT_MOV, SLJIT_R4, 0, SLJIT_IMM, (sljit_sw) arg_slots);
        label_lmax_ready = sljit_emit_label(C);
        sljit_set_label(jump_lmax_ready, label_lmax_ready);
        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_STATUS, SLJIT_R4, 0);
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R4, 0, SLJIT_R4, 0, SLJIT_R3, 0);
        sljit_emit_op2(C, SLJIT_SUB, SLJIT_R4, 0, SLJIT_R4, 0,
                       SLJIT_IMM, (sljit_sw) arg_slots);
        sljit_emit_op2(C, SLJIT_SHL, SLJIT_R4, 0, SLJIT_R4, 0,
                       SLJIT_IMM, (sljit_sw) slot_shift);

        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_STACK);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R3),
                       SLJIT_OFFSETOF(RuntimeStack, store));
        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R3),
                       SLJIT_OFFSETOF(RuntimeStack, max_size));
        sljit_emit_op2(C, SLJIT_SHL, SLJIT_R3, 0, SLJIT_R3, 0,
                       SLJIT_IMM, (sljit_sw) slot_shift);
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_R3, 0); /* store_end */
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R5, 0, REGISTER_SP, 0, SLJIT_R4, 0);
        to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_SIG_GREATER, SLJIT_R5, 0, SLJIT_R0, 0);
    } else {
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_STACK);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R3),
                       SLJIT_OFFSETOF(RuntimeStack, store));
        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R3),
                       SLJIT_OFFSETOF(RuntimeStack, max_size));
        sljit_emit_op2(C, SLJIT_SHL, SLJIT_R3, 0, SLJIT_R3, 0,
                       SLJIT_IMM, (sljit_sw) slot_shift);
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R4, 0, SLJIT_R4, 0, SLJIT_R3, 0); /* store_end */
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R5, 0, REGISTER_SP, 0,
                       SLJIT_IMM, (sljit_sw) need_bytes);
        to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_SIG_GREATER, SLJIT_R5, 0, SLJIT_R4, 0);
    }

    /* 6. instance receiver non-null - only for the statically bound mode;
     *    virtual/interface emit their NPE inline before the lookup */
    if (is_special) {
        sljit_emit_op2(C, SLJIT_SUB, SLJIT_R4, 0, REGISTER_SP, 0,
                       SLJIT_IMM, (sljit_sw) (arg_slots * (s32) sizeof(StackEntry)));
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R4),
                       SLJIT_OFFSETOF(StackEntry, rvalue));
        to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R4, 0, SLJIT_IMM, 0);
    }

    /* -------- commit: link a pooled child frame (straight-line, no calls,
     * no safepoints; the frame chain and shared SP stay consistent so a GC
     * entering at the callee's entry safepoint sees a complete frame) -------- */

    /* R1 = top_runtime, R2 = child */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R2),
                   SLJIT_OFFSETOF(Runtime, next));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header), SLJIT_R3, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, parent), SLJIT_R3, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, son), SLJIT_IMM, 0);
    if (target_dynamic) {
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_TARGET);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, method), SLJIT_R0, 0);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(MethodInfo, _this_class));
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, clazz), SLJIT_R0, 0);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_CHILD);
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R0),
                       SLJIT_OFFSETOF(CodeAttribute, code));
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, pc), SLJIT_R0, 0);
    } else {
        sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, method),
                       SLJIT_IMM, (sljit_sw) m);
        sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, clazz),
                       SLJIT_IMM, (sljit_sw) m->_this_class);
        sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, pc),
                       SLJIT_IMM, (sljit_sw) ca->code);
    }
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, jdwp_bp_skip_pc),
                   SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, jit_exception_jump_ptr),
                   SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, jit_exception_bc_pos),
                   SLJIT_IMM, 0);
    /* localvar = base = SP - para_slots;  shared sp = base + max(locals, args) */
    sljit_emit_op2(C, SLJIT_SUB, SLJIT_R4, 0, REGISTER_SP, 0,
                   SLJIT_IMM, (sljit_sw) (arg_slots * (s32) sizeof(StackEntry)));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, localvar), SLJIT_R4, 0);
    if (target_dynamic) {
        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R5, 0, SLJIT_MEM1(SLJIT_SP),
                       sizeof(sljit_sw) * LOCAL_CALL_STATUS);
        sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_MEM1(SLJIT_R2),
                       SLJIT_OFFSETOF(Runtime, localvar_slots), SLJIT_R5, 0);
        sljit_emit_op2(C, SLJIT_SHL, SLJIT_R5, 0, SLJIT_R5, 0,
                       SLJIT_IMM, (sljit_sw) slot_shift);
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R4, 0, SLJIT_R4, 0, SLJIT_R5, 0);
    } else {
        sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, localvar_slots),
                       SLJIT_IMM, lmax);
        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R4, 0, SLJIT_R4, 0,
                       SLJIT_IMM, (sljit_sw) (lmax * (s32) sizeof(StackEntry)));
    }
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R5, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_STACK);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R5), SLJIT_OFFSETOF(RuntimeStack, sp), SLJIT_R4, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R3), SLJIT_OFFSETOF(Runtime, son), SLJIT_R2, 0);

    /* the baked target is never needed past the guards: the call takes
     * (runtime, clazz) like a native, both read from the child frame */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_CHILD, SLJIT_R2, 0);

    /* -------- the call itself: JIT -> JIT, native convention
     * (Runtime*, JClass*) -------- */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_CHILD);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(Runtime, clazz));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_ENTRY);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_R2, 0);
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_STATUS, SLJIT_RETURN_REG, 0);

    /* -------- recycle the child (all statuses, before any dispatch) -------- */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_THREADINFO);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(JavaThreadInfo, top_runtime));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_CHILD);
    /* re-read the pool head: the callee may have added runtimes to it */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, next), SLJIT_R3, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header), SLJIT_R2, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R3), SLJIT_OFFSETOF(Runtime, son), SLJIT_IMM, 0);

    /* adopt the callee's shared SP; the pre-call REGISTER_SP is stale and
     * must never be published back over the callee's state */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_STACK);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(RuntimeStack, sp));
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_SP, 0, SLJIT_R1, 0);

    /* -------- status dispatch: NORMAL is the fall-through -------- */
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_STATUS);
    {
        struct sljit_jump *jump_nonnormal, *jump_exception, *jump_normal;
        struct sljit_label *label_nonnormal, *label_exception, *label_normal;

        jump_nonnormal = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R0, 0,
                                        SLJIT_IMM, RUNTIME_STATUS_NORMAL);
        jump_normal = sljit_emit_jump(C, SLJIT_JUMP);

        label_nonnormal = sljit_emit_label(C);
        jump_exception = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R0, 0,
                                        SLJIT_IMM, RUNTIME_STATUS_EXCEPTION);
        /* ERROR / INTERRUPT: no return-value shaping, no catch lookup */
        sljit_emit_return(C, SLJIT_MOV, SLJIT_RETURN_REG, 0);

        label_exception = sljit_emit_label(C);
        {
            /* EXCEPTION: reference pushed by the callee is the shared top;
                      * exception_handle pops it against this (caller) frame */
            _gen_exception_handle(C);
        }

        label_normal = sljit_emit_label(C);
        {
            /* NORMAL: the callee left its result at (shared SP - return_slots)
                      * exactly as execute_method_impl reads it; move it down to base
                      * (the arg slots) and publish the new shared SP.  Source is read
                      * before destination is written: they overlap when
                      * max_locals == para_slots == 0. */
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP),
                           sizeof(sljit_sw) * LOCAL_CALL_CHILD);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(Runtime, localvar)); /* base */
            if (m->return_slots == 1) {
                /* full 16-byte entry: value union + separate reference field */
                sljit_emit_op2(C, SLJIT_SUB, SLJIT_R2, 0, REGISTER_SP, 0,
                               SLJIT_IMM, (sljit_sw) sizeof(StackEntry));
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R2), 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R2),
                               SLJIT_OFFSETOF(StackEntry, rvalue));
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R1), 0, SLJIT_R3, 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R1),
                               SLJIT_OFFSETOF(StackEntry, rvalue), SLJIT_R4, 0);
            } else if (m->return_slots == 2) {
                /* bit-level long/double: low-slot lvalue only, matching
                 * pop_long/push_long; no reference field is touched */
                sljit_emit_op2(C, SLJIT_SUB, SLJIT_R2, 0, REGISTER_SP, 0,
                               SLJIT_IMM, (sljit_sw) (2 * (s32) sizeof(StackEntry)));
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R2), 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R1), 0, SLJIT_R3, 0);
            }
            sljit_emit_op2(C, SLJIT_ADD, REGISTER_SP, 0, SLJIT_R1, 0,
                           SLJIT_IMM, (sljit_sw) (m->return_slots * (s32) sizeof(StackEntry)));
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                           sizeof(sljit_sw) * LOCAL_STACK);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R0),
                           SLJIT_OFFSETOF(RuntimeStack, sp), REGISTER_SP, 0);
        }

        jump_fast_done = sljit_emit_jump(C, SLJIT_JUMP);

        label_slow = sljit_emit_label(C);
        {
            /* generic fallback: full helper semantics */
            if (target_dynamic) {
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                               sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R1, 0, SLJIT_IMM, slow_idx);
                sljit_emit_op1(C, SLJIT_MOV32, SLJIT_R2, 0, SLJIT_IMM, opcode);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, 32, 32),
                                 SLJIT_IMM, SLJIT_FUNC_ADDR(invokevirtual));
            } else {
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) m);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP),
                               sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P),
                                 SLJIT_IMM, SLJIT_FUNC_ADDR(execute_method_impl));
            }
            _gen_load_sp_ip(C);
            _gen_invoke_status_dispatch(C);
        }

        label_fast_done = sljit_emit_label(C);

        sljit_set_label(jump_nonnormal, label_nonnormal);
        sljit_set_label(jump_exception, label_exception);
        sljit_set_label(jump_normal, label_normal);
        sljit_set_label(jump_fast_done, label_fast_done);
    }

    for (i = 0; i < slow_count; i++) {
        sljit_set_label(to_slow[i], label_slow);
    }
    return 1;
}


/* ---------------------- JIT -> native direct call ----------------------
 *
 * Statically bound native targets only (invokestatic / invokespecial).
 * Skips the execute_method_impl native wrapper exactly like the java
 * direct call skips the interpreter dispatch: pooled frame, inline call
 * of the resolved native_func(Runtime*, JClass*), inline status dispatch
 * and return shaping.  A native frame has no CodeAttribute: locals are
 * the argument slots only, the shared SP stays at the call boundary and
 * the native pushes its result / exception reference above it, so the
 * NORMAL shaping formula is the same as the java path.
 *
 * Virtual/interface sites reaching a native target (e.g. array clone)
 * already fall back: the dynamic lookup requires converted_code, which
 * natives do not have.
 */
static s32 _jit_emit_native_direct_invoke(struct sljit_compiler *C, MethodInfo *m, s32 is_special) {
    JClass *target_class = m->_this_class;
    s32 arg_slots = m->para_slots; /* includes `this` for instance natives */
    s32 slot_shift = 0;
    struct sljit_jump *to_slow[6];
    s32 slow_count = 0, i;
    struct sljit_jump *jump_fast_done;
    struct sljit_label *label_slow, *label_fast_done;

    while ((1 << slot_shift) < (s32) sizeof(StackEntry)) {
        slot_shift++;
    }

    /* -------- read-only guards -------- */

    /* 1. resolved native_func (failed resolution keeps the site on the
     *    generic path, which raises NoSuchMethod).  Written once during
     *    the first interpreted call, long before this caller compiles;
     *    a torn/stale read can only be NULL -> slow path. */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM0(),
                   (sljit_sw) ((c8 *) m + SLJIT_OFFSETOF(MethodInfo, native_func)));
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_ENTRY, SLJIT_R0, 0);

    /* 2. declaring class initialized (execute_method_impl clinit-loops
     *    for natives as well) */
    sljit_emit_op1(C, SLJIT_MOV_S8, SLJIT_R0, 0, SLJIT_MEM0(),
                   (sljit_sw) ((c8 *) target_class + SLJIT_OFFSETOF(JClass, status)));
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R0, 0,
                                           SLJIT_IMM, CLASS_STATUS_CLINITED);

    /* 3. thread not stopping */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_THREADINFO);
    sljit_emit_op1(C, SLJIT_MOV_U8, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(JavaThreadInfo, is_stop));
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R1, 0, SLJIT_IMM, 0);

    /* 4. runtime pool not empty */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(JavaThreadInfo, top_runtime));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header));
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R2, 0, SLJIT_IMM, 0);
    /* keep R1 = top_runtime, R2 = child for the commit below */

    /* 5. capacity: a native frame adds no operand region - it only pushes
     *    its return value / exception reference above the call boundary,
     *    at most two slots */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_STACK);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R3),
                   SLJIT_OFFSETOF(RuntimeStack, store));
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R3),
                   SLJIT_OFFSETOF(RuntimeStack, max_size));
    sljit_emit_op2(C, SLJIT_SHL, SLJIT_R3, 0, SLJIT_R3, 0,
                   SLJIT_IMM, (sljit_sw) slot_shift);
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R4, 0, SLJIT_R4, 0, SLJIT_R3, 0); /* store_end */
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R5, 0, REGISTER_SP, 0,
                   SLJIT_IMM, (sljit_sw) (2 * (s32) sizeof(StackEntry)));
    to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_SIG_GREATER, SLJIT_R5, 0, SLJIT_R4, 0);

    /* 6. receiver non-null for instance natives */
    if (is_special) {
        sljit_emit_op2(C, SLJIT_SUB, SLJIT_R4, 0, REGISTER_SP, 0,
                       SLJIT_IMM, (sljit_sw) (arg_slots * (s32) sizeof(StackEntry)));
        sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R4),
                       SLJIT_OFFSETOF(StackEntry, rvalue));
        to_slow[slow_count++] = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R4, 0, SLJIT_IMM, 0);
    }

    /* -------- commit: link a pooled child frame (locals = args only) -------- */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R2),
                   SLJIT_OFFSETOF(Runtime, next));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header), SLJIT_R3, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, parent), SLJIT_R3, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, son), SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, method),
                   SLJIT_IMM, (sljit_sw) m);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, clazz),
                   SLJIT_IMM, (sljit_sw) target_class);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, jdwp_bp_skip_pc),
                   SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, jit_exception_jump_ptr),
                   SLJIT_IMM, 0);
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, jit_exception_bc_pos),
                   SLJIT_IMM, 0);
    /* localvar = base = SP - para_slots; native frames keep the shared SP
     * at the call boundary (localvar_init(para, para) reserves nothing) */
    sljit_emit_op2(C, SLJIT_SUB, SLJIT_R4, 0, REGISTER_SP, 0,
                   SLJIT_IMM, (sljit_sw) (arg_slots * (s32) sizeof(StackEntry)));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, localvar), SLJIT_R4, 0);
    sljit_emit_op1(C, SLJIT_MOV_S16, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, localvar_slots),
                   SLJIT_IMM, arg_slots);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R3), SLJIT_OFFSETOF(Runtime, son), SLJIT_R2, 0);

    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_TARGET, SLJIT_IMM, (sljit_sw) m);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_CHILD, SLJIT_R2, 0);

    /* -------- the call: native_func(Runtime*, JClass*) -------- */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_CHILD);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_IMM, (sljit_sw) target_class);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_ENTRY);
    sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_R2, 0);
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_STATUS, SLJIT_RETURN_REG, 0);

    /* -------- recycle the child (all statuses) -------- */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_THREADINFO);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(JavaThreadInfo, top_runtime));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_CHILD);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R2), SLJIT_OFFSETOF(Runtime, next), SLJIT_R3, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R1),
                   SLJIT_OFFSETOF(Runtime, runtime_pool_header), SLJIT_R2, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_RUNTIME);
    sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R3), SLJIT_OFFSETOF(Runtime, son), SLJIT_IMM, 0);

    /* adopt the native's shared SP */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_STACK);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0),
                   SLJIT_OFFSETOF(RuntimeStack, sp));
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_SP, 0, SLJIT_R1, 0);

    /* -------- status dispatch -------- */
    sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                   sizeof(sljit_sw) * LOCAL_CALL_STATUS);
    {
        struct sljit_jump *jump_nonnormal, *jump_exception, *jump_normal;
        struct sljit_label *label_nonnormal, *label_exception, *label_normal;

        jump_nonnormal = sljit_emit_cmp(C, SLJIT_NOT_EQUAL, SLJIT_R0, 0,
                                        SLJIT_IMM, RUNTIME_STATUS_NORMAL);
        jump_normal = sljit_emit_jump(C, SLJIT_JUMP);

        label_nonnormal = sljit_emit_label(C);
        jump_exception = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_R0, 0,
                                        SLJIT_IMM, RUNTIME_STATUS_EXCEPTION);
        /* ERROR / INTERRUPT: propagate, no reshaping */
        sljit_emit_return(C, SLJIT_MOV, SLJIT_RETURN_REG, 0);

        label_exception = sljit_emit_label(C);
        {
            /* EXCEPTION: the native pushed the reference at the call
                      * boundary (one slot below the adopted SP) with push_ref, i.e.
                      * into the rvalue field; replicate the wrapper's
                      * pop/dispose/push so the reference lands at base+1 for the
                      * caller's handler search */
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP),
                           sizeof(sljit_sw) * LOCAL_CALL_CHILD);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(Runtime, localvar)); /* base */
            sljit_emit_op2(C, SLJIT_SUB, SLJIT_R2, 0, REGISTER_SP, 0,
                           SLJIT_IMM, (sljit_sw) sizeof(StackEntry));
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R2),
                           SLJIT_OFFSETOF(StackEntry, rvalue));
            /* push_ref fills only the rvalue field and the pop side reads
             * rvalue too: the reference must land at base+8, not base+0 */
            sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(StackEntry, rvalue), SLJIT_R2, 0);
            sljit_emit_op2(C, SLJIT_ADD, REGISTER_SP, 0, SLJIT_R1, 0,
                           SLJIT_IMM, (sljit_sw) sizeof(StackEntry));
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                           sizeof(sljit_sw) * LOCAL_STACK);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R0),
                           SLJIT_OFFSETOF(RuntimeStack, sp), REGISTER_SP, 0);
            _gen_exception_handle(C);
        }

        label_normal = sljit_emit_label(C);
        {
            /* NORMAL: result at (shared SP - return_slots), same formula as
                      * the java direct path */
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP),
                           sizeof(sljit_sw) * LOCAL_CALL_CHILD);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R1),
                           SLJIT_OFFSETOF(Runtime, localvar)); /* base */
            if (m->return_slots == 1) {
                sljit_emit_op2(C, SLJIT_SUB, SLJIT_R2, 0, REGISTER_SP, 0,
                               SLJIT_IMM, (sljit_sw) sizeof(StackEntry));
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R2), 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R4, 0, SLJIT_MEM1(SLJIT_R2),
                               SLJIT_OFFSETOF(StackEntry, rvalue));
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R1), 0, SLJIT_R3, 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R1),
                               SLJIT_OFFSETOF(StackEntry, rvalue), SLJIT_R4, 0);
            } else if (m->return_slots == 2) {
                sljit_emit_op2(C, SLJIT_SUB, SLJIT_R2, 0, REGISTER_SP, 0,
                               SLJIT_IMM, (sljit_sw) (2 * (s32) sizeof(StackEntry)));
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R3, 0, SLJIT_MEM1(SLJIT_R2), 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_MEM1(SLJIT_R1), 0, SLJIT_R3, 0);
            }
            sljit_emit_op2(C, SLJIT_ADD, REGISTER_SP, 0, SLJIT_R1, 0,
                           SLJIT_IMM, (sljit_sw) (m->return_slots * (s32) sizeof(StackEntry)));
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP),
                           sizeof(sljit_sw) * LOCAL_STACK);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_R0),
                           SLJIT_OFFSETOF(RuntimeStack, sp), REGISTER_SP, 0);
        }

        jump_fast_done = sljit_emit_jump(C, SLJIT_JUMP);

        label_slow = sljit_emit_label(C);
        {
            /* generic fallback: full execute_method_impl native semantics */
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) m);
            sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP),
                           sizeof(sljit_sw) * LOCAL_RUNTIME);
            sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P),
                             SLJIT_IMM, SLJIT_FUNC_ADDR(execute_method_impl));
            _gen_load_sp_ip(C);
            _gen_invoke_status_dispatch(C);
        }

        label_fast_done = sljit_emit_label(C);

        sljit_set_label(jump_nonnormal, label_nonnormal);
        sljit_set_label(jump_exception, label_exception);
        sljit_set_label(jump_normal, label_normal);
        sljit_set_label(jump_fast_done, label_fast_done);
    }

    for (i = 0; i < slow_count; i++) {
        sljit_set_label(to_slow[i], label_slow);
    }
    return 1;
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

static s32 _jit_fixed_instruction_length(u8 op) {
    switch (op) {
        case op_bipush:
        case op_ldc:
        case op_iload:
        case op_lload:
        case op_fload:
        case op_dload:
        case op_aload:
        case op_istore:
        case op_lstore:
        case op_fstore:
        case op_dstore:
        case op_astore:
        case op_ret:
        case op_newarray:
            return 2;

        case op_sipush:
        case op_ldc_w:
        case op_ldc2_w:
        case op_iinc:
        case op_ifeq:
        case op_ifne:
        case op_iflt:
        case op_ifge:
        case op_ifgt:
        case op_ifle:
        case op_if_icmpeq:
        case op_if_icmpne:
        case op_if_icmplt:
        case op_if_icmpge:
        case op_if_icmpgt:
        case op_if_icmple:
        case op_if_acmpeq:
        case op_if_acmpne:
        case op_goto:
        case op_jsr:
        case op_getstatic:
        case op_putstatic:
        case op_getfield:
        case op_putfield:
        case op_invokevirtual:
        case op_invokespecial:
        case op_invokestatic:
        case op_new:
        case op_anewarray:
        case op_checkcast:
        case op_instanceof:
        case op_ifnull:
        case op_ifnonnull:
            return 3;

        case op_multianewarray:
            return 4;

        case op_invokeinterface:
        case op_invokedynamic:
        case op_goto_w:
        case op_jsr_w:
            return 5;

        default:
            return 1;
    }
}

static const u8 *_jit_next_instruction(const u8 *code, const u8 *ip, const u8 *end) {
    u8 op = *ip;
    if (op == op_wide) {
        if (ip + 1 >= end) return end;
        return ip + ((ip[1] == op_iinc) ? 6 : 4);
    }
    if (op == op_tableswitch || op == op_lookupswitch) {
        s32 pos = 4 - (s32) ((ip - code) % 4);
        const u8 *p = ip + pos;
        if (p + 8 > end) return end;
        if (op == op_tableswitch) {
            s32 low, high;
            if (p + 12 > end) return end;
            low = *((const s32 *) (p + 4));
            high = *((const s32 *) (p + 8));
            if (high < low) return end;
            p += 12 + (high - low + 1) * 4;
        } else {
            s32 count = *((const s32 *) (p + 4));
            if (count < 0) return end;
            p += 8 + count * 8;
        }
        return p <= end ? p : end;
    }
    {
        s32 len = _jit_fixed_instruction_length(op);
        return ip + len <= end ? ip + len : end;
    }
}


static void _jit_local_exclude(u8 *excluded, s32 max_locals, s32 index, s32 slots) {
    s32 i;
    for (i = 0; i < slots; i++) {
        if (index + i >= 0 && index + i < max_locals) {
            excluded[index + i] = 1;
        }
    }
}

/*
 * Pick only locals which are used exclusively by integer bytecodes.  Reference
 * locals are deliberately excluded: the collector scans VM stack slots, not
 * native registers.
 */
static void _jit_select_hot_int_locals(CodeAttribute *ca, s32 selected[2]) {
    s32 max_locals = ca->max_locals;
    s32 *score;
    u8 *excluded;
    const u8 *code = ca->bytecode_for_jit;
    const u8 *end = code + ca->code_length;
    const u8 *ip = code;
    s32 i;

    selected[0] = -1;
    selected[1] = -1;
    if (max_locals <= 0) {
        return;
    }
    score = jvm_calloc(sizeof(s32) * max_locals);
    excluded = jvm_calloc(sizeof(u8) * max_locals);
    if (!score || !excluded) {
        if (score) jvm_free(score);
        if (excluded) jvm_free(excluded);
        return;
    }

    while (ip < end) {
        u8 op = *ip;
        s32 index = -1;
        s32 slots = 1;
        s32 is_int = 0;

        if (op >= op_iload_0 && op <= op_iload_3) {
            index = op - op_iload_0;
            is_int = 1;
        } else if (op >= op_istore_0 && op <= op_istore_3) {
            index = op - op_istore_0;
            is_int = 1;
        } else if (op == op_iload || op == op_istore || op == op_iinc) {
            index = ip[1];
            is_int = 1;
        } else if (op >= op_aload_0 && op <= op_aload_3) {
            index = op - op_aload_0;
        } else if (op >= op_astore_0 && op <= op_astore_3) {
            index = op - op_astore_0;
        } else if (op >= op_fload_0 && op <= op_fload_3) {
            index = op - op_fload_0;
        } else if (op >= op_fstore_0 && op <= op_fstore_3) {
            index = op - op_fstore_0;
        } else if (op >= op_lload_0 && op <= op_lload_3) {
            index = op - op_lload_0;
            slots = 2;
        } else if (op >= op_lstore_0 && op <= op_lstore_3) {
            index = op - op_lstore_0;
            slots = 2;
        } else if (op >= op_dload_0 && op <= op_dload_3) {
            index = op - op_dload_0;
            slots = 2;
        } else if (op >= op_dstore_0 && op <= op_dstore_3) {
            index = op - op_dstore_0;
            slots = 2;
        } else if (op == op_aload || op == op_astore || op == op_fload || op == op_fstore || op == op_ret) {
            index = ip[1];
        } else if (op == op_lload || op == op_lstore || op == op_dload || op == op_dstore) {
            index = ip[1];
            slots = 2;
        } else if (op == op_wide && ip + 3 < end) {
            u8 wide_op = ip[1];
            index = *((const u16 *) (ip + 2));
            if (wide_op == op_iload || wide_op == op_istore || wide_op == op_iinc) {
                is_int = 1;
            } else {
                slots = (wide_op == op_lload || wide_op == op_lstore
                         || wide_op == op_dload || wide_op == op_dstore)
                            ? 2
                            : 1;
            }
        }

        if (index >= 0 && index < max_locals) {
            if (is_int) {
                score[index]++;
            } else {
                _jit_local_exclude(excluded, max_locals, index, slots);
            }
        }
        ip = _jit_next_instruction(code, ip, end);
    }

    for (i = 0; i < max_locals; i++) {
        s32 slot;
        if (excluded[i] || score[i] < 3) continue;
        slot = (selected[0] < 0 || score[i] > score[selected[0]]) ? 0 : 1;
        if (slot == 0) {
            selected[1] = selected[0];
            selected[0] = i;
        } else if (selected[1] < 0 || score[i] > score[selected[1]]) {
            selected[1] = i;
        }
    }

    jvm_free(score);
    jvm_free(excluded);
}


//-----------------------------------------------------------------
//------------------------------  gen jit impl  ----------------------
//-----------------------------------------------------------------

void gen_jit_suspend_check_func() {
    struct sljit_compiler *C = sljit_create_compiler(NULL);
    sljit_set_context(C, 0, 0, JIT_SCRATCH_REGS, JIT_SAVED_REGS, LOCAL_COUNT * sizeof(sljit_sw));

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
        sljit_emit_op1(C, SLJIT_MOV_U8, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(JavaThreadInfo, is_stop));
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


    check_suspend = sljit_generate_code(C, 0, NULL);
    sljit_uw len = sljit_get_generated_code_size(C);
    sljit_free_compiler(C);
    //dump_code(check_suspend, len);
}

/* opcodes the TOS cache understands; everything else flushes first.
 * Excluded on purpose: references (aload/astore/ldc/aconst_null),
 * div/rem (throw needs the operands on the stack), frem/drem and
 * f2i/f2l/d2i/d2l (C callouts), dup/swap/pop (stack shuffles),
 * goto/switch/ifnull/if_acmp (merge points or ref compares),
 * lcmp/fcmp/dcmp (result pushed after helper call). */
static s32 _jit_tos_interested(u8 op) {
    return op == op_nop
           || op == op_iinc
           || (op >= op_iconst_m1 && op <= op_iconst_5)
           || op == op_lconst_0 || op == op_lconst_1
           || (op >= op_fconst_0 && op <= op_fconst_2)
           || op == op_dconst_0 || op == op_dconst_1
           || op == op_bipush || op == op_sipush || op == op_ldc2_w
           || (op >= op_iload && op <= op_dload)
           || (op >= op_iload_0 && op <= op_dload_3)
           || (op >= op_iaload && op <= op_saload)
           || (op >= op_istore && op <= op_dstore)
           || (op >= op_istore_0 && op <= op_dstore_3)
           || (op >= op_iastore && op <= op_sastore && op != op_aastore)
           || (op >= op_iadd && op <= op_dsub)
           || (op >= op_imul && op <= op_dmul)
           || op == op_fdiv || op == op_ddiv
           || (op >= op_ineg && op <= op_dneg)
           || (op >= op_ishl && op <= op_lxor)
           || (op >= op_i2l && op <= op_l2i)
           || op == op_l2f || op == op_l2d
           || op == op_f2d || op == op_d2f
           || (op >= op_i2b && op <= op_i2s)
           || (op >= op_ifeq && op <= op_ifle)
           || (op >= op_if_icmpeq && op <= op_if_icmple);
}

s32 gen_jit_bytecode_func(struct sljit_compiler *C, MethodInfo *method, Runtime *runtime) {
#if JIT_DEBUG
    if (
        0
    ) {
        s32 debug = 1;
    } else {
        return JIT_GEN_ERROR;
    }
#endif


    CodeAttribute *ca = method->converted_code;
    u8 *ip = ca->bytecode_for_jit;
    u8 *end = ca->code_length + ip;
    s32 i;
    if (jit_gen_context) {
        jit_gen_context->jit_code = ca->bytecode_for_jit;
        jit_gen_context->runtime_code = ca->code;
        jit_gen_context->current_ip = ip;
        _jit_select_hot_int_locals(ca, jit_gen_context->hot_local);
    }

    {
        // exception pc need label
        ExceptionTable *e = ca->exception_table;
        for (i = 0; i < ca->exception_table_length; i++) {
            s32 pos = (e + i)->handler_pc;
            pairlist_putl(method->pos_2_label, pos, -1); // save label pos in list
        }
    }
    JClass *clazz = method->_this_class;

    void *genfunc;
    s32 native_local_slots = LOCAL_COUNT;

    /* Start a context(function entry), have 2 arguments.
     * Convention is the native one - (Runtime *runtime, JClass *clazz) -
     * so a compiled java method and a JNI native are the same callable
     * shape (jit_func == java_native_fun) and every caller site emits one
     * uniform icall sequence.  The body does not need the clazz argument;
     * the method identity arrives via runtime->method (set by
     * execute_method_impl and by the direct-call fast paths).
     * Return stays W: this vendored SLJIT validates every emit_return op
     * against the declared width (SLJIT_ARGUMENT_CHECKS), and the body's
     * existing returns are word MOVs.  The status values are 0..3, so the
     * s32 C typedef and the ARGS2(32,..) icalls below read the same eax. */
    sljit_emit_enter(C, 0, SLJIT_ARGS2(W, P, P), JIT_SCRATCH_REGS | SLJIT_ENTER_FLOAT(5), JIT_SAVED_REGS,
                     native_local_slots * sizeof(sljit_sw));

    /* SLJIT_SP is the init address of local var.  S0 is REGISTER_SP, so
     * every runtime-relative load must happen before the SP assignment
     * below; the runtime pointer is mirrored into S1 (whose clazz
     * argument the body never uses) to survive it. */
    //S1 = runtime
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_S1, 0, SLJIT_S0, 0);
    //arr[LOCAL_RUNTIME]= runtime
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME, SLJIT_S1, 0);
    //arr[LOCAL_METHOD]= runtime->method (only consumer: the safepoint
    //helper's interrupt-jump pointer lookup)
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, method));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_METHOD, SLJIT_R0, 0);

    //R0=runtime->stack
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, stack));
    //arr[LOCAL_STACK]= runtime->stack
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK, SLJIT_R0, 0);
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, SLJIT_OFFSETOF(RuntimeStack, sp));
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
    //S0(REGISTER_SP)= runtime->stack->sp  (last runtime-relative load)
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP);
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_SP, 0, SLJIT_MEM1(SLJIT_R0), 0);

    if (jit_gen_context && jit_gen_context->hot_local[0] >= 0) {
        sljit_emit_op1(C, SLJIT_MOV_S32, REGISTER_HOT_LOCAL0, 0, SLJIT_MEM1(REGISTER_LOCALVAR),
                       sizeof(LocalVarItem) * jit_gen_context->hot_local[0] + SLJIT_OFFSETOF(LocalVarItem, ivalue));
    }
    if (jit_gen_context && jit_gen_context->hot_local[1] >= 0) {
        sljit_emit_op1(C, SLJIT_MOV_S32, REGISTER_HOT_LOCAL1, 0, SLJIT_MEM1(REGISTER_LOCALVAR),
                       sizeof(LocalVarItem) * jit_gen_context->hot_local[1] + SLJIT_OFFSETOF(LocalVarItem, ivalue));
    }

    _gen_jump_to_suspend_check(C, ip, -1);
    //S0=sp, S1=localvar, S2/S3=optional hot int locals
#if JIT_DEBUG
    //_debug_gen_print_callstack(C);
    //_debug_gen_print_stack(C);
    sljit_emit_op0(C, SLJIT_NOP);
#endif
    while (ip < end) {
        u8 cur_inst = *ip;
        s32 code_idx = (s32) (ip - ca->bytecode_for_jit);
        if (jit_gen_context) {
            jit_gen_context->current_ip = ip;
        }

        //generate label
        if (pairlist_getl(method->pos_2_label, code_idx)) {
            /* merge point: other paths arrive with an empty cache */
            _gen_tos_flush(C);
            struct sljit_label *label = sljit_emit_label(C);
            pairlist_putl(method->pos_2_label, code_idx, (intptr_t) label);
        }
        if (!_jit_tos_interested(cur_inst)) {
            /* opcode outside the cache-aware set: values must reach
             * their slots before its (memory based) emitter runs */
            _gen_tos_flush(C);
        }
        {
            s32 fused_len = 0;
            if (_jit_try_emit_fusion_peephole(C, method, ca, code_idx, ip, end, &fused_len)) {
                ip += fused_len;
                continue;
            }
            if (_jit_try_emit_getfield_ireturn(C, method, clazz, runtime, code_idx, ip, end, &fused_len)) {
                ip += fused_len;
                continue;
            }
            if (_jit_try_emit_putfield_return(C, method, clazz, runtime, code_idx, ip, end, &fused_len)) {
                ip += fused_len;
                continue;
            }
        }
        switch (cur_inst) {
            case op_nop: {
                sljit_emit_op0(C, SLJIT_NOP);
                ip++;
                break;
            }
            case op_aconst_null: {
                //push_ref(stack, 0);
                _gen_stack_push_ref(C, SLJIT_IMM, (sljit_sw) NULL);
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
                _gen_tos_push_imm(C, DATATYPE_INT, (s32) (cur_inst - op_iconst_0));

                ip++;
                break;
            }
            case op_lconst_0:
            case op_lconst_1: {
                //push_long(stack, value);
                _gen_tos_push_imm(C, DATATYPE_LONG, (s32) (cur_inst - op_lconst_0));
                ip++;
                break;
            }
            case op_fconst_0:
            case op_fconst_1:
            case op_fconst_2: {
                // push_float(stack, value);
                _gen_tos_push_fconst32(C, (f32) (cur_inst - op_fconst_0));
                ip++;
                break;
            }
            case op_dconst_0:
            case op_dconst_1: {
                _gen_tos_push_fconst64(C, (f64) (cur_inst - op_dconst_0));
                ip++;
                break;
            }
            case op_bipush: {
                //push_int(stack, v);
                _gen_tos_push_imm(C, DATATYPE_INT, (s8) ip[1]);
                ip += 2;
                break;
            }
            case op_sipush: {
                // push_int(stack, i);
                _gen_tos_push_imm(C, DATATYPE_INT, *((s16 *) (ip + 1)));
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
                        JClass *cl = classes_load_get_with_clinit(clazz->jloader, class_get_constant_classref(clazz, index)->name, runtime);
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
                    ip += 2;
                } else {
                    ip += 3;
                }

                break;
            }

            case op_ldc2_w: {
                s64 value = class_get_constant_long(clazz, *((u16 *) (ip + 1))); //long or double bits
                if (class_get_constant_item(clazz, *((u16 *) (ip + 1)))->tag == CONSTANT_DOUBLE) {
                    Long2Double l2d;
                    l2d.l = value;
                    _gen_tos_push_fconst64(C, l2d.d);
                } else {
                    _gen_tos_push_imm(C, DATATYPE_LONG, value);
                }
                ip += 3;

                break;
            }


            case op_iload: {
                _gen_tos_load_local(C, DATATYPE_INT, (u8) ip[1]);
                ip += 2;
                break;
            }
            case op_fload: {
                _gen_tos_load_local(C, DATATYPE_FLOAT, (u8) ip[1]);
                ip += 2;
                break;
            }

            case op_aload: {
                s32 index = (u8) ip[1];
                _gen_a_load(C, index);

                ip += 2;
                break;
            }
            case op_lload: {
                _gen_tos_load_local(C, DATATYPE_LONG, (u8) ip[1]);
                ip += 2;
                break;
            }
            case op_dload: {
                _gen_tos_load_local(C, DATATYPE_DOUBLE, (u8) ip[1]);
                ip += 2;
                break;
            }

            case op_iload_0:
            case op_iload_1:
            case op_iload_2:
            case op_iload_3: {
                _gen_tos_load_local(C, DATATYPE_INT, cur_inst - op_iload_0);

                ip++;
                break;
            }
            case op_lload_0:
            case op_lload_1:
            case op_lload_2:
            case op_lload_3: {
                _gen_tos_load_local(C, DATATYPE_LONG, cur_inst - op_lload_0);

                ip++;
                break;
            }
            case op_fload_0:
            case op_fload_1:
            case op_fload_2:
            case op_fload_3: {
                _gen_tos_load_local(C, DATATYPE_FLOAT, cur_inst - op_fload_0);

                ip++;
                break;
            }
            case op_dload_0:
            case op_dload_1:
            case op_dload_2:
            case op_dload_3: {
                _gen_tos_load_local(C, DATATYPE_DOUBLE, cur_inst - op_dload_0);

                ip++;
                break;
            }
            case op_aload_0:
            case op_aload_1:
            case op_aload_2:
            case op_aload_3: {
                _gen_a_load(C, cur_inst - op_aload_0);

                ip++;
                break;
            }
            case op_iaload: {
                _gen_arr_load(C, DATATYPE_INT);
                ip++;
                break;
            }
            case op_faload: {
                _gen_arr_load(C, DATATYPE_FLOAT);
                ip++;
                break;
            }
            case op_laload: {
                _gen_arr_load(C, DATATYPE_LONG);
                ip++;
                break;
            }
            case op_daload: {
                _gen_arr_load(C, DATATYPE_DOUBLE);
                ip++;
                break;
            }
            case op_aaload: {
                _gen_arr_load(C, DATATYPE_REFERENCE);
                ip++;
                break;
            }
            case op_baload: {
                _gen_arr_load(C, DATATYPE_BYTE);
                ip++;
                break;
            }
            case op_caload: {
                _gen_arr_load(C, DATATYPE_JCHAR);
                ip++;
                break;
            }
            case op_saload: {
                _gen_arr_load(C, DATATYPE_SHORT);
                ip++;
                break;
            }
            case op_istore: {
                s32 index = (u8) ip[1];
                if (!_gen_tos_try_store_local(C, DATATYPE_INT, index)) {
                    _gen_tos_flush(C);
                    _gen_i_f_store(C, index);
                }
                ip += 2;
                break;
            }
            case op_fstore: {
                s32 index = (u8) ip[1];
                if (!_gen_tos_try_store_local(C, DATATYPE_FLOAT, index)) {
                    _gen_tos_flush(C);
                    _gen_i_f_store(C, index);
                }
                ip += 2;
                break;
            }
            case op_astore: {
                s32 index = (u8) ip[1];
                _gen_a_store(C, index);
                ip += 2;
                break;
            }
            case op_lstore: {
                s32 index = (u8) ip[1];
                if (!_gen_tos_try_store_local(C, DATATYPE_LONG, index)) {
                    _gen_tos_flush(C);
                    _gen_l_d_store(C, index);
                }
                ip += 2;
                break;
            }
            case op_dstore: {
                s32 index = (u8) ip[1];
                if (!_gen_tos_try_store_local(C, DATATYPE_DOUBLE, index)) {
                    _gen_tos_flush(C);
                    _gen_l_d_store(C, index);
                }
                ip += 2;
                break;
            }
            case op_istore_0:
            case op_istore_1:
            case op_istore_2:
            case op_istore_3: {
                if (!_gen_tos_try_store_local(C, DATATYPE_INT, cur_inst - op_istore_0)) {
                    _gen_tos_flush(C);
                    _gen_i_f_store(C, cur_inst - op_istore_0);
                }
                ip++;
                break;
            }
            case op_lstore_0:
            case op_lstore_1:
            case op_lstore_2:
            case op_lstore_3: {
                if (!_gen_tos_try_store_local(C, DATATYPE_LONG, cur_inst - op_lstore_0)) {
                    _gen_tos_flush(C);
                    _gen_l_d_store(C, cur_inst - op_lstore_0);
                }
                ip++;
                break;
            }
            case op_fstore_0:
            case op_fstore_1:
            case op_fstore_2:
            case op_fstore_3: {
                if (!_gen_tos_try_store_local(C, DATATYPE_FLOAT, cur_inst - op_fstore_0)) {
                    _gen_tos_flush(C);
                    _gen_i_f_store(C, cur_inst - op_fstore_0);
                }
                ip++;
                break;
            }
            case op_dstore_0:
            case op_dstore_1:
            case op_dstore_2:
            case op_dstore_3: {
                if (!_gen_tos_try_store_local(C, DATATYPE_DOUBLE, cur_inst - op_dstore_0)) {
                    _gen_tos_flush(C);
                    _gen_l_d_store(C, cur_inst - op_dstore_0);
                }
                ip++;
                break;
            }
            case op_astore_0:
            case op_astore_1:
            case op_astore_2:
            case op_astore_3: {
                _gen_a_store(C, cur_inst - op_astore_0);
                ip++;
                break;
            }
            case op_fastore:
            case op_iastore: {
                _gen_arr_store(C, DATATYPE_INT);
                ip++;
                break;
            }
            case op_dastore:
            case op_lastore: {
                _gen_arr_store(C, DATATYPE_LONG);
                ip++;
                break;
            }
            case op_aastore: {
                _gen_arr_store(C, DATATYPE_REFERENCE);
                ip++;
                break;
            }
            case op_bastore: {
                _gen_arr_store(C, DATATYPE_BYTE);
                ip++;
                break;
            }
            case op_castore: {
                _gen_arr_store(C, DATATYPE_JCHAR);
                ip++;
                break;
            }
            case op_sastore: {
                _gen_arr_store(C, DATATYPE_SHORT);
                ip++;
                break;
            }
            case op_pop: {
                _gen_stack_size_modify(C, -1);
                ip++;
                break;
            }
            case op_pop2: {
                _gen_stack_size_modify(C, -2);
                ip++;
                break;
            }
            case op_dup: {
                //add1
                _gen_stack_size_modify(C, 1);
                //-2  ==>  -1
                _gen_stack_peek_entry(C, -2, SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, lvalue), SLJIT_MEM1(REGISTER_SP), -1 * sizeof(StackEntry) + SLJIT_OFFSETOF(StackEntry, rvalue));

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

                ip++;
                break;
            }
            case op_iadd: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_ADD);
                ip++;
                break;
            }
            case op_ladd: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_LONG, SLJIT_ADD);
                ip++;
                break;
            }
            case op_fadd: {
                _gen_tos_arith_2op(C, DATATYPE_FLOAT, DATATYPE_FLOAT, SLJIT_ADD_F32);
                ip++;
                break;
            }
            case op_dadd: {
                _gen_tos_arith_2op(C, DATATYPE_DOUBLE, DATATYPE_DOUBLE, SLJIT_ADD_F64);
                ip++;
                break;
            }
            case op_isub: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_SUB);
                ip++;
                break;
            }
            case op_lsub: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_LONG, SLJIT_SUB);
                ip++;
                break;
            }
            case op_fsub: {
                _gen_tos_arith_2op(C, DATATYPE_FLOAT, DATATYPE_FLOAT, SLJIT_SUB_F32);
                ip++;
                break;
            }
            case op_dsub: {
                _gen_tos_arith_2op(C, DATATYPE_DOUBLE, DATATYPE_DOUBLE, SLJIT_SUB_F64);
                ip++;
                break;
            }
            case op_imul: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_MUL);
                ip++;
                break;
            }
            case op_lmul: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_LONG, SLJIT_MUL);
                ip++;
                break;
            }
            case op_fmul: {
                _gen_tos_arith_2op(C, DATATYPE_FLOAT, DATATYPE_FLOAT, SLJIT_MUL_F32);
                ip++;
                break;
            }
            case op_dmul: {
                _gen_tos_arith_2op(C, DATATYPE_DOUBLE, DATATYPE_DOUBLE, SLJIT_MUL_F64);
                ip++;
                break;
            }
            case op_idiv: {
                /* the divide-by-zero throw pops both operands off the stack */
                _gen_tos_flush(C);
                _gen_arith_int_2op(C, SLJIT_DIV_S32);
                ip++;
                break;
            }
            case op_ldiv: {
                _gen_tos_flush(C);
                _gen_arith_long_2op(C, SLJIT_DIV_SW);
                ip++;
                break;
            }
            case op_fdiv: {
                _gen_tos_arith_2op(C, DATATYPE_FLOAT, DATATYPE_FLOAT, SLJIT_DIV_F32);
                ip++;
                break;
            }
            case op_ddiv: {
                _gen_tos_arith_2op(C, DATATYPE_DOUBLE, DATATYPE_DOUBLE, SLJIT_DIV_F64);
                ip++;
                break;
            }
            case op_irem: {
                _gen_tos_flush(C);
                _gen_arith_int_2op(C, SLJIT_DIVMOD_S32);
                ip++;
                break;
            }
            case op_lrem: {
                _gen_tos_flush(C);
                _gen_arith_long_2op(C, SLJIT_DIVMOD_SW);
                ip++;
                break;
            }
            case op_frem: {
                _gen_tos_flush(C);
                _gen_stack_peek_float(C, -2, SLJIT_FR0, 0);
                _gen_stack_peek_float(C, -1, SLJIT_FR1, 0);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(F32, F32, F32), SLJIT_IMM, SLJIT_FUNC_ADDR(fmodf));
                _gen_stack_set_float(C, -2, SLJIT_FR0, 0);
                _gen_stack_size_modify(C, -1);
                ip++;
                break;
            }
            case op_drem: {
                _gen_tos_flush(C);
                _gen_stack_peek_double(C, -4, SLJIT_FR0, 0);
                _gen_stack_peek_double(C, -2, SLJIT_FR1, 0);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(F64, F64, F64), SLJIT_IMM, SLJIT_FUNC_ADDR(fmod));
                _gen_stack_set_double(C, -4, SLJIT_FR0, 0);
                _gen_stack_size_modify(C, -2);
                ip++;
                break;
            }
            case op_ineg: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_INT, DATATYPE_INT, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_op2(C, SLJIT_SUB, dst, 0, SLJIT_IMM, 0, src, 0);
                sljit_emit_op1(C, SLJIT_MOV_S32, dst, 0, dst, 0);
                ip++;
                break;
            }
            case op_lneg: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_LONG, DATATYPE_LONG, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_op2(C, SLJIT_SUB, dst, 0, SLJIT_IMM, 0, src, 0);
                ip++;
                break;
            }
            case op_fneg: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_FLOAT, DATATYPE_FLOAT, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_fop1(C, SLJIT_NEG_F32, dst, 0, src, 0);
                ip++;
                break;
            }
            case op_dneg: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_DOUBLE, DATATYPE_DOUBLE, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_fop1(C, SLJIT_NEG_F64, dst, 0, src, 0);
                ip++;
                break;
            }
            case op_ishl: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_SHL32);
                ip++;
                break;
            }
            case op_lshl: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_INT, SLJIT_SHL);
                ip++;
                break;
            }
            case op_ishr: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_ASHR32);
                ip++;
                break;
            }
            case op_lshr: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_INT, SLJIT_ASHR);
                ip++;
                break;
            }
            case op_iushr: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_LSHR32);
                ip++;
                break;
            }
            case op_lushr: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_INT, SLJIT_LSHR);
                ip++;
                break;
            }
            case op_iand: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_AND);
                ip++;
                break;
            }
            case op_land: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_LONG, SLJIT_AND);
                ip++;
                break;
            }
            case op_ior: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_OR);
                ip++;
                break;
            }
            case op_lor: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_LONG, SLJIT_OR);
                ip++;
                break;
            }
            case op_ixor: {
                _gen_tos_arith_2op(C, DATATYPE_INT, DATATYPE_INT, SLJIT_XOR);
                ip++;
                break;
            }
            case op_lxor: {
                _gen_tos_arith_2op(C, DATATYPE_LONG, DATATYPE_LONG, SLJIT_XOR);
                ip++;
                break;
            }

            case op_iinc: {
                _gen_local_get_int(C, (u8) ip[1], SLJIT_R0, 0);
                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, (s8) ip[2]);
                _gen_local_set_int(C, (u8) ip[1], SLJIT_R0, 0);

                ip += 3;
                break;
            }
            case op_i2l: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_INT, DATATYPE_LONG, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_op1(C, SLJIT_MOV_S32, dst, 0, src, 0);

                ip++;
                break;
            }
            case op_i2f: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_INT, DATATYPE_FLOAT, &src);
                sljit_emit_fop1(C, SLJIT_CONV_F32_FROM_S32, jit_gen_context->tos.v[idx].reg, 0, src, 0);

                ip++;
                break;
            }
            case op_i2d: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_INT, DATATYPE_DOUBLE, &src);
                sljit_emit_fop1(C, SLJIT_CONV_F64_FROM_S32, jit_gen_context->tos.v[idx].reg, 0, src, 0);

                ip++;
                break;
            }
            case op_l2i: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_LONG, DATATYPE_INT, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_op1(C, SLJIT_MOV_S32, dst, 0, src, 0);

                ip++;
                break;
            }
            case op_l2f: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_LONG, DATATYPE_FLOAT, &src);
                sljit_emit_fop1(C, SLJIT_CONV_F32_FROM_SW, jit_gen_context->tos.v[idx].reg, 0, src, 0);

                ip++;
                break;
            }
            case op_l2d: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_LONG, DATATYPE_DOUBLE, &src);
                sljit_emit_fop1(C, SLJIT_CONV_F64_FROM_SW, jit_gen_context->tos.v[idx].reg, 0, src, 0);

                ip++;
                break;
            }
            case op_f2i: {
                _gen_tos_flush(C);
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                _gen_save_sp_ip(C);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS1(32, F32), SLJIT_IMM,
                                 SLJIT_FUNC_ADDR(jvm_float_to_int));
                _gen_load_sp_ip(C);
                _gen_stack_set_int(C, -1, SLJIT_RETURN_REG, 0);

                ip++;
                break;
            }
            case op_f2l: {
                _gen_tos_flush(C);
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                _gen_save_sp_ip(C);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS1(W, F32), SLJIT_IMM,
                                 SLJIT_FUNC_ADDR(jvm_float_to_long));
                _gen_load_sp_ip(C);
                _gen_stack_set_long(C, -1, SLJIT_RETURN_REG, 0);
                _gen_stack_size_modify(C, 1);

                ip++;
                break;
            }
            case op_f2d: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_FLOAT, DATATYPE_DOUBLE, &src);
                sljit_emit_fop1(C, SLJIT_CONV_F64_FROM_F32, jit_gen_context->tos.v[idx].reg, 0, src, 0);

                ip++;
                break;
            }
            case op_d2i: {
                _gen_tos_flush(C);
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                _gen_save_sp_ip(C);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS1(32, F64), SLJIT_IMM,
                                 SLJIT_FUNC_ADDR(jvm_double_to_int));
                _gen_load_sp_ip(C);
                _gen_stack_set_int(C, -2, SLJIT_RETURN_REG, 0);
                _gen_stack_size_modify(C, -1);

                ip++;
                break;
            }
            case op_d2l: {
                _gen_tos_flush(C);
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                _gen_save_sp_ip(C);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS1(W, F64), SLJIT_IMM,
                                 SLJIT_FUNC_ADDR(jvm_double_to_long));
                _gen_load_sp_ip(C);
                _gen_stack_set_long(C, -2, SLJIT_RETURN_REG, 0);

                ip++;
                break;
            }
            case op_d2f: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_DOUBLE, DATATYPE_FLOAT, &src);
                sljit_emit_fop1(C, SLJIT_CONV_F32_FROM_F64, jit_gen_context->tos.v[idx].reg, 0, src, 0);

                ip++;
                break;
            }
            case op_i2b: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_INT, DATATYPE_INT, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_op1(C, SLJIT_MOV_S8, dst, 0, src, 0);

                ip++;
                break;
            }
            case op_i2c: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_INT, DATATYPE_INT, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_op1(C, SLJIT_MOV_U16, dst, 0, src, 0);

                ip++;
                break;
            }
            case op_i2s: {
                sljit_s32 src;
                s32 idx = _gen_tos_prepare_unary(C, DATATYPE_INT, DATATYPE_INT, &src);
                sljit_s32 dst = jit_gen_context->tos.v[idx].reg;
                sljit_emit_op1(C, SLJIT_MOV_S16, dst, 0, src, 0);

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
                if (_gen_fused_cmp_if(C, method, ip, code_idx, end, op_lcmp)) {
                    ip += 4;
                    break;
                }
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

                ip++;
                break;
            }
            case op_fcmpl:
            case op_fcmpg: {
                if (_gen_fused_cmp_if(C, method, ip, code_idx, end, cur_inst)) {
                    ip += 4;
                    break;
                }
                _gen_stack_peek_float(C, -1, SLJIT_FR0, 0);
                _gen_stack_peek_float(C, -2, SLJIT_FR1, 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_IMM, cur_inst);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, 32, F32, F32), SLJIT_IMM, SLJIT_FUNC_ADDR(fcmp));
                _gen_stack_set_int(C, -2, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -1);

                ip++;
                break;
            }
            case op_dcmpl:
            case op_dcmpg: {
                if (_gen_fused_cmp_if(C, method, ip, code_idx, end, cur_inst)) {
                    ip += 4;
                    break;
                }
                _gen_stack_peek_double(C, -2, SLJIT_FR0, 0);
                _gen_stack_peek_double(C, -4, SLJIT_FR1, 0);
                sljit_emit_op1(C, SLJIT_MOV, SLJIT_R0, 0, SLJIT_IMM, cur_inst);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, 32, F64, F64), SLJIT_IMM, SLJIT_FUNC_ADDR(dcmp));
                _gen_stack_set_int(C, -4, SLJIT_R0, 0);
                _gen_stack_size_modify(C, -3);
                ip++;
                break;
            }
            case op_ifeq: {
                _gen_tos_if1(C, method, ip, code_idx, SLJIT_EQUAL);
                ip += 3;
                break;
            }
            case op_ifne: {
                _gen_tos_if1(C, method, ip, code_idx, SLJIT_NOT_EQUAL);
                ip += 3;
                break;
            }
            case op_iflt: {
                _gen_tos_if1(C, method, ip, code_idx, SLJIT_SIG_LESS);
                ip += 3;
                break;
            }
            case op_ifge: {
                _gen_tos_if1(C, method, ip, code_idx, SLJIT_SIG_GREATER_EQUAL);
                ip += 3;
                break;
            }
            case op_ifgt: {
                _gen_tos_if1(C, method, ip, code_idx, SLJIT_SIG_GREATER);
                ip += 3;
                break;
            }
            case op_ifle: {
                _gen_tos_if1(C, method, ip, code_idx, SLJIT_SIG_LESS_EQUAL);
                ip += 3;
                break;
            }
            case op_if_icmpeq: {
                _gen_tos_if2(C, method, ip, code_idx, SLJIT_EQUAL);
                ip += 3;
                break;
            }
            case op_if_icmpne: {
                _gen_tos_if2(C, method, ip, code_idx, SLJIT_NOT_EQUAL);
                ip += 3;
                break;
            }
            case op_if_icmplt: {
                _gen_tos_if2(C, method, ip, code_idx, SLJIT_SIG_LESS);
                ip += 3;
                break;
            }
            case op_if_icmpge: {
                _gen_tos_if2(C, method, ip, code_idx, SLJIT_SIG_GREATER_EQUAL);
                ip += 3;
                break;
            }
            case op_if_icmpgt: {
                _gen_tos_if2(C, method, ip, code_idx, SLJIT_SIG_GREATER);
                ip += 3;
                break;
            }
            case op_if_icmple: {
                _gen_tos_if2(C, method, ip, code_idx, SLJIT_SIG_LESS_EQUAL);
                ip += 3;
                break;
            }
            case op_if_acmpeq: {
                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_stack_peek_ref(C, -2, SLJIT_R1, 0);
                _gen_stack_size_modify(C, -2);
                _gen_sp_apply(C);
                _gen_cmp_reg2(C, method, ip, code_idx, SLJIT_R0, SLJIT_R1, SLJIT_EQUAL);
                ip += 3;
                break;
            }
            case op_if_acmpne: {
                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_stack_peek_ref(C, -2, SLJIT_R1, 0);
                _gen_stack_size_modify(C, -2);
                _gen_sp_apply(C);
                _gen_cmp_reg2(C, method, ip, code_idx, SLJIT_R0, SLJIT_R1, SLJIT_NOT_EQUAL);
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

                ip += 2;
                break;
            }


            case op_tableswitch: {
                s32 pos = 0;
                pos = (s32) (4 - ((((u64) (intptr_t) ip) - (u64) (intptr_t) (ca->bytecode_for_jit)) % 4)); //4 byte对齐


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
                _gen_sp_apply(C);
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
                        sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_MEM1(SLJIT_R0), SLJIT_OFFSETOF(struct V2PTable, jump_ptr));
                    }
                }
                label_default = sljit_emit_label(C);
                {
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
                pos = (s32) (4 - ((((u64) (intptr_t) ip) - (u64) (intptr_t) (ca->bytecode_for_jit)) % 4)); //4 byte对齐

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
                _gen_sp_apply(C);
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
                    {
                        //found left
                        sljit_emit_op1(C, SLJIT_MOV_S32, SLJIT_R2, 0, SLJIT_MEM1(SLJIT_R1), SLJIT_OFFSETOF(struct V2PTable, bc_pos));
                        sljit_emit_op2(C, SLJIT_SUB, SLJIT_R2, 0, SLJIT_R2, 0, SLJIT_IMM, code_idx);
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
                    /* numeric static: load straight into the cache */
                    u8 cache_dt;
                    s32 idx2;
                    sljit_s32 dst;
                    if (fi->datatype_bytes == 8) {
                        cache_dt = (fi->datatype_idx == DATATYPE_DOUBLE) ? DATATYPE_DOUBLE : DATATYPE_LONG;
                    } else if (fi->datatype_bytes == 4 && fi->datatype_idx == DATATYPE_FLOAT) {
                        cache_dt = DATATYPE_FLOAT;
                    } else {
                        cache_dt = DATATYPE_INT;
                    }
                    idx2 = _gen_tos_reserve(C, cache_dt);
                    dst = jit_gen_context->tos.v[idx2].reg;
                    switch (fi->datatype_bytes) {
                        case 4: {
                            if (cache_dt == DATATYPE_FLOAT) {
                                sljit_emit_fop1(C, SLJIT_MOV_F32, dst, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            } else {
                                sljit_emit_op1(C, SLJIT_MOV_S32, dst, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            }
                            break;
                        }
                        case 1: {
                            sljit_emit_op1(C, SLJIT_MOV_S8, dst, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            break;
                        }
                        case 8: {
                            if (cache_dt == DATATYPE_DOUBLE) {
                                sljit_emit_fop1(C, SLJIT_MOV_F64, dst, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            } else {
                                sljit_emit_op1(C, SLJIT_MOV, dst, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            }
                            break;
                        }
                        case 2: {
                            if (fi->datatype_idx == DATATYPE_JCHAR) {
                                sljit_emit_op1(C, SLJIT_MOV_U16, dst, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            } else {
                                sljit_emit_op1(C, SLJIT_MOV_S16, dst, 0, SLJIT_MEM0(), (sljit_sw) ptr);
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                    _gen_stack_size_modify(C, _tos_slots(cache_dt));
                }

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
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, -1);

                //field address = ins + JVM_OBJECT_BODY_OFFSET + fi->offset_instance
                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R0, 0,
                               SLJIT_IMM, (sljit_sw) (JVM_OBJECT_BODY_OFFSET + fi->offset_instance));


                if (fi->isrefer) {
                    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_R2), 0);
                    _gen_stack_set_ref(C, -1, SLJIT_R0, 0);
                } else {
                    /* numeric field: pop this, load straight into the cache */
                    u8 cache_dt;
                    s32 idx2;
                    sljit_s32 dst;
                    if (fi->datatype_bytes == 8) {
                        cache_dt = (fi->datatype_idx == DATATYPE_DOUBLE) ? DATATYPE_DOUBLE : DATATYPE_LONG;
                    } else if (fi->datatype_bytes == 4 && fi->datatype_idx == DATATYPE_FLOAT) {
                        cache_dt = DATATYPE_FLOAT;
                    } else {
                        cache_dt = DATATYPE_INT;
                    }
                    idx2 = _gen_tos_reserve(C, cache_dt);
                    dst = jit_gen_context->tos.v[idx2].reg;
                    switch (fi->datatype_bytes) {
                        case 4: {
                            if (cache_dt == DATATYPE_FLOAT) {
                                sljit_emit_fop1(C, SLJIT_MOV_F32, dst, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            } else {
                                sljit_emit_op1(C, SLJIT_MOV_S32, dst, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            }
                            break;
                        }
                        case 1: {
                            sljit_emit_op1(C, SLJIT_MOV_S8, dst, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            break;
                        }
                        case 8: {
                            if (cache_dt == DATATYPE_DOUBLE) {
                                sljit_emit_fop1(C, SLJIT_MOV_F64, dst, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            } else {
                                sljit_emit_op1(C, SLJIT_MOV, dst, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            }
                            break;
                        }
                        case 2: {
                            if (fi->datatype_idx == DATATYPE_JCHAR) {
                                sljit_emit_op1(C, SLJIT_MOV_U16, dst, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            } else {
                                sljit_emit_op1(C, SLJIT_MOV_S16, dst, 0, SLJIT_MEM1(SLJIT_R2), 0);
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                    /* pop this, push the cached value */
                    _gen_stack_size_modify(C, -1 + _tos_slots(cache_dt));
                }
                //ip
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
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, -stack_size);


                //field address = ins + JVM_OBJECT_BODY_OFFSET + fi->offset_instance
                sljit_emit_op2(C, SLJIT_ADD, SLJIT_R2, 0, SLJIT_R0, 0,
                               SLJIT_IMM, (sljit_sw) (JVM_OBJECT_BODY_OFFSET + fi->offset_instance));

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
                ip += 3;
                break;
            }

            case op_invokevirtual:
            case op_invokeinterface: {
                u16 invoke_idx = *((u16 *) (ip + 1));
                if (cur_inst == op_invokevirtual
                    && _jit_try_emit_accessor_invoke(C, clazz, runtime, invoke_idx, 1)) {
                    ip += 3;
                    break;
                }
                _gen_save_sp_ip(C);

#if !(_JVM_DEBUG_METHOD_PROFILE || _JVM_DEBUG_SLOW_CALL_PROFILE)
                /* profile builds must keep every call inside the C helpers */
                if (getenv("MINI_JVM_NO_JIT_DIRECT") == NULL) {
                    ConstantMethodRef *cmr = class_get_constant_method_ref(clazz, invoke_idx);
                    /* table dispatch: receiver NPE, then a vtable slot fetch
                     * or an itable row scan keyed by the compile-time
                     * resolved dispatch plan.  The declared method's
                     * descriptor provides para/return geometry; the
                     * runtime target's own metadata provides the rest, so
                     * abstract-declared sites work too. */
                    if (cmr && cmr->methodInfo
                        && _jit_emit_direct_invoke(C, cmr->methodInfo, cmr, runtime, 0, 1,
                                                   cur_inst, invoke_idx)) {
                        ip += (cur_inst == op_invokevirtual) ? 3 : 5;
                        break;
                    }
                }
#endif

                // The method described by this CMR has different methods for different instances
                // s32 _gen_invokevirtual(Runtime *runtime, u16 idx)
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_op1(C, SLJIT_MOV_U16, SLJIT_R1, 0, SLJIT_IMM, invoke_idx);
                sljit_emit_op1(C, SLJIT_MOV32, SLJIT_R2, 0, SLJIT_IMM, cur_inst);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, 32, 32), SLJIT_IMM, SLJIT_FUNC_ADDR(invokevirtual));
                _gen_load_sp_ip(C);
                _gen_invoke_status_dispatch(C);

                if (cur_inst == op_invokevirtual) {
                    ip += 3;
                } else {
                    ip += 5;
                }
                break;
            }


            case op_invokespecial:
            case op_invokestatic: {
                u16 invoke_idx = *((u16 *) (ip + 1));
                if (cur_inst == op_invokespecial
                    && _jit_try_emit_accessor_invoke(C, clazz, runtime, invoke_idx, 0)) {
                    ip += 3;
                    break;
                }
                _gen_save_sp_ip(C);

                ConstantMethodRef *cmr = class_get_constant_method_ref(clazz, invoke_idx);
                MethodInfo *m = cmr->methodInfo;

#if !(_JVM_DEBUG_METHOD_PROFILE || _JVM_DEBUG_SLOW_CALL_PROFILE)
                /* profile builds must keep every call inside execute_method_impl */
                if (getenv("MINI_JVM_NO_JIT_DIRECT") == NULL) {
                    if (m && m->is_native && !m->is_sync
                        && !(m->access_flags & ACC_SYNCHRONIZED)
                        && _jit_emit_native_direct_invoke(C, m, cur_inst == op_invokespecial)) {
                        ip += 3;
                        break;
                    }
                    if (_jit_direct_call_target_ok(m)
                        && _jit_emit_direct_invoke(C, m, NULL, runtime, cur_inst == op_invokespecial, 0, -1, 0)) {
                        ip += 3;
                        break;
                    }
                }
#endif

                //R0 = method
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_IMM, (sljit_sw) m);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(execute_method_impl));
                _gen_load_sp_ip(C);
                _gen_invoke_status_dispatch(C);

                ip += 3;
                break;
            }

            case op_invokedynamic: {
                _gen_save_sp_ip(C);

                s32 idx = *((u16 *) (ip + 1));

                ConstantInvokeDynamic *cid = class_get_invoke_dynamic(clazz, idx);
                BootstrapMethod *bootMethod = &clazz->bootstrapMethodAttr->bootstrap_methods[cid->bootstrap_method_attr_index]; //Boot

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
                _gen_invoke_status_dispatch(C);

                ip += 5;
                break;
            }


            case op_new: {
                s32 idx = *((u16 *) (ip + 1));

                ConstantClassRef *ccf = class_get_constant_classref(clazz, idx);
                if (!ccf->clazz) {
                    Utf8String *clsName = class_get_utf8_string(clazz, ccf->stringIndex);
                    ccf->clazz = classes_load_get_with_clinit(clazz->jloader, clsName, runtime);
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
                    _gen_exception_check_throw_handle(C, SLJIT_EQUAL,
                                                      SLJIT_RETURN_REG, 0,
                                                      SLJIT_IMM, 0,
                                                      JVM_ERROR_OUTOFMEMORY, 0);
                    _gen_stack_push_ref(C, SLJIT_RETURN_REG, 0);
                } else {
                    return JIT_GEN_ERROR;
                }

                ip += 3;
                break;
            }


            case op_newarray: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_SIG_LESS, SLJIT_R0, 0,
                                                  SLJIT_IMM, 0,
                                                  JVM_EXCEPTION_NEGATIVEARRAYSIZE, -1);
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
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL,
                                                  SLJIT_RETURN_REG, 0,
                                                  SLJIT_IMM, 0,
                                                  JVM_ERROR_OUTOFMEMORY, -1);
                _gen_stack_set_ref(C, -1, SLJIT_RETURN_REG, 0);

                ip += 2;
                break;
            }

            case op_anewarray: {
                _gen_stack_peek_int(C, -1, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_SIG_LESS, SLJIT_R0, 0,
                                                  SLJIT_IMM, 0,
                                                  JVM_EXCEPTION_NEGATIVEARRAYSIZE, -1);
                _gen_save_sp_ip(C);

                s32 idx = *((u16 *) (ip + 1));
                JClass *arr_class = pairlist_get(clazz->arr_class_type, (__refer) (intptr_t) idx);

                if (!arr_class) {
                    //cache to speed
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
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL,
                                                  SLJIT_RETURN_REG, 0,
                                                  SLJIT_IMM, 0,
                                                  JVM_ERROR_OUTOFMEMORY, -1);
                _gen_stack_set_ref(C, -1, SLJIT_RETURN_REG, 0);

                ip += 3;

                break;
            }

            case op_arraylength: {
                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, -1);

                _gen_stack_peek_ref(C, -1, SLJIT_R0, 0);
                _gen_stack_set_int(C, -1, SLJIT_MEM1(SLJIT_R0), (sljit_sw) JVM_ARRAY_LENGTH_OFFSET);

                ip++;
                break;
            }


            case op_athrow: {
                _gen_exception_handle(C);

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

                ip += 3;
                break;
            }

            case op_monitorenter: {
                // =====================================================================
                //                Instance *ins = (Instance *) pop_ref(stack);
                //                jthread_lock(&ins->mb, runtime);
                // =====================================================================

                _gen_stack_pop_ref(C, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, 0);
                /* monitor acquisition may block or fail; publish the
                 * consumed operand stack and current bytecode before the
                 * call, as the interpreter does before entering jthread_lock. */
                _gen_save_sp_ip(C);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(jthread_lock));

                /* jthread_lock returns zero on success, but can return -1
                 * (for example monitor allocation failure) or ERROR after
                 * Thread.stop while waiting. Match interpreter semantics:
                 * propagate ERROR and never execute the protected region
                 * without owning its monitor. */
                struct sljit_jump *jump_lock_ok;
                struct sljit_label *label_lock_ok;
                jump_lock_ok = sljit_emit_cmp(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0,
                                              SLJIT_IMM, RUNTIME_STATUS_NORMAL);
                {
                    sljit_emit_op1(C, SLJIT_MOV, SLJIT_RETURN_REG, 0,
                                   SLJIT_IMM, RUNTIME_STATUS_ERROR);
                    sljit_emit_return(C, SLJIT_MOV, SLJIT_RETURN_REG, 0);
                }
                label_lock_ok = sljit_emit_label(C);
                sljit_set_label(jump_lock_ok, label_lock_ok);
                _gen_load_sp_ip(C);

                ip++;
                break;
            }
            case op_monitorexit: {
                // =====================================================================
                //                Instance *ins = (Instance *) pop_ref(stack);
                //                jthread_unlock(&ins->mb, runtime);
                // =====================================================================

                _gen_stack_pop_ref(C, SLJIT_R0, 0);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0, JVM_EXCEPTION_NULLPOINTER, 0);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS2(32, P, P), SLJIT_IMM, SLJIT_FUNC_ADDR(jthread_unlock));

                ip++;
                break;
            }

            case op_wide: {
                ip++;

                cur_inst = *ip;
                switch (cur_inst) {
                    case op_iload:
                    case op_fload: {
                        _gen_i_f_load(C, *((u16 *) (ip + 1)));

                        ip += 3;
                        break;
                    }
                    case op_aload: {
                        _gen_a_load(C, *((u16 *) (ip + 1)));

                        ip += 3;
                        break;
                    }
                    case op_lload:
                    case op_dload: {
                        _gen_l_d_load(C, *((u16 *) (ip + 1)));

                        ip += 3;
                        break;
                    }
                    case op_istore:
                    case op_fstore: {
                        _gen_i_f_store(C, *((u16 *) (ip + 1)));

                        ip += 3;
                        break;
                    }
                    case op_astore: {
                        _gen_a_store(C, *((u16 *) (ip + 1)));

                        ip += 3;
                        break;
                    }
                    case op_lstore:
                    case op_dstore: {
                        _gen_l_d_store(C, *((u16 *) (ip + 1)));

                        ip += 3;
                        break;
                    }
                    case op_ret: {
                        //__refer addr = localvar_getRefer(runtime->localvar, *((u16 *) (ip + 1)));
                        // ip = (u8 *) addr;

                        _gen_local_get_ref(C, *((u16 *) (ip + 1)), SLJIT_R0, 0);
                        sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_R0, 0);

                        return JIT_GEN_ERROR;

                        ip += 3;
                        break;
                    }
                    case op_iinc: {
                        s32 idx = *((u16 *) (ip + 1));
                        s32 v = *((s16 *) (ip + 3));
                        //runtime->localvar[*((u16 *) (ip + 1))].ivalue += *((s16 *) (ip + 3));
                        _gen_local_get_int(C, idx, SLJIT_R0, 0);
                        sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, v);
                        _gen_local_set_int(C, idx, SLJIT_R0, 0);

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


                _gen_save_sp_ip(C);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R1, 0, SLJIT_IMM, (sljit_sw) desc);
                sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R2, 0, SLJIT_IMM, count);
                sljit_emit_icall(C, SLJIT_CALL, SLJIT_ARGS3(32, P, P, 32), SLJIT_IMM, SLJIT_FUNC_ADDR(multiarray));
                _gen_load_sp_ip(C);
                _gen_exception_check_throw_handle(C, SLJIT_EQUAL, SLJIT_RETURN_REG, 0,
                                                  SLJIT_IMM, RUNTIME_STATUS_EXCEPTION, -1, 0);

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
                _gen_sp_apply(C);

                struct sljit_jump *jump_if_true, *jump_out, *jump_away;
                struct sljit_label *label_out, *label_true;
                //if R0 == 0 then jump to equ_0
                jump_if_true = sljit_emit_cmp(C, cur_inst == op_ifnull ? SLJIT_EQUAL : SLJIT_NOT_EQUAL, SLJIT_R0, 0, SLJIT_IMM, 0);
                {
                    jump_out = sljit_emit_jump(C, SLJIT_JUMP);
                }
                label_true = sljit_emit_label(C);
                {
                    _gen_jump_to_suspend_check(C, ip, offset);
                    jump_away = sljit_emit_jump(C, SLJIT_JUMP);
                    pairlist_putl(method->jump_2_pos, (s64) (intptr_t) jump_away, code_idx + offset);
                }
                label_out = sljit_emit_label(C);
                //
                sljit_set_label(jump_if_true, label_true);
                sljit_set_label(jump_out, label_out);

                ip += 3;
                break;
            }

            case op_breakpoint: {
                ip += 1;
                break;
            }


            case op_goto_w: {
                s32 offset = *((s32 *) (ip + 1));
                _gen_goto(C, method, code_idx, offset);

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
    } //end while

    //interrupt detected,then return
    struct sljit_label *label_interrupt_handle = sljit_emit_label(C);
    {
        // The taken safepoint path already published SP, PC and hot locals.
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
    genfunc = sljit_generate_code(C, 0, NULL);
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

    /* freeze label addresses into pos_2_label: the pairlist outlives this
     * function, but sljit_label structs die with the compiler in
     * construct_jit(); OSR entry generation reads these values later */
    {
        s32 k;
        for (k = 0; k < method->pos_2_label->count; k++) {
            Pair p = pairlist_get_pair(method->pos_2_label, k);
            if (p.rightl > 0) {
                pairlist_putl(method->pos_2_label, p.leftl,
                              (intptr_t) sljit_get_label_addr((struct sljit_label *) (intptr_t) p.rightl));
            }
        }
    }

    ca->jit.len = (s32) sljit_get_generated_code_size(C);

    //Execute code
    ca->jit.func = (jit_func) genfunc;
    runtime->jvm->collector->jit_heap_size += ca->jit.len;
#if _JVM_DEBUG_LOG_LEVEL > 2
    jvm_printf("jit compile method %s.%s%s ,func length:%d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), utf8_cstr(method->descriptor), ca->jit.len);
#endif

    return JIT_GEN_SUCCESS;
}


/*
 * Release-publish the machine-code entry for JIT->JIT direct calls.
 * gen_jit_bytecode_func() has fully initialized func/len/switch/exception
 * metadata (and SLJIT has synced its code cache) before returning, so a
 * full barrier + plain store orders the publication; the generated call
 * sites read the slot acquire-ordered.  Synchronized methods keep the
 * generic entry: their monitor protocol lives in execute_method_impl.
 */
static void _jit_publish_direct_entry(MethodInfo *method) {
    if (method->is_sync || (method->access_flags & ACC_SYNCHRONIZED)) {
        return;
    }
    MEMORY_BARRIER();
    method->converted_code->jit.direct_entry = (__refer) method->converted_code->jit.func;
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
    struct sljit_compiler *C = sljit_create_compiler(NULL);
    if (!C) {
        ca->jit.state = JIT_GEN_ERROR;
        return;
    }
    ca->jit.state = JIT_GEN_COMPILING;
    {
        JitGenContext context;
        JitGenContext *previous_context = jit_gen_context;
        memset(&context, 0, sizeof(context));
        context.hot_local[0] = -1;
        context.hot_local[1] = -1;
        jit_gen_context = &context;
        ca->jit.state = gen_jit_bytecode_func(C, method, runtime);
        jit_gen_context = previous_context;
        if (ca->jit.state == JIT_GEN_SUCCESS) {
            /* OSR trampolines must preload the same hot-int-local
             * registers the body's prologue loads */
            ca->jit.hot_local[0] = (s16) context.hot_local[0];
            ca->jit.hot_local[1] = (s16) context.hot_local[1];
        }
    }

    if (ca->jit.state == JIT_GEN_SUCCESS) {
        s32 debug = 1;
        method->is_jit = 1;
        _jit_publish_direct_entry(method);
    }
#if(JIT_CODE_DUMP)
    if (ca->jit.state == JIT_GEN_SUCCESS) {
        c8 path[300];
        c8 cname[128];
        snprintf(cname, sizeof(cname), "%s", utf8_cstr(method->_this_class->name));
        for (c8 *p = cname; *p; p++) if (*p == '/') *p = '_';
        snprintf(path, sizeof(path), "d:/tmp/jitdump/%s__%s.bin", cname, utf8_cstr(method->name));
        FILE *fp = fopen(path, "wb");
        if (fp) {
            fwrite(ca->jit.func, ca->jit.len, 1, fp);
            fclose(fp);
        }
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

void jit_destroy(Jit *jit) {
    while (jit->switchtable) {
        SwitchTable *tmp = jit->switchtable->next;
        if (jit->switchtable->table) {
            jvm_free(jit->switchtable->table); // 先释放table数组
        }
        jvm_free(jit->switchtable);
        jit->switchtable = tmp;
    }

    if (jit->osr_entry_list) {
        s32 i;
        for (i = 0; i < jit->osr_entry_list->count; i++) {
            Pair p = pairlist_get_pair(jit->osr_entry_list, i);
            if (p.right) {
                sljit_free_code((void *) (intptr_t) p.rightl, NULL);
            }
        }
        pairlist_destroy(jit->osr_entry_list);
        jit->osr_entry_list = NULL;
    }

    if (jit->ex_jump_table) {
        jvm_free(jit->ex_jump_table);
        jit->ex_jump_table = NULL;
    }

    if (jit->func) {
        /* retract the published direct-call entry before the code memory
         * goes away; live callers of this class cannot exist at unload */
        jit->direct_entry = NULL;
        sljit_free_code(jit->func, NULL);
    }
}

void jit_set_exception_jump_addr(Runtime *runtime, CodeAttribute *ca, s32 index) {
    if (ca->jit.ex_jump_table) {
        runtime->jit_exception_bc_pos = ca->jit.ex_jump_table[index].bc_pos;
        runtime->jit_exception_jump_ptr = ca->jit.ex_jump_table[index].exception_handle_jump_ptr;
    }
}

/* ---------------------- on-stack replacement ----------------------
 *
 * A method whose loop runs millions of iterations inside ONE invocation
 * never reaches the entry-count JIT threshold.  At a hot backward branch
 * the interpreter calls jit_osr_execute(runtime, loop-header bc_pos):
 * the method is compiled if needed, then a per-(method, loop) trampoline
 * replicates the body prologue (native convention, same frame layout,
 * hot-int-local preload) and tail-jumps into the compiled body at the
 * loop-header label.  The body's own return epilogue then unwinds the
 * trampoline frame, so the compiled run returns straight to the
 * interpreter call site.  Prerequisite (checked by the caller): the
 * backward target sees an empty operand stack - true for javac loops,
 * where all loop-carried state lives in locals.
 */
static __refer _jit_gen_osr_trampoline(MethodInfo *method, s32 bc_pos) {
    CodeAttribute *ca = method->converted_code;
    /* pos_2_label entries are frozen ADDRESSES after generation (the
     * sljit_label structs die with the body compiler); <=0 marks a
     * position that never got a label (dead code) */
    intptr_t entry_addr = pairlist_getl(method->pos_2_label, bc_pos);
    sljit_uw target;
    struct sljit_compiler *C;
    __refer code;

    if (entry_addr <= 0) {
        return NULL; /* bc_pos is not an enterable label */
    }
    target = (sljit_uw) entry_addr;

    C = sljit_create_compiler(NULL);
    if (!C) {
        return NULL;
    }
    /* the frame must mirror the method body exactly, or the body's return
     * epilogue would not balance this prologue */
    sljit_emit_enter(C, 0, SLJIT_ARGS1(W, P), JIT_SCRATCH_REGS | SLJIT_ENTER_FLOAT(5), JIT_SAVED_REGS,
                     LOCAL_COUNT * sizeof(sljit_sw));

    /* replicate the body prologue; S0 is REGISTER_SP, so every
     * runtime-relative load happens before the SP assignment and the
     * runtime pointer is mirrored into S1 to survive it */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_S1, 0, SLJIT_S0, 0); /* S1 = runtime */
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME, SLJIT_S1, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, method));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_METHOD, SLJIT_R0, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, stack));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK, SLJIT_R0, 0);
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_R0, 0, SLJIT_IMM, SLJIT_OFFSETOF(RuntimeStack, sp));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP, SLJIT_R0, 0);
    sljit_emit_op2(C, SLJIT_ADD, SLJIT_R0, 0, SLJIT_S1, 0, SLJIT_IMM, SLJIT_OFFSETOF(Runtime, pc));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_RUNTIME_PC, SLJIT_R0, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, thrd_info));
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_THREADINFO, SLJIT_R0, 0);
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_LOCALVAR, 0, SLJIT_MEM1(SLJIT_S1), SLJIT_OFFSETOF(Runtime, localvar));
    if (ca->jit.hot_local[0] >= 0) {
        sljit_emit_op1(C, SLJIT_MOV_S32, REGISTER_HOT_LOCAL0, 0, SLJIT_MEM1(REGISTER_LOCALVAR),
                       sizeof(LocalVarItem) * ca->jit.hot_local[0] + SLJIT_OFFSETOF(LocalVarItem, ivalue));
    }
    if (ca->jit.hot_local[1] >= 0) {
        sljit_emit_op1(C, SLJIT_MOV_S32, REGISTER_HOT_LOCAL1, 0, SLJIT_MEM1(REGISTER_LOCALVAR),
                       sizeof(LocalVarItem) * ca->jit.hot_local[1] + SLJIT_OFFSETOF(LocalVarItem, ivalue));
    }
    sljit_emit_op1(C, SLJIT_MOV_P, SLJIT_R0, 0, SLJIT_MEM1(SLJIT_SP), sizeof(sljit_sw) * LOCAL_STACK_SP);
    sljit_emit_op1(C, SLJIT_MOV_P, REGISTER_SP, 0, SLJIT_MEM1(SLJIT_R0), 0); /* S0 = sp, last */

    sljit_emit_ijump(C, SLJIT_JUMP, SLJIT_IMM, (sljit_sw) target);

    code = (__refer) sljit_generate_code(C, 0, NULL);
    if (sljit_get_compiler_error(C) != SLJIT_ERR_COMPILED) {
        if (code) {
            sljit_free_code(code, NULL);
            code = NULL;
        }
    }
    sljit_free_compiler(C);
    return code;
}

s32 jit_osr_execute(Runtime *runtime, s32 bc_pos) {
    MethodInfo *method = runtime->method;
    CodeAttribute *ca = method ? method->converted_code : NULL;
    __refer entry;

    if (!ca) {
        return -1;
    }
    if (ca->jit.state == JIT_GEN_UNKNOW) {
        spin_lock(&ca->compile_lock);
        if (ca->jit.state == JIT_GEN_UNKNOW) {
            construct_jit(method, runtime);
        }
        spin_unlock(&ca->compile_lock);
    }
    if (ca->jit.state != JIT_GEN_SUCCESS) {
        return -1;
    }
    /* the list is a plain Pairlist: guard lookup+create against its
     * realloc while another thread OSRs the same loop */
    spin_lock(&ca->compile_lock);
    entry = (__refer) pairlist_getl(ca->jit.osr_entry_list, bc_pos);
    if (!entry) {
        entry = _jit_gen_osr_trampoline(method, bc_pos);
        if (entry) {
            if (!ca->jit.osr_entry_list) {
                ca->jit.osr_entry_list = pairlist_create(4);
            }
            if (!ca->jit.osr_entry_list
                || pairlist_putl(ca->jit.osr_entry_list, bc_pos, (intptr_t) entry) < 0) {
                /* cache allocation failed: an unowned entry could never be
                 * reclaimed by jit_destroy - free it now and keep the
                 * loop interpreted */
                sljit_free_code(entry, NULL);
                entry = NULL;
            }
        }
    }
    spin_unlock(&ca->compile_lock);
    if (!entry) {
        return -1;
    }
    if (getenv("MINI_JVM_TRACE_OSR")) {
        jvm_printf("[OSR] enter %s.%s at %d\n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), bc_pos);
    }
    return ((s32 (*)(Runtime *)) entry)(runtime);
}

#else

void jit_init(CodeAttribute *ca) {
}

void jit_destroy(Jit *jit) {
}

void jit_set_exception_jump_addr(Runtime *runtime, CodeAttribute *ca, s32 index) {
}

void construct_jit(MethodInfo *method, Runtime *runtime) {
}

s32 jit_invoke_from_jit(MethodInfo *method, Runtime *runtime) {
    return execute_method_impl(method, runtime);
}

s32 jit_osr_execute(Runtime *runtime, s32 bc_pos) {
    return -1;
}

#endif
