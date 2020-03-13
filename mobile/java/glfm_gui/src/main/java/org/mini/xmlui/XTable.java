package org.mini.xmlui;

public class XTable
        extends XPanel {
    static public final String XML_NAME = "table";

    public XTable(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    boolean parseNoTagText(){
        return false;
    }
}
