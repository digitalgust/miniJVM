package test;

import org.mini.reflect.ReflectClass;
import org.mini.vm.VmUtil;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

interface OtherInterface {
    String doSomething(int a, String b);
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface MyAnnotation {
    String value() default "default";

    int number() default 42;

}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface SimpleAnnotation {
}

@MyAnnotation(value = "test", number = 123)
@SimpleAnnotation
public class AnnotationTest {

    //descriptor:  (Ljava/lang/Class;Ljava/util/Map;)Ljava/lang/annotation/Annotation;
    //signature:  <T::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TT;>;Ljava/util/Map<Ljava/lang/String;TT;>;)TT;
    <T extends Annotation> T getAnnotation(Class<T> annotationClass, Map<String, T> map) {
        return this.getClass().getAnnotation(annotationClass);
    }

    public static void main(String[] args) throws Exception {
        Class<?> clazz = AnnotationTest.class;

        Method m = clazz.getDeclaredMethod("getAnnotation", Class.class, Map.class);
        clazz = ReflectClass.class;
        m = clazz.getDeclaredMethod("findAnnotation", String.class, Class.class, Class.class);
        System.out.println("m.toString() = " + m);
        System.out.println("m.toGenericString() = " + m.toGenericString());
        System.out.println("m.getParameterTypes() = " + Arrays.asList(m.getParameterTypes()));
        System.out.println("m.getReturnType() = " + m.getReturnType());
        System.out.println("m.getExceptionTypes() = " + m.getExceptionTypes());
        System.out.println("m.getGenericParameterTypes() = " + Arrays.asList(m.getGenericParameterTypes()));
        System.out.println("m.getGenericReturnType() = " + m.getGenericReturnType());
        System.out.println("m.getGenericExceptionTypes() = " + m.getGenericExceptionTypes());

        System.out.println("类名: " + clazz.getName());
        System.out.println("是否为注解: " + clazz.isAnnotation());

        // 测试获取注解
        Annotation[] annotations = clazz.getAnnotations();
        System.out.println("注解数量: " + annotations.length);

        for (Annotation annotation : annotations) {
            System.out.println("注解: " + annotation);
            System.out.println("注解类型: " + annotation.annotationType().getName());
        }

        // 测试检查特定注解是否存在
        System.out.println("是否有MyAnnotation: " + clazz.isAnnotationPresent(MyAnnotation.class));
        System.out.println("是否有SimpleAnnotation: " + clazz.isAnnotationPresent(SimpleAnnotation.class));

        // 测试获取特定注解
        MyAnnotation myAnn = clazz.getAnnotation(MyAnnotation.class);
        if (myAnn != null) {
            System.out.println("找到MyAnnotation: " + myAnn);
        }

        SimpleAnnotation simpleAnn = clazz.getAnnotation(SimpleAnnotation.class);
        if (simpleAnn != null) {
            System.out.println("找到SimpleAnnotation: " + simpleAnn);
        }

        // 测试getDeclaredAnnotations
        Annotation[] declaredAnnotations = clazz.getDeclaredAnnotations();
        System.out.println("声明的注解数量: " + declaredAnnotations.length);

        // 测试getAnnotationsByType
        MyAnnotation[] myAnnotations = clazz.getAnnotationsByType(MyAnnotation.class);
        System.out.println("MyAnnotation数组大小: " + myAnnotations.length);

        // Test ReflectClass annotation access
        try {
            Class<?> reflectClassType = Class.forName("org.mini.reflect.ReflectClass");
            Object reflectClass = reflectClassType.getConstructor(long.class).newInstance(0L);
            // Note: In a real test, you'd get the actual class handle
            System.out.println("ReflectClass created successfully");
        } catch (Exception e) {
            System.err.println("Error creating ReflectClass: " + e.getMessage());
        }

        InvocationHandler handler = (ins, method, params) -> {
            switch (method.getName()) {
                case "value":
                    return "test";
                case "number":
                    return 123;
                case "toString":
                    return "MyAnnotation@";
                default:
                    throw new UnsupportedOperationException("Unsupported method: " + method.getName());
            }
        };

        MyAnnotation myAnnotation = (MyAnnotation) Proxy.newProxyInstance(AnnotationTest.class.getClassLoader()
                , new Class[]{MyAnnotation.class}
                , handler);
        System.out.println("MyAnnotation: " + myAnnotation);
        System.out.println("MyAnnotation.value(): " + myAnnotation.value());
        System.out.println("MyAnnotation.number(): " + myAnnotation.number());

        MyAnnotation myAnnotation2 = new MyAnnotationImpl(handler);
        System.out.println("MyAnnotation2: " + myAnnotation2);
        System.out.println("MyAnnotation2.value(): " + myAnnotation2.value());
        System.out.println("MyAnnotation2.number(): " + myAnnotation2.number());

        InvocationHandler handler1 = (ins, method, params) -> {
            switch (method.getName()) {
                case "doSomething":
                    int a = (int) params[0];
                    String b = (String) params[1];
                    return a + b;
                case "toString":
                    return "OtherInterface@";
                default:
                    throw new UnsupportedOperationException("Unsupported method: " + method.getName());
            }
        };
        OtherInterface proxy1 = (OtherInterface) Proxy.newProxyInstance(AnnotationTest.class.getClassLoader()
                , new Class[]{OtherInterface.class}
                , handler1);
        System.out.println("proxy1: " + proxy1);
        System.out.println("proxy1.doSomething(6, \"test\"): " + proxy1.doSomething(6, "test"));

        OtherInterface proxy2 = new OtherInterfaceImpl(handler1);
        System.out.println("proxy2: " + proxy2);
        System.out.println("proxy2.doSomething(6, \"test\"): " + proxy2.doSomething(6, "test"));
    }

    static class OtherInterfaceImpl extends java.lang.reflect.Proxy implements OtherInterface {
        public OtherInterfaceImpl(InvocationHandler handler) {
            super(handler);
        }

        @Override
        public String doSomething(int a, String b) {
            String s = (String) VmUtil.wrapInvoke(this.h, this, OtherInterface.class, "doSomething", "(ILjava/lang/String;)Ljava/lang/String;", new Object[]{a, b});
            return s;
        }
    }

    static class MyAnnotationImpl extends java.lang.reflect.Proxy implements MyAnnotation {


        public MyAnnotationImpl(InvocationHandler handler) {
            super(handler);
        }

        @Override
        public String value() {
            String s = (String) VmUtil.wrapInvoke(this.h, this, MyAnnotation.class, "value", "()Ljava/lang/String;", null);
            return s;
        }

        @Override
        public int number() {
            Integer integer = (Integer) VmUtil.wrapInvoke(this.h, this, MyAnnotation.class, "number", "()I", null);
            return integer;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return MyAnnotation.class;
        }
    }


} 