/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.i18n;

import java.io.*;

/**
 * General prototype for character converting stream writers.
 *
 * @author  Nik Shaylor
 * @version 1.0 11/16/99
 */
public abstract class StreamWriter extends Writer {

    /** Output stream to write to */
    public OutputStream out;

    /**
     * Open the writer
     */
    public Writer open(OutputStream out, String enc)
        throws UnsupportedEncodingException {

        this.out = out;
        return this;
    }

    /**
     * Flush the writer and the output stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void flush() throws IOException {
        if (out != null) {
            out.flush();   
        }
    }

    /**
     * Close the writer and the output stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        if (out != null) {
            out.close();      
        }
    }

    /**
     * Get the size in bytes of an array of chars
     */
    public abstract int sizeOf(char[] array, int offset, int length);

}

