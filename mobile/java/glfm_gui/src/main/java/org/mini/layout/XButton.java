package org.mini.layout;

import org.mini.gui.GButton;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.gscript.Str;
import org.mini.layout.xmlpull.KXmlParser;

/**
 *
 */
public class XButton
        extends XObject implements GActionListener {

    static public final String XML_NAME = "button";
    //
    protected String pic;
    protected String onClick;
    protected int fontSize = XDef.DEFAULT_FONT_SIZE;
    protected int addon = XDef.SPACING_BUTTON_ADD;
    protected char emoji = 0;

    protected GButton button;

    public XButton() {
        super(null);
    }

    public XButton(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("pic")) {
            pic = attValue;
        } else if (attName.equals("onclick")) {
            onClick = XUtil.getField(attValue, 0);
        } else if (attName.equals("addon")) {
            addon = Integer.parseInt(attValue);
        } else if (attName.equals("emoji")) {
            emoji = (char) Integer.parseInt(attValue, 16);
        }
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser,assist);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, getXmlTag());
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


    protected int getDefaultWidth(int parentViewW) {
        int w = XUtil.measureWidth(parentViewW, text, fontSize);
        w += addon;
        return w;
    }

    protected int getDefaultHeight(int parentViewH) {
        return XDef.DEFAULT_COMPONENT_HEIGHT;
    }


    protected void createGui() {
        if (button == null) {
            button = new GButton(text, x, y, width, height);
            initGui();
            button.setActionListener(this);
            button.setIcon(emoji);
        } else {
            button.setLocation(x, y);
            button.setSize(width, height);
        }
    }


    public GObject getGui() {
        return button;
    }
}
