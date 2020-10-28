
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include "jvm.h"
#include "../utils/utf8_string.h"
#include "garbage.h"
#include "jvm_util.h"
#include "java_native_std.h"
#include "jdwp.h"


void thread_boundle(Runtime *runtime) {

    JClass *thread_clazz = classes_load_get_c(NULL, STR_CLASS_JAVA_LANG_THREAD, runtime);
    //为主线程创建Thread实例
    Instance *t = instance_create(runtime, thread_clazz);
    instance_hold_to_thread(t, runtime);
    runtime->threadInfo->jthread = t;//Thread.init currentThread() need this
    instance_init(t, runtime);
    jthread_init(t, runtime);
    instance_release_from_thread(t, runtime);

}

void thread_unboundle(Runtime *runtime) {

    runtime->threadInfo->is_suspend = 1;
    Instance *t = runtime->threadInfo->jthread;
    //主线程实例被回收
    jthread_dispose(t);
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
                   i | 0xffffff00, pd->cost, pd->count, pd->count ? (pd->cost / pd->count) : 0, inst_name[i]);
    }
}

#endif

ClassLoader *classloader_create(c8 *path) {
    ClassLoader *class_loader = jvm_calloc(sizeof(ClassLoader));
    spin_init(&class_loader->lock, 0);

    //split classpath
    class_loader->classpath = arraylist_create(0);
    Utf8String *g_classpath = utf8_create_c(path);
    Utf8String *tmp = NULL;
    s32 i = 0;
    while (i < g_classpath->length) {
        if (tmp == NULL) {
            tmp = utf8_create();
        }
        c8 ch = utf8_char_at(g_classpath, i++);
        if (i == g_classpath->length) {
            if (ch != ';' && ch != ':')utf8_insert(tmp, tmp->length, ch);
            ch = ';';
        }
        if (ch == ';' || ch == ':') {
            if (utf8_last_indexof_c(tmp, "/") == tmp->length - 1)
                utf8_remove(tmp, tmp->length - 1);
            arraylist_push_back(class_loader->classpath, tmp);
            tmp = NULL;
        } else {
            utf8_insert(tmp, tmp->length, ch);
        }
    }
    utf8_destory(g_classpath);
    //创建类容器
    class_loader->classes = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);

    //创建jstring 相关容器
    class_loader->table_jstring_const = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);

    return class_loader;
}

void classloader_destory(ClassLoader *class_loader) {
    HashtableIterator hti;
    hashtable_iterate(class_loader->classes, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        gc_refer_release(v);
    }

    hashtable_clear(class_loader->classes);
    s32 i;
    for (i = 0; i < class_loader->classpath->length; i++) {
        utf8_destory(arraylist_get_value(class_loader->classpath, i));
    }
    arraylist_destory(class_loader->classpath);
    hashtable_destory(class_loader->classes);

    hashtable_destory(class_loader->table_jstring_const);

    class_loader->classes = NULL;

    spin_destroy(&class_loader->lock);
    jvm_free(class_loader);
}

void classloader_release_classs_static_field(ClassLoader *class_loader) {
    HashtableIterator hti;
    hashtable_iterate(class_loader->classes, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        JClass *clazz = (JClass *) (v);
        class_clear_refer(clazz);
    }
}

void classloader_add_jar_path(ClassLoader *class_loader, Utf8String *jarPath) {
    s32 i;
    for (i = 0; i < class_loader->classpath->length; i++) {
        if (utf8_equals(arraylist_get_value(class_loader->classpath, i), jarPath)) {
            return;
        }
    }
    Utf8String *jarPath1 = utf8_create_copy(jarPath);
    arraylist_push_back(class_loader->classpath, jarPath1);
}

void set_jvm_state(int state) {
    jvm_state = state;
}

int get_jvm_state() {
    return jvm_state;
}

void _on_jvm_sig(int no) {

    jvm_printf("[ERROR]jvm signo:%d  errno: %d , %s\n", no, errno, strerror(errno));
    exit(no);
}

void jvm_init(c8 *p_bootclasspath, c8 *p_classpath, StaticLibRegFunc regFunc) {
    signal(SIGABRT, _on_jvm_sig);
    signal(SIGFPE, _on_jvm_sig);
    signal(SIGSEGV, _on_jvm_sig);
    signal(SIGTERM, _on_jvm_sig);
#ifdef SIGPIPE
    signal(SIGPIPE, _on_jvm_sig);
#endif
    if (get_jvm_state() != JVM_STATUS_UNKNOW) {
        return;
    }
    set_jvm_state(JVM_STATUS_INITING);

    if (!p_classpath) {
        p_bootclasspath = "./";
    }

    //
    open_log();

#if _JVM_DEBUG_PROFILE
    profile_init();
#endif
    //
    init_jni_func_table();

    //创建线程容器
    thread_list = arraylist_create(0);
    //创建垃圾收集器
    garbage_collector_create();

    memset(&jvm_runtime_cache, 0, sizeof(OptimizeCache));

    //本地方法库
    native_libs = arraylist_create(0);
    reg_std_native_lib();
    reg_net_native_lib();
    reg_jdwp_native_lib();
    if (regFunc)regFunc(&jnienv);//register static lib

    boot_classloader = classloader_create(p_bootclasspath);

    //装入系统属性
    sys_properties_load(boot_classloader);
    sys_properties_set_c("java.class.path", p_classpath);
    sys_properties_set_c("sun.boot.class.path", p_bootclasspath);

    //启动调试器
    jdwp_start_server();

    set_jvm_state(JVM_STATUS_RUNNING);

    //init load thread, string etc
    Runtime *runtime = runtime_create(NULL);
    Utf8String *clsName = utf8_create_c(STR_CLASS_JAVA_LANG_INTEGER);
    classes_load_get(NULL, clsName, runtime);
    utf8_clear(clsName);
    utf8_append_c(clsName, STR_CLASS_JAVA_LANG_THREAD);
    classes_load_get(NULL, clsName, runtime);
    utf8_clear(clsName);
    utf8_append_c(clsName, STR_CLASS_ORG_MINI_REFLECT_LAUNCHER);
    classes_load_get(NULL, clsName, runtime);
    //开始装载类
    utf8_destory(clsName);
    runtime_destory(runtime);
    runtime = NULL;
}

void jvm_destroy(StaticLibRegFunc unRegFunc) {
    while (threadlist_count_none_daemon() > 0 && !collector->exit_flag) {//wait for other thread over ,
        threadSleep(20);
    }
    set_jvm_state(JVM_STATUS_STOPED);
    //waiting for daemon thread terminate
    s32 i;
    while (thread_list->length) {
        thread_stop_all();
        for (i = 0; i < thread_list->length; i++) {
            Runtime *r = threadlist_get(i);
            if (r) {
                if (!r->son) {//未在执行jvm指令
                    thread_unboundle(r);//
                }
            }
        }
        threadSleep(20);
    }
    jdwp_stop_server();
    //
    garbage_collector_destory();
    //

    arraylist_destory(thread_list);
    native_lib_destory();
    sys_properties_dispose();
    close_log();
#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("[INFO]jvm destoried\n");
#endif
    set_jvm_state(JVM_STATUS_UNKNOW);
}

s32 execute_jvm(c8 *p_bootclasspath, c8 *p_classpath, c8 *p_mainclass, ArrayList *java_para) {
    jvm_init(p_bootclasspath, p_classpath, NULL);


    Runtime *runtime = runtime_create(NULL);
    thread_boundle(runtime);

    //准备参数
    s32 count = java_para ? java_para->length : 0;
    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_STRING);
    Instance *arr = jarray_create_by_type_name(runtime, count, ustr);
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
    s32 ret = call_method(p_mainclass, p_methodname, p_methodtype, runtime);

    thread_unboundle(runtime);
    runtime_destory(runtime);

    jvm_destroy(NULL);
    return ret;
}


s32 call_method(c8 *p_mainclass, c8 *p_methodname, c8 *p_methodtype, Runtime *p_runtime) {
    if (!p_mainclass) {
        jvm_printf("No main class .\n");
        return 1;
    }
    //创建运行时栈
    Runtime *runtime = p_runtime;
    if (!p_runtime) {
        runtime = runtime_create(NULL);
        thread_boundle(runtime);
    }

    //开始装载类

    Utf8String *str_mainClsName = utf8_create_c(p_mainclass);
    utf8_replace_c(str_mainClsName, ".", "/");

    //装入主类

    JClass *clazz = classes_load_get(NULL, str_mainClsName, runtime);

    s32 ret = 0;
    if (clazz) {
        Utf8String *methodName = utf8_create_c(p_methodname);
        Utf8String *methodType = utf8_create_c(p_methodtype);

        MethodInfo *m = find_methodInfo_by_name(str_mainClsName, methodName, methodType, runtime);
        if (m) {



            s64 start = currentTimeMillis();
#if _JVM_DEBUG_LOG_LEVEL > 0
            jvm_printf("\n[INFO]main thread start\n");
#endif
            //调用主方法
            if (jdwp_enable) {
                if (jdwp_suspend_on_start)jthread_suspend(runtime);
                jvm_printf("[JDWP]waiting for jdwp(port:%s) debug client connected...\n", JDWP_TCP_PORT);
            }//jdwp 会启动调试器
            runtime->method = NULL;
            runtime->clazz = clazz;
            ret = execute_method(m, runtime);
            if (ret != RUNTIME_STATUS_NORMAL && ret != RUNTIME_STATUS_INTERRUPT) {
                print_exception(runtime);
            }
#if _JVM_DEBUG_LOG_LEVEL > 0
            jvm_printf("[INFO]main thread over %llx , spent : %lld\n", (s64) (intptr_t) runtime->threadInfo->jthread, (currentTimeMillis() - start));
#endif

#if _JVM_DEBUG_PROFILE
            profile_print();
#endif


        }
        utf8_destory(methodName);
        utf8_destory(methodType);
    }
    if (!p_runtime) {
        thread_unboundle(runtime);
        runtime_destory(runtime);
    }


    utf8_destory(str_mainClsName);
    //

    return ret;
}


s32 execute_method(MethodInfo *method, Runtime *runtime) {
    jthread_block_exit(runtime);
    s32 ret = execute_method_impl(method, runtime);
    jthread_block_enter(runtime);
    return ret;
}
