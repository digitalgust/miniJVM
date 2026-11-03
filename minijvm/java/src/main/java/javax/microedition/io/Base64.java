/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import java.io.IOException;

/** RFC 4648 basic Base64 codec without line wrapping. */
public final class Base64 {
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private Base64() {
    }

    public static String encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    public static String encode(byte[] data, int offset, int length) {
        checkBounds(data == null ? -1 : data.length, offset, length);
        char[] output = new char[((length + 2) / 3) * 4];
        int input = offset;
        int end = offset + length;
        int out = 0;
        while (input < end) {
            int a = data[input++] & 0xff;
            int b = input < end ? data[input++] & 0xff : -1;
            int c = input < end ? data[input++] & 0xff : -1;
            output[out++] = ALPHABET[a >>> 2];
            output[out++] = ALPHABET[((a & 3) << 4) | (b < 0 ? 0 : b >>> 4)];
            output[out++] = b < 0 ? '=' : ALPHABET[((b & 15) << 2) | (c < 0 ? 0 : c >>> 6)];
            output[out++] = c < 0 ? '=' : ALPHABET[c & 63];
        }
        return new String(output);
    }

    public static byte[] decode(String encoded) throws IOException {
        return decode(encoded, 0, encoded.length());
    }

    public static byte[] decode(String encoded, int offset, int length)
            throws IOException {
        if (encoded == null) {
            throw new NullPointerException();
        }
        checkBounds(encoded.length(), offset, length);
        if ((length & 3) != 0) {
            throw new IOException("Base64 string not a multiple of 4");
        }
        if (length == 0) {
            return new byte[0];
        }
        int padding = 0;
        if (encoded.charAt(offset + length - 1) == '=') {
            padding++;
        }
        if (encoded.charAt(offset + length - 2) == '=') {
            padding++;
        }
        byte[] output = new byte[(length / 4) * 3 - padding];
        int out = 0;
        for (int i = offset; i < offset + length; i += 4) {
            boolean last = i + 4 == offset + length;
            int a = decodeValue(encoded.charAt(i));
            int b = decodeValue(encoded.charAt(i + 1));
            char third = encoded.charAt(i + 2);
            char fourth = encoded.charAt(i + 3);
            int c = third == '=' ? 0 : decodeValue(third);
            int d = fourth == '=' ? 0 : decodeValue(fourth);
            if (a < 0 || b < 0 || c < 0 || d < 0 ||
                    (!last && (third == '=' || fourth == '=')) ||
                    (third == '=' && fourth != '=')) {
                throw new IOException("Invalid character in Base64 string");
            }
            if (out < output.length) {
                output[out++] = (byte) ((a << 2) | (b >>> 4));
            }
            if (out < output.length) {
                output[out++] = (byte) ((b << 4) | (c >>> 2));
            }
            if (out < output.length) {
                output[out++] = (byte) ((c << 6) | d);
            }
        }
        return output;
    }

    private static int decodeValue(char value) {
        if (value >= 'A' && value <= 'Z') return value - 'A';
        if (value >= 'a' && value <= 'z') return value - 'a' + 26;
        if (value >= '0' && value <= '9') return value - '0' + 52;
        if (value == '+') return 62;
        if (value == '/') return 63;
        return -1;
    }

    private static void checkBounds(int size, int offset, int length) {
        if (size < 0) throw new NullPointerException();
        if (offset < 0 || length < 0 || offset > size - length) {
            throw new IndexOutOfBoundsException();
        }
    }
}
