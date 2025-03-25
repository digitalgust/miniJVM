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


public class FileHandler extends StreamHandler {
    private MeteredStream meter;
    private boolean append;
    private int limit;
    private int count;
    private String pattern;
    private String lockFileName;
    private FileOutputStream lockStream;
    private File files[];
    private static final int MAX_LOCKS = 100;
    private static java.util.HashMap locks = new java.util.HashMap();


    private class MeteredStream extends OutputStream {
        OutputStream out;
        int written;

        MeteredStream(OutputStream out, int written) {
            this.out = out;
            this.written = written;
        }

        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        public void write(byte buff[]) throws IOException {
            out.write(buff);
            written += buff.length;
        }

        public void write(byte buff[], int off, int len) throws IOException {
            out.write(buff, off, len);
            written += len;
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            out.close();
        }
    }

    private void open(File fname, boolean append) throws IOException {
        int len = 0;
        if (append) {
            len = (int) fname.length();
        }
        FileOutputStream fout = new FileOutputStream(fname.toString(), append);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        meter = new MeteredStream(bout, len);
        setOutputStream(meter);
    }


    private void configure() {
        LogManager manager = LogManager.getLogManager();

        String cname = FileHandler.class.getName();

        pattern = manager.getStringProperty(cname + ".pattern", "%h/java%u.log");
        limit = manager.getIntProperty(cname + ".limit", 0);
        if (limit < 0) {
            limit = 0;
        }
        count = manager.getIntProperty(cname + ".count", 1);
        if (count <= 0) {
            count = 1;
        }
        append = manager.getBooleanProperty(cname + ".append", false);
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
    }


    public FileHandler() throws IOException, SecurityException {
        checkAccess();
        configure();
        openFiles();
    }


    public FileHandler(String pattern) throws IOException, SecurityException {
        checkAccess();
        configure();
        this.pattern = pattern;
        this.limit = 0;
        this.count = 1;
        openFiles();
    }


    public FileHandler(String pattern, boolean append) throws IOException, SecurityException {
        checkAccess();
        configure();
        this.pattern = pattern;
        this.limit = 0;
        this.count = 1;
        this.append = append;
        openFiles();
    }


    public FileHandler(String pattern, int limit, int count)
            throws IOException, SecurityException {
        if (limit < 0 || count < 1) {
            throw new IllegalArgumentException();
        }
        checkAccess();
        configure();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        openFiles();
    }


    public FileHandler(String pattern, int limit, int count, boolean append)
            throws IOException, SecurityException {
        if (limit < 0 || count < 1) {
            throw new IllegalArgumentException();
        }
        checkAccess();
        configure();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        this.append = append;
        openFiles();
    }


    private void openFiles() throws IOException {
        LogManager manager = LogManager.getLogManager();
        manager.checkAccess();
        if (count < 1) {
            throw new IllegalArgumentException("file count = " + count);
        }
        if (limit < 0) {
            limit = 0;
        }


        InitializationErrorManager em = new InitializationErrorManager();
        setErrorManager(em);


        int unique = -1;
        for (; ; ) {
            unique++;
            if (unique > MAX_LOCKS) {
                throw new IOException("Couldn't get lock for " + pattern);
            }

            lockFileName = generate(pattern, 0, unique).toString() + ".lck";


            synchronized (locks) {
                if (locks.get(lockFileName) != null) {
                    continue;
                }
                try {
                    lockStream = new FileOutputStream(lockFileName);
                } catch (IOException ix) {
                    continue;
                }

                locks.put(lockFileName, lockFileName);
                break;
            }
        }

        files = new File[count];
        for (int i = 0; i < count; i++) {
            files[i] = generate(pattern, i, unique);
        }


        if (append) {
            open(files[0], true);
        } else {
            rotate();
        }


        Exception ex = em.lastException;
        if (ex != null) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof SecurityException) {
                throw (SecurityException) ex;
            } else {
                throw new IOException("Exception: " + ex);
            }
        }


        setErrorManager(new ErrorManager());
    }


    private File generate(String pattern, int generation, int unique) throws IOException {
        File file = null;
        String word = "";
        int ix = 0;
        boolean sawg = false;
        boolean sawu = false;
        while (ix < pattern.length()) {
            char ch = pattern.charAt(ix);
            ix++;
            char ch2 = 0;
            if (ix < pattern.length()) {
                ch2 = Character.toLowerCase(pattern.charAt(ix));
            }
            if (ch == '/') {
                if (file == null) {
                    file = new File(word);
                } else {
                    file = new File(file, word);
                }
                word = "";
                continue;
            } else if (ch == '%') {
                if (ch2 == 't') {
                    String tmpDir = System.getProperty("java.io.tmpdir");
                    if (tmpDir == null) {
                        tmpDir = System.getProperty("user.home");
                    }
                    file = new File(tmpDir);
                    ix++;
                    word = "";
                    continue;
                } else if (ch2 == 'h') {
                    file = new File(System.getProperty("user.home"));
                    ix++;
                    word = "";
                    continue;
                } else if (ch2 == 'g') {
                    word = word + generation;
                    sawg = true;
                    ix++;
                    continue;
                } else if (ch2 == 'u') {
                    word = word + unique;
                    sawu = true;
                    ix++;
                    continue;
                } else if (ch2 == '%') {
                    word = word + "%";
                    ix++;
                    continue;
                }
            }
            word = word + ch;
        }
        if (count > 1 && !sawg) {
            word = word + "." + generation;
        }
        if (unique > 0 && !sawu) {
            word = word + "." + unique;
        }
        if (word.length() > 0) {
            if (file == null) {
                file = new File(word);
            } else {
                file = new File(file, word);
            }
        }
        return file;
    }


    private synchronized void rotate() {
        Level oldLevel = getLevel();
        setLevel(Level.OFF);

        super.close();
        for (int i = count - 2; i >= 0; i--) {
            File f1 = files[i];
            File f2 = files[i + 1];
            if (f1.exists()) {
                if (f2.exists()) {
                    f2.delete();
                }
                f1.renameTo(f2);
            }
        }
        try {
            open(files[0], false);
        } catch (IOException ix) {


            reportError(null, ix, ErrorManager.OPEN_FAILURE);

        }
        setLevel(oldLevel);
    }


    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
        if (limit > 0 && meter.written >= limit) {

            rotate();
        }
    }


    public synchronized void close() throws SecurityException {
        super.close();

        if (lockFileName == null) {
            return;
        }
        try {


            lockStream.close();
        } catch (Exception ex) {

        }
        synchronized (locks) {
            locks.remove(lockFileName);
        }
        lockFileName = null;
        lockStream = null;
    }

    private static class InitializationErrorManager extends ErrorManager {
        Exception lastException;

        public void error(String msg, Exception ex, int code) {
            lastException = ex;
        }
    }


}

