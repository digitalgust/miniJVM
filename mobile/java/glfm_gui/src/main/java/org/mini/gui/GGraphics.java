/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glwrap.GLUtil;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GGraphics {

    //these constants are compartiable with j2me

    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;

    //Causes the Sprite to appear reflected about its vertical center.
    public static final int TRANS_MIRROR = 2;
    //Causes the Sprite to appear reflected about its vertical center and then rotated clockwise by 180 degrees.
    public static final int TRANS_MIRROR_ROT180 = 1;
    //Causes the Sprite to appear reflected about its vertical center and then rotated clockwise by 270 degrees.
    public static final int TRANS_MIRROR_ROT270 = 4;
    //Causes the Sprite to appear reflected about its vertical center and then rotated clockwise by 90 degrees.
    public static final int TRANS_MIRROR_ROT90 = 7;
    //No transform is applied to the Sprite.
    public static final int TRANS_NONE = 0;
    //Causes the Sprite to appear rotated clockwise by 180 degrees.
    public static final int TRANS_ROT180 = 6;
    //Causes the Sprite to appear rotated clockwise by 270 degrees.
    public static final int TRANS_ROT270 = 3;
    //Causes the Sprite to appear rotated clockwise by 90 degrees.
    public static final int TRANS_ROT90 = 5;

    public static final int SOLID = 0;
    public static final int DOTTED = 1;

    protected GCanvas canvas;
    protected long vg;

    int curColor = 0;
    byte r, g, b, a;
    float fontSize = GToolkit.getStyle().getTextFontSize();
    int clipX, clipY, clipW, clipH;
    int strokeStyle;

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

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

        x += canvas.getX();
        y += canvas.getY();

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, (arcWidth + arcHeight) / 2);
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

    public void drawRect(int x1, int y1, int w1, int h1) {
        x1 += canvas.getX();
        y1 += canvas.getY();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x1, y1, w1, h1, 0);
        nvgStrokeColor(vg, nvgRGBA(r, g, b, a));
        nvgStroke(vg);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

        x += canvas.getX();
        y += canvas.getY();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, (arcWidth + arcHeight) / 2);
        nvgStrokeColor(vg, nvgRGBA(r, g, b, a));
        nvgStroke(vg);
    }

    private int j2meAnchorToNanovg(int j2meNanchor) {
        int r = 0;
        if ((j2meNanchor & 0x01) != 0) {
            r |= NVG_ALIGN_CENTER;
        }
        if ((j2meNanchor & 0x02) != 0) {
            r |= NVG_ALIGN_MIDDLE;
        }
        if ((j2meNanchor & 0x04) != 0) {
            r |= NVG_ALIGN_LEFT;
        }
        if ((j2meNanchor & 0x08) != 0) {
            r |= NVG_ALIGN_RIGHT;
        }
        if ((j2meNanchor & 0x10) != 0) {
            r |= NVG_ALIGN_TOP;
        }
        if ((j2meNanchor & 0x20) != 0) {
            r |= NVG_ALIGN_BOTTOM;
        }
        if ((j2meNanchor & 0x40) != 0) {
            r |= NVG_ALIGN_BASELINE;
        }
        if (j2meNanchor == 0) {
            r = NVG_ALIGN_LEFT | NVG_ALIGN_TOP;
        }
        return r;
    }

    public void drawString(String str, int x, int y, int anchor) {
        x += canvas.getX();
        y += canvas.getY();
        nvgFontSize(vg, fontSize);
        anchor = j2meAnchorToNanovg(anchor);
        nvgTextAlign(vg, anchor);

        byte[] ba = GLUtil.toUtf8(str);
        if (ba == null || ba.length <= 0) {
            return;
        }

        nvgFillColor(vg, nvgRGBA(r, g, b, a));
        nvgTextJni(vg, x, y, ba, 0, ba.length);
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        anchor = j2meAnchorToNanovg(anchor);
        x += canvas.getX();
        y += canvas.getY();
        nvgFontSize(vg, fontSize);
        nvgTextAlign(vg, anchor);
        str = str.substring(offset, len);
        byte[] b = GLUtil.toUtf8(str);
        if (b == null || b.length <= 0) {
            return;
        }
        nvgTextJni(vg, x, y, b, 0, b.length);
    }

    public void drawChar(char character, int x, int y, int anchor) {
        anchor = j2meAnchorToNanovg(anchor);
        x += canvas.getX();
        y += canvas.getY();
        nvgFontSize(vg, fontSize);
        nvgTextAlign(vg, anchor);
        byte[] b = GLUtil.toUtf8(character + "");
        nvgTextJni(vg, x, y, b, 0, b.length);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        anchor = j2meAnchorToNanovg(anchor);
        x += canvas.getX();
        y += canvas.getY();
        nvgFontSize(vg, fontSize);
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
        if (img == null) return;
        drawImage(img, x, y, img.getWidth(), img.getHeight(), anchor);
    }

    public void drawImage(GImage img, int x, int y, int w, int h, int anchor) {
        if (img == null) return;
        x += canvas.getX();
        y += canvas.getY();
        if ((anchor & RIGHT) != 0) {
            x -= img.getWidth();
        } else if ((anchor & HCENTER) != 0) {
            x -= img.getWidth() / 2;
        }
        if ((anchor & BOTTOM) != 0) {
            y -= img.getHeight();
        } else if ((anchor & VCENTER) != 0) {
            y -= img.getHeight() / 2;
        }

        byte[] imgPaint = nvgImagePattern(vg, x, y, w, h, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), 1f);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, 0);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);
    }

    public void drawRegion(GImage src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int anchor) {
        if (src == null || width == 0 || height == 0) {
            return;
        }
        if (x_src < 0 || y_src < 0 || x_src + width > src.getWidth() || y_src + height > src.getHeight()) {
            return;
        }

        x_dest += canvas.getX();
        y_dest += canvas.getY();

        final int imgw = src.getWidth();
        final int imgh = src.getHeight();

        float clipX, clipY, clipW, clipH;
        clipX = x_dest;
        clipY = y_dest;
        clipW = width;
        clipH = height;
        float ix = 0f, iy = 0f, iw = imgw;
        float px = 0f, py = 0f, pw = 0f, ph = 0f;
        float rot = 0f;
        switch (transform) {
            case TRANS_NONE:
                px = x_dest - x_src;
                py = y_dest - y_src;
                pw = imgw;
                ph = imgh;
                ix = px;
                iy = py;
                clipW = width;
                clipH = height;
                break;
            case TRANS_ROT90:
                px = x_dest - (imgh - y_src - height);
                py = y_dest - x_src;
                pw = imgh;
                ph = imgw;
                rot = 90f;
                ix = px + imgh;
                iy = py;
                clipW = height;
                clipH = width;
                break;
            case TRANS_ROT180:
                px = x_dest - (imgw - x_src - width);
                py = y_dest - (imgh - y_src - height);
                pw = imgw;
                ph = imgh;
                rot = 180f;
                ix = px + imgw;
                iy = py + imgh;
                clipW = width;
                clipH = height;
                break;
            case TRANS_ROT270:
                px = x_dest - y_src;
                py = y_dest - (imgw - x_src - width);
                pw = imgh;
                ph = imgw;
                rot = 270f;
                ix = px;
                iy = py + imgw;
                clipW = height;
                clipH = width;
                break;
            case TRANS_MIRROR:
                px = x_dest - (imgw - x_src - width);
                py = y_dest - y_src;
                pw = imgw;
                ph = imgh;
                ix = px + imgw;
                iy = py;
                iw = -imgw;
                clipW = width;
                clipH = height;
                break;
            case TRANS_MIRROR_ROT90:
                px = x_dest - (imgh - y_src - height);
                py = y_dest - (imgw - x_src - width);
                pw = imgh;
                ph = imgw;
                rot = 90f;
                ix = px + imgh;
                iy = py + imgw;
                iw = -imgw;
                clipW = height;
                clipH = width;
                break;
            case TRANS_MIRROR_ROT180:
                px = x_dest - x_src;
                py = y_dest - (imgh - y_src - height);
                pw = imgw;
                ph = imgh;
                rot = 180f;
                iw = -imgw;
                ix = px;
                iy = py + imgh;
                clipW = width;
                clipH = height;
                break;
            case TRANS_MIRROR_ROT270:
                px = x_dest - y_src;
                py = y_dest - x_src;
                pw = imgh;
                ph = imgw;
                rot = 270f;
                iw = -imgw;
                ix = px;
                iy = py;
                clipW = height;
                clipH = width;
                break;
            default:
                throw new IllegalArgumentException("IllegalArgumentException");
        }

        if ((anchor & RIGHT) != 0) {
            px -= clipW;
            ix -= clipW;
            clipX -= clipW;
        } else if ((anchor & HCENTER) != 0) {
            px -= clipW / 2;
            ix -= clipW / 2;
            clipX -= clipW / 2;
        }
        if ((anchor & BOTTOM) != 0) {
            py -= clipH;
            iy -= clipH;
            clipY -= clipH;
        } else if ((anchor & VCENTER) != 0) {
            py -= clipH / 2;
            iy -= clipH / 2;
            clipY -= clipH / 2;
        }

        nvgSave(vg);
        nvgScissor(vg, clipX, clipY, clipW, clipH);
        byte[] imgPaint = nvgImagePattern(vg, ix, iy, iw, imgh, rot / 180.0f * (float) Math.PI, src.getNvgTextureId(vg), 1f);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, px, py, pw, ph, 0);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);
        nvgRestore(vg);

    }


    /**
     * Notice: the rgbData is ABGR format
     * IMPORTANT : This mehod maybe copy large mount of data when the area is big, so it's slow sometimes.
     *
     * @param rgbData
     * @param offset
     * @param scanlength
     * @param x
     * @param y
     * @param width
     * @param height
     * @param processAlpha
     */
    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
        ImageMutable rgbImg = (ImageMutable) GImage.createImageMutable(width, height);
        rgbImg.setPix(rgbData, offset, scanlength, 0, 0, width, height);
        rgbImg.updateImage();
        drawImage(rgbImg, x, y, LEFT | TOP);
    }

    public int getColor() {
        return curColor;
    }

    /**
     * not implementation
     *
     * @param x_src
     * @param y_src
     * @param width
     * @param height
     * @param x_dest
     * @param y_dest
     * @param anchor
     */
    public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
        x_src += canvas.getX();
        y_src += canvas.getY();

        x_dest += canvas.getX();
        y_dest += canvas.getY();

        throw new RuntimeException("Not implementation");
    }

    public void clipRect(int x, int y, int width, int height) {
        clipX = clipX > x ? clipX : x;
        clipY = clipY > x ? clipY : y;
        clipW = clipX + clipW < clipX + width ? clipW : width;
        clipH = clipY + clipH < clipY + height ? clipH : height;
        setClip(clipX, clipY, clipW, clipH);
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
        nvgScissor(vg, x + (int) canvas.getX(), y + (int) canvas.getY(), w, h);
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

    public void setStrokeStyle(int style) {
        strokeStyle = style;
    }

    public int getStrokeStyle() {
        return strokeStyle;
    }

    public void setGrayScale(int value) {

    }
}
