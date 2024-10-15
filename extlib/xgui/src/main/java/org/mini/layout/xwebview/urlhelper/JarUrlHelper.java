package org.mini.layout.xwebview.urlhelper;

import org.mini.layout.xwebview.UrlHelper;

import java.net.URL;

public class JarUrlHelper extends UrlHelper {
    public String getProtocol() {
        return "file";
    }


//    @Override
//    public XResource getResource(String url) {
//        return null;
//    }
//
//    @Override
//    public void getResource(String url, XResourceReady rr) {
//
//    }
//


    public String mergUrl(URL url, String path) {
        String tmppath = url.getPath();
        if (tmppath.indexOf("!/") < 0) {
            tmppath = tmppath + "!/" + path;
        } else {
            if(path.startsWith("/")) {
                tmppath = tmppath.substring(0, tmppath.indexOf("!/")) + "!/" + path;
            }else{
                tmppath = tmppath.substring(0, tmppath.lastIndexOf("/")) + "/" + path;
            }
        }
        return url.getProtocol() + ":" + tmppath;
    }

}
