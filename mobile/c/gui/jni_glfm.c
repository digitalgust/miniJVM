/* nuklear - 1.32.0 - public domain */
#define WIN32_LEAN_AND_MEAN

#include <stdio.h>
#include <string.h>

#include "glfm.h"
#include "linmath.h"

//#define STB_IMAGE_IMPLEMENTATION

//#include "deps/include/stb_image.h"

#include "jvm.h"
#include "media.h"

extern const char *glfmGetResRoot();

extern int gladLoadGLES2Loader(void *fun);

GlobeRefer refers;


/* ==============================   local tools  =================================*/


Instance *createJavaString(Runtime *runtime, c8 *cstr) {
    if (cstr == NULL) {
        return NULL;
    }
    JniEnv *env = runtime->jnienv;
    Utf8String *ustr = env->utf8_create_part_c(cstr, 0, strlen(cstr));
    Instance *jstr = env->jstring_create(ustr, runtime);
    env->utf8_destory(ustr);
    return jstr;
}

/* ==============================   jni callback =================================*/
static void _callback_error_before_init(int error, const char *description) {
    fprintf(stderr, "GLFM Error: %s\n", description);
}

static void _callback_surface_error(GLFMDisplay *window, const char *description) {
    if (refers._callback_surface_error) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        Instance *jstr = createJavaString(runtime, description);
        env->push_ref(runtime->stack, jstr);

        s32 ret = env->execute_method(refers._callback_surface_error, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

static bool _callback_key(GLFMDisplay *window, GLFMKey key, GLFMKeyAction action, int mods) {
    if (refers._callback_key) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, key);
        env->push_int(runtime->stack, action);
        env->push_int(runtime->stack, mods);

        s32 ret = env->execute_method(refers._callback_key, runtime);
        if (ret) {
            env->print_exception(runtime);
        } else {
            return env->pop_int(runtime->stack);
        }
    }
    return false;
}

static void _callback_character(GLFMDisplay *window, const char *utf8, int modifiers) {
    if (refers._callback_character) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        Instance *ins = createJavaString(runtime, utf8);
        env->push_ref(runtime->stack, ins);
        env->push_int(runtime->stack, modifiers);

        s32 ret = env->execute_method(refers._callback_character, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

static void _callback_mainloop(GLFMDisplay *window, f64 frameTime) {
    if (refers._callback_mainloop) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_double(runtime->stack, frameTime);

        s32 ret = env->execute_method(refers._callback_mainloop, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_memory_warning(GLFMDisplay *window) {
    if (refers._callback_memory_warning) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        s32 ret = env->execute_method(refers._callback_memory_warning, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_keyboard_visible(GLFMDisplay *window, bool visible, f64 x, f64 y, f64 w, f64 h) {
    if (refers._callback_keyboard_visible) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, visible);
        env->push_double(runtime->stack, x);
        env->push_double(runtime->stack, y);
        env->push_double(runtime->stack, w);
        env->push_double(runtime->stack, h);

        s32 ret = env->execute_method(refers._callback_keyboard_visible, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

bool _callback_touch(GLFMDisplay *window, s32 touch, GLFMTouchPhase phase, f64 x, f64 y) {
    if (refers._callback_touch) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, touch);
        env->push_int(runtime->stack, phase);
        env->push_double(runtime->stack, x);
        env->push_double(runtime->stack, y);

        s32 ret = env->execute_method(refers._callback_touch, runtime);
        if (ret) {
            env->print_exception(runtime);
        } else {
            return env->pop_int(runtime->stack);
        }
    }
    return 1;
}


void _callback_surface_resized(GLFMDisplay *window, s32 w, s32 h) {
    if (refers._callback_surface_resized) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, w);
        env->push_int(runtime->stack, h);

        s32 ret = env->execute_method(refers._callback_surface_resized, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_surface_destroyed(GLFMDisplay *window) {
    if (refers._callback_surface_destroyed) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);

        s32 ret = env->execute_method(refers._callback_surface_destroyed, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_app_focus(GLFMDisplay *window, bool focus) {
    if (refers._callback_app_focus) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, focus);

        s32 ret = env->execute_method(refers._callback_app_focus, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_surface_created(GLFMDisplay *window, s32 w, s32 h) {
    gladLoadGLES2Loader(glfmGetProcAddress);
    if (refers._callback_surface_created) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, w);
        env->push_int(runtime->stack, h);

        s32 ret = env->execute_method(refers._callback_surface_created, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void
_callback_photo_picked(GLFMDisplay *window, s32 uid, const c8 *url, c8 *data, s32 length) {
    gladLoadGLES2Loader(glfmGetProcAddress);
    if (refers._callback_surface_created) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        Instance *jstr_url = createJavaString(runtime, url);
        Instance *jarr = NULL;
        if (data) {
            jarr = env->jarray_create_by_type_index(runtime, length, DATATYPE_BYTE);
            memcpy(jarr->arr_body, data, length);
        }
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, uid);
        env->push_ref(runtime->stack, jstr_url);
        env->push_ref(runtime->stack, jarr);

        s32 ret = env->execute_method(refers._callback_photo_picked, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_notify(GLFMDisplay *window, const c8 *key, const c8 *val) {
    gladLoadGLES2Loader(glfmGetProcAddress);
    if (refers._callback_surface_created) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        Instance *insKey = createJavaString(runtime, key);
        env->push_ref(runtime->stack, insKey);
        Instance *insVal = createJavaString(runtime, val);
        env->push_ref(runtime->stack, insVal);

        s32 ret = env->execute_method(refers._callback_notify, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_orientation_changed(GLFMDisplay *window, GLFMInterfaceOrientation orientation) {
    if (refers._callback_surface_resized) {
        Runtime *runtime = getRuntimeCurThread(refers.env);
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64) (intptr_t) window);
        env->push_int(runtime->stack, orientation);

        s32 ret = env->execute_method(refers._callback_orientation_changed, runtime);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

/* ==============================   jni glfm =================================*/

int org_mini_glfm_Glfm_glfmSetCallBack(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t)
            env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    refers.glfm_callback = env->localvar_getRefer(runtime->localvar, pos++);

    //this object not refered by jvm , so needs to hold by jni manaul
    if (refers.glfm_callback) env->instance_release_from_thread(refers.glfm_callback, runtime);
    //env->instance_hold_to_thread(refers.glfm_callback, runtime);
    env->garbage_refer_hold(runtime->jvm->collector, refers.glfm_callback);


    glfmSetMainLoopFunc(window, _callback_mainloop);
    glfmSetKeyFunc(window, _callback_key);
    glfmSetCharFunc(window, _callback_character);
    glfmSetAppFocusFunc(window, _callback_app_focus);
    glfmSetTouchFunc(window, _callback_touch);
    glfmSetSurfaceErrorFunc(window, _callback_surface_error);
    glfmSetSurfaceCreatedFunc(window, _callback_surface_created);
    glfmSetSurfaceResizedFunc(window, _callback_surface_resized);
    glfmSetSurfaceDestroyedFunc(window, _callback_surface_destroyed);
    glfmSetMemoryWarningFunc(window, _callback_memory_warning);
    glfmSetKeyboardVisibilityChangedFunc(window, _callback_keyboard_visible);
    glfmSetPhotoPickedFunc(window, _callback_photo_picked);
    glfmSetNotifyFunc(window, _callback_notify);
    glfmSetOrientationChangedFunc(window, _callback_orientation_changed);


    Instance *jloader = clazz->jloader;
    c8 *name_s, *type_s;
    {
        name_s = "onKey";
        type_s = "(JIII)Z";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_key = env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name,
                                                            name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onCharacter";
        type_s = "(JLjava/lang/String;I)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_character = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onKeyboardVisible";
        type_s = "(JZDDDD)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_keyboard_visible = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "mainLoop";
        type_s = "(JD)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_mainloop = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onMemWarning";
        type_s = "(J)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_memory_warning = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    {
        name_s = "onTouch";
        type_s = "(JIIDD)Z";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_touch = env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name,
                                                              name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {

        name_s = "onSurfaceError";
        type_s = "(JLjava/lang/String;)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_error = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    {
        name_s = "onSurfaceCreated";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_created = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    {
        name_s = "onSurfaceResize";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_resized = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onSurfaceDestroyed";
        type_s = "(J)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_destroyed = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    {
        name_s = "onAppFocus";
        type_s = "(JZ)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_app_focus = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    {
        name_s = "onPhotoPicked";
        type_s = "(JILjava/lang/String;[B)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_photo_picked = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    {
        name_s = "onNotify";
        type_s = "(JLjava/lang/String;Ljava/lang/String;)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_notify = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    {
        name_s = "onOrientationChanged";
        type_s = "(JI)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_orientation_changed = env->find_methodInfo_by_name(
                refers.glfm_callback->mb.clazz->name, name, type, jloader, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

    return 0;
}


int org_mini_glfm_Glfm_glfmSetDisplayConfig(Runtime *runtime, JClass *clazz) {//
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 preferredAPI = env->localvar_getInt(runtime->localvar, pos++);
    s32 colorFormat = env->localvar_getInt(runtime->localvar, pos++);
    s32 depthFormat = env->localvar_getInt(runtime->localvar, pos++);
    s32 stencilFormat = env->localvar_getInt(runtime->localvar, pos++);
    s32 multisample = env->localvar_getInt(runtime->localvar, pos++);

    glfmSetDisplayConfig(window, preferredAPI, colorFormat, depthFormat, stencilFormat,
                         multisample);

    return 0;
}

int org_mini_glfm_Glfm_glfmSetSupportedInterfaceOrientation(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 allowedOrientations = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetSupportedInterfaceOrientation(window, allowedOrientations);
    return 0;
}


int org_mini_glfm_Glfm_glfmGetInterfaceOrientation(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmGetInterfaceOrientation(window));
    return 0;
}

int org_mini_glfm_Glfm_glfmGetSupportedInterfaceOrientation(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmGetSupportedInterfaceOrientation(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetMultitouchEnabled(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 multitouchEnabled = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetMultitouchEnabled(window, multitouchEnabled);
    return 0;
}


int org_mini_glfm_Glfm_glfmGetMultitouchEnabled(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmGetMultitouchEnabled(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmGetDisplayWidth(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfmGetDisplaySize(window, &w, &h);
    env->push_int(runtime->stack, w);
    return 0;
}

int org_mini_glfm_Glfm_glfmGetDisplayHeight(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfmGetDisplaySize(window, &w, &h);
    env->push_int(runtime->stack, h);
    return 0;
}

int org_mini_glfm_Glfm_glfmGetDisplayScale(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    env->push_double(runtime->stack, glfmGetDisplayScale(window));
    return 0;
}

int org_mini_glfm_Glfm_glfmGetDisplayChromeInsets(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    if (r != NULL && r->arr_length >= 4) {
        glfmGetDisplayChromeInsets(window, &((f64 *) r->arr_body)[0], &((f64 *) r->arr_body)[1],
                                   &((f64 *) r->arr_body)[2], &((f64 *) r->arr_body)[3]);
    }
    return 0;
}

int org_mini_glfm_Glfm_glfmGetDisplayChrome(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    env->push_int(runtime->stack, glfmGetDisplayChrome(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetDisplayChrome(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 uiChrome = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetDisplayChrome(window, uiChrome);
    return 0;
}


int org_mini_glfm_Glfm_glfmGetRenderingAPI(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmGetRenderingAPI(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmHasTouch(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmHasTouch(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetMouseCursor(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 mouseCursor = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetMouseCursor(window, mouseCursor);
    return 0;
}


int org_mini_glfm_Glfm_glfmExtensionSupported(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    Instance *ext = env->localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr = env->utf8_create();
    env->jstring_2_utf8(ext, ustr, runtime);
    env->push_int(runtime->stack, glfmExtensionSupported(env->utf8_cstr(ustr)));
    env->utf8_destory(ustr);
    return 0;
}


int org_mini_glfm_Glfm_glfmIsKeyboardVisible(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    env->push_int(runtime->stack, glfmIsKeyboardVisible(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetKeyboardVisible(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 visible = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetKeyboardVisible(window, visible);
    return 0;
}


int org_mini_glfm_Glfm_glfmGetResRoot(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    Instance *jstr = createJavaString(runtime, glfmGetResRoot());
    env->push_ref(runtime->stack, jstr);
    return 0;
}

int org_mini_glfm_Glfm_glfmGetSaveRoot(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    Instance *jstr = createJavaString(runtime, glfmGetSaveRoot());
    env->push_ref(runtime->stack, jstr);
    return 0;
}

int org_mini_glfm_Glfm_glfmGetClipBoardContent(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    Instance *jstr = createJavaString(runtime, getClipBoardContent());
    env->push_ref(runtime->stack, jstr);
    return 0;
}

int org_mini_glfm_Glfm_glfmSetClipBoardContent(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    Instance *jstr = env->localvar_getRefer(runtime->localvar, 0);
    Utf8String *ustr = utf8_create();
    env->jstring_2_utf8(jstr, ustr, runtime);
    setClipBoardContent(utf8_cstr(ustr));
    utf8_destory(ustr);
    return 0;
}

int org_mini_glfm_Glfm_glfmPickPhotoAlbum(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 uid = env->localvar_getInt(runtime->localvar, pos++);
    s32 type = env->localvar_getInt(runtime->localvar, pos++);
    pickPhotoAlbum(window, uid, type);
    return 0;
}

int org_mini_glfm_Glfm_glfmPickPhotoCamera(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 uid = env->localvar_getInt(runtime->localvar, pos++);
    s32 type = env->localvar_getInt(runtime->localvar, pos++);
    pickPhotoCamera(window, uid, type);
    return 0;
}

int org_mini_glfm_Glfm_glfmImageCrop(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 uid = env->localvar_getInt(runtime->localvar, pos++);
    Instance *jstr = env->localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr = utf8_create();
    env->jstring_2_utf8(jstr, ustr, runtime);
    s32 x = env->localvar_getInt(runtime->localvar, pos++);
    s32 y = env->localvar_getInt(runtime->localvar, pos++);
    s32 w = env->localvar_getInt(runtime->localvar, pos++);
    s32 h = env->localvar_getInt(runtime->localvar, pos++);
    imageCrop(window, uid, utf8_cstr(ustr), x, y, w, h);
    utf8_destory(ustr);
    return 0;
}


int org_mini_glfm_Glfm_glfmPlayVideo(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *jstr = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *jstrMime = env->localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr = env->utf8_create();
    Utf8String *ustrMime = env->utf8_create();
    env->jstring_2_utf8(jstr, ustr, runtime);
    env->jstring_2_utf8(jstrMime, ustrMime, runtime);
    void *panel = playVideo(window, env->utf8_cstr(ustr), env->utf8_cstr(ustrMime));
    env->utf8_destory(ustr);
    env->utf8_destory(ustrMime);
    env->push_long(runtime->stack, (s64) (intptr_t) panel);
    return 0;
}

int org_mini_glfm_Glfm_glfmStartVideo(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    void *panel = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    startVideo(window, panel);
    return 0;
}

int org_mini_glfm_Glfm_glfmPauseVideo(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    void *panel = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    pauseVideo(window, panel);
    return 0;
}

int org_mini_glfm_Glfm_glfmStopVideo(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    void *panel = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    stopVideo(window, panel);
    return 0;
}

/* ==============================   jni utils =================================*/

int org_mini_glfm_utils_Gutil_f2b(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *farr = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *barr = env->localvar_getRefer(runtime->localvar, pos++);
    if (farr->arr_length == barr->arr_length * 4) {
        memcpy(barr->arr_body, farr->arr_body, barr->arr_length);
    }
    env->push_ref(runtime->stack, barr);
    return 0;
}

void vec_add(Instance *ra, Instance *aa, Instance *ba) {
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    int i;
    for (i = 0; i < ra->arr_length; ++i)
        r[i] = a[i] + b[i];
}

int org_mini_glfm_utils_Gutil_vec_add(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    vec_add(ra, aa, ba);
    env->push_ref(runtime->stack, ra);
    return 0;
}

void vec_sub(Instance *ra, Instance *aa, Instance *ba) {
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    int i;
    for (i = 0; i < ra->arr_length; ++i)
        r[i] = a[i] - b[i];
}

int org_mini_glfm_utils_Gutil_vec_sub(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    vec_sub(ra, aa, ba);
    env->push_ref(runtime->stack, ra);
    return 0;
}

float vec_mul_inner(Instance *aa, Instance *ba) {
    int i;
    float r = 0;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    for (i = 0; i < aa->arr_length; ++i)
        r += a[i] * b[i];
    return r;
}

int org_mini_glfm_utils_Gutil_vec_mul_inner(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    float r = vec_mul_inner(aa, ba);
    env->push_float(runtime->stack, r);
    return 0;
}

void vec_scale(Instance *ra, Instance *aa, float f) {
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    int i;
    for (i = 0; i < ra->arr_length; ++i)
        r[i] = a[i] * f;
}

int org_mini_glfm_utils_Gutil_vec_scale(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    vec_scale(ra, aa, f.f);
    env->push_ref(runtime->stack, ra);
    return 0;
}

float vec_len(Instance *ra) {
    return (float) sqrt(vec_mul_inner(ra, ra));
}

int org_mini_glfm_utils_Gutil_vec_len(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    float f = vec_len(ra);
    env->push_float(runtime->stack, f);
    return 0;
}

int org_mini_glfm_utils_Gutil_vec_normal(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    float k = 1.f / vec_len(aa);
    vec_scale(ra, aa, k);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfm_utils_Gutil_vec_reflect(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    float p = 2.f * vec_mul_inner(aa, ba);
    int i;
    for (i = 0; i < 4; ++i)
        r[i] = a[i] - p * b[i];
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfw_utils_Gutil_vec4_slerp(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float i2f;
    i2f.i = env->localvar_getInt(runtime->localvar, pos++);
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    vec4_slerp(r, a, b, i2f.f);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfw_utils_Gutil_vec4_from_mat4x4(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    quat_from_mat4x4(r, a);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfm_utils_Gutil_vec_mul_cross(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    r[0] = a[1] * b[2] - a[2] * b[1];
    r[1] = a[2] * b[0] - a[0] * b[2];
    r[2] = a[0] * b[1] - a[1] * b[0];
    if (ra->arr_length > 3)r[3] = 1.f;
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_identity(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_identity((vec4 *) r->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_dup(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_dup((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_row(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    int row = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_row((GLfloat *) r->arr_body, (vec4 *) m1->arr_body, row);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_col(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    int col = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_col((GLfloat *) r->arr_body, (vec4 *) m1->arr_body, col);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_transpose(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_transpose((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_add(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_add((vec4 *) r->arr_body, (vec4 *) m1->arr_body, (vec4 *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_sub(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_sub((vec4 *) r->arr_body, (vec4 *) m1->arr_body, (vec4 *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_mul(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_mul((vec4 *) r->arr_body, (vec4 *) m1->arr_body, (vec4 *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_mul_vec4(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_mul_vec4((GLfloat *) r->arr_body, (vec4 *) m1->arr_body, (GLfloat *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_from_vec3_mul_outer(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_from_vec3_mul_outer((vec4 *) r->arr_body, (GLfloat *) m1->arr_body,
                               (GLfloat *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_translate(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_translate((vec4 *) r->arr_body, x.f, y.f, z.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_translate_in_place(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_translate_in_place((vec4 *) r->arr_body, x.f, y.f, z.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_scale(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_scale((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_scale_aniso(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_scale_aniso((vec4 *) r->arr_body, (vec4 *) m1->arr_body, x.f, y.f, z.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_rotate(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z, a;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    a.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate((vec4 *) r->arr_body, (vec4 *) m1->arr_body, x.f, y.f, z.f, a.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_rotateX(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate_X((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_rotateY(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate_Y((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_rotateZ(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate_Z((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_invert(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_invert((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_orthonormalize(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_orthonormalize((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_ortho(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float l, r, b, t, n, f;
    l.i = env->localvar_getInt(runtime->localvar, pos++);
    r.i = env->localvar_getInt(runtime->localvar, pos++);
    b.i = env->localvar_getInt(runtime->localvar, pos++);
    t.i = env->localvar_getInt(runtime->localvar, pos++);
    n.i = env->localvar_getInt(runtime->localvar, pos++);
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_ortho((vec4 *) ra->arr_body, l.f, r.f, b.f, t.f, n.f, f.f);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_frustum(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float l, r, b, t, n, f;
    l.i = env->localvar_getInt(runtime->localvar, pos++);
    r.i = env->localvar_getInt(runtime->localvar, pos++);
    b.i = env->localvar_getInt(runtime->localvar, pos++);
    t.i = env->localvar_getInt(runtime->localvar, pos++);
    n.i = env->localvar_getInt(runtime->localvar, pos++);
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_frustum((vec4 *) ra->arr_body, l.f, r.f, b.f, t.f, n.f, f.f);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_perspective(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float y_fov, aspect, n, f;
    y_fov.i = env->localvar_getInt(runtime->localvar, pos++);
    aspect.i = env->localvar_getInt(runtime->localvar, pos++);
    n.i = env->localvar_getInt(runtime->localvar, pos++);
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_perspective((vec4 *) r->arr_body, y_fov.f, aspect.f, n.f, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_look_at(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_eye = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_center = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_up = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_look_at((vec4 *) r->arr_body, (float *) vec3_eye->arr_body,
                   (float *) vec3_center->arr_body,
                   (float *) vec3_up->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfm_utils_Gutil_mat4x4_trans_rotate_scale(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_trans = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec4_rotate = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_scale = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_trans_rotate_scale((vec4 *) r->arr_body, (float *) vec3_trans->arr_body,
                              (float *) vec4_rotate->arr_body,
                              (float *) vec3_scale->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}


s32 org_mini_glfm_utils_Gutil_img_fill(Runtime *runtime, JClass *clazz) {
    static const s32 BYTES_PER_PIXEL = 4;
    s32 pos = 0;
    Instance *canvasArr = localvar_getRefer(runtime->localvar, pos++);
    if (!canvasArr)return 0;
    u8 *canvas = (u8 *) canvasArr->arr_body;
    s32 ioffset = localvar_getInt(runtime->localvar, pos++);
    s32 offset = ioffset * BYTES_PER_PIXEL;
    s32 ilen = localvar_getInt(runtime->localvar, pos++);
    s32 len = ilen * BYTES_PER_PIXEL;
    Int2Float argb;//argb.c3--a  argb.c2--b   argb.c1--g  argb.c0--r
    argb.i = localvar_getInt(runtime->localvar, pos++);

    if (canvasArr->arr_length < offset || len == 0 || argb.c3 == 0) {
        //
    } else {
        if (offset + len > canvasArr->arr_length)len = canvasArr->arr_length - offset;

        u8 a = argb.c3;
        u8 b = argb.c2;
        u8 g = argb.c1;
        u8 r = argb.c0;
        //
        s32 i, imax;
        if (a == 0) {
            //do nothing
        } else if (a == 0xff) {//alpha = 1.0
            Int2Float color;
            color.c3 = a;
            color.c2 = r;
            color.c1 = g;
            color.c0 = b;
            s32 *icanvas = (s32 *) canvas;
            for (i = ioffset, imax = ioffset + ilen; i < imax; i++)icanvas[i] = color.i;
        } else {
            f32 falpha = ((f32) a) / 0xff;
            for (i = offset, imax = offset + len; i < imax; i += BYTES_PER_PIXEL) {
                canvas[i + 0] = b * falpha + (1.0f - falpha) * canvas[i + 0];
                canvas[i + 1] = g * falpha + (1.0f - falpha) * canvas[i + 1];
                canvas[i + 2] = r * falpha + (1.0f - falpha) * canvas[i + 2];
            }
        }
    }
    return 0;
}

typedef struct Point2d {
    f32 x;
    f32 y;
} Point2d;

typedef struct Bound2d {
    s32 x;
    s32 y;
    s32 w;
    s32 h;
} Bound2d;

typedef struct Box2d {
    s32 x1;
    s32 y1;
    s32 x2;
    s32 y2;
} Box2d;

s32 org_mini_glfm_utils_Gutil_img_draw(Runtime *runtime, JClass *clazz) {
    static const s32 CELL_BYTES = 4;
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    Instance *canvasArr = localvar_getRefer(runtime->localvar, pos++);
    s32 canvasWidth = localvar_getInt(runtime->localvar, pos++);
    Instance *imgArr = localvar_getRefer(runtime->localvar, pos++);
    s32 imgWidth = localvar_getInt(runtime->localvar, pos++);
    Bound2d clip;
    clip.x = localvar_getInt(runtime->localvar, pos++);
    clip.y = localvar_getInt(runtime->localvar, pos++);
    clip.w = localvar_getInt(runtime->localvar, pos++);
    clip.h = localvar_getInt(runtime->localvar, pos++);

    Int2Float i2f;
    i2f.i = localvar_getInt(runtime->localvar, pos++);
    f32 M00 = i2f.f;
    i2f.i = localvar_getInt(runtime->localvar, pos++);
    f32 M01 = i2f.f;
    i2f.i = localvar_getInt(runtime->localvar, pos++);
    f32 M02 = i2f.f;
    i2f.i = localvar_getInt(runtime->localvar, pos++);
    f32 M10 = i2f.f;
    i2f.i = localvar_getInt(runtime->localvar, pos++);
    f32 M11 = i2f.f;
    i2f.i = localvar_getInt(runtime->localvar, pos++);
    f32 M12 = i2f.f;

    i2f.i = localvar_getInt(runtime->localvar, pos++);
    f32 alpha = i2f.f;
    s32 isBitmapFontDraw = localvar_getInt(runtime->localvar, pos++);
    Int2Float fontRGB;
    fontRGB.i = localvar_getInt(runtime->localvar, pos++);

//    u8 a = fontRGB.c3;
//    u8 b = fontRGB.c2;
//    u8 g = fontRGB.c1;
//    u8 r = fontRGB.c0;

    s32 process = 0;
    if (!canvasArr || !imgArr || alpha == 0.f || canvasWidth == 0 || imgWidth == 0 || clip.w == 0 || clip.h == 0) {
        //do nothing
    } else {
        u8 *canvas = (u8 *) canvasArr->arr_body;
        u8 *img = (u8 *) imgArr->arr_body;
        s32 canvasHeight = canvasArr->arr_length / CELL_BYTES / canvasWidth;
        s32 imgHeight = imgArr->arr_length / CELL_BYTES / imgWidth;

        //fix clip in canvas range
        if (clip.x < 0)clip.x = 0;
        if (clip.y < 0)clip.y = 0;
        if (clip.x + clip.w > canvasWidth)clip.w = canvasWidth - clip.x;
        if (clip.y + clip.h > canvasHeight)clip.h = canvasHeight - clip.y;

        //effecient draw , only loop in clip rectange
        if (M00 == 1.0f && M11 == 1.0f && M01 == 0.0f && M10 == 0.0f) {//no scale , no rotate
            Bound2d translatedImg;
            translatedImg.x = M02;
            translatedImg.y = M12;
            translatedImg.w = imgWidth;
            translatedImg.h = imgHeight;

            //calc clip and image intersection
            Box2d intersection;
            intersection.x1 = clip.x > translatedImg.x ? clip.x : translatedImg.x;
            intersection.y1 = clip.y > translatedImg.y ? clip.y : translatedImg.y;
            intersection.x2 = clip.x + clip.w < translatedImg.x + translatedImg.w ? clip.x + clip.w : translatedImg.x + translatedImg.w;
            intersection.y2 = clip.y + clip.h < translatedImg.y + translatedImg.h ? clip.y + clip.h : translatedImg.y + translatedImg.h;

            if (intersection.x1 > canvasWidth || intersection.x2 < 0 || intersection.y1 > canvasHeight || intersection.y2 < 0) {
                //do nothing
                process=1;
            } else {
                //calc area to draw in image
                Box2d imgArea;
                imgArea.x1 = intersection.x1 - M02;
                imgArea.x2 = intersection.x2 - M02;
                imgArea.y1 = intersection.y1 - M12;
                imgArea.y2 = intersection.y2 - M12;

                s32 imgRowBytes = imgWidth * CELL_BYTES;
                s32 cvsRowBytes = canvasWidth * CELL_BYTES;

                s32 imgy, canvasy;
                for (imgy = imgArea.y1, canvasy = intersection.y1; imgy < imgArea.y2; imgy++, canvasy++) {
                    s32 imgRowByteStart = imgy * imgRowBytes;
                    s32 cvsRowByteStart = canvasy * cvsRowBytes;
                    s32 imgx, canvasx;
                    for (imgx = imgArea.x1, canvasx = intersection.x1; imgx < imgArea.x2; imgx++, canvasx++) {
                        s32 imgColByteStart = imgRowByteStart + imgx * CELL_BYTES;
                        u8 b, g, r, a;
                        a = img[imgColByteStart + 3];
                        if (a == 0) {
                            continue;
                        }

                        if (isBitmapFontDraw) {
                            b = fontRGB.c2;
                            g = fontRGB.c1;
                            r = fontRGB.c0;
                        } else {
                            b = img[imgColByteStart + 0];
                            g = img[imgColByteStart + 1];
                            r = img[imgColByteStart + 2];
                        }
                        if (a == 0xff) {
                            s32 cvsColByteStart = cvsRowByteStart + canvasx * CELL_BYTES;
                            canvas[cvsColByteStart + 0] = b;
                            canvas[cvsColByteStart + 1] = g;
                            canvas[cvsColByteStart + 2] = r;
                            canvas[cvsColByteStart + 3] = a;
//                            *((s32 *) (canvas + cvsColByteStart)) = *((s32 *) (img + imgColByteStart));
                        } else {
                            f32 falpha = (f32) a / 0xff;
                            s32 cvsColByteStart = cvsRowByteStart + canvasx * CELL_BYTES;
                            canvas[cvsColByteStart + 0] = b * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 0];
                            canvas[cvsColByteStart + 1] = g * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 1];
                            canvas[cvsColByteStart + 2] = r * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 2];
                            canvas[cvsColByteStart + 3] = a * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 3];
                        }
                    }
                }
                process = 1;
            }
        }

        //translate , rotate , scale draw, loop full image
        if (!process) {
            u8 *canvas = (u8 *) canvasArr->arr_body;

            s32 imgRowBytes = imgWidth * CELL_BYTES;
            s32 cvsRowBytes = canvasWidth * CELL_BYTES;
            s32 imgy, imgx;
            for (imgy = 0; imgy < imgHeight; imgy++) {
                s32 imgRowByteStart = imgy * imgRowBytes;
                for (imgx = 0; imgx < imgWidth; imgx++) {
                    s32 dx = round(imgx * M00 + imgy * M01 + M02);
                    s32 dy = round(imgx * M10 + imgy * M11 + M12);
                    if (dx >= clip.x && dx < clip.x + clip.w && dy >= clip.y && dy < clip.y + clip.h) {
                        s32 imgColByteStart = imgRowByteStart + imgx * CELL_BYTES;
                        u8 a = img[imgColByteStart + 3];
                        if (a == 0) {
                            continue;
                        }
                        u8 b = isBitmapFontDraw ? fontRGB.c2 : img[imgColByteStart + 0];
                        u8 g = isBitmapFontDraw ? fontRGB.c1 : img[imgColByteStart + 1];
                        u8 r = isBitmapFontDraw ? fontRGB.c0 : img[imgColByteStart + 2];
                        s32 cvsColByteStart = dy * cvsRowBytes + dx * CELL_BYTES;
                        if (a == 0xff) {
                            canvas[cvsColByteStart + 0] = b;
                            canvas[cvsColByteStart + 1] = g;
                            canvas[cvsColByteStart + 2] = r;
                            canvas[cvsColByteStart + 3] = a;
                        } else {
                            f32 falpha = (f32) a / 0xff;
                            canvas[cvsColByteStart + 0] = b * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 0];
                            canvas[cvsColByteStart + 1] = g * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 1];
                            canvas[cvsColByteStart + 2] = r * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 2];
                            canvas[cvsColByteStart + 3] = a * falpha + (1.0f - falpha) * canvas[cvsColByteStart + 3];
                        }
                    }
                }
            }
            process = 1;
        }
    }
    if (process) {
        env->push_int(runtime->stack, 0);// success
    } else {
        env->push_int(runtime->stack, 1);// failer
    }
    return 0;
}


static java_native_method method_glfm_table[] = {
        {"org/mini/gl/GLMath", "f2b",                                  "([F[B)[B",                                 org_mini_glfm_utils_Gutil_f2b},
        {"org/mini/gl/GLMath", "vec_add",                              "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_add},
        {"org/mini/gl/GLMath", "vec_sub",                              "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_sub},
        {"org/mini/gl/GLMath", "vec_scale",                            "([F[FF)[F",                                org_mini_glfm_utils_Gutil_vec_scale},
        {"org/mini/gl/GLMath", "vec_mul_inner",                        "([F[F)[F",                                 org_mini_glfm_utils_Gutil_vec_mul_inner},
        {"org/mini/gl/GLMath", "vec_len",                              "([F)F",                                    org_mini_glfm_utils_Gutil_vec_len},
        {"org/mini/gl/GLMath", "vec_normal",                           "([F[F)[F",                                 org_mini_glfm_utils_Gutil_vec_normal},
        {"org/mini/gl/GLMath", "vec_mul_cross",                        "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_mul_cross},
        {"org/mini/gl/GLMath", "vec_reflect",                          "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_reflect},
        {"org/mini/gl/GLMath", "vec4_slerp",                           "([F[F[FF)[F",                              org_mini_glfw_utils_Gutil_vec4_slerp},
        {"org/mini/gl/GLMath", "vec4_from_mat4x4",                     "([F[F[FF)[F",                              org_mini_glfw_utils_Gutil_vec4_from_mat4x4},
        {"org/mini/gl/GLMath", "mat4x4_identity",                      "([F)[F",                                   org_mini_glfm_utils_Gutil_mat4x4_identity},
        {"org/mini/gl/GLMath", "mat4x4_dup",                           "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_dup},
        {"org/mini/gl/GLMath", "mat4x4_row",                           "([F[FI)[F",                                org_mini_glfm_utils_Gutil_mat4x4_row},
        {"org/mini/gl/GLMath", "mat4x4_col",                           "([F[FI)[F",                                org_mini_glfm_utils_Gutil_mat4x4_col},
        {"org/mini/gl/GLMath", "mat4x4_transpose",                     "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_transpose},
        {"org/mini/gl/GLMath", "mat4x4_add",                           "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_add},
        {"org/mini/gl/GLMath", "mat4x4_sub",                           "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_sub},
        {"org/mini/gl/GLMath", "mat4x4_mul",                           "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_mul},
        {"org/mini/gl/GLMath", "mat4x4_mul_vec4",                      "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_mul_vec4},
        {"org/mini/gl/GLMath", "mat4x4_from_vec3_mul_outer",           "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_from_vec3_mul_outer},
        {"org/mini/gl/GLMath", "mat4x4_translate",                     "([FFFF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_translate},
        {"org/mini/gl/GLMath", "mat4x4_translate_in_place",            "([FFFF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_translate_in_place},
        {"org/mini/gl/GLMath", "mat4x4_scale",                         "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_scale},
        {"org/mini/gl/GLMath", "mat4x4_scale_aniso",                   "([F[FFFF)[F",                              org_mini_glfm_utils_Gutil_mat4x4_scale_aniso},
        {"org/mini/gl/GLMath", "mat4x4_rotate",                        "([F[FFFFF)[F",                             org_mini_glfm_utils_Gutil_mat4x4_rotate},
        {"org/mini/gl/GLMath", "mat4x4_rotateX",                       "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_rotateX},
        {"org/mini/gl/GLMath", "mat4x4_rotateY",                       "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_rotateY},
        {"org/mini/gl/GLMath", "mat4x4_rotateZ",                       "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_rotateZ},
        {"org/mini/gl/GLMath", "mat4x4_invert",                        "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_invert},
        {"org/mini/gl/GLMath", "mat4x4_orthonormalize",                "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_orthonormalize},
        {"org/mini/gl/GLMath", "mat4x4_ortho",                         "([FFFFFFF)[F",                             org_mini_glfm_utils_Gutil_mat4x4_ortho},
        {"org/mini/gl/GLMath", "mat4x4_frustum",                       "([FFFFFFF)[F",                             org_mini_glfm_utils_Gutil_mat4x4_frustum},
        {"org/mini/gl/GLMath", "mat4x4_perspective",                   "([FFFFF)[F",                               org_mini_glfm_utils_Gutil_mat4x4_perspective},
        {"org/mini/gl/GLMath", "mat4x4_look_at",                       "([F[F[F[F)[F",                             org_mini_glfm_utils_Gutil_mat4x4_look_at},
        {"org/mini/gl/GLMath", "mat4x4_trans_rotate_scale",            "([F[F[F[F)[F",                             org_mini_glfm_utils_Gutil_mat4x4_trans_rotate_scale},
        {"org/mini/gl/GLMath", "img_fill",                             "([BIII)V",                                 org_mini_glfm_utils_Gutil_img_fill},
        {"org/mini/gl/GLMath", "img_draw",                             "([BI[BIIIIIFFFFFFFZI)I",                   org_mini_glfm_utils_Gutil_img_draw},
        {"org/mini/glfm/Glfm", "glfmSetCallBack",                      "(JLorg/mini/glfm/GlfmCallBack;)V",         org_mini_glfm_Glfm_glfmSetCallBack},
        {"org/mini/glfm/Glfm", "glfmSetDisplayConfig",                 "(JIIIII)V",                                org_mini_glfm_Glfm_glfmSetDisplayConfig},
        {"org/mini/glfm/Glfm", "glfmSetSupportedInterfaceOrientation", "(JI)V",                                    org_mini_glfm_Glfm_glfmSetSupportedInterfaceOrientation},
        {"org/mini/glfm/Glfm", "glfmGetInterfaceOrientation",          "(J)I",                                     org_mini_glfm_Glfm_glfmGetInterfaceOrientation},
        {"org/mini/glfm/Glfm", "glfmGetSupportedInterfaceOrientation", "(J)I",                                     org_mini_glfm_Glfm_glfmGetSupportedInterfaceOrientation},
        {"org/mini/glfm/Glfm", "glfmSetMultitouchEnabled",             "(JZ)V",                                    org_mini_glfm_Glfm_glfmSetMultitouchEnabled},
        {"org/mini/glfm/Glfm", "glfmGetMultitouchEnabled",             "(J)Z",                                     org_mini_glfm_Glfm_glfmGetMultitouchEnabled},
        {"org/mini/glfm/Glfm", "glfmGetDisplayWidth",                  "(J)I",                                     org_mini_glfm_Glfm_glfmGetDisplayWidth},
        {"org/mini/glfm/Glfm", "glfmGetDisplayHeight",                 "(J)I",                                     org_mini_glfm_Glfm_glfmGetDisplayHeight},
        {"org/mini/glfm/Glfm", "glfmGetDisplayScale",                  "(J)D",                                     org_mini_glfm_Glfm_glfmGetDisplayScale},
        {"org/mini/glfm/Glfm", "glfmGetDisplayChromeInsets",           "(J[D)V",                                   org_mini_glfm_Glfm_glfmGetDisplayChromeInsets},
        {"org/mini/glfm/Glfm", "glfmGetDisplayChrome",                 "(J)I",                                     org_mini_glfm_Glfm_glfmGetDisplayChrome},
        {"org/mini/glfm/Glfm", "glfmSetDisplayChrome",                 "(JI)V",                                    org_mini_glfm_Glfm_glfmSetDisplayChrome},
        {"org/mini/glfm/Glfm", "glfmGetRenderingAPI",                  "(J)I",                                     org_mini_glfm_Glfm_glfmGetRenderingAPI},
        {"org/mini/glfm/Glfm", "glfmHasTouch",                         "(J)Z",                                     org_mini_glfm_Glfm_glfmHasTouch},
        {"org/mini/glfm/Glfm", "glfmSetMouseCursor",                   "(JI)V",                                    org_mini_glfm_Glfm_glfmSetMouseCursor},
        {"org/mini/glfm/Glfm", "glfmExtensionSupported",               "(Ljava/lang/String;)Z",                    org_mini_glfm_Glfm_glfmExtensionSupported},
        {"org/mini/glfm/Glfm", "glfmSetKeyboardVisible",               "(JZ)V",                                    org_mini_glfm_Glfm_glfmSetKeyboardVisible},
        {"org/mini/glfm/Glfm", "glfmIsKeyboardVisible",                "(J)Z",                                     org_mini_glfm_Glfm_glfmIsKeyboardVisible},
        {"org/mini/glfm/Glfm", "glfmGetResRoot",                       "()Ljava/lang/String;",                     org_mini_glfm_Glfm_glfmGetResRoot},
        {"org/mini/glfm/Glfm", "glfmGetSaveRoot",                      "()Ljava/lang/String;",                     org_mini_glfm_Glfm_glfmGetSaveRoot},
        {"org/mini/glfm/Glfm", "glfmGetClipBoardContent",              "()Ljava/lang/String;",                     org_mini_glfm_Glfm_glfmGetClipBoardContent},
        {"org/mini/glfm/Glfm", "glfmSetClipBoardContent",              "(Ljava/lang/String;)V",                    org_mini_glfm_Glfm_glfmSetClipBoardContent},
        {"org/mini/glfm/Glfm", "glfmPickPhotoAlbum",                   "(JII)V",                                   org_mini_glfm_Glfm_glfmPickPhotoAlbum},
        {"org/mini/glfm/Glfm", "glfmPickPhotoCamera",                  "(JII)V",                                   org_mini_glfm_Glfm_glfmPickPhotoCamera},
        {"org/mini/glfm/Glfm", "glfmImageCrop",                        "(JILjava/lang/String;IIII)V",              org_mini_glfm_Glfm_glfmImageCrop},
        {"org/mini/glfm/Glfm", "glfmPlayVideo",                        "(JLjava/lang/String;Ljava/lang/String;)J", org_mini_glfm_Glfm_glfmPlayVideo},
        {"org/mini/glfm/Glfm", "glfmPauseVideo",                       "(JJ)V",                                    org_mini_glfm_Glfm_glfmPauseVideo},
        {"org/mini/glfm/Glfm", "glfmStopVideo",                        "(JJ)V",                                    org_mini_glfm_Glfm_glfmStopVideo},
        {"org/mini/glfm/Glfm", "glfmStartVideo",                       "(JJ)V",                                    org_mini_glfm_Glfm_glfmStartVideo},

};

s32 count_GlfmFuncTable() {
    return sizeof(method_glfm_table) / sizeof(java_native_method);
}

__refer ptr_GlfmFuncTable() {
    return &method_glfm_table[0];
}
