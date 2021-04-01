/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebsee.invoke.bytecode;


import com.ebsee.invoke.CallSite;
import com.ebsee.invoke.MethodHandle;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Gust
 */
public class LambdaUtil {
    static Class[] primitiveClass; //cant init static , because some type wasnt init
    static String[] primitiveTag = {"S", "C", "B", "I", "F", "Z", "D", "J", "V"};

    public static boolean isPrimitive(String s) {
        for (String i : primitiveTag) {
            if (i.equals(s)) return true;
        }
        return false;
    }



//    /**
//     * 返回c MethodInfo 地址
//     *
//     * @param callsite
//     * @return
//     */
//    public static long getMethodInfoHandle(CallSite callsite) {
//        if (callsite != null && callsite.getTarget() != null) {
//            Method m = callsite.getTarget().getMethod();
//            return findMethod(m.getDeclaringClass().getName(), m.getName(), LambdaUtil.getDescriptor(m));
//        } else {
//            return 0;
//        }
//    }


    public static String getDescriptor(Method m) {
        return m.toString();
    }

    static public String getMethodReturnType(String descriptor) {
        String s = descriptor.substring(descriptor.indexOf(')') + 1);
        if (s.equals("V")) {
            return "void";
        }
        return s;
    }


    public static String[] getMethodPara(String descriptor) {
        List<String> paras = splitSignature(
                descriptor.substring(descriptor.indexOf("(") + 1, descriptor.indexOf(")"))
        );

        return paras.toArray(new String[paras.size()]);
    }

    public static Method findMethod(DynamicClassLoader loader, String className, String methodName, String spec) {
        return null;
    }


    static public String getMethodSignature(Class<?>[] ptypes, Class<?> rtype) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Class<?> c : ptypes) {
            sb.append(getDescriptorByClass(c));
        }
        sb.append(')');
        sb.append(getDescriptorByClass(rtype));
        return sb.toString();
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


    static public Class getClassByDescriptor(String s) {
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
                }
                try {
                    return Class.forName(s);
                } catch (Exception e) {
                }
                throw new RuntimeException("class not found:" + s);
            default:
                try {
                    return Class.forName(s);
                } catch (Exception e) {
                }
                throw new RuntimeException("class not found:" + s);
        }
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

}
