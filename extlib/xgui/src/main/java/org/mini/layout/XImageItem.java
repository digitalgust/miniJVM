package org.mini.layout;

import org.mini.gui.GImage;
import org.mini.gui.GImageItem;
import org.mini.gui.GObject;
import org.mini.gui.GSubImage;
import org.mini.layout.loader.XmlExtAssist;
import org.xmlpull.v1.KXmlParser;

public class XImageItem extends XObject {
    static public final String XML_NAME = "img"; //xml tag名
    protected String pic;
    protected int img_w = XDef.DEFAULT_COMPONENT_HEIGHT, img_h = XDef.DEFAULT_COMPONENT_HEIGHT;
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
        if (attName.equals("pic") || attName.equals("src")) {
            pic = attValue;
        } else if (attName.equals("border")) {
            border = "0".equals(attValue) ? false : true;
        } else if (attName.equals("onclick")) {
            onClick = attValue;
        } else if (attName.equals("alpha")) {
            alpha = (float) Integer.parseInt(attValue) / 255;
        }
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
        if (pic != null) { //等pic和picpara都解析完再取子图尺寸
            GImage img = applyPicPara(getAssist().loadImage(pic));
            if (img != null) {
                img_w = img.getWidth();
                img_h = img.getHeight();
            }
        }
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, getXmlTag());
    }

    protected int getDefaultWidth(int parentViewW) {
        return img_w;
    }

    protected int getDefaultHeight(int parentViewH) {
        return img_h;
    }

    protected <T extends GObject> T createGuiImpl() {
        GImage img = null;
        if (pic != null) {
            img = applyPicPara(getAssist().loadImage(pic));
        }
        return (T) new GImageItem(getAssist().getForm(), img);
    }

    @Override
    protected void createAndSetGui() {
        if (imgItem == null) {
            imgItem = createGuiImpl();
            initGuiMore();
            imgItem.setLocation(x, y);
            imgItem.setSize(width, height);
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

    public String getPic() {
        return pic;
    }

    public void setPic(String s) {
        pic = s;
        if (imgItem != null) {
            GImage img = applyPicPara(getAssist().loadImage(s));
            imgItem.setImg(img);
        }
    }

    /**
     * 运行时切换 picpara 子图索引（不改对象、不上传纹理，可每帧调用），未用 picpara 时无效果
     */
    public void setPicIndex(int index) {
        if (imgItem != null && imgItem.getImg() instanceof GSubImage) {
            ((GSubImage) imgItem.getImg()).setIndex(index);
        }
    }
}
