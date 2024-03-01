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
            if (pos < 0) {
                return sign.substring(1, sign.length() - 1);
            } else {
                //System.out.println("----:" + sign);
                String s = sign.substring(pos + 1, sign.length() - 2);//get content in < >
                List<String> ss = splitSignature(s);
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
        String s = signature;
        //从后往前拆分方法参数，从栈中弹出放入本地变量
        while (s.length() > 0) {
            char ch = s.charAt(0);
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
                    String tmps = s.substring(0, 1);
                    args.add(tmps);
                    s = s.substring(1);
                    break;
                }
                case 'L': {
                    int ltCount = 0;
                    int end = 1;
                    while (!((ch = s.charAt(end)) == ';' && ltCount == 0)) {// Ljava/util/List<Ljava/lang/Object;>;
                        if (ch == '<') ltCount++;
                        if (ch == '>') ltCount--;
                        end++;
                    }
                    end++;
                    String tmps = s.substring(0, end);
                    args.add(tmps);
                    s = s.substring(end);
                    break;
                }
                case '[': {
                    int end = 1;
                    while (s.charAt(end) == '[') {//去掉多维中的 [[[[LObject; 中的 [符
                        end++;
                    }
                    if (s.charAt(end) == 'L') {
                        int ltCount = 0;
                        while (!((ch = s.charAt(end)) == ';' && ltCount == 0)) {// Ljava/util/List<Ljava/lang/Object;>;
                            if (ch == '<') ltCount++;
                            if (ch == '>') ltCount--;
                            end++;
                        }
                        end++;
                    } else {
                        end++;
                    }
                    String tmps = s.substring(0, end);
                    args.add(tmps);
                    s = s.substring(end);
                    break;
                }
            }

        }

        return args;
    }


    final native void mapClass(long classId);

}
