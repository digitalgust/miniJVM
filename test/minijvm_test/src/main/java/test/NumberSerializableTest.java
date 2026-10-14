package test;

import java.io.Serializable;

/** Regression coverage for Number's inherited Serializable contract. */
public class NumberSerializableTest {
    private static class CustomNumber extends Number {
        public int intValue() { return 7; }
        public long longValue() { return 7L; }
        public float floatValue() { return 7.0f; }
        public double doubleValue() { return 7.0; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(Serializable.class.isAssignableFrom(Number.class), "Number must implement Serializable");
        Number[] numbers = {
            Byte.valueOf((byte) 1), Short.valueOf((short) 2), Integer.valueOf(3),
            Long.valueOf(4L), Float.valueOf(5.0f), Double.valueOf(6.0), new CustomNumber()
        };
        for (int i = 0; i < numbers.length; i++) {
            check(numbers[i] instanceof Serializable, "Number subclass " + i);
            Serializable value = (Serializable) numbers[i];
            check(value == numbers[i], "Cast must preserve identity");
        }
        Serializable inherited = new CustomNumber();
        check(((Number) inherited).intValue() == 7, "Inherited interface conversion");
        Number boxed = (Number & Serializable) 1;
        check(boxed.intValue() == 1, "Boxed intersection cast");
        System.out.println("NumberSerializableTest PASS");
    }
}
