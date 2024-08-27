package org.minijvm.activity.bridge;


import org.minijvm.activity.JvmNativeActivity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过json串反射调用java方法
 */
public class ReflectUtil {


    public static RMCDescriptor fromJson(String jsonStr) {
        if (jsonStr != null) {
            JsonParser<RMCDescriptor> parser = new JsonParser<>();
            return parser.deserial(jsonStr, RMCDescriptor.class);
        }
        return null;
    }

    private static class TokenOfString {
        public int pos;
    }

    //Ljava/util/List<Ljava/lang/Object;>;BII
    private static List<String> splitSignature(String signature) {
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


    private static Class getClassByDescriptor(ClassLoader loader, String s) {
        s = s.replace('/', '.');
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


    private static Class<?>[] getMethodPara(ClassLoader loader, String descriptor) {
        List<String> paras = splitSignature(
                descriptor.substring(descriptor.indexOf("(") + 1, descriptor.indexOf(")"))
        );
        Class<?>[] paras_class;
        paras_class = new Class[paras.size()];
        for (int i = 0; i < paras.size(); i++) {
            paras_class[i] = getClassByDescriptor(loader, paras.get(i));
        }
        return paras_class;
    }


    static private Object invokeImpl(Class c, String name, Class[] types, Object[] javaPara, Object instance) {
        try {


            Method m = c.getMethod(name, types);
            if (m != null) {
                Object ret = m.invoke(instance, javaPara);
                return ret;
            } else {
                throw new RuntimeException("Method not found:" + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object invokeJava(String className, String methodDesc, String paraJson, String insJson, JvmNativeActivity activity) {
        Class c = null;
        Object ins = null;
        Object[] para = null;
        try {
            c = Class.forName(className);

            String name = methodDesc.substring(0, methodDesc.indexOf('('));
            String desc = methodDesc.substring(methodDesc.indexOf('('));
            Class[] types = getMethodPara(c.getClassLoader(), desc);
            if (insJson != null && insJson.length() > 0) {
                JsonParser parser = new JsonParser();
                ins = parser.deserial(insJson, c);
            } else if (className.equalsIgnoreCase(activity.getClass().getName())) {
                ins = activity;
            }
            if (paraJson != null && paraJson.length() > 0) {
                JsonParser parser = new JsonParser();
                para = (Object[]) parser.deserial(paraJson, Object[].class);
            }

            return invokeImpl(c, name, types, para, ins);
        } catch (Exception e) {
            return null;
        }
    }

}
