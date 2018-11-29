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

    /**
     * 
     * 
     * @param oldgo the old focus
     */
    public void focusGot(GObject oldgo);

    /**
     * 
     * @param newgo the new focus
     */
    public void focusLost(GObject newgo);
}
