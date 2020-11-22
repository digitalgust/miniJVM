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

import org.mini.reflect.ReflectField;
import org.mini.reflect.ReflectMethod;
import org.mini.reflect.vm.RConst;

import java.lang.annotation.Annotation;

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
 * @author Kenneth Russell
 * @author Nakul Saraiya
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getMethods()
 * @see java.lang.Class#getMethod(String, Class[])
 * @see java.lang.Class#getDeclaredMethods()
 * @see java.lang.Class#getDeclaredMethod(String, Class[])
 */
public class Method<T> extends AccessibleObject implements Member {

    Class clazz;
    ReflectMethod refMethod;

    public Method(Class cl, ReflectMethod refm) {
        refMethod = refm;
        clazz = cl;
    }


    public ReflectMethod getReflectMethod() {
        return refMethod;
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
        Class c = null;
        try {
            c = Class.forName(className, false, cloader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (c != null) {
            ReflectMethod rm = ReflectMethod.findMethod(cloader, className, methodName, methodSignature);
            if (rm != null) {
                return new Method(c, rm);
            }
        }
        return null;
    }

    public String getDescriptor() {
        return refMethod.descriptor;
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if ((refMethod.accessFlags & RConst.ACC_STATIC) != 0) {
            sb.append("static ");
        }
        if ((refMethod.accessFlags & RConst.ACC_PUBLIC) != 0) {
            sb.append("public ");
        } else if ((refMethod.accessFlags & RConst.ACC_PRIVATE) != 0) {
            sb.append("private ");
        } else if ((refMethod.accessFlags & RConst.ACC_PROTECTED) != 0) {
            sb.append("protected ");
        }
        sb.append(refMethod.getReturnType().getCanonicalName()).append(' ');
        sb.append(getDeclaringClass().getName()).append('.');
        sb.append(refMethod.methodName).append('(');
        Class[] ps = getParameterTypes();
        for (int i = 0; i < ps.length; i++) {
            sb.append(ps[i].getCanonicalName());
            if (i + 1 < ps.length) sb.append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    public String toGenericString() {
        StringBuilder sb = new StringBuilder();
        if ((refMethod.accessFlags & RConst.ACC_STATIC) != 0) {
            sb.append("static ");
        }
        if ((refMethod.accessFlags & RConst.ACC_PUBLIC) != 0) {
            sb.append("public ");
        } else if ((refMethod.accessFlags & RConst.ACC_PRIVATE) != 0) {
            sb.append("private ");
        } else if ((refMethod.accessFlags & RConst.ACC_PROTECTED) != 0) {
            sb.append("protected ");
        }
        sb.append(refMethod.getGenericReturnType().getTypeName()).append(' ');
        sb.append(getDeclaringClass().getName()).append('.');
        sb.append(refMethod.methodName).append('(');
        Type[] ps = getGenericParameterTypes();
        for (int i = 0; i < ps.length; i++) {
            sb.append(ps[i].getTypeName());
            if (i + 1 < ps.length) sb.append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    public boolean hasGenericInformation() {
        return refMethod.hasGenericInformation();
    }

    public Type[] getGenericParameterTypes() {
        return refMethod.getGenericParameterTypes();
    }

    public Type getGenericReturnType() {
        return refMethod.getGenericReturnType();
    }
}
