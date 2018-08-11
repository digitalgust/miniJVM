/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.TimerTask;
import org.mini.glfm.Glfm;
import static org.mini.nanovg.Gutil.toUtf8;
import static org.mini.gui.GToolkit.nvgRGBA;
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
import static org.mini.nanovg.Nanovg.nvgScissor;
import static org.mini.nanovg.Nanovg.nvgStroke;
import static org.mini.nanovg.Nanovg.nvgStrokeColor;
import static org.mini.nanovg.Nanovg.nvgStrokeWidth;
import static org.mini.nanovg.Nanovg.nvgTextAlign;
import static org.mini.nanovg.Nanovg.nvgTextJni;
import static org.mini.nanovg.Nanovg.nvgTextMetrics;
import static org.mini.nanovg.Nanovg.nvgTranslate;

/**
 *
 * @author gust
 */
public class GList extends GContainer {

    char preicon;
    byte[] preicon_arr = toUtf8("" + ICON_CHEVRON_RIGHT);
    int[] images;
    String[] labels;
    int curIndex;
    boolean pulldown;
    public static final int MODE_MULTI_LINE = 1, MODE_SINGLE_LINE = 0;
    int mode = MODE_SINGLE_LINE;
    GScrollBar scrollBar;
    float[] lineh = {0f};

    float[] popBoundle;
    float[] normalBoundle;
    float list_image_size = 28;
    float list_item_heigh = 40;
    float list_rows = 5;
    float list_cols = 1;
    float pad = 10;

    public GList(int left, int top, int width, int height) {
        setLocation(left, top);
        setSize(width, height);
        normalBoundle = boundle;
    }

    public void setIcon(char icon) {
        preicon = icon;
    }

    public void setItems(int[] imgs, String[] labs) {
        if (imgs == null || labs == null || imgs.length != labs.length) {
            throw new IllegalArgumentException("need images and labels count equals.");
        }
        images = imgs;
        labels = labs;
    }

    public int[] getImages() {
        return images;
    }

    public void setMode(int m) {
        this.mode = m;
    }

    public int getMode() {
        return this.mode;
    }

    public String[] getLabels() {
        return labels;
    }

    public int getSelectedIndex() {
        return curIndex;
    }

    public void setSelectedIndex(int i) {
        curIndex = i;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        if (isInArea(x, y)) {
            if (pressed) {
                boolean inScroll = false;
                if (scrollBar != null) {
                    inScroll = scrollBar.isInArea(x, y);
                }
                if (!inScroll) {
                    if (pulldown) {
                        float stackh = (labels.length / list_cols) * (list_item_heigh) + pad;
                        float pos = scrollBar.getPos() * (stackh - popBoundle[HEIGHT]) + (y - getY());
                        curIndex = (int) (pos / stackh * labels.length);
                        if (actionListener != null) {
                            actionListener.action(this);
                        }
                    }
                    pulldown = !pulldown;
                    parent.setFocus(this);
                }
            }
        }
        super.mouseButtonEvent(button, pressed, x, y);
    }

    int startX, startY;

    @Override
    public void touchEvent(int phase, int x, int y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        if (isInBoundle(boundle, rx, ry)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                startX = x;
                startY = y;
                if (task != null) {
                    task.cancel();
                    task = null;
                }
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                boolean inScroll = false;
                if (scrollBar != null) {
                    inScroll = isInBoundle(scrollBar.boundle, x - getX(), y - getY());
                }
                if (!inScroll && startX == x && startY == y) {
                    if (pulldown) {
                        float stackh = (labels.length / list_cols) * (list_item_heigh) + pad;
                        float pos = scrollBar.getPos() * (stackh - popBoundle[HEIGHT]) + (y - getY());
                        curIndex = (int) (pos / stackh * labels.length);
                        if (actionListener != null) {
                            actionListener.action(this);
                        }
                    }
                    pulldown = !pulldown;
                }
            }
        }
        super.touchEvent(phase, x, y);
    }

    @Override
    public void dragEvent(float scrollX, float scrollY, float x, float y) {
        scrollEvent(scrollX, scrollY, x, y);
    }

    @Override
    public void scrollEvent(float scrollX, float scrollY, float x, float y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        if (isInBoundle(boundle, rx, ry) && scrollBar != null) {
            scrollBar.setPos(scrollBar.getPos() - 1.f / labels.length * (float) (scrollY / list_item_heigh));
        }
    }

    //每多长时间进行一次惯性动作
    long inertiaPeriod = 16;
    //总共做多少次操作
    long maxMoveCount = 120;
    //惯性任务
    TimerTask task;

    @Override
    public void inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {
        double dx = x2 - x1;
        final double dy = y2 - y1;
        task = new TimerTask() {
            //惯性速度
            double speed = dy / (moveTime / inertiaPeriod);
            //阴力
            double resistance = -speed / maxMoveCount;
            //
            float count = 0;

            @Override
            public void run() {
//                System.out.println("inertia " + speed);
                speed += resistance;
                scrollBar.setPos(scrollBar.getPos() - 1.f / labels.length * (float) (speed / list_item_heigh));
                flush();
                if (count++ > maxMoveCount) {
                    try {
                        this.cancel();
                    } catch (Exception e) {
                    }
                }
            }
        };
        getTimer().schedule(task, 0, inertiaPeriod);
    }

    /**
     *
     * @param vg
     * @return
     */
    public boolean update(long vg) {
        if (parent.getFocus() != this) {
            pulldown = false;
        }
        if (mode == MODE_MULTI_LINE) {
            pulldown = true;
        }

        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        nvgTextMetrics(vg, null, null, lineh);

        if (pulldown && labels != null) {
            popBoundle = new float[4];

            if (mode == MODE_MULTI_LINE) {
                popBoundle[LEFT] = normalBoundle[LEFT];
                popBoundle[TOP] = normalBoundle[TOP];
                popBoundle[WIDTH] = normalBoundle[WIDTH];
                popBoundle[HEIGHT] = normalBoundle[HEIGHT];
                list_rows = popBoundle[HEIGHT] / list_item_heigh;
            } else {
                float popH = labels.length * list_item_heigh;
                if (labels.length > list_rows) {
                    popH = list_rows * list_item_heigh;
                }
                float popY = 0;
                if (popH > parent.getH()) {// small than frame height
                    popH = parent.getH();
                    popY = parent.getY();
                } else if (normalBoundle[TOP] + popH < parent.getH()) {
                    popY = normalBoundle[TOP];
                } else {
                    popY = parent.getH() - popH;
                }

                popBoundle[LEFT] = normalBoundle[LEFT];
                popBoundle[TOP] = popY;
                popBoundle[WIDTH] = normalBoundle[WIDTH];
                popBoundle[HEIGHT] = popH;
            }
            boundle = popBoundle;
            if (scrollBar == null) {
                scrollBar = new GScrollBar(0, GScrollBar.VERTICAL,
                        (int) (boundle[WIDTH] - 22 + 1),
                        (int) (pad),
                        20,
                        (int) (boundle[HEIGHT] - pad * 2));
                add(scrollBar);
            }
            drawPop(vg,
                    getX(),
                    getY(),
                    getW(),
                    getH(),
                    images, labels);
            super.update(vg);
        } else {
            boundle = normalBoundle;
            float x = getX();
            float y = getY();
            float w = getW();
            float h = getH();
            nvgScissor(vg, x, y, w, list_item_heigh);
            drawNormal(vg, x, y, w, list_item_heigh);
        }

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
        float thumb = h - pad;
        drawImage(vg, x + pad, y + h * 0.5f - thumb / 2, thumb, thumb, curIndex);

        drawText(vg, x + thumb + pad + pad, y + h / 2, thumb, thumb, curIndex);

        nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
        nvgFontFace(vg, GToolkit.getFontIcon());
        nvgTextJni(vg, x + w - thumb, y + h * 0.5f, preicon_arr, 0, preicon_arr.length);
    }

    void drawPop(long vg, float x, float y, float w, float h, int[] images, String[] strs) {
        if (images == null) {
            images = new int[]{0};
            strs = new String[]{""};
        }
        int nimages = images.length;
        float cornerRadius = 3.0f;
        byte[] shadowPaint, fadePaint;
        float thumb = list_item_heigh - pad;
        float stackh = (nimages / list_cols) * (list_item_heigh) + pad;

        nvgSave(vg);
//	nvgClearState(vg);
        nvgScissor(vg, x, y, w, h);

        // Window
        GToolkit.getStyle().drawEditBoxBase(vg, x, y, w, h);
//        nvgBeginPath(vg);
//        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
//        nvgFillColor(vg, nvgRGBA(60, 60, 60, 192));
//        nvgFill(vg);

        // Drop shadow
//        shadowPaint = nvgBoxGradient(vg, x, y + 2, w, h, cornerRadius * 2, 10, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
//        nvgBeginPath(vg);
//        nvgRect(vg, x - 10, y - 10, w + 20, h + 30);
//        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
//        nvgPathWinding(vg, NVG_HOLE);
//        nvgFillPaint(vg, shadowPaint);
//        nvgFill(vg);
        nvgSave(vg);
        float th = -(stackh - h) * scrollBar.getPos();
        nvgTranslate(vg, 0, th);

        for (int i = 0; i < nimages; i++) {
            float tx, ty;
            tx = x + pad;
            ty = y + pad;
            tx += (i % list_cols) * (thumb + pad);
            ty += (i / list_cols) * (thumb + pad);

            if (curIndex == i) {
                GToolkit.drawRect(vg, tx, ty, w - (thumb + pad), list_item_heigh - pad, GToolkit.getStyle().getSelectedColor());
            }

            drawImage(vg, tx, ty, thumb, thumb, i);

            drawText(vg, tx + thumb + pad, ty + thumb / 2, thumb, thumb, i);
        }
        nvgRestore(vg);

        // Hide fades
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
    }

    void drawText(long vg, float tx, float ty, float pw, float ph, int i) {
        nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
        //Nanovg.nvgScissor(vg, x, y, w, h);
        byte[] b = toUtf8(labels[i]);
        Nanovg.nvgTextJni(vg, tx, ty, b, 0, b.length);
        //Nanovg.nvgResetScissor(vg);
    }

    void drawImage(long vg, float px, float py, float pw, float ph, int i) {

        byte[] shadowPaint, imgPaint;
        float ix, iy, iw, ih;
        float thumb = pw;
        int[] imgw = {0}, imgh = {0};

        nvgImageSize(vg, images[i], imgw, imgh);
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

        imgPaint = nvgImagePattern(vg, px + ix, py + iy, iw, ih, 0.0f / 180.0f * (float) Math.PI, images[i], 0.8f);
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

}
