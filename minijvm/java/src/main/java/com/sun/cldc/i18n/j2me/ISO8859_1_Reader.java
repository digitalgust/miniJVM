/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n.j2me;

import com.sun.cldc.i18n.StreamReader;
import java.io.IOException;

/** ISO-8859-1 decoder. */
public final class ISO8859_1_Reader extends StreamReader {
    public int read() throws IOException {
        return in.read();
    }

    public int read(char[] output, int offset, int length) throws IOException {
        if (output == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > output.length - length) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return 0;
        }
        int first = in.read();
        if (first < 0) {
            return -1;
        }
        output[offset] = (char) first;
        int count = 1;
        while (count < length) {
            int value = in.read();
            if (value < 0) {
                break;
            }
            output[offset + count++] = (char) value;
        }
        return count;
    }

    public int sizeOf(byte[] array, int offset, int length) {
        return length;
    }
}
