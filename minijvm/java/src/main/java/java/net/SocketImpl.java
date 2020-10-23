/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.net;

import org.mini.net.SocketNative;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Gust
 */
public class SocketImpl implements Closeable {

    byte[] fd;

    String host;
    int port;//remotePort
    String localHost;
    int localport;

    SocketImpl() throws IOException {
        fd = SocketNative.open0();
        //System.out.println("open fd:" + fd);
        if (fd == null) {
            throw new IOException("Init socket error");
        }
    }

    void bind(byte[] sockfd, String host, int port) throws IOException {
        localHost = host;
        localport = port;
        int ret = SocketNative.bind0(sockfd, SocketNative.toCStyle(localHost), SocketNative.toCStyle(localport + ""), SocketNative.NET_PROTO_TCP);
        if (ret < 0) {
            throw new IOException("bind error");
        }
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        if (fd == null) {
            throw new IOException("socket not open");
        }
        if (bindpoint instanceof InetSocketAddress) {
            InetSocketAddress inetBindpoint = (InetSocketAddress) bindpoint;
            localHost = inetBindpoint.getAddress().getHostName();
            localport = inetBindpoint.getPort();
            bind(fd, localHost, localport);
        }
    }

    @Override
    public void close() throws IOException {
        if (fd != null) {
            //System.out.println("close fd:" + fd);
            SocketNative.close0(fd);
            fd = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public String getLocalIp() {
        return localHost;
    }

    public byte[] getFileDesc() {
        return fd;
    }
}
