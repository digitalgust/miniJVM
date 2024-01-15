#include "jvm.h"

#include <SDL/SDL.h>

int g_wnd = -1;
SDL_Surface* screen;

static s32 org_lwjgl_input_Display_create_V0(Runtime *runtime, JClass *clazz) {
  SDL_Init(SDL_INIT_VIDEO);
  SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
  SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
  SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
  SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 16);
  SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
  
  screen = SDL_SetVideoMode(1024, 768, 0, SDL_OPENGL);
  
  return 0;
}


static s32 org_lwjgl_input_Display_update_V0(Runtime *runtime,
                                               JClass *clazz) {
  SDL_GL_SwapBuffers();

  return 0;
}

static java_native_method METHODS_LWJGL_MOUSE_TABLE[] = {
    {"org/lwjgl/opengl/Display", "create", "()V",
     org_lwjgl_input_Display_create_V0},
    {"org/lwjgl/opengl/Display", "update", "()V",
     org_lwjgl_input_Display_update_V0},
};

void reg_lwjgl_display_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_MOUSE_TABLE,
                 sizeof(METHODS_LWJGL_MOUSE_TABLE) / sizeof(java_native_method));
}
