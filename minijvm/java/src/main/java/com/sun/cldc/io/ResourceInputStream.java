/* SPDX-License-Identifier: MIT */
package com.sun.cldc.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/** Input stream over a resource loaded by the miniJVM native runtime. */
public final class ResourceInputStream extends InputStream {
    private byte[] data;
    private int position;

    public ResourceInputStream(String name) throws IOException {
        data = open(normalize(name));
        if (data == null) {
            throw new IOException("Resource not found: " + name);
        }
    }

    public int read() throws IOException {
        ensureOpen();
        return position < data.length ? data[position++] & 0xff : -1;
    }

    public int read(byte[] output, int offset, int length) throws IOException {
        ensureOpen();
        if (output == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > output.length - length) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return 0;
        }
        if (position >= data.length) {
            return -1;
        }
        int count = Math.min(length, data.length - position);
        System.arraycopy(data, position, output, offset, count);
        position += count;
        return count;
    }

    public int available() throws IOException {
        ensureOpen();
        return data.length - position;
    }

    public void close() {
        data = null;
        position = 0;
    }

    private void ensureOpen() throws IOException {
        if (data == null) {
            throw new IOException("Stream closed");
        }
    }

    private static String normalize(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        }
        Vector parts = new Vector();
        int start = 0;
        while (start <= name.length()) {
            int slash = name.indexOf('/', start);
            int end = slash < 0 ? name.length() : slash;
            String part = name.substring(start, end);
            if (part.length() == 0 || ".".equals(part)) {
                // Ignore duplicate separators and current-directory markers.
            } else if ("..".equals(part)) {
                if (parts.size() == 0) {
                    throw new IOException("Resource escapes root");
                }
                parts.removeElementAt(parts.size() - 1);
            } else {
                parts.addElement(part);
            }
            if (slash < 0) {
                break;
            }
            start = slash + 1;
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                result.append('/');
            }
            result.append((String) parts.elementAt(i));
        }
        if (result.toString().endsWith(".class")) {
            throw new IOException("Class resources are not exposed");
        }
        return result.toString();
    }

    private static native byte[] open(String name) throws IOException;
}
