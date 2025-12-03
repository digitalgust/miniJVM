package org.minijvm.activity.bridge;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class JsonPrinter {
    ClassLoader classLoader;

    StringBuilder print(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
            return sb;
        }
        Class clazz = obj.getClass();
        if (sb == null) sb = new StringBuilder();
        if (obj instanceof String) {
            printString((String) obj, sb);
        } else if (isSimpleType(clazz)) {
            printSimpleType(obj, sb);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            printCollection((Collection) obj, sb);
        } else if (clazz.isArray()) {
            printArray(obj, sb);
        } else if (Map.class.isAssignableFrom(clazz)) {
            printMap((Map) obj, sb);
        } else {
            sb.append("{");
            Field[] fields = clazz.getDeclaredFields();
            boolean moreFields = false;
            for (Field f : fields) {
                String fieldName = f.getName();
                Class c = f.getType();

                Method method = getMethodByName(fieldName, clazz);
                if (method != null) {
                    Type pt = method.getGenericReturnType();
                    //System.out.println(method);
                    Class childClazz = method.getReturnType();
                    try {
                        Object re = method.invoke(obj);
                        if (moreFields) sb.append(", ");
                        sb.append('"').append(fieldName).append("\": ");
                        print(re, sb);
                        moreFields = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("[JSON]" + clazz.getName() + " field '" + fieldName + "' getter not found.");
                }
            }
            sb.append("}");
        }
        return sb;
    }

    private StringBuilder printSimpleType(Object s, StringBuilder sb) {
        sb.append(s);
        return sb;
    }

    private StringBuilder printString(String s, StringBuilder sb) {
        sb.append('"');
        String s2 = s;
        s2 = s2.replace("\\", "\\\\");//  \
        s2 = s2.replace("\"", "\\\""); // "
        s2 = s2.replace("\n", "\\n");// newline
        s2 = s2.replace("\r", "\\r");// return
        s2 = s2.replace("\t", "\\t");// tab
        s2 = s2.replace("\b", "\\b");// backspace
        s2 = s2.replace("\f", "\\f");// formfeed
        sb.append(s2);
        sb.append('"');
        return sb;
    }

    private StringBuilder printArray(Object arr, StringBuilder sb) {
        sb.append("[");
        Class componentType = arr.getClass().getComponentType();
        for (int i = 0, imax = Array.getLength(arr); i < imax; i++) {
            Object elem = Array.get(arr, i);
            if (isSimpleType(componentType)) sb.append(elem);
            else print(elem, sb);
            if (i < imax - 1) sb.append(", ");
        }
        sb.append("]");
        return sb;
    }

    private StringBuilder printCollection(Collection collection, StringBuilder sb) {
        sb.append("[");
        boolean moreElem = false;
        for (Iterator it = collection.iterator(); it.hasNext(); ) {
            Object elem = it.next();
            if (moreElem) sb.append(", ");
            print(elem, sb);
            moreElem = true;
        }
        sb.append("]");
        return sb;
    }


    private StringBuilder printMap(Map map, StringBuilder sb) {
        sb.append("{");
        boolean moreElem = false;
        for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            if (moreElem) sb.append(", ");
            print(key, sb);
            sb.append(": ");
            Object value = map.get(key);
            print(value, sb);
            moreElem = true;
        }
        sb.append("}");
        return sb;
    }

    private boolean isSimpleType(Class clazz) {
        if (clazz == Integer.class || clazz == int.class
                || clazz == Long.class || clazz == long.class
                || clazz == Character.class || clazz == char.class
                || clazz == Short.class || clazz == short.class
                || clazz == Byte.class || clazz == byte.class
                || clazz == Float.class || clazz == float.class
                || clazz == Double.class || clazz == double.class
                || clazz == Boolean.class || clazz == boolean.class
        ) {
            return true;
        }
        return false;
    }

    static private Method getMethodByName(String name, Class<?> clazz) {
        String tmp = name.substring(0, 1).toUpperCase() + name.substring(1);
        String mName0 = "get" + tmp;
        String mName1 = "is" + tmp;
        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if (m.getParameterTypes().length == 0) {
                if (m.getName().equals(mName0) || m.getName().equals(mName1)) {
                    return m;
                }
            }
        }
        return null;
    }

    public final String serial(Object obj) {
        if (obj == null) throw new RuntimeException("null can't serialization");
        classLoader = obj.getClass().getClassLoader();
        StringBuilder sb = print(obj, null);
        return sb.toString();
    }

    /**
     * for test
     */

    private static class Foo {
        int[] ia;
        List<Short> list;
        Map<String, List<Byte>> m;
        long t;
        byte b;

        Foo() {
        }

        void init() {
            ia = new int[]{7, 8};
            list = new ArrayList();
            list.add((short) 9);
            list.add((short) 10);

            m = new HashMap<>();
            m.put("U", new ArrayList<>());
            m.get("U").add((byte) 110);
            t = System.currentTimeMillis();
            b = (byte) 120;
        }

        public int[] getIa() {
            return ia;
        }

        public List<Short> getList() {
            return list;
        }

        public Map<String, List<Byte>> getM() {
            return m;
        }

        public long getT() {
            return t;
        }

        public byte getB() {
            return b;
        }

        public void setIa(int[] ia) {
            this.ia = ia;
        }

        public void setList(List<Short> list) {
            this.list = list;
        }

        public void setM(Map<String, List<Byte>> m) {
            this.m = m;
        }

        public void setT(long t) {
            this.t = t;
        }

        public void setB(byte b) {
            this.b = b;
        }
    }

//    static public final void main(String[] args) throws Exception {
//        //serial
//        Foo t = new Foo();
//        t.init();
//        String s = new JsonPrinter().serial(t);
//        System.out.println(s);
//
//        Foo t1 = new JsonParser<Foo>().deserial(s, Foo.class);
//        int debug = 1;
//    }
}
