/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author Gust
 */
public class GImageItem extends GObject {

    protected GImage img;
    protected float alpha = 1.f;
    protected float alphaFly = alpha * .5f;
    protected boolean drawBorder = true;


    public GImageItem(GForm form) {
        this(form, null);
    }

    public GImageItem(GForm form, GImage img) {
        super(form);
        this.img = img;
    }

    @Override
    public void setText(String text) {
        super.setText(text);
    }


    public boolean paint(long vg) {

        float x = getX();
        float y = getY();
        return paintFlying(vg, x, y);
    }

    boolean paintFlying(long vg, float x, float y) {
        float w = getW();
        float h = getH();
        if (img == null) {
            return true;
        }
        float ix, iy, iw, ih;
        int[] imgw = {0}, imgh = {0};

        nvgImageSize(vg, img.getNvgTextureId(vg), imgw, imgh);
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
        float a = alpha;
        if (getForm().getFlyingObject() == this) {
            a = alphaFly;
        }

        //画图
        if (img != null) {
            if (drawBorder) {
                byte[] imgPaint;
                imgPaint = nvgImagePattern(vg, x + ix + 2, y + iy + 2, iw - 4, ih - 4, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), a);
                nvgBeginPath(vg);
                nvgRoundedRect(vg, x, y, w, h, 5);
                nvgFillPaint(vg, imgPaint);
                nvgFill(vg);
            } else {
                byte[] imgPaint = nvgImagePattern(vg, x + ix + 1, y + iy + 1, iw - 2, ih - 2, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), a);
                nvgBeginPath(vg);
                nvgRoundedRect(vg, x, y, w, h, 0);
                nvgFillPaint(vg, imgPaint);
                nvgFill(vg);
            }
        }
        //画框
        if (drawBorder) {

            byte[] shadowPaint;
            shadowPaint = nvgBoxGradient(vg, x, y, w, h, 5, 3, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
            nvgBeginPath(vg);
            nvgRect(vg, x - 5, y - 5, w + 10, h + 10);
            nvgRoundedRect(vg, x, y, w, h, 6);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, shadowPaint);
            nvgFill(vg);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, 3.5f);
            nvgStrokeWidth(vg, 1.0f);
            nvgStrokeColor(vg, getColor());
            nvgStroke(vg);
        }
        //画字
        if (getText() != null) {
            GToolkit.drawTextWithShadow(vg, x + 3, y + 3, w - 6, h - 6, getText(), getFontSize(), getColor(), GToolkit.getStyle().getTextShadowColor());
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
        this.alphaFly = alpha * .5f;
    }

    /**
     * @return the img
     */
    public GImage getImg() {
        return img;
    }

    /**
     * @param img the img to set
     */
    public void setImg(GImage img) {
        this.img = img;
    }

    /**
     * @return the drawBorder
     */
    public boolean isDrawBorder() {
        return drawBorder;
    }

    /**
     * @param drawBorder the drawBorder to set
     */
    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    boolean bt_pressed;
    float oldX, oldY;

    private boolean validAction(float releaseX, float releaseY) {
        if (releaseX >= oldX && releaseX <= oldX + getW() && releaseY >= oldY && releaseY < oldY + getH()) {
            return true;
        }
        return false;
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                bt_pressed = true;
                oldX = getX();
                oldY = getY();
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                if (validAction(x, y)) doAction();
                bt_pressed = false;
            } else if (!isInArea(x, y)) {
                bt_pressed = false;
            }
        }
    }

    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (button == Glfw.GLFW_MOUSE_BUTTON_1) {//left
                if (pressed) {
                    bt_pressed = true;
                    oldX = getX();
                    oldY = getY();
                } else {
                    if (validAction(x, y)) doAction();
                    bt_pressed = false;
                }
            }
        } else {
            bt_pressed = false;
        }
    }
}
