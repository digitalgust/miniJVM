/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.event.GFocusChangeListener;
import org.mini.util.CodePointBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.mini.glwrap.GLUtil.toCstyleBytes;

/**
 * @author Gust
 */
public abstract class GTextObject extends GContainer implements GFocusChangeListener {
    //for keyboard union action
    GObject defaultUnionObj = new GObject(form) {
    };
    protected GObject unionObj = defaultUnionObj;//if this object exists, the keyboard not disappear

    //for undo redo
    static class UserAction {
        static final int ADD = 0, DEL = 1;

        UserAction(int mod, String t, int c) {
            addOrDel = mod;
            txt = t;
            caretIndex = c;
        }

        int addOrDel;
        String txt;
        int caretIndex;
    }

    protected List<UserAction> undoQ = new ArrayList();
    protected List<UserAction> redoQ = new ArrayList();
    static final int MAXUNDO = 15;


    protected String hint;
    protected byte[] hint_arr;
    protected CodePointBuilder textsb = new CodePointBuilder();
    protected byte[] text_arr;


    protected boolean selectMode = false;
    protected boolean editable = true;
    boolean shift = false;


    protected GTextObject(GForm form) {
        super(form);
    }

    public void setHint(String hint) {
        this.hint = hint;
        hint_arr = toCstyleBytes(hint);
    }

    public String getHint() {
        return hint;
    }

    abstract void onSetText(String text);

    public void setText(String text) {
        this.textsb.setLength(0);
        if (text != null) {
            this.textsb.append(text);
        }
        onSetText(text);
        text_arr = null;
        doStateChanged(this);
    }

    public String getText() {
        return textsb.toString();
    }

    public int getTextSize() {
        return textsb.length();
    }

    public void insertTextByIndex(int index, int ch) {
        if(!editable)return;
        textsb.insertCodePoint(index, ch);
        text_arr = null;
        doStateChanged(this);
        StringBuilder undo = new StringBuilder();
        undo.appendCodePoint(ch);
        putInUndo(UserAction.ADD, undo.toString(), index);
    }

    public void insertTextByIndex(int index, String str) {
        if(!editable)return;
        textsb.insert(index, str);
        text_arr = null;
        doStateChanged(this);
        putInUndo(UserAction.ADD, str, index);
    }

    public void deleteTextByIndex(int index) {
        if(!editable)return;
        int ch = textsb.codePointAt(index);
        textsb.deleteCodePointAt(index);
        text_arr = null;
        doStateChanged(this);
        StringBuilder undo = new StringBuilder();
        undo.appendCodePoint(ch);
        putInUndo(UserAction.DEL, undo.toString(), index);
    }

    public void deleteTextRange(int start, int end) {
        if(!editable)return;
        String str = textsb.substring(start, end);
        textsb.delete(start, end);
        text_arr = null;
        doStateChanged(this);
        putInUndo(UserAction.DEL, str, start);
    }


    public void deleteAll() {
        if(!editable)return;
        String str = textsb.toString();
        textsb.setLength(0);
        text_arr = null;
        doStateChanged(this);
        putInUndo(UserAction.DEL, str, 0);
    }

    abstract public String getSelectedText();

    abstract public void deleteSelectedText();

    abstract public void insertTextAtCaret(String str);

    abstract void resetSelect();

    abstract public void setCaretIndex(int caretIndex);

    abstract public int getCaretIndex();

    public void doSelectText() {

    }

    public void doSelectAll() {

    }

    public void doCopyClipBoard() {
        String s = getSelectedText();
        if (s != null) {
            Glfm.glfmSetClipBoardContent(s);
            Glfw.glfwSetClipboardString(GCallBack.getInstance().getDisplay(), s);
        }
    }

    public void doCut() {
        if(!editable)return;
        doCopyClipBoard();
        deleteSelectedText();
    }

    public void doPasteClipBoard() {
        if(!editable)return;
        deleteSelectedText();
        String s = Glfm.glfmGetClipBoardContent();
        if (s == null) {
            s = Glfw.glfwGetClipboardString(GCallBack.getInstance().getDisplay());
        }
        if (s != null) {
            insertTextAtCaret(s);
        }
    }

    long winContext;

    @Override
    public void focusGot(GObject go) {
        if (editable) {
            GForm.showKeyboard(this);
        }
    }

    @Override
    public void focusLost(GObject newgo) {
        if (newgo != unionObj && newgo != this) {
            GForm.hideKeyboard(form);
        }
        GToolkit.disposeEditMenu();
        touched = false;
    }

    boolean touched;

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (touchid != Glfw.GLFW_MOUSE_BUTTON_1) return;
        if (isInArea(x, y)) {
            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan: {
                    touched = true;
                    if (editable && !Glfm.glfmIsKeyboardVisible(GCallBack.getInstance().getDisplay())) {
                        GForm.showKeyboard(this);
                    }
                    break;
                }
                case Glfm.GLFMTouchPhaseEnded: {
                    if (touched) {
                        touched = false;
                    }
                    break;
                }
            }
        }
        super.touchEvent(touchid, phase, x, y);
    }

    @Override
    public void longTouchedEvent(int x, int y) {
        GToolkit.callEditMenu(this, x, y);
        //System.out.println("long toucched");
        super.longTouchedEvent(x, y);
    }


    @Override
    public void keyEventGlfm(int key, int action, int mods) {
        super.keyEventGlfm(key,action,mods);
        int glfwAction = 0;
        if (action == Glfm.GLFMKeyActionPressed) {
            glfwAction = Glfw.GLFW_PRESS;
        } else if (action == Glfm.GLFMKeyActionRepeated) {
            glfwAction = Glfw.GLFW_REPEAT;
        } else if (action == Glfm.GLFMKeyActionReleased) {
            glfwAction = Glfw.GLFW_RELEASE;
        }
        int glfwKey = Glfw.GLFW_KEY_UNKNOWN;
        switch (key) {
            case Glfm.GLFMKeyBackspace: {
                glfwKey = Glfw.GLFW_KEY_BACKSPACE;
                break;
            }
            case Glfm.GLFMKeyEnter: {
                glfwKey = Glfw.GLFW_KEY_ENTER;
                break;
            } //move key
            case Glfm.GLFMKeyLeft: {
                glfwKey = Glfw.GLFW_KEY_LEFT;
                break;
            }
            case Glfm.GLFMKeyRight: {
                glfwKey = Glfw.GLFW_KEY_RIGHT;
                break;
            }
            case Glfm.GLFMKeyUp: {
                glfwKey = Glfw.GLFW_KEY_UP;
                break;
            }
            case Glfm.GLFMKeyDown: {
                glfwKey = Glfw.GLFW_KEY_DOWN;
                break;
            }
        }
        int glfwMod = 0;
        if ((mods & Glfm.GLFMKeyModifierCtrl) != 0) {
            glfwMod = glfwMod | Glfw.GLFW_MOD_CONTROL;
        } else if ((mods & Glfm.GLFMKeyModifierShift) != 0) {
            glfwMod = glfwMod | Glfw.GLFW_MOD_SHIFT;
        } else if ((mods & Glfm.GLFMKeyModifierAlt) != 0) {
            glfwMod = glfwMod | Glfw.GLFW_MOD_ALT;
        }
        keyEventGlfw(glfwKey, -1, glfwAction, glfwMod);
    }

    static public boolean isEditMenuShown() {
        if (GToolkit.getEditMenu() == null) {
            return false;
        }
        return GToolkit.getEditMenu().getParent() != null;
    }

    /**
     * @return the unionObj
     */
    public GObject getUnionObj() {
        if (unionObj == defaultUnionObj) {
            return null;
        }
        return unionObj;
    }

    /**
     * @param unionObj the unionObj to set
     */
    public void setUnionObj(GObject unionObj) {
        if (unionObj == null) {
            this.unionObj = defaultUnionObj;
        } else {
            this.unionObj = unionObj;
        }
    }

    /**
     * @return the editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * @param editable the editable to set
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void putInUndo(int mod, String t, int caretIndex) {
        undoQ.add(new UserAction(mod, t, caretIndex));
        if (undoQ.size() > MAXUNDO) {
            undoQ.remove(0);
        }
    }

    public void putInUndo(UserAction te) {
        undoQ.add(te);
        if (undoQ.size() > MAXUNDO) {
            undoQ.remove(0);
        }
    }

    public UserAction getUndo() {
        if (!undoQ.isEmpty()) {
            UserAction te = undoQ.remove(undoQ.size() - 1);
            return te;
        }
        return null;
    }


    public void putInRedo(int mod, String t, int caretIndex) {
        redoQ.add(new UserAction(mod, t, caretIndex));
        if (redoQ.size() > MAXUNDO) {
            redoQ.remove(0);
        }
    }

    public void putInRedo(UserAction te) {
        redoQ.add(te);
        if (redoQ.size() > MAXUNDO) {
            redoQ.remove(0);
        }
    }

    public UserAction getRedo() {
        if (!redoQ.isEmpty()) {
            UserAction te = redoQ.remove(redoQ.size() - 1);
            return te;
        }
        return null;
    }

    public void undo() {
        if(!editable)return;
        UserAction action = getUndo();
        if (action != null) {
            if (action.addOrDel == UserAction.ADD) {
                textsb.delete(action.caretIndex, action.caretIndex + action.txt.codePointCount(0, action.txt.length()));
            } else {
                textsb.insert(action.caretIndex, action.txt);
            }
            setCaretIndex(action.caretIndex);
            putInRedo(action);
            text_arr = null;
        }
    }

    public void redo() {
        if(!editable)return;
        UserAction action = getRedo();
        if (action != null) {
            if (action.addOrDel == UserAction.ADD) {
                textsb.insert(action.caretIndex, action.txt);
            } else {
                textsb.delete(action.caretIndex, action.caretIndex + action.txt.codePointCount(0, action.txt.length()));
            }
            setCaretIndex(action.caretIndex);
            putInUndo(action);
            text_arr = null;
        }
    }

    public void setScrollBar(boolean enable) {

    }
}
