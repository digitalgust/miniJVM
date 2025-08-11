package org.mini.json;

import org.mini.util.SysLog;

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
        module.addDeserializer(List.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                return map2obj(p, ArrayList.class, types);
            }
        });
        module.addDeserializer(Set.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                return map2obj(p, LinkedHashSet.class, types);
            }
        });
        module.addDeserializer(Map.class, new StdDeserializer(null) {

            @Override
            public Object deserialize(JsonCell p, String types) {
                Map map = new HashMap();

                if (types == null || types.indexOf(',') < 0) {
                    JsonMap<JsonCell, JsonCell> jsonMap = (JsonMap) p;
                    for (JsonCell key : jsonMap.keySet()) {
                        try {
                            Object K = map2obj(key, null, null);
                            Object V = map2obj(jsonMap.get(key), null, null);
                            map.put(K, V);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    String qualifier = types.substring(types.indexOf('<') + 1, types.lastIndexOf('>'));
                    //resolve key value qualifier
                    int left = qualifier.indexOf('<');
                    int comma = qualifier.indexOf(',');
                    int splitPos = comma;
                    if (left >= 0 && left < comma) { //the left type contains '<'
                        int leftCnt = 1;
                        int i;
                        //find matched right '>'
                        for (i = left + 1; leftCnt > 0; i++) {
                            ;//start from '<', end with '>'
                            char c = qualifier.charAt(i);
                            if (c == '<') leftCnt++;
                            if (c == '>') leftCnt--;
                        }
                        splitPos = i;
                    }
                    String keyClassName = qualifier.substring(0, splitPos).trim();
                    String valueClassName = qualifier.substring(splitPos + 1).trim();

                    String keyType = keyClassName;
                    if (keyType.indexOf('<') >= 0) {
                        keyType = keyType.substring(0, keyType.indexOf('<'));
                    }
                    String valueType = valueClassName;
                    if (valueType.indexOf('<') >= 0) {
                        valueType = valueType.substring(0, valueType.indexOf('<'));
                    }
                    keyType = TypeNameConverter.convertTypeNameToClassName(keyType);
                    valueType = TypeNameConverter.convertTypeNameToClassName(valueType);
                    JsonMap<JsonCell, JsonCell> jsonMap = (JsonMap) p;
                    for (JsonCell key : jsonMap.keySet()) {
                        try {
                            Object K = map2obj(key, Class.forName(keyType, true, classLoader), keyClassName);
                            if (K instanceof Polymorphic) {
                                K = map2obj(key, ((Polymorphic) K).getType(), keyClassName);
                            }
                            Object V = map2obj(jsonMap.get(key), Class.forName(valueType, true, classLoader), valueClassName);
                            if (V instanceof Polymorphic) {
                                V = map2obj(jsonMap.get(key), ((Polymorphic) V).getType(), valueClassName);
                            }
                            map.put(K, V);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
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

    public static class JsonNumber implements JsonCell { //include null too
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
                //guess
                if (numStr.equalsIgnoreCase("true")) {
                    return Boolean.TRUE;
                }
                if (numStr.equalsIgnoreCase("false")) {
                    return Boolean.FALSE;
                }
                if (numStr.equals("null")) {
                    return null;
                }
                try {
                    return Integer.valueOf(numStr);
                } catch (Exception e) {
                }
                try {
                    return Long.valueOf(numStr);
                } catch (Exception e) {
                }
                try {
                    return Float.valueOf(numStr);
                } catch (Exception e) {
                }
                try {
                    return Double.valueOf(numStr);
                } catch (Exception e) {
                }
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
                        if (ch == 'r') {
                            sb.append('\r');
                        } else if (ch == 'n') {
                            sb.append('\n');
                        } else if (ch == 't') {
                            sb.append('\t');
                        } else if (ch == 'b') {
                            sb.append('\b');
                        } else if (ch == 'f') {
                            sb.append('\f');
                        } else if (ch == 'u') {
                            StringBuilder sb2 = new StringBuilder();
                            for (int i = 0; i < 4; i++) {
                                index++;
                                sb2.append(s.charAt(index));
                            }
                            sb.append((char) Integer.parseInt(sb2.toString(), 16));
                        } else {
                            sb.append(ch);
                        }
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
                    if (ch == '\"') {//string started
                        int slash = 0;//
                        for (int j = index - 1; j >= 0; j--) {
                            if (s.charAt(j) != '\\') {// process "abc\\\"def"
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
                        if (nextch == ',' || nextch == ':' || nextch == ']' || nextch == '}') {
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
            JsonMap<JsonCell, JsonCell> map = new JsonMap();
            while (true) {
                //name
                pos = getNext(s, pos, sb);
                if (sb.length() == 0) {
                    break;
                }
//                if (sb.charAt(0) != '"') {
//                    throw new RuntimeException("[JSON]error: field name need quotation : " + s);
//                }
                String name = sb.toString();//sb.substring(1, sb.length() - 1);
                JsonCell cell = parse(name, 0);
                //:
                pos = getNext(s, pos, sb);
                //value
                pos = getNext(s, pos, sb);
                String value = sb.toString();
                map.put(cell, parse(value, 0));
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
            if (clazz == null && types == null && json.getType() == JsonCell.TYPE_MAP) {
                clazz = Map.class;
            }
            StdDeserializer<?> deser = findDeserializer(clazz);
            if (deser != null) {  // process list map set
                return deser.deserialize(json, types);
            }
            //other class
            switch (json.getType()) {
                case JsonCell.TYPE_MAP: {
                    JsonMap<JsonCell, JsonCell> map = (JsonMap<JsonCell, JsonCell>) json;
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
                    for (JsonCell jc : map.keySet()) {
                        if (jc.getType() != JsonCell.TYPE_STRING) {
                            throw new RuntimeException("[JSON]error: field name need quotation : " + jc.toString());
                        }
                        String fieldName = ((JsonString) jc).toString();
                        JsonCell childJson = map.get(jc);
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
                                SysLog.warn("[JSON]" + clazz.getName() + " field '" + fieldName + "' setter not found.");
                            }
                        }
                    }
                    return ins;
                }
                case JsonCell.TYPE_LIST: {

                    if (types == null) {
                        SysLog.warn("[JSON] need type declare , class:" + clazz);
                    }

                    if (clazz == null) {
                        clazz = ArrayList.class;
                    }

                    JsonList<JsonCell> list = (JsonList) json;
                    if (clazz.isArray()) {
                        String typevar = types;
                        if (typevar != null && typevar.indexOf('[') >= 0) {
                            typevar = typevar.substring(1);
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
                        Class clazzQualified = null;
                        String typevar = null;
                        if (types != null) {
                            typevar = types;
                            if (typevar.indexOf('<') > 0) {
                                typevar = typevar.substring(typevar.indexOf('<') + 1, typevar.length() - 1);
                            }
                            String className = typevar;
                            if (className.indexOf('<') >= 0) {
                                className = className.substring(0, className.indexOf('<'));
                            }
                            clazzQualified = Class.forName(className, true, classLoader);
                        }
                        Collection collection = (Collection) clazz.newInstance();
                        for (JsonCell cell : list) {
                            Object ins = map2obj(cell, clazzQualified, typevar);
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
            System.err.println("error on parse " + clazz + " , str:" + json.toString());
            e.printStackTrace();
        }
        return null;
    }


    public final T deserial(String s, Class clazz) {
        return (T) deserial(s, clazz, null);
    }

    public final T deserial(String s, Class clazz, ClassLoader classLoader) {
        return (T) deserial(s, clazz, classLoader, null);
    }

    public final T deserial(String s, Class clazz, ClassLoader classLoader, String types) {
        if (clazz == null) throw new RuntimeException("Class can't null");
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        this.classLoader = classLoader;
        JsonCell json = parse(s, 0);
        return (T) map2obj(json, clazz, types);
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


    public static class TypeNameConverter {

        /**
         * 将Java类型名称转换为其对应的类名表示形式。
         * "java.lang.String"  -> "java.lang.String"
         * "java.lang.String[]" -> "[Ljava.lang.String;"
         * "int[][]" -> "[[I"
         *
         * @param typeName 类型名称，如 "java.lang.String" 或 "java.lang.String[]" 或 "int[][]"
         * @return 转换后的类名，如 "java.lang.String" 对于数组类型，或者 "[Ljava.lang.String;" 对于非数组类型
         */
        public static String convertTypeNameToClassName(String typeName) {
            if (typeName.endsWith("[]")) {
                // 如果是数组类型，则转换为类名表示形式
                int arrayDimension = 0;
                while (typeName.endsWith("[]")) {
                    typeName = typeName.substring(0, typeName.length() - 2);
                    arrayDimension++;
                }
                if (isPrimitiveType(typeName)) {
                    // 如果是基本类型，使用JVM的内部表示
                    return new String(new char[arrayDimension]).replace("\0", "[") + primitiveTypeToInternalForm(typeName);
                } else {
                    // 如果是引用类型，使用标准的类名表示
                    return new String(new char[arrayDimension]).replace("\0", "[") + "L" + typeName + ";";
                }
            } else {
                // 如果是非数组类型，直接返回或转换基本类型
                if (isPrimitiveType(typeName)) {
                    return primitiveTypeToInternalForm(typeName);
                } else {
                    return typeName;
                }
            }
        }

        private static boolean isPrimitiveType(String typeName) {
            return typeName.equals("boolean") || typeName.equals("byte") ||
                    typeName.equals("char") || typeName.equals("short") ||
                    typeName.equals("int") || typeName.equals("long") ||
                    typeName.equals("float") || typeName.equals("double");
        }

        private static String primitiveTypeToInternalForm(String typeName) {
            switch (typeName) {
                case "boolean":
                    return "Z";
                case "byte":
                    return "B";
                case "char":
                    return "C";
                case "short":
                    return "S";
                case "int":
                    return "I";
                case "long":
                    return "J";
                case "float":
                    return "F";
                case "double":
                    return "D";
                default:
                    throw new IllegalArgumentException("Invalid primitive type: " + typeName);
            }
        }

    }
}

