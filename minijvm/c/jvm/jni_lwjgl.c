#include "jvm.h"

s32 org_lwjgl_opengl_GL11_glGetError_I0(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  push_int(stack, 0);
  return 0;
}

static java_native_method METHODS_LWJGL_TABLE[] = {
    {"org/lwjgl/opengl/GL11", "glGetError", "()I",
     org_lwjgl_opengl_GL11_glGetError_I0},
};

void reg_lwjgl_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_TABLE,
                 sizeof(METHODS_LWJGL_TABLE) / sizeof(java_native_method));
}
