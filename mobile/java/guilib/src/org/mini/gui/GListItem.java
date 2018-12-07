/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import static org.mini.gui.GObject.TYPE_LISTITEM;
import static org.mini.nanovg.Nanovg.nvgImageSize;

/**
 *
 * @author Gust
 */
public class GListItem extends GObject {

    GImage img;
    String label;
    GList list;

    public GListItem(GImage img, String lab) {
        this.img = img;
        this.label = lab;
    }

    @Override
    public int getType() {
        return TYPE_LISTITEM;
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
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    int mouseX, mouseY;

    @Override
    public void touchEvent(int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan:
                mouseX = x;
                mouseY = y;
                break;
            case Glfm.GLFMTouchPhaseMoved:
                break;
            case Glfm.GLFMTouchPhaseEnded:
                if (Math.abs(y - mouseY) < list.list_item_heigh && Math.abs(x - mouseX) < list.list_item_heigh) {
                    select();
                }
                break;
            default:
                break;
        }

    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        if (pressed) {
            mouseY = y;
        } else if (Math.abs(y - mouseY) < list.list_item_heigh) {
            select();
        }
    }

    int getIndex() {
        return parent.getElements().indexOf(this);
    }

    void select() {
        int index = getIndex();
        list.select(index);
        list.pulldown = false;
        list.changeCurPanel();
        flush();
        if (actionListener != null) {
            actionListener.action(this);
        }
    }

    @Override
    public boolean update(long vg) {
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        boolean outOfFilter = false;
        if (list.isOutOfFilter(getIndex())) {
            outOfFilter = true;
        }
        float pad = 5;
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
//            GList.drawImage(vg, tx, ty, thumb, thumb, img);
            GToolkit.drawImage(vg, img, tx, ty, thumb, thumb, !outOfFilter, outOfFilter ? 0.5f : 0.8f);
        }
        float[] c = outOfFilter ? GToolkit.getStyle().getHintFontColor() : GToolkit.getStyle().getTextFontColor();
        GList.drawText(vg, tx + thumb + pad, ty + thumb / 2, thumb, thumb, label, c);
        return true;
    }

}
