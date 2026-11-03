/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n.j2me;

import com.sun.cldc.i18n.StreamWriter;
import java.io.IOException;

/** UTF-8 encoder for Unicode scalar values. */
public final class UTF_8_Writer extends StreamWriter {
    public void write(char[] input, int offset, int length) throws IOException {
        if (input == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > input.length - length) {
            throw new IndexOutOfBoundsException();
        }
        int end = offset + length;
        while (offset < end) {
            char first = input[offset++];
            int codePoint;
            if (Character.isHighSurrogate(first)) {
                if (offset < end && Character.isLowSurrogate(input[offset])) {
                    codePoint = Character.toCodePoint(first, input[offset++]);
                } else {
                    codePoint = 0xfffd;
                }
            } else if (Character.isLowSurrogate(first)) {
                codePoint = 0xfffd;
            } else {
                codePoint = first;
            }
            writeCodePoint(codePoint);
        }
    }

    private void writeCodePoint(int value) throws IOException {
        if (value <= 0x7f) {
            out.write(value);
        } else if (value <= 0x7ff) {
            out.write(0xc0 | (value >>> 6));
            out.write(0x80 | (value & 0x3f));
        } else if (value <= 0xffff) {
            out.write(0xe0 | (value >>> 12));
            out.write(0x80 | ((value >>> 6) & 0x3f));
            out.write(0x80 | (value & 0x3f));
        } else {
            out.write(0xf0 | (value >>> 18));
            out.write(0x80 | ((value >>> 12) & 0x3f));
            out.write(0x80 | ((value >>> 6) & 0x3f));
            out.write(0x80 | (value & 0x3f));
        }
    }

    public int sizeOf(char[] input, int offset, int length) {
        if (input == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > input.length - length) {
            throw new IndexOutOfBoundsException();
        }
        int size = 0;
        int end = offset + length;
        while (offset < end) {
            char value = input[offset++];
            if (value <= 0x7f) {
                size++;
            } else if (value <= 0x7ff) {
                size += 2;
            } else if (Character.isHighSurrogate(value) && offset < end &&
                    Character.isLowSurrogate(input[offset])) {
                offset++;
                size += 4;
            } else {
                size += 3;
            }
        }
        return size;
    }
}
