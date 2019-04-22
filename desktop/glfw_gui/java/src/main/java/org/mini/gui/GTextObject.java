/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.TimerTask;
import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;
import org.mini.gui.event.GStateChangeListener;
import static org.mini.nanovg.Gutil.toUtf8;

/**
 *
 * @author Gust
 */
public abstract class GTextObject extends GObject implements GFocusChangeListener {

    String hint;
    byte[] hint_arr;
    StringBuilder textsb = new StringBuilder();
    byte[] text_arr;
    boolean editable = true;

    GStateChangeListener stateChangeListener;

    private static EditMenu editMenu;

    boolean selectMode = false;

    GObject unionObj;//if this object exists, the keyboard not disappear

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
    }

    public void deleteTextByIndex(int index) {
        textsb.deleteCharAt(index);
        text_arr = null;
        doStateChange();
    }

    public void deleteAll() {
        textsb.setLength(0);
        text_arr = null;
        doStateChange();
    }

    abstract public String getSelectedText();

    abstract public void deleteSelectedText();

    abstract public void insertTextAtCaret(String str);

    abstract void resetSelect();

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
        if (editable) {
            GForm.showKeyboard();
        }
    }

    @Override
    public void focusLost(GObject newgo) {
        if (newgo != unionObj && newgo != this) {
            GForm.hideKeyboard();
        }
        disposeEditMenu();
        touched = false;
    }

    boolean touched;

    @Override
    public void touchEvent(int phase, int x, int y) {
        if (isInArea(x, y)) {
            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan: {
                    touched = true;
                    break;
                }
                case Glfm.GLFMTouchPhaseEnded: {
                    if (touched) {
                        if (getForm() != null && editable) {
                            //System.out.println("touched textobject");
                        }
                        touched = false;
                    }
                    break;
                }
            }
        }
        super.touchEvent(phase, x, y);
    }

    @Override
    public void longTouchedEvent(int x, int y) {
        callEditMenu(this, x, y);
        //System.out.println("long toucched");
        super.longTouchedEvent(x, y);
    }

    static public boolean isEditMenuShown() {
        if (editMenu == null) {
            return false;
        }
        return editMenu.getParent() != null;
    }

    static public GMenu getEditMenu() {
        return editMenu;
    }

    /**
     * @return the unionObj
     */
    public GObject getUnionObj() {
        return unionObj;
    }

    /**
     * @param unionObj the unionObj to set
     */
    public void setUnionObj(GObject unionObj) {
        this.unionObj = unionObj;
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

    public class EditMenu extends GMenu {

        boolean shown = false;
        GTextObject text;

        public EditMenu(int left, int top, int width, int height) {
            super(left, top, width, height);
        }

        @Override
        public boolean update(long vg) {
            if (text != null && text.getParent().getForm() == null) {
                disposeEditMenu();
            }
            return super.update(vg);
        }
    }

    /**
     * 唤出基于form层的编辑菜单,选中菜单项后消失,失去焦点后消失
     *
     * @param focus
     * @param x
     * @param y
     */
    synchronized void callEditMenu(GTextObject focus, float x, float y) {
        float menuH = 40, menuW = 300;

        float mx = x - menuW / 2;
        if (mx < 10) {
            mx = 10;
        } else if (mx + menuW > focus.getForm().getDeviceWidth()) {
            mx = focus.getForm().getDeviceWidth() - menuW;
        }
        mx -= getForm().getX();
        float my = y - 20 - menuH;
        if (my < 20) {
            my = y + 10;
        } else if (my + menuH > focus.getForm().getDeviceHeight()) {
            my = focus.getForm().getDeviceHeight() - menuH;
        }
        my -= getForm().getY();

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
                    disposeEditMenu();
                }
            });
            item = editMenu.addItem(GLanguage.getString("Paste"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    if (editable) {
                        editMenu.text.doPasteClipBoard();
                    }
                    disposeEditMenu();
                }
            });
            item = editMenu.addItem(GLanguage.getString("Cut"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    if (editable) {
                        editMenu.text.doCut();
                    }
                    disposeEditMenu();
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

        getForm().add(editMenu);
        //System.out.println("edit menu show");
    }

    synchronized void disposeEditMenu() {
        GForm.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                GForm gf = getForm();
                if (gf != null && editMenu != null) {
                    gf.remove(editMenu);
//                    if (editMenu.text == null) {
//                    } else {
//                        if (editMenu.text.unionObj != editMenu.text.parent.getFocus()) {
//                            gf.remove(editMenu);
//                        }
//                    }
                    resetSelect();
                    selectMode = false;
                }
            }
        }, 0);
        //System.out.println("edit menu dispose");
    }
}
