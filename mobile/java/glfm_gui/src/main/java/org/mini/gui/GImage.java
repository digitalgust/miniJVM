/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.mini.nanovg.Gutil;
import org.mini.nanovg.Nanovg;

/**
 * 装入图片文件或使用纹理图片生成一个GImage对象 load image or generate texture GImage
 *
 * @author gust
 */
public class GImage {

    int texture = -1;
    int[] w_h_d = new int[3];
    byte[] data;

    GImage() {

    }

    GImage(byte[] data) {
        this.data = data;
        Gutil.image_load_whd(data, w_h_d);
    }

    static public GImage createImage(int textureid, int w, int h) {
        GImage img = new GImage();
        img.texture = textureid;
        img.w_h_d[0] = w;
        img.w_h_d[1] = h;
        return img;
    }

    static public GImage createImage(String filepath) {
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
//        int tex = Nanovg.nvgCreateImage(vg, Gutil.toUtf8(filepath), 0);
//        int[] w = new int[1];
//        int[] h = new int[1];
//        Nanovg.nvgImageSize(vg, tex, w, h);
//        GImage img = new GImage(tex, w[0], h[0]);
//        return img;
        return createImage(b);
    }

    static public GImage createImage(byte[] data) {
        if (data == null) {
            return null;
        }
        GImage img = new GImage(data);

        return img;
    }

    static public GImage createImageFromJar(String filepath) {
        try {
            if (filepath == null) {
                return null;
            }
            InputStream is = "".getClass().getResourceAsStream(filepath);
            if (is != null && is.available() > 0) {
                byte[] data = new byte[is.available()];
                is.read(data);
                return createImage(data);
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
        if (texture == -1) {
            if (data != null) {

                texture = Nanovg.nvgCreateImageMem(vg, 0, data, data.length);
                int[] w = new int[1];
                int[] h = new int[1];
                Nanovg.nvgImageSize(vg, texture, w, h);
                data = null;
            }
        }
        return texture;
    }

    @Override
    public void finalize() {
        try {
            GForm.deleteImage(texture);
        } catch (Throwable e) {
        }
    }
}
