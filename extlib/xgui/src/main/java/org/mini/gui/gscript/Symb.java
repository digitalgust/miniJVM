package org.mini.gui.gscript;

public class Symb extends DataType {

    static public final byte NONE = 0, ADD = 1, SUB = 2, MUL = 3, DIV = 4, GRE = 5, LES = 6, EQU = 7, GE = 8, LE = 9, NE = 10, NOT = 11, AND = 12, OR = 13, LP = 14, RP = 15;
    private byte value = NONE;
    private String s_value = null;

    public Symb(String opSymbol) {
        type = DTYPE_SYMB;
        s_value = opSymbol;
        if (opSymbol.equals("+")) {
            value = ADD;
        } else if (opSymbol.equals("-")) {
            value = SUB;
        } else if (opSymbol.equals("*")) {
            value = MUL;
        } else if (opSymbol.equals("/")) {
            value = DIV;
        } else if (opSymbol.equals(">")) {
            value = GRE;
        } else if (opSymbol.equals("<")) {
            value = LES;
        } else if (opSymbol.equals("=")) {
            value = EQU;
        } else if (opSymbol.equals(">=")) {
            value = GE;
        } else if (opSymbol.equals("<=")) {
            value = LE;
        } else if (opSymbol.equals("<>")) {
            value = NE;
        } else if (opSymbol.equals("!")) {
            value = NOT;
        } else if (opSymbol.equals("(")) {
            value = LP;
        } else if (opSymbol.equals(")")) {
            value = RP;
        } else if (opSymbol.equals("&")) {
            value = AND;
        } else if (opSymbol.equals("|")) {
            value = OR;
        } else {
            value = NONE;

        }
    }

    public byte getVal() {
        return value;
    }

    public String getString() {
        return s_value;
    }

    public boolean isLogicOp() { //是逻辑计算符
        byte ot = value;
        if (ot == GRE
                || ot == LES
                || ot == EQU
                || ot == GE
                || ot == LE
                || ot == NE
                || ot == NOT
                || ot == AND
                || ot == OR) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isCompOp() { //是比较逻辑计算符
        byte ot = value;
        if (ot == GRE
                || ot == LES
                || ot == EQU
                || ot == GE
                || ot == LE
                || ot == NE) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isArithOp() { //是算术运算符
        byte ot = value;
        if (ot == ADD
                || ot == SUB
                || ot == MUL
                || ot == DIV) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return s_value;
    }
}
