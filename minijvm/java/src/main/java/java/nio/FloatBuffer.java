/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio;

public abstract class FloatBuffer
        extends Buffer
        implements Comparable<FloatBuffer> {
    private final boolean readOnly;

    protected FloatBuffer() {
        this.readOnly = false;
    }

    protected FloatBuffer(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public static FloatBuffer allocate(int capacity) {
        return new FloatBufferImpl(new float[capacity], 0, capacity, false);
    }

    public static FloatBuffer wrap(float[] array) {
        return wrap(array, 0, array.length);
    }

    public static FloatBuffer wrap(float[] array, int offset, int length) {
        return new FloatBufferImpl(array, offset, length, false);
    }

    public abstract FloatBuffer asReadOnlyBuffer();

    public abstract FloatBuffer slice();

    protected abstract void doPut(int offset, float value);

    public abstract FloatBuffer put(float[] src, int offset, int length);

    protected abstract float doGet(int offset);

    public abstract FloatBuffer get(float[] dst, int offset, int length);

    public boolean hasArray() {
        return false;
    }

    public FloatBuffer compact() {
        int remaining = remaining();

        if (position != 0) {
            FloatBuffer b = slice();
            position = 0;
            put(b);
        }

        position = remaining;
        limit(capacity());

        return this;
    }

    public FloatBuffer put(FloatBuffer src) {
        if (src.hasArray()) {
            checkPut(position, src.remaining(), false);

            put(src.array(), src.arrayOffset() + src.position, src.remaining());
            src.position(src.position() + src.remaining());

            return this;
        } else {
            float[] buffer = new float[src.remaining()];
            src.get(buffer);
            return put(buffer);
        }
    }

    public int compareTo(FloatBuffer o) {
        int end = (remaining() < o.remaining() ? remaining() : o.remaining());

        for (int i = 0; i < end; ++i) {
            float d = get(position + i) - o.get(o.position + i);
            if (d != 0) {
                return (int) d;
            }
        }
        return remaining() - o.remaining();
    }

    public boolean equals(Object o) {
        return o instanceof FloatBuffer && compareTo((FloatBuffer) o) == 0;
    }

    public float[] array() {
        throw new UnsupportedOperationException();
    }

    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    public FloatBuffer put(int offset, float val) {
        checkPut(offset, 1, true);
        doPut(offset, val);
        return this;
    }

    public FloatBuffer put(float val) {
        put(position, val);
        ++position;
        return this;
    }

    public FloatBuffer put(float[] arr) {
        return put(arr, 0, arr.length);
    }

    public float get() {
        checkGet(position, 1, false);
        return doGet(position++);
    }

    public float get(int position) {
        checkGet(position, 1, true);
        return doGet(position);
    }

    public FloatBuffer get(float[] dst) {
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
