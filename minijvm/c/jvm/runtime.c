

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"


/* Stack Initialization */
RuntimeStack *stack_create(s32 entry_size) {
    RuntimeStack *stack = jvm_calloc(sizeof(RuntimeStack));
    stack->store = (StackEntry *) jvm_calloc(sizeof(StackEntry) * entry_size);
    stack->sp = stack->store;
    stack->max_size = entry_size;
    return stack;
}

void stack_destroy(RuntimeStack *stack) {
    if (stack->store) {
        jvm_free(stack->store);
        stack->store = NULL;
    }
    jvm_free(stack);
}


/* push Integer */
void push_int_jni(RuntimeStack *stack, s32 value) {
    push_int(stack, value);
}


/* pop Integer */
s32 pop_int_jni(RuntimeStack *stack) {
    return pop_int(stack);
}

/* push Double */
void push_double_jni(RuntimeStack *stack, f64 value) {
    push_double(stack, value);
}

/* pop Double */
f64 pop_double_jni(RuntimeStack *stack) {
    return pop_double(stack);
}

/* push Float */
void push_float_jni(RuntimeStack *stack, f32 value) {
    push_float(stack, value);
}

/* pop Float */
f32 pop_float_jni(RuntimeStack *stack) {
    return pop_float(stack);
}


/* push Long */
void push_long_jni(RuntimeStack *stack, s64 value) {
    push_long(stack, value);
}

/* pop Long */
s64 pop_long_jni(RuntimeStack *stack) {
    return pop_long(stack);
}

/* push Ref */
void push_ref_jni(RuntimeStack *stack, __refer value) {
    push_ref(stack, value);
}

__refer pop_ref_jni(RuntimeStack *stack) {
    return pop_ref(stack);
}


void push_entry_jni(RuntimeStack *stack, StackEntry *entry) {
    push_entry(stack, entry);
}

/* Pop Stack Entry */
void pop_entry_jni(RuntimeStack *stack, StackEntry *entry) {
    pop_entry(stack, entry);
}

void pop_empty_jni(RuntimeStack *stack) {
    pop_empty(stack);
}

/* Entry to Int */
s32 entry_2_int_jni(StackEntry *entry) {
    return entry_2_int(entry);
}

s64 entry_2_long_jni(StackEntry *entry) {
    return entry_2_long(entry);
}

__refer entry_2_refer_jni(StackEntry *entry) {
    return entry_2_refer(entry);
}

void peek_entry_jni(StackEntry *src, StackEntry *dst) {
    peek_entry(src, dst);
}


s32 is_ref(StackEntry *entry) {
    if (entry->rvalue)
        return 1;
    return 0;
}

//======================= runtime =============================
Runtime *runtime_create(MiniJVM *jvm) {
    if (!jvm) {
        jvm_printf("[ERROR]create runtime must have a jvm.\n");
        return NULL;
    }
    Runtime *runtime = runtime_create_inl(NULL);
    runtime->jvm = jvm;
//    runtime->jvm = jvm;
    return runtime;
}

void runtime_destroy(Runtime *runtime) {
    runtime_destroy_inl(runtime);
}

void print_runtime_stack(Runtime *r) {
    Utf8String *ustr = utf8_create();
    getRuntimeStack(r, ustr);
    jvm_printf("%s", utf8_cstr(ustr));
    utf8_destroy(ustr);
}

s32 getRuntimeDepth(Runtime *top) {
    top = top->thrd_info->top_runtime;
    s32 deep = 0;
    while (top) {
        deep++;
        top = top->son;
    }
    deep--;//top need not
    return deep;
}

Runtime *getLastSon(Runtime *top) {
    while (top) {
        if (!top->son)return top;
        top = top->son;
    }
    return NULL;
}

Runtime *getTopRuntime(Runtime *runtime) {
    if (runtime) {
        return runtime->thrd_info->top_runtime;
    }
    return NULL;
}

s64 getInstructPointer(Runtime *runtime) {
    if (runtime && runtime->method && runtime->method->converted_code) {
        return runtime->pc - runtime->method->converted_code->code;
    }
    return -1;
}

void getRuntimeStackWithOutReturn(Runtime *runtime, Utf8String *ustr) {
    if (!runtime)return;
    Runtime *trun = getLastSon(runtime);
    while (trun) {
        if (!trun->parent)break;
        if (trun->method) {
            s32 lineNo = -1;
            if (trun->method->converted_code) {
                lineNo = getLineNumByIndex(trun->method->converted_code, ((s64) trun->pc) - (s64) trun->method->converted_code->code);
            }
            utf8_append(ustr, trun->method->_this_class->name);
            utf8_append_c(ustr, ".");
            utf8_append(ustr, trun->method->name);
            utf8_append_c(ustr, ":");
            utf8_append_s64(ustr, lineNo, 10);
            utf8_append_c(ustr, " | ");
        }
        trun = trun->parent;
    }
}

void getRuntimeStack(Runtime *runtime, Utf8String *ustr) {
    if (!runtime)return;
    Runtime *trun = getLastSon(runtime);
    utf8_append_c(ustr, "call stack:\n");
    while (trun) {
        if (!trun->parent)break;
        if (trun->method) {
            s32 lineNo = -1;
            if (trun->method->converted_code) {
                lineNo = getLineNumByIndex(trun->method->converted_code, ((s64) trun->pc) - (s64) trun->method->converted_code->code);
            }
            utf8_append_c(ustr, "    ");
            utf8_append(ustr, trun->method->_this_class->name);
            utf8_append_c(ustr, ".");
            utf8_append(ustr, trun->method->name);
            utf8_append_c(ustr, ":");
            utf8_append_s64(ustr, lineNo, 10);
            utf8_append_c(ustr, "\n");
        }
        trun = trun->parent;
    }
}

void getExceptionStack(Runtime *runtime, Utf8String *ustr) {
    s32 i, imax;
    utf8_append_c(ustr, "Exception threw: ");
    Instance *ins = (runtime->stack->sp - 1)->ins;
    if (ins) {
        utf8_append(ustr, ins->mb.clazz->name);
        utf8_append_c(ustr, ": ");
        c8 *ptr = getInstanceFieldPtr(ins, runtime->jvm->shortcut.throwable_detailMessage);
        if (ptr) {
            Instance *jstr_detailMessage = getFieldRefer(ptr);
            if (jstr_detailMessage) {
                Utf8String *ustr2 = utf8_create();
                jstring_2_utf8(jstr_detailMessage, ustr2, runtime);
                utf8_append(ustr, ustr2);
            }
        }
    }
    utf8_pushback(ustr, '\n');
    for (i = 0, imax = runtime->thrd_info->stacktrack->length; i < imax; i++) {
        utf8_append_c(ustr, "    at ");
        MethodInfo *method = arraylist_get_value(runtime->thrd_info->stacktrack, i);
        utf8_append(ustr, method->_this_class->name);
        utf8_append_c(ustr, ".");
        utf8_append(ustr, method->name);
        if (method->_this_class->name) {
            utf8_append_c(ustr, "(");
            utf8_append(ustr, method->_this_class->name);
            utf8_append_c(ustr, ".java:");
            s32 lineNo = method->converted_code ? (s32) (intptr_t) arraylist_get_value(runtime->thrd_info->lineNo, i) : -1;
            utf8_append_s64(ustr, lineNo, 10);
            utf8_append_c(ustr, ")");
        }
        utf8_append_c(ustr, "\n");
    }
}

//======================= localvar =============================




void localvar_setInt_jni(LocalVarItem *localvar, s32 index, s32 val) {
    localvar_setInt(localvar, index, val);
}

void localvar_setRefer_jni(LocalVarItem *localvar, s32 index, __refer val) {
    localvar_setRefer(localvar, index, val);
}

s32 localvar_getInt_jni(LocalVarItem *localvar, s32 index) {
    return localvar_getInt(localvar, index);
}

__refer localvar_getRefer_jni(LocalVarItem *localvar, s32 index) {
    return localvar_getRefer(localvar, index);
}

void localvar_setLong_2slot_jni(LocalVarItem *localvar, s32 index, s64 val) {
    localvar_setLong(localvar, index, val);
}

s64 localvar_getLong_2slot_jni(LocalVarItem *localvar, s32 index) {
    return localvar_getLong(localvar, index);
}