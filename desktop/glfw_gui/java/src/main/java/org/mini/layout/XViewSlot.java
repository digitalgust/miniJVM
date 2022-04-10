package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GViewSlot;
import org.mini.gui.event.GStateChangeListener;

/**
 * <viewslot>
 * <panel>aaaaaa</></panel>
 * <panel>bbbbbb</panel>
 * </viewslot>
 */

public class XViewSlot extends XContainer {
    static public final String XML_NAME = "viewslot";

    protected int scroll = GViewSlot.SCROLL_MODE_HORIZONTAL;

    protected GViewSlot viewSlot;

    public XViewSlot() {
        super(null);
    }

    public XViewSlot(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("scroll")) {
            scroll = attValue.equalsIgnoreCase("h") ? GViewSlot.SCROLL_MODE_HORIZONTAL : GViewSlot.SCROLL_MODE_VERTICAL;
        }
    }

    int parseMoveMode(String modeStr) {
        int mode = GViewSlot.MOVE_FIXED;
        if (modeStr != null) {
            for (String move : modeStr.split(",")) {
                if (move.equalsIgnoreCase("left")) {
                    mode |= GViewSlot.MOVE_LEFT;
                } else if (move.equalsIgnoreCase("right")) {
                    mode |= GViewSlot.MOVE_RIGHT;
                } else if (move.equalsIgnoreCase("up")) {
                    mode |= GViewSlot.MOVE_UP;
                } else if (move.equalsIgnoreCase("down")) {
                    mode |= GViewSlot.MOVE_DOWN;
                }
            }
        }
        return mode;
    }


    void align() {
        super.align();
        for (int i = 0; i < children.size(); i++) {
            XObject xo = children.get(i);
            xo.raw_widthPercent = 100;
            xo.raw_heightPercent = 100;
            if (xo instanceof XContainer) {
                ((XContainer) xo).reSize(this.viewW, this.viewH);
            }
            viewSlot.setSlotMoveMode(i, parseMoveMode(children.get(i).moveMode));
        }
        viewSlot.reSizeChildren();
    }

    @Override
    public GObject getGui() {
        return viewSlot;
    }

    protected void createGui() {
        if (viewSlot == null) {
            viewSlot = new GViewSlot(x, y, width, height, scroll);
            initGui();
            viewSlot.setLocation(x, y);
            viewSlot.setSize(width, height);
            viewSlot.setStateChangeListener(getRoot().getEventHandler());

        } else {
            viewSlot.setLocation(x, y);
            viewSlot.setSize(width, height);
            for (int i = 0; i < viewSlot.getElementSize(); i++) {
                viewSlot.setSlotMoveMode(i, parseMoveMode(children.get(i).moveMode));
            }
            viewSlot.reSizeChildren();
        }

    }

}
