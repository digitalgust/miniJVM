/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui.guilib;

import org.mini.gui.*;
import org.mini.gui.gscript.*;
import org.mini.http.MiniHttpClient;
import org.mini.json.JsonParser;
import org.mini.layout.*;
import org.mini.nanovg.Nanovg;

import java.net.URL;
import java.util.ArrayList;


/**
 * xml ui script libary
 *
 * @author Gust
 */
public class GuiScriptLib extends Lib {
    GForm form;


    /**
     *
     */
    public GuiScriptLib(GForm form) {
        this.form = form;

        {
            methodNames.put("flushGui".toLowerCase(), this::flushGui);//  set background color
            methodNames.put("setBgColor".toLowerCase(), this::setBgColor);//  set background color
            methodNames.put("setColor".toLowerCase(), this::setColor);//  set background color
            methodNames.put("setText".toLowerCase(), this::setText);//  set text
            methodNames.put("getText".toLowerCase(), this::getText);//  get text
            methodNames.put("setCmd".toLowerCase(), this::setCmd);//
            methodNames.put("getCmd".toLowerCase(), this::getCmd);//
            methodNames.put("close".toLowerCase(), this::close);//  close frame
            methodNames.put("getCurSlot".toLowerCase(), this::getCurSlot);//
            methodNames.put("showSlot".toLowerCase(), this::showSlot);//
            methodNames.put("getImg".toLowerCase(), this::getImg);//
            methodNames.put("setImg".toLowerCase(), this::setImg);//
            methodNames.put("setImgPath".toLowerCase(), this::setImgPath);//
            methodNames.put("getAttachStr".toLowerCase(), this::getAttachStr);//
            methodNames.put("setAttachStr".toLowerCase(), this::setAttachStr);//
            methodNames.put("getAttachInt".toLowerCase(), this::getAttachInt);//
            methodNames.put("setAttachInt".toLowerCase(), this::setAttachInt);//
            methodNames.put("setBgColorHexStr".toLowerCase(), this::setBgColorHexStr);//  set background color
            methodNames.put("setColorHexStr".toLowerCase(), this::setColorHexStr);//  set color
            methodNames.put("setPreIconColor".toLowerCase(), this::setPreIconColor);//  set color
            methodNames.put("clearPreIconColor".toLowerCase(), this::clearPreIconColor);//  set color
            methodNames.put("getListIdx".toLowerCase(), this::getListIdx);//
            methodNames.put("setImgAlphaStr".toLowerCase(), this::setImgAlphaStr);//
            methodNames.put("setEnable".toLowerCase(), this::setEnable);//
            methodNames.put("getEnable".toLowerCase(), this::getEnable);//
            methodNames.put("setListIdx".toLowerCase(), this::setListIdx);//
            methodNames.put("setCheckBox".toLowerCase(), this::setCheckBox);//
            methodNames.put("getCheckBox".toLowerCase(), this::getCheckBox);//
            methodNames.put("setScrollBar".toLowerCase(), this::setScrollBar);//
            methodNames.put("getScrollBar".toLowerCase(), this::getScrollBar);//
            methodNames.put("setSwitch".toLowerCase(), this::setSwitch);//
            methodNames.put("getSwitch".toLowerCase(), this::getSwitch);//
            methodNames.put("getX".toLowerCase(), this::getX);//
            methodNames.put("getY".toLowerCase(), this::getY);//
            methodNames.put("getW".toLowerCase(), this::getW);//
            methodNames.put("getH".toLowerCase(), this::getH);//
            methodNames.put("setXY".toLowerCase(), this::setXY);//
            methodNames.put("setWH".toLowerCase(), this::setWH);//
            methodNames.put("loadXmlUI".toLowerCase(), this::loadXmlUI);//
            methodNames.put("uiExist".toLowerCase(), this::uiExist);//
            methodNames.put("getListText".toLowerCase(), this::getListText);//
            methodNames.put("showBar".toLowerCase(), this::showBar);//
            methodNames.put("showMsg".toLowerCase(), this::showMsg);//
            methodNames.put("showConfirm".toLowerCase(), this::showConfirm);//
            methodNames.put("insertText".toLowerCase(), this::insertText);//
            methodNames.put("deleteText".toLowerCase(), this::deleteText);//
            methodNames.put("getCaretPos".toLowerCase(), this::getCaretPos);//
            methodNames.put("showTitle".toLowerCase(), this::showTitle);//
            methodNames.put("setBgImg".toLowerCase(), this::setBgImg);//
            methodNames.put("setVisible".toLowerCase(), this::setVisible);//
            methodNames.put("getVisible".toLowerCase(), this::getVisible);//
            methodNames.put("httpGet".toLowerCase(), this::httpGet);//
            methodNames.put("httpGetSync".toLowerCase(), this::httpGetSync);//
            methodNames.put("httpPost".toLowerCase(), this::httpPost);//
            methodNames.put("httpPostSync".toLowerCase(), this::httpPostSync);//

        }
    }

    public Func getFuncByName(String name) {
        if (form == null) { //如果通过解析XML得到form的时候，创建这个lib时，form还没有创建成功，因此进行补充设置
            form = GCallBack.getInstance().getApplication().getForm();
        }
        return super.getFuncByName(name);
    }


    // -------------------------------------------------------------------------
    // inner method
    // -------------------------------------------------------------------------

    /**
     * @param url
     * @param callback like: CONTAINER_NAME.SCRIPT_NAME
     */
    public static void doHttpCallback(GForm form, String callback, String url, int code, String reply) {
        if (callback != null) {
            if (callback.contains(".")) {
                String[] ss = callback.split("\\.");
                GContainer gobj = GToolkit.getComponent(form, ss[0]);
                if (gobj != null) {
                    Interpreter inp = gobj.getInterpreter();
                    inp.callSub(ss[1] + "(\"" + url + "\"," + code + ",\"" + reply + "\")");
                    return;
                }
            }
            System.out.println("httpRequest callback no GContainer specified: " + callback);

        }
    }

    public static void showProgressBar(GForm form, int progress) {
        long vg = GCallBack.getInstance().getNvContext();
        //GToolkit.drawTextLine(vg, 0, 0, "waitting ..." + progress, GToolkit.getStyle().getTextFontSize(), GToolkit.getStyle().getTextFontColor(), Nanovg.NVG_ALIGN_LEFT | Nanovg.NVG_ALIGN_TOP);
        int w = GCallBack.getInstance().getDeviceWidth();
        int h = GCallBack.getInstance().getDeviceHeight();
        //System.out.println("progress:" + progress);
        final String panName = "_INNER_PROGRESS_BAR";
        GObject go = GToolkit.getComponent(form, panName);
        if (go == null) {
            go = new GPanel(form, 0, h - 4, 0, 4) {
                @Override
                public void setSize(float w, float h) {
                    super.setSize(w, h);
                    layer = GObject.LAYER_INNER;
                }
            };
            go.setName(panName);
            go.setBgColor(GColorSelector.BLUE_HALF);
            form.add(go);
        }
        go.setLocation(0, h - go.getH());
        go.setSize(progress * w / 100f, go.getH());
        if (progress == 100) {
            GForm.addCmd(new GCmd(() -> {
                GObject go1 = GToolkit.getComponent(form, panName);
                if (go1 != null) go1.setSize(0, go1.getH());
            }));
        }
    }

    // -------------------------------------------------------------------------
    // implementation
    // -------------------------------------------------------------------------


    public DataType flushGui(ArrayList<DataType> para) {
        GForm.flush();
        return null;
    }

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

    public DataType setPreIconColor(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            float[] color;
            String rgbaStr = Interpreter.popBackStr(para);
            try {
                int c = (int) Long.parseLong(rgbaStr, 16);
                color = Nanovg.nvgRGBA((byte) ((c >> 24) & 0xff), (byte) ((c >> 16) & 0xff), (byte) ((c >> 8) & 0xff), (byte) ((c >> 0) & 0xff));
                gobj.setPreiconColor(color);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public DataType clearPreIconColor(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            gobj.setPreiconColor(GToolkit.getStyle().getTextFontColor());
        }
        return null;
    }


    public DataType setText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        DataType dt = Interpreter.popBack(para);
        String text = dt.getString();
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
        int left = (int) form.getX();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(form, compont);
            if (go != null) {
                left = (int) go.getX();
            } else {
                left = -1;
            }
        }
        return Interpreter.getCachedInt(left);
    }

    public DataType getY(ArrayList<DataType> para) {
        int top = (int) form.getY();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(form, compont);
            if (go != null) {
                top = (int) go.getY();
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


    public DataType getListText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        String selectText = "";
        if (gobj != null && gobj instanceof GList && ((GList) gobj).getSelectedIndex() >= 0) {
            selectText = ((GList) gobj).getSelectedItem().getText();
        }
        return Interpreter.getCachedStr(selectText);
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

    private DataType getEnable(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            boolean en = gobj.isEnable();
            return Interpreter.getCachedBool(en);
        }
        return Interpreter.getCachedBool(false);
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


    public DataType showBar(ArrayList<DataType> para) {
        String msg = Interpreter.popBackStr(para);
        GForm.addMessage(msg);
        return null;
    }

    public DataType showMsg(ArrayList<DataType> para) {
        String msg = Interpreter.popBackStr(para);
        GFrame f = GToolkit.getMsgFrame(form, GLanguage.getString("Message"), msg);
        GToolkit.showFrame(f);
        form.flush();
        return null;
    }

    public DataType showConfirm(ArrayList<DataType> para) {
        String msg = Interpreter.popBackStr(para);
        String callback = Interpreter.popBackStr(para);
        GFrame f = GToolkit.getConfirmFrame(
                form,
                GLanguage.getString("Message"),
                msg,
                GLanguage.getString("Ok"),
                (obj) -> {
                    if (callback != null) {
                        if (callback.contains(".")) {
                            String[] ss = callback.split("\\.");
                            GContainer gobj = GToolkit.getComponent(form, ss[0]);
                            Interpreter inp = gobj.getInterpreter();
                            inp.callSub(ss[1] + "()");
                        } else {
                            System.out.println("showConfirm callback format \"PAN.subname\" ,but : " + callback);
                        }
                    }
                },
                null,
                null);
        GToolkit.showFrame(f);
        form.flush();
        return null;
    }


    private DataType insertText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        int pos = Interpreter.popBackInt(para);
        String str = Interpreter.popBackStr(para);
        if (gobj != null && gobj instanceof GTextObject) {
            ((GTextObject) gobj).insertTextByIndex(pos, str);
        }
        return null;
    }

    private DataType deleteText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        int pos = Interpreter.popBackInt(para);
        int pos1 = Interpreter.popBackInt(para);
        if (gobj != null && gobj instanceof GTextObject) {
            ((GTextObject) gobj).deleteTextRange(pos, pos1);
        }
        return null;
    }

    private DataType getCaretPos(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        int index = 0;
        if (gobj != null && gobj instanceof GTextObject) {
            index = ((GTextObject) gobj).getCaretIndex();
        }
        return Interpreter.getCachedInt(index);
    }

    private DataType showTitle(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null && gobj instanceof GFrame) {
            boolean show = Interpreter.popBackBool(para);
            ((GFrame) gobj).setTitleShow(show);
        }
        return null;
    }

    private DataType setBgImg(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            String imgpath = Interpreter.popBackStr(para);
            gobj.setBgImg(GToolkit.getCachedImageFromJar(imgpath));
            if (!para.isEmpty()) {
                String alphaStr = Interpreter.popBackStr(para);
                float f = Float.parseFloat(alphaStr);
                gobj.setBgImgAlpha(f);
            }
        }
        return null;
    }

    private DataType setVisible(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            boolean show = Interpreter.popBackBool(para);
            gobj.setVisible(show);
        }
        return null;
    }

    private DataType getVisible(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(form, compont);
        if (gobj != null) {
            boolean show = gobj.isVisible();
            return Interpreter.getCachedBool(show);
        }
        return Interpreter.getCachedBool(false);
    }

    private DataType httpRequestImpl(String href, String postData, String callback, boolean async) {

        if (href != null) {
            try {
                URL url = new URL(href);
            } catch (Exception e) {
                doHttpCallback(form, callback, href, -1, "url format error");
                return null;
            }

            MiniHttpClient hc = new MiniHttpClient(href, null, new MiniHttpClient.DownloadCompletedHandle() {
                @Override
                public void onCompleted(MiniHttpClient client, String url, byte[] data) {
                    if (data != null) {
                        try {
                            JsonParser<HttpRequestReply> jp = new JsonParser();
                            HttpRequestReply msg = jp.deserial(new String(data, "UTF-8"), HttpRequestReply.class);
                            if (async) {//异步要交给主线程执行，因为实践中出现死锁，GContainer 的elements 同步比较多，多线程死锁
                                GCmd cmd = new GCmd(
                                        () -> {
                                            doHttpCallback(form, callback, url, msg.getCode(), msg.getReply());
                                        });
                                GForm.addCmd(cmd);
                                GForm.flush();
                            } else {
                                doHttpCallback(form, callback, url, msg.getCode(), msg.getReply());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            if (postData != null) {
                hc.setPostData(postData);
                hc.setHeader("Content-Type", "application/x-www-form-urlencoded");
            }
            if (async) {
                hc.setProgressListener(new MiniHttpClient.ProgressListener() {
                    @Override
                    public void onProgress(MiniHttpClient client, int progress) {
                        showProgressBar(form, progress);
                    }
                });
                hc.start();
            } else {
                hc.run();
            }
        }
        return null;
    }


    public DataType httpGetSync(ArrayList<DataType> para) {
        if (para.size() < 1) {
            System.out.println("call sub error: httpGetSync(url,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, null, callback, false);
    }

    public DataType httpGet(ArrayList<DataType> para) {
        if (para.size() < 1) {
            System.out.println("call sub error: httpGet(url,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, null, callback, true);
    }

    public DataType httpPostSync(ArrayList<DataType> para) {
        if (para.size() < 2) {
            System.out.println("call sub error: httpPostSync(url,postdata,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String postData = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, postData, callback, false);
    }

    public DataType httpPost(ArrayList<DataType> para) {
        if (para.size() < 2) {
            System.out.println("call sub error: httpPost(url,postdata,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String postData = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, postData, callback, true);
    }

}
