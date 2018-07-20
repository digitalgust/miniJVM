/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import org.mini.net.SocketNative;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cldc.io.Connector;
import javax.cldc.io.ContentConnection;
import javax.cldc.io.NBServerSocket;
import javax.cldc.io.NBSocket;

/**
 *
 * @author gust
 */
public class HttpServer {

    public static void main(String args[]) {
        HttpServer f = new HttpServer();
//        f.t12();
//        f.t13();
//        f.t18();
        f.t20();
        f.t21();
    }

    void t12() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final NBServerSocket srvsock = (NBServerSocket) Connector.open("serversocket://:8080");

                    //建一个线程，过5秒钟关掉自己
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                int MAX = 50;
                                for (int i = 0; i < MAX; i++) {
                                    System.out.println("server would close at " + (MAX - i) + " second later.");
                                    Thread.sleep(1000);
                                }
                                if (srvsock != null) {
                                    srvsock.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }).start();
                    System.out.println("server socket listen 8080...");
                    srvsock.listen();
                    while (true) {
                        try {
                            NBSocket cltsock;
                            try {
                                cltsock = srvsock.accept();
                            } catch (IOException e) {
                                break;
                            }
                            cltsock.setOption(SocketNative.SO_BLOCK, SocketNative.VAL_NON_BLOCK, 0);
                            System.out.println("accepted client socket:" + cltsock);
                            byte[] buf = new byte[256];
                            StringBuffer tmps = new StringBuffer();
                            int rlen;
                            while ((rlen = cltsock.read(buf, 0, 256)) != -1) {
                                String s = new String(buf, 0, rlen);
                                tmps.append(s);
                                String s1 = tmps.toString();
                                if (s1.indexOf("\n\n") >= 0 || s1.indexOf("\r\n\r\n") >= 0) {
                                    break;
                                }
                            }
                            //System.out.println("RECV: " + tmps.toString());
                            String sbuf = "HTTP/1.0 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\nFor mini_jvm test. ( EGLS Beijing co.,ltd)" + Calendar.getInstance().getTime().toString();
                            int sent = 0;
                            while ((sent) < sbuf.length()) {
                                int wlen = cltsock.write(sbuf.getBytes(), sent, sbuf.length() - sent);
                                if (wlen == -1) {
                                    break;
                                }
                                sent += wlen;
                            }
                            cltsock.close();
                            if (false) {
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    srvsock.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    void t13() {
        for (int j = 0; j < 1; j++) {

            try {
                Thread.sleep(2000);

                NBSocket conn = (NBSocket) Connector.open("socket://127.0.0.1:8080");
                conn.setOption(SocketNative.SO_BLOCK, SocketNative.VAL_NON_BLOCK, 0);
                String request = "GET / HTTP/1.1\r\n\r\n";
                conn.write(request.getBytes(), 0, request.length());
                byte[] rcvbuf = new byte[256];
                int len = 0;
                int zero = 0;
                while (len != -1) {
                    len = conn.read(rcvbuf, 0, 256);
                    if (len == 0) {
                        zero++;
                    }
                    if (zero > 3000000) {
                        break;
                    }
                    for (int i = 0; i < len; i++) {
                        System.out.print((char) rcvbuf[i]);
                    }

                };
                System.out.print("\nend\n");
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    byte[] getViaContentConnection(String url) throws IOException {
        ContentConnection c = null;
        DataInputStream dis = null;
        byte[] data;
        try {
            System.out.println("url:" + url);
            c = (ContentConnection) Connector.open(url);
            int len = (int) c.getLength();
            dis = c.openDataInputStream();
            if (len > 0) {
                data = new byte[len];
                dis.readFully(data);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = dis.read()) != -1) {

                    baos.write(ch);
                }
                data = baos.toByteArray();
            }
        } finally {
            if (dis != null) {
                dis.close();
            }
            if (c != null) {
                c.close();
            }
        }
        return data;
    }

    void t18() {
        String url = "http://360.cn/";
        System.out.println("Connect to :" + url);
        try {
            byte[] data = getViaContentConnection(url);
            for (int i = 0; i < data.length; i++) {
                System.out.print((char) data[i]);
            }
        } catch (IOException e) {
//            System.out.println(ex.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    class RequestHandle extends Thread {

        Socket cltsock;

        RequestHandle(Socket sock) {
            this.cltsock = sock;
        }

        public void run() {
            try {
                StringBuilder tmps = new StringBuilder();
                //cltsock.setSoTimeout(20);
                InputStream in = cltsock.getInputStream();
                int ch = 0;
                while ((ch) != -1) {
                    try {

                        ch = in.read();

                    } catch (SocketTimeoutException e) {
                        System.out.println("timeout " + cltsock.toString());
                        break;
                    }
                    tmps.append((char) ch);

                    if (tmps.indexOf("\n\n") >= 0 || tmps.indexOf("\r\n\r\n") >= 0) {
                        break;
                    }
                }
                System.out.println("receive:" + tmps.toString());
                OutputStream out = cltsock.getOutputStream();
                String sbuf = "HTTP/1.0 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\nFor mini_jvm test. ( EGLS Beijing co.,ltd)" + Calendar.getInstance().getTime().toString();
                int sent = 0;
                while ((sent) < sbuf.length()) {
                    out.write(sbuf.charAt(sent));
                    sent++;
                }
                cltsock.close();
            } catch (IOException ex) {
                Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void t20() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    int port = 8088;
                    final ServerSocket srvsock = new ServerSocket(port);

                    //建一个线程，过5秒钟关掉自己
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                int MAX = 50;
                                for (int i = 0; i < MAX; i++) {
                                    System.out.println("server would close at " + (MAX - i) + " second later.");
                                    Thread.sleep(1000);
                                }
                                if (srvsock != null) {
                                    srvsock.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }).start();
                    System.out.println("server socket listen " + port + "...");
                    while (true) {
                        try {
                            Socket cltsock;
                            try {
                                cltsock = srvsock.accept();
                            } catch (IOException e) {
                                break;
                            }
                            System.out.println("accepted client socket:" + cltsock);

                            RequestHandle handler = new RequestHandle(cltsock);
                            handler.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    srvsock.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    void t21() {
        for (int j = 0; j < 1; j++) {

            try {
                Thread.sleep(2000);

                Socket socket = new Socket("127.0.0.1", 8080);
                String request = "GET / HTTP/1.1\r\n\r\n";
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                out.write(request.getBytes(), 0, request.length());
                byte[] rcvbuf = new byte[256];
                int len = 0;
                int zero = 0;
                while (len != -1) {
                    len = in.read(rcvbuf, 0, 256);
                    if (len == 0) {
                        zero++;
                    }
                    if (zero > 3000000) {
                        break;
                    }
                    for (int i = 0; i < len; i++) {
                        System.out.print((char) rcvbuf[i]);
                    }

                };
                System.out.print("\nend\n");
                socket.close();
            } catch (Exception e) {

            }
        }
    }

}
