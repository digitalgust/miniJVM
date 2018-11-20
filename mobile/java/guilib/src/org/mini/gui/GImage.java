/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mini.nanovg.Gutil;
import org.mini.nanovg.Nanovg;

/**
 * 装入图片文件或使用纹理图片生成一个GImage对象 load image or generate texture GImage
 *
 * @author gust
 */
public class GImage {

    int texture;
    int[] w_h_d = new int[3];

    public GImage(int textureid, int w, int h) {
        texture = textureid;
        w_h_d[0] = w;
        w_h_d[1] = h;
    }

    static public GImage createImage(long vg, String filepath) {
        if (filepath == null) {
            return null;
        }
        int tex = Nanovg.nvgCreateImage(vg, Gutil.toUtf8(filepath), 0);
        int[] w = new int[1];
        int[] h = new int[1];
        Nanovg.nvgImageSize(vg, tex, w, h);
        GImage img = new GImage(tex, w[0], h[0]);
        return img;
    }

    static public GImage createImage(long vg, byte[] data) {
        if (data == null) {
            return null;
        }
        int tex = Nanovg.nvgCreateImageMem(vg, 0, data, data.length);
        int[] w = new int[1];
        int[] h = new int[1];
        Nanovg.nvgImageSize(vg, tex, w, h);
        GImage img = new GImage(tex, w[0], h[0]);
        return img;
    }

    static public GImage createImageFromJar(long vg, String filepath) {
        try {
            if (filepath == null) {
                return null;
            }
            InputStream is = "".getClass().getResourceAsStream(filepath);
            if (is != null && is.available() > 0) {
                byte[] data = new byte[is.available()];
                is.read(data);
                return createImage(vg, data);
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

    public int getTexture() {
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
