package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GSwitch;
import org.mini.layout.xmlpull.KXmlParser;

public class XSwitch
        extends XObject {
    static public final String XML_NAME = "switch";

    GSwitch switcher;
    protected String onClick;

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
        if (attName.equals("onclick")) {
            onClick = attValue;
        }
    }


    protected int getDefaultWidth(int parentViewW) {
        return (int) GSwitch.DEFAULT_WIDTH;
    }

    protected int getDefaultHeight(int parentViewH) {
        return (int) GSwitch.DEFAULT_HEIGHT;
    }

    protected void createGui() {
        if (switcher == null) {
            switcher = new GSwitch(getAssist().getForm());
            initGui();
        } else {
            switcher.setLocation(x, y);
            switcher.setSize(width, height);
        }
    }


    public GObject getGui() {
        return switcher;
    }
}
