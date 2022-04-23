/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.gui.gscript.*;
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
        methodNames.put("setLocation".toLowerCase(), 4);//
        methodNames.put("setSize".toLowerCase(), 5);//
        methodNames.put("getLocation".toLowerCase(), 6);//
        methodNames.put("getSize".toLowerCase(), 7);//
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
    public DataType call(Interpreter inp, ArrayList para, int methodID) {
        switch (methodID) {
            case 0:
                return setBgColor(para);
            case 1:
                return setColor(para);
            case 2:
                return setText(para);
            case 3:
                return getText(para);
            case 4:
                return setLocation(para);
            case 5:
                return setSize(para);
            case 6:
                return getLocation(para);
            case 7:
                return getSize(para);
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


    public DataType setBgColor(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            float[] color;
            int r = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            int g = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            int b = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            int a = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            color = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);
            gobj.setBgColor(color);


        }
        return null;
    }

    public DataType setColor(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            int r = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            int g = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            int b = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            int a = ((Int) Interpreter.vPopBack(para)).getValAsInt();
            float[] color = Nanovg.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a);

            gobj.setColor(color);
        }
        return null;
    }

    public DataType setBgColorHexStr(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            float[] color;
            String rgbaStr = ((Str) Interpreter.vPopBack(para)).getVal();
            try {
                int c = (int) Long.parseLong(rgbaStr, 16);
                color = Nanovg.nvgRGBA((byte) ((c >> 24) & 0xff), (byte) ((c >> 16) & 0xff), (byte) ((c >> 8) & 0xff), (byte) ((c >> 0) & 0xff));
                gobj.setBgColor(color);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public DataType setColorHexStr(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            float[] color;
            String rgbaStr = ((Str) Interpreter.vPopBack(para)).getVal();
            try {
                int c = (int) Long.parseLong(rgbaStr, 16);
                color = Nanovg.nvgRGBA((byte) ((c >> 24) & 0xff), (byte) ((c >> 16) & 0xff), (byte) ((c >> 8) & 0xff), (byte) ((c >> 0) & 0xff));
                gobj.setColor(color);
            } catch (Exception e) {
            }
        }
        return null;
    }


    public DataType setText(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        String text = ((Str) Interpreter.vPopBack(para)).getVal();
        GToolkit.setCompText(compont, text);
        return null;
    }

    public DataType getText(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        String text = GToolkit.getCompText(compont);
        return new Str(text == null ? "" : text);
    }

    public DataType setLocation(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();

        int x = ((Int) Interpreter.vPopBack(para)).getValAsInt();
        int y = ((Int) Interpreter.vPopBack(para)).getValAsInt();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            gobj.setLocation(x, y);
        }

        return null;
    }

    public DataType setSize(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        int w = ((Int) Interpreter.vPopBack(para)).getValAsInt();
        int h = ((Int) Interpreter.vPopBack(para)).getValAsInt();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            gobj.setSize(w, h);
        }
        return null;
    }


    static int[] ARRAY_POS_0 = {0};
    static int[] ARRAY_POS_1 = {1};

    public DataType getLocation(ArrayList para) {
        Array array = new Array(new int[]{2});
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject go = GToolkit.getComponent(compont);
        if (go != null) {
            array.setValue(ARRAY_POS_0, new Int((int) go.getLocationLeft()));
            array.setValue(ARRAY_POS_1, new Int((int) go.getLocationTop()));
        }
        return array;
    }

    public DataType getSize(ArrayList para) {
        Array array = new Array(new int[]{2});
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject go = GToolkit.getComponent(compont);
        if (go != null) {
            array.setValue(ARRAY_POS_0, new Int((int) go.getW()));
            array.setValue(ARRAY_POS_1, new Int((int) go.getH()));
        }
        return array;
    }

    public DataType setCmd(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        String cmd = ((Str) Interpreter.vPopBack(para)).getVal();
        GToolkit.setCompCmd(compont, cmd);
        return null;
    }

    public DataType getCmd(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        String text = GToolkit.getCompCmd(compont);
        if (text == null) {
            text = "";
        }
        return new Str(text);
    }

    public DataType close(ArrayList para) {
        Str p1 = (Str) Interpreter.vPopBack(para);
        String compont = p1 != null ? p1.getVal() : null;
        GToolkit.closeFrame(compont);
        return null;
    }

    public DataType getCurSlot(ArrayList para) {
        Int val = new Int(0);
        Str p1 = (Str) Interpreter.vPopBack(para);
        String compont = p1 != null ? p1.getVal() : null;

        GObject go = GToolkit.getComponent(compont);
        if (go instanceof GViewSlot) {
            val.setVal(((GViewSlot) go).getCurrentSlot());
        }
        return val;
    }

    public DataType showSlot(ArrayList para) {
        Str p1 = (Str) Interpreter.vPopBack(para);
        String compont = p1 != null ? p1.getVal() : null;
        GObject go = GToolkit.getComponent(compont);
        int slot = ((Int) Interpreter.vPopBack(para)).getValAsInt();
        Int time = ((Int) Interpreter.vPopBack(para));
        if (go instanceof GViewSlot) {
            ((GViewSlot) go).moveTo(slot, time == null ? 200 : time.getVal());
        }
        return null;
    }


    public DataType setImgPath(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        String img = ((Str) Interpreter.vPopBack(para)).getVal();
        GToolkit.setCompImage(compont, img);
        return null;
    }

    public DataType setImg(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        Object img = ((Obj) Interpreter.vPopBack(para)).getVal();
        if (img instanceof GImage) {
            GToolkit.setCompImage(compont, (GImage) img);
        }
        return null;
    }

    public DataType getImg(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GImage img = GToolkit.getCompImage(compont);
        return new Obj(img);
    }

    public DataType setAttachStr(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        String str = ((Str) Interpreter.vPopBack(para)).getVal();
        GToolkit.setCompAttachment(compont, str);
        return null;
    }

    public DataType getAttachStr(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        String text = GToolkit.getCompAttachment(compont);
        return new Str(text);
    }


    public DataType setAttachInt(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        int val = ((Int) Interpreter.vPopBack(para)).getValAsInt();
        GToolkit.setCompAttachment(compont, Integer.valueOf(val));
        return null;
    }

    public DataType getAttachInt(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        Integer val = GToolkit.getCompAttachment(compont);
        return new Int(val == null ? 0 : val.intValue());
    }


    public DataType getListIdx(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject gobj = GToolkit.getComponent(compont);
        int selectIndex = -1;
        if (gobj != null && gobj instanceof GList) {
            selectIndex = ((GList) gobj).getSelectedIndex();
        }
        return new Int(selectIndex);
    }

    private DataType setImgAlphaStr(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        GObject gobj = GToolkit.getComponent(compont);
        if (gobj != null) {
            String alphaStr = ((Str) Interpreter.vPopBack(para)).getVal();
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

    private DataType setEnable(ArrayList para) {
        String compont = ((Str) Interpreter.vPopBack(para)).getVal();
        boolean enable = ((Bool) Interpreter.vPopBack(para)).getVal();
        GToolkit.setCompEnable(compont, enable);
        return null;
    }

}
