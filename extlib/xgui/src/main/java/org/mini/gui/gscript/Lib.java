package org.mini.gui.gscript;

import java.util.*;

abstract public class Lib {
    protected Map<String, Func> methodNames = new HashMap<>();
    protected Interpreter inp;

    /**
     * 构造方法
     */
    public Lib() {
    }

    public Func getFuncByName(String name) {
        return methodNames.get(name);
    }

    public void setInterpreter(Interpreter interpreter) {
        this.inp = interpreter;
    }
}
