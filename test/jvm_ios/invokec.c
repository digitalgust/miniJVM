//
//  call_c.c
//  iostestTests
//
//  Created by Gust on 2017/10/6.
//  Copyright © 2017年 Gust. All rights reserved.
//

#include "../../mini_jvm/jvm/jvm.h"
#include "../../mini_jvm/utils/arraylist.h"
#include "iostestTests-Bridging-Header.h"

int call_jvm(char* app_path) {
    s32 ret ;
    char path[512];
    memset(&path,0,512);
    strcat(path,app_path);
    strcat(path,"/lib/minijvm_rt.jar");
    strcat(path,";");
    strcat(path,app_path);
    strcat(path,"/lib/minijvm_test.jar");
    printf("classpath: %s\n",path);
    ArrayList * java_para=arraylist_create(0);
    ret= execute_jvm(path, "test/Foo3", java_para);
    arraylist_destory(java_para);
    return ret;
}

