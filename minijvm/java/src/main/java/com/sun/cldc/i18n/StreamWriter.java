/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/** Base class for the small set of character encoders bundled with miniJVM. */
public abstract class StreamWriter extends Writer {
    protected OutputStream out;

    public Writer open(OutputStream output, String encoding)
            throws UnsupportedEncodingException {
        if (output == null) {
            throw new NullPointerException();
        }
        out = output;
        return this;
    }

    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    public void close() throws IOException {
        OutputStream output = out;
        out = null;
        if (output != null) {
            output.close();
        }
    }

    public abstract int sizeOf(char[] array, int offset, int length);
}
