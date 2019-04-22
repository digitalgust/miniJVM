/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;

import java.io.*;
import com.sun.cldc.i18n.*;

/**
 * Default J2ME class for input stream readers
 *
 * @author  Nik Shaylor, Antero Taivalsaari
 * @version 1.0 10/18/99
 * @version 1.1 03/29/02
 */

public class ISO8859_1_Reader extends StreamReader {

    private static int BBUF_LEN = 128;

    /**
     * Read a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    synchronized public int read() throws IOException {
        return in.read();
    }

    /**
     * Read characters into a portion of an array.
     *
     * @exception  IOException  If an I/O error occurs
     */
    synchronized public int read(char cbuf[], int off, int len) 
        throws IOException {

        // Allocate a private buffer to speed up reading
        int bbuflen = (len > BBUF_LEN) ? BBUF_LEN : len;
        byte bbuf[] = new byte[bbuflen];

        int count = 0;
        while (count < len) {
            int nbytes = len - count;
            if (nbytes > bbuflen) nbytes = bbuflen;
            nbytes = in.read(bbuf, 0, nbytes);

            if (nbytes == -1) {
                return (count == 0) ? -1 : count;
            }

            for (int i = 0; i < nbytes; i++) {
                cbuf[off++] = (char)(bbuf[i] & 0xFF);
            }

            count += nbytes;
        }
        return len;
    }

    /**
     * Get the size in chars of an array of bytes
     */
    public int sizeOf(byte[] array, int offset, int length) {
        return length;
    }

}

