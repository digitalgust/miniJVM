package org.mini.layout;

import org.mini.gui.GFrame;
import org.mini.gui.GObject;
import org.mini.gui.GToolkit;


/**
 *
 */
public class XFrame
        extends XContainer {

    static public final String XML_NAME = "frame";

    protected GFrame frame;

    protected String title;
    protected String titleBgPic;
    protected float titleBgPicAlpha = GObject.DEFAULT_BG_ALPHA;
    protected String viewBgPic;
    protected float viewBgPicAlpha = GObject.DEFAULT_BG_ALPHA;
    protected String onCloseScript;
    protected String onInitScript;
    boolean closable = true;
    boolean titleShow = true;


    public XFrame(XContainer parent) {
        super(parent);
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        attName = attName.toLowerCase();
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("closable")) {
            closable = "0".equals(attValue) ? false : true;
        } else if (attName.equals("titleshow")) {
            titleShow = "0".equals(attValue) ? false : true;
        } else if (attName.equals("title")) {
            title = attValue;
        } else if (attName.equals("onclose")) {
            onCloseScript = attValue;
        } else if (attName.equals("oninit")) {
            onInitScript = attValue;
        } else if (attName.equals("titlebgpic")) {
            titleBgPic = attValue;
        } else if (attName.equals("titlebgpicalpha")) {
            titleBgPicAlpha = Float.valueOf(attValue);
        } else if (attName.equals("viewbgpic")) {
            viewBgPic = attValue;
        } else if (attName.equals("viewbgpicalpha")) {
            viewBgPicAlpha = Float.valueOf(attValue);
        }
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    protected int getTrialViewH() {
        int tmpViewH = super.getTrialViewH();
        if (tmpViewH != XDef.NODEF) {
            return tmpViewH - (int) GFrame.TITLE_HEIGHT;
        }
        return tmpViewH;
    }

    protected void preAlignVertical() {
        super.preAlignVertical();
        if (y == XDef.NODEF) {
            if (raw_yPercent != XDef.NODEF && parent != null && parent.getTrialViewH() != XDef.NODEF) {
                y = raw_yPercent * parent.getTrialViewH() / 100;
            } else {
                y = 0;
            }
        }
    }

    protected void preAlignHorizontal() {
        super.preAlignHorizontal();
        if (x == XDef.NODEF) {
            if (raw_xPercent != XDef.NODEF && parent != null && parent.getTrialViewH() != XDef.NODEF) {
                x = raw_xPercent * parent.getTrialViewW() / 100;
            } else {
                x = 0;
            }
        }
    }

    /**
     * how pix viewW less than width
     *
     * @return
     */
    protected int getDiff_viewW2Width() {
        return 4;
    }

    /**
     * how pix viewH less than height
     *
     * @return
     */
    protected int getDiff_ViewH2Height() {
        return 4;
    }

    protected <T extends GObject> T createGuiImpl() {
        return (T) new GFrame(getAssist().getForm(), title, x, y, width, height);
    }

    protected void createAndSetGui() {
        if (frame == null) {
            frame = createGuiImpl();
            initGuiMore();
            frame.setClosable(closable);
            frame.setOnCloseScript(onCloseScript);
            frame.setOnInitScript(onInitScript);
            frame.getTitlePanel().setBgImg(GToolkit.getCachedImageFromJar(titleBgPic));
            frame.getTitlePanel().setBgImgAlpha(titleBgPicAlpha);
            frame.getView().setBgImg(GToolkit.getCachedImageFromJar(viewBgPic));
            frame.getView().setBgImgAlpha(viewBgPicAlpha);
            frame.setTitleShow(titleShow);
        } else {
            frame.setLocation(x, y);
            frame.setSize(width, height);
        }
    }

    protected boolean isFreeObj() {
        return true;
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
