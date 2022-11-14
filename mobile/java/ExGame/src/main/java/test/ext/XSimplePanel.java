package test.ext;

import org.mini.gui.GObject;
import org.mini.layout.XContainer;
import org.mini.layout.XPanel;

public class XSimplePanel extends XPanel {

    static public final String XML_NAME = "test.ext.XSimplePanel";


    public XSimplePanel(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    protected GObject createGuiImpl() {
        return new SimplePanel(getAssist().getForm(), x, y, width, height);
    }
}
