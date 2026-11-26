/* SPDX-License-Identifier: MIT */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glwrap.GLUtil;
import org.mini.gui.callback.GCallBack;
import org.mini.http.MiniHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * GImage is a wrap of nvg_texture
 * GImage may be an immutable image or a mutable image
 *
 * @author gust
 */
public abstract class GImage implements GAttachable {
    public static final int IMAGE_PER_INIT_WIDTH = 16;
    public static final int IMAGE_PER_INIT_HEIGHT = 16;
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

    /**
     * 加载 URL 图片；重定向时可能先返回占位图。
     *
     * @param url
     * @return
     */
    static public GImage createImage(URL url) {
        if (url == null) return null;
        UrlImage result = new UrlImage();
        MiniHttpClient client = new MiniHttpClient(url.toString(), null, (client1, url1, data) -> {
            GImage decoded = createImage(data);
            if (decoded != null) {
                result.image = decoded;
                GForm.flush();
            }
        });
        client.run();//同步
        return result;
    }

    private static final class UrlImage extends GImage {
        private volatile GImage image = createImageMutable(IMAGE_PER_INIT_WIDTH, IMAGE_PER_INIT_HEIGHT);
        public int getWidth() { return image.getWidth(); }
        public int getHeight() { return image.getHeight(); }
        public int getNvgTextureId() { return image.getNvgTextureId(); }
        public int getNvgTextureId(long vg) { return image.getNvgTextureId(vg); }
        public int getGLTextureId() { return image.getGLTextureId(); }
    }

    private static byte[] readImageData(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count == 0) {
                int value = input.read();
                if (value == -1) break;
                output.write(value);
            } else {
                output.write(buffer, 0, count);
            }
        }
        return output.toByteArray();
    }

    static public GImage createImage(String filepath) {
        return createImage(filepath, 0);
    }

    static public GImage createImage(String filepath, int imageflag) {
        if (filepath == null) {
            return null;
        }
        File f = new File(filepath);
        try (InputStream input = new FileInputStream(f)) {
            return createImage(readImageData(input), imageflag);
        } catch (IOException ex) {
            return null;
        }
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
        if (whd[0] <= 0 || whd[1] <= 0) return null;
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
            InputStream resource = GCallBack.getInstance().getResourceAsStream(filepath);
            if (resource == null) return null;
            try (InputStream is = resource) {
                return createImage(readImageData(is), imageflag);
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
     * 子图在纹理中的偏移（GSubImage 用），整图返回 0
     */
    public int getSx() {
        return 0;
    }

    public int getSy() {
        return 0;
    }

    /**
     * 底层纹理完整尺寸（GSubImage 返回父图尺寸），整图与 getWidth/getHeight 相同
     */
    public int getTexWidth() {
        return getWidth();
    }

    public int getTexHeight() {
        return getHeight();
    }


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
