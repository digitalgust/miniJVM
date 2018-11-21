/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.gui.impl.GuiCallBack;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import static org.mini.gl.GL.GL_COLOR_BUFFER_BIT;
import static org.mini.gl.GL.GL_DEPTH_BUFFER_BIT;
import static org.mini.gl.GL.GL_STENCIL_BUFFER_BIT;
import static org.mini.gl.GL.glClear;
import org.mini.nanovg.StbFont;
import static org.mini.gl.GL.glClearColor;
import static org.mini.gl.GL.glViewport;
import org.mini.glfm.Glfm;
import static org.mini.gui.GObject.TYPE_FORM;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GToolkit.nvgRGBA;
import org.mini.gui.event.GAppActiveListener;
import org.mini.gui.event.GKeyboardShowListener;
import org.mini.gui.event.GNotifyListener;
import org.mini.gui.event.GPhotoPickedListener;
import org.mini.nanovg.Gutil;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.NVG_ALIGN_MIDDLE;
import static org.mini.nanovg.Nanovg.nvgBeginFrame;
import static org.mini.nanovg.Nanovg.nvgEndFrame;
import static org.mini.nanovg.Nanovg.nvgFillColor;
import static org.mini.nanovg.Nanovg.nvgFontFace;
import static org.mini.nanovg.Nanovg.nvgFontSize;
import static org.mini.nanovg.Nanovg.nvgTextAlign;

/**
 *
 * @author gust
 */
public class GForm extends GViewPort {

    String title;
    long display; //glfw display
    long vg; //nk contex
    GuiCallBack callback;
    static StbFont gfont;
    float fps;
    float fpsExpect = 30;
    long last, count;
    //
    float pxRatio;

    int fbWidth, fbHeight;
    //
    boolean premult;

    GPhotoPickedListener pickListener;
    GKeyboardShowListener keyshowListener;
    GAppActiveListener activeListener;
    GNotifyListener notifyListener;

    final static List<Integer> pendingDeleteImage = new ArrayList();

    static Timer timer = new Timer(true);//用于更新画面，UI系统采取按需刷新的原则

    public GForm(GuiCallBack ccb) {
        this.title = title;

        callback = ccb;

        fbWidth = ccb.getFrameBufferWidth();
        fbHeight = ccb.getFrameBufferHeight();
        int winWidth, winHeight;
        winWidth = ccb.getDeviceWidth();
        winHeight = ccb.getDeviceHeight();

        pxRatio = ccb.getDeviceRatio();

        setLocation(0, 0);
        setSize(winWidth, winHeight);

        setViewLocation(0, 0);
        setViewSize(winWidth, winHeight);

    }

    public int getType() {
        return TYPE_FORM;
    }

    public GuiCallBack getCallBack() {
        return this.callback;
    }

    static public void setGFont(StbFont pgfont) {
        gfont = pgfont;
    }

    static public StbFont getGFont() {
        return gfont;
    }

    public long getNvContext() {
        return callback.getNvContext();
    }

    public long getWinContext() {
        return display;
    }

    public int getDeviceWidth() {
        return (int) callback.getDeviceWidth();
    }

    public int getDeviceHeight() {
        return (int) callback.getDeviceHeight();
    }

    public void setTitle(String title) {
        callback.setDisplayTitle(title);
    }

    @Override
    public void init() {
        display = callback.getDisplay();
        vg = callback.getNvContext();
        if (vg == 0) {
            System.out.println("callback.getNvContext() is null.");
        }

        //System.out.println("fbWidth=" + fbWidth + "  ,fbHeight=" + fbHeight);
        flush();
    }

    public void display(long vg) {

        long startAt, endAt, cost;
        try {
            startAt = System.currentTimeMillis();

            // Update and render
            glViewport(0, 0, fbWidth, fbHeight);
            if (premult) {
                glClearColor(0, 0, 0, 0);
            } else {
                glClearColor(0.3f, 0.3f, 0.32f, 1.0f);
            }
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

            int winWidth, winHeight;
            winWidth = callback.getDeviceWidth();
            winHeight = callback.getDeviceHeight();

            nvgBeginFrame(vg, winWidth, winHeight, pxRatio);
            //drawDebugInfo(vg);
            Nanovg.nvgReset(vg);
            Nanovg.nvgResetScissor(vg);
            Nanovg.nvgScissor(vg, 0, 0, winWidth, winHeight);
            update(vg);
            nvgEndFrame(vg);

            //
            count++;
            endAt = System.currentTimeMillis();
            cost = endAt - startAt;
            if (cost > 1000) {
                //System.out.println("fps:" + count);
                fps = count;
                last = endAt;
                count = 0;
            }
//                if (cost < 1000 / fpsExpect) {
//                    Thread.sleep((long) (1000 / fpsExpect - cost));
//                }
            synchronized (pendingDeleteImage) {
                for (int i = pendingDeleteImage.size() - 1; i >= 0; i--) {
                    Integer tex = pendingDeleteImage.get(i);
                    if (tex != null) {
                        Nanovg.nvgDeleteImage(vg, tex);
                        System.out.println("delete image " + tex);
                    }
                }
                pendingDeleteImage.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void drawDebugInfo(long vg) {
        float font_size = 15;
        nvgFontSize(vg, font_size);
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, Nanovg.NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        GuiCallBack cb = (GuiCallBack) callback;
        float dx = 2, dy = 40;
        byte[] b;
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 255));

        b = Gutil.toUtf8("touch x,y:" + cb.mouseX + "," + cb.mouseY);
        Nanovg.nvgTextJni(vg, dx, dy, b, 0, b.length);
        dy += font_size;
        b = Gutil.toUtf8("form:" + getX() + "," + getY() + "," + getW() + "," + getH() + "  " + getViewX() + "," + getViewY() + "," + getViewW() + "," + getViewH());

        Nanovg.nvgTextJni(vg, dx, dy, b, 0, b.length);
        dy += font_size;
        if (focus != null) {
            b = Gutil.toUtf8("focus:" + focus.getX() + "," + focus.getY() + "," + focus.getW() + "," + focus.getH() + "  " + ((focus instanceof GContainer) ? ((GContainer) focus).getViewX() + "," + ((GContainer) focus).getViewY() + "," + ((GContainer) focus).getViewW() + "," + ((GContainer) focus).getViewH() : ""));
            Nanovg.nvgTextJni(vg, dx, dy, b, 0, b.length);
        }
    }

    TimerTask tt_OnTouch = new TimerTask() {
        public void run() {
            flush();
        }
    };

    void tt_setupOnTouch() {
        timer.schedule(tt_OnTouch, 0L);//, (long) (1000 / fpsExpect));
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

    public void KeyboardPopEvent(boolean visible, float x, float y, float w, float h) {
        if (keyshowListener != null) {
            keyshowListener.keyboardShow(visible, x, y, w, h);
        }
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

    public static void deleteImage(int texture) {
        synchronized (pendingDeleteImage) {
            pendingDeleteImage.add(texture);
        }
    }

    public void onAppFocus(boolean focus) {
        //System.out.println("app focus:" + focus);
        if (activeListener != null) {
            activeListener.onAppActive(focus);
        }
    }

    public void onNotify(String key, String val) {
        if (notifyListener != null) {
            notifyListener.onNotify(key, val);
        }
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

}
