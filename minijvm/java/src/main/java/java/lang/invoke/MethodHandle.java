/* Copyright (c) 2008-2016, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package java.lang.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodHandle {

    static final int REF_invokeVirtual = 5;
    static final int REF_invokeStatic = 6;
    static final int REF_invokeSpecial = 7;
    static final int REF_newInvokeSpecial = 8;
    static final int REF_invokeInterface = 9;

    final int kind;
    private final ClassLoader loader;
    final Method method; //MUST NOT change this name it used in 
    private volatile MethodType type;

    MethodHandle(int kind, Method method) {
        this.kind = kind;
        this.loader = ClassLoader.getSystemClassLoader();
        this.method = method;
    }

    MethodHandle(int kind, ClassLoader loader, Method method) {
        this.kind = kind;
        this.loader = loader;
        this.method = method;
    }

    MethodHandle(int kind, String className, String methodName, String spec) {
        this.kind = kind;
        this.loader = ClassLoader.getSystemClassLoader();
        try {
            this.method = Method.findMethod(this.loader, className, methodName, spec);
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
        sb.append(method.getName()).append(method.getSignature());
        return sb.toString();
    }

    public MethodType type() {
        if (type == null) {
            type = new MethodType(loader, method.getSignature());
        }
        return type;
    }
    
    public Method getMethod(){
        return method;
    }

    public MethodHandle asType(MethodType mt) {
        return null;
    }

    public MethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        return this;
    }

    public final Object invokeExact(Object... args) throws Throwable {
        Object ins;
        Object[] paras;
        if ((method.getModifiers() & Modifier.STATIC) != 0) {
            ins = null;
            paras = args;
        } else {
            ins = args[0];
            paras = new Object[args.length - 1];
            System.arraycopy(args, 1, paras, 0, args.length - 1);
        }
        if (paras.length != method.getParameterTypes().length) {
            throw new IllegalArgumentException();
        }
        return method.invoke(ins, paras);
    }
}
