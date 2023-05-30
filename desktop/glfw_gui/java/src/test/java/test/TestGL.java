package test;

import org.mini.gl.GL;
import org.mini.gl.GLMath;
import org.mini.glfw.Glfw;

import static org.mini.gl.GL.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author gust
 */
public class TestGL {

    boolean exit = false;
    long curWin;
    int mx, my;

    class CallBack extends GlfwCallbackAdapter {

        @Override
        public void key(long window, int key, int scancode, int action, int mods) {
            System.out.println("key:" + key + " action:" + action);
            if (key == Glfw.GLFW_KEY_ESCAPE && action == Glfw.GLFW_PRESS) {
                Glfw.glfwSetWindowShouldClose(window, Glfw.GLFW_TRUE);
            }
        }

        @Override
        public void mouseButton(long window, int button, boolean pressed) {
            if (window == curWin) {
                String bt = button == Glfw.GLFW_MOUSE_BUTTON_LEFT ? "LEFT" : button == Glfw.GLFW_MOUSE_BUTTON_2 ? "RIGHT" : "OTHER";
                String press = pressed ? "pressed" : "released";
                System.out.println(bt + " " + mx + " " + my + "  " + press);
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
        }
    }

    byte[] mask = new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, //   这是最下面的一行

            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x03, (byte) 0x80, (byte) 0x01, (byte) 0xC0, //   麻

            (byte) 0x06, (byte) 0xC0, (byte) 0x03, (byte) 0x60, //   烦

            (byte) 0x04, (byte) 0x60, (byte) 0x06, (byte) 0x20, //   的

            (byte) 0x04, (byte) 0x30, (byte) 0x0C, (byte) 0x20, //   初

            (byte) 0x04, (byte) 0x18, (byte) 0x18, (byte) 0x20, //   始

            (byte) 0x04, (byte) 0x0C, (byte) 0x30, (byte) 0x20, //   化

            (byte) 0x04, (byte) 0x06, (byte) 0x60, (byte) 0x20, //   ，

            (byte) 0x44, (byte) 0x03, (byte) 0xC0, (byte) 0x22, //   不

            (byte) 0x44, (byte) 0x01, (byte) 0x80, (byte) 0x22, //   建

            (byte) 0x44, (byte) 0x01, (byte) 0x80, (byte) 0x22, //   议

            (byte) 0x44, (byte) 0x01, (byte) 0x80, (byte) 0x22, //   使

            (byte) 0x44, (byte) 0x01, (byte) 0x80, (byte) 0x22, //   用

            (byte) 0x44, (byte) 0x01, (byte) 0x80, (byte) 0x22,
            (byte) 0x44, (byte) 0x01, (byte) 0x80, (byte) 0x22,
            (byte) 0x66, (byte) 0x01, (byte) 0x80, (byte) 0x66,
            (byte) 0x33, (byte) 0x01, (byte) 0x80, (byte) 0xCC,
            (byte) 0x19, (byte) 0x81, (byte) 0x81, (byte) 0x98,
            (byte) 0x0C, (byte) 0xC1, (byte) 0x83, (byte) 0x30,
            (byte) 0x07, (byte) 0xE1, (byte) 0x87, (byte) 0xE0,
            (byte) 0x03, (byte) 0x3F, (byte) 0xFC, (byte) 0xC0,
            (byte) 0x03, (byte) 0x31, (byte) 0x8C, (byte) 0xC0,
            (byte) 0x03, (byte) 0x3F, (byte) 0xFC, (byte) 0xC0,
            (byte) 0x06, (byte) 0x64, (byte) 0x26, (byte) 0x60,
            (byte) 0x0C, (byte) 0xCC, (byte) 0x33, (byte) 0x30,
            (byte) 0x18, (byte) 0xCC, (byte) 0x33, (byte) 0x18,
            (byte) 0x10, (byte) 0xC4, (byte) 0x23, (byte) 0x08,
            (byte) 0x10, (byte) 0x63, (byte) 0xC6, (byte) 0x08,
            (byte) 0x10, (byte) 0x30, (byte) 0x0C, (byte) 0x08,
            (byte) 0x10, (byte) 0x18, (byte) 0x18, (byte) 0x08,
            (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x08 // 这是最上面的一行
    };

    float Pi = 3.1415926f;

    void draw1() {
        GL.glClear(GL.GL_COLOR_BUFFER_BIT);

        GL.glEnable(GL.GL_POLYGON_STIPPLE);

        GL.glPolygonStipple(mask);

        GL.glRectf(-0.5f, -0.5f, 0.0f, 0.0f);   // 在左下方绘制一个有镂空效果的正方形

        GL.glDisable(GL.GL_POLYGON_STIPPLE);

        GL.glRectf(0.0f, 0.0f, 0.5f, 0.5f);     // 在右上方绘制一个无镂空效果的正方形

        //GL.glShadeModel(GL.GL_FLAT);
        GL.glClear(GL.GL_COLOR_BUFFER_BIT);
        GL.glBegin(GL.GL_TRIANGLE_FAN);
        {
            GL.glColor3f(1.0f, 1.0f, 1.0f);
            GL.glVertex2f(0.0f, 0.0f);
            for (int i = 0; i <= 8; ++i) {
                GL.glColor3f(i & 0x04, i & 0x02, i & 0x01);
                GL.glVertex2f((float) Math.cos(i * Pi / 4), (float) Math.sin(i * Pi / 4));
            }
        }
        GL.glEnd();
    }

    void draw0() {
        GL.glColor3f(0.f, 1.f, 0.f);
        //GL.glRectf(-0.5f, -0.5f, 0.5f, 0.5f);
        GL.glPointSize(5.f);
        GL.glBegin(GL.GL_POINTS);
        {
            GL.glColor3f(1f, 0.f, 0.f);
            GL.glVertex2f(-0.8f, 0.1f);
            GL.glVertex2f(-0.8f, 0.2f);
        }
        GL.glEnd();
        GL.glBegin(GL.GL_POLYGON);
        {
            float R = 0.2f;
            int n = 20;
            for (int i = 0; i < n; ++i) {
                GL.glColor3f(1f / (i + 1), 1.f / (i + 1), 1.f / (i + 1));
                GL.glVertex2f((float) (R * Math.cos(2 * Math.PI / n * i)), (float) (R * Math.sin(2 * Math.PI / n * i)));
            }
        }
        GL.glEnd();
        GL.glLineWidth(15.f);
        float x, factor = 0.1f;
        GL.glBegin(GL.GL_LINES);
        {
            GL.glColor3f(1f, 1.f, 1.f);
            GL.glVertex2f(-1.0f, 0.0f);
            GL.glVertex2f(1.0f, 0.0f);         // 以上两个点可以画x轴
            GL.glVertex2f(0.0f, -1.0f);
            GL.glVertex2f(0.0f, 1.0f);         // 以上两个点可以画y轴
        }
        GL.glEnd();

        GL.glBegin(GL.GL_LINE_STRIP);
        {
            for (x = -1.0f / factor; x < 1.0f / factor; x += 0.01f) {
                GL.glVertex2f(x * factor, (float) Math.sin(x) * factor);
            }
        }
        GL.glEnd();
    }

    int day = 200; // day的变化：从0到359
    int w, h;
    float[] projection = new float[16], view = new float[16];
    Ball sun = new Ball(69600000, 8, Ball.SOLID);
    Ball earth = new Ball(15945000, 8, Ball.WIRE);
    Ball moon = new Ball(4345000, 8, Ball.WIRE);

    void init() {

//        glMatrixMode(GL_PROJECTION);
//        glLoadIdentity();
//        gluPerspective(75, 1, 1, 400000000);
        GL.glViewport(0, 0, (int) w, (int) h);
        GL.glMatrixMode(GL.GL_PROJECTION);
        GLMath.mat4x4_perspective(projection,
                1.5f,
                (float) w / (float) h,
                1.f, 400000000f);
        GL.glLoadMatrixf(projection, 0);
        glMatrixMode(GL_MODELVIEW);

//        glLoadIdentity();
//        gluLookAt(0, -200000000, 200000000, 0, 0, 0, 0, 0, 1);
        GL.glMatrixMode(GL.GL_MODELVIEW);
        {
            float[] eye = {0.f, 0.f, 200000000f};
            float[] center = {0.f, 0.f, 0.f};
            float[] up = {0.f, -1.f, 0.f};
            GLMath.mat4x4_look_at(view, eye, center, up);
        }
        GL.glLoadMatrixf(view, 0);
    }

    float[] sun_light_position = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] sun_light_ambient = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] sun_light_diffuse = {1.0f, 1.0f, 1.0f, 1.0f};
    float[] sun_light_specular = {1.0f, 1.0f, 1.0f, 1.0f};
    //
    float[] sun_mat_ambient = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] sun_mat_diffuse = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] sun_mat_specular = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] sun_mat_emission = {0.5f, 0.0f, 0.0f, 1.0f};
    float sun_mat_shininess = 0.0f;

    void draw2() {
//        glLoadIdentity();
//        glEnable(GL_DEPTH_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // 绘制红色的“太阳”
        // 定义太阳光源，它是一种白色的光源
        {

            glLightfv(GL_LIGHT0, GL_POSITION, sun_light_position, 0);
            glLightfv(GL_LIGHT0, GL_AMBIENT, sun_light_ambient, 0);
            glLightfv(GL_LIGHT0, GL_DIFFUSE, sun_light_diffuse, 0);
            glLightfv(GL_LIGHT0, GL_SPECULAR, sun_light_specular, 0);

            glEnable(GL_LIGHT0);
            glEnable(GL_LIGHTING);
//            glEnable(GL_DEPTH_TEST);
        }
        // 定义太阳的材质并绘制太阳
        {

            glMaterialfv(GL_FRONT, GL_AMBIENT, sun_mat_ambient, 0);
            glMaterialfv(GL_FRONT, GL_DIFFUSE, sun_mat_diffuse, 0);
            glMaterialfv(GL_FRONT, GL_SPECULAR, sun_mat_specular, 0);
            glMaterialfv(GL_FRONT, GL_EMISSION, sun_mat_emission, 0);
            glMaterialf(GL_FRONT, GL_SHININESS, sun_mat_shininess);

        }
//        glutSolidSphere(69600000, 20, 20);
        // 绘制蓝色的“地球”
        sun.draw();
        glColor3f(0.0f, 0.0f, 1.0f);
        GL.glRotatef((float) (day / 360.0 * 360.0), 0.0f, 0.0f, -1.0f);
        glTranslatef(150000000f, 0.0f, 0.0f);
//        glutSolidSphere(15945000, 20, 20);
        earth.draw();
        // 绘制黄色的“月亮”
        glColor3f(1.0f, 1.0f, 0.0f);
        GL.glRotatef((float) (day / 30.0 * 360.0 - day / 360.0 * 360.0), 0.0f, 0.0f, -1.0f);
        glTranslatef(38000000f, 0.0f, 0.0f);
//        glutSolidSphere(4345000, 20, 20);
        moon.draw();
        glEnd();
//        day++;
        glFlush();
    }

    void draw3() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ZERO);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        glBlendFunc(GL_ONE, GL_ONE);

        GL.glColor4f(1, 0, 0, 0.5f);
        GL.glRectf(-1f, -1, 0.5f, 0.5f);
        GL.glColor4f(0, 1, 0, 0.5f);
        GL.glRectf(-0.5f, -0.5f, 1, 1);
    }

    void testNormal() {
        float[] p0 = {1, 1, 0};
        float[] p1 = {0, 0, 0};
        float[] p2 = {-1, 1, 0};

        float[] tmp0 = {0, 0, 0};
        float[] tmp1 = {0, 0, 0};
        float[] tmp2 = {0, 0, 0};
        GLMath.vec_mul_cross(tmp2, GLMath.vec_sub(tmp0, p1, p0), GLMath.vec_sub(tmp1, p2, p1));
        //Gutil.vec_normal(tmp0, tmp2);
        tmp0[0] = tmp2[0];
        tmp0[1] = tmp2[1];
        tmp0[2] = tmp2[2];
        System.out.println(tmp0[0] + "," + tmp0[1] + "," + tmp0[2]);
    }

    int test;

    void t1() {
        Glfw.glfwInit();
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MAJOR, 2);
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MINOR, 0);
//        Glfw.glfwWindowHint(Glfw.GLFW_DEPTH_BITS, 16);
//        Glfw.glfwWindowHint(Glfw.GLFW_TRANSPARENT_FRAMEBUFFER, Glfw.GLFW_TRUE);
        long win = Glfw.glfwCreateWindow(300, 300, "hello glfw".getBytes(), 0, 0);
        if (win != 0) {
            Glfw.glfwSetCallback(win, new CallBack());
            Glfw.glfwMakeContextCurrent(win);
//            Glfw.glfwSwapInterval(1);

            w = Glfw.glfwGetFramebufferWidth(win);
            h = Glfw.glfwGetFramebufferHeight(win);
            System.out.println("w=" + w + "  ,h=" + h);

            test = 0;
            switch (test) {
                case 2:
                    init();
                    break;
            }
            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!Glfw.glfwWindowShouldClose(win)) {

                int sleep = 100;
                switch (test) {
                    case 0:
                        draw0();
                        break;
                    case 1:
                        draw1();
                        break;
                    case 2:
                        draw2();
                        sleep = 1000;
                        break;
                    case 3:
                        draw3();
                        break;
                }

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
            Glfw.glfwTerminate();
        }
    }

    public static void main(String[] args) {
        TestGL gt = new TestGL();
        gt.t1();

    }
}
