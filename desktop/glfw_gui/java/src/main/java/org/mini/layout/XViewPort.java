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

    protected <T extends GObject> T createGuiImpl() {
        return (T) new GViewPort(getAssist().getForm());
    }

    protected void createAndSetGui() {
        if (viewPort == null) {
            viewPort = createGuiImpl();
            initGuiMore();
            viewPort.setLocation(x, y);
            viewPort.setSize(width, height);
        } else {
            viewPort.setLocation(x, y);
            viewPort.setSize(width, height);
        }
    }
}
