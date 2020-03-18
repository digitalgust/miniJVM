package org.mini.layout;

import org.mini.gui.GContainer;
import org.mini.gui.GGraphics;
import org.mini.gui.GObject;
import org.mini.gui.event.GChildrenListener;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.xmlpull.KXmlParser;
import org.mini.layout.xmlpull.XmlPullParser;
import org.mini.nanovg.Gutil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public abstract class XContainer
        extends XObject implements GChildrenListener {
    static public final String SCRIPT_XML_NAME = "script"; // 脚本代码

    protected Vector<XMenu> menus = new Vector();
    private XObject[] children = new XObject[4];
    private short size = 0;
    public int align = GGraphics.LEFT | GGraphics.TOP;
    private int depth = -1;
    // 脚本引擎
    private Interpreter inp;// 脚本引擎

    protected XEventHandler eventHandler;
    protected static Vector<String> extGuiClassName = new Vector();

    public XContainer() {
        super(null);
    }

    public XContainer(XContainer parent) {
        super(parent);
    }


    static public void registerGUI(String guiClassName) {
        if (!extGuiClassName.contains(guiClassName)) {
            extGuiClassName.addElement(guiClassName);
        }
    }

    static public void unregisterGUI(String guiClassName) {
        extGuiClassName.removeElement(guiClassName);
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
        if (xcon == null) {
            return;
        }
        int move = Integer.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            if (children[i] == xcon) {
                move = i;
                size--;
                //System.out.println("removed: " + xcon + "," + size);
            }
            if (i > move) {
                children[i - 1] = children[i];
            }
        }
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

    protected void preAlignVertical() {


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
                height = raw_heightPercent * parentTrialViewH / 100;
            } else {
                height = totalH;
            }
            viewH = height - getDiff_ViewH2Height();
        }

        createGui();
        for (int i = 0; i < size(); i++) {
            XObject xo = elementAt(i);
            GObject go = xo.getGui();
            if (go != null) ((GContainer) getGui()).add(go);
        }
    }

    protected void preAlignHorizontal() {
        super.preAlignHorizontal();

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


    protected void createGui() {

    }

    private void addListenerToContainer() {
        GContainer gc = (GContainer) getGui();
        if (gc != null) {
            gc.addChildrenListener(this);
        }
        for (int i = 0; i < size; i++) {
            XObject xo = elementAt(i);
            if (xo instanceof XContainer) {
                XContainer xc = (XContainer) xo;
                xc.addListenerToContainer();
            }
        }
    }

    private void removeListenerFromContainer() {
        GContainer gc = (GContainer) getGui();
        if (gc != null) {
            gc.removeChildrenListener(this);
        }
        for (int i = 0; i < size; i++) {
            XObject xo = elementAt(i);
            if (xo instanceof XContainer) {
                XContainer xc = (XContainer) xo;
                xc.removeListenerFromContainer();
            }
        }
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

        viewW = width - getDiff_viewW2Width();
        viewH = height - getDiff_viewW2Width();
    }

    protected void alignMenus() {
        for (XMenu menu : menus) {
            menu.preAlignHorizontal();
            menu.preAlignVertical();
            menu.createGui();
            GContainer gc = (GContainer) getGui();
            gc.add(menu.getGui());
        }
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
        alignMenus();
        addListenerToContainer();
    }


    public void reSize(int guiRootW, int guiRootH) {
        int tx = x;
        int ty = y;
        resetBoundle();
        removeListenerFromContainer();
        setRootSize(guiRootW, guiRootH);

        preAlignHorizontal();
        preAlignVertical();

        align();
        alignMenus();
        addListenerToContainer();
        x = tx;
        y = ty;
        getGui().setLocation(x, y);
    }


    protected void resetBoundle() {
        super.resetBoundle();
        for (int i = 0; i < size; i++) {
            XObject xo = elementAt(i);
            xo.resetBoundle();
        }

        for (XMenu menu : menus) {
            menu.resetBoundle();
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
        } else if (tagName.equals(XList.XML_NAME)) { //list
            XList xlist = new XList(this);
            xlist.parse(parser);
            add(xlist);
        } else if (tagName.equals(XMenu.XML_NAME)) { //menu
            XMenu xmenu = new XMenu(this);
            xmenu.parse(parser);
            menus.add(xmenu);
        } else if (tagName.equals(XPanel.XML_NAME)) { //panel
            XPanel panel = new XPanel(this);
            panel.parse(parser);
            add(panel);
        } else if (tagName.equals(XViewPort.XML_NAME)) { //viewport
            XViewPort viewport = new XViewPort(this);
            viewport.parse(parser);
            add(viewport);
        } else if (tagName.equals(XViewSlot.XML_NAME)) { //viewslot
            XViewSlot viewSlot = new XViewSlot(this);
            viewSlot.parse(parser);
            add(viewSlot);
        } else if (tagName.equals(XFrame.XML_NAME)) { //viewslot
            XFrame frame = new XFrame(this);
            frame.parse(parser);
            add(frame);
        } else if (tagName.equals(SCRIPT_XML_NAME)) {
            parser.next();
            String scode = parser.getText();
            inp = new Interpreter();
            inp.loadFromString(scode);// 装入代码
            inp.register(new ScriptLib(this));
            toEndTag(parser, tagName); // 跳过结束符

        } else {
            boolean found = false;
            for (String s : extGuiClassName) {
                if (s.equals(tagName)) {
                    try {
                        Class clazz = Class.forName(s);
                        XObject xobj = (XObject) clazz.newInstance();
                        xobj.setParent(this);
                        xobj.parse(parser);
                        add(xobj);
                        found = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!found) {
                new Exception("xml tag not found:" + tagName).printStackTrace();
                toEndTag(parser, tagName);
            }
        }

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

    //===================================================================
    //         if any GObject add to GContainer
    //         the xsystem would be collective action
    //===================================================================

    @Override
    public void onChildAdd(GObject child) {
        Object obj = child.getAttachment();
        if (child.getAttachment() instanceof XObject) {
            XObject xobj = (XObject) obj;
            xobj.setParent(this);
            add(xobj);
            color = null;
        }
    }

    @Override
    public void onChildRemove(GObject child) {
        Object obj = child.getAttachment();
        if (child.getAttachment() instanceof XObject) {
            XObject xobj = (XObject) obj;
            xobj.setParent(null);
            remove(xobj);
        }
    }
}
