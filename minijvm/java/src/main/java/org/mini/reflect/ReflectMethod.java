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
    public String descriptor;// ex: (Ljava/lang/Class;Ljava/util/Map;)Ljava/lang/annotation/Annotation;
    public String signature;// ex:  <T::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TT;>;Ljava/util/Map<Ljava/lang/String;TT;>;)TT;
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
    public String annotations; // Added for annotation support

    public ReflectMethod(Class c, long mid) {
        if (mid == 0) {
            throw new IllegalArgumentException();
        }
        clazzObj = c;
        //System.out.println("mid:" + mid);
        this.methodId = mid;
        mapMethod(methodId);

        // 使用descriptor解析参数类型（用于getParameterTypes）
        paras = ReflectClass.splitDescriptor(descriptor).toArray(new String[0]);
        paras_class = getMethodPara(clazzObj.getClassLoader(), descriptor);

        // 使用signature解析泛型参数类型（用于getGenericParameterTypes）
        paras_type = paras_class;
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
        return invoke(obj, convertArguments(args), false);
    }

    public void checkReceiver(Object obj) {
        if ((accessFlags & RConst.ACC_STATIC) != 0) return;
        if (obj == null) throw new NullPointerException();
        if (!clazzObj.isInstance(obj)) throw new IllegalArgumentException("instance type mismatch");
    }

    public Object invoke(Object obj, Object[] args, boolean overrideAccess) throws IllegalAccessException {
        checkReceiver(obj);
        if (!overrideAccess && (accessFlags & RConst.ACC_PRIVATE) != 0) throw new IllegalAccessException();
        return invokeMethod(obj, args);
    }

    public Object[] convertArguments(Object[] args) {
        if (args == null) args = new Object[0];
        if (args.length != paras_class.length) throw new IllegalArgumentException("argument count mismatch");
        Object[] result = args.clone();
        for (int i = 0; i < result.length; i++) result[i] = convertArgument(paras_class[i], args[i]);
        return result;
    }

    public static Object convertArgument(Class<?> target, Object value) {
        if (!target.isPrimitive()) {
            if (value != null && !target.isInstance(value)) throw new IllegalArgumentException("argument type mismatch");
            return value;
        }
        if (target == boolean.class && value instanceof Boolean || target == char.class && value instanceof Character) return value;
        int rank = value instanceof Byte ? 1 : value instanceof Short ? 2 : value instanceof Character ? 3
            : value instanceof Integer ? 4 : value instanceof Long ? 5 : value instanceof Float ? 6 : value instanceof Double ? 7 : 0;
        int required = target == byte.class ? 1 : target == short.class ? 2 : target == int.class ? 4
            : target == long.class ? 5 : target == float.class ? 6 : target == double.class ? 7 : 0;
        if (rank == 0 || required == 0 || rank > required) throw new IllegalArgumentException("argument type mismatch");
        Number number = value instanceof Character ? Integer.valueOf(((Character) value).charValue()) : (Number) value;
        if (target == byte.class) return Byte.valueOf(number.byteValue());
        else if (target == short.class) return Short.valueOf(number.shortValue());
        else if (target == int.class) return Integer.valueOf(number.intValue());
        else if (target == long.class) return Long.valueOf(number.longValue());
        else if (target == float.class) return Float.valueOf(number.floatValue());
        else return Double.valueOf(number.doubleValue());
    }


    public static Class<?>[] getMethodPara(ClassLoader loader, String descriptor) {
        List<String> paras = ReflectClass.splitDescriptor(descriptor);
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

        @Override
        public String toString() {
            return name;
        }
    }

    public static Type[] getGenericParaType(String signature) {
        if (signature == null || signature.isEmpty()) {
            return new Type[0];
        }
        List<String> paras = ReflectClass.splitGenericSignature(signature);
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
        return getReturnType(clazzObj.getClassLoader(), descriptor);
    }

    static public Class<?> getReturnType(ClassLoader loader, String descriptor) {
        String s = ReflectClass.getDescriptorReturnType(descriptor);
        if (s == null) {
            return Void.TYPE;
        }
        if (s.equals("V")) {
            return Void.TYPE;
        }
        return ReflectClass.getClassByDescriptor(loader, s);
    }

    public Type getGenericReturnType() {
        return getGenericReturnType(signature == null ? descriptor : signature);
    }

    static public Type getGenericReturnType(String signature) {
        if (signature == null || signature.isEmpty()) {
            TypeMethodImpl t = new TypeMethodImpl();
            t.name = "void";
            return t;
        }
        String s = ReflectClass.getGenericReturnType(signature);
        if (s == null) {
            TypeMethodImpl t = new TypeMethodImpl();
            t.name = "void";
            return t;
        }
        TypeMethodImpl t = new TypeMethodImpl();
        t.name = ReflectClass.getNameByDescriptor(s);
        return t;
    }

    public Type[] getGenericExceptionTypes() {
        Class[] exceptions = getExceptionTypes();
        Type[] types = new Type[exceptions.length];
        for (int i = 0; i < exceptions.length; i++) {
            TypeMethodImpl t = new TypeMethodImpl();
            t.name = ReflectClass.getNameByDescriptor(ReflectClass.getDescriptorByClass(exceptions[i]));
            types[i] = t;
        }
        return types;
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
