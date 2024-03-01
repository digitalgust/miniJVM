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


package java.lang.reflect;

import org.mini.reflect.ReflectArray;
import org.mini.vm.RefNative;

/**
 * The <code>Array</code> class provides static methods to dynamically create
 * and access Java arrays.
 *
 * <p>
 * <code>Array</code> permits widening conversions to occur during a get or set
 * operation, but throws an <code>IllegalArgumentException</code> if a narrowing
 * conversion would occur.
 *
 * @author Nakul Saraiya
 */
public final class Array {

    /**
     * Constructor. Class Array is not instantiable.
     */
    private Array() {
    }

    /**
     * Creates a new array with the specified component typeTag and length.
     * Invoking this method is equivalent to creating an array as follows:
     * <blockquote>
     * <pre>
     * int[] x = {length};
     * Array.newInstance(componentType, x);
     * </pre>
     * </blockquote>
     *
     * @param componentType the <code>Class</code> object representing the
     *                      component typeTag of the new array
     * @param length        the length of the new array
     * @return the new array
     * @throws NullPointerException       if the specified
     *                                    <code>componentType</code> parameter is null
     * @throws IllegalArgumentException   if componentType is Void.TYPE
     * @throws NegativeArraySizeException if the specified
     *                                    <code>length</code> is negative
     */
    public static Object newInstance(Class<?> componentType, int length)
        throws NegativeArraySizeException {
        return ReflectArray.newArray(componentType, length);
    }

    /**
     * Creates a new array with the specified component typeTag and dimensions.
     * If <code>componentType</code> represents a non-array class or interface,
     * the new array has <code>dimensions.length</code> dimensions and&nbsp;
     * <code>componentType&nbsp;</code> as its component typeTag. If
     * <code>componentType</code> represents an array class, the number of
     * dimensions of the new array is equal to the sum of
     * <code>dimensions.length</code> and the number of dimensions of
     * <code>componentType</code>. In this case, the component typeTag of the
     * new array is the component typeTag of <code>componentType</code>.
     *
     * <p>
     * The number of dimensions of the new array must not exceed the number of
     * array dimensions supported by the implementation (typically 255).
     *
     * @param componentType the <code>Class</code> object representing the
     *                      component typeTag of the new array
     * @param dimensions    an array of <code>int</code> types representing the
     *                      dimensions of the new array
     * @return the new array
     * @throws NullPointerException       if the specified
     *                                    <code>componentType</code> argument is null
     * @throws IllegalArgumentException   if the specified
     *                                    <code>dimensions</code> argument is a zero-dimensional array, or if the
     *                                    number of requested dimensions exceeds the limit on the number of array
     *                                    dimensions supported by the implementation (typically 255), or if
     *                                    componentType is Void.TYPE.
     * @throws NegativeArraySizeException if any of the components in the
     *                                    specified <code>dimensions</code> argument is negative.
     */
    public static Object newInstance(Class<?> componentType, int[] dimensions)
        throws IllegalArgumentException {
        return ReflectArray.multiNewArray(componentType, dimensions);
    }

    /**
     * Returns the length of the specified array object, as an <code>int</code>.
     *
     * @param array the array
     * @return the length of the array
     * @throws IllegalArgumentException if the object argument is not an
     *                                  array
     */
    public static int getLength(Object array)
        throws IllegalArgumentException {
        return ReflectArray.getLength(array);
    }


    static private void checkExeception(Object array, int index, Class clazz) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array == null) {
            throw new NullPointerException();
        } else if (index < 0 || index > ReflectArray.getLength(array)) {
            throw new IndexOutOfBoundsException();
        } else if (!array.getClass().isArray() || array.getClass() != clazz) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the value of the indexed component in the specified array object.
     * The value is automatically wrapped in an object if it has a primitive
     * typeTag.
     *
     * @param array the array
     * @param index the index
     * @return the (possibly wrapped) value of the indexed component in the
     * specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an
     *                                        array
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     */
    public static Object get(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array == null) {
            throw new NullPointerException();
        }
        if (array instanceof boolean[]) {
            return (Boolean) getBoolean(array, index);
        } else if (array instanceof byte[]) {
            return (Byte) getByte(array, index);
        } else if (array instanceof short[]) {
            return (Short) getShort(array, index);
        } else if (array instanceof char[]) {
            return (Character) getChar(array, index);
        } else if (array instanceof int[]) {
            return (Integer) getInt(array, index);
        } else if (array instanceof long[]) {
            return (Long) getLong(array, index);
        } else if (array instanceof float[]) {
            return (Float) getFloat(array, index);
        } else if (array instanceof double[]) {
            return (Double) getDouble(array, index);
        } else {
            checkExeception(array, index, array.getClass());
            int pos = index * RefNative.refIdSize();
            return RefNative.heap_get_ref(ReflectArray.getBodyPtr(array), pos);
        }

    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>boolean</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static boolean getBoolean(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, boolean[].class);
        int pos = index * 1;
        return RefNative.heap_get_byte(ReflectArray.getBodyPtr(array), pos) != 0;
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>byte</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static byte getByte(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, byte[].class);
        int pos = index * Byte.BYTES;
        return RefNative.heap_get_byte(ReflectArray.getBodyPtr(array), pos);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>char</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static char getChar(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, char[].class);
        int pos = index * Character.BYTES;
        return (char) RefNative.heap_get_short(ReflectArray.getBodyPtr(array), pos);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>short</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static short getShort(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, short[].class);
        int pos = index * Short.BYTES;
        return RefNative.heap_get_short(ReflectArray.getBodyPtr(array), pos);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as an <code>int</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static int getInt(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, int[].class);
        int pos = index * Integer.BYTES;
        return RefNative.heap_get_int(ReflectArray.getBodyPtr(array), pos);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>long</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static long getLong(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, long[].class);
        int pos = index * Long.BYTES;
        return RefNative.heap_get_long(ReflectArray.getBodyPtr(array), pos);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>float</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static float getFloat(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, float[].class);
        int pos = index * Float.BYTES;
        return RefNative.heap_get_float(ReflectArray.getBodyPtr(array), pos);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>double</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @throws NullPointerException           If the specified object is null
     * @throws IllegalArgumentException       If the specified object is not an array,
     *                                        or if the indexed element cannot be converted to the return typeTag by an
     *                                        identity or widening conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#get
     */
    public static double getDouble(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, double[].class);
        int pos = index * Double.BYTES;
        return RefNative.heap_get_double(ReflectArray.getBodyPtr(array), pos);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified new value. The new value is first automatically unwrapped
     * if the array has a primitive component typeTag.
     *
     * @param array the array
     * @param index the index into the array
     * @param value the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the array component typeTag is primitive and an
     *                                        unwrapping conversion fails
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     */
    public static void set(Object array, int index, Object value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array == null) {
            throw new NullPointerException();
        }
        if (array instanceof boolean[]) {
            setBoolean(array, index, (Boolean) value);
        } else if (array instanceof byte[]) {
            setByte(array, index, (Byte) value);
        } else if (array instanceof short[]) {
            setShort(array, index, (Short) value);
        } else if (array instanceof char[]) {
            setChar(array, index, (Character) value);
        } else if (array instanceof int[]) {
            setInt(array, index, (Integer) value);
        } else if (array instanceof long[]) {
            setLong(array, index, (Long) value);
        } else if (array instanceof float[]) {
            setFloat(array, index, (Float) value);
        } else if (array instanceof double[]) {
            setDouble(array, index, (Double) value);
        } else {
            checkExeception(array, index, array.getClass());
            int pos = index * RefNative.refIdSize();
            RefNative.heap_put_ref(ReflectArray.getBodyPtr(array), pos, value);
        }
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>boolean</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param z     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setBoolean(Object array, int index, boolean z)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, boolean[].class);
        int pos = index * Byte.BYTES;
        RefNative.heap_put_byte(ReflectArray.getBodyPtr(array), pos, z ? (byte) 1 : 0);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>byte</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param b     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setByte(Object array, int index, byte b)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, byte[].class);
        int pos = index * Byte.BYTES;
        RefNative.heap_put_byte(ReflectArray.getBodyPtr(array), pos, b);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>char</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param c     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setChar(Object array, int index, char c)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, char[].class);
        int pos = index * Character.BYTES;
        RefNative.heap_put_short(ReflectArray.getBodyPtr(array), pos, (short) c);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>short</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param s     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setShort(Object array, int index, short s)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, short[].class);
        int pos = index * Short.BYTES;
        RefNative.heap_put_short(ReflectArray.getBodyPtr(array), pos, s);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>int</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param i     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setInt(Object array, int index, int i)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, int[].class);
        int pos = index * Integer.BYTES;
        RefNative.heap_put_int(ReflectArray.getBodyPtr(array), pos, i);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>long</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param l     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setLong(Object array, int index, long l)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, long[].class);
        int pos = index * Long.BYTES;
        RefNative.heap_put_long(ReflectArray.getBodyPtr(array), pos, l);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>float</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param f     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setFloat(Object array, int index, float f)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, float[].class);
        int pos = index * Float.BYTES;
        RefNative.heap_put_float(ReflectArray.getBodyPtr(array), pos, f);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>double</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param d     the new value of the indexed component
     * @throws NullPointerException           If the specified object argument is null
     * @throws IllegalArgumentException       If the specified object argument is not
     *                                        an array, or if the specified value cannot be converted to the underlying
     *                                        array's component typeTag by an identity or a primitive widening
     *                                        conversion
     * @throws ArrayIndexOutOfBoundsException If the specified
     *                                        <code>index</code> argument is negative, or if it is greater than or
     *                                        equal to the length of the specified array
     * @see Array#set
     */
    public static void setDouble(Object array, int index, double d)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkExeception(array, index, double[].class);
        int pos = index * Double.BYTES;
        RefNative.heap_put_double(ReflectArray.getBodyPtr(array), pos, d);
    }

}
