/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

/**
 *
 * @author Gust
 */
public class SpecTest {

    public static void main(String args[]) {
        SpecTest obj = new SpecTest();
        print("test start");
        test_int();
        test_long();
        test_float();
        test_double();
        test_jump();
        test_typecast();
        test_array();
        test_other();
        test_field();
        test_method();
        test_cal();
        print("test end");
    }

    static void printerr(String s) {
        System.out.println("test error :" + s);
        try {
            throw new Exception();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    static void print(String s) {
        System.out.println(s);
    }

    static void test_cal() {

        class Lua {

            public static final int MASK_B = 0xff800000;
            public static final int MASK_C = 0x7fc000;
            public static final int POS_B = 23;
            public static final int POS_C = 14;
        }
        int i = 0x100401d;
        System.out.println("Lua.MASK_B=" + Lua.MASK_B + "Lua.MASK_C=" + Lua.MASK_C);

        int m = Lua.MASK_B | Lua.MASK_C;

        int a = i & (Lua.MASK_B | Lua.MASK_C);

        int b = (2 << Lua.POS_B) | (1 << Lua.POS_C);
        if (a != b) {
            printerr("iand");
        }
    }

    static public void test_int() {

        //iconst
        int c0, c1, c2, c3, c4, c5, cm;
        cm = -1;
        if ((cm) != -1) {
            printerr("iconst_m1, iload, istore");
        }
        c0 = 0;
        if ((c0) != 0) {
            printerr("iconst_0, iload0, istore0");
        }
        c1 = 1;
        if ((c1) != 1) {
            printerr("iconst_1, iload1, istore1");
        }
        c2 = 2;
        if ((c2) != 2) {
            printerr("iconst_2, iload2, istore2");
        }
        c3 = 3;
        if ((c3) != 3) {
            printerr("iconst_3, iload3, istore3");
        }
        c4 = 4;
        if ((c4) != 4) {
            printerr("iconst_4");
        }
        c5 = 5;
        if ((c5) != 5) {
            printerr("iconst_5");
        }

        //
        int i1, i2, r;
        i1 = 0x10101010;
        i2 = 0x01010101;
        r = 0x11111111;
        if (i1 + i2 != r) {
            printerr("iadd");
        }
        i1 = 0x11111111;
        i2 = 0x01010101;
        r = 0x10101010;
        if (i1 - i2 != r) {
            printerr("isub");
        }
        i1 = 33333333;
        i2 = 2;
        r = 66666666;
        if (i1 * i2 != r) {
            printerr("imul");
        }
        i1 = 66666666;
        i2 = 33333333;
        r = 2;
        if (i1 / i2 != r) {
            printerr("idiv");
        }
        i1 = 0x10000000;
        i2 = 2;
        r = 0;
        if (i1 % i2 != r) {
            printerr("irem");
        }
        i1 = 1;
        i2 = 0;
        r = -1;
        if (-i1 != r) {
            printerr("ined");
        }
        i1 = 0x01010101;
        i2 = 0x10101010;
        r = 0;
        if ((i1 & i2) != r) {
            printerr("iand");
        }

        i1 = 0x01010101;
        i2 = 0x10101010;
        r = 0x11111111;
        if ((i1 | i2) != r) {
            printerr("ior");
        }

        i1 = 0x1;
        i2 = 0x1;
        r = 0;
        if ((i1 ^ i2) != r) {
            printerr("ixor");
        }

        i1 = 0xffffffff;
        i2 = 0;
        r = 0;
        i1++;
        if ((i1) != r) {
            printerr("iinc");
        }
        i1 = 0x1;
        i2 = 31;
        r = 0x80000000;
        if ((i1 << i2) != r) {
            printerr("ishl, bipush");
        }
        i1 = 0x80000000;
        i2 = 31;
        r = 0xffffffff;
        if ((i1 >> i2) != r) {
            printerr("ishr");
        }
        i1 = 0x80000000;
        i2 = 31;
        r = 0x1;
        if ((i1 >>> i2) != r) {
            printerr("iushr");
        }

    }

    static void test_long_1() {
        int i_;//localvar[0]
        long c1, c3;//localvar[1],localvar[3]
        c1 = 1L;
        if ((c1) != 1L) {
            printerr("lconst_1, lload_1, lstore_1");
        }

        c3 = 3L;
        if ((c3) != 3L) {
            printerr("lload_3, lstore_3");
        }
    }

    static public void test_long() {

        //
        long c0, c2, c_, cm;
        cm = -1L;
        if ((cm) != -1L) {
            printerr("lload, lstore, ldc2_w");
        }
        c0 = 0L;
        if ((c0) != 0L) {
            printerr("lconst_0, lload_0, lstore_0");
        }
        test_long_1();
        c2 = 2L;
        if ((c2) != 2L) {
            printerr("lload_2, lstore_2");
        }

        //
        long j1, j2, r;
        j1 = 0x1010101010101010L;
        j2 = 0x0101010101010101L;
        r = 0x1111111111111111L;
        if (j1 + j2 != r) {
            printerr("ladd");
        }
        j1 = 0x1111111111111111L;
        j2 = 0x0101010101010101L;
        r = 0x1010101010101010L;
        if (j1 - j2 != r) {
            printerr("lsub");
        }
        j1 = 3333333333333333L;
        j2 = 2;
        r = 6666666666666666L;
        if (j1 * j2 != r) {
            printerr("lmul");
        }
        j1 = 6666666666666666L;
        j2 = 3333333333333333L;
        r = 2;
        if (j1 / j2 != r) {
            printerr("ldiv");
        }
        j1 = 0x1000000000000000L;
        j2 = 2;
        r = 0;
        if (j1 % j2 != r) {
            printerr("lrem");
        }
        j1 = 1;
        j2 = 0;
        r = -1;
        if (-j1 != r) {
            printerr("lned");
        }
        j1 = 0x0101010101010101L;
        j2 = 0x1010101010101010L;
        r = 0;
        if ((j1 & j2) != r) {
            printerr("land");
        }

        j1 = 0x0101010101010101L;
        j2 = 0x1010101010101010L;
        r = 0x1111111111111111L;
        if ((j1 | j2) != r) {
            printerr("lor");
        }

        j1 = 0x1;
        j2 = 0x1;
        r = 0;
        if ((j1 ^ j2) != r) {
            printerr("lxor");
        }
        j1 = 0x1;
        j2 = 63;
        r = 0x8000000000000000L;
        if ((j1 << j2) != r) {
            printerr("lshl");
        }
        j1 = 0x8000000000000000L;
        j2 = 63;
        r = 0xffffffffffffffffL;
        if ((j1 >> j2) != r) {
            printerr("lshr");
        }
        j1 = 0x8000000000000000L;
        j2 = 63;
        r = 0x1;
        if ((j1 >>> j2) != r) {
            printerr("lushr");
        }

    }

    static public void test_float() {

        //
        float c0, c1, c2, c3, cm;
        cm = -1.f;
        if ((cm) != -1.f) {
            printerr("fload, fstore");
        }
        c0 = 0.f;
        if ((c0) != 0.f) {
            printerr("fconst_0, fload0, fstore0");
        }
        c1 = 1.f;
        if ((c1) != 1.f) {
            printerr("fconst_1, fload1, fstore1");
        }
        c2 = 2.f;
        if ((c2) != 2.f) {
            printerr("iconst_2, fload2, fstore2");
        }
        c3 = 3.f;
        if ((c3) != 3.f) {
            printerr(" fload3, fstore3");
        }

        //
        float f1, f2, r;
        f1 = .5f;
        f2 = .5f;
        r = 1.f;
        if (f1 + f2 != r) {
            printerr("fadd");
        }
        f1 = .5f;
        f2 = .5f;
        r = 0.f;
        if (f1 - f2 != r) {
            printerr("fsub");
        }
        f1 = .5f;
        f2 = .5f;
        r = 0.25f;
        if (f1 * f2 != r) {
            printerr("fmul");
        }
        f1 = .5f;
        f2 = .5f;
        r = 1.f;
        if (f1 / f2 != r) {
            printerr("fdiv");
        }
        f1 = .5f;
        f2 = .5f;
        r = 0.f;
        if (f1 % f2 != r) {
            printerr("frem");
        }
        f1 = .5f;
        f2 = .5f;
        r = -.5f;
        if (-f1 != r) {
            printerr("fned");
        }

    }

    static void test_double_1() {
        int i;
        double c1, c3;
        c1 = 1.d;
        if ((c1) != 1L) {
            printerr("dconst_1, dload_1, dstore_1");
        }
        c3 = 3.d;
        if ((c3) != 3.d) {
            printerr("dload_3, dstore_3");
        }
    }

    static public void test_double() {
        //
        double c0, c2, c_, cm;
        cm = -1.d;
        if ((cm) != -1L) {
            printerr("dload, dstore, ldc2_w");
        }
        c0 = 0.d;
        if ((c0) != 0.d) {
            printerr("dconst_0, dload_0, dstore_0");
        }
        test_double_1();
        c2 = 2.d;
        if ((c2) != 2.d) {
            printerr("dload_2, dstore_2");
        }

        //
        double d1, d2, r;
        d1 = .5d;
        d2 = .5d;
        r = 1.d;
        if (d1 + d2 != r) {
            printerr("dadd");
        }
        d1 = .5d;
        d2 = .5d;
        r = 0.d;
        if (d1 - d2 != r) {
            printerr("dsub");
        }
        d1 = .5d;
        d2 = .5d;
        r = 0.25d;
        if (d1 * d2 != r) {
            printerr("dmul");
        }
        d1 = .5d;
        d2 = .5d;
        r = 1.d;
        if (d1 / d2 != r) {
            printerr("ddiv");
        }
        d1 = .5d;
        d2 = .5d;
        r = 0.d;
        if (d1 % d2 != r) {
            printerr("drem");
        }
        d1 = .5d;
        d2 = .5d;
        r = -.5d;
        if (-d1 != r) {
            printerr("dned");
        }
    }

    static void test_jump() {
        //=====================================================
        int i1, i2, r;
        i1 = 0x1;
        i2 = 0;
        r = 0;
        if (i1 == 0) {
            printerr("ifeq");
        }

        i1 = 0;
        i2 = 0;
        r = 0;
        if (i1 != 0) {
            printerr("ifne");
        }
        i1 = 0;
        i2 = 0;
        r = 0;
        if (i1 < 0) {
            printerr("iflt");
        }
        i1 = -1;
        i2 = 0;
        r = 0;
        if (i1 >= 0) {
            printerr("ifge");
        }
        i1 = 0;
        i2 = 0;
        r = 0;
        if (i1 > 0) {
            printerr("ifgt");
        }
        i1 = 1;
        i2 = 0;
        r = 0;
        if (i1 <= 0) {
            printerr("ifle");
        }

        //=====================================================
        i1 = 0x80000000;
        i2 = 0x7fffffff;
        r = 0;
        if (i1 == i2) {
            printerr("icmpeq");
        }

        i1 = 0x7fffffff;
        i2 = 0x7fffffff;
        r = 0;
        if (i1 != i2) {
            printerr("icmpne");
        }
        i1 = 0x7fffffff;
        i2 = 0x7fffffff;
        r = 0;
        if (i1 < i2) {
            printerr("icmplt");
        }
        i1 = 0x7ffffffe;
        i2 = 0x7fffffff;
        r = 0;
        if (i1 >= i2) {
            printerr("icmpge");
        }
        i1 = 0x7fffffff;
        i2 = 0x7fffffff;
        r = 0;
        if (i1 > i2) {
            printerr("icmpgt");
        }
        i1 = 0x7fffffff;
        i2 = 0x7ffffffe;
        r = 0;
        if (i1 <= i2) {
            printerr("icmple");
        }

        //long, 
        long j1, j2;
        j1 = 2;
        j2 = 1;
        if (j2 >= j1) {
            printerr("lcmp");
        }

        //float
        float f1, f2;
        f1 = .5f;
        f2 = .5f;
        if (f1 > f2) {
            printerr("fcmpl");
        }
        if (f1 < f2) {
            printerr("fcmpg");
        }
        f1 = .5f;
        f2 = .4f;
        if (f1 == f2) {
            printerr("fcmpl");
        }

        //double
        double d1, d2;
        d1 = .5d;
        d2 = .5d;
        if (d1 > d2) {
            printerr("dcmpl");
        }
        if (d1 < d2) {
            printerr("dcmpg");
        }
        d1 = .5f;
        d2 = .4f;
        if (d1 == d2) {
            printerr("dcmpl");
        }
        //
        boolean z = true;
        while (z) {
            if (z) {
                break;
            }
            printerr("goto");
        }

    }

    static void test_typecast() {

        //int
        int i = 0x80808080;
        char c = (char) i;
        if (c != 0x8080) {
            printerr("i2c");
        }
        short s = (short) i;
        if (s != 0xffff8080) {
            printerr("i2s");
        }
        byte b = (byte) i;
        if (b != 0xffffff80) {
            printerr("i2b");
        }
        long j = i;
        if (j != 0xffffffff80808080L) {
            printerr("i2c");
        }
        i = 5;
        float f = i;
        if (f != 5.f) {
            printerr("i2f");
        }
        double d = i;
        if (d != 5.d) {
            printerr("i2d");
        }

        //long
        j = 0x0000000080000000L;
        i = (int) j;
        if (i != 0xffffffff80000000L) {
            printerr("l2i");
        }
        j = 5L;
        f = j;
        if (f != 5.f) {
            printerr("l2f");
        }
        d = j;
        if (d != 5.d) {
            printerr("l2d");
        }

        //float
        f = 5.5f;
        i = (int) f;
        if (i != 5) {
            printerr("f2i");
        }
        j = (long) d;
        if (j != 5) {
            printerr("f2l");
        }
        d = f;
        if (d != 5.5d) {
            printerr("f2d");
        }

        //double
        d = 5.5d;
        i = (int) d;
        if (i != 5) {
            printerr("d2i");
        }
        j = (long) d;
        if (j != 5) {
            printerr("d2l");
        }
        f = (float) d;
        if (f != 5.5f) {
            printerr("d2f");
        }
    }

    static void test_array() {
        //
        byte[] ba = {1, 2};
        ba[0] = ba[1];
        if (ba[0] != 2 || ba[1] != 2) {
            printerr("newarray, baload, bastore");
        }
        if (ba.length > 2 || ba.length < 0) {
            printerr("arraylength");
        }
        try {
            ba[-1] = 0;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            ba[2] = ba[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        short[] sa = {1, 2};
        sa[0] = sa[1];
        if (sa[0] != 2 || sa[1] != 2) {
            printerr("newarray, saload, sastore");
        }

        try {
            sa[-1] = 0;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            sa[2] = sa[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        char[] ca = {1, 2};
        ca[0] = ca[1];
        if (ca[0] != 2 || ca[1] != 2) {
            printerr("newarray, caload, castore");
        }

        try {
            ca[-1] = 0;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            ca[2] = ca[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        ca[0] = 0xffff;
        if (ca[0] < 0) {
            printerr("char arr type error");
        }

        //
        int[] ia = {1, 2};
        ia[0] = ia[1];
        if (ia[0] != 2 || ia[1] != 2) {
            printerr("newarray, iaload, iastore");
        }

        try {
            ia[-1] = 0;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            ia[2] = ia[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        long[] ja = {1, 2};
        ja[0] = ja[1];
        if (ja[0] != 2 || ja[1] != 2) {
            printerr("newarray, laload, lastore");
        }

        try {
            ja[-1] = 0;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            ja[2] = ja[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        float[] fa = {1, 2};
        fa[0] = fa[1];
        if (fa[0] != 2 || fa[1] != 2) {
            printerr("newarray, faload, fastore");
        }

        try {
            fa[-1] = 0;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            fa[2] = fa[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        double[] da = {1, 2};
        da[0] = da[1];
        if (da[0] != 2 || da[1] != 2) {
            printerr("newarray, daload, dastore");
        }

        try {
            da[-1] = 0;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            da[2] = da[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        boolean[] za = {false, true};
        za[0] = za[1];
        if (!za[0] || !za[1]) {
            printerr("newarray, baload, bastore");
        }

        try {
            za[-1] = false;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            za[2] = za[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        Object[] oa = {new Object(), new Integer(2)};
        oa[0] = oa[1];
        if (!(oa[0] instanceof Integer) || !(oa[1] instanceof Integer)) {
            printerr("anewarray, aaload, aastore");
        }

        try {
            oa[-1] = null;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            oa[2] = oa[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }

        //
        Object[][] oma = new Object[2][2];
        oma[0] = new Object[]{new Object(), new Integer(2)};
        oma[1] = new Object[]{3.5f, true, 9};
        if (oma[0].length != 2 || oma[1].length != 3) {
            printerr("multianewarray, aaload, aastore");
        }

        try {
            oma[-1] = null;
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
        try {
            oma[2] = oma[0];
            printerr("arr index out of bounds");
        } catch (Exception e) {
        }
    }

    static void test_other() {
        Object o = null;
        if (o != null) {
            printerr("ifnonnull");
        }

        o = new Integer(6);
        if (o == null) {
            printerr("ifnull aconst_null");
        }

        try {
            Short io = (Short) o;
            printerr("checkcast");
        } catch (Exception e) {
        }
        //=========================================================

        int ch = 5;
        int v = 0;
        switch (ch) {
            case 4:
                v = 98;
                break;
            case 5:
                v = 85;
                break;
            case 6:
                v = 108;
                break;
            default:
                v = 90;
        }
        if (v != 85) {
            printerr("tableswitch");
        }

        //
        ch = 1000;
        v = 0;
        switch (ch) {
            case 1:
                v = 98;
                break;
            case 100:
                v = 85;
                break;
            case 1000:
                v = 108;
                break;
            default:
                v = 90;
        }
        if (v != 108) {
            printerr("lookupswitch");
        }

        //=========================================================
        class A extends Thread implements Cloneable {

            public Object lock = new Object();

            volatile int v1 = 0, v2 = 0;
            volatile boolean start = false, exit = false;

            public void run() {
                synchronized (lock) {
                    while (!exit) {
                        try {
                            v2++;
                            start = true;
                            //System.out.println("a.v2=" + v2);
                            lock.notify();
                            lock.wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }

        A a = new A();
        if (!(a instanceof Thread) || !(a instanceof Cloneable)) {
            printerr("instanceof");
        }

        a.start();
        synchronized (a.lock) {
            while (true) {
                try {
                    if (!a.start) {//wait v2++ first
                        a.lock.wait(5);
                        continue;
                    }
                    a.v1++;

                    //System.out.println("a.v1=" + a.v1);
                    if (a.v1 >= 2) {
                        a.exit = true;
                        a.lock.notify();
                        break;
                    } else {
                        a.lock.notify();
                        a.lock.wait();
                    }
                } catch (InterruptedException ex) {
                }
            }
        }

        if (a.v2 != 2) {
            printerr("monitorenter, monitorexit");
        }
        //=========================================================

        try {
            throw new NullPointerException();
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                printerr("athrow");
            }
        }
    }

    static class XF {

        byte b;
        short s;
        char c;
        int i;
        long j;
        float f;
        double d;
        Object o;

        static byte sb;
        static short ss;
        static char sc;
        static int si;
        static long sj;
        static float sf;
        static double sd;
        static Object so;

        byte rb() {
            return b;
        }

        short rs() {
            return s;
        }

        char rc() {
            return c;
        }

        int ri() {
            return i;
        }

        long rj() {
            return j;
        }

        float rf() {
            return f;
        }

        double rd() {
            return d;
        }

        Object ro() {
            return o;
        }
    }

    static void test_field() {
        XF.sb = (byte) 0xff;
        if (XF.sb != -1) {
            printerr("getstatic, putstatic b");
        }
        XF.ss = (short) 0xffff;
        if (XF.sb != -1) {
            printerr("getstatic, putstatic s");
        }
        XF.sc = (char) 0xffff;
        if (XF.sc != 0xffff) {
            printerr("getstatic, putstatic c");
        }
        XF.si = 0xffffffff;
        if (XF.si != -1) {
            printerr("getstatic, putstatic i");
        }
        XF.sj = 0xffff;
        if (XF.sj != 0xffff) {
            printerr("getstatic, putstatic j");
        }
        XF.sf = .5f;
        if (XF.sf != 0.5f) {
            printerr("getstatic, putstatic f");
        }
        XF.sd = .5d;
        if (XF.sf != 0.5d) {
            printerr("getstatic, putstatic d");
        }
        XF.so = new Integer(1);
        Object o = XF.so;
        if (XF.so != o) {
            printerr("getstatic, putstatic r");
        }

        //
        XF xf = new XF();
        xf.b = (byte) 0xff;
        if (xf.b != -1) {
            printerr("getfield, putfield b");
        }
        xf.s = (short) 0xffff;
        if (xf.b != -1) {
            printerr("getfield, putfield s");
        }
        xf.c = (char) 0xffff;
        if (xf.c != 0xffff) {
            printerr("getfield, putfield c");
        }
        xf.i = 0xffffffff;
        if (xf.i != -1) {
            printerr("getfield, putfield i");
        }
        xf.j = 0xffff;
        if (xf.j != 0xffff) {
            printerr("getfield, putfield j");
        }
        xf.f = .5f;
        if (xf.f != 0.5f) {
            printerr("getfield, putfield f");
        }
        xf.d = .5d;
        if (xf.f != 0.5d) {
            printerr("getfield, putfield d");
        }
        xf.o = new Integer(1);
        o = xf.o;
        if (xf.o != o) {
            printerr("getfield, putfield r");
        }

        //
        xf.b = (byte) 0xff;
        if (xf.rb() != -1) {
            printerr("ireturn b");
        }
        xf.s = (short) 0xffff;
        if (xf.rb() != -1) {
            printerr("ireturn s");
        }
        xf.c = (char) 0xffff;
        if (xf.rc() != 0xffff) {
            printerr("ireturn c");
        }
        xf.i = 0xffffffff;
        if (xf.ri() != -1) {
            printerr("ireturn i");
        }
        xf.j = 0xffff;
        if (xf.rj() != 0xffff) {
            printerr("lreturn j");
        }
        xf.f = .5f;
        if (xf.rf() != 0.5f) {
            printerr("freturn f");
        }
        xf.d = .5d;
        if (xf.rd() != 0.5d) {
            printerr("dreturn d");
        }
        xf.o = new Integer(1);
        o = xf.o;
        if (xf.ro() != o) {
            printerr("areturn r");
        }

    }

    static void test_method() {
        Human one = new Son();
        if (one.age() != 10) {
            printerr("invokeinterface");
        }

        Grandpa man = new Son();
        if (man.age() != 10) {
            printerr("invokevirtual");
        }

        if (!man.lastName().equals("Zhang")) {
            printerr("invokevirtual");
        }

        if (!Son.live().equals("earth")) {
            printerr("invokestatic");
        }

        Human jason = () -> 30;

        if (jason.age() != 30) {
            printerr("invokedynamic");
        }
    }
}

//=========================================================
interface Human {

    int age();
}

class Grandpa implements Human {

    public int age() {
        return 60;
    }

    public String lastName() {
        return "Zhang";
    }

    public static String live() {
        return "earth";
    }
}

class Father extends Grandpa {

    public int age() {
        return 35;
    }
}

class Son extends Father {

    public int age() {
        return 10;
    }
}

        //=========================================================
