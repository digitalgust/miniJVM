package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.layout.xmlpull.KXmlParser;


public class XBr
        extends XObject {
    static public final String XML_NAME = "br";

    public XBr(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        toEndTag(parser, XML_NAME);
    }

    protected int getDefaultWidth(int parentViewW) {
        return parentViewW;
    }

    protected int getDefaultHeight(int parentViewH) {
        return 1;
    }


    protected <T extends GObject> T createGuiImpl() {
        return null;
    }

    @Override
    protected void createAndSetGui() {

    }

    @Override
    public GObject getGui() {
        return null;
    }
}
