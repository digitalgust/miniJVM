/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RefNative;

/**
 *
 * 反射mini jvm中的 Runtime, 其包含调用堆栈相关的信息
 * <pre>
 * long sfid = RefNative.getStackFrame(Thread.currentThread());
 * System.out.println("sfid="+Long.toString(sfid, 16));
 * StackFrame sf = new StackFrame(sfid);
 * System.out.println("StackFrame:" + sf.method.methodName);
 * </pre>
 *
 * @author gust
 */
public class StackFrame {

    //不可随意改动字段类型及名字，要和native一起改
    public long runtimeId;
    public long classId;
    public long parentId;
    public long pc;
    public long byteCode;
    public long methodId;

    public StackFrame son;
    public StackFrame parent;
    public ReflectMethod method;
    public long[] localVariables;
    public long localThis;

    public StackFrame(long rid) {
        this(rid, null);
    }

    public StackFrame(long rid, StackFrame son) {
        this.runtimeId = rid;
        this.son = son;
        mapRuntime(runtimeId);
        if (methodId != 0) {
            method = new ReflectMethod(null, methodId);
        }
        if (parentId != 0) {
            parent = new StackFrame(parentId, this);
        }
    }

    public StackFrame getLastSon() {
        return son == null ? this : son.getLastSon();
    }

    public StackFrame getTopParent() {
        return parent == null ? this : parent.getTopParent();
    }
    /**
     * 返回 lastson=0 起的第frameID个runtime
     *
     * @param frameID
     * @return
     */
    public StackFrame getFrameByIndex(long frameID) {
        StackFrame r = getLastSon();
        for (int i = 0; i < frameID; i++) {
            r = r.parent;
        }
        return r;
    }

    public int getDeepth() {
        int deep = 0;
        StackFrame r = this;
        while (r != null) {
            r = r.son;
            deep++;
        }
        deep--;//顶层
        return deep;
    }

    public String toString() {
        return "StackFrame:"
                + "|" + Long.toString(runtimeId, 16)
                + "|class:" + Long.toString(classId, 16)
                + "|parent:" + Long.toString(parentId, 16)
                + "|pc:" + Long.toString(pc, 16)
                + "|" + Long.toString(byteCode, 16)
                + "|pos:" + (pc - byteCode)
                + "|this:" + Long.toString(localThis, 16)
                + "|" + method.methodName
                + "|" + RefNative.id2obj(classId);
    }

    final native void mapRuntime(long runtimeId);
}
