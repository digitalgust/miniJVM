/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n.j2me;

import com.sun.cldc.i18n.StreamWriter;
import java.io.IOException;

/** ISO-8859-1 encoder using '?' for characters outside Latin-1. */
public final class ISO8859_1_Writer extends StreamWriter {
    public void write(int value) throws IOException {
        out.write(value <= 0xff ? value : '?');
    }

    public void write(char[] input, int offset, int length) throws IOException {
        if (input == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > input.length - length) {
            throw new IndexOutOfBoundsException();
        }
        for (int end = offset + length; offset < end; offset++) {
            write(input[offset]);
        }
    }

    public int sizeOf(char[] array, int offset, int length) {
        return length;
    }
}
