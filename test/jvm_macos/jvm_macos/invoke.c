//
//  invoke.c
//  jvm_macos
//
//  Created by Gust on 2018/1/30.
//  Copyright © 2018年 Gust. All rights reserved.
//

#include "../../../minijvm/c/jvm/jvm.h"
#include "../../../minijvm/c/utils/arraylist.h"
#include "../../../minijvm/c/utils/utf8_string.h"
#include "jvm_macos-Bridging-Header.h"

int call_jvm(char* app_path) {
    
    s32 ret ;
    Utf8String *bootstrappath=utf8_create_c(app_path);
    utf8_append_c(bootstrappath,"/lib/minijvm_rt.jar");
    utf8_append_c(bootstrappath,PATHSEPARATOR);
    Utf8String *path=utf8_create_c(app_path);
    utf8_append_c(path,"/libex/glfw_gui.jar");
    utf8_append_c(path,PATHSEPARATOR);
    utf8_append_c(path,app_path);
    utf8_append_c(path,"/libex/xgui.jar");
    utf8_append_c(path,PATHSEPARATOR);
    utf8_append_c(path,app_path);
    utf8_append_c(path,"/libex/minijvm_test.jar");
    printf("classpath: %s\n",utf8_cstr(path));
    
    ArrayList * java_para=arraylist_create(0);
    MiniJVM *jvm=jvm_create();
    jvm->jdwp_enable = 1;
    ret = jvm_init(jvm, utf8_cstr(bootstrappath), utf8_cstr(path));
    //ret = call_main(jvm, "test/HttpServer", java_para);
    //ret = call_main(jvm, "test/BpDeepTest", java_para);
    ret = call_main(jvm, "org.mini.glfw.GlfwMain", java_para);
    //ret = call_main(jvm, "test/ReflectTest", java_para);
    //ret = call_main(jvm, "test/LambdaTest", java_para);
    utf8_destroy(path);
    utf8_destroy(bootstrappath);
    arraylist_destroy(java_para);
    jvm_destroy(jvm);
    return ret;
}


