/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.gui.event.GChildrenListener;
import org.mini.nanovg.Nanovg;

import java.util.ArrayList;
import java.util.List;

import static org.mini.nanovg.Nanovg.nvgSave;

/**
 * @author gust
 */
abstract public class GContainer extends GObject {

    protected final List<GObject> elements = new ArrayList();
    private final List<GChildrenListener> childrenListeners = new ArrayList();
    protected GObject focus;
    float[] visableArea = new float[4];

    public abstract float getInnerX();

    public abstract float getInnerY();

    public abstract float getInnerW();

    public abstract float getInnerH();

    public abstract void setInnerLocation(float x, float y);

    public abstract void setInnerSize(float x, float y);

    public abstract float[] getInnerBoundle();

    public boolean isInArea(float x, float y) {
        float absx = getX();
        float absy = getY();
        return x >= absx && x <= absx + getW()
                && y >= absy && y <= absy + getH();
    }

    public float[] getVisableArea() {
        float x1 = getX();
        float y1 = getY();
        float x2 = x1 + getW();
        float y2 = y1 + getH();
        if (parent != null) {
            float[] parentVA = parent.getVisableArea();
            visableArea[0] = x1 > parentVA[0] ? x1 : parentVA[0];
            visableArea[1] = y1 > parentVA[1] ? y1 : parentVA[1];
            visableArea[2] = x2 < parentVA[2] ? x2 : parentVA[2];
            visableArea[3] = y2 < parentVA[3] ? y2 : parentVA[3];
        } else {
            visableArea[0] = x1;
            visableArea[1] = y1;
            visableArea[2] = x2;
            visableArea[3] = y2;
        }
        return visableArea;
    }

    //
    //  these methods : getElements getElementSize add remove clear
    //  they aren't public ,
    //  because it protect combin Conponent that : GFrame GList
    //  GFrame can't direct add children in it , GList too
    //

    public List<GObject> getElements() {
        return getElementsImpl();
    }

    public int getElementSize() {
        return elements.size();
    }

    public void add(GObject nko) {
        addImpl(nko);
    }

    public void add(int index, GObject nko) {
        addImpl(index, nko);
    }

    public void remove(GObject nko) {
        removeImpl(nko);
    }

    public void remove(int index) {
        removeImpl(index);
    }

    public boolean contains(GObject son) {
        return containsImpl(son);
    }

    public void clear() {
        clearImpl();
    }

    //inner method
    List<GObject> getElementsImpl() {
        return elements;
    }

    int getElementSizeImpl() {
        return elements.size();
    }

    void addImpl(GObject nko) {
        if (nko != null) {
            addImpl(elements.size(), nko);
            if (nko.isFront()) {
                setFocus(nko);
            }
        }
    }

    void addImpl(int index, GObject nko) {
        if (nko != null) {
            synchronized (elements) {
                if (!elements.contains(nko)) {
                    elements.add(index, nko);
                    nko.setParent(this);
                    nko.init();
                    onAdd(nko);
                }
            }
        }
    }

    void removeImpl(GObject nko) {
        if (nko != null) {
            synchronized (elements) {
                if (focus == nko) {
                    setFocus(null);
                }
                onRemove(nko);
                nko.setParent(null);
                nko.destroy();
                elements.remove(nko);
            }
        }
    }

    void removeImpl(int index) {
        synchronized (elements) {
            GObject nko = elements.get(index);
            removeImpl(nko);
        }
    }

    boolean containsImpl(GObject son) {
        return elements.contains(son);
    }

    void clearImpl() {
        synchronized (elements) {
            int size = elements.size();
            for (int i = 0; i < size; i++) {
                removeImpl(elements.size() - 1);
            }
        }
    }

    public <T extends GObject> T findByName(String name) {
        if (name == null) {
            return null;
        }
        synchronized (elements) {
            for (int i = 0, imax = elements.size(); i < imax; i++) {
                GObject go = elements.get(i);
                if (name.equals(go.name)) {
                    return (T) go;
                }
                if (go instanceof GContainer) {
                    GObject sub = ((GContainer) go).findByName(name);
                    if (sub != null) {
                        return (T) sub;
                    }
                }
            }
        }
        return null;
    }

    /**
     * find uiobject by x,y , if the pos is menu return immediate, no menu there then return other type uiobject
     *
     * @param x
     * @param y
     * @return
     */
    <T extends GObject> T findSonByXY(float x, float y) {
        GObject front = null, mid = null, back = null, menu = null;
        synchronized (elements) {
            for (int i = 0; i < elements.size(); i++) {
                GObject nko = elements.get(i);
                if (nko.isInArea(x, y)) {
                    if (nko instanceof GMenu) {
                        return (T) nko;
                    } else if (nko.isFront()) {
                        front = nko;
                    } else if (nko.isBack()) {
                        back = nko;
                    } else {
                        mid = nko;
                    }
                }
            }
        }
        return front != null ? (T) front : (mid != null ? (T) mid : (T) back);
    }

    /**
     * find the frontest uiobject, son of son of son ...
     *
     * @param x
     * @param y
     * @return
     */
    public <T extends GObject> T findByXY(float x, float y) {
        synchronized (elements) {
            for (int i = 0; i < elements.size(); i++) {
                GObject nko = elements.get(i);
                if (nko.isInArea(x, y)) {
                    if (nko instanceof GContainer) {
                        GObject re = ((GContainer) nko).findByXY(x, y);
                        if (re != null) {
                            return (T) re;
                        } else {
                            return (T) nko;
                        }
                    } else {
                        return (T) nko;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return the focus
     */
    public <T extends GObject> T getFocus() {
        return (T) focus;
    }

    /**
     * @param go
     */
    public void setFocus(GObject go) {
        if (go instanceof GMenu) {
            return;
        }
        if (this.focus != go) {
            GObject old = this.focus;
            this.focus = go;
            //notify all focus of sons
            if (old != null) {
                old.doFocusLost(go);
                if (old instanceof GContainer) {
                    ((GContainer) old).setFocus(null);
                }
            }
            if (focus != null) {
                focus.doFocusGot(old);
            }
        }
    }

    public void addChildrenListener(GChildrenListener listener) {
        if (!childrenListeners.contains(listener)) childrenListeners.add(listener);
    }

    public void removeChildrenListener(GChildrenListener listener) {
        childrenListeners.remove(listener);
    }

    public void onAdd(GObject obj) {
        for (GChildrenListener l : childrenListeners) {
            try {
                l.onChildAdd(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onRemove(GObject obj) {
        for (GChildrenListener l : childrenListeners) {
            try {
                l.onChildRemove(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void reSize() {

    }

    @Override
    public boolean paint(long ctx) {
        try {

            synchronized (elements) {
//                //更新所有UI组件

                if (focus != null && (focus instanceof GFrame || focus instanceof GList)) {
                    elements.remove(focus);
                    elements.add(focus);
                }
                for (int i = 0, imax = elements.size(); i < imax; i++) {
                    GObject nko = elements.get(i);
                    if (nko.isBack()) {
                        drawObj(ctx, nko);
                        continue;
                    }
                }
                for (int i = 0, imax = elements.size(); i < imax; i++) {
                    GObject nko = elements.get(i);
                    if (!nko.isBack() && !nko.isMenu()) {
                        drawObj(ctx, nko);
                        continue;
                    }
                }
                for (int i = 0, imax = elements.size(); i < imax; i++) {
                    GObject nko = elements.get(i);
                    if (nko.isMenu()) {
                        drawObj(ctx, nko);
                        continue;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void drawObj(long ctx, GObject nko) {
        if (!nko.isVisible()) {
            return;
        }

        nvgSave(ctx);
//        Nanovg.nvgReset(ctx);
        float[] va = getVisableArea();//left-top , right-bottom
        float vx = nko.getX();
        float vy = nko.getY();
        float vw = nko.getW();
        float vh = nko.getH();
        if (va[2] <= va[0] || va[3] <= va[1] || vx + vw <= va[0] || vx >= va[2] || vy >= va[3] || vy + vh <= va[1]) {
            //out of visable area
        } else {

            Nanovg.nvgScissor(ctx, va[0], va[1], va[2] - va[0], va[3] - va[1]);

            nko.paint(ctx);

            if (paintDebug && (focus == nko)) {
                Nanovg.nvgScissor(ctx, vx, vy, vw, vh);
                Nanovg.nvgBeginPath(ctx);
                Nanovg.nvgRect(ctx, vx + 1, vy + 1, vw - 2, vh - 2);
                Nanovg.nvgStrokeColor(ctx, Nanovg.nvgRGBA((byte) 255, (byte) 0, (byte) 0, (byte) 255));
                Nanovg.nvgStroke(ctx);

                Nanovg.nvgBeginPath(ctx);
                Nanovg.nvgRect(ctx, nko.getX() + 2, nko.getY() + 2, nko.getW() - 4, nko.getH() - 4);
                Nanovg.nvgStrokeColor(ctx, Nanovg.nvgRGBA((byte) 0, (byte) 0, (byte) 255, (byte) 255));
                Nanovg.nvgStroke(ctx);

            }
        }
        Nanovg.nvgRestore(ctx);
    }

    @Override
    public void keyEventGlfw(int key, int scanCode, int action, int mods) {
        if (!isEnable()) {
            return;
        }
        if (focus != null) {
            focus.keyEventGlfw(key, scanCode, action, mods);
        }
    }

    @Override
    public void characterEvent(char character) {
        if (!isEnable()) {
            return;
        }
        if (focus != null) {
            focus.characterEvent(character);
        }
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (!isEnable()) {
            return;
        }
        GObject found = findSonByXY(x, y);
        if (found instanceof GMenu) {
            if (!((GMenu) found).isContextMenu()) {
                setFocus(null);
            }
            found.mouseButtonEvent(button, pressed, x, y);
            return;
        } else if (found instanceof GFrame) {//this fix frame may not be active
            setFocus(found);
            found.mouseButtonEvent(button, pressed, x, y);
            return;
        }

        if (focus != null && focus.isInArea(x, y)) {
            focus.mouseButtonEvent(button, pressed, x, y);
        } else {
            if (pressed) {
                setFocus(found);
            }
            if (focus != null) {
                focus.mouseButtonEvent(button, pressed, x, y);
            }
        }
    }

    @Override
    public void cursorPosEvent(int x, int y) {

        if (!isEnable()) {
            return;
        }
        if (focus != null && focus.isInArea(x, y)) {
            focus.cursorPosEvent(x, y);
        }

    }

    @Override
    public void dropEvent(int count, String[] paths) {
        if (!isEnable()) {
            return;
        }
        if (focus != null) {
            focus.dropEvent(count, paths);
        }
    }

    @Override
    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        if (!isEnable()) {
            return false;
        }
        setFocus(findSonByXY(x, y));
        if (focus != null && focus.isInArea(x, y)) {
            return focus.scrollEvent(scrollX, scrollY, x, y);
        }
        return false;
    }

    @Override
    public void clickEvent(int button, int x, int y) {
        if (!isEnable()) {
            return;
        }
        if (focus != null && focus.isInArea(x, y)) {
            focus.clickEvent(button, x, y);
        }
    }

    @Override
    public boolean dragEvent(float dx, float dy, float x, float y) {
        if (!isEnable()) {
            return false;
        }

        if (focus != null) {
            return focus.dragEvent(dx, dy, x, y);
        }
        GObject found = findSonByXY(x, y);
        if (found instanceof GMenu) {
            return found.dragEvent(dx, dy, x, y);
        } else if (found != null && found.isFront()) {
            return found.dragEvent(dx, dy, x, y);
        }
        return false;
    }

    ///==========================
    @Override
    public void keyEventGlfm(int key, int action, int mods) {
        if (!isEnable()) {
            return;
        }
        if (focus != null) {
            focus.keyEventGlfm(key, action, mods);
        }
    }

    @Override
    public void characterEvent(String str, int mods) {
        if (!isEnable()) {
            return;
        }
        if (focus != null) {
            focus.characterEvent(str, mods);
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        if (!isEnable()) {
            return;
        }
        GObject found = findSonByXY(x, y);
        if (found instanceof GMenu) {
            if (!((GMenu) found).isContextMenu()) {
                setFocus(null);
            }
            found.touchEvent(touchid, phase, x, y);
            return;
        } else if (found instanceof GFrame) {//this fix frame may not be active
            setFocus(found);
            found.touchEvent(touchid, phase, x, y);
            return;
        }

        if (focus != null && focus.isInArea(x, y)) {
            focus.touchEvent(touchid, phase, x, y);
        } else {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                setFocus(found);
            }
            if (focus != null) {
                focus.touchEvent(touchid, phase, x, y);
            }
        }
    }

    @Override
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, long moveTime) {

        if (!isEnable()) {
            return false;
        }
        if (focus != null && focus.isInArea((float) x1, (float) y1)) {
            return focus.inertiaEvent(x1, y1, x2, y2, moveTime);
        }
        return false;
    }

    @Override
    public void longTouchedEvent(int x, int y) {
        if (!isEnable()) {
            return;
        }
        GObject found = findSonByXY(x, y);
        if (found instanceof GMenu) {
            if (!((GMenu) found).isContextMenu()) {
                setFocus(null);
            }
            found.longTouchedEvent(x, y);
            return;
        }

        setFocus(found);

        if (focus != null) {
            focus.longTouchedEvent(x, y);
        }
    }

}
