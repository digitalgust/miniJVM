
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


public abstract class Handler {
    private static final int offValue = Level.OFF.intValue();
    private LogManager manager = LogManager.getLogManager();
    private Filter filter;
    private Formatter formatter;
    private Level logLevel = Level.ALL;
    private ErrorManager errorManager = new ErrorManager();
    private String encoding;


    boolean sealed = true;


    protected Handler() {
    }


    public abstract void publish(LogRecord record);


    public abstract void flush();


    public abstract void close() throws SecurityException;


    public void setFormatter(Formatter newFormatter) throws SecurityException {
        checkAccess();

        newFormatter.getClass();
        formatter = newFormatter;
    }


    public Formatter getFormatter() {
        return formatter;
    }


    public void setEncoding(String encoding)
            throws SecurityException, java.io.UnsupportedEncodingException {
        checkAccess();
        if (encoding != null) {

        }
        this.encoding = encoding;
    }


    public String getEncoding() {
        return encoding;
    }


    public void setFilter(Filter newFilter) throws SecurityException {
        checkAccess();
        filter = newFilter;
    }


    public Filter getFilter() {
        return filter;
    }


    public void setErrorManager(ErrorManager em) {
        checkAccess();
        if (em == null) {
            throw new NullPointerException();
        }
        errorManager = em;
    }


    public ErrorManager getErrorManager() {
        checkAccess();
        return errorManager;
    }


    protected void reportError(String msg, Exception ex, int code) {
        try {
            errorManager.error(msg, ex, code);
        } catch (Exception ex2) {
            System.err.println("Handler.reportError caught:");
            ex2.printStackTrace();
        }
    }


    public synchronized void setLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException();
        }
        checkAccess();
        logLevel = newLevel;
    }


    public synchronized Level getLevel() {
        return logLevel;
    }


    public boolean isLoggable(LogRecord record) {
        int levelValue = getLevel().intValue();
        if (record.getLevel().intValue() < levelValue || levelValue == offValue) {
            return false;
        }
        Filter filter = getFilter();
        if (filter == null) {
            return true;
        }
        return filter.isLoggable(record);
    }


    void checkAccess() throws SecurityException {
        if (sealed) {
            manager.checkAccess();
        }
    }
}
