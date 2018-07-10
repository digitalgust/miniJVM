package test;

import java.io.File;
import java.io.IOException;

class Foo2 {

    public void t0() {
        System.out.println("HelloWorld.");
    }

    public void t1() {

        byte f0 = 100;
        byte f1 = (byte) (5 % f0 - 2 * (9 + 5) / 2);
        System.out.println("+ = " + (f0 + f1));
        System.out.println("- = " + (f0 - f1));
        System.out.println("* = " + (f0 * f1));
        System.out.println("/ = " + (f0 / f1));
        System.out.println("% = " + (f0 % f1));
        byte[] val = new byte[10];
        f1 = 100;
        f1 = (byte) (f1 * f1 / f1 + f1 - f1 / 2);
        System.out.println(f1);
        val[0] = f1;
        System.out.println(val[0]);
        for (int i = 0; i < val.length; i++) {
            val[i] = (byte) (5 % f0 - 2 * (9 + 5) / 2);
        }
        System.out.println("strs.length=" + val.length);
        for (int i = 0; i < val.length; i++) {
            System.out.println("val[" + i + "]=" + val[i]);
        }
    }

    public void t2() {

        short f0 = 100;
        short f1 = (short) (5 % f0 - 2 * (9 + 5) / 2);
        System.out.println("+ = " + (f0 + f1));
        System.out.println("- = " + (f0 - f1));
        System.out.println("* = " + (f0 * f1));
        System.out.println("/ = " + (f0 / f1));
        System.out.println("% = " + (f0 % f1));
        short[] val = new short[10];
        f1 = 1000;
        f1 = (short) (f1 * f1 / f1 + f1 - f1 / 2);
        System.out.println(f1);
        val[0] = f1;
        System.out.println(val[0]);
        for (int i = 0; i < val.length; i++) {
            val[i] = (short) (5 % f0 - 2 * (9 + 5) / 2);
        }
        System.out.println("strs.length=" + val.length);
        for (int i = 0; i < val.length; i++) {
            System.out.println("val[" + i + "]=" + val[i]);
        }
    }

    public void t3() {

        int f0 = 100;
        int f1 = 5 % f0 - 2 * (9 + 5) / 2;;
        System.out.println("+ = " + (f0 + f1));
        System.out.println("- = " + (f0 - f1));
        System.out.println("* = " + (f0 * f1));
        System.out.println("/ = " + (f0 / f1));
        System.out.println("% = " + (f0 % f1));
        int[] val = new int[10];
        f1 = 100000000;
        f1 = f1 * f1 / f1 + f1 - f1 / 2;
        System.out.println(f1);
        val[0] = f1;
        System.out.println(val[0]);
        for (int i = 0; i < val.length; i++) {
            val[i] = 5 % f0 - 2 * (9 + 5) / 2;
        }
        System.out.println("strs.length=" + val.length);
        for (int i = 0; i < val.length; i++) {
            System.out.println("val[" + i + "]=" + val[i]);
        }
    }

    public void t4() {

        long f0 = 100;
        long f1 = 5 % f0 - 2 * (9 + 5) / 2;;
        System.out.println("+ = " + (f0 + f1));
        System.out.println("- = " + (f0 - f1));
        System.out.println("* = " + (f0 * f1));
        System.out.println("/ = " + (f0 / f1));
        System.out.println("% = " + (f0 % f1));
        long[] val = new long[10];
        f1 = 1000000000000000L;
        f1 = f1 * f1 / f1 + f1 - f1 / 2;
        System.out.println(f1);
        val[0] = f1;
        System.out.println(val[0]);
        for (int i = 0; i < val.length; i++) {
            val[i] = 5 % f0 - 2 * (9 + 5) / 2;
        }
        System.out.println("strs.length=" + val.length);
        for (int i = 0; i < val.length; i++) {
            System.out.println("val[" + i + "]=" + val[i]);
        }
    }

    public void t5() {

        float f0 = 1.5f;
        float f1 = f0;
        System.out.println("+ = " + (f0 + f1));
        System.out.println("- = " + (f0 - f1));
        System.out.println("* = " + (f0 * f1));
        System.out.println("/ = " + (f0 / f1));
        System.out.println("% = " + (f0 % f1));
        float[] val = new float[10];
        f1 = 5 % f0 - 0.6f * (0.5f + 5) / 9.8f;
        System.out.println(f1);
        val[0] = f1;
        System.out.println(val[0]);
        for (int i = 0; i < val.length; i++) {
            val[i] = i % f0 - 0.6f * (0.5f + 5) / 9.8f;
        }
        System.out.println("strs.length=" + val.length);
        for (int i = 0; i < val.length; i++) {
            System.out.println("val[" + i + "]=" + val[i]);
        }
    }

    public void t6() {

        double f0 = 1.5f;
        double f1 = f0;
        System.out.println("+ = " + (f0 + f1));
        System.out.println("- = " + (f0 - f1));
        System.out.println("* = " + (f0 * f1));
        System.out.println("/ = " + (f0 / f1));
        System.out.println("% = " + (f0 % f1));
        double[] val = new double[10];
        f1 = 5 % f0 - 0.6f * (0.5f + 5) / 9.8f;
        System.out.println(f1);
        val[0] = f1;
        System.out.println(val[0]);
        for (int i = 0; i < val.length; i++) {
            val[i] = i % f0 - 0.6f * (0.5f + 5) / 9.8f;
        }
        System.out.println("strs.length=" + val.length);
        for (int i = 0; i < val.length; i++) {
            System.out.println("val[" + i + "]=" + val[i]);
        }
    }

    class T7class {

        Object obj_r;
    }

    void t7_1(T7class t7) {

        t7.obj_r = new Long(1);//在方法结束时，此对象应该被释放

        t7.obj_r = new Integer(2);

    }

    void t7() {
        for (int i = 0; i < 1000000; i++) {
            T7class t7 = new T7class();
            new T7class();
            t7_1(t7);
        }
    }

    void t8() throws RuntimeException {
        int i = 0;
        try {
            i = 1;
            if (true) {
                throw new Exception("exception test 1");
            }
            i = 0;
        } catch (Exception e) {
            i = 2;
        }
        System.out.println("i=" + i);

        i = 3;
        if (true) {
            //throw new RuntimeException();
        }

        System.out.println("i=" + i);
    }

    void t9() {
        int ch = 5;
        int v = 66;
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
        System.out.println("v=" + v);
    }

    void t10() {
        int ch = 1000;
        int v = 66;
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
        System.out.println("v=" + v);
    }

    void t11() {
        int[][][] a3 = new int[2][2][];
        a3[1][1] = new int[3];
        a3[1][1][2] = 9;
        System.out.println("arr print:" + a3[1][1][2]);
    }

    void t12() {
        String src = "a,,b,,c,d,,";
        src = src.substring(2);
        String splitor = ",,";
        String[] result = src.split(splitor);
        for (String s : result) {
            System.out.println("|" + s + "|");
        }
    }

    void t13() {
        File f = new File("./");
        System.out.println("path:" + f.getAbsolutePath());
        System.out.println(System.getProperty("file.separator"));
    }

    void t14() {
        try {
//            System.out.println("Waiting for input something:");
//            int a = System.in.read();
//            int b = System.in.read();
//            System.out.println(Integer.toHexString(a) + "," + Integer.toHexString(b));
        } catch (Exception ex) {
        }
    }

    void t15() {
        System.out.println("SB.sb1=" + SB.sb1);
        System.out.println("SB.sb2=" + SB.sb2);
        System.out.println("SA.sa1=" + SA.sa1);
        System.out.println("SA.sa2=" + SA.sa2);
    }

    void t16() {
        int SIZE = 16 * 1024 * 1024;
        int[] array = new int[SIZE];
        int x = 0;
        for (int i = 0; i < SIZE; i++) {
            x += array[i];
        }
    }

    void t17_sub(int n) {

        int[] x = new int[n];
        int[] y = new int[n];

        for (int i = 0; i < n; i++) {
            x[i] = i + 1;
        }
        for (int k = 0; k < 1000; k++) {
            for (int i = n - 1; i >= 0; --i) {
                y[i] += x[i];
            }
        }

        System.out.println(y[0] + " " + y[n - 1]);
    }

    void t17() {
        int n;
        long start;
        start = System.currentTimeMillis();
        n = 1000;
        t17_sub(1000);
        System.out.println("n = " + n + " ," + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        n = 3000;
        t17_sub(n);
        System.out.println("n = " + n + " ," + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        n = 5000;
        t17_sub(n);
        System.out.println("n = " + n + " ," + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        n = 7000;
        t17_sub(n);
        System.out.println("n = " + n + " ," + (System.currentTimeMillis() - start));

    }

    static int ca;
    int ia;

    void t18() {
        int b;
        for (int i = 0; i < 100000000; i++) {
            ca = i;
            ia = ca;
            b = ia;
        }
    }

    void t19() {
        Class c = int.class;
        System.out.println("name = " + c.getName());
        System.out.println("name=" + Integer.class);
    }

    void t20() {
        //以空格分割
        String str1 = "1 2 3          4 54       5 6";
        String[] numbers = str1.split(" +");
        for (String temp : numbers) {
            System.out.println(temp);
        }

        // 替换，替换所有的数字为*
        String str2 = "abd123:adad46587:asdadasadsfgi#%^^9090";
        System.out.println(str2.replaceAll("[0-9]", "*"));
        System.out.println(str2.replaceAll("\\d", "*"));

        // 匹配匹配邮箱
        String mail1 = "ababc@asa.com";
        String mail2 = "ababc@asa.com.cn";
        String mail3 = "ababc@asa";
        //        String mainRegex = "[0-9a-zA-Z_]+@[0-9a-zA-Z_]++(\\.[0-9a-zA-Z_]+{2,4})+";
        String mainRegex = "\\w+@\\w+(\\.\\w{2,4})+";

        System.out.println(mail1.matches(mainRegex));//true
        System.out.println(mail2.matches(mainRegex));//true
        System.out.println(mail3.matches(mainRegex));//false
    }
    
    enum COLOR{
        RED,GREEN,BLUE
    }
    
    enum RGB{
        RED(0xff0000),
        GREEN(0x00ff00),
        BLUE(0x0000ff)
        ;
        
        int argb;
        RGB(int rgb){
            this.argb=rgb;
        }
    }
    
    void t21(){
        System.out.println(RGB.class+" "+RGB.RED);
        System.out.println(COLOR.class+" "+COLOR.RED);
    }

    public static void main(String args[]) {
        Foo2 obj = new Foo2();
//        obj.t1();
//        obj.t2();
//        obj.t3();
//        obj.t4();
//        obj.t5();
//        obj.t6();
//        obj.t7();
//        obj.t8();
//        obj.t9();
//        obj.t10();
//        obj.t11();
//        obj.t12();
//        obj.t13();
//        obj.t14();
//        obj.t15();
//        obj.t16();
//        obj.t17();
//        obj.t18();
//        obj.t19();
        obj.t20();
//        obj.t21();
    }
}

class SA {

    static int sa1 = 5;
    static int sa2 = SB.sb2;
}

class SB {

    static int sb1 = SA.sa2;
    static int sb2 = 9;
}
