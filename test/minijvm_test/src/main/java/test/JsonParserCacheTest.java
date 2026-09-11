package test;

import org.mini.json.JsonParser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/** Regression test: JsonParser's reflection cache must not pin app classes. */
public class JsonParserCacheTest {

    public static class Bean {
        public int value;
    }

    public static void main(String[] args) throws Exception {
        Field cacheField = JsonParser.class.getDeclaredField("binderCache");
        if (Modifier.isStatic(cacheField.getModifiers())) {
            throw new AssertionError("JsonParser binder cache must not be static");
        }
        cacheField.setAccessible(true);

        JsonParser<Bean> first = new JsonParser<>();
        Bean bean = first.deserial("{\"value\":123}", Bean.class);
        if (bean == null || bean.value != 123) {
            throw new AssertionError("JsonParser deserialization failed");
        }

        Map firstCache = (Map) cacheField.get(first);
        JsonParser<Bean> second = new JsonParser<>();
        Map secondCache = (Map) cacheField.get(second);
        if (firstCache == secondCache || firstCache.isEmpty() || !secondCache.isEmpty()) {
            throw new AssertionError("JsonParser binder caches are not instance-scoped");
        }

        System.out.println("JsonParserCacheTest PASS");
    }
}
