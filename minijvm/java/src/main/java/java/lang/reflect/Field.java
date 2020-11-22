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

import org.mini.reflect.ReflectClass;
import org.mini.reflect.ReflectField;

import java.lang.annotation.Annotation;

/**
 * A <code>Field</code> provides information about, and dynamic access to, a
 * single field of a class or an interface. The reflected field may be a class
 * (static) field or an instance field.
 *
 * <p>
 * A <code>Field</code> permits widening conversions to occur during a get or
 * set access operation, but throws an <code>IllegalArgumentException</code> if
 * a narrowing conversion would occur.
 *
 * @author Kenneth Russell
 * @author Nakul Saraiya
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getFields()
 * @see java.lang.Class#getField(String)
 * @see java.lang.Class#getDeclaredFields()
 * @see java.lang.Class#getDeclaredField(String)
 */
public final class Field<T> extends AccessibleObject implements Member {

    Class clazz;
    ReflectField refField;

    public Field(Class cl, ReflectField reff) {
        refField = reff;
        clazz = cl;
    }

    public ReflectField getRefField() {
        return refField;
    }

    public String getName() {
        return refField.fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Field) {
            return refField == ((Field) o).refField;
        }
        return false;
    }

    @Override
    public Class<T> getDeclaringClass() {
        return clazz;
    }

    @Override
    public int getModifiers() {
        return refField.accessFlags;
    }

    @Override
    public boolean isSynthetic() {
        return (refField.accessFlags & Modifier.SYNTHETIC) != 0;
    }

    public Class<?> getType() {
        return ReflectClass.getClassByDescriptor(clazz.getClassLoader(), refField.descriptor);
    }

    public Type getGenericType() {
        return refField.getGenericType();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> class_) {

        return null;
    }

    @Override
    public Annotation[] getAnnotations() {

        return new Annotation[0];

    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    /**
     * Returns the value of the field represented by this {@code Field}, on the
     * specified object. The value is automatically wrapped in an object if it
     * has a primitive type.
     *
     * <p>
     * The underlying field's value is obtained as follows:
     *
     * <p>
     * If the underlying field is a static field, the {@code obj} argument is
     * ignored; it may be null.
     *
     * <p>
     * Otherwise, the underlying field is an instance field. If the specified
     * {@code obj} argument is null, the method throws a
     * {@code NullPointerException}. If the specified object is not an instance
     * of the class or interface declaring the underlying field, the method
     * throws an {@code IllegalArgumentException}.
     *
     * <p>
     * If this {@code Field} object is enforcing Java language access control,
     * and the underlying field is inaccessible, the method throws an
     * {@code IllegalAccessException}. If the underlying field is static, the
     * class that declared the field is initialized if it has not already been
     * initialized.
     *
     * <p>
     * Otherwise, the value is retrieved from the underlying instance or static
     * field. If the field has a primitive type, the value is wrapped in an
     * object before being returned, otherwise it is returned as is.
     *
     * <p>
     * If the field is hidden in the type of {@code obj}, the field's value is
     * obtained according to the preceding rules.
     *
     * @param obj object from which the represented field's value is to be
     *            extracted
     * @return the value of the represented field in object {@code obj};
     * primitive values are wrapped in an appropriate object before being
     * returned
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof).
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     */
    public Object get(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance {@code boolean} field.
     *
     * @param obj the object to extract the {@code boolean} value from
     * @return the value of the {@code boolean} field
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code boolean} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public boolean getBoolean(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Boolean) refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance {@code byte} field.
     *
     * @param obj the object to extract the {@code byte} value from
     * @return the value of the {@code byte} field
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code byte} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public byte getByte(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Byte) refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance field of type {@code char} or of
     * another primitive type convertible to type {@code char} via a widening
     * conversion.
     *
     * @param obj the object to extract the {@code char} value from
     * @return the value of the field converted to type {@code char}
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code char} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public char getChar(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Character) refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance field of type {@code short} or of
     * another primitive type convertible to type {@code short} via a widening
     * conversion.
     *
     * @param obj the object to extract the {@code short} value from
     * @return the value of the field converted to type {@code short}
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code short} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public short getShort(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Short) refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance field of type {@code int} or of
     * another primitive type convertible to type {@code int} via a widening
     * conversion.
     *
     * @param obj the object to extract the {@code int} value from
     * @return the value of the field converted to type {@code int}
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code int} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public int getInt(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Integer) refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance field of type {@code long} or of
     * another primitive type convertible to type {@code long} via a widening
     * conversion.
     *
     * @param obj the object to extract the {@code long} value from
     * @return the value of the field converted to type {@code long}
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code long} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public long getLong(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Long) refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance field of type {@code float} or of
     * another primitive type convertible to type {@code float} via a widening
     * conversion.
     *
     * @param obj the object to extract the {@code float} value from
     * @return the value of the field converted to type {@code float}
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code float} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public float getFloat(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Float) refField.getValObj(obj);
    }

    /**
     * Gets the value of a static or instance field of type {@code double} or of
     * another primitive type convertible to type {@code double} via a widening
     * conversion.
     *
     * @param obj the object to extract the {@code double} value from
     * @return the value of the field converted to type {@code double}
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is
     *                                     inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if the field value cannot be
     *                                     converted to the type {@code double} by a widening conversion.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#get
     */
    public double getDouble(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        return (Double) refField.getValObj(obj);
    }

    /**
     * Sets the field represented by this {@code Field} object on the specified
     * object argument to the specified new value. The new value is
     * automatically unwrapped if the underlying field has a primitive type.
     *
     * <p>
     * The operation proceeds as follows:
     *
     * <p>
     * If the underlying field is static, the {@code obj} argument is ignored;
     * it may be null.
     *
     * <p>
     * Otherwise the underlying field is an instance field. If the specified
     * object argument is null, the method throws a
     * {@code NullPointerException}. If the specified object argument is not an
     * instance of the class or interface declaring the underlying field, the
     * method throws an {@code IllegalArgumentException}.
     *
     * <p>
     * If this {@code Field} object is enforcing Java language access control,
     * and the underlying field is inaccessible, the method throws an
     * {@code IllegalAccessException}.
     *
     * <p>
     * If the underlying field is final, the method throws an
     * {@code IllegalAccessException} unless {@code setAccessible(true)} has
     * succeeded for this {@code Field} object and the field is non-static.
     * Setting a final field in this way is meaningful only during
     * deserialization or reconstruction of instances of classes with blank
     * final fields, before they are made available for access by other parts of
     * a program. Use in any other context may have unpredictable effects,
     * including cases in which other parts of a program continue to use the
     * original value of this field.
     *
     * <p>
     * If the underlying field is of a primitive type, an unwrapping conversion
     * is attempted to convert the new value to a value of a primitive type. If
     * this attempt fails, the method throws an
     * {@code IllegalArgumentException}.
     *
     * <p>
     * If, after possible unwrapping, the new value cannot be converted to the
     * type of the underlying field by an identity or widening conversion, the
     * method throws an {@code IllegalArgumentException}.
     *
     * <p>
     * If the underlying field is static, the class that declared the field is
     * initialized if it has not already been initialized.
     *
     * <p>
     * The field is set to the possibly unwrapped and widened new value.
     *
     * <p>
     * If the field is hidden in the type of {@code obj}, the field's value is
     * set according to the preceding rules.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     */
    public void set(Object obj, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, value);
    }

    /**
     * Sets the value of a field as a {@code boolean} on the specified object.
     * This method is equivalent to {@code set(obj, zObj)}, where {@code zObj}
     * is a {@code Boolean} object and {@code zObj.booleanValue() == z}.
     *
     * @param obj the object whose field should be modified
     * @param z   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setBoolean(Object obj, boolean z)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Boolean) z);
    }

    /**
     * Sets the value of a field as a {@code byte} on the specified object. This
     * method is equivalent to {@code set(obj, bObj)}, where {@code bObj} is a
     * {@code Byte} object and {@code bObj.byteValue() == b}.
     *
     * @param obj the object whose field should be modified
     * @param b   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setByte(Object obj, byte b)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Byte) b);
    }

    /**
     * Sets the value of a field as a {@code char} on the specified object. This
     * method is equivalent to {@code set(obj, cObj)}, where {@code cObj} is a
     * {@code Character} object and {@code cObj.charValue() == c}.
     *
     * @param obj the object whose field should be modified
     * @param c   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setChar(Object obj, char c)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Character) c);
    }

    /**
     * Sets the value of a field as a {@code short} on the specified object.
     * This method is equivalent to {@code set(obj, sObj)}, where {@code sObj}
     * is a {@code Short} object and {@code sObj.shortValue() == s}.
     *
     * @param obj the object whose field should be modified
     * @param s   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setShort(Object obj, short s)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Short) s);
    }

    /**
     * Sets the value of a field as an {@code int} on the specified object. This
     * method is equivalent to {@code set(obj, iObj)}, where {@code iObj} is a
     * {@code Integer} object and {@code iObj.intValue() == i}.
     *
     * @param obj the object whose field should be modified
     * @param i   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setInt(Object obj, int i)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Integer) i);
    }

    /**
     * Sets the value of a field as a {@code long} on the specified object. This
     * method is equivalent to {@code set(obj, lObj)}, where {@code lObj} is a
     * {@code Long} object and {@code lObj.longValue() == l}.
     *
     * @param obj the object whose field should be modified
     * @param l   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setLong(Object obj, long l)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Long) l);
    }

    /**
     * Sets the value of a field as a {@code float} on the specified object.
     * This method is equivalent to {@code set(obj, fObj)}, where {@code fObj}
     * is a {@code Float} object and {@code fObj.floatValue() == f}.
     *
     * @param obj the object whose field should be modified
     * @param f   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setFloat(Object obj, float f)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Float) f);
    }

    /**
     * Sets the value of a field as a {@code double} on the specified object.
     * This method is equivalent to {@code set(obj, dObj)}, where {@code dObj}
     * is a {@code Double} object and {@code dObj.doubleValue() == d}.
     *
     * @param obj the object whose field should be modified
     * @param d   the new value for the field of {@code obj} being modified
     * @throws IllegalAccessException      if this {@code Field} object is
     *                                     enforcing Java language access control and the underlying field is either
     *                                     inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying field (or a
     *                                     subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null and the
     *                                     field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked by
     *                                     this method fails.
     * @see Field#set
     */
    public void setDouble(Object obj, double d)
            throws IllegalArgumentException, IllegalAccessException {
        refField.setValObj(obj, (Double) d);
    }

    public String toString() {
        return ReflectClass.getNameByDescriptor(refField.descriptor) + refField.fieldName;
    }

    public String toGenericString() {
        return ReflectClass.getNameByDescriptor(refField.signature) + refField.fieldName;
    }

}
