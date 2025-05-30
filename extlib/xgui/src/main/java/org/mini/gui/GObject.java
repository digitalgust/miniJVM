/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glwrap.GLUtil;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GCallbackUI;
import org.mini.gui.event.*;
import org.mini.gui.gscript.Interpreter;
import org.mini.nanovg.Nanovg;
import org.mini.util.SysLog;

import java.io.ByteArrayOutputStream;

import static org.mini.gui.GToolkit.nvgRGBA;

/**
 * @author gust
 */
abstract public class GObject implements GAttachable {

    //
    public static final int TOUCH_RANGE = 20;//pix
    public static final byte LAYER_BACK = 2,
            LAYER_NORMAL = 4,
            LAYER_FRONT = 6,
            LAYER_MENU_OR_POPUP = 8,
            LAYER_INNER = 10;

    public static String ICON_SEARCH = "\uD83D\uDD0D";
    public static byte[] ICON_SEARCH_BYTE = GLUtil.toCstyleBytes(ICON_SEARCH);
    public static String ICON_CIRCLED_CROSS = "\u2297";//"\u2716";
    public static byte[] ICON_CIRCLED_CROSS_BYTE = GLUtil.toCstyleBytes(ICON_CIRCLED_CROSS);
    public static String ICON_CHEVRON_RIGHT = "\uE75E";
    public static byte[] ICON_CHEVRON_RIGHT_BYTE = GLUtil.toCstyleBytes(ICON_CHEVRON_RIGHT);
    public static String ICON_CHEVRON_DOWN = "\uE75C";
    public static byte[] ICON_CHEVRON_DOWN_BYTE = GLUtil.toCstyleBytes(ICON_CHEVRON_DOWN);
    public static String ICON_CHECK = "\u2714";
    public static byte[] ICON_CHECK_BYTE = GLUtil.toCstyleBytes(ICON_CHECK);
    public static String ICON_LOGIN = "\uE740";
    public static byte[] ICON_LOGIN_BYTE = GLUtil.toCstyleBytes(ICON_LOGIN);
    public static String ICON_TRASH = "\uE729";
    public static byte[] ICON_TRASH_BYTE = GLUtil.toCstyleBytes(ICON_TRASH);
    public static String ICON_CLOSE = "\u2716";
    public static byte[] ICON_CLOSE_BYTE = GLUtil.toCstyleBytes(ICON_CLOSE);
    public static String ICON_RIGHT = "\u2705";
    public static byte[] ICON_RIGHT_BYTE = GLUtil.toCstyleBytes(ICON_RIGHT);
    public static String ICON_INFO = "\uE705";
    public static byte[] ICON_INFO_BYTE = GLUtil.toCstyleBytes(ICON_INFO);
    public static String ICON_EYES = "\uD83D\uDC53";
    public static byte[] ICON_EYES_BYTE = GLUtil.toCstyleBytes(ICON_EYES);
    //
    public static final int LEFT = 0;
    public static final int TOP = 1;
    public static final int WIDTH = 2;
    public static final int HEIGHT = 3;

    GForm form;
    static boolean paintDebug = false;

    /**
     * drag gobject move out of it's boundle
     * if gobject need drag action self ,like select text in textbox ,then it can't dragfly
     * //是否可以被鼠标拖到组件自己之外的坐标处,比如在文本框中选中文本需要拖动，则文本框组件无法dragfly
     */
    protected boolean flyable = false;
    int flyOffsetX, flyOffsetY;

    protected GContainer parent;

    protected float[] boundle = new float[4];

    protected float[] bgColor;
    protected float[] color;
    protected float[] disabledColor;
    protected float[] flyingColor;

    private float fontSize = -1;

    protected GActionListener actionListener;

    protected GHrefListener hrefListener;

    protected GFocusChangeListener focusListener;

    protected GStateChangeListener stateChangeListener;

    protected GFlyListener flyListener;

    protected GSizeChangeListener sizeChangeListener;

    protected GLocationChangeListener locationChangeListener;

    protected boolean visible = true;

    protected boolean enable = true;
    protected boolean inited = false;
    protected boolean destroyed = false;

    protected byte layer = LAYER_NORMAL;

    protected boolean fixedLocation = false;

    protected String name;

    private String text;

    protected Object attachment;//用户自定义数据

    private String cmd;//类似attachment 用于附加String类型用户数据
    protected GLayout layout;

    protected boolean paintWhenOutOfScreen = false;

    //脚本触发器
    /**
     * two call formate:
     * framename.fun(1,2)  'assignment Interpreter by parent component name
     * fun(1,2)            'not assignment, it will find the first Interpreter of parents
     */
    private String onClinkScript;
    private String onStateChangeScript;
    protected String onCloseScript;
    protected String onInitScript;

    private GImage bgImg;

    public static final float DEFAULT_BG_ALPHA = 0.5f;
    private float bgImgAlpha = DEFAULT_BG_ALPHA;

    float cornerRadius = 0.0f;
    private String href;


    protected GObject(GForm form) {
        if (this instanceof GCallbackUI) {
            //SysLog.info("new GCallbackUI " + this);
        } else if (this instanceof GForm) {//只有GForm可以传空进来
            this.form = (GForm) this;
        } else {
            if (form == null) throw new RuntimeException("Form can not be null");
            this.form = form;
        }
    }

    /**
     *
     */
    public void setFixed(boolean fixed) {
        fixedLocation = fixed;
    }

    public boolean isFixed() {
        return fixedLocation;
    }


    public void focus() {
        if (parent != null) {
            parent.setCurrent(this);
            parent.focus();
        }
    }


    public <T extends GObject> T findParentByName(String name) {
        if (name == null) return null;
        if (parent != null) {
            if (name.equals(parent.getName())) {
                return (T) parent;
            } else {
                return parent.findParentByName(name);
            }
        }
        return null;
    }

    boolean paintFlying(long vg, float x, float y) {
        return true;
    }

    protected float getCornerRadius() {
        return cornerRadius;
    }

    protected void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public boolean paint(long vg) {
        if (bgImg != null) {
            GToolkit.drawImageStretch(vg, bgImg, getX(), getY(), getW(), getH(), false, bgImgAlpha, getCornerRadius());
        }
        return true;
    }

    public void keyEventGlfw(int key, int scanCode, int action, int mods) {
    }

    public void characterEvent(char character) {
    }

    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
    }

    public void clickEvent(int button, int x, int y) {
    }

    public void cursorPosEvent(int x, int y) {
    }

    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        if (flyable) {
            GForm form = getForm();
            if (form != null) {
                GObject f = form.getFlyingObject();
                if (f == null) {
                    form.setFlyingObject(this);
                    flyOffsetX = (int) (x - getX());
                    flyOffsetY = (int) (y - getY());
                    doFlyBegin();
                } else if (f == this) {
                    doFlying();
                }
            }
        }
        return false;
    }

    public void dropEvent(int count, String[] paths) {
    }

    public void longTouchedEvent(int x, int y) {
    }

    public void keyEventGlfm(int key, int action, int mods) {
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
        return x >= bound[LEFT] && x <= bound[LEFT] + bound[WIDTH] && y >= bound[TOP] && y <= bound[TOP] + bound[HEIGHT];
    }

    public boolean isInArea(float x, float y) {
        float absx = getX();
        float absy = getY();
        return x >= absx && x <= absx + getW() && y >= absy && y <= absy + getH();
    }

    public float[] getBoundle() {
        return boundle;
    }

    public <T extends GContainer> T getParent() {
        return (T) parent;
    }

    public void setParent(GContainer p) {
        parent = p;
    }

    public void setLocation(float x, float y) {
        float oldLeft = boundle[LEFT];
        float oldTop = boundle[TOP];
        boundle[LEFT] = x;
        boundle[TOP] = y;
        doLocationChanged(oldLeft, oldTop, x, y);
    }

    public GObject findParent(GContainer p) {
        if (p != null) {
            if (parent != null) {
                if (parent == p) {
                    return parent;
                } else {
                    return parent.findParent(p);
                }
            }
        }
        return null;
    }

    public void setSize(float w, float h) {
        if (boundle[WIDTH] != w || boundle[HEIGHT] != h) {
            boundle[WIDTH] = w;
            boundle[HEIGHT] = h;
            doSizeChanged(w, h);
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
        setLocation(boundle[LEFT] + dx, boundle[TOP] + dy);
    }

    /**
     * @return the bgColor
     */
    public float[] getBgColor() {
        if (bgColor == null) return GToolkit.getStyle().getBackgroundColor();
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

    public void setBgColor(int rgba) {
        bgColor = nvgRGBA(rgba);
    }

    /**
     * @return the color
     */
    public float[] getColor() {
        if (isFlying()) return getFlyingColor();
        if (color == null) {
            if (isEnable()) return GToolkit.getStyle().getTextFontColor();
            else return GToolkit.getStyle().getDisabledTextFontColor();
        }
        return color;
    }


    public void setPreiconColor(float[] preiconColor) {
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

    public void setColor(int rgba) {
        setColor(nvgRGBA(rgba));
    }

    public float[] getDisabledColor() {
        if (disabledColor == null) {
            return GToolkit.getStyle().getDisabledTextFontColor();
        }
        return disabledColor;
    }

    public float[] getFlyingColor() {
        if (flyingColor == null) {
            float[] c = color == null ? GToolkit.getStyle().getTextFontColor() : color;
            flyingColor = new float[4];
            for (int i = 0; i < 3; i++) {
                flyingColor[i] = c[i];
            }
            flyingColor[3] /= 2;
        }
        return flyingColor;
    }

    public float getFontSize() {
        if (fontSize <= 0) return GToolkit.getStyle().getTextFontSize();
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
        if (visible == v) return;
        visible = v;
        doStateChanged(this);
    }

    public boolean isVisible() {
        return visible;
    }


    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        if (this.enable == enable) return;
        this.enable = enable;
        doStateChanged(this);
    }


    public GForm getForm() {
        return form;
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
    public <T extends Object> T getAttachment() {
        return (T) attachment;
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
        return layer == LAYER_FRONT;
    }

    /**
     * @param front the front to set
     */
    public void setFront(boolean front) {
        if (front && this.layer == LAYER_FRONT) return;
        if (!front && this.layer == LAYER_NORMAL) return;
        this.layer = front ? LAYER_FRONT : LAYER_NORMAL;
        if (parent != null) parent.reLayer();
        doStateChanged(this);
    }

    /**
     * @return the front
     */
    public boolean isBack() {
        return layer == LAYER_BACK;
    }

    /**
     * @param back the front to set
     */
    public void setBack(boolean back) {
        if (back && this.layer == LAYER_BACK) return;
        if (!back && this.layer == LAYER_NORMAL) return;
        this.layer = back ? LAYER_BACK : LAYER_NORMAL;
        if (parent != null) parent.reLayer();
        doStateChanged(this);
    }

    public boolean isMenu() {
        return layer == LAYER_MENU_OR_POPUP;
    }

    public boolean isContextMenu() {
        return false;
    }

    protected void doAction() {
        if (actionListener != null && visible && enable) {
            if (onClinkScript != null) {
                Interpreter inp = parseInpByCall(onClinkScript);
                String funcName = parseInstByCall(onClinkScript);
                if (inp != null && funcName != null) {
                    try {
                        inp.callSub(funcName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            actionListener.action(this);
        }
        if (href != null && hrefListener != null) {
            hrefListener.gotoHref(this, href);
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

    void doSizeChanged(float dw, float dh) {
        if (sizeChangeListener != null) {
            sizeChangeListener.onSizeChange((int) dw, (int) dh);
        }
    }

    void doLocationChanged(float oldLeft, float oldTop, float newLeft, float newTop) {
        if (locationChangeListener != null) {
            if (oldLeft != newLeft || oldTop != newTop) {
                locationChangeListener.onLocationChange(oldLeft, oldTop, newLeft, newTop);
            }
        }
    }

    public GSizeChangeListener getSizeChangeListener() {
        return sizeChangeListener;
    }

    public void setSizeChangeListener(GSizeChangeListener sizeChangeListener) {
        this.sizeChangeListener = sizeChangeListener;
    }

    public GLocationChangeListener getLocationChangeListener() {
        return locationChangeListener;
    }

    public void setLocationChangeListener(GLocationChangeListener locationChangeListener) {
        this.locationChangeListener = locationChangeListener;
    }

    public GStateChangeListener getStateChangeListener() {
        return stateChangeListener;
    }

    public void setStateChangeListener(GStateChangeListener stateChangeListener) {
        this.stateChangeListener = stateChangeListener;
    }

    void doStateChanged(GObject go) {
        if (stateChangeListener != null) {
            if (onStateChangeScript != null) {
                Interpreter inp = parseInpByCall(onStateChangeScript);
                String funcName = parseInstByCall(onStateChangeScript);
                if (inp != null && funcName != null) {
                    try {
                        inp.callSub(funcName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            stateChangeListener.onStateChange(go);
        }
    }

    public String toString() {
        return super.toString() + "|" + name + "|" + text + "(" + boundle[LEFT] + "," + boundle[TOP] + "," + boundle[WIDTH] + "," + boundle[HEIGHT] + ")";
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public boolean isFlyable() {
        return flyable;
    }

    public void setFlyable(boolean flyable) {
        this.flyable = flyable;
    }

    public void setFlyListener(GFlyListener flyListener) {
        this.flyListener = flyListener;
    }

    public GFlyListener getFlyListener() {
        return flyListener;
    }

    void doFlyBegin() {
        if (flyListener != null && flyable) {
            flyListener.flyBegin(this, GCallBack.getInstance().getTouchOrMouseX(), GCallBack.getInstance().getTouchOrMouseY());
        }
    }

    void doFlyEnd() {
        if (flyListener != null && flyable) {
            flyListener.flyEnd(this, GCallBack.getInstance().getTouchOrMouseX(), GCallBack.getInstance().getTouchOrMouseY());
        }
    }

    void doFlying() {
        if (flyListener != null && flyable) {
            flyListener.flying(this, GCallBack.getInstance().getTouchOrMouseX(), GCallBack.getInstance().getTouchOrMouseY());
        }
    }

    public boolean isFlying() {
        return getForm() != null && getForm().getFlyingObject() == this;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return this.cmd;
    }


    //================================  script   =====================================

    /**
     * @param funcCall
     * @return
     */
    protected Interpreter parseInpByCall(String funcCall) {
        Interpreter inp = null;
        //two call formate:
        // framename.fun(1,2)
        // fun(1,2)
        int leftQ = funcCall.indexOf('(');
        int point = funcCall.indexOf('.');
        if (leftQ > 0) {
            if (point >= 0 && point < leftQ) {
                String containerName = funcCall.substring(0, point);
                inp = getInterpreter(containerName);
            } else {
                inp = getInterpreter();
            }
        }
        return inp;
    }

    protected String parseInstByCall(String funcCall) {
        String funcName = null;
        //two call formate:
        // framename.fun(1,2)
        // fun(1,2)
        int leftQ = funcCall.indexOf('(');
        int point = funcCall.indexOf('.');
        if (leftQ > 0) {
            if (point >= 0 && point < leftQ) {
                funcName = funcCall.substring(point + 1);
            } else {
                funcName = funcCall;
            }
        }
        return funcName;
    }


    public void setOnClinkScript(String onClinkScript) {
        this.onClinkScript = onClinkScript;
    }

    public void setOnStateChangeScript(String onStateChangeScript) {
        this.onStateChangeScript = onStateChangeScript;
    }

    public String getOnClinkScript() {
        return onClinkScript;
    }

    public String getOnStateChangeScript() {
        return onStateChangeScript;
    }

    public Interpreter getInterpreter() {
        return getInterpreter(null);
    }

    public Interpreter getInterpreter(String containerName) {
        if (getParent() == null) return null;
        if (containerName == null) return getParent().getInterpreter();
        return getParent().getInterpreter(containerName);
    }
    //================================    =====================================


    public GLayout getLayout() {
        return layout;
    }

    public void setLayout(GLayout layout) {
        this.layout = layout;
    }

    byte getLayer() {
        return layer;
    }

    protected ByteArrayOutputStream utf32ToBytes(int pchar, ByteArrayOutputStream baos) {
        if (baos == null) {
            baos = new ByteArrayOutputStream();
        }
        for (int i = 3; i >= 0; i--) {
            byte b = (byte) (pchar >> (i * 8));
            if (b != 0) baos.write(b);
        }
        return baos;
    }

    public GImage getBgImg() {
        return bgImg;
    }

    public void setBgImg(GImage bgImg) {
        this.bgImg = bgImg;
    }

    public void setBgImgAlpha(float bgImgAlpha) {
        this.bgImgAlpha = bgImgAlpha;
    }

    public float getBgImgAlpha() {
        return bgImgAlpha;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public GHrefListener getHrefListener() {
        return hrefListener;
    }

    public void setHrefListener(GHrefListener hrefListener) {
        this.hrefListener = hrefListener;
    }

    public void flushNow() {
        GForm.flush();
    }


    public String getOnCloseScript() {
        return onCloseScript;
    }

    public String getOnInitScript() {
        return onInitScript;
    }

    public void setOnCloseScript(String onCloseScript) {
        this.onCloseScript = onCloseScript;
        //fix no interpreter when script is set
        if (onCloseScript != null) {
            Interpreter inp = getInterpreter();
            if (inp == null) {
                inp.loadFromString("");
            }
        }
    }

    public void setOnInitScript(String onInitScript) {
        this.onInitScript = onInitScript;
        //fix no interpreter when script is set
        if (onInitScript != null) {
            Interpreter inp = getInterpreter();
            if (inp == null) {
                inp.loadFromString("");
            }
        }
    }

    public boolean isInited() {
        return inited;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void init() {
        inited = true;
        if (onInitScript != null) {
            Interpreter inp = parseInpByCall(onInitScript);
            String funcName = parseInstByCall(onInitScript);
            if (inp != null && funcName != null) {
                try {
                    inp.callSub(funcName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void destroy() {
        destroyed = true;
        if (onCloseScript != null) {
            Interpreter inp = parseInpByCall(onCloseScript);
            String funcName = parseInstByCall(onCloseScript);
            if (inp != null && funcName != null) {
                try {
                    inp.callSub(funcName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
