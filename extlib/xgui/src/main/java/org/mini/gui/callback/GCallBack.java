/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui.callback;

import org.mini.apploader.GApplication;
import org.mini.glfm.GlfmCallBack;
import org.mini.glfw.GlfwCallback;
import org.mini.gui.GForm;
import org.mini.util.SysLog;

import java.io.InputStream;
import java.util.logging.Level;

/**
 * @author Gust
 */
public abstract class GCallBack implements GlfwCallback, GlfmCallBack {

    public static final int PICK_PHOTO_DEVICE_ALBUM = 0x1000;
    public static final int PICK_PHOTO_DEVICE_CAMERA = 0x2000;
    public static final int PICK_PHOTO_RESIZE_1024 = 0x100;
    public static final int PICK_PHOTO_RESIZE_NONE = 0x000;
    public static final int PICK_PHOTO_TYPE_IMAGE = 0x1;
    public static final int PICK_PHOTO_TYPE_MOIVE = 0x2;
    public static final float FPS_DEFAULT = 60f;
    static GCallBack instance;

    protected GApplication gapp;
    protected GDesktop desktop;

    float[] insets = new float[4];

    public static GCallBack getInstance() {
        if (instance == null) {

            try {
                SysLog.getLogger().setLevel(Level.INFO);
                System.out.println("uuid=" + System.getProperty("uuid"));
//                System.setProperty("com.sun.midp.io.http.proxy", "127.0.0.1:1087");
//                System.setProperty("https.proxyHost", "127.0.0.1");
//                System.setProperty("https.proxyPort", "10808");
                Class glfw = Class.forName("org.mini.glfw.Glfw");
                //SysLog.info("load gui native " + glfw);
                Class glfm = Class.forName("org.mini.glfm.Glfm");
                //SysLog.info("load gui native " + glfm);
                Class c = Class.forName(System.getProperty("gui.driver"));
                instance = (GCallBack) c.newInstance();
                instance.desktop = new GDesktop(instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public GApplication getApplication() {
        return gapp;
    }

    public void setApplication(GApplication app) {
        if (app != null) {
            desktop.setSize(getDeviceWidth(), getDeviceHeight());
            desktop.setCurrent(null);
            gapp = app;
        }
    }

    protected abstract void onFormSet(GForm form);

    public GDesktop getDesktop() {
        return desktop;
    }


    private ClassLoader getClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            if (gapp == null) {
                loader = ClassLoader.getSystemClassLoader();
            } else {
                loader = gapp.getClass().getClassLoader();
            }
        }
        return loader;
    }

    public InputStream getResourceAsStream(String path) {
        return getClassLoader().getResourceAsStream(path);
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

    public abstract void init(int w, int h);

    public abstract void destroy();

    public abstract void setDisplay(long winContext);

    public abstract void setFps(float fpsExpect);

    public abstract float getFps();

    public abstract int getTouchOrMouseX();

    public abstract int getTouchOrMouseY();

    public abstract void getInsets(float[] top_right_bottom_left);

    public float getInsetTop() {
        getInsets(insets);
        return insets[0];
    }

    public float getInsetRight() {
        getInsets(insets);
        return insets[1];
    }

    public float getInsetBottom() {
        getInsets(insets);
        return insets[2];
    }

    public float getInsetLeft() {
        getInsets(insets);
        return insets[3];
    }

    public abstract Thread getOpenglThread();

    public void pickPhoto(int uid, int deviceAndType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void playeVideo(String url, String mimeType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    //============================== glfm
    @Override
    public void onRender(long display) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mainLoop(long display, double frameTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean onTouch(long display, int touch, int phase, double x, double y) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean onKey(long display, int keyCode, int action, int modifiers) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onCharacter(long display, String str, int modifiers) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onKeyboardVisible(long display, boolean visible, double x, double y, double w, double h) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSurfaceError(long display, String description) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSurfaceCreated(long display, int width, int height) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSurfaceResize(long display, int width, int height) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSurfaceDestroyed(long display) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onMemWarning(long display) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onAppFocus(long display, boolean focused) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onPhotoPicked(long display, int uid, String url, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onNotify(long display, String key, String val) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onOrientationChanged(long display, int orientation) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    //============================== glfw
    @Override
    public void error(int error, String description) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void monitor(long monitor, boolean connected) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void framebufferSize(long window, int x, int y) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void windowPos(long window, int x, int y) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void windowSize(long window, int width, int height) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean windowClose(long window) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void windowRefresh(long window) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void windowFocus(long window, boolean focused) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void windowIconify(long window, boolean iconified) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void key(long window, int key, int scancode, int action, int mods) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void character(long window, char character) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseButton(long window, int button, boolean pressed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cursorPos(long window, int x, int y) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cursorEnter(long window, boolean entered) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void scroll(long window, double scrollX, double scrollY) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void drop(long window, int count, String[] paths) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mainLoop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
