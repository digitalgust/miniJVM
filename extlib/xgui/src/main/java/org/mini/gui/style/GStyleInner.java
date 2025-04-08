package org.mini.gui.style;


import static org.mini.gui.GToolkit.nvgRGBA;

/**
 * 用于存储其他style的值
 * <p>
 * 此类的的主要目的是,在GToolkit的静态变量中不存外部类的实例
 * 因为第三方应用GApplication结束时,还存在引用,这样可能导致第三方类无法释放
 */
public class GStyleInner extends GStyle {
    float textFontSize;
    float titleFontSize;
    float iconFontSize;
    float iconFontWidth;
    float[] textFontColor;
    float[] textShadowColor;
    float[] disabledTextFontColor;
    float[] frameBackground;
    float[] frameTitleColor;
    float[] hintFontColor;
    float[] editBackground;
    float[] selectedColor;
    float[] unselectedColor;
    float[] backgroundColor;
    float[] listBackgroundColor;
    float[] popBackgroundColor;
    float[] highColor;
    float[] lowColor;

    public GStyleInner(GStyle source) {
        set(source);
    }

    void set(GStyle source) {
        textFontSize = source.getTextFontSize();
        titleFontSize = source.getTitleFontSize();
        iconFontSize = source.getIconFontSize();
        iconFontWidth = source.getIconFontWidth();
        textFontColor = source.getTextFontColor();
        textShadowColor = source.getTextShadowColor();
        disabledTextFontColor = source.getDisabledTextFontColor();
        frameBackground = source.getFrameBackground();
        frameTitleColor = source.getFrameTitleColor();
        hintFontColor = source.getHintFontColor();
        editBackground = source.getEditBackground();
        selectedColor = source.getSelectedColor();
        unselectedColor = source.getUnselectedColor();
        backgroundColor = source.getBackgroundColor();
        listBackgroundColor = source.getListBackgroundColor();
        popBackgroundColor = source.getPopBackgroundColor();
        highColor = source.getHighColor();
        lowColor = source.getLowColor();
    }


    @Override
    public float getTextFontSize() {
        return textFontSize;
    }


    @Override
    public float getTitleFontSize() {
        return titleFontSize;
    }


    @Override
    public float getIconFontSize() {
        return iconFontSize;
    }

    @Override
    public float getIconFontWidth() {
        return iconFontWidth;
    }


    @Override
    public float[] getTextFontColor() {
        return textFontColor;
    }


    @Override
    public float[] getTextShadowColor() {
        return textShadowColor;
    }


    @Override
    public float[] getDisabledTextFontColor() {
        return disabledTextFontColor;
    }


    @Override
    public float[] getFrameBackground() {
        return frameBackground;
    }


    @Override
    public float[] getFrameTitleColor() {
        return frameTitleColor;
    }


    @Override
    public float[] getHintFontColor() {
        return hintFontColor;
    }


    @Override
    public float[] getEditBackground() {
        return editBackground;
    }


    @Override
    public float[] getSelectedColor() {
        return selectedColor;
    }


    @Override
    public float[] getUnselectedColor() {
        return unselectedColor;
    }


    @Override
    public float[] getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public float[] getListBackgroundColor() {
        return listBackgroundColor;
    }


    @Override
    public float[] getPopBackgroundColor() {
        return popBackgroundColor;
    }


    public float[] getHighColor() {
        return highColor;
    }

    public float[] getLowColor() {
        return lowColor;
    }

    public void copyFrom(GStyle style) {
        set(style);
    }
}
