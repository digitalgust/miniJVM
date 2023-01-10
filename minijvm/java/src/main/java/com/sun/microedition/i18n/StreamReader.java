/*
 *
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 *
 */



package com.sun.microedition.i18n;

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

