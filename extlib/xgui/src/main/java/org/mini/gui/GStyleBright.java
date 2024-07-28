/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import static org.mini.gui.GToolkit.nvgRGBA;

/**
 * @author gust
 */
public class GStyleBright extends GStyle {

    @Override
    public float getTextFontSize() {
        return 16f;
    }

    @Override
    public float getTitleFontSize() {
        return 18f;
    }

    @Override
    public float getIconFontSize() {
        return 35f;
    }

    float[] textFontColor = nvgRGBA(0x0, 0x0, 0x0, 0xb0);

    @Override
    public float[] getTextFontColor() {
        return textFontColor;
    }

    float[] textShadowColor = nvgRGBA(0xff, 0xff, 0xff, 0xb0);

    @Override
    public float[] getTextShadowColor() {
        return textShadowColor;
    }

    float[] disabledTextFontColor = nvgRGBA(0x72, 0x72, 0x72, 0x80);

    @Override
    public float[] getDisabledTextFontColor() {
        return disabledTextFontColor;
    }

    float[] frameBackground = nvgRGBA(0xdc, 0xdc, 0xdc, 0xff);

    @Override
    public float[] getFrameBackground() {
        return frameBackground;
    }

    float[] frameTitleColor = nvgRGBA(0x40, 0x40, 0x40, 0xff);

    @Override
    public float[] getFrameTitleColor() {
        return frameTitleColor;
    }

    float[] hintFontColor = nvgRGBA(0x72, 0x72, 0x72, 64);

    @Override
    public float[] getHintFontColor() {
        return hintFontColor;
    }

    float[] editBackground = nvgRGBA(0xfc, 0xfc, 0xfc, 0xff);

    @Override
    public float[] getEditBackground() {
        return editBackground;
    }

    @Override
    public float getIconFontWidth() {
        return 18;
    }

    float[] selectedColor = nvgRGBA(0x80, 0x80, 0xff, 0x40);

    @Override
    public float[] getSelectedColor() {
        return selectedColor;
    }

    float[] unselectedColor = nvgRGBA(0x80, 0x80, 0x80, 0x10);

    @Override
    public float[] getUnselectedColor() {
        return unselectedColor;
    }

    float[] backgroundColor = nvgRGBA(0xec, 0xec, 0xec, 0xff);

    @Override
    public float[] getBackgroundColor() {
        return backgroundColor;
    }

    float[] listBackgroundColor = nvgRGBA(0xec, 0xec, 0xec, 0x30);

    @Override
    public float[] getListBackgroundColor() {
        return listBackgroundColor;
    }

    float[] popBackgroundColor = nvgRGBA(0xec, 0xec, 0xec, 0xd0);

    @Override
    public float[] getPopBackgroundColor() {
        return popBackgroundColor;
    }

    float[] highColor = nvgRGBA(0xff, 0xff, 0xff, 0xd0);//0x006080ff

    public float[] getHighColor() {
        return highColor;
    }

    float[] lowColor = nvgRGBA(0xff, 0xff, 0xff, 0x80);//0x801010ff

    public float[] getLowColor() {
        return lowColor;
    }
}
