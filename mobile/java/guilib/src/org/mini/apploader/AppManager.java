/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.mini.glfm.Glfm;
import org.mini.gui.GButton;
import org.mini.gui.GForm;
import org.mini.gui.GGraphics;
import org.mini.gui.GImage;
import org.mini.gui.GImageItem;
import org.mini.gui.GLabel;
import org.mini.gui.GLanguage;
import org.mini.gui.GList;
import org.mini.gui.GListItem;
import org.mini.gui.GMenu;
import org.mini.gui.GObject;
import org.mini.gui.GPanel;
import org.mini.gui.GTextBox;
import org.mini.gui.GTextField;
import org.mini.gui.GToolkit;
import org.mini.gui.GViewPort;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GKeyboardShowListener;
import org.mini.gui.event.GPhotoPickedListener;
import org.mini.gui.impl.GuiCallBack;

/**
 *
 * @author Gust
 */
public class AppManager {

    static final String APP_NAME_LABEL = "APP_NAME_LABEL";
    static final String APP_ICON_ITEM = "APP_ICON_ITEM";
    static final String APP_RUN_BTN = "APP_RUN_BTN";
    static final String APP_DESC_LABEL = "APP_DESC_LABEL";
    static final String APP_UPGRADE_BTN = "APP_UPGRADE_BTN";
    static final String APP_DELETE_BTN = "APP_DELETE_BTN";

    static final String note = "Start Webserver for upload jar";

    static {
        GLanguage.addString("Exit", new String[]{"Exit", "退出", "退出"});
        GLanguage.addString("Start Webserver for upload jar", new String[]{"Start Webserver for upload jar", "启动Web服务器上传", "啟動Web伺服器上傳"});
        GLanguage.addString("Download app from website:", new String[]{"Download app from website:", "从网站下载App", "從網站下載App"});
        GLanguage.addString("Download", new String[]{"Download", "下载", "下載"});
        GLanguage.addString("Start", new String[]{"Start", "启动", "啟動"});
        GLanguage.addString("Stop", new String[]{"Stop", "停止", "停止"});
        GLanguage.addString("Webserver listen on : ", new String[]{"Webserver listen on : ", "Web服务器临听 : ", "Web伺服器臨聽 : "});
        GLanguage.addString("Application list : ", new String[]{"Application list : ", "App 列表 : ", "App 列表"});
        GLanguage.addString("Back", new String[]{"Back", "返回", "返回"});
        GLanguage.addString("Run", new String[]{"Run", "运行", "運行"});
        GLanguage.addString("Set as boot app", new String[]{"Set as boot app", "设为启动App", "設為啟動App"});
        GLanguage.addString("Upgrade", new String[]{"Upgrade", "升级", "升級"});
        GLanguage.addString("Delete", new String[]{"Delete", "删除", "刪除"});
        GLanguage.addString("Version: ", new String[]{"Version: ", "版本: ", "版本: "});
        GLanguage.addString("Upgrade url: ", new String[]{"Upgrade url: ", "升级地址: ", "升級地址: "});
        GLanguage.addString("File size: ", new String[]{"File size: ", "文件大小: ", "文件大小: "});
        GLanguage.addString("Description: ", new String[]{"Description: ", "描述: ", "描述: "});
    }

    static AppManager instance = new AppManager();

    GForm preForm;

    GForm form;
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
        preForm = GuiCallBack.getInstance().getForm();
        if (preForm == form) {
            preForm = null;
        }
        form = new GForm(GuiCallBack.getInstance()) {

            @Override
            public void init() {
                super.init();

                final GuiCallBack ccb = GuiCallBack.getInstance();
                devW = ccb.getDeviceWidth();
                devH = ccb.getDeviceHeight();
                //System.out.println("devW , devH " + devW + " , " + devH);

                Glfm.glfmSetKeyboardVisible(ccb.getDisplay(), false);
                GLanguage.setCurLang(GlfmMain.getDefaultLang());

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
        GuiCallBack.getInstance().setForm(form);
        reloadAppList();
    }

    GPanel getMainPanel() {

        mainPanel = new GPanel();
        mainPanel.setLocation(0, 0);
        mainPanel.setSize(devW * 2, devH);
        form.add(mainPanel);

        float y = pad;
        GButton exitbtn = new GButton(GLanguage.getString("Exit"), pad, pad, addW, addH);
        mainPanel.add(exitbtn);
        exitbtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (preForm != null) {
                    GuiCallBack.getInstance().setForm(preForm);
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
                GlfmMain.setDefaultLang(GLanguage.ID_ENG);
                AppManager.getInstance().active();
            }
        });
        langList.addItem(item);
        item = new GListItem(null, "简体中文");
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GLanguage.setCurLang(GLanguage.ID_CHN);
                GlfmMain.setDefaultLang(GLanguage.ID_CHN);
                AppManager.getInstance().active();
            }
        });
        langList.addItem(item);
        item = new GListItem(null, "繁體中文");
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GLanguage.setCurLang(GLanguage.ID_CHT);
                GlfmMain.setDefaultLang(GLanguage.ID_CHT);
                AppManager.getInstance().active();
            }
        });
        langList.setSelectedIndex(GlfmMain.getDefaultLang());
        langList.addItem(item);
        mainPanel.add(langList);
        y += 35 + pad;

        GLabel downLab = new GLabel(GLanguage.getString("Download app from website:"), pad, y, devW - pad * 2, addH);
        downLab.setAlign(GGraphics.LEFT | GGraphics.VCENTER);
        mainPanel.add(downLab);
        y += downLab.getH() + pad;

        GTextField downtextfd = new GTextField("http://", "application jar url", pad, y, devW - pad * 3 - addW * 2, addH);
        mainPanel.add(downtextfd);
        GButton downbtn = new GButton(GLanguage.getString("Download"), devW - addW * 2 - pad, y, addW * 2, addH);
        mainPanel.add(downbtn);
        downbtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                String url = downtextfd.getText();

                MiniHttpClient hc = new MiniHttpClient(url, new MiniHttpClient.DownloadCompletedHandle() {
                    @Override
                    public void onCompleted(String url, byte[] data) {
                        System.out.println("download success " + url + " ,size: " + data.length);
                        String jarName = null;
                        if (url.lastIndexOf('/') > 0) {
                            jarName = url.substring(url.lastIndexOf('/') + 1);
                            if (jarName.indexOf('?') > 0) {
                                jarName = jarName.substring(0, jarName.indexOf('?'));
                            }
                        }
                        if (jarName != null && data != null) {
                            GlfmMain.addApp(jarName, data);
                        }
                        reloadAppList();
                    }
                });
                hc.start();
            }
        });
        y += addH + pad;
        GLabel uploadLab = new GLabel(GLanguage.getString(note), pad, y, devW - pad * 3 - addW * 2, addH);
        uploadLab.setAlign(GGraphics.LEFT | GGraphics.VCENTER);
        mainPanel.add(uploadLab);

        GButton uploadbtn = new GButton(GLanguage.getString("Start"), devW - addW * 2 - pad, y, addW * 2, addH);
        mainPanel.add(uploadbtn);
        uploadbtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (webServer != null) {
                    webServer.stopServer();
                }
                if (uploadbtn.getText().equals(GLanguage.getString("Stop"))) {
                    uploadbtn.setText(GLanguage.getString("Start"));
                    uploadLab.setText(GLanguage.getString(note));
                } else {
                    webServer = new MiniHttpServer();
                    webServer.setUploadCompletedHandle(new MiniHttpServer.UploadCompletedHandle() {
                        @Override
                        public void onCompleted(List<MiniHttpServer.UploadFile> files) {
                            for (MiniHttpServer.UploadFile f : files) {
                                GlfmMain.addApp(f.filename, f.data);
                            }
                            reloadAppList();
                        }
                    });
                    webServer.start();
                    uploadbtn.setText(GLanguage.getString("Stop"));
                    uploadLab.setText(GLanguage.getString("Webserver listen on : ") + webServer.getPort());
                }
            }
        });
        y += addH + pad;

        GLabel appsLab = new GLabel(GLanguage.getString("Application list : "), pad, y, devW - pad * 3 - addW * 2, addH);
        appsLab.setAlign(GGraphics.LEFT | GGraphics.VCENTER);
        mainPanel.add(appsLab);
        y += addH + pad;

        GTextField search = new GTextField("", "Search", pad, y, devW, addH);
        search.setBoxStyle(GTextField.BOX_STYLE_SEARCH);
        mainPanel.add(search);
        y += addH + pad;

        appList = new GList(0, y, devW, devH - y);
        appList.setShowMode(GList.MODE_MULTI_SHOW);
        appList.setItemHeight(50);
        mainPanel.add(appList);

        //right
        float x1 = devW + pad;
        y = pad;
        GButton back2listBtn = new GButton("< " + GLanguage.getString("Back"), x1, y, addW, addH);
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

        form.setKeyshowListener(new GKeyboardShowListener() {
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

        GButton runBtn = new GButton(GLanguage.getString("Run"), pad, y, devW - pad * 2, bigBtnH);
        runBtn.setName(APP_RUN_BTN);
        contentView.add(runBtn);
        runBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        GlfmMain.runApp(appName);
                    }
                }
            }
        });
        y += bigBtnH + pad;

        GButton defaultBtn = new GButton(GLanguage.getString("Set as boot app"), pad, y, devW - pad * 2, bigBtnH);
        contentView.add(defaultBtn);
        defaultBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    GlfmMain.setDefaultApp(curSelectedItem.getLabel());
                }
            }
        });
        y += bigBtnH + pad;

        GButton upgradeBtn = new GButton(GLanguage.getString("Upgrade"), pad, y, devW - pad * 2, bigBtnH);
        upgradeBtn.setName(APP_UPGRADE_BTN);
        contentView.add(upgradeBtn);
        upgradeBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        String url = GlfmMain.getApplicationUpgradeurl(appName);
                        if (url != null) {
                            MiniHttpClient hc = new MiniHttpClient(url, new MiniHttpClient.DownloadCompletedHandle() {
                                @Override
                                public void onCompleted(String url, byte[] data) {
                                    System.out.println("download success " + url + " ,size: " + data.length);
                                    String jarName = null;
                                    if (url.lastIndexOf('/') > 0) {
                                        jarName = url.substring(url.lastIndexOf('/') + 1);
                                        if (jarName.indexOf('?') > 0) {
                                            jarName = jarName.substring(0, jarName.indexOf('?'));
                                        }
                                    }
                                    if (jarName != null && data != null) {
                                        GlfmMain.addApp(jarName, data);
                                    }
                                    reloadAppList();
                                    updateContentViewInfo(jarName);
                                }
                            });
                            hc.start();
                        }
                    }
                }
            }
        });
        y += bigBtnH + pad;

        GButton deleteBtn = new GButton(GLanguage.getString("Delete"), pad, y, devW - pad * 2, bigBtnH);
        deleteBtn.setName(APP_DELETE_BTN);
        deleteBtn.setBgColor(128, 16, 8, 255);
        contentView.add(deleteBtn);
        deleteBtn.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (curSelectedItem != null) {
                    String appName = curSelectedItem.getLabel();
                    if (appName != null) {
                        GlfmMain.removeApp(appName);
                        reloadAppList();
                    }
                }

            }
        });
        y += bigBtnH + pad;
    }

    void reloadAppList() {
        if (appList == null) {
            return;
        }
        appList.removeItemAll();
        List<String> list = GlfmMain.getAppList();
        if (list != null && list.size() > 0) {
            for (String appName : list) {
                //System.out.println("appName:" + appName);
                if (GlfmMain.isJarExists(appName)) {
                    byte[] iconBytes = GlfmMain.getApplicationIcon(appName);
                    GImage img = null;
                    if (iconBytes != null) {
                        img = GImage.createImage(iconBytes);
                    }
                    GListItem item = new GListItem(img, appName) {
                        public boolean update(long vg) {
                            super.update(vg);
                            if (getLabel() != null && getLabel().equals(GlfmMain.getDefaultApp())) {
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
        nameLab.setText(GlfmMain.getApplicationName(appName));
        //
        GTextBox descLab = (GTextBox) contentView.findByName(APP_DESC_LABEL);

        String txt = GLanguage.getString("Version: ") + "\n  " + GlfmMain.getApplicationVersion(appName) + "\n"
                + GLanguage.getString("File size: ") + "\n  " + GlfmMain.getApplicationFileSize(appName) + "\n"
                + GLanguage.getString("Upgrade url: ") + "\n  " + GlfmMain.getApplicationUpgradeurl(appName) + "\n"
                + GLanguage.getString("Description: ") + "\n  " + GlfmMain.getApplicationDesc(appName) + "\n";
        descLab.setText(txt);

        //re set image
        GImageItem icon = (GImageItem) contentView.findByName(APP_ICON_ITEM);
        icon.setImg(curSelectedItem.getImg());
        contentView.reBoundle();
    }

    void mainPanelShowLeft() {
        float panelX = mainPanel.getX();
        float panelY = mainPanel.getY();
        form.inertiaEvent(panelX, panelY, panelX + 200, panelY, 100);
        if (curSelectedItem != null) {
            appList.setSelectedIndex(-1);
            curSelectedItem = null;
        }
    }

    void mainPanelShowRight() {
        float panelX = mainPanel.getX();
        float panelY = mainPanel.getY();
        form.inertiaEvent(panelX + 200, panelY, panelX, panelY, 100);
    }

}
