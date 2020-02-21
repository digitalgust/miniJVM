/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import org.mini.gl.GL;
import org.mini.glfw.Glfw;
import org.mini.nanovg.Gutil;

import static org.mini.gl.GL.*;
import static org.mini.glfw.Glfw.*;
import static test.Alpha.gluLookAt;
import static test.Alpha.gluPerspective;

/**
 *
 * @author gust
 */
public class RenderTexure {

    int w, h;

    public static void main(String[] args) {
        RenderTexure gt = new RenderTexure();
        gt.t1();

    }

    void t1() {
        Glfw.glfwInit();
        
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        long win = Glfw.glfwCreateWindow(500, 500, "hello glfw".getBytes(), 0, 0);
        if (win != 0) {
            Glfw.glfwSetCallback(win, new CallBack());
            Glfw.glfwMakeContextCurrent(win);
            Gutil.printGlVersion();
//            Glfw.glfwSwapInterval(1);

            w = Glfw.glfwGetFramebufferWidth(win);
            h = Glfw.glfwGetFramebufferHeight(win);
            System.out.println("w=" + w + "  ,h=" + h);

            SetupCamera();
            SetupResource();
            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!Glfw.glfwWindowShouldClose(win)) {
                int sleep = 100;

                renderScene();
                //GL.glLoadIdentity();
                //Gutil.drawCood();

                Glfw.glfwPollEvents();
                Glfw.glfwSwapBuffers(win);

                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ex) {
                }
                count++;
                now = System.currentTimeMillis();
                if (now - last > 1000) {
                    System.out.println("fps:" + count);
                    last = now;
                    count = 0;
                }
            }
            removeResource();
            Glfw.glfwTerminate();
        }
    }

    boolean exit = false;
    long curWin;
    int mx, my;
    boolean rotate = true;

    class CallBack extends GlfwCallbackAdapter {

        @Override
        public void key(long window, int key, int scancode, int action, int mods) {
            System.out.println("key:" + key + " action:" + action);
            if (key == Glfw.GLFW_KEY_ESCAPE && action == Glfw.GLFW_PRESS) {
                Glfw.glfwSetWindowShouldClose(window, Glfw.GLFW_TRUE);
            }
            if (key == Glfw.GLFW_KEY_V) {
                if (mods == Glfw.GLFW_MOD_CONTROL) {

                    String string = Glfw.glfwGetClipboardString(window);
                    if (string != null) {
                        System.out.println("Clipboard contains " + string);
                    } else {
                        System.out.println("Clipboard does not contain a string\n");
                    }
                }
            }
        }

        @Override
        public void mouseButton(long window, int button, boolean pressed) {
            if (window == curWin) {
                String bt = button == Glfw.GLFW_MOUSE_BUTTON_LEFT ? "LEFT" : button == Glfw.GLFW_MOUSE_BUTTON_2 ? "RIGHT" : "OTHER";
                String press = pressed ? "pressed" : "released";
                System.out.println(bt + " " + mx + " " + my + "  " + press);
            }
            if (pressed) {
                rotate = false;
            } else {
                rotate = true;
            }
        }

        @Override
        public void drop(long window, int count, String[] paths) {
            for (int i = 0; i < count; i++) {
                System.out.println(i + " " + paths[i]);
            }
        }

        @Override
        public void cursorPos(long window, int x, int y) {
            curWin = window;
            mx = x;
            my = y;
        }

        @Override
        public boolean windowClose(long window) {
            System.out.println("byebye");
            return true;
        }

        @Override
        public void windowSize(long window, int width, int height) {
            System.out.println("resize " + width + " " + height);
        }

        @Override
        public void framebufferSize(long window, int x, int y) {
            GL.glViewport(0, 0, x, y);
        }
    }

    //===========================================================
    //===========================================================
    //===========================================================
    /**
     * **************************************************************************************************
     * 全局变量定义
     * ***************************************************************************************************
     */
    final int TEXTURE_WIDTH = 512;
    final int TEXTURE_HEIGHT = 512;
    final double NEAR_PLANE = 1.0f;
    final double FAR_PLANE = 1000.0f;

    int[] fbo = {0};        // FBO对象的句柄
    int[] render_buffer = {0};
    int[] rendertarget = {0};        // 纹理对象的句柄

    /**
     * **************************************************************************************************
     * 全局函数定义
     * ***************************************************************************************************
     */
// 初始化摄像机
    void SetupCamera() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective(25, (double) w / (double) h, NEAR_PLANE, FAR_PLANE);
        gluLookAt(2, 5, 5, 0, 0, 0, 0, 1, 0);

        // 各种变换应该在GL_MODELVIEW模式下进行
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Z-buffer
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        // 启用2D贴图
        glEnable(GL_TEXTURE_2D);
    }

// 初始化几何形体
    void SetupResource() {
        // 创建FBO对象
        glGenFramebuffers(1, fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

        // 创建纹理
        glGenTextures(1, rendertarget, 0);
        glBindTexture(GL_TEXTURE_2D, rendertarget[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TEXTURE_WIDTH, TEXTURE_HEIGHT, 0, GL_RGB, GL_UNSIGNED_BYTE, null, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, rendertarget[0], 0);

        //创建深度缓冲区
        glGenRenderbuffers(1, render_buffer, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, render_buffer[0]);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, render_buffer[0]); // now actually attach it

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("framebuffer object error.");
        } else {
            System.out.println("framebuffer object ok");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    void removeResource() {
        glDeleteTextures(1, rendertarget, 0);
        glDeleteRenderbuffers(1, render_buffer, 0);
        glDeleteFramebuffers(1, fbo, 0);
    }

// 渲染到窗体
    void Render() {
        SetupCamera();
        // 绑定默认FBO（窗体帧缓冲区的ID是0）
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, rendertarget[0]);
        glViewport(0, 0, w, h);

        // 渲染
        glClearColor(0, 0, 1, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glBegin(GL_POLYGON);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glColor3f(1, 1, 1);

        glTexCoord2f(1, 1);
        glVertex3f(1, 1, 0);

        glTexCoord2f(0, 1);
        glVertex3f(-1, 1, 0);

        glTexCoord2f(0, 0);
        glVertex3f(-1, -1, 0);

        glTexCoord2f(1, 0);
        glVertex3f(1, -1, 0);

        glEnd();
        glBindTexture(GL_TEXTURE_2D, 0); // 取消绑定，因为如果不取消，渲染到纹理的时候会使用纹理本身

//    glutSwapBuffers();
    }
    Light light = new Light();
// 渲染到纹理

    int[] curFrameBuffer = {0};

    void RenderToTarget() {

        glGetIntegerv(GL_FRAMEBUFFER_BINDING, curFrameBuffer, 0);
        glPushAttrib(GL_VIEWPORT_BIT);
        glMatrixMode(GL_PROJECTION);
        GL.glPushMatrix();
        glMatrixMode(GL_MODELVIEW);
        GL.glPushMatrix();
        // 绑定渲染目标
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glViewport(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        light.setCamera();
        light.draw();
//        // 渲染
//        glClearColor(1, 1, 0, 1);
//        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//
//        glBegin(GL_POLYGON);
//
//        glMatrixMode(GL_MODELVIEW);
//        glLoadIdentity();
//
//        glColor4f(1, 0, 0, 1);
//        glVertex3d(0, 1, 0);
//        glVertex3d(-1, -1, 0);
//        glVertex3d(1, -1, 0);
//
//        glEnd();
        glMatrixMode(GL_MODELVIEW);
        GL.glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        GL.glPopMatrix();
        glPopAttrib();
        glBindFramebuffer(GL_FRAMEBUFFER, curFrameBuffer[0]);
    }

    void Clear() {

    }

    void renderScene() {
        RenderToTarget();
        Render();
        //Render1();
    }

}
