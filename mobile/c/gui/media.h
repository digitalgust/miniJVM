//
// Created by gust on 2018/1/17.
//

#ifndef JNI_GUI_JNI_GLFW_H
#define JNI_GUI_JNI_GLFW_H

//tag dont delete this line, builder will auto insert here



#define NUTIL_API extern

typedef struct _GlobeRefer GlobeRefer;
extern GlobeRefer refers;

__refer ptr_GLFuncTable();

s32 count_GLFuncTable();

__refer ptr_GlfmFuncTable();

s32 count_GlfmFuncTable();

__refer ptr_NanovgFuncTable();

s32 count_NanovgFuncTable();


__refer ptr_MiniAudioFuncTable();

s32 count_MiniAudioFuncTable();

//
Runtime *getRuntimeCurThread(JniEnv *env);

s64 get_gl_proc(const char *namez);

struct _GlobeRefer {
    MiniJVM *jvm;
    JniEnv *env;
    Instance *glfm_callback;

    MethodInfo *_callback_surface_error;
    MethodInfo *_callback_key;
    MethodInfo *_callback_character;
    MethodInfo *_callback_render;
    MethodInfo *_callback_memory_warning;
    MethodInfo *_callback_keyboard_visible;
    MethodInfo *_callback_touch;
    MethodInfo *_callback_surface_destroyed;
    MethodInfo *_callback_surface_resized;
    MethodInfo *_callback_app_focus;
    MethodInfo *_callback_surface_created;
    MethodInfo *_callback_photo_picked;
    MethodInfo *_callback_notify;
    MethodInfo *_callback_orientation_changed;

    //
    MethodInfo *_callback_minial_on_send_frames;
    MethodInfo *_callback_minial_on_recv_frames;
    MethodInfo *_callback_minial_on_stop;
    
    //
    Pairlist *runtime_list;
};


// dont delete the comment .for generate jni
/*

 */
//implementation



#endif //JNI_GUI_JNI_GLFW_H
