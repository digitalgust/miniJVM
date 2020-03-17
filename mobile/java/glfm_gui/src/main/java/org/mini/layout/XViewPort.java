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
            viewPort = new GViewPort();
            viewPort.setLocation(x, y);
            viewPort.setSize(width, height);
            viewPort.setName(name);
            viewPort.setAttachment(this);
            for (int i = 0; i < size(); i++) {
                XObject xo = elementAt(i);
                GObject go = xo.getGui();
                if (go != null) viewPort.add(go);
            }
        } else {
            viewPort.setLocation(x, y);
            viewPort.setSize(width, height);
        }
    }
}
