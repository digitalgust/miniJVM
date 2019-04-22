/*
 * @(#)Socket.java	1.15 02/10/14 @(#)
 *
 * Copyright (c) 1999-2002 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */
package org.mini.net.serversocket;

import com.sun.cldc.io.ConnectionBaseInterface;
import java.io.IOException;
import javax.cldc.io.Connection;
import javax.cldc.io.Connector;
import org.mini.net.SocketNative;
import javax.cldc.io.ServerSocketConnection;


/*
 * Note: Since this class references the TCP socket protocol class that
 * extends the NetworkConnectionBaseClass. The native networking will be
 * initialized when this class loads if needed without extending
 * NetworkConnectionBase.
 */
/**
 * StreamConnectionNotifier to the TCP Server Socket API.
 *
 * @author Nik Shaylor
 * @version 1.0 10/08/99
 */
public class Protocol implements ConnectionBaseInterface, ServerSocketConnection {

    /**
     * Socket object used by native code, for now must be the first field.
     */
    private int handle;

    /**
     * Flag to indicate connection is currently open.
     */
    boolean connectionOpen = false;

    int port;

    String ip;

    @Override
    public Connection openPrim(String name, int mode, boolean timeouts) throws IOException {
        if (!name.startsWith("//")) {
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "bad socket connection name: " + name
                    /* #endif */);
        }
        int i = name.indexOf(':');
        if (i < 0) {
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "bad socket connection name: port missing"
                    /* #endif */);
        }
        String hostname = name.substring(2, i);
        int port;
        try {
            port = Integer.parseInt(name.substring(i + 1));
        } catch (NumberFormatException e) {
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "bad socket connection name: bad port"
                    /* #endif */);
        }
        // cstring is always NUL terminated (note the extra byte allocated).
        // This avoids awkward char array manipulation in C code.
        byte cstring[] = new byte[hostname.length()];
        for (int n = 0; n < hostname.length(); n++) {
            cstring[n] = (byte) (hostname.charAt(n));
        }
        if ((this.handle = SocketNative.open0()) < 0) {
            if (this.handle < 0);
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "connection failed: error = " + errorCode
                    /* #endif */);
        }
        if(SocketNative.bind0(this.handle,cstring, port)<0){
            throw new IOException("bind error");
        }
        this.connectionOpen = true;
        return this;
    }

    /**
     * Checks if the connection is open.
     *
     * @exception IOException is thrown, if the stream is not open
     */
    void ensureOpen() throws IOException {
        if (!connectionOpen) {
            throw new IOException("Connection closed");
        }
    }

    /**
     * Returns a connection that represents a server side socket connection.
     * <p>
     * Polling the native code is done here to allow for simple asynchronous
     * native code to be written. Not all implementations work this way (they
     * block in the native code) but the same Java code works for both.
     *
     * @return a socket to communicate with a client.
     *
     * @exception IOException if an I/O error occurs when creating the input
     * stream
     */
    synchronized public javax.cldc.io.SocketConnection accept()
            throws IOException {

        org.mini.net.socket.Protocol con;

        ensureOpen();

        while (true) {
            int clt_handle = SocketNative.accept0(this.handle);
            if (clt_handle >= 0) {
                con = new org.mini.net.socket.Protocol();
                con.open(clt_handle, Connector.READ_WRITE);
                break;
            }else{
                throw new IOException("accept error, maybe listen() before accept()");
            }
            /* Wait a while for I/O to become ready */
            //Waiter.waitForIO(); 
        }

        return con;
    }

    /**
     * Gets the local address to which the socket is bound.
     *
     * <P>
     * The host address(IP number) that can be used to connect to this end of
     * the socket connection from an external system. Since IP addresses may be
     * dynamically assigned, a remote application will need to be robust in the
     * face of IP number reasssignment.</P>
     * <P>
     * The local hostname (if available) can be accessed from
     * <code> System.getProperty("microedition.hostname")</code>
     * </P>
     *
     * @return the local address to which the socket is bound
     * @exception IOException if the connection was closed
     * @see ServerSocketConnection
     */
    public String getLocalAddress() throws IOException {
        ensureOpen();
        return ip + ":" + port;
    }

    /**
     * Returns the local port to which this socket is bound.
     *
     * @return the local port number to which this socket is connected
     * @exception IOException if the connection was closed
     * @see ServerSocketConnection
     */
    public int getLocalPort() throws IOException {
        ensureOpen();
        return port;
    }

    /**
     * Closes the connection, accesses the handle field.
     *
     * @exception IOException if an I/O error occurs when closing the connection
     */
    public void close() throws IOException {
        if (connectionOpen) {
            SocketNative.close0(handle);
            connectionOpen = false;
        }
    }



    @Override
    public void listen() throws IOException {
        SocketNative.listen0(handle);
    }

}
