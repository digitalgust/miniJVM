package org.mini.layout.gscript;

public class Int extends DataType {

    private long value = 0;

    public Int(String s) {
        type = DTYPE_INT;
        value = Long.parseLong(s);
    }

    public Int(long i) {
        type = DTYPE_INT;
        value = i;
    }

    public long getValLong() {
        return value;
    }
    
    public int getVal(){
        return (int)value;
    }

    public void setVal(int i) {
        value = i;
    }

    public String getString() {
        return Long.toString(value);
    }

    public String toString() {
        return getString();
    }
}
