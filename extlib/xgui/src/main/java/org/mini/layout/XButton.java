package org.mini.layout;

import org.mini.gui.GButton;
import org.mini.gui.GObject;
import org.mini.layout.loader.XmlExtAssist;
import org.mini.nanovg.Nanovg;
import org.xmlpull.v1.KXmlParser;

/**
 *
 */
public class XButton
        extends XObject {

    static public final String XML_NAME = "button";
    //
    protected String pic;
    protected int addon = XDef.SPACING_BUTTON_ADD;

    protected int align = Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE;
    float[] preiconColor;

    protected GButton button;

    public XButton(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("align")) {
            align = 0;
            for (String s : attValue.split(",")) {
                align |= XUtil.parseAlign(s);
            }
        } else if (attName.equals("pic")) {
            pic = attValue;
        } else if (attName.equals("addon")) {
            addon = Integer.parseInt(attValue);
        } else if (attName.equals("pcolor")) {
            preiconColor = parseHexColor(attValue);
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
        int w = XUtil.measureWidth(parentViewW, text, getFontSize(), false);
        w += addon;
        return w;
    }

    protected int getDefaultHeight(int parentViewH) {
        return XDef.DEFAULT_COMPONENT_HEIGHT;
    }


    protected <T extends GObject> T createGuiImpl() {
        return (T) new GButton(getAssist().getForm(), text, x, y, width, height);
    }

    protected void createAndSetGui() {
        if (button == null) {
            button = createGuiImpl();
            initGuiMore();
            button.setPreIcon(preicon);
            if (preiconColor != null) {
                button.setPreiconColor(preiconColor);
            }
            button.setAlign(align);
        } else {
            button.setLocation(x, y);
            button.setSize(width, height);
        }
    }


    public <T extends GObject> T getGui() {
        return (T) button;
    }
}
