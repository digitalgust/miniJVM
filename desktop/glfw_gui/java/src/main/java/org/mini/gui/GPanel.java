/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

/**
 * @author gust
 */
public class GPanel extends GContainer {

    public GPanel() {
        this(0f, 0f, 1f, 1f);
    }

    public GPanel(int left, int top, int width, int height) {
        this((float) left, top, width, height);
    }

    public GPanel(float left, float top, float width, float height) {
        setLocation(left, top);
        setSize(width, height);
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

}
