/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.nanovg.Nanovg;

import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.gui.GToolkit.getStyle;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.nanovg.Nanovg.*;

/**
 * @author Gust
 */
public class GListItem extends GContainer {

    protected byte[] preicon_arr;
    protected String preicon;
    protected float[] preiconColor;

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

    public void setPreIcon(String preicon) {
        if (preicon == null || preicon.trim().length() == 0) return;
        this.preicon = preicon;
        preicon_arr = toCstyleBytes(preicon);
    }

    public float[] getPreiconColor() {
        return preiconColor;
    }

    public void setPreiconColor(float[] preiconColor) {
        this.preiconColor = preiconColor;
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
        GObject found = findSonByXY(x, y);
        if (found != null && found.actionListener != null && found.getOnClinkScript() == null) {
            super.touchEvent(touchid, phase, x, y);
        } else {
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
    }

    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        return false;
    }


    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        GObject found = findSonByXY(x, y);
        if (found != null && found.actionListener != null && found.getOnClinkScript() == null) {
            super.mouseButtonEvent(button, pressed, x, y);
        } else {
            if (pressed) {
                oldX = getX();
                oldY = getY();
            } else {
                if (validAction(x, y)) select();
            }
        }
    }

    int getIndex() {
        return parent.getElementsImpl().indexOf(this);
    }

    void select() {
        if (visible && enable) {
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
        super.paint(vg);
        float x = getX();
        float y = getY();
        float w = getW();
        float h = getH();

        boolean outOfFilter = false;
        if (list.isOutOfFilter(getIndex())) {
            outOfFilter = true;
        }
        float pad = 2;
        float thumb = list.list_item_heigh - pad;

        float tx, ty;
        tx = x + pad;
        ty = y + pad * .5f;
        float tw = w - (pad * 2);
        float th = list.list_item_heigh - pad;

        nvgSave(vg);
        Nanovg.nvgIntersectScissor(vg, tx, ty, tw, th);


        if (list.isSelected(getIndex())) {
            GToolkit.drawRect(vg, tx, ty, tw, th, GToolkit.getStyle().getSelectedColor());
        } else {
            GToolkit.drawRect(vg, tx, ty, tw, th, GToolkit.getStyle().getUnselectedColor());
        }
        float[] c = outOfFilter ? GToolkit.getStyle().getHintFontColor() : enable ? getColor() : getDisabledColor();

        if (img != null) {
            GToolkit.drawImage(vg, img, tx + pad, ty + pad, thumb - pad * 2, thumb - pad * 2, !outOfFilter, outOfFilter ? 0.5f : 0.8f);
        } else if (preicon_arr != null) {
            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());
            float[] pc = preiconColor == null ? getStyle().getTextFontColor() : preiconColor;
            pc = enable ? pc : getDisabledColor();
            nvgFillColor(vg, pc);
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgTextJni(vg, x + thumb * 0.5f, y + thumb * 0.5f + 2, preicon_arr, 0, preicon_arr.length);
        }
        nvgFillColor(vg, c);
        GToolkit.drawTextLine(vg, tx + ((img == null && preicon_arr == null) ? 0 : thumb) + pad, ty + thumb / 2, getText(), list.getFontSize(), c, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        Nanovg.nvgRestore(vg);
        return true;
    }

}
