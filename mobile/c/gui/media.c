//
// Created by Gust on 2018/2/1.
//

#include "jvm_util.h"
#include "jvm.h"
#include "media.h"


void JNI_OnLoad_mini(MiniJVM *jvm) {
    JniEnv *env = jvm->env;
    refers.env = env;

    refers.runtime_list=pairlist_create(10);


    env->native_reg_lib(jvm, ptr_GlfmFuncTable(), count_GlfmFuncTable());
    env->native_reg_lib(jvm, ptr_GLFuncTable(), count_GLFuncTable());
    env->native_reg_lib(jvm, ptr_NanovgFuncTable(), count_NanovgFuncTable());
    env->native_reg_lib(jvm, ptr_MiniAudioFuncTable(), count_MiniAudioFuncTable());
}

void JNI_OnUnload_mini(MiniJVM *jvm) {
    JniEnv *env = jvm->env;
    env->native_remove_lib(jvm, ptr_GlfmFuncTable());
    env->native_remove_lib(jvm, ptr_GLFuncTable());
    env->native_remove_lib(jvm, ptr_NanovgFuncTable());

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

tss_t TSS_KEY_RUNTIME_OF_THREAD;
Runtime *getRuntimeCurThread(JniEnv *env) {
    static s32 init=0;
    if(!init){
        tss_create(&TSS_KEY_RUNTIME_OF_THREAD,NULL);
        //tss_create(&TSS_KEY_RUNTIME_OF_THREAD,(tss_dtor_t)env->runtime_destory);
        init = 1;
    }
    if (env->get_jvm_state(refers.jvm) != JVM_STATUS_RUNNING) {
        return NULL;
    }
    thrd_t t = env->thrd_current();
    Runtime *runtime = tss_get(TSS_KEY_RUNTIME_OF_THREAD);
    if (!runtime) {
        runtime = env->runtime_create(refers.jvm);
        env->thread_boundle(runtime);
        env->jthread_set_daemon_value(runtime->thrd_info->jthread, runtime, 1);
        tss_set(TSS_KEY_RUNTIME_OF_THREAD,runtime);
    }
    
    return runtime;//
}

/* ===============================================================
 *
 *                          DEMO
 *
 * ===============================================================*/

