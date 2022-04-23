package org.mini.gui.gscript;

abstract public class DataType {

    public static final byte DTYPE_INT = 0;
    public static final byte DTYPE_STR = 1;
    public static final byte DTYPE_BOOL = 2;
    public static final byte DTYPE_ARRAY = 3;
    public static final byte DTYPE_OBJ = 4;
    public static final byte DTYPE_SYMB = 5;
    //
    public byte type;

    //此变量是否可变, 在源码中(Statemenu.ExprCell)的 或者赋值给变量的 不可变,
    // 运算过程中的都可变
    private boolean mutable = true;

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(boolean m) {
        mutable = m;
    }


    public abstract String getString();
//  abstract void setVal(DataType d);
}
