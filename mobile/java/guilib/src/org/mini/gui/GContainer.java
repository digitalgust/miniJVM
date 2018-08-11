/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.mini.glfm.Glfm;
import static org.mini.gui.GToolkit.nvgRGBA;
import org.mini.nanovg.Nanovg;
import static org.mini.nanovg.Nanovg.nvgBeginPath;
import static org.mini.nanovg.Nanovg.nvgSave;
import static org.mini.nanovg.Nanovg.nvgScissor;
import static org.mini.nanovg.Nanovg.nvgStroke;
import static org.mini.nanovg.Nanovg.nvgStrokeColor;

/**
 *
 * @author gust
 */
abstract public class GContainer extends GObject {

    LinkedList<GObject> elements = new LinkedList();
    GObject focus;

    //异步添加删除form
    List<AddRemoveItem> cache = Collections.synchronizedList(new LinkedList());
    List<AddRemoveItem> cacheBack = Collections.synchronizedList(new LinkedList());

    class AddRemoveItem {

        static final int ADD = 0;
        static final int REMOVE = 1;
        int operation;
        GObject go;

        AddRemoveItem(int op, GObject go) {
            operation = op;
            this.go = go;
        }
    }

    GObject findFocus(float x, float y) {
        for (Iterator<GObject> it = elements.iterator(); it.hasNext();) {
            GObject nko = it.next();
            if (nko.isInArea(x, y)) {
                return nko;
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
     * @param focus the focus to set
     */
    public void setFocus(GObject go) {
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
            cache.add(new AddRemoveItem(AddRemoveItem.ADD, nko));
            nko.init();
            nko.setParent(this);
        }
    }

    public void add(int index, GObject nko) {
        if (nko != null) {
            cache.add(index, new AddRemoveItem(AddRemoveItem.ADD, nko));
            nko.init();
            nko.setParent(this);
        }
    }

    public void remove(GObject nko) {
        if (nko != null) {
            nko.setParent(null);
            nko.destory();
            cache.add(new AddRemoveItem(AddRemoveItem.REMOVE, nko));
        }
    }

    public void remove(int index) {
        GObject nko = elements.get(index);
        if (nko != null) {
            nko.setParent(null);
            nko.destory();
            cache.add(new AddRemoveItem(AddRemoveItem.REMOVE, nko));
        }
    }

    public boolean isSon(GObject son) {
        return elements.contains(son);
    }

    public void onAdd(GObject obj) {

    }

    public void onRemove(GObject obj) {

    }

    public void reBoundle() {

    }

    @Override
    public boolean update(long ctx) {
        int menuCount = 0;
        List tmp = cacheBack;
        cacheBack = cache;
        cache = tmp;
        //菜单加在最前面,focus 在之后,其他组件再在其后
        for (AddRemoveItem ari : cacheBack) {
            if (ari.operation == AddRemoveItem.ADD) {
                setFocus(ari.go);
                if (ari.go instanceof GMenu) {
                    menuCount++;
                    elements.addFirst(ari.go);
                } else {
                    elements.add(menuCount, ari.go);
                }
                onAdd(ari.go);
            } else {
                boolean success = elements.remove(ari.go);
                setFocus(null);
                if (success && ari.go instanceof GMenu) {
                    menuCount--;
                }
                onRemove(ari.go);
            }
        }
        cacheBack.clear();
        //如果focus不是第一个，则移到第一个，这样遮挡关系才正确
        if (focus != null && !(focus instanceof GMenu)&&this instanceof GForm) {
            elements.remove(focus);
            elements.add(menuCount, focus);
        }
        //更新所有UI组件
        GObject[] arr = elements.toArray(new GObject[elements.size()]);
        for (int i = arr.length - 1; i >= 0; i--) {
            GObject nko = arr[i];
            if (nko.isVisable()) {
                float x = nko.getViewX();
                float y = nko.getViewY();
                float w = nko.getViewW();
                float h = nko.getViewH();

                nvgSave(ctx);
                nvgScissor(ctx, x, y, w, h);
                Nanovg.nvgIntersectScissor(ctx, getViewX(), getViewY(), getViewW(), getViewH());
                nko.update(ctx);

//                if (focus == nko) {
//                    nvgScissor(ctx, x, y, w, h);
//                    nvgBeginPath(ctx);
//                    Nanovg.nvgRect(ctx, x + 1, y + 1, w - 2, h - 2);
//                    nvgStrokeColor(ctx, nvgRGBA(255, 0, 0, 255));
//                    nvgStroke(ctx);
//
//                    nvgBeginPath(ctx);
//                    Nanovg.nvgRect(ctx, nko.getX() + 2, nko.getY() + 2, nko.getW() - 4, nko.getH() - 4);
//                    nvgStrokeColor(ctx, nvgRGBA(0, 0, 255, 255));
//                    nvgStroke(ctx);
//
//                }

                Nanovg.nvgRestore(ctx);
            }
        }
        return true;
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
        setFocus(findFocus(x, y));

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
    public void scrollEvent(float scrollX, float scrollY, float x, float y) {
        setFocus(findFocus(x, y));
        if (focus != null && focus.isInArea(x, y)) {
            focus.scrollEvent(scrollX, scrollY, x, y);
        }
    }

    @Override
    public void clickEvent(int button, int x, int y) {
        if (focus != null && focus.isInArea(x, y)) {
            focus.clickEvent(button, x, y);
        }
    }

    public void dragEvent(float dx, float dy, float x, float y) {
        setFocus(findFocus(x, y));
        if (focus != null && focus.isInArea(x, y)) {
            focus.dragEvent(dx, dy, x, y);
        }
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
        if (phase == Glfm.GLFMTouchPhaseBegan) {
            setFocus(findFocus(x, y));
        }
        if (focus != null) {
            focus.touchEvent(phase, x, y);
        }
    }

    @Override
    public void inertiaEvent(float x1, float y1, float x2, float y2, long moveTime) {

        if (focus != null && focus.isInArea((float) x1, (float) y1)) {
            focus.inertiaEvent(x1, y1, x2, y2, moveTime);
        }
    }

    @Override
    public void longTouchedEvent(int x, int y) {
        setFocus(findFocus(x, y));

        if (focus != null) {
            focus.longTouchedEvent(x, y);
        }
    }
}
