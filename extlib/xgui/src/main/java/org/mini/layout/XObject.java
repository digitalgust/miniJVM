package org.mini.layout;

import org.mini.gui.*;
import org.mini.gui.gscript.Interpreter;
import org.xmlpull.v1.KXmlParser;

public abstract class XObject implements GLayout {

    //used for align
    protected int x = XDef.NODEF, y = XDef.NODEF, width = XDef.NODEF, height = XDef.NODEF;
    //protected int raw_xPercent = XDef.NODEF, raw_yPercent = XDef.NODEF, raw_widthPercent = XDef.NODEF, raw_heightPercent = XDef.NODEF;
    //used for realign
    protected int raw_x = XDef.NODEF, raw_y = XDef.NODEF, raw_width = XDef.NODEF, raw_height = XDef.NODEF;
    protected int raw_xPercent = XDef.NODEF, raw_yPercent = XDef.NODEF, raw_widthPercent = XDef.NODEF, raw_heightPercent = XDef.NODEF;

    //供子组件显示的区域
    protected int viewW = XDef.NODEF, viewH = XDef.NODEF;

    protected boolean vfloat = false, hfloat = false;

    protected boolean hidden = false; //是否显示

    protected boolean enable = true; //是否显示

    protected boolean fly = false; //是否可飞

    protected boolean frontest = false; //始终最前端显示

    protected boolean backest = false; //始终最后端显示

    protected String name = null; //组件名字

    protected String text = null; //文本

    protected int fontSize = XDef.DEFAULT_FONT_SIZE;

    protected String preicon;

    protected String cmd = null;

    protected String attachment = null;

    protected String moveMode;

    protected XContainer parent;

    protected float[] bgColor;

    protected float[] color;
    protected String bgPic = null;
    protected float bgPicAlpha = GObject.DEFAULT_BG_ALPHA;

    protected String href = null;
    // 脚本引擎
    protected String script;// 脚本

    protected String onClinkScript = null; //click exec

    protected String onStateChangeScript = null; //state change exec

    protected boolean fixed = false;
    protected String onCloseScript;
    protected String onInitScript;
    //

    XmlExtAssist assist;

    /**
     * @param xc
     */
    public XObject(XContainer xc) {
        parent = xc;
    }

    @Override
    public XContainer getParent() {
        return parent;
    }


    @Override
    public void setParent(GLayout p) {
        if (p != null) {
            if (p instanceof XContainer) {
                XContainer xc = (XContainer) p;
                if (!xc.children.contains(this)) {
                    xc.children.add(this);
                }
                this.parent = xc;
            } else {
                System.out.println("error ============");
            }
        } else {
            if (this.parent != null) {
                this.parent.children.remove(this);
            }
            this.parent = null;
        }
    }

    public void reSize(int parentW, int parentH) {

    }


    public XContainer getRoot() {
        if (parent == null) {
            if (this instanceof XContainer) return (XContainer) this;
            else return null;
        }
        return parent.getRoot();
    }

    /**
     * 解析对应的xml
     *
     * @param parser KXmlParser
     * @throws Exception
     */
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        this.assist = assist;
        //iterator attribute
        int attCount = parser.getAttributeCount();
        for (int i = 0; i < attCount; i++) {
            String attName = parser.getAttributeName(i);
            String attValue = parser.getAttributeValue(i);


            parseMoreAttribute(attName, attValue);
        }
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        attName = attName.toLowerCase();
        if (attName.equals("name")) { // 标题
            name = attValue;
        } else if (attName.equals("attachment")) {
            attachment = attValue;
        } else if (attName.equals("cmd")) {
            cmd = attValue;
        } else if (attName.equals("onclick")) {
            onClinkScript = attValue;
        } else if (attName.equals("onchange")) {
            onStateChangeScript = attValue;
        } else if (attName.equals("fly")) {
            fly = "0".equals(attValue) ? false : true;
        } else if (attName.equals("hidden")) {
            hidden = "0".equals(attValue) ? false : true;
        } else if (attName.equals("enable")) {
            enable = "0".equals(attValue) ? false : true;
        } else if (attName.equals("move")) { // viewslot move mode
            moveMode = attValue;
        } else if (attName.equals("fixed")) {
            fixed = "0".equals(attValue) ? false : true;
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
            bgColor = parseHexColor(attValue);
        } else if (attName.equals("color")) {
            color = parseHexColor(attValue);
        } else if (attName.equals("front")) {
            frontest = "0".equals(attValue) ? false : true;
        } else if (attName.equals("back")) {
            backest = "0".equals(attValue) ? false : true;
        } else if (attName.equals("fontsize")) {
            fontSize = Integer.parseInt(attValue);
        } else if (attName.equals("preicon")) {
            preicon = attValue;
        } else if (attName.equals("bgpic")) {
            bgPic = attValue;
        } else if (attName.equals("bgpicalpha")) {
            bgPicAlpha = Float.parseFloat(attValue);
        } else if (attName.equals("href")) {
            href = attValue;
        } else if (attName.equals("onclose")) {
            onCloseScript = attValue;
        } else if (attName.equals("oninit")) {
            onInitScript = attValue;
        }
    }

    protected float[] parseHexColor(String hexColor) {
        try {
            int c = (int) Long.parseLong(hexColor, 16);
            float[] color = new float[4];
            color[0] = ((c >> 24) & 0xff) / 255f;
            color[1] = ((c >> 16) & 0xff) / 255f;
            color[2] = ((c >> 8) & 0xff) / 255f;
            color[3] = ((c >> 0) & 0xff) / 255f;
            return color;
        } catch (Exception e) {
        }
        return null;
    }

    public final void initGuiMore() {
        GObject gui = getGui();
        if (gui != null) {
            gui.setLayout(this);
            gui.setEnable(enable);
            gui.setName(name);
            gui.setText(text);
            gui.setFontSize(fontSize);
            gui.setAttachment(attachment);
            gui.setCmd(cmd);
            gui.setFront(frontest);
            gui.setBack(backest);
            gui.setFlyable(fly);
            gui.setOnClinkScript(onClinkScript);
            gui.setActionListener(assist.getEventHandler());
            gui.setHrefListener(assist.getEventHandler());
            gui.setStateChangeListener(assist.getEventHandler());
            gui.setOnStateChangeScript(onStateChangeScript);
            gui.setHref(href);
            gui.setFixed(fixed);
            if (fly) {
                gui.setFlyListener(getRoot().getEventHandler());
            }
            if (color != null) {
                gui.setColor(color);
            }
            if (bgColor != null) {
                gui.setBgColor(bgColor);
            }
            if (bgPic != null) {
                GImage img = getAssist().loadImage(bgPic);
                gui.setBgImg(img);
                gui.setBgImgAlpha(bgPicAlpha);
            }

            if (gui instanceof GContainer) {

                GContainer container = (GContainer) gui;
                if (container != null && script != null) {
                    container.loadScript(script);// 装入代码
                    Interpreter inp = container.getInterpreter();
                    if (inp != null) {
                        if (assist != null) {
                            assist.interpreterSetup(inp);
                        }
                    }
                }
            }

            gui.setOnCloseScript(onCloseScript);
            gui.setOnInitScript(onInitScript);
            gui.setVisible(!hidden);
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
        if (getGui() != null) {
            getGui().setText(s);
        }
    }


    public String getText() {
        return text;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        if (getGui() != null) {
            getGui().setEnable(enable);
        }
        this.enable = enable;
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

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
        if (getGui() != null) {
            getGui().setAttachment(attachment);
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
        viewW = (raw_width != XDef.NODEF) ? (width - getDiff_viewW2Width()) : XDef.NODEF;
        viewH = (raw_height != XDef.NODEF) ? (height - getDiff_ViewH2Height()) : XDef.NODEF;
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

    /**
     * XFrame and XMenu is float gobject
     *
     * @return
     */
    protected boolean isFreeObj() {
        return false;
    }

    /**
     * tr in table and attribute is <tr h="float"></tr>
     *
     * @return
     */
    protected boolean isHfloat() {
        return hfloat;
    }

    /**
     * tr in table and attribute is <td w="float"></tr>
     *
     * @return
     */
    protected boolean isVfloat() {
        return vfloat;
    }

    public abstract <T extends GObject> T getGui();

    protected abstract String getXmlTag();

    protected abstract void createAndSetGui();

    protected abstract <T extends GObject> T createGuiImpl();


    public XmlExtAssist getAssist() {
        if (assist == null) return parent.getAssist();
        return assist;
    }
}

