package org.mini.apploader;

import org.mini.glfm.Glfm;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.event.GActionListener;
import org.mini.gui.callback.GCallbackUI;

import static org.mini.nanovg.Nanovg.*;

public class GHomeButton extends GPanel implements GActionListener, GCallbackUI {
    public static final float DEF_X = 16f, DEF_Y = 40f;
    public static final float DEF_W = 32f, DEF_H = 32f;
    public static final float ICON_WH = 8f, PAD = 0f;
    public static final String FLOAT_HOME_BOTTOM = "FLOAT_HOME_BOTTOM";

    GImage butImg = GImage.createImageFromJar("/res/ui/home.png");
    GImage downImg = GImage.createImageFromJar("/res/ui/yellow.png");
    //    GImage uploadImg = GImage.createImageFromJar("/res/ui/green.png");
//    GImage msgImg = GImage.createImageFromJar("/res/ui/red.png");
    GImage srvImg = GImage.createImageFromJar("/res/ui/blue.png");

    boolean drag = false;
    boolean moved = false;// is moved the button

    int cIdx = 0;
    long markEndAt = 0;
    float[] inset = new float[4];

    public GHomeButton() {
        super(null, GCallBack.getInstance().getDeviceWidth() * .5f, GCallBack.getInstance().getDeviceHeight() * .5f, DEF_W, DEF_H);
        setName(FLOAT_HOME_BOTTOM);
        int saveX = AppLoader.getHomeIconX();
        int saveY = AppLoader.getHomeIconY();
        setLocation(saveX, saveY);
        checkLocation();
        layer = LAYER_INNER;
        setActionListener(this);
        paintWhenOutOfScreen = true;// 在屏幕外也需要绘制
    }


    void checkLocation() {
        int oldx = (int) getX();
        int oldy = (int) getY();
        int deviceW = GCallBack.getInstance().getDeviceWidth();
        int deviceH = GCallBack.getInstance().getDeviceHeight();
        long vg = GCallBack.getInstance().getNvContext();
        long display = GCallBack.getInstance().getDisplay();


        GCallBack.getInstance().getInsets(inset);

        int tx = oldx, ty = oldy;
        int top = (int) (inset[0]);
        int bt = (int) (inset[2]);
        if (oldy < top) ty = top;
        if (oldy + DEF_H > deviceH - bt) ty = (int) (deviceH - bt - DEF_H);

        int right = (int) (inset[1]);
        int left = (int) (inset[3]);
        if (oldx < left) tx = left;
        if (oldx + DEF_W > deviceW - right) tx = (int) (deviceW - right - DEF_W);

        if (tx != oldx || ty != oldy) {
            setLocation(tx, ty);
            AppLoader.setHomeIconX(tx);
            AppLoader.setHomeIconY(ty);
        }
    }

    @Override
    public boolean paint(long vg) {
        checkLocation();

        GToolkit.drawImage(vg, butImg, getX(), getY(), getW(), getH(), false, 0.7f);

        if (System.currentTimeMillis() - markEndAt < 0) {
            drawMark(vg);
        }
        //draw small state icon
        float drawX = getX() + PAD + 1;
        float drawY = getY() + getH() * .5f - 4f;
        if (AppManager.getInstance().getWebServer() != null) {
            GToolkit.drawImage(vg, srvImg, drawX, drawY, ICON_WH, ICON_WH, false, 0.6f);
            drawX += ICON_WH + PAD;
        }
        if (drawX - getX() + ICON_WH + PAD > getW()) {
            drawY -= ICON_WH + PAD;
            drawX = getX() + PAD;
        }
//        if (AppManager.getInstance().getHttpClients().size() > 0) {
//            GToolkit.drawImage(vg, downImg, drawX, drawY, ICON_WH, ICON_WH, false, 0.6f);
//            drawX += ICON_WH + PAD;
//        }
        if (drawX - getX() + ICON_WH + PAD > getW()) {
            drawY -= ICON_WH + PAD;
            drawX = getX() + PAD;
        }
        return super.paint(vg);
    }


    @Override
    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        if (drag) {
            move(dx, dy);
            checkLocation();
            if (Math.abs(dx) > 2.f || Math.abs(dy) > 2) {
                moved = true;
            }
            if (getX() < 0) {
                boundle[LEFT] = 0f;
            }
            if (getX() + getW() > GCallBack.getInstance().getDeviceWidth()) {
                boundle[LEFT] = GCallBack.getInstance().getDeviceWidth() - getW();
            }
            if (getY() < 0) {
                boundle[TOP] = 0;
            }
            if (getY() + getH() > GCallBack.getInstance().getDeviceHeight()) {
                boundle[TOP] = GCallBack.getInstance().getDeviceHeight() - getH();
            }
            AppLoader.setHomeIconX((int) getX());
            AppLoader.setHomeIconY((int) getY());
            return true;
        }
        return super.dragEvent(button, dx, dy, x, y);
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (isInArea(x, y)) {
            if (pressed) {
                drag = true;
            } else {
                if (moved) {
                    drag = false;
                    moved = false;
                } else {
                    doAction();
                }
            }
        }
    }


    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (isInArea(x, y)) {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                drag = true;
            } else if (phase == Glfm.GLFMTouchPhaseEnded) {
                if (moved) {
                    drag = false;
                    moved = false;
                } else {
                    doAction();
                }
            } else if (!isInArea(x, y)) {

            }
        }
    }

    @Override
    public void action(GObject gobj) {
        GApplication app = GCallBack.getInstance().getApplication();
        if (app != AppManager.getInstance()) {
            app.pauseApp();
            AppManager.getInstance().active();
            AppManager.getInstance().mainPanelShowLeft();
        }
    }

    public void setDrawMarkSecond(int second) {
        markEndAt = System.currentTimeMillis() + second * 1000;
        cIdx = (int) (Math.random() * colors.length);
    }

    private void drawMark(long vg) {
        if (cIdx >= colors.length) cIdx = 0;

        nvgBeginPath(vg);
//        nvgRect(vg, getX(), getY(), getW(), getH());
        nvgCircle(vg, getX() + getW() * .5f, getY() + getH() * .5f, getW() * .5f - 2f);
        nvgFillColor(vg, nvgRGBA(colors[cIdx][0], colors[cIdx][1], colors[cIdx][2], (byte) 0x40));
        nvgFill(vg);
        cIdx++;
    }

    byte[][] colors = {
            {(byte) 0x00, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x01, (byte) 0xff, (byte) 0x0b,},
            {(byte) 0x02, (byte) 0xff, (byte) 0x17,},
            {(byte) 0x03, (byte) 0xff, (byte) 0x23,},
            {(byte) 0x04, (byte) 0xff, (byte) 0x2f,},
            {(byte) 0x05, (byte) 0xff, (byte) 0x3b,},
            {(byte) 0x06, (byte) 0xff, (byte) 0x47,},
            {(byte) 0x07, (byte) 0xff, (byte) 0x53,},
            {(byte) 0x08, (byte) 0xff, (byte) 0x5f,},
            {(byte) 0x09, (byte) 0xff, (byte) 0x6b,},
            {(byte) 0x0a, (byte) 0xff, (byte) 0x77,},
            {(byte) 0x0b, (byte) 0xff, (byte) 0x83,},
            {(byte) 0x0c, (byte) 0xff, (byte) 0x8f,},
            {(byte) 0x0d, (byte) 0xff, (byte) 0x9b,},
            {(byte) 0x0e, (byte) 0xff, (byte) 0xa7,},
            {(byte) 0x0f, (byte) 0xff, (byte) 0xb3,},
            {(byte) 0x10, (byte) 0xff, (byte) 0xbf,},
            {(byte) 0x11, (byte) 0xff, (byte) 0xcb,},
            {(byte) 0x12, (byte) 0xff, (byte) 0xd7,},
            {(byte) 0x13, (byte) 0xff, (byte) 0xe3,},
            {(byte) 0x14, (byte) 0xff, (byte) 0xef,},
            {(byte) 0x15, (byte) 0xff, (byte) 0xfb,},
            {(byte) 0x16, (byte) 0xf7, (byte) 0xff,},
            {(byte) 0x17, (byte) 0xeb, (byte) 0xff,},
            {(byte) 0x18, (byte) 0xdf, (byte) 0xff,},
            {(byte) 0x19, (byte) 0xd3, (byte) 0xff,},
            {(byte) 0x1a, (byte) 0xc7, (byte) 0xff,},
            {(byte) 0x1b, (byte) 0xbb, (byte) 0xff,},
            {(byte) 0x1c, (byte) 0xaf, (byte) 0xff,},
            {(byte) 0x1d, (byte) 0xa3, (byte) 0xff,},
            {(byte) 0x1e, (byte) 0x97, (byte) 0xff,},
            {(byte) 0x1f, (byte) 0x8b, (byte) 0xff,},
            {(byte) 0x20, (byte) 0x7f, (byte) 0xff,},
            {(byte) 0x21, (byte) 0x73, (byte) 0xff,},
            {(byte) 0x22, (byte) 0x67, (byte) 0xff,},
            {(byte) 0x23, (byte) 0x5b, (byte) 0xff,},
            {(byte) 0x24, (byte) 0x4f, (byte) 0xff,},
            {(byte) 0x25, (byte) 0x43, (byte) 0xff,},
            {(byte) 0x26, (byte) 0x37, (byte) 0xff,},
            {(byte) 0x27, (byte) 0x2b, (byte) 0xff,},
            {(byte) 0x28, (byte) 0x1f, (byte) 0xff,},
            {(byte) 0x29, (byte) 0x13, (byte) 0xff,},
            {(byte) 0x2a, (byte) 0x07, (byte) 0xff,},
            {(byte) 0x2b, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x2c, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x2d, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x2e, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x2f, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x30, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x31, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x32, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x33, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x34, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x35, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x36, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x37, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x38, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x39, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x3a, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x3b, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x3c, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x3d, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x3e, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x3f, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x40, (byte) 0x00, (byte) 0xff,},
            {(byte) 0x41, (byte) 0x00, (byte) 0xf3,},
            {(byte) 0x42, (byte) 0x00, (byte) 0xe7,},
            {(byte) 0x43, (byte) 0x00, (byte) 0xdb,},
            {(byte) 0x44, (byte) 0x00, (byte) 0xcf,},
            {(byte) 0x45, (byte) 0x00, (byte) 0xc3,},
            {(byte) 0x46, (byte) 0x00, (byte) 0xb7,},
            {(byte) 0x47, (byte) 0x00, (byte) 0xab,},
            {(byte) 0x48, (byte) 0x00, (byte) 0x9f,},
            {(byte) 0x49, (byte) 0x00, (byte) 0x93,},
            {(byte) 0x4a, (byte) 0x00, (byte) 0x87,},
            {(byte) 0x4b, (byte) 0x00, (byte) 0x7b,},
            {(byte) 0x4c, (byte) 0x00, (byte) 0x6f,},
            {(byte) 0x4d, (byte) 0x00, (byte) 0x63,},
            {(byte) 0x4e, (byte) 0x00, (byte) 0x57,},
            {(byte) 0x4f, (byte) 0x00, (byte) 0x4b,},
            {(byte) 0x50, (byte) 0x00, (byte) 0x3f,},
            {(byte) 0x51, (byte) 0x00, (byte) 0x33,},
            {(byte) 0x52, (byte) 0x00, (byte) 0x27,},
            {(byte) 0x53, (byte) 0x00, (byte) 0x1b,},
            {(byte) 0x54, (byte) 0x00, (byte) 0x0f,},
            {(byte) 0x55, (byte) 0x00, (byte) 0x03,},
            {(byte) 0x56, (byte) 0x07, (byte) 0x00,},
            {(byte) 0x57, (byte) 0x13, (byte) 0x00,},
            {(byte) 0x58, (byte) 0x1f, (byte) 0x00,},
            {(byte) 0x59, (byte) 0x2b, (byte) 0x00,},
            {(byte) 0x5a, (byte) 0x37, (byte) 0x00,},
            {(byte) 0x5b, (byte) 0x43, (byte) 0x00,},
            {(byte) 0x5c, (byte) 0x4f, (byte) 0x00,},
            {(byte) 0x5d, (byte) 0x5b, (byte) 0x00,},
            {(byte) 0x5e, (byte) 0x67, (byte) 0x00,},
            {(byte) 0x5f, (byte) 0x73, (byte) 0x00,},
            {(byte) 0x60, (byte) 0x7f, (byte) 0x00,},
            {(byte) 0x61, (byte) 0x8b, (byte) 0x00,},
            {(byte) 0x62, (byte) 0x97, (byte) 0x00,},
            {(byte) 0x63, (byte) 0xa3, (byte) 0x00,},
            {(byte) 0x64, (byte) 0xaf, (byte) 0x00,},
            {(byte) 0x65, (byte) 0xbb, (byte) 0x00,},
            {(byte) 0x66, (byte) 0xc7, (byte) 0x00,},
            {(byte) 0x67, (byte) 0xd3, (byte) 0x00,},
            {(byte) 0x68, (byte) 0xdf, (byte) 0x00,},
            {(byte) 0x69, (byte) 0xeb, (byte) 0x00,},
            {(byte) 0x6a, (byte) 0xf7, (byte) 0x00,},
            {(byte) 0x6b, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x6c, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x6d, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x6e, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x6f, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x70, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x71, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x72, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x73, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x74, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x75, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x76, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x77, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x78, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x79, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x7a, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x7b, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x7c, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x7d, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x7e, (byte) 0xff, (byte) 0x00,},
            {(byte) 0x7f, (byte) 0xff, (byte) 0x00,},
    };
}
