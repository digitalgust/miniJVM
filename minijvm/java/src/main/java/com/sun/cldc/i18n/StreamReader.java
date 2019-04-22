/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.i18n;

import java.io.*;

/**
 * General prototype for character converting stream readers.
 *
 * @author  Nik Shaylor
 * @version 1.0 11/16/99
 * @version 1.1 05/24/01
 */
public abstract class StreamReader extends Reader {

    /** Input stream to read from */
    public InputStream in;

    /**
     * Open the reader
     */
    public Reader open(InputStream in, String enc)
        throws UnsupportedEncodingException {

        this.in = in;
        return this;
    }

    /**
     * Tell whether the underlying byte stream is ready to be read.  Return
     * false for those streams that do not support available(), such as the
     * Win32 console stream.
     */
    public boolean ready() {
        try {
            return in.available() > 0;
        } catch (IOException x) {
            return false;
        }
    }

    /**
     * Tell whether this stream supports the mark() operation.
     */
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Mark the present position in the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        if (in.markSupported()) {
            in.mark(readAheadLimit);
        } else {
            throw new IOException("mark() not supported");
        }
    }

    /**
     * Reset the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void reset() throws IOException {
        in.reset();
    }

    /**
     * Close the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        in.close();
        in = null;
    }

    /**
     * Get the size in chars of an array of bytes
     */
    public abstract int sizeOf(byte[] array, int offset, int length);

}

