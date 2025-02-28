/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.http;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gust
 */
public class MiniHttpClient extends Thread {

    String url;
    ByteArrayOutputStream baos;
    DownloadCompletedHandle handle;
    Map<String, String> outHeaders = new HashMap();
    boolean exit;
    HttpConnection c = null;
    public static final CltLogger DEFAULT_LOGGER = new CltLogger() {
        @Override
        public void log(String s) {
            System.out.println(s);
        }
    };
    CltLogger logger = DEFAULT_LOGGER;
    ProgressListener progressListener;

    public MiniHttpClient(final String url, CltLogger logger, final DownloadCompletedHandle handle) {
        this.url = url;
        this.handle = handle;
        exit = false;
        if (logger != null) this.logger = logger;
    }

    public void setPostData(ByteArrayOutputStream baos) {
        this.baos = baos;
    }

    public void setPostData(String str) {
        if (str == null) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(str.getBytes("utf-8"));
            this.baos = baos;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setHeader(String key, String value) {
        outHeaders.put(key, value);
    }

    void outputHeaders() {
        try {
            for (String key : outHeaders.keySet()) {
                c.setRequestProperty(key, outHeaders.get(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract static public class CltLogger {
        public abstract void log(String s);
    }

    public static interface ProgressListener {
        public void onProgress(MiniHttpClient client, int progress);
    }

    public void stopNow() {
        if (c != null) {
            try {
                updateProgress(100);
                c.close();
            } catch (IOException ex) {
            }
        }
        exit = true;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void run() {

        DataInputStream dis = null;
        byte[] data;
        try {
            logger.log("http url:" + url);
            updateProgress(5);
            c = (HttpConnection) Connector.open(url);
            outputHeaders();
            if (baos != null) {
                c.setRequestMethod(HttpConnection.POST);
                byte[] d = baos.toByteArray();
                c.setRequestProperty("Content-Length", String.valueOf(d.length));
                c.openDataOutputStream().write(d);
            } else {
                c.setRequestMethod(HttpConnection.GET);
            }
            int rescode = c.getResponseCode();
            if (rescode == 200) {
                int len = (int) c.getLength();
                dis = c.openDataInputStream();
                if (len > 0) {

                    int part10percent = len / 100;
                    int p = 1;

                    data = new byte[len];
                    byte[] buf = new byte[4096];
                    int read = 0;
                    while (read < len) {
                        //read += dis.read(data, read, len - read);
                        int r = dis.read(buf);
                        if (r == -1) {
                            break;
                        }
                        System.arraycopy(buf, 0, data, read, r);
                        read += r;

                        //System.out.println("read:" + read);
                        if (read > part10percent * p) {
                            p++;
                            updateProgress(p);
                        }
                    }
                } else {
                    updateProgress(20);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int ch;
                    while ((ch = dis.read()) != -1 || exit) {
                        baos.write(ch);
                        updateProgress(50);
                    }
                    data = baos.toByteArray();
                }
                updateProgress(100);
                if (handle != null) {
                    handle.onCompleted(this, url, data);
                }
            } else if (rescode == 301 || rescode == 302) {
                String redirect = c.getHeaderField("Location");
                logger.log("redirect:" + redirect);
                MiniHttpClient hc = new MiniHttpClient(redirect, logger, handle);
                hc.setPostData(baos);
                hc.outHeaders = outHeaders;
                hc.setProgressListener(getProgressListener());
                hc.start();
            } else {
                updateProgress(100);
                if (handle != null) {
                    handle.onCompleted(this, url, null);
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            updateProgress(100);
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

    public String getHeader(String key) {
        try {
            return c.getHeaderField(key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface DownloadCompletedHandle {

        void onCompleted(MiniHttpClient client, String url, byte[] data);
    }

    private void updateProgress(int progress) {
        if (progressListener != null) {
            progressListener.onProgress(this, progress);
        }
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }
}
