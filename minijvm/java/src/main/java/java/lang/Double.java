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
 * The Double class wraps a value of the primitive type <code>double</code> in
 * an object. An object of type <code>Double</code> contains a single field
 * whose type is <code>double</code>.
 * <p>
 * In addition, this class provides several methods for converting a
 * <code>double</code> to a <code>String</code> and a <code>String</code> to a
 * <code>double</code>, as well as other constants and methods useful when
 * dealing with a <code>double</code>.
 *
 * @author Lee Boynton
 * @author Arthur van Hoff
 * @version 12/17/01 (CLDC 1.1)
 * @since JDK1.0, CLDC 1.1
 */
public final class Double  extends Number implements Comparable<Double>{
    //type of bytes
    public static final int   BYTES = 8;

    /**
     * The positive infinity of type <code>double</code>. It is equal to the
     * value returned by
     * <code>Double.longBitsToDouble(0x7ff0000000000000L)</code>.
     */
    public static final double POSITIVE_INFINITY = 1.0 / 0.0;

    /**
     * The negative infinity of type <code>double</code>. It is equal to the
     * value returned by
     * <code>Double.longBitsToDouble(0xfff0000000000000L)</code>.
     */
    public static final double NEGATIVE_INFINITY = -1.0 / 0.0;

    /**
     * A Not-a-Number (NaN) value of type <code>double</code>. It is equal to
     * the value returned by
     * <code>Double.longBitsToDouble(0x7ff8000000000000L)</code>.
     */
    public static final double NaN = 0.0d / 0.0;

    /**
     * The largest positive finite value of type <code>double</code>. It is
     * equal to the value returned by
     * <blockquote><pre>
     * <code>Double.longBitsToDouble(0x7fefffffffffffffL)</code>
     * </pre></blockquote>
     */
    public static final double MAX_VALUE = 1.79769313486231570e+308;
    
    public static final Class<Double>     TYPE = (Class<Double>) Class.getPrimitiveClass("double");

    /**
     * The smallest positive value of type <code>double</code>. It is equal to
     * the value returned by <code>Double.longBitsToDouble(0x1L)</code>.
     */
//  public static final double MIN_VALUE = 4.94065645841246544e-324;
    public static final double MIN_VALUE = longBitsToDouble(1L);
    
    public static final int MAX_EXPONENT = 1023;
    public static final int MIN_EXPONENT = -1022;

    /**
     * Creates a string representation of the <code>double</code> argument. All
     * characters mentioned below are ASCII characters.
     * <ul>
     * <li>If the argument is NaN, the result is the string "NaN".
     * <li>Otherwise, the result is a string that represents the sign and
     * magnitude (absolute value) of the argument. If the sign is negative, the
     * first character of the result is '<code>-</code>'
     * ('<code>\u002d</code>'); if the sign is positive, no sign character
     * appears in the result. As for the magnitude <i>m</i>:
     * <li>If <i>m</i> is infinity, it is represented by the characters
     * <code>"Infinity"</code>; thus, positive infinity produces the result
     * <code>"Infinity"</code> and negative infinity produces the result
     * <code>"-Infinity"</code>.
     * <li>If <i>m</i> is zero, it is represented by the characters
     * <code>"0.0"</code>; thus, negative zero produces the result
     * <code>"-0.0"</code> and positive zero produces the result
     * <code>"0.0"</code>.
     * <li>If <i>m</i> is greater than or equal to 10<sup>-3</sup> but less than
     * 10<sup>7</sup>, then it is represented as the integer part of
     * <i>m</i>, in decimal form with no leading zeroes, followed by
     * <code>'.'</code> (<code>\u002E</code>), followed by one or more decimal
     * digits representing the fractional part of <i>m</i>.
     * <li>If <i>m</i> is less than 10<sup>-3</sup> or not less than
     * 10<sup>7</sup>, then it is represented in so-called "computerized
     * scientific notation." Let <i>n</i> be the unique integer such that
     * 10<sup>n</sup>&lt;=<i>m</i>&lt;10<sup>n+1</sup>; then let <i>a</i> be the
     * mathematically exact quotient of <i>m</i> and 10<sup>n</sup> so that
     * 1&lt;=<i>a</i>&lt;10. The magnitude is then represented as the integer
     * part of <i>a</i>, as a single decimal digit, followed by <code>'.'</code>
     * (<code>\u002E</code>), followed by decimal digits representing the
     * fractional part of <i>a</i>, followed by the letter <code>'E'</code>
     * (<code>\u0045</code>), followed by a representation of <i>n</i> as a
     * decimal integer, as produced by the method {@link Integer#toString(int)}.
     * </ul><p>
     * How many digits must be printed for the fractional part of
     * <i>m</i> or <i>a</i>? There must be at least one digit to represent the
     * fractional part, and beyond that as many, but only as many, more digits
     * as are needed to uniquely distinguish the argument value from adjacent
     * values of type <code>double</code>. That is, suppose that
     * <i>x</i> is the exact mathematical value represented by the decimal
     * representation produced by this method for a finite nonzero argument
     * <i>d</i>. Then <i>d</i> must be the <code>double</code> value nearest to
     * <i>x</i>; or if two <code>double</code> values are equally close to
     * <i>x</i>, then <i>d</i> must be one of them and the least significant bit
     * of the significand of <i>d</i> must be <code>0</code>.
     *
     * @param d the <code>double</code> to be converted.
     * @return a string representation of the argument.
     */
    public static String toString(double d) {
        return new FloatingDecimal(d).toJavaFormatString();
    }

    /**
     * Returns a new <code>Double</code> object initialized to the value
     * represented by the specified string. The string <code>s</code> is
     * interpreted as the representation of a floating-point value and a
     * <code>Double</code> object representing that value is created and
     * returned.
     * <p>
     * If <code>s</code> is <code>null</code>, then a
     * <code>NullPointerException</code> is thrown.
     * <p>
     * Leading and trailing whitespace characters in s are ignored. The rest of
     * <code>s</code> should constitute a <i>FloatValue</i> as described by the
     * lexical rule:
     * <blockquote><pre><i>
     * FloatValue:
     *
     *        Sign<sub>opt</sub> FloatingPointLiteral
     * </i></pre></blockquote>
     * where <i>Sign</i> and <i>FloatingPointLiteral</i> are as defined in
     * Section 3.10.2 of the
     * <a href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>. If it does not have the form of a
     * <i>FloatValue</i>, then a <code>NumberFormatException</code> is thrown.
     * Otherwise, it is regarded as representing an exact decimal value in the
     * usual "computerized scientific notation"; this exact decimal value is
     * then conceptually converted to an "infinitely precise" binary value that
     * is then rounded to type <code>double</code> by the usual round-to-nearest
     * rule of IEEE 754 floating-point arithmetic. Finally, a new object of
     * class <code>Double</code> is created to represent the <code>double</code>
     * value.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>Double</code> initialized to the value
     * represented by the string argument.
     * @exception NumberFormatException if the string does not contain a
     * parsable number.
     */
    public static Double valueOf(String s) throws NumberFormatException {
        return new Double(FloatingDecimal.readJavaFormatString(s).doubleValue());
    }

    public static Double valueOf(double i) {
        return new Double(i);
    }

    /**
     * Returns a new double initialized to the value represented by the
     * specified <code>String</code>, as performed by the <code>valueOf</code>
     * method of class <code>Double</code>.
     *
     * @param s the string to be parsed.
     * @return the double value represented by the string argument.
     * @exception NumberFormatException if the string does not contain a
     * parsable double.
     * @see java.lang.Double#valueOf(String)
     * @since JDK1.2
     */
    public static double parseDouble(String s) throws NumberFormatException {
        return FloatingDecimal.readJavaFormatString(s).doubleValue();
    }

    /**
     * Returns true if the specified number is the special Not-a-Number (NaN)
     * value.
     *
     * @param v the value to be tested.
     * @return  <code>true</code> if the value of the argument is NaN;
     * <code>false</code> otherwise.
     */
    static public boolean isNaN(double v) {
        return (v != v);
    }

    /**
     * Returns true if the specified number is infinitely large in magnitude.
     *
     * @param v the value to be tested.
     * @return  <code>true</code> if the value of the argument is positive
     * infinity or negative infinity; <code>false</code> otherwise.
     */
    static public boolean isInfinite(double v) {
        return (v == POSITIVE_INFINITY) || (v == NEGATIVE_INFINITY);
    }

    /**
     * The value of the Double.
     */
    private double value;

    /**
     * Constructs a newly allocated <code>Double</code> object that represents
     * the primitive <code>double</code> argument.
     *
     * @param value the value to be represented by the <code>Double</code>.
     */
    public Double(double value) {
        this.value = value;
    }

    /**
     * Constructs a newly allocated <code>Double</code> object that represents
     * the floating-point value of type <code>double</code> represented by the
     * string. The string is converted to a <code>double</code> value as if by
     * the <code>valueOf</code> method.
     *
     * @param s a string to be converted to a <code>Double</code>.
     * @exception NumberFormatException if the string does not contain a
     * parsable number.
     * @see java.lang.Double#valueOf(java.lang.String)
     */
    /* REMOVED from CLDC
    public Double(String s) throws NumberFormatException {
        // REMIND: this is inefficient
        this(valueOf(s).doubleValue());
    }
     */
    /**
     * Returns true if this Double value is the special Not-a-Number (NaN)
     * value.
     *
     * @return  <code>true</code> if the value represented by this object is NaN;
     * <code>false</code> otherwise.
     */
    public boolean isNaN() {
        return isNaN(value);
    }

    /**
     * Returns true if this Double value is infinitely large in magnitude.
     *
     * @return  <code>true</code> if the value represented by this object is
     * positive infinity or negative infinity; <code>false</code> otherwise.
     */
    public boolean isInfinite() {
        return isInfinite(value);
    }

    /**
     * Returns a String representation of this Double object. The primitive
     * <code>double</code> value represented by this object is converted to a
     * string exactly as if by the method <code>toString</code> of one argument.
     *
     * @return a <code>String</code> representation of this object.
     * @see java.lang.Double#toString(double)
     */
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Returns the value of this Double as a byte (by casting to a byte).
     *
     * @since JDK1.1
     */
    public byte byteValue() {
        return (byte) value;
    }

    /**
     * Returns the value of this Double as a short (by casting to a short).
     *
     * @since JDK1.1
     */
    public short shortValue() {
        return (short) value;
    }

    /**
     * Returns the integer value of this Double (by casting to an int).
     *
     * @return the <code>double</code> value represented by this object is
     * converted to type <code>int</code> and the result of the conversion is
     * returned.
     */
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the long value of this Double (by casting to a long).
     *
     * @return the <code>double</code> value represented by this object is
     * converted to type <code>long</code> and the result of the conversion is
     * returned.
     */
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the float value of this Double.
     *
     * @return the <code>double</code> value represented by this object is
     * converted to type <code>float</code> and the result of the conversion is
     * returned.
     * @since JDK1.0
     */
    public float floatValue() {
        return (float) value;
    }

    /**
     * Returns the double value of this Double.
     *
     * @return the <code>double</code> value represented by this object.
     */
    public double doubleValue() {
        return (double) value;
    }

    /**
     * Returns a hashcode for this <code>Double</code> object. The result is the
     * exclusive OR of the two halves of the long integer bit representation,
     * exactly as produced by the method {@link #doubleToLongBits(double)}, of
     * the primitive <code>double</code> value represented by this
     * <code>Double</code> object. That is, the hashcode is the value of the
     * expression:
     * <blockquote><pre>
     * (int)(v^(v>>>32))
     * </pre></blockquote>
     * where <code>v</code> is defined by:
     * <blockquote><pre>
     * long v = Double.doubleToLongBits(this.doubleValue());
     * </pre></blockquote>
     *
     * @return a <code>hash code</code> value for this object.
     */
    public int hashCode() {
        return hashCode(value);
    }

    public static int hashCode(double v) {
        long bits = doubleToLongBits(v);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * Adds two {@code double} values together as per the + operator.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the sum of {@code a} and {@code b}
     * @jls 4.2.4 Floating-Point Operations
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static double sum(double a, double b) {
        return a + b;
    }

    /**
     * Compares this object against the specified object. The result is
     * <code>true</code> if and only if the argument is not <code>null</code>
     * and is a <code>Double</code> object that represents a double that has the
     * identical bit pattern to the bit pattern of the double represented by
     * this object. For this purpose, two <code>double</code> values are
     * considered to be the same if and only if the method
     * {@link #doubleToLongBits(double)} returns the same long value when
     * applied to each.
     * <p>
     * Note that in most cases, for two instances of class <code>Double</code>,
     * <code>d1</code> and <code>d2</code>, the value of
     * <code>d1.equals(d2)</code> is <code>true</code> if and only if
     * <blockquote><pre>
     *   d1.doubleValue()&nbsp;== d2.doubleValue()
     * </pre></blockquote>
     * <p>
     * also has the value <code>true</code>. However, there are two exceptions:
     * <ul>
     * <li>If <code>d1</code> and <code>d2</code> both represent
     * <code>Double.NaN</code>, then the <code>equals</code> method returns
     * <code>true</code>, even though <code>Double.NaN==Double.NaN</code> has
     * the value <code>false</code>.
     * <li>If <code>d1</code> represents <code>+0.0</code> while <code>d2</code>
     * represents <code>-0.0</code>, or vice versa, the <code>equals</code> test
     * has the value <code>false</code>, even though <code>+0.0==-0.0</code> has
     * the value <code>true</code>. This allows hashtables to operate properly.
     * </ul>
     *
     * @param obj the object to compare with.
     * @return  <code>true</code> if the objects are the same; <code>false</code>
     * otherwise.
     */
    public boolean equals(Object obj) {
        return (obj instanceof Double)
                && (doubleToLongBits(((Double) obj).value)
                == doubleToLongBits(value));
    }

    /**
     * Returns a representation of the specified floating-point value according
     * to the IEEE 754 floating-point "double format" bit layout.
     * <p>
     * Bit 63 (the bit that is selected by the mask
     * <code>0x8000000000000000L</code>) represents the sign of the
     * floating-point number. Bits 62-52 (the bits that are selected by the mask
     * <code>0x7ff0000000000000L</code>) represent the exponent. Bits 51-0 (the
     * bits that are selected by the mask <code>0x000fffffffffffffL</code>)
     * represent the significand (sometimes called the mantissa) of the
     * floating-point number.
     * <p>
     * If the argument is positive infinity, the result is
     * <code>0x7ff0000000000000L</code>.
     * <p>
     * If the argument is negative infinity, the result is
     * <code>0xfff0000000000000L</code>.
     * <p>
     * If the argument is NaN, the result is <code>0x7ff8000000000000L</code>.
     * <p>
     * In all cases, the result is a <code>long</code> integer that, when given
     * to the {@link #longBitsToDouble(long)} method, will produce a
     * floating-point value equal to the argument to
     * <code>doubleToLongBits</code>.
     *
     * @param value a double precision floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static native long doubleToLongBits(double value);

    /**
     * Returns a representation of the specified floating-point value according
     * to the IEEE 754 floating-point "double format" bit layout.
     * <p>
     * Bit 63 (the bit that is selected by the mask
     * <code>0x8000000000000000L</code>) represents the sign of the
     * floating-point number. Bits 62-52 (the bits that are selected by the mask
     * <code>0x7ff0000000000000L</code>) represent the exponent. Bits 51-0 (the
     * bits that are selected by the mask <code>0x000fffffffffffffL</code>)
     * represent the significand (sometimes called the mantissa) of the
     * floating-point number.
     * <p>
     * If the argument is positive infinity, the result is
     * <code>0x7ff0000000000000L</code>.
     * <p>
     * If the argument is negative infinity, the result is
     * <code>0xfff0000000000000L</code>.
     * <p>
     * If the argument is NaN, the result is the <code>long</code> integer
     * representing the actual NaN value. Unlike the
     * <code>doubleToLongBits</code> method, <code>doubleToRawLongBits</code>
     * does not collapse NaN values.
     * <p>
     * In all cases, the result is a <code>long</code> integer that, when given
     * to the {@link #longBitsToDouble(long)} method, will produce a
     * floating-point value equal to the argument to
     * <code>doubleToRawLongBits</code>.
     *
     * @param value a double precision floating-point number.
     * @return the bits that represent the floating-point number.
     */
    /* REMOVED from CLDC
    public static native long doubleToRawLongBits(double value);
     */
    /**
     * Returns the double-float corresponding to a given bit representation. The
     * argument is considered to be a representation of a floating-point value
     * according to the IEEE 754 floating-point "double precision" bit layout.
     * That floating-point value is returned as the result.
     * <p>
     * If the argument is <code>0x7ff0000000000000L</code>, the result is
     * positive infinity.
     * <p>
     * If the argument is <code>0xfff0000000000000L</code>, the result is
     * negative infinity.
     * <p>
     * If the argument is any value in the range
     * <code>0x7ff0000000000001L</code> through <code>0x7fffffffffffffffL</code>
     * or in the range <code>0xfff0000000000001L</code> through
     * <code>0xffffffffffffffffL</code>, the result is NaN. All IEEE 754 NaN
     * values of type <code>double</code> are, in effect, lumped together by the
     * Java programming language into a single value called NaN.
     * <p>
     * In all other cases, let <i>s</i>, <i>e</i>, and <i>m</i> be three values
     * that can be computed from the argument:
     * <blockquote><pre>
     * int s = ((bits >> 63) == 0) ? 1 : -1;
     * int e = (int)((bits >> 52) & 0x7ffL);
     * long m = (e == 0) ?
     *                 (bits & 0xfffffffffffffL) << 1 :
     *                 (bits & 0xfffffffffffffL) | 0x10000000000000L;
     * </pre></blockquote> Then the floating-point result equals the value of
     * the mathematical expression
     * <i>s</i>&#183;<i>m</i>&#183;2<sup>e-1075</sup>.
     *
     * @param bits any <code>long</code> integer.
     * @return the <code>double</code> floating-point value with the same bit
     * pattern.
     */
    public static native double longBitsToDouble(long bits);

    /**
     * Compares two Doubles numerically. There are two ways in which comparisons
     * performed by this method differ from those performed by the Java language
     * numerical comparison operators (<code>&lt;, &lt;=,
     * ==, &gt;= &gt;</code>) when applied to primitive doubles:
     * <ul><li>
     * <code>Double.NaN</code> is considered by this method to be equal to
     * itself and greater than all other double values (including
     * <code>Double.POSITIVE_INFINITY</code>).
     * <li>
     * <code>0.0d</code> is considered by this method to be greater than
     * <code>-0.0d</code>.
     * </ul>
     * This ensures that Double.compareTo(Object) (which inherits its behavior
     * from this method) obeys the general contract for Comparable.compareTo,
     * and that the <i>natural order</i> on Doubles is <i>total</i>.
     *
     * @param anotherDouble the <code>Double</code> to be compared.
     * @return the value <code>0</code> if <code>anotherDouble</code> is
     * numerically equal to this Double; a value less than <code>0</code> if
     * this Double is numerically less than <code>anotherDouble</code>; and a
     * value greater than <code>0</code> if this Double is numerically greater
     * than <code>anotherDouble</code>.
     *
     * @since JDK1.2
     * @see Comparable#compareTo(Object)
     */
    /* REMOVED from CLDC
    public int compareTo(Double anotherDouble) {
        double thisVal = value;
        double anotherVal = anotherDouble.value;

        if (thisVal < anotherVal)
            return -1;       // Neither val is NaN, thisVal is smaller
        if (thisVal > anotherVal)
            return 1;        // Neither val is NaN, thisVal is larger

        long thisBits = Double.doubleToLongBits(thisVal);
        long anotherBits = Double.doubleToLongBits(anotherVal);

        return (thisBits == anotherBits ?  0 : // Values are equal
                (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                 1));                          // (0.0, -0.0) or (NaN, !NaN)
    }
     */
    /**
     * Compares this Double to another Object. If the Object is a Double, this
     * function behaves like <code>compareTo(Double)</code>. Otherwise, it
     * throws a <code>ClassCastException</code> (as Doubles are comparable only
     * to other Doubles).
     *
     * @param o the <code>Object</code> to be compared.
     * @return the value <code>0</code> if the argument is a Double numerically
     * equal to this Double; a value less than <code>0</code> if the argument is
     * a Double numerically greater than this Double; and a value greater than
     * <code>0</code> if the argument is a Double numerically less than this
     * Double.
     * @exception <code>ClassCastException</code> if the argument is not a
     * <code>Double</code>.
     * @see java.lang.Comparable
     * @since JDK1.2
     */
    /* REMOVED from CLDC*/
    public int compareTo(Double anotherDouble) {
        return Double.compare(value, anotherDouble.value);
    }

    /**
     * Compares the two specified <code>double</code> values. The sign
     * of the integer value returned is the same as that of the
     * integer that would be returned by the call:
     * <pre>
     *    new Double(d1).compareTo(new Double(d2))
     * </pre>
     *
     * @param   d1        the first <code>double</code> to compare
     * @param   d2        the second <code>double</code> to compare
     * @return  the value <code>0</code> if <code>d1</code> is
     *		numerically equal to <code>d2</code>; a value less than
     *          <code>0</code> if <code>d1</code> is numerically less than
     *		<code>d2</code>; and a value greater than <code>0</code>
     *		if <code>d1</code> is numerically greater than
     *		<code>d2</code>.
     * @since 1.4
     */
    public static int compare(double d1, double d2) {
        if (d1 < d2)
            return -1;		 // Neither val is NaN, thisVal is smaller
        if (d1 > d2)
            return 1;		 // Neither val is NaN, thisVal is larger

        long thisBits = Double.doubleToLongBits(d1);
        long anotherBits = Double.doubleToLongBits(d2);

        return (thisBits == anotherBits ?  0 : // Values are equal
                (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                 1));                          // (0.0, -0.0) or (NaN, !NaN)
    }
     
}
