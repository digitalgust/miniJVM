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
 * Generic interface for stream conversion writing of 
 * specific character encoded input streams.
 *.
 */
public class Gen_Writer extends StreamWriter {
   /** Saved encoding string from construction. */
    private String enc;
    /** Native handle for conversion routines. */
    private int id;
    /** Local buffer to write converted characters. */
    private byte[] buf;
    /** Maximum length of characters in local buffer. */
    private int maxByteLen;

    /**
     * Constructor for generic writer.
     * @param enc character encoding to use for byte to character
     * conversion.
     * @exception ClassNotFoundException is thrown if the conversion
     * class is not available
     */
    Gen_Writer(String enc) throws ClassNotFoundException {
        id = Conv.getHandler(enc);
        if (id == -1) {
            throw new ClassNotFoundException();
        }
        this.enc = enc;
        maxByteLen = Conv.getMaxByteLength(id);
        buf = new byte[maxByteLen];
    }

    /**
     * Generic routine to open an OutputStream with a specific
     * character encoding.
     * 
     * @param out the output stream to process
     * @param enc the character encoding for the output stream
     * @return Writer instance for converted characters
     * @throws UnsupportedEncodingException if encoding is not supported
     */
    public Writer open(OutputStream out, String enc) 
        throws UnsupportedEncodingException {
        if (!enc.equals(this.enc)) {
            throw new UnsupportedEncodingException();
        }
        return super.open(out, enc);
    }

    /**
     * Write a single converted character.
     *
     * @param c the character to be output
     * @exception IOException is thrown if the output stream 
     * could not be written with the converted bytes
     */
    synchronized public void write(int c) throws IOException {
        char cbuf[] = {(char)c};

        int len = Conv.charToByte(id, cbuf, 0, 1, buf, 0, buf.length);

        if (len > 0) {
            out.write(buf, 0, len);
        }
    }

    /**
     * Write a block of converted characters.
     *
     * @param cbuf output buffer of characters to convert
     * @param off initial offset into the provided buffer
     * @param len length of characters in the buffer
     * @exception IOException is thrown if the output stream 
     * could not be written with the converted bytes
     */
    synchronized public void write(char cbuf[], int off, int len) 
        throws IOException {
        int maxlen = len * maxByteLen;
        if (buf.length < maxlen) {
            buf = new byte[maxlen];
        }

        len = Conv.charToByte(id, cbuf, off, len, buf, 0, buf.length);

        if (len > 0) {
            out.write(buf, 0, len);
        }

        if (buf.length > maxByteLen) {
            buf = new byte[maxByteLen];
        }
    }

    /**
     * Write a block of converted characters from a string.
     *
     * @param str string to convert
     * @param off initial offset into the string
     * @param len length of characters in the string to process
     * @exception IOException is thrown if the output stream 
     * could not be written with the converted bytes
     */
    synchronized public void write(String str, int off, int len) 
        throws IOException {
        for (int i = 0; i < len; i++) {
            write(str.charAt(off + i));
        }
    }

    /** 
     * Get the size of the converted bytes as a Unicode 
     * byte array.
     *
     * @param cbuf array of bytes to compute size
     * @param off offset in the provided buffer
     * @param len length of bytes to process
     * @return length of converted characters.
     */
    public int sizeOf(char[] cbuf, int off, int len) {
        return Conv.sizeOfUnicodeInByte(id, cbuf, off, len);
    }
}
