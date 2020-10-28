/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect.vm;

/**
 * @author gust
 */
public class RefNative {


    public static native int refIdSize();

    public static native long obj2id(Object o);

    public static native Object id2obj(long objId);

    public static native Class[] getClasses();

    public static native Class getClassByName(String className);

    public static native int setLocalVal(long frame, int slot, byte type, long value, int bytes);

    public static native int getLocalVal(long frame, int slot, ValueType val);

    public static native Object newWithoutInit(Class cl);

    //thread method
    static public native Thread[] getThreads();

    static public native int getStatus(Thread t);

    static public native int suspendThread(Thread t);

    static public native int resumeThread(Thread t);

    static public native int getSuspendCount(Thread t);

    static public native int getFrameCount(Thread t);

    static public native int stopThread(Thread t, long objid);

    static public native long getStackFrame(Thread t);

    //
    public static native Object[] getGarbageReferedObjs();

    public static native int getGarbageStatus();

    public static native Class defineClass(ClassLoader cloader, String name, byte[] bytecodes, int offset, int length);

    public static native void addJarToClasspath(String jarFullPath);

    public static native Class findLoadedClass0(ClassLoader loader, String name);

    //
    //
    public static native long heap_calloc(int capacity);

    public static native void heap_free(long memAddr);

    public static native void heap_put_byte(long memAddr, int pos, byte value);

    public static native byte heap_get_byte(long memAddr, int pos);

    public static native void heap_put_short(long memAddr, int pos, short value);

    public static native short heap_get_short(long memAddr, int pos);

    public static native void heap_put_int(long memAddr, int pos, int value);

    public static native int heap_get_int(long memAddr, int pos);

    public static native void heap_put_long(long memAddr, int pos, long value);

    public static native long heap_get_long(long memAddr, int pos);

    public static native void heap_put_float(long memAddr, int pos, float value);

    public static native float heap_get_float(long memAddr, int pos);

    public static native void heap_put_double(long memAddr, int pos, double value);

    public static native double heap_get_double(long memAddr, int pos);

    public static native void heap_put_ref(long memAddr, int pos, Object value);

    public static native Object heap_get_ref(long memAddr, int pos);

    public static native void heap_copy(long srcMemAddr, int srcPos, long destMemAddr, int destPos, int length);

    public static native int heap_endian();

    public static native Class<?> getCallerClass();
}
