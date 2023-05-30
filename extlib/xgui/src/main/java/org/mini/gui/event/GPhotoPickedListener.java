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
public interface GPhotoPickedListener {
    void onPicked(int uid, String url, byte[] data);
}
