package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GSwitch;
import org.mini.layout.loader.XmlExtAssist;
import org.xmlpull.v1.KXmlParser;

public class XSwitch
        extends XObject {
    static public final String XML_NAME = "switch";

    GSwitch switcher;
    protected boolean selected = true;

    public XSwitch(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    boolean parseNoTagText() {
        return false;
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
        toEndTag(parser, XML_NAME);
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("selected")) {
            selected = "0".equals(attValue) ? false : true;
        }
    }

    protected int getDefaultWidth(int parentViewW) {
        return (int) GSwitch.DEFAULT_WIDTH;
    }

    protected int getDefaultHeight(int parentViewH) {
        return (int) GSwitch.DEFAULT_HEIGHT;
    }

    protected <T extends GObject> T createGuiImpl() {
        return (T) new GSwitch(getAssist().getForm(), selected, x, y, width, height);
    }

    protected void createAndSetGui() {
        if (switcher == null) {
            switcher = createGuiImpl();
            initGuiMore();
        } else {
            switcher.setLocation(x, y);
            switcher.setSize(width, height);
        }
    }


    public GObject getGui() {
        return switcher;
    }
}
