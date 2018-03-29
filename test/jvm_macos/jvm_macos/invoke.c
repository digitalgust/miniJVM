//
//  invoke.c
//  jvm_macos
//
//  Created by Gust on 2018/1/30.
//  Copyright © 2018年 Gust. All rights reserved.
//

#include "../../../mini_jvm/jvm/jvm.h"
#include "../../../mini_jvm/utils/arraylist.h"
#include "jvm_macos-Bridging-Header.h"

int call_jvm(char* app_path) {
    s32 ret ;
    char path[512];
    memset(&path,0,512);
    strcat(path,app_path);
    strcat(path,"/lib/minijvm_rt.jar");
    strcat(path,";");
    strcat(path,app_path);
    strcat(path,"/lib/luaj.jar");
    printf("classpath: %s\n",path);
    java_debug=0;
    ArrayList * java_para=arraylist_create(0);
//    ret= execute_jvm(path, "test/Gears", java_para);
//    ret= execute_jvm(path, "test/GuiTest", java_para);
//    ret= execute_jvm(path, "test/Light", java_para);
    ret= execute_jvm(path, "Sample", java_para);
    arraylist_destory(java_para);
    return ret;
}

