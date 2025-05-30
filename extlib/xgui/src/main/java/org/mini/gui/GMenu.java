/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.nanovg.Nanovg;

import java.util.Iterator;

import static org.mini.gui.GToolkit.FEEL_DIMENSION;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author Gust
 */
public class GMenu extends GContainer {

    protected float[] lineh = new float[1];
    protected int selectedIndex = -1;

    protected int markIndex = -1;
    /**
     * contextMenu :
     * change focus when this menu touched , the value true would not change current focus, false would chang
     * like the edit menu "copy" "paste" can not change the current ui focus
     */
    protected boolean contextMenu = false;

    public GMenu(GForm form) {
        this(form, 0f, 0f, 1f, 1f);
    }

    public GMenu(GForm form, float left, float top, float width, float height) {
        super(form);
        layer = LAYER_MENU_OR_POPUP;
        setLocation(left, top);
        setSize(width, height);

        setCornerRadius(4.f);
    }


    @Override
    public void setLocation(float x, float y) {
        super.setLocation(x, y);
        reAlign();
    }

    @Override
    public void setSize(float w, float h) {
        super.setSize(w, h);
        reAlign();
    }

    public GMenuItem addItem(int index, String itemTag, GImage img) {
        GMenuItem item = new GMenuItem(form, itemTag, img, GMenu.this);
        addImpl(index, item);
        return item;
    }

    public GMenuItem addItem(String itemTag, GImage img) {
        GMenuItem item = new GMenuItem(form, itemTag, img, GMenu.this);
        addImpl(item);
        return item;
    }

    public void removeItem(int index) {
        removeImpl(index);
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

    public void reAlign() {
        int size = elements.size();
        if (size > 0) {
            float item_w = getW() / size;
            float item_h = getH();
            int i = 0;
            for (Iterator it = elements.iterator(); it.hasNext(); ) {
                GMenuItem item = (GMenuItem) it.next();
                item.setLocation(i * item_w, 0);
                item.setSize(item_w, item_h);
                i++;
            }
        }
    }


    static float[] GRADIENT_COLOR0 = {1.0f, 1.0f, 1.0f, 0.125f};
    static float[] GRADIENT_COLOR1 = {0.0f, 0.0f, 0.0f, 0.125f};
    static float[] BORDER_COLOR = {0.0f, 0.0f, 0.0f, 0.2f};
    static float[] SPREATOR_COLOR = {192f / 255f, 192f / 255f, 192f / 255f, 0.2f};

    /**
     * @param vg
     * @return
     */
    public boolean paint(long vg) {

        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        //画底板
        byte[] bg;
        //System.out.println("draw==========="+touched);
        //background
        //渐变
        if (GToolkit.getFeel() == FEEL_DIMENSION) {
            bg = nvgLinearGradient(vg, x, y, x, y + h, GRADIENT_COLOR0, GRADIENT_COLOR1);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, getCornerRadius() - 1);
            nvgFillPaint(vg, bg);
            nvgFill(vg);
        } else {
//            nvgBeginPath(vg);
//            nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, getCornerRadius() - 0.5f);
//            nvgFillColor(vg, GRADIENT_COLOR0);
//            nvgFill(vg);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 1f, y + 1f, w - 2, h - 2, getCornerRadius() - 0.5f);
            nvgFillColor(vg, this.bgColor != null ? bgColor : GToolkit.getStyle().getPopBackgroundColor());
            nvgFill(vg);
        }

        //边框
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, getCornerRadius() - 0.5f);
        nvgStrokeColor(vg, BORDER_COLOR);
        nvgStroke(vg);

        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, Nanovg.NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

        nvgTextMetrics(vg, null, null, lineh);

        for (int i = 0, imax = elements.size(); i < imax; i++) {
            //畫竖线
            GMenuItem item = (GMenuItem) elements.get(i);
            float dx = item.getX();
            float dy = item.getY();
            if (i > 0) {
                nvgBeginPath(vg);
                nvgFillColor(vg, nvgRGBA(192, 192, 192, 48));
                nvgRect(vg, dx - 1, dy + 3, 2, h - 6);
                nvgFill(vg);
            }
        }
        super.paint(vg);
        return true;
    }

    /**
     * @return the contextMenu
     */
    @Override
    public boolean isContextMenu() {
        return contextMenu;
    }

    /**
     * @param contextMenu the contextMenu to set
     */
    public void setContextMenu(boolean contextMenu) {
        this.contextMenu = contextMenu;
    }

    public void setMarkIndex(int markIndex) {
        if (markIndex >= getElementSize()) {
            markIndex = getElementSize() - 1;
        }
        this.markIndex = markIndex;
    }

    public int getMarkIndex() {
        return markIndex;
    }
}
