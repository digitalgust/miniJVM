/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n;

import com.sun.cldc.i18n.j2me.ISO8859_1_Reader;
import com.sun.cldc.i18n.j2me.ISO8859_1_Writer;
import com.sun.cldc.i18n.j2me.UTF_8_Reader;
import com.sun.cldc.i18n.j2me.UTF_8_Writer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/** Encoding bridge used by the CLDC String and stream classes. */
public final class Helper {
    private static String defaultEncoding = initDefaultEncoding();

    private Helper() {
    }

    private static String initDefaultEncoding() {
        String value = System.getProperty("microedition.encoding");
        return value == null ? "ISO-8859-1" : value;
    }

    public static Reader getStreamReader(InputStream input) {
        try {
            return getStreamReader(input, defaultEncoding);
        } catch (UnsupportedEncodingException unsupported) {
            defaultEncoding = "ISO-8859-1";
            try {
                return getStreamReader(input, defaultEncoding);
            } catch (UnsupportedEncodingException impossible) {
                throw new RuntimeException(impossible.toString());
            }
        }
    }

    public static Reader getStreamReader(InputStream input, String encoding)
            throws UnsupportedEncodingException {
        if (input == null || encoding == null) {
            throw new NullPointerException();
        }
        StreamReader reader = newReader(encoding);
        return reader.open(input, encoding);
    }

    public static Writer getStreamWriter(OutputStream output) {
        try {
            return getStreamWriter(output, defaultEncoding);
        } catch (UnsupportedEncodingException unsupported) {
            defaultEncoding = "ISO-8859-1";
            try {
                return getStreamWriter(output, defaultEncoding);
            } catch (UnsupportedEncodingException impossible) {
                throw new RuntimeException(impossible.toString());
            }
        }
    }

    public static Writer getStreamWriter(OutputStream output, String encoding)
            throws UnsupportedEncodingException {
        if (output == null || encoding == null) {
            throw new NullPointerException();
        }
        StreamWriter writer = newWriter(encoding);
        return writer.open(output, encoding);
    }

    public static char[] byteToCharArray(byte[] data, int offset, int length) {
        try {
            return byteToCharArray(data, offset, length, defaultEncoding);
        } catch (UnsupportedEncodingException unsupported) {
            throw new RuntimeException(unsupported.toString());
        }
    }

    public static char[] byteToCharArray(byte[] data, int offset, int length,
                                         String encoding)
            throws UnsupportedEncodingException {
        checkBounds(data == null ? -1 : data.length, offset, length);
        StreamReader reader = newReader(encoding);
        char[] result = new char[reader.sizeOf(data, offset, length)];
        reader.open(new ByteArrayInputStream(data, offset, length), encoding);
        int used = 0;
        try {
            while (used < result.length) {
                int count = reader.read(result, used, result.length - used);
                if (count < 0) {
                    break;
                }
                used += count;
            }
            reader.close();
        } catch (IOException failure) {
            throw new RuntimeException(failure.toString());
        }
        if (used == result.length) {
            return result;
        }
        char[] trimmed = new char[used];
        System.arraycopy(result, 0, trimmed, 0, used);
        return trimmed;
    }

    public static byte[] charToByteArray(char[] data, int offset, int length) {
        try {
            return charToByteArray(data, offset, length, defaultEncoding);
        } catch (UnsupportedEncodingException unsupported) {
            throw new RuntimeException(unsupported.toString());
        }
    }

    public static byte[] charToByteArray(char[] data, int offset, int length,
                                         String encoding)
            throws UnsupportedEncodingException {
        checkBounds(data == null ? -1 : data.length, offset, length);
        StreamWriter writer = newWriter(encoding);
        ByteArrayOutputStream output =
                new ByteArrayOutputStream(writer.sizeOf(data, offset, length));
        writer.open(output, encoding);
        try {
            writer.write(data, offset, length);
            writer.close();
        } catch (IOException failure) {
            throw new RuntimeException(failure.toString());
        }
        return output.toByteArray();
    }

    private static StreamReader newReader(String encoding)
            throws UnsupportedEncodingException {
        String name = normalize(encoding);
        if ("UTF8".equals(name)) {
            return new UTF_8_Reader();
        }
        if ("ISO88591".equals(name) || "USASCII".equals(name)) {
            return new ISO8859_1_Reader();
        }
        throw new UnsupportedEncodingException("Encoding " + encoding + " not found");
    }

    private static StreamWriter newWriter(String encoding)
            throws UnsupportedEncodingException {
        String name = normalize(encoding);
        if ("UTF8".equals(name)) {
            return new UTF_8_Writer();
        }
        if ("ISO88591".equals(name) || "USASCII".equals(name)) {
            return new ISO8859_1_Writer();
        }
        throw new UnsupportedEncodingException("Encoding " + encoding + " not found");
    }

    private static String normalize(String encoding) {
        if (encoding == null) {
            throw new NullPointerException();
        }
        StringBuffer normalized = new StringBuffer();
        for (int i = 0; i < encoding.length(); i++) {
            char value = encoding.charAt(i);
            if (value != '-' && value != '_' && value != ':') {
                normalized.append(Character.toUpperCase(value));
            }
        }
        return normalized.toString();
    }

    private static void checkBounds(int size, int offset, int length) {
        if (size < 0) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > size - length) {
            throw new IndexOutOfBoundsException();
        }
    }
}
