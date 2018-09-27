/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.NVG_HOLE;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgBoxGradient;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgImagePattern;
import static org.mini.nanovg.Nanovg.nvgImageSize;
import static org.mini.nanovg.Nanovg.nvgPathWinding;
import static org.mini.nanovg.Nanovg.nvgRect;
import static org.mini.nanovg.Nanovg.nvgRoundedRect;
import static org.mini.nanovg.Nanovg.nvgStroke;
import static org.mini.nanovg.Nanovg.nvgStrokeColor;
import static org.mini.nanovg.Nanovg.nvgStrokeWidth;

/**
 *
 * @author Gust
 */
public class GImageItem extends GObject {

    GImage img;
    float alpha = 1.f;
    boolean drawBoader = true;

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
        if (img == null) {
            return true;
        }
        if (drawBoader) {
            if (img == null) {
                return true;
            }

            byte[] shadowPaint, imgPaint;
            float ix, iy, iw, ih;
            int[] imgw = {0}, imgh = {0};

            nvgImageSize(vg, img.getTexture(), imgw, imgh);
            if (imgw[0] < imgh[0]) {
                iw = w;
                ih = iw * (float) imgh[0] / (float) imgw[0];
                ix = 0;
                iy = -(ih - h) * 0.5f;
            } else {
                ih = h;
                iw = ih * (float) imgw[0] / (float) imgh[0];
                ix = -(iw - w) * 0.5f;
                iy = 0;
            }

            imgPaint = nvgImagePattern(vg, x + ix + 1, y + iy + 1, iw - 2, ih - 2, 0.0f / 180.0f * (float) Math.PI, img.getTexture(), 1.0f);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, w, h, 5);
            nvgFillPaint(vg, imgPaint);
            nvgFill(vg);

            shadowPaint = nvgBoxGradient(vg, x, y, w, h, 5, 3, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
            nvgBeginPath(vg);
            nvgRect(vg, x - 5, y - 5, w + 10, h + 10);
            nvgRoundedRect(vg, x, y, w, h, 6);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, shadowPaint);
            nvgFill(vg);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x - 0.5f, y - 0.5f, w, h, 4 - 0.5f);
            nvgStrokeWidth(vg, 1.0f);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);
        } else {

            byte[] imgPaint = nvgImagePattern(vg, x, y, w, h, 0.0f / 180.0f * (float) Math.PI, img.getTexture(), alpha);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, w, h, 0);
            nvgFillPaint(vg, imgPaint);
            nvgFill(vg);
        }
        return true;
    }

    /**
     * @return the alpha
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * @param alpha the alpha to set
     */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

}
