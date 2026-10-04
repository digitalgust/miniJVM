package org.mini.layout;

import org.mini.gui.GColorSelector;

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


    protected void preAlignHorizontal() {
        super.preAlignHorizontal();
        if (isBlank() && raw_width == XDef.NODEF && raw_widthPercent == XDef.NODEF) {
            viewW = width = 0;
        }
    }

    protected void preAlignVertical() {
        int size = children.size();
        if (size == 1) {
            XObject xo = children.get(0);
            if (!((XContainer) xo).isBlank() && xo.raw_heightPercent == XDef.NODEF && xo.raw_height == XDef.NODEF) {
                xo.raw_heightPercent = 100;
            }
        }
        if (isBlank() && raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF) {
            viewH = height = 0;
        }
        super.preAlignVertical();

        int floatCount = 0;
        int nonFloatPix = 0;
        for (int i = 0; i < size; i++) {
            XObject xo = children.get(i);
            if (xo.vfloat) {
                floatCount++;
            } else {
                nonFloatPix += xo.height;
            }
        }

        if (viewH - nonFloatPix > 0) {
            int floatAvgH = floatCount == 0 ? 0 : (viewH - nonFloatPix) / floatCount;
            int yOffset = 0;
            for (int i = 0; i < size; i++) {
                XObject xo = children.get(i);
                xo.y += yOffset;
                if (xo.getGui() != null) xo.getGui().setLocation(xo.x, xo.y);
                if (xo.vfloat) {
                    yOffset += floatAvgH - xo.height;
//                    xo.viewH = xo.height = floatAvgH;
                    //key
                    xo.raw_heightPercent = Math.round((float) floatAvgH * 100 / viewH);
//                    xo.getGui().setSize(xo.width, xo.height);
                    int tx = xo.x;
                    int ty = xo.y;
                    //不在这里align: 外层build/reSize的align会递归整棵树,
                    //此处再align会使align=right/center的子组件偏移叠加两次
                    ((XTr) xo).reSize(this.viewW, this.viewH, false);
                    xo.x = tx;
                    xo.y = ty;
                    if (xo.getGui() != null) xo.getGui().setLocation(xo.x, xo.y);
                }
            }
        }

    }

}
