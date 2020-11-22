/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

import org.mini.reflect.ReflectArray;

class IntBufferImpl extends IntBuffer {
    protected int[] array;

    int arrayOffset;

    IntBufferImpl(int[] array, int offset, int length, boolean readOnly) {
        super(readOnly);

        this.array = array;
        this.arrayOffset = offset;
        this.capacity = length;
        this.limit = length;
        this.position = 0;
        address = ReflectArray.getBodyPtr(array);
    }

    public IntBufferImpl asReadOnlyBuffer() {
        IntBufferImpl b = new IntBufferImpl(array, arrayOffset, capacity, false);
        b.position(position());
        b.limit(limit());
        return b;
    }

    public boolean hasArray() {
        return true;
    }

    public int[] array() {
        return array;
    }

    public IntBufferImpl slice() {
        return new IntBufferImpl
                (array, arrayOffset + position, remaining(), true);
    }

    public int arrayOffset() {
        return arrayOffset;
    }

    protected void doPut(int position, int val) {
        array[arrayOffset + position] = val;
    }

    public IntBufferImpl put(IntBufferImpl src) {
        int length = src.remaining();
        checkPut(position, length, false);
        src.get(array, arrayOffset + position, length);
        position += length;
        return this;
    }

    public IntBufferImpl put(int[] src, int offset, int length) {
        checkPut(position, length, false);

        System.arraycopy(src, offset, array, arrayOffset + position, length);
        position += length;

        return this;
    }

    public IntBufferImpl get(int[] dst, int offset, int length) {
        checkGet(position, length, false);

        System.arraycopy(array, arrayOffset + position, dst, offset, length);
        position += length;

        return this;
    }

    protected int doGet(int position) {
        return array[arrayOffset + position];
    }

    public String toString() {
        return "(IntBufferImplImpl with array: " + array
                + " arrayOffset: " + arrayOffset
                + " position: " + position
                + " limit: " + limit
                + " capacity: " + capacity + ")";
    }
}
