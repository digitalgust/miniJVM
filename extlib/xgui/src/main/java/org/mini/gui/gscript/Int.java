package org.mini.gui.gscript;

public class Int extends DataType {

    private long value = 0;

    Int(String s) {
        type = DTYPE_INT;
        if (s.indexOf("+") >= 0) {
            s = s.substring(s.indexOf("+") + 1);
        }
        value = Long.parseLong(s);
    }

    Int(long i) {
        this(i, true);
    }

    Int(long i, boolean mutable) {
        type = DTYPE_INT;
        value = i;
        this.setRecyclable(mutable);
    }

    public int getValAsInt() {
        return (int) value;
    }

    public long getVal() {
        return (int) value;
    }

    public void setVal(long i) {
        if (isRecyclable()) {
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
