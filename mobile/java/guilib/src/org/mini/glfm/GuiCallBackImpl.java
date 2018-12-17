/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.glfm;

import org.mini.apploader.GlfmMain;
import org.mini.gui.GForm;
import org.mini.gui.GObject;
import org.mini.gui.GToolkit;
import org.mini.guijni.GuiCallBack;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ANTIALIAS;
import static org.mini.nanovg.Nanovg.NVG_DEBUG;
import static org.mini.nanovg.Nanovg.NVG_STENCIL_STROKES;

/**
 *
 * @author Gust
 */
public class GuiCallBackImpl extends GuiCallBack {

    long display;
    int winWidth, winHeight;
    int fbWidth, fbHeight;
    float pxRatio;

    public int mouseX, mouseY, lastX, lastY;
    long mouseLastPressed;
    int LONG_TOUCH_TIME = 500;
    int LONG_TOUCH_MAX_DISTANCE = 5;//移动距离超过40单位时可以产生惯性

    int INERTIA_MIN_DISTANCE = 20;//移动距离超过40单位时可以产生惯性
    int INERTIA_MAX_MILLS = 300;//在300毫秒内的滑动可以产生惯性

    //
    double moveStartX;
    double moveStartY;
    long moveStartAt;

    GForm gform;

    long vg;

    static GuiCallBackImpl instance = new GuiCallBackImpl();

    static public void glinit(long winContext) {

        Glfm.glfmSetDisplayConfig(winContext,
                Glfm.GLFMRenderingAPIOpenGLES3,
                Glfm.GLFMColorFormatRGBA8888,
                Glfm.GLFMDepthFormat16,
                Glfm.GLFMStencilFormat8,
                Glfm.GLFMMultisampleNone);

        GuiCallBackImpl.getInstance().setDisplay(winContext);
        Glfm.glfmSetCallBack(winContext, GuiCallBackImpl.getInstance());

    }

    public static GuiCallBackImpl getInstance() {
        return instance;
    }

    private GuiCallBackImpl() {
    }

    public void setDisplay(long display) {
        this.display = display;
    }

    public GForm getForm() {
        return gform;
    }

    public void setForm(GForm form) {
        gform = form;
    }

    public long getDisplay() {
        return display;
    }

    public long getNvContext() {
        return vg;
    }

    public int getDeviceWidth() {
        return (int) winWidth;
    }

    public int getDeviceHeight() {
        return (int) winHeight;
    }

    public int getFrameBufferWidth() {
        return (int) fbWidth;
    }

    public int getFrameBufferHeight() {
        return (int) fbHeight;
    }

    public float getDeviceRatio() {
        return pxRatio;
    }

    public String getResRoot() {
        return Glfm.glfmGetResRoot();
    }

    public void setDisplayTitle(String title) {
        ;
    }

    void init() {
        fbWidth = Glfm.glfmGetDisplayWidth(display);
        fbHeight = Glfm.glfmGetDisplayHeight(display);
        // Calculate pixel ration for hi-dpi devices.
        pxRatio = (float) Glfm.glfmGetDisplayScale(display);
        winWidth = (int) (fbWidth / pxRatio);
        winHeight = (int) (fbHeight / pxRatio);

        vg = Nanovg.nvgCreateGLES3(NVG_ANTIALIAS | NVG_STENCIL_STROKES | NVG_DEBUG);
        if (vg == 0) {
            System.out.println("Could not init nanovg.\n");
        } else {
            System.out.println("nanovg success.");
        }
        GToolkit.FontHolder.loadFont(vg);

    }

    @Override
    public void mainLoop(long display, double frameTime) {

        try {
            if (gform != null) {
                if (gform.getWinContext() == 0) {
                    gform.init();
                }
            }
            if (GObject.flushReq()) {
                if (gform != null) {
                    gform.display(vg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceCreated(long display, int width, int height) {
        init();
        GlfmMain.onSurfaceCreated();
    }

    @Override
    public boolean onKey(long display, int keyCode, int action, int modifiers) {
        if (gform == null) {
            return true;
        }
        GObject focus = gform.getFocus();
        //System.out.println("keyCode  :" + keyCode + "   action=" + action + "   modifiers=" + modifiers);
        if (focus != null) {
            focus.keyEvent(keyCode, action, modifiers);
        } else {
            gform.keyEvent(keyCode, action, modifiers);
        }
        gform.flush();
        return true;
    }

    @Override
    public void onCharacter(long window, String str, int modifiers) {
        if (gform == null) {
            return;
        }
        GObject focus = gform.getFocus();
        //System.out.println("onCharacter  :" + str + "   mod=" + modifiers);
        if (focus != null) {
            focus.characterEvent(str, modifiers);
        } else {
            gform.characterEvent(str, modifiers);
        }

        gform.flush();
    }

    @Override
    public boolean onTouch(long display, int touch, int phase, double x, double y) {
        GForm form = this.gform;
        if (form == null) {
            return true;
        }

        x /= Glfm.glfmGetDisplayScale(display);
        y /= Glfm.glfmGetDisplayScale(display);
        lastX = mouseX;
        lastY = mouseY;
        mouseX = (int) x;
        mouseY = (int) y;

        long cur = System.currentTimeMillis();
        //
        boolean long_touched = false;
//        System.out.println("   touch=" + touch + "   phase=" + phase + "   x=" + x + "   y=" + y);
//            System.out.println("display=" + display + "   win=" + win);
        if (display == display) {

            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan: {//
                    mouseLastPressed = cur;

                    //处理惯性
                    moveStartX = x;
                    moveStartY = y;
                    moveStartAt = System.currentTimeMillis();
                    break;
                }
                case Glfm.GLFMTouchPhaseEnded: {//

                    long cost = System.currentTimeMillis() - moveStartAt;
                    if ((Math.abs(x - moveStartX) > INERTIA_MIN_DISTANCE || Math.abs(y - moveStartY) > INERTIA_MIN_DISTANCE)
                            && cost < INERTIA_MAX_MILLS) {//在短时间内进行了滑动操作
                        form.inertiaEvent((float) moveStartX, (float) moveStartY, (float) x, (float) y, cost);
                    }
                    //检测长按
                    long_touched = cur - mouseLastPressed > LONG_TOUCH_TIME && Math.abs(x - moveStartX) < LONG_TOUCH_MAX_DISTANCE && Math.abs(y - moveStartY) < LONG_TOUCH_MAX_DISTANCE;

                    //处理惯性
                    moveStartX = 0;
                    moveStartY = 0;
                    moveStartAt = 0;
                    break;
                }
                case Glfm.GLFMTouchPhaseMoved: {//
                    form.dragEvent(mouseX - lastX, mouseY - lastY, mouseX, mouseY);
                    break;
                }
                case Glfm.GLFMTouchPhaseHover: {//
                    break;
                }
            }

            //click event
            if (long_touched) {
                form.longTouchedEvent(mouseX, mouseY);
                long_touched = false;
            }
            form.touchEvent(phase, mouseX, mouseY);
        }
        GObject.flush();
        return true;
    }

    @Override
    public void onSurfaceDestroyed(long window) {

    }

    @Override
    public void onSurfaceResize(long window, int width, int height) {
        fbWidth = Glfm.glfmGetDisplayWidth(display);
        fbHeight = Glfm.glfmGetDisplayHeight(display);

        // Calculate pixel ration for hi-dpi devices.
        pxRatio = (float) Glfm.glfmGetDisplayScale(display);
        winWidth = (int) (fbWidth / pxRatio);
        winHeight = (int) (fbHeight / pxRatio);

        if (gform == null) {
            return;
        }
        gform.getBoundle()[GObject.WIDTH] = width;
        gform.getBoundle()[GObject.HEIGHT] = height;
        gform.flush();
    }

    public void onSurfaceError(long display, String description) {
        if (gform == null) {
            return;
        }
        gform.flush();
    }

    @Override
    public void onKeyboardVisible(long display, boolean visible, double x, double y, double w, double h) {
        //System.out.println("keyboardVisableEvent:" + display + "," + visible + "," + x + "," + y + "," + w + "," + h);
        if (gform == null) {
            return;
        }
        x /= Glfm.glfmGetDisplayScale(display);
        y /= Glfm.glfmGetDisplayScale(display);
        w /= Glfm.glfmGetDisplayScale(display);
        h /= Glfm.glfmGetDisplayScale(display);
        gform.KeyboardPopEvent(visible, (float) x, (float) y, (float) w, (float) h);
    }

    @Override
    public void onPhotoPicked(long display, int uid, String url, byte[] data) {
        if (gform == null) {
            return;
        }
        gform.onPhotoPicked(uid, url, data);
    }

    @Override
    public void onAppFocus(long display, boolean focused) {
        if (gform == null) {
            return;
        }
        gform.onAppFocus(focused);
    }

    @Override
    public void onNotify(long display, String key, String val) {
        if (gform == null) {
            return;
        }
        gform.onNotify(key, val);
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

    @Override
    public void onMemWarning(long display) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //==============================
    public String getAppSaveRoot() {
        return Glfm.glfmGetSaveRoot();
    }

    public String getAppResRoot() {
        return Glfm.glfmGetResRoot();
    }

    @Override
    public void init(int w, int h) {
        init();
    }

    @Override
    public void destory() {

    }

}
