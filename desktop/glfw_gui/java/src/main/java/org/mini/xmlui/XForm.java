package org.mini.xmlui;

import org.mini.gui.GForm;
import org.mini.gui.GObject;

public class XForm extends XContainer {
    static public final String XML_NAME = "form";
    GForm form;

    public XForm(XContainer xc) {
        super(xc);
    }

    @Override
    String getXmlTag() {
        return XML_NAME;
    }

    @Override
    public GObject getGui() {
        return form;
    }

    void createGui() {
        if (form == null) {
            form = new GForm();
            form.setLocation(x, y);
            form.setSize(width, height);
            form.setName(name);
            form.setAttachment(this);
            for (int i = 0; i < size(); i++) {
                XObject xo = elementAt(i);
                GObject go = xo.getGui();
                if (go != null) form.add(go);
            }
        } else {
            form.setLocation(x, y);
            form.setSize(width, height);
        }
    }
}
