package org.mini.xmlui;

import org.mini.gui.GImage;
import org.mini.gui.GImageItem;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.xmlui.gscript.Interpreter;
import org.mini.xmlui.gscript.Str;

public class XImageItem extends XObject implements GActionListener {
    static public final String XML_NAME = "img"; //xml tag名
    String pic;
    String cmd;
    String onClick;
    GImageItem imgItem = null;
    int width = 0;
    int height = 0;

    public XImageItem(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("pic")) {
            pic = attValue;
        } else if (attName.equals("cmd")) {
            cmd = attValue;
        } else if (attName.equals("onclick")) {
            onClick = XUtil.getField(attValue, 0);
        }
    }


    @Override
    public void action(GObject gobj) {
        if (onClick != null) {
            Interpreter inp = getRoot().getInp();
            // 执行脚本
            if (inp != null) {
                inp.putGlobalVar("cmd", new Str(cmd));
                inp.callSub(onClick);
            }
        }
        getRoot().getEventHandler().action(gobj, cmd);
    }

    void preAlignVertical() {
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parent.viewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parent.viewH / 100;
            } else {
                viewH = height = parent.viewW;
            }
        }
    }

    void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parent.viewW;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }

    @Override
    void createGui() {
        if (imgItem == null) {
            GImage img = GImage.createImageFromJar(pic);
            imgItem = new GImageItem(img);
            imgItem.setLocation(x, y);
            imgItem.setSize(width, height);
            imgItem.setName(name);
            imgItem.setAttachment(this);
        } else {
            imgItem.setLocation(x, y);
            imgItem.setSize(width, height);
        }
    }

    @Override
    GObject getGui() {
        return imgItem;
    }
}
