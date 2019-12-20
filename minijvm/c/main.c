/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * File:   test.c
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


    char *classpath = NULL;
    char *main_name = NULL;
    ArrayList *java_para = arraylist_create(0);
    s32 ret;
    //  mini_jvm   -Xmx16M -cp ../../binary/lib/minijvm_rt.jar;../../binary/lib/minijvm_test.jar;./ test/Foo1 999
    if (argc > 1) {
        s32 i;
        for (i = 1; i < argc; i++) {
            if (strcmp(argv[i], "-cp") == 0 || strcmp(argv[i], "-classpath") == 0) {
                classpath = argv[i + 1];
                i++;
            } else if (strcmp(argv[i], "-Xdebug") == 0) {
                jdwp_enable = 1;
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
                        MAX_HEAP_SIZE = num * mb * 1024 * 1024;
                    //jvm_printf("%s , %lld\n", argv[i], MAX_HEAP_SIZE);
                } else
                    jvm_printf("skiped argv: %s", argv[i]);
                //other jvm para
            } else if (main_name == NULL) {
                main_name = argv[i];
            } else {
                arraylist_push_back(java_para, argv[i]);
            }
        }
    } else {
        jdwp_enable = 0;
//        classpath = "../../binary/lib/minijvm_rt.jar;../../binary/libex/glfw_gui.jar;./";
//        main_name = "test/Gears";
//        main_name = "test/TestGL";
//        main_name = "test/AppManagerTest";
//        main_name = "test/RenderTexure";
//        main_name = "test/Alpha";
//        main_name = "test/Light";
//        main_name = "test/Shader";
//        main_name = "test/Boing";
//        main_name = "test/TestNanovg";

//        classpath = "../../binary/lib/minijvm_rt.jar;../../binary/libex/jni_test.jar;./";
//        main_name = "test/JniTest";

//        classpath = "../../binary/lib/minijvm_rt.jar;../../binary/libex/luaj.jar;./";
//        main_name = "Sample";

//        classpath = "../../../minijvm_third_lib/vm_test_rt/target/test_rt.jar;";
//        main_name = "com/egls/test/Foo1";

//        classpath = "../../../minijvm_third_lib/vm_micro_rt/target/micro_rt.jar;";
//        main_name = "test/Foo3";


//        classpath = "../../binary/lib/minijvm_rt.jar;../../binary/libex/minijvm_test.jar;./";
//        main_name = "test/HelloWorld";
//        main_name = "test/Foo1";
//        main_name = "test/Foo2";
//        main_name = "test/Foo3";
//        main_name = "test/SpecTest";
//        main_name = "test/MultiThread";
//        main_name = "test/ExecuteSpeed";
//        main_name = "test/TestFile";
//        main_name = "test/HttpServer";
//        main_name = "test/BpDeepTest";
//        main_name = "test/ReflectTest";
//        main_name = "test/LambdaTest";




//        classpath = "../../binary/lib/minijvm_rt.jar;../../binary/libex/janino.jar;../../binary/libex/commons-compiler.jar";
//        main_name = "org.codehaus.janino.Compiler";
//        arraylist_push_back(java_para,"../../binary/res/BpDeepTest.java");



    }
    ret = execute_jvm(classpath, main_name, java_para);
    arraylist_destory(java_para);
    //getchar();
    return ret;
}

