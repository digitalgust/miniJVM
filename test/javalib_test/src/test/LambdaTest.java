/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Gust
 */
public class LambdaTest {

    void t1() {
        String[] ss = new String[]{"one", "two", "three"};
        int a = 4;
        String ls = "local string";
        Arrays.asList(ss).forEach(s -> System.out.println(this + ":" + ls + a + ":" + s));
        List<String> list = new ArrayList();
        list.addAll(Arrays.asList(ss));
        list.removeIf(s -> s.length() < a);
        list.forEach(s -> System.out.println(":" + s));
    }

    public static void main(String args[]) {
        LambdaTest obj = new LambdaTest();
        obj.t1();
    }
}
