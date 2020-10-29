/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.mini.reflect.DirectMemObj;
import org.mini.reflect.vm.RefNative;

/**
 *
 * @author gust
 */
public class ReflectTest {

    public static void main(String[] args) {
        ReflectTest obj = new ReflectTest();
        obj.t1();
        obj.t2();
        obj.t3();
        obj.t4();
        System.out.println("over");
    }

    void t1() {
        try {
            System.out.println(int.class == Integer.TYPE);
            System.out.println(int.class.equals(Integer.TYPE));
            System.out.println(Integer.class.equals(Integer.TYPE));

            int[][][][] refarr = (int[][][][]) Array.newInstance(int[].class, new int[]{2, 2, 2});
            refarr[1][1][1] = new int[1];
            refarr[1][1][1][0] = 9;
            System.out.println("refarr[1][1][1][0]=" + refarr[1][1][1][0]);

            System.out.println("refarr=" + refarr);

            String[][] refarr1 = (String[][]) Array.newInstance(String.class, new int[]{2, 2});
            System.out.println("refarr1=" + refarr1);
            refarr1[1][1] = "here you are";
            System.out.println("refarr1[1][1]=" + refarr1[1][1]);

            int[] arr = new int[5];
            System.out.println("arr.name:" + arr.getClass().getName());
            System.out.println("type:" + arr.getClass().isArray());

            Class ref = "".getClass();
            System.out.println("ref.name=" + ref.getName());

            Constructor<String> con = ref.getConstructor(String.class);
            String cs = con.newInstance("testpara constructor");
            System.out.println("cs=" + cs);

            Method method = String.class.getMethod("getChars", int.class, int.class, char[].class, int.class);
            Class[] para = method.getParameterTypes();
            for (Class p : para) {
                System.out.println("para:" + p);
            }
            System.out.println("return:" + method.getReturnType());

            System.out.println(new Long(0).getClass().toString());
            String s = (String) ref.newInstance();
            System.out.println(s);
            s += "abcd";
            Method m = ref.getMethod("indexOf", new Class[]{java.lang.String.class, int.class});
            if (m != null) {
                Object result = m.invoke(s, new Object[]{"cd", 1});
                System.out.println("reflect invoke result:" + result);
            } else {
                System.out.println("method not found.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Class[] classes = RefNative.getClasses();
        System.out.println("classes.size()=" + classes.length);
        for (Class cl : classes) {
            System.out.println("[" + Long.toString(RefNative.obj2id(cl), 16) + "] :" + cl.getName());
        }

    }

    void t2() {
        long lastms = System.currentTimeMillis();
        Class r = java.lang.String.class;
        Class r2 = java.lang.Long.class;
        for (int i = 0; i < 1; i++) {
            try {
                //System.out.print(" " + (System.currentTimeMillis() - lastms));
                lastms = System.currentTimeMillis();
                if (i % 10 == 0) {
                    //System.out.println();
                }
                String s = "abcd";
                s.indexOf("cd", 1);
                Method m;
                m = r.getMethod("indexOf", new Class[]{java.lang.String.class, int.class});
                if (m != null) {
                    Object result = m.invoke(s, new Object[]{"cd", 1});
                    System.out.println("reflect invoke result:" + result);
                }


                Long lo = new Long(0x1010101020202020L);

                m = r2.getMethod("longValue", new Class[]{});
                if (m != null) {
                    Object result = m.invoke(lo, new Object[]{});
                    System.out.println("reflect invoke result:" + Long.toString((Long) result, 16));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    void t3() {
        char[] ca = "abcdefg".toCharArray();
        DirectMemObj dmo = DirectMemObj.wrap(ca);
        dmo.setChar(5, 'x');
        //dmo.setChar(9, 'y');
        System.out.println("ca=" + (new String(ca)));

        char[] cb = new char[ca.length];
        dmo.copyTo(1, cb, 0, 4);
        System.out.println("cb=" + (new String(cb)));

        char[] cc = new char[ca.length]; 

        dmo = DirectMemObj.wrap(cc);
        dmo.copyFrom(0, cb, 0, 4);
        dmo.copyFrom(2, ca, 4, 2);
        System.out.println("cc=" + (new String(cc)));

        dmo = DirectMemObj.allocate(1024, byte.class);
        for (int i = 0; i < 100; i++) {
            dmo.setByte(i, (byte) i);
        }
        for (int i = 0; i < 100; i++) {
            System.out.print(" " + dmo.getByte(i));
        }
        System.out.println();
    }

    static <T> int getLength(T arr) {
        return Array.getLength(arr);
    }

    class A {

        void print() {
            System.out.println("A");
        }
    }

    class B extends A {

        void print() {
            System.out.println("B");
        }
    }

    class C extends B {

        void print() {
            System.out.println("C");
        }
    }

//    void load(A a) {
//        System.out.print("load A ");
//        a.print();
//    }
//
//    void load(B b) {
//        System.out.print("load B ");
//        b.print();
//    }

    <T> void load(T t) {
        System.out.print("load T ");
        //t.print();
    }

    C save() {
        System.out.print("save C ");
        return new C();
    }

    void t4() {
        byte[] b = new byte[4];
        System.out.println(getLength(b));

        C c = new C();
        load(c);

        B bb = save();
    }
}
