package org.mini.xmlui.gscript;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
//import main.Util;

/**
 * 解释器 Interpreter , can execute script that like basic language phrase.
 * <p>
 * the next version would be comment with english all , sorry
 * <p>
 * KNOWN ISSUE:
 * <p>
 * MODIFY LOG :
 * 20070117 修改了load()方法，使支持本地文件载入，使可以支持GB2312文件<br/>
 * 修改了start()方法，使外部传进来的环境变量都变为小写，否则可能无法找到此变量 修改了calcExprLogImpl()方法，修正计算(true)<br/>
 * 表达式的错误 修改了calcExprNumImpl()方法，修正计算 (true) 表达式的错误<br/>
 * 修正了Stdlib中一些标准方法的错误，因上0.8.0中修改了容器，所以弹出的先后顺序有误 修正了load中，对行尾注释的错误处理 <br/>
 * 20080411 修正了变量赋值时，没有判定变量是局部变量或是全局变量，直接放入了局部变量 加入了loadFromString <br/>
 * 20080628 修改了parseArrayPos()中的数组维数类型的判断，原只判定维数为串时报错，改为，如果维数为非Int型报错 <br/>
 * 20090510 添加了Obj类，用于存储对象数据类型 <br/>
 * 20090705 修正了数组赋值时，没有判定变量是局部变量或是全局变量 <br/>
 * 20090721 修正了数组取值，增加了嵌套维数a[b[a+b]] <br/>
 * 20110819 添加预编译处理过程,加快执行速度<br/>
 * 20130720 修改了数值类型为整型<br/>
 *
 * <p>
 * Title: </p>
 * <p>
 * Description: </p>
 * <p>
 * Copyright: Copyright (c) 2007</p>
 * <p>
 * Company: </p>
 *
 * @author Gust
 * @version 1.0.0 sourceforge opened first version.
 * @ lastModify 2013
 */
public class Interpreter {

    static final String[] STRS_RESERVED = {
            "if", "else", "eif", "while", "loop", "sub", "ret", "", "", ""
    };
    static private final String STR_SYMBOL = "+-*/><()=, []:&|!\'";
    static private final String STR_NUMERIC = "0123456789";
    //关键字代码
    static final int NOT_KEYWORD = -1,
            KEYWORD_IF = 0,
            KEYWORD_ELSE = 1,
            KEYWORD_ENDIF = 2,
            KEYWORD_WHILE = 3,
            KEYWORD_LOOP = 4,
            KEYWORD_SUB = 5,
            KEYWORD_RET = 6,
            KEYWORD_CALL = 7,
            KEYWORD_SET_VAR = 8,
            KEYWORD_SET_ARR = 9;
    public static final int ERR_ILLEGAL = 0, ERR_VAR = 1, ERR_TYPE_INVALID = 2, ERR_NOSUB = 3, ERR_NO_VAR = 4, ERR_PARA_CALC = 5, ERR_PAESEPARA = 6, ERR_NO_SRC = 7, ERR_OPSYMB = 8, ERR_ARR_OUT = 9;
    public static final String[] STRS_ERR = {
            " Illegal statment ,", " Invalid variable name ", " Data type error ", " No such method ", " No such variable ", " Method parameter error ", " Parameter count error ", " Code not load yet ", " Operation symbol error  ", " Array out of bounds  "
    };
    //源码字符串数组
    //private Vector srcCode;
    private Statement[] srcCompiled;
    //变量范围控制
    private boolean isTopCall; //是否是顶级调用
    private Hashtable globalVar = new Hashtable(); //全局变量
    //脚本中过程首地址  ,sub address in script
    private Hashtable subAddr = new Hashtable();
    //系统过程及扩充过程列表 ,extend method lib
    private Vector extSubList = new Vector();

    /**
     * 构造方法
     */
    public Interpreter() {
    }

    /**
     * 初始化类
     */
    private void init() {
        srcCompiled = null;
        //脚本中过程首地址
        subAddr.clear();
        //系统过程及扩充过程列表
        extSubList.removeAllElements();
        //初始化全局变量表
        globalVar.clear();
//        //初始化解析
//        stack.clear();
        //加入标准库
        Stdlib stdlib = new Stdlib();
        register(stdlib);
    }

    /**
     * 装载脚本
     *
     * @param path String
     */
    public void loadFromFile(String path) {

        //初始化
        init();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            File ipFile = new File(path); //j2se使用此方法
            FileInputStream is = new FileInputStream(ipFile);

//            InputStream is = "".getClass().getResourceAsStream(path);   //j2me使用
//            int ch = 0;
//            while ((ch = is.read()) != -1) {
//                baos.write(ch);
//            }
//            is.close();
//            is=null;
            byte[] buff = new byte[512];
            int count = 0;
            while ((count = is.read(buff)) > 0) {//读取文件
                baos.write(buff, 0, count);
            }
            baos.write('\n');
            is.close();
            is = null;
        } catch (Exception ex) {
        }
        byte[] srcBytes = baos.toByteArray();
        try {
            baos.close();
            baos = null;
        } catch (IOException ioe) {
        }
        //求出总行数,去掉0x0d标识，win和xnix下的换行不统一
        int lineCount = 1; //增加一行，留下余地
        for (int i = 0; i < srcBytes.length; i++) {
            int ch = srcBytes[i];
            if (ch == 0x0a) {
                lineCount++;
            }
            if (ch == 0x0d || ch == 0x09) { //预处理0x0d0a=0x20,0x09=0x20
                srcBytes[i] = 0x20;
            }
        }

        //转换为String数组
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        //StringBuffer sb = new StringBuffer();
        Vector srcCode = new Vector();
        srcCompiled = new Statement[lineCount];
        lineCount = 0;
        for (int i = 0; i < srcBytes.length; i++) {
            if (srcBytes[i] == 0x0a || i + 1 == srcBytes.length) { //行结束,或者文件结束
                try {
                    String s = new String(line.toByteArray());    //j2me使用
                    //String s = new String(line.toByteArray(), "GB2312"); //j2se使用
                    s = s.trim();
                    if (s.length() > 0) {
                        srcCode.addElement(s);
                    }
                } catch (Exception ex1) {
                }
                line.reset();
            } else {
                line.write(srcBytes[i]);
            }
        }
        try {
            line.close();
            line = null;
        } catch (IOException ioe) {
        }
        preProcess(srcCode);

    }

    /**
     * 从字符串中解析出代码
     *
     * @param code
     */
    public void loadFromString(String code) {

        init();

        int dquodation = 0;
        StringBuffer line = new StringBuffer();
        Vector v = new Vector();
        for (int i = 0, len = code.length(); i < len; i++) {
            char ch = code.charAt(i);
            if (ch == '"') {
                dquodation++;
            }
            if ((dquodation % 2) == 0 && (ch == ';' || ch == '\n')) {
                String s = line.toString().trim();
                if (s.length() > 0) {
                    v.addElement(s);
                }
                line.setLength(0);
            } else {
                line.append(ch);
            }
        }
        preProcess(v);
    }

    public Lib getLib() {
        return (Lib) extSubList.elementAt(0);
    }

    public void removeLib(Lib lib) {
        extSubList.removeElement(lib);
    }

    /**
     * 预处理
     */
    private void preProcess(Vector srcCode) {
        //预处理
        for (int i = 0; i < srcCode.size(); ) {
            //校验是否有空串
            if (srcCode.elementAt(i) == null) {
                srcCode.removeElementAt(i);
                continue;
            }
            //去掉注释
            String el = (String) srcCode.elementAt(i);
            if (el.indexOf('\'') >= 0) {
                int dqCount = 0; //双引号计数
                for (int m = 0; m < el.length(); m++) {
                    char ch = el.charAt(m);
                    if (ch == '\"') {
                        dqCount++;
                    }
                    if (ch == '\'' && (dqCount % 2) == 0) { //如果 ' 不在双引号内，则说明是注释
                        el = el.substring(0, m);
                    }
                }
            }
            el = el.trim();
            if (el.length() <= 0) {
                srcCode.removeElementAt(i);
                continue;
            }
            srcCode.setElementAt(el, i);
            i++;
        }
        srcCompiled = new Statement[srcCode.size()];
        for (int i = 0; i < srcCode.size(); i++) {
            try {
                String sc = (String) srcCode.elementAt(i);
                Statement st = Statement.parseInstruct(sc, this);
                srcCompiled[i] = st;
                st.src = sc;
                //System.out.println(i + " " + sc);
            } catch (Exception e) {
                errout(i, STRS_ERR[ERR_ILLEGAL] + srcCode.elementAt(i));
                e.printStackTrace();
                break;
            }

            //找过程起始行号
            if (srcCompiled[i].type == KEYWORD_SUB) {
                StatementSub ss = (StatementSub) srcCompiled[i];
                subAddr.put(ss.cell.subName, new Int(i)); //放入过程表中
            }
        }
    }

    /**
     * 启动执行
     *
     * @return Object
     */
    public Object start() {
        return start(null);
    }

    /**
     * @param env Hashtable
     * @return Object
     */
    public Object start(Hashtable env) {
        //变量范围控制
        isTopCall = true; //是否是顶级调用
        //开始执行代码

        try {
            if (srcCompiled == null) {
                errout(0, STRS_ERR[ERR_NO_SRC]);
            } else {
                //如果环境变量存在
                if (env != null) {
                    for (Enumeration e = env.keys(); e.hasMoreElements(); ) {
                        Object key = e.nextElement();
                        Object val = env.get(key);
                        key = ((String) key).toLowerCase();
                        globalVar.put(key, val);
                    }
                }
                //执行脚本
                return callSub(null, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 调用函数
     *
     * @param instract
     * @return
     */
    public Object callSub(String instract) {
        try {
            Statement pstat = Statement.parseInstruct(instract, this);
            if (pstat.type == KEYWORD_CALL) {
                StatementCall pstatCall = (StatementCall) pstat;
                Hashtable ht = new Hashtable();
                return callSub(pstatCall.cell, ht);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 放入全局变量
     *
     * @param varName
     * @param value
     */
    public void putGlobalVar(String varName, DataType value) {
        globalVar.put(varName, value);
    }

    /**
     * 取全局变量
     *
     * @param varName
     * @return
     */
    public Object getGlobalVar(String varName) {
        return globalVar.get(varName);
    }

    /**
     * 注册扩充方法库
     *
     * @param lib Lib
     */
    public void register(Lib lib) {
        extSubList.addElement(lib);
    }

    //--------------------------------------------------------------------------
    //                                  private method
    //--------------------------------------------------------------------------

    /**
     * 过程解析,脚本执行体
     *
     * @param paraStack      Hashtable
     * @param instrucPointer int
     * @return Object
     */
    private DataType _sub(Vector paraStack, int instrucPointer) {

        int ip = instrucPointer; //运行行号
        //构建变量表
        Hashtable localVar; //本方法的变量表,键是变量名，值是变量值
        //把当前方法的变量表压入stack
        if (isTopCall) {
            localVar = globalVar; //顶级方法的局部变量为全局变量
            isTopCall = false;
        } else {
            localVar = new Hashtable(); //非顶级调用，则是局部方法
        }

        //把参数放进列表
//        if (bolo.CompilerCfg.isProfile) {
//			debug.Profile.instance.begin("subname");
//		}
        long calls = System.currentTimeMillis();

        //把过程调用的参数放入localVar
        Statement pstat = srcCompiled[ip];
        if (pstat != null && pstat.type == KEYWORD_SUB) {
            StatementSub psubstat = (StatementSub) pstat;
            for (int i = 0, j = psubstat.cell.para.length; i < j; i++) {
                DataType pp = (DataType) (paraStack.isEmpty() ? null : paraStack.elementAt(j - i - 1));
                if (pp != null) {
                    ExprCellVar var = (ExprCellVar) (psubstat.cell.para[i].cells[0]);
                    (localVar).put(var.varName, pp);
                } else {
                    errout(ip, STRS_ERR[ERR_PAESEPARA]);
                }
            }
            ip++; //跳到下一行
        }
        calls = System.currentTimeMillis() - calls;
//        System.out.println(calls);
//        if (bolo.CompilerCfg.isProfile) {
//			debug.Profile.instance.end("subname");
//		}
        calls = System.currentTimeMillis();
        while (ip < srcCompiled.length) {
            try {
                //String instruct = srcCode[ip];
                Statement stat = srcCompiled[ip];

                //System.out.println(ip + " " + stat.src);
                int keywordCode = stat.type;
                if (keywordCode >= 0) { //是关键字语句
                    switch (keywordCode) {
                        case KEYWORD_WHILE://循环
                        {
                            StatementWhile pstatWhile = (StatementWhile) stat;
                            Bool wpdt = (Bool) calcExpr(pstatWhile.expr, localVar);
                            if (wpdt.getVal() == false) { //如果为假，则查else或endif
                                if (pstatWhile.ip_loop != -1) {
                                    ip = pstatWhile.ip_loop;
                                } else {
                                    int countWhile = 1;
                                    for (int i = ip + 1; i < srcCompiled.length; i++) {
                                        Statement tmpst = srcCompiled[i];
                                        if (tmpst.type == KEYWORD_WHILE) {
                                            countWhile++;
                                        } else if (tmpst.type == KEYWORD_LOOP) {
                                            countWhile--;
                                        }
                                        //跳转
                                        if (tmpst.type == KEYWORD_LOOP
                                                && countWhile == 0) {
                                            ip = i;
                                            pstatWhile.ip_loop = (short) ip;
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case KEYWORD_LOOP:
                            StatementLoop sl = (StatementLoop) stat;
                            if (sl.ip_while != -1) {
                                ip = sl.ip_while;
                            } else {
                                int countWhile = 1;
                                for (int i = ip - 1; i > 0; --i) {
                                    Statement tmp = srcCompiled[i];
                                    if (tmp.type == KEYWORD_WHILE) {
                                        countWhile--;
                                    } else if (tmp.type == KEYWORD_LOOP) {
                                        countWhile++;
                                    }
                                    //跳转
                                    if (tmp.type == KEYWORD_WHILE && countWhile == 0) {
                                        ip = i - 1;
                                        sl.ip_while = (short) ip;
                                        break;
                                    }
                                }
                            }
                            break;
                        case KEYWORD_IF: //if分支
                            StatementIf pstatIf = (StatementIf) stat;

                            Bool ib = (Bool) calcExpr(pstatIf.expr, localVar);
                            if (ib.getVal() == false) { //如果为假，则查else或endif
                                if (pstatIf.ip_else != -1) {
                                    ip = pstatIf.ip_else;
                                } else if (pstatIf.ip_endif != -1) {
                                    ip = pstatIf.ip_endif;
                                } else {
                                    int countIf = 1;
                                    for (int i = ip + 1; i < srcCompiled.length; i++) {
                                        Statement tmpst = srcCompiled[i];
                                        if (tmpst.type == KEYWORD_IF) {
                                            countIf++;
                                        } else if (tmpst.type == KEYWORD_ENDIF) {
                                            countIf--;
                                        }

                                        //跳转
                                        if (tmpst.type == KEYWORD_ELSE
                                                && countIf == 1) {
                                            ip = i;
                                            pstatIf.ip_else = (short) ip;
                                            break;
                                        } else if (tmpst.type == KEYWORD_ENDIF
                                                && countIf == 0) {
                                            ip = i;
                                            pstatIf.ip_endif = (short) ip;
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                        case KEYWORD_ELSE:
                            StatementElse pstatElse = (StatementElse) stat;
                            if (pstatElse.ip_endif != -1) {
                                ip = pstatElse.ip_endif;
                            } else {
                                int countIf = 1;
                                for (int i = ip + 1; i < srcCompiled.length; i++) {
                                    Statement tmpst = srcCompiled[i];
                                    if (tmpst.type == KEYWORD_IF) {
                                        countIf++;
                                    } else if (tmpst.type == KEYWORD_ENDIF) {
                                        countIf--;
                                    }
                                    //跳转
                                    if (tmpst.type == KEYWORD_ENDIF
                                            && countIf == 0) {
                                        ip = i;
                                        pstatElse.ip_endif = (short) ip;
                                        break;
                                    }
                                }
                            }
                            break;
                        case KEYWORD_CALL: {
                            StatementCall pstatCall = (StatementCall) stat;
                            callSub(pstatCall.cell, localVar);

                            break;
                        }
                        case KEYWORD_SET_VAR: {
                            StatementSetVar pstatVar = (StatementSetVar) stat;
                            _setVar(pstatVar, localVar);
                            break;
                        }
                        case KEYWORD_ENDIF: {
                            break;
                        }

                        case KEYWORD_SET_ARR: {
                            StatementSetArr pstatArr = (StatementSetArr) stat;
                            _setArr(pstatArr, localVar);
                            break;
                        }

                        case KEYWORD_SUB:
                            return null;
                        case KEYWORD_RET: //过程结束
                            StatementRet pstatRet = (StatementRet) stat;
                            if (pstatRet.expr != null) {
                                return calcExpr(pstatRet.expr, localVar);
                            } else {
                                return null;
                            }
                    }
                } else {
                    errout(ip, STRS_ERR[ERR_ILLEGAL]);
                }
            } catch (Exception ex) {
                errout(ip, STRS_ERR[ERR_ILLEGAL] + ex.getMessage());
                ex.printStackTrace();
                break;
            }
            //指针自动加一
            ip++;
        } //end while
        calls = System.currentTimeMillis() - calls;
        return null;
    }

//;----------------------------------------------------------------------------
//;                                  工具方法
//;----------------------------------------------------------------------------

    /**
     * 得到第一个单词
     *
     * @param instruct String
     * @return String
     */
    String getFirstWord(String instruct) {
        instruct = instruct.trim();
        StringBuffer tsb = new StringBuffer();
        for (int i = 0; i < instruct.length(); i++) {
            if (isSymbol(instruct.charAt(i))) {
                break;
            }
            tsb.append(instruct.charAt(i));
        }
        return tsb.toString().trim().toLowerCase();
    }

    /**
     * 是否是关键字
     *
     * @param s String
     * @return boolean
     */
    int getKeywordCode(String s) {
        if (s == null || s.length() == 0) {
            return -1;
        }
        for (int i = 0; i < STRS_RESERVED.length; i++) {
            if (s.equals(STRS_RESERVED[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 是否是过程调用
     *
     * @param s String
     * @return boolean
     */
    boolean isSubCall(String s) {
        if (getInstructType(s) == '(') {
            return true;
        }
        return false;
    }

    /**
     * 是变量
     *
     * @param s String
     * @return boolean
     */
    boolean isVar(String s) {
        if (getInstructType(s) == '=') {
            return true;
        }

        return false;
    }

    /**
     * 是数组
     *
     * @param s String
     * @return boolean
     */
    boolean isArr(String s) {
        if (getInstructType(s) == '[') {
            return true;
        }

        return false;
    }

    /**
     * 得到第一个符号,由此可判别出是一条什么语句
     *
     * @param instruct String
     * @return String
     */
    char getInstructType(String instruct) {
        for (int i = 0; i < instruct.length(); i++) {
            if ("(=[".indexOf(instruct.charAt(i)) >= 0) {
                return instruct.charAt(i);
            }
        }
        return 0;
    }

    /**
     * 是符号
     *
     * @param ch String
     * @return boolean
     */
    boolean isSymbol(char ch) {
        if (STR_SYMBOL.indexOf(ch) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * 是数字
     *
     * @param ch char
     * @return boolean
     */
    boolean isNumeric(char ch) {
        if (STR_NUMERIC.indexOf(ch) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * 是字母
     *
     * @param ch char
     * @return boolean
     */
    boolean isLetter(char ch) {
        if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
            return true;
        }
        return false;
    }

    /**
     * 解析指令
     *
     * @param s String
     * @return Vector
     * @throws Exception
     */
    Vector parseInstruct(String s) {
//    	if(!stack.isEmpty()){
//    		return stack;
//    	}
        Vector stack = new Vector();
//    	if (bolo.CompilerCfg.isProfile) {
//			debug.Profile.instance.begin("parse");
//		}

        int len = s.length();

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ') { //无用空格
                continue;
            } else if (ch == '\'') { //去掉注释
                break;
            } else if (ch == '"') { //是字符串
                sb.append(ch);
                i++;
                for (; i < s.length(); i++) { //找到全部串
                    ch = s.charAt(i);
                    sb.append(ch);
                    if (ch == '"') {
                        break;
                    }
                }
            } else if (isSymbol(ch)) { //是其他运算符号
                sb.append(ch);
                if (ch == '>') { //可能的>=
                    if (i + 1 < len) {
                        if (s.charAt(i + 1) == '=') {
                            sb.append(s.charAt(i + 1));
                            i++;
                        }
                    }
                } else if (ch == '<') { //可能的<=,  <>
                    if (i + 1 < len) {
                        if (s.charAt(i + 1) == '=' || s.charAt(i + 1) == '>') {
                            sb.append(s.charAt(i + 1));
                            i++;
                        }
                    }
                }

            } else if (isLetter(ch)) { //字母开头号的是变量或过程调用，后面可能是( [
                for (; i < s.length(); i++) { //找到变量名
                    ch = s.charAt(i);
                    if (isSymbol(ch)) {
                        break;
                    }
                    sb.append(ch);
                }
                //去掉空格
                for (; i < s.length(); i++) {
                    if ((ch = s.charAt(i)) != ' ') {
                        break;
                    }
                }
                //看单词后是什么符号

                if (ch == '[') { //是数组变量
                    int leftQ = 0;
                    do {
                        if (ch == '[') {
                            leftQ++;
                        }
                        if (ch == ']') {
                            leftQ--;
                        }
                        sb.append(ch);
                        i++;
                    } while (i < s.length() && ("+-*/,)=:><".indexOf(ch = s.charAt(i)) < 0 || leftQ > 0)); //end while
                } else if (ch == '(') { //是过程调用
                    int leftQ = 0;
                    do {
                        ch = s.charAt(i);
                        if (ch == '(') {
                            leftQ++;
                        }
                        if (ch == ')') {
                            leftQ--;
                        }
                        sb.append(ch);
                        i++;
                    } while (leftQ > 0 && i < s.length()); //end while
                }

                i--;
            } else if (isNumeric(ch)) { //数字开头常数
                for (; i < s.length(); i++) { //找到变量名
                    ch = s.charAt(i);
                    if (isSymbol(ch)) {
                        i--;
                        break;
                    }
                    sb.append(ch);
                }
            }
            //添加项目到栈中
            stack.addElement(sb.toString());
            sb.setLength(0);
        }
//        if (bolo.CompilerCfg.isProfile) {
//			debug.Profile.instance.end("parse");
//		}
        return stack;
    }

    /**
     * 输出
     *
     * @param ip int
     * @param s  String
     */
    private void errout(int ip, String s) {
        String src = "";
        if (srcCompiled[ip] != null) {
            src = srcCompiled[ip].src;
        }
        System.out.println((ip + 1) + " " + src + " : " + s);
    }

//    /**
//     *
//     * @param s String
//     */
//    private void print(String s) {
//        System.out.println("DEBUG : " + s);
//    }

    /**
     * 此对象是否是String
     *
     * @param o Object
     * @return boolean
     */
    private boolean isStr(DataType o) {
        if ((o).type == DataType.DTYPE_STR) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 此对象是否是Obj
     *
     * @param o Object
     * @return boolean
     */
    private boolean isObj(DataType o) {
        if ((o).type == DataType.DTYPE_OBJ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 此对象是否是Integer
     *
     * @param o Object
     * @return boolean
     */
    private boolean isInt(DataType o) {
        if ((o).type == DataType.DTYPE_INT) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isArr(DataType o) {
        if ((o).type == DataType.DTYPE_ARRAY) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isBool(DataType o) {
        if ((o).type == DataType.DTYPE_BOOL) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSymb(DataType o) {
        if ((o).type == DataType.DTYPE_SYMB) {
            return true;
        } else {
            return false;
        }
    }

    static public final DataType vPopFront(Vector v) {
        if (v.size() <= 0) {
            return null;
        }
        DataType o = (DataType) v.firstElement();
        if (v.size() > 0) {
            v.removeElementAt(0);
        }
        return o;
    }

    static public final DataType vPopBack(Vector v) {
        if (v.size() <= 0) {
            return null;
        }
        DataType o = (DataType) v.lastElement();
        if (v.size() > 0) {
            v.removeElementAt(v.size() - 1);
        }
        return o;
    }

    static public final void vPushFront(Vector v, DataType o) {
        v.insertElementAt(o, 0);
    }

    static public final void vPushBack(Vector v, DataType o) {
        v.addElement(o);
    }

    /**
     * 在变量列表v中查找变量
     *
     * @param name     String
     * @param localVar Stack
     * @return Object
     */
    private DataType _getVar(String name, Hashtable localVar) {
        name = name.toLowerCase();
        DataType value = (DataType) localVar.get(name);
        if (value == null) {
            value = (DataType) globalVar.get(name);
        }
        return value;
    }

    /**
     * 赋值操作
     *
     * @param stat    String
     * @param varList Stack
     * @throws Exception
     */
    private void _setVar(StatementSetVar stat, Hashtable varList) throws Exception {
        //格式化表达式
        String varName = stat.varName;

        DataType nValue = calcExpr(stat.expr, varList);

        if (nValue != null) {
            DataType varValue = (DataType) varList.get(varName);
            DataType varValue1 = (DataType) globalVar.get(varName);

            if (varValue != null) {
                if (varValue.type == nValue.type) {
                    varList.put(varName, nValue);
                } else {
                    throw new Exception(STRS_ERR[ERR_TYPE_INVALID]);
                }
            } else if (varValue1 != null) {
                if (varValue1.type == nValue.type) {
                    globalVar.put(varName, nValue);
                } else {
                    throw new Exception(STRS_ERR[ERR_TYPE_INVALID]);
                }
            } else {
                varList.put(varName, nValue);
            }
        }
    }

    //---------------------------表达式求值------------------------------
    DataType calcExpr(Expression stat, Hashtable varList) throws Exception {

        //Vector expr = parseInstruct(exprStr); //分解表达式
        Vector expr = preCalc(stat, varList); //求变量和过程调用的值

        //运算类型
        final int T_NUM = 1 //数值
                , T_STR = 2 //串
                , T_LOG = 4 //布尔值
                , T_ARR = 8 //数组指针，非数组
                , T_LOGSYM = 16 //逻辑符号
                , T_OBJ = 32; //对象
        int cType = 0, cType1 = 0; //默认为算术运算

        for (int i = 0; i < expr.size(); i++) {
            DataType o = (DataType) expr.elementAt(i);
            //串类型
            if (isStr(o)) {
                cType |= T_STR;
            } else if (isSymb(o)) { //逻辑类型
                if (((Symb) o).isLogicOp()) {
                    cType |= T_LOGSYM;
                }
            } else if (isBool(o)) {
                cType |= T_LOG;
            } else if (isArr(o)) {
                cType |= T_ARR;
            } else if (isInt(o)) {
                cType |= T_NUM;
            } else if (isObj(o)) {
                cType |= T_OBJ;
            }
        }

        //下面将判别是否运算正常
        if (((cType & T_NUM) != 0) && ((cType & T_STR) == 0) && ((cType & T_ARR) == 0) && (cType & T_LOG) == 0 && (cType & T_LOGSYM) == 0 && (cType & T_OBJ) == 0) { //数值运算，只要有数值
            cType1 = T_NUM;
        } else if ((cType & T_STR) != 0 && (cType & T_LOGSYM) == 0) { //字符串，除不许逻辑符号外都可
            cType1 = T_STR;
        } else if (((cType & T_LOG) != 0 || (cType & T_LOGSYM) != 0) && (cType & T_STR) == 0 && (cType & T_ARR) == 0 && (cType & T_OBJ) == 0) { //逻辑运算，只要有逻辑符号或逻辑值均可，但需无串，无数组
            cType1 = T_LOG;
        } else if ((cType & T_ARR) == cType) { //数组指针，其他均无
            cType1 = T_ARR;
        } else if ((cType & T_OBJ) == cType) { //对象参数，其他均无
            cType1 = T_OBJ;
        } else {
            //出错，需处理
            throw new Exception(STRS_ERR[ERR_ILLEGAL]);
        }

        DataType resultDt = null;

        switch (cType1) {
            case T_NUM:

                //左递归运算
                while (expr.size() > 1) { //当只一个单元时停止
                    calcExprNumImpl(expr);
                }
                resultDt = vPopBack(expr);
                break;
            case T_STR:
                calcExprStrImpl(expr);
                resultDt = vPopBack(expr);

                break;
            case T_LOG: //分解逻辑表达式与算术表达式，主要是把算术表达式先于逻辑运算求值
            {
                //下两个变量用于逻辑表达式求值过程
                Vector log_dElem = new Vector(); //逻辑
                Vector ari_dElem = new Vector(); //算术

                while (expr.size() > 0) {
                    DataType pdt = vPopFront(expr);

                    ari_dElem.removeAllElements();
                    if (isSymb(pdt)) {
                        Symb pst = (Symb) pdt;
                        if (pst.isCompOp()) { //以比较逻辑运算符进行拆分

                            //把刚加入log_dElem中的算术部分拿出来，算出结果再放回去
                            //从后向前找,如：3-(1+4)<>0  从<>处找到3-(1+4)
                            int rightQ = 0; //右括号计数
                            while (log_dElem.size() > 0) {
                                DataType pdt2 = vPopBack(log_dElem);
                                if (isSymb(pdt2)) {

                                    if (((Symb) pdt2).getVal() == Symb.RP) {
                                        rightQ++; //右括号+1

                                    }
                                    if (((Symb) pdt2).isLogicOp() //如果遇另一个
                                            || (((Symb) pdt2).getVal() == Symb.LP && rightQ == 0) //当遇到左括号且括号计数为0时
                                    ) {
                                        vPushBack(log_dElem, pdt2); //放回去
                                        break;
                                    }
                                    if (((Symb) pdt2).getVal() == Symb.LP) {
                                        rightQ--; //右括号-1
                                    }
                                }
                                vPushFront(ari_dElem, pdt2);
                            } //end while
                            //运算出算术表达式
                            while (ari_dElem.size() > 1) {
                                calcExprNumImpl(ari_dElem);
                                //放入逻辑表达式中
                            }
                            if (ari_dElem.size() > 0) {
                                vPushBack(log_dElem, vPopFront(ari_dElem));
                            }
                        }
                    } //end if是符号

                    //插入表达式元素
                    vPushBack(log_dElem, pdt);

                    //再找比较逻辑运算符后面的元素
                    ari_dElem.removeAllElements();
                    if (isSymb(pdt)) {

                        if (((Symb) pdt).isCompOp()) { //以比较逻辑运算符进行拆分

                            //把刚加入log_dElem中的算术部分拿出来，算出结果再放回去
                            //从后向前找,如：3-(1+4)<>0  从<>处找到3-(1+4)
                            int leftQ = 0; //左括号计数
                            while (expr.size() > 0) {
                                DataType pdt2 = vPopFront(expr);
                                if (isSymb(pdt2)) {

                                    Symb pst2 = (Symb) pdt2;
                                    byte opType = pst2.getVal();
                                    if (opType == Symb.LP) {
                                        leftQ++; //左括号+1

                                    }
                                    if (pst2.isLogicOp() //如果遇另一个
                                            || (opType == Symb.RP && leftQ == 0) //当遇到左括号且括号计数为0时
                                    ) {
                                        vPushFront(expr, pdt2); //放回去
                                        break;
                                    }
                                    if (opType == Symb.RP) {
                                        leftQ--; //左括号-1
                                    }
                                }
                                vPushBack(ari_dElem, pdt2);
                            } //end while
                            //运算出算术表达式
                            while (ari_dElem.size() > 1) {
                                calcExprNumImpl(ari_dElem);
                                //放入逻辑表达式中
                            }
                            if (ari_dElem.size() > 0) {
                                vPushBack(log_dElem, vPopFront(ari_dElem));
                            }
                        }
                    } //end if是符号
                } //end while

                //逻辑表达式求值
                while (log_dElem.size() > 1) {
                    calcExprLgcImpl(log_dElem);
                }
                resultDt = vPopBack(log_dElem);

            }
            break;
            case T_ARR:
                resultDt = vPopBack(expr);

                break;
            case T_OBJ:
                resultDt = vPopBack(expr);

                break;
            default:

                //出错，需处理
                throw new Exception(STRS_ERR[ERR_ILLEGAL]);

        }
        return resultDt;
    }

    /**
     * 求出表达式中变量和过程调用的值
     *
     * @param expr    Stack
     * @param varList Hashtable
     * @return Stack
     * @throws Exception
     */
    private Vector preCalc(Expression expr, Hashtable varList) throws Exception {
        Vector tgt = new Vector();

        for (int i = 0, len = expr.cells.length; i < len; i++) {
            ExprCell cell = expr.cells[i];
            //是串，包括变量，方法，符号,字符串

            switch (cell.type) {
                case ExprCell.EXPR_CELL_DATATYPE: {// 是字符串
                    ExprCellDataType celldt = (ExprCellDataType) cell;
                    DataType pdt = clone4calc(celldt.pit);
                    tgt.addElement(pdt);
                    break;
                }
                case ExprCell.EXPR_CELL_VAR: {//是变量
                    ExprCellVar cellv = (ExprCellVar) cell;
                    DataType pdt = _getVar(cellv.varName, varList);
                    if (pdt == null) {
                        //无变量，需处理
                        throw new Exception(STRS_ERR[ERR_NO_VAR] + cellv.varName);
                    } else {
                        tgt.addElement(pdt);
                    }
                    break;
                }
                case ExprCell.EXPR_CELL_CALL: {//是过调用
                    ExprCellCall cellc = (ExprCellCall) cell;
                    DataType pTmp = callSub(cellc, varList);
                    if (pTmp != null) {
                        tgt.addElement(pTmp);
                    }
                    break;
                }
                case ExprCell.EXPR_CELL_ARR: {//是数组
                    ExprCellArr cella = (ExprCellArr) cell;
                    tgt.addElement(_getArr(cella, varList));
                    break;
                }
            }
        }
        return tgt;
    }

    /**
     * 复制简单数据类型
     *
     * @param dt
     * @return
     * @throws Exception
     */
    private DataType clone4calc(DataType dt) throws Exception {
        switch (dt.type) {
            case DataType.DTYPE_INT:
                return new Int(((Int) dt).getVal());
            case DataType.DTYPE_SYMB:
                return dt;
            case DataType.DTYPE_STR:
                return new Str(((Str) dt).getVal());
            case DataType.DTYPE_BOOL:
                return new Bool(((Bool) dt).getVal());
            case DataType.DTYPE_OBJ:
                return new Obj(((Obj) dt).getVal());
            default:
                throw new Exception();
        }
    }

    /**
     * 字符串运算,只支持连接操作
     *
     * @param expr Stack
     */
    private void calcExprStrImpl(Vector expr) {
        StringBuffer sb = new StringBuffer();
        while (expr.size() > 0) { //不停的运算
            DataType ts = vPopFront(expr); //这里有可能是integer型,不能用强制转换
            if (ts.type != DataType.DTYPE_SYMB) { //如果不是符号
                sb.append((ts).getString());
            }
        }
        vPushFront(expr, new Str(sb.toString()));
    }

    /**
     * 求值运算实现
     *
     * @param expr Stack
     */
    private void calcExprNumImpl(Vector expr) {
        if (expr.size() == 1) { //单独变量
            return;
        } else { //表达式
            //按优先级进行计算,优先级如下：() 取负(正)值  */ + - %
            DataType element1 = vPopFront(expr);
            if (element1.type == DataType.DTYPE_SYMB) {
                if (((Symb) element1).getVal() == Symb.LP) {
                    calcExprNumImpl(expr);
                    DataType element2 = vPopFront(expr);
                    DataType element3 = vPopFront(expr);
                    if (element3.type == DataType.DTYPE_SYMB && ((Symb) element3).getVal() == Symb.RP) { //扔掉反括号
                        vPushFront(expr, element2);
                    } else { //括号中如果仍未计算完成,则继续
                        vPushFront(expr, element3);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        calcExprNumImpl(expr); //再算括号中的内容
                    }
                } else //取正值
                    if (((Symb) element1).getVal() == Symb.ADD) {
                        calcExprNumImpl(expr);
                    } else //取负值
                        if (((Symb) element1).getVal() == Symb.SUB) {
                            DataType element2 = vPopFront(expr);
                            if (element2.type == DataType.DTYPE_INT) { //立即数
                                element2 = new Int(-((Int) element2).getVal());
                                vPushFront(expr, element2);
                            } else { //表达式
                                vPushFront(expr, element2);
                                calcExprNumImpl(expr);
                                vPushFront(expr, element1);
                                calcExprNumImpl(expr);
                            }
                        }
            } else //是数字
                if (element1.type == DataType.DTYPE_INT) {
                    Symb element2 = (Symb) vPopFront(expr); //应是操作符
                    DataType element3 = vPopFront(expr); // 可能是操作数或操作符
                    //四则运算
                    if ((element2).getVal() == Symb.MUL || (element2).getVal() == Symb.DIV) {
                        if (element3.type == DataType.DTYPE_INT) {
                            long n1 = ((Int) element1).getVal();
                            long n2 = ((Int) element3).getVal();
                            Int val = new Int((element2).getVal() == Symb.MUL ? n1 * n2 : n1 / n2);
                            vPushFront(expr, val);
                        } else {
                            vPushFront(expr, element3);
                            calcExprNumImpl(expr);
                            vPushFront(expr, element2);
                            vPushFront(expr, element1);
                            calcExprNumImpl(expr);
                        }
                    } else if ((element2).getVal() == Symb.ADD || (element2).getVal() == Symb.SUB) {

                        boolean calc = false;
                        if (element3.type == DataType.DTYPE_INT) {
                            if (expr.size() == 0) { //无更多操作符和操作数时计算
                                calc = true;
                            } else {
                                DataType element4 = vPopFront(expr);
                                if (element4 != null) {
                                    if (((Symb) element4).getVal() != Symb.MUL && ((Symb) element4).getVal() != Symb.DIV) {
                                        calc = true;
                                    }
                                    vPushFront(expr, element4);
                                }
                            }
                        }
                        if (calc) {
                            long n1 = ((Int) element1).getVal();
                            long n2 = ((Int) element3).getVal();
                            Int val = new Int((element2).getVal() == Symb.ADD ? n1 + n2 : n1 - n2);
                            vPushFront(expr, val);
                        } else {
                            //先算右边的表达式
                            vPushFront(expr, element3); //放回去
                            calcExprNumImpl(expr); //计算

                            vPushFront(expr, element2);
                            vPushFront(expr, element1);
                            calcExprNumImpl(expr);
                        }
                    } else if (element2.getVal() == Symb.RP) { //是右括号
                        if (element3 != null) {
                            vPushFront(expr, element3);
                        }
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        return;
                    }

                }
        }
    }

    /**
     * 逻辑运算实现
     *
     * @param expr Stack
     */
    private void calcExprLgcImpl(Vector expr) {
        //计算逻辑表达式
        if (expr.size() == 1) { //单独变量
            return;
        } else { //表达式
            //按优先级进行计算,优先级如下：() 取负(正)值  */ + - %
            DataType element1 = vPopFront(expr);
            if (element1.type == DataType.DTYPE_SYMB) { //括号
                if (((Symb) element1).getVal() == Symb.LP) {
                    calcExprLgcImpl(expr);
                    DataType element2 = vPopFront(expr);
                    DataType element3 = vPopFront(expr);
                    if (element3.type == DataType.DTYPE_SYMB && ((Symb) element3).getVal() == Symb.RP) { //扔掉反括号
                        vPushFront(expr, element2);
                    } else { //括号中如果仍未计算完成,则继续
                        vPushFront(expr, element3);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        calcExprLgcImpl(expr); //再算括号中的内容
                    }
                } else if (((Symb) element1).getVal() == Symb.NOT) { //取反
                    DataType element2 = vPopFront(expr);
                    if (element2.type == DataType.DTYPE_BOOL) { //立即数
                        element2 = new Bool(!((Bool) element2).getVal());
                        vPushFront(expr, element2);
                    } else { //表达式
                        vPushFront(expr, element2);
                        calcExprLgcImpl(expr);
                        vPushFront(expr, element1);
                        calcExprLgcImpl(expr);
                    }
                }
            } else if (element1.type == DataType.DTYPE_INT) { //><=
                long n1, n2;

                n1 = ((Int) element1).getVal();

                DataType element2 = vPopFront(expr);
                //应是操作符
                Object element3 = vPopFront(expr); // 操作数或操作符 >= <=

                Symb operator = (Symb) element2;
                n2 = ((Int) element3).getVal();

                boolean result = false;
                if (operator.getVal() == Symb.GRE) {
                    result = n1 > n2;
                } else if (operator.getVal() == Symb.LES) {
                    result = n1 < n2;
                } else if (operator.getVal() == Symb.GE) {
                    result = n1 >= n2;
                } else if (operator.getVal() == Symb.LE) {
                    result = n1 <= n2;
                } else if (operator.getVal() == Symb.NE) {
                    result = n1 != n2;
                } else if (operator.getVal() == Symb.EQU) {
                    result = n1 == n2;
                }
                vPushFront(expr, new Bool(result));
            } else if (element1.type == DataType.DTYPE_BOOL) { //&|
                Symb element2 = (Symb) vPopFront(expr); //应是操作符
                DataType element3 = vPopFront(expr); // 操作数或操作符 >= <=
                if (element2.getVal() == Symb.AND) {
                    if (element3.type == DataType.DTYPE_BOOL) {
                        boolean result = ((Bool) element1).getVal() && ((Bool) element3).getVal();
                        vPushFront(expr, new Bool(result));
                    } else {
                        vPushFront(expr, element3);
                        calcExprLgcImpl(expr);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        calcExprLgcImpl(expr);
                    }
                } else if (element2.getVal() == Symb.OR) {
                    boolean calc = false;
                    if (element3.type == DataType.DTYPE_BOOL) {
                        if (expr.size() == 0) {
                            calc = true;
                        } else {
                            DataType element4 = vPopFront(expr); // 操作数或操作符 >= <=
                            if (element4.type == DataType.DTYPE_SYMB) {
                                if (((Symb) element4).getVal() != Symb.AND) {
                                    calc = true;
                                }
                            }
                            vPushFront(expr, element4);
                        }
                    }
                    if (calc) {
                        boolean result = ((Bool) element1).getVal() || ((Bool) element3).getVal();
                        vPushFront(expr, new Bool(result));
                    } else {
                        vPushFront(expr, element3);
                        calcExprLgcImpl(expr);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        calcExprLgcImpl(expr);
                    }

                } else if (element2.getVal() == Symb.RP) { //是右括号
                    if (element3 != null) {
                        vPushFront(expr, element3);
                    }
                    vPushFront(expr, element2);
                    vPushFront(expr, element1);
                    return;
                }

            }

        }
    }

//---------------------------过程调用------------------------------

    /**
     * 过程调用 写在脚本中的参数，a(p1,p2,p3) 在传递的vector中 p3,p2,p1 的顺序排列
     *
     * @param cell    String
     * @param varList Hashtable
     * @return Object
     * @throws Exception
     */
    private DataType callSub(ExprCellCall cell, Hashtable varList) throws Exception {
        if (cell == null) {
            return _sub(null, 0);
        } else {
            Vector paraStack = new Vector(); //参数栈
            for (int i = 0; i < cell.para.length; i++) {
                //计算表达式的值
                DataType v = null;
                v = calcExpr(cell.para[i], varList);
                if (v != null) {
                    vPushFront(paraStack, v); //参数入栈
                } else {
                    throw new Exception(STRS_ERR[ERR_PAESEPARA]);
                }
            }

            //查找脚本中的过程
            String subName = cell.subName;
            Object addr = subAddr.get(subName);
            if (addr != null) {
                int ip = (int) ((Int) addr).getVal(); //得到过程行号
                return _sub(paraStack, ip); //过程调用
            } else {
                //查找系统标准过程和用户扩充过程表
                for (int i = 0; i < extSubList.size(); i++) {
                    Lib ext = (Lib) extSubList.elementAt(i);
                    int mID = ext.getMethodID(subName);
                    if (mID >= 0) {

                        //调用外部过程
                        DataType re = ext.call(this, paraStack, mID);

                        return re;
                    }
                }

                //仍没有找到，报错
                throw new Exception(STRS_ERR[ERR_NOSUB] + ":" + subName);

            }

        }
    }

//---------------------------  数组处理  ------------------------------

    /**
     * 解析数组参数 比如 arrName[a+b][35-4],arrName[b[4]]
     *
     * @param arrStr  String
     * @param varList Hashtable
     * @return Stack 反回 a+b , 35-4
     * @throws Exception
     */
    private int[] parseArrayPos(ExprCellArr arrStr, Hashtable varList) throws Exception {
        int len = arrStr.para.length;
        int[] stack = new int[len];
        for (int i = 0; i < len; i++) {
            DataType dt = calcExpr(arrStr.para[i], varList);
            if (dt.type != DataType.DTYPE_INT) { //数组维数只能是数值型
                throw new Exception(STRS_ERR[ERR_TYPE_INVALID]);
            }
            stack[i] = (int) ((Int) dt).getVal();
        }
        return stack;
    }

    /**
     * 创建数组变量或赋值 语句是：arr[6] 或 arr[3+2][2] 或 arr[3][2] 或arr[1][2]=3
     *
     * @param stat    String
     * @param varList Hashtable
     * @throws Exception
     */
    private void _setArr(StatementSetArr stat, Hashtable varList) throws Exception {

        String arrName = stat.dimCell.arrName; //取得数组的名字
        arrName = arrName.toLowerCase();
        int[] dimPara = parseArrayPos(stat.dimCell, varList); //分解参数

        if (stat.expr == null) { //创建

            //创建数组对象，放入变量表
            Array arr = new Array(dimPara);
            varList.put(arrName, arr); //放入变量表中
        } else { //赋值
            DataType arr = (DataType) varList.get(arrName);
            if (arr == null) {
                arr = (DataType) globalVar.get(arrName);
            }
            if (arr != null && arr.type == DataType.DTYPE_ARRAY) {
                ((Array) arr).setValue(dimPara, calcExpr(stat.expr, varList)); //赋值
            }
        }
    }

    /**
     * 取数组的值
     *
     * @param arrExpr String
     * @param varList Hashtable
     * @return Object
     * @throws Exception
     */
    private DataType _getArr(ExprCellArr arrExpr, Hashtable varList) throws Exception {

        String arrName = arrExpr.arrName; //取得数组的名字

        int[] dimPara = parseArrayPos(arrExpr, varList); //分解参数

        DataType arr = (DataType) _getVar(arrName, varList);
        if (arr != null && arr.type == DataType.DTYPE_ARRAY) {
            return ((Array) arr).getValue(dimPara); //取值
        }
        return null;
    }
}
