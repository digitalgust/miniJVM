/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.nanovg.Nanovg;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author Gust
 */
public class GMenuItem extends GObject {

    protected GImage img;

    protected float[] lineh = new float[1];
    protected boolean touched = false;

    protected int redPoint;
    float[] box;

    GMenuItem(GForm form, String t, GImage i, GMenu _parent) {
        super(form);
        setText(t);
        img = i;
        parent = _parent;
        setCornerRadius(4.f);
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        if (t == null) return;
        long vg = GCallBack.instance.getNvContext();
        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextMetrics(vg, null, null, lineh);
        box = GToolkit.getTextBoundle(vg, t, getW(), GToolkit.getStyle().getTextFontSize());
    }

    boolean isSelected() {
        if (parent instanceof GMenu) {
            GMenu menu = (GMenu) parent;
            if (menu.getElementsImpl().indexOf(this) == menu.selectedIndex) {
                return true;
            }
        }
        return false;
    }

    void setSelected() {
        if (parent instanceof GMenu) {
            GMenu menu = (GMenu) parent;
            menu.selectedIndex = menu.getElementsImpl().indexOf(this);
        }
    }

    public void incMsgNew(int count) {
        redPoint += count;
    }

    public void resetMsgNew() {
        redPoint = 0;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed && button == Glfw.GLFW_MOUSE_BUTTON_1) {
                touched = true;
                doAction();
                doStateChanged(this);
            } else if (!pressed && button == Glfw.GLFW_MOUSE_BUTTON_1) {
                touched = false;
                doStateChanged(this);
            }
        }

    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                touched = true;
                doStateChanged(this);
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                touched = false;
                doAction();
                doStateChanged(this);
            }
        }

    }

    public boolean isPressed() {
        return touched;
    }

    public boolean paint(long vg) {


        super.paint(vg);

        //touched item background
        if (touched) {
            nvgFillColor(vg, nvgRGBA(255, 255, 255, 48));
            nvgBeginPath(vg);
            nvgRoundedRect(vg, getX() + 1, getY() + 1, getW() - 2, getH() - 2, getCornerRadius() - 0.5f);
            nvgFill(vg);
            //System.out.println("draw touched");
            touched = false;
        }

        float pad = 2;
        byte[] imgPaint;
        float dx = getX();
        float dy = getY();
        float dw = getW();
        float dh = getH();

        float txt_x = 0f, txt_y = 0f, img_x = 0f, img_y = 0f, img_w = 0f, img_h = 0f;

        if (img != null) {//有图
            if (text != null) {//有文字
                if (dh > lineh[0] * 2) { //上图下文排列
                    img_h = dh * .65f - pad - lineh[0];
                    img_x = dx + dw / 2 - img_h / 2;
                    img_w = img_h;
                    img_y = dy + dh * .2f;
                    txt_x = dx + dw / 2;
                    txt_y = img_y + img_h + pad + lineh[0] / 2;
                } else { //前图后文
                    img_h = dh * .7f - pad;
                    img_w = img_h;
                    img_x = dx + dw * .5f - pad - (img_w + box[WIDTH]) * .5f;
                    img_y = dy + dh * .2f;
                    txt_x = dx + dw * .5f + img_w * .5f + pad;
                    txt_y = img_y + pad + lineh[0] / 2;
                }
            } else {
                img_h = dh * .75f - pad;
                img_x = dx + dw / 2 - img_h / 2;
                img_w = img_h;
                img_y = dy + dh / 2 - img_h / 2;
            }
        } else if (text != null) {
            txt_x = dx + dw / 2;
            txt_y = dy + dh / 2;
        }
        //画图
        if (img != null) {
            float alpha = 1.f;
            if (!isSelected()) {
                alpha = 0.9f;
            }
            imgPaint = nvgImagePattern(vg, img_x, img_y, img_w, img_h, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), alpha);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, img_x, img_y, img_w, img_h, 5);
            nvgFillPaint(vg, imgPaint);
            nvgFill(vg);
        }
        //画文字
        if (text != null) {
            byte[] b = toCstyleBytes(text);
            nvgFillColor(vg, GToolkit.getStyle().getTextShadowColor());
            Nanovg.nvgTextJni(vg, txt_x + 1, txt_y + 1, b, 0, b.length);
            nvgFillColor(vg, enable ? getColor() : getDisabledColor());
            Nanovg.nvgTextJni(vg, txt_x, txt_y, b, 0, b.length);
        }

        if (redPoint > 0) {
            GToolkit.drawRedPoint(vg, redPoint > 99 ? "..." : Integer.toString(redPoint), dx + dw * .7f, dy + dh * .5f - 10, 12f);
        }
        return true;
    }
}
