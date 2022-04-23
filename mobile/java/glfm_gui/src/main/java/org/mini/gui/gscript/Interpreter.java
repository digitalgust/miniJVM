package org.mini.gui.gscript;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
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
 * 20130720 修改了数值类型为长整型<br/>
 * 20220424 添加数据回收系统,使整个运算过程中,尽可能不创建新的对象实例.减小GC压力
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

    static final String[] STRS_RESERVED = {"if", "else", "eif", "while", "loop", "sub", "ret", "", "", ""};
    static private final String STR_SYMBOL = "+-*/><()=, []:&|!\'";
    static private final String STR_NUMERIC = "0123456789";
    //关键字代码
    static final int NOT_KEYWORD = -1, KEYWORD_IF = 0, KEYWORD_ELSE = 1, KEYWORD_ENDIF = 2, KEYWORD_WHILE = 3, KEYWORD_LOOP = 4, KEYWORD_SUB = 5, KEYWORD_RET = 6, KEYWORD_CALL = 7, KEYWORD_SET_VAR = 8, KEYWORD_SET_ARR = 9;
    public static final int ERR_ILLEGAL = 0, ERR_VAR = 1, ERR_TYPE_INVALID = 2, ERR_NOSUB = 3, ERR_NO_VAR = 4, ERR_PARA_CALC = 5, ERR_PAESEPARA = 6, ERR_NO_SRC = 7, ERR_OPSYMB = 8, ERR_ARR_OUT = 9;
    public static final String[] STRS_ERR = {" Illegal statment ,", " Invalid variable name ", " Data type error ", " No such method ", " No such variable ", " Method parameter error ", " Parameter count error ", " Code not load yet ", " Operation symbol error  ", " Array out of bounds  "};
    //源码字符串数组
    //private ArrayList srcCode;
    private Statement[] srcCompiled;
    //变量范围控制
    private boolean isTopCall; //是否是顶级调用
    private LocalVarsMap<String, DataType> globalVar = new LocalVarsMap(); //全局变量
    //脚本中过程首地址  ,sub address in script
    private HashMap<String, Int> subAddr = new HashMap();
    //系统过程及扩充过程列表 ,extend method lib
    private ArrayList<Lib> extSubList = new ArrayList();


    static final int MAX_CACHE_SIZE = 512;
    private List<LocalVarsMap> varsMapCache = new ArrayList();
    private List<ArrayList> listCache = new ArrayList<>();
    private List<Int> intCache = new ArrayList<>();
    private List<Bool> boolCache = new ArrayList<>();
    private List<Str> strCache = new ArrayList<>();

    /**
     * 取得一个缓存的变量表
     *
     * @return
     */
    private LocalVarsMap getCachedTable() {
        if (varsMapCache.isEmpty()) {
            return new LocalVarsMap();
        }
        return varsMapCache.remove(varsMapCache.size() - 1);
    }

    private void putCachedTable(LocalVarsMap v) {
        v.clear();
        varsMapCache.add(v);
    }

    private ArrayList getCachedVector() {
        if (listCache.isEmpty()) {
            return new ArrayList();
        }
        return listCache.remove(listCache.size() - 1);
    }

    private void putCachedVector(ArrayList v) {
        v.clear();
        listCache.add(v);
    }

    /**
     * 减少内存分配,尽可能使用缓存
     *
     * @param v
     * @return
     */
    public Int getCachedInt(long v) {
        if (intCache.isEmpty()) {
            return new Int(v);
        }
        Int i = intCache.remove(intCache.size() - 1);
        i.setVal(v);
        return i;
    }

    public void putCachedInt(DataType v) {
        if (intCache.size() > MAX_CACHE_SIZE || v == null) return;//防内存泄漏
        if (v.isRecyclable() && v.type == DataType.DTYPE_INT) {
            intCache.add((Int) v);
        }
    }

    public Bool getCachedBool(boolean v) {
        if (boolCache.isEmpty()) {
            return new Bool(v);
        }
        Bool b = boolCache.remove(boolCache.size() - 1);
        b.setVal(v);
        return b;
    }

    public void putCachedBool(DataType v) {
        if (boolCache.size() > MAX_CACHE_SIZE || v == null) return;
        if (v.isRecyclable() && v.type == DataType.DTYPE_BOOL) {
            boolCache.add((Bool) v);
        }
    }

    public Str getCachedStr(String v) {
        if (strCache.isEmpty()) {
            return new Str(v);
        }
        Str b = strCache.remove(strCache.size() - 1);
        b.setVal(v);
        return b;
    }

    public void putCachedStr(DataType v) {
        if (strCache.size() > MAX_CACHE_SIZE || v == null) return;
        if (v.isRecyclable() && v.type == DataType.DTYPE_STR) {
            ((Str) v).setVal(null);
            strCache.add((Str) v);
        }
    }

    public void putCachedDataType(DataType v) {
        if (v == null) return;
        if (v.isRecyclable()) {
            if (v.type == DataType.DTYPE_INT) {
                putCachedInt(v);
            } else if (v.type == DataType.DTYPE_BOOL) {
                putCachedBool(v);
            } else if (v.type == DataType.DTYPE_STR) {
                putCachedStr(v);
            }
        }
    }

    /**
     * 构造方法
     */
    public Interpreter() {
    }

    /**
     * 初始化类
     */
    private void init() {//init localvar table cache
        srcCompiled = null;
        //脚本中过程首地址
        subAddr.clear();
        //系统过程及扩充过程列表
        extSubList.clear();
        //初始化全局变量表
        globalVar.clear();
//        //初始化解析
//        stack.clear();
        //加入标准库
        Stdlib stdlib = new Stdlib();
        reglib(stdlib);
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
        ArrayList srcCode = new ArrayList();
        srcCompiled = new Statement[lineCount];
        lineCount = 0;
        for (int i = 0; i < srcBytes.length; i++) {
            if (srcBytes[i] == 0x0a || i + 1 == srcBytes.length) { //行结束,或者文件结束
                try {
                    String s = new String(line.toByteArray());    //j2me使用
                    //String s = new String(line.toByteArray(), "GB2312"); //j2se使用
                    s = s.trim();
                    if (s.length() > 0) {
                        srcCode.add(s);
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
        StringBuilder line = new StringBuilder();
        ArrayList v = new ArrayList();
        for (int i = 0, len = code.length(); i < len; i++) {
            char ch = code.charAt(i);
            if (ch == '"') {
                dquodation++;
            }
            if ((dquodation % 2) == 0 && (ch == ';' || ch == '\n')) {
                String s = line.toString().trim();
                if (s.length() > 0) {
                    v.add(s);
                }
                line.setLength(0);
            } else {
                line.append(ch);
            }
        }
        //fix lost the last line
        String s = line.toString().trim();
        if (s.length() != 0) v.add(s);
        preProcess(v);
    }

    public Lib getLib() {
        return (Lib) extSubList.get(0);
    }

    public void removeLib(Lib lib) {
        extSubList.remove(lib);
    }

    /**
     * 预处理
     */
    private void preProcess(ArrayList srcCode) {
        //预处理
        for (int i = 0; i < srcCode.size(); ) {
            //校验是否有空串
            if (srcCode.get(i) == null) {
                srcCode.remove(i);
                continue;
            }
            //去掉注释
            String el = (String) srcCode.get(i);
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
                srcCode.remove(i);
                continue;
            }
            srcCode.set(i, el);
            i++;
        }
        srcCompiled = new Statement[srcCode.size()];
        for (int i = 0; i < srcCode.size(); i++) {
            try {
                String sc = (String) srcCode.get(i);
                Statement st = Statement.parseInstruct(sc, this);
                srcCompiled[i] = st;
                st.src = sc;
                //System.out.println(i + " " + sc);
            } catch (Exception e) {
                errout(i, STRS_ERR[ERR_ILLEGAL] + srcCode.get(i));
                e.printStackTrace();
                break;
            }

            //找过程起始行号
            if (srcCompiled[i].type == KEYWORD_SUB) {
                StatementSub ss = (StatementSub) srcCompiled[i];
                subAddr.put(ss.cell.subName, new Int(i, false)); //放入过程表中
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
     * @param env HashMap
     * @return Object
     */
    public DataType start(HashMap env) {
        //变量范围控制
        isTopCall = true; //是否是顶级调用
        //开始执行代码

        try {
            if (srcCompiled == null) {
                errout(0, STRS_ERR[ERR_NO_SRC]);
            } else {
                //如果环境变量存在
                if (env != null) {
                    globalVar.putAll(env);
                }
                //执行脚本
                return callSub("main()");
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
    public DataType callSub(String instract) {
        try {
            Statement pstat = Statement.parseInstruct(instract, this);
            if (pstat.type == KEYWORD_CALL) {
                StatementCall pstatCall = (StatementCall) pstat;
                LocalVarsMap ht = getCachedTable();
                DataType o = callSub(pstatCall.cell, ht);
                putCachedTable(ht);
                return o;
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
        if (value == null) return;
        value.setRecyclable(false);
        varName = varName.toLowerCase();
        globalVar.put(varName, value);
    }

    /**
     * 取全局变量
     *
     * @param varName
     * @return
     */
    public Object getGlobalVar(String varName) {
        varName = varName.toLowerCase();
        return globalVar.get(varName);
    }

    /**
     * 注册扩充方法库
     *
     * @param lib Lib
     */
    public void reglib(Lib lib) {
        extSubList.add(lib);
    }

    //--------------------------------------------------------------------------
    //                                  private method
    //--------------------------------------------------------------------------

    /**
     * 过程解析,脚本执行体
     *
     * @param paraStack      LocalVarsMap
     * @param instrucPointer int
     * @return Object
     */
    private DataType exec(ArrayList<DataType> paraStack, int instrucPointer) {

        int ip = instrucPointer; //运行行号
        //构建变量表
        LocalVarsMap<String, DataType> localVar; //本方法的变量表,键是变量名，值是变量值
        //把当前方法的变量表压入stack
        if (isTopCall) {
            localVar = globalVar; //顶级方法的局部变量为全局变量
            isTopCall = false;
        } else {
            localVar = getCachedTable(); //非顶级调用，则是局部方法
        }
        try {
            long calls = System.currentTimeMillis();

            //把过程调用的参数放入localVar
            Statement pstat = srcCompiled[ip];
            if (pstat != null && pstat.type == KEYWORD_SUB) {
                StatementSub psubstat = (StatementSub) pstat;
                for (int i = 0, j = psubstat.cell.para.length; i < j; i++) {
                    DataType pp = (paraStack.isEmpty() ? null : paraStack.get(j - i - 1));
                    if (pp != null) {
                        ExprCellVar var = (ExprCellVar) (psubstat.cell.para[i].cells[0]);
                        pp.setRecyclable(false);
                        (localVar).put(var.varName, pp);
                    } else {
                        errout(ip, STRS_ERR[ERR_PAESEPARA]);
                    }
                }
                ip++; //跳到下一行
            }
            calls = System.currentTimeMillis() - calls;
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
                                Bool wpdt = (Bool) evalExpr(pstatWhile.expr, localVar);
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
                                            if (tmpst.type == KEYWORD_LOOP && countWhile == 0) {
                                                ip = i;
                                                pstatWhile.ip_loop = (short) ip;
                                                break;
                                            }
                                        }
                                    }
                                }
                                putCachedBool(wpdt);
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

                                Bool ib = (Bool) evalExpr(pstatIf.expr, localVar);
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
                                            if (tmpst.type == KEYWORD_ELSE && countIf == 1) {
                                                ip = i;
                                                pstatIf.ip_else = (short) ip;
                                                break;
                                            } else if (tmpst.type == KEYWORD_ENDIF && countIf == 0) {
                                                ip = i;
                                                pstatIf.ip_endif = (short) ip;
                                                break;
                                            }
                                        }
                                    }
                                }
                                putCachedBool(ib);
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
                                        if (tmpst.type == KEYWORD_ENDIF && countIf == 0) {
                                            ip = i;
                                            pstatElse.ip_endif = (short) ip;
                                            break;
                                        }
                                    }
                                }
                                break;
                            case KEYWORD_CALL: {
                                StatementCall pstatCall = (StatementCall) stat;
                                DataType re = callSub(pstatCall.cell, localVar);
                                if (re != null) {
                                    putCachedDataType(re);
                                }
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

                            case KEYWORD_SUB://程序遇到sub关键字,则说明当前方法结束
                                return null;
                            case KEYWORD_RET: //过程结束
                                StatementRet pstatRet = (StatementRet) stat;
                                DataType re = null;
                                if (pstatRet.expr != null) {
                                    re = evalExpr(pstatRet.expr, localVar);
                                }
                                return re;
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


        } catch (Exception e) {
        } finally {
            if (localVar != globalVar) {
                //回收局部变量
                List<String> keylist = localVar.getKeylist();
                for (int i = 0; i < keylist.size(); i++) {
                    String key = keylist.get(i);
                    DataType dt = localVar.get(key);
                    dt.setRecyclable(true);
                    putCachedDataType(dt);
                }
                //回收局部变量表
                putCachedTable(localVar);
            }
        }
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
        StringBuilder tsb = new StringBuilder();
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
     * @return ArrayList
     * @throws Exception
     */
    ArrayList parseInstruct(String s) {
//    	if(!stack.isEmpty()){
//    		return stack;
//    	}
        ArrayList stack = new ArrayList();
//    	if (bolo.CompilerCfg.isProfile) {
//			debug.Profile.instance.begin("parse");
//		}

        int len = s.length();

        StringBuilder sb = new StringBuilder();

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
            stack.add(sb.toString());
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

    static public final <T extends DataType> T vPopFront(ArrayList<DataType> v) {
        if (v.size() <= 0) {
            return null;
        }
        DataType o = v.get(0);
        if (v.size() > 0) {
            v.remove(0);
        }
        return (T) o;
    }

    static public final <T extends DataType> T vPopBack(ArrayList<DataType> v) {
        if (v.size() <= 0) {
            return null;
        }
        DataType o = v.get(v.size() - 1);
        if (v.size() > 0) {
            v.remove(v.size() - 1);
        }
        return (T) o;
    }

    static public final void vPushFront(ArrayList v, DataType o) {
        v.add(0, o);
    }

    static public final void vPushBack(ArrayList v, DataType o) {
        v.add(o);
    }

    /**
     * 在变量列表v中查找变量
     *
     * @param name     String
     * @param localVar Stack
     * @return Object
     */
    private <T extends DataType> T _getVar(String name, LocalVarsMap<String, DataType> localVar) {
        DataType value = localVar.get(name);
        if (value == null) {
            value = globalVar.get(name);

        }
        if (value.type == DataType.DTYPE_BOOL || value.type == DataType.DTYPE_INT) {
            if (value.isRecyclable()) {
                throw new RuntimeException("can not be mutable data ");
            }
        }
        return (T) value;
    }

    /**
     * 赋值操作
     *
     * @param stat     String
     * @param varTable Stack
     * @throws Exception
     */
    private void _setVar(StatementSetVar stat, LocalVarsMap<String, DataType> varTable) throws Exception {
        //格式化表达式
        String varName = stat.varName;
        DataType nValue = evalExpr(stat.expr, varTable);
        nValue.setRecyclable(false);

        if (nValue != null) {
            DataType oldValue = varTable.get(varName);
            if (oldValue != null) {
                if (oldValue.type == nValue.type) {
                    varTable.put(varName, nValue);
                    oldValue.setRecyclable(true);
                    putCachedDataType(oldValue);
                } else {
                    throw new Exception(STRS_ERR[ERR_TYPE_INVALID]);
                }
            } else {
                DataType oldValue1 = globalVar.get(varName);
                if (oldValue1 != null) {
                    if (oldValue1.type == nValue.type) {
                        globalVar.put(varName, nValue);
                        oldValue1.setRecyclable(true);
                        putCachedDataType(oldValue1);
                    } else {
                        throw new Exception(STRS_ERR[ERR_TYPE_INVALID]);
                    }
                } else {
                    varTable.put(varName, nValue);
                }
            }
        }
    }
    //---------------------------表达式求值------------------------------

    //运算类型
    static final int T_NUM = 1 //数值
            , T_STR = 2 //串
            , T_LOG = 4 //布尔值
            , T_ARR = 8 //数组指针，非数组
            , T_LOGSYM = 16 //逻辑符号
            , T_OBJ = 32; //对象

    DataType evalExpr(Expression stat, LocalVarsMap<String, DataType> varTable) throws Exception {

        //ArrayList expr = parseInstruct(exprStr); //分解表达式
        ArrayList<DataType> expr = getCachedVector();
        evaluationCell(stat, varTable, expr); //求变量和过程调用的值

        int cType = 0, cType1 = 0; //默认为算术运算

        for (int i = 0; i < expr.size(); i++) {
            DataType o = expr.get(i);
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
                evalExprNum(expr);
                resultDt = vPopBack(expr);
                break;
            case T_STR:
                evalExprStr(expr);
                resultDt = vPopBack(expr);

                break;
            case T_LOG: //分解逻辑表达式与算术表达式，主要是把算术表达式先于逻辑运算求值
            {
                //下两个变量用于逻辑表达式求值过程
                ArrayList log_dElem = getCachedVector(); //逻辑
                ArrayList ari_dElem = getCachedVector(); //算术

                while (expr.size() > 0) {
                    DataType pdt = vPopFront(expr);

                    ari_dElem.clear();
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
                            evalExprNum(ari_dElem);
                            //放入逻辑表达式中
                            if (ari_dElem.size() > 0) {
                                vPushBack(log_dElem, vPopFront(ari_dElem));
                            }
                        }
                    } //end if是符号

                    //插入表达式元素
                    vPushBack(log_dElem, pdt);

                    //再找比较逻辑运算符后面的元素
                    ari_dElem.clear();
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
                            evalExprNum(ari_dElem);
                            if (ari_dElem.size() > 0) {
                                vPushBack(log_dElem, vPopFront(ari_dElem));
                            }
                        }
                    } //end if是符号
                } //end while

                //逻辑表达式求值
                evalExprLgc(log_dElem);

                resultDt = vPopBack(log_dElem);

                //
                putCachedVector(log_dElem);
                putCachedVector(ari_dElem);
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
        putCachedVector(expr);
        return resultDt;
    }

    /**
     * 求出表达式中变量和过程调用的值
     *
     * @param expr     Stack
     * @param varTable LocalVarsMap
     * @return Stack
     * @throws Exception
     */
    private void evaluationCell(Expression expr, LocalVarsMap<String, DataType> varTable, ArrayList<DataType> tgt) throws Exception {

        for (int i = 0, len = expr.cells.length; i < len; i++) {
            ExprCell cell = expr.cells[i];
            //是串，包括变量，方法，符号,字符串

            switch (cell.type) {
                case ExprCell.EXPR_CELL_DATATYPE: {// 是字符串
                    ExprCellDataType celldt = (ExprCellDataType) cell;
                    tgt.add(celldt.pit);
                    break;
                }
                case ExprCell.EXPR_CELL_VAR: {//是变量
                    ExprCellVar cellv = (ExprCellVar) cell;
                    DataType pdt = _getVar(cellv.varName, varTable);
                    if (pdt == null) {
                        //无变量，需处理
                        throw new Exception(STRS_ERR[ERR_NO_VAR] + cellv.varName);
                    } else {
                        tgt.add(pdt);
                    }
                    break;
                }
                case ExprCell.EXPR_CELL_CALL: {//是过程调用
                    ExprCellCall cellc = (ExprCellCall) cell;
                    DataType pTmp = callSub(cellc, varTable);
                    if (pTmp != null) {
                        tgt.add(pTmp);
                    }
                    break;
                }
                case ExprCell.EXPR_CELL_ARR: {//是数组
                    ExprCellArr cella = (ExprCellArr) cell;
                    tgt.add(_getArr(cella, varTable));
                    break;
                }
            }
        }
    }


    /**
     * 字符串运算,只支持连接操作
     *
     * @param expr Stack
     */
    private void evalExprStr(ArrayList<DataType> expr) {
        StringBuilder sb = new StringBuilder();
        while (expr.size() > 0) { //不停的运算
            DataType ts = vPopFront(expr); //这里有可能是integer型,不能用强制转换
            if (ts.type != DataType.DTYPE_SYMB) { //如果不是符号
                sb.append((ts).getString());
            }
            putCachedDataType(ts);
        }
        vPushFront(expr, getCachedStr(sb.toString()));
    }

    /**
     * 求值运算实现
     *
     * @param expr Stack
     */
    private void evalExprNum(ArrayList expr) {
        do {
            evalExprNumImpl(expr);
        } while (expr.size() > 1);

    }

    private void evalExprNumImpl(ArrayList<DataType> expr) {
        if (expr.size() == 1) { //单独变量
            if ((expr.get(0)).isRecyclable()) return;//如果是不可回收的变量,说明这个变量要么是在变量表中,要么是在statement中
            Int element1 = vPopFront(expr);
            Int val = getCachedInt(element1.getVal());//复制一个可回收的值
            vPushBack(expr, val);
            return;
        } else { //表达式
            //按优先级进行计算,优先级如下：() 取负(正)值  */ + - %
            DataType element1 = vPopFront(expr);
            if (element1.type == DataType.DTYPE_SYMB) {
                if (((Symb) element1).getVal() == Symb.LP) {// (
                    evalExprNumImpl(expr);
                    DataType element2 = vPopFront(expr);
                    DataType element3 = vPopFront(expr);
                    if (element3.type == DataType.DTYPE_SYMB && ((Symb) element3).getVal() == Symb.RP) { // (num)    扔掉反括号
                        vPushFront(expr, element2);
                    } else { // (...             括号中如果仍未计算完成,则继续
                        vPushFront(expr, element3);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        evalExprNumImpl(expr); //再算括号中的内容
                    }
                } else //取正值
                    if (((Symb) element1).getVal() == Symb.ADD) {// +
                        evalExprNumImpl(expr);
                    } else //取负值
                        if (((Symb) element1).getVal() == Symb.SUB) {// -
                            DataType element2 = vPopFront(expr);
                            if (element2.type == DataType.DTYPE_INT) { // -num       立即数
                                Int val = getCachedInt(-((Int) element2).getVal());
                                vPushFront(expr, val);
                                putCachedInt(element2);
                            } else { // -...      表达式
                                vPushFront(expr, element2);
                                evalExprNumImpl(expr);
                                vPushFront(expr, element1);
                                evalExprNumImpl(expr);
                            }
                        }
            } else if (element1.type == DataType.DTYPE_INT) {// num   是数字
                Symb element2 = vPopFront(expr); //应是操作符
                DataType element3 = vPopFront(expr); // 可能是操作数或操作符
                //四则运算
                if ((element2).getVal() == Symb.MUL || (element2).getVal() == Symb.DIV) { // num*/
                    if (element3.type == DataType.DTYPE_INT) {//num */ num
                        long n1 = ((Int) element1).getVal();
                        long n2 = ((Int) element3).getVal();
                        Int val = getCachedInt((element2).getVal() == Symb.MUL ? n1 * n2 : n1 / n2);
                        putCachedInt(element1);
                        putCachedInt(element3);
                        vPushFront(expr, val);
                    } else {  // num */ ...
                        vPushFront(expr, element3);
                        evalExprNumImpl(expr);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        evalExprNumImpl(expr);
                    }
                } else if ((element2).getVal() == Symb.ADD || (element2).getVal() == Symb.SUB) { // num +-

                    boolean calc = false;
                    if (element3.type == DataType.DTYPE_INT) { //   num +- num
                        if (expr.size() == 0) { //    无更多操作符和操作数时计算
                            calc = true;
                        } else { // num +- num ...
                            DataType element4 = vPopFront(expr);
                            if (element4 != null) { // num +- num */
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
                        Int val = getCachedInt((element2).getVal() == Symb.ADD ? n1 + n2 : n1 - n2);
                        putCachedInt(element1);
                        putCachedInt(element3);
                        vPushFront(expr, val);
                    } else {
                        //先算右边的表达式
                        vPushFront(expr, element3); //放回去
                        evalExprNumImpl(expr); //计算

                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        evalExprNumImpl(expr);
                    }
                } else if (element2.getVal() == Symb.RP) { // num)   是右括号
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
    private void evalExprLgc(ArrayList expr) {
        do {
            evalExprLgcImpl(expr);
        } while (expr.size() > 1);

    }

    private void evalExprLgcImpl(ArrayList<DataType> expr) {
        //计算逻辑表达式
        if (expr.size() == 1) { //单独变量
            if ((expr.get(0)).isRecyclable()) return;
            DataType element1 = vPopFront(expr);
            vPushBack(expr, getCachedBool(((Bool) element1).getVal()));
            putCachedBool(element1);
            return;
        } else { //表达式
            //按优先级进行计算,优先级如下：() 取负(正)值  */ + - %
            DataType element1 = vPopFront(expr);
            if (element1.type == DataType.DTYPE_SYMB) { //括号
                if (((Symb) element1).getVal() == Symb.LP) {// (
                    evalExprLgcImpl(expr);
                    DataType element2 = vPopFront(expr);
                    DataType element3 = vPopFront(expr);
                    if (element3.type == DataType.DTYPE_SYMB && ((Symb) element3).getVal() == Symb.RP) { // (dtype)  扔掉反括号
                        vPushFront(expr, element2);
                    } else { //括号中如果仍未计算完成,则继续   //(dtype...
                        vPushFront(expr, element3);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        evalExprLgcImpl(expr); //再算括号中的内容
                    }
                } else if (((Symb) element1).getVal() == Symb.NOT) { //  !  取反
                    DataType element2 = vPopFront(expr);
                    if (element2.type == DataType.DTYPE_BOOL) { // !bool    立即数
                        Bool val = getCachedBool(!((Bool) element2).getVal());
                        vPushFront(expr, val);
                        putCachedBool(element2);
                    } else { // !(  表达式
                        vPushFront(expr, element2);
                        evalExprLgcImpl(expr);
                        vPushFront(expr, element1);
                        evalExprLgcImpl(expr);
                    }
                }
            } else if (element1.type == DataType.DTYPE_INT) { // num ><=
                long n1, n2;

                n1 = ((Int) element1).getVal();

                DataType element2 = vPopFront(expr);
                //应是操作符
                Int element3 = vPopFront(expr); // 操作数或操作符 >= <=

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
                vPushFront(expr, getCachedBool(result));
                putCachedInt(element1);
                putCachedInt(element3);
            } else if (element1.type == DataType.DTYPE_BOOL) { // bool
                Symb element2 = vPopFront(expr); //应是操作符
                DataType element3 = vPopFront(expr); // 操作数或操作符 >= <=
                if (element2.getVal() == Symb.AND) {// bool &
                    if (element3.type == DataType.DTYPE_BOOL) { //bool & bool
                        boolean result = ((Bool) element1).getVal() && ((Bool) element3).getVal();
                        vPushFront(expr, getCachedBool(result));
                        putCachedBool(element1);
                        putCachedBool(element3);
                    } else { // bool & ...
                        vPushFront(expr, element3);
                        evalExprLgcImpl(expr);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        evalExprLgcImpl(expr);
                    }
                } else if (element2.getVal() == Symb.OR) {// bool |
                    boolean calc = false;
                    if (element3.type == DataType.DTYPE_BOOL) { // bool | bool
                        if (expr.size() == 0) {
                            calc = true;
                        } else { //bool | ...
                            DataType element4 = vPopFront(expr); // 操作数或操作符 >= <=
                            if (element4.type == DataType.DTYPE_SYMB) {
                                if (((Symb) element4).getVal() != Symb.AND) {// bool | bool (
                                    calc = true;
                                }
                            }
                            vPushFront(expr, element4);
                        }
                    }
                    if (calc) {
                        boolean result = ((Bool) element1).getVal() || ((Bool) element3).getVal();
                        vPushFront(expr, getCachedBool(result));
                        putCachedBool(element1);
                        putCachedBool(element3);
                    } else {
                        vPushFront(expr, element3);
                        evalExprLgcImpl(expr);
                        vPushFront(expr, element2);
                        vPushFront(expr, element1);
                        evalExprLgcImpl(expr);
                    }

                } else if (element2.getVal() == Symb.RP) { // bool)  是右括号
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
     * @param cell     String
     * @param varTable LocalVarsMap
     * @return Object
     * @throws Exception
     */
    private DataType callSub(ExprCellCall cell, LocalVarsMap varTable) throws Exception {
        if (cell == null) {
            return exec(null, 0);
        } else {
            ArrayList paraStack = getCachedVector(); //参数栈
            for (int i = 0; i < cell.para.length; i++) {
                //计算表达式的值
                DataType v = evalExpr(cell.para[i], varTable);
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
                DataType re = exec(paraStack, ip); //过程调用
                putCachedVector(paraStack);
                return re;
            } else {
                //查找系统标准过程和用户扩充过程表
                for (int i = 0; i < extSubList.size(); i++) {
                    Lib ext = (Lib) extSubList.get(i);
                    int mID = ext.getMethodID(subName);
                    if (mID >= 0) {

                        //调用外部过程
                        DataType re = ext.call(this, paraStack, mID);
                        putCachedVector(paraStack);
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
     * @param arrStr   String
     * @param varTable LocalVarsMap
     * @return Stack 反回 a+b , 35-4
     * @throws Exception
     */
    private int[] parseArrayPos(ExprCellArr arrStr, LocalVarsMap varTable) throws Exception {
        int len = arrStr.para.length;
        int[] stack = arrStr.dimPos.get();
        for (int i = 0; i < len; i++) {
            DataType dt = evalExpr(arrStr.para[i], varTable);
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
     * @param stat     String
     * @param varTable LocalVarsMap
     * @throws Exception
     */
    private void _setArr(StatementSetArr stat, LocalVarsMap<String, DataType> varTable) throws Exception {

        String arrName = stat.dimCell.arrName; //取得数组的名字
        arrName = arrName.toLowerCase();
        int[] dimPara = parseArrayPos(stat.dimCell, varTable); //分解参数

        if (stat.expr == null) { //创建

            //创建数组对象，放入变量表
            Array arr = new Array(dimPara);
            varTable.put(arrName, arr); //放入变量表中
        } else { //赋值
            DataType arr = varTable.get(arrName);
            if (arr == null) {
                arr = globalVar.get(arrName);
            }
            if (arr != null && arr.type == DataType.DTYPE_ARRAY) {
                DataType old = ((Array) arr).setValue(dimPara, evalExpr(stat.expr, varTable)); //赋值
                putCachedDataType(old);
            }
        }
    }

    /**
     * 取数组的值
     *
     * @param arrExpr  String
     * @param varTable LocalVarsMap
     * @return Object
     * @throws Exception
     */
    private DataType _getArr(ExprCellArr arrExpr, LocalVarsMap<String, DataType> varTable) throws Exception {

        String arrName = arrExpr.arrName; //取得数组的名字

        int[] dimPara = parseArrayPos(arrExpr, varTable); //分解参数

        Array arr = _getVar(arrName, varTable);
        if (arr != null && arr.type == DataType.DTYPE_ARRAY) {
            DataType dt = (arr).getValue(dimPara); //取值
            if (dt.type == DataType.DTYPE_BOOL || dt.type == DataType.DTYPE_INT) {
                if (dt.isRecyclable()) {
                    throw new RuntimeException("can not be mutable data in arr");
                }
            }
            return dt;
        }
        return null;
    }
}
