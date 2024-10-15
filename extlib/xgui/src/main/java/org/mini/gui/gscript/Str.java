package org.mini.gui.gscript;

public class Str extends DataType {

    private String value = null;

    Str(String s) {
        this(s, true);

    }

    Str(String s, boolean mutable) {
        type = DTYPE_STR;
        setVal(s);
        setRecyclable(mutable);
    }

    public void setVal(String s) {
        if (isRecyclable()) {
            value = s == null ? "" : s.intern();
        } else {
            throw new RuntimeException("var is immutable");
        }
    }

    public String getVal() {
        return value;
    }

    public String getString() {
        return value;
    }

    public String toString() {
        return value;
    }
}
