package org.mini.gui.gscript;

public class Str extends DataType {

    private String value = null;

    Str(String s) {
        this(s, true);

    }

    Str(String s, boolean mutable) {
        type = DTYPE_STR;
        value = s;
        setRecyclable(mutable);
    }

    public void setVal(String s) {
        value = s;
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
