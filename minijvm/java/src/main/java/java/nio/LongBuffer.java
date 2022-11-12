/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

public abstract class LongBuffer
        extends Buffer
        implements Comparable<LongBuffer> {
    private final boolean readOnly;

    protected LongBuffer() {
        this.readOnly = false;
    }

    protected LongBuffer(boolean readOnly, int cap) {
        this.readOnly = readOnly;
        this.capacity = this.limit = cap;
    }

    public static LongBuffer allocate(int capacity) {
        return new LongBufferImpl(new long[capacity], 0, capacity, false);
    }

    public static LongBuffer wrap(long[] array) {
        return wrap(array, 0, array.length);
    }

    public static LongBuffer wrap(long[] array, int offset, int length) {
        return new LongBufferImpl(array, offset, length, false);
    }

    public abstract LongBuffer asReadOnlyBuffer();

    public abstract LongBuffer slice();

    protected abstract void doPut(int offset, long value);

    public abstract LongBuffer put(long[] src, int offset, int length);

    protected abstract long doGet(int offset);

    public abstract LongBuffer get(long[] dst, int offset, int length);

    public boolean hasArray() {
        return false;
    }

    public LongBuffer compact() {
        int remaining = remaining();

        if (position != 0) {
            LongBuffer b = slice();
            position = 0;
            put(b);
        }

        position = remaining;
        limit(capacity());

        return this;
    }

    public LongBuffer put(LongBuffer src) {
        if (src.hasArray()) {
            checkPut(position, src.remaining(), false);

            put(src.array(), src.arrayOffset() + src.position, src.remaining());
            src.position(src.position() + src.remaining());

            return this;
        } else {
            long[] buffer = new long[src.remaining()];
            src.get(buffer);
            return put(buffer);
        }
    }

    public int compareTo(LongBuffer o) {
        int end = (remaining() < o.remaining() ? remaining() : o.remaining());

        for (int i = 0; i < end; ++i) {
            long d = get(position + i) - o.get(o.position + i);
            if (d != 0) {
                return (int) d;
            }
        }
        return remaining() - o.remaining();
    }

    public boolean equals(Object o) {
        return o instanceof LongBuffer && compareTo((LongBuffer) o) == 0;
    }

    public long[] array() {
        throw new UnsupportedOperationException();
    }

    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    public LongBuffer put(int offset, long val) {
        checkPut(offset, 1, true);
        doPut(offset, val);
        return this;
    }

    public LongBuffer put(long val) {
        put(position, val);
        ++position;
        return this;
    }

    public LongBuffer put(long[] arr) {
        return put(arr, 0, arr.length);
    }

    public long get() {
        checkGet(position, 1, false);
        return doGet(position++);
    }

    public long get(int position) {
        checkGet(position, 1, true);
        return doGet(position);
    }

    public LongBuffer get(long[] dst) {
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
