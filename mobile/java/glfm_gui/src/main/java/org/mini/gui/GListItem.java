/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;

import static org.mini.nanovg.Nanovg.*;

/**
 * @author Gust
 */
public class GListItem extends GObject {

    protected GImage img;
    protected GList list;

    public GListItem(GImage img, String lab) {
        this.img = img;
        setText(lab);
        setFontSize(GToolkit.getStyle().getTextFontSize());
        setColor(GToolkit.getStyle().getTextFontColor());
    }


    public GContainer getParent() {
        return list;
    }

    /**
     * @return the img
     */
    public GImage getImg() {
        return img;
    }

    /**
     * @param img the img to set
     */
    public void setImg(GImage img) {
        this.img = img;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return getText();
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        setText(label);
    }

    int mouseX, mouseY;
    int draged = 0;//拖动事件次数

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan:
                mouseX = x;
                mouseY = y;
                draged = 0;
                break;
            case Glfm.GLFMTouchPhaseMoved:
                break;
            case Glfm.GLFMTouchPhaseEnded:
                if (draged < 3) { //防拖动后触发点击
                    if (Math.abs(y - mouseY) < list.list_item_heigh && Math.abs(x - mouseX) < list.list_item_heigh) {
                        select();
                    }
                }
                draged = 0;
                break;
            default:
                break;
        }

    }

    public boolean dragEvent(float dx, float dy, float x, float y) {
        draged++;
        return false;
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (pressed) {
            mouseY = y;
            draged = 0;
        } else if (Math.abs(y - mouseY) < list.list_item_heigh) {
            if (draged < 3) {
                select();
            }
            draged = 0;
        }
    }

    int getIndex() {
        return parent.getElementsImpl().indexOf(this);
    }

    void select() {
        int index = getIndex();
        list.select(index);
        list.pulldown = false;
        list.changeCurPanel();
        list.doStateChanged(list);
        flush();
        doAction();
    }

    @Override
    public boolean paint(long vg) {
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        boolean outOfFilter = false;
        if (list.isOutOfFilter(getIndex())) {
            outOfFilter = true;
        }
        float pad = 2;
        float ix, iy, iw, ih;
        float thumb = list.list_item_heigh - pad;
        int[] imgw = {0}, imgh = {0};

        float tx, ty;
        tx = x + pad;
        ty = y + pad * .5f;

        if (list.isSelected(getIndex())) {
            GToolkit.drawRect(vg, tx, ty, w - (pad * 2), list.list_item_heigh - pad, GToolkit.getStyle().getSelectedColor());
        } else {
            GToolkit.drawRect(vg, tx, ty, w - (pad * 2), list.list_item_heigh - pad, GToolkit.getStyle().getUnselectedColor());
        }

        if (img != null) {
            nvgImageSize(vg, img.getTexture(vg), imgw, imgh);
            if (imgw[0] < imgh[0]) {
                iw = thumb;
                ih = iw * (float) imgh[0] / (float) imgw[0];
                ix = 0;
                iy = -(ih - thumb) * 0.5f;
            } else {
                ih = thumb;
                iw = ih * (float) imgw[0] / (float) imgh[0];
                ix = -(iw - thumb) * 0.5f;
                iy = 0;
            }
            GToolkit.drawImage(vg, img, tx, ty, thumb, thumb, !outOfFilter, outOfFilter ? 0.5f : 0.8f);
        }
        float[] c = outOfFilter ? GToolkit.getStyle().getHintFontColor() : enable ? list.color : list.disabledColor;
        GToolkit.drawTextLine(vg, tx + (img == null ? 0 : thumb) + pad, ty + thumb / 2, getText(), list.fontSize, c, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        return true;
    }

}
