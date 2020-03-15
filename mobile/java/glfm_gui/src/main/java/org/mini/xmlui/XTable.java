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

    boolean parseNoTagText() {
        return false;
    }


    void preAlignVertical() {
        super.preAlignVertical();

        int floatCount = 0;
        int nonFloatPix = 0;
        for (int i = 0; i < size(); i++) {
            XObject xo = elementAt(i);
            if (xo.vfloat) {
                floatCount++;
            } else {
                nonFloatPix += xo.height;
            }
        }

        if (viewH - nonFloatPix > 0) {
            int floatAvgH = floatCount == 0 ? 0 : (viewH - nonFloatPix) / floatCount;
            int yOffset = 0;
            for (int i = 0; i < size(); i++) {
                XObject xo = elementAt(i);
                xo.y += yOffset;
                xo.getGui().setLocation(xo.x, xo.y);
                if (xo.vfloat) {
                    yOffset += floatAvgH - xo.height;
                    xo.viewH = xo.height = floatAvgH;
                    xo.getGui().setSize(xo.width, xo.height);
                    int tx = xo.x;
                    int ty = xo.y;
                    ((XTr) xo).reSize(xo.width, xo.height);
                    xo.x=tx;
                    xo.y=ty;
                    xo.getGui().setLocation(xo.x, xo.y);
                }
            }
        }

    }

}
