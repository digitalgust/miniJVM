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
import org.mini.util.SysLog;

import static org.mini.gl.GL.*;

/**
 * render to FrameBuffer Object texture
 *
 * @author gust
 */
public class GLFrameBuffer {

    int texture_w;
    int texture_h;
    boolean hasDepth;
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
        boolean hasDepth;

        @Override
        public void run() {
            glDeleteTextures(rendertext.length, rendertext, 0);
            if (hasDepth) {
                glDeleteTextures(renderbuf1.length, renderbuf1, 0);
            }
            glDeleteFramebuffers(fboobj.length, fboobj, 0);
            SysLog.info("delete fbo success");
        }
    }

    public GLFrameBuffer(int w, int h) {
        this(w, h, 2.f);
    }

    public GLFrameBuffer(int w, int h, float scale) {
        this(w, h, scale, true);
    }

    public GLFrameBuffer(int w, int h, float scale, boolean createDepth) {
        texture_w = Math.round(w * scale);
        texture_h = Math.round(h * scale);
        hasDepth = createDepth;
    }

    public void gl_init() {
        // 创建FBO对象
        glGenFramebuffers(fbo.length, fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        // 创建颜色纹理
        glGenTextures(rendertex.length, rendertex, 0);
        glBindTexture(GL_TEXTURE_2D, rendertex[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, texture_w, texture_h, 0, GL_RGBA, GL_UNSIGNED_BYTE, null, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, rendertex[0], 0);
        // 创建深度纹理
        if (hasDepth) {
            int[] depthTexture = depth_stencil_buffer;
            glGenTextures(depthTexture.length, depthTexture, 0);
            glBindTexture(GL_TEXTURE_2D, depthTexture[0]);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, texture_w, texture_h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, null, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture[0], 0);
        }
        //
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            SysLog.error("framebuffer object error.");
        } else {
            SysLog.info("framebuffer object ok");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        fboimg = GImage.createImage(getTexture(), texture_w, texture_h, Nanovg.NVG_IMAGE_FLIPY);

    }

    public int getColorTexture() {
        return rendertex[0];
    }

    public int getDepthTexture() {
        return depth_stencil_buffer[0];
    }

    public boolean hasDepthBuffer() {
        return hasDepth;
    }

    public void delete() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteTextures(rendertex.length, rendertex, 0);
        if (hasDepth) {
            glDeleteTextures(depth_stencil_buffer.length, depth_stencil_buffer, 0);
        }
        glDeleteFramebuffers(fbo.length, fbo, 0);
    }

    @Override
    public void finalize() {
        //Don't reference to this instance
        Cleaner attachment = new Cleaner();
        attachment.rendertext[0] = rendertex[0];
        attachment.renderbuf1[0] = depth_stencil_buffer[0];
        attachment.fboobj[0] = fbo[0];
        attachment.hasDepth = hasDepth;
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

