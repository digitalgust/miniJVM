/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.TimerTask;
import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import static org.mini.gui.GObject.isInBoundle;
import static org.mini.nanovg.Gutil.toUtf8;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_LEFT;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_TOP;
import static org.mini.nanovg.Nanovg.nvgCreateNVGglyphPosition;
import static org.mini.nanovg.Nanovg.nvgCreateNVGtextRow;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgNVGglyphPosition_x;
import static org.mini.nanovg.Nanovg.nvgRestore;
import static org.mini.nanovg.Nanovg.nvgSave;
import static org.mini.nanovg.Nanovg.nvgTextAlign;
import static org.mini.nanovg.Nanovg.nvgTextBreakLinesJni;
import static org.mini.nanovg.Nanovg.nvgTextGlyphPositionsJni;
import static org.mini.nanovg.Nanovg.nvgTextJni;
import static org.mini.nanovg.Nanovg.nvgTextMetrics;

/**
 *
 * @author gust
 */
public class GTextBox extends GTextObject {

    //
    float[] lineh = {0};
    private int caretIndex;//光标在字符串中的位置

    int selectStart = -1;//选取开始
    int selectEnd = -1;//选取结束
    boolean adjustSelStart = true;//是修改选择起点还是终点
    boolean selectAdjusted;//在选取状态下,如果点击了,但是没有修改位置,取消选取状态

    int totalRows;//字符串总行数，动态计算出
    int showRows;//可显示行数

    short[][] area_detail;
    int scrollDelta;
    float scroll = 0;//0-1 区间,描述窗口滚动条件位置, 滚动符0-1分别对应文本顶部超出显示区域的高度百分比
    float totalTextHeight;//字符串总高度
    float showAreaHeight;//显示区域高度
    //
    static final int AREA_DETAIL_ADD = 7;//额外增加slot数量
    static final int AREA_START = 4;//字符串起点位置
    static final int AREA_END = 5;//字符终点位置
    static final int AREA_ROW = 6;//行号
    static final int AREA_X = LEFT;
    static final int AREA_Y = TOP;
    static final int AREA_W = WIDTH;
    static final int AREA_H = HEIGHT;

    //
    boolean drag;

    public GTextBox(String text, String hint, int left, int top, int width, int height) {
        setText(text);
        setHint(hint);
        setLocation(left, top);
        setSize(width, height);
        setFocusListener(this);
    }

    boolean isInArea(short[] bound, float x, float y) {
        return x >= bound[LEFT] && x <= bound[LEFT] + bound[WIDTH]
                && y >= bound[TOP] && y <= bound[TOP] + bound[HEIGHT];
    }

    /**
     * 返回指定位置所在字符串中的位置
     *
     * @param x
     * @param y
     * @return
     */
    int getCaretIndexFromArea(int x, int y) {
        if (area_detail != null) {
            for (short[] detail : area_detail) {
                if (detail != null) {
                    if (isInArea(detail, x, y)) {
                        for (int i = AREA_DETAIL_ADD, imax = detail.length; i < imax; i++) {
                            int x0 = detail[i];
                            int x1 = (i + 1 < imax) ? detail[i + 1] : detail[AREA_X] + detail[AREA_W];
                            if (x >= x0 && x < x1) {
                                if (x > detail[detail.length - 1]) {
                                    int cidx = detail[AREA_START] + (i - AREA_DETAIL_ADD) + 1;
                                    if (cidx > textsb.length()) {//字符串結尾加了一個0,所以會多一個字符出來
                                        cidx = textsb.length();
                                    }
                                    return cidx;
                                } else {
                                    return detail[AREA_START] + (i - AREA_DETAIL_ADD);
                                }
                            }
                        }
                        return detail[AREA_END];

                    }
                }
            }
        }
        return -1;
    }

    /**
     * 返回光标当前所在的x,y坐标,及行号,数组下标
     *
     * @return
     */
    int[] getCaretPosFromArea() {
        if (area_detail != null) {
            int i = 0;
            for (short[] detail : area_detail) {
                if (detail != null) {
                    if (caretIndex >= detail[AREA_START] && caretIndex < detail[AREA_END]) {
                        int idx = caretIndex - detail[AREA_START] + AREA_DETAIL_ADD - 1;
                        int x = idx < detail[AREA_END] ? detail[idx] : detail[AREA_X] + +detail[AREA_W];
                        return new int[]{x + (int) lineh[0] / 2, detail[AREA_Y] + (int) lineh[0] / 2, detail[AREA_ROW], i};
                    }
                }
                i++;
            }
        }
        return null;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        if (isInArea(x, y)) {
            if (button == Glfw.GLFW_MOUSE_BUTTON_1) {
                if (pressed) {
                    int caret = getCaretIndexFromArea(x, y);
                    if (caret >= 0) {
                        setCaretIndex(caret);
                        resetSelect();
                        selectStart = caret;
                        drag = true;
                    }
                } else {
                    disposeEditMenu();
                    drag = false;
                    if (selectEnd == -1 || selectStart == selectEnd) {
                        resetSelect();
                    }
                }
            } else if (button == Glfw.GLFW_MOUSE_BUTTON_2) {
                if (pressed) {

                } else {
                    callEditMenu(this, x, y);
                }
            }

        }
    }

    @Override
    public void clickEvent(int button, int x, int y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        if (isInArea(x, y)) {
            int caret = getCaretIndexFromArea(x, y);
            if (caret >= 0) {
                setCaretIndex(caret);
                resetSelect();
                drag = false;
            }
            doSelectText();
        }
    }

    @Override
    public void cursorPosEvent(int x, int y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        if (isInArea(x, y)) {
            if (drag) {
                int caret = getCaretIndexFromArea(x, y);
                if (caret >= 0) {
                    selectEnd = caret;
                }
            }
        }
    }

    /**
     *
     * @param character
     */
    @Override
    public void characterEvent(char character) {
        if (parent.getFocus() != this) {
            return;
        }
        int[] selectFromTo = getSelected();
        if (selectFromTo != null) {
            deleteSelectedText();
        }
        textsb.insert(caretIndex, character);
        caretIndex++;
        text_arr = null;
    }

    @Override
    public void keyEvent(int key, int scanCode, int action, int mods) {
        if (parent.getFocus() != this) {
            return;
        }
        if (action == Glfw.GLFW_PRESS || action == Glfw.GLFW_REPEAT) {
            switch (key) {
                case Glfw.GLFW_KEY_BACKSPACE: {
                    if (textsb.length() > 0 && caretIndex > 0) {
                        int[] selectFromTo = getSelected();
                        if (selectFromTo != null) {
                            deleteSelectedText();
                        } else {
                            textsb.delete(caretIndex - 1, caretIndex);
                            setCaretIndex(caretIndex - 1);
                            text_arr = null;
                        }
                    }
                    break;
                }
                case Glfw.GLFW_KEY_DELETE: {
                    if (textsb.length() > caretIndex) {
                        int[] selectFromTo = getSelected();
                        if (selectFromTo != null) {
                            deleteSelectedText();
                        } else {
                            textsb.delete(caretIndex, caretIndex + 1);
                            text_arr = null;
                        }
                    }
                    break;
                }
                case Glfw.GLFW_KEY_ENTER: {
                    String txt = getText();
                    if (txt != null && txt.length() > 0) {
                        int[] selectFromTo = getSelected();
                        if (selectFromTo != null) {
                            deleteSelectedText();
                        }
                        setCaretIndex(caretIndex + 1);
                        textsb.insert(caretIndex, "\n");
                        text_arr = null;
                    }
                    break;
                }
                case Glfw.GLFW_KEY_LEFT: {
                    if (textsb.length() > 0 && caretIndex > 0) {
                        setCaretIndex(caretIndex - 1);
                    }
                    break;
                }
                case Glfw.GLFW_KEY_RIGHT: {
                    if (textsb.length() > caretIndex) {
                        setCaretIndex(caretIndex + 1);
                    }
                    break;
                }
                case Glfw.GLFW_KEY_UP: {
                    int[] pos = getCaretPosFromArea();
//                    if (topShowRow > 0 && (pos == null || pos[2] == topShowRow)) {
//                        topShowRow--;
//                    }
                    setScroll(scroll - lineh[0] / (totalTextHeight - showAreaHeight));

                    if (pos != null) {
                        int cart = getCaretIndexFromArea(pos[0], pos[1] - (int) lineh[0]);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }
                    }
                    break;
                }
                case Glfw.GLFW_KEY_DOWN: {
                    int[] pos = getCaretPosFromArea();
//                    if (topShowRow < totalRows - showRows && (pos == null || pos[2] == topShowRow + showRows - 1)) {
//                        topShowRow++;
//                    }
                    setScroll(scroll + lineh[0] / (totalTextHeight - showAreaHeight));
                    if (pos != null) {
                        int cart = getCaretIndexFromArea(pos[0], pos[1] + (int) lineh[0]);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }

                    }
                    break;
                }
            }
        }
    }

    @Override
    public void touchEvent(int phase, int x, int y) {
        int rx = (int) (x - parent.getX());
        int ry = (int) (y - parent.getY());
        if (isInBoundle(boundle, rx, ry)) {
            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan: {
                    int caret = getCaretIndexFromArea(x, y);
                    if (selectMode) {
                        selectAdjusted = false;
                        if (Math.abs(caret - selectStart) < Math.abs(caret - selectEnd)) {
                            adjustSelStart = true;
                        } else {
                            adjustSelStart = false;
                        }
                    } else if (caret >= 0) {
                        setCaretIndex(caret);
                        disposeEditMenu();
                    }       //
                    if (task != null) {
                        task.cancel();
                        task = null;
                    }
                    break;
                }
                case Glfm.GLFMTouchPhaseEnded: {
                    if (selectMode) {
                        if (selectStart != -1) {
                            if (!selectAdjusted) {
                                //System.out.println("canceled:"+selectStart);
                                disposeEditMenu();
                            }
                        }
                    }
                    break;
                }
                case Glfm.GLFMTouchPhaseMoved: {
                    if (selectMode) {
                        int caret = getCaretIndexFromArea(x, y);
                        if (adjustSelStart) {
                            if (caret < selectEnd) {
                                selectStart = caret;

                            }
                        } else if (caret > selectStart) {
                            selectEnd = caret;
                            setCaretIndex(selectEnd);
                        }
                        selectAdjusted = true;
                    }
                    break;
                }
                default:
                    break;
            }
        }

    }

    /**
     *
     * @param str
     * @param mods
     * @param character
     */
    @Override
    public void characterEvent(String str, int mods) {

        int[] selectFromTo = getSelected();
        if (selectFromTo != null) {
            deleteSelectedText();
        }
        insertTextAtCaret(str);
    }

    @Override
    public void keyEvent(int key, int action, int mods) {

        if (action == Glfm.GLFMKeyActionPressed || action == Glfm.GLFMKeyActionRepeated) {
            switch (key) {
                case Glfm.GLFMKeyBackspace: {
                    if (textsb.length() > 0 && caretIndex > 0) {
                        int[] selectFromTo = getSelected();
                        if (selectFromTo != null) {
                            deleteSelectedText();
                        } else {
                            textsb.delete(caretIndex - 1, caretIndex);
                            setCaretIndex(caretIndex - 1);
                            text_arr = null;
                        }
                    }
                    break;
                }
//                case Glfm.GLFMKeyDelete: {
//                    if (textsb.length() > caretIndex) {
//                        int[] selectFromTo = getSelected();
//                        if (selectFromTo != null) {
//                            delectSelect();
//                        } else {
//                            textsb.delete(caretIndex, caretIndex + 1);
//                            text_arr = null;
//                        }
//                    }
//                    break;
//                }
                case Glfm.GLFMKeyEnter: {
                    String txt = getText();
                    if (txt != null && txt.length() > 0) {
                        int[] selectFromTo = getSelected();
                        if (selectFromTo != null) {
                            deleteSelectedText();
                        }
                        setCaretIndex(caretIndex + 1);
                        textsb.insert(caretIndex, "\n");
                        text_arr = null;
                    }
                    break;
                }
                case Glfm.GLFMKeyLeft: {
                    if (textsb.length() > 0 && caretIndex > 0) {
                        setCaretIndex(caretIndex - 1);
                    }
                    break;
                }
                case Glfm.GLFMKeyRight: {
                    if (textsb.length() > caretIndex) {
                        setCaretIndex(caretIndex + 1);
                    }
                    break;
                }
                case Glfm.GLFMKeyUp: {
                    int[] pos = getCaretPosFromArea();
//                    if (topShowRow > 0 && (pos == null || pos[2] == topShowRow)) {
//                        topShowRow--;
//                    }
                    setScroll(scroll - lineh[0] / (totalTextHeight - showAreaHeight));

                    if (pos != null) {
                        int cart = getCaretIndexFromArea(pos[0], pos[1] - (int) lineh[0]);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }
                    }
                    break;
                }
                case Glfm.GLFMKeyDown: {
                    int[] pos = getCaretPosFromArea();
//                    if (topShowRow < totalRows - showRows && (pos == null || pos[2] == topShowRow + showRows - 1)) {
//                        topShowRow++;
//                    }
                    setScroll(scroll + lineh[0] / (totalTextHeight - showAreaHeight));
                    if (pos != null) {
                        int cart = getCaretIndexFromArea(pos[0], pos[1] + (int) lineh[0]);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }

                    }
                    break;
                }
            }
        }
    }

    //每多长时间进行一次惯性动作
    long inertiaPeriod = 16;
    //总共做多少次操作
    long maxMoveCount = 120;
    //惯性任务
    TimerTask task;

    @Override
    public void inertiaEvent(double x1, double y1, double x2, double y2, final long moveTime) {
        double dx = x2 - x1;
        final double dy = y2 - y1;
        scrollDelta = 0;
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
                speed += resistance;//速度和阴力抵消为0时,退出滑动

                float dh = getOutOfShowAreaHeight();
                if (dh > 0) {
                    setScroll(scroll - (float) speed / dh);
                }
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

    @Override
    public void scrollEvent(double scrollX, double scrollY, int x, int y) {
        if (selectMode) {
            return;
        }
        if (isInArea(x, y)) {
            float dh = getOutOfShowAreaHeight();
            if (dh > 0) {
                setScroll(scroll - (float) scrollY / dh);
            }
        }
    }

    void setScroll(float p) {
        scroll = p;
        if (scroll > 1) {
            scroll = 1.f;
        }
        if (scroll < 0) {
            scroll = 0.f;
        }
    }

    float getOutOfShowAreaHeight() {
        float dh = totalTextHeight - showAreaHeight;
        return dh < 0 ? 0 : dh;
    }

    @Override
    public void deleteSelectedText() {
        int[] sarr = getSelected();
        if (sarr == null || sarr[0] == -1 || sarr[1] == -1) {
            return;
        }
        setCaretIndex(sarr[0]);
        textsb.delete(sarr[0], sarr[1]);
        text_arr = null;
        resetSelect();
    }

    @Override
    void resetSelect() {
        selectStart = selectEnd = -1;
    }

    int[] getSelected() {
        int select1 = 0, select2 = 0;
        if (selectStart != -1 && selectEnd != -1) {
            select1 = selectStart > selectEnd ? selectEnd : selectStart;
            select2 = selectStart < selectEnd ? selectEnd : selectStart;
            return new int[]{select1, select2};
        }
        return null;
    }

    @Override
    public String getSelectedText() {
        int[] sarr = getSelected();
        if (sarr == null || sarr[0] == -1 || sarr[1] == -1) {
            return null;
        }
        return textsb.substring(sarr[0], sarr[1]);
    }

    @Override
    public void insertTextAtCaret(String str) {
        for (int i = 0, imax = str.length(); i < imax; i++) {
            char character = str.charAt(i);
            textsb.insert(caretIndex, character);
            setCaretIndex(caretIndex + 1);
        }
        text_arr = null;
    }

    @Override
    public void doSelectText() {
        if (caretIndex <= 0) {
            caretIndex = 0;
            selectStart = 0;
        }
        int txtLen = textsb.length();
        if (caretIndex >= txtLen) {
            caretIndex = txtLen;
            selectEnd = txtLen;
        }

        for (int i = caretIndex - 1; i >= 0 && i < txtLen; i--) {
            char ch = textsb.charAt(i);
            if (ch > 128 || ch <= ' ' || i == 0) {
                selectStart = i;
                break;
            }
        }
        for (int i = caretIndex + 1; i < txtLen; i++) {
            char ch = textsb.charAt(i);
            if (ch > 128 || ch <= ' ' || i == txtLen - 1) {
                selectEnd = i;
                break;
            }
        }
        setCaretIndex(selectEnd);
        selectMode = true;
//        String s=textsb.substring(selectStart,selectEnd);
//        System.out.println("select :"+s);
    }

    @Override
    public void doSelectAll() {
        selectStart = 0;
        selectEnd = textsb.length();
        selectMode = true;
    }

    /**
     * @param caretIndex the caretIndex to set
     */
    private void setCaretIndex(int caretIndex) {
        this.caretIndex = caretIndex;
    }

    /**
     *
     * @param vg
     * @return
     */
    @Override
    public boolean update(long vg) {
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();
        drawTextBox(vg, x, y, w, h);
        return true;
    }

    void drawTextBox(long vg, float x, float y, float w, float h) {
        GToolkit.getStyle().drawEditBoxBase(vg, x, y, w, h);
        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);

        //字高
        nvgTextMetrics(vg, null, null, lineh);
        float lineH = lineh[0];

        float[] text_area = new float[]{x + 5f, y + 5f, w - 10f, h - 10f};
        float dx = text_area[LEFT];
        float dy = text_area[TOP];

        //画文本或提示
        if ((getText() == null || getText().length() <= 0) && parent.getFocus() != this) {
            nvgFillColor(vg, GToolkit.getStyle().getHintFontColor());
            nvgTextJni(vg, dx, dy, hint_arr, 0, hint_arr.length);
        } else {//编辑中
            int topShowRow = 0;//显示区域第一行的行号

            if (text_arr == null) {//文字被修改过
                text_arr = toUtf8(textsb.toString());
                showRows = Math.round(text_area[HEIGHT] / lineH) + 2;
                showAreaHeight = text_area[HEIGHT];

                //用于存放屏墓中各行的一些位置信息
                area_detail = new short[showRows][];
                float[] bond = new float[4];
                Nanovg.nvgTextBoxBoundsJni(vg, 0, 0, text_area[WIDTH], text_arr, 0, text_arr.length, bond);
                totalRows = Math.round((bond[HEIGHT] - bond[TOP]) / lineH);
                totalTextHeight = bond[HEIGHT];
            }
            //
            float dh = scroll * (totalTextHeight - showAreaHeight);
            dh = dh < 0 ? 0 : dh;
            dy -= dh;
            topShowRow = (int) (dh / lineH) - 1;
            //
            int posCount = 100;
            int rowCount = 10;
            long rowsHandle = nvgCreateNVGtextRow(rowCount);
            long glyphsHandle = nvgCreateNVGglyphPosition(posCount);
            int nrows, i, char_count;
            float caretx = 0;

            nvgSave(vg);
            Nanovg.nvgScissor(vg, text_area[LEFT], text_area[TOP], text_area[WIDTH], text_area[HEIGHT]);
            Nanovg.nvgIntersectScissor(vg, parent.getX(), parent.getY(), parent.getViewW(), parent.getViewH());
            //需要恢复现场
            try {

                //取选取的起始和终止位置
                int[] selectFromTo = getSelected();

                // The text break API can be used to fill a large buffer of rows,
                // or to iterate over the text just few lines (or just one) at a time.
                // The "next" variable of the last returned item tells where to continue.
                //取UTF8字符串的内存地址，供NATIVE API调用
                long ptr = GToolkit.getArrayDataPtr(text_arr);
                int start = 0;
                int end = text_arr.length;

                int char_at = 0;
                int char_starti, char_endi;

                int row_index = 0;

                //通过nvgTextBreakLinesJni进行断行
                while ((nrows = nvgTextBreakLinesJni(vg, text_arr, start, end, text_area[WIDTH], rowsHandle, rowCount)) != 0) {

                    //循环绘制行
                    for (i = 0; i < nrows; i++) {
//                        if (area_row_index >= topShowRowLocal && area_row_index < topShowRowLocal + showRows) {
                        if (dy + lineH >= text_area[TOP] && dy < text_area[TOP] + text_area[HEIGHT]) {
                            //取得第i 行的行宽
                            float row_width = Nanovg.nvgNVGtextRow_width(rowsHandle, i);

                            //返回 i 行的起始和结束位置
                            int byte_starti = (int) (Nanovg.nvgNVGtextRow_start(rowsHandle, i) - ptr);
                            int byte_endi = (int) (Nanovg.nvgNVGtextRow_end(rowsHandle, i) - ptr);

                            if (char_at == 0) {
                                //取得本行之前字符串长度
                                String preStrs = new String(text_arr, 0, byte_starti, "utf-8");
                                char_at = preStrs.length();

                            }
                            //把当前行从字节数组转成字符串
                            String curRowStrs = "";
                            curRowStrs = new String(text_arr, byte_starti, byte_endi - byte_starti, "utf-8");
                            //计算字符串起止位置
                            char_starti = char_at;
                            char_endi = char_at + curRowStrs.length() - 1;

                            caretx = dx;
                            //取得i行的各个字符的具体位置，结果存入glyphs
                            char_count = nvgTextGlyphPositionsJni(vg, dx, dy, text_arr, byte_starti, byte_endi, glyphsHandle, posCount);
                            int curRow = row_index - topShowRow;

                            if (curRow < 0 || curRow >= area_detail.length) {
                                break;
                            }
                            //把这些信息存下来，用于在点击的时候找到点击了文本的哪个位置
                            //前面存固定信息
                            area_detail[curRow] = new short[AREA_DETAIL_ADD + char_count];
                            area_detail[curRow][AREA_X] = (short) dx;
                            area_detail[curRow][AREA_Y] = (short) dy;
                            area_detail[curRow][AREA_W] = (short) text_area[WIDTH];
                            area_detail[curRow][AREA_H] = (short) lineH;
                            area_detail[curRow][AREA_START] = (short) char_starti;
                            area_detail[curRow][AREA_END] = (short) char_endi;
                            area_detail[curRow][AREA_ROW] = (short) row_index;
                            //后面把每个char的位置存下来
                            for (int j = 0; j < char_count; j++) {
                                //取第 j 个字符的X座标
                                float x0 = nvgNVGglyphPosition_x(glyphsHandle, j);
                                area_detail[curRow][AREA_DETAIL_ADD + j] = (short) x0;
                            }

                            //计算下一行开始
                            char_at = char_at + curRowStrs.length();

                            if (parent.getFocus() == this) {
                                boolean draw = false;
                                if (caretIndex >= char_starti && caretIndex <= char_endi) {
                                    caretx = area_detail[curRow][AREA_DETAIL_ADD + (caretIndex - char_starti)];
                                    draw = true;
                                    if (caretIndex != 0 && caretIndex - char_starti == 0) {//光标移到行首时，只显示在上一行行尾
                                        draw = false;
                                    }
                                } else if (caretIndex == char_endi + 1) {
                                    caretx = dx + row_width;
                                    draw = true;
                                } else if (char_count == 0) {
                                    caretx = dx;
                                    draw = true;
                                }
                                if (draw) {
                                    GToolkit.drawCaret(vg, caretx, dy, 1, lineH, false);
                                }
                            }

                            if (selectFromTo != null) {
                                int sel_start = selectFromTo[0];
                                int sel_end = selectFromTo[1];
                                float drawSelX = dx, drawSelW = row_width;
                                //本行只有选择起点
                                if (sel_start > char_starti && sel_start < char_endi) {
                                    int pos = sel_start - area_detail[curRow][AREA_START];
                                    drawSelX = area_detail[curRow][AREA_DETAIL_ADD + pos];
                                }
                                //本行有选择终点
                                if (sel_end > char_starti && sel_end < char_endi) {
                                    int pos = selectFromTo[1] - area_detail[curRow][AREA_START];
                                    drawSelW = area_detail[curRow][AREA_DETAIL_ADD + pos] - drawSelX;
                                }

                                if (sel_start >= char_endi || sel_end < char_starti) {
                                    //此行没有起点和终点
                                } else {
                                    //此行有起点或终点,或在起终点之间的整行
                                    GToolkit.drawRect(vg, drawSelX, dy, drawSelW, lineH, GToolkit.getStyle().getSelectedColor());
                                }

                            }
                            nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
                            nvgTextJni(vg, dx, dy, text_arr, byte_starti, byte_endi);

                        }
                        dy += lineH;
                        row_index++;
                    }

                    long next = Nanovg.nvgNVGtextRow_next(rowsHandle, nrows - 1);
                    start = (int) (next - ptr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            nvgRestore(vg);

            Nanovg.nvgDeleteNVGtextRow(rowsHandle);
            Nanovg.nvgDeleteNVGglyphPosition(glyphsHandle);

        }
    }

}
