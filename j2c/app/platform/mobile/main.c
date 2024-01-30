// Example app that draws a triangle. The triangle can be moved via touch or keyboard arrow keys.
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "glfm.h"


#define FILE_COMPAT_ANDROID_ACTIVITY glfmAndroidGetActivity()


#include "jvm.h"
#include "media.h"

#ifdef NDEBUG
#define LOG_DEBUG(...) do { } while (0)
#else
#if defined(GLFM_PLATFORM_ANDROID)
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_INFO, "GLFM", __VA_ARGS__)
#else
#define LOG_DEBUG(...) do { } while (0)
#endif
#endif

//extern void JNI_OnLoad_mini(JniEnv *env);
//extern void JNI_OnUnload_mini(JniEnv *env);
static GLFMDisplay *glfm_display;

// Main entry point
void glfmMain(GLFMDisplay *display) {
    glfm_display = display;

    Utf8String *bootclasspath = utf8_create();
    Utf8String *classpath = utf8_create();
    utf8_append_c(bootclasspath, glfmGetResRoot());
    utf8_append_c(bootclasspath, "/resfiles/minijvm_rt.jar");
    utf8_append_c(classpath, glfmGetResRoot());
    utf8_append_c(classpath, "/resfiles/glfm_gui.jar:");
    utf8_append_c(classpath, glfmGetResRoot());
    utf8_append_c(classpath, "/resfiles/xgui.jar:");
    utf8_append_c(classpath, glfmGetResRoot());
    utf8_append_c(classpath, "/resfiles/ExApp.jar:");
    //jvm_printf("%s\n",utf8_cstr(classpath));

    g_jvm = jvm_create(utf8_cstr(bootclasspath),utf8_cstr(classpath));
    JThreadRuntime *runtime = jthreadruntime_create();
    jthread_bound(runtime);



//    jvm_init(utf8_cstr(classpath), JNI_OnLoad_mini);
    sys_properties_set_c("glfm.res.root", glfmGetResRoot());
    sys_properties_set_c("glfm.save.root", glfmGetSaveRoot());
    sys_properties_set_c("glfm.uuid", glfmGetUUID());
//    Runtime *runtime=getRuntimeCurThread(&jnienv);

//    utf8_destory(classpath);
    c8 *p_classname = "org/mini/glfm/GlfmCallBackImpl";
    c8 *p_methodname = "glinit";
    c8 *p_methodtype = "(J)V";
//    push_long(runtime->stack,(intptr_t)display);
//    call_method_c(p_classname,p_methodname,p_methodtype,runtime);

    MethodRaw *method = find_methodraw(p_classname, p_methodname, p_methodtype);
    runtime->exec = method;
    class_clinit(runtime, get_utf8str_by_utfraw_index(runtime->exec->class_name));
    exception_check_print(runtime);

    void (*func_ptr)(JThreadRuntime *, s64) =method->func_ptr;
    func_ptr(runtime, (s64) (intptr_t) display);
    exception_check_print(runtime);

    jthread_unbound(runtime);
}

void glfmDestroy() {
//    jvm_destroy(JNI_OnUnload_mini);
    jvm_destroy(g_jvm);
    g_jvm = NULL;
}
