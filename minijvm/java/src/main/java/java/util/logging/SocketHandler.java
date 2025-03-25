
/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util.logging;

import java.io.*;
import java.net.*;


public class SocketHandler extends StreamHandler {
    private Socket sock;
    private String host;
    private int port;
    private String portProperty;


    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = SocketHandler.class.getName();

        setLevel(manager.getLevelProperty(cname + ".level", Level.ALL));
        setFilter(manager.getFilterProperty(cname + ".filter", null));
        setFormatter(manager.getFormatterProperty(cname + ".formatter", new XMLFormatter()));
        try {
            setEncoding(manager.getStringProperty(cname + ".encoding", null));
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {


            }
        }
        port = manager.getIntProperty(cname + ".port", 0);
        host = manager.getStringProperty(cname + ".host", null);
    }


    public SocketHandler() throws IOException {

        sealed = false;
        configure();

        try {
            connect();
        } catch (IOException ix) {
            System.err.println("SocketHandler: connect failed to " + host + ":" + port);
            throw ix;
        }
        sealed = true;
    }


    public SocketHandler(String host, int port) throws IOException {
        sealed = false;
        configure();
        sealed = true;
        this.port = port;
        this.host = host;
        connect();
    }

    private void connect() throws IOException {

        if (port == 0) {
            throw new IllegalArgumentException("Bad port: " + port);
        }
        if (host == null) {
            throw new IllegalArgumentException("Null host name: " + host);
        }


        sock = new Socket(host, port);
        OutputStream out = sock.getOutputStream();
        BufferedOutputStream bout = new BufferedOutputStream(out);
        setOutputStream(bout);
    }


    public synchronized void close() throws SecurityException {
        super.close();
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException ix) {

            }
        }
        sock = null;
    }


    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
    }
}
