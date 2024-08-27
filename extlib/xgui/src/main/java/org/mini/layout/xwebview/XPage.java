package org.mini.layout.xwebview;

import org.mini.gui.GContainer;
import org.mini.gui.GObject;
import org.mini.layout.UITemplate;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.XmlExtAssist;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * a xmlui page
 */
public class XPage {

    XExplorer explorer;
    XEventHandler eventDelegate;
    XmlExtAssist assistDelegate;
    URL url;
    GContainer pan;

    public XPage(String homeUrl, XExplorer explorer) {
        try {
            //urlStr="jar:http://www.foo.com/bar/baz.jar!/COM/foo/Quux.class";
            url = new URL(homeUrl);

//            URLConnection con = url.openConnection();
//            con.connect();
//            System.out.println(con.getContentLength());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        XEventHandler eventDelegate = new XPageEventDelegate(explorer, url);
        this.eventDelegate = eventDelegate;

        this.assistDelegate = new XmlExtAssist(explorer.getAssist().getForm());
        this.assistDelegate.copyFrom(explorer.getAssist());
    }

    public GContainer getGui(float width, float height) {
        if (pan != null) {
            return pan;
        }
        try {
            XResourceLoader loader = new XResourceLoader();
            loader.setURL(url);
            assistDelegate.setLoader(loader);

            String uistr = loader.loadXml(url.toString());
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


    public static class XPageEventDelegate extends XEventHandler {
        XExplorer explorer;
        XEventHandler eventHandler;
        URL url;

        XPageEventDelegate(XExplorer explorer, URL purl) {
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
                String resurl = XUrlHelper.normalizeUrl(url, href);
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
