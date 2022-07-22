/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.gui.gscript.*;
import org.mini.layout.*;
import org.mini.nanovg.Nanovg;

import java.util.ArrayList;


/**
 * xml ui script libary
 *
 * @author Gust
 */
public class GuiScriptLib extends Lib {

    {
        methodNames.put("setBgColor".toLowerCase(), 0);//  set background color
        methodNames.put("setColor".toLowerCase(), 1);//  set background color
        methodNames.put("setText".toLowerCase(), 2);//  set text
        methodNames.put("getText".toLowerCase(), 3);//  get text
        methodNames.put("setCmd".toLowerCase(), 8);//
        methodNames.put("getCmd".toLowerCase(), 9);//
        methodNames.put("close".toLowerCase(), 10);//  close frame
        methodNames.put("getCurSlot".toLowerCase(), 11);//
        methodNames.put("showSlot".toLowerCase(), 12);//
        methodNames.put("getImg".toLowerCase(), 13);//
        methodNames.put("setImg".toLowerCase(), 14);//
        methodNames.put("setImgPath".toLowerCase(), 15);//
        methodNames.put("getAttachStr".toLowerCase(), 16);//
        methodNames.put("setAttachStr".toLowerCase(), 17);//
        methodNames.put("getAttachInt".toLowerCase(), 18);//
        methodNames.put("setAttachInt".toLowerCase(), 19);//
        methodNames.put("setBgColorHexStr".toLowerCase(), 20);//  set background color
        methodNames.put("setColorHexStr".toLowerCase(), 21);//  set color
        methodNames.put("getListIdx".toLowerCase(), 22);//
        methodNames.put("setImgAlphaStr".toLowerCase(), 23);//
        methodNames.put("setEnable".toLowerCase(), 24);//
        methodNames.put("setListIdx".toLowerCase(), 25);//
        methodNames.put("setCheckBox".toLowerCase(), 26);//
        methodNames.put("getCheckBox".toLowerCase(), 27);//
        methodNames.put("setScrollBar".toLowerCase(), 28);//
        methodNames.put("getScrollBar".toLowerCase(), 29);//
        methodNames.put("setSwitch".toLowerCase(), 30);//
        methodNames.put("getSwitch".toLowerCase(), 31);//
        methodNames.put("getX".toLowerCase(), 32);//
        methodNames.put("getY".toLowerCase(), 33);//
        methodNames.put("getW".toLowerCase(), 34);//
        methodNames.put("getH".toLowerCase(), 35);//
        methodNames.put("setXY".toLowerCase(), 36);//
        methodNames.put("setWH".toLowerCase(), 37);//
        methodNames.put("loadXmlUI".toLowerCase(), 38);//
        methodNames.put("uiExist".toLowerCase(), 39);//

    }

    ;
    GForm form;

    /**
     *
     */
    public GuiScriptLib(GForm form) {
        this.form = form;
    }

    /**
     * @param para
     * @param methodID
     * @return
     */
    public DataType call(Interpreter inp, ArrayList<DataType> para, int methodID) {
        switch (methodID) {
            case 0:
                return setBgColor(para);
            case 1:
                return setColor(para);
            case 2:
                return setText(para);
            case 3:
                return getText(para);
            case 8:
                return setCmd(para);
            case 9:
                return getCmd(para);
            case 10:
                return close(para);
            case 11:
                return getCurSlot(para);
            case 12:
                return showSlot(para);
            case 13:
                return getImg(para);
            case 14:
                return setImg(para);
            case 15:
                return setImgPath(para);
            case 16:
                return getAttachStr(para);
            case 17:
                return setAttachStr(para);
            case 18:
                return getAttachInt(para);
            case 19:
                return setAttachInt(para);
            case 20:
                return setBgColorHexStr(para);
            case 21:
                return setColorHexStr(para);
            case 22:
                return getListIdx(para);
            case 23:
                return setImgAlphaStr(para);
            case 24:
                return setEnable(para);
            case 25:
                return setListIdx(para);
            case 26:
                return setCheckBox(para);
            case 27:
                return getCheckBox(para);
            case 28:
                return setScrollBar(para);
            case 29:
                return getScrollBar(para);
            case 30:
                return setSwitch(para);
            case 31:
                return getSwitch(para);
            case 32:
                return getX(para);
            case 33:
                return getY(para);
            case 34:
                return getW(para);
            case 35:
                return getH(para);
            case 36:
                return setXY(para);
            case 37:
                return setWH(para);
            case 38:
                return loadXmlUI(para);
            case 39:
                return uiExist(para);
            default:
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // inner method
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // implementation
    // -------------------------------------------------------------------------


    public DataType setBgColor(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            float[] color;
            int r = Interpreter.popBackInt(para);
            int g = Interpreter.popBackInt(para);
            int b = Interpreter.popBackInt(para);
            int a = Interpreter.popBackInt(para);
            color = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
            gobj.setBgColor(color);
        }
        return null;
    }

    public DataType setColor(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            float[] color;
            int r = Interpreter.popBackInt(para);
            int g = Interpreter.popBackInt(para);
            int b = Interpreter.popBackInt(para);
            int a = Interpreter.popBackInt(para);
            color = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
            gobj.setColor(color);
        }
        return null;
    }

    public DataType setBgColorHexStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            float[] color;
            String rgbaStr = Interpreter.popBackStr(para);
            try {
                int c = (int) Long.parseLong(rgbaStr, 16);
                color = Nanovg.nvgRGBA((byte) ((c >> 24) & 0xff), (byte) ((c >> 16) & 0xff), (byte) ((c >> 8) & 0xff), (byte) ((c >> 0) & 0xff));
                gobj.setBgColor(color);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public DataType setColorHexStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            float[] color;
            String rgbaStr = Interpreter.popBackStr(para);
            try {
                int c = (int) Long.parseLong(rgbaStr, 16);
                color = Nanovg.nvgRGBA((byte) ((c >> 24) & 0xff), (byte) ((c >> 16) & 0xff), (byte) ((c >> 8) & 0xff), (byte) ((c >> 0) & 0xff));
                gobj.setColor(color);
            } catch (Exception e) {
            }
        }
        return null;
    }


    public DataType setText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String text = Interpreter.popBackStr(para);
        GToolkit.setCompText(form, compont, text);
        return null;
    }

    public DataType getText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String text = GToolkit.getCompText(form, compont);
        return Interpreter.getCachedStr(text == null ? "" : text);
    }

    public DataType setXY(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int x = Interpreter.popBackInt(para);
        int y = Interpreter.popBackInt(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            gobj.setLocation(x, y);
        }

        return null;
    }

    public DataType setWH(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int w = Interpreter.popBackInt(para);
        int h = Interpreter.popBackInt(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            gobj.setSize(w, h);
        }
        return null;
    }


    /**
     * if no parameter , return form x
     * else return componet
     *
     * @param para
     * @return
     */
    public DataType getX(ArrayList<DataType> para) {
        int left = (int) form.getLocationLeft();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(form, compont);
            if (go != null) {
                left = (int) go.getLocationLeft();
            } else {
                left = -1;
            }
        }
        return Interpreter.getCachedInt(left);
    }

    public DataType getY(ArrayList<DataType> para) {
        int top = (int) form.getLocationTop();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(form, compont);
            if (go != null) {
                top = (int) go.getLocationTop();
            } else {
                top = -1;
            }
        }
        return Interpreter.getCachedInt(top);
    }

    public DataType getW(ArrayList<DataType> para) {
        int w = (int) form.getW();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(form, compont);
            if (go != null) {
                w = (int) go.getW();
            } else {
                w = -1;
            }
        }
        return Interpreter.getCachedInt(w);
    }

    public DataType getH(ArrayList<DataType> para) {
        int h = (int) form.getH();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(form, compont);
            if (go != null) {
                h = (int) go.getH();
            } else {
                h = -1;
            }
        }
        return Interpreter.getCachedInt(h);
    }

    public DataType setCmd(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String cmd = Interpreter.popBackStr(para);
        GToolkit.setCompCmd(form, compont, cmd);
        return null;
    }

    public DataType getCmd(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String text = GToolkit.getCompCmd(form, compont);
        if (text == null) {
            text = "";
        }
        return Interpreter.getCachedStr(text);
    }

    public DataType close(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GToolkit.closeFrame(form, compont);
        return null;
    }

    public DataType getCurSlot(ArrayList<DataType> para) {
        Int val = Interpreter.getCachedInt(0);
        String compont = Interpreter.popBackStr(para);
        GObject go = GToolkit.getComponent(form, compont);
        if (go instanceof GViewSlot) {
            val.setVal(((GViewSlot) go).getCurrentSlot());
        }
        return val;
    }

    public DataType showSlot(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject go = GToolkit.getComponent(form, compont);
        int slot = Interpreter.popBackInt(para);
        Int time = Interpreter.popBack(para);
        if (go instanceof GViewSlot) {
            ((GViewSlot) go).moveTo(slot, time == null ? 0 : time.getVal());
        }
        Interpreter.putCachedData(time);
        return null;
    }


    public DataType setImgPath(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String img = Interpreter.popBackStr(para);
        GToolkit.setCompImage(form, compont, img);
        return null;
    }

    public DataType setImg(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        Object img = Interpreter.popBackObject(para);
        if (img instanceof GImage) {
            GToolkit.setCompImage(form, compont, (GImage) img);
        }
        return null;
    }

    public DataType getImg(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GImage img = GToolkit.getCompImage(form, compont);
        return Interpreter.getCachedObj(img);
    }

    public DataType setAttachStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String str1 = Interpreter.popBackStr(para);
        GToolkit.setCompAttachment(form, compont, str1);
        return null;
    }

    public DataType getAttachStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String text = GToolkit.getCompAttachment(form, compont);
        return Interpreter.getCachedStr(text);
    }


    public DataType setAttachInt(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int val = Interpreter.popBackInt(para);
        GToolkit.setCompAttachment(form, compont, Integer.valueOf(val));
        return null;
    }

    public DataType getAttachInt(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        Integer val = GToolkit.getCompAttachment(form, compont);
        return Interpreter.getCachedInt(val == null ? 0 : val.intValue());
    }


    public DataType getListIdx(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        int selectIndex = -1;
        if (gobj != null && gobj instanceof GList) {
            selectIndex = ((GList) gobj).getSelectedIndex();
        }
        return Interpreter.getCachedInt(selectIndex);
    }


    public DataType setListIdx(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int idx = Interpreter.popBackInt(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null && gobj instanceof GList) {
            GList list = (GList) gobj;
            if (idx >= 0 && idx < list.getElements().size()) {
                list.setSelectedIndex(idx);
            }
        }
        return null;
    }

    private DataType setImgAlphaStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            String alphaStr = Interpreter.popBackStr(para);
            try {
                float alpha = Float.parseFloat(alphaStr);
                if (gobj instanceof GImageItem) {
                    ((GImageItem) gobj).setAlpha(alpha);
                }
//                else if (gobj instanceof GListItem) {
//                    ((GListItem) gobj).setAlpha(alpha);
//                } else if (gobj instanceof GMenuItem) {
//                    ((GMenuItem) gobj).setAlpha(alpha);
//                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    private DataType setEnable(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        boolean enable = Interpreter.popBackBool(para);
        GToolkit.setCompEnable(form, compont, enable);
        return null;
    }

    private DataType setCheckBox(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        boolean checked = Interpreter.popBackBool(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null && gobj instanceof GCheckBox) {
            ((GCheckBox) gobj).setChecked(checked);
        }
        return null;
    }

    private DataType getCheckBox(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        boolean checked = false;
        if (gobj != null && gobj instanceof GCheckBox) {
            checked = ((GCheckBox) gobj).isChecked();
        }
        return Interpreter.getCachedBool(checked);
    }


    private DataType setScrollBar(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        Float fv = (Float) Interpreter.popBackObject(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null && gobj instanceof GScrollBar) {
            ((GScrollBar) gobj).setPos(fv);
        }
        return null;
    }

    private DataType getScrollBar(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        Float val;
        if (gobj != null && gobj instanceof GScrollBar) {
            val = ((GScrollBar) gobj).getPos();
        } else {
            val = Float.valueOf(0);
        }
        return Interpreter.getCachedObj(val);
    }

    private DataType setSwitch(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        boolean checked = Interpreter.popBackBool(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null && gobj instanceof GSwitch) {
            ((GSwitch) gobj).setSwitcher(checked);
        }
        return null;
    }

    private DataType getSwitch(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        boolean checked = false;
        if (gobj != null && gobj instanceof GSwitch) {
            checked = ((GSwitch) gobj).getSwitcher();
        }
        return Interpreter.getCachedBool(checked);
    }

    private DataType loadXmlUI(ArrayList<DataType> para) {
        String uipath = Interpreter.popBackStr(para);
        XmlExtAssist xmlExtAssist = null;
        if (!para.isEmpty()) {
            Object sobj = Interpreter.popBackObject(para);
            if (sobj != null && sobj instanceof XmlExtAssist) {
                xmlExtAssist = (XmlExtAssist) sobj;
            }
        }
        XEventHandler eventHandler = null;
        if (!para.isEmpty()) {
            Object sobj = Interpreter.popBackObject(para);
            if (sobj != null && sobj instanceof XEventHandler) {
                eventHandler = (XEventHandler) sobj;
            }
        }

        String xmlStr = GToolkit.readFileFromJarAsString(uipath, "utf-8");
        UITemplate uit = new UITemplate(xmlStr);
        String s = uit.parse();
        //System.out.println(s);
        XObject xobj = XContainer.parseXml(s, xmlExtAssist);
        if (xobj instanceof XContainer) {
            ((XContainer) xobj).build((int) form.getW(), (int) form.getH(), eventHandler);
        }
        GToolkit.showFrame(xobj.getGui());
        return null;
    }

    public DataType uiExist(ArrayList<DataType> para) {
        boolean w = false;
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(form, compont);
            w = (go != null);
        }
        return Interpreter.getCachedBool(w);
    }
}
