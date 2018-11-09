/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import static org.mini.gui.GObject.LEFT;
import static org.mini.nanovg.Gutil.toUtf8;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_LEFT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_TOP;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgTextAlign;
import static org.mini.nanovg.Nanovg.nvgTextBoxBoundsJni;
import static org.mini.nanovg.Nanovg.nvgTextBoxJni;
import static org.mini.nanovg.Nanovg.nvgTextJni;
import static org.mini.nanovg.Nanovg.nvgTextMetrics;

/**
 *
 * @author gust
 */
public class GLabel extends GObject {

    String text;
    byte[] text_arr;
    char preicon;
    float[] lineh = {0};

    int align = NVG_ALIGN_LEFT | NVG_ALIGN_TOP;

    public GLabel() {

    }

    public GLabel(String text, int left, int top, int width, int height) {
        this(text, (float) left, top, width, height);
    }

    public GLabel(String text, float left, float top, float width, float height) {
        setText(text);
        setLocation(left, top);
        setSize(width, height);
    }

    public int getType() {
        return TYPE_LABEL;
    }

    public void setAlign(int ali) {
        align = ali;
    }

    public final void setText(String text) {
        this.text = text;
        text_arr = toUtf8(text);
    }

    public void setIcon(char icon) {
        preicon = icon;
    }

    /**
     *
     * @param vg
     * @return
     */
    public boolean update(long vg) {
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        drawText(vg, x, y, w, h);
        return true;
    }

    void drawLabel(long vg, float x, float y, float w, float h) {
        //NVG_NOTUSED(w);
        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());

        nvgTextAlign(vg, align);
        nvgTextJni(vg, x, y + h * 0.5f, text_arr, 0, text_arr.length);

    }

    void drawText(long vg, float x, float y, float w, float h) {

        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextMetrics(vg, null, null, lineh);

        nvgTextAlign(vg, align);
        float[] area = new float[]{x + 2f, y + 2f, w - 4f, h - 4f};
        float dx, dy;
        dx = area[LEFT];
        dy = area[TOP];
        if ((align & Nanovg.NVG_ALIGN_MIDDLE) != 0) {
            dy += lineh[0];
        } else if ((align & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
            dy += GToolkit.getStyle().getTextFontSize();
        }
//        if ((align & Nanovg.NVG_ALIGN_RIGHT) != 0) {
//            dx = area[LEFT] + area[WIDTH];
//        } else if ((align & Nanovg.NVG_ALIGN_CENTER) != 0) {
//            dx = area[LEFT] + area[WIDTH] / 2;
//        } else {
//            dx = area[LEFT];
//        }
//        if ((align & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
//            dy = area[TOP] + area[HEIGHT];
//        } else if ((align & Nanovg.NVG_ALIGN_MIDDLE) != 0) {
//            dy = area[TOP] + area[HEIGHT] / 2;
//        } else {
//            dy = area[TOP];
//        }

        if (text_arr != null) {
//            float[] bond = new float[4];
//            nvgTextBoxBoundsJni(vg, dx, dy, area[WIDTH], text_arr, 0, text_arr.length, bond);
//            bond[WIDTH] -= bond[LEFT];
//            bond[HEIGHT] -= bond[TOP];
//            bond[LEFT] = bond[TOP] = 0;
//
//            if (bond[HEIGHT] > area[HEIGHT]) {
//                dy -= bond[HEIGHT] - area[HEIGHT];
//            }
            nvgTextBoxJni(vg, dx, dy, area[WIDTH], text_arr, 0, text_arr.length);
        }
    }

}
