/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/** Base class for the small set of character decoders bundled with miniJVM. */
public abstract class StreamReader extends Reader {
    protected InputStream in;

    public Reader open(InputStream input, String encoding)
            throws UnsupportedEncodingException {
        if (input == null) {
            throw new NullPointerException();
        }
        in = input;
        return this;
    }

    public boolean ready() {
        try {
            return in != null && in.available() > 0;
        } catch (IOException ignored) {
            return false;
        }
    }

    public boolean markSupported() {
        return in != null && in.markSupported();
    }

    public void mark(int limit) throws IOException {
        if (!markSupported()) {
            throw new IOException("mark() not supported");
        }
        in.mark(limit);
    }

    public void reset() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
        in.reset();
    }

    public void close() throws IOException {
        InputStream input = in;
        in = null;
        if (input != null) {
            input.close();
        }
    }

    public abstract int sizeOf(byte[] array, int offset, int length);
}
