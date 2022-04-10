package org.mini.gui.gscript;

import java.util.Vector;

/* 
 * File:   Statement.h
 * Author: gust
 *
 * Created on 2011年4月23日, 下午9:09
 */
abstract class ExprCell {

    byte type;
    static public final byte EXPR_CELL_CALL = 0;
    static public final byte EXPR_CELL_DATATYPE = 1;
    static public final byte EXPR_CELL_VAR = 2;
    static public final byte EXPR_CELL_ARR = 3;
};

/**
 * 过程
 */
class ExprCellCall extends ExprCell {

    String subName;
    Expression[] para;

    public void parseCallPara(String inst, Vector paraList, Interpreter inp) {
        //查找脚本中的过程
        subName = inp.getFirstWord(inst);
        String paraVal = inst.substring(inst.indexOf('(') + 1);
        paraVal = paraVal.substring(0, paraVal.lastIndexOf(')'));
        if (paraVal.length() != 0) { //分解参数

            //用现成的拆分功能，把多个参数拆出来
            Vector sValue = inp.parseInstruct(paraVal);
            while (!sValue.isEmpty()) {

                //解析出每个表达式串，以','为分隔符
                StringBuffer exprStr = new StringBuffer();
                while (!sValue.isEmpty()) {
                    String tmps = (String) sValue.elementAt(0);
                    sValue.removeElementAt(0);
                    if (",".equals(tmps)) {
                        break; //分隔符时中断
                    }
                    exprStr.append(tmps);
                }
                paraList.addElement(exprStr.toString());
            } //end while

        }

    }

    ExprCellCall(String inst, Interpreter inp) throws Exception {
        type = (ExprCell.EXPR_CELL_CALL);
        Vector tmppara = new Vector();

        parseCallPara(inst, tmppara, inp);

        //拆分表达式
        int paralen = tmppara.size();
        para = new Expression[paralen];
        if (paralen > 0) {
            for (int i = 0; i < paralen; i++) {
                para[i] = new Expression((String) tmppara.elementAt(i), inp);
            }
        }
    }

    public String toString() {
        return subName;
    }
};

/*
 数据类型,所有常量,DataType的子类
 */
class ExprCellDataType extends ExprCell {

    DataType pit;

    ExprCellDataType(DataType p, Interpreter inp) {
        type = ExprCell.EXPR_CELL_DATATYPE;
        pit = p;
    }

    public String toString() {
        return pit.getString();
    }
};

/**
 * 普通变量
 */
class ExprCellVar extends ExprCell {

    String varName;

    ExprCellVar(String pvarname, Interpreter inp) {
        type = (ExprCell.EXPR_CELL_VAR);
        varName = pvarname;
    }

    public String toString() {
        return varName;
    }
};

/**
 * 数组变量
 */
class ExprCellArr extends ExprCell {

    String arrName;
    Expression[] para;

    void parseArr(String arrs, Vector paraList, Interpreter inp) throws Exception {
        //取得数组的名字
        arrName = (inp.getFirstWord(arrs)).toLowerCase();

        String arrStr = arrs.substring(arrs.indexOf('['));
        int leftQ = 0;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arrStr.length(); i++) {
            char ch = arrStr.charAt(i);
            if (ch == '[') {
                if (leftQ > 0) {
                    sb.append(ch);
                }
                leftQ++;
            } else if (ch == ']') {
                leftQ--;
                if (leftQ == 0) {
                    paraList.addElement(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(ch);
                }
            } else {
                sb.append(ch);
            }
        }

        if (leftQ != 0 || paraList.isEmpty()) {
            //出错，需处理

            throw new Exception(Interpreter.STRS_ERR[Interpreter.ERR_ILLEGAL]);
            //msgOut(ERR_STRS[ERR_ILLEGAL]);
            //return t_ilist();
        }

    }

    ExprCellArr(String src, Interpreter inp) throws Exception {
        type = (ExprCell.EXPR_CELL_ARR);
        Vector tmppara = new Vector();
        parseArr(src, tmppara, inp);
        //拆分表达式
        int paralen = tmppara.size();
        para = new Expression[paralen];
        for (int i = 0; i < paralen; i++) {
            para[i] = new Expression((String) tmppara.elementAt(i), inp);
        }
    }

    public String toString() {
        return arrName;
    }
};

/**
 * ------------------------------------------------------ Expression
 * ------------------------------------------------------
 */
class Expression {

    ExprCell[] cells;

    Expression(String statement, Interpreter inp) throws Exception {

        Vector tgt = new Vector();
        Vector src = inp.parseInstruct(statement);

        for (int i = 0; i < src.size(); i++) {

            String s = (String) src.elementAt(i);
            if (s.charAt(0) == '"') {// 是字符串
                s = s.substring(1, s.length() - 1);
                String ws = s;
                Str pst = new Str(ws);
                tgt.addElement(new ExprCellDataType(pst, inp));
            } else if (inp.isSymbol(s.charAt(0))) {
                Symb psyt = new Symb(s);
                if (psyt.getVal() == psyt.NONE) {
                    //错误的符号，需处理
                    throw new Exception(Interpreter.STRS_ERR[Interpreter.ERR_ILLEGAL]);
                } else {
                    tgt.addElement(new ExprCellDataType(psyt, inp));
                }
            } else if (inp.isNumeric(s.charAt(0))) {//是数字
                Int pit = new Int(s);
                tgt.addElement(new ExprCellDataType(pit, inp));
            } else if (inp.isSubCall(s)) {//是过调用
                tgt.addElement(new ExprCellCall(s, inp));
            } else if (inp.isArr(s)) {//是数组
                tgt.addElement(new ExprCellArr(s, inp));
            } else if (Bool.isBool(s)) {//是BOOL值
                Bool pbt = new Bool(s);
                tgt.addElement(new ExprCellDataType(pbt, inp));
            } else {//是变量
                tgt.addElement(new ExprCellVar(s, inp));
            }
        }

        int cellslen = tgt.size();
        cells = new ExprCell[cellslen];
        if (cellslen > 0) {
            for (int i = 0; i < cellslen; i++) {
                cells[i] = (ExprCell) tgt.elementAt(i);
            }
        }

    }
};

/**
 * ------------------------------------------------------ statement
 * ------------------------------------------------------
 */
public abstract class Statement {

    byte type;
    String src;

    static Statement parseInstruct(String stat, Interpreter inp) throws Exception {
        if ((stat).trim().length() <= 0) {
            return null;
        }
        String header = inp.getFirstWord(stat);
        int hCode = inp.getKeywordCode(header);
        if (hCode != Interpreter.NOT_KEYWORD) {
            switch (hCode) {
                case Interpreter.KEYWORD_WHILE:
                    return new StatementWhile(stat, inp);
                case Interpreter.KEYWORD_LOOP:
                    return new StatementLoop(stat, inp);
                case Interpreter.KEYWORD_IF:
                    return new StatementIf(stat, inp);
                case Interpreter.KEYWORD_ELSE:
                    return new StatementElse(stat, inp);
                case Interpreter.KEYWORD_ENDIF:
                    return new StatementEndif(stat, inp);
                case Interpreter.KEYWORD_SUB:
                    return new StatementSub(stat, inp);
                case Interpreter.KEYWORD_RET:
                    return new StatementRet(stat, inp);
                default:
                    throw new Exception("error in CStatement.cpp,unknow statement.");
            }
        } else if (inp.isArr(stat)) {
            return new StatementSetArr(stat, inp);
        } else if (inp.isSubCall(stat)) {
            return new StatementCall(stat, inp);
        } else if (inp.isVar(stat)) {
            return new StatementSetVar(stat, inp);
        } else {
            //inp.Out(Interpreter.STRS_ERR[Interpreter.ERR_ILLEGAL]);
            throw new Exception("error in CStatement.cpp,unknow statement.");
        }
    }

    public String toString() {
        return src;
    }
};

class StatementIf extends Statement {

    Expression expr = null;
    short ip_else = -1;
    short ip_endif = -1;

    StatementIf(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_IF);
        String exprstr = src.substring(Interpreter.STRS_RESERVED[Interpreter.KEYWORD_IF].length());
        expr = new Expression(exprstr, inp);
    }
};

class StatementElse extends Statement {

    short ip_endif = -1;

    StatementElse(String src, Interpreter inp) {
        type = (Interpreter.KEYWORD_ELSE);
    }
};

class StatementEndif extends Statement {

    StatementEndif(String src, Interpreter inp) {
        type = (Interpreter.KEYWORD_ENDIF);
    }
};

class StatementWhile extends Statement {

    Expression expr = null;
    short ip_loop = -1;

    StatementWhile(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_WHILE);
        String exprstr = src.substring(Interpreter.STRS_RESERVED[Interpreter.KEYWORD_WHILE].length());
        expr = new Expression(exprstr, inp);
    }
};

class StatementLoop extends Statement {

    short ip_while = -1;

    StatementLoop(String src, Interpreter inp) {
        type = (Interpreter.KEYWORD_LOOP);
    }
};

class StatementSub extends Statement {

    ExprCellCall cell;

    StatementSub(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_SUB);
        String exprstr = (src.substring(Interpreter.STRS_RESERVED[Interpreter.KEYWORD_SUB].length())).trim();
        cell = new ExprCellCall(exprstr, inp);
    }
};

class StatementRet extends Statement {

    Expression expr;

    StatementRet(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_RET);
        if (src.length() > Interpreter.STRS_RESERVED[Interpreter.KEYWORD_RET].length()) {
            String s = (src.substring(Interpreter.STRS_RESERVED[Interpreter.KEYWORD_RET].length())).trim();
            expr = new Expression(s, inp);
        }
    }
};

class StatementCall extends Statement {

    String subName;
    ExprCellCall cell;

    StatementCall(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_CALL);
        cell = new ExprCellCall(src, inp);
    }
};

class StatementSetVar extends Statement {

    String varName;
    Expression expr;

    StatementSetVar(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_SET_VAR);
        varName = inp.getFirstWord(src).toLowerCase();
        String exprStr = src.substring(src.indexOf('=') + 1);
        expr = new Expression(exprStr, inp);
    }
};

class StatementSetArr extends Statement {

    ExprCellArr dimCell; //数组维数定义,等号左边
    Expression expr; //赋值表达式,等号右边

    StatementSetArr(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_SET_ARR);
        Vector stackTmp = inp.parseInstruct(src);
        if (!stackTmp.isEmpty()) {
            String leftStr = (String) stackTmp.elementAt(0);
            //leftStr.print();
            stackTmp.removeElementAt(0);

            dimCell = new ExprCellArr(leftStr, inp);
        }
        if (!stackTmp.isEmpty()) {
            String exprStr = src.substring(src.indexOf('=') + 1);
            //exprStr.print();
            expr = new Expression(exprStr, inp);
        }
    }
};
