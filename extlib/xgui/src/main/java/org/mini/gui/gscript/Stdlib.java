package org.mini.gui.gscript;

import org.mini.apploader.AppManager;
import org.mini.crypt.XorCrypt;
import org.mini.glfm.Glfm;
import org.mini.glwrap.GLUtil;
import org.mini.gui.callback.GCallBack;
import org.mini.json.JsonParser;
import org.mini.json.JsonPrinter;
import org.mini.layout.guilib.GuiScriptLib;
import org.mini.reflect.ReflectMethod;
import org.mini.util.IntList;
import org.mini.util.SysLog;

import javax.microedition.io.Base64;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
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

    //随机数基石
    private static Random random = new Random(); //定义一个随机值
    Interpreter inp;


    public Stdlib(Interpreter inp) {
        this.inp = inp;

        methodNames.put("getEnv".toLowerCase(), this::getEnv);//
        methodNames.put("setEnv".toLowerCase(), this::setEnv);//
        methodNames.put("def".toLowerCase(), this::def); // 存入全局变量
        methodNames.put("isDef".toLowerCase(), this::isDef); // 是否存在某全局变量
        methodNames.put("isBool".toLowerCase(), this::isBool); //
        methodNames.put("isInt".toLowerCase(), this::isInt); //
        methodNames.put("isStr".toLowerCase(), this::isStr); //
        methodNames.put("isObj".toLowerCase(), this::isObj); //
        methodNames.put("isArray".toLowerCase(), this::isArray); //
        methodNames.put("print".toLowerCase(), this::print); // 向控制台输出字符串
        methodNames.put("println".toLowerCase(), this::println); // 输出回车
        methodNames.put("min".toLowerCase(), this::min);// 求最小值
        methodNames.put("max".toLowerCase(), this::max); // 求最大值
        methodNames.put("arrlen".toLowerCase(), this::arrlen); // 求数组大小
        methodNames.put("abs".toLowerCase(), this::abs); // 求取对值
        methodNames.put("random".toLowerCase(), this::random); // 得到一个随机数
        methodNames.put("mod".toLowerCase(), this::mod);// 取余
        methodNames.put("strlen".toLowerCase(), this::strlen); // 字符串长度
        methodNames.put("equals".toLowerCase(), this::equals); // 字符串比较
        methodNames.put("valueOf".toLowerCase(), this::valueOf); // 转换字符串为数值
        methodNames.put("intOf".toLowerCase(), this::valueOf); // 转换字符串为数值
        methodNames.put("idxOf".toLowerCase(), this::idxof);// 子串在母串的位置        idxof("abc","a")  结果0
        methodNames.put("indexOf".toLowerCase(), this::idxof);// 子串在母串的位置        idxof("abc","a")  结果0
        methodNames.put("lastIdxOf".toLowerCase(), this::lastIdxOf);// 子串在母串的位置        idxof("abc","a")  结果0
        methodNames.put("lastIndexOf".toLowerCase(), this::lastIdxOf);// 子串在母串的位置        idxof("abc","a")  结果0
        methodNames.put("substr".toLowerCase(), this::substr); // 截子串        substr("abcde",1,4)      结果"bcd"
        methodNames.put("replace".toLowerCase(), this::replace); // 截子串        substr("abcde",1,4)      结果"bcd"
        methodNames.put("split".toLowerCase(), this::split); // 截子串        split("abc;de",";")      结果"abc","de"
        methodNames.put("lowCase".toLowerCase(), this::lowCase); // 小写字符串
        methodNames.put("upCase".toLowerCase(), this::upCase); //     大写字符串
        methodNames.put("startsWith".toLowerCase(), this::startsWith); //
        methodNames.put("endsWith".toLowerCase(), this::endsWith); //
        methodNames.put("trim".toLowerCase(), this::trim);//字符串去空格
        methodNames.put("str2int".toLowerCase(), this::str2int);//字符串转int
        methodNames.put("isNumStr".toLowerCase(), this::isNumStr);//是数字串
        methodNames.put("invokeJava".toLowerCase(), this::invokeJava);//执行对象方法
        methodNames.put("invokeStatic".toLowerCase(), this::invokeStatic);//执行对象方法
        methodNames.put("bitGet".toLowerCase(), this::bitGet);//取整数第n位,返回bool
        methodNames.put("bitSet".toLowerCase(), this::bitSet);//设整数第n位
        methodNames.put("bitAnd".toLowerCase(), this::bitAnd);//设整数第n位
        methodNames.put("bitOr".toLowerCase(), this::bitOr);//设整数第n位
        methodNames.put("bitNot".toLowerCase(), this::bitNot);//设整数第n位
        methodNames.put("encrypt".toLowerCase(), this::encrypt);//加密  str= encrypt(str,key)
        methodNames.put("decrypt".toLowerCase(), this::decrypt);//解密  str= decrypt(str,key)
        methodNames.put("md5".toLowerCase(), this::md5);//md5  str= md5(str) 返回32位字符串(16字节串)
        methodNames.put("sha1".toLowerCase(), this::sha1);//sha1  str= sha1(str) 返回40位字符串(20字节串)
        methodNames.put("remoteMethodCall".toLowerCase(), this::remoteMethodCall);//远程调用
        methodNames.put("buyAppleProductById".toLowerCase(), this::buyAppleProductById);//远程调用
        methodNames.put("openOtherApp".toLowerCase(), this::openOtherApp);//打开其他应用
        methodNames.put("jsonParse".toLowerCase(), this::jsonParse);//把字符串解析为json对象
        methodNames.put("jsonGet".toLowerCase(), this::jsonGet);// 获取json对象某属性的值
        methodNames.put("json2Str".toLowerCase(), this::json2Str);// 把json对象转成字符串
        methodNames.put("jsonSet".toLowerCase(), this::jsonSet);// 设置json对象某属性的值
        methodNames.put("base64enc".toLowerCase(), this::base64enc); //   base64编码
        methodNames.put("base64dec".toLowerCase(), this::base64dec); //   base64解码
        methodNames.put("urlenc".toLowerCase(), this::urlenc); //   UrlEncode解码
        methodNames.put("urldec".toLowerCase(), this::urldec); //   UrlDecode解码
        methodNames.put("isnull".toLowerCase(), this::isnull); //   Obj 类型是否为空
        methodNames.put("getobjfield".toLowerCase(), this::getObjField);
        methodNames.put("setobjfield".toLowerCase(), this::setObjField);
        methodNames.put("run".toLowerCase(), this::run);   // run("sub f();println(5);ret;", "f()")
    }


    public DataType getEnv(ArrayList<DataType> para) {
        String key = Interpreter.popBackStr(para);
        return Interpreter.getCachedStr(inp.getEnvVar(key));
    }


    public DataType setEnv(ArrayList<DataType> para) {
        String key = Interpreter.popBackStr(para);
        DataType val = Interpreter.popBack(para);
        inp.setEnvVar(key, val.toString());
        return null;
    }

    public DataType isBool(ArrayList<DataType> para) {
        DataType dt = Interpreter.popBack(para);//
        return Interpreter.getCachedBool(dt.type == DataType.DTYPE_BOOL);
    }

    public DataType isInt(ArrayList<DataType> para) {
        DataType dt = Interpreter.popBack(para);//
        return Interpreter.getCachedBool(dt.type == DataType.DTYPE_INT);
    }

    public DataType isStr(ArrayList<DataType> para) {
        DataType dt = Interpreter.popBack(para);//
        return Interpreter.getCachedBool(dt.type == DataType.DTYPE_STR);
    }

    public DataType isArray(ArrayList<DataType> para) {
        DataType dt = Interpreter.popBack(para);//
        return Interpreter.getCachedBool(dt.type == DataType.DTYPE_ARRAY);
    }

    public DataType isObj(ArrayList<DataType> para) {
        DataType dt = Interpreter.popBack(para);//
        return Interpreter.getCachedBool(dt.type == DataType.DTYPE_OBJ);
    }

    /**
     * 向控制台输出字符串
     *
     * @param para String
     * @return Object
     */
    public DataType print(ArrayList<DataType> para) {
        DataType dt = Interpreter.popBack(para);//不一定是Str类型
        System.out.print(dt.getString());
        Interpreter.putCachedData(dt);
        return null;
    }

    /**
     * 向控制台输出字符串
     *
     * @param para String
     * @return Object
     */
    private DataType println(ArrayList<DataType> para) {
        DataType dt = Interpreter.popBack(para);
        String s = dt == null ? null : dt.getString();
        Interpreter.putCachedData(dt);
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
    private Int min(ArrayList<DataType> para) {
        long x = Interpreter.popBackLong(para);
        long y = Interpreter.popBackLong(para);
        return Interpreter.getCachedInt(x > y ? y : x);
    }

    /**
     * 求最大值
     *
     * @param para int
     * @return Integer
     */
    private Int max(ArrayList<DataType> para) {
        long x = Interpreter.popBackLong(para);
        long y = Interpreter.popBackLong(para);
        return Interpreter.getCachedInt(x > y ? x : y);
    }

    /**
     * 求数组大小
     *
     * @param para int
     * @return Integer
     */
    private Int arrlen(ArrayList<DataType> para) {
        Array arr = Interpreter.popBack(para);
        return Interpreter.getCachedInt(arr.elements.length);
    }

    /**
     * 求最大值
     *
     * @param para int
     * @return Integer
     */
    private Int abs(ArrayList<DataType> para) {
        long x = Interpreter.popBackLong(para);
        return Interpreter.getCachedInt(Math.abs(x));
    }


    /**
     * 产生一个随机数，这个数一定是正数
     *
     * @return int 返回一个正数
     */
    public DataType random(ArrayList<DataType> para) {
        if (!para.isEmpty()) {
            return Interpreter.getCachedInt(random.nextInt(Interpreter.popBackInt(para)));
        }
        return Interpreter.getCachedInt(random.nextInt());
    }

    /**
     * 取余
     *
     * @param para int
     * @return gscript.Int
     */
    private Int mod(ArrayList<DataType> para) {
        long x = Interpreter.popBackLong(para);
        long y = Interpreter.popBackLong(para);
        return Interpreter.getCachedInt(x % y);
    }

    /**
     * 字符串长度
     *
     * @param para int
     * @return Integer
     */
    private Int strlen(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        return Interpreter.getCachedInt(s.length());
    }

    /**
     * 比较字符串
     *
     * @param para int
     * @return gscript.Int
     */
    private Bool equals(ArrayList<DataType> para) {
        String x = Interpreter.popBackStr(para);
        String y = Interpreter.popBackStr(para);
        return Interpreter.getCachedBool(x.equals(y));
    }

    /**
     * 保存全局变量
     *
     * @param para int
     * @return gscript.Int
     */
    private DataType def(ArrayList<DataType> para) {
        String name = Interpreter.popBackStr(para);
        DataType dt = Interpreter.popBack(para);
        inp.putGlobalVar(name.toLowerCase(), dt);
        return null;
    }

    /**
     * 某名称的全局变量是否存在
     *
     * @param para int
     * @return gscript.Int
     */
    private Bool isDef(ArrayList<DataType> para) {
        String name = Interpreter.popBackStr(para);
        if (inp.getGlobalVar(name.toLowerCase()) == null) {
            return Interpreter.getCachedBool(false);
        }
        return Interpreter.getCachedBool(true);
    }

    /**
     * 某名称的全局变量是否存在
     *
     * @param para int
     * @return gscript.Int
     */
    private Int valueOf(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        if (s != null && !"".equals(s)) {
            int v = 0;
            try {
                v = Integer.parseInt(s);
            } catch (Exception e) {
            }
            return Interpreter.getCachedInt(v);
        }
        return Interpreter.getCachedInt(0);
    }

    /**
     * 子串在母串中的位置
     *
     * @param para
     * @return
     */
    private Int idxof(ArrayList<DataType> para) {
        String m = Interpreter.popBackStr(para);
        String sub = Interpreter.popBackStr(para);
        if (m != null && sub != null) {
            return Interpreter.getCachedInt(m.indexOf(sub));
        }
        return Interpreter.getCachedInt(-1);
    }

    private Int lastIdxOf(ArrayList<DataType> para) {
        String m = Interpreter.popBackStr(para);
        String sub = Interpreter.popBackStr(para);
        if (m != null && sub != null) {
            return Interpreter.getCachedInt(m.lastIndexOf(sub));
        }
        return Interpreter.getCachedInt(-1);
    }

    /**
     * 取子串,同java String 的substring方法
     *
     * @param para
     * @return
     */
    private Str substr(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        int a = (int) Interpreter.popBackLong(para);
        if (!para.isEmpty()) {
            int b = (int) Interpreter.popBackLong(para);
            return Interpreter.getCachedStr(s.substring(a, b));
        }
        return Interpreter.getCachedStr(s.substring(a));
    }

    /**
     * 替换字符串,同java String 的replaceAll方法
     *
     * @param para
     * @return
     */
    private Str replace(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        String src = Interpreter.popBackStr(para);
        String dst = Interpreter.popBackStr(para);
        s = s.replaceAll(src, dst);
        return Interpreter.getCachedStr(s);
    }


    /**
     * 拆字符串
     *
     * @param para
     * @return
     */
    private Array split(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        String splitor = Interpreter.popBackStr(para);
        String[] ss = s.split(splitor);
        IntList dim = Interpreter.getCachedIntList();
        dim.add(ss.length);
        Array arr = new Array(dim);
        for (int i = 0; i < ss.length; i++) {
            dim.clear();
            dim.add(i);
            arr.setValue(dim, Interpreter.getCachedStr(ss[i]));
        }
        Interpreter.putCachedIntList(dim);
        return arr;
    }

    private Str lowCase(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        return Interpreter.getCachedStr(s.toLowerCase());
    }

    private Str upCase(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        return Interpreter.getCachedStr(s.toUpperCase());
    }

    private Bool startsWith(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        String sub = Interpreter.popBackStr(para);
        return Interpreter.getCachedBool(s.startsWith(sub));
    }

    private Bool endsWith(ArrayList<DataType> para) {
        String s = Interpreter.popBackStr(para);
        String sub = Interpreter.popBackStr(para);
        return Interpreter.getCachedBool(s.endsWith(sub));
    }

    private DataType base64enc(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            byte[] b = str.getBytes("utf-8");
            String s = javax.microedition.io.Base64.encode(b, 0, b.length);
            return Interpreter.getCachedStr(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType base64dec(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            byte[] b = javax.microedition.io.Base64.decode(str);
            String s = new String(b, "utf-8");
            return Interpreter.getCachedStr(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType urlenc(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            String s = URLEncoder.encode(str, "utf-8");
            return Interpreter.getCachedStr(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType urldec(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            String s = URLEncoder.encode(str, "utf-8");
            return Interpreter.getCachedStr(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType isnull(ArrayList<DataType> para) {
        Object d = Interpreter.popBackObject(para);
        if (d == null) {
            return Interpreter.getCachedBool(true);
        } else {
            return Interpreter.getCachedBool(false);
        }
    }


    private DataType getObjField(ArrayList<DataType> para) {
        try {
            Object ins = Interpreter.popBackObject(para);
            String fieldName = Interpreter.popBackStr(para);

            Class c = ins.getClass();
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
                    String val = (String) field.get(ins);
                    return Interpreter.getCachedStr(val);
                } else if (fc == int.class) {
                    int val = field.getInt(ins);
                    return Interpreter.getCachedInt(val);
                } else if (fc == long.class) {
                    long val = field.getLong(ins);
                    return Interpreter.getCachedInt(val);
                } else if (fc == byte.class) {
                    byte val = field.getByte(ins);
                    return Interpreter.getCachedInt(val);
                } else if (fc == short.class) {
                    short val = field.getShort(ins);
                    return Interpreter.getCachedInt(val);
                } else if (fc == char.class) {
                    char val = field.getChar(ins);
                    return Interpreter.getCachedInt(val);
                } else if (fc == boolean.class) {
                    boolean val = field.getBoolean(ins);
                    return Interpreter.getCachedBool(val);
                } else {//include float double and others
                    Object val = field.get(ins);
                    return Interpreter.getCachedObj(val);
                }
            }
            throw new RuntimeException("error can not found field:" + fieldName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType setObjField(ArrayList<DataType> para) {
        try {
            Object ins = Interpreter.popBackObject(para);
            String fieldName = Interpreter.popBackStr(para);
            DataType val = Interpreter.popBack(para);

            Class c = ins.getClass();
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
                    field.set(ins, ((Str) val).getVal());
                } else if (fc == int.class) {
                    field.setInt(ins, ((Int) val).getValAsInt());
                } else if (fc == long.class) {
                    field.setLong(ins, ((Int) val).getVal());
                } else if (fc == byte.class) {
                    field.setByte(ins, (byte) ((Int) val).getVal());
                } else if (fc == short.class) {
                    field.setShort(ins, (short) ((Int) val).getVal());
                } else if (fc == char.class) {
                    field.setChar(ins, (char) ((Int) val).getVal());
                } else if (fc == boolean.class) {
                    field.setBoolean(ins, ((Bool) val).getVal());
                } else if (fc == float.class) {
                    if (val.type == DataType.DTYPE_OBJ) {
                        Obj objv = (Obj) val;
                        if (objv.getVal() instanceof Float) {
                            field.setFloat(ins, ((Float) objv.getVal()).floatValue());
                        }
                    }
                } else if (fc == double.class) {
                    if (val.type == DataType.DTYPE_OBJ) {
                        Obj objv = (Obj) val;
                        if (objv.getVal() instanceof Double) {
                            field.setDouble(ins, ((Double) objv.getVal()).doubleValue());
                        }
                    }
                } else {
                    if (val.type == DataType.DTYPE_OBJ) {
                        Obj objv = (Obj) val;
                        if (objv.getVal().getClass() == (fc)) {
                            field.set(ins, objv.getVal());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType invokeImpl(ArrayList<DataType> para, Class c, String name, Class[] types, Object instance) {
        try {

            Object[] javaPara = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                DataType dt = Interpreter.popBack(para);
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
                        return Interpreter.getCachedStr((String) ret);
                    } else if (retType == int.class) {
                        return Interpreter.getCachedInt(((Integer) ret).intValue());
                    } else if (retType == long.class) {
                        return Interpreter.getCachedInt(((Long) ret).longValue());
                    } else if (retType == byte.class) {
                        return Interpreter.getCachedInt(((Byte) ret).byteValue());
                    } else if (retType == short.class) {
                        return Interpreter.getCachedInt(((Short) ret).shortValue());
                    } else if (retType == char.class) {
                        return Interpreter.getCachedInt(((Character) ret).charValue());
                    } else if (retType == boolean.class) {
                        return Interpreter.getCachedBool(((Boolean) ret).booleanValue());
                    } else {//include float double and others
                        return Interpreter.getCachedObj(ret);
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

    private DataType invokeJava(ArrayList<DataType> para) {
        Object ins = Interpreter.popBackObject(para);
        String javaFunc = Interpreter.popBackStr(para);
        Class c = ins.getClass();
        String name = javaFunc.substring(0, javaFunc.indexOf('('));
        String desc = javaFunc.substring(javaFunc.indexOf('('));
        Class[] types = ReflectMethod.getMethodPara(c.getClassLoader(), desc);

        return invokeImpl(para, c, name, types, ins);
    }

    private DataType invokeStatic(ArrayList<DataType> para) {
        String className = Interpreter.popBackStr(para);
        String javaFunc = Interpreter.popBackStr(para);
        Class c = null;
        try {
            c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not found:" + className);
        }
        String name = javaFunc.substring(0, javaFunc.indexOf('('));
        String desc = javaFunc.substring(javaFunc.indexOf('('));
        Class[] types = ReflectMethod.getMethodPara(c.getClassLoader(), desc);

        return invokeImpl(para, c, name, types, null);
    }


    private DataType trim(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            return Interpreter.getCachedStr(str.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType str2int(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            str = str.trim();
            int i = Integer.parseInt(str);
            return Interpreter.getCachedInt(i);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return null;
    }

    private DataType isNumStr(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            str = str.trim();
            long i = Long.parseLong(str);
            return Interpreter.getCachedBool(true);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return Interpreter.getCachedBool(false);
    }

    private DataType bitGet(ArrayList<DataType> para) {
        try {
            long i = Interpreter.popBackLong(para);
            int bitPos = Interpreter.popBackInt(para);
            return Interpreter.getCachedBool(((i >> bitPos) & 1) == 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private DataType bitSet(ArrayList<DataType> para) {
        try {
            long i = Interpreter.popBackLong(para);
            int bitPos = Interpreter.popBackInt(para);
            boolean set = Interpreter.popBackBool(para);
            i &= ~(1 << bitPos);
            if (set) {
                i |= 1 << bitPos;
            }
            return Interpreter.getCachedInt(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType bitAnd(ArrayList<DataType> para) {
        try {
            long i = Interpreter.popBackLong(para);
            long j = Interpreter.popBackLong(para);
            long r = i & j;
            return Interpreter.getCachedInt(r);
        } catch (Exception e) {
            SysLog.error("need 2 number for bit and");
            e.printStackTrace();
        }
        return null;
    }

    private DataType bitOr(ArrayList<DataType> para) {
        try {
            long i = Interpreter.popBackLong(para);
            long j = Interpreter.popBackLong(para);
            long r = i | j;
            return Interpreter.getCachedInt(r);
        } catch (Exception e) {
            SysLog.error("need 2 number for bit or");
            e.printStackTrace();
        }
        return null;
    }

    private DataType bitNot(ArrayList<DataType> para) {
        try {
            long i = Interpreter.popBackLong(para);
            long r = ~i;
            return Interpreter.getCachedInt(r);
        } catch (Exception e) {
            SysLog.error("need 2 number for bit not");
            e.printStackTrace();
        }
        return null;
    }


    private DataType encrypt(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            String key = Interpreter.popBackStr(para);
            byte[] strBytes = str.getBytes("utf-8");
            byte[] keyBytes = key.getBytes("utf-8");
            byte[] result = XorCrypt.xor_encrypt(strBytes, keyBytes);
            str = Base64.encode(result, 0, result.length);
            str = URLEncoder.encode(str, "UTF-8");
            return Interpreter.getCachedStr(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType decrypt(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            String key = Interpreter.popBackStr(para);
            str = URLDecoder.decode(str, "UTF-8");
            byte[] strBytes = Base64.decode(str);
            byte[] keyBytes = key.getBytes("utf-8");
            byte[] result = XorCrypt.xor_decrypt(strBytes, keyBytes);
            String i = new String(result, "utf-8");
            return Interpreter.getCachedStr(i);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return Interpreter.getCachedStr("");
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private DataType md5(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes("utf-8"));
            String str1 = (byteArrayToHex(md.digest()));
            return Interpreter.getCachedStr(str1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType sha1(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(str.getBytes("utf-8"));
            String str1 = (byteArrayToHex(md.digest()));
            return Interpreter.getCachedStr(str1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType remoteMethodCall(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            String ret = Glfm.glfmRemoteMethodCall(str);
            return Interpreter.getCachedStr(ret == null ? "" : ret);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType buyAppleProductById(ArrayList<DataType> para) {
        try {
            String str = Interpreter.popBackStr(para);
            String scriptStr = Interpreter.popBackStr(para);
            Glfm.glfmBuyAppleProductById(GCallBack.getInstance().getDisplay(), str, scriptStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DataType openOtherApp(ArrayList<DataType> para) {
        try {
            String urlstr = Interpreter.popBackStr(para);
            String moreStr = Interpreter.popBackStr(para);
            int detectAppInstalled = Interpreter.popBackInt(para);
            Glfm.glfmOpenOtherApp(GLUtil.toCstyleBytes(urlstr), GLUtil.toCstyleBytes(moreStr), detectAppInstalled);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DataType jsonParse(ArrayList<DataType> para) {
        String jsonStr = Interpreter.popBackStr(para);
        try {
            JsonParser parser = new JsonParser();
            Map map = (Map) parser.deserial(jsonStr, Map.class);
            return Interpreter.getCachedObj(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DataType jsonGet(ArrayList<DataType> para) {
        Map<String, String> json = (Map) Interpreter.popBackObject(para);
        String key = Interpreter.popBackStr(para);

        return Interpreter.getCachedStr(json.get(key));
    }

    public DataType jsonSet(ArrayList<DataType> para) {
        Map<String, String> json = (Map) Interpreter.popBackObject(para);
        String key = Interpreter.popBackStr(para);
        String oldValue = json.get(key);
        String value = Interpreter.popBackStr(para);
        json.put(key, value);
        return Interpreter.getCachedStr(oldValue);
    }

    public DataType json2Str(ArrayList<DataType> para) {
        Map<String, String> json = (Map) Interpreter.popBackObject(para);
        JsonPrinter printer = new JsonPrinter();
        String s = printer.serial(json);
        return Interpreter.getCachedStr(s);
    }

    public DataType run(ArrayList<DataType> para) {
        String script = Interpreter.popBackStr(para);
        String subcall = Interpreter.popBackStr(para);
        inp.loadFromString(script);
        return inp.callSub(subcall);
    }
}
