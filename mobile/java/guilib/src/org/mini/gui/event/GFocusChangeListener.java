/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui.event;

import org.mini.gui.GObject;

/**
 *
 * @author Gust
 */
public interface GFocusChangeListener {

    public void focusGot(GObject go);

    public void focusLost(GObject go);
}
