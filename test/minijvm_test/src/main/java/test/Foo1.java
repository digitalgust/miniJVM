/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;


import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author gust
 */
public class Foo1 {

    public FInterface fi = new Q();

    interface FInterface {

        void print();
    }

    interface SubInterface extends FInterface {

    }

    class P implements SubInterface {

        @Override
        public void print() {
            System.out.println("P.print()");
        }

        public void x1() {
            System.out.println("P.x1()");
        }
    }

    class Q extends P {

        public void print() {
            System.out.println("Q.print()");
        }
    }

    void t1() {
        fi = new Q();
        fi.print();
        ((P) fi).x1();
        int a = Math.abs(-275);
        java.lang.System.out.println("a=" + a);
    }

    void t2() {
        String show = "test hashtable";
        System.out.println(show + ":" + show.hashCode());

        Integer itg = new Integer(0xf8f8f8);
        System.out.println(itg + ":" + itg.hashCode());

        Hashtable htable = new Hashtable();
        htable.put("I", itg);
        htable.put("J", new Long(0x2222222211111111L));
        htable.put("S", new String("SSS"));
        htable.put("L", new Object());
        for (Enumeration e = htable.keys(); e.hasMoreElements(); ) {
            String s = (String) e.nextElement();
            Object obj = htable.get(s);
            java.lang.System.out.println(s + " : " + obj);
        }
    }

    void t3() {
        Object o = new Double(-5.737373f);
        String s = o.toString();
    }

    void t4() {
        Vector v = new Vector();
        v.addElement(new Object());
        v.addElement(new String("((((((((((("));
        v.addElement(new Double(-5.737373f));
        v.addElement(new Byte((byte) 0xff));
        for (int i = 0; i < v.size(); i++) {
            System.out.println("v[" + i + "]=" + v.elementAt(i));
        }
    }

    void t5() {
        try {
            String s = Double.toString(-5.737373f);
            System.out.println("d=" + s);
            int[] a = new int[10];
            System.out.println("aaa" + 999 + "," + 3.4f + "," + a);
        } catch (Exception e) {
            //System.out.println("error");
        }
    }

    void t6() {
        System.out.println("test start");
        {
            int i1 = 0xffffffff;
            int i2 = 0x80808080;
            System.out.println("iand " + ((i1 & i2) != 0x80808080 ? " error" : " ok"));
            System.out.println("ior " + ((i1 | i2) != 0xffffffff ? " error" : " ok"));
            System.out.println("ixor " + ((i1 ^ i2) != 0x7f7f7f7f ? " error" : " ok"));
            System.out.println("ishl " + ((i1 << 8) != 0xffffff00 ? " error" : " ok"));
            System.out.println("ishr " + ((i1 >> 8) != 0xffffffff ? " error" : " ok"));
            System.out.println("iushr " + ((i1 >>> 8) != 0x00ffffff ? " error" : " ok"));
            i1 = 0x24242424;
            i2 = 0x12121212;
            System.out.println("iadd " + ((i1 + i2) != 0x36363636 ? " error" : " ok"));
            System.out.println("isub " + ((i1 - i2) != 0x12121212 ? " error" : " ok"));
            System.out.println("imul " + ((i1 * i2) != 0x279d1288 ? " error" : " ok"));
            System.out.println("idiv " + ((i1 / i2) != 0x2 ? " error" : " ok"));
            System.out.println("irem " + ((i1 % i2) != 0x0 ? " error" : " ok"));
            System.out.println("ineg " + ((-i1) != 0xdbdbdbdc ? " error" : " ok"));
            int i3 = 100;
            i3 += 1000;
            System.out.println("iinc " + ((i3) != 1100 ? " error" : " ok"));

        }
        {
            long j1 = 0xffffffffffffffffL;
            long j2 = 0x8080808080808080L;
            System.out.println("land " + ((j1 & j2) != 0x8080808080808080L ? " error" : " ok"));
            System.out.println("lor " + ((j1 | j2) != 0xffffffffffffffffL ? " error" : " ok"));
            System.out.println("lxor " + ((j1 ^ j2) != 0x7f7f7f7f7f7f7f7fL ? " error" : " ok"));
            System.out.println("lshl " + ((j1 << 8) != 0xffffffffffffff00L ? " error" : " ok"));
            System.out.println("lshr " + ((j1 >> 8) != 0xffffffffffffffffL ? " error" : " ok"));
            System.out.println("lushr " + ((j1 >>> 8) != 0x00ffffffffffffffL ? " error" : " ok"));
            j1 = 0x2424242424242424L;
            j2 = 0x1212121212121212L;
            System.out.println("ladd " + ((j1 + j2) != 0x3636363636363636L ? " error" : " ok"));
            System.out.println("lsub " + ((j1 - j2) != 0x1212121212121212L ? " error" : " ok"));
            System.out.println("lmul " + ((j1 * j2) != 0x51c73cb2279d1288L ? " error" : " ok"));
            System.out.println("ldiv " + ((j1 / j2) != 0x2L ? " error" : " ok"));
            System.out.println("lrem " + ((j1 % j2) != 0x0L ? " error" : " ok"));
            System.out.println("lneg " + ((-j1) != 0xdbdbdbdbdbdbdbdcL ? " error" : " ok"));
        }

        {
            double d1 = 1.2121212f;
            double d2 = 1.2121212f;
            System.out.println("dadd " + (d1 + d2) + ((d1 + d2) != 2.4242424f ? " error" : " ok"));
            System.out.println("dsub " + (d1 - d2) + ((d1 - d2) != 0.0f ? " error" : " ok"));
            System.out.println("dmul " + (d1 * d2) + ((d1 * d2) < 1.46f ? " error" : " ok"));
            System.out.println("ddiv " + (d1 / d2) + ((d1 / d2) != 1.0f ? " error" : " ok"));
            System.out.println("drem " + (d1 % d2) + ((d1 % d2) != 0.0f ? " error" : " ok"));
            System.out.println("dneg " + (-d1) + ((-d1) != -1.2121212f ? " error" : " ok"));
            System.out.println("dcmpl " + (d1 > d2) + ((d1 > d2) ? " true" : " false"));
            System.out.println("dcmpg " + (d1 < d2) + ((d1 < d2) ? " true" : " false"));
//            System.out.println("dneg " + Double.toString(-d1) + ((-d1) != -1.2121212f ? " error" : " ok"));
        }
    }

    void t7() {
        final int MAX = 100000;
        final int PRINT_COUNT = 10000;
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
        t1.start();

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

    long t81(long l) {
        return l;
    }

    void t8() {
        System.out.println("total mem:" + Runtime.getRuntime().totalMemory()
                + "   free: " + Runtime.getRuntime().freeMemory());
        System.out.println(Long.toString(t81(0x1000000120000002L), 16));
        Long.toString(t81(0x1000000120000002L), 16);
    }

    void t9() {
        double d1 = 1.2121212f;
        double d2 = 1.2121212f;
        double r = d1 + d2;
//        Double.toString(r);
        System.out.println("dadd " + Double.toString(d1 + d2));

    }

    void t10() {
        int i = 0;
        try {
            i = 1;
            if (true) {
                throw new NullPointerException("exception test 1");
            }
            i = 0;
        } catch (Exception e) {
            e.printStackTrace();
            i = 2;
        }
        System.out.println("i=" + i);
    }

    void t11() {
        String a = "0123456789";
        String b = a;
        a = a.substring(a.indexOf('2'), a.indexOf('7'));
//        a = a + "0123456789";
        System.out.println("" + a);
        System.out.println("" + a.charAt(1));
        System.out.println("" + a.indexOf('4', 1));
        String s1 = "";
        String s2 = "";
        System.out.println(s1.equals(s2));
    }

    void t12() {
        class B {
        }
        try {
            B[][] b = new B[3][3];
            B[] b1 = new B[8];
            double[][][] a = new double[4][][];
            a[0] = new double[2][2];
            a[0][0] = new double[1];
            a[0][0][0] = 5.0;
            Thread.sleep(3000);
            System.out.println("a:" + a);
            System.out.println("a[0]:" + a[0]);
            System.out.println("a[0][0]:" + a[0][0]);
            System.out.println("a[0][0][0]:" + a[0][0][0]);
        } catch (InterruptedException ex) {
        }
    }

    class MyClassLoader extends ClassLoader {
        String[] paths = new String[1];

        public MyClassLoader(String jarPath) {
            super(null);
            paths[0] = jarPath;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {

            // 加载D盘根目录下指定类名的class
            String classname = name.replace('.', File.separatorChar) + ".class";
            byte[] classData = getFileData(classname, paths);
            if (classData == null) {
                throw new ClassNotFoundException();
            } else {
                return defineClass(name, classData, 0, classData.length);
            }
        }

        protected URL findResource(String path) {
            URL url = getFileUrl(path, paths);
            return url;
        }


        /**
         * find a file bytes from classes paths
         *
         * @param name
         * @param paths
         * @return
         * @throws ClassNotFoundException
         */
        public byte[] getFileData(String name, String[] paths) {

            // 加载D盘根目录下指定类名的class
            byte[] classData = null;
            for (String s : paths) {
                if (s != null && s.length() > 0) {
                    File f = new File(s);
                    if (f.isFile()) {
                        try {
                            ZipFile zf = new ZipFile(s);
                            ZipEntry ze = zf.getEntry(name);
                            InputStream is = zf.getInputStream(ze);
                            DataInputStream dis = new DataInputStream(is);
                            classData = new byte[is.available()];
                            dis.readFully(classData);
                            dis.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (classData != null) {
                            break;
                        }
                    } else {
                        File cf = new File(s + "/" + name);
                        if (cf.exists()) {
                            try {
                                classData = new byte[(int) cf.length()];
                                RandomAccessFile fis = new RandomAccessFile(cf, "r");
                                fis.read(classData, 0, classData.length);
                            } catch (Exception e) {
                                classData = null;
                            }
                        }
                    }
                }
            }
            return classData;
        }

        /**
         * find a file url from paths ,the paths may contains jar and directory
         *
         * @param sourceName
         * @param paths
         * @return
         */
        public URL getFileUrl(String sourceName, String[] paths) {
            URL url = null;
            for (String s : paths) {
                if (s != null && s.length() > 0) {
                    File f = new File(s);
                    if (f.isFile()) {
                        try {
                            ZipFile zf = new ZipFile(f.getAbsolutePath());
                            while (sourceName.startsWith("/")) sourceName = sourceName.substring(1);
                            ZipEntry ze = zf.getEntry(sourceName);
                            if (ze != null) {
                                String us = "jar:file:" + f.getAbsolutePath() + "!/" + sourceName;
                                url = new URL(us);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        File cf = new File(s + "/" + sourceName);
                        if (cf.exists()) {
                            try {
                                String us = "file:" + cf.getAbsolutePath();
                                System.out.println(us);
                                url = new URL(us);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return url;
        }
    }

    void t13() {
        ClassLoader cl = Integer.class.getClassLoader();
        System.out.println(cl);
        System.out.println(Foo1.class.getClassLoader());
        System.out.println(Foo1.class.getClassLoader().getParent());
        System.out.println(Foo1.class.getClassLoader().getParent().getParent());
        try {
            InputStream is = this.getClass().getResourceAsStream("/sys.properties");
            System.out.println("length:" + is.available());
            is.close();

//            File f = new File("test/Foo1.class");
//            if (f.exists()) {
//                URL url = new URL(f.getAbsolutePath());
//            }
            MyClassLoader mycl = new MyClassLoader("/Users/Gust/Documents/GitHub/miniJVM/binary/libex/glfw_gui.jar");
            Thread.currentThread().setContextClassLoader(mycl);
            Class c = mycl.loadClass("test.Ball");
            System.out.println("loadClass:" + c);
            c = Class.forName("test.Ball");
            System.out.println("Class.forName:" + c);
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/test/Boing.class");
            System.out.println(is);
            if (is != null) System.out.println(is.available());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void t14() {

    }

    void t19() {
        System.out.println("fi=" + fi);
        Long[][] a = new Long[3][5];
        System.out.println("arr a:" + a);
        Object[][][] objs = new Object[4][][];
        System.out.println("arr objs:" + objs);
        objs[1] = new Object[2][];
        System.out.println("arr objs[1]:" + objs[1]);
        Short[] c = new Short[5];
        System.out.println("arr c:" + c);
        System.out.println("fi=" + fi);

        List<Integer> list = new ArrayList();
        list.add(1);
        list.add(999);
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Object o = it.next();
            System.out.println("list[i]=" + o);
        }
        Integer[] iarr = list.toArray(new Integer[0]);
        for (Integer i : iarr) {
            System.out.println("i=" + i);
        }
        System.out.println("arr type:" + iarr.getClass());

        Map<Long, String> map = new HashMap();
        map.put(3L, "Long");
        map.put(4L, "Float");
        for (Long key : map.keySet()) {
            String val = map.get(key);
            System.out.println(key + ":" + val);
        }
        List clist = Collections.synchronizedList(new ArrayList());

        System.out.println("fi=" + fi);
        int i = 0;
        while (i++ < 1000) {
            try {
                //Thread.sleep(1000);
                int debug = 1;
                debug++;
                debug++;
                String xa = "a";
                String xb = "b";
                String xc = xa + xb;
                xc = xc.substring(1);
                debug++;
                debug++;
                debug++;
                //t22();
                //System.out.println("sleep 1000");
            } catch (Exception e) {
            }
        }
    }

    public void t20_1() {

        String a = "abc";
        String b = "def";
        String c = a + b;

    }

    public void t20() {
        int i = 0;
        while (i++ < 1000) {
            t20_1();
            int debug = 1;
        }
    }

    public void t21() {
        class A {

            Object v;
        }
        class B {

            Object v;
        }
        class C {

            Object v;
        }
        A a = new A();
        B b = new B();
        C c = new C();
        a.v = b;
        b.v = c;
        c.v = a;
    }

    void t23() {
        System.out.println(2);
        long start = System.currentTimeMillis();
        for (int i = 3; i < 100000; i += 2) {
            boolean isPrime = true;
            for (int j = 3; j < i / 2; j += 2) {
                if ((i % j) == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                //System.out.println(i);
            }
        }
        System.out.println(" t23 cost: " + (System.currentTimeMillis() - start));
    }

    void t24() {
        long a = 0;
        boolean b = ((a++) % 100) == 0;
        System.out.println("a:" + a + "   b:" + b);
    }

    void t25() {
        class T25 {

            ThreadLocal<StringBuilder> var = new ThreadLocal() {
                @Override
                protected Object initialValue() {
                    return new StringBuilder();
                }
            };
        }
        ;

        final T25 t25 = new T25();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("Thread 1");
                t25.var.get().append(Thread.currentThread().getName());
                System.out.println(Thread.currentThread() + ":" + t25.var.get());
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("Thread 2");
                t25.var.get().append(Thread.currentThread().getName());
                System.out.println(Thread.currentThread() + ":" + t25.var.get());
            }
        }).start();
    }

    void t26_1(int i) {
        i++;
        if (i < 100) {
            t26_1(i);
        } else {
            return;
        }
    }

    void t26() {
        t26_1(1);
    }

    public static void exec() {
        Foo1 f = new Foo1();
        for (int i = 0; i < 1; i++) {
//            f.t1();
//            f.t2();
//            f.t3();
//            f.t4();
//            f.t5();
//            f.t6();
//            f.t7();
//            f.t8();
//            f.t9();
//            f.t10();
//            f.t11();
//            f.t12();
            f.t13();
//            f.t14();
//            f.t19();
//            f.t20();
//            f.t21();
//            f.t23();
//            f.t24();
//            f.t25();
//            f.t26();
        }
    }

    public static void main(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                System.out.println("args " + i + " :" + args[i]);
            }
            Foo1.exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
