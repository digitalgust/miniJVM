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

import org.mini.reflect.ReflectMethod;
import org.mini.vm.RefNative;

/**
 * <code>Constructor</code> provides information about, and access to, a single
 * constructor for a class.
 *
 * <p>
 * <code>Constructor</code> permits widening conversions to occur when matching
 * the actual parameters to newInstance() with the underlying constructor's
 * formal parameters, but throws an <code>IllegalArgumentException</code> if a
 * narrowing conversion would occur.
 *
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getConstructors()
 * @see java.lang.Class#getConstructor(Class[])
 * @see java.lang.Class#getDeclaredConstructors()
 *
 * @author	Kenneth Russell
 * @author	Nakul Saraiya
 */
public final class Constructor<T> extends AccessibleObject implements Member, GenericDeclaration {
    private final Class<T> clazz;
    private final ReflectMethod refMethod;
    private org.mini.reflect.GenericSignature genericInfo;
    public Constructor(Class<T> clazz, ReflectMethod refMethod) { this.clazz = clazz; this.refMethod = refMethod; }
    private synchronized org.mini.reflect.GenericSignature genericInfo() {
        if (genericInfo == null) genericInfo = org.mini.reflect.GenericSignature.forMethod(clazz, this, refMethod);
        return genericInfo;
    }
    public T newInstance(Object... initargs) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        checkAccess(RefNative.getCallerClass(), clazz, getModifiers(), null);
        if (Modifier.isAbstract(clazz.getModifiers())) throw new InstantiationException();
        if (clazz.isEnum()) throw new IllegalArgumentException("Cannot reflectively create enum objects");
        Object[] converted = refMethod.convertArguments(initargs);
        Object obj = RefNative.newWithoutInit(clazz);
        try { refMethod.invoke(obj, converted, true); }
        catch (Throwable target) { throw new InvocationTargetException(target); }
        return (T) obj;
    }
    public Class<T> getDeclaringClass() { return clazz; }
    public String getName() { return clazz.getName(); }
    public int getModifiers() { return refMethod.accessFlags; }
    public boolean isSynthetic() { return (getModifiers() & Modifier.SYNTHETIC) != 0; }
    public boolean isVarArgs() { return (getModifiers() & ACC_VARARGS) != 0; }
    public Class<?>[] getParameterTypes() { return refMethod.getParameterTypes().clone(); }
    public Class<?>[] getExceptionTypes() { return refMethod.getExceptionTypes().clone(); }
    public Type[] getGenericParameterTypes() { return genericInfo().parameters.clone(); }
    public Type[] getGenericExceptionTypes() { return genericInfo().exceptions.clone(); }
    public TypeVariable<Constructor<T>>[] getTypeParameters() { return (TypeVariable<Constructor<T>>[]) genericInfo().variables.clone(); }
    public java.lang.annotation.Annotation[] getAnnotations() { return org.mini.reflect.ReflectClass.parseAnnotations(refMethod.annotations, clazz); }
    public java.lang.annotation.Annotation[] getDeclaredAnnotations() { return getAnnotations(); }
    public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> type) {
        if (type == null) throw new NullPointerException();
        for (java.lang.annotation.Annotation a : getAnnotations()) if (type.isInstance(a)) return type.cast(a);
        return null;
    }
    public ReflectMethod getReflectMethod() { return refMethod; }
    public String toString() { return format(false); }
    public String toGenericString() { return format(true); }
    private String format(boolean generic) {
        StringBuilder text = new StringBuilder();
        String modifiers = Modifier.toString(getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED));
        if (!modifiers.isEmpty()) text.append(modifiers).append(' ');
        TypeVariable<?>[] variables = generic ? getTypeParameters() : new TypeVariable<?>[0];
        if (variables.length != 0) {
            text.append('<');
            for (int i = 0; i < variables.length; i++) {
                if (i > 0) text.append(',');
                text.append(variables[i].getName());
            }
            text.append("> ");
        }
        text.append(getName()).append('(');
        Type[] parameters = generic ? getGenericParameterTypes() : getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) text.append(',');
            String name = parameters[i].getTypeName();
            if (generic && isVarArgs() && i == parameters.length - 1 && name.endsWith("[]"))
                name = name.substring(0, name.length() - 2) + "...";
            text.append(name);
        }
        text.append(')');
        Type[] thrown = generic ? getGenericExceptionTypes() : getExceptionTypes();
        if (thrown.length != 0) {
            text.append(" throws ");
            for (int i = 0; i < thrown.length; i++) {
                if (i > 0) text.append(',');
                text.append(thrown[i].getTypeName());
            }
        }
        return text.toString();
    }
    public boolean equals(Object other) { return other instanceof Constructor && clazz == ((Constructor<?>) other).clazz && java.util.Arrays.equals(getParameterTypes(), ((Constructor<?>) other).getParameterTypes()); }
    public int hashCode() { return clazz.getName().hashCode(); }
}
