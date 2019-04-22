/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.cldc.io;

import java.io.*;
import java.util.*;

public class ResourceInputStream extends InputStream {

    // This field must be an Object, since it will point to
    // something that needs to be protected from the garbage
    // collector.
    private ByteArrayWrap handle;
    private int pos;
    private int size;

    /**
     * Fixes the resource name to be conformant with the CLDC 1.0 specification.
     * We are not allowed to use "../" to get outside of the .jar file.
     *
     * @param name the name of the resource in classpath to access.
     * @return the fixed string.
     * @exception IOException if the resource name points to a classfile, as
     * determined by the resource name's extension.
     */
    private static String fixResourceName(String name) throws IOException {
        Vector dirVector = new Vector();
        int startIdx = 0;
        int endIdx = 0;
        String curDir;

        while ((endIdx = name.indexOf('/', startIdx)) != -1) {
            if (endIdx == startIdx) {
                // We have a leading '/' or two consecutive '/'s
                startIdx++;
                continue;
            }

            curDir = name.substring(startIdx, endIdx);
            startIdx = endIdx + 1;

            if (curDir.equals(".")) {
                // Ignore a single '.' directory
                continue;
            }
            if (curDir.equals("..")) {
                // Go up a level
                try {
                    dirVector.removeElementAt(dirVector.size() - 1);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    // "/../resource" Not allowed!
                    throw new IOException();
                }
                continue;
            }
            dirVector.addElement(curDir);
        }

        // save directory structure
        StringBuffer dirName = new StringBuffer();

        int nelements = dirVector.size();
        for (int i = 0; i < nelements; ++i) {
            dirName.append((String) dirVector.elementAt(i));
            dirName.append("/");
        }

        // save filename
        if (startIdx < name.length()) {
            String filename = name.substring(startIdx);
            // Throw IOE if the resource ends with ".class", but, not
            //  if the entire name is ".class"
            if ((filename.endsWith(".class"))
                    && (!".class".equals(filename))) {
                throw new IOException();
            }
            dirName.append(name.substring(startIdx));
        }
        return dirName.toString();
    }

    /**
     * Construct a resource input stream for accessing objects in the jar file.
     *
     * @param name the name of the resource in classpath to access. The name
     * must not have a leading '/'.
     * @exception IOException if an I/O error occurs.
     */
    public ResourceInputStream(String name) throws IOException {
        String fixedName = fixResourceName(name);
        byte[] b = open(fixedName);
        if (b == null) {
            throw new IOException();
        }
        handle = new ByteArrayWrap(b);
        size = b.length;
        pos = 0;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @exception IOException if an I/O error occurs.
     */
    public int read() throws IOException {
        int result;
        if ((result = read(handle)) != -1) {
            pos++;
        }
        return result;
    }

    public synchronized void close() throws IOException {
        close(handle);
        handle = null;
    }

    public int available() throws IOException {
        return size - pos;
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0)
                || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (pos >= size) {
            return -1;
        }
        if (pos + len > size) {
            len = size - pos;
        }
        if (len <= 0) {
            return 0;
        }

        int readLength;
        if ((readLength = readBytes(handle, b, off, pos, len)) != -1) {
            pos += readLength;
        }
        return readLength;
    }

//    private static native Object open(String name) throws IOException;
//    private static native void close(Object handle) throws IOException;
//    private static native int size(Object handle) throws IOException;
//    private static native int read(Object handle) throws IOException;
//    private static native int readBytes(Object handle, byte[] b, int offset,
//                                        int pos, int len) throws IOException;
    private static native byte[] open(String name) throws IOException;

    private static void close(ByteArrayWrap handle) throws IOException {
    }

    private static int read(ByteArrayWrap handle) throws IOException {
        return handle.next();
    }

    private static int readBytes(ByteArrayWrap handle, byte[] b, int offset,
            int pos, int len) throws IOException {
        handle.setPos(pos);
        return handle.nextBlock(b, offset, len);
    }

    class ByteArrayWrap {

        private byte[] b;
        private int r_pos;

        ByteArrayWrap(byte[] src) {
            b = src;
            r_pos = 0;
        }

        int next() {
            if (b == null || r_pos >= b.length) {
                return -1;
            }
            return b[r_pos++];
        }

        void setPos(int readPos) {
            r_pos = readPos;
        }

        int nextBlock(byte[] dst, int offset, int len) {
            if (b == null || dst == null) {
                return -1;
            }
            if (len + r_pos > b.length) {
                return -1;
            }
            if (len < 0 || offset < 0 || offset > dst.length) {
                return -1;
            }
            System.arraycopy(b, r_pos, dst, offset, len);
            r_pos += len;
            return len;
        }
    }
}
