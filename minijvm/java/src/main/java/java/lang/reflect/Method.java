/*
 * @(#)Method.java	1.50 04/06/22
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package java.lang.reflect;

import java.lang.annotation.Annotation;
import org.mini.reflect.ReflectMethod;
import org.mini.reflect.vm.RefNative;

/**
 * A <code>Method</code> provides information about, and access to, a single
 * method on a class or interface. The reflected method may be a class method or
 * an instance method (including an abstract method).
 *
 * <p>
 * A <code>Method</code> permits widening conversions to occur when matching the
 * actual parameters to invoke with the underlying method's formal parameters,
 * but it throws an <code>IllegalArgumentException</code> if a narrowing
 * conversion would occur.
 *
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getMethods()
 * @see java.lang.Class#getMethod(String, Class[])
 * @see java.lang.Class#getDeclaredMethods()
 * @see java.lang.Class#getDeclaredMethod(String, Class[])
 *
 * @author Kenneth Russell
 * @author Nakul Saraiya
 */
public class Method<T> extends AccessibleObject implements Member {

    Class clazz;
    ReflectMethod refMethod;

    public Method(Class cl, ReflectMethod refm) {
        refMethod = refm;
        clazz = cl;
    }

    public Object invoke(Object obj, Object... args)
            throws IllegalAccessException,
            IllegalArgumentException {
        return refMethod.invoke(obj, args);
    }

    public String getName() {
        return refMethod.methodName;
    }

    public Class<?>[] getParameterTypes() {
        return refMethod.getParameterTypes();
    }

    public Class<?> getReturnType() {
        return refMethod.getReturnType();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Method) {
            return refMethod == ((Method) o).refMethod;
        }
        return false;
    }

    @Override
    public Class<T> getDeclaringClass() {
        return clazz;
    }

    @Override
    public int getModifiers() {
        return refMethod.accessFlags;
    }

    @Override
    public boolean isSynthetic() {
        return (refMethod.accessFlags & Modifier.SYNTHETIC) != 0;
    }

    static public Method findMethod(ClassLoader cloader, String className, String methodName, String methodSignature) {
        Class c = RefNative.getClassByName(className);
        if (c != null) {
            ReflectMethod rm = ReflectMethod.findMethod(className, methodName, methodSignature);
            if (rm != null) {
                return new Method(c, rm);
            }
        }
        return null;
    }

    public String getSignature() {
        return refMethod.signature;
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

}
