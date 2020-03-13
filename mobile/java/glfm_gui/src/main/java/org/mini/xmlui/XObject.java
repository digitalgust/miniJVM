package org.mini.xmlui;

import org.mini.gui.GObject;
import org.mini.xmlui.gscript.Interpreter;
import org.mini.xmlui.xmlpull.KXmlParser;

public abstract class XObject {

    //used for align
    protected int x = XDef.NODEF, y = XDef.NODEF, width = XDef.NODEF, height = XDef.NODEF;
    //protected int raw_xPercent = XDef.NODEF, raw_yPercent = XDef.NODEF, raw_widthPercent = XDef.NODEF, raw_heightPercent = XDef.NODEF;
    //used for realign
    protected int raw_x = XDef.NODEF, raw_y = XDef.NODEF, raw_width = XDef.NODEF, raw_height = XDef.NODEF;
    protected int raw_xPercent = XDef.NODEF, raw_yPercent = XDef.NODEF, raw_widthPercent = XDef.NODEF, raw_heightPercent = XDef.NODEF;

    //供子组件显示的区域
    int viewW = XDef.NODEF, viewH = XDef.NODEF;

    private boolean visable = true; //是否显示

    String name = null; //组件名字

    String text = null; //文本

    String cmd = null;

    String moveMode;

    XContainer parent;

    public XObject(XContainer xc) {
        parent = xc;
    }

    public XContainer getParent() {
        return parent;
    }

    public XContainer getRoot() {
        XContainer p = parent;
        while (p.parent != null) {
            p = p.parent;
        }
        return p;
    }

    public Interpreter getInterpreter(String scriptContainerName) {
        XContainer scriptHolder = getRoot();
        if (scriptContainerName != null) {
            XObject xobj = getRoot().find(scriptContainerName);
            if (xobj instanceof XContainer && ((XContainer) xobj).getInp() != null) {
                scriptHolder = (XContainer) xobj;
            }
        }
        return scriptHolder.getInp();
    }

    abstract String getXmlTag();


    /**
     *
     */
    abstract void preAlignVertical();

    abstract void preAlignHorizontal();

    abstract void createGui();

    abstract GObject getGui();

    /**
     * 解析对应的xml
     *
     * @param parser KXmlParser
     * @throws Exception
     */
    public void parse(KXmlParser parser) throws Exception {

        //iterator attribute
        int attCount = parser.getAttributeCount();
        for (int i = 0; i < attCount; i++) {
            String attName = parser.getAttributeName(i);
            String attValue = parser.getAttributeValue(i);

            if (attName.equals("name")) { // 标题
                name = attValue;
            } else if (attName.equals("move")) { // viewslot move mode
                moveMode = attValue;
            }
            parseMoreAttribute(attName, attValue);
        }
    }

    void parseMoreAttribute(String attName, String attValue) {

    }


    /**
     * 当自己被拖动时的处理
     *
     * @param px
     * @param py
     */
    public void onDrag(int px, int py) {
        setText(px + "," + py);
    }

    /**
     * 跳过无用模块，找到此事件的结束符
     *
     * @param parser  KXmlParser
     * @param tagName String
     * @throws Exception
     */
    protected void toEndTag(KXmlParser parser, String tagName) throws Exception {
        while (!(tagName.equals(parser.getName()) && parser.getEventType() == KXmlParser.END_TAG)) {
            parser.next();
        }
    }


    public void setText(String s) {
        text = s;
    }


    public String getText() {
        return text;
    }


    public String getName() {
        return name;
    }

    /**
     * 通过名字查找某组件
     *
     * @param xcname String
     * @return XObject
     */
    public XObject find(String xcname) {
        if (name != null && name.equals(xcname)) {
            return this;
        } else {
            return null;
        }
    }


    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void resetBoundle() {
        x = raw_x;
        y = raw_y;
        width = raw_width;
        height = raw_height;
        viewW = XDef.NODEF;
        viewH = XDef.NODEF;
    }
}

