#include "jvm.h"

#include <GL/freeglut_std.h>
#include <GL/glut.h>

static s32 org_lwjgl_input_Mouse_getDX_I0(Runtime *runtime, JClass *clazz) {
  push_int(runtime->stack, 0);
  return 0;
}

static s32 org_lwjgl_input_Mouse_getDY_I0(Runtime *runtime, JClass *clazz) {
  push_int(runtime->stack, 0);
  return 0;
}

static s32 org_lwjgl_input_Mouse_getEventButton_I0(Runtime *runtime, JClass *clazz) {
  push_int(runtime->stack, 0);
  return 0;
}

static s32 org_lwjgl_input_Mouse_getEventButtonState_Z0(Runtime *runtime, JClass *clazz) {
  push_int(runtime->stack, 0);
  return 0;
}

static s32 org_lwjgl_input_Mouse_next_Z0(Runtime *runtime, JClass *clazz) {
  push_int(runtime->stack, 0);
  return 0;
}

static s32 org_lwjgl_input_Mouse_setGrabbed_V1(Runtime *runtime, JClass *clazz) {
  return 0;
}

static s32 org_lwjgl_input_Mouse_create_V0(Runtime *runtime, JClass *clazz) {
  // glutMouseFunc(myMouseCallback);
  // glutPassiveMotionFunc(myPassiveMotionCallback);
}

static s32 org_lwjgl_input_Mouse_destroy_V0(Runtime *runtime,
                                               JClass *clazz) {
  glutMouseFunc(NULL);
  glutPassiveMotionFunc(NULL);
}

static java_native_method METHODS_LWJGL_MOUSE_TABLE[] = {
    {"org/lwjgl/input/Mouse", "create", "()V",
     org_lwjgl_input_Mouse_create_V0},
    {"org/lwjgl/input/Mouse", "destroy", "()V",
     org_lwjgl_input_Mouse_destroy_V0},
    {"org/lwjgl/input/Mouse", "getDX", "()I",
     org_lwjgl_input_Mouse_getDX_I0},
    {"org/lwjgl/input/Mouse", "getDY", "()I",
     org_lwjgl_input_Mouse_getDY_I0},
    {"org/lwjgl/input/Mouse", "getEventButton", "()I",
     org_lwjgl_input_Mouse_getEventButton_I0},
    {"org/lwjgl/input/Mouse", "getEventButtonState", "()Z",
     org_lwjgl_input_Mouse_getEventButtonState_Z0},
    {"org/lwjgl/input/Mouse", "next", "()Z",
     org_lwjgl_input_Mouse_next_Z0},
    {"org/lwjgl/input/Mouse", "setGrabbed", "(Z)V",
     org_lwjgl_input_Mouse_setGrabbed_V1},
};

void reg_lwjgl_mouse_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_MOUSE_TABLE,
                 sizeof(METHODS_LWJGL_MOUSE_TABLE) / sizeof(java_native_method));
}
