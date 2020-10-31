//
//  call_c.c
//  iostestTests
//
//  Created by Gust on 2017/10/6.
//  Copyright © 2017年 Gust. All rights reserved.
//

#include "../../minijvm/c/jvm/jvm.h"
#include "../../minijvm/c/utils/arraylist.h"
#include "iostestTests-Bridging-Header.h"

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
    ArrayList * java_para = arraylist_create(0);
    MiniJVM *jvm = jvm_create();
    jvm_init(jvm, bootstrappath, path);
    call_main(jvm, "test/Foo3", java_para);
    jvm_destroy(jvm);
    arraylist_destory(java_para);
    return ret;
}

