package org.mini.layout;

import org.mini.gui.GForm;
import org.mini.gui.GObject;
import org.mini.layout.xmlpull.KXmlParser;

public class XForm extends XContainer {
    static public final String XML_NAME = "form";
    GForm form;

    public XForm(XContainer xc) {
        super(xc);
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        form = new GForm(null);//GForm做特殊处理,因为所有其他组件都依赖于他,所以放入assist中
        this.assist = assist;
        assist.setForm(form);

        super.parse(parser, assist);
        initGui();
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }

    @Override
    public GObject getGui() {
        return form;
    }

    protected void createGui() {
        if (form == null) {
            form = new GForm(getAssist().getForm());
            initGui();
            form.setLocation(x, y);
            form.setSize(width, height);
        } else {
            form.setLocation(x, y);
            form.setSize(width, height);
        }
    }
}
