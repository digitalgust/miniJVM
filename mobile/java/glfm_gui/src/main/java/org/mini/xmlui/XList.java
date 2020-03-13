package org.mini.xmlui;

import org.mini.gui.GImage;
import org.mini.gui.GList;
import org.mini.gui.GListItem;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.xmlui.xmlpull.KXmlParser;
import org.mini.xmlui.xmlpull.XmlPullParser;

import java.util.Vector;

public class XList extends XObject implements GActionListener {
    static public final String XML_NAME = "list";

    static class ListItem {
        static public final String XML_NAME = "li";
        String name;
        String text;
        String pic;
    }

    Vector items = new Vector();

    GList list;
    boolean multiLine = false;

    public XList(XContainer xc) {
        super(xc);
    }

    @Override
    String getXmlTag() {
        return XML_NAME;
    }

    void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("multiline")) {
            if (attValue != null) {
                int v = 0;
                try {
                    v = Integer.parseInt(attValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                multiLine = v == 0 ? false : true;
            }
        }
    }


    /**
     * 解析
     *
     * @param parser KXmlParser
     * @throws Exception
     */
    public void parse(KXmlParser parser) throws Exception {
        super.parse(parser);
        int depth = parser.getDepth();

        //得到域
        do {
            parser.next();
            String tagName = parser.getName();
            if (parser.getEventType() == XmlPullParser.START_TAG) {

                if (tagName.equals(XList.ListItem.XML_NAME)) {
                    XList.ListItem item = new ListItem();

                    item.name = parser.getAttributeValue(null, "name");
                    item.text = parser.getAttributeValue(null, "text");
                    item.pic = parser.getAttributeValue(null, "pic");
                    items.add(item);
                }
                toEndTag(parser, XList.ListItem.XML_NAME);
                parser.require(XmlPullParser.END_TAG, null, tagName);
            }
        }
        while (!(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(XML_NAME) && depth == parser.getDepth()));

    }

    void preAlignVertical() {
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parent.viewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parent.viewH / 100;
            } else {
                viewH = height = XDef.DEFAULT_COMPONENT_HEIGHT;
            }
        }
    }

    void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parent.viewW;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }

    GObject getGui() {
        return list;
    }

    void createGui() {
        if (list == null) {
            list = new GList(x, y, width, height);
            list.setName(name);
            list.setAttachment(this);
            for (int i = 0; i < items.size(); i++) {
                ListItem item = (ListItem) items.elementAt(i);
                GImage img = null;
                if (item.pic != null) {
                    img = GImage.createImageFromJar(item.pic);
                }
                GListItem gli = new GListItem(img, item.text);
                gli.setActionListener(this);
                list.addItem(gli);
                list.setShowMode(multiLine ? GList.MODE_MULTI_SHOW : GList.MODE_SINGLE_SHOW);
            }
        } else {
            list.setLocation(x, y);
            list.setSize(width, height);
        }
    }


    @Override
    public void action(GObject gobj) {
        getRoot().getEventHandler().action(gobj, null);
    }

}
