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

import java.io.Serializable;


public class LogRecord implements Serializable {
    private static long globalSequenceNumber;
    private static int nextThreadId = 10;
    private static ThreadLocal threadIds = new ThreadLocal();


    private Level level;


    private long sequenceNumber;


    private String sourceClassName;


    private String sourceMethodName;


    private String message;


    private int threadID;


    private long millis;


    private Throwable thrown;


    private String loggerName;

    private transient boolean needToInferCaller;
    private transient Object parameters[];


    public LogRecord(Level level, String msg) {

        level.getClass();
        this.level = level;
        message = msg;

        synchronized (LogRecord.class) {
            sequenceNumber = globalSequenceNumber++;
            Integer id = (Integer) threadIds.get();
            if (id == null) {
                id = new Integer(nextThreadId++);
                threadIds.set(id);
            }
            threadID = id.intValue();
        }
        millis = System.currentTimeMillis();
        needToInferCaller = true;
    }


    public String getLoggerName() {
        return loggerName;
    }


    public void setLoggerName(String name) {
        loggerName = name;
    }


    public Level getLevel() {
        return level;
    }


    public void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException();
        }
        this.level = level;
    }


    public long getSequenceNumber() {
        return sequenceNumber;
    }


    public void setSequenceNumber(long seq) {
        sequenceNumber = seq;
    }


    public String getSourceClassName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceClassName;
    }


    public void setSourceClassName(String sourceClassName) {
        this.sourceClassName = sourceClassName;
        needToInferCaller = false;
    }


    public String getSourceMethodName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceMethodName;
    }


    public void setSourceMethodName(String sourceMethodName) {
        this.sourceMethodName = sourceMethodName;
        needToInferCaller = false;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }


    public Object[] getParameters() {
        return parameters;
    }


    public void setParameters(Object parameters[]) {
        this.parameters = parameters;
    }


    public int getThreadID() {
        return threadID;
    }


    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }


    public long getMillis() {
        return millis;
    }


    public void setMillis(long millis) {
        this.millis = millis;
    }


    public Throwable getThrown() {
        return thrown;
    }


    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }


    private void inferCaller() {
        needToInferCaller = false;

        StackTraceElement stack[] = (new Throwable()).getStackTrace();

        int ix = 0;
        while (ix < stack.length) {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (cname.equals("java.util.logging.Logger")) {
                break;
            }
            ix++;
        }

        while (ix < stack.length) {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (!cname.equals("java.util.logging.Logger")) {

                setSourceClassName(cname);
                setSourceMethodName(frame.getMethodName());
                return;
            }
            ix++;
        }


    }
}
