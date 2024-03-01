/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.vm.RConst;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 类方法的反射，以mini jvm中的 MethofInfo的实例内存地址进行初始化 初始化中会把内存中的相应变量反射到ReflectMethod实例中。
 *
 * @author gust
 */
public class ReflectMethod {

    Class clazzObj;

    //不可随意改动字段类型及名字，要和native一起改
    public long methodId;
    public String methodName;
    public String descriptor;
    public String signature;
    public short accessFlags;
    public long codeStart;
    public long codeEnd;
    public int lines;
    public short[] lineNum;

    public int argCnt;//The number of words in the frame used by arguments. Eight-byte arguments use two words; all others use one. 
    public LocalVarTable[] localVarTable;

    private String[] paras;//参数列表
    private Class<?>[] paras_class;
    private Type[] paras_type;

    public ReflectMethod(Class c, long mid) {
        if (mid == 0) {
            throw new IllegalArgumentException();
        }
        clazzObj = c;
        //System.out.println("mid:" + mid);
        this.methodId = mid;
        mapMethod(methodId);
        paras = ReflectClass.splitSignature(
                descriptor.substring(descriptor.indexOf("(") + 1, descriptor.indexOf(")"))
        ).toArray(new String[0]);
        paras_class = getMethodPara(clazzObj.getClassLoader(), descriptor);
        paras_type = getMethodParaType(signature != null ? signature : descriptor);
    }

    public Class[] getParameterTypes() {
        return paras_class;
    }

    public Type[] getGenericParameterTypes() {
        return paras_type;
    }

    public String[] getParameterStrs() {
        return paras;
    }

    public int getLineNum(long pc) {
        if (lineNum != null && lineNum.length > 0) {
            for (int i = 0; i < lineNum.length; i += 2) {
                if (pc >= lineNum[lineNum.length - 2 - i]) {
                    return lineNum[lineNum.length - 2 - i + 1];
                }
            }
        }
        return -1;
    }

    public Object invoke(Object obj, Object... args)
            throws IllegalAccessException,
            IllegalArgumentException {
        if (obj == null && ((accessFlags & RConst.ACC_STATIC) == 0)) {//none static method but obj is null
            throw new NullPointerException();
        }
        if (obj != null) {
            if (!clazzObj.isAssignableFrom(obj.getClass())) {
                throw new IllegalArgumentException("instance type error:" + obj.getClass() + " expected:" + clazzObj);
            }
        }
        Class[] pc = getParameterTypes();
        if (args.length != paras.length) {
            throw new IllegalArgumentException();
        }
        if ((accessFlags & RConst.ACC_PRIVATE) != 0) {
            throw new IllegalAccessException();
        }

        return invokeMethod(obj, args);
    }

    public static Class<?>[] getMethodPara(ClassLoader loader, String descriptor) {
        List<String> paras = ReflectClass.splitSignature(
                descriptor.substring(descriptor.indexOf("(") + 1, descriptor.indexOf(")"))
        );
        Class<?>[] paras_class;
        paras_class = new Class[paras.size()];
        for (int i = 0; i < paras.size(); i++) {
            paras_class[i] = ReflectClass.getClassByDescriptor(loader, paras.get(i));
        }
        return paras_class;
    }

    public Class<?>[] getExceptionTypes() {
        return getExceptionTypes0(methodId);
    }

    static class TypeMethodImpl implements Type {
        String name;

        public String getTypeName() {
            return name;
        }
    }

    public static Type[] getMethodParaType(String signature) {
        if (signature == null) {
            return null;
        }
        List<String> paras = ReflectClass.splitSignature(
                signature.substring(signature.indexOf("(") + 1, signature.indexOf(")")));
        Type[] paras_type;
        paras_type = new Type[paras.size()];
        for (int i = 0; i < paras.size(); i++) {
            TypeMethodImpl t = new TypeMethodImpl();
            t.name = ReflectClass.getNameByDescriptor(paras.get(i));
            paras_type[i] = t;
        }
        return paras_type;
    }

    public Class<?> getReturnType() {
        return getMethodReturnType(clazzObj.getClassLoader(), descriptor);
    }

    static public Class<?> getMethodReturnType(ClassLoader loader, String descriptor) {
        String s = descriptor.substring(descriptor.indexOf(')') + 1);
        if (s.equals("V")) {
            return Void.TYPE;
        }
        return ReflectClass.getClassByDescriptor(loader, s);
    }

    public Type getGenericReturnType() {
        return getGenericReturnType(signature == null ? descriptor : signature);
    }

    static public Type getGenericReturnType(String signature) {
        String s = signature.substring(signature.indexOf(')') + 1);
        TypeMethodImpl t = new TypeMethodImpl();
        t.name = ReflectClass.getNameByDescriptor(s);
        return t;
    }

    public boolean hasGenericInformation() {
        return signature != null;
    }

    static public ReflectMethod findMethod(ClassLoader cloader, String className, String methodName, String methodSignature) {
        try {
            Class c = Class.forName(className, false, cloader);
            long mid = findMethod0(cloader, className, methodName, methodSignature);
            if (mid != 0) {
                ReflectMethod rm = new ReflectMethod(c, mid);
                return rm;
            }
        } catch (ClassNotFoundException e) {
        }

        return null;
    }

    static public String getMethodSignature(Class<?>[] ptypes, Class<?> rtype) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Class<?> c : ptypes) {
            sb.append(ReflectClass.getDescriptorByClass(c));
        }
        sb.append(')');
        sb.append(ReflectClass.getDescriptorByClass(rtype));
        return sb.toString();
    }


    public String toString() {
        return Long.toString(methodId, 16) + "|"
                + methodName + "|"
                + descriptor + "|access:"
                + Integer.toHexString(accessFlags) + "|"
                + codeStart + "|"
                + codeEnd + "|lines:"
                + lines;
    }

    native void mapMethod(long mid);

    native Class<?>[] getExceptionTypes0(long mid);

    native Object invokeMethod(Object ins, Object... args);

    public static native long findMethod0(ClassLoader cloader, String className, String methodName, String methodSignature);

}
