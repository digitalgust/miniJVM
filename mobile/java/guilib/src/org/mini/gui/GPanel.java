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

    @Override
    public float getViewX() {
        return getX();
    }

    @Override
    public float getViewY() {
        return getY();
    }

    @Override
    public float getViewW() {
        return getW();
    }

    @Override
    public float getViewH() {
        return getH();
    }

    @Override
    public int getType() {
        return TYPE_PANEL;
    }

    @Override
    public void setViewLocation(float x, float y) {
        setLocation(x, y);
    }

    @Override
    public void setViewSize(float x, float y) {
        setSize(x, y);
    }

    @Override
    public float[] getViewBoundle() {
        return getBoundle();
    }

}
