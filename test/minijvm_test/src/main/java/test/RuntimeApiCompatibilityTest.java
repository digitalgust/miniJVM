package test;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;

/** Targeted regression for the runtime API changes of 2026-10-15. */
public class RuntimeApiCompatibilityTest {
    static int checks;
    static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }
    public static class Parent<T extends Number & Comparable<T>> {
        public List<? super T[]> field;
        public String[][] strings;
        public <U extends T, E extends Exception> List<? extends U[]> method(U[] value) throws E { return null; }
        public <V extends Number> Class<?> local() { class Local { V value; } return Local.class; }
    }
    public static class Child extends Parent<Integer> implements Callable<String> { public String call() { return "ok"; } }
    public static class Outer<T> { public class Inner<U> {} }
    public static class Owners { public Outer<String>.Inner<Integer> nested; public Map.Entry<String, Integer> entry; }
    public static class Recursive<A extends Comparable<B>, B extends Comparable<A>> {}
    public static class GenericConstructor<T extends Number> { public <U extends T> GenericConstructor(U value) {} }
    public static class Target {
        private int value;
        public Integer boxed = Integer.valueOf(3);
        public static final int CONSTANT = 9;
        private Target(int value) { this.value = value; }
        private long secret(long x, double y) { return value + x + (long) (y * 10); }
        public static int stat() { return 42; }
        public void explode() { throw new IllegalStateException("target"); }
        public Target(String text) { throw new IllegalArgumentException(text); }
    }
    public static class Base { public int result() { return 1; } }
    public static class Derived extends Base { public int result() { return 2; } }
    public abstract static class Abstract { public Abstract() {} }
    public static class State { static int count; }
    public interface Passive { int VALUE = initialize(); static int initialize() { State.count++; return 1; } }
    public static class PassiveImpl implements Passive {}
    public static class InitialField { public static int value = 73; }
    public static class PassiveJit { public static Class<?> type() { return Passive.class; } }
    static native int unlinked();

    public static void main(String[] args) throws Exception {
        check(Object.class.getGenericSuperclass() == null);
        check(int.class.getGenericSuperclass() == null);
        check(List.class.getGenericSuperclass() == null);
        check(String[].class.getGenericSuperclass() == Object.class);
        check(String[][].class.getTypeName().equals("java.lang.String[][]"));
        check(Arrays.equals(String[].class.getInterfaces(), new Class<?>[] { Cloneable.class, java.io.Serializable.class }));
        check(Arrays.equals(String[].class.getGenericInterfaces(), new Type[] { Cloneable.class, java.io.Serializable.class }));
        check(Modifier.isStatic(Parent.class.getModifiers()) && Parent.class.isMemberClass());
        check(PassiveImpl.class.getInterfaces()[0] == Passive.class && State.count == 0);
        try { Class.class.getDeclaredConstructors()[0].setAccessible(true); check(false); }
        catch (SecurityException expected) { check(true); }
        check(Modifier.toString(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL).equals("public static final"));
        AnnotatedElement annotated = String.class; check(annotated.getAnnotations() != null);
        for (int i = 0; i < 2000; i++) PassiveJit.type();
        check(State.count == 0);
        check(InitialField.class.getField("value").getInt(null) == 73);
        TypeVariable<?> t = Parent.class.getTypeParameters()[0];
        check(t.getGenericDeclaration() == Parent.class && t.getName().equals("T"));
        check(t.getBounds()[0] == Number.class);
        check(((ParameterizedType) t.getBounds()[1]).getActualTypeArguments()[0].equals(t));
        TypeVariable<?>[] clone = Parent.class.getTypeParameters(); clone[0] = null;
        check(Parent.class.getTypeParameters()[0] != null);
        check(((ParameterizedType) Child.class.getGenericSuperclass()).getActualTypeArguments()[0] == Integer.class);
        check(((ParameterizedType) Child.class.getGenericInterfaces()[0]).getActualTypeArguments()[0] == String.class);
        WildcardType wildcard = (WildcardType) ((ParameterizedType) Parent.class.getField("field").getGenericType()).getActualTypeArguments()[0];
        check(((GenericArrayType) wildcard.getLowerBounds()[0]).getGenericComponentType().equals(t));
        check(Parent.class.getField("strings").getGenericType() == String[][].class);
        Method method = Parent.class.getMethod("method", Number[].class);
        TypeVariable<?> u = method.getTypeParameters()[0], e = method.getTypeParameters()[1];
        check(u.getBounds()[0].equals(t) && u.getGenericDeclaration().equals(method));
        check(((GenericArrayType) method.getGenericParameterTypes()[0]).getGenericComponentType().equals(u));
        check(method.getGenericExceptionTypes()[0].equals(e));
        Type result = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        check(((GenericArrayType) ((WildcardType) result).getUpperBounds()[0]).getGenericComponentType().equals(u));
        Type nested = Owners.class.getField("nested").getGenericType();
        check(((ParameterizedType) ((ParameterizedType) nested).getOwnerType()).getActualTypeArguments()[0] == String.class);
        check(((ParameterizedType) Owners.class.getField("entry").getGenericType()).getOwnerType() == Map.class);
        TypeVariable<?>[] recursive = Recursive.class.getTypeParameters();
        check(((ParameterizedType) recursive[0].getBounds()[0]).getActualTypeArguments()[0].equals(recursive[1]));
        Constructor<?> generic = GenericConstructor.class.getConstructor(Number.class);
        check(generic.getTypeParameters()[0].getGenericDeclaration().equals(generic));
        check(generic.toGenericString().contains("<U>"));
        check(generic.getTypeParameters()[0].getBounds()[0].equals(GenericConstructor.class.getTypeParameters()[0]));
        check(Parent.class.getDeclaringClass() == RuntimeApiCompatibilityTest.class);
        check(RuntimeApiCompatibilityTest.class.getDeclaringClass() == null);
        check(Arrays.asList(RuntimeApiCompatibilityTest.class.getDeclaredClasses()).contains(Parent.class));
        Class<?> local = new Parent<Integer>().local();
        check(local.getDeclaringClass() == null && local.getEnclosingClass() == Parent.class);
        check(local.getEnclosingMethod().getName().equals("local"));
        check(local.isLocalClass() && !local.isAnonymousClass());
        check(((TypeVariable<?>) local.getDeclaredField("value").getGenericType()).getGenericDeclaration().equals(local.getEnclosingMethod()));

        Constructor<Target> ctor = Target.class.getDeclaredConstructor(int.class);
        // Janino's legacy constructor widening is separate from runtime access enforcement.
        if (Modifier.isPrivate(ctor.getModifiers())) {
            try { ctor.newInstance(7); check(false); } catch (IllegalAccessException expected) { check(true); }
        } else check(ctor.newInstance(7).getClass() == Target.class);
        ctor.setAccessible(true); Target target = ctor.newInstance(Byte.valueOf((byte) 7));
        Method secret = Target.class.getDeclaredMethod("secret", long.class, double.class);
        try { secret.invoke(target, 3L, 1.5); check(false); } catch (IllegalAccessException expected) { check(true); }
        secret.setAccessible(true);
        check(((Long) secret.invoke(target, Byte.valueOf((byte) 3), Float.valueOf(1.5f))).longValue() == 25);
        secret.setAccessible(false);
        try { secret.invoke(target, 3L, 1.5); check(false); } catch (IllegalAccessException expected) { check(true); }
        check(Target.class.getMethod("stat").invoke(new Object()).equals(Integer.valueOf(42)));
        check(Base.class.getMethod("result").invoke(new Derived()).equals(Integer.valueOf(2)));
        try { Target.class.getMethod("explode").invoke(target); check(false); }
        catch (InvocationTargetException expected) { check(expected.getCause() instanceof IllegalStateException && expected.getCause().getMessage().equals("target")); }
        try { Target.class.getConstructor(String.class).newInstance("ctor"); check(false); }
        catch (InvocationTargetException expected) { check(expected.getCause() instanceof IllegalArgumentException); }
        try { Abstract.class.getConstructor().newInstance(); check(false); } catch (InstantiationException expected) { check(true); }
        Field field = Target.class.getDeclaredField("value");
        if (Modifier.isPrivate(field.getModifiers())) {
            try { field.get(target); check(false); } catch (IllegalAccessException expected) { check(true); }
        } else check(field.getInt(target) == 7);
        field.setAccessible(true); field.set(target, Byte.valueOf((byte) 11)); check(field.getLong(target) == 11);
        try { field.setLong(target, 12L); check(false); } catch (IllegalArgumentException expected) { check(true); }
        try { Target.class.getField("boxed").getInt(target); check(false); } catch (IllegalArgumentException expected) { check(true); }
        Field constant = Target.class.getField("CONSTANT"); constant.setAccessible(true);
        try { constant.setInt(null, 8); check(false); } catch (IllegalAccessException expected) { check(true); }
        try { unlinked(); check(false); } catch (UnsatisfiedLinkError expected) { check(true); }
        check(new NoSuchFieldError("f") instanceof IncompatibleClassChangeError);
        check(new NoSuchMethodError("m") instanceof IncompatibleClassChangeError);
        Throwable cause = new Exception("init"); ExceptionInInitializerError init = new ExceptionInInitializerError(cause);
        check(init.getCause() == cause && init.getException() == cause);
        try { init.initCause(cause); check(false); } catch (IllegalStateException expected) { check(true); }
        Throwable plain = new Exception("plain"); check(plain.initCause(cause) == plain && plain.getCause() == cause);
        check(new RuntimeException(cause).getMessage().equals(cause.toString()));
        Error error = new Error("error"); check(error.initCause(cause) == error && error.getCause() == cause);
        double[] doubles = {0, -0.0, 1, -2.5, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY};
        for (double d : doubles) {check(Double.doubleToLongBits(Double.parseDouble(Double.toHexString(d))) == Double.doubleToLongBits(d));System.out.println("D " + Double.toHexString(d));}
        float[] floats = {0, -0.0f, 1, -2.5f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN, Float.NEGATIVE_INFINITY};
        for (float f : floats) {check(Float.floatToIntBits(Float.parseFloat(Float.toHexString(f))) == Float.floatToIntBits(f));System.out.println("F " + Float.toHexString(f));}
        check(Double.parseDouble("0x1.00000000000008p0") == 1.0);
        check(Double.doubleToLongBits(Double.parseDouble("0x1.00000000000018p0")) == Double.doubleToLongBits(1.0) + 2);
        check(Float.parseFloat("0x1.000001p0") == 1.0f);
        check(Float.floatToIntBits(Float.parseFloat("0x1.000003p0")) == Float.floatToIntBits(1.0f) + 2);
        check(Double.parseDouble("0x1p-1075") == 0.0);
        check(Double.parseDouble("0x1.0000000000001p-1075") == Double.MIN_VALUE);
        check(Float.parseFloat("0x0.000001p-126") == 0.0f);
        check(Double.isInfinite(Double.parseDouble("0x1.fffffffffffff8p1023")));
        try { Double.parseDouble("0x1"); check(false); } catch (NumberFormatException expected) { check(true); }
        check(Float.floatToIntBits(Float.parseFloat("0x1.00000100000000000001p0")) == Float.floatToIntBits(1.0f) + 1);
        check(Double.valueOf("-0X1.FP+7D").doubleValue() == -248.0);
        System.out.println("RUNTIME API PASS checks=" + checks);
    }
}
