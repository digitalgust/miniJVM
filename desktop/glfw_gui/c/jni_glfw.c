/* nuklear - 1.32.0 - public domain */
#define WIN32_LEAN_AND_MEAN

#include <stdio.h>
#include <string.h>
#include "deps/include/glad/glad.h"
#include "deps/include/GLFW/glfw3.h"
#include "deps/include/linmath.h"

//#define STB_IMAGE_IMPLEMENTATION

//#include "deps/include/stb_image.h"

#include "jvm.h"
#include "media.h"


GlobeRefer refers;


/* ==============================   local tools  =================================*/


Instance *createJavaString(Runtime *runtime, c8 *cstr) {
    JniEnv *env = runtime->jnienv;
    Utf8String *ustr = env->utf8_create_part_c(cstr, 0, strlen(cstr));
    Instance *jstr = env->jstring_create(ustr, runtime);
    env->utf8_destory(ustr);
    return jstr;
}

/* ==============================   jni callback =================================*/
static void _callback_error_before_init(int error, const char *description) {
    fprintf(stderr, "GLFW Error: %s\n", description);
}

static void _callback_error(int error, const char *description) {
    if (refers._callback_error) {
        JniEnv *env = refers.env;
        Utf8String *ustr = env->utf8_create_part_c((c8 *) description, 0, strlen(description));
        Instance *jstr = refers.env->jstring_create(ustr, refers.runtime);
        env->utf8_destory(ustr);
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_int(refers.runtime->stack, error);
        env->push_ref(refers.runtime->stack, jstr);
        s32 ret = env->execute_method(refers._callback_error, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

static void _callback_key(GLFWwindow *window, int key, int scancode, int action, int mods) {
    if (refers._callback_key) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, key);
        env->push_int(refers.runtime->stack, scancode);
        env->push_int(refers.runtime->stack, action);
        env->push_int(refers.runtime->stack, mods);
        s32 ret = env->execute_method(refers._callback_key, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

static void _callback_character(GLFWwindow *window, u32 ch) {
    if (refers._callback_character) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, ch);
        s32 ret = env->execute_method(refers._callback_character, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

static void _callback_drop(GLFWwindow *window, s32 count, const c8 **cstrs) {
    if (refers._callback_drop) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, count);
        c8 *STR_JAVA_LANG_STRING = "java/lang/String";
        Utf8String *cls = env->utf8_create_part_c(STR_JAVA_LANG_STRING, 0, strlen(STR_JAVA_LANG_STRING));
        Instance *jstrs = env->jarray_create_by_type_name(refers.runtime, count, cls);
        env->utf8_destory(cls);
        s32 i;
        for (i = 0; i < count; i++) {
            s64 val = (intptr_t) createJavaString(refers.runtime, (c8 *) cstrs[i]);
            env->jarray_set_field(jstrs, i, val);
        }
        env->push_ref(refers.runtime->stack, jstrs);
        s32 ret = env->execute_method(refers._callback_drop, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _button_callback_mouse(GLFWwindow *window, int button, int action, int mods) {
    if (refers._button_callback_mouse) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, button);
        env->push_int(refers.runtime->stack, action == GLFW_PRESS);
        s32 ret = env->execute_method(refers._button_callback_mouse, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_scroll(GLFWwindow *window, double scrollX, double scrollY) {
    if (refers._scroll_callback) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_double(refers.runtime->stack, scrollX);
        env->push_double(refers.runtime->stack, scrollY);
        s32 ret = env->execute_method(refers._scroll_callback, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_cursor_pos(GLFWwindow *window, f64 x, f64 y) {
    if (refers._callback_cursor_pos) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, x);
        env->push_int(refers.runtime->stack, y);
        s32 ret = env->execute_method(refers._callback_cursor_pos, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_cursor_enter(GLFWwindow *window, s32 enter) {
    if (refers._callback_cursor_enter) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, enter);
        s32 ret = env->execute_method(refers._callback_cursor_enter, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_window_size(GLFWwindow *window, s32 w, s32 h) {
    if (refers._callback_window_size) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, w);
        env->push_int(refers.runtime->stack, h);
        s32 ret = env->execute_method(refers._callback_window_size, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_window_pos(GLFWwindow *window, s32 w, s32 h) {
    if (refers._callback_window_pos) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, w);
        env->push_int(refers.runtime->stack, h);
        s32 ret = env->execute_method(refers._callback_window_pos, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_window_close(GLFWwindow *window) {
    if (refers._callback_window_close) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        s32 ret = env->execute_method(refers._callback_window_close, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        } else {
            env->pop_empty(refers.runtime->stack);
        }
    }
}

void _callback_window_focus(GLFWwindow *window, s32 focus) {
    if (refers._callback_window_focus) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, focus);
        s32 ret = env->execute_method(refers._callback_window_focus, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_window_iconify(GLFWwindow *window, s32 iconified) {
    if (refers._callback_window_iconify) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, iconified);
        s32 ret = env->execute_method(refers._callback_window_iconify, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_window_refresh(GLFWwindow *window) {
    if (refers._callback_window_refresh) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        s32 ret = env->execute_method(refers._callback_window_refresh, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

void _callback_framebuffer_size(GLFWwindow *window, s32 w, s32 h) {
    if (refers._callback_framebuffer_size) {
        JniEnv *env = refers.env;
        env->push_ref(refers.runtime->stack, refers.glfw_callback);
        env->push_long(refers.runtime->stack, (s64) (intptr_t) window);
        env->push_int(refers.runtime->stack, w);
        env->push_int(refers.runtime->stack, h);
        s32 ret = env->execute_method(refers._callback_framebuffer_size, refers.runtime);
        if (ret) {
            env->print_exception(refers.runtime);
        }
    }
}

int org_mini_glfw_utils_Gutil_f2b(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *farr = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *barr = env->localvar_getRefer(runtime->localvar, pos++);
    if (farr->arr_length == barr->arr_length * 4) {
        memcpy(barr->arr_body, farr->arr_body, barr->arr_length);
    }
    env->push_ref(runtime->stack, barr);
    return 0;
}

void vec_add(Instance *ra, Instance *aa, Instance *ba) {
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    int i;
    for (i = 0; i < ra->arr_length; ++i)
        r[i] = a[i] + b[i];
}

int org_mini_glfw_utils_Gutil_vec_add(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    vec_add(ra, aa, ba);
    env->push_ref(runtime->stack, ra);
    return 0;
}

void vec_sub(Instance *ra, Instance *aa, Instance *ba) {
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    int i;
    for (i = 0; i < ra->arr_length; ++i)
        r[i] = a[i] - b[i];
}

int org_mini_glfw_utils_Gutil_vec_sub(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    vec_sub(ra, aa, ba);
    env->push_ref(runtime->stack, ra);
    return 0;
}

float vec_mul_inner(Instance *aa, Instance *ba) {
    int i;
    float r;
    for (i = 0; i < aa->arr_length; ++i)
        r += aa->arr_body[i] * ba->arr_body[i];
    return r;
}

int org_mini_glfw_utils_Gutil_vec_mul_inner(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    float r = vec_mul_inner(aa, ba);
    env->push_float(runtime->stack, r);
    return 0;
}

void vec_scale(Instance *ra, Instance *aa, float f) {
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    int i;
    for (i = 0; i < ra->arr_length; ++i)
        r[i] = a[i] * f;
}

int org_mini_glfw_utils_Gutil_vec_scale(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    vec_scale(ra, aa, f.f);
    env->push_ref(runtime->stack, ra);
    return 0;
}

float vec_len(Instance *ra) {
    return (float) sqrt(vec_mul_inner(ra, ra));
}

int org_mini_glfw_utils_Gutil_vec_len(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    float f = vec_len(ra);
    env->push_float(runtime->stack, f);
    return 0;
}

int org_mini_glfw_utils_Gutil_vec_normal(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    float k = 1.f / vec_len(aa);
    vec_scale(ra, aa, k);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfw_utils_Gutil_vec_reflect(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    float p = 2.f * vec_mul_inner(aa, ba);
    int i;
    for (i = 0; i < 4; ++i)
        r[i] = a[i] - p * b[i];
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfw_utils_Gutil_vec_mul_cross(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *aa = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *ba = env->localvar_getRefer(runtime->localvar, pos++);
    GLfloat *r = (GLfloat *) ra->arr_body;
    GLfloat *a = (GLfloat *) aa->arr_body;
    GLfloat *b = (GLfloat *) ba->arr_body;
    r[0] = a[1] * b[2] - a[2] * b[1];
    r[1] = a[2] * b[0] - a[0] * b[2];
    r[2] = a[0] * b[1] - a[1] * b[0];
    if (ra->arr_length > 3)r[3] = 1.f;
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_identity(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_identity((vec4 *) r->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_dup(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_dup((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_row(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    int row = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_row((GLfloat *) r->arr_body, (vec4 *) m1->arr_body, row);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_col(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    int col = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_col((GLfloat *) r->arr_body, (vec4 *) m1->arr_body, col);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_transpose(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_transpose((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_add(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_add((vec4 *) r->arr_body, (vec4 *) m1->arr_body, (vec4 *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_sub(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_sub((vec4 *) r->arr_body, (vec4 *) m1->arr_body, (vec4 *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_mul(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_mul((vec4 *) r->arr_body, (vec4 *) m1->arr_body, (vec4 *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_mul_vec4(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_mul_vec4((GLfloat *) r->arr_body, (vec4 *) m1->arr_body, (GLfloat *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_from_vec3_mul_outer(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m2 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_from_vec3_mul_outer((vec4 *) r->arr_body, (GLfloat *) m1->arr_body, (GLfloat *) m2->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_translate(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_translate((vec4 *) r->arr_body, x.f, y.f, z.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_translate_in_place(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_translate_in_place((vec4 *) r->arr_body, x.f, y.f, z.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_scale(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_scale((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_scale_aniso(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_scale_aniso((vec4 *) r->arr_body, (vec4 *) m1->arr_body, x.f, y.f, z.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_rotate(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float x, y, z, a;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    a.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate((vec4 *) r->arr_body, (vec4 *) m1->arr_body, x.f, y.f, z.f, a.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_rotateX(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate_X((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_rotateY(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate_Y((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_rotateZ(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float f;
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_rotate_Z((vec4 *) r->arr_body, (vec4 *) m1->arr_body, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_invert(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_invert((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_orthonormalize(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *m1 = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_orthonormalize((vec4 *) r->arr_body, (vec4 *) m1->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_ortho(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float l, r, b, t, n, f;
    l.i = env->localvar_getInt(runtime->localvar, pos++);
    r.i = env->localvar_getInt(runtime->localvar, pos++);
    b.i = env->localvar_getInt(runtime->localvar, pos++);
    t.i = env->localvar_getInt(runtime->localvar, pos++);
    n.i = env->localvar_getInt(runtime->localvar, pos++);
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_ortho((vec4 *) ra->arr_body, l.f, r.f, b.f, t.f, n.f, f.f);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_frustum(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *ra = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float l, r, b, t, n, f;
    l.i = env->localvar_getInt(runtime->localvar, pos++);
    r.i = env->localvar_getInt(runtime->localvar, pos++);
    b.i = env->localvar_getInt(runtime->localvar, pos++);
    t.i = env->localvar_getInt(runtime->localvar, pos++);
    n.i = env->localvar_getInt(runtime->localvar, pos++);
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_frustum((vec4 *) ra->arr_body, l.f, r.f, b.f, t.f, n.f, f.f);
    env->push_ref(runtime->stack, ra);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_perspective(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Int2Float y_fov, aspect, n, f;
    y_fov.i = env->localvar_getInt(runtime->localvar, pos++);
    aspect.i = env->localvar_getInt(runtime->localvar, pos++);
    n.i = env->localvar_getInt(runtime->localvar, pos++);
    f.i = env->localvar_getInt(runtime->localvar, pos++);
    mat4x4_perspective((vec4 *) r->arr_body, y_fov.f, aspect.f, n.f, f.f);
    env->push_ref(runtime->stack, r);
    return 0;
}

int org_mini_glfw_utils_Gutil_mat4x4_look_at(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int pos = 0;
    Instance *r = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_eye = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_center = env->localvar_getRefer(runtime->localvar, pos++);
    Instance *vec3_up = env->localvar_getRefer(runtime->localvar, pos++);
    mat4x4_look_at((vec4 *) r->arr_body, (float *) vec3_eye->arr_body,
                   (float *) vec3_center->arr_body,
                   (float *) vec3_up->arr_body);
    env->push_ref(runtime->stack, r);
    return 0;
}

/* ==============================   jni glfw =================================*/

int org_mini_glfw_Glfw_glfwSetCallback(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    refers.glfw_callback = env->localvar_getRefer(runtime->localvar, pos++);

    //this object not refered by jvm , so needs to hold by jni manaul
    if (refers.glfw_callback) env->instance_release_from_thread(refers.glfw_callback, runtime);
    env->instance_hold_to_thread(refers.glfw_callback, runtime);

    glfwSetErrorCallback(_callback_error);
    glfwSetKeyCallback(window, _callback_key);
    glfwSetCharCallback(window, _callback_character);
    glfwSetDropCallback(window, _callback_drop);
    glfwSetMouseButtonCallback(window, _button_callback_mouse);
    glfwSetScrollCallback(window, _callback_scroll);
    glfwSetCursorPosCallback(window, _callback_cursor_pos);
    glfwSetCursorEnterCallback(window, _callback_cursor_enter);
    glfwSetWindowCloseCallback(window, _callback_window_close);
    glfwSetWindowSizeCallback(window, _callback_window_size);
    glfwSetWindowPosCallback(window, _callback_window_pos);
    glfwSetWindowFocusCallback(window, _callback_window_focus);
    glfwSetWindowIconifyCallback(window, _callback_window_iconify);
    glfwSetWindowRefreshCallback(window, _callback_window_refresh);
    glfwSetFramebufferSizeCallback(window, _callback_framebuffer_size);

    c8 *name_s, *type_s;
    {

        name_s = "error";
        type_s = "(ILjava/lang/String;)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_error =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "key";
        type_s = "(JIIII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_key =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "character";
        type_s = "(JC)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_character =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "drop";
        type_s = "(JI[Ljava/lang/String;)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_drop =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "mouseButton";
        type_s = "(JIZ)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._button_callback_mouse =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "scroll";
        type_s = "(JDD)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._scroll_callback =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "cursorPos";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_cursor_pos =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "cursorEnter";
        type_s = "(JZ)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_cursor_enter =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "windowPos";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_window_pos =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "windowSize";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_window_size =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "windowClose";
        type_s = "(J)Z";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_window_close =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "windowRefresh";
        type_s = "(J)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_window_refresh =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "windowFocus";
        type_s = "(JZ)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_window_focus =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "windowIconify";
        type_s = "(JZ)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_window_iconify =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    {
        name_s = "framebufferSize";
        type_s = "(JII)V";
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_framebuffer_size =
                env->find_methodInfo_by_name(refers.glfw_callback->mb.clazz->name, name, type, runtime);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    return 0;
}

int org_mini_glfw_Glfw_glfwGetTime(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    s32 list = env->localvar_getInt(runtime->localvar, pos++);
    env->push_double(runtime->stack, glfwGetTime());
    return 0;
}

int org_mini_glfw_Glfw_glfwSetTime(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    Long2Double t;
    t.l = env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    glfwSetTime(t.d);
    return 0;
}

int org_mini_glfw_Glfw_glfwCreateWindow(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    s32 width = env->localvar_getInt(runtime->localvar, pos++);
    s32 height = env->localvar_getInt(runtime->localvar, pos++);
    Instance *title_arr = env->localvar_getRefer(runtime->localvar, pos++);
    c8 *title = title_arr->arr_body;
    GLFWmonitor *monitor = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    GLFWwindow *share = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    GLFWwindow *window = glfwCreateWindow(width, height, title, monitor, share);
    if (!window) {
        fprintf(stderr, "Failed to open GLFW window\n");
    }
    //
    env->push_long(runtime->stack, (s64) (intptr_t) window);
    return 0;
}

int org_mini_glfw_Glfw_glfwDestroyWindow(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    glfwDestroyWindow(window);
    return 0;
}

int org_mini_glfw_Glfw_glfwWindowShouldClose(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    env->push_int(runtime->stack, GL_TRUE == glfwWindowShouldClose((GLFWwindow *) window));
    return 0;
}

int org_mini_glfw_Glfw_glfwInitJni(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    glfwSetErrorCallback(_callback_error_before_init);
    env->push_int(runtime->stack, glfwInit() == GLFW_TRUE);

    return 0;
}

int org_mini_glfw_Glfw_glfwTerminate(Runtime *runtime, JClass *clazz) {
    glfwTerminate();
    return 0;
}

int org_mini_glfw_Glfw_glfwWindowHint(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    s32 hint = env->localvar_getInt(runtime->localvar, pos++);
    s32 value = env->localvar_getInt(runtime->localvar, pos++);
    glfwWindowHint(hint, value);
    return 0;
}

int org_mini_glfw_Glfw_glfwPollEvents(Runtime *runtime, JClass *clazz) {
    refers.runtime = runtime;
    glfwPollEvents();
    refers.runtime = NULL;
    return 0;
}

int org_mini_glfw_Glfw_glfwSetWindowShouldClose(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 value = env->localvar_getInt(runtime->localvar, pos++);
    glfwSetWindowShouldClose(window, value);
    return 0;
}

int org_mini_glfw_Glfw_glfwMakeContextCurrentJni(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    glfwMakeContextCurrent(window);
    gladLoadGLLoader((GLADloadproc) glfwGetProcAddress);
    return 0;
}

int org_mini_glfw_Glfw_glfwSwapInterval(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    s32 interval = env->localvar_getInt(runtime->localvar, pos++);
    glfwSwapInterval(interval);
    return 0;
}

int org_mini_glfw_Glfw_glfwSwapBuffers(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    glfwSwapBuffers(window);
    return 0;
}

int org_mini_glfw_Glfw_glfwGetFramebufferWidth(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfwGetFramebufferSize(window, &w, &h);
    env->push_int(runtime->stack, w);
    return 0;
}

int org_mini_glfw_Glfw_glfwGetFramebufferHeight(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfwGetFramebufferSize(window, &w, &h);
    env->push_int(runtime->stack, h);
    return 0;
}

int org_mini_glfw_Glfw_glfwGetWindowWidth(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfwGetWindowSize(window, &w, &h);
    env->push_int(runtime->stack, w);
    return 0;
}

int org_mini_glfw_Glfw_glfwGetWindowHeight(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    s32 w, h;
    glfwGetWindowSize(window, &w, &h);
    env->push_int(runtime->stack, h);
    return 0;
}

int org_mini_glfw_Glfw_glfwSetWindowAspectRatio(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 numer = env->localvar_getInt(runtime->localvar, pos++);
    s32 denom = env->localvar_getInt(runtime->localvar, pos++);
    glfwSetWindowAspectRatio(window, numer, denom);
    return 0;
}

int org_mini_glfw_Glfw_glfwGetClipboardString(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    c8 *cstr = (c8 *) glfwGetClipboardString(window);
    if (cstr) {
        Utf8String *ustr = env->utf8_create_part_c(cstr, 0, strlen(cstr));
        Instance *jstr = env->jstring_create(ustr, runtime);
        env->utf8_destory(ustr);
        env->push_ref(runtime->stack, jstr);
    } else {
        env->push_ref(runtime->stack, NULL);
    }
    return 0;
}

int org_mini_glfw_Glfw_glfwSetClipboardString(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *jstr = env->localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr = env->utf8_create();
    env->jstring_2_utf8(jstr, ustr);
    glfwSetClipboardString(window, env->utf8_cstr(ustr));
    env->utf8_destory(ustr);
    return 0;
}


int org_mini_glfw_Glfw_glfwSetWindowTitle(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    GLFWwindow *window = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *jstr = env->localvar_getRefer(runtime->localvar, pos++);
    Utf8String *ustr = env->utf8_create();
    env->jstring_2_utf8(jstr, ustr);
    glfwSetWindowTitle(window, env->utf8_cstr(ustr));
    env->utf8_destory(ustr);
    return 0;
}

/* ==============================   jni gl =================================*/


static java_native_method method_glfw_table[] = {
        {"org/mini/nanovg/Gutil", "f2b",                        "([F[B)[B",                         org_mini_glfw_utils_Gutil_f2b},
        {"org/mini/nanovg/Gutil", "vec_add",                    "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_vec_add},
        {"org/mini/nanovg/Gutil", "vec_sub",                    "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_vec_sub},
        {"org/mini/nanovg/Gutil", "vec_scale",                  "([F[FF)[F",                        org_mini_glfw_utils_Gutil_vec_scale},
        {"org/mini/nanovg/Gutil", "vec_mul_inner",              "([F[F)[F",                         org_mini_glfw_utils_Gutil_vec_mul_inner},
        {"org/mini/nanovg/Gutil", "vec_len",                    "([F)F",                            org_mini_glfw_utils_Gutil_vec_len},
        {"org/mini/nanovg/Gutil", "vec_normal",                 "([F[F)[F",                         org_mini_glfw_utils_Gutil_vec_normal},
        {"org/mini/nanovg/Gutil", "vec_mul_cross",              "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_vec_mul_cross},
        {"org/mini/nanovg/Gutil", "vec_reflect",                "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_vec_reflect},
        {"org/mini/nanovg/Gutil", "mat4x4_identity",            "([F)[F",                           org_mini_glfw_utils_Gutil_mat4x4_identity},
        {"org/mini/nanovg/Gutil", "mat4x4_dup",                 "([F[F)[F",                         org_mini_glfw_utils_Gutil_mat4x4_dup},
        {"org/mini/nanovg/Gutil", "mat4x4_row",                 "([F[FI)[F",                        org_mini_glfw_utils_Gutil_mat4x4_row},
        {"org/mini/nanovg/Gutil", "mat4x4_col",                 "([F[FI)[F",                        org_mini_glfw_utils_Gutil_mat4x4_col},
        {"org/mini/nanovg/Gutil", "mat4x4_transpose",           "([F[F)[F",                         org_mini_glfw_utils_Gutil_mat4x4_transpose},
        {"org/mini/nanovg/Gutil", "mat4x4_add",                 "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_mat4x4_add},
        {"org/mini/nanovg/Gutil", "mat4x4_sub",                 "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_mat4x4_sub},
        {"org/mini/nanovg/Gutil", "mat4x4_mul",                 "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_mat4x4_mul},
        {"org/mini/nanovg/Gutil", "mat4x4_mul_vec4",            "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_mat4x4_mul_vec4},
        {"org/mini/nanovg/Gutil", "mat4x4_from_vec3_mul_outer", "([F[F[F)[F",                       org_mini_glfw_utils_Gutil_mat4x4_from_vec3_mul_outer},
        {"org/mini/nanovg/Gutil", "mat4x4_translate",           "([FFFF)[F",                        org_mini_glfw_utils_Gutil_mat4x4_translate},
        {"org/mini/nanovg/Gutil", "mat4x4_translate_in_place",  "([FFFF)[F",                        org_mini_glfw_utils_Gutil_mat4x4_translate_in_place},
        {"org/mini/nanovg/Gutil", "mat4x4_scale",               "([F[FF)[F",                        org_mini_glfw_utils_Gutil_mat4x4_scale},
        {"org/mini/nanovg/Gutil", "mat4x4_scale_aniso",         "([F[FFFF)[F",                      org_mini_glfw_utils_Gutil_mat4x4_scale_aniso},
        {"org/mini/nanovg/Gutil", "mat4x4_rotate",              "([F[FFFFF)[F",                     org_mini_glfw_utils_Gutil_mat4x4_rotate},
        {"org/mini/nanovg/Gutil", "mat4x4_rotateX",             "([F[FF)[F",                        org_mini_glfw_utils_Gutil_mat4x4_rotateX},
        {"org/mini/nanovg/Gutil", "mat4x4_rotateY",             "([F[FF)[F",                        org_mini_glfw_utils_Gutil_mat4x4_rotateY},
        {"org/mini/nanovg/Gutil", "mat4x4_rotateZ",             "([F[FF)[F",                        org_mini_glfw_utils_Gutil_mat4x4_rotateZ},
        {"org/mini/nanovg/Gutil", "mat4x4_invert",              "([F[F)[F",                         org_mini_glfw_utils_Gutil_mat4x4_invert},
        {"org/mini/nanovg/Gutil", "mat4x4_orthonormalize",      "([F[F)[F",                         org_mini_glfw_utils_Gutil_mat4x4_orthonormalize},
        {"org/mini/nanovg/Gutil", "mat4x4_ortho",               "([FFFFFFF)[F",                     org_mini_glfw_utils_Gutil_mat4x4_ortho},
        {"org/mini/nanovg/Gutil", "mat4x4_frustum",             "([FFFFFFF)[F",                     org_mini_glfw_utils_Gutil_mat4x4_frustum},
        {"org/mini/nanovg/Gutil", "mat4x4_perspective",         "([FFFFF)[F",                       org_mini_glfw_utils_Gutil_mat4x4_perspective},
        {"org/mini/nanovg/Gutil", "mat4x4_look_at",             "([F[F[F[F)[F",                     org_mini_glfw_utils_Gutil_mat4x4_look_at},
        {"org/mini/glfw/Glfw",        "glfwGetTime",                "()D",                              org_mini_glfw_Glfw_glfwGetTime},
        {"org/mini/glfw/Glfw",        "glfwSetTime",                "(D)V",                             org_mini_glfw_Glfw_glfwSetTime},
        {"org/mini/glfw/Glfw",        "glfwCreateWindow",           "(II[BJJ)J",                        org_mini_glfw_Glfw_glfwCreateWindow},
        {"org/mini/glfw/Glfw",        "glfwDestroyWindow",          "(J)V",                             org_mini_glfw_Glfw_glfwDestroyWindow},
        {"org/mini/glfw/Glfw",        "glfwWindowShouldClose",      "(J)Z",                             org_mini_glfw_Glfw_glfwWindowShouldClose},
        {"org/mini/glfw/Glfw",        "glfwSetCallback",            "(JLorg/mini/glfw/GlfwCallback;)V", org_mini_glfw_Glfw_glfwSetCallback},
        {"org/mini/glfw/Glfw",        "glfwInitJni",                "()Z",                              org_mini_glfw_Glfw_glfwInitJni},
        {"org/mini/glfw/Glfw",        "glfwTerminate",              "()V",                              org_mini_glfw_Glfw_glfwTerminate},
        {"org/mini/glfw/Glfw",        "glfwWindowHint",             "(II)V",                            org_mini_glfw_Glfw_glfwWindowHint},
        {"org/mini/glfw/Glfw",        "glfwPollEvents",             "()V",                              org_mini_glfw_Glfw_glfwPollEvents},
        {"org/mini/glfw/Glfw",        "glfwSetWindowShouldClose",   "(JI)V",                            org_mini_glfw_Glfw_glfwSetWindowShouldClose},
        {"org/mini/glfw/Glfw",        "glfwMakeContextCurrentJni",  "(J)V",                             org_mini_glfw_Glfw_glfwMakeContextCurrentJni},
        {"org/mini/glfw/Glfw",        "glfwSwapInterval",           "(I)V",                             org_mini_glfw_Glfw_glfwSwapInterval},
        {"org/mini/glfw/Glfw",        "glfwSwapBuffers",            "(J)V",                             org_mini_glfw_Glfw_glfwSwapBuffers},
        {"org/mini/glfw/Glfw",        "glfwGetFramebufferWidth",    "(J)I",                             org_mini_glfw_Glfw_glfwGetFramebufferWidth},
        {"org/mini/glfw/Glfw",        "glfwGetFramebufferHeight",   "(J)I",                             org_mini_glfw_Glfw_glfwGetFramebufferHeight},
        {"org/mini/glfw/Glfw",        "glfwGetWindowWidth",         "(J)I",                             org_mini_glfw_Glfw_glfwGetWindowWidth},
        {"org/mini/glfw/Glfw",        "glfwGetWindowHeight",        "(J)I",                             org_mini_glfw_Glfw_glfwGetWindowHeight},
        {"org/mini/glfw/Glfw",        "glfwSetWindowAspectRatio",   "(JII)V",                           org_mini_glfw_Glfw_glfwSetWindowAspectRatio},
        {"org/mini/glfw/Glfw",        "glfwGetClipboardString",     "(J)Ljava/lang/String;",            org_mini_glfw_Glfw_glfwGetClipboardString},
        {"org/mini/glfw/Glfw",        "glfwSetClipboardString",     "(JLjava/lang/String;)V",           org_mini_glfw_Glfw_glfwSetClipboardString},
        {"org/mini/glfw/Glfw",        "glfwSetWindowTitle",         "(JLjava/lang/String;)V",           org_mini_glfw_Glfw_glfwSetWindowTitle},

};

s32 count_GlfwFuncTable() {
    return sizeof(method_glfw_table) / sizeof(java_native_method);
}

__refer ptr_GlfwFuncTable() {
    return &method_glfw_table[0];
}
