package org.mini.layout.loader;

import org.mini.apploader.AppLoader;
import org.mini.apploader.GApplication;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.event.GChildrenListener;
import org.mini.gui.gscript.EnvVarProvider;
import org.mini.gui.gscript.Lib;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.XObject;
import org.mini.layout.xwebview.XuiBrowser;
import org.mini.nanovg.Nanovg;
import org.mini.util.SysLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * 提供 xml ui 装载，
 * 提供浏览器的初始化，
 * 提供本地设置的保存和加载
 */
public class XuiLoader {
    public static final String WEB_FRAME_NAME = "BROWSER_FRAME";
    XuiAppHolder appHolder;

    XmlExtAssist xmlExtAssist;
    XEventHandler eventHandler;
    EnvVarProvider envVarProvider;

    public XuiLoader(XuiAppHolder appHolder, XEventHandler eventHandler, Lib scriptLib) {
        if (appHolder == null) {
            throw new RuntimeException("app is null");
        }
        this.appHolder = appHolder;
        this.eventHandler = eventHandler;
        xmlExtAssist = new XmlExtAssist(appHolder);
        xmlExtAssist.addExtScriptLib(scriptLib);
        envVarProvider = getEnvVarProvider();
        xmlExtAssist.setEnvVarProvider(envVarProvider);
    }

    public EnvVarProvider getEnvVarProvider() {
        if (envVarProvider == null) {
            envVarProvider = new EnvVarProvider() {
                @Override
                public String getEnvVar(String s) {
                    return getSetting(s.toLowerCase());
                }

                @Override
                public void setEnvVar(String s, String s1) {
                    putSetting(s.toLowerCase(), s1);
                }
            };
            if (appHolder.getApp().getProperty("lang") == null)
                appHolder.getApp().setProperty("lang", GLanguage.getLangCode(AppLoader.getDefaultLang()));
            if (appHolder.getApp().getProperty("appid") == null)
                appHolder.getApp().setProperty("appid", "");
            if (appHolder.getApp().getProperty("appzone") == null)
                appHolder.getApp().setProperty("appzone", "");

            //用minipack的环境变量初始化本应用的环境变量
            String username = appHolder.getApp().getProperty("username");
//            if (username == null || username.trim().length() == 0) {
//                appHolder.getApp().setProperty("username", AppLoader.getProperty("username"));
//            }
            String userpass = appHolder.getApp().getProperty("userpass");
//            if (userpass == null || userpass.trim().length() == 0) {
//                appHolder.getApp().setProperty("userpass", AppLoader.getProperty("userpass"));
//            }
            String logintype = appHolder.getApp().getProperty("logintype");
//            if (logintype == null || logintype.trim().length() == 0) {
//                appHolder.getApp().setProperty("logintype", AppLoader.getProperty("logintype"));
//            }
            String token = appHolder.getApp().getProperty("token");
//            if (token == null || token.trim().length() == 0) {
//                appHolder.getApp().setProperty("token", AppLoader.getProperty("token"));
//            }

            appHolder.getApp().setProperty("sver", AppLoader.getBaseInfo("sver"));
            appHolder.getApp().setProperty("jar", System.getProperty("os.name").toLowerCase().replace(' ', '-'));
            appHolder.getApp().setProperty("from", AppLoader.getProperty("from"));
            appHolder.getApp().setProperty("cver", AppLoader.getBaseInfo("cver"));
            appHolder.getApp().setProperty("policy_url", AppLoader.getPolicyUrl());
            appHolder.getApp().setProperty("discovery_url", AppLoader.getProperty("discovery_url"));
            appHolder.getApp().setProperty("account_base_url", AppLoader.getProperty("account_base_url"));
            appHolder.getApp().setProperty("profile_url", AppLoader.getProperty("profile_url"));
            appHolder.getApp().setProperty("shop_url", AppLoader.getProperty("shop_url"));
            appHolder.getApp().setProperty("pay_url", AppLoader.getProperty("pay_url"));
            appHolder.getApp().setProperty("plugin_url", AppLoader.getProperty("plugin_url"));
            appHolder.getApp().setProperty("update_url", AppLoader.getProperty("update_url"));
        }
        return envVarProvider;
    }

    public XmlExtAssist getXmlExtAssist() {
        return xmlExtAssist;
    }

    public XEventHandler getEventHandler() {
        return eventHandler;
    }

    public <T extends GObject> T loadXmlUiWithPara(String uipath, HashMap<String, Object> para) {
        return (T) loadXmlUi(uipath, key -> {
            if (para != null) {
                return para.get(key);
            }
            return null;
        });
    }


    public <T extends GObject> T loadXmlUi(String uipath, UITemplate.VarGetter getter) {
        Exception ex;
        try {

            String xmlStr = GToolkit.readFileFromJarAsString(uipath, "utf-8");
            UITemplate uit = new UITemplate(xmlStr);
            for (String key : uit.getVariable()) {
                if (getter != null) {
                    Object v = getter.getVar(key);
                    if (v != null) {
                        uit.setVar(key, v);
                    }
                }
            }
            float[] inset = new float[4];//top,right,bottom,left
            GCallBack.getInstance().getInsets(inset);
            uit.setVar("_INSET_TOP", Integer.toString((int) inset[0]));
            uit.setVar("_INSET_RIGHT", Integer.toString((int) inset[1]));
            uit.setVar("_INSET_BOTTOM", Integer.toString((int) (inset[2])));
            uit.setVar("_INSET_LEFT", Integer.toString((int) inset[3]));

            String s = uit.parse();
            //System.out.println(s);
            XObject xobj = XContainer.parseXml(s, getXmlExtAssist());
            if (xobj instanceof XContainer) {
                int w = GCallBack.getInstance().getDeviceWidth();
                int h = GCallBack.getInstance().getDeviceHeight();
                ((XContainer) xobj).build(w, h, eventHandler);
            }
            return (T) xobj.getGui();
        } catch (Exception e) {
            SysLog.error("load xmlui error:" + uipath);
            ex = e;
            e.printStackTrace();
        }
        return (T) GToolkit.getMsgFrame(appHolder.getForm(), "错误信息", "加载" + uipath + "时出错：" + ex.getMessage());
    }


    public String getSetting(String key) {
        return appHolder.getApp().getProperty(key);
    }

    public static String getSetting(String key, Properties p) {
        if (key == null) {
            return null;
        }
        key = key.toLowerCase().trim();
        if (p == null) {
            return null;
        }
        return p.getProperty(key);
    }

    public Float getSettingAsFloat(String key) {
        return getSettingAsFloat(key, appHolder.getApp().getConfigProp());
    }

    public static Float getSettingAsFloat(String key, Properties p) {
        if (p == null) {
            return null;
        }
        try {
            String s = getSetting(key, p);
            Float f = Float.valueOf(s);
            return f;
        } catch (Exception e) {
        }
        return null;
    }

    public Integer getSettingAsInt(String key) {
        return getSettingAsInt(key, appHolder.getApp().getConfigProp());
    }

    public static Integer getSettingAsInt(String key, Properties p) {
        if (p == null) {
            return null;
        }
        try {
            String s = getSetting(key, p);
            Integer f = Integer.valueOf(s);
            return f;
        } catch (Exception e) {
        }
        return null;
    }

    public byte[] getSettingAsByteArr(String key) {
        return getSettingAsByteArr(key, appHolder.getApp().getConfigProp());
    }

    public static byte[] getSettingAsByteArr(String key, Properties p) {
        if (p == null) {
            return null;
        }
        try {
            String s = getSetting(key, p);
            byte[] b = javax.microedition.io.Base64.decode(s);
            return b;
        } catch (Exception e) {
        }
        return null;
    }


    public void putSetting(String key, Object value) {
        appHolder.getApp().setProperty(key, value == null ? null : value.toString());
    }

    public static void putSetting(String key, Object value, Properties p) {
        if (key == null) {
            return;
        }
        key = key.toLowerCase().trim();
        if (p == null || value == null) {
            return;
        }
        if (value instanceof byte[]) {
            byte[] b = (byte[]) value;
            String s = javax.microedition.io.Base64.encode(b, 0, b.length);
            p.put(key, s);
        } else {
            p.put(key, value.toString());
        }
    }


    public static void loadProp(GApplication app, String fname, Properties prop) {
        try {
            File f = new File(app.getSaveRoot() + "/" + fname);
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                prop.load(fis);
                //System.out.println(fname + " size: " + prop.size());
                fis.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void saveProp(GApplication app, String name, Properties prop) {
        try {
            File f = new File(app.getSaveRoot() + "/" + name);

            FileOutputStream fos = new FileOutputStream(f);
            prop.store(fos, "");
            fos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public XuiBrowser getBrowser() {
        return xmlExtAssist.getBrowser();
    }


    /**
     * @param listener nullable
     * @return
     */
    public GFrame getBrowserFrame(GChildrenListener listener) {
        GForm form = appHolder.getForm();
        return getBrowserFrame(form.getW() * 0.8f, form.getH() * 0.8f, listener);
    }

    public GFrame getBrowserFrame(float w, float h, GChildrenListener listener) {

        GForm form = appHolder.getForm();
        GFrame browserFrame = new GFrame(form);
        browserFrame.setSize(w, h);
        browserFrame.setName(WEB_FRAME_NAME);
        GToolkit.showFrame(browserFrame);
        browserFrame.setVisible(false);
        browserFrame.setClosable(false);

        GLabel hideBtn = new GLabel(form);
        hideBtn.setAlign(Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
        hideBtn.setName("BT_HIDE_BROWSER");
        hideBtn.setText("\u2716");
        hideBtn.setSize(25, 25);
        hideBtn.setLocation(10, (GFrame.TITLE_HEIGHT - hideBtn.getH()) * 0.5f);
        hideBtn.setActionListener((e) -> {
            GFrame f = GToolkit.getComponent(form, WEB_FRAME_NAME);
            if (f != null) {
                f.setVisible(false);
            }
        });
        browserFrame.getTitlePanel().add(hideBtn);
        browserFrame.getView().addChildrenListener(listener);
        return browserFrame;
    }

}
