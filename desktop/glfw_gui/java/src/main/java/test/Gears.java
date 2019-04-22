package test;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import org.mini.gl.GL;
import static org.mini.gl.GL.GL_AMBIENT_AND_DIFFUSE;
import static org.mini.gl.GL.GL_COLOR_BUFFER_BIT;
import static org.mini.gl.GL.GL_COMPILE;
import static org.mini.gl.GL.GL_CULL_FACE;
import static org.mini.gl.GL.GL_DEPTH_BUFFER_BIT;
import static org.mini.gl.GL.GL_DEPTH_TEST;
import static org.mini.gl.GL.GL_FRONT;
import static org.mini.gl.GL.GL_LIGHT0;
import static org.mini.gl.GL.GL_LIGHTING;
import static org.mini.gl.GL.GL_MODELVIEW;
import static org.mini.gl.GL.GL_NORMALIZE;
import static org.mini.gl.GL.GL_POSITION;
import static org.mini.gl.GL.GL_PROJECTION;
import static org.mini.gl.GL.GL_QUADS;
import static org.mini.gl.GL.GL_QUAD_STRIP;
import static org.mini.gl.GL.GL_SMOOTH;
import org.mini.glfw.Glfw;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author gust
 */
public class Gears {

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
            keyp(window, key, scancode, action, mods);
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
            reshape(window, x, y);
        }
    }

    void gear(float inner_radius, float outer_radius, float width,
            int teeth, float tooth_depth) {
        int i;
        float r0, r1, r2;
        float angle, da;
        float u, v, len;

        r0 = inner_radius;
        r1 = outer_radius - tooth_depth / 2.f;
        r2 = outer_radius + tooth_depth / 2.f;

        da = 2.f * (float) Math.PI / teeth / 4.f;

        GL.glShadeModel(GL.GL_FLAT);

        GL.glNormal3f(0.f, 0.f, 1.f);

        /* draw front face */
        GL.glBegin(GL_QUAD_STRIP);
        for (i = 0; i <= teeth; i++) {
            angle = i * 2.f * (float) Math.PI / teeth;
            GL.glVertex3f(r0 * (float) cos(angle), r0 * (float) sin(angle), width * 0.5f);
            GL.glVertex3f(r1 * (float) cos(angle), r1 * (float) sin(angle), width * 0.5f);
            if (i < teeth) {
                GL.glVertex3f(r0 * (float) cos(angle), r0 * (float) sin(angle), width * 0.5f);
                GL.glVertex3f(r1 * (float) cos(angle + 3 * da), r1 * (float) sin(angle + 3 * da), width * 0.5f);
            }
        }
        GL.glEnd();

        /* draw front sides of teeth */
        GL.glBegin(GL_QUADS);
        da = 2.f * (float) Math.PI / teeth / 4.f;
        for (i = 0; i < teeth; i++) {
            angle = i * 2.f * (float) Math.PI / teeth;

            GL.glVertex3f(r1 * (float) cos(angle), r1 * (float) sin(angle), width * 0.5f);
            GL.glVertex3f(r2 * (float) cos(angle + da), r2 * (float) sin(angle + da), width * 0.5f);
            GL.glVertex3f(r2 * (float) cos(angle + 2 * da), r2 * (float) sin(angle + 2 * da), width * 0.5f);
            GL.glVertex3f(r1 * (float) cos(angle + 3 * da), r1 * (float) sin(angle + 3 * da), width * 0.5f);
        }
        GL.glEnd();

        GL.glNormal3f(0.0f, 0.0f, -1.0f);

        /* draw back face */
        GL.glBegin(GL_QUAD_STRIP);
        for (i = 0; i <= teeth; i++) {
            angle = i * 2.f * (float) Math.PI / teeth;
            GL.glVertex3f(r1 * (float) cos(angle), r1 * (float) sin(angle), -width * 0.5f);
            GL.glVertex3f(r0 * (float) cos(angle), r0 * (float) sin(angle), -width * 0.5f);
            if (i < teeth) {
                GL.glVertex3f(r1 * (float) cos(angle + 3 * da), r1 * (float) sin(angle + 3 * da), -width * 0.5f);
                GL.glVertex3f(r0 * (float) cos(angle), r0 * (float) sin(angle), -width * 0.5f);
            }
        }
        GL.glEnd();

        /* draw back sides of teeth */
        GL.glBegin(GL_QUADS);
        da = 2.f * (float) Math.PI / teeth / 4.f;
        for (i = 0; i < teeth; i++) {
            angle = i * 2.f * (float) Math.PI / teeth;

            GL.glVertex3f(r1 * (float) cos(angle + 3 * da), r1 * (float) sin(angle + 3 * da), -width * 0.5f);
            GL.glVertex3f(r2 * (float) cos(angle + 2 * da), r2 * (float) sin(angle + 2 * da), -width * 0.5f);
            GL.glVertex3f(r2 * (float) cos(angle + da), r2 * (float) sin(angle + da), -width * 0.5f);
            GL.glVertex3f(r1 * (float) cos(angle), r1 * (float) sin(angle), -width * 0.5f);
        }
        GL.glEnd();

        /* draw outward faces of teeth */
        GL.glBegin(GL_QUAD_STRIP);
        for (i = 0; i < teeth; i++) {
            angle = i * 2.f * (float) Math.PI / teeth;

            GL.glVertex3f(r1 * (float) cos(angle), r1 * (float) sin(angle), width * 0.5f);
            GL.glVertex3f(r1 * (float) cos(angle), r1 * (float) sin(angle), -width * 0.5f);
            u = r2 * (float) cos(angle + da) - r1 * (float) cos(angle);
            v = r2 * (float) sin(angle + da) - r1 * (float) sin(angle);
            len = (float) sqrt(u * u + v * v);
            u /= len;
            v /= len;
            GL.glNormal3f(v, -u, 0.0f);
            GL.glVertex3f(r2 * (float) cos(angle + da), r2 * (float) sin(angle + da), width * 0.5f);
            GL.glVertex3f(r2 * (float) cos(angle + da), r2 * (float) sin(angle + da), -width * 0.5f);
            GL.glNormal3f((float) cos(angle), (float) sin(angle), 0.f);
            GL.glVertex3f(r2 * (float) cos(angle + 2 * da), r2 * (float) sin(angle + 2 * da), width * 0.5f);
            GL.glVertex3f(r2 * (float) cos(angle + 2 * da), r2 * (float) sin(angle + 2 * da), -width * 0.5f);
            u = r1 * (float) cos(angle + 3 * da) - r2 * (float) cos(angle + 2 * da);
            v = r1 * (float) sin(angle + 3 * da) - r2 * (float) sin(angle + 2 * da);
            GL.glNormal3f(v, -u, 0.f);
            GL.glVertex3f(r1 * (float) cos(angle + 3 * da), r1 * (float) sin(angle + 3 * da), width * 0.5f);
            GL.glVertex3f(r1 * (float) cos(angle + 3 * da), r1 * (float) sin(angle + 3 * da), -width * 0.5f);
            GL.glNormal3f((float) cos(angle), (float) sin(angle), 0.f);
        }

        GL.glVertex3f(r1 * (float) cos(0), r1 * (float) sin(0), width * 0.5f);
        GL.glVertex3f(r1 * (float) cos(0), r1 * (float) sin(0), -width * 0.5f);

        GL.glEnd();

        GL.glShadeModel(GL_SMOOTH);

        /* draw inside radius cylinder */
        GL.glBegin(GL_QUAD_STRIP);
        for (i = 0; i <= teeth; i++) {
            angle = i * 2.f * (float) Math.PI / teeth;
            GL.glNormal3f(-(float) cos(angle), -(float) sin(angle), 0.f);
            GL.glVertex3f(r0 * (float) cos(angle), r0 * (float) sin(angle), -width * 0.5f);
            GL.glVertex3f(r0 * (float) cos(angle), r0 * (float) sin(angle), width * 0.5f);
        }
        GL.glEnd();

    }

    static float view_rotx = 20.f, view_roty = 30.f, view_rotz = 0.f;
    static int gear1, gear2, gear3;
    static float angle = 0.f;

    /* OpenGL draw function & timing */
    void draw() {
        GL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        GL.glPushMatrix();
        GL.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
        GL.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
        GL.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);

        GL.glPushMatrix();
        GL.glTranslatef(-3.0f, -2.0f, 0.0f);
        GL.glRotatef(angle, 0.0f, 0.0f, 1.0f);
        GL.glCallList(gear1);
        GL.glPopMatrix();

        GL.glPushMatrix();
        GL.glTranslatef(3.1f, -2.f, 0.f);
        GL.glRotatef(-2.f * angle - 9.f, 0.f, 0.f, 1.f);
        GL.glCallList(gear2);
        GL.glPopMatrix();

        GL.glPushMatrix();
        GL.glTranslatef(-3.1f, 4.2f, 0.f);
        GL.glRotatef(-2.f * angle - 25.f, 0.f, 0.f, 1.f);
        GL.glCallList(gear3);
        GL.glPopMatrix();

        GL.glPopMatrix();
        //ball.drawBall();
    }


    /* update animation parameters */
    void animate() {
        angle = 100.f * (float) Glfw.glfwGetTime();
    }


    /* change view angle, exit upon ESC */
    void keyp(long window, int k, int s, int action, int mods) {
        if (action != Glfw.GLFW_PRESS) {
            return;
        }

        switch (k) {
            case Glfw.GLFW_KEY_Z:
                if ((mods & Glfw.GLFW_MOD_SHIFT) != 0) {
                    view_rotz -= 5.0;
                } else {
                    view_rotz += 5.0;
                }
                break;
            case Glfw.GLFW_KEY_ESCAPE:
                Glfw.glfwSetWindowShouldClose(window, Glfw.GLFW_TRUE);
                break;
            case Glfw.GLFW_KEY_UP:
                view_rotx += 5.0;
                break;
            case Glfw.GLFW_KEY_DOWN:
                view_rotx -= 5.0;
                break;
            case Glfw.GLFW_KEY_LEFT:
                view_roty += 5.0;
                break;
            case Glfw.GLFW_KEY_RIGHT:
                view_roty -= 5.0;
                break;
            default:
                return;
        }
    }


    /* new window size */
    void reshape(long window, int width, int height) {
        System.out.println("reshape");
        float h = (float) height / (float) width;
        float xmax, znear, zfar;

        znear = 5.0f;
        zfar = 30.0f;
        xmax = znear * 0.5f;

        GL.glViewport(0, 0, (int) width, (int) height);
        GL.glMatrixMode(GL_PROJECTION);
        GL.glLoadIdentity();
        GL.glFrustum(-xmax, xmax, -xmax * h, xmax * h, znear, zfar);
        GL.glMatrixMode(GL_MODELVIEW);
        GL.glLoadIdentity();
        GL.glTranslatef(0.0f, 0.0f, -20.0f);
    }

    float[] pos = new float[]{5.f, 5.f, 10.f, 0.f};
    float[] red = new float[]{0.8f, 0.1f, 0.f, 1.f};
    float[] green = new float[]{0.f, 0.8f, 0.2f, 1.f};
    float[] blue = new float[]{0.2f, 0.2f, 1.f, 1.f};

    /* program & OpenGL initialization */
    void init() {

        GL.glLightfv(GL_LIGHT0, GL_POSITION, pos, 0);
        GL.glEnable(GL_CULL_FACE);
        GL.glEnable(GL_LIGHTING);
        GL.glEnable(GL_LIGHT0);
        GL.glEnable(GL_DEPTH_TEST);

        /* make the gears */
        gear1 = GL.glGenLists(1);
        GL.glNewList(gear1, GL_COMPILE);
        GL.glMaterialfv(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, red, 0);
        gear(1.f, 4.f, 1.f, 20, 0.7f);
        GL.glEndList();

        gear2 = GL.glGenLists(1);
        GL.glNewList(gear2, GL_COMPILE);
        GL.glMaterialfv(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, green, 0);
        gear(0.5f, 2.f, 2.f, 10, 0.7f);
        GL.glEndList();

        gear3 = GL.glGenLists(1);
        GL.glNewList(gear3, GL_COMPILE);
        GL.glMaterialfv(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, blue, 0);
        gear(1.3f, 2.f, 0.5f, 10, 0.7f);
        GL.glEndList();

        GL.glEnable(GL_NORMALIZE);
    }

    void t1() {
        Glfw.glfwInit();
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MAJOR, 2);
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MINOR, 0);
        Glfw.glfwWindowHint(Glfw.GLFW_DEPTH_BITS, 16);
        Glfw.glfwWindowHint(Glfw.GLFW_TRANSPARENT_FRAMEBUFFER, Glfw.GLFW_TRUE);
        long win = Glfw.glfwCreateWindow(300, 300, "hello glfw".getBytes(), 0, 0);
        if (win != 0) {
            Glfw.glfwSetCallback(win, new CallBack());
            Glfw.glfwMakeContextCurrent(win);
//            Glfw.glfwSwapInterval(1);

            int w = Glfw.glfwGetFramebufferWidth(win);
            int h = Glfw.glfwGetFramebufferHeight(win);
            System.out.println("w=" + w + "  ,h=" + h);
            reshape(win, w, h);

            init();

            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!Glfw.glfwWindowShouldClose(win)) {

                // Draw gears
                draw();
                

                // Update animation
                animate();

                Glfw.glfwPollEvents();
                Glfw.glfwSwapBuffers(win);

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
        Gears gt = new Gears();
        gt.t1();

    }
}
