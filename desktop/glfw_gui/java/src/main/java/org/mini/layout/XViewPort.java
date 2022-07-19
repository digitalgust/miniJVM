package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GViewPort;

public class XViewPort extends XContainer {
    static public final String XML_NAME = "viewport";
    protected GViewPort viewPort;

    public XViewPort(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }

    @Override
    public GObject getGui() {
        return viewPort;
    }

    protected void createGui() {
        if (viewPort == null) {
            viewPort = new GViewPort(getAssist().getForm());
            initGui();
            viewPort.setLocation(x, y);
            viewPort.setSize(width, height);
        } else {
            viewPort.setLocation(x, y);
            viewPort.setSize(width, height);
        }
    }
}
