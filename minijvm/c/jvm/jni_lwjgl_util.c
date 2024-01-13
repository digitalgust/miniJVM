#include "jvm.h"

#include "glut_keys.h"

#include <GL/freeglut_std.h>
#include <GL/glut.h>

static struct PressedKey {
  int key;

  struct PressedKey *next;
} *g_pressedKeys = NULL;

static struct KeyEvent {
  enum { KEY_EVENT_UP = 0, KEY_EVENT_DOWN = 1 } type;
  int key;

  struct KeyEvent *next;
} *g_keyEvents = NULL;

static void mySpecialCallback(int key, int x, int y) {
  {
    struct KeyEvent *neu = malloc(sizeof(struct KeyEvent));
    neu->type = KEY_EVENT_DOWN;
    neu->key = key;
    neu->next = NULL;

    struct KeyEvent *pre = g_keyEvents;
    while (pre && pre->next)
      pre = pre->next;

    if (pre)
      g_keyEvents->next = neu;
  }

  {
    struct PressedKey *neu = malloc(sizeof(struct PressedKey));
    neu->key = SPECIAL_GLUT_KEY_OFFSET + key;
    neu->next = g_pressedKeys;

    g_pressedKeys = neu;
  }
}

static void myKeyboardCallback(unsigned char key, int x, int y) {
  {
    struct KeyEvent *neu = malloc(sizeof(struct KeyEvent));
    neu->type = KEY_EVENT_DOWN;
    neu->key = key;
    neu->next = NULL;

    struct KeyEvent *pre = g_keyEvents;
    while (pre && pre->next)
      pre = pre->next;

    if (pre)
      g_keyEvents->next = neu;
  }

  {
    struct PressedKey *pre = g_pressedKeys;
    while (pre) {
      if (pre && pre->key == key) {
        goto after;
      }

      pre = pre->next;
    }

    struct PressedKey *neu = malloc(sizeof(struct PressedKey));
    neu->key = key;
    neu->next = g_pressedKeys;

    g_pressedKeys = neu;
after:;
  }
}

static void myKeyboardUpCallback(unsigned char key, int x, int y) {
  {
    struct KeyEvent *neu = malloc(sizeof(struct KeyEvent));
    neu->type = KEY_EVENT_UP;
    neu->key = key;
    neu->next = NULL;

    struct KeyEvent *pre = g_keyEvents;
    while (pre && pre->next)
      pre = pre->next;

    if (pre)
      g_keyEvents->next = neu;
  }

  {
    struct PressedKey *pre = g_pressedKeys;
    if (pre->key == key) {
      struct PressedKey *old = pre->next;
      free(pre);

      pre = old;
      return
    }
    while (pre) {
      if (pre->next && pre->next->key == key) {
        struct PressedKey *old = pre->next->next;
        free(pre->next);

        pre->next = old;
        return;
      }

      pre = pre->next;
    }
  }
}

static s32 org_lwjgl_input_Keyboard_next_Z0(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  struct KeyEvent *old = g_keyEvents;
  if (!old) {
    push_int(stack, 0);
    return 0;
  }
  g_keyEvents = g_keyEvents->next;
  free(old);

  push_int(stack, g_keyEvents != NULL);
  return 0;
};

static s32 org_lwjgl_input_Keyboard_getEventKey_I0(Runtime *runtime,
                                                   JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  if (!g_keyEvents) {
    push_int(stack, 0);
    return 0;
  }

  push_int(stack, glut2lwjgl[g_keyEvents->key]);
  return 0;
};

static s32 org_lwjgl_input_Keyboard_getEventCharacter_C0(Runtime *runtime,
                                                         JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  if (!g_keyEvents) {
    push_int(stack, 0);
    return 0;
  }

  push_int(stack, g_keyEvents->key);
  return 0;
};

static s32 org_lwjgl_input_Keyboard_getEventKeyState_Z0(Runtime *runtime,
                                                        JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  if (!g_keyEvents) {
    push_int(stack, 0);
    return 0;
  }

  push_int(stack, g_keyEvents->type);
  return 0;
};

static s32 org_lwjgl_input_Keyboard_isKeyDown_I1(Runtime *runtime,
                                                 JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  struct PressedKey *pre = g_pressedKeys;

  s32 exp = localvar_getInt(runtime->localvar, 0);

  while (pre) {
    if (glut2lwjgl[pre->key] == exp) {
      push_int(stack, 1);
      return 0;
    }
    pre = pre->next;
  }

  push_int(stack, 0);
  return 0;
};

static s32 org_lwjgl_input_Keyboard_create_V0(Runtime *runtime, JClass *clazz) {
  glutKeyboardFunc(myKeyboardCallback);
  glutKeyboardUpFunc(myKeyboardUpCallback);
  glutSpecialFunc(mySpecialCallback);
}

static s32 org_lwjgl_input_Keyboard_destroy_V0(Runtime *runtime,
                                               JClass *clazz) {
  glutKeyboardFunc(NULL);
  glutKeyboardUpFunc(NULL);
  glutSpecialFunc(NULL);
  // TODO: free event buffers!
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

static java_native_method METHODS_LWJGL_UTIL_TABLE[] = {
    {"org/lwjgl/input/Keyboard", "create", "()V",
     org_lwjgl_input_Keyboard_create_V0},
    {"org/lwjgl/input/Keyboard", "destroy", "()V",
     org_lwjgl_input_Keyboard_destroy_V0},
    {"org/lwjgl/input/Keyboard", "next", "()Z",
     org_lwjgl_input_Keyboard_next_Z0},
    {"org/lwjgl/input/Keyboard", "getEventKey", "()I",
     org_lwjgl_input_Keyboard_getEventKey_I0},
    {"org/lwjgl/input/Keyboard", "getEventKeyState", "()Z",
     org_lwjgl_input_Keyboard_getEventKeyState_Z0},
    {"org/lwjgl/input/Keyboard", "getEventCharacter", "()C",
     org_lwjgl_input_Keyboard_getEventCharacter_C0},
    {"org/lwjgl/input/Keyboard", "isKeyDown", "(I)Z",
     org_lwjgl_input_Keyboard_isKeyDown_I1},

    {"org/lwjgl/input/Mouse", "create", "()V",
     org_lwjgl_input_Mouse_create_V0},
    {"org/lwjgl/input/Mouse", "destroy", "()V",
     org_lwjgl_input_Mouse_destroy_V0},
};

void reg_lwjgl_util_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_UTIL_TABLE,
                 sizeof(METHODS_LWJGL_UTIL_TABLE) / sizeof(java_native_method));
}
