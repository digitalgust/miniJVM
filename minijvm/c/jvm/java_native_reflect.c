//
// Created by gust on 2017/9/1.
//

#include "jvm.h"
#include "java_native_std.h"
#include "garbage.h"
#include "jvm_util.h"
#include "java_native_reflect.h"


#ifdef __cplusplus
extern "C" {
#endif


//========  native============================================================================

s32 org_mini_reflect_vm_RefNative_refIdSize(Runtime *runtime, JClass *clazz) {
    push_int(runtime->stack, sizeof(__refer));

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_refIdSize\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_obj2id(Runtime *runtime, JClass *clazz) {
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Long2Double l2d;
    l2d.l = (u64) (intptr_t) ins;
    push_long(runtime->stack, l2d.l);

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_obj2id\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_id2obj(Runtime *runtime, JClass *clazz) {
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    __refer r = (__refer) (intptr_t) l2d.l;//这里不能直接转化，可能在外部发生了数据精度丢失，只能从低位强转
    push_ref(runtime->stack, r);

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_jdwp_RefNative_id2obj\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getClasses(Runtime *runtime, JClass *clazz) {
    s32 size = (s32) sys_classloader->classes->entries;

    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_CLASS);
    Instance *jarr = jarray_create_by_type_name(runtime, size, ustr);
    utf8_destory(ustr);
    s32 i = 0;
    HashtableIterator hti;
    hashtable_iterate(sys_classloader->classes, &hti);

    for (; hashtable_iter_has_more(&hti);) {
        Utf8String *k = hashtable_iter_next_key(&hti);
        JClass *r = classes_get(k);
        jarray_set_field(jarr, i, (intptr_t) insOfJavaLangClass_create_get(runtime, r));
        i++;
    }
    push_ref(runtime->stack, jarr);//先放入栈，再关联回收器，防止多线程回收

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getClasses\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getClassByName(Runtime *runtime, JClass *clazz) {
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Utf8String *ustr = utf8_create();
    jstring_2_utf8(jstr, ustr);
    utf8_replace_c(ustr, ".", "/");
    JClass *cl = classes_load_get(ustr, runtime);
    utf8_destory(ustr);
    push_ref(runtime->stack, insOfJavaLangClass_create_get(runtime, cl));
    return 0;
}


s32 org_mini_reflect_vm_RefNative_newWithoutInit(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle((Instance *) localvar_getRefer(runtime->localvar, 0));
    Instance *ins = NULL;
    s32 ret = 0;
    if (cl && !cl->mb.arr_type_index) {//class exists and not array class
        ins = instance_create(runtime, cl);
    }
    if (ins) {
        push_ref(stack, (__refer) ins);

    } else {
        Instance *exception = exception_create(JVM_EXCEPTION_INSTANTIATION, runtime);
        push_ref(stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    }
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_reflect_vm_RefNative_newWithoutInit  class:[%llx] ins:[%llx]\n", (s64) (intptr_t) cl, (s64) (intptr_t) ins);
#endif
    return ret;
}


s32 org_mini_reflect_vm_RefNative_setLocalVal(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    Runtime *r = (Runtime *) (__refer) (intptr_t) l2d.l;
    s32 slot = localvar_getInt(runtime->localvar, pos++);
    u8 type = (u8) localvar_getInt(runtime->localvar, pos++);

    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;

    s32 bytes = localvar_getInt(runtime->localvar, pos++);
    if (slot < r->localvar_slots) {
        switch (bytes) {
            case 'R':
                localvar_setRefer(r->localvar, slot, (__refer) (intptr_t) l2d.l);
                break;
            case '8':
                localvar_setLong(r->localvar, slot, l2d.l);
                break;
            case '4':
            case '2':
            case '1':
                localvar_setInt(r->localvar, slot, (s32) l2d.l);
                break;
        }
        push_int(runtime->stack, 0);
    } else {
        push_int(runtime->stack, 1);
    }
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getLocalVal(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    Runtime *r = (Runtime *) (__refer) (intptr_t) l2d.l;
    s32 slot = localvar_getInt(runtime->localvar, pos++);
    Instance *valuetype = localvar_getRefer(runtime->localvar, pos++);

    c8 *ptr = getFieldPtr_byName_c(valuetype, JDWP_CLASS_VALUETYPE, "bytes", "C", runtime);
    u16 bytes = (u16) getFieldShort(ptr);
    ptr = getFieldPtr_byName_c(valuetype, JDWP_CLASS_VALUETYPE, "value", "J", runtime);
    if (slot < r->localvar_slots) {
        switch (bytes) {
            case 'R':
                l2d.l = (s64) (intptr_t) localvar_getRefer(r->localvar, slot);
                break;
            case '8':
                l2d.l = localvar_getLong(runtime->localvar, slot);
//                l2d.i2l.i1 = localvar_getInt(r->localvar, slot);
//                l2d.i2l.i0 = localvar_getInt(r->localvar, slot + 1);
                break;
            case '4':
            case '2':
            case '1':
                l2d.l = localvar_getInt(r->localvar, slot);
                break;
        }
        setFieldLong(ptr, l2d.l);
        push_int(runtime->stack, 0);
    } else {
        push_int(runtime->stack, 1);
    }
    return 0;
}

s32 org_mini_reflect_ReflectField_getFieldVal(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    Instance *ins = (Instance *) (__refer) (intptr_t) l2d.l;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FieldInfo *fi = (FieldInfo *) (__refer) (intptr_t) l2d.l;

    c8 *fptr;
    if (fi->access_flags & ACC_STATIC) {
        fptr = getStaticFieldPtr(fi);
    } else {
        fptr = getInstanceFieldPtr(ins, fi);
    }
    s64 val = 0;
    switch (fi->datatype_bytes) {
        case 'R':
            val = (s64) (intptr_t) getFieldRefer(fptr);

            break;
        case '8':
            val = getFieldLong(fptr);
            break;
        case '4':
            val = getFieldInt(fptr);
            break;
        case '2':
            if (fi->datatype_idx == DATATYPE_JCHAR) {
                val = getFieldChar(fptr);
            } else {
                val = getFieldShort(fptr);
            }
            break;
        case '1':
            val = getFieldByte(fptr);
            break;
    }

    push_long(runtime->stack, val);

    return 0;
}

s32 org_mini_reflect_ReflectField_setFieldVal(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    Instance *ins = (Instance *) (__refer) (intptr_t) l2d.l;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FieldInfo *fi = (FieldInfo *) (__refer) (intptr_t) l2d.l;
    s64 val = localvar_getLong(runtime->localvar, pos);

    c8 *fptr;
    if (fi->access_flags & ACC_STATIC) {
        fptr = getStaticFieldPtr(fi);
    } else {
        fptr = getInstanceFieldPtr(ins, fi);
    }
    switch (fi->datatype_bytes) {
        case 'R':
            setFieldRefer(fptr, (__refer) (intptr_t) val);
            break;
        case '8':
            setFieldLong(fptr, (s64) val);
            break;
        case '4':
            setFieldInt(fptr, (s32) val);
            break;
        case '2':
            setFieldShort(fptr, (s16) val);
            break;
        case '1':
            setFieldByte(fptr, (s8) val);
            break;
    }

    return 0;
}


s32 org_mini_reflect_ReflectArray_newArray(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    JClass *cl = insOfJavaLangClass_get_classHandle((Instance *) localvar_getRefer(runtime->localvar, pos++));
    s32 count = localvar_getInt(runtime->localvar, pos++);
    Instance *arr = jarray_create_by_type_name(runtime, count, cl->name);

    push_ref(runtime->stack, arr);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_ReflectArray_newArray\n");
#endif
    return 0;
}

s32 org_mini_reflect_ReflectArray_multiNewArray(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    JClass *cl = insOfJavaLangClass_get_classHandle((Instance *) localvar_getRefer(runtime->localvar, pos++));
    Instance *dimarr = localvar_getRefer(runtime->localvar, pos++);
    Utf8String *desc = utf8_create();
    if (cl->primitive) {
        utf8_insert(desc, 0, getDataTypeTagByName(cl->name));
    } else if (cl->mb.arr_type_index) {
        utf8_append(desc, cl->name);
    } else {
        utf8_append_c(desc, "L");
        utf8_append(desc, cl->name);
        utf8_append_c(desc, ";");
    }
    s32 i;
    for (i = 0; i < dimarr->arr_length; i++) {
        utf8_insert(desc, 0, '[');
    }

    Instance *arr = jarray_multi_create(runtime, (s32 *) dimarr->arr_body, dimarr->arr_length, desc, 0);

    push_ref(runtime->stack, arr);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_ReflectArray_multiNewArray\n");
#endif
    return 0;
}


struct _list_getthread_para {
    s32 i;
    Instance *jarr;
    s64 val;
};

void _list_iter_getthread(ArrayListValue value, void *para) {
    if (value) {
        Runtime *r = value;
        struct _list_getthread_para *p = para;
        p->val = (intptr_t) r->threadInfo->jthread;
        jarray_set_field(p->jarr, p->i, p->val);
    }
}

s32 org_mini_reflect_vm_RefNative_getThreads(Runtime *runtime, JClass *clazz) {
//    garbage_thread_lock();
    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_THREAD);
    Instance *jarr = jarray_create_by_type_name(runtime, thread_list->length, ustr);
    utf8_destory(ustr);

    struct _list_getthread_para para;
    para.i = 0;
    para.jarr = jarr;
    arraylist_iter_safe(thread_list, _list_iter_getthread, &para);
//    s32 i = 0;
//    for (i = 0; i < thread_list->length; i++) {
//        Runtime *r = threadlist_get(i);
//        if(r) {
//            l2d.r = r->threadInfo->jthread;
//            jarray_set_field(jarr, i, &l2d);
//        }
//    }
    push_ref(runtime->stack, jarr);//先放入栈，再关联回收器，防止多线程回收
//    garbage_thread_unlock();
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getThreads\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getStatus(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun)
        push_int(runtime->stack, trun->threadInfo->thread_status);
    else
        push_int(runtime->stack, THREAD_STATUS_ZOMBIE);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("com_egls_jvm_RefNative_getStatus\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_suspendThread(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        jthread_suspend(trun);
        push_int(runtime->stack, 0);
    } else
        push_int(runtime->stack, 1);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("com_egls_jvm_RefNative_suspendThread\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_resumeThread(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        jthread_resume(trun);
        push_int(runtime->stack, 0);
    } else
        push_int(runtime->stack, 1);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("com_egls_jvm_RefNative_resumeThread\n");
#endif
    return 0;
}


s32 org_mini_reflect_vm_RefNative_getSuspendCount(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        push_int(runtime->stack, trun->threadInfo->suspend_count);
    } else
        push_int(runtime->stack, 0);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("com_egls_jvm_RefNative_getSuspendCount\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getFrameCount(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    int i = 0;
    while (trun) {
        i++;
        trun = trun->son;
    }
    i--;
    push_int(runtime->stack, i);
    return 0;
}

s32 org_mini_reflect_vm_RefNative_stopThread(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 1);
    Instance *ins = (__refer) (intptr_t) l2d.l;
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        push_int(runtime->stack, 0);
    } else
        push_int(runtime->stack, 0);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("com_egls_jvm_RefNative_stopThread\n");
#endif
    return 0;
}


s32 org_mini_reflect_vm_RefNative_getStackFrame(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        while (trun) {
            if (!trun->son)break;
            trun = trun->son;
        }
        push_long(runtime->stack, (u64) (intptr_t) trun->parent);
    } else
        push_long(runtime->stack, 0);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getStackFrame %llx\n", (u64) (intptr_t) trun);
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getGarbageReferedObjs(Runtime *runtime, JClass *clazz) {
    s32 size = (s32) collector->runtime_refer_copy->length;

    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_OBJECT);
    Instance *jarr = jarray_create_by_type_name(runtime, size, ustr);
    utf8_destory(ustr);
    s32 i = 0;

    for (i = 0; i < collector->runtime_refer_copy->length; i++) {
        __refer r = arraylist_get_value(collector->runtime_refer_copy, i);
        jarray_set_field(jarr, i, (intptr_t) r);
    }
    push_ref(runtime->stack, jarr);//先放入栈，再关联回收器，防止多线程回收

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getGarbageReferedObjs %llx\n", (u64) (intptr_t) jarr);
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getGarbageStatus(Runtime *runtime, JClass *clazz) {

    push_int(runtime->stack, collector->_garbage_thread_status);//先放入栈，再关联回收器，防止多线程回收

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getGarbageStatus %d\n", collector->_garbage_thread_status);
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_defineClass(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *cloader = localvar_getRefer(runtime->localvar, pos++);
    Instance *namejstr = localvar_getRefer(runtime->localvar, pos++);
    Instance *bytesarr = localvar_getRefer(runtime->localvar, pos++);
    s32 offset = localvar_getInt(runtime->localvar, pos++);
    s32 len = localvar_getInt(runtime->localvar, pos++);

    ByteBuf *bytebuf = bytebuf_create(len);
    bytebuf_write_batch(bytebuf, bytesarr->arr_body + offset, len);
    JClass *cl = class_parse(bytebuf, runtime);
    cl->jClassLoader = cloader;
    bytebuf_destory(bytebuf);

    cl->source = cl->name;

    Instance *clIns = insOfJavaLangClass_create_get(runtime, cl);

    push_ref(runtime->stack, clIns);
//    push_ref(runtime->stack, NULL);

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_defineClass %d\n", collector->_garbage_thread_status);
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_addJarToClasspath(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jstr = localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr = utf8_create();

    jstring_2_utf8(jstr, ustr);

    classloader_add_jar_path(sys_classloader, ustr);
    utf8_destory(ustr);

    return 0;
}

s32 org_mini_reflect_ReflectClass_mapReference(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    JClass *target = (__refer) (intptr_t) l2d.l;
    if (target) {
        c8 *ptr;
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "className", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *clsName = jstring_create(target->name, runtime);
            setFieldRefer(ptr, clsName);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "superclass", "J", runtime);
        if (ptr)
            setFieldLong(ptr, (s64) (intptr_t) getSuperClass(target));
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "accessFlags", "S", runtime);
        if (ptr)setFieldShort(ptr, target->cff.access_flags);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "status", "I", runtime);
        //VERIFIED = 1;PREPARED = 2;INITIALIZED = 4;ERROR = 8;
        if (ptr)
            setFieldInt(ptr, target->status >= CLASS_STATUS_CLINITED ?
                             7 : (target->status >= CLASS_STATUS_PREPARED ? 3 : 1));
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "source", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *source = jstring_create(target->source, runtime);
            setFieldRefer(ptr, source);
        }
        //
        s32 i;
        {
            ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "fieldIds", "[J", runtime);
            if (ptr) {
                Instance *jarr = jarray_create_by_type_index(runtime, target->fieldPool.field_used, DATATYPE_LONG);
                setFieldRefer(ptr, jarr);
                for (i = 0; i < target->fieldPool.field_used; i++) {
                    s64 val = (u64) (intptr_t) &target->fieldPool.field[i];
                    jarray_set_field(jarr, i, val);
                }
            }
        }
        //
        {
            ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "methodIds", "[J", runtime);
            if (ptr) {
                Instance *jarr = jarray_create_by_type_index(runtime, target->methodPool.method_used, DATATYPE_LONG);
                setFieldRefer(ptr, jarr);
                for (i = 0; i < target->methodPool.method_used; i++) {
                    s64 val = (u64) (intptr_t) &target->methodPool.method[i];
                    jarray_set_field(jarr, i, val);
                }
            }
        }
        //
        {
            ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "interfaces", "[J", runtime);
            if (ptr) {
                Instance *jarr = jarray_create_by_type_index(runtime, target->interfacePool.clasz_used, DATATYPE_LONG);
                setFieldRefer(ptr, jarr);
                for (i = 0; i < target->interfacePool.clasz_used; i++) {
                    JClass *cl = classes_load_get(target->interfacePool.clasz[i].name, runtime);
                    s64 val = (u64) (intptr_t) cl;
                    jarray_set_field(jarr, i, val);
                }
            }
        }
    }
    return 0;
}

s32 org_mini_reflect_ReflectField_mapField(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FieldInfo *fieldInfo = (__refer) (intptr_t) l2d.l;
    if (ins && fieldInfo) {
        c8 *ptr;
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_FIELD, "fieldName", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *fieldName = jstring_create(fieldInfo->name, runtime);
            setFieldRefer(ptr, fieldName);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_FIELD, "accessFlags", "S", runtime);
        if (ptr)setFieldShort(ptr, fieldInfo->access_flags);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_FIELD, "signature", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *signature = jstring_create(fieldInfo->descriptor, runtime);
            setFieldRefer(ptr, signature);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_ARRAY, "type", "B", runtime);
        if (ptr)setFieldByte(ptr, (s8) utf8_char_at(fieldInfo->descriptor, 1));
    }
    return 0;
}

Instance *localVarTable2java(JClass *clazz, LocalVarTable *lvt, Runtime *runtime) {
    JClass *cl = classes_load_get_c(JDWP_CLASS_LOCALVARTABLE, runtime);
    Instance *ins = instance_create(runtime, cl);
    gc_refer_hold(ins);// hold by manual
    instance_init(ins, runtime);

    if (ins && lvt) {
        c8 *ptr;
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_LOCALVARTABLE, "name", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *name = jstring_create(class_get_utf8_string(clazz, lvt->name_index), runtime);
            setFieldRefer(ptr, name);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_LOCALVARTABLE, "signature", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *signature = jstring_create(class_get_utf8_string(clazz, lvt->descriptor_index), runtime);
            setFieldRefer(ptr, signature);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_LOCALVARTABLE, "codeIndex", "J", runtime);
        if (ptr)setFieldLong(ptr, lvt->start_pc);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_LOCALVARTABLE, "length", "I", runtime);
        if (ptr)setFieldInt(ptr, lvt->length);
    }
    gc_refer_release(ins);//release by manual
    return ins;
}

s32 org_mini_reflect_ReflectMethod_mapMethod(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    MethodInfo *methodInfo = (__refer) (intptr_t) l2d.l;
    if (ins && methodInfo) {
        c8 *ptr;

        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "methodName", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *methodName = jstring_create(methodInfo->name, runtime);
            setFieldRefer(ptr, methodName);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "signature", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *signature = jstring_create(methodInfo->descriptor, runtime);
            setFieldRefer(ptr, signature);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "accessFlags", "S", runtime);
        if (ptr)setFieldShort(ptr, methodInfo->access_flags);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "argCnt", "I", runtime);
        if (ptr)setFieldInt(ptr, methodInfo->para_slots);
        //
        s32 i;

        CodeAttribute *ca = methodInfo->converted_code;
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "codeStart", "J", runtime);
        if (ptr)setFieldLong(ptr, ca ? 0 : -1);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "codeEnd", "J", runtime);
        if (ptr)setFieldLong(ptr, ca ? ca->attribute_length : -1);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "lines", "I", runtime);
        if (ptr)setFieldInt(ptr, ca ? ca->line_number_table_length : 0);
        //
        if (ca) {
            {
                ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "lineNum", "[S", runtime);
                if (ptr) {
                    Instance *jarr = jarray_create_by_type_index(runtime, ca->line_number_table_length * 2, DATATYPE_SHORT);
                    setFieldRefer(ptr, jarr);
                    memcpy(jarr->arr_body, ca->line_number_table,
                           ca->line_number_table_length * 4);
                }
            }
            {
                //
                c8 *table_type = "org/mini/reflect/LocalVarTable";
                ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "localVarTable", table_type, runtime);
                if (ptr) {
                    Utf8String *ustr = utf8_create_c(table_type);
                    utf8_substring(ustr, 1, ustr->length);
                    Instance *jarr = jarray_create_by_type_name(runtime, ca->local_var_table_length, ustr);
                    setFieldRefer(ptr, jarr);
                    utf8_destory(ustr);
                    for (i = 0; i < ca->local_var_table_length; i++) {
                        LocalVarTable *lvt = &ca->local_var_table[i];
                        s64 val = (intptr_t) localVarTable2java(methodInfo->_this_class, lvt, runtime);
                        jarray_set_field(jarr, i, val);

                    }
                }
            }
        }

    }
    return 0;
}


s32 org_mini_reflect_ReflectMethod_invokeMethod(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *method_ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    MethodInfo *methodInfo = (__refer) (intptr_t) l2d.l;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Instance *argsArr = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    s32 ret = 0;
    if (methodInfo) {
        if (!(methodInfo->access_flags & ACC_STATIC)) {
            push_ref(runtime->stack, ins);
        }
        s32 i;
        for (i = 0; i < argsArr->arr_length; i++) {
            s64 val = jarray_get_field(argsArr, i);
            switch (utf8_char_at(methodInfo->paraType, i)) {
                case '4': {
                    push_int(runtime->stack, (s32) val);
                    break;
                }
                case '8': {
                    push_long(runtime->stack, val);
                    break;
                }
                case 'R': {
                    push_ref(runtime->stack, (__refer) (intptr_t) val);
                    break;
                }
            }
        }
        ret = execute_method_impl(methodInfo, runtime);
        if (ret == RUNTIME_STATUS_NORMAL) {
            utf8_char ch = utf8_char_at(methodInfo->returnType, 0);
            c8 *clsName = "org/mini/reflect/DataWrap";
            JClass *dwcl = classes_load_get_c(clsName, runtime);
            Instance *result = instance_create(runtime, dwcl);
            gc_refer_hold(result);
            instance_init(result, runtime);

            if (ch == 'V') {
            } else if (isDataReferByTag(ch)) {
                __refer ov = pop_ref(runtime->stack);
                c8 *ptr = getFieldPtr_byName_c(result, clsName, "ov", STR_INS_JAVA_LANG_OBJECT, runtime);
                setFieldRefer(ptr, ov);
            } else {
                long nv;
                if (isData8ByteByTag(ch)) {
                    nv = pop_long(runtime->stack);
                } else {
                    nv = pop_int(runtime->stack);
                }
                c8 *ptr = getFieldPtr_byName_c(result, clsName, "nv", "J", runtime);
                setFieldLong(ptr, nv);
            }
            gc_refer_release(result);
            push_ref(runtime->stack, result);
        } else {
            print_exception(runtime);
        }
    } else
        push_ref(runtime->stack, NULL);
    return ret;
}

s32 org_mini_reflect_StackFrame_mapRuntime(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    Runtime *target = (__refer) (intptr_t) l2d.l;
    if (ins && target) {
        c8 *ptr;
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "classId", "J", runtime);
        if (ptr)setFieldLong(ptr, (u64) (intptr_t) target->clazz);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "parentId", "J", runtime);
        if (ptr)
            if ((target->parent) && (target->parent->parent))
                setFieldLong(ptr, (u64) (intptr_t) target->parent);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "pc", "J", runtime);
        if (ptr)setFieldLong(ptr, (u64) (intptr_t) target->pc);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "byteCode", "J", runtime);
        if (ptr)setFieldLong(ptr, target->ca ? (u64) (intptr_t) target->ca->code : 0);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "methodId", "J", runtime);
        if (ptr)setFieldLong(ptr, (u64) (intptr_t) target->method);
        //
        if (target->method && !(target->method->access_flags & ACC_STATIC)) {//top runtime method is null
            ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "localThis", "J", runtime);
            if (ptr)setFieldLong(ptr, (s64) (intptr_t) localvar_getRefer(target->localvar, 0));
        }
    }
    return 0;
}

s32 org_mini_reflect_ReflectMethod_findMethod0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jstr_clsName = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Instance *jstr_methodName = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Instance *jstr_methodDesc = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr_clsName = utf8_create();
    Utf8String *ustr_methodName = utf8_create();
    Utf8String *ustr_methodDesc = utf8_create();

    jstring_2_utf8(jstr_clsName, ustr_clsName);
    jstring_2_utf8(jstr_methodName, ustr_methodName);
    jstring_2_utf8(jstr_methodDesc, ustr_methodDesc);

    MethodInfo *mi = find_methodInfo_by_name(ustr_clsName, ustr_methodName, ustr_methodDesc, runtime);

    utf8_destory(ustr_clsName);
    utf8_destory(ustr_methodName);
    utf8_destory(ustr_methodDesc);
    push_long(runtime->stack, (s64) (intptr_t) mi);
    return 0;
}

s32 org_mini_reflect_ReflectArray_mapArray(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    Instance *target = (__refer) (intptr_t) l2d.l;
    if (ins && target) {
        c8 *ptr;
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_ARRAY, "length", "I", runtime);
        if (ptr)setFieldInt(ptr, target->arr_length);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_ARRAY, "body_addr", "J", runtime);
        if (ptr)setFieldLong(ptr, (u64) (intptr_t) target->arr_body);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_ARRAY, "typeTag", "B", runtime);
        if (ptr)setFieldByte(ptr, (s8) utf8_char_at(target->mb.clazz->name, 1));
        //

    }
    return 0;
}

s32 org_mini_reflect_ReflectArray_getLength(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jarr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;

    push_int(runtime->stack, (jarr == NULL || jarr->mb.type != MEM_TYPE_ARR) ? 0 : jarr->arr_length);

    return 0;
}

s32 org_mini_reflect_ReflectArray_getTypeTag(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jarr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;

    push_int(runtime->stack, (jarr == NULL || jarr->mb.type != MEM_TYPE_ARR) ? 0 : (s8) utf8_char_at(jarr->mb.clazz->name, 1));

    return 0;
}

s32 org_mini_reflect_ReflectArray_getArrayBodyPtr(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jarr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;

    push_long(runtime->stack, (jarr == NULL || jarr->mb.type != MEM_TYPE_ARR) ? 0 : (s64) (intptr_t) jarr->arr_body);

    return 0;
}


s32 org_mini_reflect_DirectMemObj_setVal(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *dmo = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos++);
    s32 index = localvar_getInt(runtime->localvar, pos++);
    s64 val = localvar_getLong(runtime->localvar, pos);

    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_memAddr));
    s32 len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_length));
    c8 desc = getFieldChar(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_desc));

    s32 ret = 0;
    if (memAddr && index >= 0 && index < len) {
        switch (desc) {
            case '1': {
                setFieldByte((c8 *) ((c8 *) memAddr + index), (c8) val);
                break;
            }
            case '2': {
                setFieldShort((c8 *) ((s16 *) memAddr + index), (s16) val);
                break;
            }
            case '4': {
                setFieldInt((c8 *) ((s32 *) memAddr + index), (s32) val);
                break;
            }
            case '8': {
                setFieldLong((c8 *) ((s64 *) memAddr + index), (s64) val);
                break;
            }
            case 'R': {
                setFieldRefer((c8 *) ((__refer *) memAddr + index), (__refer) (intptr_t) val);
                break;
            }
        }
    } else {
        Instance *exception = exception_create(JVM_EXCEPTION_ILLEGALARGUMENT, runtime);
        push_ref(runtime->stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    }

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_DirectMemObj_setVal\n");
#endif
    return ret;
}

s32 org_mini_reflect_DirectMemObj_getVal(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *dmo = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos++);
    s32 index = localvar_getInt(runtime->localvar, pos++);

    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_memAddr));
    s32 len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_length));
    c8 desc = getFieldChar(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_desc));

    s32 ret = 0;
    if (memAddr && index >= 0 && index < len) {
        s64 val;
        switch (desc) {
            case '1': {
                val = getFieldByte((c8 *) ((c8 *) memAddr + index));
                break;
            }
            case '2': {
                val = getFieldShort((c8 *) ((s16 *) memAddr + index));
                break;
            }
            case '4': {
                val = getFieldInt((c8 *) ((s32 *) memAddr + index));
                break;
            }
            case '8': {
                val = getFieldLong((c8 *) ((s64 *) memAddr + index));
                break;
            }
            case 'R': {
                val = (s64) (intptr_t) getFieldRefer((c8 *) ((__refer *) memAddr + index));
                break;
            }
        }
        push_long(runtime->stack, val);
    } else {
        Instance *exception = exception_create(JVM_EXCEPTION_ILLEGALARGUMENT, runtime);
        push_ref(runtime->stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    }

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_DirectMemObj_getVal\n");
#endif
    return ret;
}

s32 org_mini_reflect_DirectMemObj_copyTo0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *dmo = localvar_getRefer(runtime->localvar, pos++);
    s32 src_off = localvar_getInt(runtime->localvar, pos++);
    Instance *tgt = localvar_getRefer(runtime->localvar, pos++);
    s32 tgt_off = localvar_getInt(runtime->localvar, pos++);
    s32 copy_len = localvar_getInt(runtime->localvar, pos++);

    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_memAddr));
    s32 dmo_len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_length));

    s32 ret = 0;
    if (src_off + copy_len > dmo_len
        || tgt_off + copy_len > tgt->arr_length) {
        Instance *exception = exception_create(JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, runtime);
        push_ref(runtime->stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    } else {
        s32 bytes = data_type_bytes[tgt->mb.arr_type_index];
        memcpy((c8 *) tgt->arr_body + (bytes * tgt_off), (c8 *) memAddr + (bytes * src_off), copy_len * (bytes));
    }


#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_DirectMemObj_copyTo0\n");
#endif
    return ret;
}

s32 org_mini_reflect_DirectMemObj_copyFrom0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *dmo = localvar_getRefer(runtime->localvar, pos++);
    s32 src_off = localvar_getInt(runtime->localvar, pos++);
    Instance *src = localvar_getRefer(runtime->localvar, pos++);
    s32 tgt_off = localvar_getInt(runtime->localvar, pos++);
    s32 copy_len = localvar_getInt(runtime->localvar, pos++);

    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_memAddr));
    s32 dmo_len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache.dmo_length));

    s32 ret = 0;
    if (src_off + copy_len > src->arr_length
        || tgt_off + copy_len > dmo_len) {
        Instance *exception = exception_create(JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, runtime);
        push_ref(runtime->stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    } else {
        s32 bytes = data_type_bytes[src->mb.arr_type_index];
        memcpy((c8 *) memAddr + (bytes * tgt_off), (c8 *) src->arr_body + (bytes * src_off), copy_len * (bytes));
    }


#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    jvm_printf("org_mini_reflect_DirectMemObj_copyFrom0\n");
#endif
    return ret;
}

s32 org_mini_reflect_DirectMemObj_heap_calloc(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    s32 size = localvar_getInt(runtime->localvar, pos++);

    __refer ptr = jvm_calloc(size);

    push_long(runtime->stack, (s64) (intptr_t) ptr);

    return 0;
}

s32 org_mini_reflect_DirectMemObj_heap_free(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;

    jvm_free(ptr);

    return 0;
}


static java_native_method method_jdwp_table[] = {
        {"org/mini/reflect/vm/RefNative",  "refIdSize",             "()I",                                                              org_mini_reflect_vm_RefNative_refIdSize},
        {"org/mini/reflect/vm/RefNative",  "obj2id",                "(Ljava/lang/Object;)J",                                            org_mini_reflect_vm_RefNative_obj2id},
        {"org/mini/reflect/vm/RefNative",  "id2obj",                "(J)Ljava/lang/Object;",                                            org_mini_reflect_vm_RefNative_id2obj},
        {"org/mini/reflect/vm/RefNative",  "getClasses",            "()[Ljava/lang/Class;",                                             org_mini_reflect_vm_RefNative_getClasses},
        {"org/mini/reflect/vm/RefNative",  "getClassByName",        "(Ljava/lang/String;)Ljava/lang/Class;",                            org_mini_reflect_vm_RefNative_getClassByName},
        {"org/mini/reflect/vm/RefNative",  "newWithoutInit",        "(Ljava/lang/Class;)Ljava/lang/Object;",                            org_mini_reflect_vm_RefNative_newWithoutInit},
        {"org/mini/reflect/vm/RefNative",  "setLocalVal",           "(JIBJI)I",                                                         org_mini_reflect_vm_RefNative_setLocalVal},
        {"org/mini/reflect/vm/RefNative",  "getLocalVal",           "(JILorg/mini/jdwp/type/ValueType;)I",                              org_mini_reflect_vm_RefNative_getLocalVal},
        {"org/mini/reflect/vm/RefNative",  "getThreads",            "()[Ljava/lang/Thread;",                                            org_mini_reflect_vm_RefNative_getThreads},
        {"org/mini/reflect/vm/RefNative",  "getStatus",             "(Ljava/lang/Thread;)I",                                            org_mini_reflect_vm_RefNative_getStatus},
        {"org/mini/reflect/vm/RefNative",  "suspendThread",         "(Ljava/lang/Thread;)I",                                            org_mini_reflect_vm_RefNative_suspendThread},
        {"org/mini/reflect/vm/RefNative",  "resumeThread",          "(Ljava/lang/Thread;)I",                                            org_mini_reflect_vm_RefNative_resumeThread},
        {"org/mini/reflect/vm/RefNative",  "getSuspendCount",       "(Ljava/lang/Thread;)I",                                            org_mini_reflect_vm_RefNative_getSuspendCount},
        {"org/mini/reflect/vm/RefNative",  "getFrameCount",         "(Ljava/lang/Thread;)I",                                            org_mini_reflect_vm_RefNative_getFrameCount},
        {"org/mini/reflect/vm/RefNative",  "stopThread",            "(Ljava/lang/Thread;J)I",                                           org_mini_reflect_vm_RefNative_stopThread},
        {"org/mini/reflect/vm/RefNative",  "getStackFrame",         "(Ljava/lang/Thread;)J",                                            org_mini_reflect_vm_RefNative_getStackFrame},
        {"org/mini/reflect/vm/RefNative",  "getGarbageReferedObjs", "()[Ljava/lang/Object;",                                            org_mini_reflect_vm_RefNative_getGarbageReferedObjs},
        {"org/mini/reflect/vm/RefNative",  "getGarbageStatus",      "()I",                                                              org_mini_reflect_vm_RefNative_getGarbageStatus},
        {"org/mini/reflect/vm/RefNative",  "defineClass",           "(Ljava/lang/ClassLoader;Ljava/lang/String;[BII)Ljava/lang/Class;", org_mini_reflect_vm_RefNative_defineClass},
        {"org/mini/reflect/vm/RefNative",  "addJarToClasspath",     "(Ljava/lang/String;)V",                                            org_mini_reflect_vm_RefNative_addJarToClasspath},
        {"org/mini/reflect/ReflectClass",  "mapReference",          "(J)V",                                                             org_mini_reflect_ReflectClass_mapReference},
        {"org/mini/reflect/ReflectField",  "mapField",              "(J)V",                                                             org_mini_reflect_ReflectField_mapField},
        {"org/mini/reflect/ReflectField",  "getFieldVal",           "(JJ)J",                                                            org_mini_reflect_ReflectField_getFieldVal},
        {"org/mini/reflect/ReflectField",  "setFieldVal",           "(JJJ)V",                                                           org_mini_reflect_ReflectField_setFieldVal},
        {"org/mini/reflect/ReflectMethod", "mapMethod",             "(J)V",                                                             org_mini_reflect_ReflectMethod_mapMethod},
        {"org/mini/reflect/ReflectMethod", "invokeMethod",          "(JLjava/lang/Object;[J)Lorg/mini/reflect/DataWrap;",               org_mini_reflect_ReflectMethod_invokeMethod},
        {"org/mini/reflect/ReflectMethod", "findMethod0",           "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)J",        org_mini_reflect_ReflectMethod_findMethod0},
        {"org/mini/reflect/StackFrame",    "mapRuntime",            "(J)V",                                                             org_mini_reflect_StackFrame_mapRuntime},
        {"org/mini/reflect/ReflectArray",  "mapArray",              "(J)V",                                                             org_mini_reflect_ReflectArray_mapArray},
        {"org/mini/reflect/ReflectArray",  "getLength",             "(J)I",                                                             org_mini_reflect_ReflectArray_getLength},
        {"org/mini/reflect/ReflectArray",  "getTypeTag",            "(J)B",                                                             org_mini_reflect_ReflectArray_getTypeTag},
        {"org/mini/reflect/ReflectArray",  "getBodyPtr",            "(J)J",                                                             org_mini_reflect_ReflectArray_getArrayBodyPtr},
        {"org/mini/reflect/ReflectArray",  "newArray",              "(Ljava/lang/Class;I)Ljava/lang/Object;",                           org_mini_reflect_ReflectArray_newArray},
        {"org/mini/reflect/ReflectArray",  "multiNewArray",         "(Ljava/lang/Class;[I)Ljava/lang/Object;",                          org_mini_reflect_ReflectArray_multiNewArray},
        {"org/mini/reflect/DirectMemObj",  "setVal",                "(IJ)V",                                                            org_mini_reflect_DirectMemObj_setVal},
        {"org/mini/reflect/DirectMemObj",  "getVal",                "(I)J",                                                             org_mini_reflect_DirectMemObj_getVal},
        {"org/mini/reflect/DirectMemObj",  "copyTo0",               "(ILjava/lang/Object;II)V",                                         org_mini_reflect_DirectMemObj_copyTo0},
        {"org/mini/reflect/DirectMemObj",  "copyFrom0",             "(ILjava/lang/Object;II)V",                                         org_mini_reflect_DirectMemObj_copyFrom0},
        {"org/mini/reflect/DirectMemObj",  "heap_calloc",           "(I)J",                                                             org_mini_reflect_DirectMemObj_heap_calloc},
        {"org/mini/reflect/DirectMemObj",  "heap_free",             "(J)V",                                                             org_mini_reflect_DirectMemObj_heap_free},

};


void reg_jdwp_native_lib() {
    native_reg_lib(&(method_jdwp_table[0]), sizeof(method_jdwp_table) / sizeof(java_native_method));
}

#ifdef __cplusplus
}
#endif
