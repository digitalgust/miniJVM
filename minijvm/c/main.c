/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * File:   main.c
 * Author: gust
 *
 * Created on 2017年7月19日, 下午3:14
 */
#include <stdio.h>

#include <signal.h>
#include "jvm/jvm_util.h"
#include "jvm/jvm.h"
#include "jvm/garbage.h"


/*
 *
 */
int main(int argc, char **argv) {

    c8 *bootclasspath = NULL;
    c8 *classpath = NULL;
    c8 *main_name = NULL;
    s32 main_set = 0;
    ArrayList *java_para = arraylist_create(0);
    s32 jdwp = 0;
    s64 maxheap = MAX_HEAP_SIZE_DEFAULT;
    s32 ret;
    Utf8String *bootcp = utf8_create();
    Utf8String *cp = utf8_create();

    //get startup dir
    Utf8String *startup_dir = utf8_create_c(argv[0]);
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    utf8_replace_c(startup_dir, "\\", "/");
#endif
    s32 dpos = utf8_last_indexof_c(startup_dir, "/");
    if (dpos > 0)utf8_substring(startup_dir, 0, dpos);
    else if (dpos < 0) {
        utf8_clear(startup_dir);
        utf8_append_c(startup_dir, "./");
    }
    if (utf8_char_at(startup_dir, startup_dir->length - 1) != '/')utf8_append_c(startup_dir, "/");
#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("App dir:%s\n", utf8_cstr(startup_dir));
#endif
    //default value
    {
        utf8_append(bootcp, startup_dir);
        utf8_append_c(bootcp, "../lib/minijvm_rt.jar");
        bootclasspath = (c8 *) utf8_cstr(bootcp);
        jdwp = 0;  // 0:disable java debug , 1:enable java debug and disable jit

        //test for graphics
        utf8_append(cp, startup_dir);
        utf8_append_c(cp, "../libex/glfw_gui.jar");
        utf8_append_c(cp, PATHSEPARATOR);
        utf8_append(cp, startup_dir);
        utf8_append_c(cp, "../libex/xgui.jar");
        utf8_append_c(cp, PATHSEPARATOR);
        utf8_append_c(cp, "./");
        utf8_append_c(cp, PATHSEPARATOR);
        classpath = (c8 *) utf8_cstr(cp);
        main_name = "org.mini.glfw.GlfwMain";

        //test case
//        utf8_append(cp, startup_dir);
//        utf8_append_c(cp, "../libex/minijvm_test.jar");
//        utf8_append_c(cp, PATHSEPARATOR);
//        utf8_append_c(cp, "./");
//        utf8_append_c(cp, PATHSEPARATOR);
//        classpath = (c8 *) utf8_cstr(cp);
//        main_name = "test.HelloWorld";
//        main_name = "test.Foo1";
//        main_name = "test.Foo2";
//        main_name = "test.Foo3";
//        main_name = "test.ThreadDaemon";
//        main_name = "test.SpecTest";
//        main_name = "test.MultiThread";
//        main_name = "test.ExecuteSpeed";
//        main_name = "test.TestFile";
//        main_name = "test.HttpServer";
//        main_name = "test.BpDeepTest";
//        main_name = "test.ReflectTest";
//        main_name = "test.LambdaTest";
//        main_name = "test.NioBufferTest";

        //compiler test
//        utf8_append(cp, startup_dir);
//        utf8_append_c(cp, "../libex/janino.jar");
//        utf8_append_c(cp, PATHSEPARATOR);
//        utf8_append(cp, startup_dir);
//        utf8_append_c(cp, "../libex/commons-compiler.jar");
//        utf8_append_c(cp, PATHSEPARATOR);
//        classpath = (c8 *) utf8_cstr(cp);
//        main_name = "org.codehaus.janino.Compiler";
//        arraylist_push_back(java_para,"../res/BpDeepTest.java");

        //test awtk
//        utf8_append(cp, startup_dir);
//        utf8_append_c(cp, "../libex/awtk_gui.jar");
//        utf8_append_c(cp, PATHSEPARATOR);
//        utf8_append(cp, startup_dir);
//        utf8_append_c(cp, "../libex/awtk_demos.jar");
//        utf8_append_c(cp, PATHSEPARATOR);
//        utf8_append_c(cp, "./");
//        utf8_append_c(cp, PATHSEPARATOR);
//        classpath = (c8 *) utf8_cstr(cp);
//        main_name = "DemoBasic";
//        main_name = "DemoButton";

        //test luaj
//        utf8_append(cp, startup_dir);
//        utf8_append_c(cp, "../libex/luncher.jar");
//        utf8_append_c(cp, PATHSEPARATOR);
//        utf8_append_c(cp, "./");
//        utf8_append_c(cp, PATHSEPARATOR);
//        classpath = (c8 *) utf8_cstr(cp);
//        main_name = "org.luaj.vm2.lib.jme.TestLuaJ";

    }//default args

    //  mini_jvm   -Xmx16M -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar;./ test/Foo1 999
    if (argc > 1) {
        s32 i;
        for (i = 1; i < argc; i++) {
            if (strcmp(argv[i], "-bootclasspath") == 0) {
                bootclasspath = argv[i + 1];
                i++;
            } else if (strcmp(argv[i], "-version") == 0) {
                classpath = NULL;
                main_name = "org.mini.vm.PrintVersion";
                i++;
            } else if (strcmp(argv[i], "-cp") == 0 || strcmp(argv[i], "-classpath") == 0) {
                classpath = argv[i + 1];
                i++;
            } else if (strcmp(argv[i], "-Xdebug") == 0) {
                jdwp = 1;
//                if (!jdwp_enable) {
//                    printf("binary not support debug, please recompile and define JDWP_DEBUG as 1 ");
//                }
            } else if (argv[i][0] == '-') {
                if (argv[i][1] == 'X' && argv[i][2] == 'm' && argv[i][3] == 'x') {//"-Xmx1G"
                    s32 alen = strlen(argv[i]);
                    s32 mb = 1;
                    if (argv[i][alen - 1] == 'g' || argv[i][alen - 1] == 'G') {
                        mb = 1000;
                    }
                    Utf8String *num_u = utf8_create_part_c(argv[i], 4, alen - 5);
                    s64 num = utf8_aton(num_u, 10);
                    if (num > 0)
                        maxheap = num * mb * 1024 * 1024;
                    //jvm_printf("%s , %lld\n", argv[i], MAX_HEAP_SIZE);
                } else
                    jvm_printf("skiped argv: %s", argv[i]);
                //other jvm para
            } else if (main_set == 0) {
                main_name = argv[i];
                main_set = 1;
            } else {
                arraylist_push_back(java_para, argv[i]);
            }
        }
    }

    MiniJVM *jvm = jvm_create();
    if (jvm != NULL) {
        jvm->startup_dir = utf8_create_copy(startup_dir);
        jvm->jdwp_enable = jdwp;
        jvm->jdwp_suspend_on_start = 0;
        jvm->max_heap_size = maxheap;//25*1024*1024;//

        ret = jvm_init(jvm, bootclasspath, classpath);
        if (ret) {
            jvm_printf("[ERROR]minijvm init error.\n");
        } else {
            ret = call_main(jvm, main_name, java_para);
        }
        jvm_destroy(jvm);
    }
    //getchar();

    utf8_destory(startup_dir);
    utf8_destory(bootcp);
    utf8_destory(cp);
    arraylist_destory(java_para);
    fflush(stdout);
    fflush(stderr);
    return ret;
}

