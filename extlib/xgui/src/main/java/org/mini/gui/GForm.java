/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.apploader.GApplication;
import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GCmd;
import org.mini.gui.callback.GDesktop;
import org.mini.gui.event.*;

import static org.mini.gui.GToolkit.nvgRGBA;

/**
 * @author gust
 */
public class GForm extends GContainer {

    protected GObject flyingObject;

    //键盘弹出,使form 向上移动
    float keyboardPopTranslateFormY;
    protected GObject editObject;
    //
    //

    protected GPhotoPickedListener pickListener;
    protected GKeyboardShowListener keyshowListener;
    protected GAppActiveListener activeListener;
    protected GNotifyListener notifyListener;
    GApplication app;

    public GForm(GApplication app) {
        super(null);
        if (app == null) {
            throw new RuntimeException("app can't be null when create a GForm");
        }
        setApp(app);
        if (app.getForm() == null) { //only one form
            app.setForm(this);
        }
        setSize(GCallBack.getInstance().getDeviceWidth(), GCallBack.getInstance().getDeviceHeight());
    }

    void setApp(GApplication app) {
        this.app = app;
    }

    public GApplication getApp() {
        return app;
    }

    public static void addCmd(GCmd cmd) {
        GDesktop.addCmd(cmd);
    }

    public static void deleteImage(int nvgTexture) {
        GDesktop.deleteImage(nvgTexture);
    }

    public boolean paint(long vg) {
        super.paint(vg);
        paintFlyingObject(vg);
        return true;
    }


    public static void flush() {
        GDesktop.flush = 4;
        //in android may flush before paint,so the menu not shown
    }

    void paintFlyingObject(long vg) {
        if (flyingObject != null) {
            int x = GCallBack.getInstance().getTouchOrMouseX();
            int y = GCallBack.getInstance().getTouchOrMouseY();
            flyingObject.paintFlying(vg, x - flyingObject.flyOffsetX, y - flyingObject.flyOffsetY);
        }

    }

    public void onPhotoPicked(int uid, String url, byte[] data) {
        if (pickListener != null) {
            pickListener.onPicked(uid, url, data);
        }
    }

    /**
     * @return the pickListener
     */
    public GPhotoPickedListener getPickListener() {
        return pickListener;
    }

    /**
     * @param pickListener the pickListener to set
     */
    public void setPickListener(GPhotoPickedListener pickListener) {
        this.pickListener = pickListener;
    }

    float tx, ty, tw, th;

    public void KeyboardPopEvent(boolean visible, float x, float y, float w, float h) {
        tx = x;
        ty = y;
        tw = w;
        th = h;
        if (visible) {
            if (editObject != null) {
                float objbtn = editObject.getY() + editObject.getH();
                if (editObject instanceof GTextBox) {
                    objbtn = ((GTextBox) editObject).getCaretY() + 10f;
                    if (objbtn < 30) {
                        objbtn = 30;
                    }
                }
                float obj2scrbtn = getH() - objbtn;
                if (h > obj2scrbtn) {
                    float trans = h - obj2scrbtn;
                    this.setLocation(getX(), getY() - trans);
                    keyboardPopTranslateFormY += trans;//多次弹出
                }
            }
        } else {
            this.setLocation(getX(), getY() + keyboardPopTranslateFormY);
            keyboardPopTranslateFormY = 0;
        }
        if (keyshowListener != null) {
            keyshowListener.keyboardShow(visible, x, y, w, h);
        }
        flush();
    }


    public static void addMessage(String s) {
        GDesktop.addMessage(s);
    }

    /**
     * @return the keyshowListener
     */
    public GKeyboardShowListener getKeyshowListener() {
        return keyshowListener;
    }

    /**
     * @param keyshowListener the keyshowListener to set
     */
    public void setKeyshowListener(GKeyboardShowListener keyshowListener) {
        this.keyshowListener = keyshowListener;
    }


    public void onAppFocus(boolean focus) {
        //System.out.println("app focus:" + focus);
        if (activeListener != null) {
            activeListener.onAppActive(focus);
        }
        flush();
    }

    public void onDeviceNotify(String key, String val) {
        if (notifyListener != null) {
            notifyListener.onNotify(key, val);
        } else {
            System.out.println("[WARN]notifyListener is null when onNotify:" + key + "," + val);
        }
    }

    public void onDeviceSizeChanged(int width, int height) {
        if (sizeChangeListener != null) {
            sizeChangeListener.onSizeChange(width, height);
        }
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        super.mouseButtonEvent(button, pressed, x, y);
        if (button == Glfw.GLFW_MOUSE_BUTTON_1 && !pressed) {
            if (flyingObject != null) {
                flyingObject.doFlyEnd();
                setFlyingObject(null);
            }
        }
    }

    public void touchEvent(int touchid, int phase, int x, int y) {
        super.touchEvent(touchid, phase, x, y);
        if (phase == Glfm.GLFMTouchPhaseEnded && touchid == Glfw.GLFW_MOUSE_BUTTON_1) {
            if (flyingObject != null) {
                flyingObject.doFlyEnd();
                setFlyingObject(null);
            }
        }
    }

    public GObject getFlyingObject() {
        return flyingObject;
    }

    public void setFlyingObject(GObject flyingObject) {
        this.flyingObject = flyingObject;
    }


    /**
     * @return the activeListener
     */
    public GAppActiveListener getActiveListener() {
        return activeListener;
    }

    /**
     * @param activeListener the activeListener to set
     */
    public void setActiveListener(GAppActiveListener activeListener) {
        this.activeListener = activeListener;
    }

    /**
     * @return the notifyListener
     */
    public GNotifyListener getNotifyListener() {
        return notifyListener;
    }

    /**
     * @param notifyListener the notifyListener to set
     */
    public void setNotifyListener(GNotifyListener notifyListener) {
        this.notifyListener = notifyListener;
    }


    public static void showKeyboard() {
        GDesktop.addCmd(GCmd.GCMD_SHOW_KEYBOARD);
    }

    public static void showKeyboard(GTextObject editObj) {
        editObj.form.editObject = editObj;
        GDesktop.addCmd(GCmd.GCMD_SHOW_KEYBOARD);
    }

    public static void hideKeyboard(GForm form) {
        form.editObject = null;
        GDesktop.addCmd(GCmd.GCMD_HIDE_KEYBOARD);
    }


}
