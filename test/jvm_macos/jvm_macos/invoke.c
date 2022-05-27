//
//  invoke.c
//  jvm_macos
//
//  Created by Gust on 2018/1/30.
//  Copyright © 2018年 Gust. All rights reserved.
//

#include "../../../minijvm/c/jvm/jvm.h"
#include "../../../minijvm/c/utils/arraylist.h"
#include "jvm_macos-Bridging-Header.h"

int call_jvm(char* app_path) {
    
        s32 ret ;
        char bootstrappath[512];
        memset(&bootstrappath,0,512);
        strcat(bootstrappath,app_path);
        strcat(bootstrappath,"/lib/minijvm_rt.jar");
        strcat(bootstrappath,";");
        char path[512];
        memset(&path,0,512);
        strcat(path,app_path);
        strcat(path,"/libex/glfw_gui.jar");
        strcat(path,";");
        strcat(path,app_path);
        strcat(path,"/libex/minijvm_test.jar");
        printf("classpath: %s\n",path);

        ArrayList * java_para=arraylist_create(0);
    MiniJVM *jvm=jvm_create();
    jvm->jdwp_enable = 1;
    ret = jvm_init(jvm, bootstrappath, path);
    //ret = call_main(jvm, "test/HttpServer", java_para);
    //ret = call_main(jvm, "test/BpDeepTest", java_para);
    ret = call_main(jvm, "org.mini.glfw.GlfwMain", java_para);
    //ret = call_main(jvm, "test/ReflectTest", java_para);
    //ret = call_main(jvm, "test/LambdaTest", java_para);
        arraylist_destory(java_para);
    jvm_destroy(jvm);
    return ret;
}


