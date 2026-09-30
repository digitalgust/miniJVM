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

    protected void preAlignHorizontal() {
        super.preAlignHorizontal();
        if (isBlank() && raw_width == XDef.NODEF && raw_widthPercent == XDef.NODEF) {
            viewW = width = 0;
        }
    }

    protected void preAlignVertical() {
        super.preAlignVertical();
        if (isBlank()) {
            if (raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF) {
                viewH = height = 0;
                if (getGui() != null) getGui().setSize(width, height);
            }
            return;
        }
        int parentTrialViewH = parent.getTrialViewH();
        if (raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF) {//只有在未定义高的情况下才进行重构, 否则进入reSize后死循环
            if (height < parentTrialViewH) {
                viewH = height = parentTrialViewH;

                int tx = x;
                int ty = y;
                // The outer build/reSize performs the final recursive align.
                // Aligning here too would add the center offset twice.
                //未定义宽的td宽度由tr分配, reSize时保持当前宽度, 否则会被父宽覆盖盖住同行的其他td
                int pw = (raw_widthPercent != XDef.NODEF) ? parent.getTrialViewW() : width;
                reSize(pw, parentTrialViewH, false);
                x = tx;
                y = ty;
                if (getGui() != null) getGui().setLocation(x, y);
            }
        }
    }

}
