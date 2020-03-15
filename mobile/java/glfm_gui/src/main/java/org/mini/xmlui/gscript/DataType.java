package org.mini.xmlui.gscript;

abstract public class DataType {

    public static final byte DTYPE_INT = 0;
    public static final byte DTYPE_STR = 1;
    public static final byte DTYPE_BOOL = 2;
    public static final byte DTYPE_ARRAY = 3;
    public static final byte DTYPE_OBJ = 4;
    public static final byte DTYPE_SYMB = 5;
    //
    byte type;

    public abstract String getString();
//  abstract void setVal(DataType d);
}
