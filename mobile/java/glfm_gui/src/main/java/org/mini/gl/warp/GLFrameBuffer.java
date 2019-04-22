/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gl.warp;

import static org.mini.gl.GL.GL_COLOR_ATTACHMENT0;
import static org.mini.gl.GL.GL_DEPTH24_STENCIL8;
import static org.mini.gl.GL.GL_DEPTH_STENCIL_ATTACHMENT;
import static org.mini.gl.GL.GL_FRAMEBUFFER;
import static org.mini.gl.GL.GL_FRAMEBUFFER_BINDING;
import static org.mini.gl.GL.GL_FRAMEBUFFER_COMPLETE;
import static org.mini.gl.GL.GL_LINEAR;
import static org.mini.gl.GL.GL_RENDERBUFFER;
import static org.mini.gl.GL.GL_RGB;
import static org.mini.gl.GL.GL_TEXTURE_2D;
import static org.mini.gl.GL.GL_TEXTURE_MAG_FILTER;
import static org.mini.gl.GL.GL_TEXTURE_MIN_FILTER;
import static org.mini.gl.GL.GL_UNSIGNED_BYTE;
import static org.mini.gl.GL.glBindFramebuffer;
import static org.mini.gl.GL.glBindRenderbuffer;
import static org.mini.gl.GL.glBindTexture;
import static org.mini.gl.GL.glCheckFramebufferStatus;
import static org.mini.gl.GL.glDeleteFramebuffers;
import static org.mini.gl.GL.glDeleteRenderbuffers;
import static org.mini.gl.GL.glDeleteTextures;
import static org.mini.gl.GL.glFramebufferRenderbuffer;
import static org.mini.gl.GL.glFramebufferTexture2D;
import static org.mini.gl.GL.glGenFramebuffers;
import static org.mini.gl.GL.glGenRenderbuffers;
import static org.mini.gl.GL.glGenTextures;
import static org.mini.gl.GL.glGetIntegerv;
import static org.mini.gl.GL.glRenderbufferStorage;
import static org.mini.gl.GL.glTexImage2D;
import static org.mini.gl.GL.glTexParameteri;
import static org.mini.gl.GL.glViewport;

/**
 * render to FrameBuffer Object texture
 *
 * @author gust
 */
public class GLFrameBuffer {

    int texture_w = 512;
    int texture_h = 512;
    int[] fbo = {0};        // FBO对象的句柄
    int[] render_buffer = {0};
    int[] rendertarget = {0};        // 纹理对象的句柄
    int[] curFrameBuffer = {0};

    public GLFrameBuffer(int w, int h) {
        texture_w = w;
        texture_h = h;
//        create();
    }

    // 初始化几何形体
    public void create() {
        // 创建FBO对象
        glGenFramebuffers(1, fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

        // 创建纹理
        glGenTextures(1, rendertarget, 0);
        glBindTexture(GL_TEXTURE_2D, rendertarget[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, texture_w, texture_h, 0, GL_RGB, GL_UNSIGNED_BYTE, null, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, rendertarget[0], 0);

        //创建深度缓冲区
        glGenRenderbuffers(1, render_buffer, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, render_buffer[0]);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, texture_w, texture_h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, render_buffer[0]); // now actually attach it

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("framebuffer object error.");
        } else {
//            System.out.println("framebuffer object ok");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void delete() {
        glDeleteTextures(1, rendertarget, 0);
        glDeleteRenderbuffers(1, render_buffer, 0);
        glDeleteFramebuffers(1, fbo, 0);
    }

    public void render(GLFrameBufferPainter painter) {
        begin();
        painter.paint();
        end();
    }

    public int getTexture() {
        return rendertarget[0];
    }

    public int getWidth() {
        return texture_w;
    }

    public int getHeight() {
        return texture_h;
    }

    void begin() {
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, curFrameBuffer, 0);
//        glPushAttrib(GL_VIEWPORT_BIT);
//        glMatrixMode(GL_PROJECTION);
//        GL.glPushMatrix();
//        glMatrixMode(GL_MODELVIEW);
//        GL.glPushMatrix();
        // 绑定渲染到纹理
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glViewport(0, 0, texture_w, texture_h);
    }

    void end() {
//        glMatrixMode(GL_MODELVIEW);
//        GL.glPopMatrix();
//        glMatrixMode(GL_PROJECTION);
//        GL.glPopMatrix();
//        glPopAttrib();
        glBindFramebuffer(GL_FRAMEBUFFER, curFrameBuffer[0]);
    }
}
