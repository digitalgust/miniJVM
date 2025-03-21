/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.gui.callback.GCallBack;

import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.gui.GToolkit.*;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GButton extends GObject {

    protected String text;
    protected byte[] text_arr;
    protected String preicon;
    protected byte[] preicon_arr;
    protected float[] preiconColor;
    protected boolean touched = false;
    float oldX, oldY;
    float[] box = new float[4];
    float lineh;
    float iconWidth;


    public GButton(GForm form) {
        this(form, null, 0f, 0f, 1f, 1f);
    }

    public GButton(GForm form, String text, float left, float top, float width, float height) {
        super(form);
        setText(text);
        setLocation(left, top);
        setSize(width, height);
        setBgColor(GColorSelector.TRANSPARENT);
        setCornerRadius(4.f);
    }


    public void setText(String text) {
        if (text == null) return;
        this.text = text;
        text_arr = toCstyleBytes(text);
        long vg = GCallBack.getInstance().getNvContext();
        float[] b = GToolkit.getTextBoundle(vg, text_arr, GCallBack.getInstance().getDeviceWidth(), getFontSize(), getFontWord(), false);
        System.arraycopy(b, 0, box, 0, 4);
        lineh = b[HEIGHT];
    }

    public String getText() {
        return this.text;
    }

    public void setPreIcon(String preicon) {
        if (preicon == null || preicon.trim().length() == 0) return;
        this.preicon = preicon;
        preicon_arr = toCstyleBytes(preicon);
        long vg = GCallBack.getInstance().getNvContext();
        float[] b = GToolkit.getTextBoundle(vg, preicon_arr, GCallBack.getInstance().getDeviceWidth(), GToolkit.getStyle().getIconFontSize(), getFontIcon(), true);
        iconWidth = b[WIDTH];
    }

    public float[] getPreiconColor() {
        return preiconColor;
    }

    public void setPreiconColor(float[] preiconColor) {
        this.preiconColor = preiconColor;
    }

    public boolean isPressed() {
        return touched;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed) {
                touched = true;
                parent.setCurrent(this);
                oldX = x;
                oldY = y;
                doStateChanged(this);
            } else {
                if (validAction(x, y)) doAction();
                touched = false;
                doStateChanged(this);
            }
        }
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        if (!isInArea(x, y) && touched) {
            touched = false;
            doStateChanged(this);
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan:
                    touched = true;
                    oldX = x;
                    oldY = y;
                    doStateChanged(this);
                    break;
                case Glfm.GLFMTouchPhaseEnded:
                    if (validAction(x, y)) doAction();
                    touched = false;
                    doStateChanged(this);
                    break;
            }
        } else if (!isInArea(x, y)) {
            if (touched) {
                touched = false;
                doStateChanged(this);
            }
        }
    }

    /**
     * @param vg
     * @return
     */
    @Override
    public boolean paint(long vg) {
        super.paint(vg);
        float x = getX();
        float y = getY();
        return paintFlying(vg, x, y);
    }

    @Override
    boolean paintFlying(long vg, float x, float y) {
        float w = getW();
        float h = getH();

        byte[] bg;


        float tw = 0, iw = 0;
        float move = 0;
        if (touched && enable) {
            move = 1;
            bg = nvgLinearGradient(vg, x, y + h, x, y, nvgRGBA(255, 255, 255, 0x10), nvgRGBA(0, 0, 0, 0x10));
        } else {
            bg = nvgLinearGradient(vg, x, y, x, y + h, nvgRGBA(255, 255, 255, 0x10), nvgRGBA(0, 0, 0, 0x10));
        }
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, getCornerRadius() - 1);
        nvgFillColor(vg, getBgColor());
        nvgFill(vg);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, getCornerRadius() - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
        nvgStroke(vg);

        float[] textColor = getColor();
        //calc text width
        if (text.length() > 0) {
            tw = box[WIDTH];
        }
        //draw preicon
        if (preicon != null) {
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());

            iw = iconWidth;

            float[] pc = preiconColor == null ? getStyle().getTextFontColor() : preiconColor;
            pc = enable ? pc : getDisabledColor();
            nvgFillColor(vg, pc);
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgTextJni(vg, x + w * 0.5f - tw * 0.5f, y + h * 0.5f + move + 1f, preicon_arr, 0, preicon_arr.length);
        }
        // draw text
        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFontBlur(vg, 2f);
        nvgFillColor(vg, GToolkit.getStyle().getTextShadowColor());
        nvgTextJni(vg, x + w * 0.5f - tw * 0.5f + iw * .5f, y + h * 0.5f + move + 1.5f, text_arr, 0, text_arr.length);
        nvgFontBlur(vg, 0);
        nvgFillColor(vg, textColor);
        nvgTextJni(vg, x + w * 0.5f - tw * 0.5f + iw * .5f, y + h * 0.5f + move + 1.5f, text_arr, 0, text_arr.length);

        return true;
    }

    boolean isBlack(float[] col) {
        int r = 0, g = 1, b = 2, a = 3;
        if (col[r] == 0.0f && col[g] == 0.0f && col[b] == 0.0f && col[a] == 0.0f) {
            return true;
        }
        return false;
    }

    private boolean validAction(float releaseX, float releaseY) {
        if (Math.abs(releaseX - oldX) < TOUCH_RANGE && Math.abs(releaseY - oldY) < TOUCH_RANGE) {
            return true;
        }
        return false;
    }

}
