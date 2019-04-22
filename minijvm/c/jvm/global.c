//
// Created by Gust on 2017/8/20 0020.
//

#include "jvm.h"
#include "jvm_util.h"
#include "garbage.h"
//======================= global var =============================

char *STRS_CLASS_EXCEPTION[] = {
        "java.io.OutOfMemoryError",
        "java.io.VirtualMachineError",
        "java.io.NoClassDefFoundError",
        "java.io.EOFException",
        "java.io.IOException",
        "java.lang.FileNotFoundException",
        "java.lang.ArithmeticException",
        "java.lang.ClassNotFoundException",
        "java.lang.NullPointerException",
        "java.lang.NoSuchMethodException",
        "java.lang.IllegalArgumentException",
        "java.lang.ClassCastException",
        "java.lang.ArrayIndexOutOfBoundsException",
        "java.lang.InstantiationException",
};

c8 *STR_CLASS_JAVA_LANG_STRING = "java/lang/String";
c8 *STR_CLASS_JAVA_LANG_STRINGBUILDER = "java/lang/StringBuilder";
c8 *STR_CLASS_JAVA_LANG_OBJECT = "java/lang/Object";
c8 *STR_CLASS_JAVA_LANG_THREAD = "java/lang/Thread";
c8 *STR_CLASS_JAVA_LANG_CLASS = "java/lang/Class";
c8 *STR_CLASS_JAVA_LANG_INVOKE_METHODTYPE = "java/lang/invoke/MethodType";
c8 *STR_CLASS_JAVA_LANG_INVOKE_METHODHANDLE = "java/lang/invoke/MethodHandle";
c8 *STR_CLASS_JAVA_LANG_INVOKE_METHODHANDLES_LOOKUP = "java/lang/invoke/MethodHandles$Lookup";
c8 *STR_CLASS_JAVA_LANG_STACKTRACE = "java/lang/StackTraceElement";
c8 *STR_CLASS_JAVA_LANG_THROWABLE = "java/lang/Throwable";
c8 *STR_CLASS_ORG_MINI_REFLECT_DIRECTMEMOBJ = "org/mini/reflect/DirectMemObj";

c8 *STR_FIELD_STACKFRAME = "stackFrame";
c8 *STR_FIELD_NAME = "name";
c8 *STR_FIELD_VALUE = "value";
c8 *STR_FIELD_COUNT = "count";
c8 *STR_FIELD_OFFSET = "offset";

c8 *STR_FIELD_CLASSHANDLE = "classHandle";

c8 *STR_METHOD_CLINIT = "<clinit>";
c8 *STR_METHOD_FINALIZE = "finalize";

c8 *STR_INS_JAVA_LANG_STRING = "Ljava/lang/String;";
c8 *STR_INS_JAVA_LANG_THREAD = "Ljava/lang/Thread;";
c8 *STR_INS_JAVA_LANG_CLASS = "Ljava/lang/Class;";
c8 *STR_INS_JAVA_LANG_OBJECT = "Ljava/lang/Object;";
c8 *STR_INS_JAVA_LANG_STACKTRACEELEMENT = "Ljava/lang/StackTraceElement;";


ClassLoader *sys_classloader;

ArrayList *native_libs;
ArrayList *thread_list; //all thread
Hashtable *sys_prop;

GcCollector *collector;

JniEnv jnienv;


c8 *data_type_str = "    ZCFDBSIJL[";

s32 data_type_bytes[DATATYPE_COUNT] = {0, 0, 0, 0,
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
};
s32 STACK_LENGHT_MAX = 4096;
s32 STACK_LENGHT_INIT = 4096;

s64 GARBAGE_PERIOD_MS = 1 * 1000;

s64 MAX_HEAP_SIZE = 30 * 1024 * 1024;


//
OptimizeCache jvm_runtime_cache;
//

s32 jvm_state = JVM_STATUS_UNKNOW;

#if _JVM_DEBUG_PROFILE

spinlock_t pro_lock;
ProfileDetail profile_instructs[INST_COUNT] = {0};

c8 *inst_name[] = {
/* 0x00 */ "op_nop",
/* 0x01 */ "op_aconst_null",
/* 0x02 */ "op_iconst_m1",
/* 0x03 */ "op_iconst_0",
/* 0x04 */ "op_iconst_1",
/* 0x05 */ "op_iconst_2",
/* 0x06 */ "op_iconst_3",
/* 0x07 */ "op_iconst_4",
/* 0x08 */ "op_iconst_5",
/* 0x09 */ "op_lconst_0",
/* 0x0A */ "op_lconst_1",
/* 0x0B */ "op_fconst_0",
/* 0x0C */ "op_fconst_1",
/* 0x0D */ "op_fconst_2",
/* 0x0E */ "op_dconst_0",
/* 0x0F */ "op_dconst_1",
/* 0x10 */ "op_bipush",
/* 0x11 */ "op_sipush",
/* 0x12 */ "op_ldc",
/* 0x13 */ "op_ldc_w",
/* 0x14 */ "op_ldc2_w",
/* 0x15 */ "op_iload",
/* 0x16 */ "op_lload",
/* 0x17 */ "op_fload",
/* 0x18 */ "op_dload",
/* 0x19 */ "op_aload",
/* 0x1A */ "op_iload_0",
/* 0x1B */ "op_iload_1",
/* 0x1C */ "op_iload_2",
/* 0x1D */ "op_iload_3",
/* 0x1E */ "op_lload_0",
/* 0x1F */ "op_lload_1",
/* 0x20 */ "op_lload_2",
/* 0x21 */ "op_lload_3",
/* 0x22 */ "op_fload_0",
/* 0x23 */ "op_fload_1",
/* 0x24 */ "op_fload_2",
/* 0x25 */ "op_fload_3",
/* 0x26 */ "op_dload_0",
/* 0x27 */ "op_dload_1",
/* 0x28 */ "op_dload_2",
/* 0x29 */ "op_dload_3",
/* 0x2A */ "op_aload_0",
/* 0x2B */ "op_aload_1",
/* 0x2C */ "op_aload_2",
/* 0x2D */ "op_aload_3",
/* 0x2E */ "op_iaload",
/* 0x2F */ "op_laload",
/* 0x30 */ "op_faload",
/* 0x31 */ "op_daload",
/* 0x32 */ "op_aaload",
/* 0x33 */ "op_baload",
/* 0x34 */ "op_caload",
/* 0x35 */ "op_saload",
/* 0x36 */ "op_istore",
/* 0x37 */ "op_lstore",
/* 0x38 */ "op_fstore",
/* 0x39 */ "op_dstore",
/* 0x3A */ "op_astore",
/* 0x3B */ "op_istore_0",
/* 0x3C */ "op_istore_1",
/* 0x3D */ "op_istore_2",
/* 0x3E */ "op_istore_3",
/* 0x3F */ "op_lstore_0",
/* 0x40 */ "op_lstore_1",
/* 0x41 */ "op_lstore_2",
/* 0x42 */ "op_lstore_3",
/* 0x43 */ "op_fstore_0",
/* 0x44 */ "op_fstore_1",
/* 0x45 */ "op_fstore_2",
/* 0x46 */ "op_fstore_3",
/* 0x47 */ "op_dstore_0",
/* 0x48 */ "op_dstore_1",
/* 0x49 */ "op_dstore_2",
/* 0x4A */ "op_dstore_3",
/* 0x4B */ "op_astore_0",
/* 0x4C */ "op_astore_1",
/* 0x4D */ "op_astore_2",
/* 0x4E */ "op_astore_3",
/* 0x4F */ "op_iastore",
/* 0x50 */ "op_lastore",
/* 0x51 */ "op_fastore",
/* 0x52 */ "op_dastore",
/* 0x53 */ "op_aastore",
/* 0x54 */ "op_bastore",
/* 0x55 */ "op_castore",
/* 0x56 */ "op_sastore",
/* 0x57 */ "op_pop",
/* 0x58 */ "op_pop2",
/* 0x59 */ "op_dup",
/* 0x5A */ "op_dup_x1",
/* 0x5B */ "op_dup_x2",
/* 0x5C */ "op_dup2",
/* 0x5D */ "op_dup2_x1",
/* 0x5E */ "op_dup2_x2",
/* 0x5F */ "op_swap",
/* 0x60 */ "op_iadd",
/* 0x61 */ "op_ladd",
/* 0x62 */ "op_fadd",
/* 0x63 */ "op_dadd",
/* 0x64 */ "op_isub",
/* 0x65 */ "op_lsub",
/* 0x66 */ "op_fsub",
/* 0x67 */ "op_dsub",
/* 0x68 */ "op_imul",
/* 0x69 */ "op_lmul",
/* 0x6A */ "op_fmul",
/* 0x6B */ "op_dmul",
/* 0x6C */ "op_idiv",
/* 0x6D */ "op_ldiv",
/* 0x6E */ "op_fdiv",
/* 0x6F */ "op_ddiv",
/* 0x70 */ "op_irem",
/* 0x71 */ "op_lrem",
/* 0x72 */ "op_frem",
/* 0x73 */ "op_drem",
/* 0x74 */ "op_ineg",
/* 0x75 */ "op_lneg",
/* 0x76 */ "op_fneg",
/* 0x77 */ "op_dneg",
/* 0x78 */ "op_ishl",
/* 0x79 */ "op_lshl",
/* 0x7A */ "op_ishr",
/* 0x7B */ "op_lshr",
/* 0x7C */ "op_iushr",
/* 0x7D */ "op_lushr",
/* 0x7E */ "op_iand",
/* 0x7F */ "op_land",
/* 0x80 */ "op_ior",
/* 0x81 */ "op_lor",
/* 0x82 */ "op_ixor",
/* 0x83 */ "op_lxor",
/* 0x84 */ "op_iinc",
/* 0x85 */ "op_i2l",
/* 0x86 */ "op_i2f",
/* 0x87 */ "op_i2d",
/* 0x88 */ "op_l2i",
/* 0x89 */ "op_l2f",
/* 0x8A */ "op_l2d",
/* 0x8B */ "op_f2i",
/* 0x8C */ "op_f2l",
/* 0x8D */ "op_f2d",
/* 0x8E */ "op_d2i",
/* 0x8F */ "op_d2l",
/* 0x90 */ "op_d2f",
/* 0x91 */ "op_i2b",
/* 0x92 */ "op_i2c",
/* 0x93 */ "op_i2s",
/* 0x94 */ "op_lcmp",
/* 0x95 */ "op_fcmpl",
/* 0x96 */ "op_fcmpg",
/* 0x97 */ "op_dcmpl",
/* 0x98 */ "op_dcmpg",
/* 0x99 */ "op_ifeq",
/* 0x9A */ "op_ifne",
/* 0x9B */ "op_iflt",
/* 0x9C */ "op_ifge",
/* 0x9D */ "op_ifgt",
/* 0x9E */ "op_ifle",
/* 0x9F */ "op_if_icmpeq",
/* 0xA0 */ "op_if_icmpne",
/* 0xA1 */ "op_if_icmplt",
/* 0xA2 */ "op_if_icmpge",
/* 0xA3 */ "op_if_icmpgt",
/* 0xA4 */ "op_if_icmple",
/* 0xA5 */ "op_if_acmpeq",
/* 0xA6 */ "op_if_acmpne",
/* 0xA7 */ "op_goto",
/* 0xA8 */ "op_jsr",
/* 0xA9 */ "op_ret",
/* 0xAA */ "op_tableswitch",
/* 0xAB */ "op_lookupswitch",
/* 0xAC */ "op_ireturn",
/* 0xAD */ "op_lreturn",
/* 0xAE */ "op_freturn",
/* 0xAF */ "op_dreturn",
/* 0xB0 */ "op_areturn",
/* 0xB1 */ "op_return",
/* 0xB2 */ "op_getstatic",
/* 0xB3 */ "op_putstatic",
/* 0xB4 */ "op_getfield",
/* 0xB5 */ "op_putfield",
/* 0xB6 */ "op_invokevirtual",
/* 0xB7 */ "op_invokespecial",
/* 0xB8 */ "op_invokestatic",
/* 0xB9 */ "op_invokeinterface",
/* 0xBA */ "op_invokedynamic",
/* 0xBB */ "op_new",
/* 0xBC */ "op_newarray",
/* 0xBD */ "op_anewarray",
/* 0xBE */ "op_arraylength",
/* 0xBF */ "op_athrow",
/* 0xC0 */ "op_checkcast",
/* 0xC1 */ "op_instanceof",
/* 0xC2 */ "op_monitorenter",
/* 0xC3 */ "op_monitorexit",
/* 0xC4 */ "op_wide",
/* 0xC5 */ "op_multianewarray",
/* 0xC6 */ "op_ifnull",
/* 0xC7 */ "op_ifnonnull",
/* 0xC8 */ "op_0xc8",
/* 0xC9 */ "op_0xc9",
/* 0xCA */ "op_breakpoint",
};

#endif

