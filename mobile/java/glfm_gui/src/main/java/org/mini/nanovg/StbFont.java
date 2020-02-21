/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.nanovg;

import org.mini.gl.GL;
import org.mini.gui.GImage;
import org.mini.gui.GToolkit;

import static org.mini.nanovg.Nanovg.*;

/**
 * warp stb_truetype.h - v0.6c - public domain ,
 * <p/>
 * authored from 2009-2012 by Sean Barrett / RAD Game Tools
 *
 * @author gust
 */
public class StbFont {

    byte[] fontBuffer;
    byte[] fontInfo;

    public StbFont(String fontPath) {
        if (fontPath == null) {
            fontBuffer = GToolkit.getFontWord();
        } else {
            fontBuffer = GToolkit.readFileFromJar(fontPath);
        }
        /* prepare font */
        fontInfo = stbtt_MakeFontInfo();
        long infoPtr = GToolkit.getArrayDataPtr(fontInfo);
        if (stbtt_InitFont(infoPtr, fontBuffer, 0) == 0) {
            System.out.println("load font failed: " + fontPath);
        }

    }

    public long getFontInfoPtr() {
        return GToolkit.getArrayDataPtr(fontInfo);
    }

    public byte[] getFontBytes() {
        return fontBuffer;
    }

    public int getWidth(String word, int fontSize) {
        if (word == null) {
            return 0;
        }
        long infoPtr = GToolkit.getArrayDataPtr(fontInfo);
        /* calculate font scaling */
        float scale = stbtt_ScaleForPixelHeight(infoPtr, fontSize);

        int x = 0;

        int[] ascent = {0}, descent = {0}, lineGap = {0};
        stbtt_GetFontVMetrics(infoPtr, ascent, descent, lineGap);

        ascent[0] *= scale;
        descent[0] *= scale;

        int[] ax = {0}, bx = {0};
        for (int i = 0; i < word.length(); ++i) {
            int ch = word.charAt(i);//word[i];
            int nch = i < word.length() - 1 ? word.charAt(i + 1) : 0;//word[i + 1];
            /* how wide is this character */
            stbtt_GetCodepointHMetrics(infoPtr, ch, ax, bx);
            x += ax[0] * scale;

            /* add kerning */
            int kern;
            kern = stbtt_GetCodepointKernAdvance(infoPtr, ch, nch);
            x += kern * scale;
        }
        return x;
    }

    static int[] PIC_WIDTH = {16, 32, 64, 128, 256, 512, 1024};

    public GImage renderToImage(String word, int fontSize) {
        int[] wh = new int[2];
        int tex = renderToTexture(word, fontSize, wh);
        return GImage.createImage(tex, wh[0], wh[1]);
    }

    public int renderToTexture(String word, int fontSize, int[] width_height) {
        if (word == null) {
            return -1;
        }
        int width = getWidth(word, fontSize);
        int pic_width = 0;
        int pic_height = 0;
        for (int i = 0, imax = PIC_WIDTH.length; i < imax; i++) {
            if (pic_width == 0) {
                if (width < PIC_WIDTH[i] || i == imax - 1) {
                    pic_width = PIC_WIDTH[i];
                }
            }
            if (pic_height == 0) {
                if (fontSize < PIC_WIDTH[i] || i == imax - 1) {
                    pic_height = PIC_WIDTH[i];
                }
            }
        }
//        int max = pic_width > pic_height ? pic_width : pic_height;
//        pic_width = pic_height = max;
        //System.out.println(pic_width + "," + pic_height);
        byte[] bitmap = new byte[pic_width * pic_height];

        long infoPtr = GToolkit.getArrayDataPtr(fontInfo);
        /* calculate font scaling */
        float scale = stbtt_ScaleForPixelHeight(infoPtr, fontSize);

        int x = 0;

        int[] ascent = {0}, descent = {0}, lineGap = {0};
        stbtt_GetFontVMetrics(infoPtr, ascent, descent, lineGap);

        ascent[0] *= scale;
        descent[0] *= scale;

        int[] ax = {0}, bx = {0};
        int[] c_x1 = {0}, c_y1 = {0}, c_x2 = {0}, c_y2 = {0};
        for (int i = 0, imax = word.length(); i < imax; ++i) {
            int ch = word.charAt(i);//word[i];
            int nch = i < imax - 1 ? word.charAt(i + 1) : 0;//word[i + 1];

            /* get bounding box for character (may be offset to account for chars that dip above or below the line */
            stbtt_GetCodepointBitmapBox(infoPtr, ch, scale, scale, c_x1, c_y1, c_x2, c_y2);

            /* compute y (different characters have different heights */
            int y = ascent[0] + c_y1[0];
            if (y < 0) {
                y = 0;
            }

            /* render character (stride and offset is important here) */
            int byteOffset = x + (y * pic_width);
            stbtt_MakeCodepointBitmapOffset(infoPtr, bitmap, byteOffset, c_x2[0] - c_x1[0], c_y2[0] - c_y1[0], pic_width, scale, scale, ch);

            /* how wide is this character */
            stbtt_GetCodepointHMetrics(infoPtr, ch, ax, bx);
            x += ax[0] * scale;

            /* add kerning */
            int kern;
            kern = stbtt_GetCodepointKernAdvance(infoPtr, ch, nch);
            x += kern * scale;
        }
        //NK.stbi_write_png("./out.png\000".getBytes(), pic_width, pic_height, 1, GToolkit.getArrayDataPtr(bitmap), pic_width);
        int tex = Gutil.genTexture2D(bitmap, pic_width, pic_height, GL.GL_R8, GL.GL_RED);
        width_height[0] = pic_width;
        width_height[1] = pic_height;
        return tex;
    }
}
