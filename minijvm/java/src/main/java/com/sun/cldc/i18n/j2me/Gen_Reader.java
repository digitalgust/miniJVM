/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n.j2me;

import com.sun.cldc.i18n.StreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/** Compatibility placeholder for legacy native-backed decoders. */
public class Gen_Reader extends StreamReader {
    Gen_Reader(String encoding) throws ClassNotFoundException {
        throw new ClassNotFoundException("Decoder not implemented: " + encoding);
    }

    public Reader open(InputStream input, String encoding)
            throws UnsupportedEncodingException {
        throw new UnsupportedEncodingException("Decoder not implemented: " + encoding);
    }

    public synchronized int read() throws IOException {
        throw new UnsupportedEncodingException("Decoder not implemented");
    }

    public synchronized int read(char[] data, int offset, int length) throws IOException {
        throw new UnsupportedEncodingException("Decoder not implemented");
    }

    public long skip(long count) throws IOException {
        throw new UnsupportedEncodingException("Decoder not implemented");
    }

    public int sizeOf(byte[] data, int offset, int length) {
        throw new UnsupportedOperationException("Decoder not implemented");
    }

    public void reset() throws IOException {
        throw new UnsupportedEncodingException("Decoder not implemented");
    }
}
