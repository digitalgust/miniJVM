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
 * GImage is a wrap of nvg_texture
 * <p>
 * 装入图片文件或使用纹理图片生成一个GImage对象 load image or generate nvg_texture GImage
 * 注意 这不是GL的纹理ID
 * it's not the OPENGL textureid.
 *
 * @author gust
 */
public class GImage {

    protected int nvg_texture = -1;
    int[] w = {0};
    int[] h = {0};
    //
    private byte[] data; //source from image data
    private int gl_texture = -1; //source from gl texture id
    private int image_init_flag;


    private GImage() {
    }

    /**
     * this method MUST BE call by gl thread
     */
    synchronized private void initimg() {
        if (nvg_texture == -1) {
            long vg = GCallBack.getInstance().getNvContext();
            if (data != null) {
                nvg_texture = Nanovg.nvgCreateImageMem(vg, image_init_flag, data, data.length);
                Nanovg.nvgImageSize(vg, nvg_texture, w, h);
                data = null;
                gl_texture = Nanovg.nvglImageHandleGL3(vg, nvg_texture);
                //System.out.println("image created with data , nvg: " + nvg_texture + " gl:" + gl_texture);
            } else if (gl_texture != -1) {
                nvg_texture = Nanovg.nvglCreateImageFromHandleGL3(vg, gl_texture, w[0], h[0], image_init_flag);
                //System.out.println("image created with gltexture , nvg: " + nvg_texture + " gl:" + gl_texture);
            }
        }
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
        GImage img = new GImage();
        img.gl_texture = gl_textureid;
        img.w[0] = w;
        img.h[0] = h;
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
        GImage img = new GImage();
        img.data = data;
        img.image_init_flag = imageflag;
        int[] whd = {0, 0, 0};
        Gutil.image_get_size(data, whd);
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

    public int getWidth() {
        return w[0];
    }

    public int getHeight() {
        return h[0];
    }

//    public int getBitDepth() {
//        return w_h_d[2];
//    }

    /**
     * MUST call by gl thread
     *
     * @return
     */
    public int getNvgTextureId() {
        if (nvg_texture == -1) {
            initimg();
        }
        return nvg_texture;
    }

    /**
     * MUST call by gl thread
     *
     * @return
     */

    public int getNvgTextureId(long vg) {
        if (nvg_texture == -1) {
            initimg();
        }
        return nvg_texture;
    }

    /**
     * MUST call by gl thread
     *
     * @return
     */

    public int getGLTextureId() {
        if (nvg_texture == -1) {
            initimg();
        }
        return gl_texture;
    }

    @Override
    public void finalize() {
        try {
            GForm.deleteImage(nvg_texture);
        } catch (Throwable e) {
        }
    }
}
