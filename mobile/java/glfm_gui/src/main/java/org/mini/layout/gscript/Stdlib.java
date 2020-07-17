package org.mini.layout.gscript;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

/**
 * 标准方法库 standard method lib <p>Title: </p> <p>Description: </p> <p>Copyright:
 * Copyright (c) 2007</p> <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class Stdlib
        extends Lib {

    {
        methodNames.put("print", 0); // 向控制台输出字符串
        methodNames.put("min", 1);// 求最小值
        methodNames.put("max", 2); // 求最大值
        methodNames.put("arrlen", 3); // 求数组大小
        methodNames.put("abs", 4); // 求取对值
        methodNames.put("random", 5); // 得到一个随机数
        methodNames.put("mod", 6);// 取余
        methodNames.put("println", 7); // 输出回车
        methodNames.put("strlen", 8); // 字符串长度
        methodNames.put("equals", 9); // 字符串比较
        methodNames.put("def", 10); // 存入全局变量
        methodNames.put("isdef", 11); // 是否存在某全局变量
        methodNames.put("valueof", 12); // 转换字符串为数值
        methodNames.put("idxof", 13);// 子串在母串的位置        idxof("abc","a")  结果0
        methodNames.put("substr", 14); // 截子串        substr("abcde",1,4)      结果"bcd"
    }

    ;

    /**
     * @return
     */
    public Map<String, Integer> getMethodNames() {
        return methodNames;
    }

    public DataType call(Interpreter inp, Vector para, int methodID) {
        switch (methodID) {
            case 0:
                return print(para);
            case 1:
                return min(para);
            case 2:
                return max(para);
            case 3:
                return arrlen(para);
            case 4:
                return abs(para);
            case 5:
                return random();
            case 6:
                return mod(para);
            case 7:
                return println(para);
            case 8:
                return strlen(para);
            case 9:
                return equals(para);
            case 10:
                return def(inp, para);
            case 11:
                return isDef(inp, para);
            case 12:
                return valueOf(para);
            case 13:
                return idxof(para);
            case 14:
                return substr(para);
        }
        return null;
    }

    /**
     * 向控制台输出字符串
     *
     * @param para String
     * @return Object
     */
    private DataType print(Vector para) {
        String s = ((DataType) (Interpreter.vPopBack(para))).getString();
        System.out.print(s);
        return null;
    }

    /**
     * 向控制台输出字符串
     *
     * @param para String
     * @return Object
     */
    private DataType println(Vector para) {
        DataType dt = (DataType) Interpreter.vPopBack(para);
        String s = dt == null ? null : dt.getString();
        if (s == null) {
            System.out.println();
        } else {
            System.out.println(s);
        }
        return null;
    }

    /**
     * 求最小值
     *
     * @param para int
     * @return Integer
     */
    private Int min(Vector para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        long y = ((Int) Interpreter.vPopBack(para)).getVal();
        return new Int(x > y ? y : x);
    }

    /**
     * 求最大值
     *
     * @param para int
     * @return Integer
     */
    private Int max(Vector para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        long y = ((Int) Interpreter.vPopBack(para)).getVal();
        return new Int(x > y ? x : y);
    }

    /**
     * 求数组大小
     *
     * @param para int
     * @return Integer
     */
    private Int arrlen(Vector para) {
        Array arr = (Array) Interpreter.vPopBack(para);
        return new Int(arr.elements.length);
    }

    /**
     * 求最大值
     *
     * @param para int
     * @return Integer
     */
    private Int abs(Vector para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        return new Int(Math.abs(x));
    }

    //随机数基石
    private static Random random = new Random(); //定义一个随机值

    /**
     * 产生一个随机数，这个数一定是正数
     *
     * @return int 返回一个正数
     */
    public static DataType random() {
        return new Int(random.nextInt());
    }

    /**
     * 取余
     *
     * @param para int
     * @return gscript.Int
     */
    private Int mod(Vector para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        long y = ((Int) Interpreter.vPopBack(para)).getVal();
        return (new Int(x % y));
    }

    /**
     * 字符串长度
     *
     * @param para int
     * @return Integer
     */
    private Int strlen(Vector para) {
        String s = Interpreter.vPopBack(para).toString();
        return new Int(s.length());
    }

    /**
     * 比较字符串
     *
     * @param para int
     * @return gscript.Int
     */
    private Bool equals(Vector para) {
        String x = ((Str) Interpreter.vPopBack(para)).getVal();
        String y = ((Str) Interpreter.vPopBack(para)).getVal();
        return new Bool(x.equals(y));
    }

    /**
     * 保存全局变量
     *
     * @param para int
     * @return gscript.Int
     */
    private DataType def(Interpreter inp, Vector para) {
        String name = ((Str) Interpreter.vPopBack(para)).getVal();
        DataType dt = (DataType) Interpreter.vPopBack(para);
        inp.putGlobalVar(name, dt);
        return null;
    }

    /**
     * 某名称的全局变量是否存在
     *
     * @param para int
     * @return gscript.Int
     */
    private Bool isDef(Interpreter inp, Vector para) {
        String name = ((Str) Interpreter.vPopBack(para)).getVal();
        if (inp.getGlobalVar(name) == null) {
            return new Bool(false);
        }
        return new Bool(true);
    }

    /**
     * 某名称的全局变量是否存在
     *
     * @param para int
     * @return gscript.Int
     */
    private Int valueOf(Vector para) {
        String s = ((Str) Interpreter.vPopBack(para)).getVal();
        if (s != null && !"".equals(s)) {
            return new Int(Integer.parseInt(s));
        }
        return new Int(0);
    }

    /**
     * 子串在母串中的位置
     *
     * @param para
     * @return
     */
    private Int idxof(Vector para) {
        String m = ((Str) Interpreter.vPopBack(para)).getVal();
        String sub = ((Str) Interpreter.vPopBack(para)).getVal();
        if (m != null && sub != null) {
            return new Int(m.indexOf(sub));
        }
        return new Int(-1);
    }

    /**
     * 取子串
     *
     * @param para
     * @return
     */
    private Str substr(Vector para) {
        String s = ((Str) Interpreter.vPopBack(para)).getVal();
        int a = (int) ((Int) Interpreter.vPopBack(para)).getVal();
        int b = (int) ((Int) Interpreter.vPopBack(para)).getVal();
        StringBuffer sb = new StringBuffer();
        for (int i = a; i < b; i++) {
            if (i < s.length()) {
                sb.append(s.charAt(i));
            }
        }
        return new Str(sb.toString());
    }
}
