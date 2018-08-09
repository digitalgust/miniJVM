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
public class GCanvas extends GContainer {

    GGraphics g;

    public GCanvas(int x, int y, int w, int h) {
        setLocation(x, y);
        setSize(w, h);
    }

    public boolean update(long vg) {
        if (g == null) {
            g = new GGraphics(this, vg);
        }
        paint(g);
        super.update(vg);
        return true;
    }

    public void paint(GGraphics g) {

    }
}
