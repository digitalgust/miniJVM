package org.mini.xmlui.gscript;

import java.util.Vector;

abstract public class Lib {
    //方法名表
    /**
     * 构造方法
     */
    public Lib() {
    }

    abstract public String[] getMethodNames();

    /**
     * 供解释器调用，取得某个名字方法的ID
     * @param name String
     * @return int
     */
    public int getMethodID(String name) {
        String[] methodNames = getMethodNames();
        if (methodNames != null) {
            for (int i = 0; i < methodNames.length; i++) {
                if (name.equals(methodNames[i].toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 用户方法实现
     * @param para Stack
     * @param methodID int
     * @return Object
     */
    abstract public DataType call(Interpreter inp, Vector para, int methodID);
}
