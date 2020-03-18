package org.mini.layout;

import org.mini.gui.GForm;
import org.mini.gui.GObject;

public class XForm extends XContainer {
    static public final String XML_NAME = "form";
    GForm form;

    public XForm() {
        super(null);
    }

    public XForm(XContainer xc) {
        super(xc);
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
            form = new GForm();
            form.setLocation(x, y);
            form.setSize(width, height);
            form.setName(name);
            form.setAttachment(this);
        } else {
            form.setLocation(x, y);
            form.setSize(width, height);
        }
    }
}
