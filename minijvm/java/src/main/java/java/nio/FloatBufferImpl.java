/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

import org.mini.reflect.ReflectArray;

class FloatBufferImpl extends FloatBuffer {
    protected float[] array;

    int arrayOffset;

    FloatBufferImpl(float[] array, int offset, int length, boolean readOnly) {
        super(readOnly);

        this.array = array;
        this.arrayOffset = offset;
        this.capacity = length;
        this.limit = length;
        this.position = 0;
        address = ReflectArray.getBodyPtr(array);
    }

    public FloatBuffer asReadOnlyBuffer() {
        FloatBuffer b = new FloatBufferImpl(array, arrayOffset, capacity, true);
        b.position(position());
        b.limit(limit());
        return b;
    }

    public boolean hasArray() {
        return true;
    }

    public float[] array() {
        return array;
    }

    public FloatBuffer slice() {
        return new FloatBufferImpl
                (array, arrayOffset + position, remaining(), false);
    }

    public int arrayOffset() {
        return arrayOffset;
    }

    protected void doPut(int position, float val) {
        array[arrayOffset + position] = val;
    }

    public FloatBuffer put(FloatBuffer src) {
        int length = src.remaining();
        checkPut(position, length, false);
        src.get(array, arrayOffset + position, length);
        position += length;
        return this;
    }

    public FloatBuffer put(float[] src, int offset, int length) {
        checkPut(position, length, false);

        System.arraycopy(src, offset, array, arrayOffset + position, length);
        position += length;

        return this;
    }

    public FloatBuffer get(float[] dst, int offset, int length) {
        checkGet(position, length, false);

        System.arraycopy(array, arrayOffset + position, dst, offset, length);
        position += length;

        return this;
    }

    protected float doGet(int position) {
        return array[arrayOffset + position];
    }

    public String toString() {
        return "(FloatBufferImpl with array: " + array
                + " arrayOffset: " + arrayOffset
                + " position: " + position
                + " limit: " + limit
                + " capacity: " + capacity + ")";
    }
}
