package org.minijvm.activity.bridge;

import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class JsonParser<T> {
    private List<SimpleModule> modules = new ArrayList<>();

    private InjectableValues injectableValues = null;

    ClassLoader classLoader;

    public JsonParser() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(List.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                return JsonParser.this.map2obj(p, ArrayList.class, types);
            }
        });
        module.addDeserializer(Set.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                return JsonParser.this.map2obj(p, LinkedHashSet.class, types);
            }
        });
        module.addDeserializer(Map.class, new StdDeserializer(null) {
            @Override
            public Object deserialize(JsonCell p, String types) {
                Map<Object, Object> map = new HashMap<>();
                if (types == null || types.indexOf(',') < 0) {
                    JsonMap<JsonCell, JsonCell> jsonMap = (JsonMap<JsonCell, JsonCell>) p;
                    for (JsonCell key : jsonMap.keySet()) {
                        try {
                            Object K = JsonParser.this.map2obj(key, null, null);
                            Object V = JsonParser.this.map2obj(jsonMap.get(key), null, null);
                            map.put(K, V);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    String qualifier = types.substring(types.indexOf('<') + 1, types.lastIndexOf('>'));
                    int left = qualifier.indexOf('<');
                    int comma = qualifier.indexOf(',');
                    int splitPos = comma;
                    if (left >= 0 && left < comma) {
                        int leftCnt = 1;
                        int i;
                        for (i = left + 1; leftCnt > 0; i++) {
                            char c = qualifier.charAt(i);
                            if (c == '<')
                                leftCnt++;
                            if (c == '>')
                                leftCnt--;
                        }
                        splitPos = i;
                    }
                    String keyClassName = qualifier.substring(0, splitPos).trim();
                    String valueClassName = qualifier.substring(splitPos + 1).trim();
                    String keyType = keyClassName;
                    if (keyType.indexOf('<') >= 0)
                        keyType = keyType.substring(0, keyType.indexOf('<'));
                    String valueType = valueClassName;
                    if (valueType.indexOf('<') >= 0)
                        valueType = valueType.substring(0, valueType.indexOf('<'));
                    keyType = TypeNameConverter.convertTypeNameToClassName(keyType);
                    valueType = TypeNameConverter.convertTypeNameToClassName(valueType);
                    JsonMap<JsonCell, JsonCell> jsonMap = (JsonMap<JsonCell, JsonCell>) p;
                    for (JsonCell key : jsonMap.keySet()) {
                        try {
                            Object K = JsonParser.this.map2obj(key, Class.forName(keyType, true, JsonParser.this.classLoader), keyClassName);
                            if (K instanceof JsonParser.Polymorphic)
                                K = JsonParser.this.map2obj(key, ((Polymorphic) K).getType(), keyClassName);
                            Object V = JsonParser.this.map2obj(jsonMap.get(key), Class.forName(valueType, true, JsonParser.this.classLoader), valueClassName);
                            if (V instanceof JsonParser.Polymorphic)
                                V = JsonParser.this.map2obj(jsonMap.get(key), ((Polymorphic) V).getType(), valueClassName);
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
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static interface Polymorphic {
        Class getType();
    }

    public static interface JsonCell {
        public static final int TYPE_MAP = 0;

        public static final int TYPE_LIST = 1;

        public static final int TYPE_STRING = 2;

        public static final int TYPE_NUMBER = 3;

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
            this.str = s;
        }

        public int getType() {
            return TYPE_STRING;
        }

        public String toString() {
            return this.str;
        }
    }

    public static class JsonNumber implements JsonCell {
        String numStr;

        public JsonNumber(String s) {
            this.numStr = s;
        }

        public int getType() {
            return TYPE_NUMBER;
        }

        public String toString() {
            return this.numStr;
        }

        public float asFloat() {
            return Float.parseFloat(this.numStr);
        }

        public double asDouble() {
            return Double.parseDouble(this.numStr);
        }

        public int asInt() {
            return Integer.parseInt(this.numStr);
        }

        public long asLong() {
            return Long.parseLong(this.numStr);
        }

        public byte asByte() {
            return Byte.parseByte(this.numStr);
        }

        public short asShort() {
            return Short.parseShort(this.numStr);
        }

        public char asChar() {
            return this.numStr.charAt(0);
        }

        public Object getValue(Class<?> clazz) {
            if (clazz == Integer.class || clazz == int.class)
                return Integer.decode(this.numStr);
            if (clazz == Float.class || clazz == float.class)
                return Float.valueOf(this.numStr);
            if (clazz == Long.class || clazz == long.class)
                return Long.decode(this.numStr);
            if (clazz == Double.class || clazz == double.class)
                return Double.valueOf(this.numStr);
            if (clazz == Short.class || clazz == short.class)
                return Short.decode(this.numStr);
            if (clazz == Byte.class || clazz == byte.class)
                return Byte.decode(this.numStr);
            if (clazz == Character.class || clazz == char.class)
                return Character.valueOf(this.numStr.charAt(0));
            if (clazz == Boolean.class || clazz == boolean.class)
                return Boolean.valueOf(this.numStr);
            if (this.numStr.equalsIgnoreCase("true"))
                return Boolean.TRUE;
            if (this.numStr.equalsIgnoreCase("false"))
                return Boolean.FALSE;
            if (this.numStr.equals("null"))
                return null;
            try {
                return Integer.valueOf(this.numStr);
            } catch (Exception exception) {
                try {
                    return Long.valueOf(this.numStr);
                } catch (Exception exception1) {
                    try {
                        return Float.valueOf(this.numStr);
                    } catch (Exception exception2) {
                        try {
                            return Double.valueOf(this.numStr);
                        } catch (Exception exception3) {
                            throw new IllegalArgumentException();
                        }
                    }
                }
            }
        }
    }

    public static abstract class StdDeserializer<T> {
        public StdDeserializer(Class<?> c) {
        }

        public abstract T deserialize(JsonCell param1JsonCell, String param1String);
    }

    public static class SimpleModule {
        Map<Class, StdDeserializer> serializeLib = (Map) new HashMap<>();

        public SimpleModule addDeserializer(Class key, StdDeserializer value) {
            this.serializeLib.put(key, value);
            return this;
        }
    }

    public void registerModule(SimpleModule module) {
        this.modules.add(module);
    }

    private StdDeserializer findDeserializer(Class<?> c) {
        for (SimpleModule module : this.modules) {
            StdDeserializer ds = module.serializeLib.get(c);
            if (ds != null)
                return ds;
        }
        return null;
    }

    public static class InjectableValues {
        Map<Class, Object> values = (Map) new HashMap<>();

        public void addValue(Class c, Object o) {
            this.values.put(c, o);
        }
    }

    public void setInjectableValues(InjectableValues values) {
        this.injectableValues = values;
    }

    private Object findInjectableValues(Class<?> c) {
        if (this.injectableValues == null)
            return null;
        Object ds = this.injectableValues.values.get(c);
        if (ds != null)
            return ds;
        return null;
    }

    private static Method getMethodByName(String name, Class<?> clazz) {
        String mName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(mName) && (m.getParameterTypes()).length == 1)
                return m;
        }
        return null;
    }

    static class ParseToken {
        String source;

        int pos;

        Object parentContainer;

        JsonCell containerKey;

        JsonCell result;

        ParseToken(String source, int pos) {
            this.source = source;
            this.pos = pos;
        }

        ParseToken(String source, int pos, Object parentContainer, JsonCell containerKey) {
            this.source = source;
            this.pos = pos;
            this.parentContainer = parentContainer;
            this.containerKey = containerKey;
        }

        void skipWhitespace() {
            while (this.pos < this.source.length() && Character.isWhitespace(this.source.charAt(this.pos)))
                this.pos++;
        }

        char currentChar() {
            return (this.pos < this.source.length()) ? this.source.charAt(this.pos) : Character.MIN_VALUE;
        }

        boolean isEnd() {
            return (this.pos >= this.source.length());
        }

        void advance() {
            this.pos++;
        }
    }

    private static void parseAndFillDirect(ParseToken token) {
        token.skipWhitespace();
        if (token.isEnd())
            return;
        char ch = token.currentChar();
        if (ch == '{') {
            JsonMap<JsonCell, JsonCell> newMap = new JsonMap<>();
            putIntoContainer(token.parentContainer, token.containerKey, newMap);
            parseMapDirect(token, newMap);
        } else if (ch == '[') {
            JsonList<JsonCell> newList = new JsonList<>();
            putIntoContainer(token.parentContainer, token.containerKey, newList);
            parseListDirect(token, newList);
        } else if (ch == '"') {
            parseStringDirect(token);
            putIntoContainer(token.parentContainer, token.containerKey, token.result);
        } else {
            parseNumberDirect(token);
            putIntoContainer(token.parentContainer, token.containerKey, token.result);
        }
    }

    private static void putIntoContainer(Object container, JsonCell key, JsonCell value) {
        if (container instanceof JsonMap) {
            ((JsonMap<JsonCell, JsonCell>) container).put(key, value);
        } else if (container instanceof JsonList) {
            ((JsonList<JsonCell>) container).add(value);
        }
    }

    private static void parseStringDirect(ParseToken token) {
        StringBuilder sb = new StringBuilder();
        token.advance();
        while (!token.isEnd()) {
            char ch = token.currentChar();
            if (ch == '"') {
                token.advance();
                break;
            }
            if (ch == '\\') {
                token.advance();
                if (!token.isEnd()) {
                    ch = token.currentChar();
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
                        StringBuilder unicode = new StringBuilder();
                        for (int i = 0; i < 4 && !token.isEnd(); i++) {
                            token.advance();
                            if (!token.isEnd())
                                unicode.append(token.currentChar());
                        }
                        sb.append((char) Integer.parseInt(unicode.toString(), 16));
                    } else {
                        sb.append(ch);
                    }
                }
            } else {
                sb.append(ch);
            }
            token.advance();
        }
        token.result = new JsonString(sb.toString());
    }

    private static void parseNumberDirect(ParseToken token) {
        StringBuilder sb = new StringBuilder();
        while (!token.isEnd()) {
            char ch = token.currentChar();
            if (ch == ',' || ch == '}' || ch == ']' || Character.isWhitespace(ch))
                break;
            sb.append(ch);
            token.advance();
        }
        token.result = new JsonNumber(sb.toString());
    }

    private static void parseMapDirect(ParseToken token, JsonMap<JsonCell, JsonCell> targetMap) {
        token.advance();
        while (!token.isEnd()) {
            token.skipWhitespace();
            if (token.isEnd() || token.currentChar() == '}') {
                if (!token.isEnd())
                    token.advance();
                break;
            }
            parseStringDirect(token);
            JsonCell key = token.result;
            token.skipWhitespace();
            if (!token.isEnd() && token.currentChar() == ':')
                token.advance();
            token.skipWhitespace();
            Object tempParentContainer = token.parentContainer;
            JsonCell tempContainerKey = token.containerKey;
            token.parentContainer = targetMap;
            token.containerKey = key;
            parseAndFillDirect(token);
            token.parentContainer = tempParentContainer;
            token.containerKey = tempContainerKey;
            token.skipWhitespace();
            if (!token.isEnd() && token.currentChar() == ',')
                token.advance();
            token.skipWhitespace();
        }
    }

    private static void parseListDirect(ParseToken token, JsonList<JsonCell> targetList) {
        token.advance();
        while (!token.isEnd()) {
            token.skipWhitespace();
            if (token.isEnd() || token.currentChar() == ']') {
                if (!token.isEnd())
                    token.advance();
                break;
            }
            Object tempParentContainer = token.parentContainer;
            JsonCell tempContainerKey = token.containerKey;
            token.parentContainer = targetList;
            token.containerKey = null;
            parseAndFillDirect(token);
            token.parentContainer = tempParentContainer;
            token.containerKey = tempContainerKey;
            token.skipWhitespace();
            if (!token.isEnd() && token.currentChar() == ',')
                token.advance();
            token.skipWhitespace();
        }
    }

    public static final JsonCell parse(String s, int pos) {
        try {
            if (s.length() == 0)
                return null;
            ParseToken token = new ParseToken(s.trim(), 0);
            char ch = token.currentChar();
            if (ch == '{') {
                JsonMap<JsonCell, JsonCell> rootMap = new JsonMap<>();
                parseMapDirect(token, rootMap);
                return rootMap;
            }
            if (ch == '[') {
                JsonList<JsonCell> rootList = new JsonList<>();
                parseListDirect(token, rootList);
                return rootList;
            }
            if (ch == '"') {
                parseStringDirect(token);
                return token.result;
            }
            parseNumberDirect(token);
            return token.result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public Object map2obj(JsonCell json, Class<?> clazz, String types) {
        if (json == null)
            return null;
        try {
            JsonMap<JsonCell, JsonCell> map;
            JsonList<JsonCell> list;
            Object ins;
            Class<?> clazzQualified;
            Field[] fields;
            String typevar;
            Collection<Object> collection;
            if (clazz == null && types == null && json.getType() == 0)
                clazz = Map.class;
            StdDeserializer<?> deser = findDeserializer(clazz);
            if (deser != null)
                return deser.deserialize(json, types);
            switch (json.getType()) {
                case JsonCell.TYPE_MAP:
                    map = (JsonMap<JsonCell, JsonCell>) json;
                    ins = findInjectableValues(clazz);
                    if (ins == null)
                        ins = clazz.newInstance();
                    fields = clazz.getFields();
                    for (Field f : fields) {
                        Class<?> c = f.getType();
                        Object o = findInjectableValues(c);
                        if (o != null)
                            f.set(ins, o);
                    }
                    for (JsonCell jc : map.keySet()) {
                        if (jc.getType() != 2)
                            throw new RuntimeException("[JSON]error: field name need quotation : " + jc.toString());
                        String fieldName = ((JsonString) jc).toString();
                        JsonCell childJson = map.get(jc);
                        Method method = getMethodByName(fieldName, clazz);
                        if (method != null) {
                            Type[] pt = method.getGenericParameterTypes();
                            Class<?> childClazz = method.getParameterTypes()[0];
                            try {
                                method.invoke(ins, new Object[]{map2obj(childJson, childClazz, pt[0].getTypeName())});
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            continue;
                        }
                        if (!(ins instanceof Polymorphic))
                            System.out.println("[JSON]" + clazz.getName() + " field '" + fieldName + "' setter not found.");
                    }
                    return ins;
                case JsonCell.TYPE_LIST:
                    if (types == null)
                        System.out.println("[JSON] need type declare , class:" + clazz);
                    if (clazz == null)
                        clazz = ArrayList.class;
                    list = (JsonList<JsonCell>) json;
                    if (clazz.isArray()) {
                        String str = types;
                        if (str != null && str.indexOf('[') >= 0)
                            str = str.substring(1);
                        Object array = Array.newInstance(clazz.getComponentType(), list.size());
                        int i = 0;
                        for (JsonCell cell : list) {
                            Object object = map2obj(cell, clazz.getComponentType(), str);
                            Array.set(array, i, object);
                            i++;
                        }
                        return array;
                    }
                    clazzQualified = null;
                    typevar = null;
                    if (types != null) {
                        typevar = types;
                        if (typevar.indexOf('<') > 0)
                            typevar = typevar.substring(typevar.indexOf('<') + 1, typevar.length() - 1);
                        String className = typevar;
                        if (className.indexOf('<') >= 0)
                            className = className.substring(0, className.indexOf('<'));
                        clazzQualified = Class.forName(className, true, this.classLoader);
                    }
                    collection = (Collection) clazz.newInstance();
                    for (JsonCell cell : list) {
                        Object object = map2obj(cell, clazzQualified, typevar);
                        if (object instanceof Polymorphic)
                            object = map2obj(cell, ((Polymorphic) object).getType(), typevar);
                        collection.add(object);
                    }
                    return collection;
                case JsonCell.TYPE_STRING:
                    return ((JsonString) json).str;
                case JsonCell.TYPE_NUMBER:
                    return ((JsonNumber) json).getValue(clazz);
            }
        } catch (Exception e) {
            System.err.println("error on parse " + clazz + " , str:" + json.toString());
            e.printStackTrace();
        }
        return null;
    }

    public final T deserial(String s, Class clazz) {
        return deserial(s, clazz, null);
    }

    public final T deserial(String s, Class clazz, ClassLoader classLoader) {
        return deserial(s, clazz, classLoader, null);
    }

    public final T deserial(String s, Class<?> clazz, ClassLoader classLoader, String types) {
        if (clazz == null)
            throw new RuntimeException("Class can't null");
        if (classLoader == null)
            classLoader = Thread.currentThread().getContextClassLoader();
        this.classLoader = classLoader;
        JsonCell json = parse(s, 0);
        return (T) map2obj(json, clazz, types);
    }

    public static final boolean isJsonString(String str) {
        if (str == null)
            return true;
        return (str.startsWith("{") || str.startsWith("["));
    }

    public static class TypeNameConverter {
        public static String convertTypeNameToClassName(String typeName) {
            if (typeName.endsWith("[]")) {
                int arrayDimension = 0;
                while (typeName.endsWith("[]")) {
                    typeName = typeName.substring(0, typeName.length() - 2);
                    arrayDimension++;
                }
                if (isPrimitiveType(typeName))
                    return (new String(new char[arrayDimension])).replace("\000", "[") + primitiveTypeToInternalForm(typeName);
                return (new String(new char[arrayDimension])).replace("\000", "[") + "L" + typeName + ";";
            }
            if (isPrimitiveType(typeName))
                return primitiveTypeToInternalForm(typeName);
            return typeName;
        }

        private static boolean isPrimitiveType(String typeName) {
            return (typeName.equals("boolean") || typeName.equals("byte") || typeName
                    .equals("char") || typeName.equals("short") || typeName
                    .equals("int") || typeName.equals("long") || typeName
                    .equals("float") || typeName.equals("double"));
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
            }
            throw new IllegalArgumentException("Invalid primitive type: " + typeName);
        }
    }
}
