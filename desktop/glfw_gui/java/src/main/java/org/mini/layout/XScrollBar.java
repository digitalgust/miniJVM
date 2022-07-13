package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GScrollBar;
import org.mini.layout.xmlpull.KXmlParser;

/**
 *
 */
public class XScrollBar
        extends XObject {

    static public final String XML_NAME = "scrollbar";
    // 当前绘制颜色
    protected float value = 0.f;
    protected int mode = GScrollBar.HORIZONTAL;

    protected GScrollBar scrollBar;


    public XScrollBar() {
        super(null);
    }

    public XScrollBar(XContainer xc) {
        super((xc));
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        try {
            if (attName.equals("value")) {
                value = Float.parseFloat(attValue);
            } else if (attName.equals("scroll")) {
                mode = attValue.equalsIgnoreCase("h") ? GScrollBar.HORIZONTAL : GScrollBar.VERTICAL;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
        toEndTag(parser, getXmlTag());
    }

//----------------------------------------------------------------------------
//                    内部方法
//----------------------------------------------------------------------------

    protected int getDefaultWidth(int parentViewW) {
        int w = XUtil.measureWidth(parentViewW, text, fontSize);
        viewW = width = w + XDef.SPACING_CHECKBOX_ADD;
        return w;
    }

    protected int getDefaultHeight(int parentViewH) {
        return XUtil.measureHeight(viewW, text, fontSize);
    }

    protected void createGui() {
        if (scrollBar == null) {
            scrollBar = new GScrollBar(value, mode, x, y, width, height);
            initGui();
        } else {
            scrollBar.setLocation(x, y);
            scrollBar.setSize(width, height);
        }
    }


    public GObject getGui() {
        return scrollBar;
    }
}
