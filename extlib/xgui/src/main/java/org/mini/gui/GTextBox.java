/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GCmd;
import org.mini.json.JsonParser;
import org.mini.json.JsonPrinter;
import org.mini.nanovg.Nanovg;
import org.mini.util.CodePointBuilder;
import org.mini.util.SysLog;

import java.util.ArrayList;
import java.util.List;

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
    static final float LINE_SCALE = 1.2f;

    protected int curCaretRow;//以回车为换行符的行数
    protected int curCaretCol;//以回车为换行符的列数

    protected int curCaretShowRow;//以显示行为行数
    protected int curCaretShowCol;//以显示行为列数
    protected float caretX, caretY;
    protected boolean showCaretPos = false;

    protected boolean mouseDrag;
    protected int caretIndex;//光标在字符串中的位置
    protected boolean adjustSelStart = true;//是修改选择起点还是终点
    protected boolean selectAdjusted;//在选取状态下,如果点击了,但是没有修改位置,取消选取状态
    protected float scroll = 0;//0-1 区间,描述窗口滚动条件位置, 滚动符0-1分别对应文本顶部超出显示区域的高度百分比
    //
    protected float[] lineh = {0};

    protected int pendingMoveToIndex = -1;
    protected boolean pendingMoveTo = false;
    //pendingMoveTo 抗抖动与对齐: 记录滚动方向.
    //1) 若方向反转(振荡)则强制停止 —— area_detail 范围有限, 光标行在 detail 边缘时
    //   firstFullyVisibleCS 可能帧间翻转导致反复滚动.
    //2) 决定停止时的对齐: 下滚(+1)时光标行对齐到显示区末行, 上滚(-1)时对齐到首行,
    //   避免连续 RIGHT/LEFT 时光标行在第一行/第二行交替跳动.
    protected int pendingMoveDir = 0;   //-1上滚, +1下滚, 0未滚

    /**
     * StyleRun defines a styled segment of text.
     */
    public static class StyleRun {
        /**
         * The start index of the segment, it is CodePoint index.
         */
        public int start;
        /**
         * The length of the segment, it is CodePoint length.
         */
        public int length;
        public float[] color;

        public StyleRun() {
        }

        public StyleRun(int start, int length, float[] color) {
            this.start = start;
            this.length = length;
            this.color = color;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public float[] getColor() {
            return color;
        }

        public void setColor(float[] color) {
            this.color = color;
        }

    }

    protected java.util.List<StyleRun> styles = new java.util.ArrayList<>();

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
        editArea.setFocusListener(this);

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

    public void setFontSize(float fontSize) {
        super.setFontSize(fontSize);
        editArea.setFontSize(fontSize);
    }

    public void setColor(float[] color) {
        super.setColor(color);
        editArea.setColor(color);
    }

    public void setBgColor(float[] bgcolor) {
        super.setBgColor(bgcolor);
        editArea.setBgColor(bgcolor);
    }

    public int getCurCaretRow() {
        return curCaretRow;
    }

    public int getCurCaretCol() {
        return curCaretCol;
    }

    public int getCurCaretShowRow() {
        return curCaretShowRow;
    }

    public int getCurCaretShowCol() {
        return curCaretShowCol;
    }

    public float getCaretX() {
        return caretX;
    }

    public float getCaretY() {
        return caretY;
    }

    public boolean isShowCaretPos() {
        return showCaretPos;
    }

    public void setShowCaretPos(boolean showCaretPos) {
        this.showCaretPos = showCaretPos;
    }

    boolean boxIsFocus() {
        return parent.getCurrent() == this;
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
        if (flyable) SysLog.info(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }

    public boolean setScroll(float p) {
        //NaN/Infinity 会让下面的比较失效从而漏进来,这里显式拦截
        //(inertiaEvent/键盘滚动/空文本除法等都可能产生脏值,统一在此堵住)
        if (Float.isNaN(p) || Float.isInfinite(p)) {
            return false;
        }
        if (p > 1.f) {
            p = 1.f;
        }
        if (p < 0) {
            p = 0.f;
        }
        float tmp = this.scroll;
        this.scroll = p;
        //用 floatToIntBits 比较,避免 scroll 之前被污染成 NaN 时 (NaN != x 恒真) 误报"变化"
        boolean changed = Float.floatToIntBits(tmp) != Float.floatToIntBits(this.scroll);
        if (changed) {
            scrollBar.setPos(this.scroll);
        }
        //resetSelect();
        return changed;
    }

    public float getScroll() {
        return scroll;
    }

    /**
     * 显示文本的指定位置
     *
     * @param charIndex
     */
    public void moveScreenToIndex(int charIndex) {
        if (charIndex < 0) {
            charIndex = 0;
        }
        if (charIndex > textsb.length()) {
            charIndex = textsb.length();
        }
//        int[] pos = editArea.getCaretPosFromArea();
//        if (pos != null) {
//            float moveOffset = charIndex < caretIndex ? (-lineh[0]) : lineh[0];
//            if (pos[1] < getY() + lineh[0] * 2) {//超出屏幕时先滚屏
//                setScroll(scroll + moveOffset / (editArea.totalTextHeight - editArea.showAreaHeight));
//            }
//        }
        pendingMoveToIndex = charIndex;
        pendingMoveTo = true;
        pendingMoveDir = 0;  //重置抗抖方向记录
    }

    /**
     * 左右方向键只在光标已经离开可视区时再触发自动滚动。
     * 两行高窗口里如果每次水平移动都要求"整行完整显示", pendingMoveTo 会在首/末可见行之间来回校正,
     * 表现为光标每按一次键就在两行间反复切换。
     */
    void ensureCaretVisibleForHorizontalMove() {
        int[] pos = editArea.getCaretPosFromArea();
        if (pos == null) {
            moveScreenToIndex(caretIndex);
            return;
        }
        float lineH = lineh[0] > 0 ? lineh[0] : getFontSize() * LINE_SCALE;
        float textTop = editArea.getY() + PAD;
        float textBottom = editArea.getY() + editArea.getH() - PAD * 2;
        float caretTop = pos[1] - lineH * 0.5f;
        float caretBottom = pos[1] + lineH * 0.5f;
        // 左右移动只要求当前行仍与可视区相交即可;
        // 若仅有1-2像素被裁掉, 继续强制对齐会让两行高窗口在上下两个位置之间来回跳动.
        if (caretBottom <= textTop || caretTop >= textBottom) {
            moveScreenToIndex(caretIndex);
        }
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
        this.selectStart = this.selectEnd = -1;
    }


    public boolean isSelected() {
        if (this.selectStart != -1 && this.selectEnd != -1) {
            return true;
        } else {
            return false;
        }
    }

    public int getSelectBegin() {
        int select1 = 0;
        if (this.selectStart != -1 && this.selectEnd != -1) {
            select1 = this.selectStart > this.selectEnd ? this.selectEnd : this.selectStart;
            return select1;
        }
        return -1;
    }

    public int getSelectEnd() {
        int select2 = 0;
        if (this.selectStart != -1 && this.selectEnd != -1) {
            select2 = this.selectStart < this.selectEnd ? this.selectEnd : this.selectStart;
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
    }

    @Override
    public void doSelectText() {
        if (this.caretIndex <= 0) {
            setCaretIndex(0);
            this.selectStart = 0;
        }
        int txtLen = textsb.length();
        if (this.caretIndex >= txtLen) {
            setCaretIndex(txtLen);
            this.selectEnd = txtLen;
        }

        for (int i = this.caretIndex - 1; i >= 0 && i < txtLen; i--) {
            int ch = textsb.codePointAt(i);
            if (ch > 128 || (!Character.isLetterOrDigit(ch) && ch != '_') || i == 0) {
                this.selectStart = i + 1;
                break;
            }
        }
        for (int i = this.caretIndex + 1; i < txtLen; i++) {
            int ch = textsb.codePointAt(i);
            if (ch > 128 || (!Character.isLetterOrDigit(ch) && ch != '_') || i == txtLen - 1) {
                this.selectEnd = i;
                break;
            }
        }
        setCaretIndex(this.selectEnd);
        selectMode = true;
//        String s=textsb.substring(selectStart,selectEnd);
//        System.out.println("select :"+s);
    }

    @Override
    public void doSelectAll() {
        this.selectStart = 0;
        this.selectEnd = textsb.length();
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
    @Override
    public void setCaretIndex(int caretIndex) {
        if (caretIndex < 0) {
            caretIndex = 0;
        } else if (caretIndex > textsb.length()) {
            caretIndex = textsb.length();
        }
        this.caretIndex = caretIndex;

        curCaretRow = 1;
        int lastIndex = 0;
        for (int i = 0, imax = textsb.length(); i < caretIndex; i++) {
            if (textsb.codePointAt(i) == '\n') {
                curCaretRow++;
                lastIndex = i + 1;
            }
        }

        curCaretCol = caretIndex - lastIndex + 1;

        super.setCaretIndex(caretIndex);
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        super.mouseButtonEvent(button, pressed, x, y);
        if (scrollBar.isInArea(x, y)) return;//不再检测是否在滚动条区域内，否则会出现拖动滚动条滑块时，文字被选中的问题

        if (isInArea(x, y) || true) {//不再检测是否在区域内，需要应对鼠标移到框外时，对拖动选择仍然有效
            if (button == Glfw.GLFW_MOUSE_BUTTON_1) {
                if (pressed) {
                    int caret = editArea.getCaretIndexFromArea(x, y);
                    if (shift) {
                        if (caretIndex == caret) {
                            selectStart = -1;
                            selectEnd = -1;
                        } else {
                            selectStart = caretIndex;
                            selectEnd = caret;
                        }
                        setCaretIndex(caret);
                    } else if (caret >= 0) {
                        setCaretIndex(caret);
                        resetSelect();
                        selectStart = caret;
                        mouseDrag = true;
                    } else {
                        GToolkit.hideEditMenu();
                    }
                    moveScreenToIndex(caret);
                } else {
                    mouseDrag = false;
                    if (selectEnd == -1 || selectStart == selectEnd) {
                        resetSelect();
                        GToolkit.hideEditMenu();
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
                selectEnd = caret;
            }
            if (y < getY()) {
                setScroll(getScroll() - editArea.ratioPerLine * 0.5f);
            }
            if (y > getY() + getH()) {
                setScroll(getScroll() + editArea.ratioPerLine * 0.5f);
            }
        }
    }

    /**
     * @param character
     */
    @Override
    public void characterEvent(char character) {
        if (this.getCurrent() != editArea) {
            return;
        }
        deleteSelectedText();
        if (visible && enable) {
            insertTextByIndex(caretIndex, character);
        }
    }

    @Override
    public void keyEventGlfw(int key, int scanCode, int action, int mods) {
        if (this.getCurrent() != editArea) {
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
            if (visible && enable) {
                switch (key) {
                    case Glfw.GLFW_KEY_BACKSPACE: {
                        if (isSelected()) {
                            deleteSelectedText();
                        } else {
                            if (textsb.length() > 0 && caretIndex > 0) {
                                moveScreenToIndex(caretIndex - 1);
                                setCaretIndex(caretIndex - 1);
                                deleteTextByIndex(caretIndex);
                            }
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_DELETE: {
                        if (isSelected()) {
                            deleteSelectedText();
                        } else if (textsb.length() > caretIndex) {
                            deleteTextByIndex(caretIndex);
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
                        ensureCaretVisibleForHorizontalMove();
                    }
                    break;
                }
                case Glfw.GLFW_KEY_RIGHT: {
                    if (textsb.length() > caretIndex) {
                        setCaretIndex(caretIndex + 1);
                        ensureCaretVisibleForHorizontalMove();
                    }
                    break;
                }
                case Glfw.GLFW_KEY_UP: {
                    int[] pos = editArea.getCaretPosFromArea();
                    if (pos != null) {
                        if (pos[1] < getY() + lineh[0]) {
                            float denom = (editArea.totalTextHeight - editArea.showAreaHeight);
                            if (denom > 0f) {
                                setScroll(scroll - lineh[0] / denom);
                            }
                        } else {
                            //pos[1]是当前行中心, 减1.0行高正好落到上一行中心(远离行边界),
                            //避免用1.5行高时落点贴在上一行下边界, 因行距微小差异偶发跳两行
                            //strict=true: 上一行若不在已记录行范围内(不可见), 返回-1, 改为滚动
                            int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] - (int) (lineh[0]), true);//定位到上一行中央
                            if (cart >= 0) {
                                setCaretIndex(cart);
                            } else {
                                //上一行不在已记录范围(不可见), 滚动一行让它进入可见区
                                float denom = (editArea.totalTextHeight - editArea.showAreaHeight);
                                if (scroll > 0f && denom > 0f) {
                                    setScroll(scroll - lineh[0] / denom);
                                }
                            }
                        }
                    } else {
                        //pos==null: 当前光标位置不在任何可见行. 原来会跳到屏幕最后一行, 行为突兀.
                        //改为: 仅在未滚到顶时向上滚动, 不移动光标
                        float denom = (editArea.totalTextHeight - editArea.showAreaHeight);
                        if (scroll > 0f && denom > 0f) {
                            setScroll(scroll - lineh[0] / denom);
                        }
                    }
                    break;
                }
                case Glfw.GLFW_KEY_DOWN: {
                    int[] pos = editArea.getCaretPosFromArea();
                    if (pos != null) {
                        if (pos[1] > getY() + getH() - lineh[0]) {
                            float denom = (editArea.totalTextHeight - editArea.showAreaHeight);
                            if (denom > 0f) {
                                setScroll(scroll + lineh[0] / denom);
                            }
                        } else {
                            //pos[1]是当前行中心, 加1.0行高正好落到下一行中心(远离行边界),
                            //避免用1.5行高时落点贴在下一行上边界, 因行距微小差异偶发跳两行
                            //strict=true: 下一行若不在已记录行(area_detail)范围内(不可见), 返回-1,
                            //避免原来"落点超出maxY -> 返回textsb.length()"导致光标从1900直接跳到1930末尾
                            int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] + (int) (lineh[0]), true);
                            if (cart >= 0 && cart <= textsb.length()) {
                                //允许 cart == textsb.length(): 末行(尤其末尾空行)的光标合法位置就是文本末尾,
                                //原来用 cart < textsb.length() 会挡掉末行空行, 导致按DOWN到不了最后一行
                                setCaretIndex(cart);
                            } else {
                                //cart == -1: 下一行不在已记录范围(area_detail)内.
                                //两种情况: (a)下一行真实存在但不可见 -> 滚动让它进入可见区;
                                //(b)当前已在文本最后一行(\n所在行), 下一行是末尾空行(永远不进area_detail) ->
                                //   光标应到文本末尾 textsb.length(). 用 pos[3](当前行detail索引)判断.
                                int[] curDetail = (pos[3] >= 0 && pos[3] < editArea.area_detail.length)
                                        ? editArea.area_detail[pos[3]] : null;
                                boolean atLastLine = curDetail != null
                                        && curDetail[EditArea.AREA_LINE_END_AT] + 1 >= textsb.length();
                                if (atLastLine) {
                                    setCaretIndex(textsb.length());
                                } else {
                                    float denom = (editArea.totalTextHeight - editArea.showAreaHeight);
                                    if (scroll < 1.0f && denom > 0f) {
                                        setScroll(scroll + lineh[0] / denom);
                                    }
                                }
                            }
                        }
                    } else {
                        //pos==null: 当前光标位置不在任何可见行(例如光标在末尾空行且该行未进area_detail).
                        //原来会跳到屏幕第一行, 行为突兀. 改为: 仅在未滚到底时向下滚动, 不移动光标,
                        //避免光标突然消失或跳到屏幕顶部
                        float denom = (editArea.totalTextHeight - editArea.showAreaHeight);
                        if (scroll < 1.0f && denom > 0f) {
                            setScroll(scroll + lineh[0] / denom);
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
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan: {
                if (editArea.isInArea(x, y)) {
                    int caret = editArea.getCaretIndexFromArea(x, y);
                    if (selectMode) {
                        selectAdjusted = false;
                        if (Math.abs(caret - selectStart) < Math.abs(caret - selectEnd)) {
                            adjustSelStart = true;
                        } else {
                            adjustSelStart = false;
                        }
                    } else if (caret >= 0) {
                        setCaretIndex(caret);
                        moveScreenToIndex(caret);
                    }       //
                    if (inertiaCmd != null) {
                        inertiaCmd = null;
                    }
                }
                break;
            }
            case Glfm.GLFMTouchPhaseEnded: {
                if (editArea.isInArea(x, y)) {
                    if (selectMode) {
                        if (selectStart != -1) {
                            GToolkit.callEditMenu(this, x, y);
                        }
                    }
                }
                break;
            }
            case Glfm.GLFMTouchPhaseMoved: {
                if (selectMode) {
                    int caret = editArea.getCaretIndexFromArea(x, y);
                    int mid = selectStart + (selectEnd - selectStart) / 2;
                    if (adjustSelStart) {
                        if (caret < mid) {
                            selectStart = caret;
                        }
                    } else if (caret > mid) {
                        selectEnd = caret;
                        setCaretIndex(selectEnd);
                    }
                    selectAdjusted = true;

                    if (y < getY()) {
                        setScroll(getScroll() - editArea.ratioPerLine * 0.5f);
                    }
                    if (y > getY() + getH()) {
                        setScroll(getScroll() + editArea.ratioPerLine * 0.5f);
                    }
                } else {
//                        int caret = editArea.getCaretIndexFromArea(x, y);
//                        setCaretIndex(caret);
                }
                break;
            }
            default:
                break;
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
                    if (visible && enable) {
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
                    if (visible && enable) {
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
                        ensureCaretVisibleForHorizontalMove();
                    }
                    break;
                }
                case Glfm.GLFMKeyRight: {
                    if (textsb.length() > caretIndex) {
                        setCaretIndex(caretIndex + 1);
                        ensureCaretVisibleForHorizontalMove();
                    }
                    break;
                }
                case Glfm.GLFMKeyUp: {
                    int[] pos = editArea.getCaretPosFromArea();
                    float denomUp = (editArea.totalTextHeight - editArea.showAreaHeight);
                    if (denomUp > 0f) {
                        setScroll(scroll - lineh[0] / denomUp);
                    }

                    if (pos != null) {
                        //strict=true: 上一行不可见时返回-1, 不跳到非法位置(与桌面glfw版一致)
                        int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] - (int) lineh[0], true);
                        if (cart >= 0) {
                            setCaretIndex(cart);
                        }
                    }
                    break;
                }
                case Glfm.GLFMKeyDown: {
                    int[] pos = editArea.getCaretPosFromArea();
                    float denomDown = (editArea.totalTextHeight - editArea.showAreaHeight);
                    if (denomDown > 0f) {
                        setScroll(scroll + lineh[0] / denomDown);
                    }
                    if (pos != null) {
                        //strict=true: 下一行不可见时返回-1, 不跳到文本末尾
                        int cart = editArea.getCaretIndexFromArea(pos[0], pos[1] + (int) lineh[0], true);
                        if (cart >= 0 && cart <= textsb.length()) {
                            setCaretIndex(cart);
                        }
                    }
                    break;
                }
            }
        }
    }

    //总共做多少次操作
    long maxMoveCount = 120;
    //惯性任务

    GCmd inertiaCmd;

    @Override
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {
        if (scrollBar.isInArea(x2, y2)) {
            return true;
        }
        if (scroll >= 1 || scroll <= 0) {
            return false;
        }
        final double dy = y2 - y1;

        //---- 公共参数兜底,防止除零/NaN 进入下面的速度计算 ----
        //getFps()在启动首秒、严重卡顿或后台返回时可能返回0,会导致 inertiaPeriod=Infinity -> speed=Infinity -> 减成 NaN 永久卡死
        float rawFps = GCallBack.getInstance().getFps();
        if (!(rawFps > 0) || rawFps < 1) {   // rawFps<=0 或 NaN
            rawFps = GCallBack.FPS_DEFAULT;
        }
        final double inertiaPeriod = 1000d / rawFps;
        //moveTime(cost)在触屏事件批量/延迟投递时可能为0,这里兜底为1ms
        final long mvTime = moveTime <= 0 ? 1 : moveTime;
        final double perSlice = mvTime / inertiaPeriod;
        if (!(perSlice > 0)) {   // perSlice<=0 或 NaN,放弃本次惯性
            return false;
        }
        final double preSpeed = dy / perSlice;
        if (Double.isInfinite(preSpeed) || Double.isNaN(preSpeed)) {
            return false;   // 拦截非法初速度,避免污染 scroll
        }

        //System.out.println("inertia time: " + moveTime + " , count: " + maxMoveCount + " pos: x1,y1,x2,y2 = " + x1 + "," + y1 + "," + x2 + "," + y2);
        Runnable task = new Runnable() {
            //惯性速度
            double speed = preSpeed;
            //阴力
            final double resistance = speed / maxMoveCount;
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
                    //speed 出现 NaN/Infinity 时立即终止,避免脏值继续污染 scroll
                    if (++count > maxMoveCount || Double.isNaN(speed) || Double.isInfinite(speed)) {
                        inertiaCmd = null;
                    }
                    GForm.addCmd(inertiaCmd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        inertiaCmd = new GCmd(task);
        GForm.addCmd(inertiaCmd);
        return true;
    }

    @Override
    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        float dh = editArea.getOutOfShowAreaHeight();
        if (dh > 0) {
            return setScroll(scroll - scrollY / dh);
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

        protected int[][] area_detail;
        protected float totalTextHeight;//字符串总高度
        protected float showAreaHeight;//显示区域高度
        protected float ratioPerLine;//每行占比


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
            return getCaretIndexFromArea(x, y, false);
        }

        /**
         * 查找指定屏幕坐标对应的字符位置.
         *
         * @param strict false(默认, 鼠标点击场景): 落点超出已记录行(下方空白)时返回 textsb.length(),
         *               即点击文本下方空白区会把光标放到文本末尾.
         *               true(方向键导航场景): 落点超出已记录行时返回 -1 表示"目标行不可见",
         *               调用方据此改为滚动而非跳到文本末尾, 避免按一次下方向键光标从1900直接跳到1930.
         */
        int getCaretIndexFromArea(int x, int y, boolean strict) {
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            //记录Y最大(最下方)的那一行的末尾字符位置, 供"点在已记录行下方"时定位到该行末尾,
            //而不是直接跳到文本末尾(textsb.length()), 避免点显示区底部空白时光标跳到1930
            int lastDetailEnd = -1;
            if (editArea.area_detail != null) {
                //如果鼠标位置超出显示区域，进行校正(仅点击场景).
                //strict(方向键)场景不做校正: 需保留原始坐标, 以便下面准确判断
                //"目标行是否超出已记录行范围(不可见)", 校正会把区外点拉回区内导致误判
                if (!strict) {
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
                }

                //根据预存的屏幕内字符串位置，查找光标所在字符位置
                for (int[] detail : editArea.area_detail) {
                    if (detail != null) {
                        minY = Math.min(minY, detail[TOP]);
                        int rowBottom = detail[TOP] + detail[HEIGHT];
                        if (rowBottom > maxY) {
                            maxY = rowBottom;
                            lastDetailEnd = detail[AREA_LINE_END_AT];
                        }
                        if (x >= detail[LEFT] && x <= detail[LEFT] + getW() && y >= detail[TOP] && y <= detail[TOP] + detail[HEIGHT]) {
                            for (int i = AREA_CHAR_POS_START, imax = detail.length; i < imax; i++) {
                                float x0 = detail[i];
                                float x1 = (i + 1 < imax) ? detail[i + 1] : x0;
                                if (x < (x0 + (x1 - x0) / 2)) {
                                    return detail[AREA_LINE_START_AT] + (i - AREA_CHAR_POS_START);
                                }
                            }
                            int ci = detail[AREA_LINE_END_AT] + 1;
                            if (ci > textsb.length()) {
                                ci = textsb.length();
                            }
                            if (textsb.codePointAt(ci - 1) == '\n') {//取得离光标最近的字符位置，如果光标在行尾，则返回光标所在行尾字符位置
                                return detail[AREA_LINE_END_AT];
                            }
                            return ci;
                        }
                    }
                }
            }
            if (y > maxY) {
                //落点在已记录行(可见行)下方:
                // strict(方向键): 返回-1表示目标行不可见, 由调用方滚动
                // 点击: 定位到最下方那一行的末尾字符位置, 而非直接跳到文本末尾.
                //   这样点显示区底部空白时, 光标停在最后一行, 不会跳到1930再触发pendingMoveTo无限滚动.
                //   只有当没有任何可见行(lastDetailEnd<0)时, 才fallback到文本末尾.
                if (strict) return -1;
                return lastDetailEnd >= 0 ? lastDetailEnd : textsb.length();
            }
            return strict ? -1 : caretIndex;
        }


        /**
         * 返回光标当前所在的x,y坐标,及行号,数组下标
         *
         * @return
         */
        int[] getCaretPosFromArea() {
            if (area_detail != null) {
                int i = 0;
                for (int[] detail : area_detail) {
                    if (detail != null) {
                        if (caretIndex == detail[AREA_LINE_START_AT]) {//当光标处于上一行尾，且是换行符，在这里来处理
                            return new int[]{detail[AREA_X], detail[AREA_Y] + (int) lineh[0] / 2, detail[AREA_ROW_NO], i};
                        } else if (caretIndex >= detail[AREA_LINE_START_AT] && caretIndex <= detail[AREA_LINE_END_AT] + 1) {
                            int codePrev = caretIndex > 0 ? textsb.codePointAt(caretIndex - 1) : 0;
                            if (codePrev == '\n') {//这里要注意，有种特殊情况是行尾没有回车符，但会自动换行，这时不会进入到此分支里面
                                //如果光标正好在换行符处，则认为光标在下一行行首，跳到下一行处理
                                //但若是文本末尾(caretIndex == textsb.length()), "下一行"是末尾空行,
                                //它不在area_detail里, 循环结束也找不到, 最终返回null导致方向键失效.
                                //此处特判: 末尾空行的光标位置 = 当前行(\n所在行)的下一行, 直接返回,
                                //让UP/DOWN能基于此位置正常定位(UP回到\n行, DOWN保持在末尾)
                                if (caretIndex == textsb.length()) {
                                    return new int[]{detail[AREA_X], detail[AREA_Y] + (int) lineh[0] + (int) lineh[0] / 2, detail[AREA_ROW_NO] + 1, i};
                                }
                            } else {
                                int idx = caretIndex - detail[AREA_LINE_START_AT] + AREA_CHAR_POS_START;
                                int x = caretIndex <= detail[AREA_LINE_END_AT] ? detail[idx] : detail[detail.length - 1];
                                return new int[]{x, detail[AREA_Y] + (int) lineh[0] / 2, detail[AREA_ROW_NO], i};
                            }
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
            nvgFontSize(vg, getFontSize());
            nvgFontFace(vg, GToolkit.getFontWord());
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);

            //字高
            nvgTextLineHeight(vg, LINE_SCALE);
            nvgTextMetrics(vg, null, null, lineh);
            float fontH = lineh[0];
            float lineH = fontH * LINE_SCALE;
            lineh[0] = lineH;
            float caretX = 0;
            float caretY = 0;

            float[] text_area = new float[]{x + PAD, y + PAD, w - PAD * 3, h - PAD * 3};
            float dx = text_area[LEFT];
            float dy = text_area[TOP];

            //sometime the field text_arr and area_detail may set as null by other thread when paint
            byte[] local_arr = tbox.text_arr;
            int[][] local_detail = this.area_detail;

            //画文本或提示
            if ((textsb == null || textsb.length() <= 0) && !boxIsFocus()) {
                nvgFillColor(vg, GToolkit.getStyle().getHintFontColor());
                nvgTextJni(vg, dx, dy, hint_arr, 0, hint_arr.length);
            } else {//编辑中
                int topShowRow = 0;//显示区域第一行的行号

                if (local_arr == null || local_detail == null) {//文字被修改过
                    local_arr = toCstyleBytes(textsb.toString());
                    tbox.text_arr = local_arr;
                    showRows = Math.round(text_area[HEIGHT] / lineH) + 2;
                    showAreaHeight = text_area[HEIGHT];

                    //用于存放屏墓中各行的一些位置信息
                    local_detail = new int[showRows][];
                    this.area_detail = local_detail;

                    float[] bond = new float[4];
                    Nanovg.nvgTextBoxBoundsJni(vg, 0, 0, text_area[WIDTH], local_arr, 0, local_arr.length, bond);
                    //注意: NanoVG 的 bounds[1]=miny(顶部y,通常为负), bounds[3]=maxy(底部y).
                    //真实包围盒高度 = maxy - miny = bond[HEIGHT] - bond[TOP], 不能直接用 bond[HEIGHT](=maxy)
                    float bondHeight = bond[HEIGHT] - bond[TOP];
                    //当文本以换行符结尾时(末行是空行), nvgTextBreakLines/nvgTextBoxBounds 不会为该空行
                    //计入高度, 但绘制循环仍会画出这个空行(光标可停在该行). 这会导致 scroll=1 时 dh 偏小,
                    //末行底部超出显示区被裁(只显示一半). 这里检测末尾换行符, 补上一个行高.
                    int txtLen = textsb.length();
                    if (txtLen > 0 && textsb.codePointAt(txtLen - 1) == '\n') {
                        bondHeight += lineH;
                    }
                    totalRows = Math.round(bondHeight / lineH);
                    totalTextHeight = bondHeight;
                    //防 totalTextHeight<=0 (空文本/退化字体) 导致 ratioPerLine=Infinity, 后续经 setScroll 扩散
                    ratioPerLine = totalTextHeight > 0 ? lineH / totalTextHeight : 0;
                }
                //
                float dh = scroll * (totalTextHeight - showAreaHeight);
                dh = dh < 0 ? 0 : dh;
                dy -= dh;
                //防 lineH<=0 (退化字体) 导致 dh/lineH=Infinity -> (int)Infinity 越界
                topShowRow = lineH > 0 ? (int) (dh / lineH) - 1 : -1;
                //
                int posCount = 400;
                int rowCount = 5;
                long rowsHandle = nvgCreateNVGtextRow(rowCount);
                long glyphsHandle = nvgCreateNVGglyphPosition(posCount);
                int nrows, i, char_count;

//                Nanovg.nvgScissor(vg, text_area[LEFT], text_area[TOP], text_area[WIDTH], text_area[HEIGHT]);
//                Nanovg.nvgIntersectScissor(vg, tbox.getX(), tbox.getY(), tbox.getW(), tbox.getH());
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
                    int firstCharOnScreen = -1;// 显示区域第一行的第一个字符的索引(相交即算, 用于点击是否可见判断)
                    int lastCharOnScreen = -1;// 显示区域最后一行的最后一个字符的索引(相交即算)
                    //完整可见的首/末行字符索引: 行完全在显示区内(dy>=区顶 且 dy+lineH<=区底).
                    //pendingMoveTo 滚动用这个判断停止时机, 保证光标行完整显示在显示区内,
                    //避免左右键上滚时光标行只露一部分(顶部偏出)累积偏差.
                    //额外记录首行末字符/末行首字符, 用于精确判断光标行是否就是首行/末行(对齐停止).
                    int firstFullyVisibleCS = -1;   //首行首字符
                    int firstFullyVisibleCE = -1;   //首行末字符
                    int lastFullyVisibleRowStart = -1; //末行首字符
                    int lastFullyVisibleCS = -1;    //末行末字符

                    int row_index = 0;

                    if (end - start == 0) {
                        GToolkit.drawCaret(vg, dx, dy, 2, fontH, false);
                        GTextBox.this.caretX = dx;
                        GTextBox.this.caretY = dy + lineH + PAD;
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
                                    if (byte_endi > local_arr.length) {
                                        byte_endi = local_arr.length;
                                    }

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

                                    caretX = dx;
                                    caretY = dy;
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
                                        local_detail[curRow] = new int[AREA_CHAR_POS_START + char_count];
                                        local_detail[curRow][AREA_X] = (int) dx;
                                        local_detail[curRow][AREA_Y] = (int) dy;
                                        local_detail[curRow][AREA_W] = (int) text_area[WIDTH];
                                        local_detail[curRow][AREA_H] = (int) lineH;
                                        local_detail[curRow][AREA_LINE_START_AT] = (int) char_starti;
                                        local_detail[curRow][AREA_LINE_END_AT] = (int) char_endi;
                                        local_detail[curRow][AREA_ROW_NO] = (int) row_index;
                                        //firstCharOnScreen/lastCharOnScreen 的判定与绘制条件(行与显示区相交即画)保持一致,
                                        //避免最后一行底部不完整时 lastCharOnScreen 不更新, 导致 pendingMoveTo 误判
                                        //"目标行不可见"而反复半屏滚动直到文本末尾
                                        if (firstCharOnScreen == -1 && dy + lineH > text_area[TOP]) {//首行底部进入显示区即算显示
                                            firstCharOnScreen = char_starti;
                                        }
                                        if (dy < text_area[TOP] + text_area[HEIGHT]) {//末行顶部在显示区内即算显示(循环持续更新到最后可见行)
                                            lastCharOnScreen = char_endi;
                                        }
                                        //完整可见(行完全在显示区内): 供 pendingMoveTo 判断光标行是否完整显示,
                                        //避免上滚停止时光标行顶部偏出显示区
                                        if (firstFullyVisibleCS == -1 && dy >= text_area[TOP]) {
                                            firstFullyVisibleCS = char_starti;
                                            firstFullyVisibleCE = char_endi;
                                        }
                                        if (dy + lineH <= text_area[TOP] + text_area[HEIGHT]) {
                                            lastFullyVisibleCS = char_endi;
                                            lastFullyVisibleRowStart = char_starti;
                                        }
                                        //后面把每个char的位置存下来
                                        for (int j = 0; j < char_count; j++) {
                                            //取第 j 个字符的X座标
                                            float x0 = nvgNVGglyphPosition_x(glyphsHandle, j);
                                            local_detail[curRow][AREA_CHAR_POS_START + j] = (int) x0;
                                        }

                                        //计算下一行开始
                                        char_at = char_at + curRowStrs.length();


                                        boolean draw = false;
                                        boolean jumpWhenReturn = false;
//                                            int code = textsb.codePointAt(caretIndex);
//                                            int codeNext = caretIndex + 1 >= textsb.length() ? 0 : textsb.codePointAt(caretIndex + 1);

                                        if (caretIndex > char_starti && caretIndex <= char_endi) {
                                            caretX = local_detail[curRow][AREA_CHAR_POS_START + (caretIndex - char_starti)];
                                            draw = true;
                                        } else if (caretIndex == char_endi + 1) {
                                            int codePrev = caretIndex - 1 < 0 ? 0 : textsb.codePointAt(caretIndex - 1);
                                            if (codePrev == '\n') {//如果光标index前一个字符是换行符，则把光标放在下一行的开头
                                                caretX = dx + 1;
                                                caretY += lineH;
                                                jumpWhenReturn = true;
                                            } else {
//                                                    caretX = local_detail[curRow][AREA_X] + local_detail[curRow][AREA_W];
                                                caretX = dx + row_width;
//                                                    if (caretX >= text_area[LEFT] + text_area[WIDTH]) {
//                                                        caretX = text_area[LEFT] + text_area[WIDTH];
//                                                    }
                                            }
                                            draw = true;
                                        } else if (caretIndex == 0 && char_starti == 0) {//特殊情况
                                            caretX = dx + 1;
                                            draw = true;
                                        }
                                        if (draw) {
                                            curCaretShowRow = curRow + topShowRow + (jumpWhenReturn ? 1 : 0);
                                            curCaretShowCol = jumpWhenReturn ? 0 : (caretIndex - char_starti);
                                            if (isEditable() && isEnable()) {
                                                if (tbox.getCurrent() == this) {
                                                    GToolkit.drawCaret(vg, caretX - 1, caretY, 2, fontH, false);
                                                } else {
                                                    GToolkit.drawCaret(vg, caretX - 1, caretY, 2, fontH, false, GColorSelector.BLUE_HALF);
                                                }
                                            } else {
                                                GToolkit.drawCaret(vg, caretX - 1, caretY, 2, fontH, false, GColorSelector.GRAY_HALF);
                                            }
                                            caretY += lineH + PAD;
                                            GTextBox.this.caretX = caretX;
                                            GTextBox.this.caretY = caretY;
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
                                                GToolkit.drawRect(vg, drawSelX, dy, drawSelW, fontH, GToolkit.getStyle().getSelectedColor());
                                            }

                                        }
                                        //nvgFillColor(vg, GTextBox.this.getColor());
                                        //nvgTextJni(vg, dx, dy + 1, local_arr, byte_starti, byte_endi);

                                        // ================== RICH TEXT DRAWING LOGIC ==================
                                        int scanCharIndex = char_starti;
                                        if (char_starti <= char_endi) { // protection for empty lines
                                            while (scanCharIndex <= char_endi) {
                                                // 1. Find style and end of segment
                                                StyleRun currentStyle = tbox.findStyleAt(scanCharIndex);
                                                int segmentEndCharIndex = char_endi;

                                                if (currentStyle != null) {
                                                    segmentEndCharIndex = Math.min(char_endi, currentStyle.start + currentStyle.length - 1);
                                                }

                                                // check for next style starts
                                                for (StyleRun run : tbox.styles) {
                                                    if (run.start > scanCharIndex && run.start - 1 < segmentEndCharIndex) {
                                                        segmentEndCharIndex = run.start - 1;
                                                    }
                                                }

                                                // 2. Get segment text bytes
                                                int subEnd = segmentEndCharIndex + 1;
                                                if (subEnd > tbox.textsb.length()) {
                                                    subEnd = tbox.textsb.length();
                                                }
                                                String segmentStr = tbox.textsb.substring(scanCharIndex, subEnd);
                                                byte[] segmentBytes = toCstyleBytes(segmentStr);
                                                //byte[] segmentBytes = tbox.textsb.toUtf8Bytes(scanCharIndex, subEnd);//需要minijvm_rt升级

                                                // 3. Set color
                                                if (currentStyle != null && currentStyle.color != null) {
                                                    nvgFillColor(vg, currentStyle.color);
                                                } else {
                                                    nvgFillColor(vg, GTextBox.this.getColor());
                                                }

                                                // 4. Find start x pos from pre-calculated glyph positions
                                                float startX = dx;
                                                int glyphIndex = scanCharIndex - char_starti;
                                                if (glyphIndex > 0 && (AREA_CHAR_POS_START + glyphIndex) < local_detail[curRow].length) {
                                                    startX = local_detail[curRow][AREA_CHAR_POS_START + glyphIndex];
                                                }

                                                // 5. Draw and update position
                                                nvgTextJni(vg, startX, dy + 1, segmentBytes, 0, segmentBytes.length);

                                                // 6. Move to next segment
                                                scanCharIndex = segmentEndCharIndex + 1;
                                            }
                                        }
                                        // ========================================================
                                    }
                                }
                                dy += lineH;
                                row_index++;
                            }

                            long next = Nanovg.nvgNVGtextRow_next(rowsHandle, nrows - 1);
                            start = (int) (next - ptr);
                        }

                        //计算moveToIndex，滚动到需要显示的行
                        if (pendingMoveTo && firstCharOnScreen >= 0 && lastCharOnScreen >= 0) {
                            //每次只滚一行, 与方向键滚动逻辑统一, 行为可预测且不会冲过头导致抖动.
                            //(原来用 max(半屏,一行), 宽区域时半屏很大, 到边界时滚半屏易与"光标行可见性"
                            // 判断冲突导致文本上下跳动)
                            //delta 除以 denom(=totalTextHeight-showAreaHeight) 而非 totalTextHeight,
                            //因为 scroll->offset 换算是 offset=scroll*denom, 这样每帧 offset 精确滚一行 lineH
                            float stepPixels = lineH;
                            float scrollDenom = totalTextHeight - showAreaHeight;
                            float delta = scrollDenom > 0 ? stepPixels / scrollDenom : 0;
                            //停止时机用"完整可见"的首/末行(行完全在显示区内), 而非"相交可见".
                            //这样光标行会完整显示, 避免左右键上滚时光标行只露一部分(顶部偏出)累积偏差.
                            //极窄显示区(不足一行)时完整可见变量为-1, 退回到相交可见变量.
                            int stopFirst = firstFullyVisibleCS >= 0 ? firstFullyVisibleCS : firstCharOnScreen;
                            int stopLast = lastFullyVisibleCS >= 0 ? lastFullyVisibleCS : lastCharOnScreen;
                            //光标行是否就是首行/末行(用首行末字符/末行首字符精确判断)
                            boolean caretAtFirstRow = firstFullyVisibleCE >= 0
                                    && pendingMoveToIndex >= firstFullyVisibleCS
                                    && pendingMoveToIndex <= firstFullyVisibleCE;
                            boolean caretAtLastRow = lastFullyVisibleRowStart >= 0
                                    && pendingMoveToIndex >= lastFullyVisibleRowStart
                                    && pendingMoveToIndex <= lastFullyVisibleCS;
                            //启动判断(还未开始滚, dir==0): 光标在可见区外才启动, 行内不滚.
                            //进行中(dir!=0): 下滚必须滚到光标行==末行才停, 上滚必须滚到光标行==首行才停,
                            //避免连续 RIGHT 时光标行在第一行/第二行交替跳动.
                            boolean needScrollDown = pendingMoveToIndex > stopLast && getScroll() < 1f
                                    && (pendingMoveDir == 0 || !caretAtLastRow);
                            boolean needScrollUp = pendingMoveToIndex < stopFirst && getScroll() > 0f
                                    && (pendingMoveDir == 0 || !caretAtFirstRow);
                            if (needScrollDown) {
                                //抗抖动: 若上一帧是反向滚动(上滚), 说明在边界振荡, 强制停止
                                if (pendingMoveDir < 0) {
                                    pendingMoveToIndex = -1;
                                    pendingMoveTo = false;
                                    pendingMoveDir = 0;
                                } else {
                                    setScroll(getScroll() + delta);
                                    pendingMoveDir = 1;
                                    flushNow();
                                }
                            } else if (needScrollUp) {
                                if (pendingMoveDir > 0) {
                                    pendingMoveToIndex = -1;
                                    pendingMoveTo = false;
                                    pendingMoveDir = 0;
                                } else {
                                    setScroll(getScroll() - delta);
                                    pendingMoveDir = -1;
                                    flushNow();
                                }
                            } else { //结束滚动
                                pendingMoveToIndex = -1;
                                pendingMoveTo = false;
                                pendingMoveDir = 0;
                            }
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
                GToolkit.drawTextLine(vg, getX() + getW() - 10f, getY() + getH() - lineH, info, 12f, getColor(), NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
            }
        }
    }

    /**
     * Add a style to a range of text.
     * Note: For now, this just adds the style. Overlapping styles are not merged.
     * The last added style will have priority in rendering.
     *
     * @param start  start character index
     * @param length character length
     * @param color  text color
     */
    public void addStyle(int start, int length, float[] color) {
        styles.add(new StyleRun(start, length, color));
        editArea.area_detail = null; //force redraw
    }

    /**
     * Clear all styles.
     */
    public void clearStyles() {
        styles.clear();
        editArea.area_detail = null; //force redraw
    }

    /**
     * Find the style for a given character index.
     * Iterates backwards so the last added style takes precedence.
     *
     * @param charIndex index
     * @return StyleRun or null
     */
    protected StyleRun findStyleAt(int charIndex) {
        for (int i = styles.size() - 1; i >= 0; i--) {
            StyleRun run = styles.get(i);
            if (charIndex >= run.start && charIndex < run.start + run.length) {
                return run;
            }
        }
        return null;
    }

    public List<StyleRun> getStyles() {
        return styles;
    }


    /**
     * find the same color to  merge
     *
     * @param srs
     */
    private void mergeStyleRunColor(List<GTextBox.StyleRun> srs) {
        for (int i = 0; i < srs.size() - 1; i++) {
            float[] sr = srs.get(i).getColor();
            for (int j = 0; j < i; j++) {
                float[] sr2 = srs.get(j).getColor();
                if (sr[0] == sr2[0] && sr[1] == sr2[1] && sr[2] == sr2[2] && sr[3] == sr2[3]) {
                    srs.get(j).color = sr;
                }
            }
        }
    }

    public void setStyles(List<StyleRun> styles) {
        if (styles == null) {
            return;
        }
        this.styles = styles;
        mergeStyleRunColor(styles);//合并相同颜色
        editArea.area_detail = null; //force redraw
    }

    public void setStyleJson(String json) {
        if (json == null) {
            return;
        }
        JsonParser<List<StyleRun>> jp = new JsonParser();
        List<StyleRun> list = jp.deserial(json, List.class, StyleRun.class.getClassLoader(), "java.util.List<org.mini.gui.GTextBox$StyleRun>");
        setStyles(list);
    }


    public String getStyleJson() {
        JsonPrinter printer = new JsonPrinter();
        return printer.serial(styles);
    }
}
