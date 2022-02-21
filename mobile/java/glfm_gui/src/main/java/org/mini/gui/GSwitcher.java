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
 * @author gust
 */
public class GSwitcher extends GObject {

    protected String text;
    protected boolean switcher;
    static public final float DEFAULT_WIDTH = 50f;
    static public final float DEFAULT_HEIGHT = 30f;

    public GSwitcher() {
        this(false, 0f, 0f, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public GSwitcher(boolean sw, int left, int top, int width, int height) {
        this(sw, (float) left, top, width, height);
    }

    public GSwitcher(boolean sw, float left, float top, float width, float height) {
        this.switcher = sw;
        setLocation(left, top);
        setSize(width, height);
    }


    public boolean getSwitcher() {
        return switcher;
    }

    public void setSwitcher(boolean p) {
        switcher = p;
        flush();
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) System.out.println(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (pressed) {
            switcher = !switcher;
            doAction();
        } else {
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan:
                switcher = !switcher;
                doAction();
                break;
            case Glfm.GLFMTouchPhaseMoved:
                break;
            case Glfm.GLFMTouchPhaseEnded:
                break;
            default:
                break;
        }
    }

    /**
     * @param vg
     * @return
     */
    public boolean paint(long vg) {
        float x = getX() + 2;
        float y = getY() + 2;
        float w = boundle[WIDTH] - 4;
        float h = boundle[HEIGHT] - 4;

        drawSliderH(vg, x, y, w, h);
        return true;
    }

    void drawSliderH(long vg, float x, float y, float w, float h) {
        float r = h * .5f;
        float rightX = w - r;
        float leftX = r;
        float dx = switcher ? rightX : leftX;
        float dy = h * .5f;
        byte[] bg, knob;

        float[] back1 = switcher ? nvgRGBA(0, 128, 0, 32) : nvgRGBA(0, 0, 0, 32);
        float[] back2 = switcher ? nvgRGBA(0, 138, 0, 128) : nvgRGBA(0, 0, 0, 128);
        // Slot
        bg = nvgBoxGradient(vg, x, y, w, h, r, r, back1, back2);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, r);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob Shadow
        bg = nvgRadialGradient(vg, x + dx, y + dy, r - 1, r - 1, nvgRGBA(0, 0, 0, 64), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        //nvgRect(vg, x, y, w, h);
        nvgCircle(vg, x + dx, y + dy, r - 2);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob
        knob = nvgLinearGradient(vg, x + dx + 1, y + dy + 1, r - 3, r - 3, nvgRGBA(255, 255, 255, 16), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgCircle(vg, x + dx, y + dy, r - 3);
        nvgFillColor(vg, GToolkit.getStyle().getBackgroundColor());
        nvgFill(vg);
        nvgFillPaint(vg, knob);
        nvgFill(vg);

    }


}
