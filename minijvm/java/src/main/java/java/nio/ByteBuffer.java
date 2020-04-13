/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

public abstract class ByteBuffer
        extends Buffer
        implements Comparable<ByteBuffer> {

    protected ByteBuffer(boolean readOnly) {
        this.readonly = readOnly;
    }

    public static ByteBuffer allocate(int capacity) {
        return new ByteBufferImpl(capacity);
    }

    public static ByteBuffer allocateDirect(int capacity) {
        return new ByteBufferImpl(capacity);
    }

    public static ByteBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }

    public static ByteBuffer wrap(byte[] array, int offset, int length) {
        return new ByteBufferImpl(array, offset, length, false);
    }

    public boolean hasArray() {
        return false;
    }

    public byte[] array() {
        return null;
    }


    public int arrayOffset() {
        return 0;
    }


    public abstract CharBuffer asCharBuffer();

    //Creates a view of this byte buffer as a char buffer.
    public abstract DoubleBuffer asDoubleBuffer();

    //Creates a view of this byte buffer as a double buffer.
    public abstract FloatBuffer asFloatBuffer();

    //Creates a view of this byte buffer as a float buffer.
    public abstract IntBuffer asIntBuffer();

    //Creates a view of this byte buffer as an int buffer.
    public abstract LongBuffer asLongBuffer();

    //Creates a view of this byte buffer as a long buffer.
    public abstract ByteBuffer asReadOnlyBuffer();

    //Creates a new, read-only byte buffer that shares this buffer's content.
    public abstract ShortBuffer asShortBuffer();

    //Creates a view of this byte buffer as a short buffer.
    public abstract ByteBuffer compact();

    //Compacts this buffer  (optional operation).
    abstract public int compareTo(ByteBuffer that);
    //Compares this buffer to another.


    public abstract ByteBuffer slice();

    public abstract ByteBuffer duplicate();


    public ByteBuffer put(byte[] arr) {
        return put(arr, 0, arr.length);
    }

    public ByteBuffer put(ByteBuffer src) {
        return this;
    }

    public abstract ByteBuffer put(byte[] arr, int offset, int len);

    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public abstract ByteBuffer get(byte[] dst, int offset, int length);


    public ByteBuffer order(ByteOrder order) {
        if (order != ByteOrder.LITTLE_ENDIAN) throw new UnsupportedOperationException();
        return this;
    }

    public ByteOrder order() {
        return ByteOrder.LITTLE_ENDIAN;
    }

    public abstract byte get(int index);

    public abstract byte get();

    //Absolute get method.
    public abstract char getChar();

    //Relative get method for reading a char value.
    public abstract char getChar(int index);

    //Absolute get method for reading a char value.
    public abstract double getDouble();

    //Relative get method for reading a double value.
    public abstract double getDouble(int index);

    //Absolute get method for reading a double value.
    public abstract float getFloat();

    //Relative get method for reading a float value.
    public abstract float getFloat(int index);

    //Absolute get method for reading a float value.
    public abstract int getInt();

    //Relative get method for reading an int value.
    public abstract int getInt(int index);

    //Absolute get method for reading an int value.
    public abstract long getLong();

    //Relative get method for reading a long value.
    public abstract long getLong(int index);

    //Absolute get method for reading a long value.
    public abstract short getShort();

    //Relative get method for reading a short value.
    public abstract short getShort(int index);

    //Absolute get method for reading a short value.

    public abstract ByteBuffer put(byte b);

    public abstract ByteBuffer put(int index, byte b);

    //Absolute put method  (optional operation);.
    public abstract ByteBuffer putChar(char value);

    //Relative put method for writing a char value  (optional operation);.
    public abstract ByteBuffer putChar(int index, char value);

    //Absolute put method for writing a char value  (optional operation);.
    public abstract ByteBuffer putDouble(double value);

    //Relative put method for writing a double value  (optional operation);.
    public abstract ByteBuffer putDouble(int index, double value);

    //Absolute put method for writing a double value  (optional operation);.
    public abstract ByteBuffer putFloat(float value);

    //Relative put method for writing a float value  (optional operation);.
    public abstract ByteBuffer putFloat(int index, float value);

    //Absolute put method for writing a float value  (optional operation);.
    public abstract ByteBuffer putInt(int value);

    //Relative put method for writing an int value  (optional operation);.
    public abstract ByteBuffer putInt(int index, int value);

    //Absolute put method for writing an int value  (optional operation);.
    public abstract ByteBuffer putLong(int index, long value);

    //Absolute put method for writing a long value  (optional operation);.
    public abstract ByteBuffer putLong(long value);

    //Relative put method for writing a long value  (optional operation);.
    public abstract ByteBuffer putShort(int index, short value);

    //Absolute put method for writing a short value  (optional operation);.
    public abstract ByteBuffer putShort(short value);

    //Relative put method for writing a short value  (optional operation);.


}
