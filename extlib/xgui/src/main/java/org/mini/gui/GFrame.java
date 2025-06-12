/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.gscript.Interpreter;
import org.mini.nanovg.Nanovg;
import org.mini.util.SysLog;

import java.util.List;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GFrame extends GContainer {

    public static final float TITLE_HEIGHT = 30.f, PAD = 2.f;
    float titleHeight = TITLE_HEIGHT;
    float padH = PAD;

    protected String title;
    protected byte[] title_arr;

    protected byte[] close_arr = ICON_CIRCLED_CROSS_BYTE;
    protected float[] close_boundle = new float[4];
    protected float[] titleColor = null;

    protected GViewPort view = new GViewPort(form);

    protected GPanel titlePanel = new TitlePanel(form);


    protected int frameMode;
    protected boolean closable = true;


    public GFrame(GForm form) {
        this(form, "", (float) 0, (float) 0, (float) 300, (float) 200);
    }

    public GFrame(GForm form, String title, float left, float top, float width, float height) {
        super(form);
        setCornerRadius(3.f);

        setTitle(title);
        setLocation(left, top);
        setSize(width, height);

        view.setLocation(PAD, titleHeight + PAD);
        view.setSize(width - PAD * 2, height - titleHeight - PAD * 2);
        addImpl(view);

        titlePanel.setLocation(1, 1);
        titlePanel.setSize(width - PAD, titleHeight);
        addImpl(titlePanel);
    }

    @Override
    public void setSize(float w, float h) {
        titlePanel.setSize(w - PAD, titleHeight);
        view.setLocation(PAD, titleHeight + PAD);
        view.setSize(w - PAD * 2, h - titleHeight - PAD * 2);
        super.setSize(w, h);
    }


    public float[] getBgColor() {
        if (view.bgColor == null) return GToolkit.getStyle().getBackgroundColor();
        return view.bgColor;
    }

    public void setBgColor(int r, int g, int b, int a) {
        view.bgColor = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public void setBgColor(float[] color) {
        view.bgColor = color;
    }

    public void setBgColor(int rgba) {
        view.bgColor = nvgRGBA(rgba);
    }

    @Override
    public float getInnerX() {
        return getX();
    }

    @Override
    public float getInnerY() {
        return getY();
    }

    @Override
    public float getInnerW() {
        return getW();
    }

    @Override
    public float getInnerH() {
        return getH();
    }

    @Override
    public void setInnerLocation(float x, float y) {
        setLocation(x, y);
    }

    @Override
    public void setInnerSize(float x, float y) {
        setSize(x, y);
    }

    @Override
    public float[] getInnerBoundle() {
        return getBoundle();
    }

    public void close() {
        if (parent != null) {
            parent.removeImpl(this);
            doStateChanged(this);
        }
    }

    public void setTitle(String title) {
        title_arr = toCstyleBytes(title);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setFrameMode(int mode) {
        frameMode = mode;
    }

    public int getFrameMode() {
        return frameMode;
    }

    public GViewPort getView() {
        return view;
    }

    public GPanel getTitlePanel() {
        return titlePanel;
    }


    /**
     * @return the closable
     */
    public boolean isClosable() {
        return closable;
    }

    /**
     * @param closable the closable to set
     */
    public void setClosable(boolean closable) {
        this.closable = closable;
    }


    public void align(int align_mod) {
        if (parent == null) {
            SysLog.warn("added to form can be set align");
            return;
        }
        if ((align_mod & Nanovg.NVG_ALIGN_LEFT) != 0) {
            move(-getX(), 0);
        } else if ((align_mod & Nanovg.NVG_ALIGN_RIGHT) != 0) {
            move(parent.getW() - (getX() + getW()), 0);
        } else if ((align_mod & Nanovg.NVG_ALIGN_CENTER) != 0) {
            move(parent.getW() / 2 - (getX() + getW() / 2), 0);
        }
        if ((align_mod & Nanovg.NVG_ALIGN_TOP) != 0) {
            move(0, -getY());
        } else if ((align_mod & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
            move(0, parent.getH() - (getY() + getH()));
        } else if ((align_mod & Nanovg.NVG_ALIGN_CENTER) != 0) {
            move(0, parent.getH() / 2 - (getY() + getH() / 2));
        }
    }

    /**
     * ensure frame close bar in form ,not in screen
     * because form maybe translate on keyboard popup
     */
    public void validLocation() {
        if (getX() + getW() < parent.getX() + 40) {
            setLocation(-(getW() - 40), getLocationTop());
        }
        if (getY() < parent.getY()) {
            setLocation(getLocationLeft(), 0);
        }
        if (getX() > parent.getX() + parent.getW() - 60) {
            setLocation(parent.getW() - 60, getLocationTop());
        }
        if (getY() > parent.getY() + parent.getH() - 30) {
            setLocation(getLocationLeft(), parent.getH() - 30);
        }
//        if (getW() > parent.getW()) {
//            setLocation(0, getLocationTop());
//        }
//        if (getH() > parent.getH()) {
//            setLocation(getLocationLeft(), 0);
//        }
    }

    @Override
    public void onAdd(GObject obj) {
        super.onAdd(obj);
        if (parent != null) {
            parent.setCurrent(this);
            validLocation();
        }
    }

    @Override
    public boolean paint(long vg) {
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();
        validLocation();


        // Window
        if (getBgImg() == null) {
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, w, h, getCornerRadius());
            nvgFillColor(vg, getBgColor());
            nvgFill(vg);
        }
        super.paint(vg);

        // Drop shadow
        float shadowBlur = 18;
        byte[] shadowPaint;
        shadowPaint = nvgBoxGradient(vg, x, y + 0.5f, w, h, getCornerRadius(), shadowBlur, nvgRGBA(0, 0, 0, 32), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x - shadowBlur, y - shadowBlur, w + shadowBlur * 2, h + shadowBlur * 2);
        nvgRoundedRect(vg, x, y, w, h, getCornerRadius());
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, shadowPaint);
        nvgFill(vg);
        return true;
    }


    int mouseX, mouseY;
    boolean dragFrame = false;

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {

        switch (button) {
            case Glfw.GLFW_MOUSE_BUTTON_1: {//left
                if (pressed) {
                    if (closable && isInBoundle(close_boundle, x, y)) {
                        close();
                    } else if (titlePanel.isInArea(x, y) && button == Glfw.GLFW_MOUSE_BUTTON_1) {
                        dragFrame = true;
                    }

                } else {
                    dragFrame = false;
                }
                break;
            }
            case Glfw.GLFW_MOUSE_BUTTON_2: {//right
                break;
            }
            case Glfw.GLFW_MOUSE_BUTTON_3: {//middle
                break;
            }
        }
//        if (isInArea(x, y)) {
//            super.mouseButtonEvent(button, pressed, x, y);
//        } else {
//            //view.setFocus(null);// x,y not in FrameArea when  popup keyboard
//        }
        super.mouseButtonEvent(button, pressed, x, y);
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) SysLog.warn(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }

    @Override
    public boolean dragEvent(int button, float dx, float dy, float x, float y) {

        if (dragFrame && button == Glfw.GLFW_MOUSE_BUTTON_1) {
            move(dx, dy);
            validLocation();

            return true;
        } else {
            return super.dragEvent(button, dx, dy, x, y);
        }
    }

    @Override
    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        return view.scrollEvent(scrollX, scrollY, x, y);
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {

        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan:
                if (closable && isInBoundle(close_boundle, x, y)) {
                    close();
                } else if (titlePanel.isInArea(x, y)) {
                    dragFrame = true;
                }
                mouseX = x;
                mouseY = y;
                break;
            case Glfm.GLFMTouchPhaseMoved:
                mouseX = x;
                mouseY = y;
                break;
            default:
                dragFrame = false;

                break;
        }

        if (isInArea(x, y)) {
            super.touchEvent(touchid, phase, x, y);
        } else {
            if (phase == Glfm.GLFMTouchPhaseBegan) view.setCurrent(null);
        }
    }

    @Override
    public String toString() {
        return title + "/" + super.toString();
    }


    /**
     * lock the list when modify it
     *
     * @return
     */
    public List<GObject> getElements() {
        return view.getElementsImpl();
    }

    public int getElementSize() {
        return view.elements.size();
    }

    public void add(GObject nko) {
        view.addImpl(nko);
    }

    public void add(int index, GObject nko) {
        view.addImpl(index, nko);
    }

    public void remove(GObject nko) {
        view.removeImpl(nko);
    }

    public void remove(int index) {
        view.removeImpl(index);
    }

    public boolean contains(GObject son) {
        return view.containsImpl(son);
    }

    public void clear() {
        view.clearImpl();
    }


    public void setTitleShow(boolean enable) {
        if (enable) {
            titleHeight = TITLE_HEIGHT;
            padH = PAD;
        } else {
            titleHeight = 0;
            padH = 0;
        }
        setSize(getW(), getH());
    }

    class TitlePanel extends GPanel {
        public TitlePanel(GForm form) {
            super(form);
        }

        @Override
        public boolean paint(long vg) {
            if (getH() < 1f) return false;

            super.paint(vg);

            boolean isFocus = false;
            if (GFrame.this.parent != null) {
                isFocus = GFrame.this.parent.getCurrent() == GFrame.this;
            }

            byte[] headerPaint;
            float x = getX();
            float y = getY();
            float w = getW();
            float h = getH();
            headerPaint = nvgLinearGradient(vg, x, y, x, y + 15, nvgRGBA(255, 255, 255, 8), nvgRGBA(0, 0, 0, 16));
            nvgBeginPath(vg);
            nvgRoundedRect(vg,
                    x,
                    y,
                    w,
                    h,
                    getCornerRadius() - 1);
            nvgFillPaint(vg, headerPaint);
            nvgFill(vg);

            nvgBeginPath(vg);
            nvgMoveTo(vg, x + 0.5f, y + 0.5f + 30);
            nvgLineTo(vg, x + 0.5f + w - 1, y + 0.5f + 30);
            nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 32));
            nvgStroke(vg);

            nvgFontSize(vg, GToolkit.getStyle().getTitleFontSize());
            nvgFontFace(vg, GToolkit.getFontWord());
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

            if (title_arr != null) {
                nvgSave(vg);
                nvgIntersectScissor(vg, x + 30, y, w - 60, h);
                nvgFontBlur(vg, 2);
                nvgFillColor(vg, GToolkit.getStyle().getTextShadowColor());
                nvgTextJni(vg, x + w / 2, y + 16 + 1, title_arr, 0, title_arr.length);

                nvgFontBlur(vg, 0);
                if (isFocus) {
                    nvgFillColor(vg, GToolkit.getStyle().getFrameTitleColor());
                } else {
                    if (titleColor == null) {
                        titleColor = new float[4];
                        System.arraycopy(GToolkit.getStyle().getFrameTitleColor(), 0, titleColor, 0, titleColor.length);
                        titleColor[3] *= 0.7f;
                    }
                    nvgFillColor(vg, titleColor);
                }
                nvgTextJni(vg, x + w / 2, y + 16, title_arr, 0, title_arr.length);
                nvgRestore(vg);
            }
            if (closable) {
                nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
                nvgFontFace(vg, GToolkit.getFontIcon());
                nvgFillColor(vg, nvgRGBA(192, 32, 32, 196));
                nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
                nvgTextJni(vg, x + 10, y + 16, close_arr, 0, close_arr.length);
            }
            close_boundle[LEFT] = x;
            close_boundle[TOP] = y;
            close_boundle[WIDTH] = 30;
            close_boundle[HEIGHT] = 30;
            return true;
        }
    }


}
