
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"
#include "jdwp.h"


void thread_boundle(Runtime *runtime) {

    JClass *thread_clazz = classes_load_get_c(NULL, STR_CLASS_JAVA_LANG_THREAD, runtime);
    //为主线程创建Thread实例
    Instance *t = instance_create(runtime, thread_clazz);
    instance_hold_to_thread(t, runtime);
    runtime->thrd_info->jthread = t;//Thread.init currentThread() need this
    instance_init(t, runtime);
    Runtime *r = jthread_get_stackframe_value(runtime->jvm, t);
    if (r) {
        gc_move_objs_thread_2_gc(r);
        runtime_destory(r);
    }
    jthread_set_stackframe_value(runtime->jvm, t, runtime);
    jthread_init(runtime->jvm, t);
    instance_release_from_thread(t, runtime);

    runtime->thrd_info->thread_status = THREAD_STATUS_RUNNING;
}

void thread_unboundle(Runtime *runtime) {

    runtime->thrd_info->is_suspend = 1;
    Instance *t = runtime->thrd_info->jthread;
    //主线程实例被回收
    jthread_dispose(t, runtime);
}

void print_exception(Runtime *runtime) {
#if _JVM_DEBUG_LOG_LEVEL > 1
    if (runtime) {
        Utf8String *stacktrack = utf8_create();
        getRuntimeStack(runtime, stacktrack);
        jvm_printf("%s\n", utf8_cstr(stacktrack));
        utf8_destory(stacktrack);

        runtime_clear_stacktrack(runtime);
    }
#endif
}

#if _JVM_DEBUG_PROFILE

void profile_init() {
    memset(&profile_instructs, 0, sizeof(ProfileDetail) * INST_COUNT);
}

void profile_put(u8 instruct_code, s64 cost_add, s64 count_add) {
    ProfileDetail *h_s_v = &profile_instructs[instruct_code];

    spin_lock(&pro_lock);
    h_s_v->cost += cost_add;
    h_s_v->count += count_add;
    spin_unlock(&pro_lock);

};

void profile_print() {
    s32 i;
    jvm_printf("id           total    count      avg inst  \n");
    for (i = 0; i < INST_COUNT; i++) {
        ProfileDetail *pd = &profile_instructs[i];
        jvm_printf("%2x %15lld %8d %8lld %s\n",
                   i | 0xffffff00, pd->cost, pd->count, pd->count ? (pd->cost / pd->count) : 0, INST_NAME[i]);
    }
}

#endif

PeerClassLoader *classloader_create(MiniJVM *jvm) {
    return classloader_create_with_path(jvm, "");
}

PeerClassLoader *classloader_create_with_path(MiniJVM *jvm, c8 *path) {
    PeerClassLoader *class_loader = jvm_calloc(sizeof(PeerClassLoader));

    class_loader->jvm = jvm;
    //split classpath
    class_loader->classpath = arraylist_create(0);
    Utf8String *g_classpath = utf8_create_c(path);
    classloader_add_jar_path(class_loader, g_classpath);
    utf8_destory(g_classpath);
    //创建类容器
    class_loader->classes = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);

    return class_loader;
}

void classloader_destory(PeerClassLoader *class_loader) {
    HashtableIterator hti;
    hashtable_iterate(class_loader->classes, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        gc_obj_release(class_loader->jvm->collector, v);
    }

    hashtable_clear(class_loader->classes);
    s32 i;
    for (i = 0; i < class_loader->classpath->length; i++) {
        utf8_destory(arraylist_get_value(class_loader->classpath, i));
    }
    arraylist_destory(class_loader->classpath);
    hashtable_destory(class_loader->classes);

    class_loader->classes = NULL;

    jvm_free(class_loader);
}

void classloader_remove_all_class(PeerClassLoader *class_loader) {
    hashtable_clear(class_loader->classes);
}

void classloader_release_class_static_field(PeerClassLoader *class_loader) {
    HashtableIterator hti;
    hashtable_iterate(class_loader->classes, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        JClass *clazz = (JClass *) (v);
        class_clear_refer(class_loader, clazz);
    }
}


void classloader_add_jar_path(PeerClassLoader *class_loader, Utf8String *jar_path) {

    Utf8String *tmp = NULL;
    s32 i = 0;
    while (i < jar_path->length) {
        if (tmp == NULL) {
            tmp = utf8_create();
        }
        c8 ch = utf8_char_at(jar_path, i++);
        if (i == jar_path->length) {
            if (ch != ';' && ch != ':')utf8_insert(tmp, tmp->length, ch);
            ch = PATHSEPARATOR;
        }
        if (ch == PATHSEPARATOR) {
            if (utf8_last_indexof_c(tmp, "/") == tmp->length - 1) {
                utf8_remove(tmp, tmp->length - 1);
            }
            //check duplicate
            s32 j;
            for (j = 0; j < class_loader->classpath->length; j++) {
                if (utf8_equals(arraylist_get_value(class_loader->classpath, j), tmp)) {
                    continue;
                }
            }
            arraylist_push_back(class_loader->classpath, tmp);
            tmp = NULL;
        } else {
            utf8_insert(tmp, tmp->length, ch);
        }
    }
}

void classloaders_add(MiniJVM *jvm, PeerClassLoader *pcl) {
    spin_lock(&jvm->lock_cloader);
    {
        arraylist_push_back_unsafe(jvm->classloaders, pcl);
    }
    spin_unlock(&jvm->lock_cloader);
}

void classloaders_remove(MiniJVM *jvm, PeerClassLoader *pcl) {
    spin_lock(&jvm->lock_cloader);
    {
        arraylist_remove_unsafe(jvm->classloaders, pcl);
    }
    spin_unlock(&jvm->lock_cloader);
}

PeerClassLoader *classLoaders_find_by_instance(MiniJVM *jvm, Instance *jloader) {
    PeerClassLoader *r = NULL;
    spin_lock(&jvm->lock_cloader);
    {
        s32 i;
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            if (pcl->jloader == jloader) {
                r = pcl;
            }
        }
    }
    spin_unlock(&jvm->lock_cloader);
    return r;
}

void classloaders_clear_all_static(MiniJVM *jvm) {
    spin_lock(&jvm->lock_cloader);
    {
        s32 i;
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            //release class static field
            classloader_release_class_static_field(pcl);
        }
    }
    spin_unlock(&jvm->lock_cloader);
}

void classloaders_destroy_all(MiniJVM *jvm) {

    spin_lock(&jvm->lock_cloader);
    {
        s32 i;
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            //release class static field
            classloader_destory(pcl);
        }
    }
    spin_unlock(&jvm->lock_cloader);
    spin_destroy(&jvm->lock_cloader);
    arraylist_destory(jvm->classloaders);
    jvm->boot_classloader = NULL;
    jvm->classloaders = NULL;
}

void set_jvm_state(MiniJVM *jvm, int state) {
    jvm->jvm_state = state;
}

s32 get_jvm_state(MiniJVM *jvm) {
    return jvm->jvm_state;
}

void _on_jvm_sig_print(int no) {
    jvm_printf("[SIGNAL]jvm sig:%d  errno: %d , %s\n", no, errno, strerror(errno));
}

void _on_jvm_sig(int no) {
    _on_jvm_sig_print(no);
    exit(no);
}

MiniJVM *jvm_create() {
    MiniJVM *jvm = jvm_calloc(sizeof(MiniJVM));
    if (!jvm) {
        jvm_printf("jvm create error.");
        return NULL;
    }
    jvm->env = &jnienv;
    jvm->max_heap_size = MAX_HEAP_SIZE_DEFAULT;
    jvm->heap_overload_percent = GARBAGE_OVERLOAD_DEFAULT;
    jvm->garbage_collect_period_ms = GARBAGE_PERIOD_MS_DEFAULT;
    return jvm;
}

s32 jvm_init(MiniJVM *jvm, c8 *p_bootclasspath, c8 *p_classpath) {
    if (!jvm) {
        jvm_printf("jvm not found.");
        return -1;
    }

    if (jvm->jdwp_enable && !_JVM_JDWP_ENABLE) {
        jvm_printf("[WARN]jvm set jdwp enable, please set _JVM_JDWP_ENABLE with 1 in jvm.h and recompile \n");
    }

    signal(SIGABRT, _on_jvm_sig);
    signal(SIGFPE, _on_jvm_sig);
    signal(SIGSEGV, _on_jvm_sig);
    signal(SIGTERM, _on_jvm_sig);
#ifdef SIGPIPE
    signal(SIGPIPE, _on_jvm_sig_print); //not exit when network sigpipe
#endif

    set_jvm_state(jvm, JVM_STATUS_INITING);

    if (!p_classpath) {
        p_bootclasspath = "./";
    }

    //
    open_log();

#if _JVM_DEBUG_PROFILE
    profile_init();
#endif
    //
    init_jni_func_table(jvm);

    //创建线程容器
    jvm->thread_list = arraylist_create(0);
    //创建垃圾收集器
    gc_create(jvm);

    //本地方法库
    jvm->native_libs = arraylist_create(0);
    reg_std_native_lib(jvm);
    reg_net_native_lib(jvm);
    reg_reflect_native_lib(jvm);
    reg_lwjgl_native_lib(jvm);
    reg_lwjgl_keyboard_native_lib(jvm);
    reg_lwjgl_mouse_native_lib(jvm);
    reg_lwjgl_display_native_lib(jvm);

    //创建jstring 相关容器
    jvm->table_jstring_const = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);
    hashtable_register_free_functions(jvm->table_jstring_const, (HashtableKeyFreeFunc) utf8_destory, NULL);

    spin_init(&jvm->lock_cloader, 0);
    jvm->boot_classloader = classloader_create_with_path(jvm, p_bootclasspath);
    jvm->classloaders = arraylist_create(4);
    classloaders_add(jvm, jvm->boot_classloader);

    //装入系统属性
    sys_properties_load(jvm);
    sys_properties_set_c(jvm, "java.class.path", p_classpath);
    sys_properties_set_c(jvm, "sun.boot.class.path", p_bootclasspath);
    sys_properties_set_c(jvm, "java.class.version", "52.0");

    //启动调试器
    jdwp_start_server(jvm);

    set_jvm_state(jvm, JVM_STATUS_RUNNING);

    //init load thread, string etc
    Runtime *runtime = runtime_create(jvm);
    Utf8String *clsName = utf8_create_c(STR_CLASS_JAVA_LANG_INTEGER);
    JClass *c = classes_load_get(NULL, clsName, runtime);
    if (!c) {
        jvm_printf("[ERROR]maybe bootstrap classpath misstake: %s \n", p_bootclasspath);
        return -1;
    }
    utf8_clear(clsName);
    utf8_append_c(clsName, STR_CLASS_JAVA_LANG_THREAD);
    classes_load_get(NULL, clsName, runtime);
    utf8_clear(clsName);
    utf8_append_c(clsName, STR_CLASS_ORG_MINI_REFLECT_LAUNCHER);
    classes_load_get(NULL, clsName, runtime);
    //开始装载类
    utf8_destory(clsName);
    gc_move_objs_thread_2_gc(runtime);
    runtime_destory(runtime);
    runtime = NULL;

    //启动垃圾回收
    gc_resume(jvm->collector);

#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("[INFO]jvm inited\n");
#endif
    return 0;
}

void jvm_destroy(MiniJVM *jvm) {
    while (threadlist_count_none_daemon(jvm) > 0 && !jvm->collector->exit_flag) {//wait for other thread over ,
        threadSleep(20);
    }
    set_jvm_state(jvm, JVM_STATUS_STOPED);
    //waiting for daemon thread terminate
    thread_stop_all(jvm);

    //thread terminated ,it unboundle itself, so need not unboundle here
//    Runtime *r;
//    while ((r = arraylist_peek_back(jvm->thread_list)) != NULL) {
//        if (r) {
//            if (!r->son) {//未在执行jvm指令
//                thread_unboundle(r);//
//            }
//        }
//        threadSleep(20);
//    }
#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("[INFO]waitting for thread terminate\n");
#endif
    while (jvm->thread_list->length) {
        threadSleep(20);
    }

    jdwp_stop_server(jvm);
    //
    gc_destory(jvm);

    hashtable_destory(jvm->table_jstring_const);
    //
    thread_lock_dispose(&jvm->threadlock);
    arraylist_destory(jvm->thread_list);
    native_lib_destory(jvm);
    sys_properties_dispose(jvm);
    close_log();
#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("[INFO]jvm destoried\n");
#endif
    set_jvm_state(jvm, JVM_STATUS_UNKNOW);
    jvm_free(jvm);
}

s32 call_main(MiniJVM *jvm, c8 *p_mainclass, ArrayList *java_para) {
    if (!jvm) {
        jvm_printf("jvm not found .\n");
        return 1;
    }
    Runtime *runtime = runtime_create(jvm);
    thread_boundle(runtime);

    //准备参数
    s32 count = java_para ? java_para->length : 0;
    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_STRING);
    Instance *arr = jarray_create_by_type_name(runtime, count, ustr, NULL);
    instance_hold_to_thread(arr, runtime);
    utf8_destory(ustr);
    int i;
    for (i = 0; i < count; i++) {
        Utf8String *utfs = utf8_create_c(arraylist_get_value(java_para, i));
        Instance *jstr = jstring_create(utfs, runtime);
        jarray_set_field(arr, i, (intptr_t) jstr);
        utf8_destory(utfs);
    }
    push_ref(runtime->stack, arr);
    instance_release_from_thread(arr, runtime);

    c8 *p_methodname = "main";
    c8 *p_methodtype = "([Ljava/lang/String;)V";
    s32 ret = call_method(jvm, p_mainclass, p_methodname, p_methodtype, runtime);

    thread_unboundle(runtime);
    gc_move_objs_thread_2_gc(runtime);
    runtime_destory(runtime);
    return ret;
}


s32 call_method(MiniJVM *jvm, c8 *p_classname, c8 *p_methodname, c8 *p_methoddesc, Runtime *p_runtime) {
    if (p_runtime && p_runtime->jvm != jvm) {
        jvm_printf("[ERROR]runtime not adapted to jvm .\n");
        return RUNTIME_STATUS_ERROR;
    }
    if (!jvm) {
        jvm_printf("[ERROR]jvm not found .\n");
        return RUNTIME_STATUS_ERROR;
    }
    //创建运行时栈
    Runtime *runtime = p_runtime;
    if (!p_runtime) {
        runtime = runtime_create(jvm);
        thread_boundle(runtime);
    }

    //开始装载类

    Utf8String *str_mainClsName = utf8_create_c(p_classname);
    utf8_replace_c(str_mainClsName, ".", "/");

    //find systemclassloader
    Instance *jloader = NULL;
    s32 ret = execute_method_impl(jvm->shortcut.launcher_getSystemClassLoader, runtime);
    if (!ret) {
        jloader = pop_ref(runtime->stack);
    } else {
        print_exception(runtime);
    }
    //装入主类
    JClass *clazz = classes_load_get(jloader, str_mainClsName, runtime);


    ret = 0;
    if (clazz) {
        Utf8String *methodName = utf8_create_c(p_methodname);
        Utf8String *methodType = utf8_create_c(p_methoddesc);

        MethodInfo *m = find_methodInfo_by_name(str_mainClsName, methodName, methodType, clazz->jloader, runtime);
        if (m) {


            s64 start = currentTimeMillis();
#if _JVM_DEBUG_LOG_LEVEL > 0
            jvm_printf("\n[INFO]main thread start\n");
#endif
            //调用主方法
            if (_JVM_JDWP_ENABLE) {
                if (jvm->jdwp_enable) {
                    if (jvm->jdwp_suspend_on_start)jthread_suspend(runtime);
                    jvm_printf("[JDWP]jdwp listening (port:%s) ...\n", JDWP_TCP_PORT);
                }//jdwp 会启动调试器
            }
            runtime->method = NULL;
            runtime->clazz = clazz;
            ret = execute_method(m, runtime);
            if (ret == RUNTIME_STATUS_EXCEPTION) {
                print_exception(runtime);
            }
#if _JVM_DEBUG_LOG_LEVEL > 0
            jvm_printf("[INFO]main thread over %llx , return %d , spent : %lld\n",
                       (s64) (intptr_t) runtime->thrd_info->jthread, ret, (currentTimeMillis() - start));
#endif

#if _JVM_DEBUG_PROFILE
            profile_print();
#endif


        }
        utf8_destory(methodName);
        utf8_destory(methodType);
    } else {
        jvm_printf("[ERROR]main class not found: %s\n", p_classname);
        ret = RUNTIME_STATUS_ERROR;
    }
    if (!p_runtime) {
        thread_unboundle(runtime);
        gc_move_objs_thread_2_gc(runtime);
        runtime_destory(runtime);
    }
    utf8_destory(str_mainClsName);
    return ret;
}


s32 execute_method(MethodInfo *method, Runtime *runtime) {
    if (!runtime || !method) {
        return RUNTIME_STATUS_ERROR;
    }
    // if not detect the son ,may cause jthread enter fake blocking state,
    // eg: call_bc-> call_native->(reenter) call_bc->ret_bc(fake_blocking)->ret_native->ret_bc
    // only the outer thread top call the java bytecode ,need check block state
    if (runtime->thrd_info->top_runtime->son == NULL) {// is top call bc, not reenter bc
        jthread_block_exit(runtime);
    }
    s32 ret = execute_method_impl(method, runtime);
    if (runtime->thrd_info->top_runtime->son == NULL) {
        jthread_block_enter(runtime);
    }
    return ret;
}
