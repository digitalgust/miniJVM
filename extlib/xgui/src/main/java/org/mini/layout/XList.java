package org.mini.layout;

import org.mini.gui.*;
import org.xmlpull.v1.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.util.Vector;

public class XList extends XObject {
    static public final String XML_NAME = "list";

    protected static class ListItem {
        static public final String XML_NAME = "li";

        XList xlist;
        String name;
        String text;
        String pic;
        String cmd;
        String attachment;
        String onClick;
        boolean selected;
        float[] color;
        String preicon;
        float[] preiconColor;
    }

    protected Vector items = new Vector();
    protected boolean multiLine = true;
    protected boolean multiSelect = false;
    protected boolean scrollbar = false;
    protected int itemheight = XDef.DEFAULT_LIST_HEIGHT;

    protected GList list;

    public XList(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("multiline")) {
            multiLine = "0".equals(attValue) ? false : true;
        } else if (attName.equals("multiselect")) {
            multiSelect = "0".equals(attValue) ? false : true;
        } else if (attName.equals("scroll")) {
            scrollbar = "0".equals(attValue) ? false : true;
        } else if (attName.equals("itemh")) {
            itemheight = Integer.parseInt(attValue);
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

                if (tagName.equals(XList.ListItem.XML_NAME)) {
                    XList.ListItem item = new ListItem();
                    item.xlist = this;
                    item.name = parser.getAttributeValue(null, "name");
                    item.pic = parser.getAttributeValue(null, "pic");
                    item.cmd = parser.getAttributeValue(null, "cmd");
                    item.color = parseHexColor(parser.getAttributeValue(null, "color"));
                    item.onClick = parser.getAttributeValue(null, "onclick");
                    item.attachment = parser.getAttributeValue(null, "attachment");
                    String tmp1 = parser.getAttributeValue(null, "selected");
                    item.selected = ("1".equals(tmp1)) ? true : false;
                    try {
                        item.preicon = parser.getAttributeValue(null, "preicon");
                        item.preiconColor = parseHexColor(parser.getAttributeValue(null, "pcolor"));
                    } catch (Exception e) {
                    }
                    String tmp2 = parser.nextText();
                    item.text = tmp2 == null ? "" : tmp2;
                    items.add(item);
                }
                toEndTag(parser, XList.ListItem.XML_NAME);
                parser.require(XmlPullParser.END_TAG, null, tagName);
            }
        }
        while (!(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(getXmlTag()) && depth == parser.getDepth()));

    }


    protected int getDefaultWidth(int parentViewW) {
        return parentViewW;
    }

    protected int getDefaultHeight(int parentViewH) {
        if (multiLine) {
            return XDef.DEFAULT_LIST_HEIGHT * items.size();
        } else {
            return XDef.DEFAULT_LIST_HEIGHT;
        }
    }


    public GObject getGui() {
        return list;
    }

    protected <T extends GObject> T createGuiImpl() {
        return (T) new GList(getAssist().getForm(), x, y, width, height);
    }

    protected void createAndSetGui() {
        if (list == null) {
            list = createGuiImpl();
            initGuiMore();
            list.setShowMode(multiLine ? GList.MODE_MULTI_SHOW : GList.MODE_SINGLE_SHOW);
            list.setSelectMode(multiSelect ? GList.MODE_MULTI_SELECT : GList.MODE_SINGLE_SELECT);
            list.setItemHeight(itemheight);
            list.setScrollBar(scrollbar);
            int selected = -1, selectCount = 0;
            for (int i = 0; i < items.size(); i++) {
                ListItem item = (ListItem) items.elementAt(i);
                GImage img = null;
                if (item.pic != null) {
                    img = getAssist().loadImage(item.pic);
                }
                GListItem gli = new GListItem(getAssist().getForm(), img, item.text);
                gli.setName(item.name);
                gli.setAttachment(item.attachment);
                gli.setActionListener(getRoot().getEventHandler());
                gli.setEnable(enable);
                gli.setCmd(item.cmd);
                gli.setOnClinkScript(item.onClick);
                gli.setPreIcon(item.preicon);
                if (item.color != null) {
                    gli.setColor(item.color);
                }
                if (item.preiconColor != null) {
                    gli.setPreiconColor(item.preiconColor);
                }
                list.add(gli);
                if (item.selected) {
                    selectCount++;
                    selected = i;
                }
            }
            list.setEnable(enable);
            if (multiSelect && selectCount > 0) {
                int[] seleArr = new int[selectCount];
                int tmp = 0;
                for (int i = 0; i < items.size(); i++) {
                    ListItem item = (ListItem) items.elementAt(i);
                    if (item.selected) {
                        seleArr[tmp] = i;
                        tmp++;
                    }
                }
                list.setSelectedIndices(seleArr);
            } else {
                list.setSelectedIndex(selected);
            }
        } else {
            list.setLocation(x, y);
            list.setSize(width, height);
        }
    }

}
