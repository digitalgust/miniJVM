/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.glfm.Glfm;
import org.mini.glwrap.GLUtil;
import org.mini.gui.*;
import org.mini.layout.*;

import java.io.*;
import java.util.*;

/**
 * @author Gust
 */
public class AppManager extends GApplication {

    static final String APP_NAME_LABEL = "APP_NAME_LABEL";
    static final String APP_ICON_ITEM = "APP_ICON_ITEM";
    static final String APP_RUN_BTN = "APP_RUN_BTN";
    static final String APP_STOP_BTN = "APP_STOP_BTN";
    static final String APP_DESC_LABEL = "APP_DESC_LABEL";
    static final String APP_UPGRADE_BTN = "APP_UPGRADE_BTN";
    static final String APP_DELETE_BTN = "APP_DELETE_BTN";

    static final String STR_EXIT = "Exit";
    static final String STR_SETTING = "SETTING";
    static final String STR_TITLE = "PLUGIN MANAGER";
    static final String STR_START_WEB_SRV_FOR_UPLOAD = "Lan webserver for upload plugin";
    static final String STR_DOWN_APP_FROM_WEB = "Download plugin from website:";
    static final String STR_DOWNLOAD = "Download";
    static final String STR_START = "Start";
    static final String STR_STOP = "Stop";
    static final String STR_WEB_LISTEN_ON = "Lan webserver on:";
    static final String STR_APP_LIST = "Plugin list : ";
    static final String STR_BACK = "Back";
    static final String STR_RUN = "Run";
    static final String STR_SET_AS_BOOT = "Put on top";
    static final String STR_UPGRADE = "upgrade";
    static final String STR_DELETE = "Delete";
    static final String STR_VERSION = "Version: ";
    static final String STR_UPGRADE_URL = "Upgrade url: ";
    static final String STR_FILE_SIZE = "File size: ";
    static final String STR_FILE_DATE = "File date: ";
    static final String STR_DESC = "Description: ";
    static final String STR_SERVER_STARTED = "Webserver started";
    static final String STR_SERVER_STOPED = "Webserver stoped";
    static final String STR_UPLOAD_FILE = "Uploaded file";
    static final String STR_SUCCESS = "Success";
    static final String STR_FAIL = "Fail";
    static final String STR_OPEN_APP_FAIL = "Open plugin failed";
    static final String STR_BRIGHT_STYLE = "Bright appearance";
    static final String STR_DARK_STYLE = "Dark appearance";
    static final String STR_MESSAGE = "Message";
    static final String STR_CONFIRM_DELETE = "Do you confirm to delete the plugin ";
    static final String STR_APP_NOT_RUNNING = "Plugin is not running ";
    static final String STR_INSTALL_FROM_LOCAL = "Install plugin from local file:";
    static final String STR_SELECT_FILE = "Browse File";

    static private void regStrings() {
        GLanguage.addString(STR_SETTING, new String[]{STR_SETTING, "设置", "设置"});
        GLanguage.addString(STR_EXIT, new String[]{STR_EXIT, "退出", "退出"});
        GLanguage.addString(STR_TITLE, new String[]{STR_TITLE, "组件管理器", "組件管理器"});
        GLanguage.addString(STR_START_WEB_SRV_FOR_UPLOAD, new String[]{STR_START_WEB_SRV_FOR_UPLOAD, "启动Web服务器上传", "啟動Web伺服器上傳"});
        GLanguage.addString(STR_DOWN_APP_FROM_WEB, new String[]{STR_DOWN_APP_FROM_WEB, "从网站下载组件", "從網站下載組件"});
        GLanguage.addString(STR_DOWNLOAD, new String[]{"", "下载", "下載"});
        GLanguage.addString(STR_START, new String[]{"", "启动", "啟動"});
        GLanguage.addString(STR_STOP, new String[]{"", "停止", "停止"});
        GLanguage.addString(STR_WEB_LISTEN_ON, new String[]{STR_WEB_LISTEN_ON, "Web服务器临听 : ", "Web伺服器臨聽 : "});
        GLanguage.addString(STR_APP_LIST, new String[]{STR_APP_LIST, "组件列表 : ", "組件列表"});
        GLanguage.addString(STR_BACK, new String[]{STR_BACK, "返回", "返回"});
        GLanguage.addString(STR_RUN, new String[]{STR_RUN, "运行", "運行"});
        GLanguage.addString(STR_SET_AS_BOOT, new String[]{STR_SET_AS_BOOT, "置顶组件", "置頂組件"});
        GLanguage.addString(STR_UPGRADE, new String[]{STR_UPGRADE, "升级", "升級"});
        GLanguage.addString(STR_DELETE, new String[]{STR_DELETE, "删除", "刪除"});
        GLanguage.addString(STR_VERSION, new String[]{STR_VERSION, "版本: ", "版本: "});
        GLanguage.addString(STR_UPGRADE_URL, new String[]{STR_UPGRADE_URL, "升级地址: ", "升級地址: "});
        GLanguage.addString(STR_FILE_SIZE, new String[]{STR_FILE_SIZE, "文件大小: ", "文件大小: "});
        GLanguage.addString(STR_FILE_DATE, new String[]{STR_FILE_DATE, "文件日期: ", "文件日期: "});
        GLanguage.addString(STR_DESC, new String[]{STR_DESC, "描述: ", "描述: "});
        GLanguage.addString(STR_SERVER_STARTED, new String[]{STR_SERVER_STARTED, "服务器已启动", "伺服器已啟動"});
        GLanguage.addString(STR_SERVER_STOPED, new String[]{STR_SERVER_STOPED, "服务器已停止", "伺服器已停止"});
        GLanguage.addString(STR_UPLOAD_FILE, new String[]{STR_UPLOAD_FILE, "文件上传结束", "文件上傳結束"});
        GLanguage.addString(STR_SUCCESS, new String[]{STR_SUCCESS, "成功", "成功"});
        GLanguage.addString(STR_FAIL, new String[]{STR_FAIL, "失败", "失敗"});
        GLanguage.addString(STR_OPEN_APP_FAIL, new String[]{STR_OPEN_APP_FAIL, "打开组件失败", "打開組件失敗"});
        GLanguage.addString(STR_BRIGHT_STYLE, new String[]{STR_BRIGHT_STYLE, "浅色外观", "淺色外觀"});
        GLanguage.addString(STR_DARK_STYLE, new String[]{STR_DARK_STYLE, "深色外观", "深色外觀"});
        GLanguage.addString(STR_MESSAGE, new String[]{STR_MESSAGE, "信息", "信息"});
        GLanguage.addString(STR_CONFIRM_DELETE, new String[]{STR_CONFIRM_DELETE, "您要删除组件吗", "您要刪除組件嗎"});
        GLanguage.addString(STR_APP_NOT_RUNNING, new String[]{STR_APP_NOT_RUNNING, "组件没有运行", "組件沒有運行"});
        GLanguage.addString(STR_INSTALL_FROM_LOCAL, new String[]{STR_INSTALL_FROM_LOCAL, "选取文件安装", "選取檔案安裝"});
        GLanguage.addString(STR_SELECT_FILE, new String[]{"", "安装", "安裝"});
    }

    static AppManager instance = new AppManager();

    //    GApplication preApp;

    GForm mgrForm;

    GViewSlot mainSlot;

    //
    GList appList;
    GViewPort contentView;
    String curSelectedJarName;
    AppmEventHandler eventHandler;
    GHomeButton floatButton;

    MiniHttpServer webServer;
    List<MiniHttpClient> httpClients = new ArrayList<>();
    Map<String, GApplication> runningApps = new HashMap<>();
    public static final int RUNNING_ITEM_COLOR = 0x00cc00ff;

    GImage runningImg = GImage.createImageFromJar("/res/ui/green.png");

    GTextBox logBox;
    static PrintStream systemOutDefault = System.out;
    static PrintStream systemErrDefault = System.err;
    static InputStream systemInDefault = System.in;

    //the app would launch after Orientation set success
    Runnable delayLauncher = null;
    String appOri;
    //
    static final int PICK_PHOTO = 101, PICK_CAMERA = 102, PICK_QR = 103, PICK_HEAD = 104;

    static float devW, devH;

    static public AppManager getInstance() {
        return instance;
    }

    void active() {
//        if (webServer != null) {
//            webServer.stopServer();
//        }
        if (GCallBack.getInstance().getApplication() == this) return;

        System.setOut(systemOutDefault);
        System.setErr(systemErrDefault);
        System.setIn(systemInDefault);
        regStrings();
        GLanguage.setCurLang(AppLoader.getDefaultLang());
        GCallBack.getInstance().setApplication(this);
        Glfm.glfmSetSupportedInterfaceOrientation(GCallBack.getInstance().getDisplay(), Glfm.GLFMInterfaceOrientationPortrait);
        Glfm.glfmSetDisplayChrome(GCallBack.getInstance().getDisplay(), Glfm.GLFMUserInterfaceChromeNavigationAndStatusBar);
        if (curSelectedJarName != null) {
            updateContentViewInfo(curSelectedJarName);
        }
        mgrForm.setSize(GCallBack.getInstance().getDeviceWidth(), GCallBack.getInstance().getDeviceHeight());
        if (mainSlot != null) mainSlot.moveTo(0, 0);
        reloadAppList();
    }

    @Override
    public GForm getForm() {
        if (mgrForm != null) return mgrForm;
        mgrForm = new GForm(null) {

            @Override
            public void init() {
                super.init();

                final GCallBack ccb = GCallBack.getInstance();
                devW = ccb.getDeviceWidth();
                devH = ccb.getDeviceHeight();
                System.out.println("devW :" + devW + ", devH  :" + devH);

                GForm.hideKeyboard(this);
                regStrings();
                GLanguage.setCurLang(AppLoader.getDefaultLang());

                if (AppLoader.getGuiStyle() == 0) {
                    GToolkit.setStyle(new GStyleBright());
                } else {
                    GToolkit.setStyle(new GStyleDark());
                }

                setPickListener((uid, url, data) -> {
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
                add(getMainPanel(this));
            }


            @Override
            public boolean paint(long vg) {
                super.paint(vg);
                if (delayLauncher != null) {
                    GForm.flush();

                    String osname = System.getProperty("os.name");
                    if ("iOS".equals(osname) || "Android".equals(osname)) {
                        float w = GCallBack.getInstance().getDeviceWidth();
                        float h = GCallBack.getInstance().getDeviceHeight();
                        if ("h".equals(appOri)) {
                            if (w < h) return true;
                        } else {
                            if (h < w) return true;
                        }
                    }
                    try {
                        delayLauncher.run();
                    } catch (Exception e) {

                    }
                    delayLauncher = null;
                }

                return true;
            }
        };
        floatButton = new GHomeButton(mgrForm);
        mgrForm.add(floatButton);
        return mgrForm;
    }

    GContainer getMainPanel(GForm form) {

        String xmlStr = "";
        try {
            xmlStr = new String(GToolkit.readFileFromJar("/res/ui/AppManager.xml"), "utf-8");
        } catch (Exception e) {
        }

        UITemplate uit = new UITemplate(xmlStr);
        for (String s : uit.getVariable()) {
            uit.setVar(s, GLanguage.getString(s));
        }
        double[] inset = new double[4];
        Glfm.glfmGetDisplayChromeInsets(form.getWinContext(), inset);
        int h = (int) (inset[0] / GCallBack.getInstance().getDeviceRatio());
        System.out.println("STATEBAR_HEIGHT " + inset[0] + " , " + inset[1] + " , " + inset[2] + " , " + inset[3]);
        if (h <= 20) {
            h = 20;
        }
        uit.setVar("STATEBAR_HEIGHT", Integer.toString(h));


        eventHandler = new AppmEventHandler();
        XContainer xcon = (XContainer) XContainer.parseXml(uit.parse(), new XmlExtAssist(form));
        xcon.build((int) devW, (int) (devH), eventHandler);
        GContainer pan = xcon.getGui();
        mainSlot = pan.findByName("SLOT_MGR");
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
        System.out.println("downloadurl=" + url);
        if (url != null) GToolkit.setCompText(mainSlot, "INPUT_URL", url);

        mgrForm.setSizeChangeListener((width, height) -> xcon.reSize(width, height));
        reloadAppList();
        return pan;
    }

    class AppmEventHandler extends XEventHandler {
        @Override
        public void action(GObject gobj) {
            String name = gobj.getName();
            if (name == null) return;
            switch (name) {
                case "APP_DELETE_BTN":
                    String jarName;
                    if (curSelectedJarName != null) {
                        jarName = curSelectedJarName;
                        if (jarName != null) {
                            GFrame confirmFrame = GToolkit.getConfirmFrame(mgrForm, GLanguage.getString(STR_MESSAGE), GLanguage.getString(STR_CONFIRM_DELETE), GLanguage.getString(STR_DELETE), okgobj -> {
                                AppLoader.removeApp(jarName);
                                reloadAppList();
                                mainPanelShowLeft();
                                okgobj.getFrame().close();
                            }, GLanguage.getString("Cancel"), null, 300f, 180f);
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
                case "BT_BACK":
                    mainPanelShowLeft();
                    break;
                case "BT_BACK1":
                    mainSlot.moveTo(0, 0);
                    break;
                case "BT_STARTWEB":
                    GButton uploadbtn = (GButton) gobj;
                    GLabel uploadLab = (GLabel) mgrForm.findByName("LAB_WEBSRV");
                    if (webServer != null) {
                        webServer.stopServer();
                        webServer = null;
                    }
                    if (uploadbtn.getText().equals(GLanguage.getString(STR_STOP))) {
                        uploadbtn.setText(GLanguage.getString(STR_START));
                        uploadLab.setText(GLanguage.getString(STR_START_WEB_SRV_FOR_UPLOAD));
                        String s = GLanguage.getString(STR_SERVER_STOPED);
                        GForm.addMessage(s);
                        log(s);
                    } else {
                        webServer = new MiniHttpServer(MiniHttpServer.DEFAULT_PORT, srvLogger);
                        webServer.setUploadCompletedHandle(files -> {
                            for (MiniHttpServer.UploadFile f : files) {
                                String s = GLanguage.getString(STR_UPLOAD_FILE) + " " + f.filename + " " + f.data.length;
                                if (AppLoader.addApp(f.filename, f.data)) {
                                    GForm.addMessage(s + " " + GLanguage.getString(STR_SUCCESS));
                                    log(s);
                                } else {
                                    GForm.addMessage(s + " " + GLanguage.getString(STR_FAIL));
                                }
                            }
                            reloadAppList();
                        });
                        webServer.start();
                        uploadbtn.setText(GLanguage.getString(STR_STOP));
                        uploadLab.setText(GLanguage.getString(STR_WEB_LISTEN_ON) + webServer.getPort());
                        String s = GLanguage.getString(STR_SERVER_STARTED);
                        GForm.addMessage(s);
                        log(s);
                    }
                    break;
                case "APP_RUN_BTN":
                    if (curSelectedJarName != null) {
                        jarName = curSelectedJarName;
                        GListItem gli = getGListItemByAttachment(curSelectedJarName);

                        String orientation = AppLoader.getApplicationOrientation(jarName);
                        if (orientation.equals("h")) {
                            Glfm.glfmSetSupportedInterfaceOrientation(GCallBack.getInstance().getDisplay(), Glfm.GLFMInterfaceOrientationLandscapeLeft);
                        } else {
                            Glfm.glfmSetSupportedInterfaceOrientation(GCallBack.getInstance().getDisplay(), Glfm.GLFMInterfaceOrientationPortrait);
                        }
                        appOri = orientation;
                        delayLauncher = new Runnable() {
                            @Override
                            public void run() {
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
                                            GForm form = app.getForm();
                                            form.add(AppManager.getInstance().getFloatButton());
                                            updateContentViewInfo(jarName);
                                            gli.setColor(RUNNING_ITEM_COLOR);
                                        } else {
                                            GForm.addMessage(GLanguage.getString(AppManager.STR_OPEN_APP_FAIL) + ": " + jarName);
//                                            GForm.addMessage("Can't found startup class ,it setup in config.txt in jar root");
                                        }
                                        floatButton.checkLocation();
                                    }
                                }
                            }
                        };
                    }
                    break;
                case "APP_STOP_BTN":
                    if (curSelectedJarName != null) {
                        //AppLoader.setBootApp(curSelectedItem.getAttachment());
                        jarName = curSelectedJarName;
                        GApplication app = runningApps.get(jarName);
                        if (app != null) {
                            app.closeApp();
                            GForm.addMessage(GLanguage.getString(STR_SUCCESS));
                            GListItem gli = getGListItemByAttachment(curSelectedJarName);
                            gli.setColor(null);
                            updateContentViewInfo(jarName);
                        } else {
                            GForm.addMessage(GLanguage.getString(STR_APP_NOT_RUNNING));
                        }
                    }
                    break;
                case "APP_UPGRADE_BTN":
                    if (curSelectedJarName != null) {
                        jarName = curSelectedJarName;
                        if (jarName != null) {
                            url = AppLoader.getApplicationUpgradeurl(jarName);
                            if (url != null) {
                                hc = new MiniHttpClient(url, cltLogger, getDownloadCallback());
                                hc.start();
                                httpClients.add(hc);
                            }
                        }
                    }
                    break;
                case "BT_LOCALFILE":
                    GFrame chooser = GToolkit.getFileChooser(mgrForm, STR_INSTALL_FROM_LOCAL, null, new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.isDirectory() || file.getName().endsWith(".jar");
                        }
                    }, mgrForm.getDeviceWidth(), mgrForm.getDeviceHeight(), gobj1 -> {
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
                                    GForm.addMessage(GLanguage.getString(STR_SUCCESS) + " " + fullpath);
                                    reloadAppList();
                                } else {
                                    GForm.addMessage(GLanguage.getString(STR_FAIL) + " " + fullpath);
                                }
                            }
                        }
                    }, null);
                    mgrForm.add(chooser);
                    break;
                case "BT_SETTING":
                    mainSlot.moveTo(2, 0);
                    break;
                case "LI_ENG":
                    GLanguage.setCurLang(GLanguage.ID_ENG);
                    AppLoader.setDefaultLang(GLanguage.ID_ENG);
                    mgrForm = null;
                    break;
                case "LI_CHS":
                    GLanguage.setCurLang(GLanguage.ID_CHN);
                    AppLoader.setDefaultLang(GLanguage.ID_CHN);
                    mgrForm = null;
                    break;
                case "LI_CHT":
                    GLanguage.setCurLang(GLanguage.ID_CHT);
                    AppLoader.setDefaultLang(GLanguage.ID_CHT);
                    mgrForm = null;
                    break;
                case "LI_BRIGHT":
                    GToolkit.setStyle(new GStyleBright());
                    AppLoader.setGuiStyle(0);
                    break;
                case "LI_DARK":
                    GToolkit.setStyle(new GStyleDark());
                    AppLoader.setGuiStyle(1);
                    break;
            }
        }


        @Override
        public void onStateChange(GObject gobj) {
            String name = gobj.getName();
            if ("INPUT_SEARCH".equals(name)) {
                GTextObject search = (GTextObject) gobj;
                String str = search.getText();
                if (appList != null) {
                    appList.filterLabelWithKey(str);
                    //System.out.println("key=" + str);
                }
            }
        }
    }

    MiniHttpClient.DownloadCompletedHandle getDownloadCallback() {
        return (client, url, data) -> {
            httpClients.remove(client);
            log("Download success " + url + " ,size: " + data.length);
            GForm.addMessage((data == null ? GLanguage.getString(STR_FAIL) : GLanguage.getString(STR_SUCCESS)) + " " + GLanguage.getString(STR_DOWNLOAD) + " " + url);
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
                    GListItem item = new GListItem(mgrForm, img, name) {
                        public boolean paint(long vg) {
                            super.paint(vg);
//                            if (getLabel() != null && getAttachment().equals(AppLoader.getBootApp())) {
//                                GToolkit.drawRedPoint(vg, "v", getX() + getW() - 20, getY() + getH() * .5f, 10);
//                            }
                            if (runningApps.get(getAttachment()) != null) {
                                GToolkit.drawImage(vg, runningImg, getX() + getW() - 30, getY() + getH() * .5f - 6f, 12f, 12f, false, 0.8f);

                            }
                            return true;
                        }
                    };
                    if (img == null) {
                        item.setPreIcon("\uD83D\uDCD5");
                    }
                    item.setAttachment(appName);
                    if (runningApps.get(appName) != null) {
                        item.setColor(RUNNING_ITEM_COLOR);
                    }
                    appList.add(item);
                    item.setActionListener(gobj -> {
                        curSelectedJarName = ((GListItem) gobj).getAttachment();
                        updateContentViewInfo(appName);
                        mainPanelShowRight();
                    });
                }
            }
        }
        GForm.flush();
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
        String txt = GLanguage.getString(STR_VERSION) + "\n  " + AppLoader.getApplicationVersion(jarName) + "\n" + GLanguage.getString(STR_FILE_SIZE) + "\n  " + AppLoader.getApplicationFileSize(jarName) + "\n" + GLanguage.getString(STR_FILE_DATE) + "\n  " + dStr + "\n" + GLanguage.getString(STR_UPGRADE_URL) + "\n  " + AppLoader.getApplicationUpgradeurl(jarName) + "\n" + GLanguage.getString(STR_DESC) + "\n  " + AppLoader.getApplicationDesc(jarName) + "\n";
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
        mainSlot.moveTo(0, 200);
    }

    void mainPanelShowRight() {
        mainSlot.moveTo(1, 200);
    }

    GHomeButton getFloatButton() {
        return floatButton;
    }

    MiniHttpServer getWebServer() {
        return webServer;
    }

    List<MiniHttpClient> getHttpClients() {
        return httpClients;
    }

    void addRunningApp(GApplication app) {
        if (app == null) return;
        runningApps.put(app.getJarName(), app);
    }

    void removeRunningApp(GApplication app) {
        if (app == null) return;
        runningApps.remove(app.getJarName());
    }

    MiniHttpServer.SrvLogger srvLogger = new MiniHttpServer.SrvLogger() {
        @Override
        void log(String s) {
            AppManager.log(s);
        }
    };

    MiniHttpClient.CltLogger cltLogger = new MiniHttpClient.CltLogger() {
        @Override
        void log(String s) {
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
}
