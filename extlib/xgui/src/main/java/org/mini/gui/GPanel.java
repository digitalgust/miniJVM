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

    public GPanel(GForm form) {
        this(form, 0f, 0f, 1f, 1f);
    }

    public GPanel(GForm form, float left, float top, float width, float height) {
        super(form);
        setLocation(left, top);
        setSize(width, height);
    }


    @Override
    public boolean paint(long vg) {
        if (getBgImg() == null && bgColor != null) {
            GToolkit.drawRect(vg, getX(), getY(), getW(), getH(), bgColor);
        }
        boolean ret = super.paint(vg);
        return ret;
    }
}
