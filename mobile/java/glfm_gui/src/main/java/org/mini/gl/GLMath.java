package org.mini.gl;

public class GLMath {


    /**
     * fill farr into barr and return barr
     *
     * @param farr
     * @param barr
     * @return
     */
    static public native byte[] f2b(float[] farr, byte[] barr);

    /**
     * vec and matrix
     */
    //vec2, vec3, vec4
    static public native float[] vec_add(float[] result, float[] vec1, float[] vec2);

    //vec2, vec3, vec4
    static public native float[] vec_sub(float[] result, float[] vec1, float[] vec2);

    //vec2, vec3, vec4
    static public native float[] vec_scale(float[] result, float[] vec1, float factor);

    //vec2, vec3, vec4
    static public native float vec_mul_inner(float[] vec1, float[] vec2);

    //vec2, vec3, vec4
    static public native float vec_len(float[] vec1);

    //vec2, vec3, vec4
    static public native float[] vec_normal(float[] result, float[] vec1);

    //vec3, vec4
    static public native float[] vec_mul_cross(float[] result, float[] vec1, float[] vec2);

    //vec3, vec4
    static public native float[] vec_reflect(float[] result, float[] vec1, float[] vec2);

    static public native float[] vec4_slerp(float[] result, float[] vec1, float[] vec2, float alpha);

    static public native float[] vec4_from_mat4x4(float[] vec4_result, float[] mat4x4);

    static public native float[] mat4x4_identity(float[] m1);

    static public native float[] mat4x4_dup(float[] r, float[] m1);

    static public native float[] mat4x4_row(float[] r, float[] m1, int row);

    static public native float[] mat4x4_col(float[] r, float[] m1, int col);

    static public native float[] mat4x4_transpose(float[] r, float[] m1);

    static public native float[] mat4x4_add(float[] r, float[] m1, float[] m2);

    static public native float[] mat4x4_sub(float[] r, float[] m1, float[] m2);

    static public native float[] mat4x4_mul(float[] r, float[] m1, float[] m2);

    static public native float[] mat4x4_mul_vec4(float[] r, float[] m1, float[] vec4);

    static public native float[] mat4x4_from_vec3_mul_outer(float[] r, float[] vec31, float[] vec32);

    static public native float[] mat4x4_translate(float[] r, float x, float y, float z);

    static public native float[] mat4x4_translate_in_place(float[] r, float x, float y, float z);

    static public native float[] mat4x4_scale(float[] r, float[] m1, float factor);

    static public native float[] mat4x4_scale_aniso(float[] r, float[] m1, float x, float y, float z);

    static public native float[] mat4x4_rotate(float[] r, float[] m1, float x, float y, float z, float a);

    static public native float[] mat4x4_rotateX(float[] r, float[] m1, float xa);

    static public native float[] mat4x4_rotateY(float[] r, float[] m1, float ya);

    static public native float[] mat4x4_rotateZ(float[] r, float[] m1, float xa);

    static public native float[] mat4x4_invert(float[] r, float[] m1);

    static public native float[] mat4x4_orthonormalize(float[] r, float[] m1);

    static public native float[] mat4x4_ortho(float[] rm, float l, float r, float b, float t, float n, float f);

    static public native float[] mat4x4_frustum(float[] rm, float l, float r, float b, float t, float n, float f);

    static public native float[] mat4x4_perspective(float[] rm, float y_fov, float aspect, float near, float far);

    static public native float[] mat4x4_look_at(float[] rm, float[] vec3_eye, float[] vec3_center, float[] vec3_up);

    static public native float[] mat4x4_trans_rotate_scale(float[] rm, float[] vec3_trans, float[] vec4_rotate, float[] vec3_scale);

}
