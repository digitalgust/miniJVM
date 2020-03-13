package org.mini.xmlui;

import org.mini.gui.GButton;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.xmlui.gscript.Interpreter;
import org.mini.xmlui.gscript.Str;
import org.mini.xmlui.xmlpull.KXmlParser;

/**
 *
 */
public class XButton
        extends XObject implements GActionListener {

    String pic;
    String onClick;
    static public final String XML_NAME = "button";
    static public final char SPLIT_CMD_CHAR = ':'; //命令中的分隔符
    // 当前绘制颜色
    int fontSize = XDef.DEFAULT_FONT_SIZE;

    GButton button;

    public XButton(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("pic")) {
            pic = attValue;
        } else if (attName.equals("cmd")) {
            cmd = attValue;
        } else if (attName.equals("onclick")) {
            onClick = XUtil.getField(attValue, 0);
        }
    }

    public void parse(KXmlParser parser) throws Exception {
        super.parse(parser);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, XML_NAME);
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


    //----------------------------------------------------------------------------
    //                    内部方法
    //----------------------------------------------------------------------------

    void preAlignVertical() {
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parent.viewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parent.viewH / 100;
            } else {
                viewH = height = XDef.DEFAULT_COMPONENT_HEIGHT;
            }
        }
    }

    void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                int w = XUtil.measureWidth(parent.viewW, text, fontSize);
                w += XDef.SPACING_BUTTON_ADD;
                viewW = width = w;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }

    void createGui() {
        if (button == null) {
            button = new GButton(text, x, y, width, height);
            button.setName(name);
            button.setAttachment(this);
            button.setActionListener(this);
        } else {
            button.setLocation(x, y);
            button.setSize(width, height);
        }
    }


    public GButton getButton() {
        return button;
    }

    GObject getGui() {
        return button;
    }
}
