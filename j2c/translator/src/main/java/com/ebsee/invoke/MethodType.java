/* Copyright (c) 2008-2016, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package com.ebsee.invoke;


import com.ebsee.invoke.bytecode.ByteCodeAssembler;
import com.ebsee.invoke.bytecode.DynamicClassLoader;
import com.ebsee.invoke.bytecode.LambdaUtil;

import java.util.ArrayList;
import java.util.List;

import static com.ebsee.invoke.bytecode.ByteCodeAssembler.*;


public final class MethodType implements java.io.Serializable {

    private static final char[] Primitives = new char[]{
            'V', 'Z', 'B', 'C', 'S', 'I', 'F', 'J', 'D'
    };

    final String spec;
    private final String rtype;
    private final String[] ptypes;
    private volatile List<Parameter> parameters;
    private volatile Result result;

    private volatile int footprint;

    public MethodType(String spec) {
        this.spec = spec;
        this.rtype = LambdaUtil.getMethodReturnType(spec);
        this.ptypes = LambdaUtil.getMethodPara(spec);
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

    public String returnType() {
        return rtype;
    }

    public String[] parameterArray() {
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
                        type.load));

                ++index;
                position += type.size;
            }

            footprint = position;

            ++i;

            String paramSpec = spec.substring(spec.indexOf(')') + 1);
            Type type = type(paramSpec);

            result = new Result(paramSpec, type.return_);

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

        String spec();

        boolean isPrimitive();
    }

    public static class Parameter implements TypeSpec {

        private final int index;
        private final int position;
        private final String spec;
        private final int load;

        private Parameter(int index, int position, String spec, int load) {
            this.index = index;
            this.position = position;
            this.spec = spec;
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

        public int load() {
            return load;
        }

        public boolean isPrimitive() {
            return LambdaUtil.isPrimitive(spec);
        }
    }

    public static class Result implements TypeSpec {

        private final String spec;
        private final int return_;

        public Result(String spec, int return_) {
            this.spec = spec;
            this.return_ = return_;
        }

        public int return_() {
            return return_; // :)
        }

        public String spec() {
            return spec;
        }

        public boolean isPrimitive() {
            return LambdaUtil.isPrimitive(spec);
        }
    }

    public MethodType changeReturnType(Class c) {
        return this;
    }

    public int parameterCount() {
        return 0;
    }
}
