package org.mini.layout;

import org.mini.gui.GImage;
import org.mini.gui.GImageItem;
import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.gscript.Str;
import org.mini.layout.xmlpull.KXmlParser;

public class XImageItem extends XObject implements GActionListener {
    static public final String XML_NAME = "img"; //xml tag名
    protected String pic;
    protected String onClick;
    protected GImageItem imgItem = null;
    protected boolean border;
    protected float alpha = 1.f;

    public XImageItem(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("pic")) {
            pic = attValue;
        } else if (attName.equals("border")) {
            border = "0".equals(attValue) ? false : true;
        } else if (attName.equals("onclick")) {
            onClick = XUtil.getField(attValue, 0);
        } else if (attName.equals("alpha")) {
            alpha = (float) Integer.parseInt(attValue) / 255;
        }
    }

    public void parse(KXmlParser parser) throws Exception {
        super.parse(parser);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, getXmlTag());
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

    protected void preAlignVertical() {
        if (height == XDef.NODEF) {
            if (raw_heightPercent != XDef.NODEF && parent.viewH != XDef.NODEF) {
                viewH = height = raw_heightPercent * parent.viewH / 100;
            } else {
                viewH = height = parent.viewW;
            }
        }
    }

    protected void preAlignHorizontal() {
        if (width == XDef.NODEF) {
            if (raw_widthPercent == XDef.NODEF) {
                viewW = width = parent.viewW;
            } else {
                viewW = width = raw_widthPercent * parent.viewW / 100;
            }
        }
    }

    @Override
    protected void createGui() {
        if (imgItem == null) {
            GImage img = GImage.createImageFromJar(pic);
            imgItem = new GImageItem(img);
            imgItem.setLocation(x, y);
            imgItem.setSize(width, height);
            imgItem.setName(name);
            imgItem.setAttachment(this);
            imgItem.setAlpha(alpha);
            imgItem.setDrawBorder(border);
        } else {
            imgItem.setLocation(x, y);
            imgItem.setSize(width, height);
        }
    }

    @Override
    public GObject getGui() {
        return imgItem;
    }
}
