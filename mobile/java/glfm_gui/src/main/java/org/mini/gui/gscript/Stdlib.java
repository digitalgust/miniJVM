package org.mini.gui.gscript;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;

/**
 * 标准方法库 standard method lib <p>Title: </p> <p>Description: </p> <p>Copyright:
 * Copyright (c) 2007</p> <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class Stdlib extends Lib {

    {
        methodNames.put("print".toLowerCase(), 0); // 向控制台输出字符串
        methodNames.put("min".toLowerCase(), 1);// 求最小值
        methodNames.put("max".toLowerCase(), 2); // 求最大值
        methodNames.put("arrlen".toLowerCase(), 3); // 求数组大小
        methodNames.put("abs".toLowerCase(), 4); // 求取对值
        methodNames.put("random".toLowerCase(), 5); // 得到一个随机数
        methodNames.put("mod".toLowerCase(), 6);// 取余
        methodNames.put("println".toLowerCase(), 7); // 输出回车
        methodNames.put("strlen".toLowerCase(), 8); // 字符串长度
        methodNames.put("equals".toLowerCase(), 9); // 字符串比较
        methodNames.put("def".toLowerCase(), 10); // 存入全局变量
        methodNames.put("isdef".toLowerCase(), 11); // 是否存在某全局变量
        methodNames.put("valueof".toLowerCase(), 12); // 转换字符串为数值
        methodNames.put("idxof".toLowerCase(), 13);// 子串在母串的位置        idxof("abc","a")  结果0
        methodNames.put("substr".toLowerCase(), 14); // 截子串        substr("abcde",1,4)      结果"bcd"
        methodNames.put("split".toLowerCase(), 15); // 截子串        substr("abcde",1,4)      结果"bcd"
        methodNames.put("base64enc".toLowerCase(), 16); //   base64编码
        methodNames.put("base64dec".toLowerCase(), 17); //   base64解码
        methodNames.put("isnull".toLowerCase(), 18); //   Obj 类型是否为空
        methodNames.put("getobjfield".toLowerCase(), 19);
        methodNames.put("setobjfield".toLowerCase(), 20);
        methodNames.put("trim".toLowerCase(), 21);//字符串去空格
        methodNames.put("str2int".toLowerCase(), 22);//字符串转int
    }

    Interpreter inp;

    /**
     * @return
     */
    public Map<String, Integer> getMethodNames() {
        return methodNames;
    }

    public DataType call(Interpreter inp, ArrayList para, int methodID) {
        this.inp = inp;
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
            case 15:
                return split(para);
            case 16:
                return base64enc(para);
            case 17:
                return base64dec(para);
            case 18:
                return isnull(para);
            case 19:
                return getObjField(para);
            case 20:
                return setObjField(para);
            case 21:
                return trim(para);
            case 22:
                return str2int(para);
        }
        return null;
    }

    /**
     * 向控制台输出字符串
     *
     * @param para String
     * @return Object
     */
    private DataType print(ArrayList para) {
        DataType dt = Interpreter.vPopBack(para);//不一定是Str类型
        String s = dt.getString();
        System.out.print(s);
        inp.putCachedDataType(dt);
        return null;
    }

    /**
     * 向控制台输出字符串
     *
     * @param para String
     * @return Object
     */
    private DataType println(ArrayList para) {
        DataType dt = (DataType) Interpreter.vPopBack(para);
        String s = dt == null ? null : dt.getString();
        if (s == null) {
            System.out.println();
        } else {
            System.out.println(s);
        }
        inp.putCachedDataType(dt);
        return null;
    }

    /**
     * 求最小值
     *
     * @param para int
     * @return Integer
     */
    private Int min(ArrayList para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        long y = ((Int) Interpreter.vPopBack(para)).getVal();
        return inp.getCachedInt(x > y ? y : x);
    }

    /**
     * 求最大值
     *
     * @param para int
     * @return Integer
     */
    private Int max(ArrayList para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        long y = ((Int) Interpreter.vPopBack(para)).getVal();
        return inp.getCachedInt(x > y ? x : y);
    }

    /**
     * 求数组大小
     *
     * @param para int
     * @return Integer
     */
    private Int arrlen(ArrayList<DataType> para) {
        Array arr = Interpreter.vPopBack(para);
        return inp.getCachedInt(arr.elements.length);
    }

    /**
     * 求最大值
     *
     * @param para int
     * @return Integer
     */
    private Int abs(ArrayList para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        return inp.getCachedInt(Math.abs(x));
    }

    //随机数基石
    private static Random random = new Random(); //定义一个随机值

    /**
     * 产生一个随机数，这个数一定是正数
     *
     * @return int 返回一个正数
     */
    public DataType random() {
        return inp.getCachedInt(random.nextInt());
    }

    /**
     * 取余
     *
     * @param para int
     * @return gscript.Int
     */
    private Int mod(ArrayList para) {
        long x = ((Int) Interpreter.vPopBack(para)).getVal();
        long y = ((Int) Interpreter.vPopBack(para)).getVal();
        return inp.getCachedInt(x % y);
    }

    /**
     * 字符串长度
     *
     * @param para int
     * @return Integer
     */
    private Int strlen(ArrayList para) {
        String s = Interpreter.vPopBack(para).toString();
        return inp.getCachedInt(s.length());
    }

    /**
     * 比较字符串
     *
     * @param para int
     * @return gscript.Int
     */
    private Bool equals(ArrayList para) {
        String x = ((Str) Interpreter.vPopBack(para)).getVal();
        String y = ((Str) Interpreter.vPopBack(para)).getVal();
        return inp.getCachedBool(x.equals(y));
    }

    /**
     * 保存全局变量
     *
     * @param para int
     * @return gscript.Int
     */
    private DataType def(Interpreter inp, ArrayList para) {
        String name = ((Str) Interpreter.vPopBack(para)).getVal();
        DataType dt = (DataType) Interpreter.vPopBack(para);
        inp.putGlobalVar(name.toLowerCase(), dt);
        return null;
    }

    /**
     * 某名称的全局变量是否存在
     *
     * @param para int
     * @return gscript.Int
     */
    private Bool isDef(Interpreter inp, ArrayList para) {
        String name = ((Str) Interpreter.vPopBack(para)).getVal();
        if (inp.getGlobalVar(name.toLowerCase()) == null) {
            return inp.getCachedBool(false);
        }
        return inp.getCachedBool(true);
    }

    /**
     * 某名称的全局变量是否存在
     *
     * @param para int
     * @return gscript.Int
     */
    private Int valueOf(ArrayList para) {
        String s = ((Str) Interpreter.vPopBack(para)).getVal();
        if (s != null && !"".equals(s)) {
            return new Int(Integer.parseInt(s));
        }
        return inp.getCachedInt(0);
    }

    /**
     * 子串在母串中的位置
     *
     * @param para
     * @return
     */
    private Int idxof(ArrayList para) {
        String m = ((Str) Interpreter.vPopBack(para)).getVal();
        String sub = ((Str) Interpreter.vPopBack(para)).getVal();
        if (m != null && sub != null) {
            return inp.getCachedInt(m.indexOf(sub));
        }
        return inp.getCachedInt(-1);
    }

    /**
     * 取子串
     *
     * @param para
     * @return
     */
    private Str substr(ArrayList para) {
        String s = ((Str) Interpreter.vPopBack(para)).getVal();
        int a = (int) ((Int) Interpreter.vPopBack(para)).getVal();
        int b = (int) ((Int) Interpreter.vPopBack(para)).getVal();
        StringBuffer sb = new StringBuffer();
        for (int i = a; i < b; i++) {
            if (i < s.length()) {
                sb.append(s.charAt(i));
            }
        }
        return inp.getCachedStr(sb.toString());
    }


    /**
     * 拆字符串
     *
     * @param para
     * @return
     */
    private Array split(ArrayList para) {
        String s = ((Str) Interpreter.vPopBack(para)).getVal();
        String splitor = ((Str) Interpreter.vPopBack(para)).getVal();
        String[] ss = s.split(splitor);
        int[] dim = new int[]{ss.length};
        Array arr = new Array(dim);
        for (int i = 0; i < ss.length; i++) {
            dim[0] = i;
            arr.setValue(dim, inp.getCachedStr(ss[i]));
        }
        return arr;
    }


    private DataType base64enc(ArrayList para) {
        try {
            String str = ((Str) (Interpreter.vPopBack(para))).getVal();
            byte[] b = str.getBytes("utf-8");
            String s = javax.cldc.io.Base64.encode(b, 0, b.length);
            return inp.getCachedStr(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType base64dec(ArrayList para) {
        try {
            String str = ((Str) (Interpreter.vPopBack(para))).getVal();
            byte[] b = javax.cldc.io.Base64.decode(str);
            String s = new String(b, "utf-8");
            return inp.getCachedStr(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType isnull(ArrayList para) {
        DataType d = Interpreter.vPopBack(para);
        if (d instanceof Obj) {
            if (((Obj) d).isNull()) {
                return inp.getCachedBool(true);
            } else {
                return inp.getCachedBool(false);
            }
        }
        return inp.getCachedBool(true);
    }


    private DataType getObjField(ArrayList para) {
        try {
            Obj obj = (Obj) Interpreter.vPopBack(para);
            String fieldName = ((Str) (Interpreter.vPopBack(para))).getVal();

            Class c = obj.getVal().getClass();
            Field field = null;
            while (field == null) {
                try {
                    field = c.getField(fieldName);
                } catch (Exception e) {
                }
                c = c.getSuperclass();
                if (c == null) {
                    break;
                }
            }
            if (field != null) {
                Object val = field.get(obj.getVal());
                return inp.getCachedStr(val.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType setObjField(ArrayList para) {
        try {
            Obj obj = (Obj) Interpreter.vPopBack(para);
            String fieldName = ((Str) (Interpreter.vPopBack(para))).getVal();
            DataType val = Interpreter.vPopBack(para);

            Class c = obj.getVal().getClass();
            Field field = null;
            while (field == null) {
                try {
                    field = c.getField(fieldName);
                } catch (Exception e) {
                }
                c = c.getSuperclass();
                if (c == null) {
                    break;
                }
            }
            if (field != null) {
                switch (val.type) {
                    case DataType.DTYPE_INT:
                        field.set(obj.getVal(), ((Int) val).getVal());
                        break;
                    case DataType.DTYPE_STR:
                        field.set(obj.getVal(), ((Str) val).getVal());
                        break;
                    case DataType.DTYPE_BOOL:
                        field.set(obj.getVal(), ((Bool) val).getVal());
                        break;
                    default:
                        System.out.println("not support in setObjField " + fieldName);
                        break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType trim(ArrayList para) {
        try {
            String str = ((Str) (Interpreter.vPopBack(para))).getVal();
            return inp.getCachedStr(str.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType str2int(ArrayList para) {
        try {
            String str = ((Str) (Interpreter.vPopBack(para))).getVal();
            str = str.trim();
            int i = Integer.parseInt(str);
            return inp.getCachedInt(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
