package org.mini.xmlui;

import org.mini.gui.GFrame;
import org.mini.gui.GObject;


/**
 *
 */
public class XFrame
        extends XContainer {

    static public final String XML_NAME = "frame";

    GFrame frame;

    private String title;
    private String onClose;


    /**
     *
     */
    public XFrame() {
        super(null);
    }

    void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("title")) {
            title = attValue;
        } else if (attName.equals("onclose")) {
            onClose = attValue;
        }
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    void setRootSize(int guiRootW, int guiRootH) {

        super.setRootSize(guiRootW, guiRootH);
        //create a frame only for get viewW and viewH
        // because fram width != viewW
        GFrame tmp = new GFrame(title, x, y, width, height);
        tmp.setAttachment(this);
        viewW = (int) tmp.getView().getW();
        viewH = (int) tmp.getView().getH();
    }

    void createGui() {
        if (frame == null) {
            frame = new GFrame(title, x, y, width, height);
            viewW = (int) frame.getView().getW();
            viewH = (int) frame.getView().getH();
            frame.setName(name);
            frame.setAttachment(this);
            for (int i = 0; i < size(); i++) {
                XObject xo = elementAt(i);
                GObject go = xo.getGui();
                if (go != null) frame.getView().add(go);
            }
        } else {
            frame.setLocation(x, y);
            frame.setSize(width, height);
        }
    }

    @Override
    public GObject getGui() {
        return frame;
    }

    /**
     *
     */
    public void cancel() {
    }



}
