package org.mini.layout.xwebview;

import org.mini.layout.xwebview.urlhelper.FileUrlHelper;
import org.mini.layout.xwebview.urlhelper.HttpUrlHelper;
import org.mini.layout.xwebview.urlhelper.JarUrlHelper;

import java.net.URL;

public abstract class UrlHelper {

    public static UrlHelper getHelper(String urlStr) {
        try {
            return getHelper(new URL(urlStr));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JarUrlHelper();
    }

    public static UrlHelper getHelper(URL homeUrl) {
        switch (homeUrl.getProtocol()) {
            case "http":
            case "https":
                return new HttpUrlHelper();
            case "file":
                return new FileUrlHelper();
            case "jar":
            default:
                return new JarUrlHelper();
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
