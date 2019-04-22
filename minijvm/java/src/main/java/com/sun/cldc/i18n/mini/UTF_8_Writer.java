/*
 * @(#)UTF_8_Writer.java	1.3 02/09/24 @(#)
 *
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;

import java.io.*;

/**
 * Writer for UTF-8 encoded output streams. NOTE: The UTF-8 writer only
 * supports UCS-2, or Unicode, to UTF-8 conversion. There is no support
 * for UTF-16 encoded charaters outside of the Basic Multilingual Plane
 * (BMP). These are encoded in UTF-16 using previously reserved values
 * between U+D800 and U+DFFF. Additionally, the UTF-8 writer does not
 * support any character that requires 4 or more UTF-8 encoded bytes.
 */
public class UTF_8_Writer extends com.sun.cldc.i18n.StreamWriter {

    /**
     * Write a portion of an array of characters.
     *
     * @param  cbuf  Array of characters
     * @param  off   Offset from which to start writing characters
     * @param  len   Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws IOException {
	byte[] outputByte = new byte[3];     // Never more than 3 encoded bytes
	char   inputChar;
	int    outputSize;
        int count = 0;

	while (count < len) {
	    inputChar = cbuf[off + count];
	    if (inputChar < 0x80) {
		outputByte[0] = (byte)inputChar;
		outputSize = 1;
	    } else if (inputChar < 0x800) {
		outputByte[0] = (byte)(0xc0 | ((inputChar >> 6) & 0x1f));
		outputByte[1] = (byte)(0x80 | (inputChar & 0x3f));
		outputSize = 2;
	    } else {
		outputByte[0] = (byte)(0xe0 | ((inputChar >> 12)) & 0x0f);
		outputByte[1] = (byte)(0x80 | ((inputChar >> 6) & 0x3f));
		outputByte[2] = (byte)(0x80 | (inputChar & 0x3f));
		outputSize = 3;
	    } 
	    out.write(outputByte, 0, outputSize);
	    count++;
	}
    }

    /**
     * Get the size in chars of an array of bytes.
     *
     * @param      array  Source buffer
     * @param      offset Offset at which to start counting characters
     * @param      length number of bytes to use for counting
     *
     * @return     number of characters that would be converted
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



