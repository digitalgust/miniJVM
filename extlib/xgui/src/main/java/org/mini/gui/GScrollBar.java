/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GScrollBar extends GObject {
    public static final int HORIZONTAL = 0, VERTICAL = 1;

    protected String text;
    protected float pos;
    protected boolean draged;
    protected int mode = HORIZONTAL;
    protected float radius = 8;
    protected float radiusBig = 12;
    protected float[] line_boundle = new float[4];

    public GScrollBar(GForm form) {
        this(form, 0f, HORIZONTAL, 0f, 0f, 1f, 1f);
    }

    public GScrollBar(GForm form, float pos, int mode, float left, float top, float width, float height) {
        super(form);
        this.pos = pos;
        this.mode = mode;
        setLocation(left, top);
        setSize(width, height);
        reBoundle();
    }

    @Override
    public void setSize(float w, float h) {
        super.setSize(w, h);
        reBoundle();
    }

    public void setMode(int mode) {
        this.mode = mode;
        reBoundle();
    }

    public void reBoundle() {
        if (mode == HORIZONTAL) {
            line_boundle[LEFT] = radiusBig;
            line_boundle[WIDTH] = boundle[WIDTH] - radiusBig * 2;
            line_boundle[TOP] = 0;
            line_boundle[HEIGHT] = boundle[HEIGHT];
        } else {
            line_boundle[LEFT] = 0;
            line_boundle[WIDTH] = boundle[WIDTH];
            line_boundle[TOP] = radiusBig;
            line_boundle[HEIGHT] = boundle[HEIGHT] - radiusBig * 2;

        }
    }


    public float getPos() {
        return pos;
    }

    public void setPos(float p) {
        pos = p;
        if (pos > 1) {
            pos = 1.f;
        }
        if (pos < 0) {
            pos = 0.f;
        }
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) System.out.println(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }

    @Override
    public boolean scrollEvent(float dx, float dy, float x, float y) {
        return dragEvent(Glfw.GLFW_MOUSE_BUTTON_1, dx, dy, x, y);
    }

    @Override
    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        if (draged && button == Glfw.GLFW_MOUSE_BUTTON_1) {
            return true;
        }
        return false;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        int rx = (int) (x - getX());
        int ry = (int) (y - getY());
        //if (isInBoundle(line_boundle, rx, ry)) {
        if (pressed) {
            draged = true;
            parent.setCurrent(this);
            float p = mode == HORIZONTAL ? (rx - line_boundle[LEFT]) / line_boundle[WIDTH] : (ry - line_boundle[TOP]) / line_boundle[HEIGHT];
            setPos(p);
        } else {
            draged = false;
            doAction();
            doStateChanged(this);
        }
        //}
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        int rx = (int) (x - getX());
        int ry = (int) (y - getY());
        if (draged) {
            float p = mode == HORIZONTAL ? (rx - line_boundle[LEFT]) / line_boundle[WIDTH] : (ry - line_boundle[TOP]) / line_boundle[HEIGHT];
            setPos(p);
            doStateChanged(this);
        }
    }

    @Override
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {
        return true;
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        int rx = (int) (x - getX());
        int ry = (int) (y - getY());
        //if (isInBoundle(line_boundle, rx, ry)) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan:
                draged = true;
                float p = mode == HORIZONTAL ? (rx - line_boundle[LEFT]) / line_boundle[WIDTH] : (ry - line_boundle[TOP]) / line_boundle[HEIGHT];
                setPos(p);
                break;
            case Glfm.GLFMTouchPhaseMoved:
                if (draged) {
                    p = mode == HORIZONTAL ? (rx - line_boundle[LEFT]) / line_boundle[WIDTH] : (ry - line_boundle[TOP]) / line_boundle[HEIGHT];
                    setPos(p);
                    doStateChanged(this);
                }
                break;
            case Glfm.GLFMTouchPhaseEnded:
                draged = false;
                doAction();
                doStateChanged(this);
                break;
            default:
                break;
        }
        //}
    }

    /**
     * @param vg
     * @return
     */
    public boolean paint(long vg) {
        super.paint(vg);
        float x = getX() + line_boundle[LEFT];
        float y = getY() + line_boundle[TOP];
        float w = line_boundle[WIDTH];
        float h = line_boundle[HEIGHT];

        if (mode == HORIZONTAL) {
            drawSliderH(vg, pos, x, y, w, h);
        } else {
            drawSliderV(vg, pos, x, y, w, h);
        }
        return true;
    }

    void drawSliderH(long vg, float pos, float x, float y, float w, float h) {
        byte[] bg, knob;
        float cy = y + (int) (h * 0.5f);
        float kr = radius;//(int) (h * 0.25f);

        // Slot
        bg = nvgBoxGradient(vg, x, cy - 2 + 1, w, 4, 2, 2, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 32));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, cy - 2, w, 4, 2);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob Shadow
        bg = nvgRadialGradient(vg, x + (int) (pos * w), cy + 1, kr - 3, kr + 3, nvgRGBA(0, 0, 0, 64), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x + (int) (pos * w) - kr - 5, cy - kr - 5, kr * 2 + 5 + 5, kr * 2 + 5 + 5 + 3);
        nvgCircle(vg, x + (int) (pos * w), cy, kr);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob
        knob = nvgLinearGradient(vg, x, cy - kr, x, cy + kr, nvgRGBA(255, 255, 255, 16), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgCircle(vg, x + (int) (pos * w), cy, kr - 1);
        nvgFillColor(vg, getBgColor());
        nvgFill(vg);
        nvgFillPaint(vg, knob);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgCircle(vg, x + (int) (pos * w), cy, kr - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 92));
        nvgStroke(vg);

    }

    void drawSliderV(long vg, float pos, float x, float y, float w, float h) {
        byte[] bg, knob;
        float cx = x + (int) (w * 0.5f);
        float kr = radius;//(int) (w * 0.25f);

        // Slot
        bg = nvgBoxGradient(vg, cx - 2 + 1, y, 4, h, 2, 2, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 32));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, cx - 2, y, 4, h, 2);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob Shadow
        bg = nvgRadialGradient(vg, cx + 1, y + (int) (pos * h), kr - 3, kr + 3, nvgRGBA(0, 0, 0, 64), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgCircle(vg, cx, y + (int) (pos * h), kr);
        nvgPathWinding(vg, NVG_SOLID);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob
        knob = nvgLinearGradient(vg, cx - kr, y, x, cx + kr, nvgRGBA(255, 255, 255, 16), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgCircle(vg, cx, y + (int) (pos * h), kr - 1);
        nvgFillColor(vg, getBgColor());
        nvgFill(vg);
        nvgFillPaint(vg, knob);
        nvgFill(vg);

    }

}
