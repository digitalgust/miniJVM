package org.mini.xmlui;

import org.mini.gui.GObject;
import org.mini.gui.GPanel;
import org.mini.gui.GToolkit;
import org.mini.nanovg.Nanovg;

import java.util.Random;

public class XPanel extends XContainer {
    static public final String XML_NAME = "panel";


    GPanel panel;

    public XPanel(XContainer xc) {
        super(xc);
    }

    @Override
    String getXmlTag() {
        return XML_NAME;
    }


    public GPanel getPanel() {
        return panel;
    }

    GObject getGui() {
        return panel;
    }

    Random random = new Random();

    void createGui() {
        if (panel == null) {
            panel = new GPanel(x, y, width, height) {
                public boolean update(long vg) {
                    Nanovg.nvgScissor(vg, getX(), getY(), width, height);
                    GToolkit.drawRect(vg, getX(), getY(), width, height, new float[]{random.nextFloat(), random.nextFloat(), 0.2f, 0.5f});
                    super.update(vg);
                    return true;
                }
            };
            panel.setName(name);
            panel.setAttachment(this);
            for (int i = 0; i < size(); i++) {
                XObject xo = elementAt(i);
                GObject go = xo.getGui();
                if (go != null) panel.add(go);
            }
        } else {
            panel.setLocation(x, y);
            panel.setSize(width, height);
        }
    }
}
