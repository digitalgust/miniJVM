/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFocusChangeListener;
import org.mini.nanovg.Nanovg;

import java.util.Timer;

import static org.mini.gui.GToolkit.nvgRGBA;

/**
 * @author gust
 */
abstract public class GObject {

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
    public static final int LEFT = 0;
    public static final int TOP = 1;
    public static final int WIDTH = 2;
    public static final int HEIGHT = 3;

    volatile static int flush;

    protected GContainer parent;

    protected float[] boundle = new float[4];

    protected float[] bgColor;
    protected float[] color;

    protected float fontSize;

    protected GActionListener actionListener;

    protected GFocusChangeListener focusListener;


    protected boolean visible = true;

    protected boolean front = false;

    protected boolean fixedLocation;

    protected String name;

    protected String text;

    protected Object attachment;

    protected Object xmlAgent;

    /**
     *
     */
    public void init() {

    }

    public void destroy() {
    }


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

    public boolean paint(long ctx) {
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

    public void touchEvent(int touchid, int phase, int x, int y) {
    }

    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        return false;
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

    public GContainer getParent() {
        return parent;
    }

    public void setParent(GContainer p) {
        parent = p;
    }

    public void setLocation(float x, float y) {
        boundle[LEFT] = x;
        boundle[TOP] = y;
        if (parent != null) {
            parent.reSize();
        }
    }

    public void setSize(float w, float h) {
        boundle[WIDTH] = w;
        boundle[HEIGHT] = h;
        if (parent != null) {
            parent.reSize();
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
            return parent.getInnerX() + boundle[LEFT];
        }
        return boundle[LEFT];
    }

    public float getY() {
        if (parent != null && !fixedLocation) {
            return parent.getInnerY() + boundle[TOP];
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
            parent.reSize();
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
        bgColor = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public void setBgColor(float[] color) {
        bgColor = color;
    }

    public void setBgColor(int rgba){
        bgColor = nvgRGBA(rgba);
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
        color = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public void setColor(float[] color) {
        this.color = color;
    }

    public void setColor(int rgba){
        color = nvgRGBA(rgba);
    }

    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
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

    public void setVisible(boolean v) {
        visible = v;
    }

    public boolean isVisible() {
        return visible;
    }

    public GForm getForm() {
        GObject go = this;
        while (!(go instanceof GForm)) {
            if (go == null) {
                return null;
            }
            go = go.getParent();
        }
        return (GForm) go;
    }

    public GFrame getFrame() {
        GObject go = this;
        while (!(go instanceof GFrame)) {
            if (go == null) {
                return null;
            }
            go = go.getParent();
        }
        return (GFrame) go;
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


    public Object getXmlAgent() {
        return xmlAgent;
    }

    public void setXmlAgent(Object xmlAgent) {
        this.xmlAgent = xmlAgent;
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

    void doAction() {
        if (actionListener != null) {
            actionListener.action(this);
        }
    }

    void doFocusLost(GObject newgo) {
        if (focusListener != null) {
            focusListener.focusLost(newgo);
        }
    }

    void doFocusGot(GObject oldgo) {
        if (focusListener != null) {
            focusListener.focusGot(oldgo);
        }
    }

    public String toString() {
        return super.toString() + "(" + boundle[LEFT] + "," + boundle[TOP] + "," + boundle[WIDTH] + "," + boundle[HEIGHT] + ")";
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }
}
