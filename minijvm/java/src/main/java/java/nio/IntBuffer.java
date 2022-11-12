/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

public abstract class IntBuffer
        extends Buffer
        implements Comparable<IntBuffer> {
    private final boolean readOnly;

    protected IntBuffer(boolean readOnly, int cap) {
        this.readOnly = readOnly;
        this.capacity = this.limit = cap;
    }

    public static IntBuffer allocate(int capacity) {
        return new IntBufferImpl(new int[capacity], 0, capacity, false);
    }

    public static IntBuffer wrap(int[] array) {
        return wrap(array, 0, array.length);
    }

    public static IntBuffer wrap(int[] array, int offset, int length) {
        return new IntBufferImpl(array, offset, length, false);
    }

    public abstract IntBuffer asReadOnlyBuffer();

    public abstract IntBuffer slice();

    protected abstract void doPut(int offset, int value);

    public abstract IntBuffer put(int[] src, int offset, int length);

    protected abstract int doGet(int offset);

    public abstract IntBuffer get(int[] dst, int offset, int length);

    public boolean hasArray() {
        return false;
    }

    public IntBuffer compact() {
        int remaining = remaining();

        if (position != 0) {
            IntBuffer b = slice();
            position = 0;
            put(b);
        }

        position = remaining;
        limit(capacity());

        return this;
    }

    public IntBuffer put(IntBuffer src) {
        if (src.hasArray()) {
            checkPut(position, src.remaining(), false);

            put(src.array(), src.arrayOffset() + src.position, src.remaining());
            src.position(src.position() + src.remaining());

            return this;
        } else {
            int[] buffer = new int[src.remaining()];
            src.get(buffer);
            return put(buffer);
        }
    }

    public int compareTo(IntBuffer o) {
        int end = (remaining() < o.remaining() ? remaining() : o.remaining());

        for (int i = 0; i < end; ++i) {
            int d = get(position + i) - o.get(o.position + i);
            if (d != 0) {
                return (int) d;
            }
        }
        return remaining() - o.remaining();
    }

    public boolean equals(Object o) {
        return o instanceof IntBuffer && compareTo((IntBuffer) o) == 0;
    }

    public int[] array() {
        throw new UnsupportedOperationException();
    }

    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    public IntBuffer put(int offset, int val) {
        checkPut(offset, 1, true);
        doPut(offset, val);
        return this;
    }

    public IntBuffer put(int val) {
        put(position, val);
        ++position;
        return this;
    }

    public IntBuffer put(int[] arr) {
        return put(arr, 0, arr.length);
    }

    public int get() {
        checkGet(position, 1, false);
        return doGet(position++);
    }

    public int get(int position) {
        checkGet(position, 1, true);
        return doGet(position);
    }

    public IntBuffer get(int[] dst) {
        return get(dst, 0, dst.length);
    }

    protected void checkPut(int position, int amount, boolean absolute) {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }

        if (position < 0 || position + amount > limit) {
            throw absolute
                    ? new IndexOutOfBoundsException()
                    : new BufferOverflowException();
        }
    }

    protected void checkGet(int position, int amount, boolean absolute) {
        if (amount > limit - position) {
            throw absolute
                    ? new IndexOutOfBoundsException()
                    : new BufferUnderflowException();
        }
    }
}
