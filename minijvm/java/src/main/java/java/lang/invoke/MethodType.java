/* Copyright (c) 2008-2016, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package java.lang.invoke;

import java.util.List;
import java.util.ArrayList;
import org.mini.reflect.ReflectClass;
import org.mini.reflect.ReflectMethod;
import org.mini.reflect.vm.ByteCodeAssembler;
import static org.mini.reflect.vm.ByteCodeAssembler.aload;
import static org.mini.reflect.vm.ByteCodeAssembler.areturn;
import static org.mini.reflect.vm.ByteCodeAssembler.dload;
import static org.mini.reflect.vm.ByteCodeAssembler.dreturn;
import static org.mini.reflect.vm.ByteCodeAssembler.fload;
import static org.mini.reflect.vm.ByteCodeAssembler.freturn;
import static org.mini.reflect.vm.ByteCodeAssembler.iload;
import static org.mini.reflect.vm.ByteCodeAssembler.ireturn;
import static org.mini.reflect.vm.ByteCodeAssembler.lload;
import static org.mini.reflect.vm.ByteCodeAssembler.lreturn;

public final class MethodType implements java.io.Serializable {

    private static final char[] Primitives = new char[]{
        'V', 'Z', 'B', 'C', 'S', 'I', 'F', 'J', 'D'
    };

    final ClassLoader loader;
    final String spec;
    private final Class<?> rtype;
    private final Class<?>[] ptypes;
    private volatile List<Parameter> parameters;
    private volatile Result result;

    private volatile int footprint;

    MethodType(ClassLoader loader, String spec) {
        this.loader = loader;
        this.spec = spec;
        this.rtype = ReflectMethod.getMethodReturnType(spec);
        this.ptypes = ReflectMethod.getMethodPara(spec);
    }

    MethodType(String spec) {
        this(ClassLoader.getSystemClassLoader(), spec);
    }

    private MethodType(Class<?> rtype, Class<?>... ptypes) {
        loader = rtype.getClassLoader();
        this.rtype = rtype;
        this.ptypes = ptypes;
        this.spec = ReflectMethod.getMethodSignature(ptypes, rtype);
    }

    public static MethodType methodType(Class rtype, Class ptype0, Class... ptypes) {
        Class[] array = new Class[ptypes.length + 1];
        array[0] = ptype0;
        System.arraycopy(ptypes, 0, array, 1, ptypes.length);
        return methodType(rtype, array);
    }

    public static MethodType methodType(Class rtype, Class... ptypes) {
        return new MethodType(rtype, ptypes);
    }

    public String toMethodDescriptorString() {
        return spec;
    }

    public String toString() {
        return spec;
    }

    public int footprint() {
        return footprint;
    }

    public Class<?> returnType() {
        return rtype;
    }

    public Class<?>[] parameterArray() {
        return ptypes;
    }

    public Iterable<Parameter> parameters() {
        if (parameters == null) {
            List<Parameter> list = new ArrayList();
            int i;
            int index = 0;
            int position = 0;
            for (i = 1; spec.charAt(i) != ')'; ++i) {
                int start = i;
                switch (spec.charAt(i)) {
                    case 'L': {
                        ++i;
                        while (spec.charAt(i) != ';') {
                            ++i;
                        }
                    }
                    break;

                    case '[': {
                        ++i;
                        while (spec.charAt(i) == '[') {
                            ++i;
                        }

                        switch (spec.charAt(i)) {
                            case 'L':
                                ++i;
                                while (spec.charAt(i) != ';') {
                                    ++i;
                                }
                                break;

                            default:
                                break;
                        }
                    }
                    break;

                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                    case 'F':
                    case 'J':
                    case 'D':
                        break;

                    default:
                        throw new RuntimeException("parameters type error");
                }

                String paramSpec = spec.substring(start, i + 1);
                Type type = type(paramSpec);

                list.add(new Parameter(index,
                        position,
                        paramSpec,
                        ptypes[index],
                        type.load));

                ++index;
                position += type.size;
            }

            footprint = position;

            ++i;

            String paramSpec = ReflectClass.getSignatureByClass(rtype);
            Type type = type(paramSpec);

            result = new Result(paramSpec, rtype, type.return_);

            parameters = list;
        }

        return parameters;
    }

    public Result result() {
        parameters(); // ensure spec has been parsed

        return result;
    }

    private static Type type(String spec) {
        switch (spec.charAt(0)) {
            case 'L':
            case '[':
                return Type.ObjectType;

            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
                return Type.IntegerType;

            case 'F':
                return Type.FloatType;

            case 'J':
                return Type.LongType;

            case 'D':
                return Type.DoubleType;

            case 'V':
                return Type.VoidType;

            default:
                throw new RuntimeException();
        }
    }

    private static enum Type {

        ObjectType(aload, areturn, 1),
        IntegerType(iload, ireturn, 1),
        FloatType(fload, freturn, 1),
        LongType(lload, lreturn, 2),
        DoubleType(dload, dreturn, 2),
        VoidType(-1, ByteCodeAssembler.return_, -1);

        public final int load;
        public final int return_;
        public final int size;

        private Type(int load, int return_, int size) {
            this.load = load;
            this.return_ = return_;
            this.size = size;
        }
    }

    public interface TypeSpec {

        public Class type();

        public String spec();
    }

    public static class Parameter implements TypeSpec {

        private final int index;
        private final int position;
        private final String spec;
        private final Class type;
        private final int load;

        private Parameter(int index, int position, String spec, Class type, int load) {
            this.index = index;
            this.position = position;
            this.spec = spec;
            this.type = type;
            this.load = load;
        }

        public int index() {
            return index;
        }

        public int position() {
            return position;
        }

        public String spec() {
            return spec;
        }

        public Class type() {
            return type;
        }

        public int load() {
            return load;
        }
    }

    public static class Result implements TypeSpec {

        private final String spec;
        private final Class type;
        private final int return_;

        public Result(String spec, Class type, int return_) {
            this.spec = spec;
            this.type = type;
            this.return_ = return_;
        }

        public int return_() {
            return return_; // :)
        }

        public String spec() {
            return spec;
        }

        public Class type() {
            return type;
        }
    }

    public MethodType changeReturnType(Class c) {
        return this;
    }

    public int parameterCount() {
        return 0;
    }
}
