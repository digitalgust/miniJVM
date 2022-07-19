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

    }

    ;

    /**
     *
     */
    public GuiScriptLib() {

    }

    /**
     * @param inp
     * @param para
     * @param methodID
     * @return
     */
    public DataType call(Interpreter inp, ArrayList<DataType> para, int methodID) {
        switch (methodID) {
            case 0:
                return setBgColor(inp, para);
            case 1:
                return setColor(inp, para);
            case 2:
                return setText(inp, para);
            case 3:
                return getText(inp, para);
            case 8:
                return setCmd(inp, para);
            case 9:
                return getCmd(inp, para);
            case 10:
                return close(inp, para);
            case 11:
                return getCurSlot(inp, para);
            case 12:
                return showSlot(inp, para);
            case 13:
                return getImg(inp, para);
            case 14:
                return setImg(inp, para);
            case 15:
                return setImgPath(inp, para);
            case 16:
                return getAttachStr(inp, para);
            case 17:
                return setAttachStr(inp, para);
            case 18:
                return getAttachInt(inp, para);
            case 19:
                return setAttachInt(inp, para);
            case 20:
                return setBgColorHexStr(inp, para);
            case 21:
                return setColorHexStr(inp, para);
            case 22:
                return getListIdx(inp, para);
            case 23:
                return setImgAlphaStr(inp, para);
            case 24:
                return setEnable(inp, para);
            case 25:
                return setListIdx(inp, para);
            case 26:
                return setCheckBox(inp, para);
            case 27:
                return getCheckBox(inp, para);
            case 28:
                return setScrollBar(inp, para);
            case 29:
                return getScrollBar(inp, para);
            case 30:
                return setSwitch(inp, para);
            case 31:
                return getSwitch(inp, para);
            case 32:
                return getX(inp, para);
            case 33:
                return getY(inp, para);
            case 34:
                return getW(inp, para);
            case 35:
                return getH(inp, para);
            case 36:
                return setXY(inp, para);
            case 37:
                return setWH(inp, para);
            case 38:
                return loadXmlUI(inp, para);
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


    public DataType setBgColor(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            float[] color;
            Int ri = Interpreter.vPopBack(para);
            Int gi = Interpreter.vPopBack(para);
            Int bi = Interpreter.vPopBack(para);
            Int ai = Interpreter.vPopBack(para);
            int r = ri.getValAsInt();
            int g = gi.getValAsInt();
            int b = bi.getValAsInt();
            int a = ai.getValAsInt();
            inp.putCachedInt(ri);
            inp.putCachedInt(gi);
            inp.putCachedInt(bi);
            inp.putCachedInt(ai);
            color = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
            gobj.setBgColor(color);
        }
        return null;
    }

    public DataType setColor(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            float[] color;
            Int ri = Interpreter.vPopBack(para);
            Int gi = Interpreter.vPopBack(para);
            Int bi = Interpreter.vPopBack(para);
            Int ai = Interpreter.vPopBack(para);
            int r = ri.getValAsInt();
            int g = gi.getValAsInt();
            int b = bi.getValAsInt();
            int a = ai.getValAsInt();
            inp.putCachedInt(ri);
            inp.putCachedInt(gi);
            inp.putCachedInt(bi);
            inp.putCachedInt(ai);
            color = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
            gobj.setColor(color);
        }
        return null;
    }

    public DataType setBgColorHexStr(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            float[] color;
            Str cstr = Interpreter.vPopBack(para);
            String rgbaStr = cstr.getVal();
            inp.putCachedStr(cstr);
            try {
                int c = (int) Long.parseLong(rgbaStr, 16);
                color = Nanovg.nvgRGBA((byte) ((c >> 24) & 0xff), (byte) ((c >> 16) & 0xff), (byte) ((c >> 8) & 0xff), (byte) ((c >> 0) & 0xff));
                gobj.setBgColor(color);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public DataType setColorHexStr(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            float[] color;
            Str cstr = Interpreter.vPopBack(para);
            String rgbaStr = cstr.getVal();
            inp.putCachedStr(cstr);
            try {
                int c = (int) Long.parseLong(rgbaStr, 16);
                color = Nanovg.nvgRGBA((byte) ((c >> 24) & 0xff), (byte) ((c >> 16) & 0xff), (byte) ((c >> 8) & 0xff), (byte) ((c >> 0) & 0xff));
                gobj.setColor(color);
            } catch (Exception e) {
            }
        }
        return null;
    }


    public DataType setText(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Str tstr = Interpreter.vPopBack(para);
        String text = tstr.getVal();
        inp.putCachedStr(tstr);
        GToolkit.setCompText(compont, text);
        return null;
    }

    public DataType getText(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        String text = GToolkit.getCompText(compont);
        return inp.getCachedStr(text == null ? "" : text);
    }

    public DataType setXY(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);

        Int ix = Interpreter.vPopBack(para);
        Int iy = Interpreter.vPopBack(para);
        int x = ix.getValAsInt();
        int y = iy.getValAsInt();
        inp.putCachedInt(ix);
        inp.putCachedInt(iy);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            gobj.setLocation(x, y);
        }

        return null;
    }

    public DataType setWH(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Int iw = Interpreter.vPopBack(para);
        Int ih = Interpreter.vPopBack(para);
        int w = iw.getValAsInt();
        int h = ih.getValAsInt();
        inp.putCachedInt(iw);
        inp.putCachedInt(ih);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            gobj.setSize(w, h);
        }
        return null;
    }


    /**
     * if no parameter , return form x
     * else return componet
     *
     * @param inp
     * @param para
     * @return
     */
    public DataType getX(Interpreter inp, ArrayList<DataType> para) {
        int left = (int) GCallBack.getInstance().getForm().getLocationLeft();
        if (!para.isEmpty()) {
            Str str = Interpreter.vPopBack(para);
            String compont = str.getVal();
            inp.putCachedStr(str);
            GObject go = GToolkit.getComponent(compont);
            if (go != null) {
                left = (int) go.getLocationLeft();
            } else {
                left = -1;
            }
        }
        return inp.getCachedInt(left);
    }

    public DataType getY(Interpreter inp, ArrayList<DataType> para) {
        int top = (int) GCallBack.getInstance().getForm().getLocationTop();
        if (!para.isEmpty()) {
            Str str = Interpreter.vPopBack(para);
            String compont = str.getVal();
            inp.putCachedStr(str);
            GObject go = GToolkit.getComponent(compont);
            if (go != null) {
                top = (int) go.getLocationTop();
            } else {
                top = -1;
            }
        }
        return inp.getCachedInt(top);
    }

    public DataType getW(Interpreter inp, ArrayList<DataType> para) {
        int w = (int) GCallBack.getInstance().getForm().getW();
        if (!para.isEmpty()) {
            Str str = Interpreter.vPopBack(para);
            String compont = str.getVal();
            inp.putCachedStr(str);
            GObject go = GToolkit.getComponent(compont);
            if (go != null) {
                w = (int) go.getW();
            } else {
                w = -1;
            }
        }
        return inp.getCachedInt(w);
    }

    public DataType getH(Interpreter inp, ArrayList<DataType> para) {
        int h = (int) GCallBack.getInstance().getForm().getH();
        if (!para.isEmpty()) {
            Str str = Interpreter.vPopBack(para);
            String compont = str.getVal();
            inp.putCachedStr(str);
            GObject go = GToolkit.getComponent(compont);
            if (go != null) {
                h = (int) go.getH();
            } else {
                h = -1;
            }
        }
        return inp.getCachedInt(h);
    }

    public DataType setCmd(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Str cmdstr = Interpreter.vPopBack(para);
        String cmd = cmdstr.getVal();
        inp.putCachedStr(cmdstr);
        GToolkit.setCompCmd(compont, cmd);
        return null;
    }

    public DataType getCmd(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        String text = GToolkit.getCompCmd(compont);
        if (text == null) {
            text = "";
        }
        return inp.getCachedStr(text);
    }

    public DataType close(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str != null ? str.getVal() : null;
        inp.putCachedStr(str);
        GToolkit.closeFrame(compont);
        return null;
    }

    public DataType getCurSlot(Interpreter inp, ArrayList<DataType> para) {
        Int val = inp.getCachedInt(0);
        Str str = Interpreter.vPopBack(para);
        String compont = str != null ? str.getVal() : null;
        inp.putCachedStr(str);
        GObject go = GToolkit.getComponent(compont);
        if (go instanceof GViewSlot) {
            val.setVal(((GViewSlot) go).getCurrentSlot());
        }
        return val;
    }

    public DataType showSlot(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str != null ? str.getVal() : null;
        inp.putCachedStr(str);
        GObject go = GToolkit.getComponent(compont);
        Int islot = Interpreter.vPopBack(para);
        int slot = islot.getValAsInt();
        Int time = Interpreter.vPopBack(para);
        if (go instanceof GViewSlot) {
            ((GViewSlot) go).moveTo(slot, time == null ? 200 : time.getVal());
        }
        inp.putCachedInt(islot);
        inp.putCachedInt(time);
        return null;
    }


    public DataType setImgPath(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Str simg = Interpreter.vPopBack(para);
        String img = simg.getVal();
        GToolkit.setCompImage(compont, img);
        inp.putCachedStr(simg);
        return null;
    }

    public DataType setImg(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Object img = ((Obj) Interpreter.vPopBack(para)).getVal();
        if (img instanceof GImage) {
            GToolkit.setCompImage(compont, (GImage) img);
        }
        return null;
    }

    public DataType getImg(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GImage img = GToolkit.getCompImage(compont);
        return new Obj(img);
    }

    public DataType setAttachStr(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Str astr = Interpreter.vPopBack(para);
        String str1 = astr.getVal();
        GToolkit.setCompAttachment(compont, str1);
        inp.putCachedStr(astr);
        return null;
    }

    public DataType getAttachStr(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        String text = GToolkit.getCompAttachment(compont);
        return inp.getCachedStr(text);
    }


    public DataType setAttachInt(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Int ai = Interpreter.vPopBack(para);
        int val = ai.getValAsInt();
        GToolkit.setCompAttachment(compont, Integer.valueOf(val));
        inp.putCachedInt(ai);
        return null;
    }

    public DataType getAttachInt(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Integer val = GToolkit.getCompAttachment(compont);
        return inp.getCachedInt(val == null ? 0 : val.intValue());
    }


    public DataType getListIdx(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        int selectIndex = -1;
        if (gobj != null && gobj instanceof GList) {
            selectIndex = ((GList) gobj).getSelectedIndex();
        }
        return inp.getCachedInt(selectIndex);
    }


    public DataType setListIdx(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Int idxInt = Interpreter.vPopBack(para);
        int idx = idxInt.getValAsInt();
        inp.putCachedInt(idxInt);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null && gobj instanceof GList) {
            GList list = (GList) gobj;
            if (idx >= 0 && idx < list.getElements().size()) {
                list.setSelectedIndex(idx);
            }
        }
        return null;
    }

    private DataType setImgAlphaStr(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            Str astr = Interpreter.vPopBack(para);
            String alphaStr = astr.getVal();
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
            inp.putCachedStr(astr);
        }
        return null;
    }

    private DataType setEnable(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Bool ebool = Interpreter.vPopBack(para);
        boolean enable = ebool.getVal();
        GToolkit.setCompEnable(compont, enable);
        inp.putCachedBool(ebool);
        return null;
    }

    private DataType setCheckBox(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Bool ebool = Interpreter.vPopBack(para);
        boolean checked = ebool.getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null && gobj instanceof GCheckBox) {
            ((GCheckBox) gobj).setChecked(checked);
        }
        inp.putCachedBool(ebool);
        return null;
    }

    private DataType getCheckBox(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        boolean checked = false;
        if (gobj != null && gobj instanceof GCheckBox) {
            checked = ((GCheckBox) gobj).isChecked();
        }
        return inp.getCachedBool(checked);
    }


    private DataType setScrollBar(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Obj val = Interpreter.vPopBack(para);
        float fv = (Float) val.getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null && gobj instanceof GScrollBar) {
            ((GScrollBar) gobj).setPos(fv);
        }
        inp.putCachedDataType(val);
        return null;
    }

    private DataType getScrollBar(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        Float val;
        if (gobj != null && gobj instanceof GScrollBar) {
            val = ((GScrollBar) gobj).getPos();
        } else {
            val = Float.valueOf(0);
        }
        return new Obj(val);
    }

    private DataType setSwitch(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        Bool ebool = Interpreter.vPopBack(para);
        boolean checked = ebool.getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null && gobj instanceof GSwitch) {
            ((GSwitch) gobj).setSwitcher(checked);
        }
        inp.putCachedBool(ebool);
        return null;
    }

    private DataType getSwitch(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String compont = str.getVal();
        inp.putCachedStr(str);
        GObject gobj = GToolkit.getComponent(compont);
        boolean checked = false;
        if (gobj != null && gobj instanceof GSwitch) {
            checked = ((GSwitch) gobj).getSwitcher();
        }
        return inp.getCachedBool(checked);
    }

    private DataType loadXmlUI(Interpreter inp, ArrayList<DataType> para) {
        Str str = Interpreter.vPopBack(para);
        String uipath = str.getVal();
        inp.putCachedStr(str);
        XmlExtAssist xmlExtAssist = null;
        if (!para.isEmpty()) {
            Obj sobj = inp.vPopBack(para);
            if (sobj.getVal() != null && sobj.getVal() instanceof XmlExtAssist) {
                xmlExtAssist = (XmlExtAssist) sobj.getVal();
            }
        }
        XEventHandler eventHandler = null;
        if (!para.isEmpty()) {
            Obj sobj = inp.vPopBack(para);
            if (sobj.getVal() != null && sobj.getVal() instanceof XEventHandler) {
                eventHandler = (XEventHandler) sobj.getVal();
            }
        }

        String xmlStr = GToolkit.readFileFromJarAsString(uipath, "utf-8");
        UITemplate uit = new UITemplate(xmlStr);
        String s = uit.parse();
        //System.out.println(s);
        XObject xobj = XContainer.parseXml(s, xmlExtAssist);
        if (xobj instanceof XContainer) {
            ((XContainer) xobj).build(GCallBack.getInstance().getDeviceWidth(), GCallBack.getInstance().getDeviceHeight(), eventHandler);
        }
        GToolkit.showFrame(xobj.getGui());
        return null;
    }

}
