/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.event.GFocusChangeListener;
import org.mini.gui.event.GStateChangeListener;

import java.util.ArrayList;
import java.util.List;

import static org.mini.nanovg.Gutil.toUtf8;

/**
 * @author Gust
 */
public abstract class GTextObject extends GObject implements GFocusChangeListener {
    //for keyboard union action
    static GObject defaultUnionObj = new GObject() {
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
    protected StringBuilder textsb = new StringBuilder();
    protected byte[] text_arr;

    protected GStateChangeListener stateChangeListener;


    protected boolean selectMode = false;


    public void setHint(String hint) {
        this.hint = hint;
        hint_arr = toUtf8(hint);
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
        doStateChange();
    }

    public String getText() {
        return textsb.toString();
    }

    public void insertTextByIndex(int index, char ch) {
        textsb.insert(index, ch);
        text_arr = null;
        doStateChange();
        putInUndo(UserAction.ADD, "" + ch, index);
    }

    public void insertTextByIndex(int index, String str) {
        textsb.insert(index, str);
        text_arr = null;
        doStateChange();
        putInUndo(UserAction.ADD, str, index);
    }

    public void deleteTextByIndex(int index) {
        char ch = textsb.charAt(index);
        textsb.deleteCharAt(index);
        text_arr = null;
        doStateChange();
        putInUndo(UserAction.DEL, "" + ch, index);
    }

    public void deleteTextRange(int start, int end) {
        String str = textsb.substring(start, end);
        textsb.delete(start, end);
        text_arr = null;
        doStateChange();
        putInUndo(UserAction.DEL, str, start);
    }


    public void deleteAll() {
        String str = textsb.toString();
        textsb.setLength(0);
        text_arr = null;
        doStateChange();
        putInUndo(UserAction.DEL, str, 0);
    }

    abstract public String getSelectedText();

    abstract public void deleteSelectedText();

    abstract public void insertTextAtCaret(String str);

    abstract void resetSelect();

    abstract void setCaretIndex(int caretIndex);

    public void doSelectText() {

    }

    public void doSelectAll() {

    }

    public void doCopyClipBoard() {
        String s = getSelectedText();
        if (s != null) {
            Glfm.glfmSetClipBoardContent(s);
            Glfw.glfwSetClipboardString(getForm().getWinContext(), s);
        }
    }

    public void doCut() {
        doCopyClipBoard();
        deleteSelectedText();
    }

    public void doPasteClipBoard() {
        deleteSelectedText();
        String s = Glfm.glfmGetClipBoardContent();
        if (s == null) {
            s = Glfw.glfwGetClipboardString(getForm().getWinContext());
        }
        if (s != null) {
            insertTextAtCaret(s);
        }
    }

    long winContext;

    @Override
    public void focusGot(GObject go) {
        if (enable) {
            GForm.showKeyboard();
        }
    }

    @Override
    public void focusLost(GObject newgo) {
        if (newgo != unionObj && newgo != this) {
            GForm.hideKeyboard();
        }
        GToolkit.disposeEditMenu();
        touched = false;
    }

    boolean touched;

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan: {
                    touched = true;
                    break;
                }
                case Glfm.GLFMTouchPhaseEnded: {
                    if (touched) {
                        if (getForm() != null && enable) {
                            //System.out.println("touched textobject");
                        }
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
        return enable;
    }

    /**
     * @param editable the editable to set
     */
    public void setEditable(boolean editable) {
        this.enable = editable;
    }

    /**
     * @return the stateChangeListener
     */
    public GStateChangeListener getStateChangeListener() {
        return stateChangeListener;
    }

    /**
     * @param stateChangeListener the stateChangeListener to set
     */
    public void setStateChangeListener(GStateChangeListener stateChangeListener) {
        this.stateChangeListener = stateChangeListener;
    }

    void doStateChange() {
        if (this.stateChangeListener != null) {
            this.stateChangeListener.onStateChange(this);
        }
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
        UserAction action = getUndo();
        if (action != null) {
            if (action.addOrDel == UserAction.ADD) {
                textsb.delete(action.caretIndex, action.caretIndex + action.txt.length());
            } else {
                textsb.insert(action.caretIndex, action.txt);
            }
            setCaretIndex(action.caretIndex);
            putInRedo(action);
            text_arr = null;
        }
    }

    public void redo() {
        UserAction action = getRedo();
        if (action != null) {
            if (action.addOrDel == UserAction.ADD) {
                textsb.insert(action.caretIndex, action.txt);
            } else {
                textsb.delete(action.caretIndex, action.caretIndex + action.txt.length());
            }
            setCaretIndex(action.caretIndex);
            putInUndo(action);
            text_arr = null;
        }
    }
}
