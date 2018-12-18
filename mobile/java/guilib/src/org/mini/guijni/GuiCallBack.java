/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.guijni;

import org.mini.glfw.GlfwCallback;
import org.mini.glfm.GlfmCallBack;
import org.mini.gui.GForm;

/**
 *
 * @author Gust
 */
public abstract class GuiCallBack implements GlfwCallback, GlfmCallBack {

    static GuiCallBack instance;

    public static GuiCallBack getInstance() {
        if (instance == null) {

            try {
                Class glfw = Class.forName("org.mini.glfw.Glfw");
                System.out.println("load gui native " + glfw);
                Class glfm = Class.forName("org.mini.glfm.Glfm");
                System.out.println("load gui native " + glfm);
                Class c = Class.forName(System.getProperty("gui.driver"));
                instance = (GuiCallBack) c.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
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

    public abstract void setDisplay(long winContext);
}
