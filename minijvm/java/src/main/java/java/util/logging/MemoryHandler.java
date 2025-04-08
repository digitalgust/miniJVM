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


public class MemoryHandler extends Handler {
    private final static int DEFAULT_SIZE = 1000;
    private Level pushLevel;
    private int size;
    private Handler target;
    private LogRecord buffer[];
    int start, count;


    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = MemoryHandler.class.getName();

        pushLevel = manager.getLevelProperty(cname + ".push", Level.SEVERE);
        size = manager.getIntProperty(cname + ".size", DEFAULT_SIZE);
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        setLevel(manager.getLevelProperty(cname + ".level", Level.ALL));
        setFilter(manager.getFilterProperty(cname + ".filter", null));
        setFormatter(manager.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
    }


    public MemoryHandler() {
        sealed = false;
        configure();
        sealed = true;

        String name = "???";
        try {
            LogManager manager = LogManager.getLogManager();
            name = manager.getProperty("java.util.logging.MemoryHandler.target");
            Class clz = ClassLoader.getSystemClassLoader().loadClass(name);
            target = (Handler) clz.newInstance();
        } catch (Exception ex) {
            System.err.println("MemoryHandler can't load handler \"" + name + "\"");
            System.err.println("" + ex);
            throw new RuntimeException("Can't load " + name);
        }
        init();
    }


    private void init() {
        buffer = new LogRecord[size];
        start = 0;
        count = 0;
    }


    public MemoryHandler(Handler target, int size, Level pushLevel) {
        if (target == null || pushLevel == null) {
            throw new NullPointerException();
        }
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        sealed = false;
        configure();
        sealed = true;
        this.target = target;
        this.pushLevel = pushLevel;
        this.size = size;
        init();
    }


    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        int ix = (start + count) % buffer.length;
        buffer[ix] = record;
        if (count < buffer.length) {
            count++;
        } else {
            start++;
        }
        if (record.getLevel().intValue() >= pushLevel.intValue()) {
            push();
        }
    }


    public synchronized void push() {
        for (int i = 0; i < count; i++) {
            int ix = (start + i) % buffer.length;
            LogRecord record = buffer[ix];
            target.publish(record);
        }

        start = 0;
        count = 0;
    }


    public void flush() {
        target.flush();
    }


    public void close() throws SecurityException {
        target.close();
        setLevel(Level.OFF);
    }


    public void setPushLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException();
        }
        LogManager manager = LogManager.getLogManager();
        checkAccess();
        pushLevel = newLevel;
    }


    public synchronized Level getPushLevel() {
        return pushLevel;
    }


    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record);
    }
}
