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
 * The class <code>Math</code> contains methods for performing basic numeric
 * operations.
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @since JDK1.0, CLDC 1.0
 */
public final strictfp class Math {

    /**
     * Don't let anyone instantiate this class.
     */
    private Math() {
    }

    /**
     * The <code>double</code> value that is closer than any other to
     * <code>e</code>, the base of the natural logarithms.
     *
     * @since CLDC 1.1
     */
    public static final double E = 2.7182818284590452354;

    /**
     * The <code>double</code> value that is closer than any other to
     * <i>pi</i>, the ratio of the circumference of a circle to its diameter.
     *
     * @since CLDC 1.1
     */
    public static final double PI = 3.14159265358979323846;

    /**
     * Returns the trigonometric sine of an angle. Special cases:
     * <ul><li>If the argument is NaN or an infinity, then the result is NaN.
     * <li>If the argument is positive zero, then the result is positive zero;
     * if the argument is negative zero, then the result is negative zero.</ul>
     *
     * @param a an angle, in radians.
     * @return the sine of the argument.
     * @since CLDC 1.1
     */
    public static native double sin(double a);

    /**
     * Returns the trigonometric cosine of an angle. Special case:
     * <ul><li>If the argument is NaN or an infinity, then the result is
     * NaN.</ul>
     *
     * @param a an angle, in radians.
     * @return the cosine of the argument.
     * @since CLDC 1.1
     */
    public static native double cos(double a);

    /**
     * Returns the trigonometric tangent of an angle. Special cases:
     * <ul><li>If the argument is NaN or an infinity, then the result is NaN.
     * <li>If the argument is positive zero, then the result is positive zero;
     * if the argument is negative zero, then the result is negative zero</ul>
     *
     * @param a an angle, in radians.
     * @return the tangent of the argument.
     * @since CLDC 1.1
     */
    public static native double tan(double a);

    /**
     * Converts an angle measured in degrees to the equivalent angle measured in
     * radians.
     *
     * @param angdeg an angle, in degrees
     * @return the measurement of the angle <code>angdeg</code> in radians.
     * @since CLDC 1.1
     */
    public static double toRadians(double angdeg) {
        return angdeg / 180.0 * PI;
    }

    /**
     * Converts an angle measured in radians to the equivalent angle measured in
     * degrees.
     *
     * @param angrad an angle, in radians
     * @return the measurement of the angle <code>angrad</code> in degrees.
     * @since CLDC 1.1
     */
    public static double toDegrees(double angrad) {
        return angrad * 180.0 / PI;
    }

    /**
     * Returns the correctly rounded positive square root of a
     * <code>double</code> value. Special cases:
     * <ul><li>If the argument is NaN or less than zero, then the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive
     * infinity.
     * <li>If the argument is positive zero or negative zero, then the result is
     * the same as the argument.</ul>
     *
     * @param a a <code>double</code> value.
     * @return the positive square root of <code>a</code>. If the argument is
     * NaN or less than zero, the result is NaN.
     * @since CLDC 1.1
     */
    public static native double sqrt(double a);

    /**
     * Returns the smallest (closest to negative infinity) <code>double</code>
     * value that is not less than the argument and is equal to a mathematical
     * integer. Special cases:
     * <ul><li>If the argument value is already equal to a mathematical integer,
     * then the result is the same as the argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative
     * zero, then the result is the same as the argument.
     * <li>If the argument value is less than zero but greater than -1.0, then
     * the result is negative zero.</ul>
     * Note that the value of <code>Math.ceil(x)</code> is exactly the value of
     * <code>-Math.floor(-x)</code>.
     *
     * @param a a <code>double</code> value. <!--@return the value
     *          &lceil;&nbsp;<code>a</code>&nbsp;&rceil;.-->
     * @return the smallest (closest to negative infinity) <code>double</code>
     * value that is not less than the argument and is equal to a mathematical
     * integer.
     * @since CLDC 1.1
     */
    public static native double ceil(double a);

    public static native double exp(double a);

    /**
     * Returns the largest (closest to positive infinity) <code>double</code>
     * value that is not greater than the argument and is equal to a
     * mathematical integer. Special cases:
     * <ul><li>If the argument value is already equal to a mathematical integer,
     * then the result is the same as the argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative
     * zero, then the result is the same as the argument.</ul>
     *
     * @param a a <code>double</code> value. <!--@return the value
     *          &lfloor;&nbsp;<code>a</code>&nbsp;&rfloor;.-->
     * @return the largest (closest to positive infinity) <code>double</code>
     * value that is not greater than the argument and is equal to a
     * mathematical integer.
     * @since CLDC 1.1
     */
    public static native double floor(double a);

    /**
     * Returns the absolute value of an <code>int</code> value. If the argument
     * is not negative, the argument is returned. If the argument is negative,
     * the negation of the argument is returned.
     * <p>
     * Note that if the argument is equal to the value of
     * <code>Integer.MIN_VALUE</code>, the most negative representable
     * <code>int</code> value, the result is that same value, which is negative.
     *
     * @param a an <code>int</code> value.
     * @return the absolute value of the argument.
     * @see java.lang.Integer#MIN_VALUE
     */
    public static int abs(int a) {
        return (a < 0) ? -a : a;
    }

    /**
     * Returns the absolute value of a <code>long</code> value. If the argument
     * is not negative, the argument is returned. If the argument is negative,
     * the negation of the argument is returned.
     * <p>
     * Note that if the argument is equal to the value of
     * <code>Long.MIN_VALUE</code>, the most negative representable
     * <code>long</code> value, the result is that same value, which is
     * negative.
     *
     * @param a a <code>long</code> value.
     * @return the absolute value of the argument.
     * @see java.lang.Long#MIN_VALUE
     */
    public static long abs(long a) {
        return (a < 0) ? -a : a;
    }

    /**
     * Returns the absolute value of a <code>float</code> value. If the argument
     * is not negative, the argument is returned. If the argument is negative,
     * the negation of the argument is returned. Special cases:
     * <ul><li>If the argument is positive zero or negative zero, the result is
     * positive zero.
     * <li>If the argument is infinite, the result is positive infinity.
     * <li>If the argument is NaN, the result is NaN.</ul>
     */
    public static float abs(float a) {
        return Float.intBitsToFloat(0x7fffffff & Float.floatToIntBits(a));
    }

    /**
     * Returns the absolute value of a <code>double</code> value. If the
     * argument is not negative, the argument is returned. If the argument is
     * negative, the negation of the argument is returned. Special cases:
     * <ul><li>If the argument is positive zero or negative zero, the result is
     * positive zero.
     * <li>If the argument is infinite, the result is positive infinity.
     * <li>If the argument is NaN, the result is NaN.</ul>
     */
    public static double abs(double a) {
        return Double.longBitsToDouble(0x7fffffffffffffffL & Double.doubleToLongBits(a));
    }

    /**
     * Returns the greater of two <code>int</code> values. That is, the result
     * is the argument closer to the value of <code>Integer.MAX_VALUE</code>. If
     * the arguments have the same value, the result is that same value.
     *
     * @param a an <code>int</code> value.
     * @param b an <code>int</code> value.
     * @return the larger of <code>a</code> and <code>b</code>.
     * @see java.lang.Long#MAX_VALUE
     */
    public static int max(int a, int b) {
        return (a >= b) ? a : b;
    }

    /**
     * Returns the greater of two <code>long</code> values. That is, the result
     * is the argument closer to the value of <code>Long.MAX_VALUE</code>. If
     * the arguments have the same value, the result is that same value.
     *
     * @param a a <code>long</code> value.
     * @param b a <code>long</code> value.
     * @return the larger of <code>a</code> and <code>b</code>.
     * @see java.lang.Long#MAX_VALUE
     */
    public static long max(long a, long b) {
        return (a >= b) ? a : b;
    }

    private static long negativeZeroFloatBits = Float.floatToIntBits(-0.0f);
    private static long negativeZeroDoubleBits = Double.doubleToLongBits(-0.0d);

    /**
     * Returns the greater of two <code>float</code> values. That is, the result
     * is the argument closer to positive infinity. If the arguments have the
     * same value, the result is that same value. If either value is
     * <code>NaN</code>, then the result is <code>NaN</code>. Unlike the the
     * numerical comparison operators, this method considers negative zero to be
     * strictly smaller than positive zero. If one argument is positive zero and
     * the other negative zero, the result is positive zero.
     *
     * @param a a <code>float</code> value.
     * @param b a <code>float</code> value.
     * @return the larger of <code>a</code> and <code>b</code>.
     */
    public static float max(float a, float b) {
        if (a != a) {
            return a; // a is NaN
        }
        if ((a == 0.0f) && (b == 0.0f)
                && (Float.floatToIntBits(a) == negativeZeroFloatBits)) {
            return b;
        }
        return (a >= b) ? a : b;
    }

    /**
     * Returns the greater of two <code>double</code> values. That is, the
     * result is the argument closer to positive infinity. If the arguments have
     * the same value, the result is that same value. If either value is
     * <code>NaN</code>, then the result is <code>NaN</code>. Unlike the the
     * numerical comparison operators, this method considers negative zero to be
     * strictly smaller than positive zero. If one argument is positive zero and
     * the other negative zero, the result is positive zero.
     *
     * @param a a <code>double</code> value.
     * @param b a <code>double</code> value.
     * @return the larger of <code>a</code> and <code>b</code>.
     */
    public static double max(double a, double b) {
        if (a != a) {
            return a; // a is NaN
        }
        if ((a == 0.0d) && (b == 0.0d)
                && (Double.doubleToLongBits(a) == negativeZeroDoubleBits)) {
            return b;
        }
        return (a >= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>int</code> values. That is, the result
     * the argument closer to the value of <code>Integer.MIN_VALUE</code>. If
     * the arguments have the same value, the result is that same value.
     *
     * @param a an <code>int</code> value.
     * @param b an <code>int</code> value.
     * @return the smaller of <code>a</code> and <code>b</code>.
     * @see java.lang.Long#MIN_VALUE
     */
    public static int min(int a, int b) {
        return (a <= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>long</code> values. That is, the result
     * is the argument closer to the value of <code>Long.MIN_VALUE</code>. If
     * the arguments have the same value, the result is that same value.
     *
     * @param a a <code>long</code> value.
     * @param b a <code>long</code> value.
     * @return the smaller of <code>a</code> and <code>b</code>.
     * @see java.lang.Long#MIN_VALUE
     */
    public static long min(long a, long b) {
        return (a <= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>float</code> values. That is, the result
     * is the value closer to negative infinity. If the arguments have the same
     * value, the result is that same value. If either value is
     * <code>NaN</code>, then the result is <code>NaN</code>. Unlike the the
     * numerical comparison operators, this method considers negative zero to be
     * strictly smaller than positive zero. If one argument is positive zero and
     * the other is negative zero, the result is negative zero.
     *
     * @param a a <code>float</code> value.
     * @param b a <code>float</code> value.
     * @return the smaller of <code>a</code> and <code>b.</code>
     * @since CLDC 1.1
     */
    public static float min(float a, float b) {
        if (a != a) {
            return a; // a is NaN
        }
        if ((a == 0.0f) && (b == 0.0f)
                && (Float.floatToIntBits(b) == negativeZeroFloatBits)) {
            return b;
        }
        return (a <= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>double</code> values. That is, the
     * result is the value closer to negative infinity. If the arguments have
     * the same value, the result is that same value. If either value is
     * <code>NaN</code>, then the result is <code>NaN</code>. Unlike the the
     * numerical comparison operators, this method considers negative zero to be
     * strictly smaller than positive zero. If one argument is positive zero and
     * the other is negative zero, the result is negative zero.
     *
     * @param a a <code>double</code> value.
     * @param b a <code>double</code> value.
     * @return the smaller of <code>a</code> and <code>b</code>.
     * @since CLDC 1.1
     */
    public static double min(double a, double b) {
        if (a != a) {
            return a; // a is NaN
        }
        if ((a == 0.0d) && (b == 0.0d)
                && (Double.doubleToLongBits(b) == negativeZeroDoubleBits)) {
            return b;
        }
        return (a <= b) ? a : b;
    }

    public static double signum(double d) {
        if (Double.isNaN(d)) return d;
        if (Double.doubleToLongBits(d) == negativeZeroDoubleBits) return d;
        if (Double.doubleToLongBits(d) == 0.0d) return d;
        if (d > 0) return 1.0d;
        else return -1.0d;
    }

    public static float signum(float f) {
        if (Float.isNaN(f)) return f;
        if (Float.floatToIntBits(f) == negativeZeroFloatBits) return f;
        if (Float.floatToIntBits(f) == 0.0f) return f;
        if (f > 0) return 1.0f;
        else return -1.0f;
    }

    /**
     * Returns the closest <code>long</code> to the argument. Special cases:
     * <ul><li>If the argument is NaN, the result is 0.
     * <li>If the argument is negative infinity or any value less than or equal to the value of Long.MIN_VALUE, the result is equal to the value of Long.MIN_VALUE.
     * <li>If the argument is positive infinity or any value greater than or equal to the value of Long.MAX_VALUE, the result is equal to the value of Long.MAX_VALUE.</ul>
     */
    public static long round(double a) {
        if (Double.isNaN(a)) return 0L;
        if (a <= Long.MIN_VALUE) return Long.MIN_VALUE;
        if (a >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) floor(a + 0.5d);
    }

    /**
     * Returns the closest <code>int</code> to the argument. Special cases:
     * <ul><li>If the argument is NaN, the result is 0.
     * <li>If the argument is negative infinity or any value less than or equal to the value of Integer.MIN_VALUE, the result is equal to the value of Integer.MIN_VALUE.
     * <li>If the argument is positive infinity or any value greater than or equal to the value of Integer.MAX_VALUE, the result is equal to the value of Integer.MAX_VALUE.</ul>
     */
    public static int round(float a) {
        if (Float.isNaN(a)) return 0;
        if (a <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (a >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) floor(a + 0.5f);
    }

    public static native double asin(double a);

    public static native double acos(double a);

    public static native double atan(double a);

    public static native double log(double a);

    public static native double atan2(double y, double x);

    public static native double pow(double a, double b);

    public static native double random();

    public static int addExact(int x, int y) {
        double r = (double) x + (double) y;
        if (r >= Integer.MIN_VALUE && r <= Integer.MAX_VALUE) {
            return x + y;
        } else {
            throw new ArithmeticException("integer overflow");
        }
    }

    public static long addExact(long x, long y) {
        double r = (double) x + (double) y;
        if (r >= Long.MIN_VALUE && r <= Long.MAX_VALUE) {
            return x + y;
        } else {
            throw new ArithmeticException("long overflow");
        }
    }

    /**
     * Returns the cube root of a <code>double</code> value. Special cases:
     * <ul><li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is infinite, then the result is an infinity with the same sign as the argument.
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.</ul>
     */
    public static double cbrt(double a) {
        if (Double.isNaN(a)) return a;
        if (Double.isInfinite(a)) return a;
        if (a == 0.0) return a;
        return Math.pow(a, 1.0 / 3.0);
    }

    /**
     * Returns the hyperbolic cosine of a <code>double</code> value. Special cases:
     * <ul><li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is infinite, then the result is positive infinity.
     * <li>If the argument is zero, then the result is 1.0.</ul>
     */
    public static double cosh(double x) {
        if (Double.isNaN(x)) return x;
        if (Double.isInfinite(x)) return Double.POSITIVE_INFINITY;
        if (x == 0.0) return 1.0;
        return (Math.exp(x) + Math.exp(-x)) / 2.0;
    }

    public static int decrementExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return a - 1;
    }

    public static long decrementExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return a - 1L;
    }

    public static double expm1(double x) {
        return Math.exp(x) - 1.0;
    }

    public static int floorDiv(int x, int y) {
        int r = x / y;
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static long floorDiv(long x, long y) {
        long r = x / y;
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static long floorMod(long x, long y) {
        return x - floorDiv(x, y) * y;
    }

    public static int floorMod(int x, int y) {
        int r = x - floorDiv(x, y) * y;
        return r;
    }

    public static double hypot(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public static double IEEEremainder(double f1, double f2) {
        return f1 - Math.floor(f1 / f2) * f2;
    }

    public static int incrementExact(int a) {
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return a + 1;
    }

    public static long incrementExact(long a) {
        if (a == Long.MAX_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return a + 1L;
    }

    public static double log10(double a) {
        return log(a) * 0.4342944819032518;
    }

    public static double log1p(double x) {
        return log(1.0 + x);
    }

    public static int multiplyExact(int x, int y) {
        long r = (long) x * (long) y;
        if ((int) r != r) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) r;
    }

    public static long multiplyExact(long x, long y) {
        long r = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 != 0)) {
            if (((y != 0) && (r / y != x)) ||
                    (x == Long.MIN_VALUE && y == -1)) {
                throw new ArithmeticException("long overflow");
            }
        }
        return r;
    }

    public static int negateExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return -a;
    }

    public static long negateExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return -a;
    }

    public static double rint(double a) {
        return floor(a + 0.5d);
    }

    public static double scalb(double d, int scaleFactor) {
        return d * Math.pow(2.0, scaleFactor);
    }

    public static float scalb(float f, int scaleFactor) {
        return (float) (f * Math.pow(2.0f, scaleFactor));
    }

    /**
     * Returns the hyperbolic sine of a <code>double</code> value. Special cases:
     * <ul><li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is infinite, then the result is an infinity with the same sign as the argument.
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.</ul>
     */
    public static double sinh(double x) {
        if (Double.isNaN(x)) return x;
        if (Double.isInfinite(x)) return x;
        if (x == 0.0) return x;
        return (Math.exp(x) - Math.exp(-x)) / 2.0;
    }

    public static int subtractExact(int x, int y) {
        int r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    public static long subtractExact(long x, long y) {
        long r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /**
     * Returns the hyperbolic tangent of a <code>double</code> value. Special cases:
     * <ul><li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * <li>If the argument is positive infinity, then the result is +1.0.
     * <li>If the argument is negative infinity, then the result is -1.0.</ul>
     */
    public static double tanh(double x) {
        if (Double.isNaN(x)) return x;
        if (Double.isInfinite(x)) return Math.signum(x);
        if (x == 0.0) return x;
        return (Math.exp(x) - Math.exp(-x)) / (Math.exp(x) + Math.exp(-x));
    }

    public static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    /**
     * Returns the size of an ulp (units in the last place) of the argument.
     * Special cases:
     * <ul><li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive or negative infinity, then the result is positive infinity.
     * <li>If the argument is positive or negative zero, then the result is Double.MIN_VALUE.
     * <li>If the argument is ±Double.MAX_VALUE, then the result is equal to 2^971.</ul>
     */
    public static double ulp(double d) {
        long bits = Double.doubleToLongBits(d);
        if (Double.isNaN(d)) return d;
        if (Double.isInfinite(d)) return Double.POSITIVE_INFINITY;
        if (bits == 0 || bits == 0x8000000000000000L) {
            return Double.MIN_VALUE;
        }
        return Double.longBitsToDouble((bits & 0x8000000000000000L) | 0x000FFFFFFFFFFFFFL);
    }

    /**
     * Returns the size of an ulp (units in the last place) of the argument.
     * Special cases:
     * <ul><li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive or negative infinity, then the result is positive infinity.
     * <li>If the argument is positive or negative zero, then the result is Float.MIN_VALUE.
     * <li>If the argument is ±Float.MAX_VALUE, then the result is equal to 2^104.</ul>
     */
    public static float ulp(float f) {
        int bits = Float.floatToIntBits(f);
        if (Float.isNaN(f)) return f;
        if (Float.isInfinite(f)) return Float.POSITIVE_INFINITY;
        if (bits == 0 || bits == 0x80000000) {
            return Float.MIN_VALUE;
        }
        return Float.intBitsToFloat((bits & 0x80000000) | 0x007FFFFF);
    }

    public static int getExponent(double d) {
        long bits = Double.doubleToLongBits(d);
        if (Double.isNaN(d)) return 0;
        if (Double.isInfinite(d)) return 1024;
        if (d == 0.0) return -1023;
        return (int) (((bits & 0x7FF0000000000000L) >>> 52) - 1023);
    }

    public static int getExponent(float f) {
        int bits = Float.floatToIntBits(f);
        if (Float.isNaN(f)) return 0;
        if (Float.isInfinite(f)) return 128;
        if (f == 0.0f) return -127;
        return ((bits & 0x7F800000) >>> 23) - 127;
    }

    public static double nextAfter(double start, double direction) {
        if (Double.isNaN(start) || Double.isNaN(direction)) {
            return Double.NaN;
        }
        if (start == direction) {
            return direction;
        }
        if (Double.isInfinite(start)) {
            return start;
        }
        if (start == 0.0) {
            return direction > 0 ? Double.MIN_VALUE : -Double.MIN_VALUE;
        }
        long bits = Double.doubleToLongBits(start);
        if (direction > start) {
            bits++;
        } else {
            bits--;
        }
        return Double.longBitsToDouble(bits);
    }

    public static float nextAfter(float start, double direction) {
        if (Float.isNaN(start) || Double.isNaN(direction)) {
            return Float.NaN;
        }
        if (start == direction) {
            return (float) direction;
        }
        if (Float.isInfinite(start)) {
            return start;
        }
        if (start == 0.0f) {
            return direction > 0 ? Float.MIN_VALUE : -Float.MIN_VALUE;
        }
        int bits = Float.floatToIntBits(start);
        if (direction > start) {
            bits++;
        } else {
            bits--;
        }
        return Float.intBitsToFloat(bits);
    }

    public static double nextDown(double d) {
        return nextAfter(d, Double.NEGATIVE_INFINITY);
    }

    public static float nextDown(float f) {
        return nextAfter(f, Float.NEGATIVE_INFINITY);
    }

    public static double nextUp(double d) {
        return nextAfter(d, Double.POSITIVE_INFINITY);
    }

    public static float nextUp(float f) {
        return nextAfter(f, Float.POSITIVE_INFINITY);
    }

    public static double copySign(double magnitude, double sign) {
        return Double.longBitsToDouble((Double.doubleToLongBits(magnitude) & ~Double.SIGN_BIT_MASK) |
                (Double.doubleToLongBits(sign) & Double.SIGN_BIT_MASK));
    }

    public static float copySign(float magnitude, float sign) {
        return Float.intBitsToFloat((Float.floatToIntBits(magnitude) & ~Float.SIGN_BIT_MASK) |
                (Float.floatToIntBits(sign) & Float.SIGN_BIT_MASK));
    }
}
