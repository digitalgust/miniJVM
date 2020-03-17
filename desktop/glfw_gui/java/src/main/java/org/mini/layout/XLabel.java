package org.mini.layout;

import org.mini.gui.GGraphics;
import org.mini.gui.GLabel;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.gscript.Str;
import org.mini.layout.xmlpull.KXmlParser;

/**
 *
 */
public class XLabel
        extends XObject implements GActionListener {

    static public final String XML_NAME = "label";
    // 当前绘制颜色
    protected int fontSize = XDef.DEFAULT_FONT_SIZE;
    protected String onClick;
    protected int align = GGraphics.LEFT | GGraphics.TOP;
    protected int addon = XDef.SPACING_LABEL_ADD;

    GLabel label;


    public XLabel(XContainer xc) {
        super((xc));
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    @Override
    public void action(GObject gobj) {
        if (onClick != null) {
            Interpreter inp = getRoot().getInp();
            // 执行脚本
            if (inp != null) {
                inp.putGlobalVar("cmd", new Str(cmd));
                inp.callSub(onClick);
            }
        }
        getRoot().getEventHandler().action(gobj, cmd);
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("align")) {
            align = 0;
            for (String s : attValue.split(",")) {
                align |= XUtil.parseAlign(s);
            }
        } else if (attName.equals("cmd")) {
            cmd = attValue;
        } else if (attName.equals("onclick")) {
            onClick = XUtil.getField(attValue, 0);
        } else if (attName.equals("addon")) {
            addon = Integer.parseInt(attValue);
        }
    }

    public void parse(KXmlParser parser) throws Exception {
        super.parse(parser);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, getXmlTag());
    }

//----------------------------------------------------------------------------
//                    内部方法
//----------------------------------------------------------------------------

    protected void preAlignVertical() {
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parent.viewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parent.viewH / 100;
            } else {
                int h = XUtil.measureHeight(viewW, text, fontSize);
                viewH = height = h;
            }
        }
    }

    protected void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                int w = XUtil.measureWidth(parent.viewW, text, fontSize);
                viewW = width = w + addon;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }


    protected void createGui() {
        if (label == null) {
            label = new GLabel(text, x, y, width, height);
            label.setName(name);
            label.setAttachment(this);
            label.setActionListener(this);
            label.setAlign(align);
            label.setShowMode(GLabel.MODE_MULTI_SHOW);
            if (color != null) {
                label.setColor(color);
            }
            if (bgColor != null) {
                label.setBgColor(bgColor);
            }
        } else {
            label.setLocation(x, y);
            label.setSize(width, height);
        }
    }


    public GObject getGui() {
        return label;
    }
}
