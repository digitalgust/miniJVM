/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.nanovg.Gutil;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_BASELINE;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_BOTTOM;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_CENTER;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_LEFT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_MIDDLE;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_RIGHT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_TOP;
import static org.mini.nanovg.Nanovg.NVG_CW;
import static org.mini.nanovg.Nanovg.nvgArc;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgClosePath;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgImagePattern;
import static org.mini.nanovg.Nanovg.nvgLineTo;
import static org.mini.nanovg.Nanovg.nvgMoveTo;
import static org.mini.nanovg.Nanovg.nvgReset;
import static org.mini.nanovg.Nanovg.nvgRoundedRect;
import static org.mini.nanovg.Nanovg.nvgSave;
import static org.mini.nanovg.Nanovg.nvgScissor;
import static org.mini.nanovg.Nanovg.nvgStroke;
import static org.mini.nanovg.Nanovg.nvgStrokeColor;
import static org.mini.nanovg.Nanovg.nvgStrokeWidth;
import static org.mini.nanovg.Nanovg.nvgTextJni;
import static org.mini.nanovg.Nanovg.nvgTranslate;

/**
 *
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

    GCanvas canvas;
    long vg;
    long font;

    int curColor = 0;
    byte r, g, b, a;

    GGraphics(GCanvas canvas, long context) {
        this.canvas = canvas;
        vg = context;
    }

    void save() {
        nvgSave(vg);
        nvgReset(vg);
        nvgStrokeWidth(vg, 1.0f);
        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
    }

    void restore() {
        nvgReset(vg);
    }

    public void setColor(int argb) {
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

        x1 += canvas.getX();;
        y1 += canvas.getY();;
        x2 += canvas.getX();;
        y2 += canvas.getY();;

        nvgStrokeColor(vg, nvgRGBA(r, g, b, a));
        nvgBeginPath(vg);
        nvgMoveTo(vg, x1, y1);
        nvgLineTo(vg, x2, y2);
        nvgStroke(vg);
    }

    public void drawRect(int x1, int y1, int w, int h) {
        x1 += canvas.getX();;
        y1 += canvas.getY();;
        nvgRoundedRect(vg, x1, y1, w, h, 0);
        nvgStrokeColor(vg, nvgRGBA(r, g, b, a));
        nvgStroke(vg);
    }

    public void drawString(String str, int x, int y, int anchor) {
        x += canvas.getX();;
        y += canvas.getY();;
        byte[] ba = Gutil.toUtf8(str + "\000");
        nvgFillColor(vg, nvgRGBA(r, g, b, a));
        nvgTextJni(vg, x, y, ba, 0, ba.length);
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {

        x += canvas.getX();
        y += canvas.getY();
        str = str.substring(offset, len);
        byte[] b = Gutil.toUtf8(str + "\000");
        nvgTextJni(vg, x, y, b, 0, b.length);
    }

    public void drawChar(char character, int x, int y, int anchor) {
        x += canvas.getX();
        y += canvas.getY();
        byte[] b = Gutil.toUtf8(character + "\000");
        nvgTextJni(vg, x, y, b, 0, b.length);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        x += canvas.getX();
        y += canvas.getY();
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

        byte[] imgPaint = nvgImagePattern(vg, x, y, w, h, 0.0f / 180.0f * (float) Math.PI, img.getTexture(), 1f);
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
        nvgScissor(vg, x, y, width, height);
    }

    public void translate(int x, int y) {
        nvgTranslate(vg, x, y);
    }

}
