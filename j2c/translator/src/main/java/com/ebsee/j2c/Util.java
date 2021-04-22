package com.ebsee.j2c;

import com.ebsee.classparser.ClassFile;
import com.ebsee.classparser.Field;
import com.ebsee.classparser.Method;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


public final class Util {

    public static final String BOOLEAN = "s8";
    public static final String BYTE = "s8";
    public static final String CHAR = "u16";
    public static final String SHORT = "s16";
    public static final String INT = "s32";
    public static final String LONG = "s64";
    public static final String FLOAT = "f32";
    public static final String DOUBLE = "f64";
    public static final String POINTER = "__refer";
    public static final String SLOT2 = "SLOT2";
    public static final String VOID = "void";


    static public final String STR_RUNTIME_TYPE_NAME = "JThreadRuntime";
    static public final String STR_VMTABLE_TYPE_NAME = "VMTable";
    static public final String STR_IMTABLE_TYPE_NAME = "IMTable";
    static public final String STR_UTFRAW_TYPE_NAME = "UtfRaw";
    static public final String STR_LABELTABLE_TYPE_NAME = "LabelTable";
    static public final String STR_INSPROP_TYPE_NAME = "InstProp";
    static public final String STR_JOBJECT_TYPE_NAME = "JObject";
    static public final String STR_JARRAY_TYPE_NAME = "JArray";
    static public final String STR_JCLASS_TYPE_NAME = "JClass";
    static public final String STR_EXCEPTIONTABLE_TYPE_NAME = "ExceptionTable";
    static public final String STR_EXCEPTIONITEM_TYPE_NAME = "ExceptionItem";

    static public final String CLASS_JAVA_LANG_STRING = "java/lang/String";
    static public final String CLASS_JAVA_LANG_OBJECT = "java/lang/Object";
    static public final String CLASS_JAVA_IO_EOF_EXCEPTION = "java/io/EOFException";
    static public final String CLASS_JAVA_IO_IO_EXCEPTION = "java/io/IOException";
    static public final String CLASS_JAVA_LANG_OUTOFMEMERYERROR = "java/lang/OutOfMemoryError";
    static public final String CLASS_JAVA_LANG_VIRTUAL_MACHINE_ERROR = "java/lang/VirtualMachineError";
    static public final String CLASS_JAVA_LANG_NO_CLASS_DEF_FOUND_ERROR = "java/lang/NoClassDefFoundError";
    static public final String CLASS_JAVA_LANG_FILE_NOT_FOUND_EXCEPTION = "java/lang/FileNotFoundException";
    static public final String CLASS_JAVA_LANG_ARITHMETIC_EXCEPTION = "java/lang/ArithmeticException";
    static public final String CLASS_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION = "java/lang/ClassNotFoundException";
    static public final String CLASS_JAVA_LANG_NULL_POINTER_EXCEPTION = "java/lang/NullPointerException";
    static public final String CLASS_JAVA_LANG_NO_SUCH_METHOD_EXCEPTION = "java/lang/NoSuchMethodException";
    static public final String CLASS_JAVA_LANG_NO_SUCH_FIELD_EXCEPTION = "java/lang/NoSuchFieldException";
    static public final String CLASS_JAVA_LANG_ILLEGAL_ARGUMENT_EXCEPTION = "java/lang/IllegalArgumentException";
    static public final String CLASS_JAVA_LANG_CLASS_CAST_EXCEPTION = "java/lang/ClassCastException";
    static public final String CLASS_JAVA_LANG_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = "java/lang/ArrayIndexOutOfBoundsException";
    static public final String CLASS_JAVA_LANG_INSTANTIATION_EXCEPTION = "java/lang/InstantiationException";


    static final String INNER_CLASS_SPLITOR = "_00024"; //$
    static final String UNDERLINE_SPLITOR = "_1";//;
    static final String SEMICOLON_SPLITOR = "_2";//;
    static final String LEFT_BRACERT_SPLITOR = "_3";//[

    static public String getClassStructTypePtr(String str) {
        return getClassStructType(str) + "*";
    }

    static public String regulString(String s) {
        return s.replace("_", UNDERLINE_SPLITOR).replaceAll("[\\.\\/\\<\\>]", "_").replace("$", INNER_CLASS_SPLITOR).replace(";", SEMICOLON_SPLITOR).replace("[", LEFT_BRACERT_SPLITOR);
    }

    static public String getClassStructType(String str) {
        return regulString("struct " + str);
    }

    static public String getClassStructTypeRawName(String str) {
        return regulString(str);
    }

    public static String getJavaSignatureCtype(String str) {

        if (str.equals("B")) {
            return BYTE;
        } else if (str.equals("S")) {
            return SHORT;
        } else if (str.equals("C")) {
            return CHAR;
        } else if (str.equals("I")) {
            return INT;
        } else if (str.equals("J")) {
            return LONG;
        } else if (str.equals("F")) {
            return FLOAT;
        } else if (str.equals("D")) {
            return DOUBLE;
        } else if (str.equals("V")) {
            return "void";
        } else if (str.equals("Z")) {
            return BOOLEAN;
        } else if (str.startsWith("L")) {
            str = str.substring(1, str.length() - 1);
            return getClassStructTypePtr(str);
        } else if (str.startsWith("[")) {
            return javaArr2CtypePtr();
        }
        //return null;
        throw new RuntimeException(str);
    }

    public static String javaArr2CtypePtr() {
        return STR_JARRAY_TYPE_NAME + " *";
    }


    public static boolean isRefer_by_Jtype(String jtype) {
        switch (jtype) {
            case "I":
            case "S":
            case "Z":
            case "C":
            case "B":
            case "J":
            case "D":
            case "F":
                return false;
            default:
                return true;
        }
    }

    public static boolean isRefer_by_Ctype(String jtype) {
        switch (jtype) {
            case VOID:
            case INT:
            case SHORT:
                //case BOOLEAN:
            case CHAR:
            case BYTE:
            case LONG:
            case DOUBLE:
            case FLOAT:
                return false;
            default:
                return true;
        }
    }

    public static String getStackFieldName_by_Jtype(String jtype) {
        switch (jtype) {
            case "I":
            case "S":
            case "Z":
            case "C":
            case "B":
                return "i";
            case "J":
                return "j";
            case "F":
                return "f";
            case "D":
                return "d";
            default:
                return "obj";
        }
    }

    public static String getStackName_by_Jtype(String jtype) {
        switch (jtype) {
            case "I":
            case "S":
            case "Z":
            case "C":
            case "B":
            case "J":
            case "F":
            case "D":
                return "";
            default:
                return "r";
        }
    }

    public static String getStackFieldName_by_Ctype(String ctype) {
        switch (ctype) {
            case INT:
            case SHORT:
            case CHAR:
            case BYTE:
                return "i";
            case LONG:
                return "j";
            case FLOAT:
                return "f";
            case DOUBLE:
                return "d";
            default:
                return "obj";
        }
    }

    public static String getStackName_by_Ctype(String ctype) {
        switch (ctype) {
            case INT:
            case SHORT:
            case CHAR:
            case BYTE:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return "";
            default:
                return "r";
        }
    }

    public static String getArrayName_by_Ctype(String ctype) {
        switch (ctype) {
            case INT:
                return "as_s32_arr";
            case SHORT:
                return "as_s16_arr";
            case CHAR:
                return "as_u16_arr";
            case BYTE:
                return "as_s8_arr";
            case LONG:
                return "as_s64_arr";
            case FLOAT:
                return "as_f32_arr";
            case DOUBLE:
                return "as_f64_arr";
            default:
                return "as_obj_arr";
        }
    }

    static public int getSlot_by_Ctype(String ctype) {
        if (DOUBLE.equals(ctype) || LONG.equals(ctype)) {
            return 2;
        } else if (VOID.equals(ctype)) {
            return 0;
        }
        return 1;
    }

    static public int getSlot_by_Jtype(String jtype) {
        if ("D".equals(jtype) || "J".equals(jtype)) {
            return 2;
        } else if ("V".equals(jtype)) {
            return 0;
        }
        return 1;
    }


    public static String getDefValue_by_Ctype(String type) {
        String returnValue = "";
        switch (type) {
            case "void": {
                returnValue = "";
                break;
            }
            case BYTE:
            case SHORT:
            case INT:
            case CHAR:
            case LONG: {
                returnValue = "0";
                break;
            }
            case FLOAT:
            case DOUBLE: {
                returnValue = "0";
                break;
            }
            default: {
                returnValue = "NULL";
            }
        }
        return returnValue;
    }

    public static List<String> getJavaMethodSignatureCtypes(String str) {
        List<String> list = splitMethodSignature(str);

        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(getJavaSignatureCtype(s));
        }
        return result;
    }

    public static List<String> splitMethodSignature(String str) {
        //System.out.print("Parse ");
        //System.out.println("signatures: \"" + str + "\"");

        List<String> result = new ArrayList<>();
        String sa = str;
        while (sa.length() > 0) {
            char c = sa.charAt(0);
            if (c == 'S' || c == 'B' || c == 'C' || c == 'I' || c == 'J' || c == 'F' || c == 'D') {
                String tmp = sa.substring(0, 1);
                result.add(tmp);
                sa = sa.substring(1);
            } else if (c == 'L') {
                int pos = sa.indexOf(';');
                String tmp = sa.substring(0, pos + 1);
                result.add(tmp);
                sa = sa.substring(pos + 1);
            } else { //'['

                String tmp = "";
                //find first not '['
                for (int i = 0; i < sa.length(); i++) {
                    c = sa.charAt(i);
                    tmp += c;
                    if (sa.charAt(i) != '[') {
                        break;
                    }
                }
                if (c == 'L') {
                    int pos = sa.indexOf(';');
                    tmp = sa.substring(0, pos + 1);
                }
                result.add(tmp);
                sa = sa.substring(tmp.length());
            }
        }
        return result;
    }


    public static String getMethodRawName(Method method) {
        return getMethodRawName(method.getClassFile().getThisClassName(), method.getMethodName(), method.getDescriptor());
    }


    public static String getMethodRawName(String className, String methodName, String signature) {
        StringBuilder result = new StringBuilder();
        result.append("func");
        result.append('.');
        result.append(className);
        result.append('.');
        result.append(methodName);
        result.append('.');
        result.append('.');
        result.append(signature.substring(signature.indexOf('(') + 1, signature.indexOf(')')));
        result.append('.');
        result.append(signature.substring(signature.indexOf(')') + 1));
        String s = result.toString();
        s = regulString(s);
        return s;
    }

    public static String getMethodDeclare(String className, String methodName, JSignature sig) {
        return sig.getResult() + " " + getMethodRawName(className, methodName, sig.javaSignature) + "(" + sig.getCTypeArgsString() + ")";
    }

    public static String getBridgeMethodName(Method method) {
        return getBridgeMethodName(method.getClassFile().getThisClassName(), method.getMethodName(), method.getDescriptor());
    }

    public static String getBridgeMethodName(String className, String methodName, String signature) {
        StringBuilder result = new StringBuilder();
        result.append("bridge");
        result.append('.');
        result.append(className);
        result.append('.');
        result.append(methodName);
        result.append('.');
        result.append('.');
        result.append(signature.substring(signature.indexOf('(') + 1, signature.indexOf(')')));
        result.append('.');
        result.append(signature.substring(signature.indexOf(')') + 1));
        String s = result.toString();
        s = regulString(s);
        return s;
    }

    public static String getBridgeMethodDeclare(String className, String methodName, String sig) {
        return "void " + getBridgeMethodName(className, methodName, sig) + "(" + STR_RUNTIME_TYPE_NAME + " *runtime, __refer ins, ParaItem *para, ParaItem *ret)";
    }

    public static String class2structDefine(String className) {
        try {
            if (className.startsWith("[")) {
//                String s = javaArr2CtypePtr(className).replace("*", "");
//                s += "" + javaArr2structDefine(className) + ";";
//                return s;
                throw new RuntimeException("Array here");
            } else {
                ClassFile c = ClassManger.getClassFile(className);
                List<Field> fieldsContainSuperClass = ClassManger.getFieldTable(className);
                StringJoiner joiner = new StringJoiner("; ", "{", ";}");
                joiner.add(getInstPropType() + " prop");
                joiner.add(getVMTableTypePtr() + " vm_table");
                for (Field f : fieldsContainSuperClass) {
                    if ((f.getAccessFlags() & Modifier.STATIC) == 0) {
                        joiner.add(getJavaSignatureCtype(f.getDescriptor()) + " " + getFieldVarName(f));
                    }
                }
                String s = getClassStructType(className) + " " + joiner + ";";
                return s;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return className + " is unknown";
        }
    }


    public static String getFieldVarName(Field field) {
        if (field == null) {
            int debug = 1;
            System.out.println("field is null");
        }
        return regulString(field.getFieldName()) + "_" + ClassManger.getFieldIndexInFieldTable(field);
    }

    public static String getStaticFieldStructVarName(String className) {
        return "static_var_" + getClassStructTypeRawName(className);
    }

    public static String getStaticFieldStructTypeRawName(String className) {
        return getClassStructTypeRawName(className) + "_static";
    }

    public static String getStaticFieldStructType(String className) {
        return getClassStructType(className) + "_static";
    }

    public static String getStaticFieldStructTypePtr(String className) {
        return getClassStructType(className) + "_static *";
    }

    public static String classStaticInit(String className) {
        try {
            if (className.startsWith("[")) {
                return "";
            } else {
                ClassFile c = ClassManger.getClassFile(className);
                StringJoiner joiner = new StringJoiner(", ", "{", "}");
                int staticVarCnt = 0;
                for (Field f : c.getFields()) {
                    if ((f.getAccessFlags() & Modifier.STATIC) != 0) {
                        if (isRefer_by_Jtype(f.getDescriptor())) {
                            joiner.add("NULL");
                        } else {
                            joiner.add("0");
                        }
                        staticVarCnt++;
                    }
                }
                if (staticVarCnt > 0) {
                    String s = getStaticFieldStructType(className) + " " + getStaticFieldStructVarName(className) + " = " + joiner + ";";
                    return s;
                } else {
                    return "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return className + " is unknown";
        }
    }

    public static int getClassStaticFieldCount(String className) {
        try {
            if (className.startsWith("[")) {
                return 0;
            } else {
                ClassFile c = ClassManger.getClassFile(className);
                StringJoiner joiner = new StringJoiner("; ", "{", "}");
                int staticVarCnt = 0;
                for (Field f : c.getFields()) {
                    if ((f.getAccessFlags() & Modifier.STATIC) != 0) {
                        joiner.add(getJavaSignatureCtype(f.getDescriptor()) + " " + getFieldVarName(f));
                        staticVarCnt++;
                    }
                }
                if (staticVarCnt > 0) return staticVarCnt;
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String classStatic2structDefine(String className) {
        try {
            if (className.startsWith("[")) {
                return "";
            } else {
                ClassFile c = ClassManger.getClassFile(className);
                StringJoiner joiner = new StringJoiner("; ", "{", "}");
                int staticVarCnt = 0;
                for (Field f : c.getFields()) {
                    if ((f.getAccessFlags() & Modifier.STATIC) != 0) {
                        joiner.add(getJavaSignatureCtype(f.getDescriptor()) + " " + getFieldVarName(f));
                        staticVarCnt++;
                    }
                }
                if (staticVarCnt > 0) joiner.add(" ");
                String s = getStaticFieldStructType(className) + " " + joiner + ";";
                return s;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return className + " is unknown";
        }
    }

    public static String getStaticFieldExternalDeclare(String className) {
        return "extern " + getStaticFieldStructType(className) + " " + getStaticFieldStructVarName(className) + ";";
    }

    public static String getCtype_by_className(String className) {
        try {
            String cn = "L" + className.replace('.', '/') + ";";
            return getJavaSignatureCtype(cn);
        } catch (Exception e) {
            e.printStackTrace();
            return className + " is unknown";
        }
    }

    public static int sizeOf(char javaType) {
        if ('Z' == javaType) return 1;
        if ('B' == javaType) return 1;
        if ('C' == javaType) return 2;
        if ('S' == javaType) return 2;
        if ('I' == javaType) return 4;
        if ('J' == javaType) return 8;
        if ('F' == javaType) return 4;
        if ('D' == javaType) return 8;
        return 0;
    }

    public static String getjavaTag_by_JtypeIndex(int type) {
        switch (type) {
            case 4:
                return "Z";
            case 5:
                return "C";
            case 6:
                return "F";
            case 7:
                return "D";
            case 8:
                return "B";
            case 9:
                return "S";
            case 10:
                return "I";
            case 11:
                return "J";
        }
        return null;
    }

    public static String getJavaDesc_by_className(String name) {
        switch (name) {
            case "int":
                return "I";
            case "char":
                return "C";
            case "float":
                return "F";
            case "double":
                return "D";
            case "byte":
                return "B";
            case "short":
                return "S";
            case "boolean":
                return "Z";
            case "long":
                return "J";
            default:
                return "L" + name.replace('.', '/') + ";";
        }
    }

    //---------------------------------    virtual method table ----------------------------------


    public static String getVMTableName(String className) {
        return "vmtable_" + getClassStructTypeRawName(className);
    }

    public static String getVMTableTypePtr() {
        return STR_VMTABLE_TYPE_NAME + "*";
    }

    public static String getVMTableType() {
        return STR_VMTABLE_TYPE_NAME;
    }

    //---------------------------------    InstProp ----------------------------------
    public static String getInstPropType() {
        return STR_INSPROP_TYPE_NAME;
    }

    public static String getInstPropTypePtr() {
        return STR_INSPROP_TYPE_NAME + "*";
    }

    //---------------------------------    UtfRaw ----------------------------------
    public static String getUtfRawType() {
        return STR_UTFRAW_TYPE_NAME;
    }

    public static String getUtfRawTypePtr() {
        return STR_UTFRAW_TYPE_NAME + "*";
    }


    //---------------------------------    LabelTable ----------------------------------
    public static String getLabelTableType() {
        return STR_LABELTABLE_TYPE_NAME;
    }

    public static String getLabelTableTypePtr() {
        return STR_LABELTABLE_TYPE_NAME + "*";
    }

    public static String getLabelTableRawName() {
        return "__labtab";
    }

    //---------------------------------    ExceptionTable ----------------------------------
    public static String getExceptionTableType() {
        return STR_EXCEPTIONTABLE_TYPE_NAME;
    }

    public static String getExceptionTableTypePtr() {
        return STR_EXCEPTIONTABLE_TYPE_NAME + "*";
    }


    public static String getExceptionTableRawName(String className, String methodName, JSignature signature) {
        return "extable_" + getMethodRawName(className, methodName, signature.getJavaSignature());
    }


    public static String getExceptionItemType() {
        return STR_EXCEPTIONITEM_TYPE_NAME;
    }
    //---------------------------------     ----------------------------------

    public static String getLambdaClassName(String className, String implMethodName) {
        return className.replace("/", "$") + "$" + implMethodName;
    }


    public static boolean deleteTree(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File sf : files) {
                //System.out.println("file:" + sf.getAbsolutePath());
                deleteTree(sf);
            }
        }
        boolean s = f.delete();
        //System.out.println("delete " + f.getAbsolutePath() + " state:" + s);
        return s;
    }
}
