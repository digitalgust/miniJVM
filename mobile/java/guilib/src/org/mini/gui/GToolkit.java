/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.Hashtable;
import org.mini.reflect.ReflectArray;
import org.mini.reflect.vm.RefNative;
import org.mini.glfm.Glfm;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;
import static org.mini.nanovg.Gutil.toUtf8;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.nvgAddFallbackFontId;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgCreateFont;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgRect;

/**
 *
 * @author gust
 */
public class GToolkit {

    static Hashtable<Long, GForm> table = new Hashtable();

    static public GForm getForm(long ctx) {
        return table.get(ctx);
    }

    static public GForm removeForm(long ctx) {
        return table.remove(ctx);
    }

    static public void putForm(long ctx, GForm win) {
        table.put(ctx, win);
    }

    /**
     *
     * 返回数组数据区首地址
     *
     * @param array
     * @return
     */
    public static long getArrayDataPtr(Object array) {
        ReflectArray reflect_arr = new ReflectArray(RefNative.obj2id(array));
        return reflect_arr.getDataPtr();
    }

    public static float[] nvgRGBA(int r, int g, int b, int a) {
        return Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    /**
     * 字体部分
     */
    static byte[] font_word = toUtf8("word"), font_icon = toUtf8("icon"), font_emoji = toUtf8("emoji");
    static int font_word_handle, font_icon_handle, font_emoji_handle;
    static boolean fontLoaded = false;
    static byte[] FONT_GLYPH_TEMPLATE = toUtf8("正");

    public static void loadFont(long vg) {
        if (fontLoaded) {
            return;
        }
        font_word_handle = nvgCreateFont(vg, font_word, toUtf8(System.getProperty("word_font_path")));
        if (font_word_handle == -1) {
            System.out.println("Could not add font.\n");
        }
        nvgAddFallbackFontId(vg, font_word_handle, font_word_handle);

        font_icon_handle = nvgCreateFont(vg, font_icon, toUtf8(System.getProperty("icon_font_path")));
        if (font_icon_handle == -1) {
            System.out.println("Could not add font.\n");
        }
        font_emoji_handle = nvgCreateFont(vg, font_emoji, toUtf8(System.getProperty("emoji_font_path")));
        if (font_emoji_handle == -1) {
            System.out.println("Could not add font.\n");
        }
        fontLoaded = true;
    }

    public static byte[] getFontWord() {
        return font_word;
    }

    public static byte[] getFontIcon() {
        return font_icon;
    }

    public static byte[] getFontEmoji() {
        return font_emoji;
    }

    public static float[] getFontBoundle(long vg) {
        float[] bond = new float[4];
        Nanovg.nvgTextBoundsJni(vg, 0, 0, FONT_GLYPH_TEMPLATE, 0, FONT_GLYPH_TEMPLATE.length, bond);
        bond[GObject.WIDTH] -= bond[GObject.LEFT];
        bond[GObject.HEIGHT] -= bond[GObject.TOP];
        bond[GObject.LEFT] = bond[GObject.TOP] = 0;
        return bond;
    }

    /**
     * 风格
     */
    static GStyle defaultStyle;

    public static GStyle getStyle() {
        if (defaultStyle == null) {
            defaultStyle = new GDefaultStyle();
        }
        return defaultStyle;
    }

    public static void setStyle(GStyle style) {
        defaultStyle = style;
    }
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
        long curTime = System.currentTimeMillis();
        if (curTime - caretLastBlink > CARET_BLINK_PERIOD) {
            caretBlink = !caretBlink;
            caretLastBlink = curTime;
        }
        if (caretBlink || !blink) {
            nvgBeginPath(vg);
            nvgFillColor(vg, nvgRGBA(255, 192, 0, 255));
            nvgRect(vg, x, y, w, h);
            nvgFill(vg);
        }
    }

    public static void drawRect(long vg, float x, float y, float w, float h, float[] color) {
        nvgBeginPath(vg);
        nvgFillColor(vg, color);
        nvgRect(vg, x, y, w, h);
        nvgFill(vg);
    }

}
