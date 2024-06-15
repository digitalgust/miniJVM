/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;
import static org.mini.nanovg.Nanovg.nvgFill;

/**
 * @author gust
 */
public abstract class GStyle {


    public abstract float getTextFontSize();

    public abstract float getTitleFontSize();

    public abstract float getIconFontWidth();

    public abstract float getIconFontSize();

    public abstract float[] getBackgroundColor();

    public abstract float[] getListBackgroundColor();

    public abstract float[] getPopBackgroundColor();

    public abstract float[] getDisabledTextFontColor();

    public abstract float[] getTextFontColor();

    public abstract float[] getTextShadowColor();

    public abstract float[] getHintFontColor();

    public abstract float[] getSelectedColor();

    public abstract float[] getUnselectedColor();

    public abstract float[] getEditBackground();

    public abstract float[] getFrameBackground();

    public abstract float[] getFrameTitleColor();

    public abstract float[] getHighColor();

    public abstract float[] getLowColor();


    public void drawEditBoxBase(long vg, float x, float y, float w, float h, float r) {
        byte[] bg;
        // Edit
        bg = nvgBoxGradient(vg, x, y, w, h, 3, 4, getEditBackground(), nvgRGBA(32, 32, 32, 192));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, r - 1f);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, r - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 16));
        nvgStroke(vg);
    }

    public void drawFieldBoxBase(long vg, float x, float y, float w, float h, float r) {
        byte[] bg;
        bg = nvgBoxGradient(vg, x, y + 1.5f, w, h, h / 2, 5, nvgRGBA(0, 0, 0, 16), nvgRGBA(0, 0, 0, 92));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, r);
        nvgFillPaint(vg, bg);
        nvgFill(vg);
    }

}
