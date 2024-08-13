package org.mini.layout.xwebview;

import org.mini.gui.GContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.XmlExtAssist;

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
public class XExplorer {
    public static final int MAX_PAGE_SIZE = 10;
    List<XPage> pages = new java.util.ArrayList<>();
    GContainer webView;
    private XEventHandler eventHandler;
    XmlExtAssist assist;
    XPage currentPage;

    /**
     * web explorer
     * this web explorer will load xml ui from web server, and display it in webview
     *
     * @param webView      for display page ui
     * @param eventHandler native event handler
     * @param assist       parse xml ui assists, like load image, load xml, register script library
     */
    public XExplorer(GContainer webView, XEventHandler eventHandler, XmlExtAssist assist) {
        this.webView = webView;
        this.eventHandler = eventHandler;
        this.assist = assist;
    }

    public void gotoPage(String homeUrl) {
        removeAfterAtCurrentPage();
        XPage page = new XPage(homeUrl, this);
        showPage(page);
    }

    public XEventHandler getEventHandler() {
        return eventHandler;
    }

    public XmlExtAssist getAssist() {
        return assist;
    }

    public XPage getCurrentPage() {
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

    private void showPage(XPage page) {
        if (webView != null && page != null) {
            GContainer pan = page.getGui(webView.getW(), webView.getH());
            webView.clear();
            webView.add(pan);
            webView.reAlign();
            currentPage = page;
            if (!pages.contains(page)) {
                pages.add(page);
            }
            webView.flushNow();
        }
    }

    public void back() {
        if (pages.size() > MAX_PAGE_SIZE) {
            pages.remove(0);
        }
        int index = pages.indexOf(currentPage);
        if (pages.size() > 0 && index >= 1) {
            XPage page = pages.get(index - 1);
            showPage(page);
        }
    }

    public void forward() {
        if (pages.size() > MAX_PAGE_SIZE) {
            pages.remove(0);
        }
        int index = pages.indexOf(currentPage);
        if (pages.size() > 0 && index < pages.size() - 1) {
            XPage page = pages.get(index + 1);
            showPage(page);
        }
    }


    public GContainer getWebView() {
        return webView;
    }
}
