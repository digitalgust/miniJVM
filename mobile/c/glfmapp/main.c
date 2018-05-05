// Example app that draws a triangle. The triangle can be moved via touch or keyboard arrow keys.
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "glfm.h"


#define FILE_COMPAT_ANDROID_ACTIVITY glfmAndroidGetActivity()


#include "jvm.h"
#include "jvm_util.h"
#include "jni_gui.h"

#ifdef NDEBUG
#define LOG_DEBUG(...) do { } while (0)
#else
#if defined(GLFM_PLATFORM_ANDROID)
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_INFO, "GLFM", __VA_ARGS__)
#else
#define LOG_DEBUG(...) do { } while (0)
#endif
#endif

extern void JNI_OnLoad_mini(JniEnv *env);
extern void JNI_OnUnload_mini(JniEnv *env);
static GLFMDisplay *glfm_display;

// Main entry point
void glfmMain(GLFMDisplay *display) {
    glfm_display=display;

    java_debug=1;

    Utf8String *classpath = utf8_create();
    utf8_append_c(classpath, glfmGetResRoot());
    utf8_append_c(classpath, "/resfiles/minijvm_rt.jar;");
    utf8_append_c(classpath, glfmGetResRoot());
    utf8_append_c(classpath, "/resfiles/glfm_gui.jar;");
    utf8_append_c(classpath, glfmGetResRoot());
    utf8_append_c(classpath, "/resfiles/ExApp.jar;");
    //jvm_printf("%s\n",utf8_cstr(classpath));

    jvm_init(utf8_cstr(classpath), JNI_OnLoad_mini);
    Runtime *runtime=getRuntimeCurThread();

    utf8_destory(classpath);
    c8* p_classname="app/GlfmMain";
    c8* p_methodname="glinit";
    c8* p_methodtype="(J)V";
    push_long(runtime->stack,(intptr_t)display);
    call_method_c(p_classname,p_methodname,p_methodtype,runtime);
}

void glfmDestroy(){
    jvm_destroy(JNI_OnUnload_mini);
}
