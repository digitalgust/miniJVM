/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package sun.misc;

import org.mini.reflect.vm.RConst;
import org.mini.reflect.vm.RefNative;

import java.lang.reflect.Field;

public final class Unsafe {
    private void Unsafe() {
    }

    private static final Unsafe theUnsafe = new Unsafe();

    public static Unsafe getUnsafe() {
        return theUnsafe;
    }

    public long allocateMemory(long bytes) {
        return RefNative.heap_calloc((int) bytes);
    }

    public void setMemory(Object base, long offset, long count, byte value) {
        long b = objectFieldBase(base);
        for (int i = 0; i < count; i++) {
            RefNative.heap_put_byte(b + offset + i, 0, value);
        }
    }

    public void freeMemory(long address) {
        RefNative.heap_free(address);
    }

    public byte getByte(long address) {
        return RefNative.heap_get_byte(address, 0);
    }

    public void putByte(long address, byte x) {
        RefNative.heap_put_byte(address, 0, x);
    }

    public short getShort(long address) {
        return RefNative.heap_get_short(address, 0);
    }

    public void putShort(long address, short x) {
        RefNative.heap_put_short(address, 0, x);
    }

    public char getChar(long address) {
        return (char) (RefNative.heap_get_short(address, 0) & 0xffff);
    }

    public void putChar(long address, char x) {
        RefNative.heap_put_short(address, 0, (short) x);
    }

    public int getInt(long address) {
        return RefNative.heap_get_int(address, 0);
    }

    public void putInt(long address, int x) {
        RefNative.heap_put_int(address, 0, x);
    }

    public long getLong(long address) {
        return RefNative.heap_get_long(address, 0);
    }

    public void putLong(long address, long x) {
        RefNative.heap_put_long(address, 0, x);
    }

    public float getFloat(long address) {
        return RefNative.heap_get_float(address, 0);
    }

    public void putFloat(long address, float x) {
        RefNative.heap_put_float(address, 0, x);
    }

    public double getDouble(long address) {
        return RefNative.heap_get_double(address, 0);
    }

    public void putDouble(long address, double x) {
        RefNative.heap_put_double(address, 0, x);
    }

    public boolean getBooleanVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_byte(base + offset, 0) != 0;
    }

    public void putBooleanVolatile(Object o, long offset, boolean x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_byte(base + offset, 0, x ? (byte) 1 : (byte) 0);
    }

    public byte getByteVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_byte(base + offset, 0);
    }


    public void putByteVolatile(Object o, long offset, byte x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_byte(base + offset, 0, x);
    }

    public short getShortVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_short(base + offset, 0);
    }

    public void putShortVolatile(Object o, long offset, short x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_short(base + offset, 0, x);
    }

    public char getCharVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return (char) RefNative.heap_get_short(base + offset, 0);
    }

    public void putCharVolatile(Object o, long offset, char x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_short(base + offset, 0, (short) x);
    }

    public int getIntVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_int(base + offset, 0);
    }

    public void putIntVolatile(Object o, long offset, int x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_int(base + offset, 0, x);
    }

    public float getFloatVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_float(base + offset, 0);
    }

    public void putFloatVolatile(Object o, long offset, float x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_float(base + offset, 0, x);
    }

    public double getDoubleVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_double(base + offset, 0);
    }

    public void putDoubleVolatile(Object o, long offset, double x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_double(base + offset, 0, x);
    }

    public long getLongVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_long(base + offset, 0);
    }

    public void putLongVolatile(Object o, long offset, long x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_long(base + offset, 0, x);
    }

    public Object getObjectVolatile(Object o, long offset) {
        long base = objectFieldBase(o);
        return RefNative.heap_get_ref(base + offset, 0);
    }

    public void putObjectVolatile(Object o, long offset, Object x) {
        long base = objectFieldBase(o);
        RefNative.heap_put_ref(base + offset, 0, x);
    }

    public int getInt(Object o, long offset) {
        return getIntVolatile(o, offset);
    }

    public void putInt(Object o, long offset, int x) {
        putIntVolatile(o, offset, x);
    }

    public short getShort(Object o, long offset) {
        return getShortVolatile(o, offset);
    }

    public void putShort(Object o, long offset, short x) {
        putShortVolatile(o, offset, x);
    }

    public byte getByte(Object o, long offset) {
        return getByteVolatile(o, offset);
    }

    public void putByte(Object o, long offset, byte x) {
        putByteVolatile(o, offset, x);
    }

    public char getChar(Object o, long offset) {
        return getCharVolatile(o, offset);
    }

    public void putChar(Object o, long offset, char x) {
        putCharVolatile(o, offset, x);
    }

    public long getLong(Object o, long offset) {
        return getLongVolatile(o, offset);
    }

    public void putLong(Object o, long offset, long x) {
        putLongVolatile(o, offset, x);
    }

    public double getDouble(Object o, long offset) {
        return getDoubleVolatile(o, offset);
    }

    public void putDouble(Object o, long offset, double x) {
        putDoubleVolatile(o, offset, x);
    }

    public float getFloat(Object o, long offset) {
        return getFloatVolatile(o, offset);
    }

    public void putFloat(Object o, long offset, float x) {
        putFloatVolatile(o, offset, x);
    }

    public Object getObject(Object o, long offset) {
        return getObjectVolatile(o, offset);
    }

    public void putObject(Object o, long offset, Object x) {
        putObjectVolatile(o, offset, x);
    }

    public void putOrderedLong(Object o, long offset, long x) {
        putLong(o, offset, x);
    }

    public void putOrderedInt(Object o, long offset, int x) {
        putInt(o, offset, x);
    }

    public void putOrderedObject(Object o, long offset, Object x) {
        putObject(o, offset, x);
    }

    public long objectFieldOffset(Field field) {
        return objectFieldOffset(field.getRefField().fieldId);
    }

    public Object staticFieldBase(Field field) {
        return null;
    }

    public long staticFieldOffset(Field field) {
        return staticFieldOffset(field.getRefField().fieldId);
    }

    public int arrayBaseOffset(Class<?> arrayClass) {
        return 0;
    }

    public int arrayIndexScale(Class<?> arrayClass) {
        return RConst.getBytes((byte) arrayClass.getName().charAt(1));
    }

    public void copyMemory(Object srcBase, long srcOffset,
                           Object destBase, long destOffset,
                           long count) {
        long srcb = objectFieldBase(srcBase);
        long destb = objectFieldBase(destBase);
        RefNative.heap_copy(srcb + srcOffset, 0, destb + destOffset, 0, (int) count);
    }

    public void copyMemory(long src, long dst, long count) {
        copyMemory(null, src, null, dst, count);
    }

    public void throwException(Throwable t) throws Throwable {
        throw t;
    }


    private native long objectFieldOffset(long native_field);

    private native long objectFieldBase(Object obj);

    private native long staticFieldOffset(long native_static_field);

    public native long getAddress(long address);

    public native void putAddress(long address, long x);

    public native boolean compareAndSwapInt(Object o, long offset, int old, int new_);

    public native boolean compareAndSwapLong(Object o, long offset, long old, long new_);

    public native boolean compareAndSwapObject(Object o, long offset, Object old, Object new_);

//    public native void park(boolean absolute, long time);
//
//    public native void unpark(Object target);


}
