#include "jvm.h"

static java_native_method METHODS_LWJGL_UTIL_TABLE[] = {
};

void reg_lwjgl_util_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_LWJGL_UTIL_TABLE,
                 sizeof(METHODS_LWJGL_UTIL_TABLE) / sizeof(java_native_method));
}
