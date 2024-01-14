#include "jvm.h"

#include "glut_keys.h"

#include <GL/freeglut_std.h>
#include <GL/glut.h>

static const short glut2lwjgl[] = {
    ['0'] = KEY_0,
    ['1'] = KEY_1,
    ['2'] = KEY_2,
    ['3'] = KEY_3,
    ['4'] = KEY_4,
    ['5'] = KEY_5,
    ['6'] = KEY_6,
    ['7'] = KEY_7,
    ['8'] = KEY_8,
    ['9'] = KEY_9,
    ['A'] = KEY_A,
    ['+'] = KEY_ADD,
    ['\''] = KEY_APOSTROPHE,
    ['@'] = KEY_AT,
    ['B'] = KEY_B,
    [8] = KEY_BACK,
    ['\\'] = KEY_BACKSLASH,
    ['C'] = KEY_C,
    [':'] = KEY_COLON,
    [','] = KEY_COMMA,
    ['D'] = KEY_D,
    [127] = KEY_DELETE,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_DOWN)] = KEY_DOWN,
    ['E'] = KEY_E,
    ['='] = KEY_EQUALS,
    [27] = KEY_ESCAPE,
    ['F'] = KEY_F,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F1)] = KEY_F1,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F10)] = KEY_F10,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F11)] = KEY_F11,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F12)] = KEY_F12,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F2)] = KEY_F2,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F3)] = KEY_F3,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F4)] = KEY_F4,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F5)] = KEY_F5,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F6)] = KEY_F6,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F7)] = KEY_F7,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F8)] = KEY_F8,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F9)] = KEY_F9,
    ['G'] = KEY_G,
    ['`'] = KEY_GRAVE,
    ['H'] = KEY_H,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_HOME)] = KEY_HOME,
    ['I'] = KEY_I,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_INSERT)] = KEY_INSERT,
    ['J'] = KEY_J,
    ['K'] = KEY_K,
    ['L'] = KEY_L,
    ['['] = KEY_LBRACKET,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_LEFT)] = KEY_LEFT,
    ['M'] = KEY_M,
    ['-'] = KEY_MINUS,
    ['*'] = KEY_MULTIPLY,
    ['N'] = KEY_N,
    ['O'] = KEY_O,
    ['P'] = KEY_P,
    ['.'] = KEY_PERIOD,
    ['Q'] = KEY_Q,
    ['R'] = KEY_R,
    [']'] = KEY_RBRACKET,
    ['\r'] = KEY_RETURN,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_RIGHT)] = KEY_RIGHT,
    ['S'] = KEY_S,
    [';'] = KEY_SEMICOLON,
    ['/'] = KEY_SLASH,
    [' '] = KEY_SPACE,
    ['T'] = KEY_T,
    ['\t'] = KEY_TAB,
    ['U'] = KEY_U,
    ['_'] = KEY_UNDERLINE,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_UP)] = KEY_UP,
    ['V'] = KEY_V,
    ['W'] = KEY_W,
    ['X'] = KEY_X,
    ['Y'] = KEY_Y,
    ['Z'] = KEY_Z,
};

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
    else
      g_keyEvents = neu;
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
    else
      g_keyEvents = neu;
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
      return;
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

  return 0;
}

static s32 org_lwjgl_input_Keyboard_destroy_V0(Runtime *runtime,
                                               JClass *clazz) {
  glutKeyboardFunc(NULL);
  glutKeyboardUpFunc(NULL);
  glutSpecialFunc(NULL);
  // TODO: free event buffers!

  return 0;
}

static java_native_method METHODS_LWJGL_KEYBOARD_TABLE[] = {
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
};

void reg_lwjgl_keyboard_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_KEYBOARD_TABLE,
                 sizeof(METHODS_LWJGL_KEYBOARD_TABLE) / sizeof(java_native_method));
}
