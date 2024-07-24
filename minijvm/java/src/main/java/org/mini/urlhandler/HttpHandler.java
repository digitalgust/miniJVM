/*
 * gust
 */

package org.mini.urlhandler;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHandler extends URLStreamHandler {
    public URLConnection openConnection(URL url) throws IOException {
        return new HttpURLConnectionImpl(url);
    }

    class HttpURLConnectionImpl extends URLConnection {
        private static final String HKEY_CONTENT_LENGTH = "content-length";

        javax.microedition.io.HttpConnection connection;
        byte[] data;
        ByteArrayOutputStream baos;

        private Map<String, List<String>> header = new HashMap<>();
        private int status;

        protected HttpURLConnectionImpl(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            if (data != null) return;

            String urlStr = url.toString();
            int index = urlStr.indexOf("?");
            if (index > 0) {
                urlStr = urlStr.substring(0, index);
            }
            CachedFile ba = caches.get(urlStr);
            if (ba != null && !ba.isExpired()) {
                if (useCaches) {  //cache hit
                    data = (byte[]) ba.resource;
                    return;
                }
            } else {
                caches.remove(urlStr);
            }
            //request
            if (connection == null) {
                Connection con = Connector.open(url.toString());
                if (con instanceof HttpConnection) {
                    connection = (HttpConnection) con;
                    if (baos != null) {
                        connection.setRequestMethod(HttpConnection.POST);
                        byte[] data = baos.toByteArray();
                        connection.setRequestProperty("Content-Length", String.valueOf(data.length));
                        connection.openDataOutputStream().write(data);
                    } else {
                        connection.setRequestMethod(HttpConnection.GET);
                    }
                    status = connection.getResponseCode();
                    if (status != HttpConnection.HTTP_OK) {
                        throw new IOException("Http status: " + status);
                    }
                    for (int i = 0; true; i++) {
                        String k = connection.getHeaderFieldKey(i);
                        if (k == null) break;
                        String v = connection.getHeaderField(k);
                        List<String> list = new ArrayList<>();
                        list.add(v);
                        header.put(k.toLowerCase(), list);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    InputStream is = connection.openDataInputStream();
                    while (true) {
                        int b = is.read();
                        if (b < 0) break;
                        baos.write(b);
                    }
                    data = (baos.toByteArray());
                    long exp = connection.getExpiration();
                    long cur = System.currentTimeMillis();
                    if (cur < exp) {
                        CachedFile res = new CachedFile(data, connection.getExpiration());
                        caches.put(urlStr, res);
                    }
                }
            }
        }


        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            if (baos == null) {
                baos = new ByteArrayOutputStream();
            }
            return baos;
        }

        @Override
        public int getContentLength() {
            return getHeaderFieldInt(HKEY_CONTENT_LENGTH, -1);
        }

        @Override
        public long getContentLengthLong() {
            return getHeaderFieldLong(HKEY_CONTENT_LENGTH, -1l);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return (header);
        }
    }
}
