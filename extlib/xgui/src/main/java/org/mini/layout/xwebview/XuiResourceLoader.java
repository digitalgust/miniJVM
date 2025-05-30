package org.mini.layout.xwebview;

import org.mini.gui.GImage;
import org.mini.layout.loader.XmlExtAssist;
import org.mini.util.SysLog;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class XuiResourceLoader implements XmlExtAssist.XLoader {

    URL url;
    boolean useCaches;
    static GImage notfoundImage;
    static String notfoundText = "<panel>" +
            "<script>\n" +
            "    <![CDATA[\n" +
            "    sub copyurl()\n" +
            "        setClipboard(getText(\"LB_URL\"))\n" +
            "        showBar(\"URL copied\")\n" +
            "    ret\n" +
            "    ]]>\n" +
            "</script>" +
            "<label>not found:</label>" +
            "<br/>" +
            "<label color=\"3333aaff\" name=\"LB_URL\" multiline=\"1\" onclick=\"copyurl()\">{URL}</label>" +
            "</panel>";

    Map<String, XuiResource> resources = new HashMap<>();

    public XuiResourceLoader() {
        this(true);
    }

    public XuiResourceLoader(boolean useCaches) {
        if (notfoundImage == null) {
            notfoundImage = GImage.createImageFromJar("/res/ui/notfound.jpg");
        }
        this.useCaches = useCaches;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public void clearCache() {
        resources.clear();
    }

    public XuiResource loadResource(String pUrl, String post) {
        String resUrl = UrlHelper.normalizeUrl(url, pUrl);
        try {
            URL u = new URL(resUrl);
            URLConnection conn = u.openConnection();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) conn;
                http.setUseCaches(useCaches);
                if (post != null) {
                    http.setRequestMethod("POST");
                    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    http.getOutputStream().write(post.getBytes("utf-8"));
                }
            }
            conn.connect();
            String type = conn.getContentType();
            InputStream o = (InputStream) conn.getContent();
            if (o != null) {
                byte[] data = new byte[o.available()];
                int read = 0;
                while (read < data.length) {
                    read += o.read(data, read, data.length - read);
                }
                XuiResource resource = new XuiResource();
                resource.url = resUrl;
                resource.type = XuiResource.getTypeByString(type);
                resource.data = data;
                return resource;
            }
        } catch (Exception e) {
            SysLog.error("loadData error:" + resUrl, e);
        }
        return null;
    }


    public GImage loadImage(String pUrl) {
        String resUrl = UrlHelper.normalizeUrl(url, pUrl);
        try {
            XuiResource res = resources.get(resUrl);
            if (res == null) {
                res = loadResource(resUrl, null);
            }
            if (res != null) {
                if (res.image == null) {
                    res.image = GImage.createImage(res.data);
                    res.type = XuiResource.TYPE_IMAGE;
                    resources.put(resUrl, res);
                }
                if (res.image == null) {
                    res.image = notfoundImage;
                }
                return res.image;
            }
        } catch (Exception e) {
            SysLog.error("loadImage error:" + resUrl, e);
        }
        return notfoundImage;
    }

    public String loadXml(String pUrl, String post) {
        String resUrl = UrlHelper.normalizeUrl(url, pUrl);
        try {
            XuiResource res = resources.get(resUrl + post);
            if (res == null) {
                res = loadResource(resUrl, post);
            }
            if (res != null) {
                if (res.xml == null) {
                    res.xml = new String(res.data, "UTF-8");
                    res.type = XuiResource.TYPE_XML;
                    resources.put(resUrl + post, res);
                }
                return res.xml;
            }
        } catch (Exception e) {
            SysLog.error("loadXml error:" + resUrl, e);
        }
        String urlStr = resUrl.replace("&", "&amp;");
        urlStr = urlStr.replace("<", "&lt;");
        urlStr = urlStr.replace(">", "&gt;");
        return notfoundText.replace("{URL}", urlStr);
    }
}
