package org.mini.layout.xwebview.urlhelper;

import org.mini.layout.xwebview.UrlHelper;

import java.net.URL;

public class HttpUrlHelper extends UrlHelper {
    public String getProtocol() {
        return "file";
    }


//    @Override
//    public void getResource(String url, XResourceReady resourceReady) {
//        if (contents.containsKey(url)) {
//            resourceReady.onResourceReady(url, contents.get(url));
//        }
//        MiniHttpClient hc = getHttpClient(url, null);
//        hc.start();//asynchronized request resource
//    }
//
//    @Override
//    public XResource getResource(String url) {
//        if (contents.containsKey(url)) {
//            return contents.get(url);
//        }
//        MiniHttpClient hc = getHttpClient(url, null);
//        hc.run();//synchronized request resource
//        return contents.get(url);
//    }
//


    public String mergUrl(URL url, String path) {
        String port = url.getPort() < 0 ? "" : ":" + url.getPort();
        String prefix = url.getProtocol() + "://" + url.getHost() + port + "/";

        String relativePath = url.getPath();
        relativePath = relativePath.indexOf('/') >= 0 ? relativePath.substring(0, relativePath.lastIndexOf('/')) : "";
        if (path.startsWith("/")) {
            return prefix + path;
        } else {
            return prefix + relativePath + "/" + path;
        }
    }


}
