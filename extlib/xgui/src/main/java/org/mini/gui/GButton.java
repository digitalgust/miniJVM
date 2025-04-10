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
    float lineh;
    float iconWidth, textWidth;

    int align = NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE;


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
        text_arr = null;
    }

    public String getText() {
        return this.text;
    }

    public void setPreIcon(String preicon) {
        if (preicon == null || preicon.trim().length() == 0) return;
        this.preicon = preicon;
        preicon_arr = null;
    }

    public float[] getPreiconColor() {
        return preiconColor;
    }

    public void setPreiconColor(float[] preiconColor) {
        this.preiconColor = preiconColor;
    }


    public void setAlign(int ali) {
        align = ali;
        if ((align & 0x7f) == 0) {
            align = 0;
            align |= NVG_ALIGN_CENTER;
            align |= NVG_ALIGN_MIDDLE;
        }
    }

    public int getAlign() {
        return align;
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

    static float[] GRADIENT_COLOR0 = {1.0f, 1.0f, 1.0f, 0.2f};
    static float[] GRADIENT_COLOR1 = {0.0f, 0.0f, 0.0f, 0.1f};

    @Override
    boolean paintFlying(long vg, float x, float y) {
        float w = getW();
        float h = getH();

        if (text_arr == null) {
            text_arr = toCstyleBytes(text);
            float[] b = GToolkit.getTextBoundle(vg, text_arr, GCallBack.getInstance().getDeviceWidth(), getFontSize(), getFontWord(), false);
            textWidth = b[WIDTH];
            lineh = b[HEIGHT];
        }

        if (preicon_arr == null && preicon != null && preicon.length() > 0) {
            preicon_arr = toCstyleBytes(preicon);
            float[] b = GToolkit.getTextBoundle(vg, preicon_arr, GCallBack.getInstance().getDeviceWidth(), GToolkit.getStyle().getIconFontSize(), getFontIcon(), true);
            iconWidth = b[WIDTH];
        }

        byte[] bg;
        float move = 0;
        if (GToolkit.getFeel() == FEEL_DIMENSION) {
            if (touched && enable) {
                move = 1;
                bg = nvgLinearGradient(vg, x, y + h, x, y, GRADIENT_COLOR0, GRADIENT_COLOR1);
            } else {
                bg = nvgLinearGradient(vg, x, y, x, y + h, GRADIENT_COLOR0, GRADIENT_COLOR1);
            }
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, getCornerRadius() - 1);
            nvgFillColor(vg, getBgColor());
            nvgFill(vg);
            nvgFillPaint(vg, bg);
            nvgFill(vg);
        } else {
            float[] c;
            if (touched && enable) {
                move = 1;
                c = GRADIENT_COLOR1;
            } else {
                c = GRADIENT_COLOR0;
            }
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, getCornerRadius() - 0.5f);
            nvgFillColor(vg, c);
            nvgFill(vg);
        }


        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, getCornerRadius() - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
        nvgStroke(vg);

        float[] textColor = getColor();
        //calc text width

        float iconx;
        float icony;
        float textx;
        float texty;

        if ((align & NVG_ALIGN_LEFT) != 0) {
            iconx = x + 2;
            textx = x + 2 + iconWidth;
        } else if ((align & NVG_ALIGN_RIGHT) != 0) {
            iconx = x + w - 2;
            textx = x + w - 2 - iconWidth;
        } else {
            iconx = x + (w - textWidth) * 0.5f;//只有图标时，图示在正中央
            textx = x + (w + iconWidth) * 0.5f;
        }

        if ((align & NVG_ALIGN_TOP) != 0) {
            icony = y + 2;
            texty = y + 4;
        } else if ((align & NVG_ALIGN_BOTTOM) != 0) {
            icony = y + h - 2;
            texty = y + h - 2;
        } else if ((align & NVG_ALIGN_BASELINE) != 0) {
            icony = y + h - 2 - iconWidth;
            texty = y + h - 2 - lineh * 0.2f - iconWidth;
        } else {
            icony = y + h * 0.5f + move + 1f;
            texty = y + h * 0.5f + move + 2f;
        }

        //draw preicon
        if (preicon != null) {
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());

            float[] pc = preiconColor == null ? getStyle().getTextFontColor() : preiconColor;
            pc = enable ? pc : getDisabledColor();
            nvgFillColor(vg, pc);
            nvgTextAlign(vg, align);
            nvgTextJni(vg, iconx, icony, preicon_arr, 0, preicon_arr.length);
        }
        // draw text
        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, align);
        nvgFontBlur(vg, 2f);
        nvgFillColor(vg, GToolkit.getStyle().getTextShadowColor());
        nvgTextJni(vg, textx, texty, text_arr, 0, text_arr.length);
        nvgFontBlur(vg, 0);
        nvgFillColor(vg, textColor);
        nvgTextJni(vg, textx, texty, text_arr, 0, text_arr.length);

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
