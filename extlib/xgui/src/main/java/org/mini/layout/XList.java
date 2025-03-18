package org.mini.layout;

import org.mini.gui.*;
import org.mini.layout.loader.XmlExtAssist;
import org.xmlpull.v1.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.util.Vector;

public class XList extends XObject {
    static public final String XML_NAME = "list";

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
                if (tagName.equals(XListItem.XML_NAME)) {
                    XContainer tmp = new XPanel(parent);
                    XListItem xitem = (XListItem) XContainer.parseSon(parser, tmp, assist);
                    items.add(xitem);
                }
                toEndTag(parser, XListItem.XML_NAME);
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
                // 创建子项，因为glist不是个标准容器，因此需要手动构建子项
                XListItem xitem = (XListItem) items.elementAt(i);
                xitem.build(viewW, itemheight, assist.getEventHandler());
                GListItem gli = (GListItem) xitem.getGui();
                gli.setPreIcon(xitem.preicon);
                gli.setPreiconColor(xitem.preiconColor);
                list.add(gli);
                if (xitem.selected) {
                    selectCount++;
                    selected = i;
                }
                if (xitem.getRawFontSize() <= 0) {
                    if (getRawFontSize() > 0) {
                        gli.setFontSize(getRawFontSize());
                    }
                }
            }
            list.setEnable(enable);
            if (multiSelect && selectCount > 0) {
                int[] seleArr = new int[selectCount];
                int tmp = 0;
                for (int i = 0; i < items.size(); i++) {
                    XListItem item = (XListItem) items.elementAt(i);
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
