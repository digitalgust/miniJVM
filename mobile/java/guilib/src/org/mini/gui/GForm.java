/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import static org.mini.gl.GL.GL_COLOR_BUFFER_BIT;
import static org.mini.gl.GL.GL_DEPTH_BUFFER_BIT;
import static org.mini.gl.GL.GL_STENCIL_BUFFER_BIT;
import org.mini.nanovg.StbFont;
import static org.mini.gl.GL.glClear;
import static org.mini.gl.GL.glClearColor;
import static org.mini.gl.GL.glViewport;
import org.mini.glfm.Glfm;
import org.mini.glfm.GlfmCallBack;
import static org.mini.gui.GToolkit.nvgRGBA;
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
public class GForm extends GPanel {

    String title;
    long display; //glfw win
    long vg; //nk contex
    GuiCallBack callback;
    static StbFont gfont;
    float fps;
    float fpsExpect = 30;
    long last, count;
    //
    float pxRatio;
    int winWidth, winHeight;
    int fbWidth, fbHeight;
    //
    boolean premult;

    Timer timer = new Timer(true);//用于更新画面，UI系统采取按需刷新的原则

    public GForm(String title, int width, int height, GuiCallBack ccb) {
        this.title = title;

        callback = ccb;

        fbWidth = ccb.getFrameBufferWidth();
        fbHeight = ccb.getFrameBufferHeight();
        winWidth = ccb.getDeviceWidth();
        winHeight = ccb.getDeviceHeight();

        pxRatio = ccb.getDeviceRatio();

        setLocation(0, 0);
        setSize(winWidth, winHeight);

    }

//    public void setCallBack(GuiCallBack callback) {
//        this.callback = callback;
//    }
    public GlfmCallBack getCallBack() {
        return this.callback;
    }

    static public void setGFont(StbFont pgfont) {
        gfont = pgfont;
    }

    static public StbFont getGFont() {
        return gfont;
    }

    public long getNvContext() {
        return vg;
    }

    public long getWinContext() {
        return display;
    }

    public int getDeviceWidth() {
        return (int) winWidth;
    }

    public int getDeviceHeight() {
        return (int) winHeight;
    }

    @Override
    public void init() {
        display = callback.getDisplay();
        vg = callback.getNvContext();
        if (vg == 0) {
            System.out.println("callback.getNvContext() is null.");
        }

        String respath = Glfm.glfmGetResRoot();
        System.setProperty("word_font_path", respath + "/resfiles/wqymhei.ttc");
        System.setProperty("icon_font_path", respath + "/resfiles/entypo.ttf");
        System.setProperty("emoji_font_path", respath + "/resfiles/NotoEmoji-Regular.ttf");
        GToolkit.loadFont(vg);

        System.out.println("fbWidth=" + fbWidth + "  ,fbHeight=" + fbHeight);
        flush();
    }

    void display(long vg) {

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

            nvgBeginFrame(vg, winWidth, winHeight, pxRatio);
            drawDebugInfo(vg);
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
        float dx = 10, dy = 40;
        byte[] b;
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 255));

        b = Gutil.toUtf8("touch x,y:" + cb.mouseX + "," + cb.mouseY);
        Nanovg.nvgTextJni(vg, dx, dy, b, 0, b.length);
        dy += font_size;
        b = Gutil.toUtf8("form x,y:" + getX() + "," + getY());
        Nanovg.nvgTextJni(vg, dx, dy, b, 0, b.length);
        dy += font_size;
        if (focus != null) {
            b = Gutil.toUtf8("focus x:" + focus.boundle[LEFT] + " y:" + focus.boundle[TOP] + " w:" + focus.boundle[WIDTH] + " h:" + focus.boundle[HEIGHT]);
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

}
