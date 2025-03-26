package org.mini.gui.callback;

import org.mini.gui.GObject;
import org.mini.gui.GToolkit;
import org.mini.nanovg.Nanovg;
import org.mini.util.SysLog;

import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.nvgAddFallbackFontId;

public class GFont {

    float size;
    byte[] data;
    byte[] fontName;
    int font_handle;

    private static GFont defaultFont;

    private GFont(float fontSize) {
        this.size = fontSize;
    }

    public static GFont getFont(String fontName, String ttfInJarPath) {
        GFont font = new GFont(GToolkit.getStyle().getTextFontSize());
        font.fontName = toCstyleBytes(fontName);
        font.loadFont(ttfInJarPath);
        return font;
    }

    void loadFont(String ttfInJarPath) {
        data = GToolkit.readFileFromJar(ttfInJarPath);
        long vg = GCallBack.getInstance().getNvContext();
        font_handle = Nanovg.nvgCreateFontMem(vg, fontName, data, data.length, 0);
        if (font_handle == -1) {
            SysLog.error("Could not add font.\n");
        }
        nvgAddFallbackFontId(vg, font_handle, font_handle);
    }

    public static GFont getDefaultFont() {
        if (defaultFont == null) {
            defaultFont = GToolkit.FontHolder.word;
        }
        return defaultFont;
    }

    public float charsWidth(char[] ch, int offset, int length) {
        String s = new String(ch, offset, length);
        return stringWidth(s);
    }

    public float charWidth(char ch) {
        return stringWidth(String.valueOf(ch));
    }

    public float getHeight() {
        return size;
    }

    public float getSize() {
        return size;
    }


    public float stringWidth(String str) {
        long vg = GCallBack.getInstance().getNvContext();
        float[] boundle = GToolkit.getTextBoundle(vg, str, 5000f, size, getFontName(), false);
        return (int) (boundle[GObject.WIDTH] - boundle[GObject.LEFT] + 1f);
    }

    public float substringWidth(String str, int offset, int len) {
        return stringWidth(str.substring(offset, offset + len));
    }

    public byte[] getFontName() {
        return fontName;
    }

    public void setFontSize(float fontSize) {
        this.size = fontSize;
    }

    public byte[] getData() {
        return data;
    }
}
