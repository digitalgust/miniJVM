/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import javafx.scene.control.ScrollBar;
import org.mini.apploader.AppLoader;
import org.mini.apploader.AppManager;
import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GFont;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;
import org.mini.gui.event.GStateChangeListener;
import org.mini.gui.style.GStyle;
import org.mini.gui.style.GStyleBright;
import org.mini.gui.style.GStyleInner;
import org.mini.nanovg.Nanovg;
import org.mini.reflect.ReflectArray;
import org.mini.util.SysLog;

import java.io.*;
import java.util.*;

import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GToolkit {

    // -------------------------- Component FEEL Constants --------------------------
    public static final int FEEL_FLAT = 0;
    public static final int FEEL_DIMENSION = 1;

    // -------------------------- Component Name Constants --------------------------
    // Confirm Frame constants
    public static final String NAME_CONFIRM_FRAME = "GTOOLKIT_CONFIRM_FRAME";
    public static final String NAME_CONFIRM_TEXTBOX = "GTOOLKIT_CONFIRM_TEXTBOX";
    public static final String NAME_CONFIRM_LEFT = "GTOOLKIT_CONFIRM_LEFT";
    public static final String NAME_CONFIRM_RIGHT = "GTOOLKIT_CONFIRM_RIGHT";

    // Message Frame constants
    public static final String NAME_MSGFRAME = "GTOOLKIT_MSGFRAME";
    public static final String NAME_MSGFRAME_TEXTBOX = "NAME_MSGFRAME_TEXTBOX";
    public static final String NAME_MSGFRAME_OK = "GTOOLKIT_MSGFRAME_OK";

    // File Chooser Frame constants
    public static final String NAME_FILECHOOSER_FRAME = "GTOOLKIT_FILECHOOSER_FRAME";
    public static final String NAME_FILECHOOSER_OK = "GTOOLKIT_FILECHOOSER_OK";
    public static final String NAME_FILECHOOSER_CANCEL = "GTOOLKIT_FILECHOOSER_CANCEL";
    public static final String NAME_FILECHOOSER_UP = "GTOOLKIT_FILECHOOSER_UP";
    public static final String NAME_FILECHOOSER_PATH = "GTOOLKIT_FILECHOOSER_PATH";
    public static final String NAME_FILECHOOSER_DEL = "GTOOLKIT_FILECHOOSER_DEL";
    public static final String NAME_FILECHOOSER_NEW = "GTOOLKIT_FILECHOOSER_NEW";
    public static final String NAME_FILECHOOSER_FILELIST = "GTOOLKIT_FILECHOOSER_FILELIST";

    // List Frame constants
    public static final String NAME_LISTFRAME = "GTOOLKIT_LISTFRAME";
    public static final String NAME_LISTFRAME_TEXTFIELD = "GTOOLKIT_LISTFRAME_TEXTFIELD";
    public static final String NAME_LISTFRAME_LIST = "GTOOLKIT_LISTFRAME_LIST";
    public static final String NAME_LISTFRAME_OK = "GTOOLKIT_LISTFRAME_OK";

    // Input Frame constants already exists
    public static final String NAME_INPUTFRAME = "GTOOLKIT_INPUTFRAME";
    public static final String NAME_INPUTFRAME_TEXTFIELD = "GTOOLKIT_INPUTFRAME_TEXTFIELD";
    public static final String NAME_INPUTFRAME_LEFT = "GTOOLKIT_INPUTFRAME_LEFT";
    public static final String NAME_INPUTFRAME_RIGHT = "GTOOLKIT_INPUTFRAME_RIGHT";
    public static final String NAME_INPUTFRAME_STATE = "GTOOLKIT_INPUTFRAME_STATE";

    // List Menu constants
    public static final String NAME_LISTMENU = "GTOOLKIT_LISTMENU";

    // Menu constants
    public static final String NAME_MENU = "GTOOLKIT_MENU";

    // Image View constants
    public static final String NAME_IMAGEVIEW = "GTOOLKIT_IMAGEVIEW";

    // Edit Menu Context constants
    public static final String NAME_EDITMENUCTX_SELECT = "EDITMENUCTX_SELECT";
    public static final String NAME_EDITMENUCTX_COPY = "EDITMENUCTX_COPY";
    public static final String NAME_EDITMENUCTX_PASTE = "EDITMENUCTX_PASTE";
    public static final String NAME_EDITMENUCTX_CUT = "EDITMENUCTX_CUT";
    public static final String NAME_EDITMENUCTX_SELECTALL = "EDITMENUCTX_SELECTALL";


    /**
     * 返回数组数据区首地址
     *
     * @param array
     * @return
     */
    public static long getArrayDataPtr(Object array) {
        return ReflectArray.getBodyPtr(array);
    }

    public static float[] nvgRGBA(int r, int g, int b, int a) {
        return Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public static float[] nvgRGBA(int rgba) {
        return Nanovg.nvgRGBA((byte) ((rgba >> 24) & 0xff), (byte) ((rgba >> 16) & 0xff), (byte) ((rgba >> 8) & 0xff), (byte) ((rgba >> 0) & 0xff));
    }

    public static byte[] readFileFromJar(String path) {
        InputStream is = null;
        try {
            is = GCallBack.getInstance().getResourceAsStream(path);
            if (is != null) {
                int av = is.available();

                if (av >= 0) {
                    byte[] b = new byte[av];
                    int r, read = 0;
                    while (read < av) {
                        r = is.read(b, read, av - read);
                        read += r;
                    }
                    return b;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        SysLog.error("load from jar fail : " + path);
        return null;
    }

    public static String readFileFromJarAsString(String path, String encode) {
        try {
            byte[] cont = readFileFromJar(path);
            String s = new String(cont, encode);
            return s;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] readFileFromFile(String path) {
        InputStream is = null;
        try {
            is = new FileInputStream(path);
            if (is != null) {
                int av = is.available();

                if (av >= 0) {
                    byte[] b = new byte[av];
                    int r, read = 0;
                    while (read < av) {
                        r = is.read(b, read, av - read);
                        read += r;
                    }
                    return b;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        SysLog.error("load from file fail : " + path);
        return null;
    }

    public static String readFileFromFileAsString(String path, String encode) {
        try {
            byte[] cont = readFileFromFile(path);
            String s = new String(cont, encode);
            return s;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static void saveStringToFile(String fileName, String contents) {
        try {
            byte[] b = contents.getBytes("utf-8");
            saveDataToFile(fileName, b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void saveDataToFile(String fileName, byte[] contents) {
        saveDataToFile(fileName, contents, 0, contents.length);
    }

    public static void saveDataToFile(String fileName, byte[] contents, int offset, int len) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(contents, offset, len);
            fos.close();
            //System.out.println("save file:" + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ----------------------------------------------------------------
     *      font
     * ----------------------------------------------------------------
     */

    /**
     * 字体部分
     */
    static byte[] FONT_GLYPH_TEMPLATE = toCstyleBytes("正");

    public static class FontHolder {

        static boolean fontLoaded = false;
        public static GFont word, icon;

        public static synchronized void loadFont(long vg) {
            if (fontLoaded) {
                return;
            }
            word = GFont.getFont("word", "/res/NotoEmoji+NotoSansCJKSC-Regular.ttf");
            icon = word;//GFont.getFont("icon", "/res/entypo.ttf");

            fontLoaded = true;
        }

        public static GFont getFont(String name) {
            switch (name) {
                case "word":
                    return word;
                case "icon":
                    return icon;
            }
            return word;
        }
    }

    public static byte[] getFontWord() {
        return FontHolder.word.getFontName();
    }

    public static byte[] getFontIcon() {
        return FontHolder.icon.getFontName();
    }


    public static float[] getFontBoundle(long vg, String string, float fontSize) {
        float[] bond = bounds.get();
        nvgSave(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, getDefaultFont());
        nvgTextBoundsJni(vg, 0f, 0f, FONT_GLYPH_TEMPLATE, 0, FONT_GLYPH_TEMPLATE.length, bond);
        bond[GObject.WIDTH] -= bond[GObject.LEFT];
        bond[GObject.HEIGHT] -= bond[GObject.TOP];
        bond[GObject.LEFT] = bond[GObject.TOP] = 0;
        nvgRestore(vg);
        return bond;
    }

    public static byte[] getDefaultFont() {
        return FontHolder.word.getFontName();
    }


    /**
     * ----------------------------------------------------------------
     *      style
     * ----------------------------------------------------------------
     */

    /**
     * 风格
     */
    static GStyleInner defaultStyle;
    static int feel = 0;

    public static GStyle getStyle() {
        if (defaultStyle == null) {
            defaultStyle = new GStyleInner(new GStyleBright());
        }
        return defaultStyle;
    }

    public static void setStyle(GStyle style) {
        if (style == null) return;
        defaultStyle = new GStyleInner(style);//copy every times, copy for not reference the source style
    }

    public static void setFeel(int pfeel) {
        feel = pfeel;
    }

    public static int getFeel() {
        return feel;
    }

    /**
     * ----------------------------------------------------------------
     *      draw
     * ----------------------------------------------------------------
     */

    /**
     * 光标
     */
    static boolean caretBlink = false;
    static long caretLastBlink;
    static long CARET_BLINK_PERIOD = 600;

    /**
     * 画光标，是否闪烁，如果为false,则一常显，为了节能，所以大多时候blink为false
     *
     * @param vg
     * @param x
     * @param y
     * @param w
     * @param h
     * @param blink
     */

    public static void drawCaret(long vg, float x, float y, float w, float h, boolean blink) {
        drawCaret(vg, x, y, w, h, blink, nvgRGBA(255, 192, 0, 255));
    }

    public static void drawCaret(long vg, float x, float y, float w, float h, boolean blink, float[] color) {
        long curTime = System.currentTimeMillis();
        if (curTime - caretLastBlink > CARET_BLINK_PERIOD) {
            caretBlink = !caretBlink;
            caretLastBlink = curTime;
        }
        if (caretBlink || !blink) {
            nvgBeginPath(vg);
            nvgFillColor(vg, color);
            nvgRect(vg, x, y, w, h);
            nvgFill(vg);
        }
    }

    static float[] RED_POINT_BACKGROUND = Nanovg.nvgRGBAf(1.f, 0, 0, 0.8f);
    static float[] RED_POINT_FRONT = Nanovg.nvgRGBAf(1.f, 1.f, 1.f, 1.f);

    public static void drawRedPoint(long vg, String text, float x, float y, float r) {
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, r);
        nvgFillColor(vg, RED_POINT_BACKGROUND);
        nvgFill(vg);

        nvgFontSize(vg, r * 2 - 8);
        nvgFillColor(vg, RED_POINT_FRONT);
        nvgFontFace(vg, GToolkit.getFontWord());
        byte[] text_arr = toCstyleBytes(text);
        nvgTextAlign(vg, Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
        if (text_arr != null) {
            Nanovg.nvgTextJni(vg, x, y + 1.5f, text_arr, 0, text_arr.length);
        }

    }

    public static void drawCircle(long vg, float x, float y, float r, float[] color, boolean fill) {
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, r);
        if (fill) {
            nvgFillColor(vg, color);
            nvgFill(vg);
        } else {
            nvgStrokeColor(vg, color);
            nvgStrokeWidth(vg, 1.0f);
            nvgStroke(vg);
        }
    }

    public static void drawRect(long vg, float x, float y, float w, float h, float[] color) {
        drawRect(vg, x, y, w, h, color, true);
    }

    public static void drawRect(long vg, float x, float y, float w, float h, float[] color, boolean fill) {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, w, h);
        if (fill) {
            nvgFillColor(vg, color);
            nvgFill(vg);
        } else {
            nvgStrokeColor(vg, color);
            nvgStrokeWidth(vg, 1.0f);
            nvgStroke(vg);
        }
    }

    public static void drawRect(long vg, float x, float y, float w, float h, float[] color, float[] bgColor) {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, w, h);
        nvgFillColor(vg, bgColor);
        nvgFill(vg);
        nvgStrokeColor(vg, color);
        nvgStrokeWidth(vg, 1.0f);
        nvgStroke(vg);
    }

    public static void drawRoundedRect(long vg, float x, float y, float w, float h, float r, float[] color) {
        nvgBeginPath(vg);
        nvgFillColor(vg, color);
        nvgRoundedRect(vg, x, y, w, h, r);
        nvgFill(vg);
    }

    public static void drawRoundedRect(long vg, float x, float y, float w, float h, float r, float[] color, float[] bgColor) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, r);
        nvgFillColor(vg, bgColor);
        nvgFill(vg);
        nvgStrokeColor(vg, color);
        nvgRoundedRect(vg, x, y, w, h, r - 0.5f);
        nvgStrokeWidth(vg, 1.0f);
        nvgStroke(vg);
    }


    /**
     * =========================================================
     * measure font
     */
    static ThreadLocal<float[]> bounds = new ThreadLocal<float[]>() {
        @Override
        protected float[] initialValue() {
            return new float[4];
        }
    };

    /**
     * @param vg
     * @param s
     * @param width
     * @param fontSize
     * @param multiline
     * @return
     */
    public static float[] getTextBoundle(long vg, String s, float width, float fontSize, boolean multiline) {
        return getTextBoundle(vg, s, width, fontSize, GToolkit.getFontWord(), multiline);
    }

    public static float[] getTextBoundle(long vg, String s, float width, float fontSize, byte[] font, boolean multiline) {

        byte[] b = toCstyleBytes(s);
        return getTextBoundle(vg, b, width, fontSize, font, multiline);
    }

    public static float[] getTextBoundle(long vg, byte[] b, float width, float fontSize, byte[] font, boolean multiline) {
        float[] bond = bounds.get();
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        if (multiline) {
            nvgTextBoxBoundsJni(vg, 0, 0, width + GLabel.TEXT_BOUND_DEC, b, 0, b.length, bond);
        } else {
            nvgTextBoundsJni(vg, 0, 0, b, 0, b.length, bond);
        }
        bond[GObject.WIDTH] -= bond[GObject.LEFT];
        bond[GObject.HEIGHT] -= bond[GObject.TOP];
        bond[GObject.LEFT] = bond[GObject.TOP] = 0;
        if (multiline) {
            bond[GObject.WIDTH] += GLabel.TEXT_BOUND_DEC;
        }
        return bond;
    }

    public static void drawTextLine(long vg, float tx, float ty, String s, float fontSize, float[] color, int align) {
        drawTextLineWithShadow(vg, tx, ty, s, fontSize, color, align, null, 0);
    }

    public static void drawTextLineWithShadow(long vg, float tx, float ty, String s, float fontSize, float[] color, int align, float[] shadowColor, float shadowBlur) {
        if (s == null) {
            return;
        }
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, align);
        byte[] b = toCstyleBytes(s);
        if (shadowColor != null) {
            nvgFontBlur(vg, shadowBlur);
            nvgFillColor(vg, color);
            Nanovg.nvgTextJni(vg, tx, ty + 1.5f, b, 0, b.length);
            nvgFontBlur(vg, 0);
        }
        nvgFillColor(vg, color);
        Nanovg.nvgTextJni(vg, tx, ty + 1.5f, b, 0, b.length);
    }

    public static void drawTextLineInBoundle(long vg, float tx, float ty, float pw, float ph, String s, float fontSize, float[] color) {
        if (s == null) {
            return;
        }
        nvgSave(vg);
        nvgScissor(vg, tx, ty, pw, ph);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, NVG_ALIGN_TOP | NVG_ALIGN_LEFT);
        nvgFillColor(vg, color);
        byte[] b = toCstyleBytes(s);
        Nanovg.nvgTextJni(vg, tx, ty + 1.5f, b, 0, b.length);
        nvgRestore(vg);
    }


    public static void drawText(long vg, float x, float y, float w, float h, String s) {

        drawTextWithShadow(vg, x, y, w, h, s, GToolkit.getStyle().getTextFontSize(), GToolkit.getStyle().getTextFontColor(), null, 0);
    }

    public static void drawText(long vg, float x, float y, float w, float h, String s, float fontSize, float[] color) {
        drawTextWithShadow(vg, x, y, w, h, s, fontSize, color, null, 0);
    }

    public static void drawTextWithShadow(long vg, float x, float y, float w, float h, String s, float fontSize, float[] color, float[] shadowColor, float shadowBlur) {
        if (s == null) {
            return;
        }
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, GToolkit.getFontWord());

        byte[] text_arr = toCstyleBytes(s);

        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);

        if (text_arr != null) {
            if (shadowColor != null) {
                nvgFontBlur(vg, shadowBlur);
                nvgFillColor(vg, shadowColor);
                nvgTextBoxJni(vg, x + 1, y + 2, w, text_arr, 0, text_arr.length);
                nvgFontBlur(vg, 0);
            }
            nvgFillColor(vg, color);
            nvgTextBoxJni(vg, x, y + 1, w, text_arr, 0, text_arr.length);
        }
    }

    public static void drawEmoj(long vg, float x, float y, float w, float h, byte[] emojBytes) {
        drawEmoj(vg, x, y, w, h, emojBytes, GToolkit.getStyle().getIconFontSize(), null);
    }

    public static void drawEmoj(long vg, float x, float y, float w, float h, byte[] emojBytes, float fontsize, float[] color) {
        if (emojBytes == null || fontsize == 0) return;

        nvgFontSize(vg, fontsize);
        nvgFontFace(vg, GToolkit.getFontIcon());

        float[] pc = color == null ? getStyle().getTextFontColor() : color;
        nvgFillColor(vg, pc);
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgTextJni(vg, x + w * 0.5f, y + h * 0.5f, emojBytes, 0, emojBytes.length);
    }

    public static void drawImageFrame(long vg, GImage img, int imgCols, int imgRows, int frameIndex, float px, float py, float pw, float ph) {
        drawImageFrame(vg, img, imgCols, imgRows, frameIndex, px, py, pw, ph, false, 0, 1.0f);
    }

    /**
     * 用于画很多行列帧组成的图片中的一帧
     *
     * @param vg
     * @param img
     * @param imgCols    图片有多少列
     * @param imgRows    图片有多少行
     * @param frameIndex 画第几个图片
     * @param px
     * @param py
     * @param pw
     * @param ph
     * @param border
     * @param alpha
     */
    public static void drawImageFrame(long vg, GImage img, int imgCols, int imgRows, int frameIndex, float px, float py, float pw, float ph, boolean border, float radius, float alpha) {
        if (img == null) {
            return;
        }
        Nanovg.nvgSave(vg);
        if (radius < 0) radius = 0;
        int frameCol = frameIndex % imgCols;
        int frameRow = frameIndex / imgCols;

        float drawX = px - pw * frameCol;
        float drawY = py - ph * frameRow;
        float drawW = pw * imgCols;
        float drawH = ph * imgRows;
        Nanovg.nvgScissor(vg, px, py, pw, ph);

        if (border) {
            drawX += 1;
            drawY += 1;
            drawW -= 2;
            drawH -= 2;
        }
        byte[] imgPaint = nvgImagePattern(vg, drawX, drawY, drawW, drawH, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), alpha);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, px, py, pw, ph, radius + (border ? 1 : 0));
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);

        if (border) {
            nvgBeginPath(vg);
            nvgRoundedRect(vg, px, py, pw, ph, radius);
            nvgStrokeWidth(vg, 1.0f);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);
        }
        Nanovg.nvgRestore(vg);
    }

    /**
     * 画图
     *
     * @param vg
     * @param img
     * @param px
     * @param py
     * @param pw
     * @param ph
     */
    public static void drawImage(long vg, GImage img, float px, float py, float pw, float ph) {
        drawImage(vg, img, px, py, pw, ph, true, 1.f, 5.f);
    }

    public static void drawImage(long vg, GImage img, float px, float py, float pw, float ph, boolean border, float alpha) {
        drawImage(vg, img, px, py, pw, ph, border, alpha, border ? 5.f : 0.f);
    }

    public static void drawImage(long vg, GImage img, float px, float py, float pw, float ph, boolean border, float alpha, float radius) {
        if (img == null) {
            return;
        }

        byte[] imgPaint;
        float ix, iy, iw, ih;
        int[] imgW = {0}, imgH = {0};
        imgW[0] = img.getWidth();
        imgH[0] = img.getHeight();

        //nvgImageSize(vg, img.getTexture(vg), imgW, imgH);
        if (imgW[0] < imgH[0]) {
            iw = pw;
            ih = iw * (float) imgH[0] / (float) imgW[0];
            ix = 0;
            iy = -(ih - ph) * 0.5f;
        } else {
            ih = ph;
            iw = ih * (float) imgW[0] / (float) imgH[0];
            ix = -(iw - pw) * 0.5f;
            iy = 0;
        }

        imgPaint = nvgImagePattern(vg, px + ix + 1, py + iy + 1, iw - 2, ih - 2, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), alpha);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, px, py, pw, ph, radius);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);

        if (border) {
//            shadowPaint = nvgBoxGradient(vg, px, py, pw, ph, 5, 3, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
//            nvgBeginPath(vg);
//            //nvgRect(vg, px - 5, py - 5, pw + 10, ph + 10);
//            nvgRoundedRect(vg, px, py, pw, ph, 6);
//            nvgPathWinding(vg, NVG_HOLE);
//            nvgFillPaint(vg, shadowPaint);
//            nvgFill(vg);

            nvgBeginPath(vg);
            float r = radius - 2;
            r = r < 0 ? 0 : r;
            nvgRoundedRect(vg, px + 1, py + 1, pw - 2, ph - 2, r);
            nvgStrokeWidth(vg, 1.0f);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);
        }
    }

    public static void drawImageStretch(long vg, GImage img, float px, float py, float pw, float ph, boolean border, float alpha, float radius) {
        if (img == null) {
            return;
        }

        byte[] imgPaint;
        int[] imgW = {0}, imgH = {0};
        imgW[0] = img.getWidth();
        imgH[0] = img.getHeight();


        imgPaint = nvgImagePattern(vg, px + 1, py + 1, pw - 2, ph - 2, 0.0f / 180.0f * (float) Math.PI, img.getNvgTextureId(vg), alpha);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, px, py, pw, ph, radius);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);

        if (border) {
//            shadowPaint = nvgBoxGradient(vg, px, py, pw, ph, 5, 3, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
//            nvgBeginPath(vg);
//            //nvgRect(vg, px - 5, py - 5, pw + 10, ph + 10);
//            nvgRoundedRect(vg, px, py, pw, ph, 6);
//            nvgPathWinding(vg, NVG_HOLE);
//            nvgFillPaint(vg, shadowPaint);
//            nvgFill(vg);

            nvgBeginPath(vg);
            float r = radius - 2;
            r = r < 0 ? 0 : r;
            nvgRoundedRect(vg, px + 1, py + 1, pw - 2, ph - 2, r);
            nvgStrokeWidth(vg, 1.0f);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);
        }
    }

    /**
     * ----------------------------------------------------------------
     * getConfirmFrame
     * ----------------------------------------------------------------
     */
    static public GFrame getConfirmFrame(GForm form, String title, String msg, String left, GActionListener leftListener, String right, GActionListener rightListener) {
        return getConfirmFrame(form, title, msg, left, leftListener, right, rightListener, 300f, 200f, false);
    }

    static public GFrame getConfirmFrame(GForm form, String title, String msg, String left, GActionListener leftListener, String right, GActionListener rightListener, float width, float height) {
        return getConfirmFrame(form, title, msg, left, leftListener, right, rightListener, width, height, false);
    }

    static public GFrame getConfirmFrame(GForm form, String title, String msg, String left, GActionListener leftListener, String right, GActionListener rightListener, float width, float height, boolean disappeOnLostFocus) {
        GFrame frame = new GFrame(form, title, 0, 0, width, height);
        frame.setName(NAME_CONFIRM_FRAME);
        frame.setFront(true);
        if (disappeOnLostFocus) {
            frame.setFocusListener(new GFocusChangeListener() {
                @Override
                public void focusGot(GObject go) {
                }

                @Override
                public void focusLost(GObject go) {
                    if (frame.getForm() != null) {
                        frame.getForm().remove(frame);
                    }
                }
            });
        }

        GContainer gp = frame.getView();
        float x = 10, y = 5, w = gp.getW() - 20, h = gp.getH() - 50;

        GTextBox tbox = new GTextBox(form, msg, "", x, y, w, h);
        tbox.setName(NAME_CONFIRM_TEXTBOX);
        tbox.setEditable(false);
        gp.add(tbox);
        y += h + 5;

        float btnWidth = w * .5f;
        if (left != null) {
            GButton leftBtn = new GButton(form, left, x, y, btnWidth, 30);
            //leftBtn.setBgColor(128, 16, 8, 255);
            leftBtn.setName(NAME_CONFIRM_LEFT);
            gp.add(leftBtn);
            leftBtn.setActionListener(gobj -> {
                if (leftListener != null) leftListener.action(gobj);
                else gobj.getFrame().close();
            });
        }

        GButton rightBtn = new GButton(form, right == null ? GLanguage.getString(null, "Cancel") : right, btnWidth + 10, y, btnWidth, 30);
        gp.add(rightBtn);
        rightBtn.setName(NAME_CONFIRM_RIGHT);
        rightBtn.setActionListener(gobj -> {
            if (rightListener != null) rightListener.action(gobj);
            else gobj.getFrame().close();
        });

        return frame;
    }

    static public GFrame getMsgFrame(GForm form, String title, String msg) {
        return getMsgFrame(form, title, msg, 300f, 200f, null);
    }

    static public GFrame getMsgFrame(GForm form, String title, String msg, GActionListener listener) {
        return getMsgFrame(form, title, msg, 300f, 200f, listener);
    }

    static public GFrame getMsgFrame(GForm form, String title, String msg, float width, float height) {
        return getMsgFrame(form, title, msg, width, height, null);
    }

    static public GFrame getMsgFrame(GForm form, String title, String msg, float width, float height, GActionListener listener) {
        final GFrame frame = new GFrame(form, title, 0, 0, width, height);
        frame.setName(NAME_MSGFRAME);
        frame.setFront(true);
        frame.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject go) {
            }

            @Override
            public void focusLost(GObject go) {
                if (frame.getForm() != null) {
                    frame.getForm().remove(frame);
                }
            }
        });

        GContainer gp = frame.getView();
        float x = 10, y = 10, w = gp.getW() - 20, h = gp.getH() - 50;

        GTextBox tbox = new GTextBox(form, msg, "", x, y, w, h);
        tbox.setName(NAME_MSGFRAME_TEXTBOX);
        tbox.setEditable(false);
        gp.add(tbox);
        y += h + 10;

        float btnWidth = w * .5f;
        GButton leftBtn = new GButton(form, GLanguage.getString(null, "Ok"), x + btnWidth * .5f, y, btnWidth, 28);
        //leftBtn.setBgColor(128, 16, 8, 255);
        leftBtn.setName(NAME_MSGFRAME_OK);
        gp.add(leftBtn);
        leftBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (listener != null) listener.action(gobj);
                else frame.close();
            }
        });

        return frame;
    }

    /**
     * ----------------------------------------------------------------
     * FileChooser
     * ----------------------------------------------------------------
     */

    static public GFrame getFileChooser(GForm form, String title, String path, FileFilter filter, float width, float height, GActionListener openAction, GActionListener cancelAction) {
        return getFileChooser(form, title, path, filter, false, width, height, openAction, cancelAction);
    }

    static public GFrame getFileChooser(GForm form, String title, String path, FileFilter filter, boolean multiSelect, float width, float height, GActionListener openAction, GActionListener cancelAction) {
        final GFrame frame = new GFrame(form, title, 0, 0, width, height);
        frame.setName(NAME_FILECHOOSER_FRAME);
        frame.setFront(true);

        if (path == null || path.length() == 0) {
            path = AppLoader.getProperty("filechooserpath");
        }
        if (path == null || "".equals(path)) {
            path = GCallBack.getInstance().getApplication().getSaveRoot();  //android can't access out of app dir
        }
        File file = new File(path);
        if (!file.exists()) {
            file = new File(".");
        }

        GContainer gp = frame.getView();
        float x = 10f, y = 10f, w = gp.getW() - 20f;
        float btnH;
        float pad = 3f;

        btnH = 30f;
        float btnWidth = 70f;
        GButton okBtn = new GButton(form, GLanguage.getString(null, "Ok"), x + w - 75f, y, btnWidth, btnH);
        //okBtn.setPreIcon("✓");
        okBtn.setName(NAME_FILECHOOSER_OK);
        gp.add(okBtn);
        okBtn.setActionListener(gobj -> {
            if (openAction != null) openAction.action(gobj);
            gobj.getFrame().close();
        });

        GButton cancelBtn = new GButton(form, GLanguage.getString(null, "Cancel"), x + w - 150f, y, btnWidth, btnH);
        //cancelBtn.setPreIcon("\uE712");
        cancelBtn.setName(NAME_FILECHOOSER_CANCEL);
        gp.add(cancelBtn);
        cancelBtn.setActionListener(gobj -> {
            if (cancelAction != null) cancelAction.action(gobj);
            gobj.getFrame().close();
        });


        y += btnH + pad;
        btnH = 35f;
        float btnW = 40f;
        GButton upBtn = new GButton(form, "", x, y, btnW, btnH);
        upBtn.setPreIcon("⬆");
        upBtn.setName(NAME_FILECHOOSER_UP);
        gp.add(upBtn);

        float labX = x + btnW + pad;
        float labW = w - 3 * (btnW + pad);
        GTextField pathLabel = new GTextField(form, file.getAbsolutePath(), "", labX, y, labW, btnH);
        pathLabel.setName(NAME_FILECHOOSER_PATH);
        pathLabel.setEditable(false);
        pathLabel.setResetEnable(false);
        gp.add(pathLabel);

        float delX = labX + labW + pad;
        GButton delBtn = new GButton(form, "", delX, y, btnW, btnH);
        delBtn.setPreIcon("\uE729");
        delBtn.setName(NAME_FILECHOOSER_DEL);
        gp.add(delBtn);
        delBtn.setActionListener(gobj -> {

            GFrame confirm = getConfirmFrame(form,
                    GLanguage.getString(null, "Message"),
                    GLanguage.getString(null, "Do you sure delete :") + pathLabel.getText(),
                    GLanguage.getString(null, "Delete"),
                    gobj1 -> {
                        File f = new File(pathLabel.getText());
                        AppLoader.deleteTree(f);
                        pathLabel.setText(f == null ? "" : f.getParent());
                        gobj1.getFrame().close();
                        chooserRefresh(upBtn);
                    },
                    GLanguage.getString(null, "Cancel"),
                    null);
            GButton btn = GToolkit.getComponent(confirm, NAME_CONFIRM_LEFT);
            btn.setPreIcon("\u26A0");
            btn.setPreiconColor(GColorSelector.YELLOW);
            GToolkit.showFrame(confirm);
        });

        GButton newBtn = new GButton(form, "", delX + btnW + pad, y, btnW, btnH);
        newBtn.setPreIcon("⊕");
        newBtn.setName(NAME_FILECHOOSER_NEW);
        gp.add(newBtn);
        newBtn.setActionListener(gobj -> {

            GFrame confirm = getInputFrame(form,
                    GLanguage.getString(null, "Message"),
                    GLanguage.getString(null, "Create new folder :"),
                    "",
                    GLanguage.getString(null, "Folder Name"),
                    GLanguage.getString(null, "Cancel"),
                    null,
                    GLanguage.getString(null, "Ok"),
                    gobj1 -> {
                        GButton up = GToolkit.getComponent(gobj.getFrame(), NAME_FILECHOOSER_UP);
                        if (up != null) {
                            File f = up.getAttachment();
                            f = new File(f.getAbsolutePath() + File.separator + GToolkit.getCompText(form, NAME_INPUTFRAME_TEXTFIELD));
                            if (f.mkdirs()) {
                                chooserRefresh(upBtn);
                            }
                        }
                    });
            GToolkit.showFrame(confirm);
        });

        y += btnH + pad;
        btnH = gp.getH() - 20f - (btnH + pad) * 2f;
        GList list = new GList(form, x, y, w, btnH);
        list.setShowMode(GList.MODE_MULTI_SHOW);
        if (multiSelect) list.setSelectMode(GList.MODE_MULTI_SELECT);
        list.setName(NAME_FILECHOOSER_FILELIST);
        list.setAttachment(filter);
        list.setScrollBar(true);
        chooserAddFilesToList(file, filter, list);
        gp.add(list);


        upBtn.setAttachment(file);
        upBtn.setActionListener(gobj -> {
            File f = gobj.getAttachment();
            if (f != null) {//the parent of roots is null , parents of /
                f = f.getParentFile();
                gobj.setAttachment(f);
                pathLabel.setText(f == null ? "" : f.getAbsolutePath());
                chooserRefresh(upBtn);
            }
            list.setSelectedIndex(-1);
        });

        return frame;
    }

    private static void chooserRefresh(GObject gobj) {
        GButton up = GToolkit.getComponent(gobj.getFrame(), NAME_FILECHOOSER_UP);
        GList list = GToolkit.getComponent(gobj.getFrame(), NAME_FILECHOOSER_FILELIST);

        File f = up.getAttachment();
        //System.out.println("=====" + (f == null ? f : f.toString() + f.exists()));
        while (f != null && !f.exists()) {
            if (f.getAbsolutePath().equals("/") || f.getAbsolutePath().equals("\\")) {//posix
                break;
            }
            if (f.getAbsolutePath().endsWith(":\\")) {
                break;
            }
            f = f.getParentFile();
        }
        up.setAttachment(f);
        chooserAddFilesToList(f, list.getAttachment(), list);
        AppLoader.setProperty("filechooserpath", "");
    }

    private static GActionListener fileChooserItemListener = gobj -> {
        File f = gobj.getAttachment();
        GList list = GToolkit.getComponent(gobj.getFrame(), NAME_FILECHOOSER_FILELIST);

        if (f.isDirectory()) {
            if (list != null) {
                chooserAddFilesToList(f, list.getAttachment(), list);
            }
            GButton upBtn = GToolkit.getComponent(gobj.getFrame(), NAME_FILECHOOSER_UP);
            if (upBtn != null) {
                AppLoader.setProperty("filechooserpath", f.getAbsolutePath());
                upBtn.setAttachment(f);
            }
        }
        GTextField lab = GToolkit.getComponent(gobj.getFrame(), NAME_FILECHOOSER_PATH);
        if (lab != null) {
            lab.setText(f.getAbsolutePath());
        }

        setChooserResult(gobj);
    };

    private static void setChooserResult(GObject listItem) {
        GList list = GToolkit.getComponent(listItem.getFrame(), NAME_FILECHOOSER_FILELIST);

        GButton okbtn = GToolkit.getComponent(listItem.getFrame(), NAME_FILECHOOSER_OK);
        if (okbtn != null) {
            //single select
            if (list.getSelectMode() == GList.MODE_SINGLE_SELECT) {
                File f = listItem.getAttachment();
                okbtn.setAttachment(f);
            } else {// multiple select
                int[] selectIndecis = list.getSelectedIndices();
                File[] files = new File[selectIndecis.length];
                for (int i = 0; i < files.length; i++) {
                    files[i] = list.getItem(selectIndecis[i]).getAttachment();
                }
            }
        }
    }

    static float[] dirColor = new float[]{0.7f, 0.7f, 0.2f, 0.7f};
    static float[] fileColor = new float[]{0.4f, 0.4f, 0.7f, 0.7f};

    private static void chooserAddFilesToList(File dir, FileFilter filter, GList list) {
        File[] files = dir == null ? File.listRoots() : dir.listFiles(filter);
        Arrays.sort(files, (f1, f2) -> {

            int i = 0;
            if (f1.isDirectory() && f2.isDirectory()) {
                i = f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            } else if (f1.isDirectory()) {
                i = -1;
            } else if (f2.isDirectory()) {
                i = 1;
            } else {
                i = f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
            return i;
        });

        list.clear();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String lab = file.getName();
            if (lab.length() == 0) lab = file.getPath();
            GListItem item = list.addItem(null, lab + "   | " + file.length() + " | " + new Date(file.lastModified()));
            if (file.isDirectory()) {
                item.setPreIcon("\uD83D\uDCC1");
                item.setPreiconColor(dirColor);
            } else {
                item.setPreIcon("\uD83D\uDCC4");
                item.setPreiconColor(fileColor);
            }
            item.setAttachment(file);
            item.setActionListener(fileChooserItemListener);
        }

    }


    /**
     * ----------------------------------------------------------------
     * getListFrame
     * ----------------------------------------------------------------
     */
    public static GFrame getListFrame(GForm form, String title, String[] strs, GImage[] imgs, GActionListener buttonListener, GActionListener itemListener) {
        return getListFrame(form, title, strs, imgs, false, null, buttonListener, itemListener);
    }

    public static GFrame getListFrame(GForm form, String title, String[] strs, GImage[] imgs, boolean multiSelect, String buttonText, GActionListener buttonListener, GActionListener itemListener) {
        return getListFrame(form, title, strs, imgs, multiSelect, buttonText, buttonListener, itemListener, 300, 250);
    }

    public static GFrame getListFrame(GForm form, String title, String[] strs, GImage[] imgs, boolean multiSelect, String buttonText, GActionListener buttonListener, GActionListener itemListener, float width, float height) {
        float pad = 2, btnW, btnH = 28;
        float y = pad;

        GFrame frame = new GFrame(form, title, 0, 0, width, height);
        frame.setName(NAME_LISTFRAME);
        frame.setFront(true);
        frame.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject go) {
            }

            @Override
            public void focusLost(GObject go) {
                frame.close();
            }
        });
        GContainer view = frame.getView();

        GTextField search = new GTextField(form, "", "search", pad, y, view.getW() - pad * 2, 30);
        search.setName(NAME_LISTFRAME_TEXTFIELD);
        search.setBoxStyle(GTextField.BOX_STYLE_SEARCH);

        view.add(search);
        y += 30 + pad;

        float h = view.getH() - y - 30 - pad * 4;
        GList glist = new GList(form, 0, y, view.getW(), h);
        glist.setName(NAME_LISTFRAME_LIST);
        glist.setShowMode(GList.MODE_MULTI_SHOW);
        glist.setSelectMode(multiSelect ? GList.MODE_MULTI_SELECT : GList.MODE_SINGLE_SELECT);

        search.setStateChangeListener(gobj -> {
            GTextObject so = (GTextObject) gobj;
            String str = so.getText();
            if (glist != null) {
                glist.filterLabelWithKey(str);
                //System.out.println("key=" + str);
            }
        });

        view.add(glist);
        y += h + pad;
        btnW = view.getW() * .5f - pad;
        if (multiSelect) {
            GCheckBox chbox = new GCheckBox(form, GLanguage.getString(null, "SeleAll"), false, pad, y, btnW, btnH);
            view.add(chbox);
            chbox.setActionListener(gobj -> {
                if (((GCheckBox) gobj).isChecked()) {
                    glist.selectAll();
                } else {
                    glist.deSelectAll();
                }
            });
        }


        GButton btn = new GButton(form, buttonText == null ? GLanguage.getString(null, "Ok") : buttonText, (view.getW() - btnW - pad), y, btnW, btnH);
        btn.setName(NAME_LISTFRAME_OK);
        frame.getView().add(btn);
        btn.setActionListener(gobj -> {
            if (buttonListener != null) {
                buttonListener.action(gobj);
            }
            gobj.getFrame().close();
        });
        //
        glist.setItems(imgs, strs);
        if (itemListener != null) {
            for (GListItem item : glist.getItems()) {
                item.setActionListener(itemListener);
            }
        }
        return frame;
    }

    /**
     * ----------------------------------------------------------------
     * getInputFrame
     * ----------------------------------------------------------------
     */
    public static GFrame getInputFrame(GForm form, String title, String msg, String defaultValue, String inputHint, String leftLabel, GActionListener leftListener, String rightLabel, GActionListener rightListener) {
        return getInputFrame(form, title, msg, defaultValue, inputHint, leftLabel, leftListener, rightLabel, rightListener, 300, 200);
    }

    public static GFrame getInputFrame(GForm form, String title, String msg, String defaultValue, String inputHint, String leftLabel, GActionListener leftListener, String rightLabel, GActionListener rightListener, float width, float height) {

        float x = 10, y;
        GFrame frame = new GFrame(form, title, 0, 0, width, height);
        frame.setName(NAME_INPUTFRAME);
        frame.setFront(true);
        frame.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject oldgo) {
            }

            @Override
            public void focusLost(GObject newgo) {
                frame.close();
            }
        });
        GContainer view = frame.getView();
        float contentWidth = view.getW() - 20;
        y = view.getH();

        float buttonWidth = contentWidth * .5f - 10;
        y -= 35f;
        GButton cancelbtn = new GButton(form, leftLabel == null ? GLanguage.getString(null, "Cancel") : leftLabel, x, y, buttonWidth, 28);
        cancelbtn.setName(NAME_INPUTFRAME_LEFT);
        view.add(cancelbtn);

        GButton okbtn = new GButton(form, rightLabel == null ? GLanguage.getString(null, "Ok") : rightLabel, x + buttonWidth + 20, y, buttonWidth, 28);
        //okbtn.setBgColor(0, 96, 128, 255);
        okbtn.setName(NAME_INPUTFRAME_RIGHT);
        view.add(okbtn);
        y -= 35;
        GTextField input = new GTextField(form, defaultValue == null ? "" : defaultValue, inputHint, x, y, contentWidth, 28);
        input.setName(NAME_INPUTFRAME_TEXTFIELD);
        view.add(input);

        y -= 25;
        GLabel lb_state = new GLabel(form, "", x, y, contentWidth, 20);
        lb_state.setName(NAME_INPUTFRAME_STATE);
        view.add(lb_state);

        y = 10;
        GLabel lb1 = new GLabel(form, msg, x, y, contentWidth, view.getH() - 100);
        lb1.setShowMode(GLabel.MODE_MULTI_SHOW);
        view.add(lb1);

        okbtn.setActionListener((GObject gobj) -> {
            if (gobj.getFrame() != null) {
                if (rightListener != null) {
                    rightListener.action(gobj);
                }
                gobj.getFrame().close();
            }
        });

        cancelbtn.setActionListener((GObject gobj) -> {
            if (leftListener != null) {
                leftListener.action(gobj);
            }
            gobj.getFrame().close();
        });

        return frame;
    }

    /**
     * ----------------------------------------------------------------
     * getListMenu
     * ----------------------------------------------------------------
     */
    public static GList getListMenu(GForm form, String[] strs, GImage[] imgs, GActionListener[] listeners) {
        return getListMenu(form, strs, imgs, listeners, -1, -1);
    }

    public static GList getListMenu(GForm form, String[] strs, GImage[] imgs, GActionListener[] listeners, float width, float height) {

        if (width < 0) {
            width = 150;
        }
        if (height < 0) {
            height = GList.ITEM_HEIGH_DEFAULT * strs.length;
        }
        GList list = new GList(form, 0, 0, width, height);
        list.setName(NAME_LISTMENU);
        list.setBgColor(getStyle().getPopBackgroundColor());
        list.setShowMode(GList.MODE_MULTI_SHOW);
        list.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject oldgo) {
            }

            @Override
            public void focusLost(GObject newgo) {
                if (list.getParent() != null) {
                    list.getParent().remove(list);
                }
            }
        });

        GActionListener common = new GActionListener() {
            GActionListener[] actions = listeners;

            @Override
            public void action(GObject gobj) {
                list.getParent().remove(list);
                int i = list.getSelectedIndex();
                if (actions != null && actions.length > i) {
                    actions[i].action(gobj);
                }
            }
        };

        list.setItems(imgs, strs);
        GListItem[] items = list.getItems();
        if (listeners != null) {
            for (int i = 0, imax = items.length; i < imax; i++) {
                items[i].setActionListener(common);
            }
        }
        list.setSize(width, height);
//        int size = items.length;
//        if (size > 8) {
//            size = 8;
//        }
//        list.setInnerSize(200, size * list.list_item_heigh);
        list.setFront(true);

        return list;
    }

    /**
     * ----------------------------------------------------------------
     * getMenu
     * ----------------------------------------------------------------
     */
    public static GMenu getMenu(GForm form, String[] strs, GImage[] imgs, GActionListener[] listener) {

        GMenu menu = new GMenu(form, 0, 0, 150, 120);
        menu.setName(NAME_MENU);
        menu.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject oldgo) {
            }

            @Override
            public void focusLost(GObject newgo) {
                if (menu.getParent() != null) {
                    menu.getParent().remove(menu);
                }
            }
        });

        for (int i = 0, imax = strs.length; i < imax; i++) {
            GMenuItem item = menu.addItem(strs[i], imgs == null ? null : imgs[i]);
            if (listener != null) {
                item.setActionListener(listener[i]);
            }
        }

        int size = strs.length;
        if (size > 5) {
            size = 5;
        }
        menu.setSize(300, 40);
        menu.setFront(true);

        return menu;
    }

    /**
     * ----------------------------------------------------------------
     * getImageView
     * ----------------------------------------------------------------
     */
    static String[] menuStrs = new String[2];


    public static GViewPort getImageView(GForm form, GImage img, final GActionListener listener) {
        float imgW = img.getWidth();
        float imgH = img.getHeight();
        final float maxW = imgW;
        final float maxH = imgH;

        float formW = form.getW() * 0.8f;
        float formH = form.getH() * 0.8f;
        menuStrs[0] = GLanguage.getString(null, "Save to album");
        menuStrs[1] = GLanguage.getString(null, "Cancel");
        GScrollBar scrollBar = new GScrollBar(form, 0.0f, GScrollBar.VERTICAL, 0, 0, 20, formH);
        GViewPort view = new GViewPort(form) {
            @Override
            public boolean paint(long vg) {
                float w = getW();
                float h = getH();
                GToolkit.drawRect(vg, getX(), getY(), w, h, GColorSelector.GRAY);
                return super.paint(vg);
            }

            @Override
            public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
                touchEvent(button, pressed ? Glfm.GLFMTouchPhaseBegan : Glfm.GLFMTouchPhaseEnded, x, y);
            }

            @Override
            public void touchEvent(int touchid, int phase, int x, int y) {
                super.touchEvent(touchid, phase, x, y);
                if (touchid != Glfw.GLFW_MOUSE_BUTTON_1) return;
                if (scrollBar.isInArea(x, y)) {
                    return;
                }
                if (phase == Glfm.GLFMTouchPhaseEnded) {
                    if (listener != null) {
                        listener.action(this);
                    } else {
                        addMenu(x, y);
                    }
                }
            }

            GActionListener[] menuListeners = new GActionListener[]{
                    (GObject gobj) -> {
                        if (img.getAttachment() instanceof String) { //if image attachment is file abs path
                            Glfm.glfmImageCrop(GCallBack.getInstance().getDisplay(), 0, img.getAttachment(), 0, 0, (int) maxW, (int) maxH);
                            GForm.addMessage("Saving..." + img.getAttachment());
                        }
                        GContainer parent = this.getParent();
                        if (parent != null) {
                            parent.remove(this);
                        }
                    },
                    (GObject gobj) -> {
                        GContainer parent = this.getParent();
                        if (parent != null) {
                            parent.remove(this);
                        }
                    }
            };

            private void addMenu(int x, int y) {
                GObject old = findByName(NAME_LISTMENU);
                if (old != null) {
                    if (!old.isInArea(x, y)) {
                        remove(old);
                    }
                } else {
                    GList menu = GToolkit.getListMenu(form, menuStrs, null, menuListeners);
                    menu.setFront(true);
                    menu.setLocation(x - getInnerX(), y - getInnerY());
                    menu.setFocusListener(null);
                    add(menu);
                }
            }
        };
        view.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject oldgo) {
            }

            @Override
            public void focusLost(GObject newgo) {
                if (view.getForm() != null) {
                    view.getForm().remove(view);
                }
            }
        });
        view.setName(NAME_IMAGEVIEW);
        view.setSize(formW, formH);
        view.setLocation(formW * 0.1f, formH * 0.1f);

        float ratioW = imgW / formW;
        float ratioH = imgH / formH;

        if (ratioW > ratioH) {
            imgW /= ratioW;
            imgH /= ratioW;
        } else {
            imgW /= ratioH;
            imgH /= ratioH;
        }

        GImageItem item = new GImageItem(form, img);
        item.setLocation(0, 0);
        item.setSize(imgW, imgH);
        view.add(item);


        scrollBar.setLocation(0 + view.getW() - scrollBar.getW(), 0f);
        scrollBar.setFront(true);
        scrollBar.setFixed(true);
        view.add(scrollBar);

        final float minW = imgW;
        final float minH = imgH;
        scrollBar.setStateChangeListener(new GStateChangeListener() {
            @Override
            public void onStateChange(GObject bar) {
                float pos = ((GScrollBar) bar).getPos();// 0.0f - 1.0f
                float w = minW + (maxW - minW) * pos;
                float h = minH + (maxH - minH) * pos;
                item.setSize(w, h);
                view.reAlign();
            }
        });


        return view;
    }

    public static void showFrame(GObject gobj) {
        if (gobj == null) return;
        GForm form = gobj.getForm();
        gobj.setLocation(form.getW() / 2 - gobj.getW() / 2, form.getH() / 2 - gobj.getH() / 2);
        form.add(gobj);
        form.setCurrent(gobj);
        gobj.setVisible(true);
    }

    public static void showFrame(GObject gobj, float x, float y) {
        if (gobj == null) return;
        GForm form = gobj.getForm();
        gobj.setLocation(x, y);
        form.add(gobj);
        form.setCurrent(gobj);
        gobj.setVisible(true);
    }

    public static void closeFrame(GForm form, String frameName) {
        if (frameName == null) return;
        GObject go = form.findByName(frameName);
        if (go != null) {
            form.remove(go);
        }
    }

    public static void closeFrame(GObject gobj) {
        if (gobj == null) return;
        GForm form = gobj.getForm();
        form.remove(gobj);
    }

    public static void showAlignedFrame(GObject gobj, int align_mod) {
        if (gobj == null) return;
        GForm form = gobj.getForm();
        if (form == null) {
            SysLog.warn("added to form can be set align");
            return;
        }
        if ((align_mod & Nanovg.NVG_ALIGN_LEFT) != 0) {
            gobj.setLocation(0, gobj.getY());
        } else if ((align_mod & Nanovg.NVG_ALIGN_RIGHT) != 0) {
            gobj.setLocation(form.getW() - gobj.getW(), gobj.getY());
        } else if ((align_mod & Nanovg.NVG_ALIGN_CENTER) != 0) {
            gobj.setLocation((form.getW() - gobj.getW()) * .5f, gobj.getY());
        }
        if ((align_mod & Nanovg.NVG_ALIGN_TOP) != 0) {
            gobj.setLocation(gobj.getX(), 0);
        } else if ((align_mod & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
            gobj.setLocation(gobj.getX(), form.getH() - gobj.getH());
        } else if ((align_mod & Nanovg.NVG_ALIGN_CENTER) != 0) {
            gobj.setLocation(gobj.getX(), (form.getH() - gobj.getH()) * .5f);
        }
        form.add(gobj);
    }


    /**
     * set component attachment by compName
     * find component from parent
     *
     * @param parent
     * @param compName
     * @param o
     */
    public static void setCompAttachment(GContainer parent, String compName, Object o) {
        if (compName == null || parent == null) return;
        GObject go = parent.findByName(compName);
        if (go != null) {
            go.setAttachment(o);
        }
    }

    public static <T extends Object> T getCompAttachment(GContainer parent, String compName) {
        if (compName != null || parent == null) {
            GObject go = parent.findByName(compName);
            if (go != null) {
                return go.getAttachment();
            }
        }
        return null;
    }

    public static Boolean getCompEnable(GContainer parent, String compName) {
        if (compName == null || parent == null) return false;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            return eitem.isEnable();
        }
        return null;
    }

    public static void setCompEnable(GContainer parent, String compName, boolean enable) {
        if (compName == null || parent == null) return;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            eitem.setEnable(enable);
        }
    }


    public static String getCompText(GContainer parent, String compName) {
        if (compName == null || parent == null) return null;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            return eitem.getText();
        }
        return "";
    }


    public static void setCompText(GContainer parent, String compName, String text) {
        if (compName == null || parent == null) return;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            eitem.setText(text);
        }
    }

    public static String getCompCmd(GContainer parent, String compName) {
        if (compName == null || parent == null) return null;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            return eitem.getCmd();
        }
        return "";
    }

    public static void setCompCmd(GContainer parent, String compName, String text) {
        if (compName == null || parent == null) return;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            eitem.setCmd(text);
        }
    }

    public static GImage getCompImage(GContainer parent, String compName) {
        if (compName == null || parent == null) return null;
        GObject eitem = parent.findByName(compName);
        if (eitem != null && eitem instanceof GImageItem) {
            return ((GImageItem) eitem).getImg();
        }
        return null;
    }

    public static void setCompImage(GContainer parent, String compName, String jarPicPath) {
        if (compName == null || parent == null) return;
        GObject eitem = parent.findByName(compName);
        if (eitem != null && eitem instanceof GImageItem) {
            ((GImageItem) eitem).setImg(getCachedImageFromJar(jarPicPath));
        }
    }

    public static void setCompImage(GContainer parent, String compName, GImage img) {
        if (compName == null || parent == null) return;
        GObject eitem = parent.findByName(compName);
        if (eitem != null && eitem instanceof GImageItem) {
            ((GImageItem) eitem).setImg(img);
        }
    }

    public static <T extends GObject> T getComponent(GContainer parent, String compName) {
        if (compName == null || parent == null) return null;
        T eitem = parent.findByName(compName);
        return eitem;
    }


    public static GActionListener getCompActionListener(GContainer parent, String compName) {
        if (compName == null || parent == null) return null;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            return eitem.getActionListener();
        }
        return null;
    }

    public static void setCompActionListener(GContainer parent, String compName, GActionListener listener) {
        if (compName == null || parent == null) return;
        GObject eitem = parent.findByName(compName);
        if (eitem != null) {
            eitem.setActionListener(listener);
        }
    }

    /**
     * ----------------------------------------------------------------
     * EditMenu
     * ----------------------------------------------------------------
     */

    private static EditMenu editMenu;


    static public EditMenu getEditMenu() {
        return editMenu;
    }

    static public void hideEditMenu() {
        if (editMenu != null) editMenu.dispose();
    }

    static public class EditMenu extends GMenu {

        GTextObject text;
        int curLang;

        public EditMenu(GForm form, float left, float top, float width, float height) {
            super(form, left, top, width, height);
            curLang = GLanguage.getCurLang();
        }

        @Override
        public boolean paint(long vg) {
            if (text != null && text.getParent().getForm() == null) {
                dispose();
            }
            return super.paint(vg);
        }


        synchronized void dispose() {
            GForm gf = getForm();
            if (gf != null) {
                gf.remove(editMenu);
                if (text != null) {
                    text.resetSelect();
                    text.selectMode = false;
                }
                form = null;
            }
            //System.out.println("edit menu dispose");
        }
    }

    /**
     * 唤出基于form层的编辑菜单,选中菜单项后消失,失去焦点后消失
     *
     * @param focus
     * @param x
     * @param y
     */
    synchronized static public void callEditMenu(GTextObject focus, float x, float y) {
        if (focus == null || focus.getForm() == null) {
            return;
        }
        GForm gform = focus.getForm();


        float menuH = 40, menuW = 300;

        float mx = x - menuW / 2;
        if (mx < 10) {
            mx = 10;
        } else if (mx + menuW > gform.getW()) {
            mx = gform.getW() - menuW;
        }
        float my = y - 40 - menuH;
        if (my < 20) {
            my = 10;
        } else if (my + menuH > gform.getH()) {
            my = gform.getH() - menuH;
        }

//        mx += gform.getX();
//        my += gform.getY();

        if (editMenu != null) {
            if (editMenu.curLang != GLanguage.getCurLang()) {
                editMenu.dispose();
                editMenu = null;
            }
        }

        if (editMenu == null) {
            editMenu = new EditMenu(gform, mx, my, menuW, menuH);
            GMenuItem item;

            item = editMenu.addItem(GLanguage.getString(null, "Select"), null);
            item.setName(NAME_EDITMENUCTX_SELECT);
            item.setActionListener(gobj -> {
                editMenu.text.doSelectText();
                setCompEnable(editMenu, NAME_EDITMENUCTX_COPY, true);
            });
            item = editMenu.addItem(GLanguage.getString(null, "Copy"), null);
            item.setName(NAME_EDITMENUCTX_COPY);
            item.setActionListener(gobj -> {
                editMenu.text.doCopyClipBoard();
                editMenu.dispose();
            });
            item = editMenu.addItem(GLanguage.getString(null, "Paste"), null);
            item.setName(NAME_EDITMENUCTX_PASTE);
            item.setActionListener(gobj -> {
                if (editMenu.text.enable) {
                    editMenu.text.doPasteClipBoard();
                }
                editMenu.dispose();
            });
            item = editMenu.addItem(GLanguage.getString(null, "Cut"), null);
            item.setName(NAME_EDITMENUCTX_CUT);
            item.setActionListener(gobj -> {
                if (editMenu.text.enable) {
                    editMenu.text.doCut();
                }
                editMenu.dispose();
            });
            item = editMenu.addItem(GLanguage.getString(null, "SeleAll"), null);
            item.setName(NAME_EDITMENUCTX_SELECTALL);
            item.setActionListener(gobj -> {
                editMenu.text.doSelectAll();
                setCompEnable(editMenu, NAME_EDITMENUCTX_COPY, true);
            });

            editMenu.setFixed(true);
            editMenu.setContextMenu(true);
        }
        if (focus.isEditable()) {
            setCompEnable(editMenu, NAME_EDITMENUCTX_PASTE, true);
            setCompEnable(editMenu, NAME_EDITMENUCTX_CUT, true);
        } else {
            setCompEnable(editMenu, NAME_EDITMENUCTX_PASTE, false);
            setCompEnable(editMenu, NAME_EDITMENUCTX_CUT, false);
        }
        editMenu.form = gform;
        editMenu.text = focus;
        editMenu.setLocation(mx, my);

        gform.add(editMenu);
        //System.out.println("edit menu show");
    }

    /**
     * ----------------------------------------------------------------
     * image cache
     * ----------------------------------------------------------------
     * <p>
     * 缓存图象系统, 采用weakhashmap, 自动管理图象资源
     * 当imagecache中的key没有强引用时,其value(图象)会被自动回收
     */
    static Map<String, GImage> imageCache = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * 从缓存中取得图象,如果缓存中没有,则加载
     * <p>
     * 图像并且返回此图象的holder就是这个String filepath, 如果此hfilepath不被GC销毁,此图象也不会被销毁
     *
     * @param filepath
     * @return
     */
    static public GImage getCachedImageFromJar(String filepath) {
        if (filepath == null || "".equals(filepath.trim())) {
            return null;
        }
        GImage img = imageCache.get(filepath);
        try {
            if (img == null) {
                img = GImage.createImageFromJar(filepath);
                if (img != null) {
                    imageCache.put(filepath, img);
                }
                SysLog.getLogger().fine("load image cache " + filepath + " " + img);
            }
        } catch (Exception e) {
        }
        return img;
    }

    static public GImage getCachedImageFromFile(String filepath) {
        return getCachedImageFromFile(filepath, null);
    }

    static public GImage getCachedImageFromFile(String filepath, GAttachable holder) {
        if (filepath == null || "".equals(filepath.trim())) {
            return null;
        }
        filepath = new String(filepath);//for holder,must new
        GImage img = imageCache.get(filepath);
        if (img == null) {
            SysLog.info("load image cache " + filepath);
            img = GImage.createImage(filepath);
            if (img != null) {
                if (holder != null) holder.setAttachment(filepath);
                imageCache.put(filepath, img);
            }
        } else {
            //System.out.println("hit image from cache " + filepath);
            if (holder != null) {
                for (Map.Entry e : imageCache.entrySet()) {
                    if (filepath.equals(e.getKey())) { //虽然两个字符串字面相同,但不是同一对象
                        holder.setAttachment(e.getKey());
                    }
                }
            }
        }
        return img;
    }
}
