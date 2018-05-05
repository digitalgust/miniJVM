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
        boundle[LEFT] = x;
        boundle[TOP] = y;
        boundle[WIDTH] = w;
        boundle[HEIGHT] = h;
    }

    public boolean update(long vg) {
        if (g == null) {
            g = new GGraphics(this, vg);
        }
        g.save();
        paint(g);
        g.restore();
        super.update(vg);
        return true;
    }

    public void paint(GGraphics g) {

    }
}
