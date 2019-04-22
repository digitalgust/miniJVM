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
public class RConst {

    static public final int ACC_PUBLIC = 0x0001;
    static public final int ACC_PRIVATE = 0x0002;
    static public final int ACC_PROTECTED = 0x0004;
    static public final int ACC_STATIC = 0x0008;
    static public final int ACC_FINAL = 0x0010;
    static public final int ACC_SYNCHRONIZED = 0x0020;
    static public final int ACC_VOLATILE = 0x0040;
    static public final int ACC_TRANSIENT = 0x0080;
    static public final int ACC_NATIVE = 0x0100;
    static public final int ACC_INTERFACE = 0x0200;
    static public final int ACC_ABSTRACT = 0x0400;
    static public final int ACC_STRICT = 0x0800;

    public static final byte TAG_ARRAY = 91; // '[' - an array object (objectID size).
    public static final byte TAG_BYTE = 66; // 'B' - a byte TAG_value (1 byte).
    public static final byte TAG_CHAR = 67; // 'C' - a character value (2 bytes).
    public static final byte TAG_OBJECT = 76; // 'L' - an object (objectID size).
    public static final byte TAG_FLOAT = 70; // 'F' - a float value (4 bytes).
    public static final byte TAG_DOUBLE = 68; // 'D' - a double value (8 bytes).
    public static final byte TAG_INT = 73; // 'I' - an int value (4 bytes).
    public static final byte TAG_LONG = 74; // 'J' - a long value (8 bytes).
    public static final byte TAG_SHORT = 83; // 'S' - a short value (2 bytes).
    public static final byte TAG_VOID = 86; // 'V' - a void value (no bytes).
    public static final byte TAG_BOOLEAN = 90; // 'Z' - a boolean value (1 byte).
    public static final byte TAG_STRING = 115; // 's' - a String object (objectID size).
    public static final byte TAG_THREAD = 116; // 't' - a Thread object (objectID size).
    public static final byte TAG_THREAD_GROUP = 103; // 'g' - a ThreadGroup object (objectID size).
    public static final byte TAG_CLASS_LOADER = 108; // 'l' - a ClassLoader object (objectID size).
    public static final byte TAG_CLASS_OBJECT = 99; // 'c' - a class object object (objectID size).

    public static char getBytes(byte type) {
        char bytes = '0';
        switch (type) {
            case TAG_BYTE:
            case TAG_BOOLEAN:
                bytes = '1';
                break;
            case TAG_SHORT:
            case TAG_CHAR:
                bytes = '2';
                break;
            case TAG_INT:
            case TAG_FLOAT:
                bytes = '4';
                break;
            case TAG_LONG:
            case TAG_DOUBLE:
                bytes = '8';
                break;
            case TAG_ARRAY:
            case TAG_OBJECT:
            case TAG_STRING:
            case TAG_THREAD:
            case TAG_THREAD_GROUP:
            case TAG_CLASS_LOADER:
            case TAG_CLASS_OBJECT:
                bytes = 'R';
                break;
            case TAG_VOID:
                bytes = '0';
                break;
        }
        return bytes;

    }
}
