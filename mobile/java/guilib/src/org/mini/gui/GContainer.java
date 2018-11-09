/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.ArrayList;
import java.util.List;
import org.mini.glfm.Glfm;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.nvgSave;
import static org.mini.nanovg.Nanovg.nvgScissor;

/**
 *
 * @author gust
 */
abstract public class GContainer extends GObject {

    final List<GObject> elements = new ArrayList();
    private final List<GMenu> menus = new ArrayList();
    private final List<GObject> fronts = new ArrayList();
    GObject focus;

    public abstract float getViewX();

    public abstract float getViewY();

    public abstract float getViewW();

    public abstract float getViewH();

    public abstract void setViewLocation(float x, float y);

    public abstract void setViewSize(float x, float y);

    public abstract float[] getViewBoundle();

    public boolean isInArea(float x, float y) {
        float absx = getViewX();
        float absy = getViewY();
        return x >= absx && x <= absx + getViewW()
                && y >= absy && y <= absy + getViewH();
    }

    public List<GObject> getElements() {
        return elements;
    }

    public int getElementSize() {
        return elements.size();
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
            if (focus != null) {
                if (focus.focusListener != null) {
                    focus.focusListener.focusLost(focus);
                }
            }
            this.focus = go;
            if (focus != null) {
                if (focus.focusListener != null) {
                    focus.focusListener.focusGot(focus);
                }
            }
        }
    }

    public void add(GObject nko) {
        if (nko != null) {
            add(elements.size(), nko);
        }
    }

    public void add(int index, GObject nko) {
        if (nko != null) {
            synchronized (elements) {
                elements.add(index, nko);
                nko.setParent(this);
                nko.init();
                onAdd(nko);
            }
        }
    }

    public void remove(GObject nko) {
        if (nko != null) {
            synchronized (elements) {
                nko.setParent(null);
                nko.destory();
                elements.remove(nko);
                if (focus == nko) {
                    focus = null;
                }
                onRemove(nko);
            }
        }
    }

    public void remove(int index) {
        synchronized (elements) {
            GObject nko = elements.get(index);
            remove(nko);
        }
    }

    public boolean contains(GObject son) {
        return elements.contains(son);
    }

    public void clear() {
        synchronized (elements) {
            int size = elements.size();
            for (int i = 0; i < size; i++) {
                remove(elements.size() - 1);
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

    public void onAdd(GObject obj) {

    }

    public void onRemove(GObject obj) {

    }

    public void reBoundle() {

    }

    @Override
    public boolean update(long ctx) {
        try {
            synchronized (elements) {
                //更新所有UI组件
                menus.clear();
                fronts.clear();
                for (GObject nko : elements) {
                    if (nko == focus) {
                        continue;
                    }
                    if (nko.getType() == TYPE_MENU) {
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
                    if (getType() == TYPE_FORM) {
                        elements.remove(focus);
                        elements.add(focus);

                    }
                }
                for (GObject m : fronts) {
                    elements.remove(m);
                    elements.add(m);
                    drawObj(ctx, m);
                }
                for (GMenu m : menus) {
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
        if (nko instanceof GContainer) {
            GContainer c = (GContainer) nko;
            x = c.getViewX();
            y = c.getViewY();
            w = c.getViewW();
            h = c.getViewH();

        } else {
            x = nko.getX();
            y = nko.getY();
            w = nko.getW();
            h = nko.getH();
        }

        nvgSave(ctx);
        nvgScissor(ctx, x, y, w, h);
        float vx = getViewX();
        float vy = getViewY();
        float vw = getViewW();
        float vh = getViewH();
        if (vx + vw < x || vx > x + w || vy > y + h || vy + vh < y) {
        } else {
            Nanovg.nvgIntersectScissor(ctx, vx, vy, vw, vh);

            nko.update(ctx);

//        if (focus == nko) {
//            nvgScissor(ctx, x, y, w, h);
//            nvgBeginPath(ctx);
//            Nanovg.nvgRect(ctx, x + 1, y + 1, w - 2, h - 2);
//            nvgStrokeColor(ctx, nvgRGBA(255, 0, 0, 255));
//            nvgStroke(ctx);
//
//            nvgBeginPath(ctx);
//            Nanovg.nvgRect(ctx, nko.getX() + 2, nko.getY() + 2, nko.getW() - 4, nko.getH() - 4);
//            nvgStrokeColor(ctx, nvgRGBA(0, 0, 255, 255));
//            nvgStroke(ctx);
//
//        }
            Nanovg.nvgRestore(ctx);
        }
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
        setFocus(findByXY(x, y));

        if (focus != null) {
            focus.mouseButtonEvent(button, pressed, x, y);
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

        setFocus(found);
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
    public void touchEvent(int phase, int x, int y) {
        GObject found = findByXY(x, y);
        if (found instanceof GMenu) {
            found.touchEvent(phase, x, y);
            return;
        }

        if (focus != null && focus.isInArea(x, y)) {
            focus.touchEvent(phase, x, y);
        } else {
            if (phase == Glfm.GLFMTouchPhaseBegan) {
                setFocus(found);
            }
            if (focus != null) {
                focus.touchEvent(phase, x, y);
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
            found.longTouchedEvent(x, y);
            return;
        }

        setFocus(found);

        if (focus != null) {
            focus.longTouchedEvent(x, y);
        }
    }

}
