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

    public static native int open0();

    public static native int bind0(int handle, byte hostname[], int port);

    public static native int connect0(int handle, byte hostname[], int port);

    public static native int listen0(int handle);

    public static native int accept0(int handle);

    private static native int readBuf(int handle, byte b[], int off, int len);

    public static native int readByte(int handle);

    private static native int writeBuf(int handle, byte b[], int off, int len);

    public static native int writeByte(int handle, int b);

    public static native int available0(int handle);

    public static native void close0(int handle);

    public static native int setOption0(int handle, int type, int val, int val2);

    public static native int getOption0(int handle, int type);

    public static native int host2ip4(byte[] hostname);

    //mode=0 get peername , mode=1 getlocalname
    public static native String getSockAddr(int handle, int mode);

    static public int write(int handle, byte[] b, int off, int len) throws IOException {
        int w = SocketNative.writeBuf(handle, b, off, len);
        if (w == -2) {
            w = 0;
        }
        return w;
    }

    static public int read(int handle, byte[] b, int off, int len) throws IOException {
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
