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
 * The Float class wraps a value of primitive type <code>float</code> in an
 * object. An object of type <code>Float</code> contains a single field whose
 * type is <code>float</code>.
 * <p>
 * In addition, this class provides several methods for converting a
 * <code>float</code> to a <code>String</code> and a <code>String</code> to a
 * <code>float</code>, as well as other constants and methods useful when
 * dealing with a <code>float</code>.
 *
 * @author Lee Boynton
 * @author Arthur van Hoff
 * @version 12/17/01 (CLDC 1.1)
 * @since JDK1.0, CLDC 1.1
 */
public final class Float extends Number implements Comparable<Float> {
    //type of bytes
    public static final int   BYTES = 4;

    /**
     * The positive infinity of type <code>float</code>. It is equal to the
     * value returned by <code>Float.intBitsToFloat(0x7f800000)</code>.
     */
    public static final float POSITIVE_INFINITY = 1.0f / 0.0f;

    /**
     * The negative infinity of type <code>float</code>. It is equal to the
     * value returned by <code>Float.intBitsToFloat(0xff800000)</code>.
     */
    public static final float NEGATIVE_INFINITY = -1.0f / 0.0f;

    /**
     * The Not-a-Number (NaN) value of type <code>float</code>. It is equal to
     * the value returned by <code>Float.intBitsToFloat(0x7fc00000)</code>.
     */
    public static final float NaN = 0.0f / 0.0f;

    /**
     * The largest positive value of type <code>float</code>. It is equal to the
     * value returned by <code>Float.intBitsToFloat(0x7f7fffff)</code>.
     */
    public static final float MAX_VALUE = 3.40282346638528860e+38f;

    /**
     * The smallest positive value of type <code>float</code>. It is equal to
     * the value returned by <code>Float.intBitsToFloat(0x1)</code>.
     */
    public static final float MIN_VALUE = 1.40129846432481707e-45f;
    
    public static final int MAX_EXPONENT = 127;
    
    public static final int MIN_EXPONENT = -126;
    
    public static final Class<Float>     TYPE = (Class<Float>) Class.getPrimitiveClass("float");

    /**
     * Returns a String representation for the specified float value. The
     * argument is converted to a readable string format as follows. All
     * characters and characters in strings mentioned below are ASCII
     * characters.
     * <ul>
     * <li>If the argument is NaN, the result is the string <tt>"NaN"</tt>.
     * <li>Otherwise, the result is a string that represents the sign and
     * magnitude (absolute value) of the argument. If the sign is negative, the
     * first character of the result is <tt>'-'</tt>
     * (<tt>'\u002d'</tt>); if the sign is positive, no sign character appears
     * in the result. As for the magnitude <var>m</var>:
     * <ul>
     * <li>If <var>m</var> is infinity, it is represented by the characters
     * <tt>"Infinity"</tt>; thus, positive infinity produces the result
     * <tt>"Infinity"</tt> and negative infinity produces the result
     * <tt>"-Infinity"</tt>.
     * <li>If <var>m</var> is zero, it is represented by the characters
     * <tt>"0.0"</tt>; thus, negative zero produces the result
     * <tt>"-0.0"</tt> and positive zero produces the result
     * <tt>"0.0"</tt>.
     * <li> If <var>m</var> is greater than or equal to 10<sup>-3</sup> but less
     * than 10<sup>7</sup>, then it is represented as the integer part of
     * <var>m</var>, in decimal form with no leading zeroes, followed by
     * <tt>'.'</tt> (<tt>\u002E</tt>), followed by one or more decimal digits
     * representing the fractional part of
     * <var>m</var>.
     * <li> If <var>m</var> is less than 10<sup>-3</sup> or not less than
     * 10<sup>7</sup>, then it is represented in so-called "computerized
     * scientific notation." Let <var>n</var> be the unique integer such that
     * 10<sup>n</sup>&lt;=<var>m</var>&lt;1; then let
     * <var>a</var> be the mathematically exact quotient of <var>m</var>
     * and 10<sup>n</sup> so that 1&lt;<var>a</var>&lt10. The magnitude is then
     * represented as the integer part of <var>a</var>, as a single decimal
     * digit, followed by <tt>'.'</tt> (<tt>\u002E</tt>), followed by decimal
     * digits representing the fractional part of
     * <var>a</var>, followed by the letter <tt>'E'</tt>
     * (<tt>\u0045</tt>), followed by a representation of <var>n</var>
     * as a decimal integer, as produced by the method
     * <tt>{@link java.lang.Integer#toString(int)}</tt> of one argument.
     * </ul>
     * How many digits must be printed for the fractional part of
     * <var>m</var> or <var>a</var>? There must be at least one digit to
     * represent the fractional part, and beyond that as many, but only as many,
     * more digits as are needed to uniquely distinguish the argument value from
     * adjacent values of type float. That is, suppose that
     * <var>x</var> is the exact mathematical value represented by the decimal
     * representation produced by this method for a finite nonzero argument
     * <var>f</var>. Then <var>f</var> must be the float value nearest to
     * <var>x</var>; or, if two float values are equally close to
     * <var>x</var>then <var>f</var> must be one of them and the least
     * significant bit of the significand of <var>f</var> must be <tt>0</tt>.
     *
     * @param f the float to be converted.
     * @return a string representation of the argument.
     */
    public static String toString(float f) {
        return new FloatingDecimal(f).toJavaFormatString();
    }

    /**
     * Returns the floating point value represented by the specified String. The
     * string <code>s</code> is interpreted as the representation of a
     * floating-point value and a <code>Float</code> object representing that
     * value is created and returned.
     * <p>
     * If <code>s</code> is <code>null</code>, then a
     * <code>NullPointerException</code> is thrown.
     * <p>
     * Leading and trailing whitespace characters in s are ignored. The rest of
     * <code>s</code> should constitute a <i>FloatValue</i> as described by the
     * lexical syntax rules:
     * <blockquote><pre><i>
     * FloatValue:
     *
     *          Sign<sub>opt</sub> FloatingPointLiteral
     * </i></pre></blockquote>
     * where <i>Sign</i>, <i>FloatingPointLiteral</i> are as defined in Section
     * 3.10.2 of the
     * <a href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>. If it does not have the form of a <i>FloatValue</i>,
     * then a <code>NumberFormatException</code> is thrown. Otherwise, it is
     * regarded as representing an exact decimal value in the usual
     * "computerized scientific notation"; this exact decimal value is then
     * conceptually converted to an "infinitely precise" binary value that is
     * then rounded to type float by the usual round-to-nearest rule of IEEE 754
     * floating-point arithmetic.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>Float</code> initialized to the value
     * represented by the <code>String</code> argument.
     * @exception NumberFormatException if the string does not contain a
     * parsable number.
     */
    public static Float valueOf(String s) throws NumberFormatException {
        return new Float(FloatingDecimal.readJavaFormatString(s).floatValue());
    }

    public static Float valueOf(float i) {
        return new Float(i);
    }

    /**
     * Returns a new float initialized to the value represented by the specified
     * <code>String</code>.
     *
     * @param s the string to be parsed.
     * @return the float value represented by the string argument.
     * @exception NumberFormatException if the string does not contain a
     * parsable float.
     * @since JDK1.2
     */
    public static float parseFloat(String s) throws NumberFormatException {
        return FloatingDecimal.readJavaFormatString(s).floatValue();
    }

    /**
     * Returns true if the specified number is the special Not-a-Number (NaN)
     * value.
     *
     * @param v the value to be tested.
     * @return  <code>true</code> if the argument is NaN; <code>false</code>
     * otherwise.
     */
    static public boolean isNaN(float v) {
        return (v != v);
    }

    /**
     * Returns true if the specified number is infinitely large in magnitude.
     *
     * @param v the value to be tested.
     * @return  <code>true</code> if the argument is positive infinity or
     * negative infinity; <code>false</code> otherwise.
     */
    static public boolean isInfinite(float v) {
        return (v == POSITIVE_INFINITY) || (v == NEGATIVE_INFINITY);
    }

    /**
     * The value of the Float.
     */
    private float value;

    /**
     * Constructs a newly allocated <code>Float</code> object that represents
     * the primitive <code>float</code> argument.
     *
     * @param value the value to be represented by the <code>Float</code>.
     */
    public Float(float value) {
        this.value = value;
    }

    /**
     * Constructs a newly allocated <code>Float</code>object that represents the
     * argument converted to type <code>float</code>.
     *
     * @param value the value to be represented by the <code>Float</code>.
     */
    public Float(double value) {
        this.value = (float) value;
    }

    /**
     * Constructs a newly allocated <code>Float</code> object that represents
     * the floating-point value of type <code>float</code> represented by the
     * string. The string is converted to a <code>float</code> value as if by
     * the <code>valueOf</code> method.
     *
     * @param s a string to be converted to a <code>Float</code>.
     * @exception NumberFormatException if the string does not contain a
     * parsable number.
     * @see java.lang.Float#valueOf(java.lang.String)
     */
    /* REMOVED from CLDC
    public Float(String s) throws NumberFormatException {
        // REMIND: this is inefficient
        this(valueOf(s).floatValue());
    }
     */
    /**
     * Returns true if this <code>Float</code> value is Not-a-Number (NaN).
     *
     * @return  <code>true</code> if the value represented by this object is NaN;
     * <code>false</code> otherwise.
     */
    public boolean isNaN() {
        return isNaN(value);
    }

    /**
     * Returns true if this Float value is infinitely large in magnitude.
     *
     * @return  <code>true</code> if the value represented by this object is
     * positive infinity or negative infinity; <code>false</code> otherwise.
     */
    public boolean isInfinite() {
        return isInfinite(value);
    }

    /**
     * Returns a String representation of this Float object. The primitive
     * <code>float</code> value represented by this object is converted to a
     * <code>String</code> exactly as if by the method <code>toString</code> of
     * one argument.
     *
     * @return a <code>String</code> representation of this object.
     * @see java.lang.Float#toString(float)
     */
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Returns the value of this Float as a byte (by casting to a byte).
     *
     * @since JDK1.1
     */
    public byte byteValue() {
        return (byte) value;
    }

    /**
     * Returns the value of this Float as a short (by casting to a short).
     *
     * @since JDK1.1
     */
    public short shortValue() {
        return (short) value;
    }

    /**
     * Returns the integer value of this Float (by casting to an int).
     *
     * @return the <code>float</code> value represented by this object converted
     * to type <code>int</code> and the result of the conversion is returned.
     */
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the long value of this Float (by casting to a long).
     *
     * @return the <code>float</code> value represented by this object is
     * converted to type <code>long</code> and the result of the conversion is
     * returned.
     */
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the float value of this <tt>Float</tt> object.
     *
     * @return the <code>float</code> value represented by this object.
     */
    public float floatValue() {
        return value;
    }

    /**
     * Returns the double value of this <tt>Float</tt> object.
     *
     * @return the <code>float</code> value represented by this object is
     * converted to type <code>double</code> and the result of the conversion is
     * returned.
     */
    public double doubleValue() {
        return (double) value;
    }

    /**
     * Returns a hashcode for this <tt>Float</tt> object. The result is the
     * integer bit representation, exactly as produced by the method
     * {@link #floatToIntBits(float)}, of the primitive float value represented
     * by this <tt>Float</tt> object.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return floatToIntBits(value);
    }

    /**
     * Compares this object against some other object. The result is
     * <code>true</code> if and only if the argument is not <code>null</code>
     * and is a <code>Float</code> object that represents a <code>float</code>
     * that has the identical bit pattern to the bit pattern of the
     * <code>float</code> represented by this object. For this purpose, two
     * float values are considered to be the same if and only if the method
     * {@link #floatToIntBits(float)} returns the same int value when applied to
     * each.
     * <p>
     * Note that in most cases, for two instances of class <code>Float</code>,
     * <code>f1</code> and <code>f2</code>, the value of
     * <code>f1.equals(f2)</code> is <code>true</code> if and only if
     * <blockquote><pre>
     *   f1.floatValue() == f2.floatValue()
     * </pre></blockquote>
     * <p>
     * also has the value <code>true</code>. However, there are two exceptions:
     * <ul>
     * <li>If <code>f1</code> and <code>f2</code> both represent
     * <code>Float.NaN</code>, then the <code>equals</code> method returns
     * <code>true</code>, even though <code>Float.NaN==Float.NaN</code> has the
     * value <code>false</code>.
     * <li>If <code>f1</code> represents <code>+0.0f</code> while
     * <code>f2</code> represents <code>-0.0f</code>, or vice versa, the
     * <code>equal</code> test has the value <code>false</code>, even though
     * <code>0.0f==-0.0f</code> has the value <code>true</code>.
     * </ul>
     * This definition allows hashtables to operate properly.
     *
     * @param obj the object to be compared
     * @return  <code>true</code> if the objects are the same; <code>false</code>
     * otherwise.
     * @see java.lang.Float#floatToIntBits(float)
     */
    public boolean equals(Object obj) {
        return (obj instanceof Float)
                && (floatToIntBits(((Float) obj).value) == floatToIntBits(value));
    }

    /**
     * Returns the bit representation of a single-float value. The result is a
     * representation of the floating-point argument according to the IEEE 754
     * floating-point "single precision" bit layout.
     * <ul>
     * <li>Bit 31 (the bit that is selected by the mask <code>0x80000000</code>)
     * represents the sign of the floating-point number.
     * <li>Bits 30-23 (the bits that are selected by the mask
     * <code>0x7f800000</code>) represent the exponent.
     * <li>Bits 22-0 (the bits that are selected by the mask
     * <code>0x007fffff</code>) represent the significand (sometimes called the
     * mantissa) of the floating-point number.
     * <li>If the argument is positive infinity, the result is
     * <code>0x7f800000</code>.
     * <li>If the argument is negative infinity, the result is
     * <code>0xff800000</code>.
     * <li>If the argument is NaN, the result is <code>0x7fc00000</code>.
     * </ul>
     * In all cases, the result is an integer that, when given to the
     * {@link #intBitsToFloat(int)} method, will produce a floating-point value
     * equal to the argument to <code>floatToIntBits</code>.
     *
     * @param value a floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static native int floatToIntBits(float value);

    public static int floatToRawIntBits(float value) {
        int result = floatToIntBits(value);
        return result;
    }
    /**
     * Returns the bit representation of a single-float value. The result is a
     * representation of the floating-point argument according to the IEEE 754
     * floating-point "single precision" bit layout.
     * <ul>
     * <li>Bit 31 (the bit that is selected by the mask <code>0x80000000</code>)
     * represents the sign of the floating-point number.
     * <li>Bits 30-23 (the bits that are selected by the mask
     * <code>0x7f800000</code>) represent the exponent.
     * <li>Bits 22-0 (the bits that are selected by the mask
     * <code>0x007fffff</code>) represent the significand (sometimes called the
     * mantissa) of the floating-point number.
     * <li>If the argument is positive infinity, the result is
     * <code>0x7f800000</code>.
     * <li>If the argument is negative infinity, the result is
     * <code>0xff800000</code>.
     * <p>
     * If the argument is NaN, the result is the integer representing the actual
     * NaN value. Unlike the <code>floatToIntBits</code> method,
     * <code>intToRawIntBits</code> does not collapse NaN values.
     * </ul>
     * In all cases, the result is an integer that, when given to the
     * {@link #intBitsToFloat(int)} method, will produce a floating-point value
     * equal to the argument to <code>floatToRawIntBits</code>.
     *
     * @param value a floating-point number.
     * @return the bits that represent the floating-point number.
     */
    /* REMOVED from CLDC
    public static native int floatToRawIntBits(float value);
     */
    /**
     * Returns the single-float corresponding to a given bit representation. The
     * argument is considered to be a representation of a floating-point value
     * according to the IEEE 754 floating-point "single precision" bit layout.
     * <p>
     * If the argument is <code>0x7f800000</code>, the result is positive
     * infinity.
     * <p>
     * If the argument is <code>0xff800000</code>, the result is negative
     * infinity.
     * <p>
     * If the argument is any value in the range <code>0x7f800001</code> through
     * <code>0x7fffffff</code> or in the range <code>0xff800001</code> through
     * <code>0xffffffff</code>, the result is NaN. All IEEE 754 NaN values of
     * type <code>float</code> are, in effect, lumped together by the Java
     * programming language into a single <code>float</code> value called NaN.
     * <p>
     * In all other cases, let <i>s</i>, <i>e</i>, and <i>m</i> be three values
     * that can be computed from the argument:
     * <blockquote><pre>
     * int s = ((bits >> 31) == 0) ? 1 : -1;
     * int e = ((bits >> 23) & 0xff);
     * int m = (e == 0) ?
     *                 (bits & 0x7fffff) << 1 :
     *                 (bits & 0x7fffff) | 0x800000;
     * </pre></blockquote> Then the floating-point result equals the value of
     * the mathematical expression <i>s&#183;m&#183;2<sup>e-150</sup></i>.
     *
     * @param bits an integer.
     * @return the single-format floating-point value with the same bit pattern.
     */
    public static native float intBitsToFloat(int bits);

    /**
     * Compares two Floats numerically. There are two ways in which comparisons
     * performed by this method differ from those performed by the Java language
     * numerical comparison operators (<code>&lt;, &lt;=,
     * ==, &gt;= &gt;</code>) when applied to primitive floats:
     * <ul><li>
     * <code>Float.NaN</code> is considered by this method to be equal to itself
     * and greater than all other float values (including
     * <code>Float.POSITIVE_INFINITY</code>).
     * <li>
     * <code>0.0f</code> is considered by this method to be greater than
     * <code>-0.0f</code>.
     * </ul>
     * This ensures that Float.compareTo(Object) (which inherits its behavior
     * from this method) obeys the general contract for Comparable.compareTo,
     * and that the <i>natural order</i> on Floats is <i>total</i>.
     *
     * @param anotherFloat the <code>Float</code> to be compared.
     * @return the value <code>0</code> if <code>anotherFloat</code> is
     * numerically equal to this Float; a value less than <code>0</code> if this
     * Float is numerically less than <code>anotherFloat</code>; and a value
     * greater than <code>0</code> if this Float is numerically greater than
     * <code>anotherFloat</code>.
     *
     * @since JDK1.2
     * @see Comparable#compareTo(Object)
     */
    /* REMOVED from CLDC
    public int compareTo(Float anotherFloat) {
        float thisVal = value;
        float anotherVal = anotherFloat.value;

        if (thisVal < anotherVal)
            return -1;       // Neither val is NaN, thisVal is smaller
        if (thisVal > anotherVal)
            return 1;        // Neither val is NaN, thisVal is larger

        int thisBits = Float.floatToIntBits(thisVal);
        int anotherBits = Float.floatToIntBits(anotherVal);

        return (thisBits == anotherBits ?  0 : // Values are equal
                (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                 1));                          // (0.0, -0.0) or (NaN, !NaN)
    }
     */
    /**
     * Compares this Float to another Object. If the Object is a Float, this
     * function behaves like <code>compareTo(Float)</code>. Otherwise, it throws
     * a <code>ClassCastException</code> (as Floats are comparable only to other
     * Floats).
     *
     * @param anotherFloat the <code>Object</code> to be compared.
     * @return the value <code>0</code> if the argument is a Float numerically
     * equal to this Float; a value less than <code>0</code> if the argument is
     * a Float numerically greater than this Float; and a value greater than
     * <code>0</code> if the argument is a Float numerically less than this
     * Float.
     * @exception <code>ClassCastException</code> if the argument is not a
     * <code>Float</code>.
     * @see java.lang.Comparable
     * @since 1.2
     */
    /* REMOVED from CLDC
    public int compareTo(Object o) {
        return compareTo((Float)o);
    }
     */
    public int compareTo(Float anotherFloat) {
        return Float.compare(value, anotherFloat.value);
    }

    /**
     * Compares the two specified <code>float</code> values. The sign
     * of the integer value returned is the same as that of the
     * integer that would be returned by the call:
     * <pre>
     *    new Float(f1).compareTo(new Float(f2))
     * </pre>
     *
     * @param   f1        the first <code>float</code> to compare.
     * @param   f2        the second <code>float</code> to compare.
     * @return  the value <code>0</code> if <code>f1</code> is
     *		numerically equal to <code>f2</code>; a value less than
     *          <code>0</code> if <code>f1</code> is numerically less than
     *		<code>f2</code>; and a value greater than <code>0</code>
     *		if <code>f1</code> is numerically greater than
     *		<code>f2</code>.
     * @since 1.4 
     */
    public static int compare(float f1, float f2) {
       if (f1 < f2)
            return -1;		 // Neither val is NaN, thisVal is smaller
        if (f1 > f2)
            return 1;		 // Neither val is NaN, thisVal is larger

        int thisBits = Float.floatToIntBits(f1);
        int anotherBits = Float.floatToIntBits(f2);

        return (thisBits == anotherBits ?  0 : // Values are equal
                (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                 1));                          // (0.0, -0.0) or (NaN, !NaN)
    }

}
