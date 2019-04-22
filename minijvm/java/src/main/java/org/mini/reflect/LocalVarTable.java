/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

/**
 * StackFrame 中的局部变量 ，对应jvm中的Runtime.localvar
 * @author gust
 */
public class LocalVarTable {
    
    //不可随意改动字段类型及名字，要和native一起改
    public long codeIndex;
    public int length;
    public String name;
    public String signature;
    public int slot;
    
    public String toString(){
        return name+"|"+signature+"|start:"+codeIndex+"|len:"+length+"|slot:"+slot;
    }
}
