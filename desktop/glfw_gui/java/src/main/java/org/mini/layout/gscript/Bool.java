package org.mini.layout.gscript;

public class Bool extends DataType {

    private boolean value = false;

    public Bool(String s) {
        type = DTYPE_BOOL;
        if (s.toLowerCase().equals("true")) {
            value = true;
        } else {
            value = false;
        }
    }

    public Bool(boolean b) {
        type = DTYPE_BOOL;
        value = b;
    }

    public void setVal(boolean b) {
        value = b;
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
