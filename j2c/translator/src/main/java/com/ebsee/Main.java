/* SPDX-License-Identifier: MIT */

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
            {"java.lang.String", "hash", "I", "hash_in_string"},
            {"java.lang.Class", "classHandle", "J", "classHandle_in_class"},
            {"java.lang.Class", "classLoader", "Ljava/lang/ClassLoader;", "classLoader_in_class"},
            {"java.lang.Thread", "daemon", "Z", "daemon_in_thread"},
            {"java.lang.Thread", "priority", "I", "priority_in_thread"},
            {"java.lang.Thread", "target", "Ljava/lang/Runnable;", "target_in_thread"},
            {"org.mini.reflect.ReflectMethod", "methodId", "J", "methodId_in_reflectmethod"},
            {"sun.misc.Launcher", "systemClassLoader", "Ljava/lang/ClassLoader;", "systemClassLoader_in_launcher"},

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

            {"org.mini.fs.InnerFileStat", "exists", "Z", "exists_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_dev", "I", "dev_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_ino", "S", "ino_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_mode", "S", "mode_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_nlink", "S", "nlink_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_uid", "S", "uid_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_gid", "S", "gid_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_rdev", "S", "rdev_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_size", "J", "size_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_atime", "J", "atime_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_mtime", "J", "mtime_in_filestat"},
            {"org.mini.fs.InnerFileStat", "st_ctime", "J", "ctime_in_filestat"},
            {"org.mini.reflect.DirectMemObj", "memAddr", "J", "address_in_directmem"},
            {"org.mini.reflect.DirectMemObj", "length", "I", "length_in_directmem"},
            {"org.mini.reflect.DirectMemObj", "typeDesc", "C", "typedesc_in_directmem"},
            {"org.mini.reflect.ReflectClass", "superclass", "J", "superclass_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "className", "Ljava/lang/String;", "name_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "accessFlags", "S", "access_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "source", "Ljava/lang/String;", "source_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "signature", "Ljava/lang/String;", "signature_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "status", "I", "status_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "classObj", "Ljava/lang/Class;", "classobj_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "fieldIds", "[J", "fieldids_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "methodIds", "[J", "methodids_in_reflectclass"},
            {"org.mini.reflect.ReflectClass", "interfaces", "[J", "interfaces_in_reflectclass"},
            {"org.mini.reflect.ReflectField", "fieldName", "Ljava/lang/String;", "name_in_reflectfield"},
            {"org.mini.reflect.ReflectField", "descriptor", "Ljava/lang/String;", "descriptor_in_reflectfield"},
            {"org.mini.reflect.ReflectField", "signature", "Ljava/lang/String;", "signature_in_reflectfield"},
            {"org.mini.reflect.ReflectField", "accessFlags", "S", "access_in_reflectfield"},
            {"org.mini.reflect.ReflectField", "type", "B", "type_in_reflectfield"},
            {"org.mini.reflect.ReflectMethod", "methodName", "Ljava/lang/String;", "name_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "descriptor", "Ljava/lang/String;", "descriptor_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "signature", "Ljava/lang/String;", "signature_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "accessFlags", "S", "access_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "codeStart", "J", "codestart_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "codeEnd", "J", "codeend_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "lines", "I", "lines_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "lineNum", "[S", "linenum_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "argCnt", "I", "argcnt_in_reflectmethod"},
            {"org.mini.reflect.ReflectMethod", "localVarTable", "[Lorg/mini/reflect/LocalVarTable;", "locals_in_reflectmethod"},
    };

    public static void main(String[] args) throws IOException {
        String jsrcPath = "../../minijvm/java/src/main/java/"
                + File.pathSeparator + "../../test/minijvm_test/src/main/java/"//
//                + File.pathSeparator + "../../../janino-continue/commons-compiler/src/main/java"//
//                + File.pathSeparator + "../../../janino-continue/janino/src/main/java"//
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
        try {
            AssistLLVM.convert(jsrcPath, classesPath, csrcPath, microDefFields);
        } finally {
            com.ebsee.compiler.MyCompiler.printSkippedSources();
        }
        System.out.println("convert success , cost :" + (System.currentTimeMillis() - startAt));
    }

}


