//
// Created by Gust on 2018/2/1.
//

#include "jvm.h"
#include "media.h"


//void JNI_OnLoad_mini(JniEnv *env) {
//    memset(&refers, 0, sizeof(GlobeRefer));
//    refers.env = env;
//
//    refers.runtime_list=pairlist_create(10);
//
//
//    env->native_reg_lib(ptr_GlfmFuncTable(), count_GlfmFuncTable());
//    env->native_reg_lib(ptr_GLFuncTable(), count_GLFuncTable());
//    env->native_reg_lib(ptr_NanovgFuncTable(), count_NanovgFuncTable());
//    env->native_reg_lib(ptr_MiniALFuncTable(), count_MiniALFuncTable());
//}
//
//void JNI_OnUnload_mini(JniEnv *env) {
//    env->native_remove_lib(ptr_GlfmFuncTable());
//    env->native_remove_lib(ptr_GLFuncTable());
//    env->native_remove_lib(ptr_NanovgFuncTable());
//
//    s32 i;
//    for(i=0;i<refers.runtime_list->count;i++){
//        Runtime *runtime=pairlist_get_pair(refers.runtime_list, i).right;
//        if(runtime){
//            thread_unboundle(runtime);
//            runtime_destory(runtime);
//        }
//    }
//    pairlist_destory(refers.runtime_list);
//
//}

JThreadRuntime *getRuntimeCurThread() {
    JThreadRuntime *runtime = tss_get(TLS_KEY_JTHREADRUNTIME);
    if (!runtime) {
        runtime = jthreadruntime_create();
        jthread_bound(runtime);
    }
    return runtime;
}

/* ===============================================================
 *
 *                          DEMO
 *
 * ===============================================================*/

