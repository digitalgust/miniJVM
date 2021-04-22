package com.ebsee.j2c;

import com.ebsee.classparser.CodeAttribute;
import com.ebsee.classparser.Field;
import com.ebsee.classparser.Method;
import com.ebsee.invoke.LambdaMetafactory;
import com.ebsee.invoke.MethodHandle;
import com.ebsee.invoke.bytecode.ByteCodeAssembler;
import org.objectweb.asm.*;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.ebsee.j2c.Util.*;

/**
 *
 */
public class MV extends MethodVisitor {

    static class ExceptionItem {
        public Label startLabel;
        public Label endLabel;
        public Label handlerLabel;
        public String exceptionClassName;
    }

    // parent
    private CV cv;

    // state
    String methodName;
    String signatureStr;
    public int access;

    // arguments
    List<String> _argTypes;
    // res
    String _resType;
    // labels
    Set<String> labels = new LinkedHashSet<>();
    List<String> usedLabels = new ArrayList<>();
    List<ExceptionItem> exceptionTables = new ArrayList<>();
    Map<String, Integer> lineNoTable = new HashMap<>();
    int labelId;
    Label curLabel = new Label(-1);
    int curLineNo = -1;

    JSignature signature;

    private List<String> strings = new ArrayList<String>();

    static final String EXCEPTION_HANDLER_NOT_FOUND = "__ExceptionHandlerNotFound";
    static final String EXCEPTION_HANDLER = "__ExceptionHandler";
    static final String METHOD_EXIT_WITHOUT_EXCEPTION_LABEL = "__NoException";

    int max_local;
    int max_stack;

    int curMethodId;
    Method curMethod;

    public MV(int access, String methodName, String signatureStr, CV cv) {
        super(Opcodes.ASM5);
        this.access = access;
        this.methodName = methodName;
        this.signatureStr = signatureStr;
        this.cv = cv;

        curMethod = ClassManger.findMethod(cv.className, methodName, this.signatureStr);
        // signature
        signature = new JSignature(curMethod);
        _argTypes = signature.getCtypeArgs();
        _resType = signature.getResult();
        // constructor`s implicit parameter
        if (isNative()) {
            String tmps = Util.getMethodDeclare(cv.className, methodName, signature);
            this.cv.declares.add(tmps + ";");
        }

        curMethodId = AssistLLVM.getMethodIndex(cv.className, methodName, signatureStr);
    }


    // =================================================================================================================

    public List<String> getOutputs() {
        return strings;
    }

    public void add(int index, String str) {
        strings.add(index, str);
    }

    public void add(String str) {
        strings.add(str);
    }

    public void addAll(Collection<String> str) {
        strings.addAll(str);
    }

    public void comment(String str) {
        strings.add("// " + str);
    }


    public int nextId() {
        return labelId++;
    }

    boolean isNative() {
        return (access & Modifier.NATIVE) != 0;
    }

    boolean isStatic() {
        return (access & Modifier.STATIC) != 0;
    }

    boolean canSkipCodeTrack() {
        return isMethodGetter(curMethod) || isMethodSetter(curMethod);
    }

    @Override
    public void visitParameter(String s, int i) {
        //System.out.println("visitParameter " + s + " " + i);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        //System.out.println("visitAnnotationDefault");
        return null;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        return super.visitAnnotation(s, b);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int i, TypePath typePath, String s, boolean b) {
        return super.visitTypeAnnotation(i, typePath, s, b);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int i, String s, boolean b) {
        return super.visitParameterAnnotation(i, s, b);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        //System.out.println("visitAttribute " + attribute);
    }

    @Override
    public void visitCode() {

        if (methodName.equals("run_benchmark")) {
            int debug = 1;
        }
        // 1) local vars & args
        int cntSlot = 0, cntArgs = 0;

        for (String s : signature.getCtypeArgs()) {
            String fname = Util.getStackFieldName_by_Ctype(s);
            String vname = Util.getStackName_by_Ctype(s);
            add(vname + "local[" + cntSlot + "]." + fname + " = p" + cntSlot + ";");
            cntArgs++;
            cntSlot++;
            if (s.equals(LONG) || s.equals(DOUBLE)) {
                cntSlot++;
            }
        }
        if (!canSkipCodeTrack()) {
            //add("if (runtime->suspend_count) " + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
        }
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stackitems) {
        //comment("; TODO FRame: " + type + " " + numLocal + " " + numStack);
        //comment("; type " + type + ", local " + numLocal + " " + Arrays.toString(local) + "," + " stack " + numStack + " " + Arrays.toString(stackitems));
        //vars.activeByFrame(type, numLocal, local);

        switch (type) {
            case Opcodes.F_APPEND: { //1

                break;
            }
            case Opcodes.F_CHOP: {//2

                break;
            }
            case Opcodes.F_FULL: {//0
                //System.out.println("expect:" + numStack + "  real:" + stack.size());
                break;
            }
            case Opcodes.F_NEW: {//-1
                break;
            }
            case Opcodes.F_SAME: {//3
                break;
            }
            case Opcodes.F_SAME1: {//4
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            case Opcodes.NOP: // 0
                break;
            // =============================================== Constants ==
            case Opcodes.ACONST_NULL: // 1
                comment("aconst null");
                add("rstack[sp++].obj = NULL;");
                break;
            case Opcodes.ICONST_M1: // 2
            case Opcodes.ICONST_0: // 3
            case Opcodes.ICONST_1: // 4
            case Opcodes.ICONST_2: // 5
            case Opcodes.ICONST_3: // 6
            case Opcodes.ICONST_4: // 7
            case Opcodes.ICONST_5: // 8
            {
                int value = opcode - Opcodes.ICONST_0;
                comment("iconst_" + value);
                add("stack[sp++].i = " + value + ";");
                break;
            }
            case Opcodes.LCONST_0: // 9
            case Opcodes.LCONST_1: // 10
            {
                long value = opcode - Opcodes.LCONST_0;
                comment(" lconst " + value);
                add("stack[sp].j = " + value + ";");
                add("sp += 2;");
                break;
            }
            case Opcodes.FCONST_0: // 11
            case Opcodes.FCONST_1: // 12
            case Opcodes.FCONST_2: // 13
            {
                int value = opcode - Opcodes.FCONST_0;
                comment(" fconst " + value);
                add("stack[sp++].f = " + value + ";");
                break;
            }
            case Opcodes.DCONST_0: // 14
            case Opcodes.DCONST_1: // 15
            {
                int value = opcode - Opcodes.DCONST_0;
                comment(" dconst " + value);
                add("stack[sp].d = " + value + ";");
                add("sp += 2;");
                break;
            }
            // =============================================== Array Load ==
            case Opcodes.IALOAD: // 46
                arrload(INT);
                break;
            case Opcodes.LALOAD: // 47
                arrload(LONG);
                break;
            case Opcodes.FALOAD: // 48
                arrload(FLOAT);
                break;
            case Opcodes.DALOAD: // 49
                arrload(DOUBLE);
                break;
            case Opcodes.AALOAD: // 50
                arrload(POINTER);
                break;
            case Opcodes.BALOAD: // 51
                arrload(BYTE);
                break;
            case Opcodes.CALOAD: // 52
                arrload(CHAR);
                break;
            case Opcodes.SALOAD: // 53
                arrload(SHORT);
                break;
            // =============================================== Array Store ==
            case Opcodes.IASTORE: // 79
                arrstore(INT);
                break;
            case Opcodes.LASTORE: // 80
                arrstore(LONG);
                break;
            case Opcodes.FASTORE: // 81
                arrstore(FLOAT);
                break;
            case Opcodes.DASTORE: // 82
                arrstore(DOUBLE);
                break;
            case Opcodes.AASTORE: // 83
                arrstore(POINTER);
                break;
            case Opcodes.BASTORE: // 84
                arrstore(BYTE); //
                break;
            case Opcodes.CASTORE: // 85
                arrstore(CHAR); //
                break;
            case Opcodes.SASTORE: // 86
                arrstore(SHORT); //
                break;
            // =============================================== Array Store ==
            case Opcodes.POP: // 87
                add("--sp; //pop");
                break;
            case Opcodes.POP2: // 88
                add("sp -= 2; //pop2");
                break;
            case Opcodes.DUP: // 89
            {
                comment("dup");
                add("stack[sp].j = stack[sp - 1].j; ");
                add("rstack[sp].obj = rstack[sp - 1].obj; ");
                add("++sp;");

            }
            break;
            case Opcodes.DUP_X1: // 90
            {
                comment("dup x1");
                add("stack[sp - 0].j = stack[sp - 1].j; ");
                add("stack[sp - 1].j = stack[sp - 2].j; ");
                add("stack[sp - 2].j = stack[sp + 0].j; ");

                add("rstack[sp - 0].obj = rstack[sp - 1].obj; ");
                add("rstack[sp - 1].obj = rstack[sp - 2].obj; ");
                add("rstack[sp - 2].obj = rstack[sp + 0].obj; ");

                add("++sp;");
            }
            break;
            case Opcodes.DUP_X2: // 91
            {
                comment("dup x2");
                add("stack[sp - 0].j = stack[sp - 1].j; ");
                add("stack[sp - 1].j = stack[sp - 2].j; ");
                add("stack[sp - 2].j = stack[sp - 3].j; ");
                add("stack[sp - 3].j = stack[sp + 0].j; ");

                add("rstack[sp - 0].obj = rstack[sp - 1].obj; ");
                add("rstack[sp - 1].obj = rstack[sp - 2].obj; ");
                add("rstack[sp - 2].obj = rstack[sp - 3].obj; ");
                add("rstack[sp - 3].obj = rstack[sp + 0].obj; ");

                add("++sp;");
            }
            break;
            case Opcodes.DUP2: // 92
            {
                comment("; dup2");
                add("stack[sp + 1].j = stack[sp - 1].j; ");
                add("stack[sp + 0].j = stack[sp - 2].j; ");

                add("rstack[sp + 1].obj = rstack[sp - 1].obj; ");
                add("rstack[sp + 0].obj = rstack[sp - 2].obj; ");
                add("sp += 2;");
            }
            break;
            case Opcodes.DUP2_X1: // 93
            {
                comment(" dup2 x1");
                add("stack[sp + 1].j = stack[sp - 1].j; ");
                add("stack[sp + 0].j = stack[sp - 2].j; ");
                add("stack[sp - 1].j = stack[sp - 3].j; ");
                add("stack[sp - 2].j = stack[sp + 1].j; ");
                add("stack[sp - 3].j = stack[sp + 0].j; ");

                add("rstack[sp + 1].obj = rstack[sp - 1].obj; ");
                add("rstack[sp + 0].obj = rstack[sp - 2].obj; ");
                add("rstack[sp - 1].obj = rstack[sp - 3].obj; ");
                add("rstack[sp - 2].obj = rstack[sp + 1].obj; ");
                add("rstack[sp - 3].obj = rstack[sp + 0].obj; ");
                add("sp += 2;");
            }
            break;
            case Opcodes.DUP2_X2: // 94
            {
                comment(" dup2 x2");
                add("stack[sp + 1].j = stack[sp - 1].j; ");
                add("stack[sp + 0].j = stack[sp - 2].j; ");
                add("stack[sp - 1].j = stack[sp - 3].j; ");
                add("stack[sp - 2].j = stack[sp - 4].j; ");
                add("stack[sp - 3].j = stack[sp + 1].j; ");
                add("stack[sp - 4].j = stack[sp + 0].j; ");

                add("rstack[sp + 1].obj = rstack[sp - 1].obj; ");
                add("rstack[sp + 0].obj = rstack[sp - 2].obj; ");
                add("rstack[sp - 1].obj = rstack[sp - 3].obj; ");
                add("rstack[sp - 2].obj = rstack[sp - 4].obj; ");
                add("rstack[sp - 3].obj = rstack[sp + 1].obj; ");
                add("rstack[sp - 4].obj = rstack[sp + 0].obj; ");

                add("sp += 2;");
            }
            break;
            case Opcodes.SWAP: // 95 (Swap only first class values)
            {
                comment("; swap");
                add("stack[sp - 0].j = stack[sp - 1].j; ");
                add("stack[sp - 1].j = stack[sp - 2].j; ");
                add("stack[sp - 2].j = stack[sp - 0].j; ");

                add("rstack[sp - 0].obj = rstack[sp - 1].obj; ");
                add("rstack[sp - 1].obj = rstack[sp - 2].obj; ");
                add("rstack[sp - 2].obj = rstack[sp - 0].obj; ");
            }
            break;
            // =============================================== ADD ==
            case Opcodes.IADD: // 96
                add("stack[sp - 2].i = stack[sp - 2].i + stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LADD: // 97
                add("stack[sp - 4].j = stack[sp - 4].j + stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            case Opcodes.FADD: // 98
                add("stack[sp - 2].f = stack[sp - 2].f + stack[sp - 1].f; ");
                add("--sp;");
                break;
            case Opcodes.DADD: // 99
                add("stack[sp - 4].d = stack[sp - 4].d + stack[sp - 2].d; ");
                add("sp -= 2;");
                break;
            // =============================================== SUB ==
            case Opcodes.ISUB: // 100
                add("stack[sp - 2].i = stack[sp - 2].i - stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LSUB: // 101
                add("stack[sp - 4].j = stack[sp - 4].j - stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            case Opcodes.FSUB: // 102
                add("stack[sp - 2].f = stack[sp - 2].f - stack[sp - 1].f; ");
                add("--sp;");
                break;
            case Opcodes.DSUB: // 103
                add("stack[sp - 4].d = stack[sp - 4].d - stack[sp - 2].d; ");
                add("sp -= 2;");
                break;
            // =============================================== MUL ==
            case Opcodes.IMUL: // 104
                add("stack[sp - 2].i = stack[sp - 2].i * stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LMUL: // 105
                add("stack[sp - 4].j = stack[sp - 4].j * stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            case Opcodes.FMUL: // 106
                add("stack[sp - 2].f = stack[sp - 2].f * stack[sp - 1].f; ");
                add("--sp;");
                break;
            case Opcodes.DMUL: // 107
                add("stack[sp - 4].d = stack[sp - 4].d * stack[sp - 2].d; ");
                add("sp -= 2;");
                break;
            // =============================================== DIV ==
            case Opcodes.IDIV: // 108
                add("if (!stack[sp - 1].i) {");
                add("    rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_ARITHMETIC_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
                add("    goto " + EXCEPTION_HANDLER + ";");
                add("}");
                add("stack[sp - 2].i = stack[sp - 2].i / stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LDIV: // 109
                add("if (!stack[sp - 2].j) {");
                add("    rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_ARITHMETIC_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
                add("    goto " + EXCEPTION_HANDLER + ";");
                add("}");
                add("stack[sp - 4].j = stack[sp - 4].j / stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            case Opcodes.FDIV: // 110
                add("stack[sp - 2].f = stack[sp - 2].f / stack[sp - 1].f; ");
                add("--sp;");
                break;
            case Opcodes.DDIV: // 111
                add("stack[sp - 4].d = stack[sp - 4].d / stack[sp - 2].d; ");
                add("sp -= 2;");
                break;
            // =============================================== REM ==
            case Opcodes.IREM: // 112
                add("if (!stack[sp - 1].i) {");
                add("    rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_ARITHMETIC_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
                add("    goto " + EXCEPTION_HANDLER + ";");
                add("}");
                add("stack[sp - 2].i = stack[sp - 2].i % stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LREM: // 113
                add("if (!stack[sp - 2].i) {");
                add("    rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_ARITHMETIC_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
                add("    goto " + EXCEPTION_HANDLER + ";");
                add("}");
                add("stack[sp - 4].j = stack[sp - 4].j % stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            case Opcodes.FREM: // 114
                add("stack[sp - 2].f = stack[sp - 2].f - ((s32)(stack[sp - 2].f / stack[sp - 1].f) * stack[sp - 1].f); ");
                add("--sp;");
                break;
            case Opcodes.DREM: // 115
                add("stack[sp - 4].d = stack[sp - 4].d - ((s32)(stack[sp - 4].d / stack[sp - 2].d) * stack[sp - 2].d); ");
                add("sp -= 2;");
                break;
            // =============================================== NEG ==
            case Opcodes.INEG: // 116
                add("stack[sp - 1].i = -stack[sp - 1].i; ");
                break;
            case Opcodes.LNEG: // 117
                add("stack[sp - 2].j = -stack[sp - 2].j; ");
                break;
            case Opcodes.FNEG: // 118
                add("stack[sp - 1].f = -stack[sp - 1].f; ");
                break;
            case Opcodes.DNEG: // 119
                add("stack[sp - 2].d = -stack[sp - 2].d; ");
                break;
            // =============================================== SH* ==
            case Opcodes.ISHL: // 120
                add("stack[sp - 2].i = stack[sp - 2].i << stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LSHL: // 121
                add("stack[sp - 3].j = stack[sp - 3].j << stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.ISHR: // 122
                add("stack[sp - 2].i = stack[sp - 2].i >> stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LSHR: // 123
                add("stack[sp - 3].j = stack[sp - 3].j >> stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.IUSHR: // 124
                add("stack[sp - 2].u = stack[sp - 2].u >> stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LUSHR: // 125
                add("stack[sp - 3].j = ((u64)stack[sp - 3].j) >> stack[sp - 1].i; ");
                add("--sp;");
                break;
            // =============================================== AND ==
            case Opcodes.IAND: // 126
                add("stack[sp - 2].i = stack[sp - 2].i & stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LAND: // 127
                add("stack[sp - 4].j = stack[sp - 4].j & stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            // =============================================== OR ==
            case Opcodes.IOR: // 128
                add("stack[sp - 2].i = stack[sp - 2].i | stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LOR: // 129
                add("stack[sp - 4].j = stack[sp - 4].j | stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            // =============================================== XOR ==
            case Opcodes.IXOR: // 130
                add("stack[sp - 2].i = stack[sp - 2].i ^ stack[sp - 1].i; ");
                add("--sp;");
                break;
            case Opcodes.LXOR: // 131
                add("stack[sp - 4].j = stack[sp - 4].j ^ stack[sp - 2].j; ");
                add("sp -= 2;");
                break;
            // =============================================== converts ==
            case Opcodes.I2L: // 133
                add("stack[sp - 1].j = (s64)stack[sp - 1].i; ");
                add("++sp;");
                break;
            case Opcodes.I2F: // 134
                add("stack[sp - 1].f = (f32)stack[sp - 1].i; ");
                break;
            case Opcodes.I2D: // 135
                add("stack[sp - 1].d = (f64)stack[sp - 1].i; ");
                add("++sp;");
                break;
            case Opcodes.L2I: // 136
                add("stack[sp - 2].i = (s32)stack[sp - 2].j; ");
                add("--sp;");
                break;
            case Opcodes.L2F: // 137
                add("stack[sp - 2].f = (f32)stack[sp - 2].j; ");
                add("--sp;");
                break;
            case Opcodes.L2D: // 138
                add("stack[sp - 2].d = (f64)stack[sp - 2].j; ");
                break;
            case Opcodes.F2I: // 139
                add("stack[sp - 1].i = (s32)stack[sp - 1].f; ");
                break;
            case Opcodes.F2L: // 140
                add("stack[sp - 1].j = (s64)stack[sp - 1].f; ");
                add("++sp;");
                break;
            case Opcodes.F2D: // 141
                add("stack[sp - 1].d = (f64)stack[sp - 1].f; ");
                add("++sp;");
                break;
            case Opcodes.D2I: // 142
                add("stack[sp - 2].i = (s32)stack[sp - 2].d; ");
                add("--sp;");
                break;
            case Opcodes.D2L: // 143
                add("stack[sp - 2].j = (s64)stack[sp - 2].d; ");
                break;
            case Opcodes.D2F: // 144
                add("stack[sp - 2].f = (f32)stack[sp - 2].d; ");
                add("--sp;");
                break;
            case Opcodes.I2B: // 145
                add("stack[sp - 1].i = (s8)stack[sp - 1].i; ");
                break;
            case Opcodes.I2C: // 146
                add("stack[sp - 1].i = (u16)stack[sp - 1].i; ");
                break;
            case Opcodes.I2S: // 147
                add("stack[sp - 1].i = (s16)stack[sp - 1].i; ");
                break;
            // =============================================== Long compares (use with IF* command) ==
            case Opcodes.LCMP: // 148
                add("{  //lcmp");
                add("s64 v1 = stack[sp - 2].j;");
                add("s64 v2 = stack[sp - 4].j;");
                add("stack[sp - 4].i = v2 == v1 ? 0: ( v2 > v1 ? 1 : -1); ");
                add("sp -= 3;");
                add("}");
                break;
            // =============================================== Float compares (use with IF* command) ==
            case Opcodes.FCMPL: // 149
            case Opcodes.FCMPG: // 150
                add("{");
                add("f32 v1 = stack[sp - 1].f;");
                add("f32 v2 = stack[sp - 2].f;");
                add("stack[sp - 2].i = v2 == v1 ? 0: ( v2 > v1 ? 1 : -1); ");
                add("--sp;");
                add("}");
                break;
            // =============================================== Double compares (use with IF* command) ==
            case Opcodes.DCMPL: // 151
            case Opcodes.DCMPG: // 152
                add("{");
                add("f64 v1 = stack[sp - 2].d;");
                add("f64 v2 = stack[sp - 4].d;");
                add("stack[sp - 4].i = v2 == v1 ? 0: ( v2 > v1 ? 1 : -1); ");
                add("sp -= 3;");
                add("}");
                break;
            // =============================================== returns ==
            case Opcodes.IRETURN: // 172
                if (!canSkipCodeTrack()) add(AssistLLVM.FUNC_METHOD_EXIT + "(runtime);");
                add("return stack[sp - 1].i;");
                break;
            case Opcodes.LRETURN: // 173
                if (!canSkipCodeTrack()) add(AssistLLVM.FUNC_METHOD_EXIT + "(runtime);");
                add("return stack[sp - 2].j;");
                break;
            case Opcodes.FRETURN: // 174
                if (!canSkipCodeTrack()) add(AssistLLVM.FUNC_METHOD_EXIT + "(runtime);");
                add("return stack[sp - 1].f;");
                break;
            case Opcodes.DRETURN: // 175
                if (!canSkipCodeTrack()) add(AssistLLVM.FUNC_METHOD_EXIT + "(runtime);");
                add("return stack[sp - 2].d;");
                break;
            case Opcodes.ARETURN: // 176
                if (!canSkipCodeTrack()) add(AssistLLVM.FUNC_METHOD_EXIT + "(runtime);");
                add("return rstack[sp - 1].obj;");
                break;
            case Opcodes.RETURN: // 177
                if (!canSkipCodeTrack()) add(AssistLLVM.FUNC_METHOD_EXIT + "(runtime);");
                add("return;");
                break;
            // =============================================== misc ==
            case Opcodes.ARRAYLENGTH: {// 190
                comment("arraylength  label  " + curLabel); //
                add("if (!rstack[sp - 1].obj) {");
                add("    rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_NULL_POINTER_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
                add("    goto " + EXCEPTION_HANDLER + ";");
                add("}");
                add("stack[sp - 1].i = rstack[sp - 1].ins->prop.arr_length; ");
                break;
            }
            case Opcodes.ATHROW: {
                // 191
                comment("athrow"); //
                comment("    " + curLabel + " in labeltable is :" + curLabel);
                add("{");
                add("    __frame->bytecodeIndex = " + curLabel.getOffsetInMethod() + ";");
                add("    __frame->lineNo = " + curLineNo + ";");
                add("    " + AssistLLVM.FUNC_THROW_EXCEPTION + "(runtime, rstack[sp - 1].obj);");
                add("    goto " + EXCEPTION_HANDLER + ";");
                add("}");
                break;
            }
            case Opcodes.MONITORENTER: {// 194
                comment("monitorenter");//
                add("--sp;");
                add("" + AssistLLVM.FUNC_JTHREAD_LOCK + "(runtime, rstack[sp].obj);");
                break;
            }
            case Opcodes.MONITOREXIT: {// 195
                comment("monitorexit");//
                add("--sp;");
                add("" + AssistLLVM.FUNC_JTHREAD_UNLOCK + "(runtime, rstack[sp].obj);");
                break;
            }
            default:
                //System.out.println("IN " + opcode);
        }
    }

    @Override
    public void visitIntInsn(int opcode, int value) {
        switch (opcode) {
            case Opcodes.BIPUSH: // 16
                comment(" bipush " + value);
                add("stack[sp++].i = " + value + ";");
                break;
            case Opcodes.SIPUSH: // 17
                comment(" sipush " + value);
                add("stack[sp++].i = " + value + ";");
                break;
            case Opcodes.NEWARRAY: // 188
                newArray(Util.getjavaTag_by_JtypeIndex(value));
                break;
            default:
                //System.out.println("visitIntInsn " + opcode + " " + value);
        }
    }

    @Override
    public void visitVarInsn(int opcode, int slot) {
        switch (opcode) {
            // =============================================== Load ==
            case Opcodes.ILOAD: // 21
                add("stack[sp++].i = local[" + slot + "].i;");
                break;
            case Opcodes.LLOAD: // 22
                add("stack[sp].j = local[" + slot + "].j;");
                add("sp += 2;");
                break;
            case Opcodes.FLOAD: // 23
                add("stack[sp++].f = local[" + slot + "].f;");
                break;
            case Opcodes.DLOAD: // 24
                add("stack[sp].d = local[" + slot + "].d;");
                add("sp += 2;");
                break;
            case Opcodes.ALOAD: // 25
                add("rstack[sp++].obj = rlocal[" + slot + "].obj;");
                break;
            // =============================================== Store (Store stack into local variable) ==
            case Opcodes.ISTORE: // 54
                add("local[" + slot + "].i = stack[--sp].i;");
                break;
            case Opcodes.LSTORE: // 55
                add("sp -= 2;");
                add("local[" + slot + "].j = stack[sp].j;");
                break;
            case Opcodes.FSTORE: // 56
                add("local[" + slot + "].f = stack[--sp].f;");
                break;
            case Opcodes.DSTORE: // 57
                add("sp -= 2;");
                add("local[" + slot + "].d = stack[sp].d;");
                break;
            case Opcodes.ASTORE: // 58
                add("rlocal[" + slot + "].obj = rstack[--sp].obj;");
                break;
            default:
                //System.out.println("visitVarInsn " + opcode + " " + slot);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String className) {
        AssistLLVM.addClassDependence(cv.className, className);

        switch (opcode) {
            case Opcodes.NEW: // 187
                _new(className);
                if (!className.equals(cv.className)) {
                }
                break;
            case Opcodes.ANEWARRAY: // 189
                if (!className.startsWith("[")) {
                    className = "L" + className + ";";
                }
                newArray(className);
                break;
            case Opcodes.CHECKCAST: {// 192
                comment("checkcast " + className);
                add("{");
                add("    s32 cast_r = " + AssistLLVM.FUNC_CHECKCAST + "(rstack[sp - 1].obj, " + AssistLLVM.getStrIndex(className) + ");");
                add("    if(!cast_r){");
                add("        rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_CLASS_CAST_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
                add("        goto " + EXCEPTION_HANDLER + ";");
                add("    }");
                add("}");
                break;
            }
            case Opcodes.INSTANCEOF: {// 193
                comment("instanceof " + className);
                add("stack[sp - 1].i = " + AssistLLVM.FUNC_INSTANCE_OF + "(rstack[sp - 1].obj, " + AssistLLVM.getStrIndex(className) + ");");
                break;
            }
            default:
                //System.out.println("visitTypeInsn " + opcode + " " + s);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String className, String name, String signature) {
        AssistLLVM.addClassDependence(cv.className, className);
        switch (opcode) {
            case Opcodes.GETSTATIC: // 178
                getstatic(className, name, signature);
                break;
            case Opcodes.PUTSTATIC: // 179
                putstatic(className, name, signature);
                break;
            case Opcodes.GETFIELD: // 180
                getfield(className, name, signature);
                break;
            case Opcodes.PUTFIELD: // 181
                putfield(className, name, signature);
                break;
            default:
                //System.out.println("visitFieldInsn " + opcode + " " + className);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String className, String methodName, String signature, boolean b) {
        AssistLLVM.addClassDependence(cv.className, className);

        switch (opcode) {
            case Opcodes.INVOKEINTERFACE://185
            case Opcodes.INVOKEVIRTUAL: // 182
            {
                comment("invoke" + (opcode == Opcodes.INVOKEVIRTUAL ? "virtual" : "interface") + " " + className + "." + methodName + signature);
                genVirtualMethodCall(className, methodName, signature);
            }
            break;
            case Opcodes.INVOKESPECIAL: // 183
            case Opcodes.INVOKESTATIC: // 184
            {
                comment("invoke" + (opcode == Opcodes.INVOKESPECIAL ? "special" : "static") + " " + className + "." + methodName + signature);
                genFixedMethodCall(className, methodName, signature);
            }
            break;
            default:
                System.out.println("visitMethodInsn " + opcode);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(final String name,
                                       final String descriptor,
                                       final Handle bootstrapMethodHandle,
                                       final Object... bootstrapMethodArguments) {
        //System.out.println("visitInvokeDynamicInsn " + name + " " + descriptor);
        try {

            Type t0 = (Type) bootstrapMethodArguments[0];
            Handle h1 = (Handle) bootstrapMethodArguments[1];
            Type t2 = (Type) bootstrapMethodArguments[2];
            String lambdaClassName = Util.getLambdaClassName(cv.className, h1.getName());
            byte[] classBytes = LambdaMetafactory.makeLambda(lambdaClassName, name, descriptor, t0.getDescriptor(), h1.getOwner(), h1.getName(), h1.getDesc(), getMethodHandleKindByTag(h1.getTag()));
            AssistLLVM.addLambdaClass(lambdaClassName, classBytes);

            //gen code
            comment("invokedynamic" + " " + lambdaClassName + ".make" + descriptor);
            genFixedMethodCall(lambdaClassName, "make", descriptor);

            AssistLLVM.addClassDependence(cv.className, lambdaClassName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int getMethodHandleKindByTag(int tag) {
        switch (tag) {
            case Opcodes.H_INVOKEINTERFACE:
                return MethodHandle.REF_invokeInterface;
            case Opcodes.H_INVOKESPECIAL:
                return MethodHandle.REF_invokeSpecial;
            case Opcodes.H_INVOKESTATIC:
                return MethodHandle.REF_invokeStatic;
            case Opcodes.H_INVOKEVIRTUAL:
                return MethodHandle.REF_invokeVirtual;
            case Opcodes.H_NEWINVOKESPECIAL:
                if (true) throw new RuntimeException("not support new invokespecial");
                return MethodHandle.REF_newInvokeSpecial;
        }
        return 0;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {

        addUsedLabel(label.toString());
        switch (opcode) {
            case Opcodes.IFEQ: // 153
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(stack[--sp].i  == 0) goto " + label + ";");
                break;
            case Opcodes.IFNE: // 154
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(stack[--sp].i  != 0) goto " + label + ";");
                break;
            case Opcodes.IFLT: // 155
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(stack[--sp].i  < 0) goto " + label + ";");
                break;
            case Opcodes.IFGE: // 156
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(stack[--sp].i  >= 0) goto " + label + ";");
                break;
            case Opcodes.IFGT: // 157
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(stack[--sp].i  > 0) goto " + label + ";");
                break;
            case Opcodes.IFLE: // 158
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(stack[--sp].i  <= 0) goto " + label + ";");
                break;
            case Opcodes.IF_ICMPEQ: // 159
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(stack[sp + 0].i  == stack[sp + 1].i) goto " + label + ";");
                break;
            case Opcodes.IF_ICMPNE: // 160
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(stack[sp + 0].i  != stack[sp + 1].i) goto " + label + ";");
                break;
            case Opcodes.IF_ICMPLT: // 161
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(stack[sp + 0].i  < stack[sp + 1].i) goto " + label + ";");
                break;
            case Opcodes.IF_ICMPGE: // 162
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(stack[sp + 0].i  >= stack[sp + 1].i) goto " + label + ";");
                break;
            case Opcodes.IF_ICMPGT: // 163
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(stack[sp + 0].i  > stack[sp + 1].i) goto " + label + ";");
                break;
            case Opcodes.IF_ICMPLE: // 164
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(stack[sp + 0].i  <= stack[sp + 1].i) goto " + label + ";");
                break;
            case Opcodes.IF_ACMPEQ: // 165
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(rstack[sp + 0].obj  == rstack[sp + 1].obj) goto " + label + ";");
                break;
            case Opcodes.IF_ACMPNE: // 166
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("sp -= 2;");
                add("if(rstack[sp + 0].obj  != rstack[sp + 1].obj) goto " + label + ";");
                break;
            case Opcodes.GOTO: // 167
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("goto " + label + ";");
                break;
            case Opcodes.JSR: // 168
                comment("not support jsr*"); // todo
                break;
            case Opcodes.RET: // 169
                comment("not support ret*"); // todo
                break;
            case Opcodes.IFNULL: // 198
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(rstack[--sp].obj  == NULL) goto " + label + ";");
                break;
            case Opcodes.IFNONNULL: // 199
                if (labels.contains(label.toString())) {
                    add("if(runtime->suspend_count)" + AssistLLVM.FUNC_CHECK_SUSPEND_AND_PAUSE + "(runtime);");
                }
                add("if(rstack[--sp].obj  != NULL) goto " + label + ";");
                break;
            default:
                add("visitJumpInsn " + opcode + " " + label.toString());
        }
    }

    @Override
    public void visitLabel(Label label) {
        labels.add(label.toString());
        add(label.toString() + ":");
        curLabel = label;
        genTryCatchLabel(label);
    }

    boolean firstLabAccessed = false;

    @Override
    public void visitLineNumber(int i, Label label) {
        comment(" line no " + i + " , " + label + " , bytecode index = " + label.getOffsetInMethod());
        curLabel = label;
        curLineNo = i;
        if (!firstLabAccessed) {
            if (!canSkipCodeTrack()) {
                add("__frame->bytecodeIndex = " + label.getOffsetInMethod() + "; //first");
                add("__frame->lineNo = " + curLineNo + ";");
            }
            firstLabAccessed = true;
        }


    }

    void genTryCatchLabel(Label label) {
        for (ExceptionItem item : exceptionTables) {
            if (label.toString().equals(item.startLabel.toString())) {
                add("__frame->bytecodeIndex = " + label.getOffsetInMethod() + "; //try catch begin");
            } else if (label.toString().equals(item.endLabel.toString())) {
                add("__frame->bytecodeIndex = " + label.getOffsetInMethod() + "; //try catch end");
            }
        }
    }


    @Override
    public void visitLdcInsn(Object o) {
//        if (methodName.equals("<init>") && cv.className.equals("java/util/OptionalDouble")) {
//            int debug = 1;
//        }

        comment(" ldc ");
        if (o instanceof String) {
            // const
            newString((String) o);
            AssistLLVM.addClassDependence(cv.className, CLASS_JAVA_LANG_STRING);
        } else if (o instanceof Integer) {
            Integer value = (Integer) o;
            add("stack[sp++].i = 0x" + Integer.toHexString(value) + ";");
        } else if (o instanceof Long) {
            Long value = (Long) o;
            add("stack[sp].j = 0x" + Long.toHexString(value) + "L;");
            add("sp += 2;");
        } else if (o instanceof Float) {
            Float value = (Float) o;
            if (Float.intBitsToFloat(0x7f800000) == value) {
                add("stack[sp++].f = 1.0 / 0.0;");
            } else if (Float.intBitsToFloat(0xff800000) == value) {
                add("stack[sp++].f = -1.0 / 0.0;");
            } else if (Float.isNaN(value)) {
                add("stack[sp++].f = 0.0 / 0.0;");
            } else {
                add("stack[sp++].f = " + value + ";");
            }
        } else if (o instanceof Double) {
            Double value = (Double) o;
            if (Double.longBitsToDouble(0x7ff0000000000000L) == value) {
                add("stack[sp].d = 1.0 / 0.0;");
            } else if (Double.longBitsToDouble(0xfff0000000000000L) == value) {
                add("stack[sp].d = -1.0 / 0.0;");
            } else if (Double.isNaN(value)) {
                add("stack[sp].d = 0.0 / 0.0;");
            } else {
                add("stack[sp].d = " + value + ";");
            }
            add("sp += 2;");
        } else {
            Type type = (Type) o;
            String className = type.getDescriptor();
            if (className.startsWith("L")) {
                className = className.substring(1, className.length() - 1);
                int debug = 1;
            }
            int index = AssistLLVM.getStrIndex(className);
            add("{");
            add("    class_clinit(runtime, get_utf8str_by_utfraw_index(" + index + "));");
            add("    JClass *clazz = classes_get(get_utf8str_by_utfraw_index(" + index + "));");
            add("    rstack[sp++].obj = ins_of_Class_create_get(runtime, clazz);");
            add("}");
        }
    }

    @Override
    public void visitIincInsn(int slot, int value) {
        comment("iinc slot " + slot + " value " + value);
        add("local[" + slot + "].i += " + value + ";");
    }


    @Override
    public void visitTableSwitchInsn(int from, int to, Label label, Label... labels) {
        addUsedLabel(label.toString());
        add("switch(stack[--sp].i){");
        for (Label l : labels) {
            addUsedLabel(l.toString());
            add("    case " + from + ":");
            add("        goto " + l.toString() + ";");
            from++;
        }
        add("    default:");
        add("        goto " + label + ";");
        add("}");
    }

    @Override
    public void visitLookupSwitchInsn(Label label, int[] values, Label[] labels) {
        addUsedLabel(label.toString());
        add("switch(stack[--sp].i){");
        for (int i = 0; i < values.length; i++) {
            addUsedLabel(labels[i].toString());
            add("    case " + values[i] + ":");
            add("        goto " + labels[i] + ";");
        }
        add("    default:");
        add("        goto " + label + ";");
        add("}");
    }

    @Override
    public void visitMultiANewArrayInsn(String s, int dims) {
        multiArr(s, dims);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int i, TypePath typePath, String s, boolean b) {
        //System.out.println("visitInsnAnnotation");
        return null;
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        AssistLLVM.addClassDependence(cv.className, type);
//        if (cv.className.equals("org/mini/gui/GContainer") && methodName.equals("paint")) {
//            System.out.println("visitTryCatchBlock " + start + " " + end + " " + type);
//        }
        comment("try catch :" + start + " " + end + " " + handler + " (" + start.getOffsetInMethod() + "," + end.getOffsetInMethod() + ")->" + handler.getOffsetInMethod());
        addUsedLabel(start.toString());
        addUsedLabel(end.toString());
        addUsedLabel(handler.toString());

        ExceptionItem item = new ExceptionItem();
        item.startLabel = start;
        item.endLabel = end;
        item.handlerLabel = handler;
        item.exceptionClassName = type;
        if (type == null) {
            int debug = 1;
        }
        exceptionTables.add(item);

    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int i, TypePath typePath, String s, boolean b) {
        //System.out.println("visitTryCatchAnnotation");
        return null;
    }

    @Override
    public void visitLocalVariable(String name, String sign, String s2, Label label, Label label1, int slot) {
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int i, TypePath typePath, Label[] labels, Label[] labels1, int[] ints, String s, boolean b) {
        return super.visitLocalVariableAnnotation(i, typePath, labels, labels1, ints, s, b);
    }


    @Override
    public void visitMaxs(int stack, int local) {
        // Maximums
        this.max_stack = stack;
        this.max_local = local;
    }

    @Override
    public void visitEnd() {
        if (!canSkipCodeTrack()) {
            addUsedLabel(EXCEPTION_HANDLER);
            add(EXCEPTION_HANDLER + ":");

//            String call = AssistLLVM.FUNC_FIND_EXCEPTION_HANDLER_INDEX + "(runtime, &" + Util.getLabelTableRawName() + ", &&" + EXCEPTION_HANDLER_NOT_FOUND + ")";
//            add("goto  *" + call + ";");
            String call = AssistLLVM.FUNC_FIND_EXCEPTION_HANDLER_INDEX + "(runtime)";
            add("switch (" + call + ") {");
            int count = 0;
            for (ExceptionItem item : exceptionTables) {
                add("    case " + count + " : goto " + item.handlerLabel.toString() + ";");
                count++;
            }
            add("    default: goto " + EXCEPTION_HANDLER_NOT_FOUND + ";");
            add("}");


            addUsedLabel(EXCEPTION_HANDLER_NOT_FOUND);
            add(EXCEPTION_HANDLER_NOT_FOUND + ":");
            String rtype = signature.getResult();
            String returnValue = getDefValue_by_Ctype(rtype);
            add(AssistLLVM.FUNC_METHOD_EXIT + "(runtime);");
            add("return " + returnValue + ";");
        }
    }

    public String getExceptionTable() {
        String rawName = Util.getExceptionTableRawName(this.cv.className, this.methodName, signature);

        int size = exceptionTables.size();
        if (size == 0) return "";
        StringBuilder sb = new StringBuilder();
        String arrName = "arr_" + rawName;
        sb.append(getExceptionItemType()).append(" ").append(arrName).append("[] = {\n");
        int count = 0;
        for (ExceptionItem item : exceptionTables) {
            sb.append("    { ").append(item.startLabel.getOffsetInMethod());
            sb.append(", ").append(item.endLabel.getOffsetInMethod());
//            sb.append(", ").append(getLabelIndex(item.handlerLabel.toString()));
            int classIndex = item.exceptionClassName == null ? -1 : AssistLLVM.getStrIndex(item.exceptionClassName);
            sb.append(", ").append(classIndex);
            sb.append("}");
            if (count < size - 1) {
                sb.append(", ");
            }
            sb.append("\n");
            count++;
        }
        sb.append("};\n");
        sb.append(Util.getExceptionTableType()).append(" ").append(rawName).append(" = {").append(size).append(", ").append(arrName).append("};\n");
        return (sb.toString());
    }

    public String getLabelTable() {
        return "";
//        String arrName = "arr_" + Util.getLabelTableRawName();
//        StringBuilder sb = new StringBuilder();
//        sb.append("static __refer ").append(arrName);
//        sb.append("[] = {");
//        int count = 0;
//        for (String s : usedLabels) {
//            sb.append("&&").append(s);
//            if (count < usedLabels.size() - 1) {
//                sb.append(", ");
//            }
//            count++;
//        }
//        sb.append("};\n");
//        String tbName = "" + Util.getLabelTableRawName();
//        sb.append("static ").append(Util.getLabelTableType()).append(' ').append(tbName);
//        sb.append(" = {").append(usedLabels.size()).append(", ").append(arrName).append("};\n");
//        return sb.toString();
    }


    /**
     * clear the code line ,remove comment ,remove space
     *
     * @param s
     * @return
     */
    String codeClear(String s) {
        String r = s;
        if (s.indexOf(";") >= 0) {
            r = s.substring(0, s.indexOf(";"));
        }
        r = r.trim();
        return r;
    }

    String mergeString(String[] ss) {
        String r = "";
        for (String s : ss) {
            r += s + "\n";
        }
        return r;
    }

    void discardDoubleLabel(String[] result) {
        //found tow label nearly, merge to one label
        Map<String, String> replacepair = new HashMap<>();
        String preLine = null;
        for (int i = 0; i < result.length; i++) {
            String cs = codeClear(result[i]);
            if (cs.endsWith(":")) {
                if (preLine != null) {
                    cs = cs.replace(":", "");
                    replacepair.put(preLine, cs);
                    preLine = cs;
                } else {
                    cs = cs.replace(":", "");
                    preLine = cs;
                }
            } else if (cs.length() == 0) {
                continue;
            } else {
                preLine = null;
            }
        }
        for (int i = 0; i < result.length; i++) {
            String str = result[i];
            for (String s1 : replacepair.keySet()) {
                String s2 = replacepair.get(s1);
                if (str.indexOf(s1 + ":") >= 0) {
                    result[i] = str.replace(s1 + ":", "// replaced " + s1 + " with " + s2);
                } else {
                    result[i] = str.replace(s1, s2);
                }
            }

        }
    }

    public void addUsedLabel(String lab) {
        if (!usedLabels.contains(lab)) {
            usedLabels.add(lab);
        }
    }

    public int getLabelIndex(String lab) {
        if (!usedLabels.contains(lab)) {
            usedLabels.add(lab);
        }
        return usedLabels.indexOf(lab);
    }

    public CV getCv() {
        return cv;
    }
    // =================================================================================================================

    boolean isMethodEmpty(Method m) {
//        if (m.getMethodName().equals("<init>") && m.getClassFile().getThisClassName().equals("java/lang/Object")) {
//            int debug = 1;
//        }
        CodeAttribute ca = m.getCodeAttribute();
        if (ca != null) {
            byte[] code = ca.getCode();
            if (code != null && code.length == 1) {
                if ((code[0] & 0xff) == ByteCodeAssembler.return_) {//empty method, do nothing
                    return true;
                }
            }
        }
        return false;
    }

    boolean isMethodGetter(Method m) {
//        if (m.getMethodName().equals("length") && m.getClassFile().getThisClassName().equals("java/lang/String")) {
//            int debug = 1;
//        }
        CodeAttribute ca = m.getCodeAttribute();
        if (ca != null) {
            byte[] code = ca.getCode();
            if (code != null && code.length == 5) {
                int code0 = code[0] & 0xff;
                int code1 = code[1] & 0xff;
                int code2 = code[2] & 0xff;
                int code3 = code[3] & 0xff;
                int code4 = code[4] & 0xff;
                if (code1 == ByteCodeAssembler.getfield
                        && code0 == ByteCodeAssembler.aload_0
                        && (code4 >= ByteCodeAssembler.ireturn && code4 <= ByteCodeAssembler.areturn)
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isMethodSetter(Method m) {
//        if (m.getMethodName().equals("length") && m.getClassFile().getThisClassName().equals("java/lang/String")) {
//            int debug = 1;
//        }
        CodeAttribute ca = m.getCodeAttribute();
        if (ca != null) {
            byte[] code = ca.getCode();
            if (code != null && code.length == 6) {
                int code0 = code[0] & 0xff;
                int code1 = code[1] & 0xff;
                int code2 = code[2] & 0xff;
                int code3 = code[3] & 0xff;
                int code4 = code[4] & 0xff;
                int code5 = code[5] & 0xff;
                if (code5 == ByteCodeAssembler.return_
                        && code0 == ByteCodeAssembler.aload_0
                        && code2 == ByteCodeAssembler.putfield
                        && (code1 == ByteCodeAssembler.aload_1 || code1 == ByteCodeAssembler.dload_1 || code1 == ByteCodeAssembler.lload_1 || code1 == ByteCodeAssembler.iload_1 || code1 == ByteCodeAssembler.fload_1)
                ) {
                    return true;
                }
            }
        }
        return false;
    }


    public void genFixedMethodCall(String className, String methodName, String descritp) {
        Method m = ClassManger.findMethod(className, methodName, descritp);
        if (m == null) {
            int debug = 1;
            System.out.println("Method not found:" + className + "." + methodName + descritp);
        }
        JSignature sig = new JSignature(m);
        if (isMethodEmpty(m)) {//do nothing
            int paraSlot = sig.getSlotSizeofArgs();
            add("sp -= " + paraSlot + ";  //pop para");
            comment("it's a empty method");
            return;
        }
//        String noerrLab = MV.METHOD_EXIT_WITHOUT_EXCEPTION_LABEL + nextId();
        String errHandLab = MV.EXCEPTION_HANDLER;
        getLabelIndex(MV.EXCEPTION_HANDLER);
//        getLabelIndex(noerrLab);

        int argsCount = sig.getCtypeArgs().size();
        int argsSlotSize = sig.getSlotSizeofArgs();
        int slotIndex = 0;
        add("{");
        add("    sp -= " + argsSlotSize + ";");
        if (!m.isStatic()) {
            add("    " + Util.STR_JOBJECT_TYPE_NAME + " *__ins = rstack[sp + 0].ins;");
            add("    if (!__ins) {");
            add("        rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_NULL_POINTER_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
            add("        goto " + EXCEPTION_HANDLER + ";");
            add("    }");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("runtime");
        for (int i = 0; i < argsCount; i++) {
            sb.append(", ");
            String s = sig.getCtypeArgs().get(i);
            String fname = Util.getStackFieldName_by_Ctype(s);
            String vname = Util.getStackName_by_Ctype(s);
            sb.append(vname + "stack[sp + " + slotIndex + "].").append(fname);
            slotIndex += Util.getSlot_by_Ctype(s);
        }
        String ret = "";
        if (!sig.getResult().equals("void")) {
            String fname = Util.getStackFieldName_by_Ctype(sig.getResult());
            String vname = Util.getStackName_by_Ctype(sig.getResult());
            ret = vname + "stack[sp]." + fname + " = ";
        }
        if (m.isSync()) {
            if (m.isStatic()) {
                add("    " + AssistLLVM.FUNC_JTHREAD_LOCK + "(runtime, (JObject *)g_classes[" + AssistLLVM.getClassIndex(className) + "].clazz);");
            } else {
                add("    " + AssistLLVM.FUNC_JTHREAD_LOCK + "(runtime, __ins);");
            }
        }
        add("    " + ret + Util.getMethodRawName(m) + "(" + sb.toString() + ");");
        if (m.isSync()) {
            if (m.isStatic()) {
                add("    " + AssistLLVM.FUNC_JTHREAD_UNLOCK + "(runtime, (JObject *)g_classes[" + AssistLLVM.getClassIndex(className) + "].clazz);");
            } else {
                add("    " + AssistLLVM.FUNC_JTHREAD_UNLOCK + "(runtime, __ins);");
            }
        }
        add("    sp += " + sig.getSlotSizeOfResult() + ";");
        add("    if (runtime->exception) {");
        add("        rstack[sp++].obj = runtime->exception;");
        add("        goto " + errHandLab + ";");
        add("    }");
        add("}");
    }


    public void genVirtualMethodCall(String className, String methodName, String descritp) {
//        if (className.equals("java/util/List") && methodName.equals("forEach")) {
//            int debug = 1;
//        }

        ClassManger.FindResult result = ClassManger.findVirtualMethod(className, methodName, descritp);
        int classIndex = AssistLLVM.getClassIndex(result.className);
        int methodIndex = result.index;
        JSignature sig = new JSignature(result.getMethod());
        String methodType = sig.getMethodType();
        Method m = ClassManger.findMethod(className, methodName, sig.getJavaSignature());

        String noerrLab = MV.METHOD_EXIT_WITHOUT_EXCEPTION_LABEL + nextId();
        String errHandLab = MV.EXCEPTION_HANDLER;
        getLabelIndex(errHandLab);

        int argsCount = sig.getCtypeArgs().size();
        int argsSlotSize = sig.getSlotSizeofArgs();
        int slotIndex = 0;
        add("{");
        add("    sp -= " + argsSlotSize + ";");
        add("    " + Util.STR_JOBJECT_TYPE_NAME + " *__ins = rstack[sp + 0].ins;");
        add("    if (!__ins) {");
        add("        rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_NULL_POINTER_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
        add("        goto " + EXCEPTION_HANDLER + ";");
        add("    }");
        add("    " + methodType + " = " + AssistLLVM.FUNC_FIND_METHOD + "(__ins->vm_table, " + classIndex + ", " + methodIndex + ");");
        StringBuilder sb = new StringBuilder();
        sb.append("runtime");
        for (int i = 0; i < argsCount; i++) {
            sb.append(", ");
            String s = sig.getCtypeArgs().get(i);
            String fname = Util.getStackFieldName_by_Ctype(s);
            String vname = Util.getStackName_by_Ctype(s);
            sb.append(vname + "stack[sp + " + slotIndex + "].").append(fname);
            slotIndex += Util.getSlot_by_Ctype(s);
        }
        String ret = "";
        if (!sig.getResult().equals("void")) {
            String fname = Util.getStackFieldName_by_Ctype(sig.getResult());
            String vname = Util.getStackName_by_Ctype(sig.getResult());
            ret = vname + "stack[sp]." + fname + " = ";
        }
        if (m.isSync()) {
            if (m.isStatic()) {
                add("    " + AssistLLVM.FUNC_JTHREAD_LOCK + "(runtime, (JObject *)g_classes[" + AssistLLVM.getClassIndex(className) + "].clazz);");
            } else {
                add("    " + AssistLLVM.FUNC_JTHREAD_LOCK + "(runtime, __ins);");
            }
        }
        add("    " + ret + "__func_p(" + sb.toString() + ");");
        if (m.isSync()) {
            if (m.isStatic()) {
                add("    " + AssistLLVM.FUNC_JTHREAD_UNLOCK + "(runtime, (JObject *)g_classes[" + AssistLLVM.getClassIndex(className) + "].clazz);");
            } else {
                add("    " + AssistLLVM.FUNC_JTHREAD_UNLOCK + "(runtime, __ins);");
            }
        }
        add("    sp += " + sig.getSlotSizeOfResult() + ";");
        add("    if (runtime->exception) {");
        add("        rstack[sp++].obj = runtime->exception;");
        add("        goto " + errHandLab + ";");
        add("    }");
        add("}");
    }


    public void newString(String src) {
        int strIndex = AssistLLVM.getStrIndex(src);
        add("rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_STRING_WITH_UTF_INDEX + "(runtime, " + strIndex + ");");
    }


    public void _new(String className) {

        // state
        int classIndex = AssistLLVM.getClassIndex(className);
        // out
        comment("new " + className);
        add("rstack[sp++].obj = " + AssistLLVM.FUNC_NEW_INSTANCE_WITH_RAWINDEX + "(runtime, " + classIndex + ");");

    }


    public void newArray(String javaArrayType) {
        multiArr("[" + javaArrayType, 1);
    }

    public void multiArr(String s, int dims) {

        String type = Util.getJavaSignatureCtype(s);
        int index = AssistLLVM.getStrIndex(s);
        comment("; newarray/multiarray Dimension Array: " + s + " " + dims);
        add("{");
        add("    s32 dim[" + dims + "];");
        for (int i = dims - 1; i >= 0; i--) {
            add("    dim[" + i + "] = stack[--sp].i;");
        }
        add("    static JClass *clazz = NULL;");
        add("    if (!clazz)clazz = " + AssistLLVM.FUNC_ARRAY_CLASS_CREATE_GET + "(" + AssistLLVM.FUNC_GET_UTF_8_STR_BY_UTFRAW_INDEX + "(" + index + "));");
        add("    rstack[sp++].obj = " + AssistLLVM.FUNC_MULTI_ARRAY_CREATE + "(runtime, dim, " + dims + ", clazz);");
        add("}");
    }

    public void putfield(String className, String name, String signature) {
        comment("putfield " + className + " " + name + " " + signature);
        Field field = ClassManger.findField(className, name, signature);

        String ty = Util.getCtype_by_className(className);
        String fname = Util.getStackFieldName_by_Jtype(signature);
        String vname = Util.getStackName_by_Jtype(signature);
        add("sp -= " + (Util.getSlot_by_Jtype(signature) + 1) + ";");
        add("((" + ty + ")rstack[sp + 0].obj)->" + getFieldVarName(field) + " = " + vname + "stack[sp + 1]." + fname + ";");
    }

    public void putstatic(String className, String name, String signature) {
        comment("putstatic " + className + " " + name + " " + signature);
        Field field = ClassManger.findField(className, name, signature);

        String fname = Util.getStackFieldName_by_Jtype(signature);
        String vname = Util.getStackName_by_Jtype(signature);
        add("sp -= " + Util.getSlot_by_Jtype(signature) + ";");
        add(Util.getStaticFieldStructVarName(field.getClassFile().getThisClassName()) + "." + getFieldVarName(field) + " = " + vname + "stack[sp]." + fname + ";");
    }


    public void getfield(String className, String name, String signature) {
        comment("getfield " + className + " " + name + " " + signature);
        Field field = ClassManger.findField(className, name, signature);

        String ty = Util.getCtype_by_className(className);
        String fname = Util.getStackFieldName_by_Jtype(signature);
        String vname = Util.getStackName_by_Jtype(signature);
        add(vname + "stack[sp - 1]." + fname + " = ((" + ty + ")rstack[sp - 1].obj)->" + getFieldVarName(field) + ";");
        add("sp += " + (Util.getSlot_by_Jtype(signature) - 1) + ";");
    }

    public void getstatic(String className, String name, String signature) {
        comment("getstatic " + className + " " + name + " " + signature);
        Field field = ClassManger.findField(className, name, signature);

        String fname = Util.getStackFieldName_by_Jtype(signature);
        String vname = Util.getStackName_by_Jtype(signature);
        add(vname + "stack[sp]." + fname + " =" + Util.getStaticFieldStructVarName(field.getClassFile().getThisClassName()) + "." + getFieldVarName(field) + ";");
        add("sp += " + Util.getSlot_by_Jtype(signature) + ";");
    }

    public void arrstore(String ctype) {
        String fname = Util.getStackFieldName_by_Ctype(ctype);
        String vname = Util.getStackName_by_Ctype(ctype);
        String arrName = Util.getArrayName_by_Ctype(ctype);
        comment("arrstore " + ctype + "  ,  " + curLabel + " bc index = " + curLabel.getOffsetInMethod());
        add("{");
        add("    " + (Util.getSlot_by_Ctype(ctype) == 2 ? "--sp;" : ";"));
        add("    " + ctype + " value = " + vname + "stack[--sp]." + fname + ";");
        add("    s32 idx = stack[--sp].i;");
        add("    " + STR_JARRAY_TYPE_NAME + " *arr = rstack[--sp].obj;");
        add("    if (!arr) {");
        add("        rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_NULL_POINTER_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
        add("        goto " + EXCEPTION_HANDLER + ";");
        add("    } else if (idx < 0 || idx >= arr->prop.arr_length) {");
        add("        rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
        add("        goto " + MV.EXCEPTION_HANDLER + ";");
        add("    }");
        add("    arr->prop." + arrName + "[idx] = value;");
        add("}");
    }


    public void arrload(String ctype) {
        String fname = Util.getStackFieldName_by_Ctype(ctype);
        String vname = Util.getStackName_by_Ctype(ctype);
        String arrName = Util.getArrayName_by_Ctype(ctype);
        comment("arrload " + ctype + "  ,  " + curLabel + " bc index = " + curLabel.getOffsetInMethod());
        add("{");
        add("    s32 idx = stack[--sp].i;");
        add("    " + STR_JARRAY_TYPE_NAME + " *arr = rstack[--sp].obj;");
        add("    if (!arr) {");
        add("        rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_NULL_POINTER_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
        add("        goto " + EXCEPTION_HANDLER + ";");
        add("    } else if (idx < 0 || idx >= arr->prop.arr_length) {");
        add("        rstack[sp++].obj = " + AssistLLVM.FUNC_CONSTRUCT_AND_THROW_EXCEPTION + "(runtime, " + AssistLLVM.getClassIndex(CLASS_JAVA_LANG_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION) + ", " + curLabel.getOffsetInMethod() + ", " + curLineNo + ");");
        add("        goto " + MV.EXCEPTION_HANDLER + ";");
        add("    }");
        add("    " + vname + "stack[sp]." + fname + " = arr->prop." + arrName + "[idx];");
        add("    sp += " + Util.getSlot_by_Ctype(ctype) + ";");
        add("}");
    }


    public void out(PrintStream ps) {
        // 0) info

        ps.print("// locals: ");
        ps.println(max_local);
        ps.print("// stack: ");
        ps.println(max_stack);
        ps.print("// args: ");
        ps.println(this._argTypes.size());

//        if (cv.className.equals("java/lang/StringBuffer") && methodName.equals("delete") && signatureStr.equals("(II)Ljava/lang/StringBuilder;")) {
//            int debug = 1;
//        }

        String methodRawName = Util.getMethodRawName(this.cv.className, this.methodName, signatureStr);
        String methodIrName = signature.getCTypeOfResult() + " " + methodRawName + "(" + signature.getCTypeArgsString() + ")";
        if (methodName.equals("<clinit>")) {
            int index = AssistLLVM.getMethodIndex(cv.className, methodName, signatureStr);
            AssistLLVM.addClinit(methodRawName, index);
        }

        String define = "" + methodIrName + "{";
        ps.println(define);
        int insert = 0;
        add(insert++, getLabelTable());

        max_local += 2;
        max_stack += 2;//may throw exception
        String stackValue = "NULL", localValue = "NULL", spPtrValue = "NULL";
        add(insert++, "StackItem local[" + max_local + "] = {0};");
        add(insert++, "RStackItem rlocal[" + max_local + "] = {0};");
        localValue = "&rlocal[0]";
        add(insert++, "StackItem stack[" + max_stack + "]/* = {0}*/;");
        add(insert++, "RStackItem rstack[" + max_stack + "] = {0};");
        add(insert++, "s32 sp = 0;");
        stackValue = "&rstack[0]";
        spPtrValue = "&sp";//"NULL";//"&sp";

        if (!canSkipCodeTrack()) {
            add(insert++, "StackFrame *__frame = " + AssistLLVM.FUNC_METHOD_ENTER
                    + "(runtime" +
                    ", " + curMethodId +
                    ", " + stackValue +
                    ", " + localValue +
                    ", " + spPtrValue + ");");
        }

        String[] result = getOutputs().toArray(new String[0]);

        labels.removeAll(usedLabels);
        for (int i = 0; i < result.length; i++) {
            for (String lb : labels) {
                if (result[i].indexOf(lb) >= 0)
                    result[i] = result[i].replace(lb + ":", "; ");
            }
        }


        String ms = mergeString(result);
        result = ms.split("\n");
        discardDoubleLabel(result);

        for (int i = 0; i < result.length; i++) {
            ps.print("    ");
            ps.println(result[i]);
        }

        // 3) end
        ps.println("}");
        ps.println("");

    }

    public void outBridge(PrintStream ps) {
//        if (cv.className.equals("com/egls/test/BpDeep") && methodName.equals("train") && signatureStr.equals("([D[D)V")) {
//            int debug = 1;
//        }

        //gen bridge function
        String methodRawName = Util.getMethodRawName(this.cv.className, this.methodName, signatureStr);
        ps.println(getBridgeMethodDeclare(this.cv.className, this.methodName, signatureStr) + " {");
        String cresult = signature.getCTypeOfResult();
        String resultStr = "";
        if (!"void".equals(cresult)) {
            resultStr = "ret->" + Util.getStackFieldName_by_Jtype(signature.getJavaResult()) + " = ";
        }
        ps.print("    " + resultStr + methodRawName + "(runtime");
        if (!isStatic()) {
            ps.print(", ins");

        }
        int count = 0;
        for (int i = isStatic() ? 0 : 1; i < _argTypes.size(); i++) {
            String s = _argTypes.get(i);
            ps.print(", para[" + (count++) + "]." + getStackFieldName_by_Ctype(s));
        }
        ps.println(");");
        ps.println("}");
        ps.println("");
    }

}
