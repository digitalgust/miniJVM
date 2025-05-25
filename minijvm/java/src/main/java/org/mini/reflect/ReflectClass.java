/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.vm.RefNative;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>
 * Class cla = "xxx".getClass();
 * Reference ref = new Reference(RefNative.obj2id(cla));
 * System.out.println("ref.name=" + ref.className);
 * try {
 * String s = (String) cla.newInstance();
 * System.out.println(s);
 * } catch (InstantiationException ex) {
 * } catch (IllegalAccessException ex) {
 * }
 * </code>
 *
 * @author gust
 */
public class ReflectClass {

    public static final byte CLASS = 1; //ReferenceType is a class.
    public static final byte INTERFACE = 2; //ReferenceType is an interface.
    public static final byte ARRAY = 3; //ReferenceType is an array.

    //不可随意改动字段类型及名字，要和native一起改
    public long classId;
    public long superclass;
    public String className;
    public short accessFlags;
    public String source;
    public String signature;
    public String annotations;
    public int status;
    long fieldIds[];
    long methodIds[];
    public long interfaces[];
    public Class classObj;//类对象

    private ReflectField[] fields;
    private ReflectMethod[] methods;

    public ReflectClass(long classId) {
        this.classId = classId;
        mapClass(classId);

    }

    private void loadFields() {
        if (fields != null) {
            return;
        }
        fields = new ReflectField[fieldIds.length];
        for (int i = 0; i < fieldIds.length; i++) {
            fields[i] = new ReflectField(this, fieldIds[i]);
        }
    }

    private void loadMethods() {
        if (methods != null) {
            return;
        }
        methods = new ReflectMethod[methodIds.length];
        for (int i = 0; i < methodIds.length; i++) {
            methods[i] = new ReflectMethod(classObj, methodIds[i]);
        }
    }

    public Class getClassObj() {
        return classObj;
    }

    public ReflectMethod[] getMethods() {
        loadMethods();
        return methods;
    }

    public ReflectMethod getMethod(String methodName, String methodSignature) {
        loadMethods();
        for (int i = 0; i < methods.length; i++) {
            ReflectMethod m = methods[i];
            if (m.methodName.equals(methodName) && m.descriptor.equals(methodSignature)) {
                return methods[i];
            }
        }
        return null;
    }

    public ReflectMethod getMethod(String methodName, Class<?>... parameterTypes) {
        loadMethods();
        for (int i = 0; i < methods.length; i++) {
            ReflectMethod m = methods[i];
            if (m.methodName.equals(methodName)) {
                boolean found = true;
                Class[] paras = m.getParameterTypes();
                if (parameterTypes.length == paras.length) {
                    for (int j = 0; j < parameterTypes.length; j++) {
                        if (!paras[j].equals(parameterTypes[j])) {
                            found = false;
                        }
                    }
                } else {
                    found = false;
                }
                if (found) {
                    return methods[i];
                }
            }
        }
        return null;
    }

    public ReflectMethod getMethod(long methodId) {
        loadMethods();
        for (int i = 0; i < methodIds.length; i++) {
            if (methods[i].methodId == methodId) {
                return methods[i];
            }
        }
        return null;
    }

    public ReflectField[] getFields() {
        loadFields();
        return fields;
    }

    public ReflectField getField(String name) {
        loadFields();
        for (int i = 0; i < fields.length; i++) {
            ReflectField f = fields[i];
            if (f.fieldName.equals(name)) {
                return f;
            }
        }
        return null;
    }

    public ReflectField getField(long fieldId) {
        loadFields();
        for (int i = 0; i < fieldIds.length; i++) {
            if (fields[i].fieldId == fieldId) {
                return fields[i];
            }
        }
        return null;
    }

    public static byte getReferenceTypeTag(long classId) {
        Class clazz = RefNative.id2obj(classId).getClass();
        if (clazz.isArray()) {
            return ARRAY;
        } else if (clazz.isInterface()) {
            return INTERFACE;
        } else {
            return CLASS;
        }
    }

    static Class[] primitiveClass; //cant init static , because some type wasnt init
    static String[] primitiveTag = {"S", "C", "B", "I", "F", "Z", "D", "J", "V"};

    static public Class getClassByDescriptor(ClassLoader loader, String s) {
        switch (s.charAt(0)) {
            case 'S':
                return Short.TYPE;
            case 'C':
                return Character.TYPE;
            case 'B':
                return Byte.TYPE;
            case 'I':
                return Integer.TYPE;
            case 'F':
                return Float.TYPE;
            case 'Z':
                return Boolean.TYPE;
            case 'D':
                return Double.TYPE;
            case 'J':
                return Long.TYPE;
            case 'V':
                return Void.TYPE;
            case 'L':
                if (s.indexOf('<') >= 0) {
                    s = s.substring(1, s.indexOf('<'));
                } else {
                    s = s.substring(1, s.length() - 1);
                }//no break here
            default:
                try {
                    return Class.forName(s, false, loader);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
        }
    }

    static public String getDescriptorByClass(Class c) {
        if (c.isArray()) {
            return "[" + getDescriptorByClass(c.getComponentType());
        } else if (c.isPrimitive()) {
            if (primitiveClass == null) {
                primitiveClass = new Class[]{short.class, char.class, byte.class, int.class, float.class, boolean.class, double.class, long.class, void.class};
            }

            int i = 0;
            for (Class pc : primitiveClass) {
                if (pc == c) {
                    return primitiveTag[i];
                }
                i++;
            }
        } else {
            return "L" + c.getName().replace('.', '/') + ";";
        }
        return null;
    }


    static public String getNameByDescriptor(String s) {
        switch (s.charAt(0)) {
            case 'S':
                return "short";
            case 'C':
                return "char";
            case 'B':
                return "byte";
            case 'I':
                return "int";
            case 'F':
                return "float";
            case 'Z':
                return "boolean";
            case 'D':
                return "double";
            case 'J':
                return "long";
            case 'V':
                return "void";
            case '[':
                return getNameByDescriptor(s.substring(1)) + "[]";
            case 'L':
                return convertReference2name(s);
            default:
                throw new RuntimeException();
        }
    }

    static public String convertReference2name(String sign) {
        if (sign != null) {
            int pos = sign.indexOf('<');
            if (sign.startsWith("L") && pos < 0) {
                return sign.substring(1, sign.length() - 1);
            } else if (sign.startsWith("[")) {
                String s = sign.substring(1);
                return convertReference2name(s) + "[]";
            } else {
                //System.out.println("----:" + sign);
                String s = sign.substring(pos + 1, sign.length() - 2);//get content in < >
                boolean isGeneric = false;
                if (s.equals("*") || s.startsWith("T") || s.indexOf(";T") > 0) {
                    isGeneric = true;
                }
                List<String> ss = isGeneric ? new ArrayList<>() : splitSignature(s);
                StringBuilder sb = new StringBuilder();
                sb.append(sign.substring(1, pos)).append('<');
                for (int i = 0, imax = ss.size(); i < imax; i++) {
                    String p = ss.get(i);
                    sb.append(convertReference2name(p));
                    if (i + 1 < imax) sb.append(',').append(' ');
                }
                sb.append('>');
                for (int i = 0; i < sb.length(); i++) {
                    if (sb.charAt(i) == '/') {
                        sb.setCharAt(i, '.');
                    }
                }
                return sb.toString();
            }
        }
        return null;
    }


    //Ljava/util/List<Ljava/lang/Object;>;BII
    public static List<String> splitSignature(String signature) {
        List<String> args = new ArrayList();
        //System.out.println("methodType:" + methodType);

        TokenOfString tokenOrder = new TokenOfString();
        while (tokenOrder.pos < signature.length()) {
            String type = getNextType(signature, tokenOrder);
            args.add(type);
        }
        return args;
    }

    private static String getNextType(String signature, TokenOfString token) {
        while (signature.length() > 0) {
            char ch = signature.charAt(token.pos);
            String types = "";
            switch (ch) {
                case 'S':
                case 'C':
                case 'B':
                case 'I':
                case 'F':
                case 'Z':
                case 'D':
                case 'J': {
                    String tmps = signature.substring(token.pos, token.pos + 1);
                    token.pos++;
                    return tmps;
                }
                case 'L': {
                    int ltCount = 0;
                    int start = token.pos;
                    token.pos++;
                    while (!((ch = signature.charAt(token.pos)) == ';' && ltCount == 0)) {// Ljava/util/List<Ljava/lang/Object;>;
                        if (ch == '<') ltCount++;
                        if (ch == '>') ltCount--;
                        token.pos++;
                    }
                    token.pos++;
                    String tmps = signature.substring(start, token.pos);
                    return tmps;
                }
                case '[': {
                    token.pos++;
                    String tmps = "[" + getNextType(signature, token);
                    return tmps;
                }
            }

        }
        return null;
    }

    private static class TokenOfString {
        public int pos;
    }

    final native void mapClass(long classId);

    /**
     * 获取这个类的注解
     *
     * @return 注解对象数组
     */
    public java.lang.annotation.Annotation[] getAnnotations() {
        return parseAnnotations(annotations, classObj);
    }

    /**
     * 静态方法：解析注解字符串为注解数组
     *
     * @param annotationsStr 注解字符串
     * @param classObj       类对象，用于获取ClassLoader
     * @return 注解对象数组
     */
    public static java.lang.annotation.Annotation[] parseAnnotations(String annotationsStr, Class<?> classObj) {
        if (annotationsStr == null || annotationsStr.isEmpty()) {
            return new java.lang.annotation.Annotation[0];
        }

        // 解析注解字符串
        // 格式: {Lcom/example/MyAnnotation;(value="test",number=123),LOtherAnnotation;}
        if (annotationsStr.startsWith("{") && annotationsStr.endsWith("}")) {
            String content = annotationsStr.substring(1, annotationsStr.length() - 1);
            if (content.isEmpty()) {
                return new java.lang.annotation.Annotation[0];
            }

            List<java.lang.annotation.Annotation> annotationList = new ArrayList<>();

            // 简单的解析：按逗号分割注解
            String[] parts = splitAnnotations(content);

            for (String part : parts) {
                try {
                    java.lang.annotation.Annotation annotation = parseAnnotation(part.trim(), classObj);
                    if (annotation != null) {
                        annotationList.add(annotation);
                    }
                } catch (Exception e) {
                    // 忽略解析错误，继续处理其他注解
                    e.printStackTrace();
                }
            }

            return annotationList.toArray(new java.lang.annotation.Annotation[annotationList.size()]);
        }

        return new java.lang.annotation.Annotation[0];
    }

    /**
     * 静态方法：分割注解字符串，处理嵌套的括号
     * 注解之间用逗号分隔，但要注意括号内的逗号不应该作为分隔符
     */
    private static String[] splitAnnotations(String content) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int depth = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                // 只有在不在括号内时才分割
                result.add(content.substring(start, i));
                start = i + 1;
            }
        }

        // 添加最后一部分
        if (start < content.length()) {
            result.add(content.substring(start));
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * 静态方法：解析单个注解字符串
     */
    private static java.lang.annotation.Annotation parseAnnotation(String annotationStr, Class<?> classObj) {
        try {
            // 解析注解类型名称
            String typeName;
            String params = "";

            int paramStart = annotationStr.indexOf('(');
            if (paramStart > 0) {
                typeName = annotationStr.substring(0, paramStart);
                params = annotationStr.substring(paramStart + 1, annotationStr.lastIndexOf(')'));
            } else {
                typeName = annotationStr;
            }

            // 转换类型名称格式
            if (typeName.startsWith("[L") && typeName.endsWith(";")) {
                // 字段/方法注解格式：[Ltest/TestAnnotation; -> test/TestAnnotation
                typeName = typeName.substring(2, typeName.length() - 1).replace('/', '.');
            } else if (typeName.startsWith("L") && typeName.endsWith(";")) {
                // 类注解格式：Ltest/TestAnnotation; -> test/TestAnnotation
                typeName = typeName.substring(1, typeName.length() - 1).replace('/', '.');
            } else {
                // 简单格式：test/TestAnnotation -> test.TestAnnotation
                typeName = typeName.replace('/', '.');
            }

            // 加载注解类
            Class<?> annotationClass = Class.forName(typeName, false, classObj.getClassLoader());

            if (annotationClass.isAnnotation()) {
                // 创建注解实例 - 使用Proxy动态代理
                return genProxyClass(annotationClass, params);
            }
        } catch (Exception e) {
            // 如果无法解析，返回null
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 静态方法：使用Proxy动态代理创建注解实例
     */
    private static java.lang.annotation.Annotation genProxyClass(Class<?> annotationClass, String params) {
        try {
            // 使用Java标准的Proxy机制创建注解代理
            java.lang.reflect.InvocationHandler handler = new AnnotationInvocationHandler(annotationClass, params);

            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    annotationClass.getClassLoader(),
                    new Class<?>[]{annotationClass},
                    handler
            );

            return (java.lang.annotation.Annotation) proxy;
        } catch (Exception e) {
            // 如果Proxy创建失败，回退到简单代理
            return createAnnotationProxy(annotationClass, params);
        }
    }

    /**
     * 静态方法：创建简单注解代理对象（回退方案）
     */
    private static java.lang.annotation.Annotation createAnnotationProxy(Class<?> annotationClass, String params) {
        // 这里创建一个简单的注解代理
        // 由于miniJVM的限制，我们返回一个基本的实现
        return new SimpleAnnotationProxy(annotationClass, params);
    }

    /**
     * 静态方法：根据注解类型查找特定注解
     */
    public static <T extends java.lang.annotation.Annotation> T findAnnotation(String annotationsStr, Class<?> classObj, Class<T> annotationClass) {
        if (annotationsStr == null || annotationsStr.isEmpty()) {
            return null;
        }

        java.lang.annotation.Annotation[] annotations = parseAnnotations(annotationsStr, classObj);
        for (java.lang.annotation.Annotation annotation : annotations) {
            if (annotationClass.equals(annotation.annotationType())) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    /**
     * 注解的InvocationHandler实现
     */
    private static class AnnotationInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final Class<?> annotationType;
        private final String params;

        public AnnotationInvocationHandler(Class<?> annotationType, String params) {
            this.annotationType = annotationType;
            this.params = params;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // 处理Annotation接口的方法
            if ("annotationType".equals(methodName)) {
                return annotationType;
            } else if ("toString".equals(methodName)) {
                return "@" + annotationType.getName() + (params.isEmpty() ? "" : "(" + params + ")");
            } else if ("equals".equals(methodName) && args != null && args.length == 1) {
                Object other = args[0];
                if (other instanceof java.lang.annotation.Annotation) {
                    return annotationType.equals(((java.lang.annotation.Annotation) other).annotationType());
                }
                return false;
            } else if ("hashCode".equals(methodName)) {
                return annotationType.hashCode();
            }

            // 处理注解方法（如value(), number()等）
            // 这里可以解析params字符串来返回对应的值
            // 目前返回默认值
            Class<?> returnType = method.getReturnType();
            if (returnType == String.class) {
                return parseStringValue(methodName, params);
            } else if (returnType == int.class) {
                return parseIntValue(methodName, params);
            } else if (returnType == boolean.class) {
                return parseBooleanValue(methodName, params);
            }

            // 返回默认值
            return getDefaultValue(returnType);
        }

        private String parseStringValue(String methodName, String params) {
            // 简单的参数解析，格式：name=value,name2=value2
            if (params != null && !params.isEmpty()) {
                String[] pairs = params.split(",");
                for (String pair : pairs) {
                    String[] parts = pair.split("=");
                    if (parts.length == 2 && parts[0].trim().equals(methodName)) {
                        String value = parts[1].trim();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            return value.substring(1, value.length() - 1);
                        }
                        return value;
                    }
                }
            }
            return ""; // 默认值
        }

        private int parseIntValue(String methodName, String params) {
            if (params != null && !params.isEmpty()) {
                String[] pairs = params.split(",");
                for (String pair : pairs) {
                    String[] parts = pair.split("=");
                    if (parts.length == 2 && parts[0].trim().equals(methodName)) {
                        try {
                            return Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                }
            }
            return 0; // 默认值
        }

        private boolean parseBooleanValue(String methodName, String params) {
            if (params != null && !params.isEmpty()) {
                String[] pairs = params.split(",");
                for (String pair : pairs) {
                    String[] parts = pair.split("=");
                    if (parts.length == 2 && parts[0].trim().equals(methodName)) {
                        return Boolean.parseBoolean(parts[1].trim());
                    }
                }
            }
            return false; // 默认值
        }

        private Object getDefaultValue(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0f;
            if (type == double.class) return 0.0;
            if (type == char.class) return '\0';
            if (type == String.class) return "";
            return null;
        }
    }

    /**
     * 简单的注解代理类
     */
    private static class SimpleAnnotationProxy implements java.lang.annotation.Annotation {
        private final Class<?> annotationType;
        private final String params;

        public SimpleAnnotationProxy(Class<?> annotationType, String params) {
            this.annotationType = annotationType;
            this.params = params;
        }

        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return (Class<? extends java.lang.annotation.Annotation>) annotationType;
        }

        @Override
        public String toString() {
            return "@" + annotationType.getName() + (params.isEmpty() ? "" : "(" + params + ")");
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof java.lang.annotation.Annotation)) return false;
            java.lang.annotation.Annotation other = (java.lang.annotation.Annotation) obj;
            return annotationType.equals(other.annotationType());
        }

        @Override
        public int hashCode() {
            return annotationType.hashCode();
        }
    }

}
