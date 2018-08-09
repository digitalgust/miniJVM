/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

/**
 *
 * @author gust
 */
public class GPanel extends GContainer {

    protected float[] viewBoundle = new float[4];//可视窗口边界, 
    float minX, maxX, minY, maxY;
    float scrollx;
    float scrolly;

    public void setLocation(float x, float y) {
        viewBoundle[LEFT] = x;
        viewBoundle[TOP] = y;
        super.setLocation(x, y);
    }

    public void setSize(float w, float h) {
        viewBoundle[WIDTH] = w;
        viewBoundle[HEIGHT] = h;
        super.setSize(w, h);
    }

    public float getViewX() {
        if (parent != null) {
            return parent.getViewX() + viewBoundle[LEFT];
        }
        return viewBoundle[LEFT];
    }

    public float getViewY() {
        if (parent != null) {
            return parent.getViewY() + viewBoundle[TOP];
        }
        return viewBoundle[TOP];
    }

    public float getViewW() {
        return viewBoundle[WIDTH];
    }

    public float getViewH() {
        return viewBoundle[HEIGHT];
    }

    @Override
    public void onAdd(GObject obj) {
        reBoundle();
    }

    @Override
    public void onRemove(GObject obj) {
        reBoundle();
    }

    public void reBoundle() {
        minX = 0;
        minY = 0;
        maxX = minX + viewBoundle[WIDTH];
        maxY = minY + viewBoundle[HEIGHT];
        for (GObject nko : elements) {
            if (nko.boundle[LEFT] < minX) {
                minX = nko.boundle[LEFT];
            }
            if (nko.boundle[LEFT] + nko.boundle[WIDTH] > maxX) {
                maxX = nko.boundle[LEFT] + nko.boundle[WIDTH];
            }
            if (nko.boundle[TOP] < minY) {
                minY = nko.boundle[TOP];
            }
            if (nko.boundle[TOP] + nko.boundle[HEIGHT] > maxY) {
                maxY = nko.boundle[TOP] + nko.boundle[HEIGHT];
            }
        }
        if (maxX - minX > viewBoundle[WIDTH]) {
            this.boundle[WIDTH] = maxX - minX;
        }
        if (maxY - minY > viewBoundle[HEIGHT]) {
            this.boundle[HEIGHT] = maxY - minY;
        }
    }

    @Override
    public void scrollEvent(double scrollX, double scrollY, int x, int y) {

        boolean found = false;
        for (GObject go : elements) {
            if (go.isInArea(x, y)) {
                go.scrollEvent(scrollX, scrollY, x, y);
                found = true;
                break;
            }
        }
        if (!found) {
            float dw = getOutOfViewWidth();
            float dh = getOutOfViewHeight();
            float dx = (dw == 0) ? 0.f : (float) scrollX / dw;
            float dy = (dh == 0) ? 0.f : (float) scrollY / dh;
            setScrollX(dx);
            setScrollY(dy);
        }
    }

    void setScrollY(float dy) {
        this.scrolly -= dy;
        if (scrolly < 0) {
            scrolly = 0;
        }
        if (scrolly > 1) {
            scrolly = 1;
        }
        boundle[TOP] = viewBoundle[TOP] + (-minY) - scrolly * getOutOfViewHeight();
    }

    void setScrollX(float dx) {
        this.scrollx -= dx;
        if (scrollx < 0) {
            scrollx = 0;
        }
        if (scrollx > 1) {
            scrollx = 1;
        }
        boundle[LEFT] = viewBoundle[LEFT] + (-minX) - scrollx * getOutOfViewWidth();
    }

    float getOutOfViewHeight() {
        return boundle[HEIGHT] - viewBoundle[HEIGHT];
    }

    float getOutOfViewWidth() {
        return boundle[WIDTH] - viewBoundle[WIDTH];
    }

}
