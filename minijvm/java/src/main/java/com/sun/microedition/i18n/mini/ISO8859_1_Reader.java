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



package com.sun.microedition.i18n.mini;

import java.io.*;
import com.sun.microedition.i18n.*;

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

