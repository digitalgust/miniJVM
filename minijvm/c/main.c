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
#include <stddef.h>
#include <stdio.h>
#include <locale.h>
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
#include <direct.h>
#else
#include <unistd.h>
#endif

#include "utils/d_type.h"
#include "utils/bytebuf.h"
#include "jvm/jvm_util.h"
#include "jvm/jvm.h"
#include "jvm/garbage.h"

extern s32 conv_platform_encoding_2_utf8(Utf8String *dst, const c8 *src);
extern s32 conv_utf8_2_platform_encoding(ByteBuf *dst, Utf8String *src);

static void set_working_dir_to_startup_dir(Utf8String *startup_dir) {
    if (!startup_dir || startup_dir->length <= 0) {
        return;
    }
    ByteBuf *platform_path = bytebuf_create(0);
    if (!platform_path) {
        return;
    }
    if (conv_utf8_2_platform_encoding(platform_path, startup_dir) >= 0 && platform_path->buf) {
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
        if (_chdir(platform_path->buf) != 0) {
            jvm_printf("[WARN]set cwd failed:%s\n", platform_path->buf);
        }
#else
        if (chdir(platform_path->buf) != 0) {
            jvm_printf("[WARN]set cwd failed:%s\n", platform_path->buf);
        }
#endif
    }
    bytebuf_destroy(platform_path);
}

/*
 *  mini_jvm  -Xmx128M -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/glfw_gui.jar;../libex/xgui.jar org.mini.glfw.GlfwMain
 *  mini_jvm  -Xdebug -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.Foo3
 *  mini_jvm  -Xdebug -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.HeapDumpTest
 */
int main(int argc, char **argv) {
    setlocale(LC_ALL, "");

    jvm_init_mem_alloc();

    c8 *bootclasspath = NULL;
    c8 *classpath = NULL;
    c8 *main_name = NULL;
    s32 main_set = 0;
    ArrayList *java_para = arraylist_create(0);
    s32 jdwp_enable = 0;
    s32 jdwp_suspend_on_start = 0;
    s32 jdwp_port = JDWP_TCP_PORT;
    s64 maxheap = MAX_HEAP_SIZE_DEFAULT;
    s32 ret;
    Utf8String *bootcp = utf8_create();
    Utf8String *cp = utf8_create();

    //get startup dir

    Utf8String *startup_dir = utf8_create();
    conv_platform_encoding_2_utf8(startup_dir, argv[0]);
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
    set_working_dir_to_startup_dir(startup_dir);// fix macos startup dir with user home but not binary dir
    //default value
    {
        utf8_append(bootcp, startup_dir);
        utf8_append_c(bootcp, "../lib/minijvm_rt.jar");
        bootclasspath = (c8 *) utf8_cstr(bootcp);
        jdwp_enable = 0; // 0:disable java debug , 1:enable java debug and disable jit

        //test for graphics
        utf8_append(cp, startup_dir);
        utf8_append_c(cp, "../libex/glfw_gui.jar");
        utf8_append_c(cp, PATHSEPARATOR);
        utf8_append(cp, startup_dir);
        utf8_append_c(cp, "../libex/xgui.jar");
        utf8_append_c(cp, PATHSEPARATOR);
        utf8_append_c(cp, "./");
        utf8_append_c(cp, PATHSEPARATOR);
        main_name = "org.mini.glfw.GlfwMain";

        //test case
        //        utf8_append(cp, startup_dir);
        //        utf8_append_c(cp, "../libex/minijvm_test.jar");
        //        utf8_append_c(cp, PATHSEPARATOR);
        //        utf8_append_c(cp, "./");
        //        utf8_append_c(cp, PATHSEPARATOR);
        //        main_name = "test.AnnotationTest";
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
        //        main_name = "DemoBasic";
        //        main_name = "DemoButton";

        //test luaj
        //        utf8_append(cp, startup_dir);
        //        utf8_append_c(cp, "../libex/luncher.jar");
        //        utf8_append_c(cp, PATHSEPARATOR);
        //        utf8_append_c(cp, "./");
        //        utf8_append_c(cp, PATHSEPARATOR);
        //        main_name = "org.luaj.vm2.lib.jme.TestLuaJ";

        classpath = (c8 *) utf8_cstr(cp);
    } //default args

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
                jdwp_enable = 1;
                //                if (!jdwp_enable) {
                //                    printf("binary not support debug, please recompile and define JDWP_DEBUG as 1 ");
                //                }
            } else if (argv[i][0] == '-') {
                if (argv[i][1] == 'X' && argv[i][2] == 'm' && argv[i][3] == 'x') {
                    //"-Xmx1G"
                    s32 alen = strlen(argv[i]);
                    s32 mb = 1;
                    if (argv[i][alen - 1] == 'g' || argv[i][alen - 1] == 'G') {
                        mb = 1000;
                    }
                    Utf8String *num_u = utf8_create_part_c(argv[i], 4, alen - 5);
                    s64 num = utf8_aton(num_u, 10);
                    if (num > 0)
                        maxheap = num * mb * 1024 * 1024;
                    utf8_destroy(num_u);
                    //jvm_printf("%s , %lld\n", argv[i], MAX_HEAP_SIZE);
                } else if (argv[i][1] == 'X' && argv[i][2] == 'r' && argv[i][3] == 'u' && argv[i][4] == 'n' && argv[i][
                               5] == 'j' && argv[i][6] == 'd' && argv[i][7] == 'w' && argv[i][8] == 'p' && argv[i][9] ==
                           ':') {
                    //-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
                    s32 alen = strlen(argv[i]);
                    Utf8String *jdwp_u = utf8_create_part_c(argv[i], 10, alen - 10);
                    s32 p = 0;
                    Utf8String *part_u = utf8_create();
                    for (;; p++) {
                        utf8_clear(part_u);
                        utf8_split_get_part(jdwp_u, ",", p, part_u);
                        if (utf8_equals_c(part_u, "server=y")) {
                            jdwp_enable = 1;
                        }
                        if (utf8_equals_c(part_u, "suspend=y")) {
                            jdwp_suspend_on_start = 1;
                        }
                        if (utf8_indexof_c(part_u, "address=") == 0) {
                            utf8_substring(part_u, 8, part_u->length);
                            jdwp_port = utf8_aton(part_u, 10);
                        }
                        if (!part_u->length) {
                            break;
                        }
                    }
                    utf8_destroy(part_u);
                    utf8_destroy(jdwp_u);
                } else
                    jvm_printf("skiped argv: %s", argv[i]);
                //other jvm para
            } else if (main_set == 0) {
                main_name = argv[i];
                main_set = 1;

                // get main class args
                for (i++; i < argc; i++) {
                    arraylist_push_back(java_para, argv[i]);
                }
                break;
            }
        }
    }

    MiniJVM *jvm = jvm_create();
    if (jvm != NULL) {
        jvm->startup_dir = utf8_create_copy(startup_dir);
        jvm->jdwp_enable = jdwp_enable;
        jvm->jdwp_suspend_on_start = jdwp_suspend_on_start;
        jvm->jdwp_port = jdwp_port;
        jvm->max_heap_size = maxheap; //25*1024*1024;//

        ret = jvm_init(jvm, bootclasspath, classpath);
        if (ret) {
            jvm_printf("[ERROR]minijvm init error.\n");
        } else {
            ret = call_main(jvm, main_name, java_para);
        }
        jvm_destroy(jvm);
    }
    //getchar();

    utf8_destroy(startup_dir);
    utf8_destroy(bootcp);
    utf8_destroy(cp);
    arraylist_destroy(java_para);
    fflush(stdout);
    fflush(stderr);

    jvm_destroy_mem_alloc();

    return ret;
}
