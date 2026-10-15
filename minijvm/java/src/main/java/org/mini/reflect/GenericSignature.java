package org.mini.reflect;

import java.lang.reflect.*;
import java.util.*;

/** Reflection types decoded from JVMS 4.7.9 signatures, without erasing their identity. */
public final class GenericSignature {
    public TypeVariable<?>[] variables = new TypeVariable<?>[0];
    public Type superclass;
    public Type[] interfaces = new Type[0];
    public Type[] parameters = new Type[0];
    public Type result;
    public Type[] exceptions = new Type[0];
    private final String text;
    private final Class<?> context;
    private final GenericDeclaration declaration;
    private final Map<String, Variable> locals = new LinkedHashMap<String, Variable>();
    private int pos;
    private boolean classDefinition;

    private GenericSignature(String text, Class<?> context, GenericDeclaration declaration) {
        this.text = text;
        this.context = context;
        this.declaration = declaration;
    }

    public static GenericSignature forClass(Class<?> type) {
        GenericSignature info = new GenericSignature(type.getRefClass().signature, type, type);
        info.classDefinition = true;
        if (info.text == null) {
            info.superclass = type.getSuperclass();
            info.interfaces = type.getInterfaces();
        } else {
            info.formals();
            info.superclass = info.type();
            List<Type> implemented = new ArrayList<Type>();
            while (info.pos < info.text.length()) implemented.add(info.type());
            info.interfaces = implemented.toArray(new Type[0]);
        }
        if (type.isInterface()) info.superclass = null;
        return info;
    }

    public static GenericSignature forMethod(Class<?> owner, GenericDeclaration method, ReflectMethod ref) {
        GenericSignature info = new GenericSignature(ref.signature, owner, method);
        info.exceptions = ref.getExceptionTypes();
        if (info.text == null) {
            info.parameters = ref.getParameterTypes();
            info.result = ref.getReturnType();
        } else {
            info.formals();
            info.expect('(');
            List<Type> args = new ArrayList<Type>();
            while (info.peek() != ')') args.add(info.type());
            info.pos++;
            info.parameters = args.toArray(new Type[0]);
            info.result = info.type();
            List<Type> thrown = new ArrayList<Type>();
            while (info.pos < info.text.length()) { info.expect('^'); thrown.add(info.type()); }
            if (!thrown.isEmpty()) info.exceptions = thrown.toArray(new Type[0]);
        }
        return info;
    }

    public static Type forField(Class<?> owner, String signature, Class<?> erased) {
        if (signature == null) return erased;
        GenericSignature info = new GenericSignature(signature, owner, owner);
        Type result = info.type();
        if (info.pos != signature.length()) throw new GenericSignatureFormatError();
        return result;
    }

    private char peek() {
        if (pos >= text.length()) throw new GenericSignatureFormatError();
        return text.charAt(pos);
    }

    private void expect(char ch) {
        if (peek() != ch) throw new GenericSignatureFormatError();
        pos++;
    }

    // Register every variable before decoding bounds, including forward and mutually recursive bounds.
    private void formals() {
        if (peek() != '<') return;
        pos++;
        List<Integer> starts = new ArrayList<Integer>();
        while (peek() != '>') {
            int start = pos;
            while (peek() != ':') pos++;
            String name = text.substring(start, pos++);
            if (locals.containsKey(name)) throw new GenericSignatureFormatError();
            locals.put(name, new Variable(name, declaration));
            starts.add(Integer.valueOf(pos));
            if (peek() != ':') skipType();
            while (peek() == ':') { pos++; skipType(); }
        }
        int end = ++pos;
        variables = locals.values().toArray(new TypeVariable<?>[0]);
        int i = 0;
        for (Variable variable : locals.values()) {
            pos = starts.get(i++).intValue();
            List<Type> bounds = new ArrayList<Type>();
            if (peek() != ':') bounds.add(type());
            while (peek() == ':') { pos++; bounds.add(type()); }
            variable.bounds = bounds.isEmpty() ? new Type[] { Object.class } : bounds.toArray(new Type[0]);
        }
        pos = end;
    }

    private void skipType() {
        char ch = peek(); pos++;
        if (ch == '[' || ch == '+' || ch == '-') { skipType(); return; }
        if (ch == 'L') {
            int depth = 0;
            while (true) {
                ch = peek(); pos++;
                if (ch == '<') depth++;
                if (ch == '>') depth--;
                if (ch == ';' && depth == 0) return;
            }
        }
        if (ch == 'T') { while (peek() != ';') pos++; pos++; return; }
        if ("BCDFIJSZV*".indexOf(ch) < 0) throw new GenericSignatureFormatError();
    }

    private Type variable(String name) {
        Variable own = locals.get(name);
        if (own != null) return own;
        Class<?> scope = context;
        // A class's own variables are already in locals; avoid reentering its initialization.
        if (classDefinition) {
            scope = context.getDeclaringClass();
            if (scope == null) {
                GenericDeclaration enclosing = context.getEnclosingMethod();
                if (enclosing == null) enclosing = context.getEnclosingConstructor();
                if (enclosing != null) for (TypeVariable<?> v : enclosing.getTypeParameters()) if (v.getName().equals(name)) return v;
                scope = context.getEnclosingClass();
            }
        }
        while (scope != null) {
            for (TypeVariable<?> v : scope.getTypeParameters()) if (v.getName().equals(name)) return v;
            Class<?> outer = scope.getDeclaringClass();
            if (outer == null) {
                GenericDeclaration enclosing = scope.getEnclosingMethod();
                if (enclosing == null) enclosing = scope.getEnclosingConstructor();
                if (enclosing != null) for (TypeVariable<?> v : enclosing.getTypeParameters()) if (v.getName().equals(name)) return v;
                outer = scope.getEnclosingClass();
            }
            scope = outer;
        }
        throw new GenericSignatureFormatError();
    }

    private Type type() {
        char ch = peek(); pos++;
        switch (ch) {
            case 'B': return byte.class;
            case 'C': return char.class;
            case 'D': return double.class;
            case 'F': return float.class;
            case 'I': return int.class;
            case 'J': return long.class;
            case 'S': return short.class;
            case 'Z': return boolean.class;
            case 'V': return void.class;
            case '[': {
                Type element = type();
                return element instanceof Class ? Array.newInstance((Class<?>) element, 0).getClass() : new ArrayType(element);
            }
            case 'T': {
                int start = pos;
                while (peek() != ';') pos++;
                String name = text.substring(start, pos++);
                return variable(name);
            }
            case 'L': {
                String name = "";
                Type owner = null;
                while (true) {
                    int start = pos;
                    while (peek() != '<' && peek() != '.' && peek() != ';') pos++;
                    name += text.substring(start, pos).replace('/', '.');
                    Class<?> raw = resolve(name);
                    List<Type> args = new ArrayList<Type>();
                    if (peek() == '<') {
                        pos++;
                        while (peek() != '>') {
                            char variance = peek();
                            if (variance == '*') { pos++; args.add(new Wildcard(new Type[] { Object.class }, new Type[0])); }
                            else if (variance == '+') { pos++; args.add(new Wildcard(new Type[] { type() }, new Type[0])); }
                            else if (variance == '-') { pos++; args.add(new Wildcard(new Type[] { Object.class }, new Type[] { type() })); }
                            else args.add(type());
                        }
                        pos++;
                    }
                    Type value = args.isEmpty() && !(owner instanceof ParameterizedType)
                        ? raw : new Parameterized(raw, owner == null ? raw.getDeclaringClass() : owner, args.toArray(new Type[0]));
                    char next = peek(); pos++;
                    if (next == ';') return value;
                    if (next != '.') throw new GenericSignatureFormatError();
                    owner = value;
                    name += '$';
                }
            }
            default: throw new GenericSignatureFormatError();
        }
    }

    private Class<?> resolve(String name) {
        try { return Class.forName(name, false, context.getClassLoader()); }
        catch (ClassNotFoundException e) { throw new TypeNotPresentException(name, e); }
    }

    private static final class Variable implements TypeVariable<GenericDeclaration> {
        private final String name;
        private final GenericDeclaration declaration;
        private Type[] bounds;
        Variable(String name, GenericDeclaration declaration) { this.name = name; this.declaration = declaration; }
        public Type[] getBounds() { return bounds.clone(); }
        public GenericDeclaration getGenericDeclaration() { return declaration; }
        public String getName() { return name; }
        public String getTypeName() { return name; }
        public String toString() { return name; }
        public boolean equals(Object other) { return other instanceof TypeVariable && name.equals(((TypeVariable<?>) other).getName()) && declaration.equals(((TypeVariable<?>) other).getGenericDeclaration()); }
        public int hashCode() { return name.hashCode() ^ declaration.hashCode(); }
    }

    private static final class Parameterized implements ParameterizedType {
        private final Class<?> raw;
        private final Type owner;
        private final Type[] args;
        Parameterized(Class<?> raw, Type owner, Type[] args) { this.raw = raw; this.owner = owner; this.args = args; }
        public Type getRawType() { return raw; }
        public Type getOwnerType() { return owner; }
        public Type[] getActualTypeArguments() { return args.clone(); }
        public boolean equals(Object other) {
            if (!(other instanceof ParameterizedType)) return false;
            ParameterizedType p = (ParameterizedType) other;
            return raw.equals(p.getRawType()) && Objects.equals(owner, p.getOwnerType()) && Arrays.equals(args, p.getActualTypeArguments());
        }
        public int hashCode() { return raw.hashCode() ^ (owner == null ? 0 : owner.hashCode()) ^ Arrays.hashCode(args); }
        public String getTypeName() {
            StringBuilder name = new StringBuilder(owner == null ? raw.getName() : owner.getTypeName() + "$" + raw.getName().substring(raw.getName().lastIndexOf('$') + 1));
            if (args.length > 0) { name.append('<'); for (int i = 0; i < args.length; i++) { if (i > 0) name.append(", "); name.append(args[i].getTypeName()); } name.append('>'); }
            return name.toString();
        }
        public String toString() { return getTypeName(); }
    }

    private static final class ArrayType implements GenericArrayType {
        private final Type element;
        ArrayType(Type element) { this.element = element; }
        public Type getGenericComponentType() { return element; }
        public String getTypeName() { return element.getTypeName() + "[]"; }
        public String toString() { return getTypeName(); }
        public boolean equals(Object other) { return other instanceof GenericArrayType && element.equals(((GenericArrayType) other).getGenericComponentType()); }
        public int hashCode() { return element.hashCode(); }
    }

    private static final class Wildcard implements WildcardType {
        private final Type[] upper, lower;
        Wildcard(Type[] upper, Type[] lower) { this.upper = upper; this.lower = lower; }
        public Type[] getUpperBounds() { return upper.clone(); }
        public Type[] getLowerBounds() { return lower.clone(); }
        public boolean equals(Object other) { return other instanceof WildcardType && Arrays.equals(upper, ((WildcardType) other).getUpperBounds()) && Arrays.equals(lower, ((WildcardType) other).getLowerBounds()); }
        public int hashCode() { return Arrays.hashCode(upper) ^ Arrays.hashCode(lower); }
        public String getTypeName() { return lower.length > 0 ? "? super " + lower[0].getTypeName() : upper[0] == Object.class ? "?" : "? extends " + upper[0].getTypeName(); }
        public String toString() { return getTypeName(); }
    }
}
