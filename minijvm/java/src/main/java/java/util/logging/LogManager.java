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
import java.util.*;


public class LogManager {

    private static LogManager manager;

    private Properties props = new Properties();
    private final static Level defaultLevel = Level.INFO;


    private Hashtable loggers = new Hashtable();

    private LogNode root = new LogNode(null);
    private Logger rootLogger;


    private volatile boolean readPrimordialConfiguration;


    private boolean initializedGlobalHandlers = true;

    private boolean deathImminent;

    static {

        if (manager == null) {
            manager = new LogManager();
        }


        manager.rootLogger = manager.new RootLogger();
        manager.addLogger(manager.rootLogger);


        Logger.global.setLogManager(manager);
        manager.addLogger(Logger.global);


    }


    private class Cleaner extends Thread {
        public void run() {
            synchronized (LogManager.this) {
                deathImminent = true;
                initializedGlobalHandlers = true;
            }
            reset();
        }
    }


    protected LogManager() {
        try {
            Runtime.getRuntime().addShutdownHook(new Cleaner());
        } catch (IllegalStateException e) {
        }
    }


    public static LogManager getLogManager() {
        if (manager != null) {
            manager.readPrimordialConfiguration();
        }
        return manager;
    }

    private void readPrimordialConfiguration() {
        if (!readPrimordialConfiguration) {
            synchronized (this) {
                if (!readPrimordialConfiguration) {
                    if (System.out == null) {
                        return;
                    }
                    readPrimordialConfiguration = true;
                }
            }
        }
    }


    public synchronized boolean addLogger(Logger logger) {
        String name = logger.getName();
        if (name == null) {
            throw new NullPointerException();
        }

        Logger old = (Logger) loggers.get(name);
        if (old != null) {

            return false;
        }


        loggers.put(name, logger);


        Level level = getLevelProperty(name + ".level", null);
        if (level != null) {
            doSetLevel(logger, level);
        }
        int ix = 1;
        for (; ; ) {
            int ix2 = name.indexOf(".", ix);
            if (ix2 < 0) {
                break;
            }
            String pname = name.substring(0, ix2);
            if (getProperty(pname + ".level") != null) {

                Logger plogger = Logger.getLogger(pname);
            }
            ix = ix2 + 1;
        }


        LogNode node = findNode(name);
        node.logger = logger;
        Logger parent = null;
        LogNode nodep = node.parent;
        while (nodep != null) {
            if (nodep.logger != null) {
                parent = nodep.logger;
                break;
            }
            nodep = nodep.parent;
        }

        if (parent != null) {
            doSetParent(logger, parent);
        }

        node.walkAndSetParent(logger);

        return true;
    }


    private static void doSetLevel(final Logger logger, final Level level) {
        logger.setLevel(level);

    }


    private static void doSetParent(final Logger logger, final Logger parent) {

        logger.setParent(parent);
    }


    private LogNode findNode(String name) {
        if (name == null || name.equals("")) {
            return root;
        }
        LogNode node = root;
        while (name.length() > 0) {
            int ix = name.indexOf(".");
            String head;
            if (ix > 0) {
                head = name.substring(0, ix);
                name = name.substring(ix + 1);
            } else {
                head = name;
                name = "";
            }
            if (node.children == null) {
                node.children = new HashMap();
            }
            LogNode child = (LogNode) node.children.get(head);
            if (child == null) {
                child = new LogNode(node);
                node.children.put(head, child);
            }
            node = child;
        }
        return node;
    }


    public synchronized Logger getLogger(String name) {
        return (Logger) loggers.get(name);
    }


    public synchronized Enumeration getLoggerNames() {
        return loggers.keys();
    }


    public void readConfiguration() throws IOException, SecurityException {


        String fname = System.getProperty("java.util.logging.config.file");
        if (fname == null) {
            fname = System.getProperty("java.home");
            if (fname == null) {
                throw new Error("Can't find java.home ??");
            }
            File f = new File(fname, "lib");
            f = new File(f, "logging.properties");
            fname = f.getCanonicalPath();
        }
        InputStream in = new FileInputStream(fname);
        BufferedInputStream bin = new BufferedInputStream(in);
        try {
            readConfiguration(bin);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }


    public void reset() throws SecurityException {
        checkAccess();
        synchronized (this) {
            props = new Properties();


            initializedGlobalHandlers = true;
        }
        Enumeration enum1 = getLoggerNames();
        while (enum1.hasMoreElements()) {
            String name = (String) enum1.nextElement();
            resetLogger(name);
        }
    }


    private void resetLogger(String name) {
        Logger logger = getLogger(name);
        if (logger == null) {
            return;
        }

        Handler[] targets = logger.getHandlers();
        for (int i = 0; i < targets.length; i++) {
            Handler h = targets[i];
            logger.removeHandler(h);
            try {
                h.close();
            } catch (Exception ex) {

            }
        }
        if (name != null && name.equals("")) {

            logger.setLevel(defaultLevel);
        } else {
            logger.setLevel(null);
        }
    }


    private String[] parseClassNames(String propertyName) {
        String hands = getProperty(propertyName);
        if (hands == null) {
            return new String[0];
        }
        hands = hands.trim();
        int ix = 0;
        Vector result = new Vector();
        while (ix < hands.length()) {
            int end = ix;
            while (end < hands.length()) {
                if (Character.isWhitespace(hands.charAt(end))) {
                    break;
                }
                if (hands.charAt(end) == ',') {
                    break;
                }
                end++;
            }
            String word = hands.substring(ix, end);
            ix = end + 1;
            word = word.trim();
            if (word.length() == 0) {
                continue;
            }
            result.add(word);
        }
        return (String[]) result.toArray(new String[result.size()]);
    }


    public void readConfiguration(InputStream ins) throws IOException, SecurityException {
        checkAccess();
        reset();


        props.load(ins);

        String names[] = parseClassNames("config");

        for (int i = 0; i < names.length; i++) {
            String word = names[i];
            try {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(word);
                clz.newInstance();
            } catch (Exception ex) {
                System.err.println("Can't load config class \"" + word + "\"");
                System.err.println("" + ex);

            }
        }

        setLevelsOnExistingLoggers();

        synchronized (this) {
            initializedGlobalHandlers = false;
        }
    }


    public String getProperty(String name) {
        return props.getProperty(name);
    }


    String getStringProperty(String name, String defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return val.trim();
    }


    int getIntProperty(String name, int defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }


    boolean getBooleanProperty(String name, boolean defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        val = val.toLowerCase();
        if (val.equals("true") || val.equals("1")) {
            return true;
        } else if (val.equals("false") || val.equals("0")) {
            return false;
        }
        return defaultValue;
    }


    Level getLevelProperty(String name, Level defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Level.parse(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }


    Filter getFilterProperty(String name, Filter defaultValue) {
        String val = getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Filter) clz.newInstance();
            }
        } catch (Exception ex) {


        }

        return defaultValue;
    }


    Formatter getFormatterProperty(String name, Formatter defaultValue) {
        String val = getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Formatter) clz.newInstance();
            }
        } catch (Exception ex) {


        }

        return defaultValue;
    }


    private synchronized void initializeGlobalHandlers() {
        if (initializedGlobalHandlers) {
            return;
        }
        initializedGlobalHandlers = true;
        if (deathImminent) {


            return;
        }


        String names[] = parseClassNames("handlers");
        for (int i = 0; i < names.length; i++) {
            String word = names[i];
            try {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(word);
                Handler h = (Handler) clz.newInstance();
                try {


                    String levs = getProperty(word + ".level");
                    if (levs != null) {
                        h.setLevel(Level.parse(levs));
                    }
                } catch (Exception ex) {
                    System.err.println("Can't set level for " + word);

                }
                rootLogger.addHandler(h);
            } catch (Exception ex) {
                System.err.println("Can't load log handler \"" + word + "\"");
                System.err.println("" + ex);
                ex.printStackTrace();
            }
        }
    }


    public void checkAccess() throws SecurityException {
    }


    private static class LogNode {
        HashMap children;
        Logger logger;
        LogNode parent;

        LogNode(LogNode parent) {
            this.parent = parent;
        }


        void walkAndSetParent(Logger parent) {
            if (children == null) {
                return;
            }
            Iterator values = children.values().iterator();
            while (values.hasNext()) {
                LogNode node = (LogNode) values.next();
                if (node.logger == null) {
                    node.walkAndSetParent(parent);
                } else {
                    doSetParent(node.logger, parent);
                }
            }
        }
    }


    private class RootLogger extends Logger {

        private RootLogger() {
            super("", null);
            setLevel(defaultLevel);
        }

        public void log(LogRecord record) {

            initializeGlobalHandlers();
            super.log(record);
        }

        public void addHandler(Handler h) {
            initializeGlobalHandlers();
            super.addHandler(h);
        }

        public void removeHandler(Handler h) {
            initializeGlobalHandlers();
            super.removeHandler(h);
        }

        public Handler[] getHandlers() {
            initializeGlobalHandlers();
            return super.getHandlers();
        }
    }


    synchronized private void setLevelsOnExistingLoggers() {
        Enumeration enum1 = props.propertyNames();
        while (enum1.hasMoreElements()) {
            String key = (String) enum1.nextElement();
            if (!key.endsWith(".level")) {

                continue;
            }
            int ix = key.length() - 6;
            String name = key.substring(0, ix);
            Level level = getLevelProperty(key, null);
            if (level == null) {
                System.err.println("Bad level value for property: " + key);
                continue;
            }
            Logger l = getLogger(name);
            if (l == null) {
                continue;
            }
            l.setLevel(level);
        }
    }
}
