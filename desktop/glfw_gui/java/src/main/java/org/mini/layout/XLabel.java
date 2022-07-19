package org.mini.layout;

import org.mini.gui.GLabel;
import org.mini.gui.GObject;
import org.mini.layout.xmlpull.KXmlParser;
import org.mini.nanovg.Nanovg;

/**
 *
 */
public class XLabel
        extends XObject {

    static public final String XML_NAME = "label";
    // 当前绘制颜色
    protected int align = Nanovg.NVG_ALIGN_LEFT | Nanovg.NVG_ALIGN_TOP;
    protected int addon = XDef.SPACING_LABEL_ADD;
    protected boolean multiLine = false;

    GLabel label;

    public XLabel() {
        super(null);
    }

    public XLabel(XContainer xc) {
        super((xc));
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
        } else if (attName.equals("addon")) {
            addon = Integer.parseInt(attValue);
        } else if (attName.equals("multiline")) {
            multiLine = "0".equals(attValue) ? false : true;
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
        if (getText().startsWith("1.")) {
            int debug = 1;
        }
        int w = XUtil.measureWidth(parentViewW, text, fontSize);
        w = w + addon;
        return w;
    }

    protected int getDefaultHeight(int parentViewH) {
        if (multiLine) {
            return XUtil.measureHeight(viewW, text, fontSize);
        } else {
            return XDef.DEFAULT_COMPONENT_HEIGHT;
        }
    }

    protected void createGui() {
        if (label == null) {
            label = new GLabel(text, x, y, width, height);
            initGui();
            label.setAlign(align);
            label.setShowMode(multiLine ? GLabel.MODE_MULTI_SHOW : GLabel.MODE_SINGLE_SHOW);

        } else {
            label.setLocation(x, y);
            label.setSize(width, height);
        }
    }


    public GObject getGui() {
        return label;
    }
}
