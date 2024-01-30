/* nuklear - 1.32.0 - public domain */
#define WIN32_LEAN_AND_MEAN

#include <stdio.h>
#include <string.h>
#include <garbage.h>

#include "glfm.h"
#include "linmath.h"


//#define STB_IMAGE_IMPLEMENTATION

//#include "deps/include/stb_image.h"

#include "jvm.h"
#include "media.h"

extern const char *glfmGetResRoot();

extern int gladLoadGLES2Loader(void *fun);

extern s32 jstring_2_utf8(struct java_lang_String *jstr, Utf8String *utf8);

GlobeRefer refers;

typedef float GLfloat;

/* ==============================   local tools  =================================*/


JObject *createJavaString(JThreadRuntime *runtime, c8 *cstr) {
    if (cstr == NULL) {
        return NULL;
    }
    Utf8String *ustr = utf8_create_part_c(cstr, 0, strlen(cstr));
    JObject *jstr = construct_string_with_ustr(runtime, ustr);
    utf8_destory(ustr);
    return jstr;
}

/* ==============================   jni callback =================================*/
static void _callback_error_before_init(int error, const char *description) {
    fprintf(stderr, "GLFM Error: %s\n", description);
}

static void _callback_surface_error(GLFMDisplay *window, const char *description) {
    if (refers._callback_surface_error) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        JObject *jstr = createJavaString(runtime, description);

        void (*func_ptr)(JThreadRuntime *runtime, JObject *, s64, struct java_lang_String *) =refers._callback_surface_error->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, (struct java_lang_String *) jstr);
        exception_check_print(runtime);
    }
}

static bool _callback_key(GLFMDisplay *window, GLFMKey key, GLFMKeyAction action, int mods) {
    if (refers._callback_key) {
        JThreadRuntime *runtime = getRuntimeCurThread();

        s32 (*func_ptr)(JThreadRuntime *runtime, struct org_mini_glfm_GlfmCallBack *p0, s64 p1, s32 p3, s32 p4, s32 p5) =refers._callback_key->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, key, action, mods);
        exception_check_print(runtime);
    }
    return false;
}

static void _callback_character(GLFMDisplay *window, const char *utf8, int modifiers) {
    if (refers._callback_character) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        JObject *ins = createJavaString(runtime, utf8);
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_glfm_GlfmCallBack *p0, s64 p1, struct java_lang_String *p3, s32 p4) =refers._callback_character->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, ins, modifiers);
        exception_check_print(runtime);
    }
}

static void _callback_render(GLFMDisplay *window) {
    if (refers._callback_render) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1) =refers._callback_render->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window);
        exception_check_print(runtime);
    }
}

void _callback_memory_warning(GLFMDisplay *window) {
    if (refers._callback_memory_warning) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_glfm_GlfmCallBack *p0, s64 p1) =refers._callback_memory_warning->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window);
        exception_check_print(runtime);
    }
}

void _callback_keyboard_visible(GLFMDisplay *window, bool visible, f64 x, f64 y, f64 w, f64 h) {
    if (refers._callback_keyboard_visible) {
        JThreadRuntime *runtime = getRuntimeCurThread();

        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1, s32 p3, f64 p4, f64 p6, f64 p8, f64 p10) =refers._callback_keyboard_visible->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, visible, x, y, w, h);
        exception_check_print(runtime);
    }
}

bool _callback_touch(GLFMDisplay *window, s32 touch, GLFMTouchPhase phase, f64 x, f64 y) {
    if (refers._callback_touch) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        s32 (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1, s32 p3, s32 p4, f64 p5, f64 p7) =refers._callback_touch->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, touch, phase, x, y);
        exception_check_print(runtime);
    }
    return 0;
}


void _callback_surface_resized(GLFMDisplay *window, s32 w, s32 h) {
    if (refers._callback_surface_resized) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_glfm_GlfmCallBack *p0, s64 p1, s32 p3, s32 p4) =refers._callback_surface_resized->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, w, h);
        exception_check_print(runtime);
    }
}

void _callback_surface_destroyed(GLFMDisplay *window) {
    if (refers._callback_surface_destroyed) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1) =refers._callback_surface_destroyed->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window);
        exception_check_print(runtime);
    }
}

void _callback_app_focus(GLFMDisplay *window, bool focus) {
    if (refers._callback_app_focus) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1, s32 p3) =refers._callback_app_focus->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, focus);
        exception_check_print(runtime);
    }
}

void _callback_surface_created(GLFMDisplay *window, s32 w, s32 h) {
    gladLoadGLES2Loader(glfmGetProcAddress);
    if (refers._callback_surface_created) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1, s32 p3, s32 p4) =refers._callback_surface_created->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, w, h);
        exception_check_print(runtime);
    }
}

void _callback_photo_picked(GLFMDisplay *window, s32 uid, const c8 *url, c8 *data, s32 length) {
    gladLoadGLES2Loader(glfmGetProcAddress);
    if (refers._callback_photo_picked) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        JObject *jstr_url = createJavaString(runtime, url);
        JArray *jarr = NULL;
        if (data) {
            jarr = multi_array_create_by_typename(runtime, &length, 1, "[B");
            memcpy(jarr->prop.as_c8_arr, data, length);
        }
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1, s32 p3, struct java_lang_String *p4, JArray *p5) =refers._callback_photo_picked->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, uid, jstr_url, jarr);
        exception_check_print(runtime);
    }
}

void _callback_notify(GLFMDisplay *window, const c8 *key, const c8 *val) {
    gladLoadGLES2Loader(glfmGetProcAddress);
    if (refers._callback_notify) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        __refer insKey = createJavaString(runtime, key);
        __refer insVal = createJavaString(runtime, val);
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1, struct java_lang_String *p3, struct java_lang_String *p4) =refers._callback_notify->raw->func_ptr;
        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, insKey, insVal);
        exception_check_print(runtime);
    }
}

void _callback_orientation_changed(GLFMDisplay *window, GLFMInterfaceOrientation orientation) {
    if (refers._callback_orientation_changed) {
        JThreadRuntime *runtime = getRuntimeCurThread();
        void (*func_ptr)(JThreadRuntime *runtime, struct org_mini_gui_GCallBack *p0, s64 p1, s32 p3) =refers._callback_orientation_changed->raw->func_ptr;

        func_ptr(runtime, refers.glfm_callback, (s64) (intptr_t) window, orientation);
        exception_check_print(runtime);
    }
}

/* ==============================   jni glfm =================================*/

void func_org_mini_glfm_Glfm_glfmSetCallBack__JLorg_mini_glfm_GlfmCallBack_2_V(JThreadRuntime *runtime, s64 p0, struct org_mini_glfm_GlfmCallBack *p2) {

    GLFMDisplay *window = (__refer) (intptr_t) p0;

    //this object not refered by jvm , so needs to hold by jni manaul
    if (refers.glfm_callback) instance_release_from_thread(refers.glfm_callback, runtime);
    refers.glfm_callback = p2;

    //instance_hold_to_thread(refers.glfm_callback, runtime);
    gc_refer_hold(refers.glfm_callback);


    glfmSetRenderFunc(window, _callback_render);
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


    c8 *name_s, *type_s;
    {
        name_s = "onKey";
        type_s = "(JIII)Z";
        refers._callback_key = find_methodInfo_by_name(utf8_cstr(refers.glfm_callback->prop.clazz->name),
                                                       name_s, type_s);
    }
    {
        name_s = "onCharacter";
        type_s = "(JLjava/lang/String;I)V";
        refers._callback_character = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }
    {
        name_s = "onKeyboardVisible";
        type_s = "(JZDDDD)V";
        refers._callback_keyboard_visible = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }
    {
        name_s = "onRender";//glfm changed :from mainloop to onRender
        type_s = "(J)V";
        refers._callback_render = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }
    {
        name_s = "onMemWarning";
        type_s = "(J)V";
        refers._callback_memory_warning = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }

    {
        name_s = "onTouch";
        type_s = "(JIIDD)Z";
        refers._callback_touch = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }
    {

        name_s = "onSurfaceError";
        type_s = "(JLjava/lang/String;)V";
        refers._callback_surface_error = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }

    {
        name_s = "onSurfaceCreated";
        type_s = "(JII)V";
        refers._callback_surface_created = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }

    {
        name_s = "onSurfaceResize";
        type_s = "(JII)V";
        refers._callback_surface_resized = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }
    {
        name_s = "onSurfaceDestroyed";
        type_s = "(J)V";
        refers._callback_surface_destroyed = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }

    {
        name_s = "onAppFocus";
        type_s = "(JZ)V";
        refers._callback_app_focus = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }

    {
        name_s = "onPhotoPicked";
        type_s = "(JILjava/lang/String;[B)V";
        refers._callback_photo_picked = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }

    {
        name_s = "onNotify";
        type_s = "(JLjava/lang/String;Ljava/lang/String;)V";
        refers._callback_notify = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }

    {
        name_s = "onOrientationChanged";
        type_s = "(JI)V";
        refers._callback_orientation_changed = find_methodInfo_by_name(
                utf8_cstr(refers.glfm_callback->prop.clazz->name), name_s, type_s);
    }
}


void func_org_mini_glfm_Glfm_glfmSetDisplayConfig__JIIIII_V(JThreadRuntime *runtime, s64 p0, s32 p2, s32 p3, s32 p4, s32 p5, s32 p6) {//
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    s32 preferredAPI = p2;
    s32 colorFormat = p3;
    s32 depthFormat = p4;
    s32 stencilFormat = p5;
    s32 multisample = p6;

    glfmSetDisplayConfig(window, preferredAPI, colorFormat, depthFormat, stencilFormat,
                         multisample);

}
//
//void func_org_mini_glfm_Glfm_glfmSetSupportedInterfaceOrientation__JI_V(JThreadRuntime *runtime, s64 p0, s32 p2) {
//    GLFMDisplay *window = (__refer) (intptr_t) p0;
//    s32 allowedOrientations = p2;
//    glfmSetUserInterfaceOrientation(window, allowedOrientations);
//}
//
//
//s32 func_org_mini_glfm_Glfm_glfmGetSupportedInterfaceOrientation__J_I(JThreadRuntime *runtime, s64 p0) {
//    GLFMDisplay *window = (__refer) (intptr_t) p0;
//    return glfmGetUserInterfaceOrientation(window);
//}


void func_org_mini_glfm_Glfm_glfmSetMultitouchEnabled__JZ_V(JThreadRuntime *runtime, s64 p0, s32 p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    glfmSetMultitouchEnabled(window, p2);
}


s32 func_org_mini_glfm_Glfm_glfmGetMultitouchEnabled__J_Z(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    return glfmGetMultitouchEnabled(window);
}


s32 func_org_mini_glfm_Glfm_glfmGetDisplayWidth__J_I(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    s32 w, h;
    glfmGetDisplaySize(window, &w, &h);
    return w;
}

s32 func_org_mini_glfm_Glfm_glfmGetDisplayHeight__J_I(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    s32 w, h;
    glfmGetDisplaySize(window, &w, &h);
    return h;
}

f64 func_org_mini_glfm_Glfm_glfmGetDisplayScale__J_D(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    return glfmGetDisplayScale(window);
}

void func_org_mini_glfm_Glfm_glfmGetDisplayChromeInsets__J_3D_V(JThreadRuntime *runtime, s64 p0, JArray *p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    JObject *r = p2;
    if (r != NULL && r->prop.arr_length >= 4) {
        glfmGetDisplayChromeInsets(window, &(r->prop.as_f64_arr)[0], &(r->prop.as_f64_arr)[1],
                                   &(r->prop.as_f64_arr)[2], &(r->prop.as_f64_arr)[3]);
    }
}

s32 func_org_mini_glfm_Glfm_glfmGetDisplayChrome__J_I(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    return glfmGetDisplayChrome(window);
}


void func_org_mini_glfm_Glfm_glfmSetDisplayChrome__JI_V(JThreadRuntime *runtime, s64 p0, s32 p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    glfmSetDisplayChrome(window, p2);
}


s32 func_org_mini_glfm_Glfm_glfmGetRenderingAPI__J_I(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    return glfmGetRenderingAPI(window);
}


s32 func_org_mini_glfm_Glfm_glfmHasTouch__J_Z(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    return glfmHasTouch(window);
}


void func_org_mini_glfm_Glfm_glfmSetMouseCursor__JI_V(JThreadRuntime *runtime, s64 p0, s32 p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    glfmSetMouseCursor(window, p2);
}


s32 func_org_mini_glfm_Glfm_glfmExtensionSupported__Ljava_lang_String_2_Z(JThreadRuntime *runtime, struct java_lang_String *p0) {
    Utf8String *ustr = utf8_create();
    jstring_2_utf8(p0, ustr);
    s32 ret = glfmExtensionSupported(utf8_cstr(ustr));
    utf8_destory(ustr);
    return ret;
}


s32 func_org_mini_glfm_Glfm_glfmIsKeyboardVisible__J_Z(JThreadRuntime *runtime, s64 p0) {
    return glfmIsKeyboardVisible((__refer) (intptr_t) p0);
}


void func_org_mini_glfm_Glfm_glfmSetKeyboardVisible__JZ_V(JThreadRuntime *runtime, s64 p0, s32 p2) {
    glfmSetKeyboardVisible((__refer) (intptr_t) p0, p2);
}


s8 func_org_mini_glfm_Glfm_glfmIsMetalSupported__J_Z(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    s32 r = glfmIsMetalSupported(window);
    return r;
}


s64 func_org_mini_glfm_Glfm_glfmGetMetalView__J_J(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) ((intptr_t) p0);
    s64 view = (s64) ((intptr_t) glfmGetMetalView(window));
    return view;
}


s8 func_org_mini_glfm_Glfm_glfmIsHapticFeedbackSupported__J_Z(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) ((intptr_t) p0);
    s32 r = glfmIsHapticFeedbackSupported(window);
    return r;
}


s32 func_org_mini_glfm_Glfm_glfmGetSupportedInterfaceOrientation__J_I(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    return glfmGetSupportedInterfaceOrientation(window);
}


s32 func_org_mini_glfm_Glfm_glfmGetInterfaceOrientation__J_I(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    return glfmGetInterfaceOrientation(window);
}

void func_org_mini_glfm_Glfm_glfmPerformHapticFeedback__JI_V(JThreadRuntime *runtime, s64 p0, s32 p2) {
    GLFMDisplay *window = (__refer) ((intptr_t) p0);
    s32 style = p2;
    glfmPerformHapticFeedback(window, style);
}

void func_org_mini_glfm_Glfm_glfmSwapBuffers__J_V(JThreadRuntime *runtime, s64 p0) {
    GLFMDisplay *window = (__refer) ((intptr_t) p0);
    glfmSwapBuffers(window);
}

void func_org_mini_glfm_Glfm_glfmSetSupportedInterfaceOrientation__JI_V(JThreadRuntime *runtime, s64 p0, s32 p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    s32 allowedOrientations = p2;
    glfmSetSupportedInterfaceOrientation(window, allowedOrientations);
}

struct java_lang_String *func_org_mini_glfm_Glfm_glfmGetResRoot___Ljava_lang_String_2(JThreadRuntime *runtime) {
    JObject *jstr = createJavaString(runtime, glfmGetResRoot());
    return (struct java_lang_String *) jstr;
}

struct java_lang_String *func_org_mini_glfm_Glfm_glfmGetSaveRoot___Ljava_lang_String_2(JThreadRuntime *runtime) {
    JObject *jstr = createJavaString(runtime, glfmGetSaveRoot());
    return (struct java_lang_String *) jstr;
}

struct java_lang_String *func_org_mini_glfm_Glfm_glfmGetClipBoardContent___Ljava_lang_String_2(JThreadRuntime *runtime) {
    JObject *jstr = createJavaString(runtime, getClipBoardContent());
    return (struct java_lang_String *) jstr;
}

void func_org_mini_glfm_Glfm_glfmSetClipBoardContent__Ljava_lang_String_2_V(JThreadRuntime *runtime, struct java_lang_String *p0) {
    Utf8String *ustr = utf8_create();
    jstring_2_utf8(p0, ustr);
    setClipBoardContent(utf8_cstr(ustr));
    utf8_destory(ustr);
}

void func_org_mini_glfm_Glfm_glfmPickPhotoAlbum__JII_V(JThreadRuntime *runtime, s64 p0, s32 p2, s32 p3) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    s32 uid = p2;
    s32 type = p3;
    pickPhotoAlbum(window, uid, type);
}

void func_org_mini_glfm_Glfm_glfmPickPhotoCamera__JII_V(JThreadRuntime *runtime, s64 p0, s32 p2, s32 p3) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    s32 uid = p2;
    s32 type = p3;
    pickPhotoCamera(window, uid, type);
}

void func_org_mini_glfm_Glfm_glfmImageCrop__JILjava_lang_String_2IIII_V(JThreadRuntime *runtime, s64 p0, s32 p2, struct java_lang_String *p3, s32 p4, s32 p5, s32 p6, s32 p7) {
    GLFMDisplay *window = p0;
    s32 uid = p2;
    JObject *jstr = p3;
    Utf8String *ustr = utf8_create();
    jstring_2_utf8(jstr, ustr);
    s32 x = p4;
    s32 y = p5;
    s32 w = p6;
    s32 h = p7;
    imageCrop(window, uid, utf8_cstr(ustr), x, y, w, h);
    utf8_destory(ustr);
}


s64 func_org_mini_glfm_Glfm_glfmPlayVideo__JLjava_lang_String_2Ljava_lang_String_2_J(JThreadRuntime *runtime, s64 p0, struct java_lang_String *p2, struct java_lang_String *p3) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    JObject *jstr = p2;
    JObject *jstrMime = p3;
    Utf8String *ustr = utf8_create();
    Utf8String *ustrMime = utf8_create();
    jstring_2_utf8(jstr, ustr);
    jstring_2_utf8(jstrMime, ustrMime);
    void *panel = playVideo(window, utf8_cstr(ustr), utf8_cstr(ustrMime));
    utf8_destory(ustr);
    utf8_destory(ustrMime);
    return (s64) (intptr_t) panel;
}

void func_org_mini_glfm_Glfm_glfmStartVideo__JJ_V(JThreadRuntime *runtime, s64 p0, s64 p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    void *panel = (__refer) (intptr_t) p2;
    startVideo(window, panel);
}

void func_org_mini_glfm_Glfm_glfmPauseVideo__JJ_V(JThreadRuntime *runtime, s64 p0, s64 p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    void *panel = (__refer) (intptr_t) p2;
    pauseVideo(window, panel);
}

void func_org_mini_glfm_Glfm_glfmStopVideo__JJ_V(JThreadRuntime *runtime, s64 p0, s64 p2) {
    GLFMDisplay *window = (__refer) (intptr_t) p0;
    void *panel = (__refer) (intptr_t) p2;
    stopVideo(window, panel);
}

/* ==============================   jni utils =================================*/

JArray *func_org_mini_gl_GLMath_f2b___3F_3B__3B(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    JObject *farr = p0;
    JObject *barr = p1;
    if (farr->prop.arr_length == barr->prop.arr_length * 4) {
        memcpy(barr->prop.as_c8_arr, farr->prop.as_c8_arr, barr->prop.arr_length);
    }
    return barr;
}

static inline void vec_add(JArray *ra, JArray *aa, JArray *ba) {
    GLfloat *r = (GLfloat *) ra->prop.as_f32_arr;
    GLfloat *a = (GLfloat *) aa->prop.as_f32_arr;
    GLfloat *b = (GLfloat *) ba->prop.as_f32_arr;
    int i;
    for (i = 0; i < ra->prop.arr_length; ++i)
        r[i] = a[i] + b[i];
}

JArray *func_org_mini_gl_GLMath_vec_1add___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    JArray *ra = p0;
    JArray *aa = p1;
    JArray *ba = p2;
    vec_add(ra, aa, ba);
    return ra;
}

static inline void vec_sub(JArray *ra, JArray *aa, JArray *ba) {
    GLfloat *r = (GLfloat *) ra->prop.as_f32_arr;
    GLfloat *a = (GLfloat *) aa->prop.as_f32_arr;
    GLfloat *b = (GLfloat *) ba->prop.as_f32_arr;
    int i;
    for (i = 0; i < ra->prop.arr_length; ++i)
        r[i] = a[i] - b[i];
}

JArray *func_org_mini_gl_GLMath_vec_1sub___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    vec_sub(p0, p1, p2);
    return p0;
}

static inline float vec_mul_inner(JArray *aa, JArray *ba) {
    int i;
    float r = 0;
    GLfloat *a = (GLfloat *) aa->prop.as_f32_arr;
    GLfloat *b = (GLfloat *) ba->prop.as_f32_arr;
    for (i = 0; i < aa->prop.arr_length; ++i)
        r += a[i] * b[i];
    return r;
}

f32 func_org_mini_gl_GLMath_vec_1mul_1inner___3F_3F_F(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    float r = vec_mul_inner(p0, p1);
    return r;
}

void vec_scale(JArray *ra, JArray *aa, float f) {
    GLfloat *r = (GLfloat *) ra->prop.as_f32_arr;
    GLfloat *a = (GLfloat *) aa->prop.as_f32_arr;
    int i;
    for (i = 0; i < ra->prop.arr_length; ++i)
        r[i] = a[i] * f;
}

JArray *func_org_mini_gl_GLMath_vec_1scale___3F_3FF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, f32 p2) {
    vec_scale(p0, p1, p2);
    return p0;
}

float vec_len(JArray *ra) {
    return (float) sqrt(vec_mul_inner(ra, ra));
}

f32 func_org_mini_gl_GLMath_vec_1len___3F_F(JThreadRuntime *runtime, JArray *p0) {
    f32 f = vec_len(p0);
    return f;
}

JArray *func_org_mini_gl_GLMath_vec_1normal___3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    float k = 1.f / vec_len(p1);
    vec_scale(p0, p1, k);
    return p0;
}

JArray *func_org_mini_gl_GLMath_vec_1reflect___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    GLfloat *r = (GLfloat *) p0->prop.as_f32_arr;
    GLfloat *a = (GLfloat *) p1->prop.as_f32_arr;
    GLfloat *b = (GLfloat *) p2->prop.as_f32_arr;
    float p = 2.f * vec_mul_inner(p1, p2);
    int i;
    for (i = 0; i < 4; ++i)
        r[i] = a[i] - p * b[i];
    return p0;
}

JArray *func_org_mini_gl_GLMath_vec4_1slerp___3F_3F_3FF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2, f32 p3) {
    GLfloat *r = (GLfloat *) p0->prop.as_f32_arr;
    GLfloat *a = (GLfloat *) p1->prop.as_f32_arr;
    GLfloat *b = (GLfloat *) p2->prop.as_f32_arr;
    vec4_slerp(r, a, b, p3);
    return p0;
}

JArray *func_org_mini_gl_GLMath_vec4_1from_1mat4x4___3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    GLfloat *r = (GLfloat *) p0->prop.as_f32_arr;
    GLfloat *a = (GLfloat *) p1->prop.as_f32_arr;
    quat_from_mat4x4(r, a);
    return p0;
}

JArray *func_org_mini_gl_GLMath_vec_1mul_1cross___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    GLfloat *r = (GLfloat *) p0->prop.as_f32_arr;
    GLfloat *a = (GLfloat *) p1->prop.as_f32_arr;
    GLfloat *b = (GLfloat *) p2->prop.as_f32_arr;
    r[0] = a[1] * b[2] - a[2] * b[1];
    r[1] = a[2] * b[0] - a[0] * b[2];
    r[2] = a[0] * b[1] - a[1] * b[0];
    if (p0->prop.arr_length > 3)r[3] = 1.f;
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1identity___3F__3F(JThreadRuntime *runtime, JArray *p0) {
    mat4x4_identity((vec4 *) p0->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1dup___3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    mat4x4_dup((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1row___3F_3FI__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, s32 p2) {
    mat4x4_row((GLfloat *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1col___3F_3FI__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, s32 p2) {
    mat4x4_col((GLfloat *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1transpose___3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    mat4x4_transpose((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1add___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    mat4x4_add((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, (vec4 *) p2->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1sub___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    mat4x4_sub((vec4 *) (vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, (vec4 *) p2->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1mul___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    mat4x4_mul((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, (vec4 *) p2->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1mul_1vec4___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    mat4x4_mul_vec4((GLfloat *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, (GLfloat *) p2->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1from_1vec3_1mul_1outer___3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2) {
    mat4x4_from_vec3_mul_outer((vec4 *) p0->prop.as_f32_arr, (GLfloat *) p1->prop.as_f32_arr,
                               (GLfloat *) p2->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1translate___3FFFF__3F(JThreadRuntime *runtime, JArray *p0, f32 p1, f32 p2, f32 p3) {
    mat4x4_translate((vec4 *) p0->prop.as_f32_arr, p1, p2, p3);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1translate_1in_1place___3FFFF__3F(JThreadRuntime *runtime, JArray *p0, f32 p1, f32 p2, f32 p3) {
    mat4x4_translate_in_place((vec4 *) p0->prop.as_f32_arr, p1, p2, p3);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1scale___3F_3FF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, f32 p2) {
    mat4x4_scale((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1scale_1aniso___3F_3FFFF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, f32 p2, f32 p3, f32 p4) {
    mat4x4_scale_aniso((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2, p3, p4);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1rotate___3F_3FFFFF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, f32 p2, f32 p3, f32 p4, f32 p5) {
    mat4x4_rotate((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2, p3, p4, p5);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1rotateX___3F_3FF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, f32 p2) {
    mat4x4_rotate_X((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1rotateY___3F_3FF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, f32 p2) {
    mat4x4_rotate_Y((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1rotateZ___3F_3FF__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, f32 p2) {
    mat4x4_rotate_Z((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr, p2);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1invert___3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    mat4x4_invert((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1orthonormalize___3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1) {
    mat4x4_orthonormalize((vec4 *) p0->prop.as_f32_arr, (vec4 *) p1->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1ortho___3FFFFFFF__3F(JThreadRuntime *runtime, JArray *p0, f32 p1, f32 p2, f32 p3, f32 p4, f32 p5, f32 p6) {
    mat4x4_ortho((vec4 *) p0->prop.as_f32_arr, p1, p2, p3, p4, p5, p6);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1frustum___3FFFFFFF__3F(JThreadRuntime *runtime, JArray *p0, f32 p1, f32 p2, f32 p3, f32 p4, f32 p5, f32 p6) {
    mat4x4_frustum((vec4 *) p0->prop.as_f32_arr, p1, p2, p3, p4, p5, p6);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1perspective___3FFFFF__3F(JThreadRuntime *runtime, JArray *p0, f32 p1, f32 p2, f32 p3, f32 p4) {
    mat4x4_perspective((vec4 *) p0->prop.as_f32_arr, p1, p2, p3, p4);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1look_1at___3F_3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2, JArray *p3) {
    mat4x4_look_at((vec4 *) p0->prop.as_f32_arr, (float *) p1->prop.as_f32_arr,
                   (float *) p2->prop.as_f32_arr,
                   (float *) p3->prop.as_f32_arr);
    return p0;
}

JArray *func_org_mini_gl_GLMath_mat4x4_1trans_1rotate_1scale___3F_3F_3F_3F__3F(JThreadRuntime *runtime, JArray *p0, JArray *p1, JArray *p2, JArray *p3) {
    mat4x4_trans_rotate_scale((vec4 *) p0->prop.as_f32_arr, (float *) p1->prop.as_f32_arr,
                              (float *) p2->prop.as_f32_arr,
                              (float *) p3->prop.as_f32_arr);
    return p0;
}


void func_org_mini_gl_GLMath_img_1fill___3BIII_V(JThreadRuntime *runtime, JArray *p0, s32 p1, s32 p2, s32 p3) {
    static const s32 BYTES_PER_PIXEL = 4;
    JArray *canvasArr = p0;// byte array
    if (!canvasArr)return;
    u8 *canvas = (u8 *) canvasArr->prop.as_c8_arr;
    s32 ioffset = p1;
    s32 ilen = p2;
    if (ioffset < 0)ioffset = 0;
    if (ioffset + ilen > canvasArr->prop.arr_length / BYTES_PER_PIXEL)ilen = canvasArr->prop.arr_length / BYTES_PER_PIXEL - ioffset;
    s32 offset = ioffset * BYTES_PER_PIXEL;
    s32 len = ilen * BYTES_PER_PIXEL;
    Int2Float argb;//argb.c3--a  argb.c2--b   argb.c1--g  argb.c0--r
    argb.i = p3;

    if (canvasArr->prop.arr_length < offset || len == 0 || argb.c3 == 0) {
        //
    } else {
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

s32 func_org_mini_gl_GLMath_img_1draw___3BI_3BIIIIIFFFFFFFZI_I(JThreadRuntime *runtime, JArray *p0, s32 p1, JArray *p2, s32 p3, s32 p4, s32 p5, s32 p6, s32 p7, f32 p8, f32 p9, f32 p10, f32 p11, f32 p12, f32 p13, f32 p14, s8 p15, s32 p16) {

    static const s32 CELL_BYTES = 4;
    s32 pos = 0;
    JArray *canvasArr = p0;
    s32 canvasWidth = p1;
    JArray *imgArr = p2;
    s32 imgWidth = p3;
    Bound2d clip;
    clip.x = p4;
    clip.y = p5;
    clip.w = p6;
    clip.h = p7;

    f32 M00 = p8;
    f32 M01 = p9;
    f32 M02 = p10;
    f32 M10 = p11;
    f32 M11 = p12;
    f32 M12 = p13;

    f32 alpha = p14;
    s32 isBitmapFontDraw = p15;
    Int2Float fontRGB;
    fontRGB.i = p16;

//    u8 a = fontRGB.c3;
//    u8 b = fontRGB.c2;
//    u8 g = fontRGB.c1;
//    u8 r = fontRGB.c0;

    s32 process = 0;
    if (!canvasArr || !imgArr || alpha == 0.f || canvasWidth == 0 || imgWidth == 0 || clip.w == 0 || clip.h == 0) {
        //do nothing
    } else {
        u8 *canvas = (u8 *) canvasArr->prop.as_c8_arr;
        u8 *img = (u8 *) imgArr->prop.as_c8_arr;
        s32 canvasHeight = canvasArr->prop.arr_length / CELL_BYTES / canvasWidth;
        s32 imgHeight = imgArr->prop.arr_length / CELL_BYTES / imgWidth;

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
                process = 1;
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
            u8 *canvas = (u8 *) canvasArr->prop.as_c8_arr;

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
        return 0;// success
    } else {
        return 1;// failer
    }
}

//
//static java_native_method method_glfm_table[] = {
//        {"org/mini/nanovg/Gutil", "f2b",                             "([F[B)[B",                                 org_mini_glfm_utils_Gutil_f2b},
//        {"org/mini/nanovg/Gutil", "vec_add",                         "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_add},
//        {"org/mini/nanovg/Gutil", "vec_sub",                         "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_sub},
//        {"org/mini/nanovg/Gutil", "vec_scale",                       "([F[FF)[F",                                org_mini_glfm_utils_Gutil_vec_scale},
//        {"org/mini/nanovg/Gutil", "vec_mul_inner",                   "([F[F)[F",                                 org_mini_glfm_utils_Gutil_vec_mul_inner},
//        {"org/mini/nanovg/Gutil", "vec_len",                         "([F)F",                                    org_mini_glfm_utils_Gutil_vec_len},
//        {"org/mini/nanovg/Gutil", "vec_normal",                      "([F[F)[F",                                 org_mini_glfm_utils_Gutil_vec_normal},
//        {"org/mini/nanovg/Gutil", "vec_mul_cross",                   "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_mul_cross},
//        {"org/mini/nanovg/Gutil", "vec_reflect",                     "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_vec_reflect},
//        {"org/mini/nanovg/Gutil", "vec4_slerp",                      "([F[F[FF)[F",                              org_mini_glfw_utils_Gutil_vec4_slerp},
//        {"org/mini/nanovg/Gutil", "vec4_from_mat4x4",                "([F[F[FF)[F",                              org_mini_glfw_utils_Gutil_vec4_from_mat4x4},
//        {"org/mini/nanovg/Gutil", "mat4x4_identity",                 "([F)[F",                                   org_mini_glfm_utils_Gutil_mat4x4_identity},
//        {"org/mini/nanovg/Gutil", "mat4x4_dup",                      "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_dup},
//        {"org/mini/nanovg/Gutil", "mat4x4_row",                      "([F[FI)[F",                                org_mini_glfm_utils_Gutil_mat4x4_row},
//        {"org/mini/nanovg/Gutil", "mat4x4_col",                      "([F[FI)[F",                                org_mini_glfm_utils_Gutil_mat4x4_col},
//        {"org/mini/nanovg/Gutil", "mat4x4_transpose",                "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_transpose},
//        {"org/mini/nanovg/Gutil", "mat4x4_add",                      "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_add},
//        {"org/mini/nanovg/Gutil", "mat4x4_sub",                      "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_sub},
//        {"org/mini/nanovg/Gutil", "mat4x4_mul",                      "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_mul},
//        {"org/mini/nanovg/Gutil", "mat4x4_mul_vec4",                 "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_mul_vec4},
//        {"org/mini/nanovg/Gutil", "mat4x4_from_vec3_mul_outer",      "([F[F[F)[F",                               org_mini_glfm_utils_Gutil_mat4x4_from_vec3_mul_outer},
//        {"org/mini/nanovg/Gutil", "mat4x4_translate",                "([FFFF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_translate},
//        {"org/mini/nanovg/Gutil", "mat4x4_translate_in_place",       "([FFFF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_translate_in_place},
//        {"org/mini/nanovg/Gutil", "mat4x4_scale",                    "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_scale},
//        {"org/mini/nanovg/Gutil", "mat4x4_scale_aniso",              "([F[FFFF)[F",                              org_mini_glfm_utils_Gutil_mat4x4_scale_aniso},
//        {"org/mini/nanovg/Gutil", "mat4x4_rotate",                   "([F[FFFFF)[F",                             org_mini_glfm_utils_Gutil_mat4x4_rotate},
//        {"org/mini/nanovg/Gutil", "mat4x4_rotateX",                  "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_rotateX},
//        {"org/mini/nanovg/Gutil", "mat4x4_rotateY",                  "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_rotateY},
//        {"org/mini/nanovg/Gutil", "mat4x4_rotateZ",                  "([F[FF)[F",                                org_mini_glfm_utils_Gutil_mat4x4_rotateZ},
//        {"org/mini/nanovg/Gutil", "mat4x4_invert",                   "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_invert},
//        {"org/mini/nanovg/Gutil", "mat4x4_orthonormalize",           "([F[F)[F",                                 org_mini_glfm_utils_Gutil_mat4x4_orthonormalize},
//        {"org/mini/nanovg/Gutil", "mat4x4_ortho",                    "([FFFFFFF)[F",                             org_mini_glfm_utils_Gutil_mat4x4_ortho},
//        {"org/mini/nanovg/Gutil", "mat4x4_frustum",                  "([FFFFFFF)[F",                             org_mini_glfm_utils_Gutil_mat4x4_frustum},
//        {"org/mini/nanovg/Gutil", "mat4x4_perspective",              "([FFFFF)[F",                               org_mini_glfm_utils_Gutil_mat4x4_perspective},
//        {"org/mini/nanovg/Gutil", "mat4x4_look_at",                  "([F[F[F[F)[F",                             org_mini_glfm_utils_Gutil_mat4x4_look_at},
//        {"org/mini/nanovg/Gutil", "mat4x4_trans_rotate_scale",       "([F[F[F[F)[F",                             org_mini_glfw_utils_Gutil_mat4x4_trans_rotate_scale},
//        {"org/mini/glfm/Glfm",    "glfmSetCallBack",                 "(JLorg/mini/glfm/GlfmCallBack;)V",         org_mini_glfm_Glfm_glfmSetCallBack},
//        {"org/mini/glfm/Glfm",    "glfmSetDisplayConfig",            "(JIIIII)V",                                org_mini_glfm_Glfm_glfmSetDisplayConfig},
//        {"org/mini/glfm/Glfm",    "glfmSetUserInterfaceOrientation", "(JI)V",                                    org_mini_glfm_Glfm_glfmSetUserInterfaceOrientation},
//        {"org/mini/glfm/Glfm",    "glfmGetUserInterfaceOrientation", "(J)I",                                     org_mini_glfm_Glfm_glfmGetUserInterfaceOrientation},
//        {"org/mini/glfm/Glfm",    "glfmSetMultitouchEnabled",        "(JZ)V",                                    org_mini_glfm_Glfm_glfmSetMultitouchEnabled},
//        {"org/mini/glfm/Glfm",    "glfmGetMultitouchEnabled",        "(J)Z",                                     org_mini_glfm_Glfm_glfmGetMultitouchEnabled},
//        {"org/mini/glfm/Glfm",    "glfmGetDisplayWidth",             "(J)I",                                     org_mini_glfm_Glfm_glfmGetDisplayWidth},
//        {"org/mini/glfm/Glfm",    "glfmGetDisplayHeight",            "(J)I",                                     org_mini_glfm_Glfm_glfmGetDisplayHeight},
//        {"org/mini/glfm/Glfm",    "glfmGetDisplayScale",             "(J)D",                                     org_mini_glfm_Glfm_glfmGetDisplayScale},
//        {"org/mini/glfm/Glfm",    "glfmGetDisplayChromeInsets",      "(J[D)V",                                   org_mini_glfm_Glfm_glfmGetDisplayChromeInsets},
//        {"org/mini/glfm/Glfm",    "glfmGetDisplayChrome",            "(J)I",                                     org_mini_glfm_Glfm_glfmGetDisplayChrome},
//        {"org/mini/glfm/Glfm",    "glfmSetDisplayChrome",            "(JI)V",                                    org_mini_glfm_Glfm_glfmSetDisplayChrome},
//        {"org/mini/glfm/Glfm",    "glfmGetRenderingAPI",             "(J)I",                                     org_mini_glfm_Glfm_glfmGetRenderingAPI},
//        {"org/mini/glfm/Glfm",    "glfmHasTouch",                    "(J)Z",                                     org_mini_glfm_Glfm_glfmHasTouch},
//        {"org/mini/glfm/Glfm",    "glfmSetMouseCursor",              "(JI)V",                                    org_mini_glfm_Glfm_glfmSetMouseCursor},
//        {"org/mini/glfm/Glfm",    "glfmExtensionSupported",          "(Ljava/lang/String;)Z",                    org_mini_glfm_Glfm_glfmExtensionSupported},
//        {"org/mini/glfm/Glfm",    "glfmSetKeyboardVisible",          "(JZ)V",                                    org_mini_glfm_Glfm_glfmSetKeyboardVisible},
//        {"org/mini/glfm/Glfm",    "glfmIsKeyboardVisible",           "(J)Z",                                     org_mini_glfm_Glfm_glfmIsKeyboardVisible},
//        {"org/mini/glfm/Glfm",    "glfmGetResRoot",                  "()Ljava/lang/String;",                     org_mini_glfm_Glfm_glfmGetResRoot},
//        {"org/mini/glfm/Glfm",    "glfmGetSaveRoot",                 "()Ljava/lang/String;",                     org_mini_glfm_Glfm_glfmGetSaveRoot},
//        {"org/mini/glfm/Glfm",    "glfmGetClipBoardContent",         "()Ljava/lang/String;",                     org_mini_glfm_Glfm_glfmGetClipBoardContent},
//        {"org/mini/glfm/Glfm",    "glfmSetClipBoardContent",         "(Ljava/lang/String;)V",                    org_mini_glfm_Glfm_glfmSetClipBoardContent},
//        {"org/mini/glfm/Glfm",    "glfmPickPhotoAlbum",              "(JII)V",                                   org_mini_glfm_Glfm_glfmPickPhotoAlbum},
//        {"org/mini/glfm/Glfm",    "glfmPickPhotoCamera",             "(JII)V",                                   org_mini_glfm_Glfm_glfmPickPhotoCamera},
//        {"org/mini/glfm/Glfm",    "glfmImageCrop",                   "(JILjava/lang/String;IIII)V",              org_mini_glfm_Glfm_glfmImageCrop},
//        {"org/mini/glfm/Glfm",    "glfmPlayVideo",                   "(JLjava/lang/String;Ljava/lang/String;)J", org_mini_glfm_Glfm_glfmPlayVideo},
//        {"org/mini/glfm/Glfm",    "glfmPauseVideo",                  "(JJ)V",                                    org_mini_glfm_Glfm_glfmPauseVideo},
//        {"org/mini/glfm/Glfm",    "glfmStopVideo",                   "(JJ)V",                                    org_mini_glfm_Glfm_glfmStopVideo},
//        {"org/mini/glfm/Glfm",    "glfmStartVideo",                  "(JJ)V",                                    org_mini_glfm_Glfm_glfmStartVideo},
//
//};
//
//s32 count_GlfmFuncTable() {
//    return sizeof(method_glfm_table) / sizeof(java_native_method);
//}
//
//__refer ptr_GlfmFuncTable() {
//    return &method_glfm_table[0];
//}
