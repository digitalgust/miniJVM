package org.mini.layout;

import org.mini.gui.GCheckBox;
import org.mini.gui.GObject;
import org.xmlpull.v1.KXmlParser;

/**
 *
 */
public class XCheckBox
        extends XObject {

    static public final String XML_NAME = "checkbox";
    // 当前绘制颜色
    protected boolean selected = true;

    protected GCheckBox checkBox;

    public XCheckBox(XContainer xc) {
        super((xc));
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("selected")) {
            selected = "0".equals(attValue) ? false : true;
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
        viewW = width = w + XDef.SPACING_CHECKBOX_ADD;
        return w;
    }

    protected int getDefaultHeight(int parentViewH) {
        return XUtil.measureHeight(viewW, text, fontSize);
    }

    protected <T extends GObject> T createGuiImpl() {
        return (T) new GCheckBox(getAssist().getForm(), text, false, x, y, width, height);
    }

    protected void createAndSetGui() {
        if (checkBox == null) {
            checkBox = createGuiImpl();
            initGuiMore();
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
