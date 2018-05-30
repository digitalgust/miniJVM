package test;

class Foo3 {

    public static void main(String args[]) {
//        Object o = null;
//        o.toString();

        t6();
        t7();
        t8();
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

    public static int MAX = 500000;
    public static int PRINT_COUNT = 10000;

    static void t7() {
        //
        String[] strs = new String[MAX];
        String c = null;
        for (int i = 0; i < MAX; i++) {
            String a = "abc";
            String b = "def";
            c = a + b + i;
            strs[i] = c;
        }
        System.out.println("main c=\"" + c + "\"");
        String a = "abc";
        System.out.println("a=" + a);
        String b = "abc";
        System.out.println("b=" + b);
        a = a.substring(1);
        System.out.println("a=" + a);
        System.out.println("b=" + b);

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
