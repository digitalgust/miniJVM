package test.ext;

import org.mini.gui.GObject;
import org.mini.layout.XContainer;
import org.mini.layout.XList;

public class XCustomList extends XList {
    static public final String XML_NAME = "test.ext.XCustomList";


    public XCustomList(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }


    protected GObject createGuiImpl() {
        return new GCustomList(getAssist().getForm(), x, y, width, height);
    }

}
