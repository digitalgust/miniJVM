package org.mini.util;


import com.sun.cldc.i18n.Helper;

import java.util.Arrays;

/**
 * a StringBuilder like , for simplify text edit
 */
public class CodePointBuilder {
    private int[] value;
    private int count;

    public CodePointBuilder() {
        this(16);
    }


    public CodePointBuilder(int initSize) {
        value = new int[initSize];
        count = 0;
    }


    public CodePointBuilder(String str) {
        this(str.length() + 32);
        append(str);
    }

    public CodePointBuilder(byte[] bytes, int offset, int len, String encode) {
        this(len);
        try {
            char[] chars = Helper.byteToCharArray(bytes, offset, len, encode);
            int n = copyCodePoints(chars, 0, chars.length, value, count);
            count += n;
        } catch (Exception e) {
        }
    }

    static int copyCodePoints(char[] chars, int srcBegin, int srcEnd, int[] dst, int dstOffset) {

        int n = 0, codepoint = 0;
        for (int i = srcBegin; i < srcEnd; ) {
            n++;
            char ch = chars[i];
            i++;
            if (Character.isHighSurrogate(ch)) {
                if (i < srcEnd) {
                    char ch1 = chars[i];
                    if (Character.isLowSurrogate(ch1)) {
                        i++;
                        codepoint = Character.toCodePoint(ch, ch1);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                codepoint = ch;
            }
            dst[dstOffset] = codepoint;
            dstOffset++;
        }
        return n;
    }


    public CodePointBuilder append(String str) {
        int len = str.codePointCount(0, str.length());
        int newcount = count + len;
        if (newcount > value.length) {
            ensureCapacity(newcount);
        }
        copyCodePoints(str, 0, str.length(), value, count);
        count = newcount;
        return this;
    }

    static int copyCodePoints(String str, int srcBegin, int srcEnd, int[] dst, int dstOffset) {

        int n = 0, codepoint = 0;
        for (int i = srcBegin; i < srcEnd; ) {
            n++;
            char ch = str.charAt(i);
            i++;
            if (Character.isHighSurrogate(ch)) {
                if (i < srcEnd) {
                    char ch1 = str.charAt(i);
                    if (Character.isLowSurrogate(ch1)) {
                        i++;
                        codepoint = Character.toCodePoint(ch, ch1);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                codepoint = ch;
            }
            dst[dstOffset] = codepoint;
            dstOffset++;
        }
        return n;
    }


    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > value.length) {//只大不小
            expandCapacity(minimumCapacity);
        }
    }

    void expandCapacity(int minimumCapacity) {
        int newCapacity = (value.length + 1) * 2;
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        } else if (minimumCapacity > newCapacity) {
            newCapacity = minimumCapacity;
        }

        int newValue[] = new int[newCapacity];
        System.arraycopy(value, 0, newValue, 0, count);
        value = newValue;
    }


    public String substring(int start) {
        return substring(start, count);
    }

    public String substring(int start, int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > count) {
            throw new StringIndexOutOfBoundsException(end);
        }
        if (start > end) {
            throw new StringIndexOutOfBoundsException(end - start);
        }
        return new String(value, start, end - start);
    }

    public void setLength(int newLength) {
        if (newLength < 0) throw new StringIndexOutOfBoundsException(newLength);

        if (count < newLength) {
            Arrays.fill(value, count, newLength, '\0');
        }

        count = newLength;
    }

    public int length() {
        return count;
    }

    public CodePointBuilder insert(int offset, String str) {
        if ((offset < 0) || (offset > count)) {
            throw new StringIndexOutOfBoundsException();
        }

        if (str == null) {
            str = String.valueOf(str);
        }
        int len = str.codePointCount(0, str.length());
        int newcount = count + len;
        if (newcount > value.length) {
            ensureCapacity(newcount);
        }
        System.arraycopy(value, offset, value, offset + len, count - offset);
        copyCodePoints(str, 0, str.length(), value, offset);
        count = newcount;
        return this;
    }

    public CodePointBuilder appendCodePoint(int codePoint) {// 😀
        final int count = this.count;

        if (Character.isValidCodePoint(codePoint)) {
            ensureCapacity(count + 1);
            value[count] = codePoint;
            this.count = count + 1;
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }


    public CodePointBuilder insertCodePoint(int offset, int codePoint) {
        final int count = this.count;

        if (Character.isValidCodePoint(codePoint)) {
            ensureCapacity(count + 1);
            System.arraycopy(value, offset, value, offset + 1, count - offset);
            value[offset] = codePoint;
            this.count = count + 1;
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }


//    public CodePointBuilder insert(int offset, String str) {
//        if ((offset < 0) || (offset > length()))
//            throw new StringIndexOutOfBoundsException(offset);
//        if (str == null)
//            str = "null";
//
//        int len = str.codePointCount(0, str.length());
//        expandCapacity(count + len);
//        System.arraycopy(value, offset, value, offset + len, count - offset);
//        getCodepoints(str, 0, str.length(), value, offset);
//        this.count = count + 1;
//        return this;
//    }

    public CodePointBuilder deleteCodePointAt(int index) {
        if ((index < 0) || (index >= count)) throw new StringIndexOutOfBoundsException(index);
        System.arraycopy(value, index + 1, value, index, count - index - 1);
        count--;
        return this;
    }

    public CodePointBuilder delete(int start, int end) {
        if (start < 0) throw new StringIndexOutOfBoundsException(start);
        if (end > count) end = count;
        if (start > end) throw new StringIndexOutOfBoundsException();
        int len = end - start;
        if (len > 0) {
            System.arraycopy(value, start + len, value, start, count - end);
            count -= len;
        }
        return this;
    }

    public int codePointAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.appendCodePoint(value[i]);
        return sb.toString();
    }

    public CodePointBuilder append(long l) {
        if (l == Long.MIN_VALUE) {
            append("-9223372036854775808");
            return this;
        }
        append(Long.toString(l));
        return this;
    }

    public CodePointBuilder insert(int offset, long l) {
        if (l == Long.MIN_VALUE) {
            append("-9223372036854775808");
            return this;
        }
        insert(offset, Long.toString(l));
        return this;
    }

    public CodePointBuilder append(char c) {
        appendCodePoint(c);
        return this;
    }

    public CodePointBuilder insert(int offset, char c) {
        insertCodePoint(offset, c);
        return this;
    }


    public CodePointBuilder append(float f) {
        append(Float.toString(f));
        return this;
    }

    public CodePointBuilder insert(int offset, float f) {
        insert(offset, Float.toString(f));
        return this;
    }

    public CodePointBuilder append(double d) {
        append(Double.toString(d));
        return this;
    }

    public CodePointBuilder insert(int offset, double d) {
        insert(offset, Double.toString(d));
        return this;
    }


    public CodePointBuilder append(Object o) {
        append(o == null ? "null" : o.toString());
        return this;
    }

    public CodePointBuilder insert(int offset, Object o) {
        insert(offset, o == null ? "null" : o.toString());
        return this;
    }

    public CodePointBuilder replace(int start, int end, String s) {
        delete(start, end);
        insert(start, s);
        return this;
    }

    public CodePointBuilder reverse() {
        for (int i = count / 2; i >= 0; i--) {
            int a = value[i];
            value[i] = value[count - i];
            value[count - i] = a;
        }
        return this;
    }
}
