#include <stdio.h>
#include <stdlib.h>


#include "jvm.h"
#include "garbage.h"
#include "bytebuf.h"

c8 const *STR_JAVA_LANG_CLASS = "java/lang/Class";
c8 const *STR_JAVA_LANG_OBJECT = "java/lang/Object";
c8 const *STR_JAVA_LANG_STRING = "java/lang/String";
c8 const *STR_JAVA_LANG_THREAD = "java/lang/Thread";
c8 const *STR_JAVA_LANG_INTEGER = "java/lang/Integer";
c8 const *STR_JAVA_LANG_OUT_OF_MEMORY_ERROR = "java/io/OutOfMemoryError";
c8 const *STR_JAVA_LANG_VIRTUAL_MACHINE_ERROR = "java/io/VirtualMachineError";
c8 const *STR_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION = "java/lang/ClassNotFoundException";
c8 const *STR_JAVA_LANG_ARITHMETIC_EXCEPTION = "java/lang/ArithmeticException";
c8 const *STR_JAVA_LANG_NULL_POINTER_EXCEPTION = "java/lang/NullPointerException";
c8 const *STR_JAVA_LANG_NO_SUCH_METHOD_EXCEPTION = "java/lang/NoSuchMethodException";
c8 const *STR_JAVA_LANG_NO_SUCH_FIELD_EXCEPTION = "java/lang/NoSuchFieldException";
c8 const *STR_JAVA_LANG_ILLEGAL_ARGUMENT_EXCEPTION = "java/lang/IllegalArgumentException";
c8 const *STR_JAVA_LANG_CLASS_CAST_EXCEPTION = "java/lang/ClassCastException";
c8 const *STR_JAVA_LANG_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = "java/lang/ArrayIndexOutOfBoundsException";
c8 const *STR_JAVA_LANG_INSTANTIATION_EXCEPTION = "java/lang/InstantiationException";
c8 const *STR_JAVA_LANG_STACKTRACEELEMENT = "java/lang/StackTraceElement";

//=====================================================================
//define variable
tss_t TLS_KEY_JTHREADRUNTIME;
tss_t TLS_KEY_UTF8STR_CACHE;
Jvm *g_jvm = NULL;
ProCache g_procache;
//=====================================================================

//=====================================================================

FieldInfo *fieldinfo_create_with_raw(FieldRaw *fieldRaw) {

    FieldInfo *field = jvm_calloc(sizeof(JClass));
    field->raw = fieldRaw;
    field->name = get_utf8str(&g_strings[fieldRaw->name]);
//    if (utf8_equals_c(field->name, "out")) {
//        int debug = 1;
//    }
    field->desc = get_utf8str(&g_strings[fieldRaw->desc_name]);
    field->signature = fieldRaw->signature_name < 0 ? NULL : get_utf8str(&g_strings[fieldRaw->signature_name]);
    field->offset_ins = fieldRaw->offset_ins;
    field->is_refer = isDataReferByTag(utf8_char_at(field->desc, 0));
    field->is_static = (fieldRaw->access & ACC_STATIC) != 0;
    return field;
}

void fieldinfo_destroy(MethodInfo *fieldInfo) {
    jvm_free(fieldInfo);
}

MethodInfo *methodinfo_create_with_raw(MethodRaw *methodRaw) {

    MethodInfo *method = jvm_calloc(sizeof(JClass));
    method->raw = methodRaw;
    method->name = get_utf8str(&g_strings[methodRaw->name]);
    method->desc = get_utf8str(&g_strings[methodRaw->desc_name]);
    method->signature = methodRaw->signature_name < 0 ? NULL : get_utf8str(&g_strings[methodRaw->signature_name]);
    method->func_ptr = methodRaw->func_ptr;
    method->paratype = utf8_create();
    parseMethodPara(method->desc, method->paratype);
    method->returntype = utf8_create();
    utf8_append_part(method->returntype, method->desc, utf8_indexof_c(method->desc, ")") + 1, 1);
    return method;
}

void methodinfo_destroy(MethodInfo *methodInfo) {
    utf8_destory(methodInfo->paratype);
    utf8_destory(methodInfo->returntype);
    jvm_free(methodInfo);
}


JObject *ins_of_Class_create_get(JThreadRuntime *runtime, JClass *clazz) {
    JClass *java_lang_class = g_procache.java_lang_class_raw->clazz;
    if (java_lang_class) {
        if (clazz->ins_of_Class) {
            return clazz->ins_of_Class;
        } else {
            JObject *ins = new_instance_with_class(runtime, java_lang_class);
            gc_refer_hold(ins);
            jclass_init_insOfClass(runtime, ins);
            clazz->ins_of_Class = (__refer) ins;
            jclass_set_classHandle(ins, clazz);
            return (JObject *) ins;
        }
    }
    return NULL;
}

JClass *_jclass_create_inner() {
    JClass *clazz = jvm_calloc(sizeof(JClass));
    clazz->prop.type = INS_TYPE_CLASS;
    clazz->fields = arraylist_create(0);
    clazz->methods = arraylist_create(0);
    clazz->interfaces = arraylist_create(0);
    return clazz;
}

JClass *primitive_class_create_get(JThreadRuntime *runtime, Utf8String *ustr) {
    JClass *clazz = get_class_by_name(ustr);
    if (!clazz) {
        garbage_thread_lock();
        clazz = _jclass_create_inner();
        clazz->name = utf8_create_copy(ustr);
        clazz->primitive = 1;
        classes_put(clazz);
        gc_refer_hold(clazz);
        garbage_thread_unlock();
    }
    if (!clazz->ins_of_Class) {
        clazz->ins_of_Class = ins_of_Class_create_get(runtime, clazz);
    }
    return clazz;
}

JClass *array_class_create_get(Utf8String *name) {
    if (!name || name->length == 0 || utf8_char_at(name, 0) != '[') {
        return NULL;
    }
    JClass *clazz = hashtable_get(g_jvm->classes, name);
    if (clazz)return clazz;
    clazz = _jclass_create_inner();
    clazz->name = utf8_create_copy(name);
    c8 tag = utf8_char_at(name, 1);
    clazz->array_cell_type = getDataTypeIndex(tag);
    //
    Utf8String *cell_type = utf8_create();
    if (tag == '[') {
        utf8_append_part(cell_type, name, 1, name->length - 1);
        clazz->array_cell_class = array_class_create_get(cell_type);
    } else {
        if (isDataReferByTag(tag)) {//object
            utf8_append_part(cell_type, name, 2, name->length - 3);
            clazz->array_cell_class = classes_get(cell_type);
        } else {//primitive
            c8 *primitive_type = getDataTypeFullName(tag);
            utf8_append_c(cell_type, primitive_type);
            JThreadRuntime *runtime = tss_get(TLS_KEY_JTHREADRUNTIME);
            clazz->array_cell_class = primitive_class_create_get(runtime, cell_type);
        }
    }
    utf8_destory(cell_type);
//
    classes_put(clazz);
    gc_refer_hold(clazz);
    return clazz;
}

JClass *jclass_create_with_raw(ClassRaw *classRaw) {

    JClass *clazz = _jclass_create_inner();
    clazz->name = get_utf8str(&g_strings[classRaw->name]);
    clazz->source_name = get_utf8str(&g_strings[classRaw->source_name]);
    clazz->signature = classRaw->signature_name < 0 ? NULL : get_utf8str(&g_strings[classRaw->signature_name]);

    clazz->prop.members = classRaw->static_fields;
    clazz->raw = classRaw;
    classRaw->clazz = clazz;
    //printf("create class :%s\n", utf8_cstr(clazz->name));

    Utf8String *utf8 = utf8_create();
    Utf8String *num = utf8_create();

    //method
    UtfRaw *utfRaw = &g_strings[classRaw->method_arr];
    utf8_clear(utf8);
    utf8_append_c(utf8, utfRaw->str);
    utf8_substring(utf8, 1, utf8->length - 1);
    s32 i;
    for (i = 0;; i++) {
        utf8_clear(num);
        utf8_split_get_part(utf8, ",", i, num);
        if (num->length > 0) {
            s32 index = atoi(utf8_cstr(num));
            MethodRaw *methodRaw = &g_methods[index];
            MethodInfo *method = methodinfo_create_with_raw(methodRaw);
            method->clazz = clazz;
            arraylist_push_back(clazz->methods, method);
        } else {
            break;
        }
    }
    //field
    utfRaw = &g_strings[classRaw->field_arr];
    utf8_clear(utf8);
    utf8_append_c(utf8, utfRaw->str);
    utf8_substring(utf8, utf8_indexof_c(utf8, "[") + 1, utf8_indexof_c(utf8, "]"));
    for (i = 0;; i++) {
        utf8_clear(num);
        utf8_split_get_part(utf8, ",", i, num);
        if (num->length > 0) {
            s32 index = atoi(utf8_cstr(num));
            FieldRaw *fieldRaw = &g_fields[index];
            FieldInfo *field = fieldinfo_create_with_raw(fieldRaw);
            field->clazz = clazz;
            if (utf8_equals_c(clazz->name, "java/lang/ref/Reference")) {
                if (utf8_equals_c(field->name, "target")) {
                    field->is_ref_target = 1;
                }
            }
            arraylist_push_back(clazz->fields, field);
        } else {
            break;
        }
    }
    utf8_destory(utf8);
    utf8_destory(num);

    gc_refer_hold(clazz);
    return clazz;
}


void jclass_destroy(JClass *clazz) {
    s32 i;
    for (i = 0; i < clazz->methods->length; i++) {
        methodinfo_destroy(arraylist_get_value(clazz->methods, i));
    }
    arraylist_destory(clazz->methods);
    for (i = 0; i < clazz->fields->length; i++) {
        fieldinfo_destroy(arraylist_get_value(clazz->fields, i));
    }
    arraylist_destory(clazz->fields);
    //if (clazz->ins_of_Class)jobject_destroy(clazz->ins_of_Class);

    jvm_free(clazz);
}

JClass *classes_get(Utf8String *className) {
    if (utf8_char_at(className, 0) == '[') {
        return array_class_create_get(className);
    }
    JClass *clazz = hashtable_get(g_jvm->classes, className);
    return clazz;
}

JClass *classes_get_c(c8 const *className) {
    Utf8String *cache = tss_get(TLS_KEY_UTF8STR_CACHE);
    utf8_clear(cache);
    utf8_append_c(cache, className);
    return classes_get(cache);
}


void class_load(Utf8String *className) {
    garbage_thread_lock();

    JClass *clazz = classes_get(className);
    if (!clazz) {

        //load
        ClassRaw *classRaw = find_classraw(utf8_cstr(className));
        if (classRaw) {
            JClass *clazz = jclass_create_with_raw(classRaw);
            classes_put(clazz);
            clazz->status = CLASS_STATUS_LOADED;
            //jvm_printf("load : %s\n", utf8_cstr(className));
        } else {
            jvm_printf("class not found : %s\n", utf8_cstr(className));
        }
    }
    garbage_thread_unlock();
}

void class_clinit(JThreadRuntime *runtime, Utf8String *className) {
    ClassRaw *classRaw = find_classraw(utf8_cstr(className));
    if (!classRaw)return;

    garbage_thread_lock();
    runtime->no_pause++;
    //load this
    class_load(className);
    JClass *clazz = classes_get(className);
    if (clazz->status < CLASS_STATUS_PREPARING) {
        clazz->status = CLASS_STATUS_PREPARING;

        if (classRaw) {
            //load dependence classes
            Utf8String *utf8 = utf8_create();
            Utf8String *num = utf8_create();
            s32 i;
            //parse "[5,7,8]" the num is the class name index
            UtfRaw *utfRaw = &g_strings[classRaw->depd_arr];
            utf8_clear(utf8);
            utf8_append_c(utf8, utfRaw->str);
            utf8_substring(utf8, utf8_indexof_c(utf8, "[") + 1, utf8_indexof_c(utf8, "]"));
            for (i = 0;; i++) {
                utf8_clear(num);
                utf8_split_get_part(utf8, ",", i, num);
                if (num->length > 0) {
                    s32 index = (s32) utf8_aton(num, 10);
                    Utf8String *dcName = get_utf8str(&g_strings[index]);
                    JClass *dcClazz = classes_get(dcName);
                    if (!dcClazz) {
                        class_clinit(runtime, dcName);
                    }
                } else {
                    break;
                }
            }
            utf8_destory(utf8);
            utf8_destory(num);

            class_prepar(clazz);
            clazz->status = CLASS_STATUS_PREPARED;
        }
    }
    if (clazz->status < CLASS_STATUS_CLINITING) {
        clazz->status = CLASS_STATUS_CLINITING;//anti reenter
        c8 *methodName = "<clinit>";
        c8 *signature = "()V";
        MethodRaw *methodRaw = find_methodraw(utf8_cstr(className), methodName, signature);
        if (methodRaw) {
            class_clinit_func_t func = (class_clinit_func_t) methodRaw->func_ptr;
            func(runtime);
            exception_check_print(runtime);
            //jvm_printf("clinit :%s\n", utf8_cstr(clazz->name));
        }
        clazz->status = CLASS_STATUS_CLINITED;
    }

    runtime->no_pause--;
    garbage_thread_unlock();
}


void sys_properties_set_c(c8 *key, c8 *val) {
    Utf8String *ukey = utf8_create_c(key);
    Utf8String *uval = utf8_create_c(val);
    hashtable_put(g_jvm->sys_prop, ukey, uval);
}

s32 sys_properties_load() {
    hashtable_register_free_functions(g_jvm->sys_prop,
                                      (HashtableKeyFreeFunc) utf8_destory,
                                      (HashtableValueFreeFunc) utf8_destory);
    Utf8String *ustr = NULL;
    Utf8String *prop_name = utf8_create_c("./sys.properties");
    ByteBuf *buf = load_file_from_classpath(prop_name);
    if (buf) {
        ustr = utf8_create();
        while (bytebuf_available(buf)) {
            c8 ch = (c8) bytebuf_read(buf);
            utf8_insert(ustr, ustr->length, ch);
        }
        bytebuf_destory(buf);
    }
    utf8_destory(prop_name);
    //parse
    if (ustr) {
        utf8_replace_c(ustr, "\r\n", "\n");
        utf8_replace_c(ustr, "\r", "\n");
        Utf8String *line = utf8_create();
        while (ustr->length > 0) {
            s32 lineEndAt = utf8_indexof_c(ustr, "\n");
            utf8_clear(line);
            if (lineEndAt >= 0) {
                utf8_append_part(line, ustr, 0, lineEndAt);
                utf8_substring(ustr, lineEndAt + 1, ustr->length);
            } else {
                utf8_append_part(line, ustr, 0, ustr->length);
                utf8_substring(ustr, ustr->length, ustr->length);
            }
            s32 eqAt = utf8_indexof_c(line, "=");
            if (eqAt > 0) {
                Utf8String *key = utf8_create();
                Utf8String *val = utf8_create();
                utf8_append_part(key, line, 0, eqAt);
                utf8_append_part(val, line, eqAt + 1, line->length - (eqAt + 1));
                hashtable_put(g_jvm->sys_prop, key, val);
            }
        }
        utf8_destory(line);
        utf8_destory(ustr);
    }

    //modify os para
#if __JVM_OS_MAC__
    sys_properties_set_c("os.name", "Mac");
    sys_properties_set_c("path.separator", ":");
    sys_properties_set_c("file.separator", "/");
    sys_properties_set_c("line.separator", "\n");
#elif __JVM_OS_LINUX__
    sys_properties_set_c("os.name", "Linux");
    sys_properties_set_c("path.separator", ":");
    sys_properties_set_c("file.separator", "/");
    sys_properties_set_c("line.separator", "\n");
#elif __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
    sys_properties_set_c("os.name", "Windows");
    sys_properties_set_c("path.separator", ";");
    sys_properties_set_c("file.separator", "\\");
    sys_properties_set_c("line.separator", "\r\n");
#endif
    return 0;
}

void fill_procache() {
    static c8 *className = "java/lang/String";
    static c8 *methodName = "<init>";
    static c8 *signature = "([C)V";
    g_procache.java_lang_object_raw = find_classraw(STR_JAVA_LANG_OBJECT);
    g_procache.java_lang_string_raw = find_classraw(STR_JAVA_LANG_STRING);
    g_procache.java_lang_thread_raw = find_classraw(STR_JAVA_LANG_THREAD);
    g_procache.java_lang_class_raw = find_classraw(STR_JAVA_LANG_CLASS);
    g_procache.java_lang_string_init_C_raw = find_methodraw(className, methodName, signature);

}


void class_prepar(JClass *clazz) {

    s32 i;

    Utf8String *utf8 = utf8_create();
    Utf8String *num = utf8_create();

    //interface
    UtfRaw *utfRaw = &g_strings[clazz->raw->interface_name_arr];
    utf8_clear(utf8);
    utf8_append_c(utf8, utfRaw->str);
    utf8_substring(utf8, utf8_indexof_c(utf8, "[") + 1, utf8_indexof_c(utf8, "]"));
    for (i = 0;; i++) {
        utf8_clear(num);
        utf8_split_get_part(utf8, ",", i, num);
        if (num->length > 0) {
            s32 index = atoi(utf8_cstr(num));
            Utf8String *interfaceName = get_utf8str(&g_strings[index]);
            JClass *interface = get_class_by_name(interfaceName);
            if (!interface) {
                jvm_printf("[ERROR] class not found: %s\n", utf8_cstr(interfaceName));
            }
            arraylist_push_back(clazz->interfaces, interface);
        } else {
            break;
        }
    }
    //super
    if (clazz->raw->super_name == -1) {
        clazz->superclass = NULL;
    } else {
        ClassRaw *superRaw = find_classraw(g_strings[clazz->raw->super_name].str);
        Utf8String *superName = get_utf8str(&g_strings[superRaw->name]);
        JClass *superclass = get_class_by_name(superName);
        if (!superclass) {
            superclass = jclass_create_with_raw(superRaw);
        }
        clazz->superclass = superclass;
    }

    utf8_destory(utf8);
    utf8_destory(num);
}

Jvm *jvm_create(c8 *bootclasspath, c8 *classpath) {
    //tsl
    tss_create(&TLS_KEY_JTHREADRUNTIME, NULL);
    tss_create(&TLS_KEY_UTF8STR_CACHE, NULL);

    Jvm *jvm = jvm_calloc(sizeof(Jvm));
    jvm->classes = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);
    jvm->thread_list = arraylist_create(32);
    jvm->collector = garbage_collector_create();
    jvm->classloaders = arraylist_create(4);
    //创建jstring 相关容器
    jvm->table_jstring_const = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);
    jvm->sys_prop = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);

    g_jvm = jvm;
    jvm_printf("[INFO]jvm created\n");
    sys_properties_load();
    sys_properties_set_c("sun.boot.class.path", bootclasspath);
    sys_properties_set_c("java.class.path", classpath);
    fill_procache();
    garbage_start();


    return jvm;
}

void jvm_destroy(Jvm *jvm) {
    garbage_collector_destory(g_jvm->collector);
    HashtableIterator hti;
    hashtable_iterate(jvm->classes, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        jclass_destroy(v);
    }
    s32 i;
    for (i = 0; i < g_strings_count; i++) {
        if (g_strings[i].ustr) {
            utf8_destory(g_strings[i].ustr);
            g_strings[i].ustr = NULL;
        }
    }
    hashtable_destory(jvm->classes);
    arraylist_destory(jvm->thread_list);
    arraylist_destory(jvm->classloaders);
    hashtable_destory(jvm->table_jstring_const);
    hashtable_destory(jvm->sys_prop);
    jvm_free(jvm);
    jvm_printf("[INFO]jvm destroied\n");
}

StackFrame *stackframe_create() {
    StackFrame *frame = jvm_calloc(sizeof(StackFrame));
    return frame;
}

void stackframe_destroy(StackFrame *stackframe) {
    jvm_free(stackframe);
}

JThreadRuntime *jthreadruntime_create() {
    JThreadRuntime *runtime = jvm_calloc(sizeof(JThreadRuntime));
    runtime->stacktrack = arraylist_create(32);
    runtime->lineNo = arraylist_create(32);
    return runtime;
}

void jthreadruntime_destroy(__refer jthreadruntime) {
    JThreadRuntime *runtime = (JThreadRuntime *) jthreadruntime;
    arraylist_destory(runtime->stacktrack);
    arraylist_destory(runtime->lineNo);
    jvm_free(jthreadruntime);
}

/**
 *==============================================================
 *                        java thread
 *==============================================================
 */
JObject *new_jthread(JThreadRuntime *runtime) {
    ClassRaw *raw = g_procache.java_lang_thread_raw;
    if (!raw) {
        return NULL;
    }
    JObject *ins = new_instance_with_classraw(runtime, raw);
    return ins;
}


void jthreadlock_create(InstProp *mb) {
    garbage_thread_lock();
    if (!mb->thread_lock) {
        ThreadLock *tl = jvm_calloc(sizeof(ThreadLock));
        thread_lock_init(tl);
        mb->thread_lock = tl;
    }
    garbage_thread_unlock();
}

void jthreadlock_destory(InstProp *mb) {
    thread_lock_dispose(mb->thread_lock);
    if (mb->thread_lock) {
        jvm_free(mb->thread_lock);
        mb->thread_lock = NULL;
    }
}

s32 jthread_yield() {
    thrd_yield();
    return 0;
}


void jthread_lock(JThreadRuntime *runtime, JObject *jobj) {
    InstProp *mb = (InstProp *) jobj;
    if (mb == NULL)return;
    if (!mb->thread_lock) {
        jthreadlock_create(mb);
    }


    ThreadLock *jtl = mb->thread_lock;
    //can pause when lock
    while (mtx_trylock(&jtl->mutex_lock) != thrd_success) {
        check_suspend_and_pause(runtime);
        jthread_yield();
    }
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("  lock: %llx   lock holder: %s \n", (s64) (intptr_t) (runtime->threadInfo->jthread),
               utf8_cstr(mb->clazz->name));
#endif
}

void jthread_unlock(JThreadRuntime *runtime, JObject *jobj) {
    InstProp *mb = (InstProp *) jobj;
    if (mb == NULL)return;
    if (!mb->thread_lock) {
        jthreadlock_create(mb);
    }

    ThreadLock *jtl = mb->thread_lock;
    mtx_unlock(&jtl->mutex_lock);

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("unlock: %llx   lock holder: %s, \n", (s64) (intptr_t) (runtime->threadInfo->jthread),
               utf8_cstr(mb->clazz->name));
#endif
}

s32 jthread_notify(InstProp *mb) {
    if (mb == NULL)return -1;
    if (mb->thread_lock == NULL) {
        jthreadlock_create(mb);
    }
    cnd_signal(&mb->thread_lock->thread_cond);
    return 0;
}

s32 jthread_notifyAll(InstProp *mb) {
    if (mb == NULL)return -1;
    if (mb->thread_lock == NULL) {
        jthreadlock_create(mb);
    }
    cnd_broadcast(&mb->thread_lock->thread_cond);
    return 0;
}

s32 jthread_suspend(JThreadRuntime *runtime) {
    spin_lock(&runtime->lock);
//    MethodInfo *m = runtime->threadInfo->top_runtime->method;
//    jvm_printf("suspend %lx ,%s\n", runtime->threadInfo->jthread, m ? utf8_cstr(m->name) : "");
    runtime->suspend_count++;
    spin_unlock(&runtime->lock);
    return 0;
}

u8 jthread_block_enter(JThreadRuntime *runtime) {
    u8 s = runtime->thread_status;
    runtime->thread_status = THREAD_STATUS_BLOCKED;
    return s;
}

void jthread_block_exit(JThreadRuntime *runtime, u8 state) {
    runtime->thread_status = state;//THREAD_STATUS_RUNNING;
    check_suspend_and_pause(runtime);
}

s32 jthread_resume(JThreadRuntime *runtime) {
    spin_lock(&runtime->lock);
//    MethodInfo *m = runtime->top_runtime->method;
//    jvm_printf("resume %lx ,%s\n", runtime->threadInfo->jthread, m ? utf8_cstr(m->name) : "");
    if (runtime->suspend_count > 0)runtime->suspend_count--;
    spin_unlock(&runtime->lock);
    return 0;
}

s32 jthread_waitTime(InstProp *mb, JThreadRuntime *runtime, s64 waitms) {
    if (mb == NULL)return -1;
    if (!mb->thread_lock) {
        jthreadlock_create(mb);
    }
    u8 s = jthread_block_enter(runtime);
    if (waitms) {
        waitms += currentTimeMillis();
        struct timespec t;
        //clock_gettime(CLOCK_REALTIME, &t);
        t.tv_sec = waitms / 1000;
        t.tv_nsec = (waitms % 1000) * 1000000;
        cnd_timedwait(&mb->thread_lock->thread_cond, &mb->thread_lock->mutex_lock, &t);
    } else {
        cnd_wait(&mb->thread_lock->thread_cond, &mb->thread_lock->mutex_lock);
    }
    jthread_block_exit(runtime, s);
    return 0;
}

s32 jthread_sleep(JThreadRuntime *runtime, s64 ms) {
    u8 s = jthread_block_enter(runtime);
    threadSleep(ms);
    jthread_block_exit(runtime, s);
    return 0;
}


s32 jthread_prepar(JThreadRuntime *runtime) {

    Utf8String *ustr = utf8_create();

    utf8_append_c(ustr, STR_JAVA_LANG_CLASS);
    class_clinit(runtime, ustr);
    utf8_clear(ustr);
    utf8_append_c(ustr, STR_JAVA_LANG_NULL_POINTER_EXCEPTION);
    class_clinit(runtime, ustr);
    utf8_clear(ustr);
    utf8_append_c(ustr, STR_JAVA_LANG_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION);
    class_clinit(runtime, ustr);
    utf8_clear(ustr);
    utf8_append_c(ustr, STR_JAVA_LANG_INTEGER);
    class_clinit(runtime, ustr);
    utf8_clear(ustr);
    utf8_append_c(ustr, STR_JAVA_LANG_THREAD);
    class_clinit(runtime, ustr);
    utf8_clear(ustr);
//    if (!runtime->jthread) {
//        JObject *jthread = new_jthread(runtime);
//        runtime->jthread = jthread;
//    }


    utf8_destory(ustr);

    return 0;
}

s32 jthread_run(__refer p) {
    JThreadRuntime *runtime = (JThreadRuntime *) p;

    if (runtime->exec) {
        jthread_bound(runtime);
        class_clinit(runtime, get_utf8str_by_utfraw_index(runtime->exec->class_name));


        jthread_run_t run = (jthread_run_t) runtime->exec->func_ptr;
        s64 startAt = currentTimeMillis();
        run(runtime, runtime->jthread);
        jvm_printf("thread over, %lld\n", (currentTimeMillis() - startAt));
        exception_check_print(runtime);
    }
    jthread_unbound(runtime);
    return 0;
}


JThreadRuntime *jthread_start(JObject *jthread) {

    MethodRaw *method = find_methodraw(utf8_cstr(jthread->prop.clazz->name), "run", "()V");

    JThreadRuntime *runtime = jthread_get_stackFrame(jthread);
    runtime->jthread = jthread;
    runtime->exec = method;
    runtime->thread_status = THREAD_STATUS_NEW;
    thrd_create(&runtime->thread, jthread_run, runtime);
    return runtime;
}

JThreadRuntime *jthread_bound(JThreadRuntime *runtime) {
    if (!runtime) {
        runtime = jthreadruntime_create();
    }
    s32 ret = tss_set(TLS_KEY_JTHREADRUNTIME, runtime);
    Utf8String *ustr = utf8_create();
    tss_set(TLS_KEY_UTF8STR_CACHE, ustr);
    jthread_prepar(runtime);
    if (!runtime->jthread) {
        runtime->jthread = new_instance_with_name(runtime, STR_JAVA_LANG_THREAD);
        gc_refer_hold(runtime->jthread);
        instance_init(runtime, runtime->jthread);
        JThreadRuntime *r = jthread_get_stackFrame(runtime->jthread);
        if (r) {
            gc_move_refer_thread_2_gc(r);
            jthreadruntime_destroy(r);
        }
        jthread_set_stackFrame(runtime->jthread, runtime);
    }


    arraylist_push_back(g_jvm->thread_list, runtime);
    runtime->thread_status = THREAD_STATUS_RUNNING;
    return runtime;
}

void jthread_unbound(JThreadRuntime *runtime) {
    arraylist_remove(g_jvm->thread_list, runtime);
    runtime->thread_status = THREAD_STATUS_DEAD;
    if (runtime->context_classloader)gc_refer_release(runtime->context_classloader);
    gc_refer_release(runtime->jthread);
    jthreadruntime_destroy(runtime);
    tss_set(TLS_KEY_JTHREADRUNTIME, NULL);
    Utf8String *ustr = tss_get(TLS_KEY_UTF8STR_CACHE);
    utf8_destory(ustr);
    tss_set(TLS_KEY_UTF8STR_CACHE, NULL);
}

/**
 *==============================================================
 *                        jvm
 *==============================================================
 */


s32 jvm_run_main(Utf8String *mainClass) {
    g_jvm = jvm_create("", "");

    utf8_replace_c(mainClass, ".", "/");
    c8 *methodName = "main";
    c8 *signature = "([Ljava/lang/String;)V";
    MethodRaw *method = find_methodraw(utf8_cstr(mainClass), methodName, signature);
    if (method) {
        JThreadRuntime *runtime = jthreadruntime_create();
        runtime->exec = method;
        runtime->thread = thrd_current();
        jthread_run(runtime);
    } else {
        jvm_printf("[ERROR]can not found %s.%s%s\n", utf8_cstr(mainClass), methodName, signature);
    }
    //printf("threads count %d\n", g_jvm->thread_list->length);
    s32 i;
    while (g_jvm->thread_list->length) {
        s32 alive = 0;
        for (i = 0; i < g_jvm->thread_list->length; i++) {
            JThreadRuntime *r = arraylist_get_value(g_jvm->thread_list, i);
            if (r->thread_status != THREAD_STATUS_DEAD) {//todo daemon thread
                alive++;
            }
        }
        if (!alive)break;
        threadSleep(20);
    }
    threadSleep(100);
    jvm_destroy(g_jvm);
    printf("jvm destroied\n");
    g_jvm = NULL;
    return 0;
}
