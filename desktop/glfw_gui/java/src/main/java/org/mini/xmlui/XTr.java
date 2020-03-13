package org.mini.xmlui;

import org.mini.xmlui.xmlpull.KXmlParser;
import org.mini.xmlui.xmlpull.XmlPullParser;


public class XTr
        extends XPanel {

    static public final String XML_NAME = "tr";

    public XTr(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    /**
     * 解析
     *
     * @param parser KXmlParser
     * @throws Exception
     */
    public void parse(KXmlParser parser) throws Exception {
        depth = parser.getDepth();
        //得到域
        do {
            parser.next();
            String tagName = parser.getName();

            if (parser.getEventType() == XmlPullParser.START_TAG) {
                parseSon(parser);
                parser.require(XmlPullParser.END_TAG, null, tagName);
            }
        }
        while (!(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(XML_NAME) && depth == parser.getDepth()));
    }


    boolean isSameHeightRow() {
        return true;
    }


    void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parent.viewW;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
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
