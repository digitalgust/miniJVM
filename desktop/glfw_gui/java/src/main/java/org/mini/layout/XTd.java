package org.mini.layout;

public class XTd
        extends XPanel {

    static public final String XML_NAME = "td";


    public XTd(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    protected void preAlignVertical() {
        super.preAlignVertical();
        int parentTrialViewH = parent.getTrialViewH();
        if (height < parentTrialViewH) {
            viewH = height = parentTrialViewH;

            int tx = x;
            int ty = y;
            reSize(width, height);
            x = tx;
            y = ty;
            getGui().setLocation(x, y);
        }
    }

}
