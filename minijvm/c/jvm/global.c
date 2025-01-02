//
// Created by Gust on 2017/8/20 0020.
//

#include "jvm.h"
#include "jvm_util.h"
#include "garbage.h"
//======================= global var =============================


JniEnv jnienv;


c8 *DATA_TYPE_STR = "    ZCFDBSIJL[R";

s32 DATA_TYPE_BYTES[DATATYPE_COUNT] = {0, 0, 0, 0,
                                       sizeof(c8),
                                       sizeof(u16),
                                       sizeof(f32),
                                       sizeof(f64),
                                       sizeof(c8),
                                       sizeof(s16),
                                       sizeof(s32),
                                       sizeof(s64),
                                       sizeof(__refer),
                                       sizeof(__refer),
                                       sizeof(__refer),
};

const c8 STR_CLASS_JAVA_LANG_BOOLEAN[] = "java/lang/Boolean";
const c8 STR_CLASS_JAVA_LANG_BYTE[] = "java/lang/Byte";
const c8 STR_CLASS_JAVA_LANG_SHORT[] = "java/lang/Short";
const c8 STR_CLASS_JAVA_LANG_CHARACTER[] = "java/lang/Character";
const c8 STR_CLASS_JAVA_LANG_INTEGER[] = "java/lang/Integer";
const c8 STR_CLASS_JAVA_LANG_LONG[] = "java/lang/Long";
const c8 STR_CLASS_JAVA_LANG_FLOAT[] = "java/lang/Float";
const c8 STR_CLASS_JAVA_LANG_DOUBLE[] = "java/lang/Double";
const c8 STR_CLASS_JAVA_LANG_STRING[] = "java/lang/String";
const c8 STR_CLASS_JAVA_LANG_STRINGBUILDER[] = "java/lang/StringBuilder";
const c8 STR_CLASS_JAVA_LANG_OBJECT[] = "java/lang/Object";
const c8 STR_CLASS_JAVA_LANG_THREAD[] = "java/lang/Thread";
const c8 STR_CLASS_JAVA_LANG_CLASS[] = "java/lang/Class";
const c8 STR_CLASS_JAVA_LANG_CLASSLOADER[] = "java/lang/ClassLoader";
const c8 STR_CLASS_JAVA_LANG_REF_REFERENCE[] = "java/lang/ref/Reference";
const c8 STR_CLASS_JAVA_LANG_REF_WEAKREFERENCE[] = "java/lang/ref/WeakReference";
const c8 STR_CLASS_JAVA_LANG_INVOKE_METHODTYPE[] = "java/lang/invoke/MethodType";
const c8 STR_CLASS_JAVA_LANG_INVOKE_METHODHANDLE[] = "java/lang/invoke/MethodHandle";
const c8 STR_CLASS_JAVA_LANG_INVOKE_METHODHANDLES_LOOKUP[] = "java/lang/invoke/MethodHandles$Lookup";
const c8 STR_CLASS_JAVA_LANG_STACKTRACE[] = "java/lang/StackTraceElement";
const c8 STR_CLASS_JAVA_LANG_THROWABLE[] = "java/lang/Throwable";
const c8 STR_CLASS_ORG_MINI_REFLECT_DIRECTMEMOBJ[] = "org/mini/reflect/DirectMemObj";
const c8 STR_CLASS_SUN_MISC_LAUNCHER[] = "sun/misc/Launcher";
const c8 STR_CLASS_ORG_MINI_REFLECT_REFLECTMETHOD[] = "org/mini/reflect/ReflectMethod";
const c8 STR_CLASS_JAVA_LANG_OUTOFMEMORYERROR[] = "java.lang.OutOfMemoryError";
const c8 STR_CLASS_JAVA_LANG_VIRTUALMACHINEERROR[] = "java.lang.VirtualMachineError";
const c8 STR_CLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR[] = "java.lang.NoClassDefFoundError";
const c8 STR_CLASS_JAVA_IO_EOF[] = "java.io.EOFException";
const c8 STR_CLASS_JAVA_IO_IO[] = "java.io.IOException";
const c8 STR_CLASS_JAVA_IO_FILENOTFOUND[] = "java.io.FileNotFoundException";
const c8 STR_CLASS_JAVA_LANG_ARITHMETIC[] = "java.lang.ArithmeticException";
const c8 STR_CLASS_JAVA_LANG_CLASSNOTFOUND[] = "java.lang.ClassNotFoundException";
const c8 STR_CLASS_JAVA_LANG_NULLPOINTER[] = "java.lang.NullPointerException";
const c8 STR_CLASS_JAVA_LANG_NOSUCHMETHOD[] = "java.lang.NoSuchMethodException";
const c8 STR_CLASS_JAVA_LANG_NOSUCHFIELD[] = "java.lang.NoSuchFieldException";
const c8 STR_CLASS_JAVA_LANG_ILLEGALARGUMENT[] = "java.lang.IllegalArgumentException";
const c8 STR_CLASS_JAVA_LANG_CLASSCAST[] = "java.lang.ClassCastException";
const c8 STR_CLASS_JAVA_LANG_ARRAYINDEXOUTOFBOUNDS[] = "java.lang.ArrayIndexOutOfBoundsException";
const c8 STR_CLASS_JAVA_LANG_INSTANTIATION[] = "java.lang.InstantiationException";
const c8 STR_CLASS_JAVA_LANG_INTERRUPTED[] = "java/lang/InterruptedException";
const c8 STR_CLASS_ORG_MINI_VM_VMSTOPEXCEPTION[] = "org/mini/vm/VmStopException";
const c8 STR_CLASS_JAVA_LANG_ILLEGALTHREADSTATE[] = "java.lang.IllegalThreadStateException";


const c8 STR_FIELD_STACKFRAME[] = "stackFrame";
const c8 STR_FIELD_NAME[] = "name";
const c8 STR_FIELD_VALUE[] = "value";
const c8 STR_FIELD_COUNT[] = "count";
const c8 STR_FIELD_OFFSET[] = "offset";
const c8 STR_FIELD_CLASSHANDLE[] = "classHandle";
const c8 STR_FIELD_CLASSLOADER[] = "classLoader";
const c8 STR_METHOD_CLINIT[] = "<clinit>";
const c8 STR_METHOD_FINALIZE[] = "finalize";
const c8 STR_INS_JAVA_LANG_STRING[] = "Ljava/lang/String;";
const c8 STR_INS_JAVA_LANG_THREAD[] = "Ljava/lang/Thread;";
const c8 STR_INS_JAVA_LANG_CLASS[] = "Ljava/lang/Class;";
const c8 STR_INS_JAVA_LANG_OBJECT[] = "Ljava/lang/Object;";
const c8 STR_INS_JAVA_LANG_STACKTRACEELEMENT[] = "Ljava/lang/StackTraceElement;";
const c8 STR_VM_JAVA_LIBRARY_PATH[] = "java.library.path";
const c8 STR_VM_SUN_BOOT_CLASS_PATH[] = "sun.boot.class.path";
const c8 STR_VM_JAVA_CLASS_PATH[] = "java.class.path";
const c8 STR_JNI_LIB_NOT_FOUND[] = "lib not found:%s\n";
const c8 STR_JNI_ONLOAD_NOT_FOUND[] = "register function not found:%s\n";
const c8 STR_JNI_ON_LOAD[] = "JNI_OnLoad";


const c8 *STRS_CLASS_EXCEPTION[] = {
        STR_CLASS_JAVA_LANG_OUTOFMEMORYERROR,
        STR_CLASS_JAVA_LANG_VIRTUALMACHINEERROR,
        STR_CLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR,
        STR_CLASS_JAVA_IO_EOF,
        STR_CLASS_JAVA_IO_IO,
        STR_CLASS_JAVA_IO_FILENOTFOUND,
        STR_CLASS_JAVA_LANG_ARITHMETIC,
        STR_CLASS_JAVA_LANG_CLASSNOTFOUND,
        STR_CLASS_JAVA_LANG_NULLPOINTER,
        STR_CLASS_JAVA_LANG_NOSUCHMETHOD,
        STR_CLASS_JAVA_LANG_NOSUCHFIELD,
        STR_CLASS_JAVA_LANG_ILLEGALARGUMENT,
        STR_CLASS_JAVA_LANG_CLASSCAST,
        STR_CLASS_JAVA_LANG_ARRAYINDEXOUTOFBOUNDS,
        STR_CLASS_JAVA_LANG_INSTANTIATION,
        STR_CLASS_JAVA_LANG_INTERRUPTED,
        STR_CLASS_ORG_MINI_VM_VMSTOPEXCEPTION,
        STR_CLASS_JAVA_LANG_ILLEGALTHREADSTATE,
};

c8 *INST_NAME[] = {
        /* 0x00 */ "nop",
        /* 0x01 */ "aconst_null",
        /* 0x02 */ "iconst_m1",
        /* 0x03 */ "iconst_0",
        /* 0x04 */ "iconst_1",
        /* 0x05 */ "iconst_2",
        /* 0x06 */ "iconst_3",
        /* 0x07 */ "iconst_4",
        /* 0x08 */ "iconst_5",
        /* 0x09 */ "lconst_0",
        /* 0x0A */ "lconst_1",
        /* 0x0B */ "fconst_0",
        /* 0x0C */ "fconst_1",
        /* 0x0D */ "fconst_2",
        /* 0x0E */ "dconst_0",
        /* 0x0F */ "dconst_1",
        /* 0x10 */ "bipush",
        /* 0x11 */ "sipush",
        /* 0x12 */ "ldc",
        /* 0x13 */ "ldc_w",
        /* 0x14 */ "ldc2_w",
        /* 0x15 */ "iload",
        /* 0x16 */ "lload",
        /* 0x17 */ "fload",
        /* 0x18 */ "dload",
        /* 0x19 */ "aload",
        /* 0x1A */ "iload_0",
        /* 0x1B */ "iload_1",
        /* 0x1C */ "iload_2",
        /* 0x1D */ "iload_3",
        /* 0x1E */ "lload_0",
        /* 0x1F */ "lload_1",
        /* 0x20 */ "lload_2",
        /* 0x21 */ "lload_3",
        /* 0x22 */ "fload_0",
        /* 0x23 */ "fload_1",
        /* 0x24 */ "fload_2",
        /* 0x25 */ "fload_3",
        /* 0x26 */ "dload_0",
        /* 0x27 */ "dload_1",
        /* 0x28 */ "dload_2",
        /* 0x29 */ "dload_3",
        /* 0x2A */ "aload_0",
        /* 0x2B */ "aload_1",
        /* 0x2C */ "aload_2",
        /* 0x2D */ "aload_3",
        /* 0x2E */ "iaload",
        /* 0x2F */ "laload",
        /* 0x30 */ "faload",
        /* 0x31 */ "daload",
        /* 0x32 */ "aaload",
        /* 0x33 */ "baload",
        /* 0x34 */ "caload",
        /* 0x35 */ "saload",
        /* 0x36 */ "istore",
        /* 0x37 */ "lstore",
        /* 0x38 */ "fstore",
        /* 0x39 */ "dstore",
        /* 0x3A */ "astore",
        /* 0x3B */ "istore_0",
        /* 0x3C */ "istore_1",
        /* 0x3D */ "istore_2",
        /* 0x3E */ "istore_3",
        /* 0x3F */ "lstore_0",
        /* 0x40 */ "lstore_1",
        /* 0x41 */ "lstore_2",
        /* 0x42 */ "lstore_3",
        /* 0x43 */ "fstore_0",
        /* 0x44 */ "fstore_1",
        /* 0x45 */ "fstore_2",
        /* 0x46 */ "fstore_3",
        /* 0x47 */ "dstore_0",
        /* 0x48 */ "dstore_1",
        /* 0x49 */ "dstore_2",
        /* 0x4A */ "dstore_3",
        /* 0x4B */ "astore_0",
        /* 0x4C */ "astore_1",
        /* 0x4D */ "astore_2",
        /* 0x4E */ "astore_3",
        /* 0x4F */ "iastore",
        /* 0x50 */ "lastore",
        /* 0x51 */ "fastore",
        /* 0x52 */ "dastore",
        /* 0x53 */ "aastore",
        /* 0x54 */ "bastore",
        /* 0x55 */ "castore",
        /* 0x56 */ "sastore",
        /* 0x57 */ "pop",
        /* 0x58 */ "pop2",
        /* 0x59 */ "dup",
        /* 0x5A */ "dup_x1",
        /* 0x5B */ "dup_x2",
        /* 0x5C */ "dup2",
        /* 0x5D */ "dup2_x1",
        /* 0x5E */ "dup2_x2",
        /* 0x5F */ "swap",
        /* 0x60 */ "iadd",
        /* 0x61 */ "ladd",
        /* 0x62 */ "fadd",
        /* 0x63 */ "dadd",
        /* 0x64 */ "isub",
        /* 0x65 */ "lsub",
        /* 0x66 */ "fsub",
        /* 0x67 */ "dsub",
        /* 0x68 */ "imul",
        /* 0x69 */ "lmul",
        /* 0x6A */ "fmul",
        /* 0x6B */ "dmul",
        /* 0x6C */ "idiv",
        /* 0x6D */ "ldiv",
        /* 0x6E */ "fdiv",
        /* 0x6F */ "ddiv",
        /* 0x70 */ "irem",
        /* 0x71 */ "lrem",
        /* 0x72 */ "frem",
        /* 0x73 */ "drem",
        /* 0x74 */ "ineg",
        /* 0x75 */ "lneg",
        /* 0x76 */ "fneg",
        /* 0x77 */ "dneg",
        /* 0x78 */ "ishl",
        /* 0x79 */ "lshl",
        /* 0x7A */ "ishr",
        /* 0x7B */ "lshr",
        /* 0x7C */ "iushr",
        /* 0x7D */ "lushr",
        /* 0x7E */ "iand",
        /* 0x7F */ "land",
        /* 0x80 */ "ior",
        /* 0x81 */ "lor",
        /* 0x82 */ "ixor",
        /* 0x83 */ "lxor",
        /* 0x84 */ "iinc",
        /* 0x85 */ "i2l",
        /* 0x86 */ "i2f",
        /* 0x87 */ "i2d",
        /* 0x88 */ "l2i",
        /* 0x89 */ "l2f",
        /* 0x8A */ "l2d",
        /* 0x8B */ "f2i",
        /* 0x8C */ "f2l",
        /* 0x8D */ "f2d",
        /* 0x8E */ "d2i",
        /* 0x8F */ "d2l",
        /* 0x90 */ "d2f",
        /* 0x91 */ "i2b",
        /* 0x92 */ "i2c",
        /* 0x93 */ "i2s",
        /* 0x94 */ "lcmp",
        /* 0x95 */ "fcmpl",
        /* 0x96 */ "fcmpg",
        /* 0x97 */ "dcmpl",
        /* 0x98 */ "dcmpg",
        /* 0x99 */ "ifeq",
        /* 0x9A */ "ifne",
        /* 0x9B */ "iflt",
        /* 0x9C */ "ifge",
        /* 0x9D */ "ifgt",
        /* 0x9E */ "ifle",
        /* 0x9F */ "if_icmpeq",
        /* 0xA0 */ "if_icmpne",
        /* 0xA1 */ "if_icmplt",
        /* 0xA2 */ "if_icmpge",
        /* 0xA3 */ "if_icmpgt",
        /* 0xA4 */ "if_icmple",
        /* 0xA5 */ "if_acmpeq",
        /* 0xA6 */ "if_acmpne",
        /* 0xA7 */ "goto",
        /* 0xA8 */ "jsr",
        /* 0xA9 */ "ret",
        /* 0xAA */ "tableswitch",
        /* 0xAB */ "lookupswitch",
        /* 0xAC */ "ireturn",
        /* 0xAD */ "lreturn",
        /* 0xAE */ "freturn",
        /* 0xAF */ "dreturn",
        /* 0xB0 */ "areturn",
        /* 0xB1 */ "return",
        /* 0xB2 */ "getstatic",
        /* 0xB3 */ "putstatic",
        /* 0xB4 */ "getfield",
        /* 0xB5 */ "putfield",
        /* 0xB6 */ "invokevirtual",
        /* 0xB7 */ "invokespecial",
        /* 0xB8 */ "invokestatic",
        /* 0xB9 */ "invokeinterface",
        /* 0xBA */ "invokedynamic",
        /* 0xBB */ "new",
        /* 0xBC */ "newarray",
        /* 0xBD */ "anewarray",
        /* 0xBE */ "arraylength",
        /* 0xBF */ "athrow",
        /* 0xC0 */ "checkcast",
        /* 0xC1 */ "instanceof",
        /* 0xC2 */ "monitorenter",
        /* 0xC3 */ "monitorexit",
        /* 0xC4 */ "wide",
        /* 0xC5 */ "multianewarray",
        /* 0xC6 */ "ifnull",
        /* 0xC7 */ "ifnonnull",
        /* 0xC8 */ "goto_w",
        /* 0xC9 */ "jsr_w",
        /* 0xCA */ "breakpoint",
        /* 0xCB */ "getstatic_ref",//
        /* 0xCC */ "getstatic_long",
        /* 0xCD */ "getstatic_int",
        /* 0xCE */ "getstatic_short",
        /* 0xCF */ "getstatic_jchar",
        /* 0xD0 */ "getstatic_byte",
        /* 0xD1 */ "putstatic_ref",
        /* 0xD2 */ "putstatic_long",
        /* 0xD3 */ "putstatic_int",
        /* 0xD4 */ "putstatic_short",
        /* 0xD5 */ "putstatic_byte",
        /* 0xD6 */ "getfield_ref",
        /* 0xD7 */ "getfield_long",
        /* 0xD8 */ "getfield_int",
        /* 0xD9 */ "getfield_short",
        /* 0xDA */ "getfield_jchar",
        /* 0xDB */ "getfield_byte",
        /* 0xDC */ "putfield_ref",
        /* 0xDD */ "putfield_long",
        /* 0xDE */ "putfield_int",
        /* 0xDF */ "putfield_short",
        /* 0xE0 */ "putfield_byte",
        /* 0xE1 */ "invokevirtual_fast",
        /* 0xE2 */ "invokespecial_fast",
        /* 0xE3 */ "invokestatic_fast",
        /* 0xE4 */ "invokeinterface_fast",
        /* 0xE5 */ "invokedynamic_fast",
        /* 0xE6 */ "0xF6",
        /* 0xE7 */ "0xF7",
        /* 0xE8 */ "0xF8",
        /* 0xE9 */ "0xF9",
        /* 0xEA */ "0xFA",
        /* 0xEB */ "0xFB",
        /* 0xEC */ "0xFC",
        /* 0xED */ "0xFD",
        /* 0xEE */ "0xFE",
        /* 0xEF */ "0xFF",
};
#if _JVM_DEBUG_PROFILE

spinlock_t pro_lock;
ProfileDetail profile_instructs[INST_COUNT] = {0};


#endif

