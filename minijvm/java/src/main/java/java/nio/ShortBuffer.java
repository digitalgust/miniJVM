/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

public abstract class ShortBuffer
        extends Buffer
        implements Comparable<ShortBuffer> {
    private final boolean readOnly;

    protected ShortBuffer() {
        this.readOnly = false;
    }

    protected ShortBuffer(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public static ShortBuffer allocate(int capacity) {
        return new ShortBufferImpl(new short[capacity], 0, capacity, false);
    }

    public static ShortBuffer wrap(short[] array) {
        return wrap(array, 0, array.length);
    }

    public static ShortBuffer wrap(short[] array, int offset, int length) {
        return new ShortBufferImpl(array, offset, length, false);
    }

    public abstract ShortBuffer asReadOnlyBuffer();

    public abstract ShortBuffer slice();

    protected abstract void doPut(int offset, short value);

    public abstract ShortBuffer put(short[] src, int offset, int length);

    protected abstract short doGet(int offset);

    public abstract ShortBuffer get(short[] dst, int offset, int length);

    public boolean hasArray() {
        return false;
    }

    public ShortBuffer compact() {
        int remaining = remaining();

        if (position != 0) {
            ShortBuffer b = slice();
            position = 0;
            put(b);
        }

        position = remaining;
        limit(capacity());

        return this;
    }

    public ShortBuffer put(ShortBuffer src) {
        if (src.hasArray()) {
            checkPut(position, src.remaining(), false);

            put(src.array(), src.arrayOffset() + src.position, src.remaining());
            src.position(src.position() + src.remaining());

            return this;
        } else {
            short[] buffer = new short[src.remaining()];
            src.get(buffer);
            return put(buffer);
        }
    }

    public int compareTo(ShortBuffer o) {
        int end = (remaining() < o.remaining() ? remaining() : o.remaining());

        for (int i = 0; i < end; ++i) {
            int d = get(position + i) - o.get(o.position + i);
            if (d != 0) {
                return d;
            }
        }
        return remaining() - o.remaining();
    }

    public boolean equals(Object o) {
        return o instanceof ShortBuffer && compareTo((ShortBuffer) o) == 0;
    }

    public short[] array() {
        throw new UnsupportedOperationException();
    }

    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    public ShortBuffer put(int offset, short val) {
        checkPut(offset, 1, true);
        doPut(offset, val);
        return this;
    }

    public ShortBuffer put(short val) {
        put(position, val);
        ++position;
        return this;
    }

    public ShortBuffer put(short[] arr) {
        return put(arr, 0, arr.length);
    }

    public short get() {
        checkGet(position, 1, false);
        return doGet(position++);
    }

    public short get(int position) {
        checkGet(position, 1, true);
        return doGet(position);
    }

    public ShortBuffer get(short[] dst) {
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
