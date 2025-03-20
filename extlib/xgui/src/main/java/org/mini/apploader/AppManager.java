/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.apploader.bean.LangBean;
import org.mini.glfm.Glfm;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GDesktop;
import org.mini.gui.event.GNotifyListener;
import org.mini.gui.gscript.EnvVarProvider;
import org.mini.gui.gscript.Interpreter;
import org.mini.layout.guilib.GuiScriptLib;
import org.mini.layout.guilib.HttpRequestReply;
import org.mini.gui.style.GStyleBright;
import org.mini.gui.style.GStyleDark;
import org.mini.http.MiniHttpClient;
import org.mini.http.MiniHttpServer;
import org.mini.json.JsonParser;
import org.mini.layout.loader.UITemplate;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.loader.XmlExtAssist;
import org.mini.layout.loader.XuiAppHolder;
import org.mini.layout.xwebview.*;
import org.mini.nanovg.Nanovg;

import java.io.*;
import java.util.*;

/**
 * @author Gust
 */
public class AppManager extends GApplication implements XuiAppHolder {
    public static final String POLICY_URL = "POLICY_URL";
    public static final String DISCOVERY_URL = "DISCOVERY_URL";
    public static final String ACCOUNT_BASE_URL = "ACCOUNT_BASE_URL";

    static final String APP_NAME_LABEL = "APP_NAME_LABEL";
    static final String APP_ICON_ITEM = "APP_ICON_ITEM";
    static final String APP_RUN_BTN = "APP_RUN_BTN";
    static final String APP_STOP_BTN = "APP_STOP_BTN";
    static final String APP_DESC_LABEL = "APP_DESC_LABEL";
    static final String APP_UPGRADE_BTN = "APP_UPGRADE_BTN";
    static final String APP_DELETE_BTN = "APP_DELETE_BTN";

    static final String STR_PLUGIN = "PLUGIN";
    static final String STR_DISCOVERY = "DISCOVERY";
    static final String STR_MY = "MY";
    static final String STR_EXIT = "STR_EXIT";
    static final String STR_SETTING = "SETTING";
    static final String STR_TITLE = "PLUGIN MANAGER";
    static final String STR_START_WEB_SRV_FOR_UPLOAD = "STR_START_WEB_SRV_FOR_UPLOAD";
    static final String STR_DOWN_APP_FROM_WEB = "STR_DOWN_APP_FROM_WEB";
    static final String STR_DOWNLOAD = "STR_DOWNLOAD";
    static final String STR_START = "STR_START";
    static final String STR_STOP = "STR_STOP";
    static final String STR_CLOSE = "STR_CLOSE";
    static final String STR_WEB_LISTEN_ON = "STR_WEB_LISTEN_ON";
    static final String STR_APP_LIST = "STR_APP_LIST";
    static final String STR_BACK = "STR_BACK";
    static final String STR_RUN = "STR_RUN";
    static final String STR_SET_AS_BOOT = "STR_SET_AS_BOOT";
    static final String STR_UPGRADE = "STR_UPGRADE";
    static final String STR_DELETE = "STR_DELETE";
    static final String STR_VERSION = "STR_VERSION";
    static final String STR_UPGRADE_URL = "STR_UPGRADE_URL";
    static final String STR_FILE_SIZE = "STR_FILE_SIZE";
    static final String STR_FILE_DATE = "STR_FILE_DATE";
    static final String STR_DESC = "STR_DESC";
    static final String STR_SERVER_STARTED = "STR_SERVER_STARTED";
    static final String STR_SERVER_STOPED = "STR_SERVER_STOPED";
    static final String STR_UPLOAD_FILE = "STR_UPLOAD_FILE";
    static final String STR_SUCCESS = "STR_SUCCESS";
    static final String STR_FAIL = "STR_FAIL";
    static final String STR_OPEN_APP_FAIL = "STR_OPEN_APP_FAIL";
    static final String STR_BRIGHT_STYLE = "STR_BRIGHT_STYLE";
    static final String STR_DARK_STYLE = "STR_DARK_STYLE";
    static final String STR_MESSAGE = "STR_MESSAGE";
    static final String STR_CONFIRM_DELETE = "STR_CONFIRM_DELETE";
    static final String STR_APP_NOT_RUNNING = "STR_APP_NOT_RUNNING";
    static final String STR_INSTALL_FROM_LOCAL = "STR_INSTALL_FROM_LOCAL";
    static final String STR_SELECT_FILE = "STR_SELECT_FILE";

    public static final float[] RUNNING_ITEM_COLOR = {0.0f, 0.8f, 0.0f, 1.0f}; //0x00cc00ff;

    static AppManager instance = new AppManager();

    GForm mgrForm;
    float[] inset = new float[4];//top,right,bottom,left
    GViewSlot mainSlot;

    public static String CVERSION = "1.0.0";
    //
    GList appList;
    GViewPort contentView;
    String curSelectedJarName;
    PluginEventHandler eventHandler;
    GHomeButton floatButton;

    MiniHttpServer webServer;
    List<MiniHttpClient> httpClients = new ArrayList<>();
    Map<String, GApplication> runningApps = new HashMap<>();

    GImage runningImg = GImage.createImageFromJar("/res/ui/green.png");
    GImage titleImg = GImage.createImageFromJar("/res/ui/title.png");

    GTextBox logBox;
    static PrintStream systemOutDefault = System.out;
    static PrintStream systemErrDefault = System.err;
    static InputStream systemInDefault = System.in;

    //the app would launch after Orientation set success
    String appOri;
    //
    static final int PICK_PHOTO = 101, PICK_CAMERA = 102, PICK_QR = 103, PICK_HEAD = 104;

    static float devW, devH;
    XmlExtAssist assist;
    GContainer webView;

    /**
     * @return
     */

    static public AppManager getInstance() {
        return instance;
    }

    public void active() {
        if (GCallBack.getInstance().getApplication() == this) return;

        onResume();

        GCallBack.getInstance().setApplication(this);
        if (curSelectedJarName != null) {
            updateContentViewInfo(curSelectedJarName);
        }
        if (mgrForm != null) {
            mgrForm.setSize(GCallBack.getInstance().getDeviceWidth(), GCallBack.getInstance().getDeviceHeight());
            mainPanelShowLeft();
            reloadAppList();
        }
    }

    public void onResume() {
        System.setOut(systemOutDefault);
        System.setErr(systemErrDefault);
        System.setIn(systemInDefault);

        GCallBack.getInstance().setFps(GCallBack.FPS_DEFAULT);

        Glfm.glfmSetSupportedInterfaceOrientation(GCallBack.getInstance().getDisplay(), Glfm.GLFMInterfaceOrientationPortrait);
        Glfm.glfmSetDisplayChrome(GCallBack.getInstance().getDisplay(), Glfm.GLFMUserInterfaceChromeNavigationAndStatusBar);

        GLanguage.setCurLang(AppLoader.getDefaultLang());

        if (AppLoader.getGuiStyle() == 0) {
            GToolkit.setStyle(new GStyleBright());
        } else {
            GToolkit.setStyle(new GStyleDark());
        }

        if (mgrForm != null) {
            GForm.hideKeyboard(mgrForm);
        }
    }

    private void regStrings() {

        JsonParser<LangBean> parser = new JsonParser<>();
        String s = GToolkit.readFileFromJarAsString("/res/lang.json", "utf-8");
        LangBean langBean = parser.deserial(s, LangBean.class);

        for (String key : langBean.getLang().keySet()) {
            GLanguage.addString(getAppId(), key, langBean.getLang().get(key));
        }
    }

    @Override
    public void onInit() {
        regStrings();

        mgrForm = loadXmlForm();
        initForm();
        initExplorer();

        //init form
        if (floatButton == null) {
            floatButton = new GHomeButton();
        }
        GCallBack.getInstance().getDesktop().add(floatButton);


        reloadAppList();
    }

    /**
     * ==============================================================================
     * GForm that is used to show plugin manager
     * ==============================================================================
     */

    GForm loadXmlForm() {

        final GCallBack ccb = GCallBack.getInstance();
        devW = ccb.getDeviceWidth();
        devH = ccb.getDeviceHeight();

        String xmlStr = "";
        try {
            xmlStr = new String(GToolkit.readFileFromJar("/res/ui/AppManager.xml"), "utf-8");
        } catch (Exception e) {
        }

        // init script environment
        assist = new XmlExtAssist(this);
        updateScriptEnvironment();
        assist.setEnvVarProvider(envVarProvider);

        UITemplate uit = new UITemplate(xmlStr);
        for (String s : uit.getVariable()) {
            uit.setVar(s, getString(s));
        }

        GCallBack.getInstance().getInsets(inset);
        int h = (int) (inset[0]);
        //System.out.println("STATEBAR_HEIGHT " + inset[0] + " , " + inset[1] + " , " + inset[2] + " , " + inset[3]);
        if (h <= 20) {
            h = 20;
        }
        uit.setVar("STATEBAR_HEIGHT", Integer.toString(h));
        h = (int) (inset[2]);
        uit.setVar("NAV_HEIGHT", Integer.toString(h));


        eventHandler = new PluginEventHandler();//使支持openpage, downloadinstall, downloadsave
        XContainer xcon = (XContainer) XContainer.parseXml(uit.parse(), assist);
        xcon.build((int) devW, (int) (devH), eventHandler);
        GForm form = xcon.getGui();
        mainSlot = form.findByName("SLOT_MGR");
        appList = mainSlot.findByName("LIST_APP");
        contentView = mainSlot.findByName("VP_CONTENT");
        logBox = mainSlot.findByName("INPUT_LOG");
        GList langList = mainSlot.findByName("LIST_LANG");
        langList.setSelectedIndex(AppLoader.getDefaultLang());
        GList styleList = mainSlot.findByName("LIST_STYLE");
        if (AppLoader.getGuiStyle() == 0) {
            styleList.setSelectedIndex(0);
        } else {
            styleList.setSelectedIndex(1);
        }
        String url = AppLoader.getDownloadUrl();
        //System.out.println("downloadurl=" + url);
        if (url != null) GToolkit.setCompText(mainSlot, "INPUT_URL", url);

        form.setSizeChangeListener((width, height) -> {
            xcon.reSize(width, height);
//                browser.getWebView().getLayout().reSize(width, height);
            reloadAppList();
        });
        return form;
    }

    public void initForm() {
        //System.out.println("devW :" + devW + ", devH  :" + devH);

        onResume();

        mgrForm.setNotifyListener(new GNotifyListener() {
            @Override
            public void onNotify(String key, String val) {
                //System.out.println("[INFO]notify:" + key + "," + val);
                try {
                    switch (key) {
                        case NOTIFY_KEY_IOS_PURCHASE:
                            if (val.indexOf(':') > 0) {
                                String[] ss = val.split(":");
                                if (ss.length > 2) {
                                    int code = Integer.parseInt(ss[0]);
                                    String receipt = ss[1];
                                    byte[] scriptBytes = javax.microedition.io.Base64.decode(ss[2]);
                                    String script = new String(scriptBytes, "utf-8");
                                    //System.out.println("script:" + script);
                                    Interpreter inp = new Interpreter();
                                    inp.reglib(new GuiScriptLib(AppManager.getInstance()));
                                    inp.loadFromString(script);
                                    inp.putGlobalVar("iap_code", Interpreter.getCachedInt(code));
                                    inp.putGlobalVar("iap_receipt", Interpreter.getCachedStr(receipt));
                                    inp.start();
                                }
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mgrForm.setPickListener((uid, url, data) -> {
            if (data == null && url != null) {
                File f = new File(url);
                if (f.exists()) {
                    try {
                        FileInputStream fis = new FileInputStream(f);
                        data = new byte[(int) f.length()];
                        fis.read(data);
                        fis.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            switch (uid) {

                case PICK_PHOTO:
                case PICK_CAMERA: {

                    if (data != null) {

                    }
                    break;
                }
                case PICK_QR: {

                    break;
                }
                case PICK_HEAD: {
                    if (data != null) {

                    }
                    break;
                }
            }
        });
        int i = AppLoader.getGuiStyle();
        setStyleButton(i);
    }


    public MiniHttpClient.DownloadCompletedHandle getDownloadCallback() {
        return (client, url, data) -> {
            httpClients.remove(client);
            log("Download success " + url + " ,size: " + data.length);
            GForm.addMessage((data == null ? getString(STR_FAIL) : getString(STR_SUCCESS)) + " " + getString(STR_DOWNLOAD) + " " + url);
            String jarName = null;
            if (url.lastIndexOf('/') > 0) {
                jarName = url.substring(url.lastIndexOf('/') + 1);
                if (jarName.indexOf('?') > 0) {
                    jarName = jarName.substring(0, jarName.indexOf('?'));
                }
            }
            if (jarName != null && data != null) {
                AppLoader.addApp(jarName, data);
            }
            reloadAppList();
            updateContentViewInfo(jarName);
        };
    }

    void setStyleButton(int i) {
        GButton bt = GToolkit.getComponent(mgrForm, "BT_STYLE");
        if (i == 0) {
            bt.setPreIcon("◑");
        } else {
            bt.setPreIcon("\uD83D\uDD06");
        }
    }

    void initExplorer() {
        webView = GToolkit.getComponent(mgrForm, "TD_DISCOVERY");
    }

    @Override
    public GApplication getApp() {
        return this;
    }

    @Override
    public GContainer getWebView() {
        return webView;
    }


    void reloadAppList() {
        if (appList == null) {
            return;
        }
        appList.clear();
        List<String> list = AppLoader.getAppList();
        if (list != null && list.size() > 0) {
            for (String appName : list) {
                //System.out.println("appName:" + appName);
                if (AppLoader.isJarExists(appName)) {
                    byte[] iconBytes = AppLoader.getApplicationIcon(appName);
                    GImage img = null;
                    if (iconBytes != null) {
                        img = GImage.createImage(iconBytes);
                    }
                    String name = AppLoader.getApplicationName(appName);
                    GListItem item = new GListItem(mgrForm, img, name);
                    if (img == null) {
                        item.setPreIcon("\uD83D\uDCD5");
                    }
                    item.setAttachment(appName);
                    appList.add(item);
                    item.setActionListener(gobj -> {
                        curSelectedJarName = gobj.getAttachment();
                        runApp();
                    });

                    //add label
                    GLabel label = new GLabel(mgrForm, "ⓘ", item.getW() - 40, 0, 40, item.getH());
                    label.setAlign(Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
                    item.add(label);
                    label.setActionListener(gobj -> {
                        curSelectedJarName = gobj.getParent().getAttachment();//buttom's attachment
                        updateContentViewInfo(appName);
                        mainPanelShowRight();
                    });

                    //set label color
                    if (runningApps.get(appName) != null) {
                        setListItemColor(item, RUNNING_ITEM_COLOR);
                    }
                }
            }
        }
        GForm.flush();
    }

    void setListItemColor(GListItem item, float[] color) {
        if (item == null || item.getElementSize() == 0) return;
        GLabel infoLab = (GLabel) item.getElements().get(0);
        infoLab.setColor(color);
    }

    void updateContentViewInfo(String jarName) {
        GLabel nameLab = contentView.findByName(APP_NAME_LABEL);
        nameLab.setText(AppLoader.getApplicationName(jarName));
        //
        GTextBox descLab = contentView.findByName(APP_DESC_LABEL);
        String dStr = "-";
        long d = AppLoader.getApplicationFileDate(jarName);
        if (d > 0) {
            dStr = getDateString(d);
        }
        String txt = getString(STR_VERSION) + "\n  " + AppLoader.getApplicationVersion(jarName) + "\n" + getString(STR_FILE_SIZE) + "\n  " + AppLoader.getApplicationFileSize(jarName) + "\n" + getString(STR_FILE_DATE) + "\n  " + dStr + "\n" + getString(STR_UPGRADE_URL) + "\n  " + AppLoader.getApplicationUpgradeurl(jarName) + "\n" + getString(STR_DESC) + "\n  " + AppLoader.getApplicationDesc(jarName) + "\n";
        descLab.setText(txt);

        GButton stop = contentView.findByName(APP_STOP_BTN);
        if (runningApps.get(jarName) != null) {
            stop.setEnable(true);
        } else {
            stop.setEnable(false);
        }

        //re set image
        GImageItem icon = contentView.findByName(APP_ICON_ITEM);
        GListItem gli = getGListItemByAttachment(curSelectedJarName);
        if (gli != null) icon.setImg(gli.getImg());
        contentView.reAlign();
    }

    GListItem getGListItemByAttachment(String jarName) {
        if (jarName == null) return null;
        List<GObject> items = appList.getItemList();
        for (int i = 0; i < items.size(); i++) {
            if (jarName.equals(items.get(i).getAttachment())) {
                return (GListItem) items.get(i);
            }
        }
        return null;
    }

    void mainPanelShowLeft() {
        if (curSelectedJarName != null) {
            appList.setSelectedIndex(-1);
            curSelectedJarName = null;
        }
        mainSlot.moveTo(0, 100);
    }

    void mainPanelShowRight() {
        mainSlot.moveTo(1, 100);
    }

    public GHomeButton getFloatButton() {
        return floatButton;
    }

    public MiniHttpServer getWebServer() {
        return webServer;
    }

//    public List<MiniHttpClient> getHttpClients() {
//        for (Iterator<MiniHttpClient> it = httpClients.iterator(); it.hasNext(); ) {
//            MiniHttpClient httpClient = it.next();
//            if (!httpClient.isAlive()) {
//                it.remove();
//            }
//        }
//        return httpClients;
//    }

    void addRunningApp(GApplication app) {
        if (app == null) return;
        runningApps.put(app.getJarName(), app);
    }

    public void removeRunningApp(GApplication app) {
        if (app == null) return;
        runningApps.remove(app.getJarName());
    }

    MiniHttpServer.SrvLogger srvLogger = new MiniHttpServer.SrvLogger() {
        @Override
        public void log(String s) {
            AppManager.log(s);
        }
    };

    MiniHttpClient.CltLogger cltLogger = new MiniHttpClient.CltLogger() {
        @Override
        public void log(String s) {
            AppManager.log(s);
        }
    };

    /**
     * @param s
     */
    public static void log(String s) {
        GTextBox box = getInstance().logBox;
        if (box != null) {
            box.setCaretIndex(box.getText().length());
            Calendar c = Calendar.getInstance();
            box.insertTextAtCaret("\n" + getDateString(c.getTimeInMillis()) + " " + s);
            box.setScroll(1.f);
            GForm.flush();
        }
    }

    public static String getDateString(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int dayInMonth = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        String ret = String.valueOf(year);
        ret += "-";
        ret += month < 10 ? "0" + month : String.valueOf(month);
        ret += "-";
        ret += dayInMonth < 10 ? "0" + dayInMonth : String.valueOf(dayInMonth);
        ret += " ";
        ret += hour < 10 ? "0" + hour : String.valueOf(hour);
        ret += ":";
        ret += minute < 10 ? "0" + minute : String.valueOf(minute);
        ret += ":";
        ret += seconds < 10 ? "0" + seconds : String.valueOf(seconds);
        return ret;

    }

    public GApplication[] getRunningApps() {
        return runningApps.values().toArray(new GApplication[0]);
    }


    void startWebServer() {

        GButton uploadbtn = GToolkit.getComponent(mgrForm, "BT_STARTWEB");
        GLabel uploadLab = (GLabel) mgrForm.findByName("LAB_WEBSRV");
        if (webServer != null) {
            webServer.stopServer();
            webServer = null;
        }
        if (uploadbtn.getText().equals(getString(STR_STOP))) {
            uploadbtn.setText(getString(STR_START));
            uploadLab.setText(getString(STR_START_WEB_SRV_FOR_UPLOAD));
            String s = getString(STR_SERVER_STOPED);
            GDesktop.addMessage(s);
            log(s);
        } else {
            webServer = new MiniHttpServer(MiniHttpServer.DEFAULT_PORT, srvLogger);
            webServer.setUploadCompletedHandle(files -> {
                for (MiniHttpServer.UploadFile f : files) {
                    String s = getString(STR_UPLOAD_FILE) + " " + f.filename + " " + f.data.length;
                    if (AppLoader.addApp(f.filename, f.data)) {
                        GForm.addMessage(s + " " + getString(STR_SUCCESS));
                        log(s);
                    } else {
                        GForm.addMessage(s + " " + getString(STR_FAIL));
                    }
                }
                reloadAppList();
            });
            webServer.start();
            uploadbtn.setText(getString(STR_STOP));
            uploadLab.setText(getString(STR_WEB_LISTEN_ON) + webServer.getPort());
            String s = getString(STR_SERVER_STARTED);
            GForm.addMessage(s);
            log(s);
        }
    }

    void runApp() {
        if (curSelectedJarName != null) {
            GListItem gli = getGListItemByAttachment(curSelectedJarName);
            setListItemColor(gli, RUNNING_ITEM_COLOR);

            GDesktop.addCmd(new Runnable() {
                int retry = 0;

                @Override
                public void run() {
                    GForm.flush();

                    String orientation = AppLoader.getApplicationOrientation(curSelectedJarName);
                    if (orientation.equalsIgnoreCase("h")) {
                        Glfm.glfmSetSupportedInterfaceOrientation(GCallBack.getInstance().getDisplay(), Glfm.GLFMInterfaceOrientationLandscapeLeft | Glfm.GLFMInterfaceOrientationLandscapeRight);
                    } else {
                        Glfm.glfmSetSupportedInterfaceOrientation(GCallBack.getInstance().getDisplay(), Glfm.GLFMInterfaceOrientationPortrait);
                    }
                    appOri = orientation;
                    String osname = System.getProperty("os.name");
                    if ("iOS".equalsIgnoreCase(osname) || "Android".equalsIgnoreCase(osname)) {
                        //float w = GCallBack.getInstance().getDeviceWidth();
                        //float h = GCallBack.getInstance().getDeviceHeight();
                        GCallBack.getInstance().getInsets(inset);
                        //System.out.println("[INFO] INSET:" + inset[0] + " , " + inset[1] + " , " + inset[2] + " , " + inset[3]);

                        if ("h".equals(appOri)) {
                            int ori = Glfm.glfmGetInterfaceOrientation(GCallBack.getInstance().getDisplay());
                            if ((ori & Glfm.GLFMInterfaceOrientationLandscapeLeft) == 0
                                    && (ori & Glfm.GLFMInterfaceOrientationLandscapeRight) == 0
                            ) {
                                if (retry++ < 120) { // wait for turn orientation
                                    GDesktop.addCmd(this);
                                }
                                return;
                            }
                        }
                    }

                    String jarName = curSelectedJarName;
                    if (jarName != null) {
                        GApplication app = runningApps.get(jarName);
                        if (app != null && app.getState() != AppState.STATE_CLOSED) {
                            GCallBack.getInstance().setApplication(app);
                            app.resumeApp();
                        } else {
                            app = AppLoader.runApp(jarName);
                            if (app != AppManager.this) {
                                runningApps.put(jarName, app);
                                updateContentViewInfo(jarName);
                                setListItemColor(gli, RUNNING_ITEM_COLOR);
                            } else {
                                GForm.addMessage(getString(AppManager.STR_OPEN_APP_FAIL) + ": " + jarName);
//                                            GForm.addMessage("Can't found startup class ,it setup in config.txt in jar root");
                            }
                        }
                    }
                }
            });
        }
    }

    void stopApp() {

        if (curSelectedJarName != null) {
            //AppLoader.setBootApp(curSelectedItem.getAttachment());
            String tmpJarName = curSelectedJarName;
            GApplication app = runningApps.get(tmpJarName);
            if (app != null) {
                app.closeApp();
                GForm.addMessage(getString(STR_SUCCESS));
                GListItem gli = getGListItemByAttachment(curSelectedJarName);
                setListItemColor(gli, null);
                updateContentViewInfo(tmpJarName);
            } else {
                GForm.addMessage(getString(STR_APP_NOT_RUNNING));
            }
        }
    }

    class PluginEventHandler extends XEventHandler {
        @Override
        public void action(GObject gobj) {
            String name = gobj.getName();
            if (name == null) return;
            switch (name) {
                case "APP_DELETE_BTN":
                    String tmpJarName;
                    if (curSelectedJarName != null) {
                        tmpJarName = curSelectedJarName;
                        if (tmpJarName != null) {
                            GFrame confirmFrame = GToolkit.getConfirmFrame(mgrForm, getString(STR_MESSAGE), getString(STR_CONFIRM_DELETE), getString(STR_DELETE), okgobj -> {
                                AppLoader.removeApp(tmpJarName);
                                reloadAppList();
                                mainPanelShowLeft();
                                okgobj.getFrame().close();
                            }, getString("Cancel"), null, 300f, 180f);
                            GToolkit.showFrame(confirmFrame);
                        }
                    }
                    break;
                case "BT_DOWN":
                    String url = GToolkit.getCompText(mgrForm, "INPUT_URL");
                    AppLoader.setDownloadUrl(url);

                    MiniHttpClient hc = new MiniHttpClient(url, cltLogger, getDownloadCallback());
                    hc.start();
                    httpClients.add(hc);
                    break;
                case "BT_TEST":
                    break;
                case "BT_BACK":
                    mainPanelShowLeft();
                    break;
                case "BT_STARTWEB":
                    startWebServer();
                    break;
                case "APP_RUN_BTN":
                    runApp();
                    break;
                case "APP_STOP_BTN":
                    stopApp();
                    break;
                case "APP_UPGRADE_BTN":
                    if (curSelectedJarName != null) {
                        tmpJarName = curSelectedJarName;
                        url = AppLoader.getApplicationUpgradeurl(tmpJarName);
                        if (url != null) {
                            hc = new MiniHttpClient(url, cltLogger, getDownloadCallback());
                            hc.start();
                            httpClients.add(hc);
                        }
                    }
                    break;
                case "BT_LOCALFILE":
                    GFrame chooser = GToolkit.getFileChooser(mgrForm, getString(STR_INSTALL_FROM_LOCAL), null, new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.isDirectory() || file.getName().endsWith(".jar");
                        }
                    }, GCallBack.getInstance().getDeviceWidth() * .9f, GCallBack.getInstance().getDeviceHeight() * .9f, gobj1 -> {
                        File f = gobj1.getAttachment();
                        if (f.isFile()) {
                            String fullpath = f.getAbsolutePath();
                            String path = fullpath;
                            int idx = path.lastIndexOf(File.separator);
                            if (idx >= 0) {
                                path = path.substring(idx + 1);
                            }
                            if (path.length() >= 0) {
                                if (AppLoader.addApp(path, fullpath)) {
                                    GForm.addMessage(getString(STR_SUCCESS) + " " + fullpath);
                                    reloadAppList();
                                } else {
                                    GForm.addMessage(getString(STR_FAIL) + " " + fullpath);
                                }
                            }
                        }
                    }, null);
                    chooser.getTitlePanel().setBgImg(titleImg);
                    chooser.getTitlePanel().setBgImgAlpha(0.3f);
                    GToolkit.showFrame(chooser);
                    break;
                case "BT_SETTING":
                    mainSlot.moveTo(2, 0);
                    break;
                case "LI_BRIGHT":
                    GToolkit.setStyle(new GStyleBright());
                    AppLoader.setGuiStyle(0);
                    setStyleButton(0);
                    break;
                case "LI_DARK":
                    GToolkit.setStyle(new GStyleDark());
                    AppLoader.setGuiStyle(1);
                    setStyleButton(1);
                    break;
                case "BT_STYLE": {
                    int i = AppLoader.getGuiStyle();
                    i = i == 0 ? 1 : 0;
                    setStyleButton(i);
                    GToolkit.setStyle(i == 0 ? new GStyleBright() : new GStyleDark());
                    AppLoader.setGuiStyle(i);
                    break;
                }
            }
        }


        @Override
        public void onStateChange(GObject gobj) {
            String name = gobj.getName();
            if (name == null) {
                return;
            }
            switch (name) {
                case "INPUT_SEARCH":
                    GTextObject search = (GTextObject) gobj;
                    String str = search.getText();
                    if (appList != null) {
                        appList.filterLabelWithKey(str);
                        //System.out.println("key=" + str);
                    }
                    break;
                case "LIST_LANG":
                    int selectedIndex = ((GList) gobj).getSelectedIndex();
                    GLanguage.setCurLang(selectedIndex);
                    AppLoader.setDefaultLang(selectedIndex);
                    onInit();
                    break;
            }
        }
    }


    EnvVarProvider envVarProvider = new EnvVarProvider() {

        @Override
        public void setEnvVar(String envName, String envValue) {
            if (envName == null) {
                System.out.println("[WARN]envvar key is null");
            }
            if (envValue == null) {
                envValue = "";
            }
            String enLow = envName.toLowerCase();
            AppLoader.setProperty(enLow, envValue);
        }

        @Override
        public String getEnvVar(String envName) {
            String val = AppLoader.getProperty(envName.toLowerCase());
            if (val == null) {
                val = "";
            }
            //System.out.println("getEnvVar:" + envName + "=" + val);
            return val;
        }
    };

    void updateScriptEnvironment() {
        String from = AppLoader.getBaseInfo("from");
        String profile = AppLoader.getBaseInfo("profile");
        String policyUrl = AppLoader.getBaseInfo(from + "." + profile + ".policyUrl");
        envVarProvider.setEnvVar("policy_url", policyUrl);
        envVarProvider.setEnvVar("lang", GLanguage.getLangCode(AppLoader.getDefaultLang()));
        envVarProvider.setEnvVar("appid", AppLoader.getBaseInfo("appid"));
        envVarProvider.setEnvVar("appzone", AppLoader.getBaseInfo("appzone"));
        envVarProvider.setEnvVar("sver", AppLoader.getBaseInfo("sver"));
        envVarProvider.setEnvVar("jar", System.getProperty("os.name").toLowerCase().replace(' ', '-'));
        envVarProvider.setEnvVar("from", AppLoader.getBaseInfo("from"));
        envVarProvider.setEnvVar("cver", AppLoader.getBaseInfo("cver"));
        envVarProvider.setEnvVar("discovery_url", "");
        envVarProvider.setEnvVar("account_base_url", "");
        envVarProvider.setEnvVar("profile_url", "");
        envVarProvider.setEnvVar("shop_url", "");
        envVarProvider.setEnvVar("pay_url", "");
        envVarProvider.setEnvVar("plugin_url", "");
    }


    public String[] getPolicy(String url) {
        try {
            if (url == null) {
                return null;
            }
            XuiResourceLoader loader = new XuiResourceLoader();
            XuiResource res = loader.loadResource(url);
            if (res != null) {
                String json = res.getString();
                if (json != null) {
                    JsonParser<HttpRequestReply> parser = new JsonParser<>();
                    HttpRequestReply reply = parser.deserial(json, HttpRequestReply.class);
                    if (reply != null && reply.getCode() == 0) {
                        String s = reply.getReply();
                        String[] ss = s.split("\n");
                        return ss;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
