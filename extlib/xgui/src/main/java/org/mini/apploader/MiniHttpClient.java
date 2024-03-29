/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Gust
 */
public class MiniHttpClient extends Thread {

    String url;
    DownloadCompletedHandle handle;
    boolean exit;
    HttpConnection c = null;
    public static final MiniHttpClient.CltLogger DEFAULT_LOGGER = new CltLogger() {
        @Override
        void log(String s) {
            System.out.println(s);
        }
    };
    CltLogger logger = DEFAULT_LOGGER;

    public MiniHttpClient(final String url, CltLogger logger, final DownloadCompletedHandle handle) {
        this.url = url;
        this.handle = handle;
        exit = false;
        if (logger != null) this.logger = logger;
    }

    abstract static public class CltLogger {
        abstract void log(String s);
    }

    public void stopNow() {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ex) {
            }
        }
        exit = true;
    }

    @Override
    public void run() {

        DataInputStream dis = null;
        byte[] data;
        try {
            logger.log("http url:" + url);
            c = (HttpConnection) Connector.open(url);
            int rescode = c.getResponseCode();
            if (rescode == 200) {
                int len = (int) c.getLength();
                dis = c.openDataInputStream();
                if (len > 0) {

                    int part10percent = len / 10;
                    int p = 1;

                    data = new byte[len];
                    int read = 0;
                    while (read < len) {
                        read += dis.read(data, read, len - read);

                        if (len / part10percent > p) {
                            p++;
                            logger.log("Download http data " + p + "0%");
                        }
                    }

                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int ch;
                    while ((ch = dis.read()) != -1 || exit) {

                        baos.write(ch);
                    }
                    data = baos.toByteArray();

                }
                if (handle != null) {
                    handle.onCompleted(this, url, data);
                }
            } else if (rescode == 301 || rescode == 302) {
                String redirect = c.getHeaderField("Location");
                logger.log("redirect:" + redirect);
                MiniHttpClient hc = new MiniHttpClient(redirect, logger, handle);
                hc.start();
            } else {
                if (handle != null) {
                    handle.onCompleted(this, url, null);
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            if (handle != null) {
                handle.onCompleted(this, url, null);
            }
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public interface DownloadCompletedHandle {

        void onCompleted(MiniHttpClient client, String url, byte[] data);
    }

}
