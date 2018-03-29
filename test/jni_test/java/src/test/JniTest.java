package test;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author gust
 */
public class JniTest {

    static {
        String s;
//        s = System.setProperty("java.library.path", "../../jni_test/cmake-build-release/");
//        System.out.println("java.library.path:"+s);
        s=System.getProperty("java.library.path");
        System.out.println("java.library.path:"+s);
        System.loadLibrary("jnitest"); //加载 linux:libjnitest.so  ,win: libjnitest.dll ,mac libjnitest.dylib
    }

    public static native int getValue(int old);

    public static void main(String[] args) {
        int a = 1;
        a = getValue(a);
        System.out.println(a);
    }
}
