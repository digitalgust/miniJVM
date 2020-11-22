//
// Created by gust on 2017/9/1.
//

#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"


#ifdef __cplusplus
extern "C" {
#endif

static c8 *JDWP_CLASS_REFERENCE = "org/mini/reflect/ReflectClass";
static c8 *JDWP_CLASS_FIELD = "org/mini/reflect/ReflectField";
static c8 *JDWP_CLASS_METHOD = "org/mini/reflect/ReflectMethod";
static c8 *JDWP_CLASS_ARRAY = "org/mini/reflect/ReflectArray";
static c8 *JDWP_CLASS_RUNTIME = "org/mini/reflect/StackFrame";
static c8 *JDWP_CLASS_LOCALVARTABLE = "org/mini/reflect/LocalVarTable";
static c8 *JDWP_CLASS_VALUETYPE = "org/mini/reflect/vm/ValueType";

//========  native============================================================================

s32 org_mini_reflect_vm_RefNative_refIdSize(Runtime *runtime, JClass *clazz) {
    push_int(runtime->stack, sizeof(__refer));

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_refIdSize\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_obj2id(Runtime *runtime, JClass *clazz) {
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Long2Double l2d;
    l2d.l = (u64) (intptr_t) ins;
    push_long(runtime->stack, l2d.l);

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_obj2id\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_id2obj(Runtime *runtime, JClass *clazz) {
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    __refer r = (__refer) (intptr_t) l2d.l;//这里不能直接转化，可能在外部发生了数据精度丢失，只能从低位强转
    push_ref(runtime->stack, r);

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_jdwp_RefNative_id2obj\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getClasses(Runtime *runtime, JClass *clazz) {

    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_CLASS);
    Instance *jarr = NULL;
    utf8_destory(ustr);

    MiniJVM *jvm = runtime->jvm;
    spin_lock(&jvm->lock_cloader);
    {
        s32 i, count = 0;
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            count += pcl->classes->entries;
        }
        jarr = jarray_create_by_type_name(runtime, count, ustr);
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            HashtableIterator hti;
            hashtable_iterate(pcl->classes, &hti);
            for (; hashtable_iter_has_more(&hti);) {
                Utf8String *k = hashtable_iter_next_key(&hti);
                JClass *cl = hashtable_get(pcl->classes, k);
                jarray_set_field(jarr, i, (intptr_t) insOfJavaLangClass_create_get(runtime, cl));
            }
        }
    }
    spin_unlock(&jvm->lock_cloader);
    push_ref(runtime->stack, jarr);//先放入栈，再关联回收器，防止多线程回收

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getClasses\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getBootstrapClassByName(Runtime *runtime, JClass *clazz) {
    Instance *jstr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Utf8String *ustr = utf8_create();
    jstring_2_utf8(jstr, ustr, runtime);
    utf8_replace_c(ustr, ".", "/");
    JClass *cl = classes_load_get(NULL, ustr, runtime);
    utf8_destory(ustr);
    if (cl) {
        push_ref(runtime->stack, insOfJavaLangClass_create_get(runtime, cl));
    } else {
        push_ref(runtime->stack, NULL);
    }
    return 0;
}


s32 org_mini_reflect_vm_RefNative_newWithoutInit(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, 0));
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
#if _JVM_DEBUG_LOG_LEVEL > 5
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
    Instance *ins = localvar_getRefer(runtime->localvar, pos);
    pos++;
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
    if (fi->isrefer) {
        val = (s64) (intptr_t) getFieldRefer(fptr);
    } else {
        switch (fi->datatype_bytes) {
            case 8:
                val = getFieldLong(fptr);
                break;
            case 4:
                val = getFieldInt(fptr);
                break;
            case 2:
                if (fi->datatype_idx == DATATYPE_JCHAR) {
                    val = getFieldChar(fptr);
                } else {
                    val = getFieldShort(fptr);
                }
                break;
            case 1:
                val = getFieldByte(fptr);
                break;
        }
    }
    push_long(runtime->stack, val);

    return 0;
}

s32 org_mini_reflect_ReflectField_setFieldVal(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Long2Double l2d;
    Instance *ins = localvar_getRefer(runtime->localvar, pos);
    pos++;
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
    if (fi->isrefer) {
        setFieldRefer(fptr, (__refer) (intptr_t) val);
    } else {
        switch (fi->datatype_bytes) {
            case 8:
                setFieldLong(fptr, (s64) val);
                break;
            case 4:
                setFieldInt(fptr, (s32) val);
                break;
            case 2:
                setFieldShort(fptr, (s16) val);
                break;
            case 1:
                setFieldByte(fptr, (s8) val);
                break;
        }
    }
    return 0;
}


s32 org_mini_reflect_ReflectArray_newArray(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, pos++));
    s32 count = localvar_getInt(runtime->localvar, pos++);
    if (cl->mb.clazz->primitive) {
        u8 t = getDataTypeTagByName(cl->name);
        s32 typeIndex = getDataTypeIndex(t);
        Instance *arr = jarray_create_by_type_index(runtime, count, typeIndex);
        push_ref(runtime->stack, arr);
    } else {
        Instance *arr = jarray_create_by_type_name(runtime, count, cl->name);
        push_ref(runtime->stack, arr);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_ReflectArray_newArray\n");
#endif
    return 0;
}

s32 org_mini_reflect_ReflectArray_multiNewArray(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    JClass *cl = insOfJavaLangClass_get_classHandle(runtime, (Instance *) localvar_getRefer(runtime->localvar, pos++));
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
    utf8_destory(desc);
    push_ref(runtime->stack, arr);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_ReflectArray_multiNewArray\n");
#endif
    return 0;
}


struct _ListGetThreadPara {
    s32 i;
    Instance *jarr;
    s64 val;
};

void _list_iter_getthread(ArrayListValue value, void *para) {
    if (value) {
        Runtime *r = value;
        struct _ListGetThreadPara *p = para;
        p->val = (intptr_t) r->thrd_info->jthread;
        jarray_set_field(p->jarr, p->i, p->val);
    }
}

s32 org_mini_reflect_vm_RefNative_getThreads(Runtime *runtime, JClass *clazz) {
//    garbage_thread_lock();
    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_THREAD);
    Instance *jarr = jarray_create_by_type_name(runtime, runtime->jvm->thread_list->length, ustr);
    utf8_destory(ustr);

    struct _ListGetThreadPara para;
    para.i = 0;
    para.jarr = jarr;
    arraylist_iter_safe(runtime->jvm->thread_list, _list_iter_getthread, &para);
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
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getThreads\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getStatus(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun)
        push_int(runtime->stack, trun->thrd_info->thread_status);
    else
        push_int(runtime->stack, THREAD_STATUS_ZOMBIE);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("com_egls_jvm_RefNative_getStatus\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_suspendThread(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        jthread_suspend(trun);
        push_int(runtime->stack, 0);
    } else
        push_int(runtime->stack, 1);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("com_egls_jvm_RefNative_suspendThread\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_resumeThread(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        jthread_resume(trun);
        push_int(runtime->stack, 0);
    } else
        push_int(runtime->stack, 1);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("com_egls_jvm_RefNative_resumeThread\n");
#endif
    return 0;
}


s32 org_mini_reflect_vm_RefNative_getSuspendCount(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        push_int(runtime->stack, trun->thrd_info->suspend_count);
    } else
        push_int(runtime->stack, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("com_egls_jvm_RefNative_getSuspendCount\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getFrameCount(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
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
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        push_int(runtime->stack, 0);
    } else
        push_int(runtime->stack, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("com_egls_jvm_RefNative_stopThread\n");
#endif
    return 0;
}


s32 org_mini_reflect_vm_RefNative_getStackFrame(Runtime *runtime, JClass *clazz) {
    Instance *thread = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Runtime *trun = (Runtime *) jthread_get_stackframe_value(runtime->jvm, thread);//线程结束之后会清除掉runtime,因为其是一个栈变量，不可再用
    if (trun) {
        while (trun) {
            if (!trun->son)break;
            trun = trun->son;
        }
        push_long(runtime->stack, (u64) (intptr_t) trun->parent);
    } else
        push_long(runtime->stack, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getStackFrame %llx\n", (u64) (intptr_t) trun);
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getGarbageMarkCounter(Runtime *runtime, JClass *clazz) {
    push_int(runtime->stack, runtime->jvm->collector->mark_cnt);

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getGarbageMarkCounter \n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getGarbageStatus(Runtime *runtime, JClass *clazz) {

    push_int(runtime->stack, runtime->jvm->collector->_garbage_thread_status);//先放入栈，再关联回收器，防止多线程回收

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getGarbageStatus %d\n", runtime->jvm->collector->_garbage_thread_status);
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
    JClass *cl = class_parse(cloader, bytebuf, runtime);
    bytebuf_destory(bytebuf);

    if (!cl->source)cl->source = cl->name; //maybe lambda generated class

    Instance *clIns = insOfJavaLangClass_create_get(runtime, cl);

    setFieldRefer(getInstanceFieldPtr(clIns, runtime->jvm->shortcut.class_classLoader), cloader);

    push_ref(runtime->stack, clIns);
//    push_ref(runtime->stack, NULL);

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_defineClass %d\n", runtime->jvm->collector->_garbage_thread_status);
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_findLoadedClass0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jloader = localvar_getRefer(runtime->localvar, pos++);
    Instance *namejstr = localvar_getRefer(runtime->localvar, pos++);

    Utf8String *ustr = utf8_create();
    jstring_2_utf8(namejstr, ustr, runtime);
    JClass *cl = classes_get(runtime->jvm, jloader, ustr);
    utf8_destory(ustr);
    if (cl) {
        Instance *clIns = insOfJavaLangClass_create_get(runtime, cl);
        push_ref(runtime->stack, clIns);
    } else {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_findLoadedClass0 \n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_initNativeClassLoader(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jloader = localvar_getRefer(runtime->localvar, pos++);
    Instance *parent = localvar_getRefer(runtime->localvar, pos++);

    MiniJVM *jvm = runtime->jvm;

    PeerClassLoader *cloader = classloader_create(jvm);
    cloader->jloader = jloader;
    cloader->parent = parent;

    classloaders_add(jvm, cloader);


#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_initNativeClassLoader \n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_destroyNativeClassLoader(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jloader = localvar_getRefer(runtime->localvar, pos++);
    Instance *parent = localvar_getRefer(runtime->localvar, pos++);

    MiniJVM *jvm = runtime->jvm;

    PeerClassLoader *cloader = classLoaders_find_by_instance(jvm, jloader);
    cloader->jloader = jloader;
    cloader->parent = parent;


    classloaders_remove(jvm, cloader);
    classloader_destory(cloader);


#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_destroyNativeClassLoader \n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_getCallerClass(Runtime *runtime, JClass *clazz) {
    s32 found = 0;
    if (runtime->parent) {
        if (runtime->parent->parent) {
            push_ref(runtime->stack, runtime->parent->parent->clazz->ins_class);
            found = 1;
        }
    }
    if (!found) {
        push_ref(runtime->stack, NULL);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_vm_RefNative_getCallerClass\n");
#endif
    return 0;
}

s32 org_mini_reflect_vm_RefNative_addJarToClasspath(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jstr = localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr = utf8_create();

    jstring_2_utf8(jstr, ustr, runtime);

    classloader_add_jar_path(runtime->jvm->boot_classloader, ustr);
    utf8_destory(ustr);

    return 0;
}

s32 org_mini_reflect_ReflectClass_mapClass(Runtime *runtime, JClass *clazz) {
    int pos = 0;
    Instance *ins = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Long2Double l2d;
    l2d.l = l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    JClass *target = (__refer) (intptr_t) l2d.l;
    if (target) {
        c8 *ptr;
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "classObj", STR_INS_JAVA_LANG_CLASS, runtime);
        if (ptr) {
            setFieldRefer(ptr, insOfJavaLangClass_create_get(runtime, target));
        }
        //
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
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_REFERENCE, "signature", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *signature = jstring_create(target->signature, runtime);
            setFieldRefer(ptr, signature);
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
                    JClass *cl = classes_load_get(ins->mb.clazz->jloader, target->interfacePool.clasz[i].name, runtime);
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
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_FIELD, "descriptor", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *descriptor = jstring_create(fieldInfo->descriptor, runtime);
            setFieldRefer(ptr, descriptor);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_FIELD, "signature", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *signature = jstring_create(fieldInfo->signature, runtime);
            setFieldRefer(ptr, signature);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_FIELD, "type", "B", runtime);
        if (ptr)setFieldByte(ptr, (s8) utf8_char_at(fieldInfo->descriptor, 0));
    }
    return 0;
}

Instance *localVarTable2java(JClass *clazz, LocalVarTable *lvt, Runtime *runtime) {
    JClass *cl = classes_load_get_c(NULL, JDWP_CLASS_LOCALVARTABLE, runtime);
    Instance *ins = instance_create(runtime, cl);
    instance_hold_to_thread(ins, runtime);// hold by manual
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
    instance_release_from_thread(ins, runtime);//release by manual
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
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "descriptor", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *descriptor = jstring_create(methodInfo->descriptor, runtime);
            setFieldRefer(ptr, descriptor);
        }
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_METHOD, "signature", STR_INS_JAVA_LANG_STRING, runtime);
        if (ptr) {
            Instance *signature = jstring_create(methodInfo->signature, runtime);
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
        if (!(methodInfo->is_static)) {
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
            JClass *dwcl = classes_load_get_c(NULL, clsName, runtime);
            Instance *result = instance_create(runtime, dwcl);
            instance_hold_to_thread(result, runtime);
            instance_init(result, runtime);

            if (ch == 'V') {
            } else if (isDataReferByTag(ch)) {
                __refer ov = pop_ref(runtime->stack);
                c8 *ptr = getFieldPtr_byName_c(result, clsName, "ov", STR_INS_JAVA_LANG_OBJECT, runtime);
                setFieldRefer(ptr, ov);
            } else {
                s64 nv;
                if (isData8ByteByTag(ch)) {
                    nv = pop_long(runtime->stack);
                } else {
                    nv = pop_int(runtime->stack);
                }
                c8 *ptr = getFieldPtr_byName_c(result, clsName, "nv", "J", runtime);
                setFieldLong(ptr, nv);
            }
            instance_release_from_thread(result, runtime);
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
        if (ptr)setFieldLong(ptr, target->method->converted_code ? (u64) (intptr_t) target->method->converted_code->code : 0);
        //
        ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "methodId", "J", runtime);
        if (ptr)setFieldLong(ptr, (u64) (intptr_t) target->method);
        //
        if (target->method && !(target->method->is_static)) {//top runtime method is null
            ptr = getFieldPtr_byName_c(ins, JDWP_CLASS_RUNTIME, "localThis", "J", runtime);
            if (ptr)setFieldLong(ptr, (s64) (intptr_t) localvar_getRefer(target->localvar, 0));
        }
    }
    return 0;
}

s32 org_mini_reflect_ReflectMethod_findMethod0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jloader = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Instance *jstr_clsName = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Instance *jstr_methodName = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Instance *jstr_methodDesc = (Instance *) localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr_clsName = utf8_create();
    Utf8String *ustr_methodName = utf8_create();
    Utf8String *ustr_methodDesc = utf8_create();

    jstring_2_utf8(jstr_clsName, ustr_clsName, runtime);
    jstring_2_utf8(jstr_methodName, ustr_methodName, runtime);
    jstring_2_utf8(jstr_methodDesc, ustr_methodDesc, runtime);

    MethodInfo *mi = find_methodInfo_by_name(ustr_clsName, ustr_methodName, ustr_methodDesc, jloader, runtime);

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
    Instance *jarr = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos);

    push_int(runtime->stack, (jarr == NULL || jarr->mb.type != MEM_TYPE_ARR) ? 0 : jarr->arr_length);

    return 0;
}

s32 org_mini_reflect_ReflectArray_getTypeTag(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jarr = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos);

    push_int(runtime->stack, (jarr == NULL || jarr->mb.type != MEM_TYPE_ARR) ? 0 : (s8) utf8_char_at(jarr->mb.clazz->name, 1));

    return 0;
}

s32 org_mini_reflect_ReflectArray_getArrayBodyPtr(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *jarr = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos);

    push_long(runtime->stack, (jarr == NULL || jarr->mb.type != MEM_TYPE_ARR) ? 0 : (s64) (intptr_t) jarr->arr_body);

    return 0;
}


s32 org_mini_reflect_DirectMemObj_setVal(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *dmo = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos++);
    s32 index = localvar_getInt(runtime->localvar, pos++);
    s64 val = localvar_getLong(runtime->localvar, pos);

    ShortCut *jvm_runtime_cache = &runtime->jvm->shortcut;
    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_memAddr));
    s32 len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_length));
    c8 desc = getFieldChar(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_desc));

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

#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_DirectMemObj_setVal\n");
#endif
    return ret;
}

s32 org_mini_reflect_DirectMemObj_getVal(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *dmo = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos++);
    s32 index = localvar_getInt(runtime->localvar, pos++);

    ShortCut *jvm_runtime_cache = &runtime->jvm->shortcut;
    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_memAddr));
    s32 len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_length));
    c8 desc = getFieldChar(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_desc));

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

#if _JVM_DEBUG_LOG_LEVEL > 5
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

    ShortCut *jvm_runtime_cache = &runtime->jvm->shortcut;
    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_memAddr));
    s32 dmo_len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_length));

    s32 ret = 0;
    if (src_off + copy_len > dmo_len
        || tgt_off + copy_len > tgt->arr_length) {
        Instance *exception = exception_create(JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, runtime);
        push_ref(runtime->stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    } else {
        s32 bytes = DATA_TYPE_BYTES[tgt->mb.arr_type_index];
        memcpy((c8 *) tgt->arr_body + (bytes * tgt_off), (c8 *) memAddr + (bytes * src_off), copy_len * (bytes));
    }


#if _JVM_DEBUG_LOG_LEVEL > 5
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

    ShortCut *jvm_runtime_cache = &runtime->jvm->shortcut;
    __refer memAddr = getFieldRefer(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_memAddr));
    s32 dmo_len = getFieldInt(getInstanceFieldPtr(dmo, jvm_runtime_cache->dmo_length));

    s32 ret = 0;
    if (src_off + copy_len > src->arr_length
        || tgt_off + copy_len > dmo_len) {
        Instance *exception = exception_create(JVM_EXCEPTION_ARRAYINDEXOUTOFBOUNDS, runtime);
        push_ref(runtime->stack, (__refer) exception);
        ret = RUNTIME_STATUS_EXCEPTION;
    } else {
        s32 bytes = DATA_TYPE_BYTES[src->mb.arr_type_index];
        memcpy((c8 *) memAddr + (bytes * tgt_off), (c8 *) src->arr_body + (bytes * src_off), copy_len * (bytes));
    }


#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("org_mini_reflect_DirectMemObj_copyFrom0\n");
#endif
    return ret;
}

s32 org_mini_reflect_vm_RefNative_heap_calloc(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    s32 size = localvar_getInt(runtime->localvar, pos++);

    __refer ptr = jvm_calloc(size);

    push_long(runtime->stack, (s64) (intptr_t) ptr);

    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_free(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;

    jvm_free(ptr);

    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_put_byte(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    pos++;
    s32 val = localvar_getInt(runtime->localvar, pos);

    *((s8 *) ptr + index) = val;
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_get_byte(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    s32 val = *((s8 *) ptr + index);
    push_int(runtime->stack, val);
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_put_short(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    pos++;
    s32 val = localvar_getInt(runtime->localvar, pos);

    *((s16 *) ((s8 *) ptr + index)) = val;
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_get_short(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    s32 val = *((s16 *) ((s8 *) ptr + index));
    push_int(runtime->stack, val);
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_put_int(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    pos++;
    s32 val = localvar_getInt(runtime->localvar, pos);

    *((s32 *) ((s8 *) ptr + index)) = val;
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_get_int(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    s32 val = *((s32 *) ((s8 *) ptr + index));
    push_int(runtime->stack, val);
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_put_long(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    pos++;
    s64 val = localvar_getLong(runtime->localvar, pos);

    *((s64 *) ((s8 *) ptr + index)) = val;
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_get_long(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    s64 val = *((s64 *) ((s8 *) ptr + index));
    push_long(runtime->stack, val);
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_put_ref(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    pos++;
    __refer val = (__refer) (intptr_t) localvar_getRefer(runtime->localvar, pos);

    *((__refer *) ((s8 *) ptr + index)) = val;
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_get_ref(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer ptr = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 index = localvar_getInt(runtime->localvar, pos);
    __refer val = *((__refer *) ((s8 *) ptr + index));
    push_ref(runtime->stack, val);
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_copy(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    __refer src = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 srcPos = localvar_getInt(runtime->localvar, pos);
    pos++;
    __refer dest = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s32 destPos = localvar_getInt(runtime->localvar, pos);
    pos++;
    s32 len = localvar_getInt(runtime->localvar, pos);
    pos++;

    memcpy((s8 *) dest + destPos, (s8 *) src + srcPos, len);
    return 0;
}

s32 org_mini_reflect_vm_RefNative_heap_little_endian(Runtime *runtime, JClass *clazz) {
    push_int(runtime->stack, __JVM_LITTLE_ENDIAN__);
    return 0;
}

s32 org_mini_reflect_DirectMemObj_objectFieldOffset(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    FieldInfo *fi = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    push_long(runtime->stack, fi ? -1 : (s64) (intptr_t) fi->offset_instance);
    return 0;
}

s32 org_mini_reflect_DirectMemObj_staticFieldOffset(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    FieldInfo *fi = (__refer) (intptr_t) localvar_getLong(runtime->localvar, pos);
    push_long(runtime->stack, fi && (fi->access_flags & ACC_STATIC) ? -1 : (s64) (intptr_t) fi->offset);
    return 0;
}

s32 org_mini_reflect_DirectMemObj_objectFieldBase(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *ins = localvar_getRefer(runtime->localvar, pos);
    push_long(runtime->stack, ins ? -1 : (s64) (intptr_t) ins->arr_body);
    return 0;
}


static java_native_method METHODS_REFLECT_TABLE[] = {
        {"org/mini/reflect/vm/RefNative",  "refIdSize",                "()I",                                                                              org_mini_reflect_vm_RefNative_refIdSize},
        {"org/mini/reflect/vm/RefNative",  "obj2id",                   "(Ljava/lang/Object;)J",                                                            org_mini_reflect_vm_RefNative_obj2id},
        {"org/mini/reflect/vm/RefNative",  "id2obj",                   "(J)Ljava/lang/Object;",                                                            org_mini_reflect_vm_RefNative_id2obj},
        {"org/mini/reflect/vm/RefNative",  "getClasses",               "()[Ljava/lang/Class;",                                                             org_mini_reflect_vm_RefNative_getClasses},
        {"org/mini/reflect/vm/RefNative",  "getBootstrapClassByName",  "(Ljava/lang/String;)Ljava/lang/Class;",                                            org_mini_reflect_vm_RefNative_getBootstrapClassByName},
        {"org/mini/reflect/vm/RefNative",  "newWithoutInit",           "(Ljava/lang/Class;)Ljava/lang/Object;",                                            org_mini_reflect_vm_RefNative_newWithoutInit},
        {"org/mini/reflect/vm/RefNative",  "setLocalVal",              "(JIBJI)I",                                                                         org_mini_reflect_vm_RefNative_setLocalVal},
        {"org/mini/reflect/vm/RefNative",  "getLocalVal",              "(JILorg/mini/jdwp/type/ValueType;)I",                                              org_mini_reflect_vm_RefNative_getLocalVal},
        {"org/mini/reflect/vm/RefNative",  "getThreads",               "()[Ljava/lang/Thread;",                                                            org_mini_reflect_vm_RefNative_getThreads},
        {"org/mini/reflect/vm/RefNative",  "getStatus",                "(Ljava/lang/Thread;)I",                                                            org_mini_reflect_vm_RefNative_getStatus},
        {"org/mini/reflect/vm/RefNative",  "suspendThread",            "(Ljava/lang/Thread;)I",                                                            org_mini_reflect_vm_RefNative_suspendThread},
        {"org/mini/reflect/vm/RefNative",  "resumeThread",             "(Ljava/lang/Thread;)I",                                                            org_mini_reflect_vm_RefNative_resumeThread},
        {"org/mini/reflect/vm/RefNative",  "getSuspendCount",          "(Ljava/lang/Thread;)I",                                                            org_mini_reflect_vm_RefNative_getSuspendCount},
        {"org/mini/reflect/vm/RefNative",  "getFrameCount",            "(Ljava/lang/Thread;)I",                                                            org_mini_reflect_vm_RefNative_getFrameCount},
        {"org/mini/reflect/vm/RefNative",  "stopThread",               "(Ljava/lang/Thread;J)I",                                                           org_mini_reflect_vm_RefNative_stopThread},
        {"org/mini/reflect/vm/RefNative",  "getStackFrame",            "(Ljava/lang/Thread;)J",                                                            org_mini_reflect_vm_RefNative_getStackFrame},
        {"org/mini/reflect/vm/RefNative",  "getGarbageMarkCounter",    "()I",                                                                              org_mini_reflect_vm_RefNative_getGarbageMarkCounter},
        {"org/mini/reflect/vm/RefNative",  "getGarbageStatus",         "()I",                                                                              org_mini_reflect_vm_RefNative_getGarbageStatus},
        {"org/mini/reflect/vm/RefNative",  "getCallerClass",           "()Ljava/lang/Class;",                                                              org_mini_reflect_vm_RefNative_getCallerClass},
        {"org/mini/reflect/vm/RefNative",  "defineClass",              "(Ljava/lang/ClassLoader;Ljava/lang/String;[BII)Ljava/lang/Class;",                 org_mini_reflect_vm_RefNative_defineClass},
        {"org/mini/reflect/vm/RefNative",  "findLoadedClass0",         "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;",                     org_mini_reflect_vm_RefNative_findLoadedClass0},
        {"org/mini/reflect/vm/RefNative",  "initNativeClassLoader",    "(Ljava/lang/ClassLoader;Ljava/lang/ClassLoader;)V",                                org_mini_reflect_vm_RefNative_initNativeClassLoader},
        {"org/mini/reflect/vm/RefNative",  "destroyNativeClassLoader", "(Ljava/lang/ClassLoader;)V",                                                       org_mini_reflect_vm_RefNative_destroyNativeClassLoader},
        {"org/mini/reflect/vm/RefNative",  "addJarToClasspath",        "(Ljava/lang/String;)V",                                                            org_mini_reflect_vm_RefNative_addJarToClasspath},
        {"org/mini/reflect/vm/RefNative",  "heap_calloc",              "(I)J",                                                                             org_mini_reflect_vm_RefNative_heap_calloc},
        {"org/mini/reflect/vm/RefNative",  "heap_free",                "(J)V",                                                                             org_mini_reflect_vm_RefNative_heap_free},
        {"org/mini/reflect/vm/RefNative",  "heap_put_byte",            "(JIB)V",                                                                           org_mini_reflect_vm_RefNative_heap_put_byte},
        {"org/mini/reflect/vm/RefNative",  "heap_get_byte",            "(JI)B",                                                                            org_mini_reflect_vm_RefNative_heap_get_byte},
        {"org/mini/reflect/vm/RefNative",  "heap_put_short",           "(JIS)V",                                                                           org_mini_reflect_vm_RefNative_heap_put_short},
        {"org/mini/reflect/vm/RefNative",  "heap_get_short",           "(JI)S",                                                                            org_mini_reflect_vm_RefNative_heap_get_short},
        {"org/mini/reflect/vm/RefNative",  "heap_put_int",             "(JII)V",                                                                           org_mini_reflect_vm_RefNative_heap_put_int},
        {"org/mini/reflect/vm/RefNative",  "heap_get_int",             "(JI)I",                                                                            org_mini_reflect_vm_RefNative_heap_get_int},
        {"org/mini/reflect/vm/RefNative",  "heap_put_long",            "(JIJ)V",                                                                           org_mini_reflect_vm_RefNative_heap_put_long},
        {"org/mini/reflect/vm/RefNative",  "heap_get_long",            "(JI)J",                                                                            org_mini_reflect_vm_RefNative_heap_get_long},
        {"org/mini/reflect/vm/RefNative",  "heap_put_float",           "(JIF)V",                                                                           org_mini_reflect_vm_RefNative_heap_put_int},
        {"org/mini/reflect/vm/RefNative",  "heap_get_float",           "(JI)F",                                                                            org_mini_reflect_vm_RefNative_heap_get_int},
        {"org/mini/reflect/vm/RefNative",  "heap_put_double",          "(JID)V",                                                                           org_mini_reflect_vm_RefNative_heap_put_long},
        {"org/mini/reflect/vm/RefNative",  "heap_get_double",          "(JI)D",                                                                            org_mini_reflect_vm_RefNative_heap_get_long},
        {"org/mini/reflect/vm/RefNative",  "heap_put_ref",             "(JILjava/lang/Object;)V",                                                          org_mini_reflect_vm_RefNative_heap_put_ref},
        {"org/mini/reflect/vm/RefNative",  "heap_get_ref",             "(JI)Ljava/lang/Object;",                                                           org_mini_reflect_vm_RefNative_heap_get_ref},
        {"org/mini/reflect/vm/RefNative",  "heap_copy",                "(JIJII)V",                                                                         org_mini_reflect_vm_RefNative_heap_copy},
        {"org/mini/reflect/vm/RefNative",  "heap_endian",              "()I",                                                                              org_mini_reflect_vm_RefNative_heap_little_endian},
        {"org/mini/reflect/ReflectClass",  "mapClass",                 "(J)V",                                                                             org_mini_reflect_ReflectClass_mapClass},
        {"org/mini/reflect/ReflectField",  "mapField",                 "(J)V",                                                                             org_mini_reflect_ReflectField_mapField},
        {"org/mini/reflect/ReflectField",  "getFieldVal",              "(Ljava/lang/Object;J)J",                                                           org_mini_reflect_ReflectField_getFieldVal},
        {"org/mini/reflect/ReflectField",  "setFieldVal",              "(Ljava/lang/Object;JJ)V",                                                          org_mini_reflect_ReflectField_setFieldVal},
        {"org/mini/reflect/ReflectMethod", "mapMethod",                "(J)V",                                                                             org_mini_reflect_ReflectMethod_mapMethod},
        {"org/mini/reflect/ReflectMethod", "invokeMethod",             "(JLjava/lang/Object;[J)Lorg/mini/reflect/DataWrap;",                               org_mini_reflect_ReflectMethod_invokeMethod},
        {"org/mini/reflect/ReflectMethod", "findMethod0",              "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)J", org_mini_reflect_ReflectMethod_findMethod0},
        {"org/mini/reflect/StackFrame",    "mapRuntime",               "(J)V",                                                                             org_mini_reflect_StackFrame_mapRuntime},
        {"org/mini/reflect/ReflectArray",  "mapArray",                 "(Ljava/lang/Object;)V",                                                            org_mini_reflect_ReflectArray_mapArray},
        {"org/mini/reflect/ReflectArray",  "getLength",                "(Ljava/lang/Object;)I",                                                            org_mini_reflect_ReflectArray_getLength},
        {"org/mini/reflect/ReflectArray",  "getTypeTag",               "(Ljava/lang/Object;)B",                                                            org_mini_reflect_ReflectArray_getTypeTag},
        {"org/mini/reflect/ReflectArray",  "getBodyPtr",               "(Ljava/lang/Object;)J",                                                            org_mini_reflect_ReflectArray_getArrayBodyPtr},
        {"org/mini/reflect/ReflectArray",  "newArray",                 "(Ljava/lang/Class;I)Ljava/lang/Object;",                                           org_mini_reflect_ReflectArray_newArray},
        {"org/mini/reflect/ReflectArray",  "multiNewArray",            "(Ljava/lang/Class;[I)Ljava/lang/Object;",                                          org_mini_reflect_ReflectArray_multiNewArray},
        {"org/mini/reflect/DirectMemObj",  "setVal",                   "(IJ)V",                                                                            org_mini_reflect_DirectMemObj_setVal},
        {"org/mini/reflect/DirectMemObj",  "getVal",                   "(I)J",                                                                             org_mini_reflect_DirectMemObj_getVal},
        {"org/mini/reflect/DirectMemObj",  "copyTo0",                  "(ILjava/lang/Object;II)V",                                                         org_mini_reflect_DirectMemObj_copyTo0},
        {"org/mini/reflect/DirectMemObj",  "copyFrom0",                "(ILjava/lang/Object;II)V",                                                         org_mini_reflect_DirectMemObj_copyFrom0},
        {"sun/misc/Unsafe",                "objectFieldOffset",        "(J)J",                                                                             org_mini_reflect_DirectMemObj_objectFieldOffset},
        {"sun/misc/Unsafe",                "objectFieldBase",          "(Ljava/lang/Object;)J",                                                            org_mini_reflect_DirectMemObj_objectFieldBase},
        {"sun/misc/Unsafe",                "staticFieldOffset",        "(J)J",                                                                             org_mini_reflect_DirectMemObj_staticFieldOffset},

};


void reg_reflect_native_lib(MiniJVM *jvm) {
    native_reg_lib(jvm, &(METHODS_REFLECT_TABLE[0]), sizeof(METHODS_REFLECT_TABLE) / sizeof(java_native_method));
}

#ifdef __cplusplus
}
#endif
