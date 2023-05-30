package org.mini.layout;

import org.mini.gui.GCanvas;
import org.mini.gui.GObject;

public class XCanvas extends XPanel {
    static public final String XML_NAME = "canvas";


    public XCanvas(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }


    protected GObject createGuiImpl() {
        return new GCanvas(getAssist().getForm(), x, y, width, height);
    }

}
