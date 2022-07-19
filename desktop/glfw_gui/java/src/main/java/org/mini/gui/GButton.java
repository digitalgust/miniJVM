/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.glwrap.GLUtil.toUtf8;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GButton extends GObject {

    protected String text;
    protected byte[] text_arr;
    protected char preicon;
    protected byte[] preicon_arr;
    protected boolean bt_pressed = false;
    float oldX, oldY;

    private boolean validAction(float releaseX, float releaseY) {
        if (releaseX >= oldX && releaseX <= oldX + getW() && releaseY >= oldY && releaseY < oldY + getH()) {
            return true;
        }
        return false;
    }

    public GButton() {
        this(null, 0f, 0f, 1f, 1f);
    }

    public GButton(String text, int left, int top, int width, int height) {
        this(text, (float) left, top, width, height);
    }

    public GButton(String text, float left, float top, float width, float height) {
        setText(text);
        setLocation(left, top);
        setSize(width, height);
    }


    public void setText(String text) {
        this.text = text;
        text_arr = toUtf8(text);
    }

    public String getText() {
        return this.text;
    }

    public void setIcon(char icon) {
        preicon = icon;
        preicon_arr = toUtf8("" + preicon);
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed) {
                bt_pressed = true;
                parent.setFocus(this);
                oldX = getX();
                oldY = getY();
            } else {
                bt_pressed = false;
                if (validAction(x, y)) doAction();
            }
        }
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        if (!isInArea(x, y)) {
            bt_pressed = false;
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                bt_pressed = true;
                oldX = getX();
                oldY = getY();
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                if (validAction(x, y)) doAction();
                bt_pressed = false;
            } else if (!isInArea(x, y)) {
                bt_pressed = false;
            }
        }
    }

    /**
     * @param vg
     * @return
     */
    @Override
    public boolean paint(long vg) {
        float x = getX();
        float y = getY();
        return paintFlying(vg, x, y);
    }

    @Override
    boolean paintFlying(long vg, float x, float y) {
        float w = getW();
        float h = getH();

        byte[] bg;

        float cornerRadius = 4.0f;
        float tw = 0, iw = 0;
        float move = 0;
        if (bt_pressed) {
            move = 1;
            bg = nvgLinearGradient(vg, x, y + h, x, y, nvgRGBA(255, 255, 255, 0x10), nvgRGBA(0, 0, 0, 0x10));
        } else {
            bg = nvgLinearGradient(vg, x, y, x, y + h, nvgRGBA(255, 255, 255, 0x10), nvgRGBA(0, 0, 0, 0x10));
        }
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, cornerRadius - 1);
        nvgFillColor(vg, getBgColor());
        nvgFill(vg);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, cornerRadius - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
        nvgStroke(vg);

        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        tw = nvgTextBoundsJni(vg, 0, 0, text_arr, 0, text_arr.length, null);
        if (preicon != 0) {
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());

            iw = nvgTextBoundsJni(vg, 0, 0, preicon_arr, 0, preicon_arr.length, null);
            //iw += h * 0.15f;
        }

        if (preicon != 0) {
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());
            nvgFillColor(vg, nvgRGBA(255, 255, 255, 96));
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgTextJni(vg, x + w * 0.5f - tw * 0.5f - iw * 0.5f, y + h * 0.5f + move, preicon_arr, 0, preicon_arr.length);
        }

        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFontBlur(vg, 1f);
        nvgFillColor(vg, GToolkit.getStyle().getTextShadowColor());
        nvgTextJni(vg, x + w * 0.5f - tw * 0.5f + iw * 0.25f, y + h * 0.5f + move, text_arr, 0, text_arr.length);
        nvgFontBlur(vg, 0);
        nvgFillColor(vg, enable ? (isFlying() ? getFlyingColor() : getColor()) : getDisabledColor());
        nvgTextJni(vg, x + w * 0.5f - tw * 0.5f + iw * 0.25f, y + h * 0.5f + move, text_arr, 0, text_arr.length);

        return true;
    }

    boolean isBlack(float[] col) {
        int r = 0, g = 1, b = 2, a = 3;
        if (col[r] == 0.0f && col[g] == 0.0f && col[b] == 0.0f && col[a] == 0.0f) {
            return true;
        }
        return false;
    }
}
