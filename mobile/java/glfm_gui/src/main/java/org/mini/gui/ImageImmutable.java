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
 * <p>
 * 装入图片文件或使用纹理图片生成一个GImage对象 load image or generate nvg_texture GImage
 * 注意 这不是GL的纹理ID
 * it's not the OPENGL textureid.
 *
 * @author gust
 */
public class ImageImmutable extends GImage {

    int nvg_texture = -1;
    int[] w = {0};
    int[] h = {0};
    //
    byte[] data; //source from image data
    int gl_texture = -1; //source from gl texture id
    int image_init_flag;
    //
    Object attachment;


    ImageImmutable() {
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


    public int getWidth() {
        return w[0];
    }

    public int getHeight() {
        return h[0];
    }


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
            System.out.println("finalize image " + this);
        } catch (Throwable e) {
        }
    }

    @Override
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public <T extends Object> T getAttachment() {
        return (T) attachment;
    }
}
