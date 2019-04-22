/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import static org.mini.nanovg.Nanovg.NVG_ALIGN_LEFT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_MIDDLE;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgTextAlign;

/**
 *
 * @author gust
 */
public class GCanvas extends GPanel {

    GGraphics g;

    public GCanvas() {

    }

    public GCanvas(int x, int y, int w, int h) {
        this((float) x, y, w, h);
    }

    public GCanvas(float x, float y, float w, float h) {
        setLocation(x, y);
        setSize(w, h);
    }

    public int getType() {
        return TYPE_CANVAS;
    }

    public boolean update(long vg) {
        if (g == null) {
            g = new GGraphics(this, vg);
        }
        nvgFontSize(vg, g.getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        paint(g);
        super.update(vg);
        return true;
    }

    public void paint(GGraphics g) {

    }
}
