/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui.event;

/**
 *
 * @author Gust
 */
public interface GAppActiveListener {
    /**
     * 应用程序前后台切换, 
     * 
     * application change to active or background 
     * 
     * @param active 
     */
    void onAppActive(boolean active);
}
