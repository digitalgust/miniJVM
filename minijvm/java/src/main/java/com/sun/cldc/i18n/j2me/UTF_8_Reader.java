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


package com.sun.cldc.i18n.j2me;

import java.io.*;

/**
 * Reader for UTF-8 encoded input streams.
 */
public class UTF_8_Reader extends com.sun.cldc.i18n.StreamReader {
    /**
     * signals that no byte is available, but not the end of stream
     */
    private static final int NO_BYTE = -2;
    /**
     * read ahead buffer that to holds part of char from the last read
     */
    private int[] readAhead;
    /**
     * when reading first of a char byte we need to know if the first read
     */
    private boolean newRead;

    /**
     * Constructs a UTF-8 reader.
     */
    public UTF_8_Reader() {
        readAhead = new int[3];
        prepareForNextChar();
    }

    /**
     * Read a block of UTF8 characters.
     *
     * @param cbuf output buffer for converted characters read
     * @param off  initial offset into the provided buffer
     * @param len  length of characters in the buffer
     * @return the number of converted characters
     * @throws IOException is thrown if the input stream
     *                     could not be read for the raw unconverted character
     */
//    public int read1(char cbuf[], int off, int len) throws IOException {
//        int count = 0;
//        int firstByte;
//        int extraBytes;
//        int currentChar = 0;
//        int nextByte;
//
//        if (len == 0) {
//            return 0;
//        }
//
//        newRead = true;
//        while (count < len) {
//            firstByte = getByteOfCurrentChar(0);
//            if (firstByte < 0) {
//                if (firstByte == -1 && count == 0) {
//                    // end of stream
//                    return -1;
//                }
//
//                return count;
//            }
//
//            switch (firstByte >> 4) {
//                case 0:
//                case 1:
//                case 2:
//                case 3:
//                case 4:
//                case 5:
//                case 6:
//                case 7:
//                    /* 7 bits: 0xxxxxxx */
//                    extraBytes = 0;
//                    currentChar = firstByte;
//                    break;
//
//                case 12:
//                case 13:
//                    /* 11 bits: 110x xxxx   10xx xxxx */
//                    extraBytes = 1;
//                    currentChar = firstByte & 0x1F;
//                    break;
//
//                case 14:
//                    /* 16 bits: 1110 xxxx  10xx xxxx  10xx xxxx */
//                    extraBytes = 2;
//                    currentChar = firstByte & 0x0F;
//                    break;
//
//                default:
//                    /* we do not handle characters greater the 16 bits */
//                    throw new UTFDataFormatException("invalid first byte " +
//                            Integer.toBinaryString(firstByte));
//            }
//
//            for (int j = 1; j <= extraBytes; j++) {
//                nextByte = getByteOfCurrentChar(j);
//                if (nextByte == NO_BYTE) {
//                    // done for now, comeback later for the rest of char
//                    return count;
//                }
//
//                if (nextByte == -1) {
//                    // end of stream in the middle of char
//                    throw new UTFDataFormatException("partial character");
//                }
//
//                if ((nextByte & 0xC0) != 0x80) {
//                    throw new UTFDataFormatException("invalid byte " +
//                            Integer.toBinaryString(nextByte));
//                }
//
//                // each extra byte has 6 bits more of the char
//                currentChar = (currentChar << 6) + (nextByte & 0x3F);
//            }
//
//            cbuf[off + count] = (char) currentChar;
//            count++;
//            prepareForNextChar();
//        }
//
//        return count;
//    }
    public int read(char cbuf[], int off, int len) throws IOException {

        if (len == 0) {
            return 0;
        }

        int outputSize = 0; //记录转换后的Unicode字符串的字节数
        // b1 表示UTF-8编码的pInput中的高字节, b2 表示次高字节, ...
        int b1, b2, b3, b4, b5, b6;
        int codepoint;

        while (true) {
            b1 = in.read();
            if (b1 < 0) {
                if (outputSize == 0) {
                    outputSize = -1;
                }
                break;
            }
            int utfbytes = enc_get_utf8_size((byte) b1);
            codepoint = 0;
            switch (utfbytes) {
                case 1:
                    codepoint = b1;
                    break;
                case 2:
                    b2 = in.read();
                    if (b2 < 0) return -1;
                    if ((b2 & 0xc0) != 0x80)
                        return -1;
                    codepoint = ((b1 << 6) + (b2 & 0x3F)) & 0xff;
                    codepoint |= (((b1 >>> 2) & 0x07) & 0xff) << 8;
                    break;

                case 3:
                    b2 = in.read();
                    if (b2 < 0) return -1;
                    b3 = in.read();
                    if (b3 < 0) return -1;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80))
                        return -1;
                    codepoint = ((b2 << 6) + (b3 & 0x3F)) & 0xff;
                    codepoint |= (((b1 << 4) + ((b2 >>> 2) & 0x0F)) & 0xff) << 8;
                    break;

                case 4:
                    b2 = in.read();
                    if (b2 < 0) return -1;
                    b3 = in.read();
                    if (b3 < 0) return -1;
                    b4 = in.read();
                    if (b4 < 0) return -1;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                            || ((b4 & 0xC0) != 0x80))
                        return -1;
                    codepoint = ((b3 << 6) + (b4 & 0x3F)) & 0xff;
                    codepoint |= (((b2 << 4) + ((b3 >>> 2) & 0x0F)) & 0xff) << 8;
                    codepoint |= ((((b1 << 2) & 0x1C) + ((b2 >>> 4) & 0x03)) & 0xff) << 16;
                    break;

                case 5:
                    b2 = in.read();
                    if (b2 < 0) return -1;
                    b3 = in.read();
                    if (b3 < 0) return -1;
                    b4 = in.read();
                    if (b4 < 0) return -1;
                    b5 = in.read();
                    if (b5 < 0) return -1;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                            || ((b4 & 0xC0) != 0x80) || ((b5 & 0xC0) != 0x80))
                        return -1;
                    codepoint = ((b4 << 6) + (b5 & 0x3F)) & 0xff;
                    codepoint |= (((b3 << 4) + ((b4 >>> 2) & 0x0F)) & 0xff) << 8;
                    codepoint |= (((b2 << 2) + ((b3 >>> 4) & 0x03)) & 0xff) << 16;
                    codepoint |= ((b1 << 6) & 0xff) << 24;
                    break;

                case 6:
                    b2 = in.read();
                    if (b2 < 0) return -1;
                    b3 = in.read();
                    if (b3 < 0) return -1;
                    b4 = in.read();
                    if (b4 < 0) return -1;
                    b5 = in.read();
                    if (b5 < 0) return -1;
                    b6 = in.read();
                    if (b6 < 0) return -1;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                            || ((b4 & 0xC0) != 0x80) || ((b5 & 0xC0) != 0x80)
                            || ((b6 & 0xC0) != 0x80))
                        return -1;
                    codepoint = ((b5 << 6) + (b6 & 0x3F)) & 0xff;
                    codepoint |= (((b5 << 4) + ((b6 >>> 2) & 0x0F)) & 0xff) << 8;
                    codepoint |= (((b3 << 2) + ((b4 >>> 4) & 0x03)) & 0xff) << 16;
                    codepoint |= ((((b1 << 6) & 0x40) + (b2 & 0x3F)) & 0xff) << 24;
                    break;

                default:
                    return -1;
            }
            if (utfbytes >= 4) {
                codepoint -= 0x10000;
                char c1 = (char) (codepoint >> 10);
                cbuf[off + outputSize] = (char) (0xD800 | (c1 & 0x3ff));
                outputSize++;
                cbuf[off + outputSize] = (char) (0xDC00 | (codepoint & 0x3ff));
                outputSize++;
            } else {
                cbuf[off + outputSize] = (char) codepoint;
                outputSize++;
            }

            if (outputSize >= len) break;
        }
        return outputSize;
    }

    private int enc_get_utf8_size(byte pInput) {
        int c = pInput & 0xff;
        if (c < 0x80) return 1;                // 0xxxxxxx 返回0
        if (c >= 0x80 && c < 0xC0) return -1;     // 10xxxxxx 返回-1
        if (c >= 0xC0 && c < 0xE0) return 2;      // 110xxxxx 返回2
        if (c >= 0xE0 && c < 0xF0) return 3;      // 1110xxxx 返回3
        if (c >= 0xF0 && c < 0xF8) return 4;      // 11110xxx 返回4
        if (c >= 0xF8 && c < 0xFC) return 5;      // 111110xx 返回5
        if (c >= 0xFC) return 6;                // 1111110x 返回6
        return 0;
    }

    /**
     * Get one of the raw bytes for the current character to be converted
     * from look ahead buffer.
     *
     * @param byteOfChar which raw byte to get 0 for the first, 2 for the last
     * @return a byte value, NO_BYTE for no byte available or -1 for end of
     * stream
     * @throws IOException if an I/O error occurs.
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
     *
     * @param readAheadLimit number of characters to buffer ahead
     * @throws IOException is thrown, for all calls to this method
     *                     because marking is not supported for UTF8 readers
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported");
    }

    /**
     * Reset the read ahead marks is not supported for UTF8 readers.
     *
     * @throws IOException is thrown, for all calls to this method
     *                     because marking is not supported for UTF8 readers
     */
    public void reset() throws IOException {
        throw new IOException("reset() not supported");
    }

    /**
     * Get the size in chars of an array of bytes.
     *
     * @param array  Source buffer
     * @param offset Offset at which to start counting characters
     * @param length number of bytes to use for counting
     * @return number of characters that would be converted
     */
    /*
     * This method is only used by our internal Helper class in the method
     * byteToCharArray to know how much to allocate before using a
     * reader. If we encounter bad encoding we should return a count
     * that includes that character so the reader will throw an IOException
     */
//    public int sizeOf(byte[] array, int offset, int length) {
//        int count = 0;
//        int endOfArray;
//
//        for (endOfArray = offset + length; offset < endOfArray; ) {
//            count++;
//            switch (((int)array[offset] & 0xff) >> 4) {
//            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
//                /* 0xxxxxxx */
//                offset++;
//                break;
//
//            case 12: case 13:
//                /* 110x xxxx   10xx xxxx */
//                offset += 2;
//                break;
//
//            case 14:
//                /* 1110 xxxx  10xx xxxx  10xx xxxx */
//                offset += 3;
//                break;
//
//            default:
//                /*
//                 * we do not support characters greater than 16 bits
//                 * return the current count, the reader will catch this
//                 */
//                return count;
//            }
//        }
//
//        return count;
//    }
    public int sizeOf(byte[] array, int offset, int length) {
        //assert(pInput != NULL && Unic != NULL);
        int outputSize = 0; //记录转换后的Unicode字符串的字节数
        // b1 表示UTF-8编码的pInput中的高字节, b2 表示次高字节, ...
        int b1, b2, b3, b4, b5, b6;
        int end = offset + length;

        while (offset < end) {
            b1 = array[offset];
            int utfbytes = enc_get_utf8_size((byte) b1);
            switch (utfbytes) {
                case 1:
                    break;
                case 2:
                    b2 = array[offset + 1];
                    b2 &= 0xff;
                    if ((b2 & 0xc0) != 0x80)
                        offset = length;
                    break;

                case 3:
                    b2 = array[offset + 1];
                    b2 &= 0xff;
                    b3 = array[offset + 2];
                    b3 &= 0xff;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80))
                        offset = length;
                    break;

                case 4:
                    b2 = array[offset + 1];
                    b2 &= 0xff;
                    b3 = array[offset + 2];
                    b3 &= 0xff;
                    b4 = array[offset + 3];
                    b4 &= 0xff;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                            || ((b4 & 0xC0) != 0x80))
                        offset = length;
                    break;

                case 5:
                    b2 = array[offset + 1];
                    b2 &= 0xff;
                    b3 = array[offset + 2];
                    b3 &= 0xff;
                    b4 = array[offset + 3];
                    b4 &= 0xff;
                    b5 = array[offset + 4];
                    b5 &= 0xff;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                            || ((b4 & 0xC0) != 0x80) || ((b5 & 0xC0) != 0x80))
                        offset = length;
                    break;

                case 6:
                    b2 = array[offset + 1];
                    b2 &= 0xff;
                    b3 = array[offset + 2];
                    b3 &= 0xff;
                    b4 = array[offset + 3];
                    b4 &= 0xff;
                    b5 = array[offset + 4];
                    b5 &= 0xff;
                    b6 = array[offset + 5];
                    b6 &= 0xff;
                    if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                            || ((b4 & 0xC0) != 0x80) || ((b5 & 0xC0) != 0x80)
                            || ((b6 & 0xC0) != 0x80))
                        offset = length;
                    break;

                default:
                    offset = length;
            }
            if (utfbytes >= 4) {
                outputSize += 2;
            } else {
                outputSize++;
            }
            offset += utfbytes;
        }
        return outputSize;
    }
}
