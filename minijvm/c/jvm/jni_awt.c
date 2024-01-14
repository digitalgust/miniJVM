#include "jvm.h"
#include "jvm_util.h"

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

static s32 javax_imageio_ImageIO_readInternal_V0(Runtime *runtime,
                                                 JClass *clazz) {
  Instance *iAry = localvar_getRefer(runtime->localvar, 0);
  int x, y, comp;
  u8 *bytes = stbi_load_from_memory((u8 *)iAry->arr_body, iAry->arr_length, &x,
                                    &y, &comp, 4);
  Instance *rgb = jarray_create_by_type_index(runtime, x * y, DATATYPE_INT);

  if (comp == 4) {
    for (int i = 0; i < x * y * comp; i += comp) {
      rgb->arr_body[i] = bytes[i + 3];
      rgb->arr_body[i + 1] = bytes[i + 0];
      rgb->arr_body[i + 2] = bytes[i + 1];
      rgb->arr_body[i + 3] = bytes[i + 2];
    }
  } else {
    for (int i = 0; i < x * y * comp; i += comp) {
      rgb->arr_body[i] = 1.f;
      rgb->arr_body[i + 1] = bytes[i + 0];
      rgb->arr_body[i + 2] = bytes[i + 1];
      rgb->arr_body[i + 3] = bytes[i + 2];
    }
  }

  Instance *iSize = localvar_getRefer(runtime->localvar, 1);
  jarray_set_field(iSize, 0, x);
  jarray_set_field(iSize, 1, y);

  push_ref(runtime->stack, rgb);

  return 0;
}

static s32 java_awt_image_BufferedImage_getRGB_AI7(Runtime *runtime,
                                                   JClass *clazz) {
  Instance *thiz = localvar_getRefer(runtime->localvar, 0);
  int startX = localvar_getInt(runtime->localvar, 1);
  int startY = localvar_getInt(runtime->localvar, 2);
  int w = localvar_getInt(runtime->localvar, 3);
  int h = localvar_getInt(runtime->localvar, 4);
  Instance *outAry = localvar_getRefer(runtime->localvar, 5);
  int offset = localvar_getInt(runtime->localvar, 6);
  int stride = localvar_getInt(runtime->localvar, 7);

  c8 *fptr = getFieldPtr_byName_c(thiz, "java/awt/image/BufferedImage", "rgb",
                                  "[I", runtime);
  c8 *fwptr = getFieldPtr_byName_c(thiz, "java/awt/image/BufferedImage",
                                   "width", "I", runtime);
  c8 *fhptr = getFieldPtr_byName_c(thiz, "java/awt/image/BufferedImage",
                                   "height", "I", runtime);
  Instance *thizRgb = getFieldRefer(fptr);
  s32 ow = getFieldInt(fwptr);
  s32 oh = getFieldInt(fhptr);

  if (!outAry) {
    outAry = jarray_create_by_type_index(runtime, stride * h, DATATYPE_INT);
  }

  for (int x = 0; x < w; ++x) {
    for (int y = 0; y < h; ++y) {
      jarray_set_field(
          outAry, y * stride + x,
          jarray_get_field(thizRgb, (y + startY) * ow + startX + x));
    }
  }

  push_ref(runtime->stack, outAry);
  return 0;
}

static java_native_method METHODS_AWT_TABLE[] = {
    {"javax/imageio/ImageIO", "readInternal", "([B[I)[I",
     javax_imageio_ImageIO_readInternal_V0},
    {"java/awt/image/BufferedImage", "getRGB", "(IIII[III)[I",
     java_awt_image_BufferedImage_getRGB_AI7}};

void reg_awt_native_lib(MiniJVM *jvm) {
  native_reg_lib(jvm, METHODS_AWT_TABLE,
                 sizeof(METHODS_AWT_TABLE) / sizeof(java_native_method));
}
