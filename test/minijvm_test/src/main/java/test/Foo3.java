package test;

import java.util.Vector;

class Foo3 {

    public static void main(String args[]) {
        long t = System.currentTimeMillis();
//        t5();
//        t6();
        t7();
//        t8();
//        t9();
        System.out.println("spent:" + (System.currentTimeMillis() - t));
    }

    static char ch = '\u9F08';

    static void t5() {
        System.out.println("" + ch);
        ch = '\u9F09';
        System.out.println("" + (int) ch);
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


    static public String result = "";
    static Vector list = new Vector();

    static void t7() {
        final int MAX = 5000000;
        final int PRINT_COUNT = 10000;
        Thread t = new Thread(new Runnable() {
            public Vector list = new Vector(MAX);
            public String result = "";

            public void run() {
                try {
                    System.out.println("total mem:" + Runtime.getRuntime().totalMemory()
                            + "   free: " + Runtime.getRuntime().freeMemory());

                } catch (Exception ex) {
                }

                long start = System.currentTimeMillis();
                System.out.println("thread here.");
                long j = 0;
                String a = "abc";
                String b = "def";
                String c = null;
                for (int i = 0; i < MAX; i++) {
                    c = a + b + i;
                    result = c;
                    list.addElement(c);
                    list.removeElementAt(0);
                    if (i % PRINT_COUNT == 0) {
                        //System.out.println(this + " thread i=" + i);
                    }
                    j = result.hashCode();
                }
                System.out.println(this + " list.size():" + list.size());
                System.out.println(this + " thread cost: " + (System.currentTimeMillis() - start));
                System.out.println("j=" + j);
            }
        });
        t.start();


        //

        long start = System.currentTimeMillis();
        long j = 0;
        String c = null;
        String a = "abc";
        String b = "def";
        for (int i = 0; i < MAX; i++) {
            c = a + b + i;
            result = c;
            list.addElement(c);
            list.removeElementAt(0);
            if (i % PRINT_COUNT == 0) {
                //System.out.println("main i=" + i);
            }
            j = result.hashCode();
        }
        System.out.println("main list.size():" + list.size());
        System.out.println("main thread cost: " + (System.currentTimeMillis() - start));
        System.out.println("j=" + j);

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
            } catch (Exception ex) {
            }
            return null;
        }

        void finalize() {
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

    static void t9() {
        System.out.println(topla(11, 4));
        System.out.println(System.getProperty("os.name"));
    }

    static int topla(int a, int b) {
        int var = 0;
        for (int i = 0; i < 10000000; i++) {
            var += a + b;
        }
        return var;
    }
}
