package org.mini.layout;

import org.mini.gui.*;
import org.mini.layout.loader.XmlExtAssist;
import org.xmlpull.v1.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.util.Vector;

public class XMenu extends XObject {
    static public final String XML_NAME = "menu";

    static class MenuItem {
        static public final String XML_NAME = "mi";
        String name;
        String attachment;
        String cmd;
        String text;
        String pic;
        String onClick;
        float[] color;
        String preicon;
    }

    protected Vector items = new Vector();
    protected boolean contextMenu = false;
    int mark = -1;

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
        } else if (attName.equals("mark")) {
            mark = Integer.parseInt(attValue.trim());
        }
    }


    /**
     * 解析
     *
     * @param parser KXmlParser
     * @throws Exception
     */
    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
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
                    item.color = parseHexColor(parser.getAttributeValue(null, "color"));
                    item.attachment = parser.getAttributeValue(null, "attachment");
                    item.onClick = parser.getAttributeValue(null, "onclick");
                    item.cmd = parser.getAttributeValue(null, "cmd");
                    try {
                        item.preicon = parser.getAttributeValue(null, "preicon");
                    } catch (Exception e) {
                    }
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
        super.preAlignVertical();
        if (y == XDef.NODEF) {
            if (raw_yPercent != XDef.NODEF && parent != null && parent.getTrialViewH() != XDef.NODEF) {
                y = raw_yPercent * parent.getTrialViewH() / 100;
            } else {
                y = 0;
            }
        }
    }

    protected void preAlignHorizontal() {
        super.preAlignHorizontal();
        if (x == XDef.NODEF) {
            if (raw_xPercent != XDef.NODEF && parent != null && parent.getTrialViewH() != XDef.NODEF) {
                x = raw_xPercent * parent.getTrialViewW() / 100;
            } else {
                x = 0;
            }
        }
    }


    protected boolean isFreeObj() {
        return true;
    }

    protected int getDefaultWidth(int parentViewW) {
        return parentViewW;
    }

    protected int getDefaultHeight(int parentViewH) {
        return XDef.DEFAULT_COMPONENT_HEIGHT;
    }

    public GObject getGui() {
        return menu;
    }

    protected <T extends GObject> T createGuiImpl() {
        return (T) new GMenu(getAssist().getForm(), x, y, width, height);
    }

    protected void createAndSetGui() {
        if (menu == null) {
            menu = createGuiImpl();
            initGuiMore();
            for (int i = 0; i < items.size(); i++) {
                MenuItem item = (MenuItem) items.elementAt(i);
                GImage img = null;
                if (item.pic != null) {
                    img = getAssist().loadImage(item.pic);
                }
                GMenuItem gli = menu.addItem(item.text, img);
                gli.setActionListener(getRoot().getEventHandler());
                gli.setName(item.name);
                gli.setAttachment(item.attachment);
                gli.setEnable(enable);
                gli.setCmd(item.cmd);
                gli.setColor(item.color);
                gli.setOnClinkScript(item.onClick);
            }
            menu.setContextMenu(contextMenu);
            if (mark >= 0) {
                menu.setMarkIndex(mark);
            }
        } else {
            menu.setLocation(x, y);
            menu.setSize(width, height);
        }
    }

}
