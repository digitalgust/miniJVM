package org.mini.gui.callback;

import org.mini.apploader.GApplication;
import org.mini.glwrap.GLUtil;
import org.mini.gui.*;
import org.mini.nanovg.Nanovg;

import static org.mini.gl.GL.*;
import static org.mini.gl.GL.GL_STENCIL_BUFFER_BIT;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;


/**
 * 桌面，屏幕上只有一个
 *
 * @author Gust
 */
public class GDesktop extends GPanel implements GCallbackUI {


    protected String title;
    protected long vg; //nk contex
    protected float pxRatio;
    static GCmdHandler cmdHandler;
    GCallBack callback;
    public static byte flush = 3;
    GForm curForm;

    GDesktop(GCallBack callback) {
        super(null);
        this.callback = callback;
        setLocation(0, 0);
        cmdHandler = new GCmdHandler();
        add(cmdHandler);
    }


    public void setTitle(String title) {
        callback.setDisplayTitle(title);
    }

    public String getTitle() {
        return title;
    }

    public void checkAppRun(GApplication gapp) {
        if (gapp == null) return;

        GForm gform = gapp.getForm();
        if (gform != curForm) {
            //here can not using this.remove() and this.add(),
            // it would call curForm.init() and curForm.destroy()
            elements.remove(curForm);
            curForm = gform;
            elements.add(0, curForm);

            curForm.setSize(GCallBack.getInstance().getDeviceWidth(), GCallBack.getInstance().getDeviceHeight());

        }
        if (!curForm.isInited()) {
            curForm.cb_init();
            gapp.startApp();
        }
    }


    public void display(long vg) {

        try {
            // Update and render
            pxRatio = callback.getDeviceRatio();
            int fbWidth, fbHeight;
            fbWidth = callback.getFrameBufferWidth();
            fbHeight = callback.getFrameBufferHeight();
            glViewport(0, 0, fbWidth, fbHeight);
            float[] bgc = getBgColor();
            glClearColor(bgc[0], bgc[1], bgc[2], bgc[3]);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

            int winWidth, winHeight;
            winWidth = callback.getDeviceWidth();
            winHeight = callback.getDeviceHeight();

            nvgBeginFrame(vg, winWidth, winHeight, pxRatio);
            //drawDebugInfo(vg);
            Nanovg.nvgReset(vg);
            Nanovg.nvgResetScissor(vg);
            Nanovg.nvgScissor(vg, 0, 0, winWidth, winHeight);
            paint(vg);
            nvgEndFrame(vg);
            try {
                cmdHandler.process(); //GOpenGLPanel.gl_paint() here be called
            } catch (Exception e) {
                e.printStackTrace();
            }
            //
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized public boolean flushReq() {
        if (flush > 0) {
            flush--;
            return true;
        }
        return false;
    }

    public static void flush() {
        flush = 3;
        //in android may flush before paint,so the menu not shown
    }

    void drawDebugInfo(long vg) {
        float font_size = 15;
        nvgFontSize(vg, font_size);
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, Nanovg.NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        float dx = 2, dy = 40;
        byte[] b;
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 255));

        dy += font_size;
        b = GLUtil.toCstyleBytes("form:" + getX() + "," + getY() + "," + getW() + "," + getH() + "  " + getInnerX() + "," + getInnerY() + "," + getInnerW() + "," + getInnerH());

        Nanovg.nvgTextJni(vg, dx, dy, b, 0, b.length);
        dy += font_size;
        if (current != null) {
            b = GLUtil.toCstyleBytes("focus:" + current.getX() + "," + current.getY() + "," + current.getW() + "," + current.getH() + "  " + ((current instanceof GContainer) ? ((GContainer) current).getInnerX() + "," + ((GContainer) current).getInnerY() + "," + ((GContainer) current).getInnerW() + "," + ((GContainer) current).getInnerH() : ""));
            Nanovg.nvgTextJni(vg, dx, dy, b, 0, b.length);
        }
    }


    public void onDeviceSizeChanged(int width, int height) {
        setSize(width, height);
        if (curForm != null) {
            curForm.setSize(width, height);
            curForm.onDeviceSizeChanged(width, height);
        }
    }


    @Override
    public GForm getForm() {
        return curForm;
    }

    public static void addCmd(int cmd) {
        cmdHandler.addCmd(cmd);
    }

    public static void addCmd(GCmd cmd) {
        cmdHandler.addCmd(cmd);
    }

    public static void deleteImage(int texture) {
        cmdHandler.addCmd(() -> {
            Nanovg.nvgDeleteImage(GCallBack.getInstance().getNvContext(), texture);
        });
    }

    /**
     * @param s
     */
    public static void addMessage(String s) {
        cmdHandler.addCmd(s, null);
    }

    public void addMessage(String s, Runnable work) {
        cmdHandler.addCmd(s, work);
    }

    public static void clearMessage() {
        cmdHandler.addCmd(GCmd.GCMD_CLEAR_MESSAGE);
    }

    public float getRatio() {
        return pxRatio;
    }

    public void KeyboardPopEvent(boolean visible, float x, float y, float w, float h) {
        getForm().KeyboardPopEvent(visible, x, y, w, h);
    }

    public void onPhotoPicked(int uid, String url, byte[] data) {
        getForm().onPhotoPicked(uid, url, data);
    }

    public void onAppFocus(boolean focused) {
        getForm().onAppFocus(focused);
    }

    public void onDeviceNotify(String key, String val) {
        getForm().onDeviceNotify(key, val);
    }
}