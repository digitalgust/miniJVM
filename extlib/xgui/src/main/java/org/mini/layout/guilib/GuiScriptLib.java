/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.layout.guilib;

import org.mini.apploader.AppLoader;
import org.mini.apploader.AppManager;
import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GCmd;
import org.mini.gui.callback.GDesktop;
import org.mini.gui.gscript.*;
import org.mini.http.MiniHttpClient;
import org.mini.json.JsonParser;
import org.mini.layout.*;
import org.mini.layout.loader.UITemplate;
import org.mini.layout.loader.XmlExtAssist;
import org.mini.nanovg.Nanovg;
import org.mini.util.SysLog;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;


/**
 * xml ui script libary
 *
 * @author Gust
 */
public class GuiScriptLib extends Lib {
    FormHolder formHolder;

    static float[] inset = new float[4];//top,right,bottom,left

    /**
     *
     */
    public GuiScriptLib(FormHolder formHolder) {
        this.formHolder = formHolder;

        {
            methodNames.put("flushGui".toLowerCase(), this::flushGui);//  set background color
            methodNames.put("setBgColor".toLowerCase(), this::setBgColor);//  set background color
            methodNames.put("setColor".toLowerCase(), this::setColor);//  set background color
            methodNames.put("getDefaultColor".toLowerCase(), this::getDefaultTextColorHexStr);//  set background color
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
            methodNames.put("getBgImg".toLowerCase(), this::getBgImg);//
            methodNames.put("setBgImgPath".toLowerCase(), this::setBgImgPath);//
            methodNames.put("getAttachStr".toLowerCase(), this::getAttachStr);//
            methodNames.put("setAttachStr".toLowerCase(), this::setAttachStr);//
            methodNames.put("getAttachInt".toLowerCase(), this::getAttachInt);//
            methodNames.put("setAttachInt".toLowerCase(), this::setAttachInt);//
            methodNames.put("setBgColorHexStr".toLowerCase(), this::setBgColorHexStr);//  set background color
            methodNames.put("setColorHexStr".toLowerCase(), this::setColorHexStr);//  set color
            methodNames.put("setPreIconColor".toLowerCase(), this::setPreIconColor);//  set color
            methodNames.put("clearPreIconColor".toLowerCase(), this::clearPreIconColor);//  set color
            methodNames.put("setImgAlphaStr".toLowerCase(), this::setImgAlphaStr);//
            methodNames.put("setEnable".toLowerCase(), this::setEnable);//
            methodNames.put("getEnable".toLowerCase(), this::getEnable);//
            methodNames.put("getListIdx".toLowerCase(), this::getListIdx);//
            methodNames.put("getListIndex".toLowerCase(), this::getListIdx);//
            methodNames.put("setListIdx".toLowerCase(), this::setListIdx);//
            methodNames.put("setListIndex".toLowerCase(), this::setListIdx);//
            methodNames.put("setCheckBox".toLowerCase(), this::setCheckBox);//
            methodNames.put("getCheckBox".toLowerCase(), this::getCheckBox);//
            methodNames.put("setScrollBar".toLowerCase(), this::setScrollBar);//
            methodNames.put("getScrollBar".toLowerCase(), this::getScrollBar);//
            methodNames.put("setMenuMarkIndex".toLowerCase(), this::setMenuMarkIndex);//
            methodNames.put("getMenuMarkIndex".toLowerCase(), this::getMenuMarkIndex);//
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
            methodNames.put("showMsg".toLowerCase(), this::showMsg);//参数：内容，失去焦点关闭，回调函数，回调参数
            methodNames.put("showConfirm".toLowerCase(), this::showConfirm);//
            methodNames.put("insertText".toLowerCase(), this::insertText);//
            methodNames.put("deleteText".toLowerCase(), this::deleteText);//
            methodNames.put("getCaretPos".toLowerCase(), this::getCaretPos);//
            methodNames.put("showTitle".toLowerCase(), this::showTitle);//
            methodNames.put("setBgImg".toLowerCase(), this::setBgImg);//
            methodNames.put("setVisible".toLowerCase(), this::setVisible);//
            methodNames.put("getVisible".toLowerCase(), this::getVisible);//
            methodNames.put("httpGet".toLowerCase(), this::httpGet);//
            methodNames.put("httpGetAsync".toLowerCase(), this::httpGet);//
            methodNames.put("httpGetSync".toLowerCase(), this::httpGetSync);//
            methodNames.put("httpPost".toLowerCase(), this::httpPost);//
            methodNames.put("httpPostAsync".toLowerCase(), this::httpPost);//
            methodNames.put("httpPostSync".toLowerCase(), this::httpPostSync);//
            methodNames.put("urlGetAsString".toLowerCase(), this::urlGetAsString);//
            methodNames.put("setClipboard".toLowerCase(), this::setClipboard);//
            methodNames.put("getClipboard".toLowerCase(), this::getClipboard);//
            methodNames.put("getVersion".toLowerCase(), this::getVersion);//
            methodNames.put("compareVersion".toLowerCase(), this::compareVersion);//

        }
    }

    public Func getFuncByName(String name) {
        return super.getFuncByName(name);
    }


    // -------------------------------------------------------------------------
    // inner method
    // -------------------------------------------------------------------------
    public static void doCallback(GForm form, String callback, String para) {
        if (callback != null) {
            if (callback.contains(".")) {
                String[] ss = callback.split("\\.");
                GContainer gobj = GToolkit.getComponent(form, ss[0]);
                if (gobj == null) {
                    gobj = GToolkit.getComponent(GCallBack.getInstance().getApplication().getForm(), ss[0]);
                }
                if (gobj != null) {
                    Interpreter inp = gobj.getInterpreter();
                    inp.callSub(ss[1] + "(" + para + ")");
                    return;
                }
            }
            //System.out.println("[WARN]httpRequest callback no GContainer specified: " + callback);

        }
    }

    /**
     * @param url
     * @param callback like: CONTAINER_NAME.SCRIPT_NAME
     */
    public static void doHttpCallback(GForm form, String callback, String url, int code, String reply) {
        if (callback != null) {
            if (callback.contains(".")) {
                String[] ss = callback.split("\\.");
                GContainer gobj = GToolkit.getComponent(form, ss[0]);
                if (gobj == null) {
                    gobj = GToolkit.getComponent(GCallBack.getInstance().getApplication().getForm(), ss[0]);
                }
                if (gobj != null) {
                    Interpreter inp = gobj.getInterpreter();
                    inp.callSub(ss[1] + "(\"" + url + "\"," + code + ",\"" + reply + "\")");
                    return;
                }
            }
            //System.out.println("[WARN]httpRequest callback no GContainer specified: " + callback);

        }
    }

    public static void showProgressBar(GForm form, int progress) {
        GForm.addCmd(new GCmd(() -> {
            int w = GCallBack.getInstance().getDeviceWidth();
            GCallBack.getInstance().getInsets(inset);
            final String panName = "_INNER_PROGRESS_BAR";
            GObject go = GToolkit.getComponent(form, panName);
            if (go == null) {
                go = new GPanel(form, 0, 0, 0, 4) {
                    @Override
                    public void setSize(float w, float h) {
                        super.setSize(w, h);
                        layer = GObject.LAYER_INNER;
                    }
                };
                go.setName(panName);
                go.setBgColor(GColorSelector.GREEN_HALF);
                form.add(go);
            }
            go.setLocation(0, inset[0]);
            go.setSize(progress * w / 100f, go.getH());
            AppManager.getInstance().getFloatButton().setDrawMarkSecond(60);
            if (progress >= 100) {
                GObject go1 = GToolkit.getComponent(form, panName);
                if (go1 != null) go1.setSize(0, go1.getH());
                AppManager.getInstance().getFloatButton().setDrawMarkSecond(0);
            }
        }));
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
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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

    public DataType getDefaultTextColorHexStr(ArrayList<DataType> para) {
        float[] fcolor = GToolkit.getStyle().getTextFontColor();
        int c = (((int) (fcolor[0] * 0xff)) << 24)
                | (((int) (fcolor[1] * 0xff)) << 16)
                | (((int) (fcolor[2] * 0xff)) << 8)
                | (((int) (fcolor[3] * 0xff)));
        String hex = Integer.toHexString(c);
        return Interpreter.getCachedStr(hex);
    }

    public DataType setBgColorHexStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj != null) {
            gobj.setPreiconColor(GToolkit.getStyle().getTextFontColor());
        }
        return null;
    }


    public DataType setText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        DataType dt = Interpreter.popBack(para);
        String text = dt.getString();
        GToolkit.setCompText(formHolder.getForm(), compont, text);
        return null;
    }

    public DataType getText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String text = GToolkit.getCompText(formHolder.getForm(), compont);
        return Interpreter.getCachedStr(text == null ? "" : text);
    }

    public DataType setXY(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int x = Interpreter.popBackInt(para);
        int y = Interpreter.popBackInt(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj != null) {
            gobj.setLocation(x, y);
        }

        return null;
    }

    public DataType setWH(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int w = Interpreter.popBackInt(para);
        int h = Interpreter.popBackInt(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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
        int left = (int) formHolder.getForm().getX();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(formHolder.getForm(), compont);
            if (go != null) {
                left = (int) go.getX();
            } else {
                left = -1;
            }
        }
        return Interpreter.getCachedInt(left);
    }

    public DataType getY(ArrayList<DataType> para) {
        int top = (int) formHolder.getForm().getY();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(formHolder.getForm(), compont);
            if (go != null) {
                top = (int) go.getY();
            } else {
                top = -1;
            }
        }
        return Interpreter.getCachedInt(top);
    }

    public DataType getW(ArrayList<DataType> para) {
        int w = (int) formHolder.getForm().getW();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(formHolder.getForm(), compont);
            if (go != null) {
                w = (int) go.getW();
            } else {
                w = -1;
            }
        }
        return Interpreter.getCachedInt(w);
    }

    public DataType getH(ArrayList<DataType> para) {
        int h = (int) formHolder.getForm().getH();
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(formHolder.getForm(), compont);
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
        GToolkit.setCompCmd(formHolder.getForm(), compont, cmd);
        return null;
    }

    public DataType getCmd(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String text = GToolkit.getCompCmd(formHolder.getForm(), compont);
        if (text == null) {
            text = "";
        }
        return Interpreter.getCachedStr(text);
    }

    public DataType close(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GToolkit.closeFrame(formHolder.getForm(), compont);
        return null;
    }

    public DataType getCurSlot(ArrayList<DataType> para) {
        Int val = Interpreter.getCachedInt(0);
        String compont = Interpreter.popBackStr(para);
        GObject go = GToolkit.getComponent(formHolder.getForm(), compont);
        if (go instanceof GViewSlot) {
            val.setVal(((GViewSlot) go).getCurrentSlot());
        }
        return val;
    }

    public DataType showSlot(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject go = GToolkit.getComponent(formHolder.getForm(), compont);
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
        GToolkit.setCompImage(formHolder.getForm(), compont, img);
        return null;
    }


    public DataType getBgImg(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        GImage img = null;
        if (gobj != null) {
            img = gobj.getBgImg();
        }
        return Interpreter.getCachedObj(img);
    }


    public DataType setBgImgPath(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String imgPath = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        GImage img = GToolkit.getCachedImageFromJar(imgPath);
        if (gobj != null && img != null) {
            gobj.setBgImg(img);
        }
        return null;
    }

    public DataType setImg(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        Object img = Interpreter.popBackObject(para);
        if (img instanceof GImage) {
            GToolkit.setCompImage(formHolder.getForm(), compont, (GImage) img);
        }
        return null;
    }

    public DataType getImg(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GImage img = GToolkit.getCompImage(formHolder.getForm(), compont);
        return Interpreter.getCachedObj(img);
    }

    public DataType setAttachStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String str1 = Interpreter.popBackStr(para);
        GToolkit.setCompAttachment(formHolder.getForm(), compont, str1);
        return null;
    }

    public DataType getAttachStr(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        String text = GToolkit.getCompAttachment(formHolder.getForm(), compont);
        return Interpreter.getCachedStr(text);
    }


    public DataType setAttachInt(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int val = Interpreter.popBackInt(para);
        GToolkit.setCompAttachment(formHolder.getForm(), compont, Integer.valueOf(val));
        return null;
    }

    public DataType getAttachInt(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        Integer val = GToolkit.getCompAttachment(formHolder.getForm(), compont);
        return Interpreter.getCachedInt(val == null ? 0 : val.intValue());
    }


    public DataType getListIdx(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        int selectIndex = -1;
        if (gobj != null && gobj instanceof GList) {
            selectIndex = ((GList) gobj).getSelectedIndex();
        }
        return Interpreter.getCachedInt(selectIndex);
    }


    public DataType getListText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        String selectText = "";
        if (gobj != null && gobj instanceof GList && ((GList) gobj).getSelectedIndex() >= 0) {
            selectText = ((GList) gobj).getSelectedItem().getText();
        }
        return Interpreter.getCachedStr(selectText);
    }


    public DataType setListIdx(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        DataType idxD = Interpreter.popBack(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        int idx = -1;
        if (idxD instanceof Int) {
            idx = ((Int) idxD).getValAsInt();
        } else if (idxD instanceof Str) {
            try {
                idx = Integer.parseInt(((Str) idxD).getVal());
            } catch (Exception e) {
            }
        }
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
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj != null) {
            String alphaStr = Interpreter.popBackStr(para);
            try {
                float alpha = Float.parseFloat(alphaStr);
                if (gobj instanceof GImageItem) {
                    ((GImageItem) gobj).setAlpha(alpha);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    private DataType setEnable(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        boolean enable = Interpreter.popBackBool(para);
        GToolkit.setCompEnable(formHolder.getForm(), compont, enable);
        return null;
    }

    private DataType getEnable(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj != null) {
            boolean en = gobj.isEnable();
            return Interpreter.getCachedBool(en);
        }
        return Interpreter.getCachedBool(false);
    }

    private DataType setCheckBox(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        boolean checked = Interpreter.popBackBool(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj != null && gobj instanceof GCheckBox) {
            ((GCheckBox) gobj).setChecked(checked);
        }
        return null;
    }

    private DataType getCheckBox(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        boolean checked = false;
        if (gobj != null && gobj instanceof GCheckBox) {
            checked = ((GCheckBox) gobj).isChecked();
        }
        return Interpreter.getCachedBool(checked);
    }


    private DataType setScrollBar(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int fv = Interpreter.popBackInt(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj instanceof GScrollBar) {
            ((GScrollBar) gobj).setPos(fv / 100f);
        }
        return null;
    }

    private DataType getScrollBar(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        float fv;
        if (gobj instanceof GScrollBar) {
            fv = ((GScrollBar) gobj).getPos();
        } else {
            fv = 0f;
        }
        int v = (int) (fv * 100);
        return Interpreter.getCachedInt(v);
    }


    private DataType setMenuMarkIndex(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        int fv = Interpreter.popBackInt(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj instanceof GMenu) {
            ((GMenu) gobj).setMarkIndex(fv);
        }
        return null;
    }

    private DataType getMenuMarkIndex(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        int fv;
        if (gobj instanceof GMenu) {
            fv = ((GMenu) gobj).getMarkIndex();
        } else {
            fv = -1;
        }
        return Interpreter.getCachedInt(fv);
    }

    private DataType setSwitch(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        boolean checked = Interpreter.popBackBool(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj instanceof GSwitch) {
            ((GSwitch) gobj).setSwitcher(checked);
        }
        return null;
    }

    private DataType getSwitch(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        boolean checked = false;
        if (gobj instanceof GSwitch) {
            checked = ((GSwitch) gobj).getSwitcher();
        }
        return Interpreter.getCachedBool(checked);
    }

    private DataType loadXmlUI(ArrayList<DataType> para) {
        String uipath = Interpreter.popBackStr(para);
        XmlExtAssist xmlExtAssist = null;
        if (!para.isEmpty()) {
            Object sobj = Interpreter.popBackObject(para);
            if (sobj instanceof XmlExtAssist) {
                xmlExtAssist = (XmlExtAssist) sobj;
            }
        }
        XEventHandler eventHandler = null;
        if (!para.isEmpty()) {
            Object sobj = Interpreter.popBackObject(para);
            if (sobj instanceof XEventHandler) {
                eventHandler = (XEventHandler) sobj;
            }
        }

        String xmlStr = GToolkit.readFileFromJarAsString(uipath, "utf-8");
        UITemplate uit = new UITemplate(xmlStr);
        String s = uit.parse();
        //System.out.println(s);
        XObject xobj = XContainer.parseXml(s, xmlExtAssist);
        if (xobj instanceof XContainer) {
            ((XContainer) xobj).build((int) formHolder.getForm().getW(), (int) formHolder.getForm().getH(), eventHandler);
        }
        GToolkit.showFrame(xobj.getGui());
        return null;
    }

    public DataType uiExist(ArrayList<DataType> para) {
        boolean w = false;
        if (!para.isEmpty()) {
            String compont = Interpreter.popBackStr(para);
            GObject go = GToolkit.getComponent(formHolder.getForm(), compont);
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
        boolean focusSensitive = para.isEmpty() ? false : Interpreter.popBackBool(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        String callbackPara = para.isEmpty() ? "" : Interpreter.popBackStr(para);

        GFrame f = GToolkit.getMsgFrame(formHolder.getForm(), AppManager.getInstance().getString("Message"), msg);
        if (!focusSensitive) {
            f.setFocusListener(null);
        }
        if (callback != null && !callback.isEmpty()) {
            GButton bt = GToolkit.getComponent(f, GToolkit.NAME_MSGFRAME_OK);
            bt.setActionListener((obj) -> {
                doCallback(formHolder.getForm(), callback, callbackPara);
            });
        }
        GToolkit.showFrame(f);
        formHolder.getForm().flush();
        return null;
    }

    public DataType showConfirm(ArrayList<DataType> para) {
        String msg = Interpreter.popBackStr(para);
        String callback = Interpreter.popBackStr(para);
        GFrame f = GToolkit.getConfirmFrame(
                formHolder.getForm(),
                AppManager.getInstance().getString("Message"),
                msg,
                AppManager.getInstance().getString("Ok"),
                (obj) -> {
                    if (callback != null) {
                        if (callback.contains(".")) {
//                            String[] ss = callback.split("\\.");
//                            GContainer gobj = GToolkit.getComponent(formHolder.getForm(), ss[0]);
//                            Interpreter inp = gobj.getInterpreter();
//                            inp.callSub(ss[1] + "(1)");
                            doCallback(formHolder.getForm(), callback, "1");
                        } else {
                            SysLog.info("showConfirm callback format \"PAN.subname\" ,but : " + callback);
                        }
                    }
                    obj.getFrame().close();
                },
                null,
                (obj) -> {
                    if (callback != null) {
                        if (callback.contains(".")) {
//                            String[] ss = callback.split("\\.");
//                            GContainer gobj = GToolkit.getComponent(formHolder.getForm(), ss[0]);
//                            Interpreter inp = gobj.getInterpreter();
//                            inp.callSub(ss[1] + "(0)");
                            doCallback(formHolder.getForm(), callback, "0");
                        } else {
                            SysLog.info("showConfirm callback format \"PAN.subname\" ,but : " + callback);
                        }
                    }
                    obj.getFrame().close();
                });
        GToolkit.showFrame(f);
        formHolder.getForm().flush();
        return null;
    }


    private DataType insertText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        int pos = Interpreter.popBackInt(para);
        String str = Interpreter.popBackStr(para);
        if (gobj instanceof GTextObject) {
            ((GTextObject) gobj).insertTextByIndex(pos, str);
        }
        return null;
    }

    private DataType deleteText(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        int pos = Interpreter.popBackInt(para);
        int pos1 = Interpreter.popBackInt(para);
        if (gobj instanceof GTextObject) {
            ((GTextObject) gobj).deleteTextRange(pos, pos1);
        }
        return null;
    }

    private DataType getCaretPos(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        int index = 0;
        if (gobj != null && gobj instanceof GTextObject) {
            index = ((GTextObject) gobj).getCaretIndex();
        }
        return Interpreter.getCachedInt(index);
    }

    private DataType showTitle(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj != null && gobj instanceof GFrame) {
            boolean show = Interpreter.popBackBool(para);
            ((GFrame) gobj).setTitleShow(show);
        }
        return null;
    }

    private DataType setBgImg(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
        if (gobj != null) {
            boolean show = Interpreter.popBackBool(para);
            gobj.setVisible(show);
        }
        return null;
    }

    private DataType getVisible(ArrayList<DataType> para) {
        String compont = Interpreter.popBackStr(para);
        GObject gobj = GToolkit.getComponent(formHolder.getForm(), compont);
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
                if (async) {//异步要交给主线程执行，因为实践中出现死锁，GContainer 的elements 同步比较多，多线程死锁
                    GCmd cmd = new GCmd(
                            () -> {
                                doHttpCallback(formHolder.getForm(), callback, href, -1, "url format error");
                            });
                    GDesktop.addCmd(cmd);
                } else {
                    doHttpCallback(formHolder.getForm(), callback, href, -1, "url format error");
                }
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
                                            doHttpCallback(formHolder.getForm(), callback, url, msg.getCode(), msg.getReply());
                                        });
                                GForm.addCmd(cmd);
                                GForm.flush();
                            } else {
                                doHttpCallback(formHolder.getForm(), callback, url, msg.getCode(), msg.getReply());
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
                        showProgressBar(formHolder.getForm(), progress);
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
            SysLog.error("call sub error: httpGetSync(url,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, null, callback, false);
    }

    public DataType httpGet(ArrayList<DataType> para) {
        if (para.size() < 1) {
            SysLog.error("call sub error: httpGet(url,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, null, callback, true);
    }

    public DataType httpPostSync(ArrayList<DataType> para) {
        if (para.size() < 2) {
            SysLog.error("call sub error: httpPostSync(url,postdata,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String postData = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, postData, callback, false);
    }

    public DataType httpPost(ArrayList<DataType> para) {
        if (para.size() < 2) {
            SysLog.error("call sub error: httpPost(url,postdata,callback)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        String postData = Interpreter.popBackStr(para);
        String callback = para.isEmpty() ? null : Interpreter.popBackStr(para);
        return httpRequestImpl(href, postData, callback, true);
    }

    public DataType urlGetAsString(ArrayList<DataType> para) {
        if (para.size() < 1) {
            SysLog.error("call sub error: urlGetAsString(url)");
            return null;
        }
        String href = Interpreter.popBackStr(para);
        try {
            URL url = new URL(href);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            byte[] data = new byte[is.available()];
            is.read(data);
            return Interpreter.getCachedStr(new String(data, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Interpreter.getCachedStr("");
    }

    public DataType setClipboard(ArrayList<DataType> para) {
        if (para.size() < 1) {
            SysLog.error("call sub error: setClipboard(url)");
            return null;
        }
        String str = Interpreter.popBackStr(para);
        Glfw.glfwSetClipboardString(GCallBack.getInstance().getDisplay(), str);
        Glfm.glfmSetClipBoardContent(str);
        return null;
    }

    public DataType getClipboard(ArrayList<DataType> para) {
        String s = Glfw.glfwGetClipboardString(GCallBack.getInstance().getDisplay());
        if (s == null) s = Glfm.glfmGetClipBoardContent();
        if (s == null) s = "";
        return Interpreter.getCachedStr(s);
    }

    public DataType getVersion(ArrayList<DataType> para) {
        String jarName = Interpreter.popBackStr(para);
        String ver = "";
        if (jarName != null) {
            ver = AppLoader.getApplicationVersion(jarName);
        }
        return Interpreter.getCachedStr(ver);
    }

    public DataType compareVersion(ArrayList<DataType> para) {
        String v1 = Interpreter.popBackStr(para);
        String v2 = Interpreter.popBackStr(para);
        int ret = AppLoader.compareVersions(v1, v2);
        return Interpreter.getCachedInt(ret);
    }
}
