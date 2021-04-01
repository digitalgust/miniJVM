/* Copyright (c) 2008-2016, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package com.ebsee.invoke;

import com.ebsee.invoke.bytecode.DynamicClassLoader;
import com.ebsee.invoke.bytecode.LambdaUtil;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodHandle {

    public static class MethodShadow {

        int kind;
        private String className;
        private String methodName;
        private String spec;
        MethodType methodType;

        public MethodShadow(String cn, String mn, String s) {
            this.className = cn;
            this.methodName = mn;
            this.spec = s;
            methodType = new MethodType(s);
        }

        public String getDeclaringClass() {
            return className;
        }

        public String getName() {
            return methodName;
        }

        public String getDescriptor() {
            return spec;
        }

        public int getModifiers() {
            return 0;
        }

        public MethodType getMethodType() {
            return methodType;
        }
    }

    public static final int REF_invokeVirtual = 5;
    public static final int REF_invokeStatic = 6;
    public static final int REF_invokeSpecial = 7;
    public static final int REF_newInvokeSpecial = 8;
    public static final int REF_invokeInterface = 9;

    final int kind;
    private final DynamicClassLoader loader;
    final MethodShadow method; //MUST NOT change this name it used in
    private volatile MethodType type;

    MethodHandle(int kind, MethodShadow method) {
        this.kind = kind;
        this.loader = new DynamicClassLoader();
        this.method = method;
    }

    MethodHandle(int kind, DynamicClassLoader loader, MethodShadow method) {
        this.kind = kind;
        this.loader = loader;
        this.method = method;
    }

    public MethodHandle(String className, String methodName, String spec, int kind) {
        this.kind = kind;
        this.loader = new DynamicClassLoader();
        try {
            this.method = new MethodShadow(className, methodName, spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (method.getDeclaringClass() != null) {
            sb.append(method.getDeclaringClass());
            sb.append(".");
        }
        sb.append(method.getName()).append(method.spec);
        return sb.toString();
    }

    public MethodType type() {
        if (type == null) {
            type = new MethodType(method.spec);
        }
        return type;
    }

    public MethodShadow getMethod() {
        return method;
    }

    public MethodHandle asType(MethodType mt) {
        return null;
    }

    public MethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        return this;
    }

}
