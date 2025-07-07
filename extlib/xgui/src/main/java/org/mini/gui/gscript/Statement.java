package org.mini.gui.gscript;


import java.util.ArrayList;

/*
 * File:   Statement.h
 * Author: gust
 *
 * Created on 2011年4月23日, 下午9:09
 */
abstract class ExprCell {

    byte celltype;
    static public final byte EXPR_CELL_CALL = 0;
    static public final byte EXPR_CELL_DATATYPE = 1;
    static public final byte EXPR_CELL_VAR = 2;
    static public final byte EXPR_CELL_ARR = 3;
    static public final byte EXPR_CELL_EXPR = 4;
};

/**
 * 过程
 */
class ExprCellCall extends ExprCell {

    String subName;
    Expression[] para;

    public void parseCallPara(String inst, ArrayList paraList, Interpreter inp) {
        //查找脚本中的过程
        subName = inp.getFirstWord(inst);
        String paraVal = inst.substring(inst.indexOf('(') + 1);
        paraVal = paraVal.substring(0, paraVal.lastIndexOf(')'));
        if (paraVal.length() != 0) { //分解参数

            //用现成的拆分功能，把多个参数拆出来
            ArrayList sValue = inp.parseInstruct(paraVal);
            while (!sValue.isEmpty()) {

                //解析出每个表达式串，以','为分隔符
                StringBuffer exprStr = new StringBuffer();
                while (!sValue.isEmpty()) {
                    String tmps = (String) sValue.get(0);
                    sValue.remove(0);
                    if (",".equals(tmps)) {
                        break; //分隔符时中断
                    }
                    exprStr.append(tmps);
                }
                paraList.add(exprStr.toString());
            } //end while
            Interpreter.putCachedVector(sValue);
        }

    }

    ExprCellCall(String inst, Interpreter inp) throws Exception {
        celltype = (ExprCell.EXPR_CELL_CALL);
        ArrayList tmppara = Interpreter.getCachedVector();

        parseCallPara(inst, tmppara, inp);

        //拆分表达式
        int paralen = tmppara.size();
        para = new Expression[paralen];
        if (paralen > 0) {
            for (int i = 0; i < paralen; i++) {
                para[i] = new Expression((String) tmppara.get(i), inp);
            }
        }

        Interpreter.putCachedVector(tmppara);
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
        celltype = ExprCell.EXPR_CELL_DATATYPE;
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
        celltype = (ExprCell.EXPR_CELL_VAR);
        varName = pvarname.toLowerCase();
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

    void parseArr(String arrs, ArrayList paraList, Interpreter inp) throws Exception {
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
                    paraList.add(sb.toString());
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
        celltype = (ExprCell.EXPR_CELL_ARR);
        ArrayList tmppara = Interpreter.getCachedVector();
        parseArr(src, tmppara, inp);
        //拆分表达式
        int paralen = tmppara.size();
        para = new Expression[paralen];
        for (int i = 0; i < paralen; i++) {
            para[i] = new Expression((String) tmppara.get(i), inp);
        }

        Interpreter.putCachedVector(tmppara);
    }

    public String toString() {
        return arrName;
    }
};

/**
 * ------------------------------------------------------ Expression
 * ------------------------------------------------------
 */
class Expression extends ExprCell {
    byte type;//表达式求值类型，
    boolean containsArrExpr = false;

    ExprCell[] cells;

    // 检查字符串是否是带符号的数字（如"-1", "+123"）
    private boolean isSignedNumber(String s) {
        if (s == null || s.length() < 2) {
            return false;
        }
        char first = s.charAt(0);
        if (first != '+' && first != '-') {
            return false;
        }
        // 检查后面的字符是否都是数字
        for (int i = 1; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    Expression(String statement, Interpreter inp) throws Exception {
        celltype = EXPR_CELL_EXPR;

        ArrayList tgt = Interpreter.getCachedVector();
        ArrayList src = inp.parseInstruct(statement);

        for (int i = 0; i < src.size(); i++) {

            String s = (String) src.get(i);
            if (s.charAt(0) == '"') {// 是字符串
                s = s.substring(1, s.length() - 1);
                String ws = s;
                Str pst = new Str(ws);
                pst.setRecyclable(false);
                tgt.add(new ExprCellDataType(pst, inp));
            } else if (isSignedNumber(s)) {//是带符号的数字
                Int pit = new Int(s);
                pit.setRecyclable(false);
                tgt.add(new ExprCellDataType(pit, inp));
            } else if (inp.isSymbol(s.charAt(0))) {
                Symb psyt = new Symb(s);
                psyt.setRecyclable(false);
                if (psyt.getVal() == psyt.NONE) {
                    //错误的符号，需处理
                    throw new Exception(Interpreter.STRS_ERR[Interpreter.ERR_ILLEGAL] + ":" + statement);
                } else {
                    tgt.add(new ExprCellDataType(psyt, inp));
                }
            } else if (inp.isNumeric(s.charAt(0))) {//是数字
                Int pit = new Int(s);
                pit.setRecyclable(false);
                tgt.add(new ExprCellDataType(pit, inp));
            } else if (inp.isSubCall(s)) {//是过调用
                tgt.add(new ExprCellCall(s, inp));
            } else if (inp.isArr(s)) {//是数组
                tgt.add(new ExprCellArr(s, inp));
                containsArrExpr = true;
            } else if (Bool.isBool(s)) {//是BOOL值
                Bool pbt = new Bool(s);
                pbt.setRecyclable(false);
                tgt.add(new ExprCellDataType(pbt, inp));
            } else {//是变量
                tgt.add(new ExprCellVar(s, inp));
            }
        }

        init(tgt, inp);
        Interpreter.putCachedVector(src);
        Interpreter.putCachedVector(tgt);
    }

    Expression(ArrayList tgt, Interpreter inp) throws Exception {
        celltype = EXPR_CELL_EXPR;
        init(tgt, inp);
    }

    private void init(ArrayList tgt, Interpreter inp) throws Exception {
        //把子表达式找出来，合并成一个Expression，作为一个元素加入到list中
        for (int i = 0; i < tgt.size() - 2; i++) {
            ExprCell curr = (ExprCell) tgt.get(i);
            if (curr instanceof ExprCellDataType) {
                ExprCellDataType dt = (ExprCellDataType) curr;
                if (dt.pit instanceof Symb && ((Symb) dt.pit).getVal() == Symb.LP) {
                    // 找到对应的右括号
                    int bracketCount = 1;
                    int endIndex = i + 1;
                    for (int j = i + 1; j < tgt.size(); j++) {
                        ExprCell cell = (ExprCell) tgt.get(j);
                        if (cell instanceof ExprCellDataType) {
                            ExprCellDataType cellDt = (ExprCellDataType) cell;
                            if (cellDt.pit instanceof Symb) {
                                if (((Symb) cellDt.pit).getVal() == Symb.LP) bracketCount++;
                                if (((Symb) cellDt.pit).getVal() == Symb.RP) bracketCount--;
                            }
                        }
                        if (bracketCount == 0) {
                            endIndex = j;
                            break;
                        }
                    }

                    if (bracketCount == 0) {
                        // 提取子表达式内容,直接把子表达式转为数组，用Expression (ExprCell[] para, Interpreter inp)初始化
                        int subExprLength = endIndex - (i + 1);
                        boolean hasArrExpr = false;
                        ArrayList<ExprCell> subExpr = Interpreter.getCachedVector();
                        for (int j = 0; j < subExprLength; j++) {
                            ExprCell cell = (ExprCell) tgt.get(i + 1 + j);
                            subExpr.add(cell);
                            if (cell instanceof ExprCellArr) {
                                hasArrExpr = true;
                            }
                        }

                        Expression subExpression = new Expression(subExpr, inp);
                        subExpression.containsArrExpr = hasArrExpr;
                        Interpreter.putCachedVector(subExpr);

                        // 移除旧的元素
                        for (int j = endIndex; j >= i; j--) {
                            tgt.remove(j);
                        }

                        // 添加新的子表达式
                        tgt.add(i, subExpression);
                        i--; // 重新检查当前位置
                    }
                }
            }
        }

        //进一步，如果这个表达式有逻辑操作符Symb.isLogicOp()==true,同时这个表达式中还有算述运算 Symb.isArithOp()==true，
        // 则把这个逻辑表达式中的算术表达式，合并成一个Expression，作为一个子表达式加入到list中
        boolean hasLogicOp = false;
        boolean hasArithOp = false;

        // 首先检查是否同时存在逻辑运算符和算术运算符
        for (int i = 0; i < tgt.size(); i++) {
            ExprCell cell = (ExprCell) tgt.get(i);
            if (cell instanceof ExprCellDataType) {
                ExprCellDataType dt = (ExprCellDataType) cell;
                if (dt.pit instanceof Symb) {
                    Symb sym = (Symb) dt.pit;
                    if (sym.isLogicOp()) hasLogicOp = true;
                    if (sym.isArithOp()) hasArithOp = true;
                }
            }
        }

        // 如果同时存在两种运算符，处理算术表达式
        if (hasLogicOp && hasArithOp) {
            for (int i = 0; i < tgt.size(); i++) {
                int start = -1;
                int end = -1;

                // 找到算术表达式的开始和结束
                for (int j = i; j < tgt.size(); j++) {
                    ExprCell cell = (ExprCell) tgt.get(j);
                    if (cell instanceof ExprCellDataType) {
                        ExprCellDataType dt = (ExprCellDataType) cell;
                        if (dt.pit instanceof Symb) {
                            Symb sym = (Symb) dt.pit;
                            if (sym.isArithOp()) {
                                if (start == -1) start = j - 1;
                                end = j + 2; // 包含运算符后面的操作数
                                j++; // 跳过下一个操作数
                            } else if (sym.isLogicOp()) {
                                if (start != -1) break; // 遇到逻辑运算符就结束当前算术表达式
                            }
                        }
                    }
                }

                // 如果找到了算术表达式，将其合并
                if (start >= 0 && end > start && end <= tgt.size()) {
                    ArrayList<ExprCell> arithExpr = Interpreter.getCachedVector();
                    for (int j = start; j < end; j++) {
                        arithExpr.add((ExprCell) tgt.get(j));
                    }

                    Expression subExpression = new Expression(arithExpr, inp);
                    Interpreter.putCachedVector(arithExpr);

                    // 移除原来的元素
                    for (int j = end - 1; j >= start; j--) {
                        tgt.remove(j);
                    }

                    // 添加新的子表达式
                    tgt.add(start, subExpression);

                    // 调整索引
                    i = start;
                }
            }
        }


        //把list转为数组
        int cellslen = tgt.size();
        cells = new ExprCell[cellslen];
        if (cellslen > 0) {
            for (int i = 0; i < cellslen; i++) {
                cells[i] = (ExprCell) tgt.get(i);
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
                case Interpreter.KEYWORD_FOR:
                    return new StatementFor(stat, inp);
                case Interpreter.KEYWORD_EFOR:
                    return new StatementEfor(stat, inp);
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
    ArrayList<Object> initValues; //存储初始化值,可以是Expression或ArrayList

    StatementSetArr(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_SET_ARR);
        ArrayList stackTmp = inp.parseInstruct(src);
        if (!stackTmp.isEmpty()) {
            String leftStr = (String) stackTmp.get(0);
            stackTmp.remove(0);
            dimCell = new ExprCellArr(leftStr, inp);
        }

        // 检查是否有初始化值
        if (stackTmp.size() > 2 && stackTmp.get(0).equals("=") && stackTmp.get(1).equals("[")) {
            String initStr = src.substring(src.indexOf("=") + 1);
            initStr = initStr.substring(initStr.indexOf("[") + 1);
            initStr = initStr.substring(0, initStr.lastIndexOf(']'));

            // 解析嵌套的初始化值
            initValues = parseNestedInitValues(initStr, inp);
        } else {
            if (!stackTmp.isEmpty()) {
                String exprStr = src.substring(src.indexOf('=') + 1);
                expr = new Expression(exprStr, inp);
            }
        }
        Interpreter.putCachedVector(stackTmp);
    }

    // 解析嵌套的初始化值
    private ArrayList<Object> parseNestedInitValues(String initStr, Interpreter inp) throws Exception {
        ArrayList<Object> result = new ArrayList<>();
        ArrayList sValue = inp.parseInstruct(initStr);

        while (!sValue.isEmpty()) {
            StringBuffer exprStr = new StringBuffer();
            int bracketCount = 0;

            while (!sValue.isEmpty()) {
                String tmps = (String) sValue.get(0);
                sValue.remove(0);

                if (tmps.equals("[")) {
                    bracketCount++;
                    exprStr.append(tmps);
                } else if (tmps.equals("]")) {
                    bracketCount--;
                    exprStr.append(tmps);
                    if (bracketCount == 0) {
                        // 遇到完整的括号对，递归解析内部的值
                        String nestedStr = exprStr.toString();
                        result.add(parseNestedInitValues(nestedStr.substring(1, nestedStr.length() - 1), inp));
                        exprStr.setLength(0);
                        break;
                    }
                } else if (tmps.equals(",") && bracketCount == 0) {
                    break;
                } else {
                    exprStr.append(tmps);
                }
            }

            if (exprStr.length() > 0) {
                // 检查是否是表达式（包含运算符）
                boolean isExpression = false;
                for (int i = 0; i < exprStr.length(); i++) {
                    char ch = exprStr.charAt(i);
                    if ("+-*/%><=!&|".indexOf(ch) >= 0) {
                        isExpression = true;
                        break;
                    }
                }

                if (isExpression) {
                    // 如果是表达式，创建 Expression 对象
                    result.add(new Expression(exprStr.toString(), inp));
                } else {
                    // 如果不是表达式，尝试解析为常量
                    String value = exprStr.toString().trim();
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        result.add(new Expression(value, inp));
                    } else if (value.startsWith("\"") && value.endsWith("\"")) {
                        result.add(new Expression(value, inp));
                    } else {
                        try {
                            // 尝试解析为数字
                            Long.parseLong(value);
                            result.add(new Expression(value, inp));
                        } catch (NumberFormatException e) {
                            // 如果不是数字，当作变量处理
                            result.add(new Expression(value, inp));
                        }
                    }
                }
            }
        }

        Interpreter.putCachedVector(sValue);
        return result;
    }
};

class StatementFor extends Statement {
    StatementSetVar initStat;    // 初始化表达式
    Expression condExpr;    // 条件表达式
    StatementSetVar stepStat;    // 步进表达式
    short ip_efor = -1;    // efor语句的位置
    boolean firstEnter = true;// 是否 是第一次进入循环

    StatementFor(String src, Interpreter inp) throws Exception {
        type = (Interpreter.KEYWORD_FOR);
        // 解析for语句的三个部分：初始化、条件、步进
        String content = src.substring(Interpreter.STRS_RESERVED[Interpreter.KEYWORD_FOR].length());
        content = content.substring(content.indexOf('(') + 1, content.lastIndexOf(')'));

        // 使用 parseInstruct 分割表达式
        ArrayList<String> parts = new ArrayList<>();
        ArrayList<String> tokens = inp.parseInstruct(content);
        StringBuilder currentExpr = new StringBuilder();

        while (!tokens.isEmpty()) {
            String token = (String) tokens.get(0);
            tokens.remove(0);

            if (token.equals(",")) {
                parts.add(currentExpr.toString().trim());
                currentExpr.setLength(0);
            } else {
                currentExpr.append(token);
            }
        }
        Interpreter.putCachedVector(tokens);
        // 添加最后一个表达式
        if (currentExpr.length() > 0) {
            parts.add(currentExpr.toString().trim());
        }

        if (parts.size() != 3) {
            throw new Exception("Invalid for statement format: expected 3 expressions");
        }

        // 创建三个表达式
        initStat = new StatementSetVar(parts.get(0), inp);
        condExpr = new Expression(parts.get(1), inp);
        stepStat = new StatementSetVar(parts.get(2), inp);
    }
}

class StatementEfor extends Statement {
    short ip_for = -1;    // for语句的位置

    StatementEfor(String src, Interpreter inp) {
        type = (Interpreter.KEYWORD_EFOR);
    }
}

