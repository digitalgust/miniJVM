/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RConst;
import org.mini.reflect.vm.RefNative;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 类方法的反射，以mini jvm中的 MethofInfo的实例内存地址进行初始化 初始化中会把内存中的相应变量反射到ReflectMethod实例中。
 *
 * @author gust
 */
public class ReflectMethod {

    ReflectClass refClass;

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

    public ReflectMethod(ReflectClass c, long mid) {
        if (mid == 0) {
            throw new IllegalArgumentException();
        }
        refClass = c;
        //System.out.println("mid:" + mid);
        this.methodId = mid;
        mapMethod(methodId);
        paras = ReflectClass.splitSignature(
                descriptor.substring(descriptor.indexOf("(") + 1, descriptor.indexOf(")"))
        ).toArray(new String[0]);
        paras_class = getMethodPara(descriptor);
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
        Class[] pc = getParameterTypes();
        if (args.length != paras.length) {
            throw new IllegalArgumentException();
        }
        if ((accessFlags & RConst.ACC_PRIVATE) != 0) {
            throw new IllegalAccessException();
        }
        long[] argslong = new long[paras.length];

        for (int i = 0; i < paras.length; i++) {
            switch (paras[i].charAt(0)) {
                case 'S':
                    argslong[i] = ((Short) args[i]);
                    break;
                case 'C':
                    argslong[i] = ((Character) args[i]);
                    break;
                case 'B':
                    argslong[i] = ((Byte) args[i]);
                    break;
                case 'I':
                    argslong[i] = ((Integer) args[i]);
                    break;
                case 'F':
                    argslong[i] = Float.floatToIntBits(((Float) args[i]));
                    break;
                case 'Z':
                    argslong[i] = ((Boolean) args[i]) ? 1 : 0;
                    break;
                case 'D':
                    argslong[i] = Double.doubleToLongBits(((Double) args[i]));
                    break;
                case 'J':
                    argslong[i] = ((Long) args[i]);
                    break;
                default:
                    argslong[i] = RefNative.obj2id(args[i]);
                    break;
            }
        }
        DataWrap result = invokeMethod(methodId, obj, argslong);//todo result would be gc
        char rtype = descriptor.charAt(descriptor.indexOf(')') + 1);
        switch (rtype) {
            case 'S':
                return ((short) result.nv);
            case 'C':
                return ((char) result.nv);
            case 'B':
                return ((byte) result.nv);
            case 'I':
                return ((int) result.nv);
            case 'F':
                return Float.intBitsToFloat((int) result.nv);
            case 'Z':
                return (result.nv != 0);
            case 'D':
                return Double.longBitsToDouble((long) result.nv);
            case 'J':
                return result.nv;
            default:
                return result.ov;
        }
    }

    public static Class<?>[] getMethodPara(String descriptor) {
        List<String> paras = ReflectClass.splitSignature(
                descriptor.substring(descriptor.indexOf("(") + 1, descriptor.indexOf(")"))
        );
        Class<?>[] paras_class;
        paras_class = new Class[paras.size()];
        for (int i = 0; i < paras.size(); i++) {
            paras_class[i] = ReflectClass.getClassByDescriptor(paras.get(i));
        }
        return paras_class;
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
        return getMethodReturnType(descriptor);
    }

    static public Class<?> getMethodReturnType(String descriptor) {
        String s = descriptor.substring(descriptor.indexOf(')') + 1);
        if (s.equals("V")) {
            return Void.TYPE;
        }
        return ReflectClass.getClassByDescriptor(s);
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

    static public ReflectMethod findMethod(String className, String methodName, String methodSignature) {
        long mid = findMethod0(className, methodName, methodSignature);
        if (mid != 0) {
            ReflectMethod rm = new ReflectMethod(null, mid);
            return rm;
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

    final native void mapMethod(long mid);

    native DataWrap invokeMethod(long mid, Object ins, long[] args_long);

    public static native long findMethod0(String className, String methodName, String methodSignature);

}
