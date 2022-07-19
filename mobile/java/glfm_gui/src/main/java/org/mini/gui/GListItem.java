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

    public GListItem(GForm form, GImage img, String lab) {
        super(form);
        this.img = img;
        setText(lab);
    }


    @Override
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

    float oldX, oldY;

    private boolean validAction(float releaseX, float releaseY) {
        if (releaseX >= oldX && releaseX <= oldX + getW() && releaseY >= oldY && releaseY < oldY + getH()) {
            return true;
        }
        return false;
    }

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan:
                oldX = getX();
                oldY = getY();
                break;
            case Glfm.GLFMTouchPhaseMoved:
                break;
            case Glfm.GLFMTouchPhaseEnded:
                if (validAction(x, y)) select();
                break;
            default:
                break;
        }

    }

    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        return false;
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (pressed) {
            oldX = getX();
            oldY = getY();
        } else {
            if (validAction(x, y)) select();
        }
    }

    int getIndex() {
        return parent.getElementsImpl().indexOf(this);
    }

    void select() {
        if (enable) {
            int index = getIndex();
            list.select(index);
            list.pulldown = false;
            list.changeCurPanel();
            list.doStateChanged(list);
            GForm.flush();
            doAction();
        }
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
            nvgImageSize(vg, img.getNvgTextureId(vg), imgw, imgh);
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
        float[] c = outOfFilter ? GToolkit.getStyle().getHintFontColor() : enable ? getColor() : getDisabledColor();
        GToolkit.drawTextLine(vg, tx + (img == null ? 0 : thumb) + pad, ty + thumb / 2, getText(), list.getFontSize(), c, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        return true;
    }

}
