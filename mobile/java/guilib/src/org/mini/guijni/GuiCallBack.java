/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.guijni;

import org.mini.glfm.GuiCallBackImpl;
import org.mini.glfm.GlfmCallBack;
import org.mini.glfw.GlfwCallback;
import org.mini.gui.GForm;

/**
 *
 * @author Gust
 */
public abstract class GuiCallBack implements GlfwCallback, GlfmCallBack {

    public static GuiCallBack getInstance() {
        return GuiCallBackImpl.getInstance();
    }

    public abstract String getAppSaveRoot();

    public abstract String getAppResRoot();

    public abstract long getNvContext();

    public abstract void setDisplayTitle(String title);

    public abstract long getDisplay();

    public abstract int getFrameBufferHeight();

    public abstract int getDeviceWidth();

    public abstract int getDeviceHeight();

    public abstract float getDeviceRatio();

    public abstract int getFrameBufferWidth();

    public abstract void setForm(GForm form);

    public abstract GForm getForm();

    public abstract void init(int w, int h);

    public abstract void destory();
}
