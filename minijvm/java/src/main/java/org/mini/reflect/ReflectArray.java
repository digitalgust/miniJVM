/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

/**
 * 反射一个数组实例，如
 *
 * int[] a=new int[3]; Array rarr = new Array(RefNative.obj2id(a)); int
 * v=(int)rarr.getVal(2);
 *
 * 即可通过rarr 访问内数组成员
 *
 * @author gust
 */
public class ReflectArray {

    public long arrayId;

    //

    /**
     *
     * @param array
     */
    public ReflectArray(long array) {
        arrayId = array;
    }


    public static native int getLength(Object arr);

    public static native byte getTypeTag(Object arr);

    public static native long getBodyPtr(Object array);

    /*
     * Private
     */
    public static native Object newArray(Class componentType, int length);

    public static native Object multiNewArray(Class componentType, int[] dimensions) throws IllegalArgumentException;
}
