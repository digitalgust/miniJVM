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
public interface GStyle {

    float getTextFontSize();

    float getTitleFontSize();

    float getIconFontSize();

    float[] getTextFontColor();

    float[] getHintFontColor();
    
    float[] getSelectedColor();

    float[] getEditBackground();

    float[] getFrameBackground();

    float[] getFrameTitleColor();

    float getIconFontWidth();

    void drawEditBoxBase(long vg, float x, float y, float w, float h);

    void drawFieldBoxBase(long vg, float x, float y, float w, float h);
}
