/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media.audio;

/**
 *
 * @author Gust
 */
public interface AudioListener {

    void onCapture(int millSecond, byte[] data);
    
    void onPlayback(int millSecond, byte[] data);

    void onStop();
}
