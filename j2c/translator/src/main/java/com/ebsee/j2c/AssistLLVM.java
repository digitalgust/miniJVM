package com.ebsee.j2c;

import com.ebsee.classparser.ClassFile;
import com.ebsee.classparser.CodeAttribute;
import com.ebsee.classparser.Field;
import com.ebsee.classparser.Method;
import com.ebsee.compiler.MyCompiler;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;

public class AssistLLVM {
    //common
    static final String INDEX_TEMP = "\0index\0";
    static final String FUNC_CONSTRUCT_STRING_WITH_UTF_INDEX = "construct_string_with_utfraw_index";
    static final String FUNC_NEW_INSTANCE_WITH_RAWINDEX = "new_instance_with_classraw_index";
    static final String FUNC_NEW_INSTANCE_WITH_RAWINDEX_AND_INIT = "new_instance_with_classraw_index_and_init";
    static final String FUNC_MULTI_ARRAY_CREATE = "multi_array_create";
    static final String FUNC_THROW_EXCEPTION = "throw_exception";
    static final String FUNC_JTHREAD_LOCK = "jthread_lock";
    static final String FUNC_JTHREAD_UNLOCK = "jthread_unlock";
    static final String FUNC_METHOD_ENTER = "method_enter";
    static final String FUNC_METHOD_EXIT = "method_exit";
    static final String FUNC_GET_STACKFRAME_EXCEPTION = "get_stackframe_exception";
    static final String FUNC_FIND_EXCEPTION_HANDLER_INDEX = "find_exception_handler_index";
    static final String FUNC_CHECKCAST = "checkcast";
    static final String FUNC_INSTANCE_OF = "instance_of_classname_index";
    static final String FUNC_CHECK_SUSPEND_AND_PAUSE = "check_suspend_and_pause";
    static final String FUNC_FIND_METHOD = "find_method";
    static final String FUNC_EXCEPTION_CHECK = "exception_check";
    static final String FUNC_ARRAY_CLASS_CREATE_GET = "array_class_create_get";
    static final String FUNC_GET_UTF_8_STR_BY_UTFRAW_INDEX = "get_utf8str_by_utfraw_index";


    static Set<Method> nativemethod = new LinkedHashSet<>();//native
    //llfile
    static Set<String> defines = new LinkedHashSet<>();//typedef
    static Set<String> declares = new HashSet<>();//extern

    static List<String> strings = new ArrayList<>();
    static Map<String, Integer> str2index = new HashMap();

    //metafile
    static List<String> classraw = new ArrayList<>();
    static Map<String, Integer> classraw2index = new HashMap();

    static List<String> methodraw = new ArrayList<>();
    static Map<String, Integer> methodraw2index = new HashMap();

    static List<String> fieldraw = new ArrayList<>();
    static Map<String, Integer> fieldraw2index = new HashMap();

    static Set<String> lambdaClasses = new LinkedHashSet<>();//lambda classes path


    static Map<Integer, String> clinitMethods = new LinkedHashMap<>();

    static Map<String, Set<Integer>> class2dependence = new HashMap();

    static int interfaceCount = 0;

    static String jsrcPath;
    static String classesPath;
    static String csrcPath;
    static String[][] microDefFields;

    static public void convert(String pjsrcPath, String pclassesPath, String pcsrcPath, String[][] pmicroDefFields) throws IOException {
        jsrcPath = pjsrcPath;
        classesPath = pclassesPath;
        csrcPath = pcsrcPath;
        microDefFields = pmicroDefFields;


        javaSrc2class(jsrcPath, classesPath);

        class2c(classesPath, csrcPath);

//        conv("java.lang.Object", classesPath, csrcPath);
//        conv("java.io.PrintStream", classesPath, csrcPath);
//        conv("java.lang.System", classesPath, csrcPath);
//        conv("java.lang.Throwable", classesPath, csrcPath);
//        conv("java.lang.NullPointerException", classesPath, csrcPath);
//        conv("java.lang.String", classesPath, csrcPath);
//        conv("java.lang.StringBuilder", classesPath, csrcPath);
//        conv("test.Test", classesPath, csrcPath);
//        conv("test.TestParent", classesPath, csrcPath);

        //gen clinit call
        AssistLLVM.genSourceFile(csrcPath);
    }

    static void javaSrc2class(String srcPath, String classesPath) throws IOException {

        File f = new File(classesPath);
        if (!f.exists()) {
            f.mkdirs();
        }
        MyCompiler.compile(srcPath, classesPath);
        List<String> files = new ArrayList<>();
        MyCompiler.find(classesPath, files, null, ".class");

        ClassManger.init(files);

    }

    static void class2c(String classesPath, String cPath) throws IOException {
        List<String> files = new ArrayList<>();
        MyCompiler.find(classesPath, files, null, ".class");
        Collections.sort(files);

        String classesAbsPath = new File(classesPath).getAbsolutePath();
        for (String cp : files) {
            String className = cp.substring(classesAbsPath.length() + 1);
            className = className.replaceAll("[\\\\/]{1,}", ".");
            className = className.replace(".class", "");
            conv(className, classesPath, cPath);
        }
        //process lambda
        for (String className : lambdaClasses) {
            conv(className, classesPath, cPath);
        }
        System.out.println("converted classes :" + (files.size() + lambdaClasses.size()));
    }

    static void conv(String className, String classesPath, String cPath) throws IOException {

        String outFileName = className.replace("_", "_0005f").replace("$", "_") + ".c";
        File outFileDir = new File(cPath);
        if (!outFileDir.exists()) {
            outFileDir.mkdirs();
        }
        File outfile = new File(cPath, outFileName);
        PrintStream ps = new PrintStream(outfile);
        CV cv = new CV(ps);

        // read class
        String fn = classesPath + className.replace('.', '/') + ".class";
        System.out.println("class convert to c:" + outfile);
        InputStream is = new FileInputStream(fn);
        ClassReader cr = new ClassReader(is);
        cr.accept(cv, 0);
        ps.flush();
        is.close();


    }

    public static void addLambdaClass(String className, byte[] bytes) {
        lambdaClasses.add(className);
        String path = classesPath + "/" + className + ".class";
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(bytes);
            fos.close();
        } catch (IOException iOException) {
            iOException.printStackTrace();
        }
        ClassManger.addClassFile(path);
    }


    static public void genSourceFile(String outpath) {
        genMetaDataH(outpath);
        genMetaDataC(outpath);
        genJNI(outpath);
    }

    static private void genMetaDataC(String outpath) {
        try {
            File f = new File(outpath + "/metadata.c");
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);

            ps.println("#include <stdlib.h>");
            ps.println("#include \"metadata.h\"\n\n");

            int i = 0;

            //strings
            ps.println("const s32 g_strings_count = " + strings.size() + ";");
            ps.println("// utf8_size, utf16_size, utf8_str, java_str");
            ps.println("UtfRaw g_strings[] = {");
            for (String s : strings) {
                ps.println(s + " // " + Integer.toHexString(i) + " " + i++);
            }
            ps.println("};\n");

            i = 0;
            ps.println("const s32 g_fields_count = " + fieldraw.size() + ";");
            ps.println("//name, desc, signature_name, class_name, access, offset_ins, static_ptr");
            ps.println("FieldRaw g_fields[] = {");
            for (String s : fieldraw) {
                ps.println(s + " " + i++);
            }
            ps.println("};\n");

            i = 0;
            ps.println("const s32 g_methods_count = " + methodraw.size() + ";");
            ps.println("//index, name, desc, signature_name, class_name, bytecode, access, max_stack, max_local, func_ptr, func_bridge, exception");
            ps.println("MethodRaw g_methods[] = {");
            for (String s : methodraw) {
                ps.println(s + " // " + i++);
            }
            ps.println("};\n");

            i = 0;
            ps.println("const s32 g_classes_count = " + classraw.size() + ";");
            ps.println("//name, super_name, source_name, signature_name, acc_flag, interface_name_arr, method_arr, field_arr");
            ps.println("ClassRaw g_classes[] = {");
            for (String s : classraw) {
                ps.println(s + " // " + i++);
            }
            ps.println("};\n");


            ps.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("generate clinits.ll error");
        }
    }

    static private void genMetaDataH(String outpath) {
        String genfile = "/metadata.h";
        try {
            File f = new File(outpath + genfile);
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);

            //declare
            ps.println("#include <stdlib.h>");
            ps.println("#include \"../../vm/jvm.h\"");
            ps.println("\n");

            genTypeDef(ps);
            ps.println("\n");

            ps.println("//define class struct");
            for (String s : defines) {
                ps.println(s);
            }
            ps.println("\n");

            for (String s : declares) {
                ps.println(s);
            }
            ps.println();


            ps.println("\n");
            ps.println("\n");


            ps.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("generate " + genfile + " error");
        }
    }

    static private void genTypeDef(PrintStream ps) {
        String[][] ctypeArr = {
                {"java.lang.String", "java_lang_String"},
                {"java.lang.Object", "java_lang_Object"},
                {"java.lang.Class", "java_lang_Class"},
                {"java.lang.ClassLoader", "java_lang_ClassLoader"},
                {"java.lang.Thread", "java_lang_Thread"},
                {"java.lang.StackTraceElement", "java_lang_StackTraceElement"},
        };


        ps.println("// typedef classname");
        for (String[] pair : ctypeArr) {
            ps.println("typedef " + Util.getClassStructType(pair[0]) + " " + pair[1] + ";");
        }

        String[][] staticFieldArr = {
                {"java.lang.System", "java_lang_System_static"},
        };
        ps.println("// typedef static filed struct name ");
        for (String[] pair : staticFieldArr) {
            ps.println("typedef " + Util.getStaticFieldStructType(pair[0]) + " " + pair[1] + ";");
        }

        ps.println("// define filed name ");
        if (microDefFields != null) {
            for (String[] pair : microDefFields) {
                Field field = ClassManger.findField(pair[0].replace('.', '/'), pair[1], pair[2]);
                if (field == null) {
                    System.out.println("field not found:" + pair[0] + " " + pair[1] + " " + pair[2]);
                }
                ps.println("#define " + pair[3] + " " + Util.getFieldVarName(field));
            }
        }

    }

    static private void genJNI(String outpath) {
        try {
            File f = new File(outpath + "/native_gen.txt");
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);

            //declare
            ps.println("#include <stdlib.h>");
            ps.println("#include \"../../vm/jvm.h\"");

            ps.println("//native methods");
            Method[] sorted = nativemethod.toArray(new Method[nativemethod.size()]);
            Arrays.sort(sorted, (t0, t1) -> {
                return (t0.getClassFile().getThisClassName() + t0.getMethodName()).compareTo(t1.getClassFile().getThisClassName() + t1.getMethodName());
            });
            for (Method m : sorted) {
                JSignature sig = new JSignature(m);
                String rawName = Util.getMethodRawName(m.getClassFile().getThisClassName(), m.getMethodName(), sig.getJavaSignature());
                ps.println(sig.getCTypeOfResult() + " " + rawName + "(" + sig.getCTypeArgsString() + "){\n    return " + Util.getDefValue_by_Ctype(sig.getCTypeOfResult()) + ";\n}");
                ps.println("\n");
            }

            ps.println("\n");
            ps.println("\n");


            ps.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("generate clinits.ll error");
        }
    }

    static public void addClinit(String s, int i) {
        clinitMethods.put(i, s);
    }

    static private int addGlobalStringDefine(String src) {
        if (src == null) return -1;
        try {
            char[] carr = src.toCharArray();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF(src);
            dos.close();

            byte[] barr = baos.toByteArray();//src.getBytes("utf-8");
            String dest = "    {" + (barr.length - 2) + ", " + carr.length + ", \"";
            for (int i = 2; i < barr.length; i++) {
                byte sb = barr[i];
                if (sb == '"') {
                    dest += "\\\"";
                } else if (sb == '\\') {
                    dest += "\\\\";
                } else if (sb < 32 || sb > 126) {
                    String s = "00" + Integer.toString(sb & 0xff, 8);
                    dest += "\\";
                    dest += s.substring(s.length() - 3);
                } else {
                    dest += (char) sb;
                }
            }
            dest += "\", NULL, NULL},";
            Integer i = str2index.get(src);
            if (i == null) {
                strings.add(dest);
                int index = strings.indexOf(dest);
                str2index.put(src, index);
                i = index;
            }
            return i;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    static public int getStrIndex(String src) {
        if (!str2index.containsKey(src)) {
            return addGlobalStringDefine(src);
        }
        return str2index.get(src);
    }


    static private int addMethodInfo(Method m) {
        String className = m.getClassFile().getThisClassName();
        JSignature sig = new JSignature(m);
        CodeAttribute ca = m.getCodeAttribute();
        String methodinfo = "    {";
        methodinfo += INDEX_TEMP;//would be replace
        methodinfo += ", ";
        methodinfo += addGlobalStringDefine(m.getMethodName());
        methodinfo += ", ";
        methodinfo += addGlobalStringDefine(m.getDescriptor());
        methodinfo += ", ";
        methodinfo += addGlobalStringDefine(m.getSignature());
        methodinfo += ", ";
        methodinfo += addGlobalStringDefine(m.getClassFile().getThisClassName());//
        methodinfo += ", ";
        methodinfo += addGlobalStringDefine("0");//bytecode
        methodinfo += ", ";
        methodinfo += m.getAccessFlags();
        methodinfo += ", ";
        methodinfo += ca == null ? 0 : ca.getMaxStack();
        methodinfo += ", ";
        methodinfo += ca == null ? 0 : ca.getMaxLocals();
        methodinfo += ", ";
        String rawName = Util.getMethodRawName(className, m.getMethodName(), sig.getJavaSignature());
        declares.add("extern " + sig.getCTypeOfResult() + " " + rawName + "(" + sig.getCTypeArgsString() + ");");
        if (m.isNative()) {
            //nativemethod.add(sig.getCTypeOfResult() + " " + rawName + "(" + sig.getCTypeArgsString() + "){\n    return " + Util.getDefValueByCType(sig.getCTypeOfResult()) + ";\n}");
            nativemethod.add(m);
        }
        methodinfo += rawName;
        methodinfo += ", ";
        declares.add("extern " + Util.getBridgeMethodDeclare(className, m.getMethodName(), sig.getJavaSignature()) + ";");
        methodinfo += Util.getBridgeMethodName(className, m.getMethodName(), sig.getJavaSignature());
        methodinfo += ", ";

        int exTabLen = 0;
        if (m.getCodeAttribute() != null) {
            exTabLen = m.getCodeAttribute().getExceptionTableLength();
        }
        if (m.isNative() || exTabLen == 0) {
            methodinfo += "NULL";
        } else {
            String extabName = Util.getExceptionTableRawName(className, m.getMethodName(), sig);
            declares.add("extern " + Util.STR_EXCEPTIONTABLE_TYPE_NAME + " " + extabName + ";");
            methodinfo += "&" + extabName;
        }
        methodinfo += "},";
        String s = m.getClassFile().getThisClassName() + "." + m.getMethodName() + m.getDescriptor();
        Integer i = methodraw2index.get(s);
        if (i == null) {
            methodraw.add(methodinfo);
            i = methodraw.indexOf(methodinfo);
            methodinfo = methodinfo.replace(INDEX_TEMP, i.toString());
            methodraw.set(i, methodinfo);
            methodraw2index.put(s, i);

        }
        return i;
    }

    static public int getMethodIndex(Method method) {
        return getMethodIndex(method.getClassFile().getThisClassName(), method.getMethodName(), method.getDescriptor());
    }

    static public int getMethodIndex(String className, String methodName, String signature) {
        String s = className + "." + methodName + signature;
        Integer i = methodraw2index.get(s);
        if (i == null) {
            Method m = ClassManger.findMethod(className, methodName, signature);
            i = addMethodInfo(m);
        }
        return i;
    }

    static private int addFieldInfo(Field f) {
        String className = f.getClassFile().getThisClassName();
        String fieldinfo = "    {";
        fieldinfo += addGlobalStringDefine(f.getFieldName());
        fieldinfo += ", ";
        fieldinfo += addGlobalStringDefine(f.getDescriptor());
        fieldinfo += ", ";
        fieldinfo += addGlobalStringDefine(f.getSignature());
        fieldinfo += ", ";
        fieldinfo += addGlobalStringDefine(f.getClassFile().getThisClassName());//
        fieldinfo += ", ";
        fieldinfo += f.getAccessFlags();
        fieldinfo += ", ";
        if ((f.getAccessFlags() & Modifier.STATIC) != 0) {
            fieldinfo += "offsetof(" + Util.getStaticFieldStructType(className) + "," + Util.getFieldVarName(f) + ")";
        } else {
            fieldinfo += "offsetof(" + Util.getClassStructType(className) + "," + Util.getFieldVarName(f) + ")";
        }
        fieldinfo += ", ";
        fieldinfo += "},";
        String s = className + "." + f.getFieldName() + " " + f.getDescriptor();

        fieldinfo += "// ";
        Integer i = fieldraw2index.get(s);
        if (i == null) {
            fieldraw.add(fieldinfo);
            i = fieldraw.indexOf(fieldinfo);
            fieldraw2index.put(s, i);
        }
        return i;
    }


    static public int getFieldIndex(String className, String fieldName, String signature) {
        String s = className + "." + fieldName + signature;
        Integer i = fieldraw2index.get(s);
        if (i == null) {
            Field f = ClassManger.findField(fieldName, signature, className);
            i = addFieldInfo(f);
        }
        return i;
    }

    static private String getClassInfo(ClassFile cf) {
        String className = cf.getThisClassName();

        List<Integer> methods = new ArrayList<>();
        for (Method m : cf.getMethods()) {
            int mindex = getMethodIndex(m);
            methods.add(mindex);
        }
//        if (className.equals("java/lang/Thread")) {
//            int debug = 1;
//        }
        List<Integer> fields = new ArrayList<>();
        for (Field m : cf.getFields()) {
            int findex = addFieldInfo(m);
            fields.add(findex);
        }

        List<Integer> interfaces = new ArrayList<>();
        for (String s : cf.getInterfaceClasses()) {
            int findex = addGlobalStringDefine(s);
            interfaces.add(findex);
        }

        String classinfo = "    {";
        classinfo += getClassIndex(className);
        classinfo += ", ";
        classinfo += "NULL";
        classinfo += ", ";
        classinfo += addGlobalStringDefine(className);
        classinfo += ", ";
        classinfo += cf.getSuperClassName() == null ? -1 : addGlobalStringDefine(cf.getSuperClassName());
        classinfo += ", ";
        classinfo += addGlobalStringDefine(cf.getSourceFile());//source
        classinfo += ", ";
        classinfo += addGlobalStringDefine(cf.getSignature());//
        classinfo += ", ";
        classinfo += cf.getAccessFlags();//
        classinfo += ", ";
        classinfo += addGlobalStringDefine(interfaces.toString());//
        classinfo += ", ";
        classinfo += addGlobalStringDefine(methods.toString());//
        classinfo += ", ";
        classinfo += addGlobalStringDefine(fields.toString());//
        classinfo += ", ";
        addClassDependence(className, null);
//        if (className.equals("java/lang/Integer")) {
//            Set<Integer> set = class2dependence.get(className);
//            int debug = 1;
//        }
        classinfo += addGlobalStringDefine(class2dependence.get(className).toString());//
        classinfo += ", ";

        classinfo += "sizeof(" + Util.getClassStructType(className) + ")";
        classinfo += ", ";
        classinfo += ClassManger.getMethodTree(className).size();//
        classinfo += ", ";
        String declare1 = Util.getVMTableName(className);
        declares.add("extern VMTable " + declare1 + "[];");
        classinfo += declare1;
        classinfo += ", ";
        Method finalizeM = ClassManger.findFinalizeMethod(className);
        classinfo += finalizeM == null ? "NULL" : Util.getMethodRawName(finalizeM.getClassFile().getThisClassName(), finalizeM.getMethodName(), finalizeM.getDescriptor());
        classinfo += ", ";
        classinfo += "&" + Util.getStaticFieldStructVarName(className);
        classinfo += "},";
        classinfo += "// ";


        return classinfo;
    }

    static public int getClassIndex(String className) {
        Integer i = classraw2index.get(className);
        if (i == null) {
            classraw.add(className);
            i = classraw.indexOf(className);
            classraw2index.put(className, i);
        }
        return i;
    }

    static public void updateClassInfo(String className) {
        Integer i = getClassIndex(className);
        ClassFile f = ClassManger.getClassFile(className);
        classraw.set(i, getClassInfo(f));
    }

    static public void addDefine(String s) {
        defines.add(s);
    }


    static public void addClassDependence(String className, String depClassName) {
        Set<Integer> set = class2dependence.get(className);
        if (set == null) {
            set = new LinkedHashSet<>();
            class2dependence.put(className, set);
        }
        if (depClassName != null && !className.equals(depClassName)) {
            set.add(getStrIndex(depClassName));
            //System.out.println(className + " -> " + depClassName);
        }
    }


    public int getInterfaceCount() {
        return interfaceCount;
    }


}
