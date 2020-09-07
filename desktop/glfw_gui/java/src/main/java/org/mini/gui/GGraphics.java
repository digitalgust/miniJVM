/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.nanovg.Gutil;
import org.mini.nanovg.StbFont;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GGraphics {

    public static final int LEFT = NVG_ALIGN_LEFT;
    public static final int HCENTER = NVG_ALIGN_CENTER;
    public static final int RIGHT = NVG_ALIGN_RIGHT;
    public static final int TOP = NVG_ALIGN_TOP;
    public static final int VCENTER = NVG_ALIGN_MIDDLE;
    public static final int BOTTOM = NVG_ALIGN_BOTTOM;
    public static final int BASELINE = NVG_ALIGN_BASELINE;

    protected GCanvas canvas;
    protected long vg;
    protected StbFont font;

    int curColor = 0;
    byte r, g, b, a;
    float fontSize = GToolkit.getStyle().getTextFontSize();
    int clipX, clipY, clipW, clipH;

    public GGraphics(GCanvas canvas, long context) {
        this.canvas = canvas;
        vg = context;
    }

    public long getNvContext() {
        return vg;
    }

    public GCanvas getCanvas() {
        return canvas;
    }

    public void setColor(int pr, int pg, int pb) {
        r = (byte) (pr & 0xff);
        g = (byte) (pg & 0xff);
        b = (byte) (pb & 0xff);
        curColor = (curColor & 0xff000000) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    public void setColor(int argb) {
        curColor = argb;
        a = (byte) (0xff);
        r = (byte) (0xff & (argb >> 16));
        g = (byte) (0xff & (argb >> 8));
        b = (byte) (0xff & (argb >> 0));
    }

    public void setAlpha(int b) {
        curColor = (curColor & 0x00ffffff) | ((b & 0xff) << 24);
    }

    public void setARGBColor(int argb) {
        curColor = argb;
        a = (byte) (0xff & (argb >> 24));
        r = (byte) (0xff & (argb >> 16));
        g = (byte) (0xff & (argb >> 8));
        b = (byte) (0xff & (argb >> 0));
    }

    public void fillRect(int x, int y, int w, int h) {

        x += canvas.getX();
        y += canvas.getY();

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, 0);
        nvgFillColor(vg, nvgRGBA(r, g, b, a));
        nvgFill(vg);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        x += canvas.getX();
        y += canvas.getY();
        nvgBeginPath(vg);
        nvgFillColor(vg, nvgRGBA(r, g, b, a));
        nvgArc(vg, x, y, width / 2, (float) (startAngle * Math.PI / 180), (float) (arcAngle * Math.PI / 180), NVG_CW);
        nvgClosePath(vg);
        nvgFill(vg);
    }

    int thickness = 1;

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {

        x += canvas.getX();
        y += canvas.getY();
        nvgBeginPath(vg);
        nvgArc(vg, x, y, width / 2, (float) (startAngle * Math.PI / 180), (float) (arcAngle * Math.PI / 180), NVG_CW);
        nvgClosePath(vg);
        nvgStroke(vg);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {

        x1 += canvas.getX();
        y1 += canvas.getY();
        x2 += canvas.getX();
        y2 += canvas.getY();

        nvgStrokeColor(vg, nvgRGBA(r, g, b, a));
        nvgBeginPath(vg);
        nvgMoveTo(vg, x1, y1);
        nvgLineTo(vg, x2, y2);
        nvgStroke(vg);
    }

    public void drawRect(int x1, int y1, int w, int h) {
        x1 += canvas.getX();
        y1 += canvas.getY();
        nvgRoundedRect(vg, x1, y1, w, h, 0);
        nvgStrokeColor(vg, nvgRGBA(r, g, b, a));
        nvgStroke(vg);
    }

    public void drawString(String str, int x, int y, int anchor) {
        x += canvas.getX();
        y += canvas.getY();
        nvgTextAlign(vg, anchor);

        byte[] ba = Gutil.toUtf8(str);
        if (ba == null || ba.length <= 0) {
            return;
        }

        nvgFillColor(vg, nvgRGBA(r, g, b, a));
        nvgTextJni(vg, x, y, ba, 0, ba.length);
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {

        x += canvas.getX();
        y += canvas.getY();
        nvgTextAlign(vg, anchor);
        str = str.substring(offset, len);
        byte[] b = Gutil.toUtf8(str);
        if (b == null || b.length <= 0) {
            return;
        }
        nvgTextJni(vg, x, y, b, 0, b.length);
    }

    public void drawChar(char character, int x, int y, int anchor) {
        x += canvas.getX();
        y += canvas.getY();
        nvgTextAlign(vg, anchor);
        byte[] b = Gutil.toUtf8(character + "");
        nvgTextJni(vg, x, y, b, 0, b.length);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        x += canvas.getX();
        y += canvas.getY();
        nvgTextAlign(vg, anchor);
        String s = new String(data, offset, length);
        drawString(s, x, y, anchor);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        x1 += canvas.getX();
        y1 += canvas.getY();
        x2 += canvas.getX();
        y2 += canvas.getY();
        x3 += canvas.getX();
        y3 += canvas.getY();
        nvgFillColor(vg, nvgRGBA(r, g, b, a));
        nvgBeginPath(vg);
        nvgMoveTo(vg, x1, y1);
        nvgLineTo(vg, x2, y2);
        nvgLineTo(vg, x3, y3);
        nvgLineTo(vg, x1, y1);
        nvgClosePath(vg);
        nvgFill(vg);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        x1 += canvas.getX();
        y1 += canvas.getY();
        x2 += canvas.getX();
        y2 += canvas.getY();
        x3 += canvas.getX();
        y3 += canvas.getY();
        nvgStrokeColor(vg, nvgRGBA(r, g, b, a));
        nvgBeginPath(vg);
        nvgMoveTo(vg, x1, y1);
        nvgLineTo(vg, x2, y2);
        nvgLineTo(vg, x3, y3);
        nvgLineTo(vg, x1, y1);
        nvgStroke(vg);
    }

    public void drawImage(GImage img, int x, int y, int anchor) {
        drawImage(img, x, y, img.getWidth(), img.getHeight(), anchor);
    }

    public void drawImage(GImage img, int x, int y, int w, int h, int anchor) {

        x += canvas.getX();
        y += canvas.getY();

        byte[] imgPaint = nvgImagePattern(vg, x, y, w, h, 0.0f / 180.0f * (float) Math.PI, img.getTexture(vg), 1f);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, 0);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);
    }

    public void drawRegion(GImage src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int anchor) {
        x_src += canvas.getX();
        y_src += canvas.getY();

        x_dest += canvas.getX();
        y_dest += canvas.getY();
    }

    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
        x += canvas.getX();
        y += canvas.getY();
    }

    public int getColor() {
        return curColor;
    }

    public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
        x_src += canvas.getX();
        y_src += canvas.getY();

        x_dest += canvas.getX();
        y_dest += canvas.getY();
    }

    public void clipRect(int x, int y, int width, int height) {
        setClip(x, y, width, height);
    }

    public void translate(int x, int y) {
        nvgTranslate(vg, x, y);
    }

    /**
     * @return the fontSize
     */
    public float getFontSize() {
        return fontSize;
    }

    /**
     * @param fontSize the fontSize to set
     */
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setClip(int x, int y, int w, int h) {
        clipX = x;
        clipY = y;
        clipW = w;
        clipH = h;
        nvgScissor(vg, x, y, w, h);
    }

    public int getClipX() {
        return clipX;
    }

    public int getClipY() {
        return clipY;
    }

    public int getClipWidth() {
        return clipW;
    }

    public int getClipHeight() {
        return clipH;
    }


}
