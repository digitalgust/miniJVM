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
    protected long blinkPeriod = 0;       //呼吸边框周期ms, 0=关闭
    protected float[] blinkColor = null;  //呼吸边框颜色, null=用getColor()
    protected final float[] blinkStroke = new float[4]; //渲染时复用, 避免每帧分配

    public GImageItem(GForm form) {
        this(form, null);
    }

    public GImageItem(GForm form, GImage img) {
        super(form);
        this.img = img;
        setCornerRadius(5.f);
    }

    /**
     * 开启呼吸边框: 周期periodMs内边框alpha在0~原值间平滑起伏, 用于选中提示
     */
    public void setBlink(long periodMs) {
        this.blinkPeriod = periodMs;
    }

    public void setBlink(long periodMs, float[] color) {
        this.blinkPeriod = periodMs;
        this.blinkColor = color;
    }

    /**
     * 描边用颜色: 闪烁关闭时返回普通color, 开启时按当前时间调制alpha
     */
    protected float[] getBlinkStroke() {
        float[] c = getColor();
        if (blinkPeriod <= 0) {
            return c;
        }
        if (blinkColor != null) {
            c = blinkColor;
        }
        float ph = (System.currentTimeMillis() % blinkPeriod) / (float) blinkPeriod;
        float a = 0.5f - 0.5f * (float) Math.cos(ph * 2f * (float) Math.PI);
        blinkStroke[0] = c[0];
        blinkStroke[1] = c[1];
        blinkStroke[2] = c[2];
        blinkStroke[3] = c[3] * a;
        return blinkStroke;
    }

    private void strokeBorder(long vg, float x, float y, float w, float h) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, getCornerRadius() - 2.f);
        nvgStrokeWidth(vg, 1.0f);
        nvgStrokeColor(vg, getBlinkStroke());
        nvgStroke(vg);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
    }


    public boolean paint(long vg) {
        super.paint(vg);
        float x = getX();
        float y = getY();
        return paintFlying(vg, x, y);
    }

    int[] imgw = {0}, imgh = {0};

    boolean paintFlying(long vg, float x, float y) {
        float w = getW();
        float h = getH();
        if (img == null) {
            //无前景图时不画任何东西, 但闪烁中(选中态)仍画呼吸边框表示选中
            if (drawBorder && blinkPeriod > 0) {
                strokeBorder(vg, x, y, w, h);
            }
            return true;
        }
        float ix, iy, iw, ih;

        imgw[0] = img.getWidth();
        imgh[0] = img.getHeight();
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
                float scalex = (iw - 4) / (float) imgw[0];
                float scaley = (ih - 4) / (float) imgh[0];
                imgPaint = nvgImagePattern(vg, x + ix + 2 - img.getSx() * scalex, y + iy + 2 - img.getSy() * scaley, img.getTexWidth() * scalex, img.getTexHeight() * scaley, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), a);
                nvgBeginPath(vg);
                nvgRoundedRect(vg, x, y, w, h, getCornerRadius());
                nvgFillPaint(vg, imgPaint);
                nvgFill(vg);
            } else {
                float scalex = (iw - 2) / (float) imgw[0];
                float scaley = (ih - 2) / (float) imgh[0];
                byte[] imgPaint = nvgImagePattern(vg, x + ix + 1 - img.getSx() * scalex, y + iy + 1 - img.getSy() * scaley, img.getTexWidth() * scalex, img.getTexHeight() * scaley, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), a);
                nvgBeginPath(vg);
                nvgRoundedRect(vg, x, y, w, h, 0);
                nvgFillPaint(vg, imgPaint);
                nvgFill(vg);
            }
        }
        //画框
        if (drawBorder) {

            float r = getCornerRadius();
            byte[] shadowPaint;
            shadowPaint = nvgBoxGradient(vg, x, y, w, h, 5, 3, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
            nvgBeginPath(vg);
            nvgRect(vg, x - r, y - r, w + r * 2, h + r * 2);
            nvgRoundedRect(vg, x, y, w, h, 6);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, shadowPaint);
            nvgFill(vg);

            strokeBorder(vg, x, y, w, h);
        }
        //画字
        if (getText() != null) {
            GToolkit.drawTextWithShadow(vg, x + 3, y + 3, w - 6, h - 6, getText(), getFontSize(), getColor(), GToolkit.getStyle().getTextShadowColor(), 1f);
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

    boolean touched;
    float oldX, oldY;

    private boolean validAction(float releaseX, float releaseY) {
        if (Math.abs(releaseX - oldX) < TOUCH_RANGE && Math.abs(releaseY - oldY) < TOUCH_RANGE) {
            return true;
        }
        return false;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed) {
                touched = true;
                parent.setCurrent(this);
                oldX = x;
                oldY = y;
                doStateChanged(this);
            } else {
                if (validAction(x, y)) doAction();
                touched = false;
                doStateChanged(this);
            }
        }
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        if (!isInArea(x, y) && touched) {
            touched = false;
            doStateChanged(this);
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan:
                    touched = true;
                    oldX = x;
                    oldY = y;
                    doStateChanged(this);
                    break;
                case Glfm.GLFMTouchPhaseEnded:
                    if (validAction(x, y)) doAction();
                    touched = false;
                    doStateChanged(this);
                    break;
            }
        } else if (!isInArea(x, y)) {
            if (touched) {
                touched = false;
                doStateChanged(this);
            }
        }
    }

}
