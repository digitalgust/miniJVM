/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.nanovg;

import org.mini.gl.GL;
import org.mini.reflect.DirectMemObj;
import org.mini.reflect.ReflectArray;

import java.io.UnsupportedEncodingException;

import static org.mini.gl.GL.*;
import static org.mini.nanovg.Nanovg.stbi_load;

/**
 * @author gust
 */
public class Gutil {

    static StbFont defaultFont;

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

    static public void printMat4(float[] mat4) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.print(" " + mat4[i * 4 + j]);
            }
            System.out.println();
        }
        System.out.println();
    }


    public static byte[] toUtf8(String s) {
        if (s == null) {
            return null;
        }
        int pos = s.lastIndexOf('\000');
        if (pos >= 0 && pos == s.length() - 1) {

        } else {
            s += '\000';
        }
        byte[] barr = null;
        try {
            barr = s.getBytes("utf-8");
        } catch (UnsupportedEncodingException ex) {
        }
        return barr;
    }

    public static byte[] image_parse_from_file_content(byte[] fileCont, int[] w_h_d) {
        if (fileCont == null) {
            System.out.println("ERROR: image parse file content is null");
        }
        int[] x = {0}, y = {0}, n = {0};
        long ptr = ReflectArray.getBodyPtr(fileCont);
        long raw_data_handle = Nanovg.stbi_load_from_memory(ptr, fileCont.length, x, y, n, 0);
        if (raw_data_handle == 0) {
            System.out.println("ERROR: failed to load image from file content , size:" + fileCont.length);
            return null;
        }
        w_h_d[0] = x[0];
        w_h_d[1] = y[0];
        w_h_d[2] = n[0];

        byte[] b = image_parse(raw_data_handle, w_h_d);
        Nanovg.stbi_image_free(raw_data_handle);
        return b;
    }

    public static byte[] image_parse_from_file_path(String filename, int[] w_h_d) {
        int[] x = {0}, y = {0}, n = {0};
        byte[] fb = toUtf8(filename);
        long raw_data_handle = stbi_load(fb, x, y, n, 4);
        if (raw_data_handle == 0) {
            System.out.println("ERROR: failed to load image: " + filename);
            return null;
        }
        w_h_d[0] = x[0];
        w_h_d[1] = y[0];
        w_h_d[2] = n[0];

        byte[] b = image_parse(raw_data_handle, w_h_d);
        Nanovg.stbi_image_free(raw_data_handle);
        return b;
    }

    public static byte[] image_parse(long raw_data_handle, int[] w_h_d) {
        if (raw_data_handle == 0) {
            System.out.println("ERROR: image raw data is null");
            return null;
        }
        int w = w_h_d[0];
        int h = w_h_d[1];
        int de = w_h_d[2];
        DirectMemObj dmo = new DirectMemObj(raw_data_handle, w * h * de);

        //find pow 2
        byte[] d = new byte[w * h * de];

        dmo.copyTo(0, d, 0, d.length);
        return d;
    }

    public static void checkGlError(String tag) {
        int err = glGetError();
        if (err != 0) {
            System.out.println("gl error tag:" + tag + "  code:" + err + "[" + Integer.toHexString(err) + "]");
            new Throwable().printStackTrace();
        }
    }

    /**
     * load image return opengl GL_TEXTURE_2D id
     *
     * @param filename
     * @param w_h_d    new int[3] for image w,h,bit depth
     * @return
     */
    public static int gl_image_load(String filename, int[] w_h_d) {

        byte[] d = image_parse_from_file_path(filename, w_h_d);

        int format = w_h_d[2] < 4 ? GL_RGB : GL_RGBA;
        return genTexture2D(d, w_h_d[0], w_h_d[1], format, format);
    }

    public static int gl_image_load(byte[] fileCont, int[] w_h_d) {

        byte[] d = image_parse_from_file_content(fileCont, w_h_d);
        int format = w_h_d[2] < 4 ? GL_RGB : GL_RGBA;
        return genTexture2D(d, w_h_d[0], w_h_d[1], format, format);
    }

    public static void image_get_size(byte[] fileCont, int[] w_h_d) {
        int[] x = {0}, y = {0}, n = {0};
        long ptr = ReflectArray.getBodyPtr(fileCont);
        long data = Nanovg.stbi_load_from_memory(ptr, fileCont.length, x, y, n, 0);
        if (data == 0) {
            System.out.println("ERROR: failed to load image: " + fileCont);
            return;
        }
        w_h_d[0] = x[0];
        w_h_d[1] = y[0];
        w_h_d[2] = n[0];
        Nanovg.stbi_image_free(data);
    }


    static public int genTexture2D(byte[] data, int w, int h, int gl_inner_format, int gl_format) {
        int[] tex = {0};
        glGenTextures(1, tex, 0);
        glBindTexture(GL_TEXTURE_2D, tex[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, gl_inner_format, w, h, 0, gl_format, GL_UNSIGNED_BYTE, data, 0);
        glGenerateMipmap(GL_TEXTURE_2D);
        return tex[0];
    }

    static public StbFont getDefaultFont() {
        if (defaultFont == null) {
            defaultFont = new StbFont("/res/wqymhei.ttc");
        }
        return defaultFont;
    }

    static public void printGlVersion() {
        byte[] b;
        String name = new String(glGetString(GL.GL_VENDOR)); //返回负责当前OpenGL实现厂商的名字
        String biaoshifu = new String(glGetString(GL_RENDERER)); //返回一个渲染器标识符，通常是个硬件平台
        String OpenGLVersion = new String(glGetString(GL_VERSION)); //返回当前OpenGL实现的版本号
        b = glGetString(GL.GL_MAJOR_VERSION);
        String majorVersion = b == null ? "" : new String(b);
        b = glGetString(GL.GL_MINOR_VERSION);
        String minorVersion = b == null ? "" : new String(b);
        System.out.println("OpenGL vendor：" + name);
        System.out.println("OpenGL renderer：" + biaoshifu);
        System.out.println("OpenGL version：" + OpenGLVersion);
        System.out.println("OpenGL version：" + majorVersion + "." + minorVersion);
    }
}
