/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;
import org.mini.nanovg.Nanovg;
import org.mini.reflect.ReflectArray;
import org.mini.reflect.vm.RefNative;

import java.io.InputStream;
import java.util.Hashtable;

import static org.mini.nanovg.Gutil.toUtf8;
import static org.mini.nanovg.Nanovg.*;

/**
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
     * 返回数组数据区首地址
     *
     * @param array
     * @return
     */
    public static long getArrayDataPtr(Object array) {
        return ReflectArray.getBodyPtr(RefNative.obj2id(array));
    }

    public static float[] nvgRGBA(int r, int g, int b, int a) {
        return Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public static byte[] readFileFromJar(String path) {
        try {
            InputStream is = "".getClass().getResourceAsStream(path);
            int av = is.available();
            if (is != null && av >= 0) {
                byte[] b = new byte[av];
                int len = is.read(b);
                if (len != av) {
                    throw new RuntimeException("read file from jar error: " + path);
                } else {
                    //System.out.println("load from jar: " + path + " ,bytes:" + len);
                }
                return b;
            } else {
                System.out.println("load from jar fail : " + path);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 字体部分
     */
    static byte[] FONT_GLYPH_TEMPLATE = toUtf8("正");

    public static class FontHolder {

        static byte[] font_word = toUtf8("word"), font_icon = toUtf8("icon"), font_emoji = toUtf8("emoji");
        static int font_word_handle, font_icon_handle, font_emoji_handle;
        static boolean fontLoaded = false;
        static byte[] data_word;
        static byte[] data_icon;
        static byte[] data_emoji;

        public static synchronized void loadFont(long vg) {
            if (fontLoaded) {
                return;
            }
            data_word = readFileFromJar("/res/wqymhei.ttc");
            font_word_handle = Nanovg.nvgCreateFontMem(vg, font_word, data_word, data_word.length, 0);
            if (font_word_handle == -1) {
                System.out.println("Could not add font.\n");
            }
            nvgAddFallbackFontId(vg, font_word_handle, font_word_handle);

            data_icon = readFileFromJar("/res/entypo.ttf");
            font_icon_handle = Nanovg.nvgCreateFontMem(vg, font_icon, data_icon, data_icon.length, 0);
            if (font_icon_handle == -1) {
                System.out.println("Could not add font.\n");
            }
            data_emoji = readFileFromJar("/res/emoji.ttf");
            font_emoji_handle = Nanovg.nvgCreateFontMem(vg, font_emoji, data_emoji, data_emoji.length, 0);
            if (font_emoji_handle == -1) {
                System.out.println("Could not add font.\n");
            }

            fontLoaded = true;
        }
    }

    public static byte[] getFontWord() {
        return FontHolder.font_word;
    }

    public static byte[] getFontIcon() {
        return FontHolder.font_icon;
    }

    public static byte[] getFontEmoji() {
        return FontHolder.font_emoji;
    }

    public static float[] getFontBoundle(long vg) {
        float[] bond = new float[4];
        nvgTextBoundsJni(vg, 0, 0, FONT_GLYPH_TEMPLATE, 0, FONT_GLYPH_TEMPLATE.length, bond);
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

    static float[] RED_POINT_BACKGROUND = Nanovg.nvgRGBAf(1.f, 0, 0, 1.f);
    static float[] RED_POINT_FRONT = Nanovg.nvgRGBAf(1.f, 1.f, 1.f, 1.f);

    public static void drawRedPoint(long vg, String text, float x, float y, float r) {
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, r);
        nvgFillColor(vg, RED_POINT_BACKGROUND);
        nvgFill(vg);

        nvgFontSize(vg, r * 2 - 4);
        nvgFillColor(vg, RED_POINT_FRONT);
        nvgFontFace(vg, GToolkit.getFontWord());
        byte[] text_arr = toUtf8(text);
        nvgTextAlign(vg, Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
        if (text_arr != null) {
            Nanovg.nvgTextJni(vg, x, y + 1, text_arr, 0, text_arr.length);
        }

    }

    public static void drawCircle(long vg, float x, float y, float r, float[] color, boolean fill) {
        if (fill) {
            nvgBeginPath(vg);
            nvgCircle(vg, x, y, r);
            nvgFillColor(vg, color);
            nvgFill(vg);
        } else {
            nvgBeginPath(vg);
            nvgCircle(vg, x, y, r);
            nvgStrokeColor(vg, color);
            nvgStrokeWidth(vg, 1.0f);
            nvgStroke(vg);
        }
    }

    public static void drawRect(long vg, float x, float y, float w, float h, float[] color) {
        nvgBeginPath(vg);
        nvgFillColor(vg, color);
        nvgRect(vg, x, y, w, h);
        nvgFill(vg);
    }

    public static void drawRoundedRect(long vg, float x, float y, float w, float h, float r, float[] color) {
        nvgBeginPath(vg);
        nvgFillColor(vg, color);
        nvgRoundedRect(vg, x, y, w, h, r);
        nvgFill(vg);
    }

    public static float[] getTextBoundle(long vg, String s, float width) {
        float[] bond = new float[4];
        byte[] b = toUtf8(s);
        nvgTextBoxBoundsJni(vg, 0, 0, width, b, 0, b.length, bond);
//        bond[GObject.WIDTH] -= bond[GObject.LEFT];
//        bond[GObject.HEIGHT] -= bond[GObject.TOP];
//        bond[GObject.LEFT] = bond[GObject.TOP] = 0;
        return bond;
    }

    public static void drawText(long vg, float x, float y, float w, float h, String s) {

        drawText(vg, x, y, w, h, s, GToolkit.getStyle().getTextFontSize(), GToolkit.getStyle().getTextFontColor());
    }

    public static void drawText(long vg, float x, float y, float w, float h, String s, float fontSize, float[] color) {

        nvgFontSize(vg, fontSize);
        nvgFillColor(vg, color);
        nvgFontFace(vg, GToolkit.getFontWord());

        byte[] text_arr = toUtf8(s);

        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);

        if (text_arr != null) {
//            float[] bond = new float[4];
//            nvgTextBoxBoundsJni(vg, x, y, w, text_arr, 0, text_arr.length, bond);
//            bond[WIDTH] -= bond[LEFT];
//            bond[HEIGHT] -= bond[TOP];
//            bond[LEFT] = bond[TOP] = 0;
//
//            if (bond[HEIGHT] > h) {
//                y -= bond[HEIGHT] - h;
//            }
            nvgTextBoxJni(vg, x, y, w, text_arr, 0, text_arr.length);
        }
    }

    public static void drawImage(long vg, GImage img, float px, float py, float pw, float ph) {
        drawImage(vg, img, px, py, pw, ph, true, 1.f);
    }

    public static void drawImage(long vg, GImage img, float px, float py, float pw, float ph, boolean border, float alpha) {
        if (img == null) {
            return;
        }

        byte[] shadowPaint, imgPaint;
        float ix, iy, iw, ih;
        int[] imgW = {0}, imgH = {0};

        nvgImageSize(vg, img.getTexture(vg), imgW, imgH);
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

        imgPaint = nvgImagePattern(vg, px + ix + 1, py + iy + 1, iw - 2, ih - 2, 0.0f / 180.0f * (float) Math.PI, img.getTexture(vg), alpha);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, px, py, pw, ph, 5);
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
            nvgRoundedRect(vg, px + 1, py + 1, pw - 2, ph - 2, 4 - 0.5f);
            nvgStrokeWidth(vg, 1.0f);
            nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
            nvgStroke(vg);
        }
    }

    /**
     * return a frame to confirm msg
     *
     * @param title
     * @param msg
     * @param left
     * @param leftListener
     * @param right
     * @param rightListener
     * @return
     */
    static public GFrame getConfirmFrame(String title, String msg, String left, GActionListener leftListener, String right, GActionListener rightListener) {
        GFrame frame = new GFrame(title, 0, 0, 300, 170);
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
        float x = 10, y = 10, w = gp.getW() - 20;

        GLabel lb1 = new GLabel(msg, x, y, w, 80);
        lb1.setShowMode(GLabel.MODE_MULTI_SHOW);
        gp.add(lb1);
        y += 85;

        if (left != null) {
            GButton leftBtn = new GButton(left, x, y, 135, 28);
            leftBtn.setBgColor(128, 16, 8, 255);
            gp.add(leftBtn);
            leftBtn.setActionListener(leftListener);
        }

        GButton rightBtn = new GButton(right == null ? GLanguage.getString("Cancel") : right, x + 145, y, 135, 28);
        gp.add(rightBtn);
        rightBtn.setActionListener(rightListener == null ? new GActionListener() {
            @Override
            public void action(GObject gobj) {
                frame.close();
            }
        } : rightListener);

        return frame;
    }

    /**
     * @param title
     * @param strs
     * @param imgs
     * @param buttonListener
     * @param itemListener
     * @return
     */
    public static GFrame getListFrame(String title, String[] strs, GImage[] imgs, GActionListener buttonListener, GActionListener itemListener) {
        float pad = 2, btnW = 80, btnH = 28;
        float y = pad;

        GFrame frame = new GFrame(title, 0, 0, 300, 500);

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

        GTextField search = new GTextField("", "search", pad, y, frame.getW() - pad * 2, 30);
        search.setName("search");
        search.setBoxStyle(GTextField.BOX_STYLE_SEARCH);
        view.add(search);
        y += 30 + pad;

        float h = view.getH() - y - 30 - pad * 4;
        GList glist = new GList(0, y, view.getW(), h);
        glist.setName("list");
        glist.setShowMode(GList.MODE_MULTI_SHOW);
        glist.setSelectMode(GList.MODE_MULTI_SELECT);
        frame.getView().add(glist);
        y += h + pad;

        GCheckBox chbox = new GCheckBox(GLanguage.getString("SeleAll"), false, pad, y, view.getW() * .5f, btnH);
        view.add(chbox);
        chbox.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (((GCheckBox) gobj).isChecked()) {
                    glist.selectAll();
                } else {
                    glist.deSelectAll();
                }
            }
        });

        GButton btn = new GButton(GLanguage.getString("Perform"), (view.getW() - btnW - pad), y, btnW, btnH);
        btn.setName("perform");
        frame.getView().add(btn);
        btn.setActionListener(buttonListener);
        //
        glist.setItems(imgs, strs);

        return frame;
    }

    public static GFrame getInputFrame(String title, String msg, String defaultValue, String inputHint, String leftLabel, GActionListener leftListener, String rightLabel, GActionListener rightListener) {

        int x = 8, y = 10;
        GFrame frame = new GFrame(title, 50, 50, 300, 170);
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

        GLabel lb1 = new GLabel(msg, x, y, 280, 20);
        view.add(lb1);

        y += 25;
        GTextField input = new GTextField(defaultValue == null ? "" : defaultValue, inputHint, x, y, 280, 28);
        input.setName("input");
        view.add(input);
        y += 35;
        GLabel lb_state = new GLabel("", x, y, 280, 20);
        lb_state.setName("state");
        view.add(lb_state);
        y += 25;

        GButton cancelbtn = new GButton(leftLabel == null ? GLanguage.getString("Cancel") : leftLabel, x, y, 135, 28);
        view.add(cancelbtn);

        GButton okbtn = new GButton(rightLabel == null ? GLanguage.getString("Ok") : rightLabel, x + 145, y, 135, 28);
        okbtn.setBgColor(0, 96, 128, 255);
        view.add(okbtn);

        if (rightListener != null) {
            okbtn.setActionListener(rightListener);
        } else {
            okbtn.setActionListener((GObject gobj) -> {
                if (gobj.getFrame() != null) {
                    gobj.getFrame().close();
                }
            });
        }

        if (leftListener != null) {
            cancelbtn.setActionListener(leftListener);
        } else {
            cancelbtn.setActionListener((GObject gobj) -> {
                if (gobj.getFrame() != null) {
                    gobj.getFrame().close();
                }
            });
        }
        return frame;
    }

    public static GList getListMenu(String[] strs, GImage[] imgs, GActionListener[] listener) {

        GList list = new GList(0, 0, 150, 120);
        list.setShowMode(GList.MODE_MULTI_SHOW);
        list.setBgColor(GToolkit.getStyle().getFrameBackground());
        list.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject oldgo) {
            }

            @Override
            public void focusLost(GObject newgo) {
                if (list.getForm() != null) {
                    list.getForm().remove(list);
                }
            }
        });

        list.setItems(imgs, strs);
        GListItem[] items = list.getItems();
        if (listener != null) {
            for (int i = 0, imax = items.length; i < imax; i++) {
                items[i].setActionListener(listener[i]);
            }
        }
        int size = items.length;
        if (size > 8) {
            size = 8;
        }
        list.setSize(200, size * list.list_item_heigh);
        list.setInnerSize(200, size * list.list_item_heigh);
        list.setFront(true);

        return list;
    }

    public static GMenu getMenu(String[] strs, GImage[] imgs, GActionListener[] listener) {

        GMenu menu = new GMenu(0, 0, 150, 120);
        menu.setFocusListener(new GFocusChangeListener() {
            @Override
            public void focusGot(GObject oldgo) {
            }

            @Override
            public void focusLost(GObject newgo) {
                if (menu.getForm() != null) {
                    menu.getForm().remove(menu);
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

    public static GViewPort getImageView(GForm form, GImage img, GActionListener listener) {

        GViewPort view = new GViewPort() {
            GImage image = img;

            @Override
            public void longTouchedEvent(int x, int y) {
                GList menu = new GList();
                GListItem item = menu.addItems(null, GLanguage.getString("Save to album"));
                item.setActionListener((GObject gobj) -> {
                });
                item = menu.addItems(null, GLanguage.getString("Cancel"));
                item.setActionListener((GObject gobj) -> {
                    if (gobj.getForm() != null) {
                        gobj.getForm().remove(menu);
                    }
                });
                menu.setFront(true);
                add(menu);
            }

            @Override
            public boolean update(long vg) {
                float w = getW();
                float h = getH();

                GToolkit.drawImage(vg, image, getX(), getY(), w, h);

                return true;
            }

            @Override
            public void touchEvent(int phase, int x, int y) {
                if (getForm() != null) {
                    if (getElements().isEmpty()) {//no menu
                        getForm().remove(this);
                        //System.out.println("picture removed");
                    }
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

        view.setFront(true);

        float imgW = img.getWidth();
        float imgH = img.getHeight();

        float formW = form.getW();
        float formH = form.getH();

        float ratioW = formW / imgW;
        float ratioH = formH / imgH;

        if (formW < formH) {
            imgW *= ratioW;
            imgH *= ratioW;
        } else {
            imgW *= ratioH;
            imgH *= ratioH;
        }
        view.setSize(imgW, imgH);
        view.setLocation((formW - view.getW()) / 2, (formH - view.getH()) / 2);

        return view;
    }


    private static EditMenu editMenu;


    static public EditMenu getEditMenu() {
        return editMenu;
    }

    static public void disposeEditMenu() {
        if (editMenu != null) editMenu.dispose();
    }

    static public class EditMenu extends GMenu {

        boolean shown = false;
        GTextObject text;

        public EditMenu(int left, int top, int width, int height) {
            super(left, top, width, height);
        }

        @Override
        public boolean update(long vg) {
            if (text != null && text.getParent().getForm() == null) {
                dispose();
            }
            return super.update(vg);
        }


        synchronized void dispose() {
            GForm gf = getForm();
            if (gf != null) {
                gf.remove(editMenu);
                if (text != null) {
                    text.resetSelect();
                    text.selectMode = false;
                }
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

        float menuH = 40, menuW = 300;

        float mx = x - menuW / 2;
        if (mx < 10) {
            mx = 10;
        } else if (mx + menuW > focus.getForm().getDeviceWidth()) {
            mx = focus.getForm().getDeviceWidth() - menuW;
        }
        mx -= focus.getForm().getX();
        float my = y - 20 - menuH;
        if (my < 20) {
            my = y + 10;
        } else if (my + menuH > focus.getForm().getDeviceHeight()) {
            my = focus.getForm().getDeviceHeight() - menuH;
        }
        my -= focus.getForm().getY();

        if (editMenu == null) {
            editMenu = new EditMenu((int) mx, (int) my, (int) menuW, (int) menuH);
            editMenu.setFront(true);
            GMenuItem item;

            item = editMenu.addItem(GLanguage.getString("Select"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    editMenu.text.doSelectText();
                }
            });
            item = editMenu.addItem(GLanguage.getString("Copy"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    editMenu.text.doCopyClipBoard();
                    editMenu.dispose();
                }
            });
            item = editMenu.addItem(GLanguage.getString("Paste"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    if (editMenu.text.editable) {
                        editMenu.text.doPasteClipBoard();
                    }
                    editMenu.dispose();
                }
            });
            item = editMenu.addItem(GLanguage.getString("Cut"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    if (editMenu.text.editable) {
                        editMenu.text.doCut();
                    }
                    editMenu.dispose();
                }
            });
            item = editMenu.addItem(GLanguage.getString("SeleAll"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    editMenu.text.doSelectAll();
                }
            });

            editMenu.setFixed(true);
            editMenu.setContextMenu(true);
        }
        editMenu.text = focus;
        editMenu.setLocation(mx, my);
        //editMenu.move(mx - editMenu.getX(), my - editMenu.getY());

        focus.getForm().add(editMenu);
        //System.out.println("edit menu show");
    }

}
