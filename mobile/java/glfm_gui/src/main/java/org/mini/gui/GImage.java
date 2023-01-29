/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glwrap.GLUtil;
import org.mini.nanovg.Nanovg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * GImage is a wrap of nvg_texture
 * GImage may be an immutable image or a mutable image
 *
 * @author gust
 */
public abstract class GImage implements GAttachable {

    //
    Object attachment;


    protected GImage() {
    }


    static public GImage createImage(int gl_textureid, int w, int h) {

        return createImage(gl_textureid, w, h, 0);
    }

    /**
     * image flag options:
     * public static final int NVG_IMAGE_REPEATX = 1<<1;
     * public static final int NVG_IMAGE_REPEATY = 1<<2;
     * public static final int NVG_IMAGE_FLIPY = 1<<3;
     * public static final int NVG_IMAGE_PREMULTIPLIED = 1<<4;
     * public static final int NVG_IMAGE_NEAREST = 1<<5;
     *
     * @param gl_textureid
     * @param w
     * @param h
     * @param imageflag
     * @return
     */
    static public GImage createImage(int gl_textureid, int w, int h, int imageflag) {
        ImageImmutable img = new ImageImmutable();
        img.gl_texture = gl_textureid;
        img.w[0] = w;
        img.h[0] = h;
        img.image_init_flag = imageflag;
        img.gc = false;
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
        ImageImmutable img = new ImageImmutable();
        img.data = data;
        img.image_init_flag = imageflag;
        int[] whd = {0, 0, 0};
        GLUtil.image_get_size(data, whd);
        img.w[0] = whd[0];
        img.h[0] = whd[1];
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
            //System.out.println("jar img path: " + filepath);
            InputStream is = GCallBack.getInstance().getResourceAsStream(filepath);
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

    static public <T extends GImage> T createImageMutable(int width, int height) {
        ImageMutable img = new ImageMutable(width, height);
        return (T) img;
    }

    static public <T extends GImage> T createImageMutable(int width, int height, int imageflag) {
        ImageMutable img = new ImageMutable(width, height, imageflag);
        return (T) img;
    }

    public abstract int getWidth();

    public abstract int getHeight();


    /**
     * MUST call by gl thread
     *
     * @return
     */
    public abstract int getNvgTextureId();


    public abstract int getNvgTextureId(long vg);

    /**
     * MUST call by gl thread
     *
     * @return
     */

    public abstract int getGLTextureId();

    @Override
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public <T extends Object> T getAttachment() {
        return (T) attachment;
    }
}
