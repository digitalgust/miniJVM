package com.ebsee;


import com.ebsee.j2c.AssistLLVM;
import com.ebsee.j2c.Util;

import java.io.*;


/**
 *
 */
public class Main {
    static String[][] microDefFields = {
            {"java.lang.String", "value", "[C", "value_in_string"},
            {"java.lang.String", "count", "I", "count_in_string"},
            {"java.lang.String", "offset", "I", "offset_in_string"},
            {"java.lang.Class", "classHandle", "J", "classHandle_in_class"},
            {"org.mini.reflect.ReflectMethod", "methodId", "J", "methodId_in_reflectmethod"},

            {"java.lang.Boolean", "value", "Z", "value_in_boolean"},
            {"java.lang.Byte", "value", "B", "value_in_byte"},
            {"java.lang.Short", "value", "S", "value_in_short"},
            {"java.lang.Character", "value", "C", "value_in_character"},
            {"java.lang.Integer", "value", "I", "value_in_integer"},
            {"java.lang.Long", "value", "J", "value_in_long"},
            {"java.lang.Float", "value", "F", "value_in_float"},
            {"java.lang.Double", "value", "D", "value_in_double"},

            {"java.lang.ref.WeakReference", "target", "Ljava/lang/Object;", "target_in_weakreference"},

            {"java.lang.Thread", "stackFrame", "J", "stackFrame_in_thread"},
            {"java.lang.StackTraceElement", "declaringClass", "Ljava/lang/String;", "declaringClass_in_stacktraceelement"},
            {"java.lang.StackTraceElement", "methodName", "Ljava/lang/String;", "methodName_in_stacktraceelement"},
            {"java.lang.StackTraceElement", "fileName", "Ljava/lang/String;", "fileName_in_stacktraceelement"},
            {"java.lang.StackTraceElement", "lineNumber", "I", "lineNumber_in_stacktraceelement"},
            {"java.lang.StackTraceElement", "parent", "Ljava/lang/StackTraceElement;", "parent_in_stacktraceelement"},
            {"java.lang.StackTraceElement", "declaringClazz", "Ljava/lang/Class;", "declaringClazz_in_stacktraceelement"},
    };

    public static void main(String[] args) throws IOException {
        String jsrcPath = "../../minijvm/java/src/main/java/"
                + File.pathSeparator + "../../test/minijvm_test/src/main/java/"//
//                + File.pathSeparator + "../../mobile/java/glfm_gui/src/main/java"//
//                + File.pathSeparator + "../../mobile/java/ExApp/src/main/java"//
//                + File.pathSeparator + "../../../g3d/src/main/java/"//
                ;
        String classesPath = "../app/generted/classes/";
        String csrcPath = "../app/generted/c/";


        if (args.length < 3) {
            System.out.println("Posix :");
            System.out.println("Convert java to c file:");
            System.out.println("java -cp ./translator/dist/translator.jar com.ebsee.Main ./app/java ./app/out/classes ./app/out/c/");
        } else {
            jsrcPath = args[0] + "/";
            classesPath = args[1] + "/";
            csrcPath = args[2] + "/";
        }

        System.out.println("java source *.java path      : " + jsrcPath);
        System.out.println("classes *.class output path  : " + classesPath);
        System.out.println("c *.c output path            : " + csrcPath);

        File f;
        boolean res;
        f = new File(classesPath);
        System.out.println(f.getAbsolutePath() + (f.exists() ? " exists " : " not exists"));
        res = Util.deleteTree(f);
        res = f.mkdirs();
        f = new File(csrcPath);
        System.out.println(f.getAbsolutePath() + (f.exists() ? " exists " : " not exists"));
        res = Util.deleteTree(f);
        res = f.mkdirs();

        long startAt = System.currentTimeMillis();
        AssistLLVM.convert(jsrcPath, classesPath, csrcPath, microDefFields);
        System.out.println("convert success , cost :" + (System.currentTimeMillis() - startAt));
    }

}


