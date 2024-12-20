package java.net;

import org.mini.net.SocketNative;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Gust
 */
public class Socket extends SocketImpl {

    boolean isConnected = false;

    protected Socket(byte[] sockfd) throws IOException {
        fd = sockfd;
        isConnected = true;
    }

    public Socket() throws IOException {
        super();
    }

    public Socket(InetAddress address, int port) throws IOException {
        this(address.getHostAddress(), port);
    }

    public Socket(String host, int port) throws UnknownHostException, IOException {
        this();
        connect(host, port);
    }

    InputStream inputStream;
    OutputStream outputStream;

    public InputStream getInputStream() throws IOException {
        if (fd == null) {
            throw new IOException("socket not open");
        }
        connect(host, port);
        inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                int r = SocketNative.readByte(fd);
                if (r == -1) {
                    throw new IOException("socket read error");
                } else if (r == -2) {
                    throw new SocketTimeoutException("read timeout");
                }
                return r;
            }

            @Override
            public int read(byte[] b, int offset, int len) throws IOException {
                return SocketNative.read(fd, b, offset, len);
            }
        };
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        if (fd == null) {
            throw new IOException("socket not open");
        }
        connect(host, port);
        outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                int sent = SocketNative.writeByte(fd, b);
                if (sent == -1) {
                    throw new IOException("socket read error");
                } else if (sent == -2) {
                    throw new SocketTimeoutException("read timeout");
                }
            }

            @Override
            public void write(byte[] b, int offset, int len) throws IOException {
                SocketNative.write(fd, b, offset, len);
            }

        };
        return outputStream;
    }

    private void connect(String host, int port) throws IOException {
        if (isConnected) {
            return;
        }
        if (fd != null) {
            this.host = host;
            this.port = port;
            int ret = SocketNative.connect0(fd, SocketNative.toCStyle(host), SocketNative.toCStyle(port + ""), SocketNative.NET_PROTO_TCP);
            if (ret < 0) {
                throw new IOException("socket connect error");
            }
            isConnected = true;

        }
    }

    public void connect(SocketAddress endpoint) throws IOException {
        if (endpoint instanceof InetSocketAddress) {
            InetSocketAddress inetBindpoint = (InetSocketAddress) endpoint;
            connect(inetBindpoint.getAddress().getHostName(), inetBindpoint.getPort());
        }
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        connect(endpoint);
    }


    public void shutdownInput() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    public void shutdownOutput() throws IOException {
        if (outputStream != null) {
            outputStream.close();
        }
    }


    public void setNonBlock(boolean on) throws SocketException {
        if (fd != null) {
            SocketNative.setOption0(fd, SocketNative.SO_NONBLOCK, on ? 1 : 0, 0);
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        if (fd != null) {
            //setOption0(handle,OP_TYPE_NON_BLOCK,OP_VAL_NON_BLOCK);
        }
    }

    public void setSoLinger(boolean on, int linger) {
        SocketNative.setOption0(fd, SocketNative.SO_LINGER, on ? 1 : 0, linger);
    }

    public int getSoLinger() {
        return SocketNative.getOption0(fd, SocketNative.SO_LINGER);
    }

    public void setSoTimeout(int millSecond) {
        SocketNative.setOption0(fd, SocketNative.SO_TIMEOUT, millSecond, 0);
    }

    public int getSoTimeout() {
        return SocketNative.getOption0(fd, SocketNative.SO_TIMEOUT);
    }

    public void setSendBufferSize(int size) {
        SocketNative.setOption0(fd, SocketNative.SO_SNDBUF, size, 0);
    }

    public int getSendBufferSize() {
        return SocketNative.getOption0(fd, SocketNative.SO_SNDBUF);
    }

    public void setReceiveBufferSize(int size) {
        SocketNative.setOption0(fd, SocketNative.SO_RCVBUF, size, 0);
    }

    public int getReceiveBufferSize() {
        return SocketNative.getOption0(fd, SocketNative.SO_RCVBUF);
    }

    public void setKeepAlive(boolean on) {
        SocketNative.setOption0(fd, SocketNative.SO_KEEPALIVE, on ? 1 : 0, 0);
    }

    public boolean getKeepAlive() {
        return SocketNative.getOption0(fd, SocketNative.SO_KEEPALIVE) == 0 ? false : true;
    }

    public SocketAddress getRemoteSocketAddress() {
        String s = SocketNative.getSockAddr(fd, 0);
        if (s != null) {
            int index = s.lastIndexOf(':');
            if (index >= 0) {
                host = s.substring(0, index);
                port = Integer.parseInt(s.substring(index + 1));
            }
        }
        InetSocketAddress isa = new InetSocketAddress(host, port);
        return isa;
    }

    public SocketAddress getLocalSocketAddress() {
        String s = SocketNative.getSockAddr(fd, 1);
        if (s != null) {
            int index = s.lastIndexOf(':');
            if (index >= 0) {
                localHost = s.substring(0, index);
                localport = Integer.parseInt(s.substring(index + 1));
            }
        }
        InetSocketAddress isa = new InetSocketAddress(localHost, localport);
        return isa;
    }

    public String toString() {
        return getRemoteSocketAddress() + "/" + getLocalSocketAddress();
    }
}
