package org.mini.gui.gscript;

public class Int extends DataType {

    private long value = 0;

    public Int(String s) {
        type = DTYPE_INT;
        value = Long.parseLong(s);
    }

    public Int(long i) {
        this(i, true);
    }

    public Int(long i, boolean mutable) {
        type = DTYPE_INT;
        value = i;
        this.setMutable(mutable);
    }

    public int getValAsInt() {
        return (int) value;
    }

    public long getVal() {
        return (int) value;
    }

    public void setVal(long i) {
        if (isMutable()) {
            value = i;
        } else {
            throw new RuntimeException("var is immutable");
        }
    }

    public String getString() {
        return Long.toString(value);
    }

    public String toString() {
        return getString();
    }
}
