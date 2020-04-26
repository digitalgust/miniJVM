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
    private final List<GMenu> menus = new ArrayList();
    private final List<GObject> fronts = new ArrayList();
    private final List<GChildrenListener> childrenListeners = new ArrayList();
    protected GObject focus;

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
                nko.setParent(null);
                nko.destroy();
                boolean b = elements.remove(nko);
                if (focus == nko) {
                    if (b) {
                        focus.doFocusLost(null);
                    }
                    focus = null;
                }
                onRemove(nko);
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

    public GObject findByName(String name) {
        if (name == null) {
            return null;
        }
        synchronized (elements) {
            for (GObject go : elements) {
                if (name.equals(go.name)) {
                    return go;
                }
                if (go instanceof GContainer) {
                    GObject sub = ((GContainer) go).findByName(name);
                    if (sub != null) {
                        return sub;
                    }
                }
            }
        }
        return null;
    }

    public GObject findByXY(float x, float y) {
        synchronized (elements) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                GObject nko = elements.get(i);
                if (nko.isInArea(x, y)) {
                    return nko;
                }
            }
        }
        return null;
    }

    /**
     * @return the focus
     */
    public GObject getFocus() {
        return focus;
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
                //更新所有UI组件
                menus.clear();
                fronts.clear();
                for (int i = 0, imax = elements.size(); i < imax; i++) {
                    GObject nko = elements.get(i);
                    if (nko == focus) {
                        continue;
                    }
                    if (nko instanceof GMenu) {
                        menus.add((GMenu) nko);
                        continue;
                    }
                    if (nko.isFront()) {
                        fronts.add(nko);
                        continue;
                    }

                    if (nko.isVisible()) {
                        drawObj(ctx, nko);
                    }
                }
                if (focus != null) {
                    drawObj(ctx, focus);

                    //frame re sort
                    if (this instanceof GForm) {
                        elements.remove(focus);
                        elements.add(focus);

                    }
                }
                for (int i = 0, imax = fronts.size(); i < imax; i++) {
                    GObject m = fronts.get(i);
                    elements.remove(m);
                    elements.add(m);
                    drawObj(ctx, m);
                }
                for (int i = 0, imax = menus.size(); i < imax; i++) {
                    GMenu m = menus.get(i);
                    elements.remove(m);
                    elements.add(m);
                    drawObj(ctx, m);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void drawObj(long ctx, GObject nko) {
        float x, y, w, h;
//        if (nko instanceof GContainer) {
//            GContainer c = (GContainer) nko;
//            x = c.getX();
//            y = c.getY();
//            w = c.getW();
//            h = c.getH();
//
//        } else {
        x = nko.getX();
        y = nko.getY();
        w = nko.getW();
        h = nko.getH();
//        }

        nvgSave(ctx);
//        Nanovg.nvgReset(ctx);
        Nanovg.nvgScissor(ctx, x, y, w, h);
        float vx = this.getX();
        float vy = this.getY();
        float vw = this.getW();
        float vh = this.getH();
        if (vx + vw < x || vx > x + w || vy > y + h || vy + vh < y) {
        } else {
            Nanovg.nvgIntersectScissor(ctx, vx, vy, vw, vh);

            nko.paint(ctx);

//            if (focus == nko) {
//                Nanovg.nvgScissor(ctx, x, y, w, h);
//                Nanovg.nvgBeginPath(ctx);
//                Nanovg.nvgRect(ctx, x + 1, y + 1, w - 2, h - 2);
//                Nanovg.nvgStrokeColor(ctx, Nanovg.nvgRGBA((byte) 255, (byte) 0, (byte) 0, (byte) 255));
//                Nanovg.nvgStroke(ctx);
//
//                Nanovg.nvgBeginPath(ctx);
//                Nanovg.nvgRect(ctx, nko.getX() + 2, nko.getY() + 2, nko.getW() - 4, nko.getH() - 4);
//                Nanovg.nvgStrokeColor(ctx, Nanovg.nvgRGBA((byte) 0, (byte) 0, (byte) 255, (byte) 255));
//                Nanovg.nvgStroke(ctx);
//
//            }
        }
        Nanovg.nvgRestore(ctx);
    }

    @Override
    public void keyEvent(int key, int scanCode, int action, int mods) {
        if (focus != null) {
            focus.keyEvent(key, scanCode, action, mods);
        }
    }

    @Override
    public void characterEvent(char character) {
        if (focus != null) {
            focus.characterEvent(character);
        }
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        GObject found = findByXY(x, y);
        if (found instanceof GMenu) {
            if (!((GMenu) found).isContextMenu()) {
                setFocus(null);
            }
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

        if (focus != null && focus.isInArea(x, y)) {
            focus.cursorPosEvent(x, y);
        }

    }

    @Override
    public void dropEvent(int count, String[] paths) {
        if (focus != null) {
            focus.dropEvent(count, paths);
        }
    }

    @Override
    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        setFocus(findByXY(x, y));
        if (focus != null && focus.isInArea(x, y)) {
            return focus.scrollEvent(scrollX, scrollY, x, y);
        }
        return false;
    }

    @Override
    public void clickEvent(int button, int x, int y) {
        if (focus != null && focus.isInArea(x, y)) {
            focus.clickEvent(button, x, y);
        }
    }

    @Override
    public boolean dragEvent(float dx, float dy, float x, float y) {
        GObject found = findByXY(x, y);
        if (found instanceof GMenu) {
            return found.dragEvent(dx, dy, x, y);
        }

        if (focus != null && focus.isInArea(x, y)) {
            return focus.dragEvent(dx, dy, x, y);
        }
        return false;
    }

    ///==========================
    @Override
    public void keyEvent(int key, int action, int mods) {
        if (focus != null) {
            focus.keyEvent(key, action, mods);
        }
    }

    @Override
    public void characterEvent(String str, int mods) {
        if (focus != null) {
            focus.characterEvent(str, mods);
        }
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        GObject found = findByXY(x, y);
        if (found instanceof GMenu) {
            if (!((GMenu) found).isContextMenu()) {
                setFocus(null);
            }
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
        if (focus != null && focus.isInArea((float) x1, (float) y1)) {
            return focus.inertiaEvent(x1, y1, x2, y2, moveTime);
        }
        return false;
    }

    @Override
    public void longTouchedEvent(int x, int y) {
        GObject found = findByXY(x, y);
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
