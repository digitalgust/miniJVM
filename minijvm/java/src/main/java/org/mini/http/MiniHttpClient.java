/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.http;

import org.mini.util.SysLog;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.*;
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
            SysLog.info(s);
        }
    };
    CltLogger logger = DEFAULT_LOGGER;
    ProgressListener progressListener;
    File downloadFile = null;

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
        try {
            //logger.log("[INFO]http url:" + url);
            updateProgress(5);
            c = (HttpConnection) Connector.open(url);
            if (baos != null) {
                c.setRequestMethod(HttpConnection.POST);
                outputHeaders();
                byte[] d = baos.toByteArray();
                c.setRequestProperty("Content-Length", String.valueOf(d.length));
                c.openDataOutputStream().write(d);
            } else {
                c.setRequestMethod(HttpConnection.GET);
                outputHeaders();
            }
            int rescode = c.getResponseCode();
            if (rescode == 200) {
                int len = (int) c.getLength();
                dis = c.openDataInputStream();
                downloadFile = File.createTempFile("cache", ".tmp");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(downloadFile);

                    if (len > 0) {

                        int part10percent = len / 100;
                        int p = 1;

                        byte[] buf = new byte[4096];
                        int read = 0;
                        while (read < len) {
                            //read += dis.read(data, read, len - read);
                            int r = dis.read(buf);
                            if (r == -1) {
                                break;
                            }
                            fos.write(buf, 0, r);
                            read += r;

                            //System.out.println("read:" + read);
                            if (read > part10percent * p) {
                                p++;
                                updateProgress(p);
                            }
                        }

                    } else {
                        updateProgress(20);
                        int ch;
                        while ((ch = dis.read()) != -1 || exit) {
                            fos.write(ch);
                            updateProgress(50);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.log("[ERROR]http error:" + e.getMessage());
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e1) {
                        }
                    }
                }
                updateProgress(100);
                doHandler();
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
                doHandler();
            }
        } catch (Exception e) {
            //logger.log("[ERRO]http error:" + e.getCodeStack());
            updateProgress(100);
            doHandler();
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

    private void doHandler() {
        if (handle != null) {
            if (handle instanceof DownloadFileHandle) {
                ((DownloadFileHandle) handle).onCompleted(this, url, downloadFile);
            } else {
                byte[] data = readFile(downloadFile);
                handle.onCompleted(this, url, data);
            }
        }
    }

    public static byte[] readFile(File file) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            int flen = (int) file.length();
            byte[] data = new byte[flen];
            int read = 0;
            while ((read += fis.read(data, read, flen - read)) < flen) ;
            return data;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }


    /**
     * 下载数据按文件处理
     */
    public interface DownloadFileHandle extends DownloadCompletedHandle {

        /**
         * 文件下载完成
         *
         * @param client
         * @param url
         * @param downloadFile
         */

        abstract void onCompleted(MiniHttpClient client, String url, File downloadFile);

        /**
         * 不允许继承子类处理这个方法
         * 原因是，这个byte[] data参数，是下载数据，未知情况下不知道有多大，可能造成内存溢出
         *
         * @param client
         * @param url
         * @param data
         */
        default void onCompleted(MiniHttpClient client, String url, byte[] data) {
        }
    }

    /**
     * 下载数据直接返回
     */
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
