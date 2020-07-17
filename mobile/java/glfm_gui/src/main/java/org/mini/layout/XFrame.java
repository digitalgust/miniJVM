package org.mini.layout;

import org.mini.gui.GFrame;
import org.mini.gui.GObject;
import org.mini.gui.event.GStateChangeListener;
import org.mini.layout.gscript.Interpreter;
import org.mini.layout.gscript.Str;


/**
 *
 */
public class XFrame
        extends XContainer implements GStateChangeListener {

    static public final String XML_NAME = "frame";

    protected GFrame frame;

    protected String title;
    protected String onClose;
    boolean closable = true;


    /**
     *
     */
    public XFrame() {
        super(null);
    }

    public XFrame(XContainer parent) {
        super(parent);
    }

    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("closable")) {
            closable = "0".equals(attValue) ? false : true;
        } else if (attName.equals("title")) {
            title = attValue;
        } else if (attName.equals("onclose")) {
            onClose = attValue;
        }
    }

    public String getXmlTag() {
        return XML_NAME;
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
        return 34;
    }

    protected void createGui() {
        if (frame == null) {
            frame = new GFrame(title, x, y, width, height);
            frame.setName(name);
            frame.setXmlAgent(this);
            frame.setClosable(closable);
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


    @Override
    public void onStateChange(GObject gobj) {
        if (onClose != null) {
            Interpreter inp = getRoot().getInp();
            // 执行脚本
            if (inp != null) {
                inp.putGlobalVar("cmd", new Str(cmd));
                inp.callSub(onClose);
            }
        }
        getRoot().getEventHandler().onStateChange(gobj, cmd);
    }
}
