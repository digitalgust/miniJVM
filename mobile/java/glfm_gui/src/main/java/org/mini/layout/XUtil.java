/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.layout;

import org.mini.gui.GCallBack;
import org.mini.gui.GGraphics;
import org.mini.gui.GObject;
import org.mini.gui.GToolkit;
import org.mini.nanovg.Nanovg;

import java.io.*;

import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class XUtil {


    static public final char DEFAULT_SPLITTER = '.';

    public static final String getField(String s, int index) {
        return getField(s, index, DEFAULT_SPLITTER);
    }

    /**
     * 得到字符串中第index个字段,字段以:分隔
     *
     * @param s
     * @param index
     * @return
     */
    public static final String getField(String s, int index, char splitter) {

        StringBuffer newsb = new StringBuffer();
        if (s != null) {
            try {
                int counter = 0; // 子句计数器
                for (int i = 0, len = s.length(); i < len; i++) {
                    char curch = s.charAt(i);
                    if (curch == splitter) { // 遇到换行符计数器加一
                        counter++;
                        continue; // 不加到新串中去
                    }
                    if (index == counter - 1) { // 超过时停止

                        break;
                    }
                    if (index == counter) { // 当前句时向新串中加字符
                        newsb.append(curch);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return newsb.toString();
    }

    public static final String setField(String s, int index, String n) {
        return setField(s, index, n, DEFAULT_SPLITTER);
    }

    /**
     * 得到字符串中第index个字段,字段以:分隔
     *
     * @param s
     * @param index
     * @return
     */
    public static final String setField(String s, int index, String n, char splitter) {
        StringBuffer newsb = new StringBuffer();
        boolean isadd = false;
        if (s != null) {
            try {
                int counter = 0; // 子句计数器
                for (int i = 0, len = s.length(); i < len; i++) {
                    char curch = s.charAt(i);
                    if (curch == splitter) { // 遇到换行符计数器加一
                        counter++;
                    }
                    if (index == counter) { // 当前句时向新串中加字符
                        if (!isadd) {
                            isadd = true;
                            if (index != 0) {
                                newsb.append(splitter);
                            }
                            newsb.append(n);
                        }
                    } else {
                        newsb.append(curch);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return newsb.toString();
    }


    /**
     * 把STR转换成UTF8输入流
     *
     * @param s
     * @return
     */
    public static final InputStream str2utf8is(String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeUTF(s);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        byte[] tmpb = new byte[baos.size() - 2];
        System.arraycopy(baos.toByteArray(), 2, tmpb, 0, tmpb.length);
        return new ByteArrayInputStream(tmpb);
    }


    /**
     * measure a string height with display width
     *
     * @param width
     * @param str
     * @return
     */
    public static int measureHeight(int width, String str, int fontSize) {
        long vg = GCallBack.getInstance().getNvContext();
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, GToolkit.getFontWord());
        float[] boundle = GToolkit.getTextBoundle(vg, str, width);
        return (int) (boundle[GObject.HEIGHT] - boundle[GObject.TOP] + 1f);
    }

    public static int measureWidth(int width, String str, int fontSize) {
        //if (true) return str.length() * fontSize;
        long vg = GCallBack.getInstance().getNvContext();
        float[] boundle = GToolkit.getTextBoundle(vg, str, width, fontSize, GToolkit.getFontWord());
        return (int) (boundle[GObject.WIDTH] - boundle[GObject.LEFT] + 1f);
    }

    public static int parseAlign(String align) {
        if (align.equalsIgnoreCase("left")) {
            return Nanovg.NVG_ALIGN_LEFT;
        }
        if (align.equalsIgnoreCase("hcenter")) {
            return Nanovg.NVG_ALIGN_CENTER;
        }
        if (align.equalsIgnoreCase("right")) {
            return Nanovg.NVG_ALIGN_RIGHT;
        }
        if (align.equalsIgnoreCase("top")) {
            return Nanovg.NVG_ALIGN_TOP;
        }
        if (align.equalsIgnoreCase("vcenter")) {
            return Nanovg.NVG_ALIGN_MIDDLE;
        }
        if (align.equalsIgnoreCase("bottom")) {
            return Nanovg.NVG_ALIGN_BOTTOM;
        }
        if (align.equalsIgnoreCase("center")) {
            return Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE;
        }
        return 0;
    }


}
