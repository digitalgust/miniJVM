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

import org.mini.vm.RefNative;

import java.util.*;
import java.lang.ref.WeakReference;


public class Logger {
    private static final Handler emptyHandlers[] = new Handler[0];
    private static final int offValue = Level.OFF.intValue();
    private LogManager manager;
    private String name;
    private ArrayList handlers;
    private boolean useParentHandlers = true;
    private Filter filter;

    private static Object treeLock = new Object();


    private Logger parent;
    private ArrayList kids;
    private Level levelObject;
    private volatile int levelValue;


    public static final Logger global = new Logger("global");


    protected Logger(String name, String resourceBundleName) {
        this.manager = LogManager.getLogManager();
        this.name = name;
        levelValue = Level.INFO.intValue();
    }


    private Logger(String name) {

        this.name = name;
        levelValue = Level.INFO.intValue();
    }


    void setLogManager(LogManager manager) {
        this.manager = manager;
    }


    public static synchronized Logger getLogger(String name) {
        LogManager manager = LogManager.getLogManager();
        Logger result = manager.getLogger(name);
        if (result == null) {
            result = new Logger(name, null);
            manager.addLogger(result);
            result = manager.getLogger(name);
        }
        return result;
    }


    public static synchronized Logger getAnonymousLogger() {
        LogManager manager = LogManager.getLogManager();
        Logger result = new Logger(null, null);
        Logger root = manager.getLogger("");
        result.doSetParent(root);
        return result;
    }


    public static synchronized Logger getAnonymousLogger(String resourceBundleName) {
        LogManager manager = LogManager.getLogManager();
        Logger result = new Logger(null, resourceBundleName);
        Logger root = manager.getLogger("");
        result.doSetParent(root);
        return result;
    }


    public void setFilter(Filter newFilter) throws SecurityException {
        filter = newFilter;
    }


    public Filter getFilter() {
        return filter;
    }


    public void log(LogRecord record) {
        if (record.getLevel().intValue() < levelValue || levelValue == offValue) {
            return;
        }
        synchronized (this) {
            if (filter != null && !filter.isLoggable(record)) {
                return;
            }
        }


        Logger logger = this;
        while (logger != null) {
            Handler targets[] = logger.getHandlers();

            if (targets != null) {
                for (int i = 0; i < targets.length; i++) {
                    targets[i].publish(record);
                }
            }

            if (!logger.getUseParentHandlers()) {
                break;
            }

            logger = logger.getParent();
        }
    }


    private void doLog(LogRecord lr) {
        lr.setLoggerName(name);
        log(lr);
    }


    public void log(Level level, String msg) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        doLog(lr);
    }


    public void log(Level level, String msg, Object param1) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        Object params[] = {param1};
        lr.setParameters(params);
        doLog(lr);
    }


    public void log(Level level, String msg, Object params[]) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setParameters(params);
        doLog(lr);
    }


    public void log(Level level, String msg, Throwable thrown) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setThrown(thrown);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Object param1) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object params[] = {param1};
        lr.setParameters(params);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Object params[]) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Throwable thrown) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }


    private void doLog(LogRecord lr, String rbname) {
        lr.setLoggerName(name);
        log(lr);
    }


    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr, bundleName);
    }


    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Object param1) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object params[] = {param1};
        lr.setParameters(params);
        doLog(lr, bundleName);
    }


    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Object params[]) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        doLog(lr, bundleName);
    }


    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Throwable thrown) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr, bundleName);
    }


    public void entering(String sourceClass, String sourceMethod) {
        if (Level.FINER.intValue() < levelValue) {
            return;
        }
        logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
    }


    public void entering(String sourceClass, String sourceMethod, Object param1) {
        if (Level.FINER.intValue() < levelValue) {
            return;
        }
        Object params[] = {param1};
        logp(Level.FINER, sourceClass, sourceMethod, "ENTRY {0}", params);
    }


    public void entering(String sourceClass, String sourceMethod, Object params[]) {
        if (Level.FINER.intValue() < levelValue) {
            return;
        }
        String msg = "ENTRY";
        for (int i = 0; i < params.length; i++) {
            msg = msg + " {" + i + "}";
        }
        logp(Level.FINER, sourceClass, sourceMethod, msg, params);
    }


    public void exiting(String sourceClass, String sourceMethod) {
        if (Level.FINER.intValue() < levelValue) {
            return;
        }
        logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
    }


    public void exiting(String sourceClass, String sourceMethod, Object result) {
        if (Level.FINER.intValue() < levelValue) {
            return;
        }
        Object params[] = {result};
        logp(Level.FINER, sourceClass, sourceMethod, "RETURN {0}", result);
    }


    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (Level.FINER.intValue() < levelValue) {
            return;
        }
        LogRecord lr = new LogRecord(Level.FINER, "THROW");
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }


    public void severe(String msg) {
        if (Level.SEVERE.intValue() < levelValue) {
            return;
        }
        log(Level.SEVERE, msg);
    }


    public void warning(String msg) {
        if (Level.WARNING.intValue() < levelValue) {
            return;
        }
        log(Level.WARNING, msg);
    }


    public void info(String msg) {
        if (Level.INFO.intValue() < levelValue) {
            return;
        }
        log(Level.INFO, msg);
    }


    public void config(String msg) {
        if (Level.CONFIG.intValue() < levelValue) {
            return;
        }
        log(Level.CONFIG, msg);
    }


    public void fine(String msg) {
        if (Level.FINE.intValue() < levelValue) {
            return;
        }
        log(Level.FINE, msg);
    }


    public void finer(String msg) {
        if (Level.FINER.intValue() < levelValue) {
            return;
        }
        log(Level.FINER, msg);
    }


    public void finest(String msg) {
        if (Level.FINEST.intValue() < levelValue) {
            return;
        }
        log(Level.FINEST, msg);
    }


    public void setLevel(Level newLevel) throws SecurityException {
        synchronized (treeLock) {
            levelObject = newLevel;
            updateEffectiveLevel();
        }
    }


    public Level getLevel() {
        return levelObject;
    }


    public boolean isLoggable(Level level) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return false;
        }
        return true;
    }


    public String getName() {
        return name;
    }


    public synchronized void addHandler(Handler handler) throws SecurityException {

        handler.getClass();
        if (handlers == null) {
            handlers = new ArrayList();
        }
        handlers.add(handler);
    }


    public synchronized void removeHandler(Handler handler) throws SecurityException {
        if (handler == null) {
            throw new NullPointerException();
        }
        if (handlers == null) {
            return;
        }
        handlers.remove(handler);
    }


    public synchronized Handler[] getHandlers() {
        if (handlers == null) {
            return emptyHandlers;
        }
        Handler result[] = new Handler[handlers.size()];
        result = (Handler[]) handlers.toArray(result);
        return result;
    }


    public synchronized void setUseParentHandlers(boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
    }


    public synchronized boolean getUseParentHandlers() {
        return useParentHandlers;
    }


    public Logger getParent() {
        synchronized (treeLock) {
            return parent;
        }
    }


    public void setParent(Logger parent) {
        if (parent == null) {
            throw new NullPointerException();
        }
        doSetParent(parent);
    }


    private void doSetParent(Logger newParent) {


        synchronized (treeLock) {


            if (parent != null) {

                for (Iterator iter = parent.kids.iterator(); iter.hasNext(); ) {
                    WeakReference ref = (WeakReference) iter.next();
                    Logger kid = (Logger) ref.get();
                    if (kid == this) {
                        iter.remove();
                        break;
                    }
                }

            }


            parent = newParent;
            if (parent.kids == null) {
                parent.kids = new ArrayList(2);
            }
            parent.kids.add(new WeakReference(this));


            updateEffectiveLevel();

        }
    }


    private void updateEffectiveLevel() {


        int newLevelValue;
        if (levelObject != null) {
            newLevelValue = levelObject.intValue();
        } else {
            if (parent != null) {
                newLevelValue = parent.levelValue;
            } else {

                newLevelValue = Level.INFO.intValue();
            }
        }


        if (levelValue == newLevelValue) {
            return;
        }

        levelValue = newLevelValue;


        if (kids != null) {
            for (int i = 0; i < kids.size(); i++) {
                WeakReference ref = (WeakReference) kids.get(i);
                Logger kid = (Logger) ref.get();
                if (kid != null) {
                    kid.updateEffectiveLevel();
                }
            }
        }
    }

}
