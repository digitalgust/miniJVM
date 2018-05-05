//
// Created by Gust on 2018/2/1.
//


#include "jvm.h"
#include "jni_gui.h"


void JNI_OnLoad_mini(JniEnv *env) {
    memset(&refers, 0, sizeof(GlobeRefer));
    refers.env = env;

    refers.runtime_list=pairlist_create(10);


    env->native_reg_lib(ptr_GlfmFuncTable(), count_GlfmFuncTable());
    env->native_reg_lib(ptr_GLFuncTable(), count_GLFuncTable());
    env->native_reg_lib(ptr_NanovgFuncTable(), count_NanovgFuncTable());
}

void JNI_OnUnload_mini(JniEnv *env) {
    env->native_remove_lib(ptr_GlfmFuncTable());
    env->native_remove_lib(ptr_GLFuncTable());
    env->native_remove_lib(ptr_NanovgFuncTable());

    s32 i;
    for(i=0;i<refers.runtime_list->count;i++){
        Runtime *runtime=pairlist_get_pair(refers.runtime_list, i).right;
        if(runtime){
            thread_unboundle(runtime);
            runtime_destory(runtime);
        }
    }
    pairlist_destory(refers.runtime_list);
    
}

Runtime *getRuntimeCurThread(){
    thrd_t t=thrd_current();
    Runtime *runtime=pairlist_get(refers.runtime_list, t);
    if(!runtime){
        runtime=runtime_create(NULL);
        thread_boundle(runtime);
        pairlist_put(refers.runtime_list, t,runtime);
    }
    return getLastSon(runtime);//
}
/* ===============================================================
 *
 *                          DEMO
 *
 * ===============================================================*/

