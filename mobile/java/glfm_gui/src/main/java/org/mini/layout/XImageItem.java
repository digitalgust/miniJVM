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
    protected int img_w = XDef.DEFAULT_COMPONENT_HEIGHT, img_h = XDef.DEFAULT_COMPONENT_HEIGHT;
    protected String onClick;
    protected GImageItem imgItem = null;
    protected boolean border;
    protected float alpha = 1.f;

    public XImageItem() {
        super(null);
    }

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
            GImage img = GImage.createImageFromJar(pic);
            if (img != null) {
                img_w = img.getWidth();
                img_h = img.getHeight();
            }
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

    protected int getDefaultWidth(int parentViewW) {
        return img_w;
    }

    protected int getDefaultHeight(int parentViewH) {
        return img_h;
    }

    @Override
    protected void createGui() {
        if (imgItem == null) {
            GImage img = GImage.createImageFromJar(pic);
            imgItem = new GImageItem(img);
            imgItem.setLocation(x, y);
            imgItem.setSize(width, height);
            imgItem.setName(name);
            imgItem.setXmlAgent(this);
            imgItem.setAlpha(alpha);
            imgItem.setDrawBorder(border);
            imgItem.setActionListener(this);
        } else {
            imgItem.setLocation(x, y);
            imgItem.setSize(width, height);
        }
    }

    @Override
    public GObject getGui() {
        return imgItem;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String s) {
        pic = s;
        if (imgItem != null) {
            GImage img = GImage.createImageFromJar(s);
            imgItem.setImg(img);
        }
    }
}
