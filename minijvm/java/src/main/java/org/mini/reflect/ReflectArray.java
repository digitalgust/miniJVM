/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RefNative;

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
    //不可随意改动字段类型及名字，要和native一起改
    //native field name ,dont change name
//    public byte typeTag;
//    long body_addr;
//    public int length;

    //
    DirectMemObj dmo;

    /**
     *
     * @param array
     */
    public ReflectArray(long array) {
        arrayId = array;
//        mapArray(arrayId);
        byte typeTag = getTypeTag(array);
        long body_addr = getBodyPtr(array);
        int length = getLength(array);
        dmo = new DirectMemObj(body_addr, length, typeTag);
    }

    public void setValObj(int index, Object val) {
        dmo.setValObj(index, val);
    }

    public Object getValObj(int index) {
        return dmo.getValObj(index);
    }

    public static long getBodyPtr(Object array) {
        if (array == null || !array.getClass().isArray()) {
            return 0;
        }
        return getBodyPtr(RefNative.obj2id(array));
    }

//    final native void mapArray(long classId);
    public static native int getLength(long arr);

    public static native byte getTypeTag(long arr);

    public static native long getBodyPtr(long array);

    /*
     * Private
     */
    public static native Object newArray(Class componentType, int length);

    public static native Object multiNewArray(Class componentType, int[] dimensions) throws IllegalArgumentException;
}
