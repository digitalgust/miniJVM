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
        strcat(path,"/libex/luaj.jar");
        strcat(path,";");
        strcat(path,app_path);
        strcat(path,"/libex/minijvm_test.jar");
        printf("classpath: %s\n",path);

        ArrayList * java_para=arraylist_create(0);
        ret= execute_jvm(bootstrappath, path, "test/HttpServer", java_para);
    //    ret= execute_jvm(bootstrappath, path, "test/BpDeepTest", java_para);
    //    ret= execute_jvm(bootstrappath, path, "Sample", java_para);
    //    ret= execute_jvm(bootstrappath, path, "test/ReflectTest", java_para);
    //    ret= execute_jvm(bootstrappath, path, "test/LambdaTest", java_para);
        arraylist_destory(java_para);
    return ret;
}


