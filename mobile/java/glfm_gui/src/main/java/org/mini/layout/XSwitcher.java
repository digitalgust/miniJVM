package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GSwitcher;
import org.mini.gui.event.GActionListener;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.gscript.Str;
import org.mini.layout.xmlpull.KXmlParser;

public class XSwitcher
        extends XObject implements GActionListener {
    static public final String XML_NAME = "switcher";

    GSwitcher switcher;
    protected String onClick;

    public XSwitcher() {
        super(null);
    }

    public XSwitcher(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    boolean parseNoTagText() {
        return false;
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
        toEndTag(parser, XML_NAME);
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("onclick")) {
            onClick = XUtil.getField(attValue, 0);
        }
    }

    @Override
    public void action(GObject gobj) {
        if (onClick != null) {
            Interpreter inp = getRoot().getInp();
            // 执行脚本
            if (inp != null) {
                inp.callSub(onClick);
            }
        }
        getRoot().getEventHandler().action(gobj, gobj.getCmd());
    }


    protected int getDefaultWidth(int parentViewW) {
        return (int) GSwitcher.DEFAULT_WIDTH;
    }

    protected int getDefaultHeight(int parentViewH) {
        return (int) GSwitcher.DEFAULT_HEIGHT;
    }

    protected void createGui() {
        if (switcher == null) {
            switcher = new GSwitcher();
            initGui();
            switcher.setActionListener(this);
        } else {
            switcher.setLocation(x, y);
            switcher.setSize(width, height);
        }
    }


    public GObject getGui() {
        return switcher;
    }
}
