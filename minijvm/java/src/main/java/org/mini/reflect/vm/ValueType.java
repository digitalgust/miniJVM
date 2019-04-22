
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect.vm;

/**
 *
 * @author gust
 */
public class ValueType {

    public byte type;
    public long value;
    public char bytes;

    public ValueType(byte type) {
        this.type = type;
        bytes = RConst.getBytes(type);
    }


    public String toString() {
        return (char) type + "|" + bytes + "|" + Long.toString(value, 16);
    }
}