/*
 * @(#)UTF_8_Reader.java	1.5 02/07/24 @(#)
 *
 * Copyright (c) 2001-2002 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */

package com.sun.cldc.i18n.mini;

import java.io.*;

/** Reader for UTF-8 encoded input streams. */
public class UTF_8_Reader extends com.sun.cldc.i18n.StreamReader {
    /** signals that no byte is available, but not the end of stream */
    private static final int NO_BYTE = -2;
    /** read ahead buffer that to holds part of char from the last read */
    private int[] readAhead;
    /** when reading first of a char byte we need to know if the first read */
    private boolean newRead;

    /** Constructs a UTF-8 reader. */
    public UTF_8_Reader() {
        readAhead = new int[3];
        prepareForNextChar();
    }

    /**
     * Read a block of UTF8 characters.
     *
     * @param cbuf output buffer for converted characters read
     * @param off initial offset into the provided buffer
     * @param len length of characters in the buffer
     * @return the number of converted characters
     * @exception IOException is thrown if the input stream 
     * could not be read for the raw unconverted character
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        int count = 0;
        int firstByte;
        int extraBytes;
        int currentChar = 0;
        int nextByte;
        
        if (len == 0) {
            return 0;
        }

        newRead = true;
        while (count < len) {
            firstByte = getByteOfCurrentChar(0);
            if (firstByte < 0) {
                if (firstByte == -1 && count == 0) {
                    // end of stream
                    return -1;
                }

                return count;
            }

            switch (firstByte >> 4) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                /* 7 bits: 0xxxxxxx */
                extraBytes = 0;
                currentChar = firstByte;
                break;

            case 12: case 13:
                /* 11 bits: 110x xxxx   10xx xxxx */
                extraBytes = 1;
                currentChar = firstByte & 0x1F;
                break;

            case 14:
                /* 16 bits: 1110 xxxx  10xx xxxx  10xx xxxx */
                extraBytes = 2;
                currentChar = firstByte & 0x0F;
                break;

            default:
                /* we do not handle characters greater the 16 bits */
                throw new UTFDataFormatException("invalid first byte " +
                    Integer.toBinaryString(firstByte));
            }

            for (int j = 1; j <= extraBytes; j++) {
                nextByte = getByteOfCurrentChar(j);
                if (nextByte == NO_BYTE) {
                    // done for now, comeback later for the rest of char
                    return count;
                }

                if (nextByte == -1) {
                    // end of stream in the middle of char
                    throw new UTFDataFormatException("partial character");
                }

                if ((nextByte & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("invalid byte " +
                        Integer.toBinaryString(nextByte));
                }

                // each extra byte has 6 bits more of the char
                currentChar = (currentChar << 6) + (nextByte & 0x3F);
            }

            cbuf[off + count] = (char)currentChar;
            count++;
            prepareForNextChar();
        }

        return count;
    }

    /**
     * Get one of the raw bytes for the current character to be converted
     * from look ahead buffer.
     *
     * @param byteOfChar which raw byte to get 0 for the first, 2 for the last
     *
     * @return a byte value, NO_BYTE for no byte available or -1 for end of
     *          stream
     *
     * @exception  IOException   if an I/O error occurs.
     */
    private int getByteOfCurrentChar(int byteOfChar) throws IOException {
        if (readAhead[byteOfChar] != NO_BYTE) {
            return readAhead[byteOfChar];
        }

        /*
         * Our read method must block until it gets one char so don't call
         * available on the first real stream for each new read().
         */
        if (!newRead && in.available() <= 0) {
            return NO_BYTE;
        }

        readAhead[byteOfChar] = in.read();

        /*
         * since we have read from the input stream,
         * this not a new read any more
         */
        newRead = false;

        return readAhead[byteOfChar];
    }

    /**
     * Prepare the reader for the next character by clearing the look
     * ahead buffer.
     */
    private void prepareForNextChar() {
        readAhead[0] = NO_BYTE;
        readAhead[1] = NO_BYTE;
        readAhead[2] = NO_BYTE;
    }

    /**
     * Tell whether this reader supports the mark() operation.
     * The UTF-8 implementation always returns false because it does not
     * support mark().
     *
     * @return false
     */
    public boolean markSupported() {
        /*
         * For readers mark() is in characters, since UTF-8 character are
         * variable length, so we can't just forward this to the underlying
         * byte InputStream like other readers do.
         * So this reader does not support mark at this time.
         */
        return false;
    }

    /**
     * Mark a read ahead character is not supported for UTF8
     * readers.
     * @param readAheadLimit number of characters to buffer ahead
     * @exception IOException is thrown, for all calls to this method
     * because marking is not supported for UTF8 readers
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported");
    }

    /**
     * Reset the read ahead marks is not supported for UTF8 readers.
     * @exception IOException is thrown, for all calls to this method
     * because marking is not supported for UTF8 readers
     */
    public void reset() throws IOException {
        throw new IOException("reset() not supported");
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
    /*
     * This method is only used by our internal Helper class in the method
     * byteToCharArray to know how much to allocate before using a
     * reader. If we encounter bad encoding we should return a count
     * that includes that character so the reader will throw an IOException
     */
    public int sizeOf(byte[] array, int offset, int length) {
        int count = 0;
        int endOfArray;

        for (endOfArray = offset + length; offset < endOfArray; ) {
            count++;
            switch (((int)array[offset] & 0xff) >> 4) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                /* 0xxxxxxx */
                offset++;
                break;

            case 12: case 13:
                /* 110x xxxx   10xx xxxx */
                offset += 2;
                break;

            case 14:
                /* 1110 xxxx  10xx xxxx  10xx xxxx */
                offset += 3;
                break;

            default:
                /*
                 * we do not support characters greater than 16 bits
                 * return the current count, the reader will catch this
                 */
                return count;
            }
        }

        return count;
    }
}
