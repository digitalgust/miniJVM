#define FALSE 0
#define TRUE 1

#include <time.h>
#include "garbage.h"
#include "jvm_util.h"
#include <math.h>


#if  defined(__JVM_OS_MAC__) || defined(__JVM_OS_LINUX__)

#else

#include <rpc.h>
#include <windows.h>
#include <tchar.h>
#include <stdio.h>

#endif

u32 is_random_init = FALSE;

s32 com_sun_cldc_io_ConsoleOutputStream_write(Runtime *runtime, JClass *clazz) {
    u8 ch = (u8) localvar_getInt(runtime->localvar, 1);
#if __JVM_OS_ANDROID__
    jvm_printf("%c", ch);
#else
    fprintf(stdout, "%c", ch);
#endif
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("com_sun_cldc_io_ConsoleOutputStream_write\n");
#endif
    return 0;
}

s32 com_sun_cldc_io_ConsoleInputStream_read(Runtime *runtime, JClass *clazz) {
    s32 ch = getchar();
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("com_sun_cldc_io_ConsoleInputStream_read\n");
#endif
    push_int(runtime->stack, ch);
    return 0;
}

s32 com_sun_cldc_io_ResourceInputStream_open(Runtime *runtime, JClass *clazz) {
    Instance *jstr = localvar_getRefer(runtime->localvar, 0);
    Utf8String *path = utf8_create();
    jstring_2_utf8(jstr, path, runtime);
    ByteBuf *buf = load_file_from_classpath(runtime->jvm->boot_classloader, path);
    if (buf) {
        s32 _j_t_bytes = buf->wp;
        Instance *_arr = jarray_create_by_type_index(runtime, _j_t_bytes, DATATYPE_BYTE);
        bytebuf_read_batch(buf, _arr->arr_body, _j_t_bytes);
        bytebuf_destroy(buf);
        push_ref(runtime->stack, _arr);
    } else {
        push_ref(runtime->stack, NULL);
    }
    utf8_destroy(path);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("com_sun_cldc_io_ConsoleInputStream_read\n");
#endif
    return 0;
}

s32 java_lang_Class_forName(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    s32 resolve = localvar_getInt(runtime->localvar, 1);
    Instance *classloader = (Instance *) localvar_getRefer(runtime->localvar, 2);
    JClass *cl = NULL;
    s32 ret = RUNTIME_STATUS_NORMAL;
    if (jstr) {
        Utf8String *ustr = utf8_create();
        jstring_2_utf8(jstr, ustr, runtime);
        utf8_replace_c(ustr, ".", "/");
        cl = classes_load_get_with_clinit(classloader, ustr, runtime);
        if (!cl) {
            Instance *exception = exception_create_str(JVM_EXCEPTION_CLASSNOTFOUND, runtime, utf8_cstr(ustr));
            push_ref(stack, (__refer) exception);
            ret = RUNTIME_STATUS_EXCEPTION;
        } else {
            Instance *ins = insOfJavaLangClass_create_get(runtime, cl);
            push_ref(stack, ins);
        }
        utf8_destroy(ustr);
    } else {
        Instance *exception = exception_create(JVM_EXCEPTION_NULLPOINTER, runtime);
        push_ref(stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_forName [%llx]\n", (s64) (intptr_t) cl);
#endif
    return ret;
}

s32 java_lang_Class_newInstance(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    Instance *ins = NULL;
    s32 ret = 0;
    if (cl && !cl->mb.arr_type_index) {//class exists and not array class
        ins = instance_create(runtime, cl);
        instance_hold_to_thread(ins, runtime);
        instance_init(ins, runtime);
        instance_release_from_thread(ins, runtime);
    }
    if (ins) {
        push_ref(stack, (__refer) ins);

    } else {
        Instance *exception = exception_create(JVM_EXCEPTION_INSTANTIATION, runtime);
        push_ref(stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_newInstance  class:[%llx] ins:[%llx]\n", (s64) (intptr_t) cl, (s64) (intptr_t) ins);
#endif
    return ret;
}

s32 java_lang_Class_isInstance(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 1);
    if (instance_of(ins, cl)) {
        push_int(stack, 1);
    } else {
        push_int(stack, 0);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_isInstance\n");
#endif
    return 0;
}

s32 java_lang_Class_isAssignableFrom(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *c0 = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    JClass *c1 = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 1));

    if (assignable_from(c0, c1)) {
        push_int(stack, 1);
    } else {
        push_int(stack, 0);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_isAssignableFrom\n");
#endif
    return 0;
}

s32 java_lang_Class_isInterface(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));

    if (cl->cff.access_flags & ACC_INTERFACE) {//
        push_int(stack, 1);
    } else {
        push_int(stack, 0);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_isAssignableFrom\n");
#endif
    return 0;
}

s32 java_lang_Class_isArray(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    if (cl->mb.arr_type_index) {
        push_int(stack, 1);
    } else {
        push_int(stack, 0);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_isArray\n");
#endif
    return 0;
}

s32 java_lang_Class_isPrimitive(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    if (cl->mb.clazz->is_primitive) {
        push_int(stack, 1);
    } else {
        push_int(stack, 0);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_isPrimitive\n");
#endif
    return 0;
}

s32 java_lang_Class_getName0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    if (cl) {
        Utf8String *ustr = utf8_create_copy(cl->name);
        utf8_replace_c(ustr, "/", ".");
        Instance *ins = jstring_create(ustr, runtime);
        push_ref(stack, ins);
        utf8_destroy(ustr);
    } else {
        push_ref(stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_getName\n");
#endif
    return 0;
}

s32 java_lang_Class_getSuperclass(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    if (cl) {

        JClass *scl = getSuperClass(cl);
        push_ref(stack, scl ? insOfJavaLangClass_create_get(runtime, scl) : NULL);
    } else {
        push_ref(stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_getSuperclass\n");
#endif
    return 0;
}

s32 java_lang_Class_getInterfaces(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
    s32 len = cl ? cl->interfacePool.clasz_used : 0;
    Utf8String *ustr = utf8_create_c(STR_INS_JAVA_LANG_CLASS);
    Instance *jarr = jarray_create_by_type_name(runtime, len, ustr, cl->jloader);
    utf8_destroy(ustr);
    s32 i;
    for (i = 0; i < len; i++) {
        ConstantClassRef *ccr = (cl->interfacePool.clasz + i);
        JClass *other = classes_load_get_with_clinit(cl->jloader, class_get_constant_utf8(cl, ccr->stringIndex)->utfstr, runtime);
        if (other) {
            Instance *cins = insOfJavaLangClass_create_get(runtime, other);
            jarray_set_field(jarr, i, (s64) (intptr_t) cins);
        }
    }
    push_ref(stack, jarr);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_getInterfaces\n");
#endif
    return 0;
}

s32 java_lang_Class_getPrimitiveClass(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstr = localvar_getRefer(runtime->localvar, 0);
    if (jstr) {
        Utf8String *ustr = utf8_create();
        jstring_2_utf8(jstr, ustr, runtime);
        JClass *cl = primitive_class_create_get(runtime, ustr);
        Instance *ins = insOfJavaLangClass_create_get(runtime, cl);
        utf8_destroy(ustr);
        push_ref(stack, ins);
    } else {
        push_ref(stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_getPrimitiveClass\n");
#endif
    return 0;
}

s32 java_lang_Class_getComponentType(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    JClass *other = insOfJavaLangClass_get_classHandle(runtime, ins);
    s32 idx = utf8_last_indexof_c(other->name, "[");
    if (idx > 0) {
        Utf8String *ustr = utf8_create_part(other->name, idx + 1, other->name->length - 1 - idx);
        c8 ch = utf8_index_of(ustr, 0);
        if (ch == 'L') {
            utf8_substring(ustr, 1, ustr->length - 2);
        } else {
            c8 *cstr = getDataTypeFullName(ch);
            utf8_clear(ustr);
            utf8_append_c(ustr, cstr);
        }

        JClass *cl = classes_load_get_with_clinit(other->jloader, ustr, runtime);
        if (cl) {
            push_ref(stack, cl->ins_class);
        } else {
            push_ref(stack, NULL);
        }
    } else {
        push_ref(stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Class_getComponentType\n");
#endif
    return 0;
}


s32 java_lang_Double_doubleToLongBits(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Double_doubleToLongBits %lf to %lld\n", l2d.d, l2d.l);
#endif
    push_long(stack, l2d.l);
    return 0;
}

s32 java_lang_Double_longBitsToDouble(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Double_longBitsToDouble\n");
#endif
    push_double(stack, l2d.d);
    return 0;
}


s32 java_lang_Float_intBitsToFloat(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Int2Float i2f;
    i2f.i = localvar_getInt(runtime->localvar, 0);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Float_intBitsToFloat r = %f\n", i2f.f);
#endif
    push_float(stack, i2f.f);
    return 0;
}

s32 java_lang_Float_floatToIntBits(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Int2Float i2f;
    i2f.i = localvar_getInt(runtime->localvar, 0);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Float_floatToIntBits r = %f\n", i2f.i);
#endif
    push_int(stack, i2f.i);
    return 0;
}

s32 java_lang_Math_exp(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = exp(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_sin r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_random(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    f64 r = 0.0f;
    s32 i;
    s32 times = 0;
    if (!is_random_init) { // Setting seed only necessary once.
        srand((u32) time(0));
        is_random_init = TRUE;
    }
    r = ((f64) rand() / (f64) RAND_MAX);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_random r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_sin(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = sin(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_sin r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}


s32 java_lang_Math_cos(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = cos(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_cos r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_tan(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = tan(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_tan r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_sqrt(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = sqrt(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_sqrt r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_ceil(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = ceil(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_ceil r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}


s32 java_lang_Math_floor(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = floor(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_floor r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_asin(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = asin(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_asin r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_acos(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = acos(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_acos r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_atan(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = atan(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_atan r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_log(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 r = log(l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_log r = %f\n", r);
#endif
    push_double(stack, r);
    return 0;
}

s32 java_lang_Math_atan2(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 y = (l2d.d);
    l2d.l = localvar_getLong(runtime->localvar, 2);
    f64 x = (l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_atan2 \n");
#endif
    push_double(stack, atan2(y, x));
    return 0;
}

s32 java_lang_Math_pow(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    f64 a = (l2d.d);
    l2d.l = localvar_getLong(runtime->localvar, 2);
    f64 b = (l2d.d);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Math_pow \n");
#endif
    push_double(stack, pow(a, b));
    return 0;
}

s32 java_lang_Object_getClass(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    JClass *cl = (__refer) ins->mb.clazz;
    if (!cl->ins_class) {
        cl->ins_class = insOfJavaLangClass_create_get(runtime, cl);
    }
    push_ref(stack, cl->ins_class);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Object_getClass %d\n", ins);
#endif
    return 0;
}

s32 java_lang_Object_clone(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    push_ref(stack, (__refer) instance_copy(runtime, ins, 0));
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Object_getClass %d\n", ins);
#endif
    return 0;
}

s32 java_lang_Object_hashCode(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    u64 a = (u64) (intptr_t) ins;
    s32 h = (s32) (a ^ (a >> 32));
    push_int(stack, h);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Object_hashCode %llx\n", (s32) (intptr_t) ins);
#endif
    return 0;
}

s32 java_lang_Object_notify(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);

    jthread_notify(&ins->mb, runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Object_notify %d\n", ins);
#endif
    return 0;
}

s32 java_lang_Object_notifyAll(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    jthread_notifyAll(&ins->mb, runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Object_notifyAll %d\n", ins);
#endif
    return 0;
}

s32 java_lang_Object_wait(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 1);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Object_wait %llx  wait %lld\n", (s64) (intptr_t) ins, l2d.l);
#endif
    s32 ret = jthread_waitTime(&ins->mb, runtime, l2d.l);
    return ret;
}

s32 java_lang_Runtime_exitInternal(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    s32 status = localvar_getInt(runtime->localvar, 1);
    //exit(status);
    runtime->jvm->collector->exit_flag = 1;
    runtime->jvm->collector->exit_code = status;
    thread_stop_all(runtime->jvm);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_exitInternal %d\n", status);
#endif
    return 0;
}

s32 java_lang_Runtime_freeMemory(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    spin_lock(&runtime->jvm->collector->lock);
    push_long(stack, runtime->jvm->max_heap_size - runtime->jvm->collector->obj_heap_size);
    spin_unlock(&runtime->jvm->collector->lock);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_freeMemory \n");
#endif
    return 0;
}

s32 java_lang_Runtime_totalMemory(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    push_long(stack, runtime->jvm->max_heap_size);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_totalMemory \n");
#endif
    return 0;
}

s32 java_lang_Runtime_gc(Runtime *runtime, JClass *clazz) {

    runtime->jvm->collector->lastgc = 0;//active gc now

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_gc \n");
#endif
    return 0;
}

s32 java_lang_Runtime_addShutdownHook(Runtime *runtime, JClass *clazz) {
    Instance *jruntime = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *jthread = (Instance *) localvar_getRefer(runtime->localvar, 1);
    arraylist_push_back(runtime->jvm->shutdown_hook, jthread);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_addShutdownHook \n");
#endif
    return 0;
}

extern s32 os_execute(Runtime *runtime, Instance *jstrArr, Instance *jlongArr, ArrayList *cstrList, const c8 *cmd);

s32 java_lang_Runtime_exec(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstrArr = (Instance *) localvar_getRefer(runtime->localvar, 0);//String[] cmdline
    Instance *jlongArr = (Instance *) localvar_getRefer(runtime->localvar, 1);//long[] process

    s32 i;
    ArrayList *ustrList = arraylist_create(jstrArr->arr_length);
    ArrayList *cstrList = arraylist_create(jstrArr->arr_length);

    ByteBuf *buf = bytebuf_create(1024);
    for (i = 0; i < jstrArr->arr_length; i++) {
        Instance *jstr = (__refer) (intptr_t) jarray_get_field(jstrArr, i);
        Utf8String *ustr = utf8_create();
        jstring_2_utf8(jstr, ustr, runtime);
        arraylist_push_back(ustrList, ustr);
        arraylist_push_back(cstrList, (__refer) utf8_cstr(ustr));
        bytebuf_write_batch(buf, (c8 *) utf8_cstr(ustr), ustr->length);
        bytebuf_write(buf, ' ');
    }
    arraylist_push_back(cstrList, NULL);//
    bytebuf_write(buf, 0);

    s32 ret = os_execute(runtime, jstrArr, jlongArr, cstrList, buf->buf);

    for (i = 0; i < ustrList->length; i++) {
        Utf8String *ustr = arraylist_get_value(ustrList, i);
        utf8_destroy(ustr);
    }
    arraylist_destroy(ustrList);
    arraylist_destroy(cstrList);
    bytebuf_destroy(buf);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_exec \n");
#endif
    return ret;
}

extern s32 os_waitfor_process(Runtime *runtime, s64 pid, s64 tid, s32 *pExitCode);

s32 java_lang_Runtime_waitFor(Runtime *runtime, JClass *clazz) {
    s64 pid = localvar_getLong(runtime->localvar, 0);
    s64 tid = localvar_getLong(runtime->localvar, 1);
    if (pid == 0) {
        exception_throw(JVM_EXCEPTION_ILLEGALARGUMENT, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }

    s32 exitCode = 0;
    s32 ret = os_waitfor_process(runtime, pid, tid, &exitCode);
    push_int(runtime->stack, exitCode);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_waitFor \n");
#endif
    return ret;
}

extern s32 os_kill_process(s64 pid);

s32 java_lang_Runtime_kill(Runtime *runtime, JClass *clazz) {
    s64 pid = localvar_getLong(runtime->localvar, 0);
    os_kill_process(pid);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_kill \n");
#endif
    return 0;
}

s32 java_lang_Runtime_maxMemory(Runtime *runtime, JClass *clazz) {

    push_long(runtime->stack, runtime->jvm->max_heap_size);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Runtime_maxMemory \n");
#endif
    return 0;
}

s32 java_lang_String_replace0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *base = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *src = (Instance *) localvar_getRefer(runtime->localvar, 1);
    Instance *dst = (Instance *) localvar_getRefer(runtime->localvar, 2);

    s32 count = jstring_get_count(base, runtime);
    s32 offset = jstring_get_offset(base, runtime);
    u16 *value = (u16 *) jstring_get_value_array(base, runtime)->arr_body;

    s32 src_count = jstring_get_count(src, runtime);
    s32 dst_count = jstring_get_count(dst, runtime);
    if (count == 0 || src == NULL || dst == NULL || src_count == 0) {
        Instance *jchar_arr = jarray_create_by_type_index(runtime, count, DATATYPE_JCHAR);
        memcpy((c8 *) jchar_arr->arr_body, (c8 *) &value[offset], count * sizeof(u16));
        push_ref(stack, jchar_arr);
    } else {

        s32 src_offset = jstring_get_offset(src, runtime);
        u16 *src_value = (u16 *) jstring_get_value_array(src, runtime)->arr_body;
        s32 dst_offset = jstring_get_offset(dst, runtime);
        u16 *dst_value = (u16 *) jstring_get_value_array(dst, runtime)->arr_body;

        ByteBuf *sb = bytebuf_create(count);
        s32 i, j;
        for (i = 0; i < count;) {
            s32 index = i + offset;
            u16 ch = value[index];
            s32 match = 0;
            if (ch == src_value[src_offset] && index + src_count <= offset + count) {
                match = 1;
                for (j = 1; j < src_count; j++) {
                    if (value[index + j] != src_value[src_offset + j]) {
                        match = 0;
                        break;
                    }
                }
            }
            if (match) {
                if (dst_value) {
                    bytebuf_write_batch(sb, (c8 *) &dst_value[dst_offset], dst_count * sizeof(ch));
                }
                i += src_count;
            } else {
                bytebuf_write_batch(sb, (c8 *) &ch, sizeof(ch));
                i++;
            }
        }
        s32 jchar_count = sb->wp / 2;
        Instance *jchar_arr = jarray_create_by_type_index(runtime, jchar_count, DATATYPE_JCHAR);
        bytebuf_read_batch(sb, (c8 *) jchar_arr->arr_body, sb->wp);
        bytebuf_destroy(sb);
        push_ref(stack, jchar_arr);
    }


#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_String_replace0 \n");
#endif
    return 0;
}

s32 java_lang_String_charAt0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    s32 index = localvar_getInt(runtime->localvar, 1);
    u16 ch = jstring_char_at(jstr, index, runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_String_charAt ch = %d\n", ch);
#endif
    push_int(stack, ch);
    return 0;
}

s32 java_lang_String_equals(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstr1 = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *jstr2 = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 r = jstring_equals(jstr1, jstr2, runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_String_equals r = %f\n", r);
#endif
    push_int(stack, r);
    return 0;
}

s32 java_lang_String_indexOf(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    u16 ch = localvar_getInt(runtime->localvar, 1);
    s32 r = jstring_index_of(jstr, ch, 0, runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_String_indexOf r = %f\n", r);
#endif
    push_int(stack, r);
    return 0;
}

s32 java_lang_String_indexOfFrom(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    u16 ch = localvar_getInt(runtime->localvar, 1);
    s32 startAt = localvar_getInt(runtime->localvar, 2);
    s32 r = jstring_index_of(jstr, ch, startAt, runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_String_indexOfFrom r = %f\n", r);
#endif
    push_int(stack, r);
    return 0;
}

s32 java_lang_String_intern0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    if (jstr) {
        Utf8String *ustr = utf8_create();
        jstring_2_utf8(jstr, ustr, runtime);
        Instance *in_jstr = (Instance *) hashtable_get(runtime->jvm->table_jstring_const, ustr);
        push_ref(stack, (__refer) in_jstr);
        utf8_destroy(ustr);
    } else {
        push_ref(stack, NULL);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_String_intern \n");
#endif
    return 0;
}

s32 java_lang_StringBuilder_append(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *jbuilder = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 1);

    if (jstr) {
        ShortCut *jvm_runtime_cache = &runtime->jvm->shortcut;
        s32 scount = getFieldInt(getInstanceFieldPtr(jstr, jvm_runtime_cache->string_count));
        if (scount) {
            c8 *ptr_bvalue = getInstanceFieldPtr(jbuilder, jvm_runtime_cache->stringbuilder_value);
            Instance *bvalue = getFieldRefer(ptr_bvalue);
            c8 *ptr_bcount = getInstanceFieldPtr(jbuilder, jvm_runtime_cache->stringbuilder_count);
            s32 bcount = getFieldInt(ptr_bcount);

            s32 soffset = getFieldInt(getInstanceFieldPtr(jstr, jvm_runtime_cache->string_offset));
            Instance *svalue = getFieldRefer(getInstanceFieldPtr(jstr, jvm_runtime_cache->string_value));
            s32 bytes = DATA_TYPE_BYTES[DATATYPE_JCHAR];
            if (bvalue->arr_length - bcount < scount) {//need expand stringbuilder
                s32 n_count = bcount + scount + 1;
                n_count = n_count > bcount * 2 ? n_count : bcount * 2;
                Instance *b_new_v = jarray_create_by_type_index(runtime, n_count, DATATYPE_JCHAR);
                memcpy(b_new_v->arr_body, bvalue->arr_body, bcount * bytes);
                setFieldRefer(ptr_bvalue, b_new_v);
                bvalue = b_new_v;
            }
            c8 *b_body = bvalue->arr_body + (bcount * bytes);
            c8 *s_body = svalue->arr_body + (soffset * bytes);
            memcpy(b_body, s_body, scount * bytes);
            setFieldInt(ptr_bcount, bcount + scount);
        }
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_StringBuilder_append \n");
#endif
    push_ref(stack, jbuilder);
    return 0;
}


s32 java_lang_System_arraycopy(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    s32 count = localvar_getInt(runtime->localvar, 4);
    s32 dest_start = localvar_getInt(runtime->localvar, 3);
    Instance *dest = (Instance *) localvar_getRefer(runtime->localvar, 2);
    s32 src_start = localvar_getInt(runtime->localvar, 1);
    Instance *src = (Instance *) localvar_getRefer(runtime->localvar, 0);
    s32 ret = 0;
    if (src == NULL || dest == NULL) {
        Instance *exception = exception_create(JVM_EXCEPTION_NULLPOINTER, runtime);
        push_ref(stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    } else {
        s32 bytes = DATA_TYPE_BYTES[src->mb.clazz->mb.arr_type_index];
        //根据元素宽
        src_start *= bytes;
        count *= bytes;
        dest_start *= bytes;
        if (src_start + count > src->arr_length * bytes || dest_start + count > dest->arr_length * bytes || count < 0) {
            Instance *exception = exception_create(JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, runtime);
            push_ref(stack, (__refer) exception);
            ret = RUNTIME_STATUS_EXCEPTION;
        } else {
            if (src->arr_body && dest->arr_body)
                memmove(&(dest->arr_body[dest_start]), &(src->arr_body[src_start]), count);
        }
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_arraycopy\n");
#endif

    return ret;
}


s32 java_lang_System_doubleToString(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    c8 buf[22];
    sprintf(buf, "%lf", l2d.d);
    Utf8String *str = utf8_create_c(buf);
    Instance *jstr = jstring_create(str, runtime);
    push_ref(stack, (__refer) jstr);
    utf8_destroy(str);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_doubleToString\n");
#endif

    return 0;
}

s32 java_lang_System_currentTimeMillis(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    s64 curMs = currentTimeMillis();
    push_long(stack, curMs);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_currentTimeMillis\n");
#endif

    return 0;
}

extern s32 os_append_libname(Utf8String *libname, const c8 *lib);

extern s32 os_load_lib_and_init(const c8 *libname, Runtime *runtime);

s32 java_lang_System_loadLibrary0(Runtime *runtime, JClass *clazz) {
    Instance *name_arr = localvar_getRefer(runtime->localvar, 0);
    if (name_arr && name_arr->arr_length) {
        Utf8String *lab = utf8_create_c(STR_VM_JAVA_LIBRARY_PATH);
        Utf8String *v = hashtable_get(runtime->jvm->sys_prop, lab);
        Utf8String *paths = utf8_create();
        if (v) {
            utf8_append(paths, v);
            utf8_append_c(paths, PATHSEPARATOR);
        }
        if (runtime->jvm->startup_dir) {
            utf8_append(paths, runtime->jvm->startup_dir);
        }

        Utf8String *libname = utf8_create();
        s32 i;
        for (i = 0;; i++) {
            utf8_clear(lab);
            utf8_clear(libname);
            utf8_split_get_part(paths, PATHSEPARATOR, i, lab);
            if (lab->length) {
                utf8_append(libname, lab);
            } else {
                break;
            }
            os_append_libname(libname, name_arr->arr_body);
            s32 ret = os_load_lib_and_init(utf8_cstr(libname), runtime);
            // load success
            if (ret)break;
        }
        utf8_destroy(lab);
        utf8_destroy(libname);
        utf8_destroy(paths);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_loadLibrary0\n");
#endif

    return 0;
}

s32 java_lang_System_load0(Runtime *runtime, JClass *clazz) {
    Instance *path_arr = localvar_getRefer(runtime->localvar, 0);
    if (path_arr && path_arr->arr_length) {
        os_load_lib_and_init(path_arr->arr_body, runtime);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_load0\n");
#endif

    return 0;
}

s32 java_lang_System_nanotime(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    s64 nano = nanoTime();
    push_long(stack, nano);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_nanotime\n");
#endif

    return 0;
}

s32 java_lang_System_identityHashCode(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *tmps = (Instance *) localvar_getRefer(runtime->localvar, 0);
    push_ref(stack, (__refer) tmps);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_identityHashCode\n");
#endif

    return 0;
}

s32 java_lang_System_getProperty0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    Instance *jstr1 = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Utf8String *key = utf8_create();
    jstring_2_utf8(jstr1, key, runtime);
    Utf8String *val = (Utf8String *) hashtable_get(runtime->jvm->sys_prop, key);
    if (val) {
        Instance *jstr = jstring_create(val, runtime);
        push_ref(stack, jstr);
    } else {
        push_ref(stack, (__refer) (intptr_t) NULL);
    }
    utf8_destroy(key);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_getProperty0 \n");
#endif
    return 0;
}

s32 java_lang_System_setProperty0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    MiniJVM *jvm = runtime->jvm;
    Instance *jstr1 = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Utf8String *key = utf8_create();
    jstring_2_utf8(jstr1, key, runtime);
    Instance *jstr2 = (Instance *) localvar_getRefer(runtime->localvar, 1);
    Utf8String *val = utf8_create();
    jstring_2_utf8(jstr2, val, runtime);
    Utf8String *old_val = (Utf8String *) hashtable_get(jvm->sys_prop, key);
    if (old_val) {
        Instance *jstr = jstring_create(old_val, runtime);
        push_ref(stack, jstr);
    } else {
        push_ref(stack, (__refer) (intptr_t) NULL);
    }
    hashtable_put(jvm->sys_prop, key, val);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_setProperty0 \n");
#endif
    return 0;
}

s32 java_lang_Thread_currentThread(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    push_ref(stack, (__refer) runtime->thrd_info->jthread);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_currentThread \n");
#endif

    return 0;
}

s32 java_lang_Thread_createStackFrame(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);

    Runtime *r = runtime_create(runtime->jvm);
    r->thrd_info->type = THREAD_TYPE_NORMAL;
    jthread_set_stackframe_value(runtime->jvm, ins, r);
    push_long(stack, (s64) (intptr_t) r);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_createStackFrame \n");
#endif

    return 0;
}

s32 java_lang_Thread_yield(Runtime *runtime, JClass *clazz) {
    jthread_yield(runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_yield \n");
#endif
    return 0;
}

s32 java_lang_Thread_sleep(Runtime *runtime, JClass *clazz) {

    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_sleep %lld\n", l2d.l);
#endif
    s32 ret = jthread_sleep(runtime, l2d.l);
    return ret;
}

s32 java_lang_Thread_start(Runtime *runtime, JClass *clazz) {

    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *thrd_rt = jthread_get_stackframe_value(runtime->jvm, ins);
    s32 ret = 0;
    if (thrd_rt->thrd_info->thread_status != THREAD_STATUS_NEW) {
        Instance *exception = exception_create(JVM_EXCEPTION_ILLEGALTHREADSTATE, runtime);
        push_ref(runtime->stack, exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    } else {
        thrd_rt->thrd_info->thread_status = THREAD_STATUS_RUNNING;
        jthread_start(ins, runtime);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_start \n");
#endif

    return ret;
}

s32 java_lang_Thread_isAlive(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *rt = jthread_get_stackframe_value(runtime->jvm, ins);
    if (rt)
        push_int(stack, rt->thrd_info->thread_status > THREAD_STATUS_NEW && rt->thrd_info->thread_status < THREAD_STATUS_ZOMBIE);
    else
        push_int(stack, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_isAlive \n");
#endif

    return 0;
}

s32 java_lang_Thread_activeCount(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

    push_int(stack, runtime->jvm->thread_list->length);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_activeCount \n");
#endif

    return 0;
}


s32 java_lang_Thread_setPriority0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_setPriority0 \n");
#endif

    return 0;
}


s32 java_lang_Thread_interrupt0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);

    Runtime *rt_thread = jthread_get_stackframe_value(runtime->jvm, ins);
    jthread_interrupt(rt_thread);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_interrupt0 \n");
#endif

    return 0;
}

s32 java_lang_Thread_interrupted0(Runtime *runtime, JClass *clazz) {
    Instance *ins_thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    if (ins_thread == NULL) {
        push_int(runtime->stack, 0);
    } else {
        Runtime *rt_thread = jthread_get_stackframe_value(runtime->jvm, ins_thread);
        push_int(runtime->stack, rt_thread->thrd_info->is_interrupt != 0);
    }
    return 0;
}

s32 java_lang_Thread_setContextClassLoader0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins_thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *ins_classloader = (Instance *) localvar_getRefer(runtime->localvar, 1);

    Runtime *rt_thread = jthread_get_stackframe_value(runtime->jvm, ins_thread);
    if (rt_thread) {
        rt_thread->thrd_info->context_classloader = ins_classloader;
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_setContextClassLoader \n");
#endif

    return 0;
}

s32 java_lang_Thread_getContextClassLoader0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins_thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *rt_thread = jthread_get_stackframe_value(runtime->jvm, ins_thread);
    if (rt_thread) {
        push_ref(stack, rt_thread->thrd_info->context_classloader);
    } else {
        push_ref(stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_Thread_getContextClassLoader \n");
#endif

    return 0;
}


s32 java_io_PrintStream_printImpl(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_io_PrintStream_printImpl \n");
#endif

    Instance *tmps = NULL;
    tmps = (Instance *) localvar_getRefer(runtime->localvar, 0);
    if (tmps) {

        c8 *fieldPtr = jstring_get_value_ptr(tmps, runtime);
        Instance *ptr = (Instance *) getFieldRefer(fieldPtr);
        //jvm_printf("printImpl [%x]\n", arr_body);
        if (ptr && ptr->arr_body) {
            u16 *jchar_arr = (u16 *) ptr->arr_body;
            s32 i = 0;
            for (; i < ptr->arr_length; i++) {
                u16 ch = jchar_arr[i];
                //swap_endian_little_big((u8*)&ch, sizeof(ch));
                printf("%c", ch);
            }
        }
    }
    return 0;
}

s32 java_io_Throwable_printStackTrace0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *tmps = (Instance *) localvar_getRefer(runtime->localvar, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_io_Throwable_printStackTrace0 %s \n", utf8_cstr(tmps->mb.clazz->name));
#endif
    return 0;
}

s32 java_io_Throwable_buildStackElement(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *ins = NULL;
    //can only build current thread stack
//    Instance *ins_thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
//    if (ins_thread) {
//        Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, ins_thread);
//        if (trun) {
    ins = build_stack_element(runtime, getLastSon(runtime));
//        }
//    }
    push_ref(stack, ins);
    return 0;
}

//===================================    avian    ========================================

s32 java_lang_System_getNativeProperties(Runtime *runtime, JClass *clazz) {
    Hashtable *sys_prop = runtime->jvm->sys_prop;
    s32 size = (s32) sys_prop->entries;
    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_STRING);
    Instance *jarr = jarray_create_by_type_name(runtime, size, ustr, NULL);
    instance_hold_to_thread(jarr, runtime);

    s32 i = 0;
    HashtableIterator hti;
    hashtable_iterate(sys_prop, &hti);

    for (; hashtable_iter_has_more(&hti);) {
        utf8_clear(ustr);
        Utf8String *k = hashtable_iter_next_key(&hti);
        Utf8String *v = hashtable_get(sys_prop, k);
        utf8_append(ustr, k);
        utf8_append_c(ustr, "=");
        utf8_append(ustr, v);
        Instance *jstr = jstring_create(ustr, runtime);
        jarray_set_field(jarr, i, (intptr_t) jstr);
        i++;
    }
    push_ref(runtime->stack, jarr);//先放入栈，
    instance_release_from_thread(jarr, runtime);

    utf8_destroy(ustr);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("java_lang_System_getNativeProperties \n");
#endif
    return 0;
}


static java_native_method METHODS_STD_TABLE[] = {
        {"com/sun/cldc/io/ConsoleOutputStream", "write",                  "(I)V",                                                          com_sun_cldc_io_ConsoleOutputStream_write},
        {"com/sun/cldc/io/ConsoleInputStream",  "read",                   "()I",                                                           com_sun_cldc_io_ConsoleInputStream_read},
        {"com/sun/cldc/io/ResourceInputStream", "open",                   "(Ljava/lang/String;)[B",                                        com_sun_cldc_io_ResourceInputStream_open},
//        {"com/sun/cldc/io/Waiter",              "waitForIO",       "",         com_sun_cldc_io_ResourceInputStream_waitForIO},
        {"java/lang/Class",                     "forName",                "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", java_lang_Class_forName},
        {"java/lang/Class",                     "newInstance",            "()Ljava/lang/Object;",                                          java_lang_Class_newInstance},
        {"java/lang/Class",                     "isInstance",             "(Ljava/lang/Object;)Z",                                         java_lang_Class_isInstance},
        {"java/lang/Class",                     "isAssignableFrom",       "(Ljava/lang/Class;)Z",                                          java_lang_Class_isAssignableFrom},
        {"java/lang/Class",                     "isInterface",            "()Z",                                                           java_lang_Class_isInterface},
        {"java/lang/Class",                     "isArray",                "()Z",                                                           java_lang_Class_isArray},
        {"java/lang/Class",                     "isPrimitive",            "()Z",                                                           java_lang_Class_isPrimitive},
        {"java/lang/Class",                     "getName0",               "()Ljava/lang/String;",                                          java_lang_Class_getName0},
        {"java/lang/Class",                     "getSuperclass",          "()Ljava/lang/Class;",                                           java_lang_Class_getSuperclass},
        {"java/lang/Class",                     "getInterfaces",          "()[Ljava/lang/Class;",                                          java_lang_Class_getInterfaces},
        {"java/lang/Class",                     "getPrimitiveClass",      "(Ljava/lang/String;)Ljava/lang/Class;",                         java_lang_Class_getPrimitiveClass},
        {"java/lang/Class",                     "getComponentType",       "()Ljava/lang/Class;",                                           java_lang_Class_getComponentType},
        {"java/lang/Double",                    "doubleToLongBits",       "(D)J",                                                          java_lang_Double_doubleToLongBits},
        {"java/lang/Double",                    "longBitsToDouble",       "(J)D",                                                          java_lang_Double_longBitsToDouble},
        {"java/lang/Float",                     "floatToIntBits",         "(F)I",                                                          java_lang_Float_floatToIntBits},
        {"java/lang/Float",                     "intBitsToFloat",         "(I)F",                                                          java_lang_Float_intBitsToFloat},
        {"java/lang/Math",                      "exp",                    "(D)D",                                                          java_lang_Math_exp},
        {"java/lang/Math",                      "random",                 "()D",                                                           java_lang_Math_random},
        {"java/lang/Math",                      "sin",                    "(D)D",                                                          java_lang_Math_sin},
        {"java/lang/Math",                      "cos",                    "(D)D",                                                          java_lang_Math_cos},
        {"java/lang/Math",                      "tan",                    "(D)D",                                                          java_lang_Math_tan},
        {"java/lang/Math",                      "sqrt",                   "(D)D",                                                          java_lang_Math_sqrt},
        {"java/lang/Math",                      "ceil",                   "(D)D",                                                          java_lang_Math_ceil},
        {"java/lang/Math",                      "floor",                  "(D)D",                                                          java_lang_Math_floor},
        {"java/lang/Math",                      "asin",                   "(D)D",                                                          java_lang_Math_asin},
        {"java/lang/Math",                      "acos",                   "(D)D",                                                          java_lang_Math_acos},
        {"java/lang/Math",                      "atan",                   "(D)D",                                                          java_lang_Math_atan},
        {"java/lang/Math",                      "log",                    "(D)D",                                                          java_lang_Math_log},
        {"java/lang/Math",                      "atan2",                  "(DD)D",                                                         java_lang_Math_atan2},
        {"java/lang/Math",                      "pow",                    "(DD)D",                                                         java_lang_Math_pow},
        {"java/lang/Object",                    "clone",                  "()Ljava/lang/Object;",                                          java_lang_Object_clone},
        {"java/lang/Object",                    "getClass",               "()Ljava/lang/Class;",                                           java_lang_Object_getClass},
        {"java/lang/Object",                    "hashCode",               "()I",                                                           java_lang_Object_hashCode},
        {"java/lang/Object",                    "notify",                 "()V",                                                           java_lang_Object_notify},
        {"java/lang/Object",                    "notifyAll",              "()V",                                                           java_lang_Object_notifyAll},
        {"java/lang/Object",                    "wait",                   "(J)V",                                                          java_lang_Object_wait},
        {"java/lang/Runtime",                   "exitInternal",           "(I)V",                                                          java_lang_Runtime_exitInternal},
        {"java/lang/Runtime",                   "freeMemory",             "()J",                                                           java_lang_Runtime_freeMemory},
        {"java/lang/Runtime",                   "totalMemory",            "()J",                                                           java_lang_Runtime_totalMemory},
        {"java/lang/Runtime",                   "gc",                     "()V",                                                           java_lang_Runtime_gc},
        {"java/lang/Runtime",                   "addShutdownHook",        "(Ljava/lang/Thread;)V",                                         java_lang_Runtime_addShutdownHook},
        {"java/lang/Runtime",                   "exec",                   "([Ljava/lang/String;[J)V",                                      java_lang_Runtime_exec},
        {"java/lang/Runtime",                   "waitFor",                "(JJ)I",                                                         java_lang_Runtime_waitFor},
        {"java/lang/Runtime",                   "kill",                   "(J)V",                                                          java_lang_Runtime_kill},
        {"java/lang/Runtime",                   "maxMemory",              "()J",                                                           java_lang_Runtime_maxMemory},
        {"java/lang/String",                    "charAt0",                "(I)C",                                                          java_lang_String_charAt0},
        {"java/lang/String",                    "replace0",               "(Ljava/lang/String;Ljava/lang/String;)[C",                      java_lang_String_replace0},
        {"java/lang/String",                    "equals",                 "(Ljava/lang/Object;)Z",                                         java_lang_String_equals},
        {"java/lang/String",                    "indexOf",                "(I)I",                                                          java_lang_String_indexOf},
        {"java/lang/String",                    "indexOf",                "(II)I",                                                         java_lang_String_indexOfFrom},
        {"java/lang/String",                    "intern0",                "()Ljava/lang/String;",                                          java_lang_String_intern0},
        {"java/lang/StringBuilder",             "append",                 "(Ljava/lang/String;)Ljava/lang/StringBuilder;",                 java_lang_StringBuilder_append},
        {"java/lang/System",                    "arraycopy",              "(Ljava/lang/Object;ILjava/lang/Object;II)V",                    java_lang_System_arraycopy},
        {"java/lang/System",                    "doubleToString",         "(D)Ljava/lang/String;",                                         java_lang_System_doubleToString},
        {"java/lang/System",                    "currentTimeMillis",      "()J",                                                           java_lang_System_currentTimeMillis},
        {"java/lang/System",                    "loadLibrary0",           "([B)V",                                                         java_lang_System_loadLibrary0},
        {"java/lang/System",                    "load0",                  "([B)V",                                                         java_lang_System_load0},
        {"java/lang/System",                    "nanoTime",               "()J",                                                           java_lang_System_nanotime},
        {"java/lang/System",                    "identityHashCode",       "(Ljava/lang/Object;)I",                                         java_lang_System_identityHashCode},
        {"java/lang/System",                    "getProperty0",           "(Ljava/lang/String;)Ljava/lang/String;",                        java_lang_System_getProperty0},
        {"java/lang/System",                    "setProperty0",           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",      java_lang_System_setProperty0},
        {"java/lang/Thread",                    "currentThread",          "()Ljava/lang/Thread;",                                          java_lang_Thread_currentThread},
        {"java/lang/Thread",                    "createStackFrame",       "()J",                                                           java_lang_Thread_createStackFrame},
        {"java/lang/Thread",                    "yield",                  "()V",                                                           java_lang_Thread_yield},
        {"java/lang/Thread",                    "sleep",                  "(J)V",                                                          java_lang_Thread_sleep},
        {"java/lang/Thread",                    "start",                  "()V",                                                           java_lang_Thread_start},
        {"java/lang/Thread",                    "isAlive",                "()Z",                                                           java_lang_Thread_isAlive},
        {"java/lang/Thread",                    "activeCount",            "()I",                                                           java_lang_Thread_activeCount},
        {"java/lang/Thread",                    "setPriority0",           "(I)V",                                                          java_lang_Thread_setPriority0},
        {"java/lang/Thread",                    "interrupt0",             "(Ljava/lang/Thread;)V",                                         java_lang_Thread_interrupt0},
        {"java/lang/Thread",                    "interrupted0",           "(Ljava/lang/Thread;)Z",                                         java_lang_Thread_interrupted0},
        {"java/lang/Thread",                    "setContextClassLoader0", "(Ljava/lang/ClassLoader;)V",                                    java_lang_Thread_setContextClassLoader0},
        {"java/lang/Thread",                    "getContextClassLoader0", "()Ljava/lang/ClassLoader;",                                     java_lang_Thread_getContextClassLoader0},
        {"java/lang/Throwable",                 "printStackTrace0",       "",                                                              java_io_Throwable_printStackTrace0},
        {"java/lang/Throwable",                 "buildStackElement",      "(Ljava/lang/Thread;)Ljava/lang/StackTraceElement;",             java_io_Throwable_buildStackElement},
        {"java/io/PrintStream",                 "printImpl",              "(Ljava/lang/String;)V",                                         java_io_PrintStream_printImpl},
        {"java/lang/System",                    "getNativeProperties",    "()[Ljava/lang/String;",                                         java_lang_System_getNativeProperties},
};


java_native_method *find_native_method(MiniJVM *jvm, c8 const *cls_name, c8 const *method_name, c8 const *method_type) {
    s32 i, j;
    for (j = 0; j < jvm->native_libs->length; j++) {
        JavaNativeLib *lib = arraylist_get_value(jvm->native_libs, j);
        java_native_method *methods = lib->methods;
        for (i = 0; i < lib->methods_count; i++)
            if (strcmp(cls_name, (methods + i)->clzname) == 0 &&
                strcmp(method_name, (methods + i)->methodname) == 0 &&
                strcmp(method_type, (methods + i)->methodtype) == 0)
                return (methods + i);
    }
    return 0;
}

s32 invoke_native_method(MiniJVM *jvm, Runtime *runtime, JClass *p, c8 const *cls_name, c8 const *method_name, c8 const *method_type) {
    java_native_method *method = find_native_method(jvm, cls_name, method_name, method_type);
    if (method != 0) {
#if _JVM_DEBUG_LOG_LEVEL > 5
        invoke_deepth(runtime);
        jvm_printf("invoke native %s/%s %s\n", method->clzname, method->methodname, method_type);
#endif
        method->func_pointer(runtime, p);
        return 1;
    }
    return 0;
}

void reg_std_native_lib(MiniJVM *jvm) {
    native_reg_lib(jvm, &(METHODS_STD_TABLE[0]), sizeof(METHODS_STD_TABLE) / sizeof(java_native_method));
}

s32 native_reg_lib(MiniJVM *jvm, java_native_method *methods, s32 method_size) {
    JavaNativeLib *lib = jvm_calloc(sizeof(JavaNativeLib));
    lib->methods_count = method_size;
    lib->methods = methods;
    arraylist_push_back(jvm->native_libs, lib);
    return 0;
}

s32 native_remove_lib(MiniJVM *jvm, JavaNativeLib *lib) {
    arraylist_remove(jvm->native_libs, lib);
    return 0;
}

s32 native_lib_destroy(MiniJVM *jvm) {
    s32 i;
    for (i = 0; i < jvm->native_libs->length; i++) {
        __refer lib = arraylist_get_value(jvm->native_libs, i);
        jvm_free(lib);
    }
    arraylist_destroy(jvm->native_libs);
    return 0;
}
