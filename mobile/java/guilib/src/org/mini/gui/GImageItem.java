/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgImagePattern;
import static org.mini.nanovg.Nanovg.nvgRoundedRect;

/**
 *
 * @author Gust
 */
public class GImageItem extends GObject {

    GImage img;

    public GImageItem(GImage img) {
        this.img = img;
    }

    @Override
    public int getType() {
        return TYPE_IMAGEITEM;
    }

    public boolean update(long vg) {

        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        byte[] imgPaint = nvgImagePattern(vg, x, y, w, h, 0.0f / 180.0f * (float) Math.PI, img.getTexture(), 1f);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, 0);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);

        return true;
    }

}
