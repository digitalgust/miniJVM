package org.mini.layout;

import org.mini.gui.*;
import org.mini.gui.event.GActionListener;
import org.mini.layout.xmlpull.KXmlParser;
import org.mini.layout.xmlpull.XmlPullParser;

import java.util.Vector;

public class XMenu extends XObject implements GActionListener {
    static public final String XML_NAME = "menu";

    static class MenuItem {
        static public final String XML_NAME = "mi";
        String name;
        String text;
        String pic;
    }

    protected Vector items = new Vector();
    protected boolean contextMenu = false;
    protected boolean fixed = true;

    protected GMenu menu;

    public XMenu(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("contextmenu")) {
            contextMenu = "0".equals(attValue) ? false : true;
        } else if (attName.equals("fixed")) {
            fixed = "0".equals(attValue) ? false : true;
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

                if (tagName.equals(MenuItem.XML_NAME)) {
                    MenuItem item = new MenuItem();

                    item.name = parser.getAttributeValue(null, "name");
                    item.pic = parser.getAttributeValue(null, "pic");
                    String tmp = parser.nextText();
                    item.text = tmp.length() == 0 ? null : tmp;
                    items.add(item);
                }
                toEndTag(parser, MenuItem.XML_NAME);
                parser.require(XmlPullParser.END_TAG, null, tagName);
            }
        }
        while (!(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(XML_NAME) && depth == parser.getDepth()));

    }

    protected void preAlignVertical() {
        int parentTrialViewH = parent.getTrialViewH();
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parentTrialViewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parentTrialViewH / 100;
            } else {
                viewH = height = XDef.DEFAULT_COMPONENT_HEIGHT;
            }
        }
        if (y == XDef.NODEF) {
            if (raw_yPercent != XDef.NODEF && parentTrialViewH != XDef.NODEF) {
                y = raw_yPercent * parentTrialViewH / 100;
            } else {
                y = XDef.DEFAULT_COMPONENT_HEIGHT;
            }
        }
    }

    protected void preAlignHorizontal() {
        int parentTrialViewW = parent.getTrialViewW();
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parentTrialViewW;
            } else {
                viewW = width = raw_widthPercent * parentTrialViewW / 100;
            }
        }
        if (x == XDef.NODEF) {
            if (raw_xPercent == XDef.NODEF) {
                x = parentTrialViewW;
            } else {
                x = raw_xPercent * parentTrialViewW / 100;
            }
        }
    }

    public GObject getGui() {
        return menu;
    }

    protected void createGui() {
        if (menu == null) {
            menu = new GMenu(x, y, width, height);
            menu.setName(name);
            menu.setAttachment(this);
            for (int i = 0; i < items.size(); i++) {
                MenuItem item = (MenuItem) items.elementAt(i);
                GImage img = null;
                if (item.pic != null) {
                    img = GImage.createImageFromJar(item.pic);
                }
                GMenuItem gli = menu.addItem(item.text, img);
                gli.setActionListener(this);
                gli.setName(item.name);
            }
            menu.setContextMenu(contextMenu);
            menu.setFixed(fixed);
        } else {
            menu.setLocation(x, y);
            menu.setSize(width, height);
        }
    }

    @Override
    public void action(GObject gobj) {
        getRoot().getEventHandler().action(gobj, null);
    }

}
