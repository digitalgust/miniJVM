package org.mini.layout.gscript;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

abstract public class Lib {
    protected Map<String, Integer> methodNames = new HashMap<>();

    /**
     * 构造方法
     */
    public Lib() {
    }

    public Map<String, Integer> getMethodNames() {
        return methodNames;
    }

    /**
     * 供解释器调用，取得某个名字方法的ID
     *
     * @param name String
     * @return int
     */
    public int getMethodID(String name) {
        Map<String, Integer> methodNames = getMethodNames();
        if (methodNames != null && name != null) {
            Integer i = methodNames.get(name.toLowerCase());
            if (i != null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 用户方法实现
     *
     * @param para     Stack
     * @param methodID int
     * @return Object
     */
    abstract public DataType call(Interpreter inp, Vector para, int methodID);
}
