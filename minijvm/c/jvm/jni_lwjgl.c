#include "jvm.h"
#include "jvm_util.h"

#include <GL/gl.h>
#include <GL/glu.h>

s32 org_lwjgl_opengl_GL11_glPushMatrix_IV(Runtime *runtime, JClass *clazz) {
  glPushMatrix();
  return 0;
}

s32 org_lwjgl_opengl_GL11_glPopMatrix_IV(Runtime *runtime, JClass *clazz) {
  glPopMatrix();
  return 0;
}

s32 org_lwjgl_opengl_GL11_glGetError_I0(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  push_int(stack, 0);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glBegin_V1(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glBegin(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glEnd_V0(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  glEnd();
  return 0;
}

s32 org_lwjgl_opengl_GL11_glEnable_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glEnable(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glViewport_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  s32 arg3 = localvar_getInt(runtime->localvar, 2);
  s32 arg4 = localvar_getInt(runtime->localvar, 3);
  // glViewport(arg1, arg2, arg3, arg4); //works, but commented *for now*
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

s32 org_lwjgl_opengl_GL11_glColor3f_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);

  glColor3f(a1.f, a2.f, a3.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glColor4f_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3, a4;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  a4.i = localvar_getInt(runtime->localvar, 3);

  glColor4f(a1.f, a2.f, a3.f, a4.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glClearDepth_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Long2Double a1;
  a1.l = localvar_getLong(runtime->localvar, 0);
  glClearDepth(a1.d);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glOrtho_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Long2Double a1, a2, a3, a4, a5, a6;
  a1.l = localvar_getLong(runtime->localvar, 0);
  a2.l = localvar_getLong(runtime->localvar, 1);
  a3.l = localvar_getLong(runtime->localvar, 2);
  a4.l = localvar_getLong(runtime->localvar, 3);
  a5.l = localvar_getLong(runtime->localvar, 4);
  a6.l = localvar_getLong(runtime->localvar, 5);
  glOrtho(a1.d, a2.d, a3.d, a4.d, a5.d, a6.d);
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
  glLoadIdentity();
  return 0;
}
s32 org_lwjgl_opengl_GL11_glClear_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glClear(arg1);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glGenLists_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  int res = glGenLists(arg1);
  push_int(stack, res);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glGenTextures_V1(Runtime *runtime, JClass *clazz) {
  Instance *buffer = localvar_getRefer(runtime->localvar, 0);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/IntBufferImpl", "array",
                                     "[I", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glGenTextures(iAry->arr_length, (GLuint *)iAry->arr_body);

  return 0;
}
s32 org_lwjgl_opengl_GL11_glGenTextures_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  u32 res;
  glGenTextures(arg1, &res);
  push_int(stack, res);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glBindTexture_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  glBindTexture(arg1, arg2);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glTexParameteri_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  s32 arg3 = localvar_getInt(runtime->localvar, 2);
  glTexParameteri(arg1, arg2, arg3);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glNewList_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  // s32 arg3 = localvar_getInt(runtime->localvar, 2);
  glNewList(arg1, arg2);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glEndList_IV(Runtime *runtime, JClass *clazz) {
  glEndList();
  return 0;
}
s32 org_lwjgl_opengl_GL11_glCallList_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  // s32 arg2 = localvar_getInt(runtime->localvar, 1);
  // s32 arg3 = localvar_getInt(runtime->localvar, 2);
  glCallList(arg1);
  return 0;
}
// glGenTextures glBindTexture
s32 org_lwjgl_opengl_GL11_glRenderMode_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  int res = glRenderMode(arg1);
  push_int(stack, res);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glFogi_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  glFogi(arg1, arg2);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glDrawArrays_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  s32 arg3 = localvar_getInt(runtime->localvar, 2);
  glDrawArrays(arg1, arg2, arg3);
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
s32 org_lwjgl_opengl_GL11_glLightModel_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  Instance *buffer = localvar_getRefer(runtime->localvar, 1);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/FloatBufferImpl",
                                     "array", "[F", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glLightModelfv(arg1, (GLfloat *)iAry->arr_body);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glAlphaFunc_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Int2Float a2;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  glAlphaFunc(arg1, a2.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glTranslatef_IV(Runtime *runtime, JClass *clazz) {
  // RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  // a4.i = localvar_getInt(runtime->localvar, 3);

  glTranslatef(a1.f, a2.f, a3.f);
  return 0;
}
s32 org_lwjgl_opengl_GL11_glScalef_IV(Runtime *runtime, JClass *clazz) {
  // RuntimeStack *stack = runtime->stack;
  Int2Float a1, a2, a3;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  // a4.i = localvar_getInt(runtime->localvar, 3);

  glScalef(a1.f, a2.f, a3.f);
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
  c8 *pBuffer =
      getFieldPtr_byName_c(buffer, "java/nio/Buffer", "address", "J", runtime);
  GLint *f1 = (GLint *)(intptr_t)getFieldLong(pBuffer);
  if (!f1) {
    abort();
    return 0;
  }
  glGetIntegerv(a1, f1);

  return 0;
}

s32 org_lwjgl_opengl_GL11_glGetFloat_IV(Runtime *runtime, JClass *clazz) {
  s32 a1 = localvar_getInt(runtime->localvar, 0);
  Instance *buffer = localvar_getRefer(runtime->localvar, 1);
  c8 *pBuffer =
      getFieldPtr_byName_c(buffer, "java/nio/Buffer", "address", "J", runtime);
  GLfloat *f1 = (GLfloat *)(intptr_t)getFieldLong(pBuffer);
  if (!f1) {
    abort();
    return 0;
  }
  glGetFloatv(a1, f1);

  return 0;
}

s32 org_lwjgl_opengl_GL11_glInterleavedArrays_IV(Runtime *runtime,
                                                 JClass *clazz) {
  s32 a1 = localvar_getInt(runtime->localvar, 0);
  s32 a2 = localvar_getInt(runtime->localvar, 1);
  Instance *buffer = localvar_getRefer(runtime->localvar, 2);
  c8 *pBuffer =
      getFieldPtr_byName_c(buffer, "java/nio/Buffer", "address", "J", runtime);
  GLfloat *pAry = (GLfloat *)(intptr_t)getFieldLong(pBuffer);
  glInterleavedArrays(a1, a2, pAry);

  return 0;
}

s32 org_lwjgl_opengl_GL11_glVertexPointer_IV(Runtime *runtime, JClass *clazz) {
  s32 size = localvar_getInt(runtime->localvar, 0);
  s32 stride = localvar_getInt(runtime->localvar, 1);
  Instance *buffer = localvar_getRefer(runtime->localvar, 2);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/FloatBufferImpl",
                                     "array", "[F", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glVertexPointer(size, GL_FLOAT, stride, (GLfloat *)iAry->arr_body);

  return 0;
}

s32 org_lwjgl_opengl_GL11_glColorPointer_IV(Runtime *runtime, JClass *clazz) {
  s32 a1 = localvar_getInt(runtime->localvar, 0);
  s32 a2 = localvar_getInt(runtime->localvar, 1);
  Instance *buffer = localvar_getRefer(runtime->localvar, 2);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/FloatBufferImpl",
                                     "array", "[F", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glColorPointer(a1, GL_FLOAT, a2, (GLfloat *)iAry->arr_body);

  return 0;
}
s32 org_lwjgl_opengl_GL11_glTexCoordPointer_IV(Runtime *runtime,
                                               JClass *clazz) {
  s32 a1 = localvar_getInt(runtime->localvar, 0);
  s32 a2 = localvar_getInt(runtime->localvar, 1);
  Instance *buffer = localvar_getRefer(runtime->localvar, 2);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/FloatBufferImpl",
                                     "array", "[F", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glTexCoordPointer(a1, GL_FLOAT, a2, (GLfloat *)iAry->arr_body);

  return 0;
}

// glEnableClientState

s32 org_lwjgl_opengl_GL11_gluPickMatrix_IV(Runtime *runtime, JClass *clazz) {
  Int2Float a1, a2, a3, a4;
  a1.i = localvar_getInt(runtime->localvar, 0);
  a2.i = localvar_getInt(runtime->localvar, 1);
  a3.i = localvar_getInt(runtime->localvar, 2);
  a4.i = localvar_getInt(runtime->localvar, 3);
  Instance *buffer = localvar_getRefer(runtime->localvar, 4);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/IntBufferImpl", "array",
                                     "[I", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  gluPickMatrix(a1.f, a2.f, a3.f, a4.f, (GLint *)iAry->arr_body);

  return 0;
}

s32 org_lwjgl_opengl_GL11_glSelectBuffer_IV(Runtime *runtime, JClass *clazz) {
  Instance *buffer = localvar_getRefer(runtime->localvar, 0);
  c8 *pBuffer = getFieldPtr_byName_c(buffer, "java/nio/IntBufferImpl", "array",
                                     "[I", runtime);
  Instance *iAry = getFieldRefer(pBuffer);
  glSelectBuffer(iAry->arr_length, (GLuint *)iAry->arr_body);

  return 0;
}

s32 org_lwjgl_opengl_GL11_gluBuild2DMipmaps_IV(Runtime *runtime,
                                               JClass *clazz) {
  int arg1, arg2, arg3, arg4, arg5, arg6;
  arg1 = localvar_getInt(runtime->localvar, 0);
  arg2 = localvar_getInt(runtime->localvar, 1);
  arg3 = localvar_getInt(runtime->localvar, 2);
  arg4 = localvar_getInt(runtime->localvar, 3);
  arg5 = localvar_getInt(runtime->localvar, 4);
  arg6 = localvar_getInt(runtime->localvar, 5);

  Instance *buffer = localvar_getRefer(runtime->localvar, 6);
  c8 *pBuffer =
      getFieldPtr_byName_c(buffer, "java/nio/Buffer", "address", "J", runtime);
  GLuint *pAry = (GLuint *)(intptr_t)getFieldLong(pBuffer);
  gluBuild2DMipmaps(arg1, arg2, arg3, arg4, arg5, arg6, pAry);

  return 0;
}
s32 org_lwjgl_opengl_GL11_glInitNames_IV(Runtime *runtime, JClass *clazz) {
  glInitNames();

  return 0;
}

s32 org_lwjgl_opengl_GL11_glPopName_IV(Runtime *runtime, JClass *clazz) {
  glPopName();

  return 0;
}

s32 org_lwjgl_opengl_GL11_glLoadName_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glLoadName(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glPushName_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glPushName(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glEnableClientState_IV(Runtime *runtime,
                                                 JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glEnableClientState(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glDisableClientState_IV(Runtime *runtime,
                                                  JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  glDisableClientState(arg1);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glBlendFunc_IV(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  s32 arg1 = localvar_getInt(runtime->localvar, 0);
  s32 arg2 = localvar_getInt(runtime->localvar, 1);
  // s32 arg3 = localvar_getInt(runtime->localvar, 2);
  glBlendFunc(arg1, arg2);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glFog_IV(Runtime *runtime, JClass *clazz) {
  s32 a1 = localvar_getInt(runtime->localvar, 0);
  Instance *buffer = localvar_getRefer(runtime->localvar, 1);
  c8 *pBuffer =
      getFieldPtr_byName_c(buffer, "java/nio/Buffer", "address", "J", runtime);
  GLfloat *pAry = (GLfloat *)(intptr_t)getFieldLong(pBuffer);
  glFogfv(a1, pAry);

  return 0;
}

s32 org_lwjgl_opengl_GL11_glTexCoord2f_V2(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Int2Float arg1 = {.i = localvar_getInt(runtime->localvar, 0)};
  Int2Float arg2 = {.i = localvar_getInt(runtime->localvar, 1)};
  glTexCoord2f(arg1.f, arg2.f);
  return 0;
}

s32 org_lwjgl_opengl_GL11_glVertex3f_V3(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  Int2Float arg1 = {.i = localvar_getInt(runtime->localvar, 0)};
  Int2Float arg2 = {.i = localvar_getInt(runtime->localvar, 1)};
  Int2Float arg3 = {.i = localvar_getInt(runtime->localvar, 2)};
  glVertex3f(arg1.f, arg2.f, arg3.f);
  return 0;
}

static java_native_method METHODS_LWJGL_TABLE[] = {
    {"org/lwjgl/opengl/GL11", "glGetError", "()I",
     org_lwjgl_opengl_GL11_glGetError_I0},
    {"org/lwjgl/opengl/GL11", "glInitNames", "()V",
     org_lwjgl_opengl_GL11_glInitNames_IV},
    {"org/lwjgl/opengl/GL11", "glPopName", "()V",
     org_lwjgl_opengl_GL11_glPopName_IV},
    {"org/lwjgl/opengl/GL11", "glPushName", "(I)V",
     org_lwjgl_opengl_GL11_glPushName_IV},
    {"org/lwjgl/opengl/GL11", "glLoadName", "(I)V",
     org_lwjgl_opengl_GL11_glLoadName_IV},
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
    {"org/lwjgl/opengl/GL11", "glOrtho", "(DDDDDD)V",
     org_lwjgl_opengl_GL11_glOrtho_IV},
    {"org/lwjgl/opengl/GL11", "glDepthFunc", "(I)V",
     org_lwjgl_opengl_GL11_glDepthFunc_IV},
    {"org/lwjgl/opengl/GL11", "glMatrixMode", "(I)V",
     org_lwjgl_opengl_GL11_glMatrixMode_IV},
    {"org/lwjgl/opengl/GL11", "glGenLists", "(I)I",
     org_lwjgl_opengl_GL11_glGenLists_IV},
    {"org/lwjgl/opengl/GL11", "glEnableClientState", "(I)V",
     org_lwjgl_opengl_GL11_glEnableClientState_IV},
    {"org/lwjgl/opengl/GL11", "glDisableClientState", "(I)V",
     org_lwjgl_opengl_GL11_glDisableClientState_IV},
    //{"org/lwjgl/opengl/GL11", "glGenTextures", "(I)I",
    // org_lwjgl_opengl_GL11_glGenTextures_IV},
    {"org/lwjgl/opengl/GL11", "glGenTextures", "(Ljava/nio/IntBuffer;)V",
     org_lwjgl_opengl_GL11_glGenTextures_V1},
    {"org/lwjgl/opengl/GL11", "glBlendFunc", "(II)V",
     org_lwjgl_opengl_GL11_glBlendFunc_IV},
    {"org/lwjgl/opengl/GL11", "glBindTexture", "(II)V",
     org_lwjgl_opengl_GL11_glBindTexture_IV},
    {"org/lwjgl/opengl/GL11", "glDrawArrays", "(III)V",
     org_lwjgl_opengl_GL11_glDrawArrays_IV},
    {"org/lwjgl/opengl/GL11", "glTexParameteri", "(III)V",
     org_lwjgl_opengl_GL11_glTexParameteri_IV},
    {"org/lwjgl/opengl/GL11", "glLoadIdentity", "()V",
     org_lwjgl_opengl_GL11_glLoadIdentity_IV},
    {"org/lwjgl/opengl/GL11", "glTranslatef", "(FFF)V",
     org_lwjgl_opengl_GL11_glTranslatef_IV},
    {"org/lwjgl/opengl/GL11", "glRotatef", "(FFFF)V",
     org_lwjgl_opengl_GL11_glRotatef_IV},
     {"org/lwjgl/opengl/GL11", "glViewport", "(IIII)V",
     org_lwjgl_opengl_GL11_glViewport_IV},
     
    {"org/lwjgl/opengl/GL11", "glColor3f", "(FFF)V",
     org_lwjgl_opengl_GL11_glColor3f_IV},
    {"org/lwjgl/opengl/GL11", "glColor4f", "(FFFF)V",
     org_lwjgl_opengl_GL11_glColor4f_IV},
    {"org/lwjgl/util/glu/GLU", "gluPerspective", "(FFFF)V",
     org_lwjgl_opengl_GL11_gluPerspective_IV},
    {"org/lwjgl/opengl/GL11", "glGetInteger", "(ILjava/nio/IntBuffer;)V",
     org_lwjgl_opengl_GL11_glGetInteger_IV},
    {"org/lwjgl/opengl/GL11", "glGetFloat", "(ILjava/nio/FloatBuffer;)V",
     org_lwjgl_opengl_GL11_glGetFloat_IV},
    {"org/lwjgl/opengl/GL11", "glLightModel", "(ILjava/nio/FloatBuffer;)V",
     org_lwjgl_opengl_GL11_glLightModel_IV},
    {"org/lwjgl/util/glu/GLU", "gluBuild2DMipmaps",
     "(IIIIIILjava/nio/ByteBuffer;)I",
     org_lwjgl_opengl_GL11_gluBuild2DMipmaps_IV},
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
    {"org/lwjgl/opengl/GL11", "glAlphaFunc", "(IF)V",
     org_lwjgl_opengl_GL11_glAlphaFunc_IV},
    {"org/lwjgl/opengl/GL11", "glScalef", "(FFF)V",
     org_lwjgl_opengl_GL11_glScalef_IV},

    {"org/lwjgl/opengl/GL11", "glNewList", "(II)V",
     org_lwjgl_opengl_GL11_glNewList_IV},
    {"org/lwjgl/opengl/GL11", "glEndList", "()V",
     org_lwjgl_opengl_GL11_glEndList_IV},
    {"org/lwjgl/opengl/GL11", "glCallList", "(I)V",
     org_lwjgl_opengl_GL11_glCallList_IV},

    {"org/lwjgl/opengl/GL11", "glFogf", "(IF)V",
     org_lwjgl_opengl_GL11_glFogf_IV},
    {"org/lwjgl/opengl/GL11", "glFog", "(ILjava/nio/FloatBuffer;)V",
     org_lwjgl_opengl_GL11_glFog_IV},
    {"org/lwjgl/opengl/GL11", "glVertexPointer", "(IILjava/nio/FloatBuffer;)V",
     org_lwjgl_opengl_GL11_glVertexPointer_IV},
    {"org/lwjgl/opengl/GL11", "glInterleavedArrays",
     "(IILjava/nio/FloatBuffer;)V",
     org_lwjgl_opengl_GL11_glInterleavedArrays_IV},
    {"org/lwjgl/opengl/GL11", "glTexCoordPointer",
     "(IILjava/nio/FloatBuffer;)V", org_lwjgl_opengl_GL11_glTexCoordPointer_IV},
    {"org/lwjgl/opengl/GL11", "glColorPointer", "(IILjava/nio/FloatBuffer;)V",
     org_lwjgl_opengl_GL11_glColorPointer_IV},
    {"org/lwjgl/util/glu/GLU", "gluErrorString", "(I)Ljava/lang/String;",
     org_lwjgl_opengl_GL11_gluErrorString_IV},

    {"org/lwjgl/opengl/GL11", "glPushMatrix", "()V",
     org_lwjgl_opengl_GL11_glPushMatrix_IV},
    {"org/lwjgl/opengl/GL11", "glPopMatrix", "()V",
     org_lwjgl_opengl_GL11_glPopMatrix_IV},

    {"org/lwjgl/opengl/GL11", "glBegin", "(I)V",
     org_lwjgl_opengl_GL11_glBegin_V1},
    {"org/lwjgl/opengl/GL11", "glEnd", "()V", org_lwjgl_opengl_GL11_glEnd_V0},

    {"org/lwjgl/opengl/GL11", "glTexCoord2f", "(FF)V",
     org_lwjgl_opengl_GL11_glTexCoord2f_V2},
    {"org/lwjgl/opengl/GL11", "glVertex3f", "(FFF)V",
     org_lwjgl_opengl_GL11_glVertex3f_V3},
};

void reg_lwjgl_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_TABLE,
                 sizeof(METHODS_LWJGL_TABLE) / sizeof(java_native_method));
}
