//
// Created by Gust on 2018/2/1.
//

#include "jvm_util.h"
#include "jvm.h"
#include "media.h"


void JNI_OnLoad_mini(JniEnv *env) {
    memset(&refers, 0, sizeof(GlobeRefer));
    refers.env = env;

    refers.runtime_list=pairlist_create(10);


    env->native_reg_lib(ptr_GlfmFuncTable(), count_GlfmFuncTable());
    env->native_reg_lib(ptr_GLFuncTable(), count_GLFuncTable());
    env->native_reg_lib(ptr_NanovgFuncTable(), count_NanovgFuncTable());
    env->native_reg_lib(ptr_MiniALFuncTable(), count_MiniALFuncTable());
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

Runtime *getRuntimeCurThread(JniEnv *env) {
    if (env->get_jvm_state() != JVM_STATUS_RUNNING) {
        return NULL;
    }
    thrd_t t = env->thrd_current();
    Runtime *runtime = env->pairlist_get(refers.runtime_list, t);
    if (!runtime) {
        runtime = env->runtime_create(NULL);
        env->thread_boundle(runtime);
        env->jthread_set_daemon_value(runtime->threadInfo->jthread, runtime, 1);
        env->pairlist_put(refers.runtime_list, t, runtime);
    }
    
    return env->getLastSon(runtime);//
}

/* ===============================================================
 *
 *                          DEMO
 *
 * ===============================================================*/

