package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GPanel;

import java.util.Random;

public class XPanel extends XContainer {
    static public final String XML_NAME = "panel";


    protected GPanel panel;

    public XPanel() {
        super(null);
    }

    public XPanel(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }


    public GObject getGui() {
        return panel;
    }

    Random random = new Random();

    protected void createGui() {
        if (panel == null) {
            panel = new GPanel(x, y, width, height)
//            {
//                public boolean paint(long vg) {
//                    Nanovg.nvgScissor(vg, getX(), getY(), width, height);
//                    GToolkit.drawRect(vg, getX(), getY(), width, height, new float[]{random.nextFloat(), random.nextFloat(), 0.2f, 0.5f});
//                    super.paint(vg);
//                    return true;
//                }
//            }
            ;
            panel.setName(name);
            panel.setAttachment(this);
        } else {
            panel.setLocation(x, y);
            panel.setSize(width, height);
        }
    }
}
