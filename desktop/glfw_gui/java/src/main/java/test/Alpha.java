package test;

import org.mini.gl.GL;
import org.mini.glfw.Glfw;

import static org.mini.gl.GL.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author gust
 */
public class Alpha {

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

    int w, h;

    void t1() {
        Glfw.glfwInit();
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MAJOR, 3);
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MINOR, 3);
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

            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!Glfw.glfwWindowShouldClose(win)) {
                int sleep = 100;

                draw2();

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
        Alpha gt = new Alpha();
        gt.t1();

    }
    //===========================================================
    //===========================================================
    //===========================================================

    static float light_position[] = {0.0f, 0.0f, 0.0f, 1.0f};
    static float light_ambient[] = {0.0f, 0.0f, 0.0f, 1.0f};
    static float light_diffuse[] = {1.0f, 1.0f, 1.0f, 1.0f};
    static float light_specular[] = {1.0f, 1.0f, 1.0f, 1.0f};

    void setLight() {

        glLightfv(GL_LIGHT0, GL_POSITION, light_position, 0);
        glLightfv(GL_LIGHT0, GL_AMBIENT, light_ambient, 0);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, light_diffuse, 0);
        glLightfv(GL_LIGHT0, GL_SPECULAR, light_specular, 0);

        glEnable(GL_LIGHT0);
        glEnable(GL_LIGHTING);
        glEnable(GL_DEPTH_TEST);
    }
    
    static float red_color[] = {1.0f, 0.0f, 0.0f, 1.0f};
    static float green_color[] = {0.0f, 1.0f, 0.0f, 0.3333f};
    static float blue_color[] = {0.0f, 0.0f, 1.0f, 0.5f};

    static float mat_specular[] = {0.3f, 0.3f, 0.3f, 1.0f};
    static float mat_emission[] = {0.3f, 0.3f, 0.3f, 1.0f};

    void setMatirial(float[] mat_diffuse, float mat_shininess) {

        glMaterialfv(GL.GL_FRONT, GL_AMBIENT_AND_DIFFUSE, mat_diffuse, 0);
        glMaterialfv(GL.GL_FRONT, GL_SPECULAR, mat_specular, 0);
        glMaterialfv(GL.GL_FRONT, GL_EMISSION, mat_emission, 0);
        glMaterialf(GL.GL_FRONT, GL_SHININESS, mat_shininess);
    }

    Ball red = new Ball(.3f, 8, Ball.SOLID);
    Ball blue = new Ball(.1f, 8, Ball.SOLID);
    Ball green = new Ball(0.05f, 8, Ball.SOLID);

    float cube_angel = 0.f;
    Cube cube = new Cube(0.1f, 0.1f, 0.1f);
    static float angle = 0.f;

    void draw2() {
        // 定义一些材质颜色

        // 清除屏幕
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 创建透视效果视图  
        GL.glMatrixMode(GL.GL_PROJECTION);//对投影矩阵操作  
        GL.glLoadIdentity();//将坐标原点移到中心  
        gluPerspective(20.0f, 1.0f, 1.0f, 20.0f);//设置透视投影矩阵
        GL.glMatrixMode(GL.GL_MODELVIEW);//对模型视景矩阵操作  
        GL.glLoadIdentity();
        gluLookAt(0.0, 0.0, -5.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);//视点转换

        
        // 启动混合并设置混合因子
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL.glShadeModel(GL_SMOOTH);
        // 设置光源
        setLight();

        // 以(0, 0, 0.5)为中心，绘制一个半径为.3的不透明红色球体（离观察者最远）
        setMatirial(red_color, 30.0f);
        glTranslatef(0.0f, 0.0f, 0.5f);
//    glutSolidSphere(0.3, 30, 30);
        red.draw();

        // 下面将绘制半透明物体了，因此将深度缓冲设置为只读
        glDepthMask(GL_FALSE);

        // 以(0.2, 0, -0.5)为中心，绘制一个半径为.2的半透明蓝色球体（离观察者最近）
        setMatirial(blue_color, 30.0f);
        glTranslatef(0.2f, 0.0f, -0.5f);
//    glutSolidSphere(0.2, 30, 30);
        blue.draw();

        // 以(0.1, 0, 0)为中心，绘制一个半径为.15的半透明绿色球体（在前两个球体之间）
        setMatirial(green_color, 30.0f);
        glTranslatef(0.1f, 0, 0);
//    glutSolidSphere(0.15, 30, 30);
        green.draw();

        // 完成半透明物体的绘制，将深度缓冲区恢复为可读可写的形式
        glDepthMask(GL_TRUE);

        setMatirial(red_color, 30.0f);
        glTranslatef(-.5f, -0.5f, 0.5f);
        if (rotate) {
            cube_angel += 10f;     //递增旋转角度计数器
        }
        if (cube_angel >= 360.0f) //如果已旋转一周,复位计数器
        {
            cube_angel = 0.0f;
        }
        GL.glRotatef(cube_angel, 0.3f, 0.3f, 0.3f);
        cube.draw();

    }

    static public void gluPerspective(double fov, double aspectRatio, double zNear, double zFar) {
        // 使用glu库函数，需要添加glu.h头文件
        //gluPerspective( fov, aspectRatio, zNear, zFar );

        // 使用OpenGL函数，但是需要添加math.h头文件
        double rFov = fov * 3.14159265 / 180.0;
        GL.glFrustum(-zNear * Math.tan(rFov / 2.0) * aspectRatio,
                zNear * Math.tan(rFov / 2.0) * aspectRatio,
                -zNear * Math.tan(rFov / 2.0),
                zNear * Math.tan(rFov / 2.0),
                zNear, zFar);
    }

    static public void gluLookAt(double eX, double eY, double eZ, double cX, double cY,
                                 double cZ, double upX, double upY, double upZ) {
        // eye and center are points, but up is a vector
        // 1. change center into a vector:
        // glTranslated(-eX, -eY, -eZ);
        cX = cX - eX;
        cY = cY - eY;
        cZ = cZ - eZ;
        // 2. The angle of center on xz plane and x axis
        // i.e. angle to rot so center in the neg. yz plane
        double a = Math.atan(cZ / cX);
        if (cX >= 0) {
            a = a + Math.PI / 2;
        } else {
            a = a - Math.PI / 2;
        }
        // 3. The angle between the center and y axis
        // i.e. angle to rot so center in the negative z axis
        double b = Math.acos(cY / Math.sqrt(cX * cX + cY * cY + cZ * cZ));
        b = b - Math.PI / 2;
        // 4. up rotate around y axis (a) radians
        double upx = upX * Math.cos(a) + upZ * Math.sin(a);
        double upz = -upX * Math.sin(a) + upZ * Math.cos(a);
        upX = upx;
        upZ = upz;
        // 5. up rotate around x axis (b) radians
        double upy = upY * Math.cos(b) - upZ * Math.sin(b);
        upz = upY * Math.sin(b) + upZ * Math.cos(b);
        upY = upy;
        upZ = upz;
        double c = Math.atan(upX / upY);
        if (upY < 0) {
            // 6. the angle between up on xy plane and y axis
            c = c + Math.PI;
        }
        GL.glRotated(Math.toDegrees(c), 0, 0, 1);
        // up in yz plane
        GL.glRotated(Math.toDegrees(b), 1, 0, 0);
        // center in negative z axis
        GL.glRotated(Math.toDegrees(a), 0, 1, 0);
        // center in yz plane
        GL.glTranslated(-eX, -eY, -eZ);
        // eye at the origin
    }

    static public void drawCood() {
        GL.glPushMatrix();
        float len = 1000f;
        GL.glBegin(GL.GL_LINES);
        GL.glColor3f(1.f, 0, 0);
        GL.glVertex3f(0, 0, 0);
        GL.glVertex3f(len, 0, 0);
        GL.glEnd();
        GL.glBegin(GL.GL_LINES);
        GL.glColor3f(0, 1.f, 0);
        GL.glVertex3f(0, 0, 0);
        GL.glVertex3f(0, len, 0);
        GL.glEnd();
        GL.glBegin(GL.GL_LINES);
        GL.glColor3f(0, 0, 1.f);
        GL.glVertex3f(0, 0, 0);
        GL.glVertex3f(0, 0, len);
        GL.glEnd();
        GL.glPopMatrix();
    }
}
