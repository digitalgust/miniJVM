/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n.j2me;

import com.sun.cldc.i18n.StreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/** Compatibility placeholder for legacy native-backed encoders. */
public class Gen_Writer extends StreamWriter {
    Gen_Writer(String encoding) throws ClassNotFoundException {
        throw new ClassNotFoundException("Encoder not implemented: " + encoding);
    }

    public Writer open(OutputStream output, String encoding)
            throws UnsupportedEncodingException {
        throw new UnsupportedEncodingException("Encoder not implemented: " + encoding);
    }

    public synchronized void write(int value) throws IOException {
        throw new UnsupportedEncodingException("Encoder not implemented");
    }

    public synchronized void write(char[] data, int offset, int length) throws IOException {
        throw new UnsupportedEncodingException("Encoder not implemented");
    }

    public synchronized void write(String value, int offset, int length) throws IOException {
        throw new UnsupportedEncodingException("Encoder not implemented");
    }

    public int sizeOf(char[] data, int offset, int length) {
        throw new UnsupportedOperationException("Encoder not implemented");
    }
}
