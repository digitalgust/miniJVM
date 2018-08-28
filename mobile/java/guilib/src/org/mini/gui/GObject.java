/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.Timer;
import static org.mini.gui.GToolkit.nvgRGBA;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;

/**
 *
 * @author gust
 */
abstract public class GObject {

    public static final int TYPE_UNKNOW = -1;
    public static final int TYPE_BUTTON = 0;
    public static final int TYPE_CANVAS = 1;
    public static final int TYPE_CHECKBOX = 3;
    public static final int TYPE_COLORSELECTOR = 4;
    public static final int TYPE_FORM = 5;
    public static final int TYPE_FRAME = 6;
    public static final int TYPE_LABEL = 7;
    public static final int TYPE_LIST = 8;
    public static final int TYPE_LISTITEM = 9;
    public static final int TYPE_MENU = 10;
    public static final int TYPE_MENUITEM = 11;
    public static final int TYPE_PANEL = 12;
    public static final int TYPE_SCROLLBAR = 13;
    public static final int TYPE_TEXTBOX = 14;
    public static final int TYPE_TEXTFIELD = 15;
    public static final int TYPE_VIEWPORT = 16;

    //
    public static final int ALIGN_H_FULL = 1;
    public static final int ALIGN_V_FULL = 2;

    public static char ICON_SEARCH = (char) 0x1F50D;
    public static char ICON_CIRCLED_CROSS = 0x2716;
    public static char ICON_CHEVRON_RIGHT = 0xE75E;
    public static char ICON_CHECK = 0x2713;
    public static char ICON_LOGIN = 0xE740;
    public static char ICON_TRASH = 0xE729;
    //
    protected GContainer parent;

    protected float[] boundle = new float[4];

    public static final int LEFT = 0;
    public static final int TOP = 1;
    public static final int WIDTH = 2;
    public static final int HEIGHT = 3;

    protected float[] bgColor;
    protected float[] color;

    protected GActionListener actionListener;

    protected GFocusChangeListener focusListener;

    volatile static int flush;

    protected boolean visable = true;

    protected boolean front = false;

    protected boolean fixedLocation;

    protected String name;

    protected Object attachment;

    /**
     *
     */
    public void init() {

    }

    public void destory() {
    }

    public abstract int getType();

    static synchronized public void flush() {
        flush = 3;
        //in android may flush before paint,so the menu not shown
    }

    public void setFixed(boolean fixed) {
        fixedLocation = fixed;
    }

    public boolean getFixed() {
        return fixedLocation;
    }

    static synchronized public boolean flushReq() {
        if (flush > 0) {
            flush--;
            return true;
        }
        return false;
    }

    public Timer getTimer() {
        GForm form = getForm();
        if (form != null) {
            return form.timer;
        }
        return null;
    }

    public boolean update(long ctx) {
        return true;
    }

    public void keyEvent(int key, int scanCode, int action, int mods) {
    }

    public void characterEvent(char character) {
    }

    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
    }

    public void clickEvent(int button, int x, int y) {
    }

    public void cursorPosEvent(int x, int y) {
    }

    public boolean dragEvent(float dx, float dy, float x, float y) {
        return false;
    }

    public void dropEvent(int count, String[] paths) {
    }

    public void longTouchedEvent(int x, int y) {
    }

    public void keyEvent(int key, int action, int mods) {
    }

    public void characterEvent(String str, int modifiers) {
    }

    public void touchEvent(int phase, int x, int y) {
    }

    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        return false;
    }

    public void KeyboardPopEvent(boolean visible, float x, float y, float w, float h) {

    }

    /**
     * 响应惯性事件,从P1到P2用了多长时间
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param moveTime
     */
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, long moveTime) {
        return false;
    }

    public static boolean isInBoundle(float[] bound, float x, float y) {
        return x >= bound[LEFT] && x <= bound[LEFT] + bound[WIDTH]
                && y >= bound[TOP] && y <= bound[TOP] + bound[HEIGHT];
    }

    public boolean isInArea(float x, float y) {
        float absx = getX();
        float absy = getY();
        return x >= absx && x <= absx + getW()
                && y >= absy && y <= absy + getH();
    }

    public float[] getBoundle() {
        return boundle;
    }

    public GObject getParent() {
        return parent;
    }

    public void setParent(GContainer p) {
        parent = p;
    }

    public void setLocation(float x, float y) {
        boundle[LEFT] = x;
        boundle[TOP] = y;
        if (parent != null) {
            parent.reBoundle();
        }
    }

    public void setSize(float w, float h) {
        boundle[WIDTH] = w;
        boundle[HEIGHT] = h;
        if (parent != null) {
            parent.reBoundle();
        }
    }

    public float getLocationLeft() {
        return boundle[LEFT];
    }

    public float getLocationTop() {
        return boundle[TOP];
    }

    public float getX() {
        if (parent != null && !fixedLocation) {
            return parent.getX() + boundle[LEFT];
        }
        return boundle[LEFT];
    }

    public float getY() {
        if (parent != null && !fixedLocation) {
            return parent.getY() + boundle[TOP];
        }
        return boundle[TOP];
    }

    public float getW() {
        return boundle[WIDTH];
    }

    public float getH() {
        return boundle[HEIGHT];
    }

    public void move(float dx, float dy) {
        boundle[LEFT] += dx;
        boundle[TOP] += dy;
        if (parent != null) {
            parent.reBoundle();
        }
    }

    /**
     * @return the bgColor
     */
    public float[] getBgColor() {
        return bgColor;
    }

    /**
     * @param r
     * @param g
     * @param b
     * @param a
     */
    public void setBgColor(int r, int g, int b, int a) {
        bgColor = nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    /**
     * @return the color
     */
    public float[] getColor() {
        return color;
    }

    /**
     * @param r
     * @param g
     * @param b
     * @param a
     */
    public void setColor(int r, int g, int b, int a) {
        color = nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    /**
     * @return the actionListener
     */
    public GActionListener getActionListener() {
        return actionListener;
    }

    /**
     * @param actionListener the actionListener to set
     */
    public void setActionListener(GActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setVisable(boolean v) {
        visable = v;
    }

    public boolean isVisable() {
        return visable;
    }

    public GForm getForm() {
        GObject go = this;
        do {
            if (go instanceof GForm) {
                return (GForm) go;
            }
        } while ((go = go.parent) != null);
        return null;
    }

    public GFrame getFrame() {
        GObject go = this;
        while ((go = go.parent) != null) {
            if (go instanceof GFrame) {
                return (GFrame) go;
            }
        }
        return null;
    }

    /**
     * @return the focusListener
     */
    public GFocusChangeListener getFocusListener() {
        return focusListener;
    }

    /**
     * @param focusListener the focusListener to set
     */
    public void setFocusListener(GFocusChangeListener focusListener) {
        this.focusListener = focusListener;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the attachment
     */
    public Object getAttachment() {
        return attachment;
    }

    /**
     * @param attachment the attachment to set
     */
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    /**
     * @return the front
     */
    public boolean isFront() {
        return front;
    }

    /**
     * @param front the front to set
     */
    public void setFront(boolean front) {
        this.front = front;
    }
}
