/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Gutil.toUtf8;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgImagePattern;
import static org.mini.nanovg.Nanovg.nvgRoundedRect;
import static org.mini.nanovg.Nanovg.nvgTextMetrics;

/**
 *
 * @author Gust
 */
public class GMenuItem extends GObject {

    String tag;
    GImage img;

    float[] lineh = new float[1];
    boolean touched = false;

    GMenuItem(String t, GImage i, GMenu _parent) {
        tag = t;
        img = i;
        parent = _parent;

    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed && button == Glfw.GLFW_MOUSE_BUTTON_1) {
                touched = true;
            } else if (!pressed && button == Glfw.GLFW_MOUSE_BUTTON_1) {
                touched = false;
                if (actionListener != null) {
                    actionListener.action(this);
                }
            }
        }

    }

    @Override
    public void touchEvent(int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                touched = true;
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                touched = false;
                if (actionListener != null) {
                    actionListener.action(this);
                }
            }
        }

    }

    public boolean update(long vg) {

        float cornerRadius = 4.0f;
        
        nvgTextMetrics(vg, null, null, lineh);

        //touched item background
        if (touched) {
            nvgFillColor(vg, nvgRGBA(255, 255, 255, 48));
            nvgBeginPath(vg);
            nvgRoundedRect(vg, getViewX() + 1, getViewY() + 1, getViewW() - 2, getViewH() - 2, cornerRadius - 0.5f);
            nvgFill(vg);
            //System.out.println("draw touched");

        }

        float pad = 5;
        byte[] imgPaint;
        float dx = getX();
        float dy = getY();
        float dw = getW();
        float dh = getH();

        float tag_x = 0f, tag_y = 0f, img_x = 0f, img_y = 0f, img_w = 0f, img_h = 0f;

        if (img != null) {
            if (tag != null) {
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
        } else if (tag != null) {
            tag_x = dx + dw / 2;
            tag_y = dy + dh / 2;
        }
        //画图
        if (img != null) {
            imgPaint = nvgImagePattern(vg, img_x, img_y, img_w, img_h, 0.0f / 180.0f * (float) Math.PI, img.getTexture(), 0.8f);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, img_x, img_y, img_w, img_h, 5);
            nvgFillPaint(vg, imgPaint);
            nvgFill(vg);
        }
        //画文字
        if (tag != null) {
            byte[] b = toUtf8(tag);
            nvgFillColor(vg, nvgRGBA(0, 0, 0, 96));
            Nanovg.nvgTextJni(vg, tag_x + 1, tag_y + 1, b, 0, b.length);
            nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
            Nanovg.nvgTextJni(vg, tag_x, tag_y, b, 0, b.length);
        }

        return true;
    }
}
