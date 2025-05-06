package org.mini.layout;

import org.mini.gui.GImage;
import org.mini.gui.GListItem;
import org.mini.gui.GObject;
import org.mini.gui.GPanel;
import org.mini.nanovg.Nanovg;
import org.xmlpull.v1.KXmlParser;

public class XListItem extends XContainer {
    static public final String XML_NAME = "li";

    String pic;
    boolean selected = false;

    float[] preiconColor;

    protected GListItem listItem;

    public XListItem(XContainer xc) {
        super(xc);
        align = Nanovg.NVG_ALIGN_LEFT | Nanovg.NVG_ALIGN_MIDDLE;
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }


    public GObject getGui() {
        return listItem;
    }


    protected <T extends GObject> T createGuiImpl() {
        GImage img = null;
        if (pic != null) {
            img = getAssist().loadImage(pic);
        }
        return (T) new GListItem(getAssist().getForm(), img, text);
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        attName = attName.toLowerCase();
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("selected")) {
            selected = "0".equals(attValue) ? false : true;
        } else if (attName.equals("pic")) {
            pic = attValue;
        } else if (attName.equals("pcolor")) {
            try {
                preiconColor = parseHexColor(attValue);
            } catch (Exception e) {
            }
        }
    }


    protected void createAndSetGui() {
        if (listItem == null) {
            listItem = createGuiImpl();
            initGuiMore();
        } else {
            listItem.setLocation(x, y);
            listItem.setSize(width, height);
        }
        super.createAndSetGui();
    }
}
