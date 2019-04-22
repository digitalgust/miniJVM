/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RefNative;

/**
 * <code>
 *      Class cla = "xxx".getClass();
 *      Reference ref = new Reference(RefNative.obj2id(cla));
 *      System.out.println("ref.name=" + ref.className);
 *      try {
 *          String s = (String) cla.newInstance();
 *          System.out.println(s);
 *      } catch (InstantiationException ex) {
 *      } catch (IllegalAccessException ex) {
 *      }
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
    public int status;
    long fieldIds[];
    long methodIds[];
    public long interfaces[];
    public long classObj;//类对象

    private ReflectField[] fields;
    private ReflectMethod[] methods;

    public ReflectClass(long classId) {
        this.classId = classId;
        mapReference(classId);

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
            methods[i] = new ReflectMethod(this, methodIds[i]);
        }
    }

    public ReflectMethod[] getMethods() {
        loadMethods();
        return methods;
    }

    public ReflectMethod getMethod(String methodName, String methodSignature) {
        loadMethods();
        for (int i = 0; i < methods.length; i++) {
            ReflectMethod m = methods[i];
            if (m.methodName.equals(methodName) && m.signature.equals(methodSignature)) {
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

    static public Class getClassBySignature(String s) {
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
                s = s.substring(1, s.length() - 1);
                return RefNative.getClassByName(s);
            default:
                return RefNative.getClassByName(s);
        }
    }

    static public String getSignatureByClass(Class c) {
        if (c.isArray()) {
            return "[" + getSignatureByClass(c.getComponentType());
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

    final native void mapReference(long classId);

}
