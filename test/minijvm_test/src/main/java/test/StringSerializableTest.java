package test;

import java.io.Serializable;

/** Marker interfaces, unchecked casts and ordinary text operations must agree with Java 8. */
public class StringSerializableTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static <T extends CharSequence & Serializable> T identity(T text) { return text; }

    @SuppressWarnings("unchecked")
    private static <T> T value() { return (T) "ok"; }

    @SuppressWarnings("unchecked")
    private static <T extends CharSequence & Serializable> T bounded() { return (T) "ok"; }

    public static void main(String[] args) {
        check(Serializable.class.isAssignableFrom(String.class), "String must implement Serializable");
        check(Serializable.class.isAssignableFrom(StringBuilder.class), "StringBuilder must implement Serializable");
        Object[] texts = { "ok", new String("ok"), new StringBuilder("ok") };
        for (int i = 0; i < texts.length; i++) {
            check(texts[i] instanceof Serializable, "Serializable text " + i);
            Serializable value = (Serializable) texts[i];
            check(value == texts[i], "Cast must preserve identity");
            check(((CharSequence) value).length() == 2, "CharSequence interface dispatch");
        }
        Serializable literal = "ok";
        Serializable builder = new StringBuilder("ok");
        check(identity("ok").length() == 2, "String generic intersection");
        check(identity(new StringBuilder("ok")).length() == 2, "Builder generic intersection");
        check(((CharSequence & Serializable) literal).length() == 2, "String intersection cast");
        check(((CharSequence & Serializable) builder).length() == 2, "Builder intersection cast");
        check("ok".equals(StringSerializableTest.<String>value()), "Unchecked type-variable cast");
        check(StringSerializableTest.<String>bounded().length() == 2, "Bounded type-variable cast");
        Object unchecked = StringSerializableTest.<Integer>value();
        check("ok".equals(unchecked), "Object consumer must not introduce a cast to Integer");
        StringSerializableTest.<Integer>value();
        try {
            Integer invalid = StringSerializableTest.<Integer>value();
            throw new AssertionError("Integer consumer must fail: " + invalid);
        } catch (ClassCastException expected) {}
        StringBuilder text = new StringBuilder("o");
        text.append('k');
        check("ok".equals(text.toString()), "Append and toString");
        check("ok".equals(new String(" ok ").trim()), "String trim and equals");
        System.out.println("StringSerializableTest PASS");
    }
}
