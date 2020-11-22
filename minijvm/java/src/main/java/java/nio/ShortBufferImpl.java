/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

import org.mini.reflect.ReflectArray;

class ShortBufferImpl extends ShortBuffer {
    protected short[] array;

    int arrayOffset;

    ShortBufferImpl(short[] array, int offset, int length, boolean readOnly) {
        super(readOnly);

        this.array = array;
        this.arrayOffset = offset;
        this.capacity = length;
        this.limit = length;
        this.position = 0;
        address = ReflectArray.getBodyPtr(array);
    }

    public ShortBuffer asReadOnlyBuffer() {
        ShortBuffer b = new ShortBufferImpl(array, arrayOffset, capacity, true);
        b.position(position());
        b.limit(limit());
        return b;
    }

    public boolean hasArray() {
        return true;
    }

    public short[] array() {
        return array;
    }

    public ShortBuffer slice() {
        return new ShortBufferImpl
                (array, arrayOffset + position, remaining(), false);
    }

    public int arrayOffset() {
        return arrayOffset;
    }

    protected void doPut(int position, short val) {
        array[arrayOffset + position] = val;
    }

    public ShortBuffer put(ShortBuffer src) {
        int length = src.remaining();
        checkPut(position, length, false);
        src.get(array, arrayOffset + position, length);
        position += length;
        return this;
    }

    public ShortBuffer put(short[] src, int offset, int length) {
        checkPut(position, length, false);

        System.arraycopy(src, offset, array, arrayOffset + position, length);
        position += length;

        return this;
    }

    public ShortBuffer get(short[] dst, int offset, int length) {
        checkGet(position, length, false);

        System.arraycopy(array, arrayOffset + position, dst, offset, length);
        position += length;

        return this;
    }

    protected short doGet(int position) {
        return array[arrayOffset + position];
    }

    public String toString() {
        return "(ShortBufferImpl with array: " + array
                + " arrayOffset: " + arrayOffset
                + " position: " + position
                + " limit: " + limit
                + " capacity: " + capacity + ")";
    }
}
