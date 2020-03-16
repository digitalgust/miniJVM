package org.mini.layout.gscript;

public class Obj extends DataType {

    private Object value = null;

    public Obj(Object s) {
        type = DTYPE_OBJ;
        value = s;
    }

    public void setVal(Object s) {
        value = s;
    }

    public Object getVal() {
        return value;
    }

    public boolean isNull() {
        return value == null;
    }

    public String getString() {
        return value.toString();
    }

    public String toString() {
        return getString();
    }
}
