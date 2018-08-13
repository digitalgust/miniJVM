/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.Iterator;
import static org.mini.gui.GToolkit.nvgRGBA;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_MIDDLE;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgLinearGradient;
import static org.mini.nanovg.Nanovg.nvgRect;
import static org.mini.nanovg.Nanovg.nvgRoundedRect;
import static org.mini.nanovg.Nanovg.nvgStroke;
import static org.mini.nanovg.Nanovg.nvgStrokeColor;
import static org.mini.nanovg.Nanovg.nvgTextAlign;
import static org.mini.nanovg.Nanovg.nvgTextMetrics;

/**
 *
 * @author Gust
 */
public class GMenu extends GContainer {

    float[] lineh = new float[1];


    public GMenu(int left, int top, int width, int height) {
        setLocation(left, top);
        setSize(width, height);
    }

    @Override
    public void setLocation(float x, float y) {
        super.setLocation(x, y);
        reAlign();
    }

    public GMenuItem addItem(int index, String itemTag, GImage img) {
        GMenuItem item = new GMenuItem(itemTag, img, GMenu.this);
        add(index, item);
        return item;
    }

    public GMenuItem addItem(String itemTag, GImage img) {
        GMenuItem item = new GMenuItem(itemTag, img, GMenu.this);
        add(item);
        return item;
    }

    public void removeItem(int index) {
        remove(index);
    }

    @Override
    public void onAdd(GObject obj) {
        super.onAdd(obj);
        reAlign();
    }

    @Override
    public void onRemove(GObject obj) {
        super.onRemove(obj);
        reAlign();
    }

    private void reAlign() {
        int size = elements.size();
        if (size > 0) {
            float item_w = getW() / size;
            float item_h = getH();
            int i = 0;
            for (Iterator it = elements.iterator(); it.hasNext();) {
                GMenuItem item = (GMenuItem) it.next();
                item.setLocation(i * item_w, 0);
                item.setSize(item_w, item_h);
                i++;
            }
        }
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

        //画底板
        byte[] bg;
        float cornerRadius = 4.0f;
        //System.out.println("draw==========="+touched);
        //background
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1f, y + 1f, w - 2, h - 2, cornerRadius - 0.5f);
        nvgFillColor(vg, nvgRGBA(0, 0, 0, 255));
        nvgFill(vg);

        //渐变
        bg = nvgLinearGradient(vg, x, y, x, y + h, nvgRGBA(255, 255, 255, 32), nvgRGBA(0, 0, 0, 32));
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, cornerRadius - 1);
        nvgFillPaint(vg, bg);
        nvgFill(vg);

        //边框
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, cornerRadius - 0.5f);
        nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
        nvgStroke(vg);

        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, Nanovg.NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

        nvgTextMetrics(vg, null, null, lineh);

        int i = 0;
        for (Iterator it = elements.iterator(); it.hasNext();) {
            //畫竖线
            GMenuItem item = (GMenuItem) it.next();
            float dx = item.getX();
            float dy = item.getY();
            if (i > 0) {
                nvgBeginPath(vg);
                nvgFillColor(vg, nvgRGBA(0, 0, 0, 48));
                nvgRect(vg, dx - 1, dy + 2, 2, h - 4);
                nvgFill(vg);
            }
            i++;
        }
        
        super.update(vg);
        return true;
    }

}
