/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
        System.out.println("over");
    }

    void t1() {
        try {
            System.out.println(int.class == Integer.TYPE);
            System.out.println(int.class.equals(Integer.TYPE));
            System.out.println(Integer.class.equals(Integer.TYPE));

            int[][][][] refarr = (int[][][][])Array.newInstance(int[].class, new int[]{2, 2, 2});
            refarr[1][1][1]=new int[1];
            refarr[1][1][1][0]=9;
            System.out.println("refarr[1][1][1][0]="+refarr[1][1][1][0]);
            
            System.out.println("refarr=" + refarr);

            String[][] refarr1 = (String[][])Array.newInstance(String.class, new int[]{2, 2});
            System.out.println("refarr1=" + refarr1);
            refarr1[1][1]="here you are";
            System.out.println("refarr1[1][1]="+refarr1[1][1]);

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
                if (RefNative.getGarbageStatus() == 1) {
                    Object[] objs = RefNative.getGarbageReferedObjs();
                    for (int n = 0; n < objs.length; n++) {
                        Object o = objs[n];
                        if (o != null && o instanceof Class) {
                            Method[] mds = ((Class) objs[n]).getMethods();
                            System.out.println("Class[" + Long.toString(RefNative.obj2id(objs[n]), 10) + "]:");
                            for (int j = 0; j < mds.length; j++) {
                                Method md = mds[j];

                                if (md == null) {
                                    System.out.println("Method[" + j + "]:" + md);
                                } else {
//                                    String[] paras = md.getParameterStrs();
//                                    int k = 0;
//                                    for (String p : paras) {
//                                        System.out.println("Method[" + j + "][" + Long.toString(RefNative.obj2id(md), 16) + "](" + Long.toString(RefNative.obj2id(md), 10) + "):" + md.getName() + " paras[" + k + "]:" + p + "|" + Long.toString(RefNative.obj2id(p), 16));
//                                        k++;
//                                    }
                                }
                            }
                        }
                    }
                    int debug = 1;
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
}
