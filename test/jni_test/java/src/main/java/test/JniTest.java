package test;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 * @author gust
 */
public class JniTest {

    static {
        String s;
        s = System.getProperty("java.library.path");
        System.out.println("java.library.path:" + s);
        System.loadLibrary("jnitest"); //加载 linux:libjnitest.so  ,win: libjnitest.dll ,mac libjnitest.dylib
    }

    public static native int getValue(long time, int v, String s);

    public static native void print(int v);

    public static void main(String[] args) {
        int a = 1;
        long t = System.currentTimeMillis();
        a = getValue(t, a, "JNI HELLO");
        print(a);
    }
}
