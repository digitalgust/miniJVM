package org.mini.layout;

import org.mini.gui.GGraphics;
import org.mini.nanovg.Gutil;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.xmlpull.KXmlParser;
import org.mini.layout.xmlpull.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public abstract class XContainer
        extends XObject {
    static public final String SCRIPT_XML_NAME = "script"; // 脚本代码
    private XObject[] children = new XObject[4];
    private short size = 0;
    public int align = GGraphics.LEFT | GGraphics.TOP;
    int depth = -1;
    // 脚本引擎
    private Interpreter inp;// 脚本引擎

    private Vector hide_elmts = new Vector(4); //隐藏组件列表
    int bgColor;
    XEventHandler eventHandler;


    public XContainer(XContainer parent) {
        super(parent);
    }

    void onChildAdded(XObject xo) {

    }

    /**
     * 添加一个组件
     *
     * @param xcon XObject
     */
    public void add(XObject xcon) {
        if (children.length <= size) {
            expand();
        }
        children[size] = xcon;
        size++;
    }

    /**
     * 清除掉此元素，并把后面的元素前移
     *
     * @param xcon XObject
     */
    public void remove(XObject xcon) {
        boolean move = false;
        for (int i = 0; i < size; i++) {
            if (children[i] == xcon || move) {
                children[i] = null;
                move = true;
                if (children.length >= i + 1) {
                    children[i] = children[i + 1];
                }
            }
        }
        size--;
    }

    public XObject elementAt(int i) {
        if (size() > i && i >= 0) {
            return children[i];
        } else {
            return null;
        }
    }

    public int indexOf(XObject xc) {
        for (int i = 0; i < size; i++) {
            if (children[i] == xc) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 取得元素个数
     *
     * @return int
     */
    public int size() {
        return size;
    }

    /**
     * 扩容容器
     */
    private void expand() {
        XObject[] tmpxc = new XObject[(children.length << 1)]; //增加一倍
        System.arraycopy(children, 0, tmpxc, 0, size);
        children = tmpxc;
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
            for (int i = 0; i < size(); i++) {
                XObject tmpxc = elementAt(i).find(xcname);
                if (tmpxc != null) {
                    return tmpxc;
                }
            }
        }
        return null;
    }


    /**
     * get hide elements
     *
     * @param name String
     * @return XFrame
     */
    public XFrame getHideElement(String name) {
        for (int i = 0; i < hide_elmts.size(); i++) {
            XFrame xf = (XFrame) hide_elmts.elementAt(i);
            if (xf.name != null && xf.name.equals(name)) {
                return xf;
            }
        }
        return null;
    }

    public XEventHandler getEventHandler() {
        return eventHandler;
    }

    /**
     * get and new script interpreter
     *
     * @return
     */
    public Interpreter getInp() {
        if (inp == null) {
            inp = new Interpreter();
            inp.loadFromString(" ");
            inp.register(new ScriptLib(this));
        }
        return inp;
    }

    public void regScriptLib(ScriptLib lib) {
        getInp().register(lib);
    }

    /**
     * all component has same height in row
     *
     * @return
     */
    boolean isSameHeightRow() {
        return false;
    }

    void preAlignVertical() {


        //follow layout
        int dx = 0, dy = 0, curRowH = 0, curRowW = 0;
        int totalH = 0;
        for (int i = 0; i < size; i++) {
            XObject xo = elementAt(i);
            xo.preAlignVertical();
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
            xo.createGui();

        }
        if (height == XDef.NODEF) {
            int parentTrialViewH = parent.getTrialViewH();
            if (raw_heightPercent != XDef.NODEF && parentTrialViewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parentTrialViewH / 100;
            } else {
                viewH = height = totalH;
            }
        }

        createGui();
    }

    void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parent.viewW;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }

        for (int i = 0; i < size; i++) {
            XObject xo = elementAt(i);
            xo.preAlignHorizontal();
        }

    }

    void align() {
        //for realign gui component
        List<List<XObject>> rows = new ArrayList();
        List<XObject> row = new ArrayList();

        //split rows
        int dy = 0;
        for (int i = 0; i < size; i++) {
            XObject xo = elementAt(i);
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
                if ((align & GGraphics.HCENTER) != 0) {
                    xo.x += hcenterPix;
                } else if ((align & GGraphics.RIGHT) != 0) {
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
                if ((align & GGraphics.VCENTER) != 0) {
                    xo.y += vcenterPix;
                } else if ((align & GGraphics.BOTTOM) != 0) {
                    xo.y += bottomPix;
                }
                if (xo.getGui() != null) xo.getGui().setLocation(xo.x, xo.y);
                if (xo instanceof XContainer) {
                    ((XContainer) xo).align();
                }
            }
        }
    }


    void createGui() {

    }

    void setRootSize(int guiRootW, int guiRootH) {
        resetBoundle();
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                width = guiRootW;
            } else {
                width = (guiRootW * raw_widthPercent / 100);
            }
        }
        if (height == XDef.NODEF) {
            if (raw_heightPercent == XDef.NODEF) {
                height = guiRootH;
            } else {
                height = (guiRootH * raw_heightPercent / 100);
            }
        }
        if (x == XDef.NODEF) {
            if (raw_xPercent == XDef.NODEF) {
                x = 0;
            } else {
                x = (guiRootW * raw_xPercent / 100);
            }
        }
        if (y == XDef.NODEF) {
            if (raw_yPercent == XDef.NODEF) {
                y = 0;
            } else {
                y = (guiRootH * raw_yPercent / 100);
            }
        }

        viewW = width;
        viewH = height;
    }

    public void build(int guiRootW, int guiRootH, XEventHandler eventHandler) {

        if (eventHandler == null) {
            this.eventHandler = new XEventHandler();
        } else {
            this.eventHandler = eventHandler;
        }

        setRootSize(guiRootW, guiRootH);

        preAlignHorizontal();
        preAlignVertical();

        align();
    }


    public void resetBoundle() {
        super.resetBoundle();
        for (int i = 0; i < size; i++) {
            XObject xo = elementAt(i);
            xo.resetBoundle();
        }
    }

    public void reSize(int guiRootW, int guiRootH) {
        resetBoundle();
        setRootSize(guiRootW, guiRootH);

        preAlignHorizontal();
        preAlignVertical();

        align();

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
                label.setText(tmp);
                add(label);
            }
        }

    }

    /**
     * 解析通用组件
     *
     * @param parser KXmlParser
     * @throws Exception
     */

    public void parseSon(KXmlParser parser) throws Exception {
        String tagName = parser.getName();
        if (tagName.equals(XBr.XML_NAME)) { //br
            XBr xbr = new XBr(this);
            xbr.parse(parser);
            add(xbr);
        } else if (tagName.equals(XButton.XML_NAME)) { //button
            XButton xaction = new XButton(this);
            xaction.parse(parser);
            add(xaction);
        } else if (tagName.equals(XLabel.XML_NAME)) { //label
            XLabel label = new XLabel(this);
            label.parse(parser);
            add(label);
        } else if (tagName.equals(XImageItem.XML_NAME)) { //img
            XImageItem img = new XImageItem(this);
            img.parse(parser);
            add(img);
        } else if (tagName.equals(XCheckBox.XML_NAME)) { //checkbox
            XCheckBox xbox = new XCheckBox(this);
            xbox.parse(parser);
            add(xbox);
        } else if (tagName.equals(XTextInput.XML_NAME)) { //textinput
            XTextInput xinput = new XTextInput(this);
            xinput.parse(parser);
            add(xinput);
        } else if (tagName.equals(XTable.XML_NAME)) { //table
            XTable xtb = new XTable(this);
            xtb.parse(parser);
            add(xtb);
        } else if (tagName.equals(XTr.XML_NAME)) { //tr
            XTr xtr = new XTr(this);
            xtr.parse(parser);
            add(xtr);
        } else if (tagName.equals(XTd.XML_NAME)) { //td
            XTd xtd = new XTd(this);
            xtd.parse(parser);
            add(xtd);
        } else if (tagName.equals(XList.XML_NAME)) { //menu
            XList xlist = new XList(this);
            xlist.parse(parser);
            add(xlist);
        } else if (tagName.equals(XPanel.XML_NAME)) { //panel
            XPanel panel = new XPanel(this);
            panel.parse(parser);
            add(panel);
        } else if (tagName.equals(XViewPort.XML_NAME)) { //viewport
            XViewPort panel = new XViewPort(this);
            panel.parse(parser);
            add(panel);
        } else if (tagName.equals(XViewSlot.XML_NAME)) { //viewslot
            XViewSlot panel = new XViewSlot(this);
            panel.parse(parser);
            add(panel);
        } else if (tagName.equals(SCRIPT_XML_NAME)) {
            parser.next();
            String scode = parser.getText();
            inp = new Interpreter();
            inp.loadFromString(scode);// 装入代码
            inp.register(new ScriptLib(this));
            toEndTag(parser, tagName); // 跳过结束符

        }

    }


    void parseMoreAttribute(String attName, String attValue) {
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
    public void parse(KXmlParser parser) throws Exception {
        //Parse our XML file
        depth = parser.getDepth();

        super.parse(parser);

        //得到域
        do {
            parser.next();
            String tagName = parser.getName();
            // 解析非标签文本
            if (parseNoTagText()) parseText(parser);

            if (parser.getEventType() == XmlPullParser.START_TAG) {
                parseSon(parser);
                parser.require(XmlPullParser.END_TAG, null, tagName);
            }
        } while (!(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(getXmlTag()) && depth == parser.getDepth()));
    }

    /**
     * 供外部调用
     *
     * @param is InputStream
     */
    public void parseXml(InputStream is) {
        try {
            //parse xml
            KXmlParser parser = new KXmlParser();
            parser.setInput(is, "UTF-8");

            parser.nextTag();

            parser.require(XmlPullParser.START_TAG, null, getXmlTag());

            parse(parser);
            parser.require(XmlPullParser.END_TAG, null, getXmlTag());

            parser.next();
            parser.require(XmlPullParser.END_DOCUMENT, null, null);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void parseXml(String uiStr) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(Gutil.toUtf8(uiStr));
            parseXml(bais);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
