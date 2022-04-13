package org.mini.layout;

import org.mini.gui.GButton;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.gui.gscript.Interpreter;
import org.mini.layout.xmlpull.KXmlParser;

/**
 *
 */
public class XButton
        extends XObject {

    static public final String XML_NAME = "button";
    //
    protected String pic;
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
        } else if (attName.equals("addon")) {
            addon = Integer.parseInt(attValue);
        } else if (attName.equals("emoji")) {
            emoji = (char) Integer.parseInt(attValue, 16);
        }
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, getXmlTag());
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
