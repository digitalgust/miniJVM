/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GCheckBox extends GObject {

    protected String text;
    protected byte[] text_arr;
    protected boolean checked;
    protected byte[] preicon_arr = toCstyleBytes(ICON_CHECK);

    public GCheckBox(GForm form) {
        this(form, "", false, 0f, 0f, 1f, 1f);
    }

    public GCheckBox(GForm form, String text, boolean checked, float left, float top, float width, float height) {
        super(form);
        setText(text);
        this.checked = checked;
        setLocation(left, top);
        setSize(width, height);
    }


    public void setText(String text) {
        this.text = text;
        text_arr = toCstyleBytes(text);
    }

    public String getText() {
        return text;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed) {
                parent.setFocus(this);
            } else {
                checked = !checked;
                doAction();
            }
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                checked = !checked;
                doAction();
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
        float w = getW();
        float h = getH();

        byte[] bg;

        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());

        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgTextJni(vg, x + 25, y + h * 0.5f, text_arr, 0, text_arr.length);

        bg = nvgBoxGradient(vg, x + 1, y + (int) (h * 0.5f) - 9 + 1, 18, 18, 3, 3, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 92));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + (int) (h * 0.5f) - 9, 18, 18, 3);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
        nvgFontFace(vg, GToolkit.getFontIcon());
        nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        if (checked) {
            nvgTextJni(vg, x + 3, y + (int) (h * 0.5f), preicon_arr, 0, preicon_arr.length);
        }
        return true;
    }

    /**
     * @return the checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * @param checked the checked to set
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

}
