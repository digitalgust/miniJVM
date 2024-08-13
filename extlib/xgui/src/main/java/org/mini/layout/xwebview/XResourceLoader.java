package org.mini.layout.xwebview;

import org.mini.gui.GImage;
import org.mini.layout.XmlExtAssist;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class XResourceLoader implements XmlExtAssist.XLoader {

    URL url;
    static GImage notfoundImage;
    static String notfoundText = "<panel><label>not found:</label><br/><label>{URL}</label></panel>";

    Map<String, XResource> resources = new HashMap<>();

    public XResourceLoader() {
        notfoundImage = GImage.createImageFromJar("/res/ui/notfound.jpg");
    }


    public void setURL(URL url) {
        this.url = url;
    }

    public void clearCache() {
        resources.clear();
    }

    public XResource loadResource(String pUrl) {
        String resUrl = XUrlHelper.normalizeUrl(url, pUrl);
        try {
            URL u = new URL(resUrl);
            URLConnection conn = u.openConnection();
            conn.connect();
            String type = conn.getContentType();
            InputStream o = (InputStream) conn.getContent();
            if (o instanceof InputStream) {
                byte[] data = new byte[o.available()];
                int read = 0;
                while (read < data.length) {
                    read += o.read(data, read, data.length - read);
                }
                XResource resource = new XResource();
                resource.url = resUrl;
                resource.type = XResource.getTypeByString(type);
                resource.data = data;
                return resource;
            }
        } catch (Exception e) {
            System.out.println("loadData error:" + resUrl);
        }
        return null;
    }


    public GImage loadImage(String pUrl) {
        String resUrl = XUrlHelper.normalizeUrl(url, pUrl);
        try {
            XResource res = resources.get(resUrl);
            if (res == null) {
                res = loadResource(resUrl);
            }
            if (res != null) {
                if (res.image == null) {
                    res.image = GImage.createImage(res.data);
                    res.type = XResource.TYPE_IMAGE;
                    resources.put(resUrl, res);
                }
                if (res.image == null) {
                    res.image = notfoundImage;
                }
                return res.image;
            }
        } catch (Exception e) {
            System.out.println("loadImage error:" + resUrl);
        }
        return notfoundImage;
    }

    public String loadXml(String pUrl) {
        String resUrl = XUrlHelper.normalizeUrl(url, pUrl);
        try {
            XResource res = resources.get(resUrl);
            if (res == null) {
                res = loadResource(resUrl);
            }
            if (res != null) {
                if (res.xml == null) {
                    res.xml = new String(res.data, "UTF-8");
                    res.type = XResource.TYPE_XML;
                    resources.put(resUrl, res);
                }
                return res.xml;
            }
        } catch (Exception e) {
            System.out.println("loadXml error:" + resUrl);
        }
        String urlStr = resUrl.replace("&", "&amp;");
        urlStr = urlStr.replace("<", "&lt;");
        urlStr = urlStr.replace(">", "&gt;");
        return notfoundText.replace("{URL}", urlStr);
    }
}
