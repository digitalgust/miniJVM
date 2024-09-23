package org.mini.layout.xwebview.urlhelper;

import org.mini.layout.xwebview.UrlHelper;

import java.net.URL;

public class FileUrlHelper extends UrlHelper {
    public String getProtocol() {
        return "file";
    }

    public String mergUrl(URL url, String path) {
        String tmppath = url.getPath();
        tmppath = tmppath.replace('\\', '/');
        if (path.startsWith("/")) {
            return url.getProtocol() + "://" + path;
        }
        tmppath = tmppath.substring(0, tmppath.lastIndexOf("/") + 1) + path;
        return url.getProtocol() + "://" + tmppath;
    }


}
