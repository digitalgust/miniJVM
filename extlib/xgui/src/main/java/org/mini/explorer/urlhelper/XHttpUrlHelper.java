package org.mini.explorer.urlhelper;

import org.mini.explorer.XUrlHelper;

import java.net.URL;

public class XHttpUrlHelper extends XUrlHelper {
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

        String tmppath = url.getPath();
        if (tmppath.endsWith("/") || path.startsWith("/") || path.indexOf("/") < 0) {
            return prefix + path;
        } else {
            return prefix + "/" + path.substring(0, path.lastIndexOf("/"));
        }
    }


}
