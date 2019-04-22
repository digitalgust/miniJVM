/*
 * @(#)Array.java	1.21 04/04/20
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package java.lang.reflect;

import org.mini.reflect.ReflectArray;
import org.mini.reflect.vm.RefNative;

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
     * component typeTag of the new array
     * @param length the length of the new array
     * @return the new array
     * @exception NullPointerException if the specified
     * <code>componentType</code> parameter is null
     * @exception IllegalArgumentException if componentType is Void.TYPE
     * @exception NegativeArraySizeException if the specified
     * <code>length</code> is negative
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
     * component typeTag of the new array
     * @param dimensions an array of <code>int</code> types representing the
     * dimensions of the new array
     * @return the new array
     * @exception NullPointerException if the specified
     * <code>componentType</code> argument is null
     * @exception IllegalArgumentException if the specified
     * <code>dimensions</code> argument is a zero-dimensional array, or if the
     * number of requested dimensions exceeds the limit on the number of array
     * dimensions supported by the implementation (typically 255), or if
     * componentType is Void.TYPE.
     * @exception NegativeArraySizeException if any of the components in the
     * specified <code>dimensions</code> argument is negative.
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
     * @exception IllegalArgumentException if the object argument is not an
     * array
     */
    public static int getLength(Object array)
            throws IllegalArgumentException {
        return ReflectArray.getLength(RefNative.obj2id(array));
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
     * @exception NullPointerException If the specified object is null
     * @exception IllegalArgumentException If the specified object is not an
     * array
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     */
    public static Object get(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>boolean</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static boolean getBoolean(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Boolean) ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>byte</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static byte getByte(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Byte) ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>char</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static char getChar(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Character) ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>short</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static short getShort(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Short) ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as an <code>int</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static int getInt(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Integer) ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>long</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static long getLong(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Long) ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>float</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static float getFloat(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Float) ra.getValObj(index);
    }

    /**
     * Returns the value of the indexed component in the specified array object,
     * as a <code>double</code>.
     *
     * @param array the array
     * @param index the index
     * @return the value of the indexed component in the specified array
     * @exception NullPointerException If the specified object is null
     * @throws IllegalArgumentException If the specified object is not an array,
     * or if the indexed element cannot be converted to the return typeTag by an
     * identity or widening conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#get
     */
    public static double getDouble(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        return (Double) ra.getValObj(index);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified new value. The new value is first automatically unwrapped
     * if the array has a primitive component typeTag.
     *
     * @param array the array
     * @param index the index into the array
     * @param value the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the array component typeTag is primitive and an
     * unwrapping conversion fails
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     */
    public static void set(Object array, int index, Object value)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, value);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>boolean</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param z the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setBoolean(Object array, int index, boolean z)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Boolean) z);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>byte</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param b the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setByte(Object array, int index, byte b)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Byte) b);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>char</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param c the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setChar(Object array, int index, char c)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Character) c);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>short</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param s the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setShort(Object array, int index, short s)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Short) s);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>int</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param i the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setInt(Object array, int index, int i)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Integer) i);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>long</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param l the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setLong(Object array, int index, long l)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Long) l);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>float</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param f the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setFloat(Object array, int index, float f)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Float) f);
    }

    /**
     * Sets the value of the indexed component of the specified array object to
     * the specified <code>double</code> value.
     *
     * @param array the array
     * @param index the index into the array
     * @param d the new value of the indexed component
     * @exception NullPointerException If the specified object argument is null
     * @throws IllegalArgumentException If the specified object argument is not
     * an array, or if the specified value cannot be converted to the underlying
     * array's component typeTag by an identity or a primitive widening
     * conversion
     * @exception ArrayIndexOutOfBoundsException If the specified
     * <code>index</code> argument is negative, or if it is greater than or
     * equal to the length of the specified array
     * @see Array#set
     */
    public static void setDouble(Object array, int index, double d)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        ReflectArray ra = new ReflectArray(RefNative.obj2id(array));
        ra.setValObj(index, (Double) d);
    }

}
