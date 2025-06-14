/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.gui.GForm;
import org.mini.gui.GLanguage;
import org.mini.gui.GToolkit;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GCmd;
import org.mini.gui.style.GStyle;
import org.mini.layout.guilib.FormHolder;
import org.mini.layout.loader.XuiLoader;
import org.mini.util.SysLog;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Gust
 */
public abstract class GApplication implements FormHolder {
    public enum AppState {
        STATE_NEW, STATE_INITED, STATE_RUNNING, STATE_PAUSEED, STATE_CLOSED,
    }

    private AppState state = AppState.STATE_NEW;

    public static final String APP_CONFIG_FILE = "localsetting.propertites";
    static final String APP_LANG_KEY = "_inner_lang";

    private GForm form;
    String saveRootPath;
    GStyle oldStyle;
    GStyle myStyle;
    int myLang;
    float myFPS;
    String jarName;

    private String appId = toString();

    {
        GCallBack.getInstance().setApplication(this);//put here to make sure GCallBack.getInstance() is not null
    }

    private int curLang = GLanguage.ID_NO_DEF;

    Properties prop = new Properties();

    //监管所有本应用的线程
    List<Thread> threads = new CopyOnWriteArrayList<>();


    public GApplication() {
    }


    public void init() {
        if (state != AppState.STATE_NEW) return;
        setSaveRoot(AppLoader.getAppDataPath(jarName));
        AppLoader.loadPropFile(getAppConfigFile(), prop);
        String langId = getProperty(APP_LANG_KEY, GLanguage.ID_NO_DEF + "");
        try {
            curLang = Integer.parseInt(langId);
        } catch (Exception e) {
        }
        state = AppState.STATE_INITED;
        try {
            onInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (form == null) {
            SysLog.error("init form failed");
        }
        startApp();
    }

    public abstract void onInit();

    /**
     * return current form
     *
     * @return
     */
    public GForm getForm() {
        return form;
    }

    public void setForm(GForm form) {
        this.form = form;
    }

    void setJarName(String jarName) {
        this.jarName = jarName;
    }

    String getJarName() {
        return jarName;
    }

    void setOldStyle(GStyle style) {
        oldStyle = style;
    }

    void setSaveRoot(String path) {
        saveRootPath = path;
    }

    public final String getSaveRoot() {
        return saveRootPath;
    }

    public AppState getState() {
        return state;
    }

    public void setState(AppState state) {
        this.state = state;
    }

    public final void startApp() {
        setState(AppState.STATE_RUNNING);
        try {
            onStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void closeApp() {
        if (getState() == AppState.STATE_CLOSED) return;
        SysLog.info("Closed app : " + this);
        try {
            onClose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        GLanguage.clear(appId);
        GToolkit.setStyle(oldStyle);
        AppManager.getInstance().removeRunningApp(this);
        AppManager.getInstance().active();
        GForm.addCmd(new GCmd(() -> {
            Thread.currentThread().setContextClassLoader(null);
        }));
        closeThreads();
        setState(AppState.STATE_CLOSED);

        if (AppLoader.isBootRun()) {
            System.exit(0);
        }
    }

    public final void pauseApp() {
        if (getState() != AppState.STATE_RUNNING) return;
        try {
            onPause();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setState(AppState.STATE_PAUSEED);
        myStyle = GToolkit.getStyle();
        GToolkit.setStyle(oldStyle);
        myLang = GLanguage.getCurLang();
        myFPS = GCallBack.getInstance().getFps();
        AppManager.getInstance().active();
    }

    public final void resumeApp() {
        if (getState() != AppState.STATE_PAUSEED) return;
        setState(AppState.STATE_RUNNING);
        GCallBack.getInstance().setFps(myFPS);
        GLanguage.setCurLang(myLang);
        oldStyle = GToolkit.getStyle();
        GToolkit.setStyle(myStyle);
        try {
            onResume();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * AppManager notify this application will start
     */
    public void onStart() {

    }

    /**
     * AppManager notify this application will close
     */
    public void onClose() {

    }

    /**
     * AppManager notify this application pause ,eg call , app enter background
     */
    public void onPause() {

    }

    /**
     * AppManager notify this application resume from pause ,eg call end , app reactived
     */
    public void onResume() {

    }

    /**
     * register language string by key,
     * String array order is: GLanguage.ID_...
     *
     * @param key
     * @param value
     */
    public void regString(String key, String[] value) {
        GLanguage.addString(appId, key, value);
    }

    /**
     * get language string by key, if not found , return key
     *
     * @param key
     * @return
     */
    public String getString(String key) {
        int lang = curLang;
        if (lang == GLanguage.ID_NO_DEF) {
            lang = GLanguage.getCurLang();
        }
        return GLanguage.getString(appId, key, lang);
    }

    public void setLanguageId(int lang) {
        curLang = lang;
        setProperty(APP_LANG_KEY, lang + "");
    }

    public int getLanguageId() {
        if (curLang != GLanguage.ID_NO_DEF) return curLang;
        return GLanguage.getCurLang();
    }

    private String getAppConfigFile() {
        return getSaveRoot() + "/" + APP_CONFIG_FILE;
    }

    public String getProperty(String key) {
        String s = prop.getProperty(key);
        return s == null ? "" : s;
    }

    public String getProperty(String key, String def) {
        return prop.getProperty(key, def);
    }

    public void setProperty(String key, String val) {
        prop.setProperty(key, val);
        AppLoader.savePropFile(getAppConfigFile(), prop);
    }

    public Properties getConfigProp() {
        return prop;
    }

    public String getAppId() {
        return appId;
    }

    public void addThread(Thread t) {
        if (!threads.contains(t)) {
            //System.out.println(this + " ADD " + t);
            threads.add(t);
        }
    }

    public void removeThread(Thread t) {
        if (threads.contains(t)) {
            threads.remove(t);
        }
    }

    public void closeThreads() {
        for (Thread t : threads) {
            try {
                //System.out.println(this + " INTERRUPT " + t);
                t.interrupt();//不能stop,会造成锁未释放，主线程死锁，操作无响应
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
