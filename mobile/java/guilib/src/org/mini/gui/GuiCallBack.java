/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfm.GlfmCallBackAdapter;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ANTIALIAS;
import static org.mini.nanovg.Nanovg.NVG_DEBUG;
import static org.mini.nanovg.Nanovg.NVG_STENCIL_STROKES;

/**
 *
 * @author Gust
 */
public class GuiCallBack extends GlfmCallBackAdapter {

    long display;
    int winWidth, winHeight;
    int fbWidth, fbHeight;
    float pxRatio;

    int mouseX, mouseY, lastX, lastY;
    long mouseLastPressed;
    int LONG_TOUCH_TIME = 600;
    int LONG_TOUCH_MAX_DISTANCE = 5;//移动距离超过40单位时可以产生惯性

    int INERTIA_MIN_DISTANCE = 20;//移动距离超过40单位时可以产生惯性
    int INERTIA_MAX_MILLS = 300;//在300毫秒内的滑动可以产生惯性

    //
    double moveStartX;
    double moveStartY;
    long moveStartAt;

    GApplication app;

    GForm gform;

    long vg;

    public GuiCallBack(long display, GApplication ap) {
        this.display = display;
        setApplication(ap);
    }

    public final void setApplication(GApplication ap) {
        this.app = ap;
        gform = null;
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

    void init() {
        fbWidth = Glfm.glfmGetDisplayWidth(display);
        fbHeight = Glfm.glfmGetDisplayHeight(display);
        // Calculate pixel ration for hi-dpi devices.
        pxRatio = (float) Glfm.glfmGetDisplayScale(display);
        winWidth = (int) (fbWidth / pxRatio);
        winHeight = (int) (fbHeight / pxRatio);

        vg = Nanovg.nvgCreateGLES2(NVG_ANTIALIAS | NVG_STENCIL_STROKES | NVG_DEBUG);
        if (vg == 0) {
            System.out.println("Could not init nanovg.\n");
        } else {
            System.out.println("nanovg success.");
        }

    }

    @Override
    public void mainLoop(long display, double frameTime) {

        try {
            if (gform == null) {
                gform = app.createdForm(this);
                if (gform != null) {
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
        if (gform == null) {
            return;
        }
        gform.boundle[GObject.WIDTH] = width;
        gform.boundle[GObject.HEIGHT] = height;
        gform.flush();
    }

    public void onSurfaceError(long display, String description) {
        if (gform == null) {
            return;
        }
        gform.flush();
    }
}
