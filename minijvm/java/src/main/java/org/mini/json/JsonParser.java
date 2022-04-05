package org.mini.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * same as jackson jason
 *
 * @param <T>
 */
public class JsonParser<T> {

    private List<SimpleModule> modules = new ArrayList();
    private InjectableValues injectableValues = null;
    ClassLoader classLoader;

    public JsonParser() {

        SimpleModule module = new SimpleModule();
        module.addDeserializer(java.util.List.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                return map2obj(p, ArrayList.class, types);
            }
        });
        module.addDeserializer(java.util.Set.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                return map2obj(p, LinkedHashSet.class, types);
            }
        });
        module.addDeserializer(java.util.Map.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                Map map = new HashMap();

                String childType = types;
                childType = childType.substring(childType.indexOf(',') + 1, childType.length() - 1).trim();
                String className = childType;
                if (className.indexOf('<') >= 0) {
                    className = className.substring(0, className.indexOf('<'));
                }
                JsonMap<String, JsonCell> jsonMap = (JsonMap) p;
                for (String key : jsonMap.keySet()) {
                    try {
                        Object ins = map2obj(jsonMap.get(key), Class.forName(className, true, classLoader), childType);
                        if (ins instanceof Polymorphic) {
                            ins = map2obj(jsonMap.get(key), ((Polymorphic) ins).getType(), childType);
                        }
                        map.put(key, ins);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return map;
            }
        });

        registerModule(module);
    }


    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 多态实现
     */
    public interface Polymorphic {
        Class getType();
    }

    public interface JsonCell {
        int TYPE_MAP = 0;
        int TYPE_LIST = 1;
        int TYPE_STRING = 2;
        int TYPE_NUMBER = 3;

        int getType();
    }

    public static class JsonMap<K, V> extends HashMap<K, V> implements JsonCell {
        public int getType() {
            return TYPE_MAP;
        }
    }

    public static class JsonList<T> extends ArrayList<T> implements JsonCell {
        public int getType() {
            return TYPE_LIST;
        }
    }

    public static class JsonString implements JsonCell {
        String str;

        public JsonString(String s) {
            str = s;
        }

        public int getType() {
            return TYPE_STRING;
        }

        public String toString() {
            return str;
        }
    }

    public static class JsonNumber implements JsonCell {
        String numStr;

        public JsonNumber(String s) {
            numStr = s;
        }

        public int getType() {
            return TYPE_NUMBER;
        }

        public String toString() {
            return numStr;
        }

        public float asFloat() {
            return Float.parseFloat(numStr);
        }

        public double asDouble() {
            return Double.parseDouble(numStr);
        }

        public int asInt() {
            return Integer.parseInt(numStr);
        }

        public long asLong() {
            return Long.parseLong(numStr);
        }

        public byte asByte() {
            return Byte.parseByte(numStr);
        }

        public short asShort() {
            return Short.parseShort(numStr);
        }

        public char asChar() {
            return numStr.charAt(0);
        }

        public Object getValue(Class<?> clazz) {
            if (clazz == Integer.class || clazz == int.class) {
                return Integer.decode(numStr);
            } else if (clazz == Float.class || clazz == float.class) {
                return Float.valueOf(numStr);
            } else if (clazz == Long.class || clazz == long.class) {
                return Long.decode(numStr);
            } else if (clazz == Double.class || clazz == double.class) {
                return Double.valueOf(numStr);
            } else if (clazz == Short.class || clazz == short.class) {
                return Short.decode(numStr);
            } else if (clazz == Byte.class || clazz == byte.class) {
                return Byte.decode(numStr);
            } else if (clazz == Character.class || clazz == char.class) {
                return Character.valueOf(numStr.charAt(0));
            } else if (clazz == Boolean.class || clazz == boolean.class) {
                return Boolean.valueOf(numStr);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    public abstract static class StdDeserializer<T> {
        public StdDeserializer(Class<?> c) {

        }

        abstract public T deserialize(JsonCell p, String types);
    }

    public static class SimpleModule {
        Map<Class, StdDeserializer> serializeLib = new HashMap<>();

        public SimpleModule addDeserializer(Class key, StdDeserializer value) {
            serializeLib.put(key, value);
            return this;
        }
    }


    public void registerModule(SimpleModule module) {
        modules.add(module);
    }

    private StdDeserializer findDeserializer(Class<?> c) {
        for (SimpleModule module : modules) {
            StdDeserializer ds = module.serializeLib.get(c);
            if (ds != null) {
                return ds;
            }
        }
        return null;
    }


    public static class InjectableValues {
        Map<Class, Object> values = new HashMap<>();

        public void addValue(Class c, Object o) {
            values.put(c, o);
        }
    }

    public void setInjectableValues(InjectableValues values) {
        injectableValues = values;
    }


    private Object findInjectableValues(Class<?> c) {
        if (injectableValues == null) return null;
        Object ds = injectableValues.values.get(c);
        if (ds != null) {
            return ds;
        }
        return null;
    }


    static private int getNext(String s, int index, StringBuilder sb) {
        sb.setLength(0);
        int len = s.length();
        while (index < len) {
            char ch = s.charAt(index);
            if (ch == '\"') {//string
                sb.append(ch);
                index++;
                while ((ch = s.charAt(index)) != '\"') {//string end
                    if (ch != '\\') {//
                        sb.append(ch);
                    } else {
                        index++;
                        ch = s.charAt(index);
                        sb.append(ch);
                    }
                    index++;
                }
                sb.append(ch);
                index++;
                break;
            } else if (ch == '{') {
                boolean inQuot = false;//if brace in string
                int braceCnt = 0;//
                while (!((ch = s.charAt(index)) == '}' && braceCnt == 1 && !inQuot)) {// {"ab}cd"}
                    if (ch == '\"') {//
                        int slash = 0;//
                        for (int j = index - 1; j >= 0; j--) {
                            if (s.charAt(j) != '\\') {
                                break;
                            }
                            slash++;
                        }
                        if (slash % 2 == 0) {//
                            inQuot = !inQuot;
                        }
                    }
                    //
                    if (!inQuot) {
                        if (ch == '{') {
                            braceCnt++;
                        } else if (ch == '}') {
                            braceCnt--;
                        }
                    }
                    sb.append(ch);
                    index++;
                }
                sb.append(ch);
                index++;
                break;
            } else if (ch == '[') {
                boolean inQuot = false;//
                int bracketCnt = 0;//
                while (!((ch = s.charAt(index)) == ']' && bracketCnt == 1 && !inQuot)) {//
                    if (ch == '\"') {//
                        int slash = 0;//
                        for (int j = index - 1; j >= 0; j--) {
                            if (s.charAt(j) != '\\') {
                                break;
                            }
                            slash++;
                        }
                        if (slash % 2 == 0) {//
                            inQuot = !inQuot;
                        }
                    }
                    //
                    if (!inQuot) {
                        if (ch == '[') {
                            bracketCnt++;
                        } else if (ch == ']') {
                            bracketCnt--;
                        }
                    }
                    sb.append(ch);
                    index++;
                }
                sb.append(ch);
                index++;
                break;
            } else if (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t') {
                index++;
            } else if (ch == ':') {
                sb.append(ch);
                index++;
                break;
            } else if (ch == ',') {
                sb.append(ch);
                index++;
                break;
            } else if (ch == '}' || ch == ']') {
                break;
            } else {
                while (index < len) {
                    ch = s.charAt(index);
                    sb.append(ch);
                    if (index + 1 < len) {
                        char nextch = s.charAt(index + 1);
                        if (nextch == ',' || nextch == ']' || nextch == '}') {
                            break;
                        }
                    }
                    index++;
                }
                index++;
                break;
            }
        }
        //System.out.println("parse:" + sb.toString());
        return index;
    }


    static private JsonMap parseMap(String s, int pos) {
        if (s == null || s.charAt(0) != '{') {
            return null;
        } else {
            pos++;
            StringBuilder sb = new StringBuilder();
            JsonMap<String, JsonCell> map = new JsonMap();
            while (true) {
                //name
                pos = getNext(s, pos, sb);
                if (sb.length() == 0) {
                    break;
                }
                if (sb.charAt(0) != '"') {
                    throw new RuntimeException("[JSON]error: field name need quotation : " + s);
                }
                String name = sb.substring(1, sb.length() - 1);
                //:
                pos = getNext(s, pos, sb);
                //value
                pos = getNext(s, pos, sb);
                String value = sb.toString();
                map.put(name, parse(value, 0));
                //,
                pos = getNext(s, pos, sb);
                if (sb.length() == 0) {
                    break;
                }

            }
            return map;
        }
    }

    static private JsonList parseList(String s, int pos) {
        if (s == null || s.charAt(0) != '[') {
            return null;
        } else {
            pos++;
            StringBuilder sb = new StringBuilder();
            JsonList<JsonCell> list = new JsonList();
            while (true) {
                //value
                pos = getNext(s, pos, sb);
                if (sb.length() == 0) {
                    break;
                }
                String value = sb.toString();
                list.add(parse(value, 0));
                //,
                pos = getNext(s, pos, sb);
                if (sb.length() == 0) {
                    break;
                }
            }
            return list;
        }

    }

    static private Method getMethodByName(String name, Class<?> clazz) {
        String mName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(mName) && m.getParameterTypes().length == 1) {
                return m;
            }
        }
        return null;
    }

    /**
     * @param s
     * @return
     */
    static public final JsonCell parse(String s, int pos) {
        try {
            if (s.length() == 0) {
                return null;
            }
            s = s.trim();
            char ch = s.charAt(0);
            if (ch == '{') {
                return parseMap(s, pos);
            } else if (ch == '[') {
                return parseList(s, pos);
            } else if (ch == '"') {
                return new JsonString(s.substring(1, s.length() - 1));
            } else {
                return new JsonNumber(s);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public Object map2obj(JsonCell json, Class<?> clazz, String types) {
        if (json == null) {
            return null;
        }
        try {
            StdDeserializer<?> deser = findDeserializer(clazz);
            if (deser != null) {
                return deser.deserialize(json, types);
            }
            switch (json.getType()) {
                case JsonCell.TYPE_MAP: {
                    JsonMap<String, JsonCell> map = (JsonMap<String, JsonCell>) json;
                    Object ins = findInjectableValues(clazz);//get default value
                    if (ins == null) ins = clazz.newInstance();
                    Field[] fields = clazz.getFields();
                    for (Field f : fields) {
                        Class c = f.getType();
                        Object o = findInjectableValues(c);
                        if (o != null) {
                            f.set(ins, o);
                        }
                    }
                    for (String fieldName : map.keySet()) {
                        JsonCell childJson = map.get(fieldName);
//                        if (fieldName.equals("COLOR_0")) {
//                            int debug = 1;
//                        }
                        Method method = getMethodByName(fieldName, clazz);
                        if (method != null) {
                            Type[] pt = method.getGenericParameterTypes();
                            //System.out.println(method);
                            Class childClazz = method.getParameterTypes()[0];
                            try {
                                method.invoke(ins, map2obj(childJson, childClazz, pt[0].getTypeName()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            if (!(ins instanceof Polymorphic)) {
                                System.out.println("[JSON]warn :" + clazz.getName() + " field '" + fieldName + "' setter not found.");
                            }
                        }
                    }
                    return ins;
                }
                case JsonCell.TYPE_LIST: {
                    if (types == null) {
                        throw new RuntimeException("[JSON]error: need type declare , class:" + clazz);
                    }

                    JsonList<JsonCell> list = (JsonList) json;
                    if (clazz.isArray()) {
                        String typevar = types;
                        if (typevar.indexOf('[') > 0) {
                            typevar = typevar.substring(0, typevar.lastIndexOf('['));
                        }

                        Object array = Array.newInstance(clazz.getComponentType(), list.size());
                        int i = 0;
                        for (JsonCell cell : list) {
                            Object ins = map2obj(cell, clazz.getComponentType(), typevar);
                            Array.set(array, i, ins);
                            i++;
                        }
                        return array;
                    } else {
                        String typevar = types;
                        if (typevar.indexOf('<') > 0) {
                            typevar = typevar.substring(typevar.indexOf('<') + 1, typevar.length() - 1);
                        }
                        String className = typevar;
                        if (className.indexOf('<') >= 0) {
                            className = className.substring(0, className.indexOf('<'));
                        }
                        Collection collection = (Collection) clazz.newInstance();
                        for (JsonCell cell : list) {
                            Object ins = map2obj(cell, Class.forName(className, true, classLoader), typevar);
                            if (ins instanceof Polymorphic) {
                                ins = map2obj(cell, ((Polymorphic) ins).getType(), typevar);
                            }
                            collection.add(ins);
                        }
                        return collection;
                    }
                }
                case JsonCell.TYPE_STRING: {
                    return ((JsonString) json).str;
                }
                case JsonCell.TYPE_NUMBER: {
                    return ((JsonNumber) json).getValue(clazz);
                }

            }
        } catch (Exception e) {
            System.err.println("error on parse " + clazz.toString() + " , str:" + json.toString());
            e.printStackTrace();
        }
        return null;
    }


    public final T deserial(String s, Class clazz) {
        if (clazz == null) throw new RuntimeException("Class can't null");
        classLoader = clazz.getClassLoader();
        JsonCell json = parse(s, 0);
        return (T) map2obj(json, clazz, null);
    }

    /**
     * @param str
     * @return true=
     */
    static public final boolean isJsonString(String str) {
        if (str == null) {
            return true;
        }
        return (str.startsWith("{") || str.startsWith("["));
    }


//    static public final void main(String[] args) throws Exception {
//        //serial
//        FileInputStream fis = new FileInputStream("a.json");
//        byte[] fb = new byte[fis.available()];
//        fis.read(fb, 0, fb.length);
//        fis.close();
//        String s = new String(fb, "utf-8");
//        Object obj = new JsonParser().parse(s, 0);
//
//
//        System.out.println(obj);
//    }
}

