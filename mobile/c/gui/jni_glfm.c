/* nuklear - 1.32.0 - public domain */
#define WIN32_LEAN_AND_MEAN

#include <stdio.h>
#include <string.h>

#include "glfm.h"
#include "linmath.h"

//#define STB_IMAGE_IMPLEMENTATION

//#include "deps/include/stb_image.h"

#include "jvm.h"
#include "jni_gui.h"

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
    fprintf(stderr, "GLFW Error: %s\n", description);
}

static void _callback_surface_error(GLFMDisplay *window, const char *description) {
    if (refers._callback_surface_error) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)
                       window);
        Instance *jstr = createJavaString(runtime, description);
        env->push_ref(runtime->stack, jstr);
        
        s32 ret = env->execute_method(refers._callback_surface_error, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

static bool _callback_key(GLFMDisplay *window, GLFMKey key, GLFMKeyAction action, int mods) {
    if (refers._callback_key) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)
                       window);
        env->push_int(runtime->stack, key);
        env->push_int(runtime->stack, action);
        env->push_int(runtime->stack, mods);
        
        s32 ret = env->execute_method(refers._callback_key, runtime,
                                      refers.glfm_callback->mb.clazz);
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
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        Instance *ins = createJavaString(runtime, utf8);
        env->push_ref(runtime->stack, ins);
        env->push_int(runtime->stack, modifiers);
        
        s32 ret = env->execute_method(refers._callback_character, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

static void _callback_mainloop(GLFMDisplay *window, f64 frameTime) {
    if (refers._callback_mainloop) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        env->push_double(runtime->stack, frameTime);
        
        s32 ret = env->execute_method(refers._callback_mainloop, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_memory_warning(GLFMDisplay *window) {
    if (refers._callback_memory_warning) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        s32 ret = env->execute_method(refers._callback_memory_warning, runtime, refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_keyboard_visible(GLFMDisplay *window, bool visible, f64 x, f64 y, f64 w, f64 h) {
    if (refers._callback_keyboard_visible) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        env->push_int(runtime->stack, visible);
        env->push_double(runtime->stack, x);
        env->push_double(runtime->stack, y);
        env->push_double(runtime->stack, w);
        env->push_double(runtime->stack, h);
        
        s32 ret = env->execute_method(refers._callback_keyboard_visible, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

bool _callback_touch(GLFMDisplay *window, s32 touch, GLFMTouchPhase phase, f64 x, f64 y) {
    if (refers._callback_touch) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        env->push_int(runtime->stack, touch);
        env->push_int(runtime->stack, phase);
        env->push_double(runtime->stack, x);
        env->push_double(runtime->stack, y);
        
        s32 ret = env->execute_method(refers._callback_touch, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        } else {
            return env->pop_int(runtime->stack);
        }
    }
    return 0;
}


void _callback_surface_resized(GLFMDisplay *window, s32 w, s32 h) {
    if (refers._callback_surface_resized) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        env->push_int(runtime->stack, w);
        env->push_int(runtime->stack, h);
        
        s32 ret = env->execute_method(refers._callback_surface_resized, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_surface_destroyed(GLFMDisplay *window) {
    if (refers._callback_surface_destroyed) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        
        s32 ret = env->execute_method(refers._callback_surface_destroyed, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_app_focus(GLFMDisplay *window, bool focus) {
    if (refers._callback_app_focus) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        env->push_int(runtime->stack, focus);
        
        s32 ret = env->execute_method(refers._callback_app_focus, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

void _callback_surface_created(GLFMDisplay *window, s32 w, s32 h) {
    gladLoadGLES2Loader(glfmGetProcAddress);
    if (refers._callback_surface_created) {
        Runtime *runtime = getRuntimeCurThread();
        JniEnv *env = refers.env;
        env->push_ref(runtime->stack, refers.glfm_callback);
        env->push_long(runtime->stack, (s64)(intptr_t)        window);
        env->push_int(runtime->stack, w);
        env->push_int(runtime->stack, h);
        
        s32 ret = env->execute_method(refers._callback_surface_created, runtime,
                                      refers.glfm_callback->mb.clazz);
        if (ret) {
            env->print_exception(runtime);
        }
    }
}

/* ==============================   jni glfm =================================*/

int org_mini_glfm_Glfm_glfmSetUserData(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)
    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    refers.glfm_callback = env->localvar_getRefer(runtime->localvar, pos++);
    
    //this object not refered by jvm , so needs to hold by jni manaul
    if (refers.glfm_callback) env->instance_release_from_thread(refers.glfm_callback, runtime);
    //env->instance_hold_to_thread(refers.glfm_callback, runtime);
    env->garbage_refer_hold(refers.glfm_callback);
    
    
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
    
    
    c8 *name_s, *type_s;
    {
        name_s = "onKey";
        type_s = "(JIII)Z";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_key =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onCharacter";
        type_s = "(JLjava/lang/String;I)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_character =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onKeyboardVisible";
        type_s = "(JZDDDD)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_keyboard_visible =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "mainLoop";
        type_s = "(JD)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_mainloop =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onMemWarning";
        type_s = "(J)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_memory_warning =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    
    {
        name_s = "onTouch";
        type_s = "(JIIDD)Z";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_touch =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        
        name_s = "onSurfaceError";
        type_s = "(JLjava/lang/String;)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_error =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    
    {
        name_s = "onSurfaceCreated";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_created =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    
    {
        name_s = "onSurfaceResize";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_resized =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "onSurfaceDestroyed";
        type_s = "(J)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_surface_destroyed =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    
    {
        name_s = "onAppFocus";
        type_s = "(JZ)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_app_focus =
        env->find_methodInfo_by_name(refers.glfm_callback->mb.clazz->name, name, type,
                                     runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    
    return 0;
}


int org_mini_glfm_Glfm_glfmSetDisplayConfig(Runtime *runtime, JClass *clazz) {//
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)
    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 preferredAPI = env->localvar_getInt(runtime->localvar, pos++);
    s32 colorFormat = env->localvar_getInt(runtime->localvar, pos++);
    s32 depthFormat = env->localvar_getInt(runtime->localvar, pos++);
    s32 stencilFormat = env->localvar_getInt(runtime->localvar, pos++);
    s32 multisample = env->localvar_getInt(runtime->localvar, pos++);
    
    glfmSetDisplayConfig(window, preferredAPI, colorFormat, depthFormat, stencilFormat, multisample);
    
    return 0;
}

int org_mini_glfm_Glfm_glfmSetUserInterfaceOrientation(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 allowedOrientations = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetUserInterfaceOrientation(window, allowedOrientations);
    return 0;
}


int org_mini_glfm_Glfm_glfmGetUserInterfaceOrientation(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)
    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_ref(runtime->stack, glfmGetUserInterfaceOrientation(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetMultitouchEnabled(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 multitouchEnabled = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetMultitouchEnabled(window, multitouchEnabled);
    return 0;
}


int org_mini_glfm_Glfm_glfmGetMultitouchEnabled(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)
    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmGetMultitouchEnabled(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmGetDisplayWidth(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfmGetDisplaySize(window, &w, &h);
    env->push_int(runtime->stack, w);
    return 0;
}

int org_mini_glfm_Glfm_glfmGetDisplayHeight(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfmGetDisplaySize(window, &w, &h);
    env->push_int(runtime->stack, h);
    return 0;
}

int org_mini_glfm_Glfm_glfmGetDisplayScale(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    env->push_double(runtime->stack, glfmGetDisplayScale(window));
    return 0;
}

int org_mini_glfm_Glfm_glfmGetDisplayChromeInsets(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
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
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    env->push_int(runtime->stack, glfmGetDisplayChrome(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetDisplayChrome(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 uiChrome = env->localvar_getInt(runtime->localvar, pos++);
    glfmSetDisplayChrome(window, uiChrome);
    return 0;
}


int org_mini_glfm_Glfm_glfmGetRenderingAPI(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmGetRenderingAPI(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmHasTouch(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    env->push_int(runtime->stack, glfmHasTouch(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetMouseCursor(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
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
    env->jstring_2_utf8(ext, ustr);
    env->push_int(runtime->stack, glfmExtensionSupported(env->utf8_cstr(ustr)));
    env->utf8_destory(ustr);
    return 0;
}


int org_mini_glfm_Glfm_glfmIsKeyboardVisible(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
    env->push_int(runtime->stack, glfmIsKeyboardVisible(window));
    return 0;
}


int org_mini_glfm_Glfm_glfmSetKeyboardVisible(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFMDisplay *window = (__refer)(intptr_t)    env->localvar_getLong_2slot(runtime->localvar, pos);
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
    env->jstring_2_utf8(jstr, ustr);
    setClipBoardContent(utf8_cstr(ustr));
    utf8_destory(ustr);
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
    for (i = 0; i < aa->arr_length; ++i)
        r += aa->arr_body[i] * ba->arr_body[i];
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


static java_native_method method_glfm_table[] = {
    {"org/mini/glfm/utils/Gutil", "f2b",                             "([F[B)[B",                         org_mini_glfm_utils_Gutil_f2b},
    {"org/mini/glfm/utils/Gutil", "vec_add",                         "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_vec_add},
    {"org/mini/glfm/utils/Gutil", "vec_sub",                         "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_vec_sub},
    {"org/mini/glfm/utils/Gutil", "vec_scale",                       "([F[FF)[F",                        org_mini_glfm_utils_Gutil_vec_scale},
    {"org/mini/glfm/utils/Gutil", "vec_mul_inner",                   "([F[F)[F",                         org_mini_glfm_utils_Gutil_vec_mul_inner},
    {"org/mini/glfm/utils/Gutil", "vec_len",                         "([F)F",                            org_mini_glfm_utils_Gutil_vec_len},
    {"org/mini/glfm/utils/Gutil", "vec_normal",                      "([F[F)[F",                         org_mini_glfm_utils_Gutil_vec_normal},
    {"org/mini/glfm/utils/Gutil", "vec_mul_cross",                   "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_vec_mul_cross},
    {"org/mini/glfm/utils/Gutil", "vec_reflect",                     "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_vec_reflect},
    {"org/mini/glfm/utils/Gutil", "mat4x4_identity",                 "([F)[F",                           org_mini_glfm_utils_Gutil_mat4x4_identity},
    {"org/mini/glfm/utils/Gutil", "mat4x4_dup",                      "([F[F)[F",                         org_mini_glfm_utils_Gutil_mat4x4_dup},
    {"org/mini/glfm/utils/Gutil", "mat4x4_row",                      "([F[FI)[F",                        org_mini_glfm_utils_Gutil_mat4x4_row},
    {"org/mini/glfm/utils/Gutil", "mat4x4_col",                      "([F[FI)[F",                        org_mini_glfm_utils_Gutil_mat4x4_col},
    {"org/mini/glfm/utils/Gutil", "mat4x4_transpose",                "([F[F)[F",                         org_mini_glfm_utils_Gutil_mat4x4_transpose},
    {"org/mini/glfm/utils/Gutil", "mat4x4_add",                      "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_mat4x4_add},
    {"org/mini/glfm/utils/Gutil", "mat4x4_sub",                      "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_mat4x4_sub},
    {"org/mini/glfm/utils/Gutil", "mat4x4_mul",                      "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_mat4x4_mul},
    {"org/mini/glfm/utils/Gutil", "mat4x4_mul_vec4",                 "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_mat4x4_mul_vec4},
    {"org/mini/glfm/utils/Gutil", "mat4x4_from_vec3_mul_outer",      "([F[F[F)[F",                       org_mini_glfm_utils_Gutil_mat4x4_from_vec3_mul_outer},
    {"org/mini/glfm/utils/Gutil", "mat4x4_translate",                "([FFFF)[F",                        org_mini_glfm_utils_Gutil_mat4x4_translate},
    {"org/mini/glfm/utils/Gutil", "mat4x4_translate_in_place",       "([FFFF)[F",                        org_mini_glfm_utils_Gutil_mat4x4_translate_in_place},
    {"org/mini/glfm/utils/Gutil", "mat4x4_scale",                    "([F[FF)[F",                        org_mini_glfm_utils_Gutil_mat4x4_scale},
    {"org/mini/glfm/utils/Gutil", "mat4x4_scale_aniso",              "([F[FFFF)[F",                      org_mini_glfm_utils_Gutil_mat4x4_scale_aniso},
    {"org/mini/glfm/utils/Gutil", "mat4x4_rotate",                   "([F[FFFFF)[F",                     org_mini_glfm_utils_Gutil_mat4x4_rotate},
    {"org/mini/glfm/utils/Gutil", "mat4x4_rotateX",                  "([F[FF)[F",                        org_mini_glfm_utils_Gutil_mat4x4_rotateX},
    {"org/mini/glfm/utils/Gutil", "mat4x4_rotateY",                  "([F[FF)[F",                        org_mini_glfm_utils_Gutil_mat4x4_rotateY},
    {"org/mini/glfm/utils/Gutil", "mat4x4_rotateZ",                  "([F[FF)[F",                        org_mini_glfm_utils_Gutil_mat4x4_rotateZ},
    {"org/mini/glfm/utils/Gutil", "mat4x4_invert",                   "([F[F)[F",                         org_mini_glfm_utils_Gutil_mat4x4_invert},
    {"org/mini/glfm/utils/Gutil", "mat4x4_orthonormalize",           "([F[F)[F",                         org_mini_glfm_utils_Gutil_mat4x4_orthonormalize},
    {"org/mini/glfm/utils/Gutil", "mat4x4_ortho",                    "([FFFFFFF)[F",                     org_mini_glfm_utils_Gutil_mat4x4_ortho},
    {"org/mini/glfm/utils/Gutil", "mat4x4_frustum",                  "([FFFFFFF)[F",                     org_mini_glfm_utils_Gutil_mat4x4_frustum},
    {"org/mini/glfm/utils/Gutil", "mat4x4_perspective",              "([FFFFF)[F",                       org_mini_glfm_utils_Gutil_mat4x4_perspective},
    {"org/mini/glfm/utils/Gutil", "mat4x4_look_at",                  "([F[F[F[F)[F",                     org_mini_glfm_utils_Gutil_mat4x4_look_at},
    {"org/mini/glfm/Glfm",        "glfmSetCallBack",                 "(JLorg/mini/glfm/GlfmCallBack;)V", org_mini_glfm_Glfm_glfmSetUserData},
    {"org/mini/glfm/Glfm",        "glfmSetDisplayConfig",            "(JIIIII)V",                        org_mini_glfm_Glfm_glfmSetDisplayConfig},
    {"org/mini/glfm/Glfm",        "glfmSetUserInterfaceOrientation", "(JI)V",                            org_mini_glfm_Glfm_glfmSetUserInterfaceOrientation},
    {"org/mini/glfm/Glfm",        "glfmGetUserInterfaceOrientation", "(J)I",                             org_mini_glfm_Glfm_glfmGetUserInterfaceOrientation},
    {"org/mini/glfm/Glfm",        "glfmSetMultitouchEnabled",        "(JZ)V",                            org_mini_glfm_Glfm_glfmSetMultitouchEnabled},
    {"org/mini/glfm/Glfm",        "glfmGetMultitouchEnabled",        "(J)Z",                             org_mini_glfm_Glfm_glfmGetMultitouchEnabled},
    {"org/mini/glfm/Glfm",        "glfmGetDisplayWidth",             "(J)I",                             org_mini_glfm_Glfm_glfmGetDisplayWidth},
    {"org/mini/glfm/Glfm",        "glfmGetDisplayHeight",            "(J)I",                             org_mini_glfm_Glfm_glfmGetDisplayHeight},
    {"org/mini/glfm/Glfm",        "glfmGetDisplayScale",             "(J)D",                             org_mini_glfm_Glfm_glfmGetDisplayScale},
    {"org/mini/glfm/Glfm",        "glfmGetDisplayChromeInsets",      "(J[D)V",                           org_mini_glfm_Glfm_glfmGetDisplayChromeInsets},
    {"org/mini/glfm/Glfm",        "glfmGetDisplayChrome",            "(J)I",                             org_mini_glfm_Glfm_glfmGetDisplayChrome},
    {"org/mini/glfm/Glfm",        "glfmSetDisplayChrome",            "(JI)V",                            org_mini_glfm_Glfm_glfmSetDisplayChrome},
    {"org/mini/glfm/Glfm",        "glfmGetRenderingAPI",             "(J)I",                             org_mini_glfm_Glfm_glfmGetRenderingAPI},
    {"org/mini/glfm/Glfm",        "glfmHasTouch",                    "(J)Z",                             org_mini_glfm_Glfm_glfmHasTouch},
    {"org/mini/glfm/Glfm",        "glfmSetMouseCursor",              "(JI)V",                            org_mini_glfm_Glfm_glfmSetMouseCursor},
    {"org/mini/glfm/Glfm",        "glfmExtensionSupported",          "(Ljava/lang/String;)Z",            org_mini_glfm_Glfm_glfmExtensionSupported},
    {"org/mini/glfm/Glfm",        "glfmSetKeyboardVisible",          "(JZ)V",                            org_mini_glfm_Glfm_glfmSetKeyboardVisible},
    {"org/mini/glfm/Glfm",        "glfmIsKeyboardVisible",           "(J)Z",                             org_mini_glfm_Glfm_glfmIsKeyboardVisible},
    {"org/mini/glfm/Glfm",        "glfmGetResRoot",                  "()Ljava/lang/String;",             org_mini_glfm_Glfm_glfmGetResRoot},
    {"org/mini/glfm/Glfm",        "glfmGetSaveRoot",                 "()Ljava/lang/String;",             org_mini_glfm_Glfm_glfmGetSaveRoot},
    {"org/mini/glfm/Glfm",        "glfmGetClipBoardContent",         "()Ljava/lang/String;",             org_mini_glfm_Glfm_glfmGetClipBoardContent},
    {"org/mini/glfm/Glfm",        "glfmSetClipBoardContent",         "(Ljava/lang/String;)V",            org_mini_glfm_Glfm_glfmSetClipBoardContent},
    
};

s32 count_GlfmFuncTable() {
    return sizeof(method_glfm_table) / sizeof(java_native_method);
}

__refer ptr_GlfmFuncTable() {
    return &method_glfm_table[0];
}
