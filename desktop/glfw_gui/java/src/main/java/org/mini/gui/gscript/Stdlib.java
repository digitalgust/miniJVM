package org.mini.gui.gscript;

import org.mini.reflect.ReflectMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        methodNames.put("invokeJava".toLowerCase(), 23);//执行对象方法
        methodNames.put("invokeStatic".toLowerCase(), 24);//执行对象方法
        methodNames.put("getbit".toLowerCase(), 25);//取整数第n位,返回bool
        methodNames.put("setbit".toLowerCase(), 26);//设整数第n位
    }


    /**
     * @return
     */
    public Map<String, Integer> getMethodNames() {
        return methodNames;
    }

    public DataType call(Interpreter inp, ArrayList<DataType> para, int methodID) {
        switch (methodID) {
            case 0:
                return print(inp, para);
            case 1:
                return min(inp, para);
            case 2:
                return max(inp, para);
            case 3:
                return arrlen(inp, para);
            case 4:
                return abs(inp, para);
            case 5:
                return random(inp);
            case 6:
                return mod(inp, para);
            case 7:
                return println(inp, para);
            case 8:
                return strlen(inp, para);
            case 9:
                return equals(inp, para);
            case 10:
                return def(inp, para);
            case 11:
                return isDef(inp, para);
            case 12:
                return valueOf(inp, para);
            case 13:
                return idxof(inp, para);
            case 14:
                return substr(inp, para);
            case 15:
                return split(inp, para);
            case 16:
                return base64enc(inp, para);
            case 17:
                return base64dec(inp, para);
            case 18:
                return isnull(inp, para);
            case 19:
                return getObjField(inp, para);
            case 20:
                return setObjField(inp, para);
            case 21:
                return trim(inp, para);
            case 22:
                return str2int(inp, para);
            case 23:
                return invokeJava(inp, para);
            case 24:
                return invokeStatic(inp, para);
            case 25:
                return getbit(inp, para);
            case 26:
                return setbit(inp, para);
        }
        return null;
    }

    /**
     * 向控制台输出字符串
     *
     * @param para String
     * @return Object
     */
    private DataType print(Interpreter inp, ArrayList<DataType> para) {
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
    private DataType println(Interpreter inp, ArrayList<DataType> para) {
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
    private Int min(Interpreter inp, ArrayList<DataType> para) {
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
    private Int max(Interpreter inp, ArrayList<DataType> para) {
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
    private Int arrlen(Interpreter inp, ArrayList<DataType> para) {
        Array arr = Interpreter.vPopBack(para);
        return inp.getCachedInt(arr.elements.length);
    }

    /**
     * 求最大值
     *
     * @param para int
     * @return Integer
     */
    private Int abs(Interpreter inp, ArrayList<DataType> para) {
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
    public DataType random(Interpreter inp) {
        return inp.getCachedInt(random.nextInt());
    }

    /**
     * 取余
     *
     * @param para int
     * @return gscript.Int
     */
    private Int mod(Interpreter inp, ArrayList<DataType> para) {
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
    private Int strlen(Interpreter inp, ArrayList<DataType> para) {
        String s = Interpreter.vPopBack(para).toString();
        return inp.getCachedInt(s.length());
    }

    /**
     * 比较字符串
     *
     * @param para int
     * @return gscript.Int
     */
    private Bool equals(Interpreter inp, ArrayList<DataType> para) {
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
    private DataType def(Interpreter inp, ArrayList<DataType> para) {
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
    private Bool isDef(Interpreter inp, ArrayList<DataType> para) {
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
    private Int valueOf(Interpreter inp, ArrayList<DataType> para) {
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
    private Int idxof(Interpreter inp, ArrayList<DataType> para) {
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
    private Str substr(Interpreter inp, ArrayList<DataType> para) {
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
    private Array split(Interpreter inp, ArrayList<DataType> para) {
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


    private DataType base64enc(Interpreter inp, ArrayList<DataType> para) {
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

    private DataType base64dec(Interpreter inp, ArrayList<DataType> para) {
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


    private DataType isnull(Interpreter inp, ArrayList<DataType> para) {
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


    private DataType getObjField(Interpreter inp, ArrayList<DataType> para) {
        try {
            Obj ins = (Obj) Interpreter.vPopBack(para);
            String fieldName = ((Str) (Interpreter.vPopBack(para))).getVal();

            Class c = ins.getVal().getClass();
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
                Class fc = field.getType();
                if (fc == String.class) {
                    String val = (String) field.get(ins.getVal());
                    return inp.getCachedStr(val);
                } else if (fc == int.class) {
                    int val = field.getInt(ins.getVal());
                    return inp.getCachedInt(val);
                } else if (fc == long.class) {
                    long val = field.getLong(ins.getVal());
                    return inp.getCachedInt(val);
                } else if (fc == byte.class) {
                    byte val = field.getByte(ins.getVal());
                    return inp.getCachedInt(val);
                } else if (fc == short.class) {
                    short val = field.getShort(ins.getVal());
                    return inp.getCachedInt(val);
                } else if (fc == char.class) {
                    char val = field.getChar(ins.getVal());
                    return inp.getCachedInt(val);
                } else if (fc == boolean.class) {
                    boolean val = field.getBoolean(ins.getVal());
                    return inp.getCachedBool(val);
                } else {//include float double and others
                    Object val = field.get(ins.getVal());
                    return new Obj(val);
                }
            }
            throw new RuntimeException("error can not found field:" + fieldName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType setObjField(Interpreter inp, ArrayList<DataType> para) {
        try {
            Obj ins = (Obj) Interpreter.vPopBack(para);
            String fieldName = ((Str) (Interpreter.vPopBack(para))).getVal();
            DataType val = Interpreter.vPopBack(para);

            Class c = ins.getVal().getClass();
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
                Class fc = field.getType();
                if (fc == String.class) {
                    field.set(ins.getVal(), ((Str) val).getVal());
                } else if (fc == int.class) {
                    field.setInt(ins.getVal(), ((Int) val).getValAsInt());
                } else if (fc == long.class) {
                    field.setLong(ins.getVal(), ((Int) val).getVal());
                } else if (fc == byte.class) {
                    field.setByte(ins.getVal(), (byte) ((Int) val).getVal());
                } else if (fc == short.class) {
                    field.setShort(ins.getVal(), (short) ((Int) val).getVal());
                } else if (fc == char.class) {
                    field.setChar(ins.getVal(), (char) ((Int) val).getVal());
                } else if (fc == boolean.class) {
                    field.setBoolean(ins.getVal(), ((Bool) val).getVal());
                } else if (fc == float.class) {
                    if (val.type == DataType.DTYPE_OBJ) {
                        Obj objv = (Obj) val;
                        if (objv.getVal() instanceof Float) {
                            field.setFloat(ins.getVal(), ((Float) objv.getVal()).floatValue());
                        }
                    }
                } else if (fc == double.class) {
                    if (val.type == DataType.DTYPE_OBJ) {
                        Obj objv = (Obj) val;
                        if (objv.getVal() instanceof Double) {
                            field.setDouble(ins.getVal(), ((Float) objv.getVal()).doubleValue());
                        }
                    }
                } else {
                    if (val.type == DataType.DTYPE_OBJ) {
                        Obj objv = (Obj) val;
                        if (objv.getVal().getClass() == (fc)) {
                            field.set(ins.getVal(), ((Float) objv.getVal()).doubleValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType tranlateValue(Interpreter inp, Class fc, Object value) {
        if (fc == String.class) {
            return inp.getCachedStr((String) value);
        } else if (fc == int.class) {
            return inp.getCachedInt(((Integer) value).intValue());
        } else if (fc == long.class) {
            return inp.getCachedInt(((Long) value).longValue());
        } else if (fc == byte.class) {
            return inp.getCachedInt(((Byte) value).byteValue());
        } else if (fc == short.class) {
            return inp.getCachedInt(((Short) value).shortValue());
        } else if (fc == char.class) {
            return inp.getCachedInt(((Character) value).charValue());
        } else if (fc == boolean.class) {
            return inp.getCachedBool(((Boolean) value).booleanValue());
        } else {//include float double and others
            return new Obj(value);
        }
    }

    private DataType invokeImpl(Interpreter inp, ArrayList<DataType> para, Class c, String name, Class[] types, Object instance) {
        try {

            Object[] javaPara = new Object[para.size()];
            for (int i = 0; i < para.size(); i++) {
                DataType dt = Interpreter.vPopBack(para);
                if (dt.type == DataType.DTYPE_INT) {
                    long dtv = ((Int) dt).getVal();
                    if (types[i] == int.class) {
                        javaPara[i] = (int) dtv;
                    } else if (types[i] == long.class) {
                        javaPara[i] = dtv;
                    } else if (types[i] == short.class) {
                        javaPara[i] = (short) dtv;
                    } else if (types[i] == char.class) {
                        javaPara[i] = (char) dtv;
                    } else if (types[i] == byte.class) {
                        javaPara[i] = (byte) dtv;
                    }
                } else if (dt.type == DataType.DTYPE_STR) {
                    javaPara[i] = ((Str) dt).getVal();
                } else if (dt.type == DataType.DTYPE_BOOL) {
                    javaPara[i] = ((Bool) dt).getVal();
                } else if (dt.type == DataType.DTYPE_OBJ) {
                    javaPara[i] = ((Obj) dt).getVal();
                }
            }

            Method m = c.getMethod(name, types);
            if (m != null) {
                Object ret = m.invoke(instance, javaPara);

                Class retType = m.getReturnType();
                if (retType != Void.TYPE) {
                    if (retType == String.class) {
                        return inp.getCachedStr((String) ret);
                    } else if (retType == int.class) {
                        return inp.getCachedInt(((Integer) ret).intValue());
                    } else if (retType == long.class) {
                        return inp.getCachedInt(((Long) ret).longValue());
                    } else if (retType == byte.class) {
                        return inp.getCachedInt(((Byte) ret).byteValue());
                    } else if (retType == short.class) {
                        return inp.getCachedInt(((Short) ret).shortValue());
                    } else if (retType == char.class) {
                        return inp.getCachedInt(((Character) ret).charValue());
                    } else if (retType == boolean.class) {
                        return inp.getCachedBool(((Boolean) ret).booleanValue());
                    } else {//include float double and others
                        return new Obj(ret);
                    }
                }
            } else {
                throw new RuntimeException("Method not found:" + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType invokeJava(Interpreter inp, ArrayList<DataType> para) {
        Object ins = ((Obj) Interpreter.vPopBack(para)).getVal();
        String javaFunc = ((Str) (Interpreter.vPopBack(para))).getVal();
        Class c = ins.getClass();
        String name = javaFunc.substring(0, javaFunc.indexOf('('));
        String desc = javaFunc.substring(javaFunc.indexOf('('));
        Class[] types = ReflectMethod.getMethodPara(c.getClassLoader(), desc);

        return invokeImpl(inp, para, c, name, types, ins);
    }

    private DataType invokeStatic(Interpreter inp, ArrayList<DataType> para) {
        String className = ((Str) Interpreter.vPopBack(para)).getVal();
        String javaFunc = ((Str) (Interpreter.vPopBack(para))).getVal();
        Class c = null;
        try {
            c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not found:" + className);
        }
        String name = javaFunc.substring(0, javaFunc.indexOf('('));
        String desc = javaFunc.substring(javaFunc.indexOf('('));
        Class[] types = ReflectMethod.getMethodPara(c.getClassLoader(), desc);

        return invokeImpl(inp, para, c, name, types, null);
    }


    private DataType trim(Interpreter inp, ArrayList<DataType> para) {
        try {
            String str = ((Str) (Interpreter.vPopBack(para))).getVal();
            return inp.getCachedStr(str.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType str2int(Interpreter inp, ArrayList<DataType> para) {
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

    private DataType getbit(Interpreter inp, ArrayList<DataType> para) {
        try {
            Int si = Interpreter.vPopBack(para);
            Int sbit = Interpreter.vPopBack(para);
            long i = si.getVal();
            int bitPos = sbit.getValAsInt();
            inp.putCachedInt(si);
            inp.putCachedInt(sbit);
            return inp.getCachedBool(((i >> bitPos) & 1) == 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType setbit(Interpreter inp, ArrayList<DataType> para) {
        try {
            Int si = Interpreter.vPopBack(para);
            Int sbit = Interpreter.vPopBack(para);
            Bool set = Interpreter.vPopBack(para);
            long i = si.getVal();
            int bitPos = sbit.getValAsInt();
            inp.putCachedInt(sbit);
            inp.putCachedBool(set);

            i &= ~(1 << bitPos);
            if (set.getVal()) {
                i |= 1 << bitPos;
            }
            si.setVal(i);
            return si;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
