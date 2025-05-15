package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GPanel;

public class XPanel extends XContainer {
    static public final String XML_NAME = "panel";


    protected GPanel panel;

    public XPanel(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }


    public GObject getGui() {
        return panel;
    }


    protected <T extends GObject> T createGuiImpl() {
        return (T) new GPanel(getAssist().getForm(), x, y, width, height);
    }

    protected void createAndSetGui() {
        if (panel == null) {
            panel = createGuiImpl();
            initGuiMore();
        } else {
            panel.setLocation(x, y);
            panel.setSize(width, height);
        }
        super.createAndSetGui();
    }
}
