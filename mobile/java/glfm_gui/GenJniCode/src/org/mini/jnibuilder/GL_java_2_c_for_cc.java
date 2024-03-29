/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.jnibuilder;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.mini.jnibuilder.Util.isPointer;
import static org.mini.jnibuilder.Util.isTypes;

/**
 * @author gust
 */
public class GL_java_2_c_for_cc {

    public static void main(String[] args) {
        GL_java_2_c_for_cc gt = new GL_java_2_c_for_cc();
        gt.buildC();
    }

    String[] path = {"src/main/java/org/mini/gl/GL.java", "org.mini.gl.GL", "org/mini/gl/GL", "../../../j2c/app/gui/jni_gl.c"};

    String[] ignore_list = {"",
            "",};

    String C_BODY_HEADER
            = ""//
            + "/*this file generated by GL_java_2_c_for_cc.java ,dont modify it manual.*/\n"
            + "#include <stdio.h>\n"
            + "#include <string.h>\n"
            + "\n"
            + "#include <glad.h>\n"
            + "\n"
            + "#include \"../out/c/metadata.h\"\n"
            + "\n"
            + "#include \"jvm.h\"\n"
            + "#include \"media.h\"\n"
            + "\n";

    String TOOL_FUNC
            = //
            "s32 count_GLFuncTable() {\n"
                    + "    return sizeof(method_gl_table) / sizeof(java_native_method);\n"
                    + "}\n"
                    + "\n"
                    + "__refer ptr_GLFuncTable() {\n"
                    + "    return &method_gl_table[0];\n"
                    + "}";
    String FUNC_TABLE_HEADER = "static java_native_method method_gl_table[] = {\n\n";
    String FUNC_TABLE_FOOTER = "};\n\n";

    String FUNC_BODY_TEMPLATE
            = //
            "${DEF_RETURN_TYPE} ${DEF_METHOD_NAME}(JThreadRuntime *runtime${PARA_LIST}) {\n"
                    + "    \n${GET_VAR}\n"
                    + "    ${RETURN_TYPE}${METHOD_NAME}(${NATIVE_ARGV});\n"
                    + "    ${PUSH_RESULT}\n"
                    + "    ${RELEASE_MEM}\n"
                    + "}\n";
    String PKG_PATH = "${PKG_PATH}";
    String PARA_LIST = "${PARA_LIST}";
    String METHOD_NAME = "${METHOD_NAME}";
    String DEF_METHOD_NAME = "${DEF_METHOD_NAME}";
    String GET_VAR = "${GET_VAR}";
    String RETURN_TYPE = "${RETURN_TYPE}";
    String DEF_RETURN_TYPE = "${DEF_RETURN_TYPE}";
    String NATIVE_ARGV = "${NATIVE_ARGV}";
    String JAVA_ARGV = "${JAVA_ARGV}";
    String JAVA_RETURN = "${JAVA_RETURN}";
    String PUSH_RESULT = "${PUSH_RESULT}";
    String RELEASE_MEM = "${RELEASE_MEM}";

    String VOID = "void";

    String FUNC_TABLE_TEMPLATE = "{\"${PKG_PATH}\",  \"${METHOD_NAME}\",  \"(${JAVA_ARGV})${JAVA_RETURN}\",  ${PKG_NAME}${METHOD_NAME}},";

    void buildC() {
        BufferedReader br = null;
        BufferedWriter bw = null;
        List<String> funcTable = new ArrayList();
        try {
            File ifile = new File(path[0]);
            br = new BufferedReader(new FileReader(ifile));
            System.out.println("open input file:" + ifile.getAbsolutePath());
            File ofile = new File(path[3]);
            bw = new BufferedWriter(new FileWriter(ofile));
            System.out.println("open output file:" + ofile.getAbsolutePath());
            bw.write(C_BODY_HEADER);
            String line, whole;
            String header = "public static native";
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                whole = new String(line.getBytes());
                if (line.startsWith(header)) {

                    String nativeArgvType = line.substring(line.indexOf("//") + 2, line.lastIndexOf("//")).trim();
                    String nativeReurnType = line.substring(line.lastIndexOf("//") + 2).trim();
                    String[] nativeArgvs = nativeArgvType.split(",");
                    line = line.substring(header.length()).trim();
                    String returnType = line.substring(0, line.indexOf(' ')).trim();
                    line = line.substring(returnType.length()).trim();
                    String methodName = line.substring(0, line.indexOf('('));
                    line = line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim();
                    String[] argvs = line.split(",");


                    //
                    String funcBodyCode = new String(FUNC_BODY_TEMPLATE.getBytes());
                    String funcTableLine = new String(FUNC_TABLE_TEMPLATE.getBytes());
                    funcTableLine = funcTableLine.replace(METHOD_NAME, methodName);
                    funcTableLine = funcTableLine.replace(PKG_PATH, path[2]);

                    //process return 
                    String returnCode = "", pushCode = "", javaReturnCode = "", releaseMemCode = "", defReturnCode = "";
                    if (nativeReurnType.length() == 0) {
                        int debug = 1;
                    }
                    boolean nativeReturnIsPointer = nativeReurnType.charAt(nativeReurnType.length() - 1) == '*';//最后一个是*号

                    if (!VOID.equals(returnType)) {
                        if ("int".equals(returnType)) {
                            defReturnCode = "s32";
                            returnCode = nativeReurnType + " _re_val = ";
                            pushCode += "return *((s32*)&_re_val);";
                            javaReturnCode = "I";
                        } else if ("float".equals(returnType)) {
                            defReturnCode = "f32";
                            returnCode = "return (f32)";
                            javaReturnCode = "F";
                        } else if ("double".equals(returnType)) {
                            defReturnCode = "f64";
                            returnCode = "return (f64)";
                            javaReturnCode = "D";
                        } else if ("byte".equals(returnType)) {
                            defReturnCode = "s8";
                            returnCode = "return (s8)";
                            javaReturnCode = "B";
                        } else if ("short".equals(returnType)) {
                            defReturnCode = "s16";
                            returnCode = "return (s16)";
                            javaReturnCode = "S";
                        } else if ("boolean".equals(returnType)) {
                            defReturnCode = "s32";
                            returnCode = "return (u8)";
                            javaReturnCode = "Z";
                        } else if ("long".equals(returnType)) {
                            defReturnCode = "s64";
                            returnCode = nativeReurnType + " _re_val = ";
                            pushCode += "return *((s64*)&_re_val);";
                            javaReturnCode = "J";
                        } else if ("String".equals(returnType)) {
                            defReturnCode = "JObject *";
                            if (nativeReturnIsPointer) {
                                returnCode = "c8* _cstr = (c8*)";
                            } else {
                                returnCode = "c8* _cstr = (c8*)&";
                            }
                            pushCode = "    JObject *jstr = construct_string_with_cstr(runtime, _cstr);\n    return jstr;";
                            javaReturnCode = "Ljava/lang/String;";
                        } else if (returnType.contains("[]")) {
                            String cType = "", jvmType = "", jvmDesc = "";
//                            if ("long[]".equals(returnType)) {
//                                cType = "s64";
//                                jvmType = "DATATYPE_LONG";
//                                jvmDesc = "[J";
//                            } else if ("int[]".equals(returnType)) {
//                                cType = "s32";
//                                jvmType = "DATATYPE_INT";
//                                jvmDesc = "[I";
//                            } else if ("float[]".equals(returnType)) {
//                                cType = "f32";
//                                jvmType = "DATATYPE_FLOAT";
//                                jvmDesc = "[F";
//                            } else 
                            if ("byte[]".equals(returnType)) {
                                cType = "c8";
                                jvmDesc = "[B";
                            } else {
                                System.out.println(" " + lineNo + " return type:" + returnType + " in :" + whole);
                            }

                            //impl
                            defReturnCode = "JArray *";
                            String entryType = nativeReurnType;//计算实体字节数，不能算指针大小
                            returnCode = nativeReurnType + " _re_val = ";
                            if (nativeReturnIsPointer) {
                                pushCode += cType + "* _ptr_re_val = (" + cType + "*)_re_val;\n";
                                entryType = nativeReurnType.substring(0, nativeReurnType.length() - 1);
                            } else {
                                pushCode += cType + "* _ptr_re_val = (" + cType + "*)&_re_val;\n";
                            }
                            pushCode += "    if (_ptr_re_val) {\n"
                                    + "        s32 bytes = strlen(_ptr_re_val);\n"
                                    + "        s32 j_t_bytes = sizeof(" + cType + ") * bytes;\n"
                                    + "        JArray *_arr = multi_array_create_by_typename(runtime, &j_t_bytes, 1, \"[B\");\n"
                                    + "        memcpy(_arr->prop.as_c8_arr, _ptr_re_val , j_t_bytes);\n"
                                    + "        return _arr;\n"
                                    + "    } else {\n"
                                    + "        return NULL;\n"
                                    + "    }";
                            javaReturnCode = jvmDesc;
                        } else {
                            System.out.println(" " + lineNo + " return type:" + returnType + " in :" + whole);
                        }
                    } else {
                        defReturnCode = "void";
                        javaReturnCode = "V";
                    }

                    funcBodyCode = funcBodyCode.replace(DEF_RETURN_TYPE, defReturnCode);
                    funcBodyCode = funcBodyCode.replace(RETURN_TYPE, returnCode);
                    funcBodyCode = funcBodyCode.replace(PUSH_RESULT, pushCode);
                    funcTableLine = funcTableLine.replace(JAVA_RETURN, javaReturnCode);

                    //process body
                    String varCode = "";
                    String nativeArgvCode = "";
                    String nativeParaCode = "";
                    String javaArgvCode = "";
                    for (int i = 0, nativei = 0; i < argvs.length; i++, nativei++) {
                        String argv = argvs[i].trim();
                        if (argv.length() == 0) {
                            continue;
                        }
                        String[] tmps = argv.trim().split(" ");
                        String argvType = tmps[0].trim();
                        String argvName = tmps[1].trim();
                        if (nativei >= nativeArgvs.length) {
                            int debug = 1;
                        }

                        //
                        nativeArgvCode += nativeArgvCode.length() > 0 ? ", " : "";
                        String curArgvType = "(" + nativeArgvs[nativei] + ")";
                        String curArgvName = "";

                        if ("int".equals(argvType)) {
                            nativeParaCode += ", s32 " + argvName;
                            curArgvName = argvName;
                            javaArgvCode += "I";
                            if (!isPointer(nativeArgvs[nativei])) {
                                curArgvType = "(" + nativeArgvs[nativei] + ")";
                            }
                        } else if ("short".equals(argvType)) {
                            nativeParaCode += ", s16 " + argvName;
                            curArgvName = argvName;
                            javaArgvCode += "S";
                        } else if ("byte".equals(argvType)) {
                            nativeParaCode += ", s8 " + argvName;
                            curArgvName = argvName;
                            javaArgvCode += "B";
                        } else if ("boolean".equals(argvType)) {
                            nativeParaCode += ", s32 " + argvName;
                            curArgvName = argvName;
                            javaArgvCode += "Z";

                        } else if ("long".equals(argvType)) {
                            nativeParaCode += ", s64 " + argvName;
                            curArgvName = argvName;
                            javaArgvCode += "J";
                        } else if ("float".equals(argvType)) {
                            nativeParaCode += ", f32 " + argvName;
                            curArgvName = argvName;
                            javaArgvCode += "F";
                        } else if ("double".equals(argvType)) {
                            nativeParaCode += ", f64 " + argvName;
                            curArgvName = argvName;
                            javaArgvCode += "D";
                        } else if ("String".equals(argvType)) {
                            nativeParaCode += ", struct java_lang_String *" + argvName;
                            varCode += "    u16* carr_" + argvName + " = " + argvName + "->value_0->prop.as_u16_arr;\n";
                            varCode += "    s32 offset_" + argvName + " = " + argvName + "->offset_1;\n";
                            varCode += "    s32 count_" + argvName + " = " + argvName + "->count_2;\n";
                            varCode += "    __refer ptr_" + argvName + " = NULL;\n";
                            varCode += "    Utf8String *u_" + argvName + ";\n";
                            varCode += "    if(" + argvName + "){\n";
                            varCode += "        u_" + argvName + " = utf8_create();\n";
                            varCode += "        unicode_2_utf8(&carr_" + argvName + "[offset_" + argvName + "], u_" + argvName + ",count_" + argvName + ");\n";
                            varCode += "        ptr_" + argvName + " = utf8_cstr(u_" + argvName + ");\n";
                            varCode += "    }\n";
                            curArgvName = "(ptr_" + argvName + ")";
                            releaseMemCode += "utf8_destory(u_" + argvName + ");";
                            javaArgvCode += "Ljava/lang/String;";
                        } else if ("byte[][]".equals(argvType) || "byte[]...".equals(argvType)) {
                            nativeParaCode += ", JArray *" + argvName;
                            varCode += "    __refer ptr_" + argvName + "[" + argvName + "->prop.arr_length];\n";
                            varCode += "    s32 i;for(i=0;i<" + argvName + "->prop.arr_length;i++){\n";
                            varCode += "        JArray *arr_" + argvName + " = (JArray *)" + argvName + "->prop.as_obj_arr[i];\n";
                            varCode += "        ptr_" + argvName + "[i] = arr_" + argvName + "->prop.as_c8_arr;\n";
                            varCode += "    }\n";
                            curArgvName = "(ptr_" + argvName + ")";
                            if ("byte[]...".equals(argvType)) {
                                curArgvType = "/*todo Despair for runtime parse unlimited para*/";
                            }
                            javaArgvCode += "[[B";

                        } else if ("Object[]".equals(argvType) || "Object...".equals(argvType)) {
                            nativeParaCode += ", JArray *" + argvName;
                            curArgvName = "(" + argvName + "->prop.as_obj_arr)";
                            if ("Object...".equals(argvType)) {
                                curArgvType = "/*todo Despair for runtime parse unlimited para*/";
                            }
                            javaArgvCode += "[Ljava/lang/Object;";

                        } else if (argvType.indexOf("[]") > 0 || argvType.indexOf("Object") >= 0) {
                            if (argvType.startsWith("byte")) {
                                varCode += "    s32 offset_" + argvName + " = 0;";
                                nativeParaCode += ", JArray *" + argvName;
                            } else if (argvType.startsWith("Object")) {
                                nativeParaCode += ", struct java_lang_Object *" + argvName + ", s32 offset_" + argvName;
                            } else {
                                nativeParaCode += ", JArray *" + argvName + ", s32 offset_" + argvName;

                            }

                            varCode += "    __refer ptr_" + argvName + " = (__refer)(intptr_t) offset_"+argvName+";\n";
                            varCode += "    if(" + argvName + ") {\n";
                            varCode += "        s32 bytes_" + argvName + " = data_type_bytes[" + argvName + "->prop.arr_type] * offset_" + argvName + ";\n";
                            varCode += "        ptr_" + argvName + " = &" + argvName + "->prop.as_s8_arr[bytes_" + argvName + "];\n";
                            varCode += "    }\n";
                            curArgvName = "(ptr_" + argvName + ")";
                            if (argvType.startsWith("int")) {
                                javaArgvCode += "[II";
                            } else if (argvType.startsWith("short")) {
                                javaArgvCode += "[SI";
                            } else if (argvType.startsWith("byte")) {
                                javaArgvCode += "[B";
                            } else if (argvType.startsWith("long")) {
                                javaArgvCode += "[JI";
                            } else if (argvType.startsWith("float")) {
                                javaArgvCode += "[FI";
                            } else if (argvType.startsWith("double")) {
                                javaArgvCode += "[DI";
                            } else if (argvType.startsWith("Object")) {
                                javaArgvCode += "Ljava/lang/Object;I";
                            } else if (argvType.startsWith("boolean")) {
                                javaArgvCode += "[ZI";
                            } else {
                                System.out.println(" " + lineNo + " array type:" + returnType + " in :" + whole);
                            }
                            i++;
                        } else {
                            System.out.println(" " + lineNo + " argv type:" + returnType + " in :" + whole);
                        }
                        nativeArgvCode += curArgvType + curArgvName;
                    }

                    String descript = "(" + javaArgvCode + ")" + javaReturnCode;
                    String defMethodName = method2rawName(path[1], methodName, descript);
                    funcBodyCode = funcBodyCode.replace(DEF_METHOD_NAME, defMethodName);

                    funcBodyCode = funcBodyCode.replace(METHOD_NAME, methodName);
                    funcBodyCode = funcBodyCode.replace(GET_VAR, varCode);
                    funcBodyCode = funcBodyCode.replace(NATIVE_ARGV, nativeArgvCode);
                    funcBodyCode = funcBodyCode.replace(PARA_LIST, nativeParaCode);
                    funcBodyCode = funcBodyCode.replace(RELEASE_MEM, releaseMemCode);
                    funcTableLine = funcTableLine.replace(JAVA_ARGV, javaArgvCode);

                    if (!isTypes(ignore_list, methodName)) {
                        bw.write(funcBodyCode);
                        funcTable.add(funcTableLine);
                    }

                }
            }
            bw.write("\n\n\n");
//            bw.write(FUNC_TABLE_HEADER);
//            for (String s : funcTable) {
//                bw.write(s + "\n");
//            }
//
//            bw.write(FUNC_TABLE_FOOTER);
//            bw.write(TOOL_FUNC);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                br.close();
                bw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("success.");
    }

    public static String method2rawName(String className, String methodName, String signature) {
        StringBuilder result = new StringBuilder();
        result.append("Java");
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

    static final String INNER_CLASS_SPLITOR = "_00024"; //$
    static final String UNDERLINE_SPLITOR = "_1";//;
    static final String SEMICOLON_SPLITOR = "_2";//;
    static final String LEFT_BRACERT_SPLITOR = "_3";//[

    static public String regulString(String s) {
        return s.replace("_", UNDERLINE_SPLITOR).replaceAll("[\\.\\/\\<\\>]", "_").replace("$", INNER_CLASS_SPLITOR).replace(";", SEMICOLON_SPLITOR).replace("[", LEFT_BRACERT_SPLITOR);
    }

}
