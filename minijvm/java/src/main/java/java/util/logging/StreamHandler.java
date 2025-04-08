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


public class StreamHandler extends Handler {
    private LogManager manager = LogManager.getLogManager();
    private OutputStream output;
    private boolean doneHeader;
    private Writer writer;


    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = StreamHandler.class.getName();

        setLevel(manager.getLevelProperty(cname + ".level", Level.INFO));
        setFilter(manager.getFilterProperty(cname + ".filter", null));
        setFormatter(manager.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        try {
            setEncoding(manager.getStringProperty(cname + ".encoding", null));
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {


            }
        }
    }


    public StreamHandler() {
        sealed = false;
        configure();
        sealed = true;
    }


    public StreamHandler(OutputStream out, Formatter formatter) {
        sealed = false;
        configure();
        setFormatter(formatter);
        setOutputStream(out);
        sealed = true;
    }


    protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
        if (out == null) {
            throw new NullPointerException();
        }
        flushAndClose();
        output = out;
        doneHeader = false;
        String encoding = getEncoding();
        if (encoding == null) {
            writer = new OutputStreamWriter(output);
        } else {
            try {
                writer = new OutputStreamWriter(output, encoding);
            } catch (UnsupportedEncodingException ex) {


                throw new Error("Unexpected exception " + ex);
            }
        }
    }


    public void setEncoding(String encoding)
            throws SecurityException, UnsupportedEncodingException {
        super.setEncoding(encoding);
        if (output == null) {
            return;
        }

        flush();
        if (encoding == null) {
            writer = new OutputStreamWriter(output);
        } else {
            writer = new OutputStreamWriter(output, encoding);
        }
    }


    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String msg;
        try {
            msg = getFormatter().format(record);
        } catch (Exception ex) {


            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }

        try {
            if (!doneHeader) {
                writer.write(getFormatter().getHead(this));
                doneHeader = true;
            }
            writer.write(msg);
        } catch (Exception ex) {


            reportError(null, ex, ErrorManager.WRITE_FAILURE);
        }
    }


    public boolean isLoggable(LogRecord record) {
        if (writer == null) {
            return false;
        }
        return super.isLoggable(record);
    }


    public synchronized void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (Exception ex) {


                reportError(null, ex, ErrorManager.FLUSH_FAILURE);
            }
        }
    }

    private synchronized void flushAndClose() throws SecurityException {
        checkAccess();
        if (writer != null) {
            try {
                if (!doneHeader) {
                    writer.write(getFormatter().getHead(this));
                    doneHeader = true;
                }
                writer.write(getFormatter().getTail(this));
                writer.flush();
                writer.close();
            } catch (Exception ex) {


                reportError(null, ex, ErrorManager.CLOSE_FAILURE);
            }
            writer = null;
            output = null;
        }
    }


    public synchronized void close() throws SecurityException {
        flushAndClose();
    }
}
