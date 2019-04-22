/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect.vm;

/**
 *
 * @author gust
 */
public class RefNative {

    ClassLoader cl;

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
}
