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
/** 
 * Character conversion base class
 */
class Conv {
    /**
     * Native method to get a handle to specific character 
     * encoding convesrion routine.
     *
     * @param encoding character encoding
     * @return identifier for requested handler, or zero if
     * the encoding was not supported.
     */
    native static int getHandler(String encoding);

    /**
     * Native function to set the maximum length in bytes 
     * for a converted string.
     *
     * @param handler handle returned from getHandler
     * @return maximum byte length, or zero if handler
     * is not valid
     * @see #getHandler
     */
    native static int getMaxByteLength(int handler);

    /**
     * Get the length of a specific converted string.
     *
     * @param handler handle returned from getHandler
     * @param b buffer of bytes to be converted
     * @param offset offset into the provided buffer
     * @param len length of data to be processed
     * @return length of the converted string, or zero
     * if arguments were not valid
     * @see #getHandler
     */
    native static int getByteLength(int handler, 
                                    byte[] b, int offset, int len);

    /**
     * Native function to convert an array of bytes to 
     * converted array of characters.
     *
     * @param handler handle returned from getHandler
     * @param input buffer of bytes to be converted
     * @param in_offset offset into the provided buffer
     * @param in_len length of data to be processed
     * @param output buffer of converted bytes
     * @param out_offset offset into the provided output buffer
     * @param out_len length of data processed
     * @return length of the converted string, or zero if
     * the arguments were not valid
     * @see #getHandler
     */
    native static int byteToChar(int handler, 
                                 byte[] input,  int in_offset,  int in_len,
                                 char[] output, int out_offset, int out_len);
    /**
     * Native function to convert an array of characters
     * to an array of converted characters.
     *
     * @param handler handle returned from getHandler
     * @param input buffer of bytes to be converted
     * @param in_offset offset into the provided buffer
     * @param in_len length of data to be processed
     * @param output buffer of converted bytes
     * @param out_offset offset into the provided output buffer
     * @param out_len length of data processed
     * @return length of the converted string, or zero if 
     * the arguments were not valid
     * @see #getHandler
     */
    native static int charToByte(int handler, 
                                 char[] input,  int in_offset,  int in_len,
                                 byte[] output, int out_offset, int out_len);
    /**
     * Native function to get the length of a specific 
     * converted string as an array of Unicode bytes.
     *
     * @param handler handle returned from getHandler
     * @param b buffer of bytes to be converted
     * @param offset offset into the provided buffer
     * @param length length of data to be processed
     * @return length of the converted string, or zero
     * if the arguments were not valid
     * @see #getHandler
     */
    native static int sizeOfByteInUnicode(int handler,
                                          byte[] b, int offset, int length);
    /**
     * Native function to get the length of a specific 
     * converted string as an array of Unicode characters.
     *
     * @param handler handle returned from getHandler
     * @param c buffer of characters to be converted
     * @param offset offset into the provided buffer
     * @param length length of data to be processed
     * @return length of the converted string, or zero
     * if the arguments were not valid
     * @see #getHandler
     */
    native static int sizeOfUnicodeInByte(int handler,
                                          char[] c, int offset, int length);
    // debug method
    // native static void println(int handler, String str);
}
