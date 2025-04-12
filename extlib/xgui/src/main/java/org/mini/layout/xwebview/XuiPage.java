package org.mini.layout.xwebview;

import org.mini.gui.GContainer;
import org.mini.gui.GObject;
import org.mini.layout.loader.UITemplate;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.loader.XmlExtAssist;

import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * a xmlui page
 */
public class XuiPage {

    XuiBrowser explorer;
    XEventHandler eventDelegate;
    XmlExtAssist assistDelegate;
    URL url;
    String post;
    boolean useCaches;
    GContainer pan;

    public XuiPage(String homeUrl, XuiBrowser browser) {
        this(homeUrl, null, true, browser);
    }

    public XuiPage(String homeUrl, String post, boolean useCaches, XuiBrowser browser) {
        try {
            //urlStr="jar:http://www.foo.com/bar/baz.jar!/COM/foo/Quux.class";
            url = new URL(homeUrl);

//            URLConnection con = url.openConnection();
//            con.connect();
//            System.out.println(con.getContentLength());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.post = post;
        this.useCaches = useCaches;
        XEventHandler eventDelegate = new XPageEventDelegate(browser, url);
        this.eventDelegate = eventDelegate;

        this.assistDelegate = new XmlExtAssist(browser.getAssist().getXuiBrowserHolder());
        this.assistDelegate.copyFrom(browser.getAssist());
    }

    public GContainer getGui(float width, float height) {
        if (pan != null) {
            return pan;
        }
        try {
            XuiResourceLoader loader = new XuiResourceLoader(useCaches);
            loader.setURL(url);
            assistDelegate.setLoader(loader);

            String uistr = loader.loadXml(url.toString(), post);
            if (uistr != null) {
                UITemplate uit = new UITemplate(uistr);
                XContainer xcon = (XContainer) XContainer.parseXml(uit.parse(), assistDelegate);
                xcon.build((int) width, (int) height, eventDelegate);
                pan = xcon.getGui();
                return pan;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }

    private String getUtf8String(byte[] b) {
        try {
            return new String(b, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void reset() {
        pan = null;
    }

    public URL getUrl() {
        return url;
    }

    public String getPost() {
        return post;
    }


    public static class XPageEventDelegate extends XEventHandler {
        XuiBrowser explorer;
        XEventHandler eventHandler;
        URL url;

        XPageEventDelegate(XuiBrowser explorer, URL purl) {
            this.explorer = explorer;
            this.eventHandler = explorer.getEventHandler();
            this.url = purl;
        }

        @Override
        public void action(GObject gobj) {
            eventHandler.action(gobj);
        }

        @Override
        public void flyBegin(GObject gObject, float x, float y) {
            eventHandler.flyBegin(gObject, x, y);
        }

        @Override
        public void flyEnd(GObject gObject, float x, float y) {
            eventHandler.flyEnd(gObject, x, y);
        }

        @Override
        public void flying(GObject gObject, float x, float y) {
            eventHandler.flying(gObject, x, y);
        }

        @Override
        public void gotoHref(GObject gobj, String href) {
            if (href != null) {
                String resurl = UrlHelper.normalizeUrl(url, href);
                explorer.gotoPage(resurl);
            }
            eventHandler.gotoHref(gobj, href);
        }

        @Override
        public void onStateChange(GObject gobj) {
            eventHandler.onStateChange(gobj);
        }
    }
}
