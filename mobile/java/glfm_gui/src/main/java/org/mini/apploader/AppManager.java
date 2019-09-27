/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.gui.*;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GKeyboardShowListener;
import org.mini.gui.event.GPhotoPickedListener;
import org.mini.gui.event.GStateChangeListener;
import org.mini.guijni.GuiCallBack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 *
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
    static final String STR_DESC = "Description: ";
    static final String STR_SERVER_STARTED = "Webserver started";
    static final String STR_SERVER_STOPED = "Webserver stoped";
    static final String STR_UPLOAD_FILE = "Uploaded file";
    static final String STR_SUCCESS = "Success";
    static final String STR_FAIL = "Fail";

    static {
        GLanguage.addString(STR_EXIT, new String[]{STR_EXIT, "退出", "退出"});
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
        GLanguage.addString(STR_DESC, new String[]{STR_DESC, "描述: ", "描述: "});
        GLanguage.addString(STR_SERVER_STARTED, new String[]{STR_SERVER_STARTED, "服务器已启动", "伺服器已啟動"});
        GLanguage.addString(STR_SERVER_STOPED, new String[]{STR_SERVER_STOPED, "服务器已停止", "伺服器已停止"});
        GLanguage.addString(STR_UPLOAD_FILE, new String[]{STR_UPLOAD_FILE, "文件上传结束", "文件上傳結束"});
        GLanguage.addString(STR_SUCCESS, new String[]{STR_SUCCESS, "成功", "成功"});
        GLanguage.addString(STR_FAIL, new String[]{STR_FAIL, "失敗", "失败"});
    }

    static AppManager instance = new AppManager();

    GApplication preApp;

    GForm mgrForm;
    GMenu menu;

    GPanel mainPanel;
    GImage logoImg;
    //
    GList appList;
    GViewPort contentView;
    GListItem curSelectedItem;

    MiniHttpServer webServer;

    //
    static final int PICK_PHOTO = 101, PICK_CAMERA = 102, PICK_QR = 103, PICK_HEAD = 104;

    static float menuH = 60, pad = 4, inputH = 60, addW = 60, addH = 30, sendW = 60, devW, devH;

    static public AppManager getInstance() {
        return instance;
    }

    public void active() {
        if (GuiCallBack.getInstance().getApplication() != this) {
            preApp = GuiCallBack.getInstance().getApplication();
        }
        if (webServer != null) {
            webServer.stopServer();
        }
        GuiCallBack.getInstance().setApplication(this);
        reloadAppList();
    }

    @Override
    public GForm getForm(GApplication app) {
        mgrForm = new GForm() {

            @Override
            public void init() {
                super.init();

                final GuiCallBack ccb = GuiCallBack.getInstance();
                devW = ccb.getDeviceWidth();
                devH = ccb.getDeviceHeight();
                //System.out.println("devW , devH " + devW + " , " + devH);

                GForm.hideKeyboard();
                GLanguage.setCurLang(AppLoader.getDefaultLang());

                setFps(30f);

                logoImg = GImage.createImageFromJar("/res/img/logo128.png");

                setPickListener(new GPhotoPickedListener() {
                    @Override
                    public void onPicked(int uid, String url, byte[] data) {
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
                    }
                });

                add(getMainPanel());

            }
        };
        return mgrForm;
    }

    GPanel getMainPanel() {

        mainPanel = new GPanel();
        mainPanel.setLocation(0, 0);
        mainPanel.setSize(devW * 2, devH);
        mgrForm.add(mainPanel);

        float y = pad;
        GButton exitbtn = new GButton(GLanguage.getString(STR_EXIT), pad, pad, addW, addH);
        mainPanel.add(exitbtn);
        exitbtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (preApp != null) {
                    GuiCallBack.getInstance().setApplication(preApp);
                }
            }
        });
        y += exitbtn.getH() + pad;

        GList langList = new GList(pad, y, devW - pad * 2, 35);
        //langList.setShowMode(GList.MODE_MULTI_SHOW);
        langList.setBgColor(GToolkit.getStyle().getFrameBackground());
        GListItem item;
        item = new GListItem(null, "English");
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GLanguage.setCurLang(GLanguage.ID_ENG);
                AppLoader.setDefaultLang(GLanguage.ID_ENG);
                AppManager.getInstance().active();
            }
        });
        langList.addItem(item);
        item = new GListItem(null, "简体中文");
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GLanguage.setCurLang(GLanguage.ID_CHN);
                AppLoader.setDefaultLang(GLanguage.ID_CHN);
                AppManager.getInstance().active();
            }
        });
        langList.addItem(item);
        item = new GListItem(null, "繁體中文");
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GLanguage.setCurLang(GLanguage.ID_CHT);
                AppLoader.setDefaultLang(GLanguage.ID_CHT);
                AppManager.getInstance().active();
            }
        });
        langList.addItem(item);
        mainPanel.add(langList);
        langList.setSelectedIndex(AppLoader.getDefaultLang());
        y += 35 + pad;

        GLabel downLab = new GLabel(GLanguage.getString(STR_DOWN_APP_FROM_WEB), pad, y, devW - pad * 2, addH);
        downLab.setAlign(GGraphics.LEFT | GGraphics.VCENTER);
        mainPanel.add(downLab);
        y += downLab.getH() + pad;

        GTextField downtextfd = new GTextField("http://bb.egls.cn:8080/down/BiBiX.jar", "application jar url", pad, y, devW - pad * 3 - addW * 2, addH);
        mainPanel.add(downtextfd);
        GButton downbtn = new GButton(GLanguage.getString(STR_DOWNLOAD), devW - addW * 2 - pad, y, addW * 2, addH);
        mainPanel.add(downbtn);
        downbtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                String url = downtextfd.getText();

                MiniHttpClient hc = new MiniHttpClient(url, getDownloadCallback());
                hc.start();
            }
        });
        y += addH + pad;
        GLabel uploadLab = new GLabel(GLanguage.getString(STR_START_WEB_SRV_FOR_UPLOAD), pad, y, devW - pad * 3 - addW * 2, addH);
        uploadLab.setAlign(GGraphics.LEFT | GGraphics.VCENTER);
        mainPanel.add(uploadLab);

        GButton uploadbtn = new GButton(GLanguage.getString(STR_START), devW - addW * 2 - pad, y, addW * 2, addH);
        mainPanel.add(uploadbtn);
        uploadbtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (webServer != null) {
                    webServer.stopServer();
                }
                if (uploadbtn.getText().equals(GLanguage.getString(STR_STOP))) {
                    uploadbtn.setText(GLanguage.getString(STR_START));
                    uploadLab.setText(GLanguage.getString(STR_START_WEB_SRV_FOR_UPLOAD));
                    GForm.addMessage(GLanguage.getString(STR_SERVER_STOPED));
                } else {
                    webServer = new MiniHttpServer();
                    webServer.setUploadCompletedHandle(new MiniHttpServer.UploadCompletedHandle() {
                        @Override
                        public void onCompleted(List<MiniHttpServer.UploadFile> files) {
                            for (MiniHttpServer.UploadFile f : files) {
                                AppLoader.addApp(f.filename, f.data);
                                GForm.addMessage(GLanguage.getString(STR_UPLOAD_FILE) + " " + f.filename);
                            }
                            reloadAppList();
                        }
                    });
                    webServer.start();
                    uploadbtn.setText(GLanguage.getString(STR_STOP));
                    uploadLab.setText(GLanguage.getString(STR_WEB_LISTEN_ON) + webServer.getPort());
                    GForm.addMessage(GLanguage.getString(STR_SERVER_STARTED));
                }
            }
        });
        y += addH + pad;

        GLabel appsLab = new GLabel(GLanguage.getString(STR_APP_LIST), pad, y, devW - pad * 3 - addW * 2, addH);
        appsLab.setAlign(GGraphics.LEFT | GGraphics.VCENTER);
        mainPanel.add(appsLab);
        y += addH + pad;

        GTextField search = new GTextField("", "Search", pad, y, devW - pad * 2, addH);
        search.setBoxStyle(GTextField.BOX_STYLE_SEARCH);
        search.setStateChangeListener(new GStateChangeListener() {
            @Override
            public void onStateChange(GObject gobj) {
                String str = search.getText();
                if (appList != null) {
                    appList.filterLabelWithKey(str);
                    //System.out.println("key=" + str);
                }
            }
        });
        mainPanel.add(search);
        y += addH + pad;

        appList = new GList(0, y, devW, devH - y);
        appList.setShowMode(GList.MODE_MULTI_SHOW);
        appList.setItemHeight(50);
        mainPanel.add(appList);

        //right
        float x1 = devW + pad;
        y = pad;
        GButton back2listBtn = new GButton("< " + GLanguage.getString(STR_BACK), x1, y, addW, addH);
        mainPanel.add(back2listBtn);
        back2listBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                mainPanelShowLeft();
            }
        });
        y += addH + pad;

        contentView = new GViewPort();
        contentView.setLocation(devW, y);
        contentView.setSize(devW, devH - y);
        buildContentView();
        mainPanel.add(contentView);

        mgrForm.setKeyshowListener(new GKeyboardShowListener() {
            @Override
            public void keyboardShow(boolean show, float kx, float ky, float kw, float kh) {
                if (show) {
                } else {
                }
                GObject.flush();
            }
        });

        reloadAppList();

        return mainPanel;
    }

    void buildContentView() {
        float y = pad, bigBtnH = 35;
        GLabel nameLab = new GLabel("", pad, y, devW - pad * 2, bigBtnH);
        nameLab.setAlign(GGraphics.HCENTER | GGraphics.VCENTER);
        nameLab.setName(APP_NAME_LABEL);
        contentView.add(nameLab);
        y += nameLab.getH() + pad;

        GImage img = curSelectedItem == null ? null : curSelectedItem.getImg();
        GImageItem imgItem = new GImageItem(img);
        imgItem.setName(APP_ICON_ITEM);
        imgItem.setLocation(pad, y);
        imgItem.setSize(128, 128);
        contentView.add(imgItem);
        y += imgItem.getH() + pad;

        GTextBox resume = new GTextBox("", "", pad, y, devW - pad * 2, 150);
        resume.setName(APP_DESC_LABEL);
        resume.setEditable(false);
        contentView.add(resume);
        y += resume.getH() + pad;

        GButton runBtn = new GButton(GLanguage.getString(STR_RUN), pad, y, devW - pad * 2, bigBtnH);
        runBtn.setName(APP_RUN_BTN);
        contentView.add(runBtn);
        runBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        AppLoader.runApp(appName);
                    }
                }
            }
        });
        y += bigBtnH + pad;

        GButton defaultBtn = new GButton(GLanguage.getString(STR_SET_AS_BOOT), pad, y, devW - pad * 2, bigBtnH);
        contentView.add(defaultBtn);
        defaultBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    AppLoader.setBootApp(curSelectedItem.getLabel());
                }
            }
        });
        y += bigBtnH + pad;

        GButton upgradeBtn = new GButton(GLanguage.getString(STR_UPGRADE), pad, y, devW - pad * 2, bigBtnH);
        upgradeBtn.setName(APP_UPGRADE_BTN);
        contentView.add(upgradeBtn);
        upgradeBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        String url = AppLoader.getApplicationUpgradeurl(appName);
                        if (url != null) {
                            MiniHttpClient hc = new MiniHttpClient(url, getDownloadCallback());
                            hc.start();
                        }
                    }
                }
            }
        });
        y += bigBtnH + pad;

        GButton deleteBtn = new GButton(GLanguage.getString(STR_DELETE), pad, y, devW - pad * 2, bigBtnH);
        deleteBtn.setName(APP_DELETE_BTN);
        deleteBtn.setBgColor(128, 16, 8, 255);
        contentView.add(deleteBtn);
        deleteBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        AppLoader.removeApp(appName);
                        reloadAppList();
                        mainPanelShowLeft();
                    }
                }

            }
        });
        y += bigBtnH + pad;
    }

    MiniHttpClient.DownloadCompletedHandle getDownloadCallback() {
        return new MiniHttpClient.DownloadCompletedHandle() {
            @Override
            public void onCompleted(String url, byte[] data) {
                //System.out.println("download success " + url + " ,size: " + data.length);
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
                //updateContentViewInfo(jarName);
            }
        };
    }

    void reloadAppList() {
        if (appList == null) {
            return;
        }
        appList.removeItemAll();
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
                        public boolean update(long vg) {
                            super.update(vg);
                            if (getLabel() != null && getLabel().equals(AppLoader.getBootApp())) {
                                GToolkit.drawRedPoint(vg, "v", getX() + getW() - 20, getY() + getH() * .5f, 10);
                            }
                            return true;
                        }
                    };
                    appList.addItem(item);
                    item.setActionListener(new GActionListener() {
                        @Override
                        public void action(GObject gobj) {
                            curSelectedItem = (GListItem) gobj;
                            updateContentViewInfo(appName);
                            mainPanelShowRight();
                        }
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

        String txt = GLanguage.getString(STR_VERSION) + "\n  " + AppLoader.getApplicationVersion(appName) + "\n"
                + GLanguage.getString(STR_FILE_SIZE) + "\n  " + AppLoader.getApplicationFileSize(appName) + "\n"
                + GLanguage.getString(STR_UPGRADE_URL) + "\n  " + AppLoader.getApplicationUpgradeurl(appName) + "\n"
                + GLanguage.getString(STR_DESC) + "\n  " + AppLoader.getApplicationDesc(appName) + "\n";
        descLab.setText(txt);

        //re set image
        GImageItem icon = (GImageItem) contentView.findByName(APP_ICON_ITEM);
        icon.setImg(curSelectedItem.getImg());
        contentView.reBoundle();
    }

    void mainPanelShowLeft() {
        float panelX = mainPanel.getX();
        float panelY = mainPanel.getY();
        mgrForm.inertiaEvent(panelX, panelY, panelX + 200, panelY, 100);
        if (curSelectedItem != null) {
            appList.setSelectedIndex(-1);
            curSelectedItem = null;
        }
    }

    void mainPanelShowRight() {
        float panelX = mainPanel.getX();
        float panelY = mainPanel.getY();
        mgrForm.inertiaEvent(panelX + 200, panelY, panelX, panelY, 100);
    }

}
