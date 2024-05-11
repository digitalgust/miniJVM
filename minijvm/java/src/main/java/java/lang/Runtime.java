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


package java.lang;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Every Java application has a single instance of class
 * <code>Runtime</code> that allows the application to interface with
 * the environment in which the application is running. The current
 * runtime can be obtained from the <code>getRuntime</code> method.
 * <p>
 * An application cannot create its own instance of this class.
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see java.lang.Runtime#getRuntime()
 * @since JDK1.0, CLDC 1.0
 */

public class Runtime {
    private static Runtime currentRuntime = new Runtime();

    /**
     * Returns the runtime object associated with the current Java application.
     * Most of the methods of class <code>Runtime</code> are instance
     * methods and must be invoked with respect to the current runtime object.
     *
     * @return the <code>Runtime</code> object associated with the current
     * Java application.
     */
    public static Runtime getRuntime() {
        return currentRuntime;
    }

    /**
     * Don't let anyone else instantiate this class
     */
    private Runtime() {
    }

    /* Helper for exit
     */
    private native void exitInternal(int status);

    /**
     * Terminates the currently running Java application. This
     * method never returns normally.
     * <p>
     * The argument serves as a status code; by convention, a nonzero
     * status code indicates abnormal termination.
     *
     * @param status exit status.
     * @since JDK1.0
     */
    public void exit(int status) {
        exitInternal(status);
    }

    /**
     * Returns the amount of free memory in the system. Calling the
     * <code>gc</code> method may result in increasing the value returned
     * by <code>freeMemory.</code>
     *
     * @return an approximation to the total amount of memory currently
     * available for future allocated objects, measured in bytes.
     */
    public native long freeMemory();

    /**
     * Returns the total amount of memory in the Java Virtual Machine.
     * The value returned by this method may vary over time, depending on
     * the host environment.
     * <p>
     * Note that the amount of memory required to hold an object of any
     * given type may be implementation-dependent.
     *
     * @return the total amount of memory currently available for current
     * and future objects, measured in bytes.
     */
    public native long totalMemory();

    /**
     * Runs the garbage collector.
     * Calling this method suggests that the Java Virtual Machine expend
     * effort toward recycling unused objects in order to make the memory
     * they currently occupy available for quick reuse. When control
     * returns from the method call, the Java Virtual Machine has made
     * its best effort to recycle all discarded objects.
     * <p>
     * The name <code>gc</code> stands for "garbage
     * collector". The Java Virtual Machine performs this recycling
     * process automatically as needed even if the
     * <code>gc</code> method is not invoked explicitly.
     * <p>
     * The method {@link System#gc()} is the conventional and convenient
     * means of invoking this method.
     */
    public native void gc();


    public void load(String absPathAndFilename) {
        System.load(absPathAndFilename);
    }

    public void loadLibrary(String dylibname) {
        System.loadLibrary(dylibname);
    }

    public Process exec(String command) throws IOException {
        StringTokenizer t = new StringTokenizer(command);
        String[] cmd = new String[t.countTokens()];
        for (int i = 0; i < cmd.length; i++)
            cmd[i] = t.nextToken();

        return exec(cmd);
    }

    public Process exec(final String[] command) throws IOException {
        final MyProcess[] process = new MyProcess[1];
        final Throwable[] exception = new Throwable[1];

        synchronized (process) {
            Thread t = new Thread() {
                public void run() {
                    synchronized (process) {
                        try {
                            long[] info = new long[5];
                            exec(command, info);
                            process[0] = new MyProcess
                                    (info[0], info[1], (int) info[2], (int) info[3],
                                            (int) info[4]);
                        } catch (Throwable e) {
                            System.out.println(e);
                            exception[0] = e;
                        } finally {
                            process.notifyAll();
                        }
                    }

                    MyProcess p = process[0];
                    if (p != null) {
                        synchronized (p) {
                            try {
                                if (p.pid != 0) {
                                    p.exitCode = Runtime.waitFor(p.pid, p.tid);
                                    p.pid = 0;
                                    p.tid = 0;
                                }
                            } finally {
                                p.notifyAll();
                            }
                        }
                    }
                }
            };
            t.setDaemon(true);
            t.start();

            while (process[0] == null && exception[0] == null) {
                try {
                    process.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (exception[0] != null) {
            if (exception[0] instanceof IOException) {
                String message = "Failed to run \"" + command[0] + "\": " + exception[0].getMessage();
                throw new IOException(message);
            } else {
                throw new RuntimeException(exception[0]);
            }
        }

        return process[0];
    }

    public native void addShutdownHook(Thread t);

    private static native void exec(String[] command, long[] process)
            throws IOException;

    private static native int waitFor(long pid, long tid);

    private static native void kill(long pid);


    public native long maxMemory();

    private static class MyProcess extends Process {
        private long pid;
        private long tid;
        private final int in;
        private final int out;
        private final int err;
        private int exitCode;

        public MyProcess(long pid, long tid, int in, int out, int err) {
            this.pid = pid;
            this.tid = tid;
            this.in = in;
            this.out = out;
            this.err = err;
        }

        public void destroy() {
            if (pid != 0) {
                kill(pid);
            }
        }

        public InputStream getInputStream() {
            return new FileInputStream(new FileDescriptor(in));
        }

        public OutputStream getOutputStream() {
            return new FileOutputStream(new FileDescriptor(out));
        }

        public InputStream getErrorStream() {
            return new FileInputStream(new FileDescriptor(err));
        }

        public synchronized int exitValue() {
            if (pid != 0) {
                throw new IllegalThreadStateException();
            }

            return exitCode;
        }

        public synchronized int waitFor() throws InterruptedException {
            while (pid != 0) {
                wait();
            }

            return exitCode;
        }
    }

}

