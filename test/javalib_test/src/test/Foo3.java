package test;

class Foo3 {

    public static void main(String args[]) {
//        Object o = null;
//        o.toString();

        //t6();
        t7();
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
//        MyThread t = new MyThread();
//        t.start();

        //
        String[] strs=new String[MAX];
        String c = null;
        for (int i = 0; i < MAX; i++) {
            String a = "abc";
            String b = "def";
            c = a + b + i;
            strs[i]=c;
//            if (i % PRINT_COUNT == 0) {
//                System.out.println("main i=" + i);
//            }
        }
        System.out.println("main c=\"" + c + "\"");
        String a="abc";
        System.out.println("a="+a);
        String b="abc";
        System.out.println("b="+b);
        a=a.substring(1);
        System.out.println("a="+a);
        System.out.println("b="+b);

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
}
