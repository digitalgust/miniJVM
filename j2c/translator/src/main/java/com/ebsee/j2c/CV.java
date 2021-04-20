package com.ebsee.j2c;

import com.ebsee.classparser.Method;
import org.objectweb.asm.*;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.*;

public class CV extends ClassVisitor {

    // out
    private PrintStream ps;

    // state
    String className;
    String superName;
    private Set<JField> staticFields = new HashSet<JField>();
    private List<JField> fields = new ArrayList<JField>();

    private List<MV> methods = new ArrayList<MV>();
    // shared states
    Set<String> declares = new HashSet<>();

    Set<String> assistFunc = new HashSet<>();

    public CV(PrintStream ps) {
        super(Opcodes.ASM5);
        this.ps = ps;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.superName = superName;

        AssistLLVM.addClassDependence(className, superName);
        for (String s : interfaces) {
            AssistLLVM.addClassDependence(className, s);
        }
    }

    public void visitSource(String source, String debug) {
//        this.ps.println("src " + source);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        AssistLLVM.addClassDependence(className, owner);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    public void visitAttribute(Attribute attr) {
        this.ps.println(" attr " + attr);
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        AssistLLVM.addClassDependence(className, name);
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if ((access & Opcodes.ACC_STATIC) != 0) {
            Util.getJavaSignatureCtype(desc);
            this.staticFields.add(new JField(className, name, desc));
        } else {
            this.fields.add(new JField(className, name, desc));
//            this.ps.println("  f  " + desc + " " + name + " " + signature + " " + value);
        }
        return null;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        //System.out.println("visitMethod " + access + ", name=" + name + ", desc=" + desc + ", signature=" + signature);
        Method m = ClassManger.findMethod(className, name, desc);
        if (m == null) {
            int debug = 1;
        }
        MV mv = new MV(access, name, desc, this);
        this.methods.add(mv);
        return mv;
    }

    public void visitEnd() {
        AssistLLVM.getClassIndex(className);
        AssistLLVM.addDefine(Util.class2structDefine(className));
        String staticStruct = Util.classStatic2structDefine(className);
        AssistLLVM.addDefine(staticStruct);
        AssistLLVM.addDefine(Util.getStaticFieldExternalDeclare(className));

        this.ps.println("// CLASS: " + this.className + " extends " + this.superName);
        this.ps.println("#include \"metadata.h\"");
        //this.ps.println(AssistLLVM.getAssistFuncDeclare());
        this.ps.println();

        String vitualMethodTableStr = constructVMTable(className);
        // declares
        for (String mdeclare : declares) {
            this.ps.println(mdeclare);
        }
        this.ps.println();

        // use classes
        this.ps.println("// generation");

        // out fields
        this.ps.println("// globals");
        this.ps.println("//" + staticStruct);
        this.ps.println(Util.classStaticInit(className));
        this.ps.println();

        this.ps.println();


        //virtual method table
        this.ps.println(vitualMethodTableStr);
        this.ps.println();

        // out methods
        for (MV method : this.methods) {
            if ((method.access & Modifier.NATIVE) == 0) {
//                ps.println(method.getLabelTable());
                ps.println(method.getExceptionTable());
                method.out(this.ps);
            }
            method.outBridge(this.ps);
        }

        for (String s : assistFunc) {
            this.ps.println(s);
        }
        this.ps.println();

        AssistLLVM.updateClassInfo(className);
    }

    public Set<JField> getStaticFields() {
        return staticFields;
    }


    public String constructVMTable(String className) {
        Map<String, List<Method>> map = ClassManger.getMethodTree(className);

//        if (className.equals("java/lang/String")) {
//            int debug = 1;
//        }
        StringBuilder sb = new StringBuilder();
        String arrName = "arr_" + Util.getVMTableName(className);
        int i = 0;
        for (String key : map.keySet()) {
            List<Method> methods = map.get(key);
            int methodSize = methods.size();

            sb.append("__refer ").append(arrName).append("_from_").append(Util.regulString(key));

            sb.append("[] = {\n");
            for (int j = 0; j < methodSize; j++) {
                Method m = methods.get(j);
                String cName = m.getClassFile().getThisClassName();
                Util.getClassStructTypePtr(cName);
                sb.append("    ").append(Util.getMethodRawName(m));

                if (j < methodSize - 1) {
                    sb.append(",");
                }
                sb.append("  //").append(j).append('\n');
            }
            sb.append("};\n");
            i++;
        }
        String vtName = Util.getVMTableType() + " " + Util.getVMTableName(className);
        sb.append(vtName);
        sb.append("[] = {\n");
        i = 0;
        for (String key : map.keySet()) {
            List<Method> methods = map.get(key);
            sb.append("    {").append(AssistLLVM.getClassIndex(key)).append(", ").append(methods.size()).append(", ").append(arrName).append("_from_").append(Util.regulString(key)).append("}, //").append(i).append('\n');
            i++;
        }
        sb.append("};\n");
        return sb.toString();
    }


}