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
public class GSwitch extends GObject {

    protected String text;
    protected boolean switcher;
    static public final float DEFAULT_WIDTH = 50f;
    static public final float DEFAULT_HEIGHT = 30f;

    public GSwitch(GForm form) {
        this(form, false, 0f, 0f, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public GSwitch(GForm form, boolean sw, float left, float top, float width, float height) {
        super(form);
        this.switcher = sw;
        setLocation(left, top);
        setSize(width, height);
    }


    public boolean getSwitcher() {
        return switcher;
    }

    public void setSwitcher(boolean p) {
        switcher = p;
        GForm.flush();
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) System.out.println(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (pressed) {
            setSwitcher(!switcher);
            doAction();
        } else {
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan:
                setSwitcher(!switcher);
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

        float[] back1 = nvgRGBA(0, 0, 0, 32);
        float[] back2 = nvgRGBA(0, 0, 0, 128);
        // Slot
        bg = nvgBoxGradient(vg, x, y, w, h, r, 3.0f, back1, back2);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, r);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        float zoom = getH() / 30f; //适应开启按钮大小
        // Knob Shadow
        bg = nvgRadialGradient(vg, x + dx, y + dy, r - 4, r - 4, nvgRGBA(0, 0, 0, 0x30), nvgRGBA(0, 0, 0, 0x00));
        nvgBeginPath(vg);
        //nvgRect(vg, x, y, w, h);
        nvgCircle(vg, x + dx, y + dy, r - 3 * zoom);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        // Knob
        knob = nvgLinearGradient(vg, x + dx + 1, y + dy + 1, r - 3, r - 3, nvgRGBA(255, 255, 255, 0x30), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgCircle(vg, x + dx, y + dy, r - 5 * zoom);
        nvgFillColor(vg, switcher ? GToolkit.getStyle().getHighColor() : GToolkit.getStyle().getLowColor());
        nvgFill(vg);
        nvgFillPaint(vg, knob);
        nvgFill(vg);

    }


}
