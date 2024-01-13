#include "jvm.h"
#include "jvm_util.h"

#include <GL/gl.h>
#include <GL/glu.h>

s32 org_lwjgl_opengl_GL11_glGetError_I0(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  push_int(stack, 0);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glEnable_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glEnable(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glDisable_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glDisable(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glShadeModel_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glShadeModel(arg1);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glClearColor_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3, a4;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  a4.i = localvar_getInt(runtime->localvar, 3);

  glClearColor(a1.f, a2.f, a3.f, a4.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glClearDepth_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Long2Double a1;
  a1.l = localvar_getLong(runtime->localvar, 0);

  glClearDepth(a1.d);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glDepthFunc_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glDepthFunc(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glMatrixMode_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glMatrixMode(arg1);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glLoadIdentity_IV(Runtime *runtime, JClass *clazz) {
  // RuntimeStack *stack = runtime->stack;
  // s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glLoadIdentity();
  return 0;
}
s32 org_lwjgl_opengl_GL11_glClear_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glClear(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glRenderMode_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glRenderMode(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glFogi_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  glFogi(arg1, arg2);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glFogf_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Int2Float a2;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  glFogf(arg1, a2.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glTranslatef_IV(Runtime *runtime, JClass *clazz) {
  // RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3, a4;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  // a4.i = localvar_getInt(runtime->localvar, 3);

  glTranslatef(a1.f, a2.f, a3.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glRotatef_IV(Runtime *runtime, JClass *clazz) {
  // RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3, a4;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  a4.i = localvar_getInt(runtime->localvar, 3);

  glRotatef(a1.f, a2.f, a3.f, a4.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_gluPerspective_IV(Runtime *runtime, JClass *clazz) {
  // RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3, a4;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  a4.i = localvar_getInt(runtime->localvar, 3);

  gluPerspective(a1.f, a2.f, a3.f, a4.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_gluErrorString_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  push_int(stack, 0);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glGetInteger_IV(Runtime *runtime, JClass *clazz) {
  s32 a1 = localvar_getInt(runtime->localvar, 0);
  Instance *buffer = localvar_getRefer(runtime->localvar, 1);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/IntBufferImpl", "array", "[I", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glGetIntegerv(a1, (GLint *)iAry->arr_body);

  return 0;
}

s32 org_lwjgl_opengl_GL11_gluPickMatrix_IV(Runtime *runtime, JClass *clazz) {
  Int2Float a1, a2, a3, a4;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  a4.i = localvar_getInt(runtime->localvar, 3);
  Instance *buffer = localvar_getRefer(runtime->localvar, 4);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/IntBufferImpl", "array", "[I", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  gluPickMatrix(a1.f, a2.f, a3.f, a4.f, (GLint *)iAry->arr_body);

  return 0;
}
s32 org_lwjgl_opengl_GL11_glSelectBuffer_IV(Runtime *runtime, JClass *clazz) {
  Instance *buffer = localvar_getRefer(runtime->localvar, 0);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/IntBufferImpl", "array", "[I", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glSelectBuffer(iAry->arr_length, (GLuint *)iAry->arr_body);

  return 0;
}
s32 org_lwjgl_opengl_GL11_glFog_IV(Runtime *runtime, JClass *clazz) {
  s32 a1 = localvar_getInt(runtime->localvar, 0);
  Instance *buffer = localvar_getRefer(runtime->localvar, 1);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/FloatBufferImpl", "array", "[F", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glFogfv(a1, (GLfloat *)iAry->arr_body);

  return 0;
}

static java_native_method METHODS_LWJGL_TABLE[] = {
    {"org/lwjgl/opengl/GL11", "glGetError", "()I",
     org_lwjgl_opengl_GL11_glGetError_I0},
    {"org/lwjgl/opengl/GL11", "glEnable", "(I)V",
     org_lwjgl_opengl_GL11_glEnable_IV},
    {"org/lwjgl/opengl/GL11", "glDisable", "(I)V",
     org_lwjgl_opengl_GL11_glDisable_IV},
    {"org/lwjgl/opengl/GL11", "glShadeModel", "(I)V",
     org_lwjgl_opengl_GL11_glShadeModel_IV},
    {"org/lwjgl/opengl/GL11", "glClearColor", "(FFFF)V",
     org_lwjgl_opengl_GL11_glClearColor_IV},
    {"org/lwjgl/opengl/GL11", "glClearDepth", "(D)V",
     org_lwjgl_opengl_GL11_glClearDepth_IV},
    {"org/lwjgl/opengl/GL11", "glDepthFunc", "(I)V",
     org_lwjgl_opengl_GL11_glDepthFunc_IV},
    {"org/lwjgl/opengl/GL11", "glMatrixMode", "(I)V",
     org_lwjgl_opengl_GL11_glMatrixMode_IV},
    {"org/lwjgl/opengl/GL11", "glLoadIdentity", "()V",
     org_lwjgl_opengl_GL11_glLoadIdentity_IV},
    {"org/lwjgl/opengl/GL11", "glTranslatef", "(FFF)V",
     org_lwjgl_opengl_GL11_glTranslatef_IV},
    {"org/lwjgl/opengl/GL11", "glRotatef", "(FFFF)V",
     org_lwjgl_opengl_GL11_glRotatef_IV},
    {"org/lwjgl/util/glu/GLU", "gluPerspective", "(FFFF)V",
     org_lwjgl_opengl_GL11_gluPerspective_IV},
    {"org/lwjgl/opengl/GL11", "glGetInteger", "(ILjava/nio/IntBuffer;)V",
     org_lwjgl_opengl_GL11_glGetInteger_IV},
    {"org/lwjgl/util/glu/GLU", "gluPickMatrix", "(FFFFLjava/nio/IntBuffer;)V",
     org_lwjgl_opengl_GL11_gluPickMatrix_IV},
    {"org/lwjgl/opengl/GL11", "glSelectBuffer", "(Ljava/nio/IntBuffer;)V",
     org_lwjgl_opengl_GL11_glSelectBuffer_IV},
    {"org/lwjgl/opengl/GL11", "glRenderMode", "(I)I",
     org_lwjgl_opengl_GL11_glRenderMode_IV},
    {"org/lwjgl/opengl/GL11", "glClear", "(I)V",
     org_lwjgl_opengl_GL11_glClear_IV},
    {"org/lwjgl/opengl/GL11", "glFogi", "(II)V",
     org_lwjgl_opengl_GL11_glFogi_IV},
    {"org/lwjgl/opengl/GL11", "glFogf", "(IF)V",
     org_lwjgl_opengl_GL11_glFogf_IV},
    {"org/lwjgl/opengl/GL11", "glFog", "(ILjava/nio/FloatBuffer;)V",
     org_lwjgl_opengl_GL11_glFog_IV},
    {"org/lwjgl/util/glu/GLU", "gluErrorString", "(I)Ljava/lang/String;",
     org_lwjgl_opengl_GL11_gluErrorString_IV},
};

void reg_lwjgl_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_TABLE,
                 sizeof(METHODS_LWJGL_TABLE) / sizeof(java_native_method));
}
