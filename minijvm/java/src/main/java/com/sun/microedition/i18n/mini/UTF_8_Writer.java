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

/**
 * Writer for UTF-8 encoded output streams. NOTE: The UTF-8 writer only
 * supports UCS-2, or Unicode, to UTF-8 conversion. There is no support
 * for UTF-16 encoded charaters outside of the Basic Multilingual Plane
 * (BMP). These are encoded in UTF-16 using previously reserved values
 * between U+D800 and U+DFFF. Additionally, the UTF-8 writer does not
 * support any character that requires 4 or more UTF-8 encoded bytes.
 */
public class UTF_8_Writer extends com.sun.microedition.i18n.StreamWriter {

    /**
     * Write a portion of an array of characters.
     *
     * @param cbuf Array of characters
     * @param off  Offset from which to start writing characters
     * @param len  Number of characters to write
     * @throws IOException If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws IOException {
        byte[] outputByte = new byte[6];     // Never more than 3 encoded bytes
        char inputChar;
        int outputSize;
        int count = 0;

        while (count < len) {
            //this code is not support codepoint
//	    inputChar = cbuf[off + count];
//	    if (inputChar < 0x80) {
//		outputByte[0] = (byte)inputChar;
//		outputSize = 1;
//	    } else if (inputChar < 0x800) {
//		outputByte[0] = (byte)(0xc0 | ((inputChar >> 6) & 0x1f));
//		outputByte[1] = (byte)(0x80 | (inputChar & 0x3f));
//		outputSize = 2;
//	    } else {
//		outputByte[0] = (byte)(0xe0 | ((inputChar >> 12)) & 0x0f);
//		outputByte[1] = (byte)(0x80 | ((inputChar >> 6) & 0x3f));
//		outputByte[2] = (byte)(0x80 | (inputChar & 0x3f));
//		outputSize = 3;
//	    }
//	    out.write(outputByte, 0, outputSize);
//	    count++;


            outputSize = 0;
            int unic = cbuf[off + count];
            count++;
            if (Character.isHighSurrogate((char) unic)) {
                int c1 = cbuf[off + count];
                count++;
                int lead = unic & 0x3ff;
                int trail = c1 & 0x3ff;
                unic = (lead << 10) | trail | 0x10000;
            }

            if (unic <= 0x0000007F) {
                // * U-00000000 - U-0000007F:  0xxxxxxx
                outputByte[0] = (byte) (unic & 0x7F);
                outputSize = 1;
            } else if (unic >= 0x00000080 && unic <= 0x000007FF) {
                // * U-00000080 - U-000007FF:  110xxxxx 10xxxxxx
                outputByte[0] = (byte) (((unic >> 6) & 0x1F) | 0xC0);
                outputByte[1] = (byte) ((unic & 0x3F) | 0x80);
                outputSize = 2;
            } else if (unic >= 0x00000800 && unic <= 0x0000FFFF) {
                // * U-00000800 - U-0000FFFF:  1110xxxx 10xxxxxx 10xxxxxx
                outputByte[0] = (byte) (((unic >> 12) & 0x0F) | 0xE0);
                outputByte[1] = (byte) (((unic >> 6) & 0x3F) | 0x80);
                outputByte[2] = (byte) ((unic & 0x3F) | 0x80);
                outputSize = 3;
            } else if (unic >= 0x00010000 && unic <= 0x001FFFFF) {
                // * U-00010000 - U-001FFFFF:  11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                outputByte[0] = (byte) (((unic >> 18) & 0x07) | 0xF0);
                outputByte[1] = (byte) (((unic >> 12) & 0x3F) | 0x80);
                outputByte[2] = (byte) (((unic >> 6) & 0x3F) | 0x80);
                outputByte[3] = (byte) ((unic & 0x3F) | 0x80);
                outputSize = 4;
            } else if (unic >= 0x00200000 && unic <= 0x03FFFFFF) {
                // * U-00200000 - U-03FFFFFF:  111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
                outputByte[0] = (byte) (((unic >> 24) & 0x03) | 0xF8);
                outputByte[1] = (byte) (((unic >> 18) & 0x3F) | 0x80);
                outputByte[2] = (byte) (((unic >> 12) & 0x3F) | 0x80);
                outputByte[3] = (byte) (((unic >> 6) & 0x3F) | 0x80);
                outputByte[4] = (byte) ((unic & 0x3F) | 0x80);
                outputSize = 5;
            } else if (unic >= 0x04000000 && unic <= 0x7FFFFFFF) {
                // * U-04000000 - U-7FFFFFFF:  1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
                outputByte[0] = (byte) (((unic >> 30) & 0x01) | 0xFC);
                outputByte[1] = (byte) (((unic >> 24) & 0x3F) | 0x80);
                outputByte[2] = (byte) (((unic >> 18) & 0x3F) | 0x80);
                outputByte[3] = (byte) (((unic >> 12) & 0x3F) | 0x80);
                outputByte[4] = (byte) (((unic >> 6) & 0x3F) | 0x80);
                outputByte[5] = (byte) ((unic & 0x3F) | 0x80);
                outputSize = 6;
            }

            out.write(outputByte, 0, outputSize);
        }
    }

    /**
     * Get the size in chars of an array of bytes.
     *
     * @param array  Source buffer
     * @param offset Offset at which to start counting characters
     * @param length number of bytes to use for counting
     * @return number of characters that would be converted
     */
    public int sizeOf(char[] array, int offset, int length) {
        int outputSize = 0;
        int inputChar;

        while (offset < length) {
            inputChar = array[offset];
            if (inputChar < 0x80) {
                outputSize++;
            } else if (inputChar < 0x800) {
                outputSize += 2;
            } else {
                outputSize += 3;
            }
            offset++;
        }
        return outputSize;
    }
}



