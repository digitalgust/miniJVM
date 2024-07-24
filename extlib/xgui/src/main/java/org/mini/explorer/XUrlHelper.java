package org.mini.explorer;

import org.mini.explorer.urlhelper.XFileUrlHelper;
import org.mini.explorer.urlhelper.XHttpUrlHelper;
import org.mini.explorer.urlhelper.XJarUrlHelper;

import java.net.URL;

public abstract class XUrlHelper {

    public static XUrlHelper getHelper(String urlStr) {
        try {
            return getHelper(new URL(urlStr));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new XJarUrlHelper();
    }

    public static XUrlHelper getHelper(URL homeUrl) {
        switch (homeUrl.getProtocol()) {
            case "http":
            case "https":
                return new XHttpUrlHelper();
            case "file":
                return new XFileUrlHelper();
            case "jar":
            default:
                return new XJarUrlHelper();
        }
    }

    public static String normalizeUrl(URL homeUrl, String pUrl) {
        String resUrl = pUrl;
        try {
            URL u = new URL(resUrl);
        } catch (Exception e) {
            resUrl = resUrl.replace("\\", "/");
            resUrl = getHelper(homeUrl).mergUrl(homeUrl, resUrl);
        }
        return resUrl;
    }

    public abstract String getProtocol();

    public abstract String mergUrl(URL rootUrl, String path);

}
