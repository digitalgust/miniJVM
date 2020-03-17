package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GViewSlot;

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
        for (int i = 0; i < size(); i++) {
            XObject xo = elementAt(i);
            if (xo instanceof XContainer) {
                ((XContainer) xo).align();
            }
        }
    }

    @Override
    public GObject getGui() {
        return viewSlot;
    }

    protected void createGui() {
        if (viewSlot == null) {
            viewSlot = new GViewSlot(x, y, width, height, scroll);
            viewSlot.setLocation(x, y);
            viewSlot.setSize(width, height);
            viewSlot.setName(name);
            viewSlot.setAttachment(this);
            for (int i = 0; i < size(); i++) {
                XObject xo = elementAt(i);
                GObject go = xo.getGui();
                if (go != null) viewSlot.add(i, go, parseMoveMode(xo.moveMode));
            }
        } else {
            viewSlot.setLocation(x, y);
            viewSlot.setSize(width, height);
            viewSlot.clear();
            for (int i = 0; i < size(); i++) {
                XObject xo = elementAt(i);
                GObject go = xo.getGui();
                if (go != null) viewSlot.add(i, go, parseMoveMode(xo.moveMode));
            }
        }
    }
}
