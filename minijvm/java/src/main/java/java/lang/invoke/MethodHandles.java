/* Copyright (c) 2008-2016, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package java.lang.invoke;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodHandles {

    static Lookup lookup = new Lookup();

    public static class Lookup {

        public static final int PUBLIC = Modifier.PUBLIC;
        public static final int PRIVATE = Modifier.PRIVATE;
        public static final int PROTECTED = Modifier.PROTECTED;
        public static final int PACKAGE = Modifier.STATIC;
        Class clazz;
        private int modes;
        private static final int ALL_MODES = (PUBLIC | PRIVATE | PROTECTED | PACKAGE);

        private Lookup(Class c) {
            this(c, ALL_MODES);
        }

        private Lookup(Class c, int modes) {
            this.clazz = c;
            this.modes = modes;
        }

        public Lookup() {
        }

        public MethodHandle findSpecial(Class<?> clazz, String name, MethodType type) throws NoSuchMethodException {
            Method m = clazz.getMethod(name, type.parameterArray());
            if ((m.getModifiers() & (Modifier.PRIVATE)) != 0) {
                MethodHandle mh = new MethodHandle(MethodHandle.REF_invokeVirtual, null, m);
                return mh;
            }
            throw new NoSuchMethodException(name);
        }

        public MethodHandle findVirtual(Class<?> clazz, String name, MethodType type) throws NoSuchMethodException {
            Method m = clazz.getMethod(name, type.parameterArray());
            if ((m.getModifiers() & (Modifier.STATIC | Modifier.PRIVATE)) == 0) {
                MethodHandle mh = new MethodHandle(MethodHandle.REF_invokeVirtual, null, m);
                return mh;
            }
            throw new NoSuchMethodException(name);
        }

        public MethodHandle findStatic(Class<?> clazz, String name, MethodType type) throws NoSuchMethodException {
            Method m = clazz.getMethod(name, type.parameterArray());
            if ((m.getModifiers() & (Modifier.STATIC)) != 0) {
                MethodHandle mh = new MethodHandle(MethodHandle.REF_invokeVirtual, null, m);
                return mh;
            }
            throw new NoSuchMethodException(name);
        }

        public MethodHandle findConstructor(Class<?> clazz, MethodType type) throws NoSuchMethodException {
            Constructor m = clazz.getConstructor(type.parameterArray());
            if ((m.getModifiers() & (Modifier.STATIC)) == 0) {
                MethodHandle mh = new MethodHandle(MethodHandle.REF_invokeVirtual, null, m);
                return mh;
            }
            throw new NoSuchMethodException();
        }

        public String toString() {
            return "lookup[" + clazz + ", " + modes + "]";
        }
    }

    public static Lookup lookup() {
        return lookup;
    }
}
