package org.mini.xmlui.gscript;

public class Str extends DataType {

    private String value = null;

    public Str(String s) {
        type = DTYPE_STR;
        value = s;
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
