package test;

import java.lang.annotation.*;
import java.lang.reflect.*;

// 定义测试注解
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@interface TestAnnotation {
    String value() default "";

    int number() default 0;

    boolean flag() default false;
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@interface SomeAnnotation {
}

// 测试类
class AnnotatedClass {
    @TestAnnotation(value = "fieldValue", number = 42, flag = true)
    @SomeAnnotation
    public String annotatedField;

    public String normalField;

    @TestAnnotation(value = "methodValue", number = 100)
    @SomeAnnotation
    public void annotatedMethod() {
        System.out.println("Annotated method called");
    }

    public void normalMethod() {
        System.out.println("Normal method called");
    }

    @TestAnnotation
    public static void staticAnnotatedMethod() {
        System.out.println("Static annotated method called");
    }
}

public class FieldMethodAnnotationTest {
    public static void main(String[] args) {
        try {
            Class<?> clazz = AnnotatedClass.class;

            System.out.println("=== Testing Field Annotations ===");

            // 测试字段注解
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                System.out.println("Field: " + field.getName());

                // 测试 getAnnotations()
                Annotation[] annotations = field.getAnnotations();
                System.out.println("  Annotations count: " + annotations.length);
                for (Annotation ann : annotations) {
                    System.out.println("  - " + ann.annotationType().getName());
                    System.out.println("    toString: " + ann.toString());
                }

                // 测试 getAnnotation()
                TestAnnotation testAnn = (TestAnnotation) field.getAnnotation(TestAnnotation.class);
                if (testAnn != null) {
                    System.out.println("  TestAnnotation found:");
                    System.out.println("    value: " + testAnn.value());
                    System.out.println("    number: " + testAnn.number());
                    System.out.println("    flag: " + testAnn.flag());
                }

                SomeAnnotation simpleAnn = (SomeAnnotation) field.getAnnotation(SomeAnnotation.class);
                if (simpleAnn != null) {
                    System.out.println("  SomeAnnotation found");
                }

                System.out.println();
            }

            System.out.println("=== Testing Method Annotations ===");

            // 测试方法注解
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                System.out.println("Method: " + method.getName());

                // 测试 getAnnotations()
                Annotation[] annotations = method.getAnnotations();
                System.out.println("  Annotations count: " + annotations.length);
                for (Annotation ann : annotations) {
                    System.out.println("  - " + ann.annotationType().getName());
                    System.out.println("    toString: " + ann.toString());
                }

                // 测试 getAnnotation()
                TestAnnotation testAnn = (TestAnnotation) method.getAnnotation(TestAnnotation.class);
                if (testAnn != null) {
                    System.out.println("  TestAnnotation found:");
                    System.out.println("    value: " + testAnn.value());
                    System.out.println("    number: " + testAnn.number());
                    System.out.println("    flag: " + testAnn.flag());
                }

                SomeAnnotation simpleAnn = (SomeAnnotation) method.getAnnotation(SomeAnnotation.class);
                if (simpleAnn != null) {
                    System.out.println("  SomeAnnotation found");
                }

                System.out.println();
            }

            System.out.println("=== Testing isAnnotationPresent ===");

            Field annotatedField = clazz.getDeclaredField("annotatedField");
            System.out.println("annotatedField has TestAnnotation: " +
                    annotatedField.isAnnotationPresent(TestAnnotation.class));
            System.out.println("annotatedField has SomeAnnotation: " +
                    annotatedField.isAnnotationPresent(SomeAnnotation.class));

            Field normalField = clazz.getDeclaredField("normalField");
            System.out.println("normalField has TestAnnotation: " +
                    normalField.isAnnotationPresent(TestAnnotation.class));

            Method annotatedMethod = clazz.getDeclaredMethod("annotatedMethod");
            System.out.println("annotatedMethod has TestAnnotation: " +
                    annotatedMethod.isAnnotationPresent(TestAnnotation.class));
            System.out.println("annotatedMethod has SomeAnnotation: " +
                    annotatedMethod.isAnnotationPresent(SomeAnnotation.class));

            Method normalMethod = clazz.getDeclaredMethod("normalMethod");
            System.out.println("normalMethod has TestAnnotation: " +
                    normalMethod.isAnnotationPresent(TestAnnotation.class));

            System.out.println("Field and Method annotation test completed successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 