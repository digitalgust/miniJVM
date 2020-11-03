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



package com.sun.cldc.i18n.mini;

import java.io.*;
import com.sun.cldc.i18n.*;

/**
 * Default J2ME class for output stream writers.
 *
 * @author  Nik Shaylor, Antero Taivalsaari
 * @version 1.0 10/18/99
 * @version 1.1 03/29/02
 */
public class ISO8859_1_Writer extends StreamWriter {

    /**
     * Write a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    synchronized public void write(int c) throws IOException {
        if (c > 255) {
            c = '?'; // was --> throw new RuntimeException("Unknown character "+c);
        }
        out.write(c);
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param  cbuf  Buffer of characters to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    synchronized public void write(char cbuf[], int off, int len) throws IOException {
        while (len-- > 0) {
            write(cbuf[off++]);
        }
    }

    /**
     * Write a portion of a string.
     *
     * @param  str  String to be written
     * @param  off  Offset from which to start reading characters
     * @param  len  Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    synchronized public void write(String str, int off, int len) throws IOException {
        for (int i = 0 ; i < len ; i++) {
            write(str.charAt(off + i));
        }
    }

    /**
     * Get the size in bytes of an array of chars
     */
    public int sizeOf(char[] array, int offset, int length) {
        return length;
    }

}

