/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.ArrayList;
import java.util.List;
import org.mini.glfm.Glfm;
import static org.mini.gui.GObject.HEIGHT;
import static org.mini.gui.GObject.isInBoundle;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Gutil.toUtf8;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_LEFT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_MIDDLE;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgImagePattern;
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
public class GMenu extends GObject {

    int selectIndex;
    float[] lineh = new float[1];
    boolean touched = false;
    List<GMenuItem> items = new ArrayList();

    class GMenuItem extends GObject {

        String tag;
        GImage img;

        GMenuItem(String t, GImage i) {
            tag = t;
            img = i;
        }
    }

    public GMenu(int left, int top, int width, int height) {
        boundle[LEFT] = left;
        boundle[TOP] = top;
        boundle[WIDTH] = width;
        boundle[HEIGHT] = height;
    }

    public void setPos(float x, float y) {
        boundle[LEFT] = x;
        boundle[TOP] = y;
        reAlign();
    }

    public void addItem(int index, String itemTag, GImage img) {
        items.add(index, new GMenuItem(itemTag, img));
        reAlign();
    }

    public void addItem(String itemTag, GImage img) {
        items.add(new GMenuItem(itemTag, img));
        reAlign();
    }

    public void removeItem(int index) {
        items.remove(index);
        reAlign();
    }

    private void reAlign() {
        if (items.size() > 0) {
            float item_w = boundle[WIDTH] / items.size();
            float item_h = boundle[HEIGHT];
            int i = 0;
            for (GMenuItem item : items) {
                item.boundle[LEFT] = boundle[LEFT] + i * item_w;
                item.boundle[TOP] = boundle[TOP];
                item.boundle[WIDTH] = item_w;
                item.boundle[HEIGHT] = item_h;
                i++;
            }
        }
    }

    public int getSelectIndex() {
        return selectIndex;
    }

    @Override
    public void touchEvent(int phase, int x, int y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        //System.out.println("in menu");
        if (isInBoundle(boundle, rx, ry)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                //System.out.println("in pressed");
                touched = true;
                int i = 0;
                for (GMenuItem item : items) {
                    if (isInBoundle(item.boundle, rx, ry)) {
                        //System.out.println("selected " + i);
                        selectIndex = i;
                        break;
                    }
                    i++;
                }
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                touched = false;
                int i = 0;
                for (GMenuItem item : items) {
                    if (isInBoundle(item.boundle, rx, ry)) {
                        if (actionListener != null && selectIndex == i) {
                            actionListener.action(this);
                        }
                        break;
                    }
                    i++;
                }
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

        Nanovg.nvgScissor(vg, x, y, w, h);
        //画底板
        byte[] bg;
        float cornerRadius = 4.0f;
        float[] color = null;
        //System.out.println("draw==========="+touched);
        //background
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1f, y + 1f, w - 2, h - 2, cornerRadius - 0.5f);
        nvgFillColor(vg, nvgRGBA(0, 0, 0, 255));
        nvgFill(vg);

        //touched item background
        if (touched) {
            nvgFillColor(vg, nvgRGBA(255, 255, 255, 48));
            GMenuItem mi = items.get(selectIndex);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, mi.boundle[LEFT] + 1, mi.boundle[TOP] + 1, mi.boundle[WIDTH] - 2, mi.boundle[HEIGHT] - 2, cornerRadius - 0.5f);
            nvgFill(vg);
            //System.out.println("draw touched");
        }

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

        float pad = 5;
        byte[] imgPaint;

        int i = 0;
        for (GMenuItem item : items) {
            float dx = item.boundle[LEFT];
            float dy = item.boundle[TOP];
            float dw = item.boundle[WIDTH];
            float dh = item.boundle[HEIGHT];

            float tag_x = 0f, tag_y = 0f, img_x = 0f, img_y = 0f, img_w = 0f, img_h = 0f;

            if (item.img != null) {
                if (item.tag != null) {
                    img_h = dh * .7f - pad - lineh[0];
                    img_x = dx + dw / 2 - img_h / 2;
                    img_w = img_h;
                    img_y = dy + dh * .2f;
                    tag_x = dx + dw / 2;
                    tag_y = img_y + img_h + pad + lineh[0] / 2;
                } else {
                    img_h = dh * .7f - pad - lineh[0];
                    img_x = dx + dw / 2 - img_h / 2;
                    img_w = img_h;
                    img_y = dy + dh / 2 - img_h / 2;
                }
            } else if (item.tag != null) {
                tag_x = dx + dw / 2;
                tag_y = dy + dh / 2;
            }
            //画图
            if (item.img != null) {
                imgPaint = nvgImagePattern(vg, img_x, img_y, img_w, img_h, 0.0f / 180.0f * (float) Math.PI, item.img.getTexture(), 0.8f);
                nvgBeginPath(vg);
                nvgRoundedRect(vg, img_x, img_y, img_w, img_h, 5);
                nvgFillPaint(vg, imgPaint);
                nvgFill(vg);
            }
            //画文字
            if (item.tag != null) {
                byte[] b = toUtf8(item.tag);
                nvgFillColor(vg, nvgRGBA(0, 0, 0, 96));
                Nanovg.nvgTextJni(vg, tag_x + 1, tag_y + 1, b, 0, b.length);
                nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
                Nanovg.nvgTextJni(vg, tag_x, tag_y, b, 0, b.length);
            }

            //畫竖线
            if (i > 0) {
                nvgBeginPath(vg);
                nvgFillColor(vg, nvgRGBA(0, 0, 0, 48));
                nvgRect(vg, dx - 1, dy + 2, 2, h - 4);
                nvgFill(vg);
            }
            i++;
        }
        return true;
    }

}
