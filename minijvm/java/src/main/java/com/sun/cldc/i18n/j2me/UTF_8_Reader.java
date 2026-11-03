/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n.j2me;

import com.sun.cldc.i18n.StreamReader;
import java.io.IOException;

/** Strict UTF-8 decoder. Malformed input is replaced with U+FFFD. */
public final class UTF_8_Reader extends StreamReader {
    private int pendingByte = -1;
    private int pendingLowSurrogate = -1;

    public int read() throws IOException {
        char[] one = new char[1];
        int count = read(one, 0, 1);
        return count < 0 ? -1 : one[0];
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
        int count = 0;
        if (pendingLowSurrogate >= 0) {
            output[offset + count++] = (char) pendingLowSurrogate;
            pendingLowSurrogate = -1;
        }
        while (count < length) {
            int codePoint = readCodePoint();
            if (codePoint < 0) {
                return count == 0 ? -1 : count;
            }
            if (codePoint <= 0xffff) {
                output[offset + count++] = (char) codePoint;
            } else {
                int value = codePoint - 0x10000;
                output[offset + count++] = (char) (0xd800 | (value >>> 10));
                char low = (char) (0xdc00 | (value & 0x3ff));
                if (count < length) {
                    output[offset + count++] = low;
                } else {
                    pendingLowSurrogate = low;
                }
            }
        }
        return count;
    }

    private int readCodePoint() throws IOException {
        int first = nextByte();
        if (first < 0) {
            return -1;
        }
        if (first < 0x80) {
            return first;
        }
        int needed;
        int value;
        int minimum;
        if (first >= 0xc2 && first <= 0xdf) {
            needed = 1;
            value = first & 0x1f;
            minimum = 0x80;
        } else if (first >= 0xe0 && first <= 0xef) {
            needed = 2;
            value = first & 0x0f;
            minimum = 0x800;
        } else if (first >= 0xf0 && first <= 0xf4) {
            needed = 3;
            value = first & 0x07;
            minimum = 0x10000;
        } else {
            return 0xfffd;
        }
        for (int i = 0; i < needed; i++) {
            int next = nextByte();
            if (next < 0) {
                return 0xfffd;
            }
            if ((next & 0xc0) != 0x80) {
                pendingByte = next;
                return 0xfffd;
            }
            value = (value << 6) | (next & 0x3f);
        }
        if (value < minimum || value > 0x10ffff ||
                (value >= 0xd800 && value <= 0xdfff)) {
            return 0xfffd;
        }
        return value;
    }

    private int nextByte() throws IOException {
        if (pendingByte >= 0) {
            int value = pendingByte;
            pendingByte = -1;
            return value;
        }
        return in.read();
    }

    public int sizeOf(byte[] input, int offset, int length) {
        if (input == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > input.length - length) {
            throw new IndexOutOfBoundsException();
        }
        int count = 0;
        int end = offset + length;
        while (offset < end) {
            int first = input[offset++] & 0xff;
            int needed = first < 0x80 ? 0 :
                    (first >= 0xc2 && first <= 0xdf ? 1 :
                    (first >= 0xe0 && first <= 0xef ? 2 :
                    (first >= 0xf0 && first <= 0xf4 ? 3 : -1)));
            if (needed < 0) {
                count++;
                continue;
            }
            int value = first & (needed == 0 ? 0x7f :
                    (needed == 1 ? 0x1f : (needed == 2 ? 0x0f : 0x07)));
            int consumed = 0;
            while (consumed < needed && offset < end &&
                    (input[offset] & 0xc0) == 0x80) {
                value = (value << 6) | (input[offset++] & 0x3f);
                consumed++;
            }
            if (consumed != needed || value > 0x10ffff ||
                    (value >= 0xd800 && value <= 0xdfff)) {
                count++;
            } else {
                count += value > 0xffff ? 2 : 1;
            }
        }
        return count;
    }

    public boolean markSupported() {
        return false;
    }
}
