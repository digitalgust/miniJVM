package org.mini.layout;

public class XTr
        extends XPanel {

    static public final String XML_NAME = "tr";

    public XTr() {
        super(null);
    }

    public XTr(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    boolean parseNoTagText() {
        return false;
    }


    boolean isSameHeightRow() {
        return true;
    }


    protected void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            int parentTrialViewW = parent.getTrialViewW();
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parentTrialViewW;
            } else {
                viewW = width = raw_widthPercent * parentTrialViewW / 100;
            }
        }


        int size = size();
        int totalPixer = 0;
        int nodefCount = 0;
        for (int i = 0; i < size; i++) {
            XTd td = (XTd) elementAt(i);
            if (td.raw_widthPercent != XDef.NODEF) {
                totalPixer += td.raw_widthPercent * viewW / 100;
            } else if (td.width != XDef.NODEF) {
                totalPixer += td.width;
            } else {
                nodefCount++;
            }
        }
        //for avg
        int tdw = viewW / size;//avg
        //for nodef
        int nodefTdW = nodefCount == 0 ? 0 : ((viewW - totalPixer) / nodefCount);
        for (int i = 0; i < size; i++) {
            XTd td = (XTd) elementAt(i);
            //if over parent.width
            if (totalPixer > viewW) {
                td.viewW = td.width = tdw;
                td.x = tdw * i;
            } else {
                if (td.width == XDef.NODEF) {
                    if (td.raw_widthPercent == XDef.NODEF) {
                        td.viewW = td.width = nodefTdW;
                    } else {
                        td.viewW = td.width = viewW * td.raw_widthPercent / 100;
                    }
                }
            }
            td.preAlignHorizontal();
        }
    }

}
