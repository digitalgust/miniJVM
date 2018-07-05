/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Arrays;

/**
 *
 * @author Gust
 */
public class LambdaTest {

    void t1() {
        String[] ss = new String[]{"one", "two", "three"};
        int a = 9;
        String ls = "local string";
        Arrays.asList(ss).forEach(s -> System.out.println(this + ":" + ls + a + ":" + s));
        Arrays.asList(ss).forEach(s -> System.out.println(this + ":" + ls + a + ":" + s));
    }

    public static void main(String args[]) {
        LambdaTest obj = new LambdaTest();
        obj.t1();
    }
}
