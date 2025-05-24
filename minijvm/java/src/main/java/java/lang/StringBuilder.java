/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package java.lang;

/**
 * A string buffer implements a mutable sequence of characters. A string buffer
 * is like a {@link String}, but can be modified. At any point in time it
 * contains some particular sequence of characters, but the length and content
 * of the sequence can be changed through certain method calls.
 * <p>
 * String buffers are safe for use by multiple threads. The methods are where
 * necessary so that all the operations on any particular instance behave as if
 * they occur in some serial order that is consistent with the order of the
 * method calls made by each of the individual threads involved.
 * <p>
 * String buffers are used by the compiler to implement the binary string
 * concatenation operator <code>+</code>. For example, the code:
 * <p>
 * <blockquote><pre>
 *     x = "a" + 4 + "c"
 * </pre></blockquote><p>
 * is compiled to the equivalent of:
 * <p>
 * <blockquote><pre>
 *     x = new StringBuilder().append("a").append(4).append("c")
 *                           .toString()
 * </pre></blockquote>
 * which creates a new string buffer (initially empty), appends the string
 * representation of each operand to the string buffer in turn, and then
 * converts the contents of the string buffer to a string. Overall, this avoids
 * creating many temporary strings.
 * <p>
 * The principal operations on a <code>StringBuilder</code> are the
 * <code>append</code> and <code>insert</code> methods, which are overloaded so
 * as to accept data of any type. Each effectively converts a given datum to a
 * string and then appends or inserts the characters of that string to the
 * string buffer. The <code>append</code> method always adds these characters at
 * the end of the buffer; the <code>insert</code> method adds the characters at
 * a specified point.
 * <p>
 * For example, if <code>z</code> refers to a string buffer object whose current
 * contents are "<code>start</code>", then the method call
 * <code>z.append("le")</code> would cause the string buffer to contain
 * "<code>startle</code>", whereas <code>z.insert(4, "le")</code> would alter
 * the string buffer to contain "<code>starlet</code>".
 * <p>
 * In general, if sb refers to an instance of a <code>StringBuilder</code>, then
 * <code>sb.append(x)</code> has the same effect as
 * <code>sb.insert(sb.length(),&nbsp;x)</code>.
 * <p>
 * Every string buffer has a capacity. As long as the length of the character
 * sequence contained in the string buffer does not exceed the capacity, it is
 * not necessary to allocate a new internal buffer array. If the internal buffer
 * overflows, it is automatically made larger.
 *
 * @author Arthur van Hoff
 * @version 12/17/01 (CLDC 1.1)
 * @see java.io.ByteArrayOutputStream
 * @see java.lang.String
 * @since JDK1.0, CLDC 1.0
 */
public class StringBuilder implements Appendable, CharSequence {

    /**
     * The value is used for character storage.
     */
    private char value[];

    /**
     * The count is the number of characters in the buffer.
     */
    private int count;

    /**
     * A flag indicating whether the buffer is shared
     */
    private boolean shared;

    /**
     * Constructs a string buffer with no characters in it and an initial
     * capacity of 16 characters.
     */
    public StringBuilder() {
        this(16);
    }

    /**
     * Constructs a string buffer with no characters in it and an initial
     * capacity specified by the <code>length</code> argument.
     *
     * @param length the initial capacity.
     * @throws NegativeArraySizeException if the <code>length</code> argument
     *                                    is less than <code>0</code>.
     */
    public StringBuilder(int length) {
        value = new char[length];
        shared = false;
    }

    /**
     * Constructs a string buffer so that it represents the same sequence of
     * characters as the string argument; in other words, the initial contents
     * of the string buffer is a copy of the argument string. The initial
     * capacity of the string buffer is <code>16</code> plus the length of the
     * string argument.
     *
     * @param str the initial contents of the buffer.
     */
    public StringBuilder(String str) {
        this(str.length() + 32);
        append(str);
    }

    /**
     * Returns the length (character count) of this string buffer.
     *
     * @return the length of the sequence of characters currently represented by
     * this string buffer.
     */
    public int length() {
        return count;
    }

    /**
     * Returns the current capacity of the String buffer. The capacity is the
     * amount of storage available for newly inserted characters; beyond which
     * an allocation will occur.
     *
     * @return the current capacity of this string buffer.
     */
    public int capacity() {
        return value.length;
    }

    /**
     * Copies the buffer value. This is normally only called when shared is
     * true. It should only be called from a method.
     */
    final void copy() {
        char newValue[] = new char[value.length];
        System.arraycopy(value, 0, newValue, 0, count);
        value = newValue;
        shared = false;
    }

    /**
     * Ensures that the capacity of the buffer is at least equal to the
     * specified minimum. If the current capacity of this string buffer is less
     * than the argument, then a new internal buffer is allocated with greater
     * capacity. The new capacity is the larger of:
     * <ul>
     * <li>The <code>minimumCapacity</code> argument.
     * <li>Twice the old capacity, plus <code>2</code>.
     * </ul>
     * If the <code>minimumCapacity</code> argument is nonpositive, this method
     * takes no action and simply returns.
     *
     * @param minimumCapacity the minimum desired capacity.
     */
    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > value.length) {
            expandCapacity(minimumCapacity);
        }
    }

    /**
     * This implements the expansion semantics of ensureCapacity but is un for
     * use internally by methods which are already .
     *
     * @see java.lang.StringBuilder#ensureCapacity(int)
     */
    void expandCapacity(int minimumCapacity) {
        int newCapacity = (value.length + 1) * 2;
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        } else if (minimumCapacity > newCapacity) {
            newCapacity = minimumCapacity;
        }

        char newValue[] = new char[newCapacity];
        System.arraycopy(value, 0, newValue, 0, count);
        value = newValue;
        shared = false;
    }

    /**
     * Sets the length of this string buffer. This string buffer is altered to
     * represent a new character sequence whose length is specified by the
     * argument. For every nonnegative index <i>k</i> less than
     * <code>newLength</code>, the character at index <i>k</i> in the new
     * character sequence is the same as the character at index <i>k</i> in the
     * old sequence if <i>k</i> is less than the length of the old character
     * sequence; otherwise, it is the null character <code>'\u0000'</code>.
     * <p>
     * In other words, if the <code>newLength</code> argument is less than the
     * current length of the string buffer, the string buffer is truncated to
     * contain exactly the number of characters given by the
     * <code>newLength</code> argument.
     * <p>
     * If the <code>newLength</code> argument is greater than or equal to the
     * current length, sufficient null characters (<code>'&#92;u0000'</code>)
     * are appended to the string buffer so that length becomes the
     * <code>newLength</code> argument.
     * <p>
     * The <code>newLength</code> argument must be greater than or equal to
     * <code>0</code>.
     *
     * @param newLength the new length of the buffer.
     * @throws IndexOutOfBoundsException if the <code>newLength</code>
     *                                   argument is negative.
     * @see java.lang.StringBuilder#length()
     */
    public void setLength(int newLength) {
        if (newLength < 0) {
            throw new StringIndexOutOfBoundsException(newLength);
        }

        if (newLength > value.length) {
            expandCapacity(newLength);
        }

        if (count < newLength) {
            if (shared) {
                copy();
            }
            for (; count < newLength; count++) {
                value[count] = '\0';
            }
        } else {
            count = newLength;
            if (shared) {
                if (newLength > 0) {
                    copy();
                } else {
                    // If newLength is zero, assume the StringBuilder is being
                    // stripped for reuse; Make new buffer of default size
                    value = new char[16];
                    shared = false;
                }
            }
        }
    }

    /**
     * The specified character of the sequence currently represented by the
     * string buffer, as indicated by the <code>index</code> argument, is
     * returned. The first character of a string buffer is at index
     * <code>0</code>, the next at index <code>1</code>, and so on, for array
     * indexing.
     * <p>
     * The index argument must be greater than or equal to <code>0</code>, and
     * less than the length of this string buffer.
     *
     * @param index the index of the desired character.
     * @return the character at the specified index of this string buffer.
     * @throws IndexOutOfBoundsException if <code>index</code> is negative or
     *                                   greater than or equal to <code>length()</code>.
     * @see java.lang.StringBuilder#length()
     */
    public char charAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    /**
     * Characters are copied from this string buffer into the destination
     * character array <code>dst</code>. The first character to be copied is at
     * index <code>srcBegin</code>; the last character to be copied is at index
     * <code>srcEnd-1</code>. The total number of characters to be copied is
     * <code>srcEnd-srcBegin</code>. The characters are copied into the subarray
     * of <code>dst</code> starting at index <code>dstBegin</code> and ending at
     * index:
     * <p>
     * <blockquote><pre>
     * dstbegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @param srcBegin start copying at this offset in the string buffer.
     * @param srcEnd   stop copying at this offset in the string buffer.
     * @param dst      the array to copy the data into.
     * @param dstBegin offset into <code>dst</code>.
     * @throws NullPointerException      if <code>dst</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if any of the following is true:
     *                                   <ul>
     *                                   <li><code>srcBegin</code> is negative
     *                                   <li><code>dstBegin</code> is negative
     *                                   <li>the <code>srcBegin</code> argument is greater than the
     *                                   <code>srcEnd</code> argument.
     *                                   <li><code>srcEnd</code> is greater than <code>this.length()</code>, the
     *                                   current length of this string buffer.
     *                                   <li><code>dstBegin+srcEnd-srcBegin</code> is greater than
     *                                   <code>dst.length</code>
     *                                   </ul>
     */
    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if ((srcEnd < 0) || (srcEnd > count)) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
        }
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    /**
     * The character at the specified index of this string buffer is set to
     * <code>ch</code>. The string buffer is altered to represent a new
     * character sequence that is identical to the old character sequence,
     * except that it contains the character <code>ch</code> at position
     * <code>index</code>.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than the length of this string buffer.
     *
     * @param index the index of the character to modify.
     * @param ch    the new character.
     * @throws IndexOutOfBoundsException if <code>index</code> is negative or
     *                                   greater than or equal to <code>length()</code>.
     * @see java.lang.StringBuilder#length()
     */
    public void setCharAt(int index, char ch) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        if (shared) {
            copy();
        }
        value[index] = ch;
    }

    /**
     * Appends the string representation of the <code>Object</code> argument to
     * this string buffer.
     * <p>
     * The argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * appended to this string buffer.
     *
     * @param obj an <code>Object</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @see java.lang.String#valueOf(java.lang.Object)
     * @see java.lang.StringBuilder#append(java.lang.String)
     */
    public StringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    /**
     * Appends the string to this string buffer.
     * <p>
     * The characters of the <code>String</code> argument are appended, in
     * order, to the contents of this string buffer, increasing the length of
     * this string buffer by the length of the argument. If <code>str</code> is
     * <code>null</code>, then the four characters <code>"null"</code> are
     * appended to this string buffer.
     * <p>
     * Let <i>n</i> be the length of the old character sequence, the one
     * contained in the string buffer just prior to execution of the
     * <code>append</code> method. Then the character at index <i>k</i> in the
     * new character sequence is equal to the character at index <i>k</i>
     * in the old character sequence, if <i>k</i> is less than <i>n</i>;
     * otherwise, it is equal to the character at index <i>k-n</i> in the
     * argument <code>str</code>.
     *
     * @param str a string.
     * @return a reference to this <code>StringBuilder</code>.
     */
//      public native  StringBuilder append(String str);
    public StringBuilder append(String str) {
        if (str == null) {
            str = String.valueOf(str);
        }

        int len = str.length();
        int newcount = count + len;
        if (newcount > value.length) {
            expandCapacity(newcount);
        }
        str.getChars(0, len, value, count);
        count = newcount;
        return this;
    }

    /**
     * Appends the string representation of the <code>char</code> array argument
     * to this string buffer.
     * <p>
     * The characters of the array argument are appended, in order, to the
     * contents of this string buffer. The length of this string buffer
     * increases by the length of the argument.
     * <p>
     * The overall effect is exactly as if the argument were converted to a
     * string by the method {@link String#valueOf(char[])} and the characters of
     * that string were then {@link #append(String) appended} to this
     * <code>StringBuilder</code> object.
     *
     * @param str the characters to be appended.
     * @return a reference to this <code>StringBuilder</code> object.
     */
    public StringBuilder append(char str[]) {
        int len = str.length;
        int newcount = count + len;
        if (newcount > value.length) {
            expandCapacity(newcount);
        }
        System.arraycopy(str, 0, value, count, len);
        count = newcount;
        return this;
    }

    /**
     * Appends the string representation of a subarray of the <code>char</code>
     * array argument to this string buffer.
     * <p>
     * Characters of the character array <code>str</code>, starting at index
     * <code>offset</code>, are appended, in order, to the contents of this
     * string buffer. The length of this string buffer increases by the value of
     * <code>len</code>.
     * <p>
     * The overall effect is exactly as if the arguments were converted to a
     * string by the method {@link String#valueOf(char[], int, int)} and the
     * characters of that string were then {@link #append(String) appended} to
     * this <code>StringBuilder</code> object.
     *
     * @param str    the characters to be appended.
     * @param offset the index of the first character to append.
     * @param len    the number of characters to append.
     * @return a reference to this <code>StringBuilder</code> object.
     */
    public StringBuilder append(char str[], int offset, int len) {
        int newcount = count + len;
        if (newcount > value.length) {
            expandCapacity(newcount);
        }
        System.arraycopy(str, offset, value, count, len);
        count = newcount;
        return this;
    }

    /**
     * Appends the string representation of the <code>boolean</code> argument to
     * the string buffer.
     * <p>
     * The argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * appended to this string buffer.
     *
     * @param b a <code>boolean</code>.
     * @return a reference to this <code>StringBuilder</code>.
     * @see java.lang.String#valueOf(boolean)
     * @see java.lang.StringBuilder#append(java.lang.String)
     */
    public StringBuilder append(boolean b) {
        return append(String.valueOf(b));
    }

    /**
     * Appends the string representation of the <code>char</code> argument to
     * this string buffer.
     * <p>
     * The argument is appended to the contents of this string buffer. The
     * length of this string buffer increases by <code>1</code>.
     * <p>
     * The overall effect is exactly as if the argument were converted to a
     * string by the method {@link String#valueOf(char)} and the character in
     * that string were then {@link #append(String) appended} to this
     * <code>StringBuilder</code> object.
     *
     * @param c a <code>char</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     */
    public StringBuilder append(char c) {
        int newcount = count + 1;
        if (newcount > value.length) {
            expandCapacity(newcount);
        }
        value[count++] = c;
        return this;
    }

    /**
     * Appends the string representation of the <code>int</code> argument to
     * this string buffer.
     * <p>
     * The argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * appended to this string buffer.
     *
     * @param i an <code>int</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @see java.lang.String#valueOf(int)
     * @see java.lang.StringBuilder#append(java.lang.String)
     */
//    public native StringBuilder append(int i);
    public StringBuilder append(int i) {
        return append(String.valueOf(i));
    }

    /**
     * Appends the string representation of the <code>long</code> argument to
     * this string buffer.
     * <p>
     * The argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * appended to this string buffer.
     *
     * @param l a <code>long</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @see java.lang.String#valueOf(long)
     * @see java.lang.StringBuilder#append(java.lang.String)
     */
    public StringBuilder append(long l) {
        return append(String.valueOf(l));
    }

    /**
     * Appends the string representation of the <code>float</code> argument to
     * this string buffer.
     * <p>
     * The argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * appended to this string buffer.
     *
     * @param f a <code>float</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @see java.lang.String#valueOf(float)
     * @see java.lang.StringBuilder#append(java.lang.String)
     * @since CLDC 1.1
     */
    public StringBuilder append(float f) {
        return append(String.valueOf(f));
    }

    /**
     * Appends the string representation of the <code>double</code> argument to
     * this string buffer.
     * <p>
     * The argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * appended to this string buffer.
     *
     * @param d a <code>double</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @see java.lang.String#valueOf(double)
     * @see java.lang.StringBuilder#append(java.lang.String)
     * @since CLDC 1.1
     */
    public StringBuilder append(double d) {
        return append(String.valueOf(d));
    }

    /**
     * Removes the characters in a substring of this <code>StringBuilder</code>.
     * The substring begins at the specified <code>start</code> and extends to
     * the character at index <code>end - 1</code> or to the end of the
     * <code>StringBuilder</code> if no such character exists. If
     * <code>start</code> is equal to <code>end</code>, no changes are made.
     *
     * @param start The beginning index, inclusive.
     * @param end   The ending index, exclusive.
     * @return This string buffer.
     * @throws StringIndexOutOfBoundsException if <code>start</code> is
     *                                         negative, greater than <code>length()</code>, or greater than
     *                                         <code>end</code>.
     * @since JDK1.2
     */
    public StringBuilder delete(int start, int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > count) {
            end = count;
        }
        if (start > end) {
            throw new StringIndexOutOfBoundsException();
        }

        int len = end - start;
        if (len > 0) {
            if (shared) {
                copy();
            }
            System.arraycopy(value, start + len, value, start, count - end);
            count -= len;
        }
        return this;
    }

    /**
     * Removes the character at the specified position in this
     * <code>StringBuilder</code> (shortening the <code>StringBuilder</code> by
     * one character).
     *
     * @param index Index of character to remove
     * @return This string buffer.
     * @throws StringIndexOutOfBoundsException if the <code>index</code> is
     *                                         negative or greater than or equal to <code>length()</code>.
     * @since JDK1.2
     */
    public StringBuilder deleteCharAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException();
        }
        if (shared) {
            copy();
        }
        System.arraycopy(value, index + 1, value, index, count - index - 1);
        count--;
        return this;
    }

    /**
     * Inserts the string representation of the <code>Object</code> argument
     * into this string buffer.
     * <p>
     * The second argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * inserted into this string buffer at the indicated offset.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param obj    an <code>Object</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.String#valueOf(java.lang.Object)
     * @see java.lang.StringBuilder#insert(int, java.lang.String)
     * @see java.lang.StringBuilder#length()
     */
    public StringBuilder insert(int offset, Object obj) {
        return insert(offset, String.valueOf(obj));
    }

    /**
     * Inserts the string into this string buffer.
     * <p>
     * The characters of the <code>String</code> argument are inserted, in
     * order, into this string buffer at the indicated offset, moving up any
     * characters originally above that position and increasing the length of
     * this string buffer by the length of the argument. If <code>str</code> is
     * <code>null</code>, then the four characters <code>"null"</code> are
     * inserted into this string buffer.
     * <p>
     * The character at index <i>k</i> in the new character sequence is equal
     * to:
     * <ul>
     * <li>the character at index <i>k</i> in the old character sequence, if
     * <i>k</i> is less than <code>offset</code>
     * <li>the character at index <i>k</i><code>-offset</code> in the argument
     * <code>str</code>, if <i>k</i> is not less than <code>offset</code> but is
     * less than <code>offset+str.length()</code>
     * <li>the character at index <i>k</i><code>-str.length()</code> in the old
     * character sequence, if <i>k</i> is not less than
     * <code>offset+str.length()</code>
     * </ul><p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param str    a string.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.StringBuilder#length()
     */
    public StringBuilder insert(int offset, String str) {
        if ((offset < 0) || (offset > count)) {
            throw new StringIndexOutOfBoundsException();
        }

        if (str == null) {
            str = String.valueOf(str);
        }
        int len = str.length();
        int newcount = count + len;
        if (newcount > value.length) {
            expandCapacity(newcount);
        } else if (shared) {
            copy();
        }
        System.arraycopy(value, offset, value, offset + len, count - offset);
        str.getChars(0, len, value, offset);
        count = newcount;
        return this;
    }

    /**
     * Inserts the string representation of the <code>char</code> array argument
     * into this string buffer.
     * <p>
     * The characters of the array argument are inserted into the contents of
     * this string buffer at the position indicated by <code>offset</code>. The
     * length of this string buffer increases by the length of the argument.
     * <p>
     * The overall effect is exactly as if the argument were converted to a
     * string by the method {@link String#valueOf(char[])} and the characters of
     * that string were then {@link #insert(int, String) inserted} into this
     * <code>StringBuilder</code> object at the position indicated by
     * <code>offset</code>.
     *
     * @param offset the offset.
     * @param str    a character array.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    public StringBuilder insert(int offset, char str[]) {
        if ((offset < 0) || (offset > count)) {
            throw new StringIndexOutOfBoundsException();
        }
        int len = str.length;
        int newcount = count + len;
        if (newcount > value.length) {
            expandCapacity(newcount);
        } else if (shared) {
            copy();
        }
        System.arraycopy(value, offset, value, offset + len, count - offset);
        System.arraycopy(str, 0, value, offset, len);
        count = newcount;
        return this;
    }

    /**
     * Inserts the string representation of the <code>boolean</code> argument
     * into this string buffer.
     * <p>
     * The second argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * inserted into this string buffer at the indicated offset.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param b      a <code>boolean</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.String#valueOf(boolean)
     * @see java.lang.StringBuilder#insert(int, java.lang.String)
     * @see java.lang.StringBuilder#length()
     */
    public StringBuilder insert(int offset, boolean b) {
        return insert(offset, String.valueOf(b));
    }

    /**
     * Inserts the string representation of the <code>char</code> argument into
     * this string buffer.
     * <p>
     * The second argument is inserted into the contents of this string buffer
     * at the position indicated by <code>offset</code>. The length of this
     * string buffer increases by one.
     * <p>
     * The overall effect is exactly as if the argument were converted to a
     * string by the method {@link String#valueOf(char)} and the character in
     * that string were then {@link #insert(int, String) inserted} into this
     * <code>StringBuilder</code> object at the position indicated by
     * <code>offset</code>.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param c      a <code>char</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws IndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.StringBuilder#length()
     */
    public StringBuilder insert(int offset, char c) {
        int newcount = count + 1;
        if (newcount > value.length) {
            expandCapacity(newcount);
        } else if (shared) {
            copy();
        }
        System.arraycopy(value, offset, value, offset + 1, count - offset);
        value[offset] = c;
        count = newcount;
        return this;
    }

    /**
     * Inserts the string representation of the second <code>int</code> argument
     * into this string buffer.
     * <p>
     * The second argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * inserted into this string buffer at the indicated offset.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param i      an <code>int</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.String#valueOf(int)
     * @see java.lang.StringBuilder#insert(int, java.lang.String)
     * @see java.lang.StringBuilder#length()
     */
    public StringBuilder insert(int offset, int i) {
        return insert(offset, String.valueOf(i));
    }

    /**
     * Inserts the string representation of the <code>long</code> argument into
     * this string buffer.
     * <p>
     * The second argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * inserted into this string buffer at the position indicated by
     * <code>offset</code>.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param l      a <code>long</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.String#valueOf(long)
     * @see java.lang.StringBuilder#insert(int, java.lang.String)
     * @see java.lang.StringBuilder#length()
     */
    public StringBuilder insert(int offset, long l) {
        return insert(offset, String.valueOf(l));
    }

    /**
     * Inserts the string representation of the <code>float</code> argument into
     * this string buffer.
     * <p>
     * The second argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * inserted into this string buffer at the indicated offset.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param f      a <code>float</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.String#valueOf(float)
     * @see java.lang.StringBuilder#insert(int, java.lang.String)
     * @see java.lang.StringBuilder#length()
     * @since CLDC 1.1
     */
    public StringBuilder insert(int offset, float f) {
        return insert(offset, String.valueOf(f));
    }

    /**
     * Inserts the string representation of the <code>double</code> argument
     * into this string buffer.
     * <p>
     * The second argument is converted to a string as if by the method
     * <code>String.valueOf</code>, and the characters of that string are then
     * inserted into this string buffer at the indicated offset.
     * <p>
     * The offset argument must be greater than or equal to <code>0</code>, and
     * less than or equal to the length of this string buffer.
     *
     * @param offset the offset.
     * @param d      a <code>double</code>.
     * @return a reference to this <code>StringBuilder</code> object.
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     * @see java.lang.String#valueOf(double)
     * @see java.lang.StringBuilder#insert(int, java.lang.String)
     * @see java.lang.StringBuilder#length()
     * @since CLDC 1.1
     */
    public StringBuilder insert(int offset, double d) {
        return insert(offset, String.valueOf(d));
    }

    /**
     * The character sequence contained in this string buffer is replaced by the
     * reverse of the sequence.
     * <p>
     * Let <i>n</i> be the length of the old character sequence, the one
     * contained in the string buffer just prior to execution of the
     * <code>reverse</code> method. Then the character at index <i>k</i> in the
     * new character sequence is equal to the character at index
     * <i>n-k-1</i> in the old character sequence.
     *
     * @return a reference to this <code>StringBuilder</code> object..
     * @since JDK1.0.2
     */
    public StringBuilder reverse() {
        if (shared) {
            copy();
        }
        int n = count - 1;
        for (int j = (n - 1) >> 1; j >= 0; --j) {
            char temp = value[j];
            value[j] = value[n - j];
            value[n - j] = temp;
        }
        return this;
    }

    /**
     * Converts to a string representing the data in this string buffer. A new
     * <code>String</code> object is allocated and initialized to contain the
     * character sequence currently represented by this string buffer. This
     * <code>String</code> is then returned. Subsequent changes to the string
     * buffer do not affect the contents of the <code>String</code>.
     * <p>
     * Implementation advice: This method can be coded so as to create a new
     * <code>String</code> object without allocating new memory to hold a copy
     * of the character sequence. Instead, the string can share the memory used
     * by the string buffer. Any subsequent operation that alters the content or
     * capacity of the string buffer must then make a copy of the internal
     * buffer at that time. This strategy is effective for reducing the amount
     * of memory allocated by a string concatenation operation when it is
     * implemented using a string buffer.
     *
     * @return a string representation of the string buffer.
     */
//    public native String toString();
    public String toString() {
        return new String(value, 0, count);
    }

    //
    // The following two methods are needed by String to efficiently
    // convert a StringBuilder into a String.  They are not public.
    // They shouldn't be called by anyone but String.
    final void setShared() {
        shared = true;
    }

    final char[] getValue() {
        return value;
    }

    public int indexOf(String str) {
        return toString().indexOf(str, 0);
    }

    public int indexOf(String str, int fromIndex) {
        return toString().indexOf(str, fromIndex);
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

    public StringBuilder append(CharSequence sequence) {
        return append(sequence.toString());
    }

    public StringBuilder append(CharSequence sequence, int start, int end) {
        return append(sequence.subSequence(start, end));
    }

    public StringBuilder appendCodePoint(int codePoint) {
        final int count = this.count;

        if (Character.isBmpCodePoint(codePoint)) {
            ensureCapacity(count + 1);
            value[count] = (char) codePoint;
            this.count = count + 1;
        } else if (Character.isValidCodePoint(codePoint)) {
            ensureCapacity(count + 2);
            Character.toSurrogates(codePoint, value, count);
            this.count = count + 2;
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }

    public int codePointAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointAtImpl(value, index, count);
    }


    public StringBuilder replace(int start, int end, String str) {
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (end > count)
            end = count;
        if (start > end)
            throw new StringIndexOutOfBoundsException("start > end");

        int len = str.length();
        int newCount = count + len - (end - start);
        if (newCount > value.length)
            expandCapacity(newCount);

        System.arraycopy(value, end, value, start + len, count - end);
        str.getChars(value, start);
        count = newCount;
        return this;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }
}
