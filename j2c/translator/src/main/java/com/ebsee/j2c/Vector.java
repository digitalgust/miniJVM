package com.ebsee.j2c;

public class Vector<E> {
    private Object[] children = new Object[4];
    private short size = 0;

    public Vector() {
    }

    public Vector(int cap) {
    }

    /**
     * 添加一个组件
     *
     * @param xcon E
     */
    public void add(E xcon) {
        if (children.length <= size) {
            expand();
        }
        children[size] = xcon;
        size++;
    }

    public void addElement(E obj) {
        add(obj);
    }

    /**
     * 清除掉此元素，并把后面的元素前移
     *
     * @param xcon XObject
     */
    public void remove(E xcon) {
        if (xcon == null) {
            return;
        }
        int move = Integer.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            if (children[i] == xcon) {
                move = i;
                size--;
                //System.out.println("removed: " + xcon + "," + size);
            }
            if (i > move) {
                children[i - 1] = children[i];
            }
        }
    }

    public void removeElementAt(int i) {
        remove((E) elementAt(i));
    }

    public Object elementAt(int i) {
        if (size() > i && i >= 0) {
            return children[i];
        } else {
            return null;
        }
    }

    public int indexOf(E xc) {
        for (int i = 0; i < size; i++) {
            if (children[i] == xc) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 取得元素个数
     *
     * @return int
     */
    public int size() {
        return size;
    }

    /**
     * 扩容容器
     */
    private void expand() {
        Object[] tmpxc = new Object[(children.length << 1)]; //增加一倍
        System.arraycopy(children, 0, tmpxc, 0, size);
        children = tmpxc;
    }

    interface A {
        default int getV() {
            return 6;
        }
    }

    interface B extends A {
    }

    class C implements B {

    }

    C c = new C();
    int s = c.getV();

    public static void main(String[] s) {
        String[] strs = new String[5];
        System.out.println(strs);
    }
}