package org.mini.glfw;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.mini.apploader.AppLoader;
import org.mini.apploader.Sync;
import org.mini.glfm.Glfm;
import org.mini.glwrap.GLUtil;
import org.mini.gui.GFrame;
import org.mini.gui.GLanguage;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GDesktop;
import org.mini.gui.GForm;
import org.mini.gui.GToolkit;
import org.mini.gui.event.GPhotoPickedListener;
import org.mini.media.MaDevice;
import org.mini.util.SysLog;

import java.io.File;
import java.io.FileFilter;

import static org.mini.gl.GL.GL_TRUE;
import static org.mini.glfw.Glfw.*;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author Gust
 */
public class GlfwCallBackImpl extends GCallBack {


    long display;

    int winWidth, winHeight;
    int fbWidth, fbHeight;
    float pxRatio;

    public int mouseX, mouseY, button;
    long mouseLastPressed;
    int clickCount = 0;
    int CLICK_PERIOD = 200;

    boolean drag;
    int hoverX, hoverY;//mouse
    int buttonOnDrag;

    long vg;

    //not in mobile
    float fps;
    float fpsExpect = FPS_DEFAULT;

    Thread openglThread;

    public GlfwCallBackImpl() {
    }

    public void setDisplay(long display) {
        this.display = display;
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
        return "./";
    }

    public void setDisplayTitle(String title) {
        Glfw.glfwSetWindowTitle(display, title);
    }

    public Thread getOpenglThread() {
        return openglThread;
    }

    public void init(int width, int height) {
        this.winWidth = width;
        this.winHeight = height;

        if (!Glfw.glfwInit()) {
            SysLog.error("glfw init error.");
            System.exit(1);
        }
        String osname = System.getProperty("os.name");
        if (osname != null) {
            if (osname.contains("Mac")) {
                Glfw.glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
                Glfw.glfwWindowHint(Glfw.GLFW_COCOA_RETINA_FRAMEBUFFER, GL_TRUE);
            } else if (osname.contains("Linux")) {
                Glfw.glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            }
        }
        //Glfw.glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);
        Glfw.glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        Glfw.glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        Glfw.glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        Glfw.glfwWindowHint(GLFW_DEPTH_BITS, 16);
        Glfw.glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);

        display = Glfw.glfwCreateWindow(winWidth, winHeight, GLUtil.toCstyleBytes(""), 0, 0);
        if (display == 0) {
            Glfw.glfwTerminate();
            System.exit(1);
        }
        Glfw.glfwSetCallback(display, this);
        Glfw.glfwMakeContextCurrent(display);
        Glfw.glfwSwapInterval(1);
        reloadWindow();
        //SysLog.info("fbWidth=" + fbWidth + "  ,fbHeight=" + fbHeight);
        openglThread = Thread.currentThread();

        vg = nvgCreateGL3(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == 0) {
            SysLog.error("Could not init nanovg.\n");
        } else {
            SysLog.info("nanovg success.");
        }

        GToolkit.FontHolder.loadFont(vg);
        AppLoader.cb_init();
    }

    @Override
    public void mainLoop() {
        //
        long last = System.currentTimeMillis(), now;
        int count = 0;

        long startAt, cost;
        while (!Glfw.glfwWindowShouldClose(display)) {
            try {
                if (gapp == null) {
                    return;
                }
                try {
                    Thread.currentThread().setContextClassLoader(gapp.getClass().getClassLoader());
                    desktop.checkAppRun(gapp);
                } catch (Exception e) {
                    gapp.closeApp();
                    desktop.addMessage("Init error: " + e.getMessage());
                    e.printStackTrace();
                }
                startAt = System.currentTimeMillis();
                //user define contents
                if (desktop.flushReq()) {
                    desktop.display(vg);
                    //GToolkit.drawText(vg, 0, 0, 200, 30, mouseX + " , " + mouseY);
                    Glfw.glfwSwapBuffers(display);
                }
                Glfw.glfwPollEvents();
                count++;
                now = System.currentTimeMillis();
                if (now - last > 1000) {
                    //System.out.println("fps:" + count);
                    fps = count;
                    last = now;
                    count = 0;
                }

                cost = now - startAt;
                //System.out.println(cost);
//                if (cost < 1000 / fpsExpect) {
//                    Thread.sleep((long) (1000 / fpsExpect - cost));
//                }
                Sync.sync((int) fpsExpect);
            } catch (Exception ex) {
                ex.printStackTrace();

            }
        }
    }

    public void destroy() {
        nvgDeleteGL3(vg);
        Glfw.glfwTerminate();
        try {
            Class.forName("org.mini.media.MaDevice");
            MaDevice.stopAll();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        vg = 0;
        System.exit(0);//some thread not exit ,that will continue running
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

    @Override
    public void key(long window, int key, int scancode, int action, int mods) {
        try {
            if (desktop == null) {
                return;
            }
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                Glfw.glfwSetWindowShouldClose(window, GLFW_TRUE);
            }
            desktop.keyEventGlfw(key, scancode, action, mods);
            GDesktop.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public void character(long window, char character) {
        try {
            if (desktop == null) {
                return;
            }
            desktop.characterEvent(character);
            GDesktop.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    public int getTouchOrMouseX() {
        return mouseX;
    }

    public int getTouchOrMouseY() {
        return mouseY;
    }

    @Override
    public void mouseButton(long window, int button, boolean pressed) {
        try {
            if (desktop == null) {
                return;
            }
            if (window == display) {
                switch (button) {
                    case Glfw.GLFW_MOUSE_BUTTON_1: {//left
                        if (pressed) {
                            drag = true;
                            hoverX = mouseX;
                            hoverY = mouseY;
                            buttonOnDrag = button;
                        } else {
                            drag = false;
                        }
                        break;
                    }
                    case Glfw.GLFW_MOUSE_BUTTON_2: {//right
                        if (pressed) {
                            drag = true;
                            hoverX = mouseX;
                            hoverY = mouseY;
                            buttonOnDrag = button;
                            //gform.longTouchedEvent(mouseX, mouseY);
                        } else {
                            drag = false;
                        }
                        break;
                    }
                    case Glfw.GLFW_MOUSE_BUTTON_3: {//middle
                        break;
                    }
                }
                desktop.mouseButtonEvent(button, pressed, mouseX, mouseY);
                //click event
                long cur = System.currentTimeMillis();
                if (pressed) {
                    if ((cur - mouseLastPressed < CLICK_PERIOD) && (this.button == button)) {
                        clickCount++;
                    } else {
                        clickCount = 0;
                    }
                    this.button = button;
                    mouseLastPressed = cur;
                }
                if (!pressed) {
                    if (clickCount > 0) {
                        desktop.clickEvent(button, mouseX, mouseY);
                        clickCount = 0;
                    }
                }
            }
            GDesktop.flush();
            //System.out.println("flushed");
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public void scroll(long window, double scrollX, double scrollY) {
        try {
            if (desktop == null) {
                return;
            }
            desktop.scrollEvent((float) scrollX * 10, (float) scrollY * 10, mouseX, mouseY);
            GDesktop.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public void cursorPos(long window, int x, int y) {
        try {
            if (desktop == null) {
                return;
            }
            //form maybe translate when keyboard popup
            x += desktop.getX();
            y += desktop.getY();

            if (display == window) {
                mouseX = x;
                mouseY = y;
                desktop.cursorPosEvent(x, y);
                if (drag) {
                    desktop.dragEvent(buttonOnDrag, x - hoverX, y - hoverY, x, y);
                    hoverX = mouseX;
                    hoverY = mouseY;
                }
                GDesktop.flush();
            }
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public boolean windowClose(long window) {
        try {
            if (desktop == null) {
                return true;
            }
            GDesktop.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
        return true;
    }

    private void reloadWindow() {
        try {
            winWidth = Glfw.glfwGetWindowWidth(display);
            winHeight = Glfw.glfwGetWindowHeight(display);
            fbWidth = Glfw.glfwGetFramebufferWidth(display);
            fbHeight = Glfw.glfwGetFramebufferHeight(display);
            // Calculate pixel ration for hi-dpi devices.
            pxRatio = (float) fbWidth / (float) winWidth;
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public void windowSize(long window, int width, int height) {
        //System.out.println("windowsize" + width + "," + height);
        try {
            if (winWidth == width && winHeight == height) {
                return;
            }
            reloadWindow();

            if (desktop == null) {
                return;
            }
            desktop.setSize(width, height);
            desktop.onDeviceSizeChanged(width, height);
            GDesktop.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public void framebufferSize(long window, int x, int y) {
        try {
            if (desktop == null) {
                return;
            }
            desktop.setSize(x, y);
            GDesktop.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public void drop(long window, int count, String[] paths) {
        try {
            if (desktop == null) {
                return;
            }
            desktop.dropEvent(count, paths);
            GDesktop.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    public void error(int error, String description) {
        SysLog.error("error: " + error + " message: " + description);
    }

    @Override
    public void monitor(long monitor, boolean connected) {
    }

    @Override
    public void windowPos(long window, int x, int y) {
    }

    @Override
    public void windowRefresh(long window) {
    }

    @Override
    public void windowFocus(long window, boolean focused) {
    }

    @Override
    public void windowIconify(long window, boolean iconified) {
    }

    @Override
    public void cursorEnter(long window, boolean entered) {
    }

    //==============================
    public void getInsets(float[] top_right_bottom_left) {
    }

    @Override
    public String getAppSaveRoot() {
        return new File("./").getAbsolutePath();
    }

    @Override
    public String getAppResRoot() {
        return new File("./").getAbsolutePath();
    }

    /**
     * 当窗口从一种分辨率进入另一种分辨率时,会重设form
     *
     * @param form
     */
    @Override
    protected void onFormSet(GForm form) {
        windowSize(display, getDeviceWidth(), getDeviceHeight());
    }

    static FileFilter imagefilter = new FileFilter() {
        String[] extensions = new String[]{".jpg", ".png", ".gif", ".jpeg"};

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String name = f.getName();
            for (String ext : extensions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    };

    static FileFilter moviefilter = new FileFilter() {
        String[] extensions = new String[]{".mp4", ".avi", ".rmvb", ".rm", ".flv", ".wmv", ".mkv", ".mpg", ".mpeg"};

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String name = f.getName();
            for (String ext : extensions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    };

    static FileFilter bothFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (imagefilter.accept(f)) {
                return true;
            } else if (moviefilter.accept(f)) {
                return true;
            } else {
                return false;
            }
        }
    };

    public void pickPhoto(int uid, int deviceAndType) {
        FileFilter filter = null;
        if (((deviceAndType & PICK_PHOTO_TYPE_IMAGE) != 0) && ((deviceAndType & PICK_PHOTO_TYPE_MOIVE) != 0)) {
            filter = bothFilter;
        } else if ((deviceAndType & PICK_PHOTO_TYPE_IMAGE) != 0) {
            filter = imagefilter;
        } else if ((deviceAndType & PICK_PHOTO_TYPE_MOIVE) != 0) {
            filter = moviefilter;
        }
        if ((deviceAndType & PICK_PHOTO_DEVICE_ALBUM) != 0 || (deviceAndType & PICK_PHOTO_DEVICE_CAMERA) != 0) {
            GForm form = getDesktop().getForm();
            if (form == null) {
                SysLog.error("pickPhoto form is null");
                return;
            }
            GFrame frame = GToolkit.getFileChooser(
                    form,
                    GLanguage.getString(null, "Pick Media File"),
                    null,
                    filter,
                    false,
                    getDesktop().getW() * 0.8f,
                    getDesktop().getH() * 0.8f,
                    gobj1 -> {
                        GPhotoPickedListener pickedListener = form.getPickListener();
                        if (pickedListener != null) {
                            String path = GToolkit.getCompText(gobj1.getFrame(), GToolkit.NAME_FILECHOOSER_PATH);
                            pickedListener.onPicked(uid, path, null);
                        }
                    },
                    null);
            GToolkit.showFrame(frame);
        } else {
            SysLog.error(String.format("pickPhoto device not support ,only album(0x%x) or camera(0x%x)", PICK_PHOTO_DEVICE_ALBUM, PICK_PHOTO_DEVICE_CAMERA));
        }
    }

    public void playeVideo(String url, String mimeType) {
        Glfm.glfmOpenOtherApp(GLUtil.toCstyleBytes(url), GLUtil.toCstyleBytes(mimeType), 0);
    }
}
