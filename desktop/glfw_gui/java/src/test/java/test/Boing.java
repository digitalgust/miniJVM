package test;

import static java.lang.Math.atan2;
import java.util.Random;
import org.mini.gl.GL;
import org.mini.gl.GLMath;
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
public class Boing {

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
            if (button != Glfw.GLFW_MOUSE_BUTTON_LEFT) {
                return;
            }

            if (pressed) {
                override_pos = true;
                set_ball_pos(cursor_x, cursor_y);
            } else {
                override_pos = false;
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

    /**
     * ***************************************************************************
     * Draw a faceted latitude band of the Boing ball.
     *
     * Parms: long_lo, long_hi Low and high longitudes of slice, resp.
     * ***************************************************************************
     */
    /**
     * ***************************************************************************
     * Convert a degree (360-based) into a radian. 360' = 2 * PI
     * ***************************************************************************
     */
    double deg2rad(double deg) {
        return deg / 360 * (2 * Math.PI);
    }

    /**
     * ***************************************************************************
     * 360' sin().
     * ***************************************************************************
     */
    double sin_deg(double deg) {
        return Math.sin(deg2rad(deg));
    }

    /**
     * ***************************************************************************
     * 360' cos().
     * ***************************************************************************
     */
    double cos_deg(double deg) {
        return Math.cos(deg2rad(deg));
    }

    /**
     * ***************************************************************************
     * Compute a cross product (for a normal vector).
     *
     * c = a x b
     ****************************************************************************
     */
    void CrossProduct(float[] a, float[] b, float[] c, float[] n) {
        float u1, u2, u3;
        float v1, v2, v3;

        u1 = b[x] - a[x];
        u2 = b[y] - a[y];
        u3 = b[y] - a[z];

        v1 = c[x] - a[x];
        v2 = c[y] - a[y];
        v3 = c[z] - a[z];

        n[x] = u2 * v3 - v2 * v3;
        n[y] = u3 * v1 - v3 * u1;
        n[z] = u1 * v2 - v1 * u2;
    }

    void set_ball_pos(float x, float y) {
        ball_x = (width / 2) - x;
        ball_y = y - (height / 2);
    }
    boolean colorToggle = false;
    final int DRAW_BALL = 0, DRAW_BALL_SHADOW = 1;
    int drawBallHow;
    final float RADIUS = 70.f;
    final float STEP_LONGITUDE = 22.5f;
    final float STEP_LATITUDE = 22.5f;
    final float DIST_BALL = (RADIUS * 2.f + RADIUS * 0.1f);
    final float VIEW_SCENE_DIST = (DIST_BALL * 3.f + 200.f);/* distance from viewer to middle of boing area */
    final float GRID_SIZE = (RADIUS * 4.5f);
    /* length (width) of grid */
    final float BOUNCE_HEIGHT = (RADIUS * 2.1f);
    final float BOUNCE_WIDTH = (RADIUS * 2.1f);
    final float SHADOW_OFFSET_X = -20.f;
    final float SHADOW_OFFSET_Y = 10.f;
    final float SHADOW_OFFSET_Z = 0.f;
    final float WALL_L_OFFSET = 0.f;
    final float WALL_R_OFFSET = 5.f;
    /* Animation speed (50.0 mimics the original GLUT demo speed) */
    final float ANIMATION_SPEED = 50.f;
    /* Maximum allowed delta time per physics iteration */
    final float MAX_DELTA_T = 0.02f;
    int x = 0, y = 1, z = 2;

    void DrawBoingBallBand(float long_lo,
            float long_hi) {
        float[] vert_ne = new float[3];
        /* "ne" means south-east, so on */
        float[] vert_nw = new float[3];
        float[] vert_sw = new float[3];
        float[] vert_se = new float[3];
        float[] vert_norm = new float[3];
        float lat_deg;

        /*
   * Iterate thru the points of a latitude circle.
   * A latitude circle is a 2D set of X,Z points.
         */
        for (lat_deg = 0;
                lat_deg <= (360 - STEP_LATITUDE);
                lat_deg += STEP_LATITUDE) {
            /*
      * Color this polygon with red or white.
             */
            if (colorToggle) {
                GL.glColor3f(0.8f, 0.1f, 0.1f);
            } else {
                GL.glColor3f(0.95f, 0.95f, 0.95f);
            }

            colorToggle = !colorToggle;

            /*
      * Change color if drawing shadow.
             */
            if (drawBallHow == DRAW_BALL_SHADOW) {
                GL.glColor3f(0.35f, 0.35f, 0.35f);
            }

            /*
      * Assign each Y.
             */
            vert_ne[y] = vert_nw[y] = (float) cos_deg(long_hi) * RADIUS;
            vert_sw[y] = vert_se[y] = (float) cos_deg(long_lo) * RADIUS;

            /*
      * Assign each X,Z with sin,cos values scaled by latitude radius indexed by longitude.
      * Eg, long=0 and long=180 are at the poles, so zero scale is sin(longitude),
      * while long=90 (sin(90)=1) is at equator.
             */
            vert_ne[x] = (float) cos_deg(lat_deg) * (RADIUS * (float) sin_deg(long_lo + STEP_LONGITUDE));
            vert_se[x] = (float) cos_deg(lat_deg) * (RADIUS * (float) sin_deg(long_lo));
            vert_nw[x] = (float) cos_deg(lat_deg + STEP_LATITUDE) * (RADIUS * (float) sin_deg(long_lo + STEP_LONGITUDE));
            vert_sw[x] = (float) cos_deg(lat_deg + STEP_LATITUDE) * (RADIUS * (float) sin_deg(long_lo));

            vert_ne[z] = (float) sin_deg(lat_deg) * (RADIUS * (float) sin_deg(long_lo + STEP_LONGITUDE));
            vert_se[z] = (float) sin_deg(lat_deg) * (RADIUS * (float) sin_deg(long_lo));
            vert_nw[z] = (float) sin_deg(lat_deg + STEP_LATITUDE) * (RADIUS * (float) sin_deg(long_lo + STEP_LONGITUDE));
            vert_sw[z] = (float) sin_deg(lat_deg + STEP_LATITUDE) * (RADIUS * (float) sin_deg(long_lo));

            /*
      * Draw the facet.
             */
            GL.glBegin(GL.GL_POLYGON);

            CrossProduct(vert_ne, vert_nw, vert_sw, vert_norm);
            GL.glNormal3f(vert_norm[x], vert_norm[y], vert_norm[z]);

            GL.glVertex3f(vert_ne[x], vert_ne[y], vert_ne[z]);
            GL.glVertex3f(vert_nw[x], vert_nw[y], vert_nw[z]);
            GL.glVertex3f(vert_sw[x], vert_sw[y], vert_sw[z]);
            GL.glVertex3f(vert_se[x], vert_se[y], vert_se[z]);
            GL.glEnd();

        }
    }

    /* Global vars */
    int windowed_xpos, windowed_ypos, windowed_width, windowed_height;
    int width, height;
    float deg_rot_y = 0.f;
    float deg_rot_y_inc = 2.f;
    boolean override_pos = false;
    float cursor_x = 0.f;
    float cursor_y = 0.f;
    float ball_x = -RADIUS;
    float ball_y = -RADIUS;
    float ball_x_inc = 1.f;
    float ball_y_inc = 2.f;
    double t;
    double t_old = 0.f;
    double dt;

    float TruncateDeg(float deg) {
        if (deg >= 360.f) {
            return (deg - 360.f);
        } else {
            return deg;
        }
    }
    Random rand = new Random();

    /**
     * ***************************************************************************
     * Bounce the ball.
     * ***************************************************************************
     */
    void BounceBall(double delta_t) {
        float sign;
        float deg;

        if (override_pos) {
            return;
        }

        /* Bounce on walls */
        if (ball_x > (BOUNCE_WIDTH / 2 + WALL_R_OFFSET)) {
            ball_x_inc = -0.5f - 0.75f * rand.nextFloat();
            deg_rot_y_inc = -deg_rot_y_inc;
        }
        if (ball_x < -(BOUNCE_HEIGHT / 2 + WALL_L_OFFSET)) {
            ball_x_inc = 0.5f + 0.75f * rand.nextFloat();
            deg_rot_y_inc = -deg_rot_y_inc;
        }

        /* Bounce on floor / roof */
        if (ball_y > BOUNCE_HEIGHT / 2) {
            ball_y_inc = -0.75f - 1.f * rand.nextFloat();
        }
        if (ball_y < -BOUNCE_HEIGHT / 2 * 0.85) {
            ball_y_inc = 0.75f + 1.f * rand.nextFloat();
        }

        /* Update ball position */
        ball_x += ball_x_inc * ((float) delta_t * ANIMATION_SPEED);
        ball_y += ball_y_inc * ((float) delta_t * ANIMATION_SPEED);

        /*
   * Simulate the effects of gravity on Y movement.
         */
        if (ball_y_inc < 0) {
            sign = -1.0f;
        } else {
            sign = 1.0f;
        }

        deg = (ball_y + BOUNCE_HEIGHT / 2) * 90 / BOUNCE_HEIGHT;
        if (deg > 80) {
            deg = 80;
        }
        if (deg < 10) {
            deg = 10;
        }

        ball_y_inc = sign * 4.f * (float) sin_deg(deg);
    }

    void DrawBoingBall() {
        float lon_deg;
        /* degree of longitude */
        double dt_total, dt2;

        GL.glPushMatrix();
        GL.glMatrixMode(GL.GL_MODELVIEW);

        /*
   * Another relative Z translation to separate objects.
         */
        GL.glTranslatef(0.0f, 0.0f, DIST_BALL);

        /* Update ball position and rotation (iterate if necessary) */
        dt_total = dt;
        while (dt_total > 0.0) {
            dt2 = dt_total > MAX_DELTA_T ? MAX_DELTA_T : dt_total;
            dt_total -= dt2;
            BounceBall(dt2);
            deg_rot_y = TruncateDeg(deg_rot_y + deg_rot_y_inc * ((float) dt2 * ANIMATION_SPEED));
        }

        /* Set ball position */
        GL.glTranslatef(ball_x, ball_y, 0.0f);

        /*
   * Offset the shadow.
         */
        if (drawBallHow == DRAW_BALL_SHADOW) {
            GL.glTranslatef(SHADOW_OFFSET_X,
                    SHADOW_OFFSET_Y,
                    SHADOW_OFFSET_Z);
        }

        /*
   * Tilt the ball.
         */
        GL.glRotatef(-20.0f, 0.0f, 0.0f, 1.0f);

        /*
   * Continually rotate ball around Y axis.
         */
        GL.glRotatef(deg_rot_y, 0.0f, 1.0f, 0.0f);

        /*
   * Set OpenGL state for Boing ball.
         */
        GL.glCullFace(GL.GL_FRONT);
        GL.glEnable(GL.GL_CULL_FACE);
        GL.glEnable(GL.GL_NORMALIZE);

        /*
   * Build a faceted latitude slice of the Boing ball,
   * stepping same-sized vertical bands of the sphere.
         */
        for (lon_deg = 0;
                lon_deg < 180;
                lon_deg += STEP_LONGITUDE) {
            /*
      * Draw a latitude circle at this longitude.
             */
            DrawBoingBallBand(lon_deg,
                    lon_deg + STEP_LONGITUDE);
        }

        GL.glPopMatrix();
//        System.out.println("error:"+GL.glGetError());

        return;
    }
    

    /**
     * ***************************************************************************
     * reshape()
     * ***************************************************************************
     */
    void reshape(long window, int w, int h) {
        float[] projection = new float[16], view = new float[16];

        GL.glViewport(0, 0, (int) w, (int) h);
        GL.glMatrixMode(GL.GL_PROJECTION);
        GLMath.mat4x4_perspective(projection,
                2.f * (float) atan2(RADIUS, 200.f),
                (float) w / (float) h,
                1.f, VIEW_SCENE_DIST);
        GL.glLoadMatrixf(projection, 0);

        GL.glMatrixMode(GL.GL_MODELVIEW);
        {
            float[] eye = {0.f, 0.f, VIEW_SCENE_DIST};
            float[] center = {0.f, 0.f, 0.f};
            float[] up = {0.f, -1.f, 0.f};
            GLMath.mat4x4_look_at(view, eye, center, up);
        }
        GL.glLoadMatrixf(view, 0);
    }

    /**
     * ***************************************************************************
     * init()
     * ***************************************************************************
     */
    void init() {
        /*
    * Clear background.
         */
        GL.glClearColor(0.55f, 0.55f, 0.55f, 0.f);

        GL.glShadeModel(GL.GL_SMOOTH);
    }

    void display() {

        GL.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GL.glPushMatrix();

        drawBallHow = DRAW_BALL_SHADOW;
        DrawBoingBall();

        //GL.DrawGrid();
        drawBallHow = DRAW_BALL;
        DrawBoingBall();
        
        GL.glFlush();
    }

    void t1() {
        Glfw.glfwInit();
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MAJOR, 2);
//        Glfw.glfwWindowHint(Glfw.GLFW_CONTEXT_VERSION_MINOR, 0);
//        Glfw.glfwWindowHint(Glfw.GLFW_DEPTH_BITS, 16);
//        Glfw.glfwWindowHint(Glfw.GLFW_TRANSPARENT_FRAMEBUFFER, Glfw.GLFW_TRUE);
        long win = Glfw.glfwCreateWindow(400, 400, "hello glfw".getBytes(), 0, 0);
        if (win != 0) {
            Glfw.glfwSetCallback(win, new CallBack());
            Glfw.glfwMakeContextCurrent(win);
            Glfw.glfwSetWindowAspectRatio(win, 1, 1);
//            Glfw.glfwSwapInterval(1);

            width = Glfw.glfwGetFramebufferWidth(win);
            height = Glfw.glfwGetFramebufferHeight(win);
            System.out.println("w=" + width + "  ,h=" + height);
            reshape(win, width, height);

            Glfw.glfwSetTime(0.0);

            init();
            long last = System.currentTimeMillis(), now;
            int count = 0;
            while (!Glfw.glfwWindowShouldClose(win)) {
                t = Glfw.glfwGetTime();
                dt = t - t_old;
                t_old = t;
                display();

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
        Boing gt = new Boing();
        gt.t1();

    }
}
