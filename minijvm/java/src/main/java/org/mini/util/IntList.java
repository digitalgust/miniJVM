package org.mini.util;

public class IntList {
    private static final int DEFAULT_SIZE = 8;
    private int[] arr = new int[DEFAULT_SIZE];
    private int pos;

    public IntList() {
        this(DEFAULT_SIZE);
    }

    public IntList(int size) {
        if (size > DEFAULT_SIZE) {
            arr = new int[size];
        }
    }

    public IntList(IntList list) {
        addAll(list);
    }

    public void add(int v) {
        ensureCap(1);
        arr[pos] = v;
        pos++;
    }

    public void addAt(int index, int v) {
        ensureCap(1);
        System.arraycopy(arr, index, arr, index + 1, arr.length - index);
        arr[index] = v;
        pos++;
    }

    public void addAll(IntList list) {
        ensureCap(list.arr.length);
        System.arraycopy(list.arr, 0, arr, pos, list.pos);
        pos += list.pos;
    }

    public void set(int index, int v) {
        if (index < 0 || index >= pos) throw new ArrayIndexOutOfBoundsException();
        arr[index] = v;
    }

    public void remove(int v) {
        int real = 0;
        for (int i = 0; i < pos; i++) {
            arr[real] = arr[i];
            if (arr[i] != v) {
                real++;
            }
        }
        pos = real;
    }

    public void removeAt(int index) {
        if (index < 0 || index >= pos) throw new ArrayIndexOutOfBoundsException();
        System.arraycopy(arr, index + 1, arr, index, arr.length - index - 1);
        pos--;
    }

    public void removeAll(IntList list) {
        for (int i = 0; i < list.pos; i++) {
            remove(list.get(i));
        }
    }

    public boolean contains(int v) {
        for (int i = 0; i < pos; i++) {
            if (arr[i] == v) return true;
        }
        return false;
    }


    public int indexOf(int v) {
        for (int i = 0; i < pos; i++) {
            if (arr[i] == v) return i;
        }
        return -1;
    }

    public int size() {
        return pos;
    }

    public void setSize(int size) {
        if (pos >= size) {
            pos = size;
        } else {
            for (int i = pos; i < size; i++) {
                add(0);
            }
        }
    }

    public int get(int index) {
        if (index < 0 || index >= pos) throw new ArrayIndexOutOfBoundsException();
        return arr[index];
    }

    public void clear() {
        pos = 0;
    }


    public void inverse() {
        int imax = pos / 2;
        for (int i = 0; i < imax; i++) {
            int thatEnd = pos - 1 - i;
            int tmp = arr[i];
            arr[i] = arr[thatEnd];
            arr[thatEnd] = tmp;
        }
    }


    private void ensureCap(int reqSize) {
        if (pos + reqSize >= arr.length) {
            int[] narr = new int[(int) ((pos + reqSize) * 1.5f)];
            System.arraycopy(arr, 0, narr, 0, pos);
            arr = narr;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < pos; i++) {
            sb.append(arr[i]);
            if (i < pos - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    public int[] getArray() {
        return arr;
    }
}
