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

    /**
     * ----------------------------------------
     * 2d image process
     * <p>
     *     imgCanvas is 4byte image , argb align
     * ----------------------------------------
     */

    /**
     * fill imgCanvas with argb , start position at fillOffset ,total fill pixels
     *
     * @param imgCanvas
     * @param fillOffset
     * @param fillPixels
     * @param argb
     * @return
     */
    static public native void img_fill(byte[] imgCanvas, int fillOffset, int fillPixels, int argb);

    /**
     * Draw img to imgCanvas,
     * limit in clipX,clipY,clipW,clipH
     * image transform M00,M01,M02(translateX),M10,M11,M12(translateY)
     * [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
     * [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
     * [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
     * <p>
     * if return is not 0, error
     *
     * @param imgCanvas
     * @param canvasWidth
     * @param img
     * @param imgWidth
     * @param clipX
     * @param clipY
     * @param clipW
     * @param clipH
     * @param transformM00
     * @param transformM01
     * @param transformM02
     * @param transformM10
     * @param transformM11
     * @param transformM12
     * @param alpha        image alpha
     * @param bitmapFont   whether draw bitmap font
     * @param fontRGB      if(bitmapFont==true) using fontRGB replace bitmapRGB
     * @return if not 0 error
     */
    static public native int img_draw(byte[] imgCanvas, int canvasWidth,
                                      byte[] img, int imgWidth,
                                      int clipX, int clipY, int clipW, int clipH,
                                      float transformM00, float transformM01, float transformM02, float transformM10, float transformM11, float transformM12,
                                      float alpha,
                                      boolean bitmapFont, int fontRGB);

}
