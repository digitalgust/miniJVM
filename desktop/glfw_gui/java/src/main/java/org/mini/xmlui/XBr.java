package org.mini.xmlui;

import org.mini.gui.GObject;
import org.mini.xmlui.xmlpull.KXmlParser;


public class XBr
        extends XObject {
    static public final String XML_NAME = "br";


    public XBr(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    public void parse(KXmlParser parser) throws Exception {
        toEndTag(parser, XML_NAME);
    }

    void preAlignVertical() {
        viewH = height = 1;

    }

    void preAlignHorizontal() {
        viewW = width = parent.viewW;
    }

    @Override
    void createGui() {

    }

    @Override
    public GObject getGui() {
        return null;
    }
}
