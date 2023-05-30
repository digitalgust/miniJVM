/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import static org.mini.gl.GL.GL_QUADS;
import static org.mini.gl.GL.glBegin;
import static org.mini.gl.GL.glEnd;
import static org.mini.gl.GL.glNormal3f;
import static org.mini.gl.GL.glPopMatrix;
import static org.mini.gl.GL.glPushMatrix;
import static org.mini.gl.GL.glTranslatef;
import static org.mini.gl.GL.glVertex3f;

/**
 *
 * @author gust
 */
public class Cube {

    float x, y, z;

    public Cube(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void draw() {
        glBegin(GL_QUADS);    //顶面
        glNormal3f(0.0f, 1.0f, 0.0f);
        glVertex3f(x, y, z);
        glVertex3f(x, y, -z);
        glVertex3f(-x, y, -z);
        glVertex3f(-x, y, z);
        glEnd();
        glBegin(GL_QUADS);    //底面
        glNormal3f(0.0f, -1.0f, 0.0f);
        glVertex3f(x, -y, z);
        glVertex3f(-x, -y, z);
        glVertex3f(-x, -y, -z);
        glVertex3f(x, -y, -z);
        glEnd();
        glBegin(GL_QUADS);    //前面
        glNormal3f(0.0f, 0.0f, 1.0f);
        glVertex3f(x, y, z);
        glVertex3f(-x, y, z);
        glVertex3f(-x, -y, z);
        glVertex3f(x, -y, z);
        glEnd();
        glBegin(GL_QUADS);    //背面
        glNormal3f(0.0f, 0.0f, -1.0f);
        glVertex3f(x, y, -z);
        glVertex3f(x, -y, -z);
        glVertex3f(-x, -y, -z);
        glVertex3f(-x, y, -z);
        glEnd();
        glBegin(GL_QUADS);    //左面
        glNormal3f(-1.0f, 0.0f, 0.0f);
        glVertex3f(-x, y, z);
        glVertex3f(-x, y, -z);
        glVertex3f(-x, -y, -z);
        glVertex3f(-x, -y, z);
        glEnd();
        glBegin(GL_QUADS);    //右面
        glNormal3f(1.0f, 0.0f, 0.0f);
        glVertex3f(x, y, z);
        glVertex3f(x, -y, z);
        glVertex3f(x, -y, -z);
        glVertex3f(x, y, -z);
        glEnd();
    }
}
