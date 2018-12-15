/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.List;

/**
 *
 * @author gust
 */
public class GPanel extends GContainer {

    @Override
    public int getType() {
        return TYPE_PANEL;
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

    @Override
    public List<GObject> getElements() {
        return super.getElements();
    }

    @Override
    public int getElementSize() {
        return elements.size();
    }

    @Override
    public void add(GObject nko) {
        super.add(nko);
    }

    @Override
    public void add(int index, GObject nko) {
        super.add(index, nko);
    }

    @Override
    public void remove(GObject nko) {
        super.remove(nko);
    }

    @Override
    public void remove(int index) {
        super.remove(index);
    }

    @Override
    public boolean contains(GObject son) {
        return super.contains(son);
    }

    @Override
    public void clear() {
        super.clear();
    }
}
