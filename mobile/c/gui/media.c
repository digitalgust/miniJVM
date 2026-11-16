//
// Created by Gust on 2018/2/1.
//

#include "jvm_util.h"
#include "jvm.h"
#include "media.h"
#include <stdlib.h>


void JNI_OnLoad_mini(MiniJVM *jvm) {
    JniEnv *env = jvm->env;
    refers.jvm = jvm;
    refers.env = env;
    refers.runtime_list = NULL;
    refers.audio_runtime_list = NULL;
    refers.runtime_registry_shutting_down = 0;
    refers.runtime_list_lock_initialized = ma_mutex_init(&refers.runtime_list_lock) == MA_SUCCESS;

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
    env->native_remove_lib(jvm, ptr_MiniAudioFuncTable());

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

static intptr_t current_thread_key(JniEnv *env) {
#if defined(_TTHREAD_WIN32_)
    /* thrd_current() returns the same -2 pseudo-handle on Windows. */
    (void) env;
    return (intptr_t) GetCurrentThreadId();
#else
    return (intptr_t) env->thrd_current();
#endif
}

static CallbackRuntimeEntry *get_or_create_runtime(CallbackRuntimeEntry **entries, intptr_t key, intptr_t thread_key, JniEnv *env) {
    CallbackRuntimeEntry *entry = *entries;
    while (entry) {
        if (entry->key == key && entry->thread_key == thread_key) return entry;
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
    entry->thread_key = thread_key;
    entry->runtime = runtime;
    entry->next = *entries;
    *entries = entry;
    return entry;
}

Runtime *getRuntimeCurThread(JniEnv *env) {
    if (!refers.runtime_list_lock_initialized
            || env->get_jvm_state(refers.jvm) != JVM_STATUS_RUNNING) {
        return NULL;
    }
    intptr_t thread_key = current_thread_key(env);
    ma_mutex_lock(&refers.runtime_list_lock);
    CallbackRuntimeEntry *entry = refers.runtime_registry_shutting_down ? NULL
            : get_or_create_runtime(&refers.runtime_list, thread_key, 0, env);
    Runtime *runtime = entry ? env->getLastSon(entry->runtime) : NULL;
    ma_mutex_unlock(&refers.runtime_list_lock);

    return runtime;
}

Runtime *acquireRuntimeForAudioDevice(JniEnv *env, void *device, CallbackRuntimeEntry **lease) {
    if (!lease) return NULL;
    *lease = NULL;
    if (!device || !refers.runtime_list_lock_initialized
            || env->get_jvm_state(refers.jvm) != JVM_STATUS_RUNNING) {
        return NULL;
    }
    intptr_t thread_key = current_thread_key(env);
    ma_mutex_lock(&refers.runtime_list_lock);
    CallbackRuntimeEntry *entry = refers.runtime_registry_shutting_down ? NULL
            : get_or_create_runtime(&refers.audio_runtime_list, (intptr_t) device, thread_key, env);
    Runtime *runtime = NULL;
    if (entry) {
        runtime = env->getLastSon(entry->runtime);
        if (runtime) {
            entry->active_callbacks++;
            *lease = entry;
        }
    }
    ma_mutex_unlock(&refers.runtime_list_lock);
    return runtime;
}

void releaseRuntimeForAudioCallback(JniEnv *env, CallbackRuntimeEntry *lease) {
    if (!lease || !refers.runtime_list_lock_initialized) return;
    CallbackRuntimeEntry *destroy = NULL;
    ma_mutex_lock(&refers.runtime_list_lock);
    if (lease->active_callbacks > 0) lease->active_callbacks--;
    if (lease->retired && lease->active_callbacks == 0) destroy = lease;
    ma_mutex_unlock(&refers.runtime_list_lock);
    if (destroy) {
        if (destroy->runtime) {
            env->thread_unboundle(destroy->runtime);
            env->runtime_destory(destroy->runtime);
        }
        free(destroy);
    }
}

void releaseRuntimeForAudioDevice(JniEnv *env, void *device) {
    if (!device || !refers.runtime_list_lock_initialized) return;
    ma_mutex_lock(&refers.runtime_list_lock);
    CallbackRuntimeEntry **link = &refers.audio_runtime_list;
    CallbackRuntimeEntry *removed = NULL;
    while (*link) {
        CallbackRuntimeEntry *entry = *link;
        if (entry->key == (intptr_t) device) {
            *link = entry->next;
            entry->retired = 1;
            if (entry->active_callbacks == 0) {
                entry->next = removed;
                removed = entry;
            } else {
                entry->next = NULL;
            }
        } else {
            link = &entry->next;
        }
    }
    ma_mutex_unlock(&refers.runtime_list_lock);
    while (removed) {
        CallbackRuntimeEntry *next = removed->next;
        if (removed->runtime) {
            env->thread_unboundle(removed->runtime);
            env->runtime_destory(removed->runtime);
        }
        free(removed);
        removed = next;
    }
}

/* ===============================================================
 *
 *                          DEMO
 *
 * ===============================================================*/

extern int has_ext(const char *ext);

s64 get_gl_proc(const char *namez) {
    int has = has_ext(namez);
    return (s64) (intptr_t) has;
}
