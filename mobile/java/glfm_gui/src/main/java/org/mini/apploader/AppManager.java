/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.gui.*;
import org.mini.layout.UITemplate;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.XViewSlot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * @author Gust
 */
public class AppManager extends GApplication {

    static final String APP_NAME_LABEL = "APP_NAME_LABEL";
    static final String APP_ICON_ITEM = "APP_ICON_ITEM";
    static final String APP_RUN_BTN = "APP_RUN_BTN";
    static final String APP_DESC_LABEL = "APP_DESC_LABEL";
    static final String APP_UPGRADE_BTN = "APP_UPGRADE_BTN";
    static final String APP_DELETE_BTN = "APP_DELETE_BTN";

    static final String STR_EXIT = "Exit";
    static final String STR_SETTING = "Setting";
    static final String STR_TITLE = "Application Manager";
    static final String STR_START_WEB_SRV_FOR_UPLOAD = "Lan webserver for upload jar";
    static final String STR_DOWN_APP_FROM_WEB = "Download app from website:";
    static final String STR_DOWNLOAD = "Download";
    static final String STR_START = "Start";
    static final String STR_STOP = "Stop";
    static final String STR_WEB_LISTEN_ON = "Lan webserver on:";
    static final String STR_APP_LIST = "Application list : ";
    static final String STR_BACK = "Back";
    static final String STR_RUN = "Run";
    static final String STR_SET_AS_BOOT = "Set as boot app";
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
    static final String STR_OPEN_APP_FAIL = "Open app failed";
    static final String STR_BRIGHT_STYLE = "Bright appearance";
    static final String STR_DARK_STYLE = "Dark appearance";
    static final String STR_MESSAGE = "Message";

    static {
        GLanguage.addString(STR_SETTING, new String[]{STR_SETTING, "设置", "设置"});
        GLanguage.addString(STR_EXIT, new String[]{STR_EXIT, "退出", "退出"});
        GLanguage.addString(STR_TITLE, new String[]{STR_TITLE, "APP 管理器", "APP 管理器"});
        GLanguage.addString(STR_START_WEB_SRV_FOR_UPLOAD, new String[]{STR_START_WEB_SRV_FOR_UPLOAD, "启动Web服务器上传", "啟動Web伺服器上傳"});
        GLanguage.addString(STR_DOWN_APP_FROM_WEB, new String[]{STR_DOWN_APP_FROM_WEB, "从网站下载App", "從網站下載App"});
        GLanguage.addString(STR_DOWNLOAD, new String[]{STR_DOWNLOAD, "下载", "下載"});
        GLanguage.addString(STR_START, new String[]{STR_START, "启动", "啟動"});
        GLanguage.addString(STR_STOP, new String[]{STR_STOP, "停止", "停止"});
        GLanguage.addString(STR_WEB_LISTEN_ON, new String[]{STR_WEB_LISTEN_ON, "Web服务器临听 : ", "Web伺服器臨聽 : "});
        GLanguage.addString(STR_APP_LIST, new String[]{STR_APP_LIST, "App 列表 : ", "App 列表"});
        GLanguage.addString(STR_BACK, new String[]{STR_BACK, "返回", "返回"});
        GLanguage.addString(STR_RUN, new String[]{STR_RUN, "运行", "運行"});
        GLanguage.addString(STR_SET_AS_BOOT, new String[]{STR_SET_AS_BOOT, "设为启动App", "設為啟動App"});
        GLanguage.addString(STR_UPGRADE, new String[]{STR_UPGRADE, "升级", "升級"});
        GLanguage.addString(STR_DELETE, new String[]{STR_DELETE, "删除", "刪除"});
        GLanguage.addString(STR_VERSION, new String[]{STR_VERSION, "版本: ", "版本: "});
        GLanguage.addString(STR_UPGRADE_URL, new String[]{STR_UPGRADE_URL, "升级地址: ", "升級地址: "});
        GLanguage.addString(STR_FILE_SIZE, new String[]{STR_FILE_SIZE, "文件大小: ", "文件大小: "});
        GLanguage.addString(STR_FILE_SIZE, new String[]{STR_FILE_SIZE, "文件日期: ", "文件日期: "});
        GLanguage.addString(STR_DESC, new String[]{STR_DESC, "描述: ", "描述: "});
        GLanguage.addString(STR_SERVER_STARTED, new String[]{STR_SERVER_STARTED, "服务器已启动", "伺服器已啟動"});
        GLanguage.addString(STR_SERVER_STOPED, new String[]{STR_SERVER_STOPED, "服务器已停止", "伺服器已停止"});
        GLanguage.addString(STR_UPLOAD_FILE, new String[]{STR_UPLOAD_FILE, "文件上传结束", "文件上傳結束"});
        GLanguage.addString(STR_SUCCESS, new String[]{STR_SUCCESS, "成功", "成功"});
        GLanguage.addString(STR_FAIL, new String[]{STR_FAIL, "失败", "失敗"});
        GLanguage.addString(STR_OPEN_APP_FAIL, new String[]{STR_OPEN_APP_FAIL, "打开应用失败", "打開應用失敗"});
        GLanguage.addString(STR_BRIGHT_STYLE, new String[]{STR_BRIGHT_STYLE, "浅色外观", "淺色外觀"});
        GLanguage.addString(STR_DARK_STYLE, new String[]{STR_DARK_STYLE, "深色外观", "深色外觀"});
        GLanguage.addString(STR_MESSAGE, new String[]{STR_MESSAGE, "信息", "信息"});
    }

    static AppManager instance = new AppManager();

//    GApplication preApp;

    GForm mgrForm;

    GViewSlot mainSlot;

    //
    GList appList;
    GViewPort contentView;
    GListItem curSelectedItem;
    AppmEventHandler eventHandler;

    MiniHttpServer webServer;

    GTextBox logBox;

    GStyle style;

    //
    static final int PICK_PHOTO = 101, PICK_CAMERA = 102, PICK_QR = 103, PICK_HEAD = 104;

    static float devW, devH;

    static public AppManager getInstance() {
        return instance;
    }

    public void active() {
        if (webServer != null) {
            webServer.stopServer();
        }
        if (style == null) {
            style = GToolkit.getStyle();
        }
        GToolkit.setStyle(style);
        GCallBack.getInstance().setApplication(this);
        reloadAppList();
    }

    @Override
    public GForm getForm() {
        mgrForm = new GForm() {

            @Override
            public void init() {
                super.init();

                final GCallBack ccb = GCallBack.getInstance();
                devW = ccb.getDeviceWidth();
                devH = ccb.getDeviceHeight();
                System.out.println("devW :" + devW + ", devH  :" + devH);

                GForm.hideKeyboard();
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

                add(getMainSlot());

            }
        };
        return mgrForm;
    }

    GViewSlot getMainSlot() {

        String xmlStr = "";
        try {
            xmlStr = new String(GToolkit.readFileFromJar("/res/ui/AppManager.xml"), "utf-8");
        } catch (Exception e) {
        }

        UITemplate uit = new UITemplate(xmlStr);
        for (String s : uit.getVariable()) {
            uit.setVar(s, GLanguage.getString(s));
        }

        eventHandler = new AppmEventHandler();
        XContainer container = (XViewSlot) XContainer.parseXml(uit.parse());
        container.build((int) devW, (int) (devH), eventHandler);
        mainSlot = (GViewSlot) (container.getGui());
        appList = (GList) mainSlot.findByName("LIST_APP");
        contentView = (GViewPort) mainSlot.findByName("VP_CONTENT");
        logBox = mainSlot.findByName("INPUT_LOG");
        GList langList = (GList) mainSlot.findByName("LIST_LANG");
        langList.setSelectedIndex(AppLoader.getDefaultLang());
        GList styleList = (GList) mainSlot.findByName("LIST_STYLE");
        if (GToolkit.getStyle() instanceof GStyleBright) {
            styleList.setSelectedIndex(0);
        } else {
            styleList.setSelectedIndex(1);
        }
        String url = AppLoader.getDownloadUrl();
        System.out.println("downloadurl=" + url);
        if (url != null) GToolkit.setCompText(mainSlot, "INPUT_URL", url);

        mgrForm.setSizeChangeListener((width, height) -> container.reSize(width, height));
        reloadAppList();
        return this.mainSlot;
    }

    class AppmEventHandler extends XEventHandler {
        @Override
        public void action(GObject gobj, String cmd) {
            String name = gobj.getName();
            if ("APP_DELETE_BTN".equals(name)) {
                String appName = curSelectedItem.getLabel();
                AppLoader.removeApp(appName);
                mainPanelShowLeft();
                reloadAppList();
            } else if ("BT_DOWN".equals(name)) {
                String url = GToolkit.getCompText("INPUT_URL");
                AppLoader.setDownloadUrl(url);

                MiniHttpClient hc = new MiniHttpClient(url, cltLogger, getDownloadCallback());
                hc.start();
            } else if ("BT_BACK".equals(name)) {
                mainPanelShowLeft();
            } else if ("BT_BACK1".equals(name)) {
                mainSlot.moveTo(0, 0);
            } else if ("BT_STARTWEB".equals(name)) {
                GButton uploadbtn = (GButton) gobj;
                GLabel uploadLab = (GLabel) mgrForm.findByName("LAB_WEBSRV");
                if (webServer != null) {
                    webServer.stopServer();
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
                            AppLoader.addApp(f.filename, f.data);
                            String s = GLanguage.getString(STR_UPLOAD_FILE) + " " + f.filename + " " + f.data.length;
                            GForm.addMessage(s);
                            log(s);
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
            } else if ("APP_RUN_BTN".equals(name)) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        AppLoader.runApp(appName);
                    }
                }
            } else if ("APP_SET_BOOT_BTN".equals(name)) {
                if (curSelectedItem != null) {
                    AppLoader.setBootApp(curSelectedItem.getLabel());
                }
            } else if ("APP_UPGRADE_BTN".equals(name)) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        String url = AppLoader.getApplicationUpgradeurl(appName);
                        if (url != null) {
                            MiniHttpClient hc = new MiniHttpClient(url, cltLogger, getDownloadCallback());
                            hc.start();
                        }
                    }
                }
            } else if ("APP_DELETE_BTN".equals(name)) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        AppLoader.removeApp(appName);
                        reloadAppList();
                        mainPanelShowLeft();
                    }
                }
            } else if ("BT_SETTING".equals(name)) {
                mainSlot.moveTo(2, 0);
            } else if ("LI_ENG".equals(name)) {
                GLanguage.setCurLang(GLanguage.ID_ENG);
                AppLoader.setDefaultLang(GLanguage.ID_ENG);
                AppManager.getInstance().active();
            } else if ("LI_CHS".equals(name)) {
                GLanguage.setCurLang(GLanguage.ID_CHN);
                AppLoader.setDefaultLang(GLanguage.ID_CHN);
                AppManager.getInstance().active();
            } else if ("LI_CHT".equals(name)) {
                GLanguage.setCurLang(GLanguage.ID_CHT);
                AppLoader.setDefaultLang(GLanguage.ID_CHT);
                AppManager.getInstance().active();
            } else if ("LI_BRIGHT".equals(name)) {
                GToolkit.setStyle(new GStyleBright());
                AppLoader.setGuiStyle(0);
                instance = new AppManager();
                active();
            } else if ("LI_DARK".equals(name)) {
                GToolkit.setStyle(new GStyleDark());
                AppLoader.setGuiStyle(1);
                instance = new AppManager();
                active();
            }
        }


        public void onStateChange(GObject gobj, String cmd) {
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
        return (url, data) -> {
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
                    GListItem item = new GListItem(img, appName) {
                        public boolean paint(long vg) {
                            super.paint(vg);
                            if (getLabel() != null && getLabel().equals(AppLoader.getBootApp())) {
                                GToolkit.drawRedPoint(vg, "v", getX() + getW() - 20, getY() + getH() * .5f, 10);
                            }
                            return true;
                        }
                    };
                    appList.add(item);
                    item.setActionListener(gobj -> {
                        curSelectedItem = (GListItem) gobj;
                        updateContentViewInfo(appName);
                        mainPanelShowRight();
                    });
                }
            }
        }
        GForm.flush();
    }

    void updateContentViewInfo(String appName) {
        GLabel nameLab = (GLabel) contentView.findByName(APP_NAME_LABEL);
        nameLab.setText(AppLoader.getApplicationName(appName));
        //
        GTextBox descLab = (GTextBox) contentView.findByName(APP_DESC_LABEL);
        String dStr = "-";
        long d = AppLoader.getApplicationFileDate(appName);
        if (d > 0) {
            dStr = getDateString(d);
        }
        String txt = GLanguage.getString(STR_VERSION) + "\n  " + AppLoader.getApplicationVersion(appName) + "\n"
                + GLanguage.getString(STR_FILE_SIZE) + "\n  " + AppLoader.getApplicationFileSize(appName) + "\n"
                + GLanguage.getString(STR_FILE_DATE) + "\n  " + dStr + "\n"
                + GLanguage.getString(STR_UPGRADE_URL) + "\n  " + AppLoader.getApplicationUpgradeurl(appName) + "\n"
                + GLanguage.getString(STR_DESC) + "\n  " + AppLoader.getApplicationDesc(appName) + "\n";
        descLab.setText(txt);

        //re set image
        GImageItem icon = (GImageItem) contentView.findByName(APP_ICON_ITEM);
        if (curSelectedItem != null) icon.setImg(curSelectedItem.getImg());
        contentView.reSize();
    }

    void mainPanelShowLeft() {
        if (curSelectedItem != null) {
            appList.setSelectedIndex(-1);
            curSelectedItem = null;
        }
        mainSlot.moveTo(0, 200);
    }

    void mainPanelShowRight() {
        mainSlot.moveTo(1, 200);
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
            box.flush();
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
        ret += minute < 10 ? "0" + hour : String.valueOf(minute);
        ret += ":";
        ret += seconds < 10 ? "0" + hour : String.valueOf(seconds);
        return ret;

    }
}
