#include "jvm.h"

#include <GL/glut.h>
#include <GL/freeglut_std.h>
#include <GL/freeglut_ext.h>
#include <GL/gl.h>

int g_wnd = -1;

void myDisplayFunc() {}

static s32 org_lwjgl_input_Display_create_V0(Runtime *runtime, JClass *clazz) {
  int argc = 1;
  char *argv[1] = {"test"};

  glutInit(&argc, argv);
  glutInitDisplayMode(GLUT_SINGLE | GLUT_RGB);
  glutInitWindowSize(1024, 768);
  g_wnd = glutCreateWindow("lwjgl");
  glutDisplayFunc(myDisplayFunc);

  return 0;
}

static s32 org_lwjgl_input_Display_update_V0(Runtime *runtime, JClass *clazz) {
  glutMainLoopEvent();
  glutSwapBuffers();

  return 0;
}

static java_native_method METHODS_LWJGL_DISPLAY_TABLE[] = {
    {"org/lwjgl/opengl/Display", "create", "()V",
     org_lwjgl_input_Display_create_V0},
    {"org/lwjgl/opengl/Display", "update", "()V",
     org_lwjgl_input_Display_update_V0},
};

void reg_lwjgl_display_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_DISPLAY_TABLE,
                 sizeof(METHODS_LWJGL_DISPLAY_TABLE) /
                     sizeof(java_native_method));
}
