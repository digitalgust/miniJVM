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

import java.util.List;

import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.glwrap.GLUtil.toUtf8;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author gust
 */
public class GFrame extends GContainer {

    public static final float TITLE_HEIGHT = 30.f, PAD = 2.f;

    protected String title;
    protected byte[] title_arr;

    protected byte[] close_arr = {(byte) 0xe2, (byte) 0x9d, (byte) 0x8e, 0};
    protected float[] close_boundle = new float[4];

    protected GViewPort view = new GViewPort(form);
    protected GPanel title_panel = new GPanel(form);

    protected int frameMode;
    protected boolean closable = true;
    protected String onCloseScript;
    protected String onInitScript;


    public GFrame(GForm form) {
        this(form, "", (float) 0, (float) 0, (float) 300, (float) 200);
    }

    public GFrame(GForm form, String title, float left, float top, float width, float height) {
        super(form);
        setTitle(title);
        setLocation(left, top);
        setSize(width, height);

        view.setLocation(PAD, TITLE_HEIGHT + PAD);
        view.setSize(width - PAD * 2, height - TITLE_HEIGHT - PAD * 2);
        addImpl(view);

        title_panel.setLocation(1, 1);
        title_panel.setSize(width - PAD, TITLE_HEIGHT);
        addImpl(title_panel);
    }

    @Override
    public void setSize(float w, float h) {
        title_panel.setSize(w - PAD, TITLE_HEIGHT);
        view.setSize(w - PAD * 2, h - TITLE_HEIGHT - PAD * 2);
        super.setSize(w, h);
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
        title_arr = toUtf8(title);
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
        if (getForm() == null) {
            System.out.println("warning: added to form can be set align");
            return;
        }
        if ((align_mod & Nanovg.NVG_ALIGN_LEFT) != 0) {
            move(-getX(), 0);
        } else if ((align_mod & Nanovg.NVG_ALIGN_RIGHT) != 0) {
            move(getForm().getW() - (getX() + getW()), 0);
        } else if ((align_mod & Nanovg.NVG_ALIGN_CENTER) != 0) {
            move(getForm().getW() / 2 - (getX() + getW() / 2), 0);
        }
        if ((align_mod & Nanovg.NVG_ALIGN_TOP) != 0) {
            move(0, -getY());
        } else if ((align_mod & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
            move(0, getForm().getH() - (getY() + getH()));
        } else if ((align_mod & Nanovg.NVG_ALIGN_CENTER) != 0) {
            move(0, getForm().getH() / 2 - (getY() + getH() / 2));
        }
    }

    /**
     * ensure frame close bar in form ,not in screen
     * because form maybe translate on keyboard popup
     */
    void validLocation() {

        if (getX() < -(getW() - 40)) {
            setLocation(-(getW() - 40), getY());
        }
        if (getY() < form.getY()) {
            setLocation(getX(), 0);
        }
        if (getX() > form.getX() + form.getW() - 40) {
            setLocation(form.getX() + form.getW() - 40, getY());
        }
        if (getY() > form.getY() + form.getH() - 30) {
            setLocation(getX(), form.getY() + form.getH() - 30);
        }
    }

    @Override
    public void onAdd(GObject obj) {
        super.onAdd(obj);
        if (parent != null) {
            parent.setFocus(this);
            validLocation();
        }
    }

    @Override
    public boolean paint(long vg) {
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();
        drawWindow(vg, title, x, y, w, h);
        super.paint(vg);
        validLocation();
        return true;
    }

    void drawWindow(long vg, String title, float x, float y, float w, float h) {
        float cornerRadius = 3.0f;
        byte[] shadowPaint;
        byte[] headerPaint;

        // Window
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgFillColor(vg, getBgColor());
        nvgFill(vg);

        // Drop shadow
        shadowPaint = nvgBoxGradient(vg, x, y + 2, w, h, cornerRadius * 2, 10, nvgRGBA(0, 0, 0, 128), nvgRGBA(0, 0, 0, 0));
        nvgBeginPath(vg);
        nvgRect(vg, x - 10, y - 10, w + 20, h + 30);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, shadowPaint);
        nvgFill(vg);

        // Header
        headerPaint = nvgLinearGradient(vg, x, y, x, y + 15, nvgRGBA(255, 255, 255, 8), nvgRGBA(0, 0, 0, 16));
        nvgBeginPath(vg);
        nvgRoundedRect(vg,
                title_panel.getX(),
                title_panel.getY(),
                title_panel.getW(),
                title_panel.getH(),
                cornerRadius - 1);
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
            nvgFontBlur(vg, 2);
            nvgFillColor(vg, GToolkit.getStyle().getTextShadowColor());
            nvgTextJni(vg, x + w / 2, y + 16 + 1, title_arr, 0, title_arr.length);

            nvgFontBlur(vg, 0);
            nvgFillColor(vg, GToolkit.getStyle().getFrameTitleColor());
            nvgTextJni(vg, x + w / 2, y + 16, title_arr, 0, title_arr.length);
        }
        if (closable) {
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());
            nvgFillColor(vg, nvgRGBA(192, 32, 32, 128));
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgTextJni(vg, x + 10, y + 16, close_arr, 0, close_arr.length);
        }
        close_boundle[LEFT] = x;
        close_boundle[TOP] = y;
        close_boundle[WIDTH] = 30;
        close_boundle[HEIGHT] = 30;

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
                    } else if (title_panel.isInArea(x, y) && button == Glfw.GLFW_MOUSE_BUTTON_1) {
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
        if (isInArea(x, y)) {
            super.mouseButtonEvent(button, pressed, x, y);
        } else {
            //view.setFocus(null);// x,y not in FrameArea when  popup keyboard
        }
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) System.out.println(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
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
                } else if (title_panel.isInArea(x, y)) {
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
            if (phase == Glfm.GLFMTouchPhaseBegan) view.setFocus(null);
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


    public String getOnCloseScript() {
        return onCloseScript;
    }

    public String getOnInitScript() {
        return onInitScript;
    }

    public void setOnCloseScript(String onCloseScript) {
        this.onCloseScript = onCloseScript;
    }

    public void setOnInitScript(String onInitScript) {
        this.onInitScript = onInitScript;
    }

    @Override
    public void init() {
        super.init();
        if (onInitScript != null) {
            Interpreter inp = parseInpByCall(onInitScript);
            String funcName = parseInstByCall(onInitScript);
            if (inp != null && funcName != null) {
                try {
                    inp.callSub(funcName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (onCloseScript != null) {
            Interpreter inp = parseInpByCall(onCloseScript);
            String funcName = parseInstByCall(onCloseScript);
            if (inp != null && funcName != null) {
                try {
                    inp.callSub(funcName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
