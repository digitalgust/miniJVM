/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.awt.event.FocusListener;
import org.mini.glfm.Glfm;
import static org.mini.nanovg.Gutil.toUtf8;
import static org.mini.gui.GToolkit.nvgRGBA;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_LEFT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_MIDDLE;
import static org.mini.nanovg.Nanovg.NVG_CCW;
import static org.mini.nanovg.Nanovg.NVG_CW;
import static org.mini.nanovg.Nanovg.NVG_HOLE;
import static org.mini.nanovg.Nanovg.nvgArc;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgBoxGradient;
import static org.mini.nanovg.Nanovg.nvgClosePath;
import static org.mini.nanovg.Nanovg.nvgFill;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFillPaint;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgImagePattern;
import static org.mini.nanovg.Nanovg.nvgImageSize;
import static org.mini.nanovg.Nanovg.nvgLinearGradient;
import static org.mini.nanovg.Nanovg.nvgPathWinding;
import static org.mini.nanovg.Nanovg.nvgRect;
import static org.mini.nanovg.Nanovg.nvgRestore;
import static org.mini.nanovg.Nanovg.nvgRoundedRect;
import static org.mini.nanovg.Nanovg.nvgSave;
import static org.mini.nanovg.Nanovg.nvgStroke;
import static org.mini.nanovg.Nanovg.nvgStrokeColor;
import static org.mini.nanovg.Nanovg.nvgStrokeWidth;
import static org.mini.nanovg.Nanovg.nvgTextAlign;
import static org.mini.nanovg.Nanovg.nvgTextJni;
import static org.mini.nanovg.Nanovg.nvgTextMetrics;

/**
 *
 * @author gust
 */
public class GList extends GContainer implements GFocusChangeListener {

    char preicon;
    byte[] preicon_arr = toUtf8("" + ICON_CHEVRON_RIGHT);
    int curIndex;
    boolean pulldown;
    public static final int MODE_MULTI_LINE = 1, MODE_SINGLE_LINE = 0;
    int mode = MODE_SINGLE_LINE;
    GScrollBar scrollBar;
    float[] lineh = {0f};
    float list_image_size = 28;
    float list_item_heigh = 40;
    float list_rows_max = 7;
    float list_rows_min = 3;

    float pad = 5;

    float left, top, width, height;

    public GList(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;

        setLocation(left, top);
        setSize(width, height);

        //
        scrollBar = new GScrollBar(0, GScrollBar.VERTICAL, 0, 0, 20, 100);
        scrollBar.setActionListener(new ScrollBarActionListener());
        popWin.add(scrollBar);
        popWin.add(popPanel);
        setFocusListener(this);
        reBoundle();
        changeCurPanel();
    }

    @Override
    public int getType() {
        return TYPE_LIST;
    }

    public void setIcon(char icon) {
        preicon = icon;
    }

    public GListItem addItems(GImage img, String lab) {
        GListItem gli = new GListItem(img, lab);
        addItem(gli);
        return gli;
    }

    public void addItem(GListItem gli) {
        if (gli != null) {
            popPanel.add(gli);
            gli.list = this;
            reBoundle();
        }
    }

    public void addItem(int index, GListItem gli) {
        if (gli != null) {
            popPanel.add(index, gli);
            gli.list = this;
            reBoundle();
        }
    }

    public void removeItem(int index) {
        popPanel.remove(index);
        reBoundle();
    }

    public void removeItem(GObject go) {
        if (go.getType() != TYPE_LISTITEM) {
            throw new IllegalArgumentException("need GListItem");
        }
        popPanel.remove(go);
        reBoundle();
    }

    @Override
    public void reBoundle() {
        int itemcount = popPanel.elements.size();
        if (itemcount <= 0) {
            return;
        }

        if (mode == MODE_MULTI_LINE) {
            popWin.setLocation(0, 0);
            popWin.setSize(width, height);

        } else {
            float popH = itemcount * list_item_heigh;
            if (itemcount < list_rows_min) {
                popH = list_rows_min * list_item_heigh;
            }
            if (itemcount > list_rows_max) {
                popH = list_rows_max * list_item_heigh;
            }

            popWin.setLocation(0, 0);
            popWin.setSize(width, popH);

        }

        reAlignItems();
        
        normalPanel.setLocation(0, 0);
        normalPanel.setSize(width, height);

        scrollBar.setPos(popPanel.scrolly);
        flush();
    }

    void reAlignItems() {
        int i = 0;
        for (GObject go : popPanel.getElements()) {
            go.setLocation(pad, i * list_item_heigh);
            go.setSize(popPanel.getViewW() - pad * 2, list_item_heigh);
            i++;
        }
    }

    void changeCurPanel() {
        int itemcount = popPanel.elements.size();
        clear();

        if (mode == MODE_MULTI_LINE) {
            setLocation(left, top);
            setSize(width, height);
            add(popWin);
        } else if (pulldown && itemcount > 0) {
            float popH = itemcount * list_item_heigh;
            if (itemcount < list_rows_min) {
                popH = list_rows_min * list_item_heigh;
            }
            if (itemcount > list_rows_max) {
                popH = list_rows_max * list_item_heigh;
            }
            float popY = 0;
            if (popH > parent.getViewH()) {// small than frame height
                popY = parent.getY();
            } else if (top + popH < parent.getViewH()) {
                popY = top;
            } else {
                popY = parent.getViewH() - popH;
            }
            setLocation(left, popY);
            setSize(popWin.getViewW(), popWin.getViewH());
            add(popWin);
        } else {
            setLocation(left, top);
            setSize(width, height);
            add(normalPanel);
        }
    }

    public void setItems(GImage[] imgs, String[] labs) {
        if ((imgs == null && labs == null) || (imgs != null && labs != null && imgs.length != labs.length)) {
            throw new IllegalArgumentException("need images and labels count equals.");
        }
        int len = imgs == null ? labs.length : imgs.length;
        for (int i = 0; i < len; i++) {
            addItems(imgs == null ? null : imgs[i], labs == null ? null : labs[i]);
        }
        reBoundle();
    }

    public void setMode(int m) {
        this.mode = m;
        reBoundle();
        changeCurPanel();
    }

    public int getMode() {
        return this.mode;
    }

    public int getSelectedIndex() {
        return curIndex;
    }

    public void setSelectedIndex(int i) {
        curIndex = i;
    }

    public GListItem getSelectedItem() {
        if (curIndex < 0 || curIndex >= popPanel.elements.size()) {
            return null;
        }
        return (GListItem) popPanel.elements.get(curIndex);
    }

    @Override
    public void focusGot(GObject go) {
    }

    @Override
    public void focusLost(GObject go) {
        pulldown = false;
        changeCurPanel();
    }

    /**
     *
     * @param vg
     * @return
     */
    @Override
    public boolean update(long vg) {

        if (curIndex < 0) {
            curIndex = 0;
        }

        int itemcount = popPanel.elements.size();

        if (curIndex >= itemcount) {
            curIndex = itemcount - 1;
        }

        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        nvgTextMetrics(vg, null, null, lineh);

        Nanovg.nvgResetScissor(vg);
        Nanovg.nvgScissor(vg, getViewX(), getViewY(), getViewW(), getViewH());
        return super.update(vg);
    }

    static void drawText(long vg, float tx, float ty, float pw, float ph, String s) {
        if (s == null) {
            return;
        }
        nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
        byte[] b = toUtf8(s);
        Nanovg.nvgTextJni(vg, tx, ty, b, 0, b.length);
    }

    static void drawImage(long vg, float px, float py, float pw, float ph, GImage img) {
        if (img == null) {
            return;
        }
        byte[] shadowPaint, imgPaint;
        float ix, iy, iw, ih;
        float thumb = pw;
        int[] imgw = {0}, imgh = {0};

        nvgImageSize(vg, img.getTexture(), imgw, imgh);
        if (imgw[0] < imgh[0]) {
            iw = thumb;
            ih = iw * (float) imgh[0] / (float) imgw[0];
            ix = 0;
            iy = -(ih - thumb) * 0.5f;
        } else {
            ih = thumb;
            iw = ih * (float) imgw[0] / (float) imgh[0];
            ix = -(iw - thumb) * 0.5f;
            iy = 0;
        }

        imgPaint = nvgImagePattern(vg, px + ix, py + iy, iw, ih, 0.0f / 180.0f * (float) Math.PI, img.getTexture(), 0.8f);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, px, py, thumb, thumb, 5);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);

        shadowPaint = nvgBoxGradient(vg, px - 1, py, thumb + 2, thumb + 2, 5, 3, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, px - 5, py - 5, thumb + 10, thumb + 10);
        nvgRoundedRect(vg, px, py, thumb, thumb, 6);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, shadowPaint);
        nvgFill(vg);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, px + 0.5f, py + 0.5f, thumb - 1, thumb - 1, 4 - 0.5f);
        nvgStrokeWidth(vg, 1.0f);
        nvgStrokeColor(vg, nvgRGBA(255, 255, 255, 192));
        nvgStroke(vg);
    }

    void drawSpinner(long vg, float cx, float cy, float r, float t) {
        float a0 = 0.0f + t * 6;
        float a1 = (float) Math.PI + t * 6;
        float r0 = r;
        float r1 = r * 0.75f;
        float ax, ay, bx, by;
        byte[] paint;

        nvgSave(vg);

        nvgBeginPath(vg);
        nvgArc(vg, cx, cy, r0, a0, a1, NVG_CW);
        nvgArc(vg, cx, cy, r1, a1, a0, NVG_CCW);
        nvgClosePath(vg);
        ax = cx + (float) Math.cos(a0) * (r0 + r1) * 0.5f;
        ay = cy + (float) Math.sin(a0) * (r0 + r1) * 0.5f;
        bx = cx + (float) Math.cos(a1) * (r0 + r1) * 0.5f;
        by = cy + (float) Math.sin(a1) * (r0 + r1) * 0.5f;
        paint = nvgLinearGradient(vg, ax, ay, bx, by, nvgRGBA(0, 0, 0, 0), nvgRGBA(0, 0, 0, 128));
        nvgFillPaint(vg, paint);
        nvgFill(vg);

        nvgRestore(vg);
    }

    static float clampf(float a, float mn, float mx) {
        return a < mn ? mn : (a > mx ? mx : a);
    }

    /**
     *
     */
    GPanel normalPanel = new GPanel() {

        @Override
        public void touchEvent(int phase, int x, int y) {

            if (phase == Glfm.GLFMTouchPhaseEnded) {
                if (!pulldown) {
                    pulldown = true;
                    GList.this.changeCurPanel();
                }
            }
            super.touchEvent(phase, x, y);

        }

        @Override
        public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
            if (pressed) {
                if (!pulldown) {
                    pulldown = true;
                    GList.this.changeCurPanel();
                }
            }
            super.mouseButtonEvent(button, pressed, x, y);
        }

        void click() {

        }

        @Override
        public boolean update(long vg) {
            drawNormal(vg, normalPanel.getViewX(), normalPanel.getViewY(), normalPanel.getViewW(), normalPanel.getViewH());
            return true;
        }

        void drawNormal(long vg, float x, float y, float w, float h) {
            byte[] bg;

            if (pulldown) {
                bg = nvgLinearGradient(vg, x, y + h, x, y, nvgRGBA(255, 255, 255, 16), nvgRGBA(0, 0, 0, 16));
            } else {
                bg = nvgLinearGradient(vg, x, y, x, y + h, nvgRGBA(255, 255, 255, 16), nvgRGBA(0, 0, 0, 16));
            }
            float cornerRadius = 4.0f;
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, cornerRadius - 1);
            nvgFillPaint(vg, bg);
            nvgFill(vg);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, cornerRadius - 0.5f);
            nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
            nvgStroke(vg);

            if (popPanel.elements.size() > 0) {
                float thumb = h - pad;
                GListItem gli = (GListItem) popPanel.elements.get(curIndex);
                drawImage(vg, x + pad, y + h * 0.5f - thumb / 2, thumb, thumb, gli.img);

                drawText(vg, x + thumb + pad + pad, y + h / 2, thumb, thumb, gli.label);

                nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
                nvgFontFace(vg, GToolkit.getFontIcon());
                nvgTextJni(vg, x + w - thumb, y + h * 0.5f, preicon_arr, 0, preicon_arr.length);
            }
        }
    };

    /**
     *
     */
    GPanel popPanel = new GPanel() {

        @Override
        public boolean update(long vg) {
            float x = getViewX();
            float y = getViewY();
            float w = getViewW();
            float h = getViewH();

            GToolkit.getStyle().drawEditBoxBase(vg, x, y, w, h);

            super.update(vg);

            // Hide fades
            byte[] fadePaint;
            fadePaint = nvgLinearGradient(vg, x, y, x, y + 6, nvgRGBA(20, 20, 20, 192), nvgRGBA(30, 30, 30, 0));
            nvgBeginPath(vg);
            nvgRect(vg, x + 2, y, w - 4, 6);
            nvgFillPaint(vg, fadePaint);
            nvgFill(vg);

            fadePaint = nvgLinearGradient(vg, x, y + h, x, y + h - 6, nvgRGBA(20, 20, 20, 192), nvgRGBA(30, 30, 30, 0));
            nvgBeginPath(vg);
            nvgRect(vg, x + 2, y + h - 6, w - 4, 6);
            nvgFillPaint(vg, fadePaint);
            nvgFill(vg);
            nvgRestore(vg);
            return true;
        }

    };

    GListPopWindow popWin = new GListPopWindow();

    class GListPopWindow extends GContainer {

        @Override
        int getType() {
            return TYPE_UNKNOW;
        }

        @Override
        public void setSize(float width, float height) {
            super.setSize(width, height);

            popPanel.setLocation(0, 0);
            popPanel.setSize(width - 20, height);

            scrollBar.setLocation(width - 20, 0);
            scrollBar.setSize(20, height);
        }

    };

    class ScrollBarActionListener implements GActionListener {

        @Override
        public void action(GObject gobj) {
            popPanel.setScrollY(((GScrollBar) gobj).getPos());
            popPanel.reBoundle();
            flush();
        }

    }
}
