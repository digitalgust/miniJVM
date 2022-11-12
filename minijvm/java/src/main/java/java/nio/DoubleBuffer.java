/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

public abstract class DoubleBuffer
        extends Buffer
        implements Comparable<DoubleBuffer> {
    private final boolean readOnly;

    protected DoubleBuffer() {
        this.readOnly = false;
    }

    protected DoubleBuffer(boolean readOnly, int cap) {
        this.readOnly = readOnly;
        this.capacity = this.limit = cap;
    }

    public static DoubleBuffer allocate(int capacity) {
        return new DoubleBufferImpl(new double[capacity], 0, capacity, false);
    }

    public static DoubleBuffer wrap(double[] array) {
        return wrap(array, 0, array.length);
    }

    public static DoubleBuffer wrap(double[] array, int offset, int length) {
        return new DoubleBufferImpl(array, offset, length, false);
    }

    public abstract DoubleBuffer asReadOnlyBuffer();

    public abstract DoubleBuffer slice();

    protected abstract void doPut(int offset, double value);

    public abstract DoubleBuffer put(double[] src, int offset, int length);

    protected abstract double doGet(int offset);

    public abstract DoubleBuffer get(double[] dst, int offset, int length);

    public boolean hasArray() {
        return false;
    }

    public DoubleBuffer compact() {
        int remaining = remaining();

        if (position != 0) {
            DoubleBuffer b = slice();
            position = 0;
            put(b);
        }

        position = remaining;
        limit(capacity());

        return this;
    }

    public DoubleBuffer put(DoubleBuffer src) {
        if (src.hasArray()) {
            checkPut(position, src.remaining(), false);

            put(src.array(), src.arrayOffset() + src.position, src.remaining());
            src.position(src.position() + src.remaining());

            return this;
        } else {
            double[] buffer = new double[src.remaining()];
            src.get(buffer);
            return put(buffer);
        }
    }

    public int compareTo(DoubleBuffer o) {
        int end = (remaining() < o.remaining() ? remaining() : o.remaining());

        for (int i = 0; i < end; ++i) {
            double d = get(position + i) - o.get(o.position + i);
            if (d != 0) {
                return (int) d;
            }
        }
        return remaining() - o.remaining();
    }

    public boolean equals(Object o) {
        return o instanceof DoubleBuffer && compareTo((DoubleBuffer) o) == 0;
    }

    public double[] array() {
        throw new UnsupportedOperationException();
    }

    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    public DoubleBuffer put(int offset, double val) {
        checkPut(offset, 1, true);
        doPut(offset, val);
        return this;
    }

    public DoubleBuffer put(double val) {
        put(position, val);
        ++position;
        return this;
    }

    public DoubleBuffer put(double[] arr) {
        return put(arr, 0, arr.length);
    }

    public double get() {
        checkGet(position, 1, false);
        return doGet(position++);
    }

    public double get(int position) {
        checkGet(position, 1, true);
        return doGet(position);
    }

    public DoubleBuffer get(double[] dst) {
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
