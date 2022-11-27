package org.mini.apploader;

import org.mini.glfm.Glfm;
import org.mini.gui.*;
import org.mini.gui.event.GActionListener;

public class GFloatButton extends GPanel implements GActionListener {
    public static final float DEF_X = 16f, DEF_Y = 40f;
    public static final float DEF_W = 32f, DEF_H = 32f;
    public static final float ICON_WH = 8f, PAD = 0f;

    GImage butImg = GImage.createImageFromJar("/res/ui/home.png");
    GImage downImg = GImage.createImageFromJar("/res/ui/yellow.png");
//    GImage uploadImg = GImage.createImageFromJar("/res/ui/green.png");
//    GImage msgImg = GImage.createImageFromJar("/res/ui/red.png");
    GImage srvImg = GImage.createImageFromJar("/res/ui/blue.png");

    boolean drag = false;
    boolean moved = false;// is moved the button

    public GFloatButton(GForm form) {
        super(form, DEF_X, DEF_Y, DEF_W, DEF_H);
        layer = LAYER_FRONT;
        setActionListener(this);
    }

    @Override
    public boolean paint(long ctx) {

        GToolkit.drawImage(ctx, butImg, getX(), getY(), getW(), getH(), false, 0.7f);
        //draw small state icon
        float drawX = getX() + PAD + 1;
        float drawY = getY() + getH() * .5f - 4f;
        if (AppManager.getInstance().getWebServer() != null) {
            GToolkit.drawImage(ctx, srvImg, drawX, drawY, ICON_WH, ICON_WH, false, 0.6f);
            drawX += ICON_WH + PAD;
        }
        if (drawX - getX() + ICON_WH + PAD > getW()) {
            drawY -= ICON_WH + PAD;
            drawX = getX() + PAD;
        }
        if (AppManager.getInstance().getHttpClients().size() > 0) {
            GToolkit.drawImage(ctx, downImg, drawX, drawY, ICON_WH, ICON_WH, false, 0.6f);
            drawX += ICON_WH + PAD;
        }
        if (drawX - getX() + ICON_WH + PAD > getW()) {
            drawY -= ICON_WH + PAD;
            drawX = getX() + PAD;
        }
        return super.paint(ctx);
    }


    @Override
    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        if (drag) {
            move(dx, dy);
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
        }
    }
}
