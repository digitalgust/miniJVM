/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RConst;
import org.mini.reflect.vm.RefNative;

import java.lang.reflect.Array;

/**
 * @author Gust
 */
public class DirectMemObj {

    /**
     * on DirectMemObj finalize call
     */
    public interface DMOFinalizer {

        void onFinalize();
    }

    //dont change field name
    long memAddr;
    int length;
    char typeDesc; //'1','2','4','8','R'

    //
    public byte typeTag;  //RConst.TAG...
    DMOFinalizer finalizer;

    //

    /**
     * @param mem_addr
     * @param len
     */
    public DirectMemObj(long mem_addr, int len) {
        this(mem_addr, len, RConst.TAG_BYTE);
    }

    /**
     * It's a direct memory access obj , danger for write operation , danger for
     * read data safe
     * <p>
     * this class main aim to process native lib memory pointer.
     * <p>
     * like an array or ByteBuffer
     * <p>
     * typeTag is RConst.xxxtag
     *
     * @param mem_addr
     * @param len
     * @param type
     */
    public DirectMemObj(long mem_addr, int len, byte type) {
        this.memAddr = mem_addr;
        this.typeTag = type;
        this.length = len;
        typeDesc = RConst.getBytes(type);
    }

    public void setFinalizer(DMOFinalizer finalizer) {
        this.finalizer = finalizer;
    }

    /**
     *
     */
    @Override
    public void finalize() {
        try {
            super.finalize();
        } catch (Throwable ex) {
        }
        if (finalizer != null) {
            finalizer.onFinalize();
            System.out.println("DMO finalized");
        }
    }

    public static <T> DirectMemObj allocate(int capacity) {
        Object arr = Array.newInstance(byte.class, capacity);
        return wrap(arr);
    }

    /**
     * direct allocate memory from heap
     *
     * @param capacity in byte
     * @return
     */
    public static <T> DirectMemObj allocate(int capacity, Class clazz) {
        Object arr = Array.newInstance(clazz, capacity);
        return wrap(arr);
    }

    Object arrayHolder;//持有warp的数组,防止回收了

    public static <T> DirectMemObj wrap(T array) {
        return wrap(array, 0, Array.getLength(array));
    }

    public static <T> DirectMemObj wrap(T array, int offset, int length) {

        long ptr = ReflectArray.getBodyPtr(array);
        int bytes = 1;
        byte tag = 0;
        if (array instanceof byte[]) {
            bytes = 1;
            tag = RConst.TAG_BYTE;
        } else if (array instanceof short[]) {
            bytes = 2;
            tag = RConst.TAG_SHORT;
        } else if (array instanceof char[]) {
            bytes = 2;
            tag = RConst.TAG_CHAR;
        } else if (array instanceof int[]) {
            bytes = 4;
            tag = RConst.TAG_INT;
        } else if (array instanceof long[]) {
            bytes = 8;
            tag = RConst.TAG_LONG;
        } else if (array instanceof float[]) {
            bytes = 4;
            tag = RConst.TAG_FLOAT;
        } else if (array instanceof double[]) {
            bytes = 8;
            tag = RConst.TAG_DOUBLE;
        } else if (array instanceof boolean[]) {
            bytes = 1;
            tag = RConst.TAG_BOOLEAN;
        } else {
            bytes = RefNative.refIdSize();
            tag = RConst.TAG_OBJECT;
        }
        if (ptr == 0 || tag == 0 || ReflectArray.getLength(array) > offset + length) {
            throw new IllegalArgumentException("espected array " + Long.toHexString(ptr) + " tag:" + (char) tag);
        }

        ptr += offset * bytes;
        DirectMemObj dmo = new DirectMemObj(ptr, length, tag);
        dmo.arrayHolder = array;
        return dmo;
    }

    /**
     * return data start memory address
     *
     * @return
     */
    public long getDataPtr() {
        return memAddr;
    }

    public int getLength() {
        return length;
    }

    public byte getTypeTag() {
        return typeTag;
    }

    public char getDesc() {
        return typeDesc;
    }

    public void setValObj(int index, Object val) {
        switch (typeDesc) {
            case '1': {
                if (typeTag == RConst.TAG_BOOLEAN) {
                    setVal(index, ((Boolean) val) ? 1 : 0);

                } else {
                    setVal(index, (Byte) val);
                }
                break;
            }
            case '2': {
                if (typeTag == RConst.TAG_CHAR) {
                    setVal(index, (Character) val);

                } else {
                    setVal(index, (Byte) val);
                }
                break;
            }
            case '4': {
                setVal(index, (Integer) val);
                break;
            }
            case '8': {
                setVal(index, (Long) val);
                break;
            }
            case 'R': {
                setVal(index, RefNative.obj2id(val));
                break;
            }
            default: {

                throw new IllegalArgumentException();
            }
        }
    }

    public Object getValObj(int index) {

        switch (typeDesc) {
            case '1':
                if (typeTag == RConst.TAG_BOOLEAN) {
                    return getVal(index) != 0;
                }
                return ((byte) getVal(index));

            case '2':
                if (typeTag == RConst.TAG_CHAR) {
                    return ((char) getVal(index));
                }
                return ((short) getVal(index));
            case '4':
                return ((int) getVal(index));
            case '8':
                return getVal(index);
            case 'R': {
                long objptr = getVal(index);
                if (objptr != 0) {
                    return RefNative.id2obj(objptr);
                }
                return null;
            }
        }
        throw new IllegalArgumentException();
    }

    public Object getObject(int index) {
        return RefNative.id2obj(getVal(index));
    }

    public byte getByte(int index) {
        return (byte) getVal(index);
    }

    public short getShort(int index) {
        return (short) getVal(index);
    }

    public char getChar(int index) {
        return (char) getVal(index);
    }

    public int getInt(int index) {
        return (int) getVal(index);
    }

    public long getLong(int index) {
        return (long) getVal(index);
    }

    public float getFloat(int index) {
        return Float.intBitsToFloat((int) getVal(index));
    }

    public double getDouble(int index) {
        return Double.longBitsToDouble(getVal(index));
    }

    public void setByte(int index, byte val) {
        setVal(index, val);
    }

    public void setShort(int index, short val) {
        setVal(index, val);
    }

    public void setChar(int index, char val) {
        setVal(index, val);
    }

    public void setInt(int index, int val) {
        setVal(index, val);
    }

    public void setLong(int index, long val) {
        setVal(index, val);
    }

    public void setFloat(int index, float val) {
        setVal(index, Float.floatToIntBits(val));
    }

    public void setDouble(int index, double val) {
        setVal(index, Double.doubleToLongBits(val));
    }

    public void copyTo(Object targetArray) {
        copyTo(0, targetArray, 0, length);
    }

    public void copyTo(int src_offset, Object targetArray, int tgt_offset, int len) {
        Class cls = targetArray.getClass();
        //System.out.println("arrtype:"+cls.getName());
        if (cls.isArray() && cls.getName().charAt(1) == typeTag) {
            copyTo0(src_offset, targetArray, tgt_offset, len);
        } else {
            throw new IllegalArgumentException("espected array and same datatype: " + typeTag);
        }
    }

    public void copyFrom(Object srcArray) {
        copyFrom(0, srcArray, 0, length);
    }

    public void copyFrom(int src_offset, Object srcArray, int tgt_offset, int len) {
        Class cls = srcArray.getClass();
        //System.out.println("arrtype:"+cls.getName());
        if (cls.isArray() && cls.getName().charAt(1) == typeTag) {
            copyFrom0(src_offset, srcArray, tgt_offset, len);
        } else {
            throw new IllegalArgumentException("espected array and same datatype: " + typeTag);
        }
    }

    private native long getVal(int index);

    private native void setVal(int index, long val);

    private native void copyTo0(int src_offset, Object tgtArr, int tgt_offset, int len);

    private native void copyFrom0(int src_offset, Object srcArr, int tgt_offset, int len);


}
