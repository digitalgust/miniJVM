package org.mini.layout.xwebview;

import org.mini.gui.callback.GCmd;
import org.mini.gui.GContainer;
import org.mini.gui.GForm;
import org.mini.layout.guilib.GuiScriptLib;
import org.mini.layout.XEventHandler;
import org.mini.layout.loader.XmlExtAssist;

import java.util.List;

/**
 * a web explorer that base xmlui but not html
 * 本类提供一个简单的web浏览器功能，负责组织页面，并显示在GContainer中
 * 本webview只能显示xmlui，支持http/file/jar协议
 *
 * <viewport>
 * <lable>This is a test</lable>
 * </viewport>
 * <p>
 * HTTP URI---------------------------------
 * http://www.foo.com/bar/baz.xml
 * <p>
 * FILE URI---------------------------------
 * file://localhost/etc/fstab/a.xml
 * file:///etc/fstab/b.xml
 * <p>
 * JAR URI---------------------------------
 * A Jar entry
 * jar:http://www.foo.com/bar/baz.jar!/COM/foo
 * A Jar file
 * jar:http://www.foo.com/bar/baz.jar!/y.xml
 * A Jar directory
 * jar:http://www.foo.com/bar/baz.jar!/COM/foo/
 */
public class XuiBrowser {
    public static final int MAX_PAGE_SIZE = 10;
    List<XuiPage> pages = new java.util.ArrayList<>();
    private XEventHandler eventHandler;
    XmlExtAssist assist;
    XuiPage currentPage;

    /**
     * web explorer
     * this web explorer will load xml ui from web server, and display it in webview
     *
     * @param eventHandler native event handler
     * @param assist       parse xml ui assists, like load image, load xml, register script library
     */
    public XuiBrowser(XEventHandler eventHandler, XmlExtAssist assist) {
        this.eventHandler = eventHandler;
        this.assist = assist;
    }

    public void gotoPage(String homeUrl) {
        removeAfterAtCurrentPage();
        GuiScriptLib.showProgressBar(assist.getForm(), 50);
        XuiPage page = new XuiPage(homeUrl, this);
        showPage(page);
    }

    public void gotoPage(String homeUrl, String post, boolean useCache) {
        removeAfterAtCurrentPage();
        GuiScriptLib.showProgressBar(assist.getForm(), 50);
        XuiPage page = new XuiPage(homeUrl, post, useCache, this);
        showPage(page);
    }

    public XEventHandler getEventHandler() {
        return eventHandler;
    }

    public XmlExtAssist getAssist() {
        return assist;
    }

    public XuiPage getCurrentPage() {
        return currentPage;
    }

    /**
     * remove all after current page ,
     */
    private void removeAfterAtCurrentPage() {
        int index = pages.indexOf(currentPage);
        while (pages.size() > index + 1) {
            pages.remove(index + 1);
        }
    }

    private void showPage(XuiPage page) {
        GContainer webView = assist.getXuiBrowserHolder().getWebView();
        GuiScriptLib.showProgressBar(assist.getForm(), 80);
        if (webView != null && page != null) {
            //上面的给线程做，下面的给主线程做，要不可能导致多线程争抢GContainer.elements死锁
            GForm.addCmd(new GCmd(() -> {
                GContainer pan = page.getGui(webView.getW(), webView.getH());
                webView.clear();
                webView.add(pan);
                webView.reAlign();
                currentPage = page;
                if (!pages.contains(page)) {
                    pages.add(page);
                }
                webView.flushNow();
                GuiScriptLib.showProgressBar(assist.getForm(), 100);
            }));
            GForm.flush();
        }
    }

    public void back() {
        if (pages.size() > MAX_PAGE_SIZE) {
            pages.remove(0);
        }
        int index = pages.indexOf(currentPage);
        if (pages.size() > 0 && index >= 1) {
            XuiPage page = pages.get(index - 1);
            showPage(page);
        }
    }

    public void forward() {
        if (pages.size() > MAX_PAGE_SIZE) {
            pages.remove(0);
        }
        int index = pages.indexOf(currentPage);
        if (pages.size() > 0 && index < pages.size() - 1) {
            XuiPage page = pages.get(index + 1);
            showPage(page);
        }
    }


    public GContainer getWebView() {
        GContainer webView = assist.getXuiBrowserHolder().getWebView();
        return webView;
    }
}
