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
public class GLShadowMapping {

    int texture_w;
    int texture_h;
    int[] fbo = {0};        // FBO对象的句柄
    int[] rendertex = {0};        // 纹理对象的句柄
    int[] curFrameBuffer = {0};
    GImage fboimg;
    public long cost;


    static class GLShadowMappingCleaner implements Runnable {
        public int[] rendertext = {0};
        int[] fboobj = {0};

        @Override
        public void run() {
            glDeleteTextures(rendertext.length, rendertext, 0);
            glDeleteFramebuffers(fboobj.length, fboobj, 0);
            System.out.println("delete fbo success");
        }
    }

    public GLShadowMapping(int w, int h) {

        texture_w = (int) w;
        texture_h = (int) h;
    }

    // 初始化几何形体
    public void gl_init() {

        // 创建FBO对象
        glGenFramebuffers(fbo.length, fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        // 创建纹理
        glGenTextures(rendertex.length, rendertex, 0);
        glBindTexture(GL_TEXTURE_2D, rendertex[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT16, texture_w, texture_h, 0, GL_DEPTH_COMPONENT,/*ios restrict*/ GL_UNSIGNED_SHORT, null, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, rendertex[0], 0);
        glReadBuffer(GL_NONE);
        glBindTexture(GL_TEXTURE_2D, 0);

        //
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("ShadowMapping object error.");
        } else {
            System.out.println("ShadowMapping object ok");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        fboimg = GImage.createImage(getTexture(), texture_w, texture_h, Nanovg.NVG_IMAGE_FLIPY);

    }

    public void delete() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteTextures(rendertex.length, rendertex, 0);
        glDeleteFramebuffers(fbo.length, fbo, 0);
    }

    public void finalize() {
        //Don't reference to this instance
        GLShadowMappingCleaner attachment = new GLShadowMappingCleaner();
        attachment.rendertext[0] = rendertex[0];
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

    public GImage getImage() {
        return fboimg;
    }

    public void begin() {
        cost = System.currentTimeMillis();
        //save current
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, curFrameBuffer, 0);
        // 绑定渲染到纹理
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glViewport(0, 0, texture_w, texture_h);
        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void end() {
        //restore preview
        glBindFramebuffer(GL_FRAMEBUFFER, curFrameBuffer[0]);
        cost = System.currentTimeMillis() - cost;
    }
}
