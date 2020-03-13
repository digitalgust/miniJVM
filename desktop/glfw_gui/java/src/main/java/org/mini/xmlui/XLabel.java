package org.mini.xmlui;

import org.mini.gui.GLabel;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.xmlui.gscript.Interpreter;
import org.mini.xmlui.gscript.Str;
import org.mini.xmlui.xmlpull.KXmlParser;

/**
 *
 */
public class XLabel
        extends XObject implements GActionListener {

    String onClick;
    static public final String XML_NAME = "checkBox";
    // 当前绘制颜色
    int fontSize = XDef.DEFAULT_FONT_SIZE;

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


    public void parse(KXmlParser parser) throws Exception {
        super.parse(parser);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, XML_NAME);
    }

//----------------------------------------------------------------------------
//                    内部方法
//----------------------------------------------------------------------------

    void preAlignVertical() {
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parent.viewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parent.viewH / 100;
            } else {
                int h = XUtil.measureHeight(viewW, text, fontSize);
                viewH = height = h;
            }
        }
    }

    void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                int w = XUtil.measureWidth(parent.viewW, text, fontSize);
                viewW = width = w + XDef.SPACING_LABEL_ADD;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }


    void createGui() {
        if (label == null) {
            label = new GLabel(text, x, y, width, height);
            label.setName(name);
            label.setAttachment(this);
            label.setActionListener(this);
        } else {
            label.setLocation(x, y);
            label.setSize(width, height);
        }
    }


    public GLabel getLabel() {
        return label;
    }

    GObject getGui() {
        return label;
    }
}
