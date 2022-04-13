package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GSwitcher;
import org.mini.gui.event.GActionListener;
import org.mini.gui.gscript.Interpreter;
import org.mini.layout.xmlpull.KXmlParser;

public class XSwitcher
        extends XObject {
    static public final String XML_NAME = "switcher";

    GSwitcher switcher;
    protected String onClick;

    public XSwitcher() {
        super(null);
    }

    public XSwitcher(XContainer xc) {
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
        return (int) GSwitcher.DEFAULT_WIDTH;
    }

    protected int getDefaultHeight(int parentViewH) {
        return (int) GSwitcher.DEFAULT_HEIGHT;
    }

    protected void createGui() {
        if (switcher == null) {
            switcher = new GSwitcher();
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
