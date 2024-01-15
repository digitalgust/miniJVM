#include "jvm.h"

#include <SDL/SDL.h>

int g_Down = 0;
int g_X = -1, g_Y = -1;
int g_XL = -1, g_YL = -1;
int g_mouseGrabbed = 0;
SDL_Event ev;

static s32 org_lwjgl_input_Mouse_getDX_I0(Runtime *runtime, JClass *clazz) {

  push_int(runtime->stack, g_X);
  g_X = 0;
  return 0;
}

static s32 org_lwjgl_input_Mouse_getDY_I0(Runtime *runtime, JClass *clazz) {
  push_int(runtime->stack, -g_Y);
  g_Y = 0;
  return 0;
}

static s32 org_lwjgl_input_Mouse_getEventButton_I0(Runtime *runtime, JClass *clazz) {
  if (!g_Down) {
    push_int(runtime->stack, -1);
    return 0;
  }

  push_int(runtime->stack, 0);
  return 0;
}

static s32 org_lwjgl_input_Mouse_getEventButtonState_Z0(Runtime *runtime, JClass *clazz) {
  push_int(runtime->stack, g_Down);
  return 0;
}

static s32 org_lwjgl_input_Mouse_next_Z0(Runtime *runtime, JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  
  int res = SDL_PollEvent(&ev);
  push_int(stack, res);
  g_XL = g_X;
  g_YL = g_Y;

  if(ev.type == SDL_MOUSEMOTION) {
    g_X = ev.motion.xrel - g_XL;
    g_Y = ev.motion.yrel - g_YL;
    // printf("%d %d\n", g_X, g_Y);
  }
  if(ev.type == SDL_QUIT) {
    SDL_Quit();
  }
  if(ev.type == SDL_MOUSEBUTTONDOWN) {
    if(ev.button.type == SDL_BUTTON_LEFT)
      g_Down = 1;
  } else if(ev.type == SDL_MOUSEBUTTONUP) {
    if(ev.button.type == SDL_BUTTON_LEFT)
      g_Down = 0;
  } 

  return 0;
}

static s32 org_lwjgl_input_Mouse_setGrabbed_V1(Runtime *runtime, JClass *clazz) {
  g_mouseGrabbed = localvar_getInt(runtime->localvar, 0);

  if(g_mouseGrabbed) {
    SDL_WM_GrabInput(SDL_GRAB_ON);
    SDL_ShowCursor(SDL_DISABLE);
  } else {
    SDL_WM_GrabInput(SDL_GRAB_OFF);
    SDL_ShowCursor(SDL_ENABLE);
  }
  return 0;
}

static s32 org_lwjgl_input_Mouse_create_V0(Runtime *runtime, JClass *clazz) {
  return 0;
}

static s32 org_lwjgl_input_Mouse_destroy_V0(Runtime *runtime,
                                               JClass *clazz) {

  return 0;
}
static s32 org_lwjgl_input_Keyboard_getEventKey_I0(Runtime *runtime,
                                                   JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  // if (!g_keyEvents) {
    push_int(stack, 0);
  //   return 0;
  // }
  // 
  // push_int(stack, glut2lwjgl[g_keyEvents->key]);
  return 0;
};
static s32 org_lwjgl_input_Keyboard_getEventCharacter_C0(Runtime *runtime,
                                                         JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  // if (!g_keyEvents) {
    push_int(stack, 0);
  //   return 0;
  // }
  // 
  // push_int(stack, g_keyEvents->key);
  return 0;
};

static s32 org_lwjgl_input_Keyboard_getEventKeyState_Z0(Runtime *runtime,
                                                        JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  // if (!g_keyEvents) {
    push_int(stack, 0);
  //   return 0;
  // }
  // 
  // push_int(stack, g_keyEvents->type);
  return 0;
};

static s32 org_lwjgl_input_Keyboard_isKeyDown_I1(Runtime *runtime,
                                                 JClass *clazz) {
  RuntimeStack *stack = runtime->stack;
  // struct PressedKey *pre = g_pressedKeys;
  // 
  // s32 exp = localvar_getInt(runtime->localvar, 0);
  // 
  // while (pre) {
  //   if (glut2lwjgl[pre->key] == exp) {
  //     push_int(stack, 1);
  //     return 0;
  //   }
  //   pre = pre->next;
  // }

  push_int(stack, 0);
  return 0;
};
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

static java_native_method METHODS_LWJGL_KEYBOARD_TABLE[] = {
    {"org/lwjgl/input/Keyboard", "create", "()V",
     org_lwjgl_input_Mouse_create_V0},
    {"org/lwjgl/input/Keyboard", "destroy", "()V",
     org_lwjgl_input_Mouse_destroy_V0},
    {"org/lwjgl/input/Keyboard", "next", "()Z",
     org_lwjgl_input_Mouse_next_Z0},
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
