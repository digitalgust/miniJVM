/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cldc.io.Connector;
import javax.cldc.io.ContentConnection;

/**
 *
 * @author Gust
 */
public class MiniHttpClient extends Thread {

    String url;
    DownloadCompletedHandle handle;
    boolean exit;
    ContentConnection c = null;

    public MiniHttpClient(final String url, final DownloadCompletedHandle handle) {
        this.url = url;
        this.handle = handle;
        exit = false;
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
            System.out.println("http url:" + url);
            c = (ContentConnection) Connector.open(url);
            int len = (int) c.getLength();
            dis = c.openDataInputStream();
            if (len > 0) {
                data = new byte[len];
                dis.readFully(data);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = dis.read()) != -1 || exit) {

                    baos.write(ch);
                }
                data = baos.toByteArray();

            }
            if (handle != null) {
                handle.onCompleted(url, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        void onCompleted(String url, byte[] data);
    }

}
