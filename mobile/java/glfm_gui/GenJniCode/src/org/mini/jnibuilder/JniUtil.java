package org.mini.jnibuilder;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author gust
 */
public class JniUtil {

    public static boolean isPointer(String nativeTypeStr) {
        return nativeTypeStr.contains("/*ptr*/");
    }

    public static boolean isStruct(String nativeTypeStr) {
        return nativeTypeStr.contains("/*none_ptr*/");
    }

    public static boolean isConstPointer(String nativeTypeStr) {
        return nativeTypeStr.contains("const");
    }

    public static boolean isTypes(String[] types, String s) {
        for (String t : types) {
            if (t.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public static String getType(String[][] TYPES_ALL, String typeAndName) {
        int lastPos = 0;
        String lastString = null;
        for (String[] types : TYPES_ALL) {
            for (String s : types) {
                if (typeAndName.startsWith(s)) {
                    if (s.length() > lastPos) {
                        lastPos = s.length();
                        lastString = s;
                    }
                }
            }
        }
        return lastString;
    }

    /* miniJVM Instance layout accessor expressions (keep in sync with jvm.h).
     * Generated C uses these instead of touching Instance members directly. */
    public static String jarrBody(String v) {
        return "jarray_body(" + v + ")";
    }

    public static String jarrLen(String v) {
        return "jarray_length(" + v + ")";
    }

}
