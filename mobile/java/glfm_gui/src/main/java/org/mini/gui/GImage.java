/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.nanovg.Gutil;
import org.mini.nanovg.Nanovg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 装入图片文件或使用纹理图片生成一个GImage对象 load image or generate nvg_texture GImage
 *
 * @author gust
 */
public class GImage {

    protected int nvg_texture = -1;
    protected int[] w_h_d = new int[3];
    //
    private byte[] data;
    private int gl_texture;
    private int image_init_flag;

    GImage() {

    }

    GImage(byte[] data) {
        this.data = data;
        Gutil.image_get_size(data, w_h_d);
    }

    static public GImage createImage(int gl_textureid, int w, int h) {

        return createImage(gl_textureid, w, h, 0);
    }

    static public GImage createImage(int gl_textureid, int w, int h, int imageflag) {
        GImage img = new GImage();
        img.gl_texture = gl_textureid;
        img.w_h_d[0] = w;
        img.w_h_d[1] = h;
        img.image_init_flag = imageflag;
        return img;
    }

    static public GImage createImage(String filepath) {
        return createImage(filepath, 0);
    }

    static public GImage createImage(String filepath, int imageflag) {
        if (filepath == null) {
            return null;
        }
        byte[] b = null;
        File f = new File(filepath);
        if (f.exists()) {
            FileInputStream fis = null;
            try {
                b = new byte[(int) f.length()];
                fis = new FileInputStream(f);
                fis.read(b);
            } catch (IOException ex) {
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ex) {
                }
            }

        }
        return createImage(b, imageflag);
    }

    static public GImage createImage(byte[] data) {
        return createImage(data, 0);
    }

    static public GImage createImage(byte[] data, int imageflag) {
        if (data == null) {
            return null;
        }
        GImage img = new GImage(data);
        img.image_init_flag = imageflag;
        return img;
    }

    static public GImage createImageFromJar(String filepath) {
        return createImageFromJar(filepath, 0);
    }

    static public GImage createImageFromJar(String filepath, int imageflag) {
        try {
            if (filepath == null) {
                return null;
            }
            InputStream is = "".getClass().getResourceAsStream(filepath);
            if (is != null && is.available() > 0) {
                byte[] data = new byte[is.available()];
                is.read(data);
                return createImage(data, imageflag);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public int getWidth() {
        return w_h_d[0];
    }

    public int getHeight() {
        return w_h_d[1];
    }

    public int getBitDepth() {
        return w_h_d[2];
    }

    public int getTexture(long vg) {
        if (nvg_texture == -1) {
            if (data != null) {

                nvg_texture = Nanovg.nvgCreateImageMem(vg, image_init_flag, data, data.length);
                int[] w = new int[1];
                int[] h = new int[1];
                Nanovg.nvgImageSize(vg, nvg_texture, w, h);
                data = null;
            } else if (gl_texture != -1) {
                nvg_texture = Nanovg.nvglCreateImageFromHandleGL3(vg, gl_texture, w_h_d[0], w_h_d[1], image_init_flag);
            }
        }
        return nvg_texture;
    }

    @Override
    public void finalize() {
        try {
            GForm.deleteImage(nvg_texture);
        } catch (Throwable e) {
        }
    }
}
