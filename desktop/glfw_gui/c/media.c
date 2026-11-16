//
// Created by Gust on 2018/2/1.
//


#include "jvm_util.h"
#include "jvm.h"
#include "media.h"

#if defined(_WIN32)
#define JNI_LIB_EXPORT __declspec(dllexport)
#else
#define JNI_LIB_EXPORT
#endif

JNI_LIB_EXPORT void JNI_OnLoad(MiniJVM *jvm) {
    memset(&refers, 0, sizeof(GlobeRefer));
    JniEnv *env = jvm->env;
    refers.jvm = jvm;
    refers.env = env;

    refers.runtime_list_lock_initialized = ma_mutex_init(&refers.runtime_list_lock) == MA_SUCCESS;

    env->native_reg_lib(jvm, ptr_GlfwFuncTable(), count_GlfwFuncTable());
    env->native_reg_lib(jvm, ptr_MiniAudioFuncTable(), count_MiniAudioFuncTable());
    env->native_reg_lib(jvm, ptr_GLFuncTable(), count_GLFuncTable());
    env->native_reg_lib(jvm, ptr_NutilFuncTable(), count_NutilFuncTable());
}

JNI_LIB_EXPORT void JNI_OnUnload(MiniJVM *jvm) {
    JniEnv *env = jvm->env;
    env->native_remove_lib(jvm, ptr_GlfwFuncTable());
    env->native_remove_lib(jvm, ptr_MiniAudioFuncTable());
    env->native_remove_lib(jvm, ptr_GLFuncTable());
    env->native_remove_lib(jvm, ptr_NutilFuncTable());

    if (refers.runtime_list_lock_initialized) {
        ma_mutex_lock(&refers.runtime_list_lock);
        refers.runtime_registry_shutting_down = 1;
        CallbackRuntimeEntry *runtime_entries = refers.runtime_list;
        CallbackRuntimeEntry *audio_entries = refers.audio_runtime_list;
        refers.runtime_list = NULL;
        refers.audio_runtime_list = NULL;
        ma_mutex_unlock(&refers.runtime_list_lock);
        while (runtime_entries) {
            CallbackRuntimeEntry *next = runtime_entries->next;
            if (runtime_entries->runtime) {
                env->thread_unboundle(runtime_entries->runtime);
                env->runtime_destory(runtime_entries->runtime);
            }
            free(runtime_entries);
            runtime_entries = next;
        }
        while (audio_entries) {
            CallbackRuntimeEntry *next = audio_entries->next;
            if (audio_entries->runtime) {
                env->thread_unboundle(audio_entries->runtime);
                env->runtime_destory(audio_entries->runtime);
            }
            free(audio_entries);
            audio_entries = next;
        }
        ma_mutex_uninit(&refers.runtime_list_lock);
        refers.runtime_list_lock_initialized = 0;
    }
}

static Runtime *get_or_create_runtime(CallbackRuntimeEntry **entries, intptr_t key, JniEnv *env) {
    CallbackRuntimeEntry *entry = *entries;
    while (entry) {
        if (entry->key == key) return entry->runtime;
        entry = entry->next;
    }

    Runtime *runtime = env->runtime_create(refers.jvm);
    if (!runtime) return NULL;
    env->thread_boundle(runtime);
    if (runtime->thrd_info && runtime->thrd_info->jthread) {
        env->jthread_set_daemon_value(runtime->thrd_info->jthread, runtime, 1);
    }

    entry = (CallbackRuntimeEntry *) calloc(1, sizeof(CallbackRuntimeEntry));
    if (!entry) {
        env->thread_unboundle(runtime);
        env->runtime_destory(runtime);
        return NULL;
    }
    entry->key = key;
    entry->runtime = runtime;
    entry->next = *entries;
    *entries = entry;
    return runtime;
}

static void destroy_runtime_entry(JniEnv *env, CallbackRuntimeEntry *entry) {
    if (entry->runtime) {
        env->thread_unboundle(entry->runtime);
        env->runtime_destory(entry->runtime);
    }
    free(entry);
}

Runtime *getRuntimeCurThread(JniEnv *env) {
    if (env->get_jvm_state(refers.jvm) != JVM_STATUS_RUNNING) {
        return NULL;
    }
    if (!refers.runtime_list_lock_initialized) return NULL;
    thrd_t t = env->thrd_current();
    ma_mutex_lock(&refers.runtime_list_lock);
    Runtime *runtime = refers.runtime_registry_shutting_down ? NULL
            : get_or_create_runtime(&refers.runtime_list, (intptr_t) t, env);
    ma_mutex_unlock(&refers.runtime_list_lock);

    return runtime ? env->getLastSon(runtime) : NULL;//
}

Runtime *getRuntimeForAudioDevice(JniEnv *env, void *device) {
    if (!device || !refers.runtime_list_lock_initialized
            || env->get_jvm_state(refers.jvm) != JVM_STATUS_RUNNING) {
        return NULL;
    }
    ma_mutex_lock(&refers.runtime_list_lock);
    Runtime *runtime = refers.runtime_registry_shutting_down ? NULL
            : get_or_create_runtime(&refers.audio_runtime_list, (intptr_t) device, env);
    ma_mutex_unlock(&refers.runtime_list_lock);
    return runtime ? env->getLastSon(runtime) : NULL;
}

void releaseRuntimeForAudioDevice(JniEnv *env, void *device) {
    if (!device || !refers.runtime_list_lock_initialized) return;
    ma_mutex_lock(&refers.runtime_list_lock);
    CallbackRuntimeEntry **link = &refers.audio_runtime_list;
    while (*link && (*link)->key != (intptr_t) device) link = &(*link)->next;
    CallbackRuntimeEntry *removed = *link;
    if (removed) *link = removed->next;
    ma_mutex_unlock(&refers.runtime_list_lock);
    if (removed) destroy_runtime_entry(env, removed);
}

/* ===============================================================
 *
 *                          DEMO
 *
 * ===============================================================*/
extern void *get_proc(const char *namez);

s64 get_gl_proc(const char *namez) {
    void *ptr = get_proc(namez);
    return (s64) (intptr_t) ptr;
}

int main(void) {

    return 0;
}
