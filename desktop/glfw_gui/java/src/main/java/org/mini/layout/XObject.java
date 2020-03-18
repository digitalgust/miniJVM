package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.xmlpull.KXmlParser;

public abstract class XObject {

    //used for align
    protected int x = XDef.NODEF, y = XDef.NODEF, width = XDef.NODEF, height = XDef.NODEF;
    //protected int raw_xPercent = XDef.NODEF, raw_yPercent = XDef.NODEF, raw_widthPercent = XDef.NODEF, raw_heightPercent = XDef.NODEF;
    //used for realign
    protected int raw_x = XDef.NODEF, raw_y = XDef.NODEF, raw_width = XDef.NODEF, raw_height = XDef.NODEF;
    protected int raw_xPercent = XDef.NODEF, raw_yPercent = XDef.NODEF, raw_widthPercent = XDef.NODEF, raw_heightPercent = XDef.NODEF;

    //供子组件显示的区域
    protected int viewW = XDef.NODEF, viewH = XDef.NODEF;

    protected boolean vfloat = false, hfloat = false;

    protected boolean visable = true; //是否显示

    protected String name = null; //组件名字

    protected String text = null; //文本

    protected String cmd = null;

    protected String moveMode;

    protected XContainer parent;

    protected float[] bgColor;

    protected float[] color;

    public XObject(XContainer xc) {
        parent = xc;
    }

    public XContainer getParent() {
        return parent;
    }


    public void setParent(XContainer parent) {
        this.parent = parent;
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


            parseMoreAttribute(attName, attValue);
        }
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        if (attName.equals("name")) { // 标题
            name = attValue;
        } else if (attName.equals("move")) { // viewslot move mode
            moveMode = attValue;
        } else if (attName.equals("w")) {
            if (attValue.equals("float")) {
                hfloat = true;
            } else if (attValue.indexOf('%') >= 0) {
                attValue = attValue.trim();
                raw_widthPercent = Integer.parseInt(attValue.substring(0, attValue.length() - 1));
            } else {
                raw_width = Integer.parseInt(attValue);
            }
        } else if (attName.equals("h")) {
            if (attValue.equals("float")) {
                vfloat = true;
            } else if (attValue.indexOf('%') >= 0) {
                attValue = attValue.trim();
                raw_heightPercent = Integer.parseInt(attValue.substring(0, attValue.length() - 1));
            } else {
                raw_height = Integer.parseInt(attValue);
            }

        } else if (attName.equals("x")) {
            if (attValue.indexOf('%') >= 0) {
                attValue = attValue.trim();
                raw_xPercent = Integer.parseInt(attValue.substring(0, attValue.length() - 1));
            } else {
                raw_x = Integer.parseInt(attValue);
            }
        } else if (attName.equals("y")) {
            if (attValue.indexOf('%') >= 0) {
                attValue = attValue.trim();
                raw_yPercent = Integer.parseInt(attValue.substring(0, attValue.length() - 1));
            } else {
                raw_y = Integer.parseInt(attValue);
            }
        } else if (attName.equals("bgcolor")) {
            bgColor = new float[4];
            int c = (int) Long.parseLong(attValue, 16);
            bgColor[0] = ((c >> 24) & 0xff) / 255f;
            bgColor[1] = ((c >> 16) & 0xff) / 255f;
            bgColor[2] = ((c >> 8) & 0xff) / 255f;
            bgColor[3] = ((c >> 0) & 0xff) / 255f;
        } else if (attName.equals("color")) {
            color = new float[4];
            int c = (int) Long.parseLong(attValue, 16);
            color[0] = ((c >> 24) & 0xff) / 255f;
            color[1] = ((c >> 16) & 0xff) / 255f;
            color[2] = ((c >> 8) & 0xff) / 255f;
            color[3] = ((c >> 0) & 0xff) / 255f;
        }
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

    protected void resetBoundle() {
        x = raw_x;
        y = raw_y;
        width = raw_width;
        height = raw_height;
        viewW = XDef.NODEF;
        viewH = XDef.NODEF;
    }

    protected int getTrialViewH() {
        if (viewH != XDef.NODEF) {
            return viewH;
        }
        int parentViewH = parent.getTrialViewH();
        if (parentViewH != XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF) {
                return parentViewH * raw_heightPercent / 100 - getDiff_ViewH2Height();
            } else {
                return XDef.NODEF;
            }
        } else {
            return XDef.NODEF;
        }
    }

    protected int getTrialViewW() {
        if (viewW != XDef.NODEF) {
            return viewW;
        }
        int parentViewW = parent.getTrialViewW();
        if (parentViewW != XDef.NODEF) {
            if (raw_widthPercent != XDef.NODEF) {
                return parentViewW * raw_widthPercent / 100 - getDiff_viewW2Width();
            } else {
                return XDef.NODEF;
            }
        } else {
            return XDef.NODEF;
        }
    }

    /**
     * how pix viewW less than width
     *
     * @return
     */
    protected int getDiff_viewW2Width() {
        return 0;
    }

    /**
     * how pix viewH less than height
     *
     * @return
     */
    protected int getDiff_ViewH2Height() {
        return 0;
    }

    protected int getDefaultWidth(int parentViewW) {
        return parentViewW;
    }

    protected int getDefaultHeight(int parentViewH) {
        return parentViewH;
    }

    protected void preAlignVertical() {
        if (height == XDef.NODEF) {
            int parentTrialViewH = parent.getTrialViewH();
            if (raw_heightPercent != XDef.NODEF && parentTrialViewH != XDef.NODEF) {
                height = raw_heightPercent * parentTrialViewH / 100;
            } else {
                height = getDefaultHeight(parentTrialViewH);
            }
            viewH = height - getDiff_ViewH2Height();
        }
    }

    protected void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            int parentTrialViewW = parent.getTrialViewW();
            if (raw_widthPercent != XDef.NODEF && parentTrialViewW != XDef.NODEF) {
                width = raw_widthPercent * parentTrialViewW / 100;
            } else {
                width = getDefaultWidth(parentTrialViewW);
            }
            viewW = width - getDiff_viewW2Width();
        }
    }

    public abstract GObject getGui();

    protected abstract String getXmlTag();

    protected abstract void createGui();


}

