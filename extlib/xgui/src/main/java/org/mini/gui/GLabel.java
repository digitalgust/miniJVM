/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.nanovg.Nanovg;

import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GLabel extends GObject {

    protected byte[] text_arr;
    protected char preicon;
    protected float[] lineh = {0};
    protected boolean pressed;

    int align = NVG_ALIGN_LEFT | NVG_ALIGN_TOP;

    public static final int TEXT_BOUND_DEC = 10;// diff with nvgTextBoxBound

    public static final int MODE_MULTI_SHOW = 1, MODE_SINGLE_SHOW = 2;
    int showMode = MODE_SINGLE_SHOW;

    public GLabel(GForm form) {
        this(form, "", 0f, 0f, 1f, 1f);
    }

    public GLabel(GForm form, String text, float left, float top, float width, float height) {
        super(form);
        setText(text);
        setLocation(left, top);
        setSize(width, height);
    }


    public void setShowMode(int m) {
        this.showMode = m;

    }

    public int getShowMode() {
        return this.showMode;
    }

    public void setAlign(int ali) {
        align = ali;
        if ((align & 0x78) == 0) {
            align |= NVG_ALIGN_TOP;
        }
    }

    public void setText(String text) {
        this.text = text;
        text_arr = toCstyleBytes(text);
    }

    public String getText() {
        return text;
    }

    public void setIcon(char icon) {
        preicon = icon;
    }

    float oldX, oldY;

    private boolean validAction(float releaseX, float releaseY) {
        if (releaseX >= oldX && releaseX <= oldX + getW() && releaseY >= oldY && releaseY < oldY + getH()) {
            return true;
        }
        return false;
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed) {
                this.pressed = true;
                parent.setCurrent(this);
                oldX = getX();
                oldY = getY();
            } else {
                this.pressed = false;
                if (validAction(x, y)) doAction();
            }
        }
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        if (!isInArea(x, y)) {
            pressed = false;
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                pressed = true;
                oldX = getX();
                oldY = getY();
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                if (validAction(x, y)) doAction();
                pressed = false;
            } else if (!isInArea(x, y)) {
                pressed = false;
            }
        }
    }

    /**
     * @param vg
     * @return
     */
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
        
        if (bgColor != null) {
            GToolkit.drawRect(vg, getX(), getY(), getW(), getH(), bgColor);
        }

        if (showMode == MODE_MULTI_SHOW) {
            drawMultiText(vg, x, y, w, h);
        } else {
            drawLine(vg, x, y, w, h);
        }
        return true;
    }

    void drawLine(long vg, float x, float y, float w, float h) {
        //NVG_NOTUSED(w);
        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgFillColor(vg, enable ? getColor() : getDisabledColor());

        nvgTextAlign(vg, align);
        if (text_arr != null) {
            float dx, dy;
            dx = x;
            dy = y;
            if ((align & Nanovg.NVG_ALIGN_CENTER) != 0) {
                dx += w * .5f;
            } else if ((align & Nanovg.NVG_ALIGN_RIGHT) != 0) {
                dx += w;
            }

            if ((align & Nanovg.NVG_ALIGN_MIDDLE) != 0) {
                dy += h * .5;
            } else if ((align & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
                dy += h;
            }
            nvgTextJni(vg, dx, dy + 2, text_arr, 0, text_arr.length);
        }

    }

    void drawMultiText(long vg, float x, float y, float w, float h) {

        nvgFontSize(vg, getFontSize());
        nvgFillColor(vg, enable ? (isFlying() ? getFlyingColor() : getColor()) : getDisabledColor());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextMetrics(vg, null, null, lineh);

        nvgTextAlign(vg, align);
        float dx, dy;
        dx = x;
        dy = y;
        if ((align & Nanovg.NVG_ALIGN_MIDDLE) != 0) {
            dy += lineh[0];
        } else if ((align & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
            dy += getFontSize();
        }

        if (text_arr != null) {
            nvgTextBoxJni(vg, dx, dy + 2, w - TEXT_BOUND_DEC, text_arr, 0, text_arr.length);
        }
    }

}
