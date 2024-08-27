package org.mini.layout;

import org.mini.glwrap.GLUtil;
import org.mini.gui.GContainer;
import org.mini.gui.GObject;
import org.xmlpull.v1.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.mini.nanovg.Nanovg;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;


public abstract class XContainer
        extends XObject {
    static public final String SCRIPT_XML_NAME = "script"; // 脚本代码

    ArrayList<XObject> children = new ArrayList<>();
    //    ArrayList<XObject> hiddens = new ArrayList<>();
    public int align = Nanovg.NVG_ALIGN_LEFT | Nanovg.NVG_ALIGN_TOP;
    private int depth = -1;

    protected XEventHandler eventHandler;


    public XContainer(XContainer parent) {
        super(parent);
    }


    /**
     * 在各子元素中查找某组件。
     *
     * @param xcname String
     * @return XObject
     */
    public XObject find(String xcname) {
        if (name != null && name.equals(xcname)) {
            return this;
        } else {
            for (int i = 0; i < children.size(); i++) {
                XObject tmpxc = children.get(i).find(xcname);
                if (tmpxc != null) {
                    return tmpxc;
                }
            }
        }
        return null;
    }


    public XEventHandler getEventHandler() {
        return eventHandler;
    }

    /**
     * all component has same height in row
     *
     * @return
     */
    boolean isSameHeightRow() {
        return false;
    }

    protected void preAlignVertical() {


        //follow layout
        int dx = 0, dy = 0, curRowH = 0, curRowW = 0;
        int totalH = 0;
        for (int i = 0; i < children.size(); i++) {
            XObject xo = children.get(i);
            xo.preAlignVertical();
            if (!xo.isFreeObj()) {//  free obj need not layout in it's parent, eg: frame menu
                curRowW = xo.width + dx;
                if (curRowW > viewW || xo instanceof XBr) {
                    dy += curRowH;
                    dx = 0;
                    curRowH = 0;

                }
                xo.x = dx;
                xo.y = dy;
                dx += xo.width;
                if (curRowH < xo.height) {
                    curRowH = xo.height;
                    totalH = dy + curRowH;
                }
            }
            xo.createAndSetGui();

        }
        if (height == XDef.NODEF) {
            int parentTrialViewH = parent.getTrialViewH();
            if (raw_heightPercent != XDef.NODEF && parentTrialViewH != XDef.NODEF) {
                height = raw_heightPercent * parentTrialViewH / 100;
            } else {
                height = totalH;
            }
            viewH = height - getDiff_ViewH2Height();
        }

        createAndSetGui();
        for (int i = 0; i < children.size(); i++) {
            XObject xo = children.get(i);
            GObject go = xo.getGui();
            if (go != null) {
                GContainer gc = getGui();
                gc.add(go);
            }
        }
    }

    protected void preAlignHorizontal() {
        super.preAlignHorizontal();

        for (int i = 0; i < children.size(); i++) {
            XObject xo = children.get(i);
            xo.preAlignHorizontal();
        }

    }

    void align() {
        //for realign gui component
        List<List<XObject>> rows = new ArrayList();
        List<XObject> row = new ArrayList();

        //split rows
        int dy = 0;
        for (int i = 0; i < children.size(); i++) {
            XObject xo = children.get(i);
            if (i == 0) {
                dy = xo.y;
            }
            if (dy != xo.y) {
                rows.add(row);
                row = new ArrayList();
                dy = xo.y;
            }
            row.add(xo);
        }
        if (row.size() > 0) rows.add(row);


        //start realign hori
        int sumHeight = 0;
        for (List<XObject> crow : rows) {
            int maxH = 0;
            int sumWidth = 0;
            for (XObject xo : crow) {
                if (maxH < xo.height) {
                    maxH = xo.height;
                }
                sumWidth += xo.width;
            }
            sumHeight += maxH;
            //tr same row height
            if (isSameHeightRow()) {
                for (XObject xo : crow) {
                    xo.viewH = xo.height = maxH;
                    xo.getGui().setSize(xo.width, xo.height);
                }
            }

            int rightPix = (viewW - sumWidth);
            int hcenterPix = rightPix / 2;
            for (XObject xo : crow) {
                if ((align & Nanovg.NVG_ALIGN_CENTER) != 0) {
                    xo.x += hcenterPix;
                } else if ((align & Nanovg.NVG_ALIGN_RIGHT) != 0) {
                    xo.x += rightPix;
                }
            }
        }
        //start realign vert
        int bottomPix = viewH - sumHeight;
        int vcenterPix = bottomPix / 2;
        for (List<XObject> crow : rows) {
            boolean rowHasFloatObj = false;
            for (XObject xo : crow) {
                if ((align & Nanovg.NVG_ALIGN_MIDDLE) != 0) {
                    xo.y += vcenterPix;
                } else if ((align & Nanovg.NVG_ALIGN_BOTTOM) != 0) {
                    xo.y += bottomPix;
                }
                if (xo.getGui() != null) xo.getGui().setLocation(xo.x, xo.y);
                if (xo instanceof XContainer) {
                    ((XContainer) xo).align();
                }
            }
        }
    }


    protected void createAndSetGui() {

    }

    void setParentSize(int parentW, int parentH) {
        resetBoundle();
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                width = parentW;
            } else {
                width = (parentW * raw_widthPercent / 100);
            }
        }
        if (height == XDef.NODEF) {
            if (raw_heightPercent == XDef.NODEF) {
                height = parentH;
            } else {
                height = (parentH * raw_heightPercent / 100);
            }
        }
        if (x == XDef.NODEF) {
            if (raw_xPercent == XDef.NODEF) {
                x = 0;
            } else {
                x = (parentW * raw_xPercent / 100);
            }
        }
        if (y == XDef.NODEF) {
            if (raw_yPercent == XDef.NODEF) {
                y = 0;
            } else {
                y = (parentH * raw_yPercent / 100);
            }
        }

        viewW = width - getDiff_viewW2Width();
        viewH = height - getDiff_viewW2Width();
    }


    public void build(int parentW, int parentH, XEventHandler eventHandler) {

        if (eventHandler == null) {
            this.eventHandler = new XEventHandler();
        } else {
            this.eventHandler = eventHandler;
        }

        assist.setEventHandler(eventHandler);
        setParentSize(parentW, parentH);

        preAlignHorizontal();
        preAlignVertical();

        align();
    }


    public void reSize(int parentW, int parentH) {
        int tx = x;
        int ty = y;
        resetBoundle();
        setParentSize(parentW, parentH);

        preAlignHorizontal();
        preAlignVertical();

        align();
        x = tx;
        y = ty;
        getGui().setLocation(x, y);
    }


    protected void resetBoundle() {
        super.resetBoundle();
        for (int i = 0; i < children.size(); i++) {
            XObject xo = children.get(i);
            xo.resetBoundle();
        }
    }


    boolean parseNoTagText() {
        return true;
    }

    /**
     * parse text without tag
     *
     * @param parser KXmlParser
     * @throws Exception
     */
    public void parseText(KXmlParser parser) throws Exception {
        String tmp = parser.getText(); //无标签文本
        if (tmp != null) {
            tmp = tmp.replace('\n', ' ');
            tmp = tmp.replace('\r', ' ');
            tmp = tmp.trim();
            if (tmp.length() > 0) {
                XLabel label = new XLabel(this);
                label.assist = assist;
                label.multiLine = true;
                label.setText(tmp);
                children.add(label);
            }
        }

    }

    /**
     * 解析通用组件
     *
     * @param parser KXmlParser
     * @throws Exception
     */

    public static XObject parseSon(KXmlParser parser, XContainer parent, XmlExtAssist assist) throws Exception {
        String tagName = parser.getName();
        switch (tagName) {
            case XBr.XML_NAME:  //br
                XBr xbr = new XBr(parent);
                xbr.parse(parser, assist);
                return xbr;
            case XButton.XML_NAME:  //button
                XButton xaction = new XButton(parent);
                xaction.parse(parser, assist);
                return (xaction);
            case XLabel.XML_NAME:  //label
                XLabel label = new XLabel(parent);
                label.parse(parser, assist);
                return (label);
            case XImageItem.XML_NAME:  //img
                XImageItem img = new XImageItem(parent);
                img.parse(parser, assist);
                return (img);
            case XCheckBox.XML_NAME:  //checkbox
                XCheckBox xbox = new XCheckBox(parent);
                xbox.parse(parser, assist);
                return (xbox);
            case XScrollBar.XML_NAME:  //scrollbar
                XScrollBar bar = new XScrollBar(parent);
                bar.parse(parser, assist);
                return (bar);
            case XTextInput.XML_NAME:  //textinput
                XTextInput xinput = new XTextInput(parent);
                xinput.parse(parser, assist);
                return (xinput);
            case XTable.XML_NAME:  //table
                XTable xtb = new XTable(parent);
                xtb.parse(parser, assist);
                return (xtb);
            case XTr.XML_NAME:  //tr
                XTr xtr = new XTr(parent);
                xtr.parse(parser, assist);
                return (xtr);
            case XTd.XML_NAME:  //td
                XTd xtd = new XTd(parent);
                xtd.parse(parser, assist);
                return (xtd);
            case XList.XML_NAME:  //list
                XList xlist = new XList(parent);
                xlist.parse(parser, assist);
                return (xlist);
            case XMenu.XML_NAME:  //menu
                XMenu xmenu = new XMenu(parent);
                xmenu.parse(parser, assist);
                return (xmenu);
            case XSwitch.XML_NAME:  //switcher
                XSwitch xswitcher = new XSwitch(parent);
                xswitcher.parse(parser, assist);
                return (xswitcher);
            case XPanel.XML_NAME: { //panel
                XPanel panel = new XPanel(parent);
                panel.parse(parser, assist);
                return (panel);
            }
            case XCanvas.XML_NAME: { //canvas
                XPanel panel = new XCanvas(parent);
                panel.parse(parser, assist);
                return (panel);
            }
            case XViewPort.XML_NAME:  //viewport
                XViewPort viewport = new XViewPort(parent);
                viewport.parse(parser, assist);
                return (viewport);
            case XViewSlot.XML_NAME:  //viewslot
                XViewSlot viewSlot = new XViewSlot(parent);
                viewSlot.parse(parser, assist);
                return (viewSlot);
            case XFrame.XML_NAME:  //viewslot
                XFrame frame = new XFrame(parent);
                frame.parse(parser, assist);
                return (frame);
            case XForm.XML_NAME:  //viewslot
                XForm form = new XForm(parent);
                form.parse(parser, assist);
                return (form);
            case SCRIPT_XML_NAME:
                parser.next();
                String scode = parser.getText();
                parent.script = scode;
                parent.toEndTag(parser, tagName); // 跳过结束符

                break;
            default:
                boolean found = false;
                if (assist != null) {
                    for (String s : assist.getExtGuiClassName()) {
                        if (s.equals(tagName)) {
                            try {
                                Class clazz = Class.forName(s, true, Thread.currentThread().getContextClassLoader());
                                Constructor con = clazz.getConstructor(XContainer.class);
                                XObject xobj = (XObject) con.newInstance(parent);
                                xobj.parse(parser, assist);
                                return (xobj);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (!found) {
                    new Exception("xml tag not found:" + tagName).printStackTrace();
                    parent.toEndTag(parser, tagName);
                }
                break;
        }
        return null;
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("align")) {
            align = 0;
            for (String s : attValue.split(",")) {
                align |= XUtil.parseAlign(s);
            }
        }
    }

    /**
     * 解析XML，统一接口
     *
     * @param parser KXmlParser
     * @throws Exception
     */
    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        //Parse our XML file
        depth = parser.getDepth();

        super.parse(parser, assist);

        //得到域
        do {
            parser.next();
            String tagName = parser.getName();
            // 解析非标签文本
            if (parseNoTagText()) parseText(parser);

            if (parser.getEventType() == XmlPullParser.START_TAG) {
                XObject xobj = XContainer.parseSon(parser, this, assist);
                if (xobj != null) {
//                    if (xobj.hidden) {
//                        hiddens.add(xobj);
//                    } else {
                    children.add(xobj);
//                    }
                }
                parser.require(XmlPullParser.END_TAG, null, tagName);
            }
        } while (!(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(getXmlTag()) && depth == parser.getDepth()));
    }

    /**
     * 供外部调用
     *
     * @param is InputStream
     */
    public static XObject parseXml(InputStream is, XmlExtAssist assist) throws Exception {
        try {
            //parse xml
            KXmlParser parser = new KXmlParser();
            parser.setInput(is, "UTF-8");

            parser.nextTag();

            XObject xobj = parseSon(parser, null, assist);

            parser.next();
            parser.require(XmlPullParser.END_DOCUMENT, null, null);

            return xobj;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static XObject parseXml(String uiStr, XmlExtAssist assist) {
        try {
            if (assist == null) throw new RuntimeException("XmlExtAssist can not be null");

            ByteArrayInputStream bais = new ByteArrayInputStream(GLUtil.toCstyleBytes(uiStr));
            XObject xobj = parseXml(bais, assist);
            return xobj;
        } catch (Exception ex) {
            System.out.println(uiStr);
            ex.printStackTrace();
        }
        return null;
    }

}
