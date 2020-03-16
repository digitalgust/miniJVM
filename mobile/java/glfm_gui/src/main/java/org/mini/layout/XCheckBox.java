package org.mini.layout;

import org.mini.gui.GCheckBox;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.gscript.Str;
import org.mini.layout.xmlpull.KXmlParser;

/**
 *
 */
public class XCheckBox
        extends XObject implements GActionListener {

    String cmd = null; //命令
    String onClick;
    static public final String XML_NAME = "checkbox";
    // 当前绘制颜色
    int fontSize = XDef.DEFAULT_FONT_SIZE;
    boolean selected = true;

    GCheckBox checkBox;


    public XCheckBox(XContainer xc) {
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

    void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("selected")) {
            selected = "0".equals(attValue) ? false : true;
        }
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
                viewW = width = w + XDef.SPACING_CHECKBOX_ADD;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }


    void createGui() {
        if (checkBox == null) {
            checkBox = new GCheckBox(text, false, x, y, width, height);
            checkBox.setName(name);
            checkBox.setAttachment(this);
            checkBox.setActionListener(this);
            checkBox.setChecked(selected);
        } else {
            checkBox.setLocation(x, y);
            checkBox.setSize(width, height);
        }
    }


    public GObject getGui() {
        return checkBox;
    }
}
