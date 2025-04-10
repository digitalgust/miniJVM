package org.mini.gui.callback;

import org.mini.apploader.AppManager;
import org.mini.apploader.GApplication;
import org.mini.glwrap.GLUtil;
import org.mini.gui.*;
import org.mini.gui.gscript.Interpreter;
import org.mini.layout.guilib.GuiScriptLib;
import org.mini.nanovg.Nanovg;

import static org.mini.gl.GL.*;
import static org.mini.gl.GL.GL_STENCIL_BUFFER_BIT;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.gui.event.GNotifyListener.NOTIFY_KEY_DEVICE_TOKEN;
import static org.mini.gui.event.GNotifyListener.NOTIFY_KEY_IOS_PURCHASE;
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
    public static byte flush = 4;
    GForm curForm;
    //==================
    long splashStartAt = 0; //开机屏
    GImage splashImg;
    public static final int SPLASH_MILLIS = 2000;

    GDesktop(GCallBack callback) {
        super(null);
        this.callback = callback;
        setLocation(0, 0);
        cmdHandler = new GCmdHandler();
        add(cmdHandler);
        splashImg = GToolkit.getCachedImageFromJar("/res/clogo.png");
    }


    public void setTitle(String title) {
        callback.setDisplayTitle(title);
    }

    public String getTitle() {
        return title;
    }

    public void checkAppRun(GApplication gapp) {
        if (gapp == null) return;

        gapp.init();

        GForm gform = gapp.getForm();
        if (gform == null) {
            return;
        }
        if (gform != curForm) {
            //here can not using this.remove() and this.add(),
            // it would call curForm.init() and curForm.destroy()
            elements.remove(curForm);
            curForm = gform;
            elements.add(0, curForm);
            if (!curForm.isInited()) {
                curForm.init();
            }

            curForm.setSize(GCallBack.getInstance().getDeviceWidth(), GCallBack.getInstance().getDeviceHeight());

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
            //
            paint(vg);

            // draw open screen
            if (splashImg != null) {
                if (splashStartAt == 0) {
                    splashStartAt = System.currentTimeMillis();
                }

                GToolkit.drawRect(vg, 0, 0, winWidth, winHeight, getBgColor(), true);
                float imgW = 128f;
                float imgH = 128f;
                float dx = (winWidth - imgW) * 0.5f;
                float dy = (winHeight - imgH) * 0.5f;
                float alpha = (float) (System.currentTimeMillis() - splashStartAt) / (SPLASH_MILLIS / 2f);//淡入
                if (System.currentTimeMillis() - splashStartAt > SPLASH_MILLIS / 2f) {
                    alpha = 1f - (alpha - 1f);//淡出
                }
                GToolkit.drawImage(vg, splashImg, dx, dy, imgW, imgH, false, alpha, 0f);
                flush();
                if (System.currentTimeMillis() - splashStartAt > 2000) {
                    splashImg = null;
                }
            }
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
        flush = 4;
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

    public static void addCmd(Runnable work) {
        cmdHandler.addCmd(work);
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
        try {
            switch (key) {
                case NOTIFY_KEY_DEVICE_TOKEN:
                    if (val != null) {
                        int start = val.indexOf('=');
                        int end = val.indexOf('}');
                        if (start >= 0 && end > 0) {
                            val = val.substring(start + 1, end);
                            val = val.trim();
                        }
                        System.setProperty("device.token", val);
                    }
                    break;
                case NOTIFY_KEY_IOS_PURCHASE:
                    if (val.indexOf(':') > 0) {
                        String[] ss = val.split(":");
                        if (ss.length > 2) {
                            int code = Integer.parseInt(ss[0]);
                            String receipt = ss[1];
                            byte[] scriptBytes = javax.microedition.io.Base64.decode(ss[2]);
                            String script = new String(scriptBytes, "utf-8");
                            //System.out.println("script:" + script);
                            Interpreter inp = new Interpreter();
                            inp.reglib(new GuiScriptLib(AppManager.getInstance()));
                            inp.loadFromString(script);
                            inp.putGlobalVar("iap_code", Interpreter.getCachedInt(code));
                            inp.putGlobalVar("iap_receipt", Interpreter.getCachedStr(receipt));
                            inp.start();
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        getForm().onDeviceNotify(key, val);
    }
}