/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;
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

    private static EditMenu editMenu;

    boolean selectMode = false;

    public void setHint(String hint) {
        this.hint = hint;
        hint_arr = toUtf8(hint);
    }

    public String getHint() {
        return hint;
    }

    public void setText(String text) {
        this.textsb.setLength(0);
        this.textsb.append(text);
    }

    public String getText() {
        return textsb.toString();
    }

    public void insertTextByIndex(int index, char ch) {
        textsb.insert(index, ch);
        text_arr = null;
    }

    public void deleteTextByIndex(int index) {
        textsb.deleteCharAt(index);
        text_arr = null;
    }

    public void deleteAll() {
        textsb.setLength(0);
        text_arr = null;
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

    @Override
    public void focusGot(GObject go) {
        if (getForm() != null) {
            Glfm.glfmSetKeyboardVisible(getForm().getWinContext(), true);
        }
    }

    @Override
    public void focusLost(GObject go) {
        if (getForm() != null) {
            Glfm.glfmSetKeyboardVisible(getForm().getWinContext(), false);
        }
        disposeEditMenu();
    }

    @Override
    public void longTouchedEvent(int x, int y) {
        callEditMenu(this, x, y);
        //System.out.println("long toucched");

    }

    class EditMenu extends GMenu {

        GTextObject text;

        public EditMenu(int left, int top, int width, int height) {
            super(left, top, width, height);
        }

        @Override
        public boolean update(long vg) {
            GForm gf = getForm();
            if (gf != null && gf.getFocus() != this) {
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
            GMenuItem item;
            item = editMenu.addItem(GLanguage.getString("Select All"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    editMenu.text.doSelectAll();
                }
            });
            item = editMenu.addItem(GLanguage.getString("Cut"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    editMenu.text.doCut();
                    disposeEditMenu();
                }
            });
            item = editMenu.addItem(GLanguage.getString("Paste"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    editMenu.text.doPasteClipBoard();
                    disposeEditMenu();
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
            item = editMenu.addItem(GLanguage.getString("Select"), null);
            item.setActionListener(new GActionListener() {
                @Override
                public void action(GObject gobj) {
                    editMenu.text.doSelectText();
                }
            });
            editMenu.setFocusListener(new GFocusChangeListener() {
                @Override
                public void focusGot(GObject go) {
                }

                @Override
                public void focusLost(GObject go) {
                    getForm().remove(editMenu);
                }
            });

        }
        editMenu.text = focus;
        editMenu.setLocation(mx, my);
        //editMenu.move(mx - editMenu.getX(), my - editMenu.getY());

        getForm().add(editMenu);
        getForm().setFocus(editMenu);
        //System.out.println("edit menu show");
    }

    synchronized void disposeEditMenu() {
        GForm gf = getForm();
        if (gf != null && editMenu != null) {
            gf.remove(editMenu);
            if (editMenu.text != null) {
                gf.setFocus(editMenu.text.getFrame());
            }
            resetSelect();
            selectMode = false;
        }
        //System.out.println("edit menu dispose");
    }
}
