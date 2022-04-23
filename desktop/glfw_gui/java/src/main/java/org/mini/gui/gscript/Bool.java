package org.mini.gui.gscript;

public class Bool extends DataType {

    private boolean value = false;

    public Bool(String s) {
        type = DTYPE_BOOL;
        if (s.toLowerCase().equals("true")) {
            value = true;
        } else {
            value = false;
        }
        this.setRecyclable(true);
    }

    public Bool(boolean b) {
        this(b, true);
    }

    public Bool(boolean b, boolean mutable) {
        type = DTYPE_BOOL;
        value = b;
        this.setRecyclable(mutable);
    }

    public void setVal(boolean b) {
        if (isRecyclable()) {
            value = b;
        } else {
            throw new RuntimeException("var is immutable");
        }
    }

    public boolean getVal() {
        return value;
    }

    public String getString() {
        return (new Boolean(value)).toString();
    }

    static public boolean isBool(String s) {
        if (s.toLowerCase().equals("true") || s.toLowerCase().equals("false")) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return getString();
    }
}
