/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.guijni;

import org.mini.glfm.GlfmCallBack;
import org.mini.glfw.GlfwCallback;
import org.mini.gui.GApplication;

/**
 * @author Gust
 */
public abstract class GuiCallBack implements GlfwCallback, GlfmCallBack {


    public static final String GLVERSION_GL3 = "GL3";
    public static final String GLVERSION_GLES3 = "GLES3";


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

//    public abstract void setForm(GForm form);
//
//    public abstract GForm getForm();

    public abstract void init(int w, int h);

    public abstract void destory();

    public abstract void setDisplay(long winContext);

    public abstract GApplication getApplication();

    public abstract void setApplication(GApplication app);

    public abstract void notifyCurrentFormChanged(GApplication app);

    public abstract String getGLVersion();
}
