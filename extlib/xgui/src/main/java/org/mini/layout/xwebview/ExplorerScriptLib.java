/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.layout.xwebview;

import org.mini.apploader.AppManager;
import org.mini.gui.*;
import org.mini.gui.gscript.DataType;
import org.mini.gui.gscript.Interpreter;
import org.mini.gui.gscript.Lib;
import org.mini.gui.guilib.GuiScriptLib;
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
public class ExplorerScriptLib extends Lib {
    XExplorerHolder holder;

    /**
     *
     */
    public ExplorerScriptLib(XExplorerHolder holder) {
        this.holder = holder;

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
            XPage page = holder.getExplorer().getCurrentPage();
            if (page != null) {// may be  href="/abc/c.xml"
                href = XUrlHelper.normalizeUrl(page.getUrl(), href); //fix as : http://www.abc.com/abc/c.xml
            }
            holder.getExplorer().gotoPage(href);
            GuiScriptLib.doHttpCallback(holder.getExplorer().getWebView().getForm(), callback, href, 0, "");
        }
        return null;
    }


    public DataType downloadInstall(ArrayList<DataType> para) {
        String href = Interpreter.popBackStr(para);
        String callback = Interpreter.popBackStr(para);
        if (href != null) {
            XPage page = holder.getExplorer().getCurrentPage();
            if (page != null) {// may be  href="/abc/c.xml"
                href = XUrlHelper.normalizeUrl(page.getUrl(), href); //fix as : http://www.abc.com/abc/c.zip
            }

            MiniHttpClient hc = new MiniHttpClient(href, null, new MiniHttpClient.DownloadCompletedHandle() {
                @Override
                public void onCompleted(MiniHttpClient client, String url, byte[] data) {
                    if (data != null) {
                        AppManager.getInstance().getDownloadCallback().onCompleted(client, url, data);
                        GuiScriptLib.doHttpCallback(holder.getExplorer().getWebView().getForm(), callback, url, 0, "");
                    }
                }
            });
            hc.start();
        }
        return null;
    }

    public DataType downloadSave(ArrayList<DataType> para) {
        String href = Interpreter.popBackStr(para);
        String callback = Interpreter.popBackStr(para);
        if (href != null) {
            XPage page = holder.getExplorer().getCurrentPage();
            if (page != null) {// may be  href="/abc/c.xml"
                href = XUrlHelper.normalizeUrl(page.getUrl(), href); //fix as : http://www.abc.com/abc/c.zip
            }
            MiniHttpClient hc = new MiniHttpClient(href, null, new MiniHttpClient.DownloadCompletedHandle() {
                @Override
                public void onCompleted(MiniHttpClient client, String url, byte[] data) {
                    if (data != null) {
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

                            GuiScriptLib.doHttpCallback(holder.getExplorer().getWebView().getForm(), callback, url, 0, path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            hc.start();
        }
        return null;
    }


    public DataType getPageBaseUrl(ArrayList<DataType> para) {
        String href = "";
        XPage page = holder.getExplorer().getCurrentPage();
        if (page != null) {// may be  href="/abc/c.xml"
            href = XUrlHelper.normalizeUrl(page.getUrl(), "/"); //fix as : http://www.abc.com/abc/c.xml
        }
        return Interpreter.getCachedStr(href);
    }

    public DataType getPageUrl(ArrayList<DataType> para) {
        String href = "";
        XPage page = holder.getExplorer().getCurrentPage();
        if (page != null) {// may be  href="/abc/c.xml"
            href = page.getUrl().toString();
        }
        return Interpreter.getCachedStr(href);
    }

    public DataType prevPage(ArrayList<DataType> para) {
        XExplorer explorer = holder.getExplorer();
        if (explorer != null) {
            explorer.back();
        }
        return null;
    }
    public DataType nextPage(ArrayList<DataType> para) {
        XExplorer explorer = holder.getExplorer();
        if (explorer != null) {
            explorer.forward();
        }
        return null;
    }
    public DataType refreshPage(ArrayList<DataType> para) {
        XExplorer explorer = holder.getExplorer();
        if (explorer != null) {
            XPage page = explorer.getCurrentPage();
            if (page != null) {
                page.reset();
                explorer.gotoPage(page.getUrl().toString());
            }
        }
        return null;
    }
}
