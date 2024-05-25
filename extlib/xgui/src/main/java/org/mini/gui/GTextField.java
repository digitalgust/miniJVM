/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.nanovg.Nanovg;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GTextField extends GTextObject {

    static public final int BOX_STYLE_EDIT = 0;
    static public final int BOX_STYLE_SEARCH = 1;

    protected float[] reset_boundle;
    protected int text_max = 400;
    protected int boxStyle = BOX_STYLE_EDIT;
    //
    protected byte[] search_arr = toCstyleBytes(ICON_SEARCH);
    protected byte[] reset_arr = toCstyleBytes(ICON_CIRCLED_CROSS);
    //
    protected float[] lineh = {0};
    protected short[] text_pos;
    //
    protected int caretIndex;
    protected int selectStart = -1;//选取开始
    protected int selectEnd = -1;//选取结束
    boolean mouseDrag;

    protected boolean password = false;//是否密码字段

    protected boolean resetEnable = true;

    protected float wordShowOffsetX = 0.f;

    public GTextField(GForm form) {
        this(form, "", "", 0f, 0f, 1f, 1f);
    }

    public GTextField(GForm form, String text, String hint, float left, float top, float width, float height) {
        super(form);
        setText(text);
        setHint(hint);
        setLocation(left, top);
        setSize(width, height);
        reset_boundle = new float[]{left + width - height, top, height, height};
        setFocusListener(this);

        setCornerRadius(4.f);
    }


    @Override
    void onSetText(String text) {
        if (text != null) {
            caretIndex = text.length();
        } else {
            caretIndex = 0;
        }
        resetSelect();
    }

    public void setBoxStyle(int boxStyle) {
        this.boxStyle = boxStyle;
    }

    public void setMaxTextLength(int len) {
        text_max = len;
    }

    public void setPasswordMode(boolean pwd) {
        password = pwd;
    }

    public boolean isPasswordMode() {
        return password;
    }


    public void setResetEnable(boolean resetEnable) {
        this.resetEnable = resetEnable;
    }

    public boolean isResetEnable() {
        return resetEnable;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        int rx = (int) (x - parent.getInnerX());
        int ry = (int) (y - parent.getInnerY());
        if (isInArea(x, y)) {
            if (button == Glfw.GLFW_MOUSE_BUTTON_1) {

                if (pressed) {
                    int caret = getCaretIndex(x, y);
                    if (shift) {
                        if (caretIndex == caret) {
                            selectStart = -1;
                            selectEnd = -1;
                        } else {
                            selectStart = caretIndex;
                            selectEnd = caret;
                        }
                        caretIndex = caret;
                    } else if (caret >= 0) {
                        setCaretIndex(caret);
                        resetSelect();
                        selectStart = caret;
                        mouseDrag = true;
                    } else {
                        GToolkit.disposeEditMenu();
                    }
                } else {
                    mouseDrag = false;
                    if (selectEnd == -1 || selectStart == selectEnd) {
                        resetSelect();
                        GToolkit.disposeEditMenu();
                    }
                    if (isInBoundle(reset_boundle, rx, ry)) {
                        if (isResetEnable()) {
                            deleteAll();
                            resetSelect();
                        }
                        if (GToolkit.getEditMenu() != null) GToolkit.getEditMenu().dispose();
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

    /**
     * 鼠标拖拽,即便光标在区域外，也会触发
     *
     * @param x
     * @param y
     */
    @Override
    public void cursorPosEvent(int x, int y) {
        if (mouseDrag) {
            int caret = getCaretIndex(x, y);
            if (caret >= 0) {
                selectEnd = caret;
            }
            setCaretIndex(caret);
        }
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) System.out.println(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }

    @Override
    public void clickEvent(int button, int x, int y) {
        if (isInArea(x, y)) {
            doSelectAll();
        }
    }

    @Override
    public void keyEventGlfw(int key, int scanCode, int action, int mods) {
        if (parent.getFocus() != this) {
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
            if (enable) {
                switch (key) {
                    case Glfw.GLFW_KEY_BACKSPACE: {
                        int[] selectFromTo = getSelected();
                        if (selectFromTo != null) {
                            deleteSelectedText();
                        } else {
                            if (textsb.length() > 0 && caretIndex > 0) {
                                setCaretIndex(caretIndex - 1);
                                deleteTextByIndex(caretIndex);
                            }
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_ENTER: {
                        if (actionListener != null) {
                            doAction();
                        } else if (unionObj != null) {
                            unionObj.doAction();
                        }
                        break;
                    }
                    case Glfw.GLFW_KEY_DELETE: {
                        if (textsb.length() > caretIndex) {
                            int[] selectFromTo = getSelected();
                            if (selectFromTo != null) {
                                deleteSelectedText();
                            } else {
                                deleteTextByIndex(caretIndex);
                            }
                        }
                        break;
                    }
                }

                if ((mods & Glfw.GLFW_MOD_CONTROL) != 0) {
                    switch (key) {
                        case Glfw.GLFW_KEY_C: {
                            String s = getSelectedText();
                            Glfw.glfwSetClipboardString(winContext, s);
                            Glfm.glfmSetClipBoardContent(s);
                            break;
                        }
                        case Glfw.GLFW_KEY_V: {
                            String s = Glfw.glfwGetClipboardString(winContext);
                            if (s == null) s = Glfm.glfmGetClipBoardContent();
                            if (s != null) {
                                deleteSelectedText();
                                insertTextAtCaret(s);
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
                    setCaretIndex(0);
                    break;
                }
                case Glfw.GLFW_KEY_DOWN: {
                    setCaretIndex(textsb.length());
                    break;
                }
            }
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (touchid != Glfw.GLFW_MOUSE_BUTTON_1) return;
        int rx = (int) (x - parent.getInnerX());
        int ry = (int) (y - parent.getInnerY());
        if (isInBoundle(boundle, rx, ry)) {
            if (phase == Glfm.GLFMTouchPhaseEnded) {
                if (isInBoundle(reset_boundle, rx, ry)) {
                    if (isResetEnable()) {
                        deleteAll();
                        resetSelect();
                    }
                    if (GToolkit.getEditMenu() != null) GToolkit.getEditMenu().dispose();
                } else {
                    if (selectMode) {
                        resetSelect();
                        if (GToolkit.getEditMenu() != null) GToolkit.getEditMenu().dispose();
                    }
                    setCaretIndex(getCaretIndex(x, y));
                }
            }
        }
        super.touchEvent(touchid, phase, x, y);
    }

    @Override
    public void characterEvent(String str, int mods) {
        if (enable) {
            boolean containEnter = false;
            if (str.indexOf('\n') >= 0) {
                str = str.replace("\n", "");
                containEnter = true;
            }
            deleteSelectedText();
            insertTextByIndex(caretIndex, str);
            setCaretIndex(caretIndex + str.length());
            if (containEnter) {
                if (actionListener != null) {
                    doAction();
                } else if (unionObj != null) {
                    unionObj.doAction();
                }
            }
        }
    }

    /**
     * @param character
     */
    @Override
    public void characterEvent(char character) {
        if (enable) {
            if (character != '\n') {
                if (character != '\r' && textsb.length() < text_max) {
                    deleteSelectedText();
                    insertTextByIndex(caretIndex, character);
                    setCaretIndex(caretIndex + 1);
                }
            } else {
                if (actionListener != null) {
                    doAction();
                } else if (unionObj != null) {
                    unionObj.doAction();
                }
            }
        }
    }

    public int getPosXByIndex(int i) {
        if (text_pos != null) {
            if (i >= 0 && i < text_pos.length) {
                return text_pos[i];
            }
        }
        return -1;
    }

    public int getCaretIndex(int x, int y) {
        if (text_pos == null) {
            return textsb.length() - 1;
        }
        for (int j = 0; j < text_pos.length; j++) {
            //取第 j 个字符的X座标
            float x0 = text_pos[j];
            float x1 = j == text_pos.length - 1 ? x0 : text_pos[j + 1];
            if (x < x0 + ((x1 - x0) / 2)) {
                return j;
            }
        }
        return text_pos.length - 1;
    }

    public int getCaretIndex() {
        return caretIndex;
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

    int[] getSelected() {
        if (selectStart != -1 && selectEnd != -1) {
            int select1, select2;
            select1 = selectStart > selectEnd ? selectEnd : selectStart;
            select2 = selectStart < selectEnd ? selectEnd : selectStart;
            return new int[]{select1, select2};
        }
        return null;
    }

    @Override
    public void deleteSelectedText() {
        int[] sarr = getSelected();
        if (sarr == null || sarr[0] == -1 || sarr[1] == -1) {
            return;
        }
        setCaretIndex(sarr[0]);
        deleteTextRange(sarr[0], sarr[1]);
        resetSelect();
    }

    @Override
    void resetSelect() {
        selectStart = selectEnd = -1;
        selectMode = false;
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
        insertTextByIndex(caretIndex, str);
        setCaretIndex(caretIndex + textsb.length());
    }

    @Override
    public void doSelectText() {
        doSelectAll();
        selectMode = true;
    }

    @Override
    public void doSelectAll() {
        selectStart = 0;
        selectEnd = textsb.length();
        selectMode = true;
    }

    @Override
    public void deleteAll() {
        super.deleteAll();
        caretIndex = 0;
    }

    /**
     * @param vg
     * @return
     */
    @Override
    public boolean paint(long vg) {
        super.paint(vg);
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        byte[] bg;
        float FONT_WIDTH = GToolkit.getStyle().getIconFontWidth();
        float leftIcons = 0.5f;//图标占位宽度
        // Edit
        if (boxStyle == BOX_STYLE_SEARCH) {
            GToolkit.getStyle().drawFieldBoxBase(vg, x, y, w, h, h * .5f - 1f);
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());
            nvgFillColor(vg, GToolkit.getStyle().getHintFontColor());
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

            nvgTextJni(vg, x + FONT_WIDTH * 1.5f, y + h * 0.55f, search_arr, 0, search_arr.length);
            leftIcons = 2;
        } else {
            GToolkit.getStyle().drawEditBoxBase(vg, x, y, w, h, getCornerRadius());
        }

        if (isResetEnable()) {
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());
            nvgFillColor(vg, GToolkit.getStyle().getHintFontColor());
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgTextJni(vg, x + w - h * 0.55f, y + h * 0.55f, reset_arr, 0, reset_arr.length);
        }

        nvgFontSize(vg, GToolkit.getStyle().getTextFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());

        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        if (text_arr == null) {//文字被修改过
            if (password) {
                int len = textsb.length();
                text_arr = new byte[len + 1];
                for (int i = 0; i < len; i++) {
                    text_arr[i] = '*';
                }
            } else {
                text_arr = toCstyleBytes(textsb.toString());
            }
        }
        float wordx = x + FONT_WIDTH * leftIcons;
        float wordy = y + boundle[HEIGHT] * 0.5f;
        float text_show_area_x = wordx;
        float text_show_area_w = boundle[WIDTH] - FONT_WIDTH * (leftIcons + 2.5f);
        float text_width = Nanovg.nvgTextBoundsJni(vg, 0, 0, text_arr, 0, text_arr.length, null);
        if (parent.getFocus() != this && (textsb == null || textsb.length() == 0)) {
            if (hint_arr != null) {
                nvgFillColor(vg, GToolkit.getStyle().getHintFontColor());
                nvgTextJni(vg, wordx, wordy, hint_arr, 0, hint_arr.length);
            }
        } else {

            long glyphsHandle = nvgCreateNVGglyphPosition(text_max);
            if (parent.getFocus() == this) {
                nvgTextMetrics(vg, null, null, lineh);
            }

            try {
                if (text_width > text_show_area_w) {
                    wordx -= text_width - text_show_area_w;
                    wordx += wordShowOffsetX;
                }
                int char_count = nvgTextGlyphPositionsJni(vg, wordx, wordy, text_arr, 0, text_arr.length, glyphsHandle, text_max);

                text_pos = new short[char_count];
                float caretx = 0;
                float text_show_area_right = text_show_area_x + text_show_area_w;
                int leftShowCharIdx = 0, rightShowCharIdx = textsb.length();
                //确定每个char的位置
                for (int j = 0; j < char_count; j++) {
                    //取第 j 个字符的X座标
                    float x0 = nvgNVGglyphPosition_x(glyphsHandle, j);
                    text_pos[j] = (short) x0;
                    if (caretIndex == j) {
                        caretx = x0;
                    }
                    if (j - 1 >= 0 && text_pos[j - 1] <= text_show_area_x && x0 > text_show_area_x) {
                        leftShowCharIdx = j;
                    }
                    if (j - 1 >= 0 && text_pos[j - 1] <= text_show_area_right && x0 > text_show_area_right) {
                        rightShowCharIdx = j;
                    }
                }

                //本次计算,下次绘制生效
                if (Math.abs(caretx - text_show_area_x) < 50 && leftShowCharIdx > 0) {
                    wordShowOffsetX += 20;
                }
                if (Math.abs(caretx - text_show_area_right) < 50 && rightShowCharIdx < text_pos.length - 1) {
                    wordShowOffsetX -= 20;
                }

                if (caretx < text_show_area_x) {
                    wordShowOffsetX += text_show_area_x - caretx;
                } else if (caretx > text_show_area_right) {
                    wordShowOffsetX -= caretx - text_show_area_right;
                }

                if (parent.getFocus() == this) {
                    GToolkit.drawCaret(vg, caretx - 1, wordy - 0.5f * lineh[0], 2, lineh[0], false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Nanovg.nvgDeleteNVGglyphPosition(glyphsHandle);

            if (selectStart != -1 && selectEnd != -1) {
                float selStartX = getPosXByIndex(selectStart);
                float selEndX = getPosXByIndex(selectEnd);
                if (selStartX > text_show_area_x + text_show_area_w) {
                    selStartX = text_show_area_x + text_show_area_w;
                }
                //修正选 择框的长度，画的时候不会超出显示区域
                float selW = selEndX - selStartX;
                if (selStartX + selW > text_show_area_x + text_show_area_w) {
                    selW = text_show_area_x + text_show_area_w - selStartX;
                }

                GToolkit.drawRect(vg, selStartX, wordy - lineh[0] * .5f, selW, lineh[0], GToolkit.getStyle().getSelectedColor());

            }
            nvgFillColor(vg, getColor());
            Nanovg.nvgScissor(vg, text_show_area_x, y, text_show_area_w, h);
            Nanovg.nvgIntersectScissor(vg, parent.getX(), parent.getY(), parent.getW(), parent.getH());
            nvgTextJni(vg, wordx, wordy, text_arr, 0, text_arr.length);
        }
        return true;
    }

}
