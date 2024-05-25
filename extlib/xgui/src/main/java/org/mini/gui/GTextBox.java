/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.nanovg.Nanovg;
import org.mini.util.CodePointBuilder;

import java.util.Timer;
import java.util.TimerTask;

import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GTextBox extends GTextObject {

    //

    protected GScrollBar scrollBar;//滚动条
    protected EditArea editArea;//编辑区

    static final int SCROLLBAR_WIDTH = 20;
    static final int PAD = 5;

    protected int curCaretRow;
    protected int curCaretCol;
    protected boolean showCaretPos = false;

    protected boolean mouseDrag;
    protected int caretIndex;//光标在字符串中的位置
    protected int selFirst = -1;//选取开始
    protected int selSecond = -1;//选取结束
    protected boolean adjustSelStart = true;//是修改选择起点还是终点
    protected boolean selectAdjusted;//在选取状态下,如果点击了,但是没有修改位置,取消选取状态
    protected float scroll = 0;//0-1 区间,描述窗口滚动条件位置, 滚动符0-1分别对应文本顶部超出显示区域的高度百分比
    //
    protected float[] lineh = {0};


    public GTextBox(GForm form) {
        this(form, "", "", 0f, 0f, 1f, 1f);
    }

    public GTextBox(GForm form, String text, String hint, float left, float top, float width, float height) {
        super(form);
        editArea = new EditArea(this, form, left, top, width - SCROLLBAR_WIDTH, height);
        add(editArea);
        scrollBar = new GScrollBar(form, scroll, GScrollBar.VERTICAL, getW() - SCROLLBAR_WIDTH, 0, SCROLLBAR_WIDTH, getH());
        scrollBar.setStateChangeListener(gobj -> {
            scroll = scrollBar.getPos();
        });

        setText(text);
        setHint(hint);
        setLocation(left, top);
        setSize(width, height);
        setFocusListener(this);

        setCornerRadius(4.f);

        reAlign();
    }

    public void setScrollBar(boolean enableScrollBar) {
        if (enableScrollBar) {
            add(scrollBar);
        } else {
            remove(scrollBar);
        }
        reAlign();
    }

    @Override
    public void setSize(float w, float h) {
        super.setSize(w, h);
        reAlign();
    }

    @Override
    public void reAlign() {
        super.reAlign();
        if (contains(scrollBar)) {
            editArea.setLocation(0, 0);
            editArea.setSize(getW() - SCROLLBAR_WIDTH, getH());
            scrollBar.setLocation(getW() - SCROLLBAR_WIDTH, 0);
            scrollBar.setSize(SCROLLBAR_WIDTH, getH());
        } else {
            editArea.setLocation(0, 0);
            editArea.setSize(getW(), getH());
        }

    }

    public int getCurCaretRow() {
        return curCaretRow;
    }

    public int getCurCaretCol() {
        return curCaretCol;
    }

    public boolean isShowCaretPos() {
        return showCaretPos;
    }

    public void setShowCaretPos(boolean showCaretPos) {
        this.showCaretPos = showCaretPos;
    }

    boolean boxIsFocus() {
        return parent.getFocus() == this;
    }

    @Override
    void onSetText(String text) {
        if (text != null) {
            setCaretIndex(text.length());
        } else {
            setCaretIndex(0);
        }
        resetSelect();
        editArea.area_detail = null;
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) System.out.println(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }

    public boolean setScroll(float p) {
        if (p > 1.f) {
            p = 1.f;
        }
        if (p < 0) {
            p = 0.f;
        }
        float tmp = this.scroll;
        this.scroll = p;
        return tmp != this.scroll;
    }

    @Override
    public void deleteSelectedText() {
        if (!isSelected()) {
            return;
        }
        setCaretIndex(getSelectBegin());
        deleteTextRange(getSelectBegin(), getSelectEnd());
        resetSelect();
    }

    @Override
    void resetSelect() {
        this.selFirst = this.selSecond = -1;
    }


    boolean isSelected() {
        if (this.selFirst != -1 && this.selSecond != -1) {
            return true;
        } else {
            return false;
        }
    }

    int getSelectBegin() {
        int select1 = 0;
        if (this.selFirst != -1 && this.selSecond != -1) {
            select1 = this.selFirst > this.selSecond ? this.selSecond : this.selFirst;
            return select1;
        }
        return -1;
    }

    int getSelectEnd() {
        int select2 = 0;
        if (this.selFirst != -1 && this.selSecond != -1) {
            select2 = this.selFirst < this.selSecond ? this.selSecond : this.selFirst;
            int len = textsb.length();
            if (select2 > len) select2 = len;
            return select2;
        }
        return -1;
    }

    @Override
    public String getSelectedText() {
        if (!isSelected()) {
            return null;
        }
        return textsb.substring(getSelectBegin(), getSelectEnd());
    }

    @Override
    public void insertTextAtCaret(String str) {
        insertTextByIndex(this.caretIndex, str);
        setCaretIndex(this.caretIndex + str.length());
    }

    @Override
    public void doSelectText() {
        if (this.caretIndex <= 0) {
            setCaretIndex(0);
            this.selFirst = 0;
        }
        int txtLen = textsb.length();
        if (this.caretIndex >= txtLen) {
            setCaretIndex(txtLen);
            this.selSecond = txtLen;
        }

        for (int i = this.caretIndex - 1; i >= 0 && i < txtLen; i--) {
            int ch = textsb.codePointAt(i);
            if (ch > 128 || (!Character.isLetterOrDigit(ch) && ch != '_') || i == 0) {
                this.selFirst = i + 1;
                break;
            }
        }
        for (int i = this.caretIndex + 1; i < txtLen; i++) {
            int ch = textsb.codePointAt(i);
            if (ch > 128 || (!Character.isLetterOrDigit(ch) && ch != '_') || i == txtLen - 1) {
                this.selSecond = i;
                break;
            }
        }
        setCaretIndex(this.selSecond);
        selectMode = true;
//        String s=textsb.substring(selectStart,selectEnd);
//        System.out.println("select :"+s);
    }

    @Override
    public void doSelectAll() {
        this.selFirst = 0;
        this.selSecond = textsb.length();
        selectMode = true;
    }

    /**
     * @return
     */
    public int getCaretIndex() {
        return this.caretIndex;
    }

    /**
     * @param caretIndex the caretIndex to set
     */
    public void setCaretIndex(int caretIndex) {
        if (caretIndex < 0) {
            caretIndex = 0;
        } else if (caretIndex > textsb.length()) {
            caretIndex = textsb.length();
        }
        this.caretIndex = caretIndex;
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        super.mouseButtonEvent(button, pressed, x, y);

        if (isInArea(x, y) || true) {//不再检测是否在区域内，需要应对鼠标移到框外时，对拖动选择仍然有效
            if (button == Glfw.GLFW_MOUSE_BUTTON_1) {
                if (pressed) {
                    int caret = editArea.getCaretIndexFromArea(x, y);
                    if (shift) {
                        if (caretIndex == caret) {
                            selFirst = -1;
                            selSecond = -1;
                        } else {
                            selFirst = caretIndex;
                            selSecond = caret;
                        }
                        caretIndex = caret;
                    } else if (caret >= 0) {
                        setCaretIndex(caret);
                        resetSelect();
                        selFirst = caret;
                        mouseDrag = true;
                    } else {
                        GToolkit.disposeEditMenu();
                    }
                } else {
                    mouseDrag = false;
                    if (selSecond == -1 || selFirst == selSecond) {
                        resetSelect();
                        GToolkit.disposeEditMenu();
                    } else {
                        selectMode = true;
                    }
                }
            } else if (button == Glfw.GLFW_MOUSE_BUTTON_2) {
                if (pressed) {

                } else {
                    GToolkit.callEditMenu(this, x, y);
                }
            }

        } else {
            if (mouseDrag) {//在区域外，且鼠标拖拽中，释放按键时，则完成选取
                if (!pressed) {
                    mouseDrag = false;
                }
            }
        }
    }

    @Override
    public void clickEvent(int button, int x, int y) {
        super.clickEvent(button, x, y);
        if (isInArea(x, y)) {
            int caret = editArea.getCaretIndexFromArea(x, y);
            if (caret >= 0) {
                setCaretIndex(caret);
                resetSelect();
                mouseDrag = false;
            }
            doSelectText();
        }
    }

    /**
     * 拖动处理，即便鼠标在框外，也会触发
     *
     * @param x
     * @param y
     */
    @Override
    public void cursorPosEvent(int x, int y) {
        super.cursorPosEvent(x, y);
        if (mouseDrag) {
            int caret = editArea.getCaretIndexFromArea(x, y);
            if (caret >= 0) {
                selSecond = caret;
            }
        }
    }

    /**
     * @param character
     */
    @Override
    public void characterEvent(char character) {
        if (this.getFocus() != editArea) {
            return;
        }
        deleteSelectedText();
        if (enable) {
            insertTextByIndex(caretIndex, character);
            setCaretIndex(getCaretIndex() + 1);
        }
    }

    @Override
    public void keyEventGlfw(int key, int scanCode, int action, int mods) {
        if (this.getFocus() != editArea) {
            return;
        }
        if (key == Glfw.GLFW_KEY_LEFT_SHIFT || key == Glfw.GLFW_KEY_RIGHT_SHIFT) {
            if (action == Glfw.GLFW_PRESS || action == Glfw.GLFW_REPEAT) {
                shift = true;
            } else {
                shift = false;
            }
        }
        if (action == Glfw.GLFW_PRESS || action == Glfw.GLFW_REPEAT) {
            //edit key
            if (enable) {
                switch (key) {
                    case Glfw.GLFW_KEY_BACKSPACE: {
                        if (isSelected()) {
                            deleteSelectedText();
                        } else {
                            if (textsb.length() > 0 && caretIndex > 0) {
                                setCaretIndex(caretIndex - 1);
                                deleteTextByIndex(caretIndex);
                            }
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_DELETE: {
                        if (textsb.length() > caretIndex) {
                            if (isSelected()) {
                                deleteSelectedText();
                            } else {
                                deleteTextByIndex(caretIndex);
                            }
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_ENTER: {
                        String txt = getText();
                        if (txt != null && txt.length() > 0) {
                            if (isSelected()) {
                                deleteSelectedText();
                            }
                            insertTextByIndex(caretIndex, '\n');
                            setCaretIndex(caretIndex + 1);
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_C: {
                        if ((mods & Glfw.GLFW_MOD_CONTROL) != 0) {
                            String s = getSelectedText();
                            Glfw.glfwSetClipboardString(winContext, s);
                            Glfm.glfmSetClipBoardContent(s);
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_V: {
                        if ((mods & Glfw.GLFW_MOD_CONTROL) != 0) {
                            String s = Glfw.glfwGetClipboardString(winContext);
                            if (s == null) s = Glfm.glfmGetClipBoardContent();
                            if (s != null) {
                                deleteSelectedText();
                                insertTextAtCaret(s);
                            }
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_A: {
                        if ((mods & Glfw.GLFW_MOD_CONTROL) != 0) {
                            doSelectAll();
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_X: {
                        if ((mods & Glfw.GLFW_MOD_CONTROL) != 0) {
                            deleteSelectedText();
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_Z: {
                        if ((mods & Glfw.GLFW_MOD_CONTROL) != 0 && (mods & Glfw.GLFW_MOD_SHIFT) != 0) {
                            redo();
                        } else if ((mods & Glfw.GLFW_MOD_CONTROL) != 0) {
                            undo();
                        }
                        break;
                    }
                }
            }

            //move key
            switch (key) {

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
                    int[] pos = editArea.getCaretPosFromArea();
                    if (pos != null) {
                        if (pos[1] < getY() + lineh[0] * 2) {
                            setScroll(scroll - lineh[0] / (editArea.totalTextHeight - editArea.showAreaHeight));
                        }
                        int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] - (int) lineh[0]);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }
                    } else {
                        for (int i = editArea.area_detail.length - 1; i >= 0; i--) {
                            if (editArea.area_detail[i] != null) {
                                int c = editArea.area_detail[i][EditArea.AREA_LINE_START_AT];
                                setCaretIndex(c);
                                break;
                            }
                        }
                    }
                    break;
                }
                case Glfw.GLFW_KEY_DOWN: {
                    int[] pos = editArea.getCaretPosFromArea();
                    if (pos != null) {
                        if (pos[1] > getY() + getH() - lineh[0] * 2) {
                            setScroll(scroll + lineh[0] / (editArea.totalTextHeight - editArea.showAreaHeight));
                        }
                        int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] + (int) lineh[0]);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }

                    } else {
                        for (int i = 0; i < editArea.area_detail.length; i++) {
                            if (editArea.area_detail[i] != null) {
                                int c = editArea.area_detail[i][EditArea.AREA_LINE_START_AT];
                                setCaretIndex(c);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        super.touchEvent(touchid, phase, x, y);

        if (touchid != Glfw.GLFW_MOUSE_BUTTON_1) return;
        if (isInArea(x, y)) {
            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan: {
                    int caret = editArea.getCaretIndexFromArea(x, y);
                    if (selectMode) {
                        selectAdjusted = false;
                        if (Math.abs(caret - selFirst) < Math.abs(caret - selSecond)) {
                            adjustSelStart = true;
                        } else {
                            adjustSelStart = false;
                        }
                    } else if (caret >= 0) {
                        setCaretIndex(caret);
                    }       //
                    if (task != null) {
                        task.cancel();
                        task = null;
                    }
                    break;
                }
                case Glfm.GLFMTouchPhaseEnded: {
                    if (selectMode) {
                        if (selFirst != -1) {
                            GToolkit.callEditMenu(this, x, y);
                        }
                    }
                    break;
                }
                case Glfm.GLFMTouchPhaseMoved: {
                    if (selectMode) {
                        int caret = editArea.getCaretIndexFromArea(x, y);
                        int mid = selFirst + (selSecond - selFirst) / 2;
                        if (adjustSelStart) {
                            if (caret < mid) {
                                selFirst = caret;
                            }
                        } else if (caret > mid) {
                            selSecond = caret;
                            setCaretIndex(selSecond);
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
     * @param str
     * @param mods
     */
    @Override
    public void characterEvent(String str, int mods) {

        if (isSelected()) {
            deleteSelectedText();
        }
        //System.out.println("input :" + (int) str.charAt(0));
        insertTextAtCaret(str);
    }

    @Override
    public void keyEventGlfm(int key, int action, int mods) {

        if (action == Glfm.GLFMKeyActionPressed || action == Glfm.GLFMKeyActionRepeated) {
            switch (key) {
                case Glfm.GLFMKeyBackspace: {
                    if (enable) {
                        if (textsb.length() > 0 && caretIndex > 0) {
                            if (isSelected()) {
                                deleteSelectedText();
                            } else {
                                setCaretIndex(caretIndex - 1);
                                deleteTextByIndex(caretIndex);
                            }
                        }
                    }
                    break;
                }
                case Glfm.GLFMKeyEnter: {
                    String txt = getText();
                    if (enable) {
                        if (txt != null && txt.length() > 0) {
                            if (isSelected()) {
                                deleteSelectedText();
                            }
                            setCaretIndex(caretIndex + 1);
                            insertTextByIndex(caretIndex, '\n');
                        }
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
                    int[] pos = editArea.getCaretPosFromArea();
                    setScroll(scroll - lineh[0] / (editArea.totalTextHeight - editArea.showAreaHeight));

                    if (pos != null) {
                        int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] - (int) lineh[0]);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }
                    }
                    break;
                }
                case Glfm.GLFMKeyDown: {
                    int[] pos = editArea.getCaretPosFromArea();
                    setScroll(scroll + lineh[0] / (editArea.totalTextHeight - editArea.showAreaHeight));
                    if (pos != null) {
                        int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] + (int) lineh[0]);
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
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {
        if (scroll >= 1 || scroll <= 0) {
            return false;
        }
        double dx = x2 - x1;
        final double dy = y2 - y1;
        float scrollDelta = 0;
        //System.out.println("inertia time: " + moveTime + " , count: " + maxMoveCount + " pos: x1,y1,x2,y2 = " + x1 + "," + y1 + "," + x2 + "," + y2);
        task = new TimerTask() {
            //惯性速度
            double speed = dy / (moveTime / inertiaPeriod);
            //阴力
            double resistance = speed / maxMoveCount;
            //
            int count = 0;

            @Override
            public void run() {
                try {
                    speed -= resistance;//速度和阴力抵消为0时,退出滑动
                    //System.out.println("count :" + count + "    inertia :" + speed + "    resistance :" + resistance);

                    float dh = editArea.getOutOfShowAreaHeight();
                    if (dh > 0) {
                        setScroll(scroll - (float) speed / dh);
                    }
                    GForm.flush();
                    if (count++ > maxMoveCount) {
                        try {
                            this.cancel();
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Timer timer = GForm.timer;
        if (timer != null) {
            timer.schedule(task, 0, inertiaPeriod);
        }
        return true;
    }

    @Override
    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        float dh = editArea.getOutOfShowAreaHeight();
        if (dh > 0) {
            return setScroll(scroll - (float) scrollY / dh);
        }
        return true;
    }

    @Override
    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        if (super.dragEvent(button, dx, dy, x, y)) {
            return true;
        }
        if (mouseDrag) {
            return true;
        }
        if (selectMode) {
            return false;
        }
        float dh = editArea.getOutOfShowAreaHeight();
        if (dh > 0) {
            setScroll(scroll - (float) dy / dh);
        }
        return true;
    }


    class EditArea extends GObject {
        //
        static final int AREA_CHAR_POS_START = 7;//额外增加slot数量
        static final int AREA_LINE_START_AT = 4;//字符串起点位置
        static final int AREA_LINE_END_AT = 5;//字符终点位置
        static final int AREA_ROW_NO = 6;//行号
        static final int AREA_X = LEFT;
        static final int AREA_Y = TOP;
        static final int AREA_W = WIDTH;
        static final int AREA_H = HEIGHT;

        protected int totalRows;//字符串总行数，动态计算出
        protected int showRows;//可显示行数

        protected short[][] area_detail;
        protected float totalTextHeight;//字符串总高度
        protected float showAreaHeight;//显示区域高度


        GTextBox tbox;

        protected EditArea(GTextBox tbox, GForm form, float left, float top, float width, float height) {
            super(form);
            this.tbox = tbox;
            setLocation(left, top);
            setSize(width, height);
        }


        /**
         * 返回指定位置所在字符串中的位置
         *
         * @param x
         * @param y
         * @return
         */
        int getCaretIndexFromArea(int x, int y) {
            if (editArea.area_detail != null) {
                //如果鼠标位置超出显示区域，进行校正
                if (x < getX()) {
                    x = (int) getX() + PAD;
                }
                if (y < getY()) {
                    y = (int) getY() + PAD;
                }
                if (x > getX() + getW()) {
                    x = (int) (getX() + getW()) - PAD;
                }
                if (y > getY() + getH()) {
                    y = (int) (getY() + getH()) - PAD;
                }

                //根据预存的屏幕内字符串位置，查找光标所在字符位置
                for (short[] detail : editArea.area_detail) {
                    if (detail != null) {
                        if (x >= detail[LEFT] && x <= detail[LEFT] + getW() && y >= detail[TOP] && y <= detail[TOP] + detail[HEIGHT]) {
                            for (int i = AREA_CHAR_POS_START, imax = detail.length; i < imax; i++) {
                                float x0 = detail[i];
                                float x1 = (i + 1 < imax) ? detail[i + 1] : x0;
                                if (x < (x0 + (x1 - x0) / 2)) {
                                    return detail[AREA_LINE_START_AT] + (i - AREA_CHAR_POS_START);
                                }
                            }
                            return detail[AREA_LINE_END_AT] + 1;
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
                        if (caretIndex >= detail[AREA_LINE_START_AT] && caretIndex <= detail[AREA_LINE_END_AT] + 1) {
                            int idx = caretIndex - detail[AREA_LINE_START_AT] + AREA_CHAR_POS_START;
                            int x = caretIndex <= detail[AREA_LINE_END_AT] ? detail[idx] : detail[detail.length - 1];
                            return new int[]{x + (int) lineh[0] / 2, detail[AREA_Y] + (int) lineh[0] / 2, detail[AREA_ROW_NO], i};
                        }
                    }
                    i++;
                }
            }
            return null;
        }

        float getOutOfShowAreaHeight() {
            float dh = totalTextHeight - showAreaHeight;
            return dh < 0 ? 0 : dh;
        }

        /**
         * @param vg
         * @return
         */
        @Override
        public boolean paint(long vg) {
            boolean ret = super.paint(vg);
            float x = getX();
            float y = getY();
            float w = getW();
            float h = getH();
            drawTextBox(vg, x, y, w, h);
            return ret;
        }

        void drawTextBox(long vg, float x, float y, float w, float h) {
            GToolkit.getStyle().drawEditBoxBase(vg, x, y, w, h, getCornerRadius());
            nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
            nvgFontFace(vg, GToolkit.getFontWord());
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);

            //字高
            nvgTextMetrics(vg, null, null, lineh);
            float lineH = lineh[0];

            float[] text_area = new float[]{x + PAD, y + PAD, w - PAD * 2, h - PAD * 2};
            float dx = text_area[LEFT];
            float dy = text_area[TOP];

            //sometime the field text_arr and area_detail may set as null by other thread when paint
            byte[] local_arr = tbox.text_arr;
            short[][] local_detail = this.area_detail;

            //画文本或提示
            if ((textsb == null || textsb.length() <= 0) && !boxIsFocus()) {
                nvgFillColor(vg, GToolkit.getStyle().getHintFontColor());
                nvgTextJni(vg, dx, dy, hint_arr, 0, hint_arr.length);
            } else {//编辑中
                int topShowRow = 0;//显示区域第一行的行号

                if (local_arr == null) {//文字被修改过
                    local_arr = toCstyleBytes(textsb.toString());
                    tbox.text_arr = local_arr;
                    showRows = Math.round(text_area[HEIGHT] / lineH) + 2;
                    showAreaHeight = text_area[HEIGHT];

                    //用于存放屏墓中各行的一些位置信息
                    local_detail = new short[showRows][];
                    this.area_detail = local_detail;

                    float[] bond = new float[4];
                    Nanovg.nvgTextBoxBoundsJni(vg, 0, 0, text_area[WIDTH], local_arr, 0, local_arr.length, bond);
                    totalRows = Math.round((bond[HEIGHT] - bond[TOP]) / lineH);
                    totalTextHeight = bond[HEIGHT];
                }
                //
                float dh = scroll * (totalTextHeight - showAreaHeight);
                dh = dh < 0 ? 0 : dh;
                dy -= dh;
                topShowRow = (int) (dh / lineH) - 1;
                //
                int posCount = 400;
                int rowCount = 5;
                long rowsHandle = nvgCreateNVGtextRow(rowCount);
                long glyphsHandle = nvgCreateNVGglyphPosition(posCount);
                int nrows, i, char_count;
                float caretx = 0;

                Nanovg.nvgScissor(vg, text_area[LEFT], text_area[TOP], text_area[WIDTH], text_area[HEIGHT]);
                Nanovg.nvgIntersectScissor(vg, tbox.getX(), tbox.getY(), tbox.getW(), tbox.getH());
                //需要恢复现场
                try {

                    //取选取的起始和终止位置

                    // The text break API can be used to fill a large buffer of rows,
                    // or to iterate over the text just few lines (or just one) at a time.
                    // The "next" variable of the last returned item tells where to continue.
                    //取UTF8字符串的内存地址，供NATIVE API调用
                    long ptr = GToolkit.getArrayDataPtr(local_arr);
                    int start = 0;
                    int end = local_arr.length - 1;

                    int char_at = 0;
                    int char_starti, char_endi;

                    int row_index = 0;

                    if (end - start == 0) {
                        GToolkit.drawCaret(vg, dx, dy, 2, lineH, false);
                    } else {//通过nvgTextBreakLinesJni进行断行

                        for (int li = 0; li < local_detail.length; li++) local_detail[li] = null;
                        /**
                         * nvgTextBreakLinesJni 断行后，包括起点和终点字符在本行
                         */
                        while ((nrows = nvgTextBreakLinesJni(vg, local_arr, start, end, text_area[WIDTH], rowsHandle, rowCount)) != 0) {

                            //循环绘制行
                            for (i = 0; i < nrows; i++) {
//                        if (area_row_index >= topShowRowLocal && area_row_index < topShowRowLocal + showRows) {
                                if (dy + lineH >= text_area[TOP] && dy < text_area[TOP] + text_area[HEIGHT]) {
                                    //取得第i 行的行宽
                                    float row_width = Nanovg.nvgNVGtextRow_width(rowsHandle, i);

                                    //返回 i 行的起始和结束位置
                                    int byte_starti = (int) (Nanovg.nvgNVGtextRow_start(rowsHandle, i) - ptr);
                                    int byte_endi = (int) (Nanovg.nvgNVGtextRow_end(rowsHandle, i) - ptr) + 1;

                                    //save herer
                                    if (char_at == 0) {
                                        //取得本行之前字符串长度
                                        CodePointBuilder preStrs = new CodePointBuilder(local_arr, 0, byte_starti, "utf-8");
                                        char_at = preStrs.length();

                                    }
                                    //把当前行从字节数组转成字符串
                                    CodePointBuilder curRowStrs;
                                    curRowStrs = new CodePointBuilder(local_arr, byte_starti, byte_endi - byte_starti, "utf-8");
                                    //计算字符串起止位置
                                    char_starti = char_at;
                                    char_endi = char_at + curRowStrs.length() - 1;

                                    caretx = dx;
                                    //取得i行的各个字符的具体位置，结果存入glyphs
                                    char_count = curRowStrs.length();
                                    /**
                                     *  nvgTextGlyphPositionsJni 包含起点字符，但不包含终点字符
                                     */
                                    int c_count = nvgTextGlyphPositionsJni(vg, dx, dy, local_arr, byte_starti, byte_endi, glyphsHandle, posCount);
                                    int curRow = row_index - topShowRow;

                                    if (curRow < 0 || curRow >= local_detail.length) {

                                    } else {
                                        //把这些信息存下来，用于在点击的时候找到点击了文本的哪个位置
                                        //前面存固定信息
                                        local_detail[curRow] = new short[AREA_CHAR_POS_START + char_count];
                                        local_detail[curRow][AREA_X] = (short) dx;
                                        local_detail[curRow][AREA_Y] = (short) dy;
                                        local_detail[curRow][AREA_W] = (short) text_area[WIDTH];
                                        local_detail[curRow][AREA_H] = (short) lineH;
                                        local_detail[curRow][AREA_LINE_START_AT] = (short) char_starti;
                                        local_detail[curRow][AREA_LINE_END_AT] = (short) char_endi;
                                        local_detail[curRow][AREA_ROW_NO] = (short) row_index;
                                        //后面把每个char的位置存下来
                                        for (int j = 0; j < char_count; j++) {
                                            //取第 j 个字符的X座标
                                            float x0 = nvgNVGglyphPosition_x(glyphsHandle, j);
                                            local_detail[curRow][AREA_CHAR_POS_START + j] = (short) x0;
                                        }

                                        //计算下一行开始
                                        char_at = char_at + curRowStrs.length();

                                        if (tbox.getFocus() == this) {
                                            boolean draw = false;
                                            if (caretIndex > char_starti && caretIndex <= char_endi) {
                                                caretx = local_detail[curRow][AREA_CHAR_POS_START + (caretIndex - char_starti)];
                                                draw = true;
//                                                if (caretIndex != 0 && caretIndex - char_starti == 0) {//光标移到行首时，只显示在上一行行尾
//                                                    draw = false;
//                                                }
                                            } else if (caretIndex == char_endi + 1) {
                                                caretx = dx + row_width;
                                                if (caretx >= text_area[LEFT] + text_area[WIDTH])
                                                    caretx = text_area[LEFT] + text_area[WIDTH] - 2;
                                                draw = true;
                                            } else if (caretIndex == 0 && char_starti == 0) {//特殊情况
                                                caretx = dx;
                                                draw = true;
                                            }
                                            if (draw) {
                                                curCaretRow = curRow + topShowRow;
                                                curCaretCol = caretIndex - char_starti;
                                                GToolkit.drawCaret(vg, caretx - 1, dy, 2, lineH, false);
                                            }
                                        }

                                        if (isSelected()) {
                                            int sel_start = getSelectBegin();
                                            int sel_end = getSelectEnd();
                                            float drawSelX = dx, drawSelW = row_width;
                                            //本行有选择起点
                                            if (sel_start > char_starti && sel_start <= char_endi) {
                                                int pos = sel_start - local_detail[curRow][AREA_LINE_START_AT];
                                                drawSelX = local_detail[curRow][AREA_CHAR_POS_START + pos];
                                                drawSelW = row_width - (drawSelX - local_detail[curRow][AREA_CHAR_POS_START]);
                                            }
                                            //本行有选择终点
                                            if (sel_end > char_starti && sel_end <= char_endi + 1) {
                                                int pos = sel_end - local_detail[curRow][AREA_LINE_START_AT];
                                                if (pos >= char_count) {//the last char
                                                    drawSelW = local_detail[curRow][AREA_CHAR_POS_START] + row_width - drawSelX;
                                                } else {
                                                    drawSelW = local_detail[curRow][AREA_CHAR_POS_START + pos] - drawSelX;
                                                }
                                            }

                                            if (sel_start >= char_endi + 1 || sel_end <= char_starti) {
                                                //此行没有起点和终点
                                            } else {
                                                //此行有起点或终点,或在起终点之间的整行
                                                GToolkit.drawRect(vg, drawSelX, dy, drawSelW, lineH, GToolkit.getStyle().getSelectedColor());
                                            }

                                        }
                                        nvgFillColor(vg, getColor());
                                        nvgTextJni(vg, dx, dy + 1, local_arr, byte_starti, byte_endi);
                                    }
                                }
                                dy += lineH;
                                row_index++;
                            }

                            long next = Nanovg.nvgNVGtextRow_next(rowsHandle, nrows - 1);
                            start = (int) (next - ptr);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Nanovg.nvgDeleteNVGtextRow(rowsHandle);
                Nanovg.nvgDeleteNVGglyphPosition(glyphsHandle);

            }
            if (showCaretPos) {
                String info = curCaretRow + ":" + curCaretCol;
                GToolkit.drawTextLine(vg, getX() + getW() * .5f, getY() + getH() - lineH, info, 12f, getColor(), NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            }
        }
    }
}
