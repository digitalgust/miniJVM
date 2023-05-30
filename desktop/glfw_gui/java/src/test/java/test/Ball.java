package test;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import org.mini.gl.GL;
import static org.mini.gl.GL.GL_LINE_LOOP;
import static org.mini.gl.GL.GL_QUADS;
import static org.mini.gl.GL.glBegin;
import static org.mini.gl.GL.glEnd;
import static org.mini.gl.GL.glVertex3f;

class Ball {

    float[][] mx;
    float pi = 3.1415926f;
    public static final int SOLID = 3000;
    public static final int WIRE = 3001;
    final int x = 0, y = 1, z = 2;

    class Point {

        float x;
        float y;
        float z;
    };
    int w, h, mode;

    public Ball(float radius, int slices, int mode) {
        w = 2 * slices;
        h = slices;
        this.mode = mode;
        mx = getPointMatrix(radius, slices);
    }

    void setMode(int mode) {
        this.mode = mode;
    }

    int getPoint(float radius, float a, float b, float[] p) {
        p[x] = (float) (radius * sin(a * pi / 180.0f) * cos(b * pi / 180.0f));
        p[y] = (float) (radius * sin(a * pi / 180.0f) * sin(b * pi / 180.0f));
        p[z] = (float) (radius * cos(a * pi / 180.0f));
        return 1;
    }

    final float[][] getPointMatrix(float radius, int slices) {
        int i, j;
        float a, b;
        float hStep = 180.0f / (h - 1);
        float wStep = 360.0f / w;
        int length = w * h;
        float[][] matrix;
        float[] vec3;
        matrix = new float[length][];
        for (a = 0.0f, i = 0; i < h; i++, a += hStep) {
            for (b = 0.0f, j = 0; j < w; j++, b += wStep) {
                vec3 = new float[3];
                matrix[i * w + j] = vec3;
                getPoint(radius, a, b, vec3);
            }
        }
        return matrix;
    }

    float[] tmp0 = {0, 0, 0};
    float[] tmp1 = {0, 0, 0};
    float[] tmp2 = {0, 0, 0};

    void drawSlice(float[] p1, float[] p2, float[] p3, float[] p4, int mode) {
        switch (mode) {
            case SOLID:
                glBegin(GL_QUADS);
                break;
            case WIRE:
                glBegin(GL_LINE_LOOP);
                break;
        }
        //vec_mul_cross(tmp2, vec_sub(tmp0, p3, p2), vec_sub(tmp1, p4, p3));
        GL.glNormal3fv(p1, 0);
        glVertex3f(p1[x], p1[y], p1[z]);
        GL.glNormal3fv(p2, 0);
        glVertex3f(p2[x], p2[y], p2[z]);
        GL.glNormal3fv(p3, 0);
        glVertex3f(p3[x], p3[y], p3[z]);
        GL.glNormal3fv(p4, 0);
        glVertex3f(p4[x], p4[y], p4[z]);
        glEnd();
    }

    public int draw() {
        int i = 0, j = 0;
        for (; i < h - 1; i++) {
            for (j = 0; j < w - 1; j++) {
                drawSlice(mx[i * w + j], mx[i * w + j + 1], mx[(i + 1) * w + j + 1], mx[(i + 1) * w + j], mode);
            }
            drawSlice(mx[i * w + j], mx[i * w], mx[(i + 1) * w], mx[(i + 1) * w + j], mode);
        }
        return 1;
    }

}
