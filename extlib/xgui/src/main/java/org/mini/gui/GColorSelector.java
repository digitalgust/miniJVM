/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * not implementation completed
 *
 * @author gust
 */
class GColorSelector extends GObject {

    /**
     * these color can't change by user
     */
    public static final float[] RED = new float[]{0xff, 0x00, 0x00, 0xff};
    public static final float[] GREEN = new float[]{0x00, 0xff, 0x00, 0xff};
    public static final float[] BLUE = new float[]{0x00, 0x00, 0xff, 0xff};
    public static final float[] YELLOW = new float[]{0xff, 0xff, 0x00, 0xff};
    public static final float[] PURPLE = new float[]{0xff, 0x00, 0xff, 0xff};
    public static final float[] CYAN = new float[]{0x00, 0xff, 0xff, 0xff};
    public static final float[] WHITE = new float[]{0xff, 0xff, 0xff, 0xff};
    public static final float[] BLACK = new float[]{0x00, 0x00, 0x00, 0xff};
    public static final float[] GRAY = new float[]{0x80, 0x80, 0x80, 0xff};
    public static final float[] TRANSPARENT = new float[]{0x00, 0x00, 0x00, 0x00};
    public static final float[] RED_HALF = new float[]{0xff, 0x00, 0x00, 0x80};
    public static final float[] GREEN_HALF = new float[]{0x00, 0xff, 0x00, 0x80};
    public static final float[] BLUE_HALF = new float[]{0x00, 0x00, 0xff, 0x80};
    public static final float[] YELLOW_HALF = new float[]{0xff, 0xff, 0x00, 0x80};
    public static final float[] PURPLE_HALF = new float[]{0xff, 0x00, 0xff, 0x80};
    public static final float[] CYAN_HALF = new float[]{0x00, 0xff, 0xff, 0x80};
    public static final float[] WHITE_HALF = new float[]{0xff, 0xff, 0xff, 0x80};
    public static final float[] BLACK_HALF = new float[]{0x00, 0x00, 0x00, 0x80};
    public static final float[] GRAY_HALF = new float[]{0x80, 0x80, 0x80, 0x80};

    protected String text;
    protected float curAngel;
    protected float oldAngel;
    protected float centX;
    protected float centY;
    protected float r_big, r_small;
    protected float selectX, selectY;

    public GColorSelector(GForm form) {
        this(form, 0f, 0f, 0f, 1f, 1f);
    }

    public GColorSelector(GForm form, float pos, float left, float top, float width, float height) {
        super(form);
        this.curAngel = pos;
        setLocation(left, top);
        setSize(width, height);
        centX = width / 2;
        centY = height / 2;

    }

    /**
     * 0.0f - 1.0f value of r,g,b,a
     *
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public static float[] getColor(float r, float g, float b, float a) {
        return new float[]{r, g, b, a};
    }

    /**
     * 0 - 255 value of r,g,b,a
     *
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public static float[] getColor(int r, int g, int b, int a) {
        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }

    public static float[] getColor(int rgba) {
        int r = (rgba >> 24) & 0xff;
        int g = (rgba >> 16) & 0xff;
        int b = (rgba >> 8) & 0xff;
        int a = (rgba) & 0xff;
        return new float[]{r / 255f, g / 255f, b / 255f, a / 255f};
    }

    public static int getHexColor(float[] color) {
        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        int a = (int) (color[3] * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float[] copyColor(float[] color) {
        if (color == null) return new float[4];
        return new float[]{color[0], color[1], color[2], color[3]};
    }

    public static void copyColor(float[] src, float[] dest) {
        if (src == null || dest == null || src.length != 4 || dest.length != 4) return;
        System.arraycopy(src, 0, dest, 0, 4);
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        if (isInArea(x, y)) {

        }
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (!pressed) {
                float offX = x - (getX() + centX);
                float offY = y - (getY() + centY);
                float r = (float) Math.sqrt(offX * offX + offY * offY);
                if (r < r_small) {
                    selectX = offX;
                    selectY = offY;
                } else if (r < r_big) {
                    curAngel = (float) (Math.atan2(offY, offX));
                    float angel = -oldAngel + curAngel;
                    System.out.println("curA:" + curAngel + "    oldA:" + oldAngel + "    result:" + angel);
                    oldAngel = curAngel;
                    float oldX = selectX;
                    float oldY = selectY;
                    selectX = (float) (Math.cos(angel) * oldX - Math.sin(angel) * oldY);//(float) Math.cos(120.0f / 180.0f * Math.PI) * r * 0.3f;
                    selectY = (float) (Math.sin(angel) * oldX + Math.cos(angel) * oldY);//(float) Math.sin(120.0f / 180.0f * Math.PI) * r * 0.4f;
                }
            } else doAction();
        }

    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase != Glfm.GLFMTouchPhaseBegan) {
                float offX = x - (getX() + centX);
                float offY = y - (getY() + centY);
                float r = (float) Math.sqrt(offX * offX + offY * offY);
                if (r < r_small) {
                    selectX = offX;
                    selectY = offY;
                } else if (r < r_big) {
                    curAngel = (float) (Math.atan2(offY, offX));
                    float angel = -oldAngel + curAngel;
                    System.out.println("curA:" + curAngel + "    oldA:" + oldAngel + "    result:" + angel);
                    oldAngel = curAngel;
                    float oldX = selectX;
                    float oldY = selectY;
                    selectX = (float) (Math.cos(angel) * oldX - Math.sin(angel) * oldY);//(float) Math.cos(120.0f / 180.0f * Math.PI) * r * 0.3f;
                    selectY = (float) (Math.sin(angel) * oldX + Math.cos(angel) * oldY);//(float) Math.sin(120.0f / 180.0f * Math.PI) * r * 0.4f;
                }
            } else if (phase != Glfm.GLFMTouchPhaseEnded) {
                doAction();
            } else {

            }
        }

    }

    /**
     * @param vg
     * @return
     */
    public boolean paint(long vg) {
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        drawColorwheel(vg, x, y, w, h);
        return true;
    }

    void drawColorwheel(long vg, float x, float y, float w, float h) {
        int i;
        float r0, r1, ax, ay, bx, by, cx, cy, aeps, r;
        float hue = (float) Math.sin(curAngel * 0.166f);
        byte[] paint;

        cx = x + w * 0.5f;
        cy = y + h * 0.5f;
        r1 = (w < h ? w : h) * 0.5f - 5.0f;
        r_big = r1;
        r0 = r1 - 20.0f;
        r_small = r0;
        aeps = 0.5f / r1;    // half a pixel arc length in radians (2pi cancels out).

        for (i = 0; i < 6; i++) {
            float a0 = (float) (i / 6.0f * Math.PI * 2.0f - aeps);
            float a1 = (float) ((i + 1.0f) / 6.0f * Math.PI * 2.0f + aeps);
            nvgBeginPath(vg);
            nvgArc(vg, cx, cy, r0, a0, a1, NVG_CW);
            nvgArc(vg, cx, cy, r1, a1, a0, NVG_CCW);
            nvgClosePath(vg);
            ax = cx + (float) Math.cos(a0) * (r0 + r1) * 0.5f;
            ay = cy + (float) Math.sin(a0) * (r0 + r1) * 0.5f;
            bx = cx + (float) Math.cos(a1) * (r0 + r1) * 0.5f;
            by = cy + (float) Math.sin(a1) * (r0 + r1) * 0.5f;
            paint = nvgLinearGradient(vg, ax, ay, bx, by, nvgHSLA((float) (a0 / (Math.PI * 2)), 1.0f, 0.55f, (byte) 255), nvgHSLA(a1 / (float) (Math.PI * 2), 1.0f, 0.55f, (byte) 255));
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        }

        nvgBeginPath(vg);
        {
            nvgCircle(vg, cx, cy, r0 - 0.5f);
            nvgCircle(vg, cx, cy, r1 + 0.5f);
            nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 64));
            nvgStrokeWidth(vg, 1.0f);
        }
        nvgStroke(vg);

        // Selector
        nvgSave(vg);
        {
            nvgTranslate(vg, cx, cy);
            nvgRotate(vg, (float) (hue * Math.PI * 2));

            // Marker on
            nvgStrokeWidth(vg, 2.0f);
            nvgBeginPath(vg);
            nvgRect(vg, r0 - 1, -3, r1 - r0 + 2, 6);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);

            paint = nvgBoxGradient(vg, r0 - 3, -5, r1 - r0 + 6, 10, 2, 4, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
            nvgBeginPath(vg);
            nvgRect(vg, r0 - 2 - 10, -4 - 10, r1 - r0 + 4 + 20, 8 + 20);
            nvgRect(vg, r0 - 2, -4, r1 - r0 + 4, 8);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, paint);
            nvgFill(vg);

            // Center triangle
            r = r0 - 6;
            ax = (float) Math.cos(120.0f / 180.0f * Math.PI) * r;
            ay = (float) Math.sin(120.0f / 180.0f * Math.PI) * r;
            bx = (float) Math.cos(-120.0f / 180.0f * Math.PI) * r;
            by = (float) Math.sin(-120.0f / 180.0f * Math.PI) * r;
            nvgBeginPath(vg);
            nvgMoveTo(vg, r, 0);
            nvgLineTo(vg, ax, ay);
            nvgLineTo(vg, bx, by);
            nvgClosePath(vg);
            paint = nvgLinearGradient(vg, r, 0, ax, ay, nvgHSLA(hue, 1.0f, 0.5f, (byte) 255), nvgRGBA(255, 255, 255, 255));
            nvgFillPaint(vg, paint);
            nvgFill(vg);
            paint = nvgLinearGradient(vg, (r + ax) * 0.5f, (0 + ay) * 0.5f, bx, by, nvgRGBA(0, 0, 0, 0), nvgRGBA(0, 0, 0, 255));
            nvgFillPaint(vg, paint);
            nvgFill(vg);
            nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 64));
            nvgStroke(vg);

            // Select circle on triangle
            float angel = -curAngel;
            ax = (float) (Math.cos(angel) * selectX - Math.sin(angel) * selectY);//(float) Math.cos(120.0f / 180.0f * Math.PI) * r * 0.3f;
            ay = (float) (Math.sin(angel) * selectX + Math.cos(angel) * selectY);//(float) Math.sin(120.0f / 180.0f * Math.PI) * r * 0.4f;
            nvgStrokeWidth(vg, 2.0f);
            nvgBeginPath(vg);
            nvgCircle(vg, ax, ay, 5);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);

            paint = nvgRadialGradient(vg, ax, ay, 7, 9, nvgRGBA(0, 0, 0, 64), nvgRGBA(0, 0, 0, 0));
            nvgBeginPath(vg);
            nvgRect(vg, ax - 20, ay - 20, 40, 40);
            nvgCircle(vg, ax, ay, 7);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, paint);
            nvgFill(vg);
            nvgTranslate(vg, -cx, -cy);
        }
        nvgRestore(vg);

    }
}
