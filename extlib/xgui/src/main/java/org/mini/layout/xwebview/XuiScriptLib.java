/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.layout.xwebview;

import org.mini.apploader.AppManager;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.callback.GCmd;
import org.mini.gui.callback.GDesktop;
import org.mini.gui.gscript.DataType;
import org.mini.gui.gscript.Interpreter;
import org.mini.gui.gscript.Lib;
import org.mini.layout.guilib.FormHolder;
import org.mini.layout.guilib.GuiScriptLib;
import org.mini.http.MiniHttpClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


/**
 * xml ui script libary
 *
 * @author Gust
 */
public class XuiScriptLib extends Lib {
    BrowserHolder browserHolder;
    FormHolder formHolder;

    /**
     *
     */
    public XuiScriptLib(BrowserHolder browserHolder, FormHolder formHolder) {
        this.browserHolder = browserHolder;
        this.formHolder = formHolder;

        // script method register
        {
            methodNames.put("openPage".toLowerCase(), this::openPage);//
            methodNames.put("downloadInstall".toLowerCase(), this::downloadInstall);//
            methodNames.put("downloadSave".toLowerCase(), this::downloadSave);//
            methodNames.put("getPageBaseUrl".toLowerCase(), this::getPageBaseUrl);//
            methodNames.put("getPageUrl".toLowerCase(), this::getPageUrl);//
            methodNames.put("prevPage".toLowerCase(), this::prevPage);//
            methodNames.put("nextPage".toLowerCase(), this::nextPage);//
            methodNames.put("refreshPage".toLowerCase(), this::refreshPage);//

        }
    }


    // -------------------------------------------------------------------------
    // inner method
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // implementation
    // -------------------------------------------------------------------------


    public DataType openPage(ArrayList<DataType> para) {
        String href = Interpreter.popBackStr(para);
        String callback = Interpreter.popBackStr(para);
        if (href != null) {
            XuiPage page = browserHolder.getBrowser().getCurrentPage();
            if (page != null) {// may be  href="/abc/c.xml"
                href = UrlHelper.normalizeUrl(page.getUrl(), href); //fix as : http://www.abc.com/abc/c.xml
            }
            browserHolder.getBrowser().gotoPage(href);
            GuiScriptLib.doHttpCallback(formHolder.getForm(), callback, href, 0, "");
        }
        return null;
    }


    public DataType downloadInstall(ArrayList<DataType> para) {
        String href = Interpreter.popBackStr(para);
        String callback = Interpreter.popBackStr(para);
        if (href != null) {
            XuiPage page = browserHolder.getBrowser().getCurrentPage();
            if (page != null) {// may be  href="/abc/c.xml"
                href = UrlHelper.normalizeUrl(page.getUrl(), href); //fix as : http://www.abc.com/abc/c.zip
            }

            MiniHttpClient hc = new MiniHttpClient(href, null, new MiniHttpClient.DownloadCompletedHandle() {
                @Override
                public void onCompleted(MiniHttpClient client, String url, byte[] data) {
                    if (data != null) {
                        //多线程防止GContainer.elements死锁
                        GCmd cmd = new GCmd(
                                () -> {
                                    AppManager.getInstance().getDownloadCallback().onCompleted(client, url, data);
                                    GuiScriptLib.doHttpCallback(formHolder.getForm(), callback, url, 0, "");
                                });
                        GForm.addCmd(cmd);
                        GForm.flush();

                    }
                }
            });
            hc.setProgressListener((MiniHttpClient client, int progress) -> {
                GuiScriptLib.showProgressBar(formHolder.getForm(), progress);
                GDesktop.flush();
            });
            hc.start();
        }
        return null;
    }

    public DataType downloadSave(ArrayList<DataType> para) {
        String href = Interpreter.popBackStr(para);
        String callback = Interpreter.popBackStr(para);
        if (href != null) {
            XuiPage page = browserHolder.getBrowser().getCurrentPage();
            if (page != null) {// may be  href="/abc/c.xml"
                href = UrlHelper.normalizeUrl(page.getUrl(), href); //fix as : http://www.abc.com/abc/c.zip
            }
            MiniHttpClient hc = new MiniHttpClient(href, null, new MiniHttpClient.DownloadCompletedHandle() {
                @Override
                public void onCompleted(MiniHttpClient client, String url, byte[] data) {
                    if (data != null) {
                        //多线程防止GContainer.elements死锁
                        GCmd cmd = new GCmd(
                                () -> {

                                    try {
                                        String saveFileName = null;
                                        if (url.lastIndexOf('/') > 0) {
                                            saveFileName = url.substring(url.lastIndexOf('/') + 1);
                                            if (saveFileName.indexOf('?') > 0) {
                                                saveFileName = saveFileName.substring(0, saveFileName.indexOf('?'));
                                            }
                                        }
                                        if (saveFileName == null) {
                                            saveFileName = new Random().nextInt(Integer.MAX_VALUE) + ".bin";
                                        }
                                        String path = GCallBack.getInstance().getAppSaveRoot() + "/tmp/" + saveFileName;
                                        FileOutputStream fos = new FileOutputStream(path);
                                        fos.write(data);
                                        fos.close();

                                        GuiScriptLib.doHttpCallback(formHolder.getForm(), callback, url, 0, path);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                        GForm.addCmd(cmd);
                        GForm.flush();
                    }
                }
            });
            hc.setProgressListener((MiniHttpClient client, int progress) -> {
                GuiScriptLib.showProgressBar(formHolder.getForm(), progress);
            });
            hc.start();
        }
        return null;
    }


    public DataType getPageBaseUrl(ArrayList<DataType> para) {
        String href = "";
        XuiPage page = browserHolder.getBrowser().getCurrentPage();
        if (page != null) {// may be  href="/abc/c.xml"
            href = UrlHelper.normalizeUrl(page.getUrl(), "/"); //fix as : http://www.abc.com/abc/c.xml
        }
        return Interpreter.getCachedStr(href);
    }

    public DataType getPageUrl(ArrayList<DataType> para) {
        String href = "";
        XuiPage page = browserHolder.getBrowser().getCurrentPage();
        if (page != null) {// may be  href="/abc/c.xml"
            href = page.getUrl().toString();
        }
        return Interpreter.getCachedStr(href);
    }

    public DataType prevPage(ArrayList<DataType> para) {
        XuiBrowser explorer = browserHolder.getBrowser();
        if (explorer != null) {
            explorer.back();
        }
        return null;
    }

    public DataType nextPage(ArrayList<DataType> para) {
        XuiBrowser explorer = browserHolder.getBrowser();
        if (explorer != null) {
            explorer.forward();
        }
        return null;
    }

    public DataType refreshPage(ArrayList<DataType> para) {
        XuiBrowser explorer = browserHolder.getBrowser();
        if (explorer != null) {
            XuiPage page = explorer.getCurrentPage();
            if (page != null) {
                page.reset();
                explorer.gotoPage(page.getUrl().toString());
            }
        }
        return null;
    }
}
