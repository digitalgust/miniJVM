package org.mini.layout;

import org.mini.gui.GColorSelector;

public class XTd extends XPanel {

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
        if (raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF) {//只有在未定义高的情况下才进行重构, 否则进入reSize后死循环
            if (height < parentTrialViewH) {
                viewH = height = parentTrialViewH;

                int tx = x;
                int ty = y;
                reSize(parent.getTrialViewW(), parentTrialViewH);
                x = tx;
                y = ty;
                getGui().setLocation(x, y);
            }
        }
    }

}
