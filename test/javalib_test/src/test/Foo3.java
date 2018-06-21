package test;

import java.util.ArrayList;
import java.util.List;

class Foo3 {

    public static void main(String args[]) {
//        t5();
//        t6();
        t7();
//        t8();
    }
    
    static char ch='\u9F08';
    
    static void t5(){
        System.out.println(""+ch);
        ch='\u9F09';
        System.out.println(""+(int)ch);
    }

    static void t6() {
        Thine e = new Thine();
        e.print();
        Thine x = (Thine) e.clone();
        x.print();
        System.out.println("x=" + x + ", e=" + e);
        e = null;
        x = null;
    }

    static void t7() {
        int MAX = 500000;
        int PRINT_COUNT = 10000;
        Thread t = new Thread(new Runnable() {
            List<String> list = new ArrayList(MAX);

            @Override
            public void run() {
                try {
                    System.out.println("total mem:" + Runtime.getRuntime().totalMemory()
                            + "   free: " + Runtime.getRuntime().freeMemory());

                } catch (Exception ex) {
                }

                long start = System.currentTimeMillis();
                System.out.println("thread here.");
                int j = 0;
                String c = null;
                for (int i = 0; i < MAX; i++) {
                    String a = "abc";
                    String b = "def";
                    c = a + b;
                    list.add(c);
                    if (i % PRINT_COUNT == 0) {
                        System.out.println(this + " thread i=" + i);
                    }
                }
                System.out.println(this + " list.size():" + list.size());
                System.out.println(this + " thread cost: " + (System.currentTimeMillis() - start));
            }
        });
        t.start();

        //
        Thread t1 = new Thread(new Runnable() {
            List<String> list = new ArrayList();

            @Override
            public void run() {
                try {
                    System.out.println("total mem:" + Runtime.getRuntime().totalMemory()
                            + "   free: " + Runtime.getRuntime().freeMemory());

                } catch (Exception ex) {
                }

                long start = System.currentTimeMillis();
                System.out.println("thread here.");
                int j = 0;
                String c = null;
                for (int i = 0; i < MAX; i++) {
                    String a = "abc";
                    String b = "def";
                    c = a + b;
                    list.add(c);
                    if (i % PRINT_COUNT == 0) {
                        System.out.println(this + " thread i=" + i);
                    }
                }
                System.out.println(this + " list.size():" + list.size());
                System.out.println(this + " thread cost: " + (System.currentTimeMillis() - start));
            }
        });
        //t1.start();

        //
        List<String> list = new ArrayList();
        long start = System.currentTimeMillis();
        String c = null;
        for (int i = 0; i < MAX; i++) {
            String a = "abc";
            String b = "def";
            c = a + b;
            list.add(c);
            if (i % PRINT_COUNT == 0) {
                System.out.println("main i=" + i);
            }
        }
        System.out.println("main list.size():" + list.size());
        System.out.println("main thread cost: " + (System.currentTimeMillis() - start));
    }

    static class Thine implements Cloneable {

        Object o = new Object();
        int p = 9;

        public void print() {
            System.out.println("o=" + o + "  ,p=" + p);
        }

        public Object clone() {
            Object c;
            try {
                c = super.clone();
                return c;
            } catch (CloneNotSupportedException ex) {
            }
            return null;
        }

        public void finalize() {
            System.out.println("destory thine later");
        }
    }

    static int f2(int a, int b) {
        return a + b;
    }

    static void t8() {

        long x = 10;

        for (int i = 1; i < 1000000; i++) {
            x += f2(i, i);
        }

        System.out.println(x);
        System.out.println("over");

    }
}
