//
// Created by Gust on 2018/2/1.
//


#include "jvm_util.h"
#include "jvm.h"
#include "media.h"


void JNI_OnLoad(JniEnv *env) {
    memset(&refers, 0, sizeof(GlobeRefer));
    refers.env = env;

    refers.runtime_list = env->pairlist_create(10);

    env->native_reg_lib(ptr_GlfwFuncTable(), count_GlfwFuncTable());
    env->native_reg_lib(ptr_MiniALFuncTable(), count_MiniALFuncTable());
    env->native_reg_lib(ptr_GLFuncTable(), count_GLFuncTable());
    env->native_reg_lib(ptr_NutilFuncTable(), count_NutilFuncTable());
}

void JNI_OnUnload(JniEnv *env) {
    env->native_remove_lib(ptr_GlfwFuncTable());
    env->native_remove_lib(ptr_MiniALFuncTable());
    env->native_remove_lib(ptr_GLFuncTable());
    env->native_remove_lib(ptr_NutilFuncTable());
}

Runtime *getRuntimeCurThread(JniEnv *env) {
    if (env->get_jvm_state() != JVM_STATUS_RUNNING) {
        return NULL;
    }
    thrd_t t = env->thrd_current();
    Runtime *runtime = env->pairlist_get(refers.runtime_list, (__refer)(intptr_t)t);
    if (!runtime) {
        runtime = env->runtime_create(NULL);
        env->thread_boundle(runtime);
        env->jthread_set_daemon_value(runtime->threadInfo->jthread, runtime, 1);
        env->pairlist_put(refers.runtime_list, (__refer)(intptr_t)t, runtime);
    }

    return env->getLastSon(runtime);//
}

/* ===============================================================
 *
 *                          DEMO
 *
 * ===============================================================*/

int main(void) {

    return 0;
}
