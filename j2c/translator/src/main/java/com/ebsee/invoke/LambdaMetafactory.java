/* Copyright (c) 2008-2016, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package com.ebsee.invoke;


import com.ebsee.invoke.bytecode.ByteCodeAssembler;
import com.ebsee.invoke.bytecode.ByteCodeConstantPool;
import com.ebsee.invoke.bytecode.ByteCodeConstantPool.PoolEntry;
import com.ebsee.invoke.bytecode.DynamicClassLoader;
import com.ebsee.invoke.bytecode.LambdaUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.ebsee.invoke.bytecode.ByteCodeAssembler.*;
import static com.ebsee.invoke.bytecode.ByteCodeStream.*;

// To understand what this is all about, please read:
//
//   http://cr.openjdk.java.net/~briangoetz/lambda/lambda-translation.html
public class LambdaMetafactory {

    private static int nextNumber = 0;

    public static final int FLAG_SERIALIZABLE = 1;
    public static final int FLAG_MARKERS = 2;
    public static final int FLAG_BRIDGES = 4;
    static DynamicClassLoader dcl = new DynamicClassLoader();

    private static Class resolveReturnInterface(MethodType type) throws ClassNotFoundException {
        int index = 1;
        String s = type.spec;

        while (s.charAt(index) != ')') {
            ++index;
        }

        if (s.charAt(++index) != 'L') {
            throw new RuntimeException();
        }

        ++index;

        int end = index + 1;
        while (s.charAt(end) != ';') {
            ++end;
        }
        String ts = s.substring(index, end);
//        Class c = SystemClassLoader.getClass(Classes.loadVMClass(type.loader, s, index, end - index));
        Class c = Class.forName(ts);//gust //RefNative.getClassByName(ts);
        if (!c.isInterface()) {
            throw new RuntimeException();
        }

        return c;
    }

    private static String constructorSpec(MethodType type) {
        return type.spec.substring(0, type.spec.indexOf(')') + 1) + "V";
//    return Classes.makeString(type.spec, 0, indexOf(')', type.spec) + 1) + "V";
    }

    private static byte[] makeFactoryCode(List<ByteCodeConstantPool.PoolEntry> pool,
                                          String className,
                                          String constructorSpec,
                                          MethodType type)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write2(out, type.footprint() + 2); // max stack
        write2(out, type.footprint()); // max locals
        write4(out, 0); // length (we'll set the real value later)

        write1(out, new_);
        write2(out, ByteCodeConstantPool.addClass(pool, className) + 1);
        write1(out, dup);

        for (MethodType.Parameter p : type.parameters()) {
            write1(out, p.load());
            write1(out, p.position());
        }

        write1(out, invokespecial);
        write2(out, ByteCodeConstantPool.addMethodRef(pool, className, "<init>", constructorSpec) + 1);

        write1(out, areturn);

        write2(out, 0); // exception handler table length
        write2(out, 0); // attribute count

        byte[] result = out.toByteArray();
        set4(result, 4, result.length - 12);

        return result;
    }

    private static byte[] makeConstructorCode(List<PoolEntry> pool,
                                              String className,
                                              MethodType type)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write2(out, 3); // max stack
        write2(out, type.footprint() + 1); // max locals
        write4(out, 0); // length (we'll set the real value later)

        write1(out, aload_0);
        write1(out, invokespecial);
        write2(out, ByteCodeConstantPool.addMethodRef(pool, "java/lang/Object", "<init>", "()V") + 1);

        for (MethodType.Parameter p : type.parameters()) {
            write1(out, aload_0);
            write1(out, p.load());
            write1(out, p.position() + 1);
            write1(out, putfield);
            write2(out, ByteCodeConstantPool.addFieldRef(pool, className, "field" + p.index(), p.spec()) + 1);
        }

        write1(out, return_);

        write2(out, 0); // exception handler table length
        write2(out, 0); // attribute count

        byte[] result = out.toByteArray();
        set4(result, 4, result.length - 12);

        return result;
    }

    static MethodHandle.MethodShadow getMethodShadow(Class c, String methodName, Class paraType, Class returnType) throws NoSuchMethodException {
        //Method m = c.getMethod(methodName);

        String desc = "(" + LambdaUtil.getDescriptorByClass(paraType) + ")" + LambdaUtil.getDescriptorByClass(returnType);
        MethodHandle.MethodShadow ms = new MethodHandle.MethodShadow(c.getName().replace('.', '/'), methodName, desc);
        return ms;
    }

    static MethodHandle.MethodShadow getMethodShadow(Class c, String methodName, Class returnType) throws NoSuchMethodException {
        //Method m = c.getMethod(methodName, returnType);
        String desc = "()" + LambdaUtil.getDescriptorByClass(returnType);
        MethodHandle.MethodShadow ms = new MethodHandle.MethodShadow(c.getName().replace('.', '/'), methodName, desc);
        return ms;
    }

    private static void maybeBoxOrUnbox(ByteArrayOutputStream out,
                                        List<PoolEntry> pool,
                                        MethodType.TypeSpec from,
                                        MethodType.TypeSpec to)
            throws IOException {
        if (to.isPrimitive()) {
            if (!(from.isPrimitive() || "V".equals(to.spec()))) {
                write1(out, invokevirtual);

                try {
                    switch (to.spec()) {
                        case "Z":
                            writeMethodReference(out, pool, getMethodShadow(Boolean.class, "booleanValue", Boolean.TYPE));
                            break;

                        case "B":
                            writeMethodReference(out, pool, getMethodShadow(Byte.class, "byteValue", Byte.TYPE));
                            break;

                        case "S":
                            writeMethodReference(out, pool, getMethodShadow(Short.class, "shortValue", Short.TYPE));
                            break;

                        case "C":
                            writeMethodReference(out, pool, getMethodShadow(Character.class, "charValue", Character.TYPE));
                            break;

                        case "I":
                            writeMethodReference(out, pool, getMethodShadow(Integer.class, "intValue", Integer.TYPE));
                            break;

                        case "F":
                            writeMethodReference(out, pool, getMethodShadow(Float.class, "floatValue", Float.TYPE));
                            break;

                        case "J":
                            writeMethodReference(out, pool, getMethodShadow(Long.class, "longValue", Long.TYPE));
                            break;

                        case "D":
                            writeMethodReference(out, pool, getMethodShadow(Double.class, "doubleValue", Double.TYPE));
                            break;

                        default:
                            throw new RuntimeException("don't know how to auto-unbox to " + to.spec());
                    }
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (from.isPrimitive()) {
            write1(out, invokestatic);

            try {
                switch (from.spec()) {
                    case "Z":
                        writeMethodReference(out, pool, getMethodShadow(Boolean.class, "valueOf", Boolean.TYPE, Boolean.class));
                        break;

                    case "B":
                        writeMethodReference(out, pool, getMethodShadow(Byte.class, "valueOf", Byte.TYPE, Byte.class));
                        break;

                    case "S":
                        writeMethodReference(out, pool, getMethodShadow(Short.class, "valueOf", Short.TYPE, Short.class));
                        break;

                    case "C":
                        writeMethodReference(out, pool, getMethodShadow(Character.class, "valueOf", Character.TYPE, Character.class));
                        break;

                    case "I":
                        writeMethodReference(out, pool, getMethodShadow(Integer.class, "valueOf", Integer.TYPE, Integer.class));
                        break;

                    case "F":
                        writeMethodReference(out, pool, getMethodShadow(Float.class, "valueOf", Float.TYPE, Float.class));
                        break;

                    case "J":
                        writeMethodReference(out, pool, getMethodShadow(Long.class, "valueOf", Long.TYPE, Long.class));
                        break;

                    case "D":
                        writeMethodReference(out, pool, getMethodShadow(Double.class, "valueOf", Double.TYPE, Double.class));
                        break;

                    default:
                        throw new RuntimeException("don't know how to autobox from " + from.spec());
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static byte[] makeInvocationCode(List<PoolEntry> pool,
                                             String className,
                                             String constructorSpec,
                                             MethodType fieldType,
                                             MethodType localType,
                                             MethodHandle implMethodHandle)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write2(out, fieldType.footprint()
                + localType.parameterArray().length + 4); // max stack
        write2(out, localType.parameterArray().length + 1); // max locals
        write4(out, 0); // length (we'll set the real value later)

        //write1(out, aload_0);

        Iterator<MethodType.Parameter> dst = implMethodHandle.type().parameters().iterator();

        boolean skip = implMethodHandle.kind != MethodHandle.REF_invokeStatic;

        for (MethodType.Parameter p : fieldType.parameters()) {
            write1(out, aload_0);
            write1(out, getfield);
            write2(out, ByteCodeConstantPool.addFieldRef(pool, className, "field" + p.index(), p.spec()) + 1);
            if (skip) {
                skip = false;
            } else {
                maybeBoxOrUnbox(out, pool, p, dst.next());
            }
        }

        for (MethodType.Parameter p : localType.parameters()) {
            write1(out, p.load());
            write1(out, p.position() + 1);
            if (skip) {
                skip = false;
            } else {
                maybeBoxOrUnbox(out, pool, p, dst.next());
            }
        }

        switch (implMethodHandle.kind) {
            case MethodHandle.REF_invokeVirtual:
                write1(out, invokevirtual);
                writeMethodReference(out, pool, implMethodHandle.method);
                break;

            case MethodHandle.REF_invokeStatic:
                write1(out, invokestatic);
                writeMethodReference(out, pool, implMethodHandle.method);
                break;

            case MethodHandle.REF_invokeSpecial:
                write1(out, invokespecial);
                writeMethodReference(out, pool, implMethodHandle.method);
                break;

            case MethodHandle.REF_newInvokeSpecial:
                write1(out, new_);
                write2(out, ByteCodeConstantPool.addClass(pool,
                        implMethodHandle.method.getDeclaringClass()) + 1);
                write1(out, dup);
                write1(out, invokespecial);
                writeMethodReference(out, pool, implMethodHandle.method);
                break;

            case MethodHandle.REF_invokeInterface:
                write1(out, invokeinterface);
                writeInterfaceMethodReference(out, pool, implMethodHandle.method);
                write1(out, implMethodHandle.method.getMethodType().parameterArray().length);
                write1(out, 0);
                break;

            default:
                throw new RuntimeException("todo: implement '" + implMethodHandle.kind + "' per http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-5.html#jvms-5.4.3.5");
        }

        if (implMethodHandle.kind != MethodHandle.REF_newInvokeSpecial) {
            maybeBoxOrUnbox(out, pool, implMethodHandle.type().result(), localType.result());
        }
        write1(out, localType.result().return_());

        write2(out, 0); // exception handler table length
        write2(out, 0); // attribute count

        byte[] result = out.toByteArray();
        set4(result, 4, result.length - 12);

        return result;
    }

    private static void writeMethodReference(OutputStream out,
                                             List<PoolEntry> pool,
                                             MethodHandle.MethodShadow method)
            throws IOException {
        write2(out, ByteCodeConstantPool.addMethodRef(pool,
                method.getDeclaringClass(),
                //                Classes.makeString(method.class_.name, 0, method.class_.name.length - 1),
                method.getName(),
                //                Classes.makeString(method.name, 0, method.name.length - 1),
                method.getDescriptor()
                //                Classes.makeString(method.spec, 0, method.spec.length - 1)
        ) + 1);

    }

    private static void writeInterfaceMethodReference(OutputStream out,
                                                      List<PoolEntry> pool,
                                                      MethodHandle.MethodShadow method)
            throws IOException {
        write2(out, ByteCodeConstantPool.addInterfaceMethodRef(pool,
                method.getDeclaringClass(),
                //                Classes.makeString(method.class_.name, 0, method.class_.name.length - 1),
                method.getName(),
                //                Classes.makeString(method.name, 0, method.name.length - 1),
                method.getDescriptor()
                //                Classes.makeString(method.spec, 0, method.spec.length - 1)
        ) + 1);
    }

    public static byte[] makeLambda(String className,
                                    String invokedName,
                                    String invokedType,
                                    String methodType,
                                    String implementationClass,
                                    String implementationName,
                                    String implementationSpec,
                                    int implementationKind) {
        return makeLambda(className, invokedName,
                new MethodType(invokedType),
                new MethodType(methodType),
                new MethodHandle(implementationClass, implementationName, implementationSpec, implementationKind),
                emptyInterfaceList);
    }

    private static byte[] makeLambda(String className, String invokedName,
                                     MethodType invokedType,
                                     MethodType methodType,
                                     MethodHandle methodImplementation,
                                     Class[] interfaces) {
        if (className == null) {
            int number;
            synchronized (java.lang.invoke.LambdaMetafactory.class) {
                number = nextNumber++;
            }
            className = "Lambda_" + number;
        }

        List<PoolEntry> pool = new ArrayList();

        int[] interfaceIndexes = new int[interfaces.length + 1];
        String invokeTypeName = invokedType.returnType().replace('.', '/');//gust
        invokeTypeName = invokeTypeName.substring(1, invokeTypeName.length() - 1);
        interfaceIndexes[0] = ByteCodeConstantPool.addClass(pool, invokeTypeName);
        for (int i = 0; i < interfaces.length; i++) {
            String name = interfaces[i].getName().replace('.', '/');
            interfaceIndexes[i + 1] = ByteCodeConstantPool.addClass(pool, name);
        }

        List<FieldData> fieldTable = new ArrayList();

        for (MethodType.Parameter p : invokedType.parameters()) {
            fieldTable.add(new FieldData(0,
                    ByteCodeConstantPool.addUtf8(pool, "field" + p.index()),
                    ByteCodeConstantPool.addUtf8(pool, p.spec())));
        }

        String constructorSpec = constructorSpec(invokedType);

        List<MethodData> methodTable = new ArrayList();

        try {
            methodTable.add(new MethodData(Modifier.PUBLIC | Modifier.STATIC,
                    ByteCodeConstantPool.addUtf8(pool, "make"),
                    ByteCodeConstantPool.addUtf8(pool, invokedType.toMethodDescriptorString()),
                    makeFactoryCode(pool, className, constructorSpec, invokedType)));

            methodTable.add(new MethodData(Modifier.PUBLIC,
                    ByteCodeConstantPool.addUtf8(pool, "<init>"),
                    ByteCodeConstantPool.addUtf8(pool, constructorSpec),
                    makeConstructorCode(pool, className, invokedType)));

            methodTable.add(new MethodData(Modifier.PUBLIC,
                    ByteCodeConstantPool.addUtf8(pool, invokedName),
                    ByteCodeConstantPool.addUtf8(pool, methodType.toMethodDescriptorString()),
                    makeInvocationCode(pool, className, constructorSpec, invokedType,
                            methodType, methodImplementation)));
        } catch (IOException e) {

            throw new RuntimeException(e);
        }

        int nameIndex = ByteCodeConstantPool.addClass(pool, className);
        int superIndex = ByteCodeConstantPool.addClass(pool, "java/lang/Object");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ByteCodeAssembler.writeClass(out, pool, nameIndex, superIndex, interfaceIndexes,
                    fieldTable.toArray(new FieldData[fieldTable.size()]),
                    methodTable.toArray(new MethodData[methodTable.size()]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        try {
//            FileOutputStream fos = new FileOutputStream(className + ".class");
//            fos.write(out.toByteArray());
//            fos.close();
//        } catch (IOException iOException) {
//        }
        return out.toByteArray();
    }

    private static CallSite makeCallSite(MethodType invokedType, byte[] classData) {
        try {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Class[] emptyInterfaceList = new Class[]{};

    public static CallSite metafactory(
            String invokedName,
            MethodType invokedType,
            MethodType samMethodType,
            MethodHandle implMethod,
            MethodType instantiatedMethodType)
            throws LambdaConversionException {
        byte[] classData = makeLambda(null, invokedName, invokedType, samMethodType, implMethod, emptyInterfaceList);
        return makeCallSite(invokedType, classData);
    }

}
