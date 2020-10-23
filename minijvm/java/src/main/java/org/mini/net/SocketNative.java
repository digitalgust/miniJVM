/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.net;

import java.io.IOException;

/**
 * @author Gust
 */
public class SocketNative {

    public static final byte VAL_BLOCK = 0;
    public static final byte SO_SNDBUF = 3;
    public static final byte SO_KEEPALIVE = 4;
    public static final byte VAL_REUSEADDR = 0;
    public static final byte SO_RCVBUF = 2;
    public static final byte SO_LINGER = 5;
    public static final byte SO_NONBLOCK = 0;
    public static final byte VAL_NON_BLOCK = 1;
    public static final byte VAL_NON_REUSEADDR = 1;
    public static final byte SO_REUSEADDR = 1;
    public static final byte SO_TIMEOUT = 6;

    /**
     * < The TCP transport protocol
     */
    public static final int NET_PROTO_TCP = 0;

    /**
     * < The UDP transport protocol
     */
    public static final int NET_PROTO_UDP = 1;

    public static byte[] toCStyle(String s) {
        try {
            return (s + "\0").getBytes("utf-8");
        } catch (Exception e) {
        }
        return null;
    }


    public static native byte[] open0();

    public static native int bind0(byte[] handle, byte[] hostname, byte[] port, int tcp_udp);

    public static native int connect0(byte[] handle, byte[] hostname, byte[] port, int tcp_udp);

    public static native byte[] accept0(byte[] handle);

    private static native int readBuf(byte[] handle, byte b[], int off, int len);

    public static native int readByte(byte[] handle);

    private static native int writeBuf(byte[] handle, byte b[], int off, int len);

    public static native int writeByte(byte[] handle, int b);

    public static native int available0(byte[] handle);

    public static native void close0(byte[] handle);

    public static native int setOption0(byte[] handle, int type, int val, int val2);

    public static native int getOption0(byte[] handle, int type);

    public static native byte[] host2ip(byte[] hostname);

    //mode=0 get peername , mode=1 getlocalname
    public static native String getSockAddr(byte[] handle, int mode);

    static public int write(byte[] handle, byte[] b, int off, int len) throws IOException {
        int w = SocketNative.writeBuf(handle, b, off, len);
        if (w == -2) {
            w = 0;
        }
        return w;
    }

    static public int read(byte[] handle, byte[] b, int off, int len) throws IOException {
        int r = SocketNative.readBuf(handle, b, off, len);
        if (r == -2) {
            r = 0;
        }
        return r;
    }


    //https implementation

    public static native byte[] sslc_construct_entry();

    public static native int sslc_init(byte[] hi);

    public static native int sslc_connect(byte[] hi, byte[] host, byte[] port);

    public static native int sslc_close(byte[] hi);

    public static native int sslc_write(byte[] hi, byte[] data, int offset, int len);

    public static native int sslc_read(byte[] hi, byte[] data, int offset, int len);

}
