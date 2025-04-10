/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.nanovg.Nanovg;

import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GLabel extends GObject {

    protected byte[] text_arr;
    protected char preicon;
    protected float[] lineh = {0};
    protected boolean touched;

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
        if ((align & 0x7f) == 0) {
            align |= NVG_ALIGN_TOP;
        }
    }

    public int getAlign() {
        return align;
    }

    public void setText(String text) {
        text = text.replace("\\n", "\n");
        super.setText(text);
        text_arr = toCstyleBytes(text);
    }


    public void setIcon(char icon) {
        preicon = icon;
    }

    float oldX, oldY;

    private boolean validAction(float releaseX, float releaseY) {
        if (Math.abs(releaseX - oldX) < TOUCH_RANGE && Math.abs(releaseY - oldY) < TOUCH_RANGE) {
            return true;
        }
        return false;
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

        if (bgColor != null) {
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, getCornerRadius() - 0.5f);
            nvgFillColor(vg, bgColor);
            nvgFill(vg);
        }

        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgFillColor(vg, getColor());

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

        if (bgColor != null) {
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, getCornerRadius() - 0.5f);
            nvgFillColor(vg, bgColor);
            nvgFill(vg);
        }

        nvgFontSize(vg, getFontSize());
        nvgFillColor(vg, getColor());
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
