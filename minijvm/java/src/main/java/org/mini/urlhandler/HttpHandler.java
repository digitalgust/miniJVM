/*
 * gust
 */

package org.mini.urlhandler;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;

public class HttpHandler extends URLStreamHandler {
    public URLConnection openConnection(URL url) throws IOException {
        return new HttpURLConnectionImpl(url);
    }

    static class HttpURLConnectionImpl extends HttpURLConnection {
        protected HttpURLConnectionImpl(URL url) {
            super(url);
        }

        @Override
        public void disconnect() {
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
