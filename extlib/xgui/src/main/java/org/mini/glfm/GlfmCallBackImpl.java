/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.glfm;

import org.mini.apploader.AppLoader;
import org.mini.apploader.Sync;
import org.mini.glfw.Glfw;
import org.mini.gui.GCallBack;
import org.mini.gui.GForm;
import org.mini.gui.GObject;
import org.mini.gui.GToolkit;
import org.mini.nanovg.Nanovg;

import static org.mini.nanovg.Nanovg.*;

/**
 * @author Gust
 */
public class GlfmCallBackImpl extends GCallBack {

    long display;
    int winWidth, winHeight;
    int fbWidth, fbHeight;
    double[] insetsDouble = {0, 0, 0, 0};
    float pxRatio;

    public int[] mouseX = new int[Glfm.MAX_SIMULTANEOUS_TOUCHES],
            mouseY = new int[Glfm.MAX_SIMULTANEOUS_TOUCHES],
            lastX = new int[Glfm.MAX_SIMULTANEOUS_TOUCHES],
            lastY = new int[Glfm.MAX_SIMULTANEOUS_TOUCHES];
    long mouseLastPressed;
    int LONG_TOUCH_TIME = 350;
    int LONG_TOUCH_MAX_DISTANCE = 5;//

    int INERTIA_MIN_DISTANCE = 10;//移动距离超过xx单位时可以产生惯性
    int INERTIA_MAX_MILLS = 200;//在300毫秒内的滑动可以产生惯性

    //
    double moveStartX;
    double moveStartY;
    double longStartX;
    double longStartY;
    long moveStartAt;


    long vg;

    float fps;
    float fpsExpect = 60;
    long startAt, cost;
    long last = System.currentTimeMillis(), now;
    int count = 0;

//    static GlfmCallBackImpl instance = new GlfmCallBackImpl();

    /**
     * the glinit method call by native function, glfmapp/main.c
     *
     * @param winContext
     */
    static public void glinit(long winContext) {

        Glfm.glfmSetDisplayConfig(winContext,
                Glfm.GLFMRenderingAPIOpenGLES3,
                Glfm.GLFMColorFormatRGBA8888,
                Glfm.GLFMDepthFormat16,
                Glfm.GLFMStencilFormat8,
                Glfm.GLFMMultisampleNone);

        GCallBack.getInstance().setDisplay(winContext);
        Glfm.glfmSetCallBack(winContext, GCallBack.getInstance());

    }

//    public static GlfmCallBackImpl getInstance() {
//        return instance;
//    }

    @Override
    protected void onFormSet(GForm form) {
        onSurfaceResize(display, getDeviceWidth(), getDeviceHeight());
    }

    private GlfmCallBackImpl() {
    }

    public void setDisplay(long display) {
        this.display = display;
    }


    void setForm(GForm form) {
        gform = form;
    }


    /**
     * @return the fps
     */
    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        fpsExpect = fps;
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
        AppLoader.cb_init();

    }

    @Override
    public void onRender(long display) {
        try {
//            startAt = System.currentTimeMillis();
            if (gapp == null) {
                return;
            }
            try {
                Thread.currentThread().setContextClassLoader(gapp.getClass().getClassLoader());//there were be an app pause and the other app setup
                gform = gapp.getForm();
                if (!gform.isInited()) {
                    gform.cb_init();
                    gapp.startApp();
                }
            } catch (Exception e) {
                gapp.closeApp();
                GForm.addMessage("Init error : " + e.getMessage());
                e.printStackTrace();
            }
            if (gform.flushReq()) {
                if (gform != null) {
                    gform.display(vg);
                }
            }
            //
            count++;
            now = System.currentTimeMillis();
            if (now - last > 1000) {
                //System.out.println("fps:" + count);
                fps = count;
                last = now;
                count = 0;
            }
//
//            cost = now - startAt;
            Glfm.glfmSwapBuffers(display);
            Sync.sync((int) fpsExpect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceCreated(long display, int width, int height) {
        init();
        System.out.println("onSurfaceCreated " + width + "," + height + "," + pxRatio);
        Glfm.glfmSetMultitouchEnabled(display, true);
        Glfm.glfmGetDisplayChromeInsets(display, insetsDouble);
    }

    @Override
    public boolean onKey(long display, int keyCode, int action, int modifiers) {
        if (gform == null) {
            return true;
        }
        GObject focus = gform.getFocus();
        //System.out.println("keyCode  :" + keyCode + "   action=" + action + "   modifiers=" + modifiers);
        if (focus != null) {
            focus.keyEventGlfm(keyCode, action, modifiers);
        } else {
            gform.keyEventGlfm(keyCode, action, modifiers);
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

    public int getTouchOrMouseX() {
        return mouseX[0];
    }

    public int getTouchOrMouseY() {
        return mouseY[0];
    }

    @Override
    public boolean onTouch(long display, int touch, int phase, double x, double y) {
        GForm form = this.gform;
        if (form == null) {
            return true;
        }
        touch += Glfw.GLFW_MOUSE_BUTTON_1; //convert touchid to mouse button

        x /= Glfm.glfmGetDisplayScale(display);
        y /= Glfm.glfmGetDisplayScale(display);

        //form maybe translate when keyboard popup
//        x += gform.getX();
//        y += gform.getY();

        lastX[touch] = mouseX[touch];
        lastY[touch] = mouseY[touch];
        mouseX[touch] = (int) x;
        mouseY[touch] = (int) y;

        long cur = System.currentTimeMillis();
        //
        boolean long_touched = false;
        //System.out.println("   touch=" + touch + "   phase=" + phase + "   x=" + x + "   y=" + y + "display=" + display + "   win=" + form);
        if (this.display == display) {

            switch (phase) {
                case Glfm.GLFMTouchPhaseBegan: {//
                    mouseLastPressed = cur;

                    //处理惯性
                    longStartX = moveStartX = x;
                    longStartY = moveStartY = y;
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
                    long_touched = cur - mouseLastPressed > LONG_TOUCH_TIME && Math.abs(x - longStartX) < LONG_TOUCH_MAX_DISTANCE && Math.abs(y - longStartY) < LONG_TOUCH_MAX_DISTANCE;

                    //处理惯性
                    moveStartX = 0;
                    moveStartY = 0;
                    moveStartAt = 0;
                    break;
                }
                case Glfm.GLFMTouchPhaseMoved: {//
                    form.dragEvent(touch, mouseX[touch] - lastX[touch], mouseY[touch] - lastY[touch], mouseX[touch], mouseY[touch]);
                    long cost = System.currentTimeMillis() - moveStartAt;
                    if (cost > INERTIA_MAX_MILLS) {//reset
                        moveStartX = x;
                        moveStartY = y;
                        moveStartAt = System.currentTimeMillis();
                    }
                    break;
                }
                case Glfm.GLFMTouchPhaseHover: {//
                    break;
                }
            }

            //click event
            if (long_touched) {
                form.longTouchedEvent(mouseX[touch], mouseY[touch]);
                long_touched = false;
            }
            form.touchEvent(touch, phase, mouseX[touch], mouseY[touch]);
        }
        GForm.flush();
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

        Glfm.glfmGetDisplayChromeInsets(display, insetsDouble);

        if (gform == null) {
            return;
        }
        //System.out.println(width + "," + height + "," + pxRatio);
        //System.out.println(winWidth + "," + winHeight);
        gform.setSize(winWidth, winHeight);
        gform.onDeviceSizeChanged(winWidth, winHeight);
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
        gform.onDeviceNotify(key, val);
    }

    @Override
    public void onOrientationChanged(long display, int orientation) {
    }

    public void getInsets(float[] top_right_bottom_left) {
        if (top_right_bottom_left != null) {
            top_right_bottom_left[0] = (float) (insetsDouble[0] / pxRatio);
            top_right_bottom_left[1] = (float) (insetsDouble[1] / pxRatio);
            top_right_bottom_left[2] = (float) (insetsDouble[2] / pxRatio);
            top_right_bottom_left[3] = (float) (insetsDouble[3] / pxRatio);
        }
    }

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
    public void destroy() {
        if (vg != 0) {
            Nanovg.nvgDeleteGL3(vg);
        }
    }


}
