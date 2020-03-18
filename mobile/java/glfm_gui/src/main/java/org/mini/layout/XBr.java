package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.layout.xmlpull.KXmlParser;


public class XBr
        extends XObject {
    static public final String XML_NAME = "br";

    public XBr() {
        super(null);
    }

    public XBr(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    public void parse(KXmlParser parser) throws Exception {
        toEndTag(parser, XML_NAME);
    }

    protected int getDefaultWidth(int parentViewW) {
        return parentViewW;
    }

    protected int getDefaultHeight(int parentViewH) {
        return 1;
    }


    @Override
    protected void createGui() {

    }

    @Override
    public GObject getGui() {
        return null;
    }
}
