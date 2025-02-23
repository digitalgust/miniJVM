/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.glwrap;

import org.mini.gui.callback.GCmd;
import org.mini.gui.GForm;
import org.mini.gui.GImage;
import org.mini.nanovg.Nanovg;

import static org.mini.gl.GL.*;

/**
 * render to FrameBuffer Object texture
 *
 * @author gust
 */
public class GLFrameBuffer {

    int texture_w;
    int texture_h;
    int[] fbo = {0};        // FBO对象的句柄
    int[] depth_stencil_buffer = {0};
    int[] rendertex = {0};        // 纹理对象的句柄
    int[] curFrameBuffer = {0};
    GImage fboimg;
    public long cost;


    static class Cleaner implements Runnable {
        public int[] rendertext = {0};
        int[] renderbuf1 = {0};
        int[] fboobj = {0};

        @Override
        public void run() {
            glDeleteTextures(rendertext.length, rendertext, 0);
            glDeleteRenderbuffers(renderbuf1.length, renderbuf1, 0);
            glDeleteFramebuffers(fboobj.length, fboobj, 0);
            System.out.println("delete fbo success");
        }
    }

    public GLFrameBuffer(int w, int h) {
        texture_w = w * 2;
        texture_h = h * 2;
    }

    public void gl_init() {
        // 创建FBO对象
        glGenFramebuffers(fbo.length, fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        // 创建纹理
        glGenTextures(rendertex.length, rendertex, 0);
        glBindTexture(GL_TEXTURE_2D, rendertex[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, texture_w, texture_h, 0, GL_RGBA, GL_UNSIGNED_BYTE, null, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, rendertex[0], 0);
        //创建深度缓冲区
        glGenRenderbuffers(depth_stencil_buffer.length, depth_stencil_buffer, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, depth_stencil_buffer[0]);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, texture_w, texture_h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depth_stencil_buffer[0]); // now actually attach it
        //
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("framebuffer object error.");
        } else {
            System.out.println("framebuffer object ok");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        fboimg = GImage.createImage(getTexture(), texture_w, texture_h, Nanovg.NVG_IMAGE_FLIPY);

    }

    public void delete() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteTextures(rendertex.length, rendertex, 0);
        glDeleteRenderbuffers(depth_stencil_buffer.length, depth_stencil_buffer, 0);
        glDeleteFramebuffers(fbo.length, fbo, 0);
    }

    @Override
    public void finalize() {
        //Don't reference to this instance
        Cleaner attachment = new Cleaner();
        attachment.rendertext[0] = rendertex[0];
        attachment.renderbuf1[0] = depth_stencil_buffer[0];
        attachment.fboobj[0] = fbo[0];
        GForm.addCmd(new GCmd(attachment));
    }


    public int getTexture() {
        return rendertex[0];
    }

    public int getTexWidth() {
        return texture_w;
    }

    public int getTexHeight() {
        return texture_h;
    }

    public GImage getFboimg() {
        return fboimg;
    }

    public void begin() {
        cost = System.currentTimeMillis();
        //save current
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, curFrameBuffer, 0);
        // 绑定渲染到纹理
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glViewport(0, 0, (int) texture_w, texture_h);
    }

    public void end() {
        glBindFramebuffer(GL_FRAMEBUFFER, curFrameBuffer[0]);
        cost = System.currentTimeMillis() - cost;
    }
}

