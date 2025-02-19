package java.nio;


import org.mini.reflect.ReflectArray;
import org.mini.vm.RefNative;

class ByteBufferImpl extends ByteBuffer {
    protected byte[] array;

    int baseOffset;

    protected ByteBufferImpl(byte[] arr, int start, int length, boolean readOnly) {
        super(readOnly);
        if (arr != null && (start < 0 || start + length > arr.length)) {
            throw new IndexOutOfBoundsException("Invalid start or length parameters");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }
        if (arr == null) {
            arr = new byte[length];
        }
        array = arr;
        this.baseOffset = start;
        this.address = ReflectArray.getBodyPtr(arr);
        this.capacity = length;
        this.limit = capacity;
        this.position = 0;
    }

    protected ByteBufferImpl(int capacity) {
        this(null, 0, capacity, false);
    }

    public void finalize() {
    }

    public ByteBuffer asReadOnlyBuffer() {
        ByteBuffer b = new ByteBufferImpl(array, 0, capacity, true);
        b.position(position());
        b.limit(limit());
        return b;
    }


    public ByteBuffer slice() {
        return new ByteBufferImpl(array, position, remaining(), false);
    }

    protected void doPut(int position, byte val) {
        RefNative.heap_put_byte(address, baseOffset + position, val);
    }

    public ByteBuffer put(ByteBuffer src) {
        checkPut(position, src.remaining(), false);
        ByteBufferImpl b = (ByteBufferImpl) src;
        RefNative.heap_copy(b.address, b.baseOffset + b.position, address, baseOffset + position, b.remaining());

        position += b.remaining();
        b.position += b.remaining();

        return this;
    }

    public ByteBuffer put(byte[] src, int start, int length) {
        if (start < 0 || start + length > src.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        checkPut(position, length, false);
        RefNative.heap_copy(ReflectArray.getBodyPtr(src), start, address, baseOffset + position, length);

        position += length;

        return this;
    }

    public ByteBuffer get(byte[] dst, int start, int length) {
        if (start < 0 || start + length > dst.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        checkGet(position, length, false);
        RefNative.heap_copy(address, baseOffset + position, ReflectArray.getBodyPtr(dst), start, length);
        position += length;
        return this;
    }

    protected byte doGet(int position) {
        return RefNative.heap_get_byte(address, baseOffset + position);
    }

    public String toString() {
        return "(ByteBufferImpl with address: " + address
                + " position: " + position
                + " limit: " + limit
                + " capacity: " + capacity + ")";
    }

    @Override
    public ByteBuffer duplicate() {
        ByteBuffer b = new ByteBufferImpl(array, 0, capacity, isReadOnly());
        b.limit(this.limit());
        b.position(this.position());
        return b;
    }


    public ByteBuffer put(int pos, byte val) {
        checkPut(pos, 1, true);
        doPut(pos, val);
        return this;
    }

    public ByteBuffer put(byte val) {
        checkPut(position, 1, false);
        doPut(position, val);
        ++position;
        return this;
    }

    public ByteBuffer put(byte[] arr) {
        return put(arr, 0, arr.length);
    }


    public ByteBuffer putDouble(int position, double val) {
        return putLong(position, Double.doubleToLongBits(val));
    }

    public ByteBuffer putFloat(int position, float val) {
        return putInt(position, Float.floatToIntBits(val));
    }

    public ByteBuffer putLong(int position, long val) {
        checkPut(position, 8, true);

        RefNative.heap_put_long(address, baseOffset + position, val);

        return this;
    }

    public ByteBuffer putInt(int position, int val) {
        checkPut(position, 4, true);

        RefNative.heap_put_int(address, baseOffset + position, val);

        return this;
    }

    public ByteBuffer putShort(int position, short val) {
        checkPut(position, 2, true);

        RefNative.heap_put_short(address, baseOffset + position, val);

        return this;
    }

    public ByteBuffer putChar(int position, char val) {
        checkPut(position, 2, true);

        RefNative.heap_put_short(address, baseOffset + position, (short) val);

        return this;
    }

    public ByteBuffer putDouble(double val) {
        return putLong(Double.doubleToLongBits(val));
    }

    public ByteBuffer putFloat(float val) {
        return putInt(Float.floatToIntBits(val));
    }

    public ByteBuffer putLong(long val) {
        checkPut(position, 8, false);

        RefNative.heap_put_long(address, baseOffset + position, val);
        position += 8;
        return this;
    }

    public ByteBuffer putInt(int val) {
        checkPut(position, 4, false);

        RefNative.heap_put_int(address, baseOffset + position, val);
        position += 4;
        return this;
    }

    public ByteBuffer putShort(short val) {
        checkPut(position, 2, false);

        RefNative.heap_put_short(address, baseOffset + position, val);
        position += 2;
        return this;
    }

    public ByteBuffer putChar(char val) {
        checkPut(position, 2, false);

        RefNative.heap_put_short(address, baseOffset + position, (short) val);
        position += 2;
        return this;
    }

    public byte get() {
        checkGet(position, 1, false);
        return doGet(position++);
    }

    public byte get(int position) {
        checkGet(position, 1, true);
        return doGet(position);
    }

    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public double getDouble(int position) {
        checkGet(position, 8, true);

        return RefNative.heap_get_double(address, baseOffset + position);

    }

    public float getFloat(int position) {
        checkGet(position, 4, true);

        return RefNative.heap_get_float(address, baseOffset + position);
    }

    public long getLong(int position) {
        checkGet(position, 8, true);

        return RefNative.heap_get_long(address, baseOffset + position);
    }

    public int getInt(int position) {
        checkGet(position, 4, true);

        return RefNative.heap_get_int(address, baseOffset + position);
    }

    public short getShort(int position) {
        checkGet(position, 2, true);

        return RefNative.heap_get_short(address, baseOffset + position);
    }

    public char getChar(int position) {
        checkGet(position, 2, true);

        return (char) RefNative.heap_get_short(address, baseOffset + position);
    }

    public double getDouble() {
        checkGet(position, 8, false);

        double r = RefNative.heap_get_double(address, baseOffset + position);
        position += 8;
        return r;
    }

    public float getFloat() {
        checkGet(position, 4, false);

        float r = RefNative.heap_get_float(address, baseOffset + position);
        position += 4;
        return r;
    }

    public long getLong() {
        checkGet(position, 8, false);

        long r = RefNative.heap_get_long(address, baseOffset + position);
        position += 8;
        return r;
    }

    public int getInt() {
        checkGet(position, 4, false);

        int r = RefNative.heap_get_int(address, baseOffset + position);
        position += 4;
        return r;
    }

    public short getShort() {
        checkGet(position, 2, false);

        short r = RefNative.heap_get_short(address, baseOffset + position);
        position += 2;
        return r;
    }

    public char getChar() {
        checkGet(position, 2, false);

        char r = (char) RefNative.heap_get_short(address, baseOffset + position);
        position += 2;
        return r;
    }


    public ByteBuffer compact() {
        int remaining = remaining();
        if (remaining > 0) {
            System.arraycopy(array, position, array, 0, remaining);
        }

        position = remaining;
        limit(capacity());

        return this;
    }

    public int compareTo(ByteBuffer o) {
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
        return o instanceof ByteBuffer && compareTo((ByteBuffer) o) == 0;
    }


    protected void checkPut(int position, int amount, boolean absolute) {
        if (isReadOnly()) {
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

    public boolean hasArray() {
        return true;
    }

    public byte[] array() {
        return array;
    }


    public int arrayOffset() {
        return baseOffset;
    }


    /**
     *
     */


    @Override
    public ShortBuffer asShortBuffer() {
        return new ShortBuffer(ByteBufferImpl.this.readonly, array.length / Short.BYTES) {

            @Override
            public ShortBuffer asReadOnlyBuffer() {
                return ByteBufferImpl.this.asReadOnlyBuffer().asShortBuffer();
            }

            @Override
            public ShortBuffer slice() {
                int pos = ByteBufferImpl.this.position;
                ByteBufferImpl.this.position(this.position * Short.BYTES);
                ShortBuffer ib = ByteBufferImpl.this.slice().asShortBuffer();
                ByteBufferImpl.this.position(pos);
                return ib;
            }

            @Override
            protected void doPut(int pos, short value) {
                RefNative.heap_put_short(ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, pos * Short.BYTES, value);
            }


            @Override
            public ShortBuffer put(short[] src, int pos, int length) {
                if (src == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= src.length || pos + length > src.length) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ReflectArray.getBodyPtr(src), pos * Short.BYTES, ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, position * Short.BYTES, length * Short.BYTES);
                position += length;
                return this;
            }

            @Override
            protected short doGet(int pos) {
                return RefNative.heap_get_short(ByteBufferImpl.this.address, pos * Short.BYTES);
            }

            @Override
            public ShortBuffer get(short[] dst, int pos, int length) {
                if (dst == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= dst.length || pos + length > dst.length) {
                    throw new ArrayIndexOutOfBoundsException("dst");
                } else if (position + length > capacity) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ByteBufferImpl.this.address, position * Short.BYTES, ReflectArray.getBodyPtr(dst), pos * Short.BYTES, length * Short.BYTES);
                position += length;
                return this;
            }
        };
    }

    @Override
    public CharBuffer asCharBuffer() {
        return new CharBuffer(ByteBufferImpl.this.readonly, array.length / Character.BYTES) {

            @Override
            public CharBuffer asReadOnlyBuffer() {
                return ByteBufferImpl.this.asReadOnlyBuffer().asCharBuffer();
            }

            @Override
            public CharBuffer slice() {
                int pos = ByteBufferImpl.this.position;
                ByteBufferImpl.this.position(this.position * Character.BYTES);
                CharBuffer ib = ByteBufferImpl.this.slice().asCharBuffer();
                ByteBufferImpl.this.position(pos);
                return ib;
            }

            @Override
            protected void doPut(int pos, char value) {
                RefNative.heap_put_short(ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, pos * Character.BYTES, (short) value);
            }


            @Override
            public CharBuffer put(char[] src, int pos, int length) {
                if (src == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= src.length || pos + length > src.length) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ReflectArray.getBodyPtr(src), pos * Character.BYTES, ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, position * Character.BYTES, length * Character.BYTES);
                position += length;
                return this;
            }

            @Override
            protected char doGet(int pos) {
                return (char) RefNative.heap_get_short(ByteBufferImpl.this.address, pos * Character.BYTES);
            }

            @Override
            public CharBuffer get(char[] dst, int pos, int length) {
                if (dst == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= dst.length || pos + length > dst.length) {
                    throw new ArrayIndexOutOfBoundsException("dst");
                } else if (position + length > capacity) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ByteBufferImpl.this.address, position * Character.BYTES, ReflectArray.getBodyPtr(dst), pos * Character.BYTES, length * Character.BYTES);
                position += length;
                return this;
            }
        };
    }

    @Override
    public DoubleBuffer asDoubleBuffer() {
        return new DoubleBuffer(ByteBufferImpl.this.readonly, array.length / Double.BYTES) {

            @Override
            public DoubleBuffer asReadOnlyBuffer() {
                return ByteBufferImpl.this.asReadOnlyBuffer().asDoubleBuffer();
            }

            @Override
            public DoubleBuffer slice() {
                int pos = ByteBufferImpl.this.position;
                ByteBufferImpl.this.position(this.position * Double.BYTES);
                DoubleBuffer ib = ByteBufferImpl.this.slice().asDoubleBuffer();
                ByteBufferImpl.this.position(pos);
                return ib;
            }

            @Override
            protected void doPut(int pos, double value) {
                RefNative.heap_put_double(ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, pos * Double.BYTES, value);
            }

            @Override
            public DoubleBuffer put(double[] src, int pos, int length) {
                if (src == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= src.length || pos + length > src.length) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ReflectArray.getBodyPtr(src), pos * Double.BYTES, ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, position * Double.BYTES, length * Double.BYTES);
                position += length;
                return this;
            }

            @Override
            protected double doGet(int pos) {
                return RefNative.heap_get_double(ByteBufferImpl.this.address, pos * Double.BYTES);
            }

            @Override
            public DoubleBuffer get(double[] dst, int pos, int length) {
                if (dst == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= dst.length || pos + length > dst.length) {
                    throw new ArrayIndexOutOfBoundsException("dst");
                } else if (position + length > capacity) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ByteBufferImpl.this.address, position * Double.BYTES, ReflectArray.getBodyPtr(dst), pos * Double.BYTES, length * Double.BYTES);
                position += length;
                return this;
            }
        };
    }

    @Override
    public FloatBuffer asFloatBuffer() {
        return new FloatBuffer(ByteBufferImpl.this.readonly, array.length / Float.BYTES) {

            @Override
            public FloatBuffer asReadOnlyBuffer() {
                return ByteBufferImpl.this.asReadOnlyBuffer().asFloatBuffer();
            }

            @Override
            public FloatBuffer slice() {
                int pos = ByteBufferImpl.this.position;
                ByteBufferImpl.this.position(this.position * Float.BYTES);
                FloatBuffer ib = ByteBufferImpl.this.slice().asFloatBuffer();
                ByteBufferImpl.this.position(pos);
                return ib;
            }

            @Override
            protected void doPut(int pos, float value) {
                RefNative.heap_put_float(ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, pos * Float.BYTES, value);
            }


            @Override
            public FloatBuffer put(float[] src, int pos, int length) {
                if (src == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= src.length || pos + length > src.length) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ReflectArray.getBodyPtr(src), pos * Float.BYTES, ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, position * Float.BYTES, length * Float.BYTES);
                position += length;
                return this;
            }

            @Override
            protected float doGet(int pos) {
                return RefNative.heap_get_float(ByteBufferImpl.this.address, pos * Float.BYTES);
            }

            @Override
            public FloatBuffer get(float[] dst, int pos, int length) {
                if (dst == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= dst.length || pos + length > dst.length) {
                    throw new ArrayIndexOutOfBoundsException("dst");
                } else if (position + length > capacity) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ByteBufferImpl.this.address, position * Float.BYTES, ReflectArray.getBodyPtr(dst), pos * Float.BYTES, length * Float.BYTES);
                position += length;
                return this;
            }
        };
    }

    @Override
    public IntBuffer asIntBuffer() {
        return new IntBuffer(ByteBufferImpl.this.readonly, array.length / Integer.BYTES) {

            @Override
            public IntBuffer asReadOnlyBuffer() {
                return ByteBufferImpl.this.asReadOnlyBuffer().asIntBuffer();
            }

            @Override
            public IntBuffer slice() {
                int pos = ByteBufferImpl.this.position;
                ByteBufferImpl.this.position(this.position * Integer.BYTES);
                IntBuffer ib = ByteBufferImpl.this.slice().asIntBuffer();
                ByteBufferImpl.this.position(pos);
                return ib;
            }

            @Override
            protected void doPut(int pos, int value) {
                RefNative.heap_put_int(ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, pos * Integer.BYTES, value);
            }

            @Override
            public IntBuffer put(int[] src, int pos, int length) {
                if (src == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= src.length || pos + length > src.length) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ReflectArray.getBodyPtr(src), pos * Integer.BYTES, ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, position * Integer.BYTES, length * Integer.BYTES);
                position += length;
                return this;
            }

            @Override
            protected int doGet(int pos) {
                return RefNative.heap_get_int(ByteBufferImpl.this.address, pos * Integer.BYTES);
            }

            @Override
            public IntBuffer get(int[] dst, int pos, int length) {
                if (dst == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= dst.length || pos + length > dst.length) {
                    throw new ArrayIndexOutOfBoundsException("dst");
                } else if (position + length > capacity) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ByteBufferImpl.this.address, position * Integer.BYTES, ReflectArray.getBodyPtr(dst), pos * Integer.BYTES, length * Integer.BYTES);
                position += length;
                return this;
            }
        };
    }

    @Override
    public LongBuffer asLongBuffer() {
        return new LongBuffer(ByteBufferImpl.this.readonly, array.length / Long.BYTES) {

            @Override
            public LongBuffer asReadOnlyBuffer() {
                return ByteBufferImpl.this.asReadOnlyBuffer().asLongBuffer();
            }

            @Override
            public LongBuffer slice() {
                int pos = ByteBufferImpl.this.position;
                ByteBufferImpl.this.position(this.position * Long.BYTES);
                LongBuffer ib = ByteBufferImpl.this.slice().asLongBuffer();
                ByteBufferImpl.this.position(pos);
                return ib;
            }

            @Override
            protected void doPut(int pos, long value) {
                RefNative.heap_put_long(ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, pos * Long.BYTES, value);
            }

            @Override
            public LongBuffer put(long[] src, int pos, int length) {
                if (src == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= src.length || pos + length > src.length) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ReflectArray.getBodyPtr(src), pos * Long.BYTES, ByteBufferImpl.this.address + ByteBufferImpl.this.baseOffset, position * Long.BYTES, length * Long.BYTES);
                position += length;
                return this;
            }

            @Override
            protected long doGet(int pos) {
                return RefNative.heap_get_long(ByteBufferImpl.this.address, pos * Long.BYTES);
            }

            @Override
            public LongBuffer get(long[] dst, int pos, int length) {
                if (dst == null) {
                    throw new NullPointerException();
                } else if (pos < 0 || pos >= dst.length || pos + length > dst.length) {
                    throw new ArrayIndexOutOfBoundsException("dst");
                } else if (position + length > capacity) {
                    throw new IndexOutOfBoundsException();
                }
                RefNative.heap_copy(ByteBufferImpl.this.address, position * Long.BYTES, ReflectArray.getBodyPtr(dst), pos * Long.BYTES, length * Long.BYTES);
                position += length;
                return this;
            }
        };
    }


}
