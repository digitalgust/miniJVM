#include "jvm.h"

#include <GL/freeglut_std.h>
#include <GL/glut.h>

static struct MouseEvent {
  enum { MOUSE_EVENT_UP = 0, MOUSE_EVENT_DOWN = 1, MOUSE_EVENT_PASSIVE = 2 } type;
  int button;
  int x;
  int y;

  struct MouseEvent *next;
} *g_mouseEvents = NULL;
int g_lastX = -1, g_lastY = -1;
int g_mouseGrabbed = 0;

static void myPassiveMotionCallback(int x, int y) {
  struct MouseEvent *neu = malloc(sizeof(struct MouseEvent));
  neu->type = MOUSE_EVENT_PASSIVE;
  neu->x = x;
  neu->y = glutGet(GLUT_WINDOW_HEIGHT) - y;
  neu->next = NULL;

  struct MouseEvent *pre = g_mouseEvents;
  while (pre && pre->next)
    pre = pre->next;

  if (pre)
    g_mouseEvents->next = neu;
  else
    g_mouseEvents = neu;

  const int centerX = glutGet(GLUT_WINDOW_WIDTH)/2;
  const int centerY = glutGet(GLUT_WINDOW_HEIGHT)/2;
  if (x != centerX || y != centerY) {
    glutWarpPointer(centerX, centerY);
  }
}

static void myMouseCallback(int button, int state, int x, int y) {
  struct MouseEvent *neu = malloc(sizeof(struct MouseEvent));
  neu->button = button;
  neu->type = (state == GLUT_DOWN) ? MOUSE_EVENT_DOWN : MOUSE_EVENT_UP;
  neu->x = x;
  neu->y = glutGet(GLUT_WINDOW_HEIGHT) - y;
  neu->next = NULL;

  struct MouseEvent *pre = g_mouseEvents;
  while (pre && pre->next)
    pre = pre->next;

  if (pre)
    g_mouseEvents->next = neu;
  else
    g_mouseEvents = neu;
}

static s32 org_lwjgl_input_Mouse_getDX_I0(Runtime *runtime, JClass *clazz) {
  if (!g_mouseEvents) {
    push_int(runtime->stack, 0);
    return 0;
  }

  push_int(runtime->stack, g_mouseEvents->x - g_lastX);
  return 0;
}

static s32 org_lwjgl_input_Mouse_getDY_I0(Runtime *runtime, JClass *clazz) {
  if (!g_mouseEvents) {
    push_int(runtime->stack, 0);
    return 0;
  }

  push_int(runtime->stack, g_mouseEvents->y - g_lastY);
  return 0;
}

static s32 org_lwjgl_input_Mouse_getEventButton_I0(Runtime *runtime, JClass *clazz) {
  if (!g_mouseEvents) {
    push_int(runtime->stack, -1);
    return 0;
  }

  push_int(runtime->stack, g_mouseEvents->button);
  return 0;
}

static s32 org_lwjgl_input_Mouse_getEventButtonState_Z0(Runtime *runtime, JClass *clazz) {
  if (!g_mouseEvents) {
    push_int(runtime->stack, 0);
    return 0;
  }

  push_int(runtime->stack, g_mouseEvents->type == MOUSE_EVENT_DOWN);
  return 0;
}

static s32 org_lwjgl_input_Mouse_next_Z0(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  struct MouseEvent *old = g_mouseEvents;

  if (!old) {
    push_int(stack, 0);
    return 0;
  }

  g_lastX = old->x;
  g_lastY = old->y;
  g_mouseEvents = g_mouseEvents->next;
  free(old);

  push_int(stack, g_mouseEvents != NULL);
  return 0;
}

static s32 org_lwjgl_input_Mouse_setGrabbed_V1(Runtime *runtime, JClass *clazz) {
  g_mouseGrabbed = localvar_getInt(runtime->localvar, 0);
  glutSetCursor(g_mouseGrabbed ? GLUT_CURSOR_NONE : GLUT_CURSOR_LEFT_ARROW);
  return 0;
}

static s32 org_lwjgl_input_Mouse_create_V0(Runtime *runtime, JClass *clazz) {
  glutMouseFunc(myMouseCallback);
  glutPassiveMotionFunc(myPassiveMotionCallback);

  return 0;
}

static s32 org_lwjgl_input_Mouse_destroy_V0(Runtime *runtime,
                                               JClass *clazz) {
  glutMouseFunc(NULL);
  glutPassiveMotionFunc(NULL);

  return 0;
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
