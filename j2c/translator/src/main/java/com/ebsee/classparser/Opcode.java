package com.ebsee.classparser;
import java.util.*;

/**
 * Represents an opcode - the first byte of a JVM instruction, describing an
 * operation to be performed. Instances of this class can be retrieved by
 * mnemonic (e.g. Opcode.IF_ICMPEQ) or byte code (e.g. Opcode.getOpcode(0x9f)).
 *
 * Each Opcode object can tell you how many bytes an instruction takes up,
 * given a code array and offset (e.g. Opcode.IF_ICMPEQ.getSize(code, offset)).
 * Most instructions have a fixed size, but three of them do not.
 *
 * The opcodes listed here come from:
 * http://java.sun.com/docs/books/jvms/second_edition/html/Mnemonics.doc.html
 *
 * For general documentation on Java enums (a special type of class having a
 * fixed set of instances), see:
 * http://java.sun.com/j2se/1.5.0/docs/guide/language/enums.html
 *
 * (Requires Java 1.6.)
 *
 * Updated: 2014-04-30
 *
 * @author David Cooper
 */
public enum Opcode
{
    NOP(0x00),
    ACONST_NULL(0x01),
    ICONST_M1(0x02),
    ICONST_0(0x03),
    ICONST_1(0x04),
    ICONST_2(0x05),
    ICONST_3(0x06),
    ICONST_4(0x07),
    ICONST_5(0x08),
    LCONST_0(0x09),
    LCONST_1(0x0A),
    FCONST_0(0x0B),
    FCONST_1(0x0C),
    FCONST_2(0x0D),
    DCONST_0(0x0E),
    DCONST_1(0x0F),
    BIPUSH(0x10, "byte"),
    SIPUSH(0x11, "byte1", "byte2"),
    LDC(0x12, "index"),
    LDC_W(0x13, "indexbyte1", "indexbyte2"),
    LDC2_W(0x14, "indexbyte1", "indexbyte2"),
    ILOAD(0x15, "index"),
    LLOAD(0x16, "index"),
    FLOAD(0x17, "index"),
    DLOAD(0x18, "index"),
    ALOAD(0x19, "index"),
    ILOAD_0(0x1A),
    ILOAD_1(0x1B),
    ILOAD_2(0x1C),
    ILOAD_3(0x1D),
    LLOAD_0(0x1E),
    LLOAD_1(0x1F),
    LLOAD_2(0x20),
    LLOAD_3(0x21),
    FLOAD_0(0x22),
    FLOAD_1(0x23),
    FLOAD_2(0x24),
    FLOAD_3(0x25),
    DLOAD_0(0x26),
    DLOAD_1(0x27),
    DLOAD_2(0x28),
    DLOAD_3(0x29),
    ALOAD_0(0x2A),
    ALOAD_1(0x2B),
    ALOAD_2(0x2C),
    ALOAD_3(0x2D),
    IALOAD(0x2E),
    LALOAD(0x2F),
    FALOAD(0x30),
    DALOAD(0x31),
    AALOAD(0x32),
    BALOAD(0x33),
    CALOAD(0x34),
    SALOAD(0x35),
    ISTORE(0x36, "index"),
    LSTORE(0x37, "index"),
    FSTORE(0x38, "index"),
    DSTORE(0x39, "index"),
    ASTORE(0x3A, "index"),
    ISTORE_0(0x3B),
    ISTORE_1(0x3C),
    ISTORE_2(0x3D),
    ISTORE_3(0x3E),
    LSTORE_0(0x3F),
    LSTORE_1(0x40),
    LSTORE_2(0x41),
    LSTORE_3(0x42),
    FSTORE_0(0x43),
    FSTORE_1(0x44),
    FSTORE_2(0x45),
    FSTORE_3(0x46),
    DSTORE_0(0x47),
    DSTORE_1(0x48),
    DSTORE_2(0x49),
    DSTORE_3(0x4A),
    ASTORE_0(0x4B),
    ASTORE_1(0x4C),
    ASTORE_2(0x4D),
    ASTORE_3(0x4E),
    IASTORE(0x4F),
    LASTORE(0x50),
    FASTORE(0x51),
    DASTORE(0x52),
    AASTORE(0x53),
    BASTORE(0x54),
    CASTORE(0x55),
    SASTORE(0x56),
    POP(0x57),
    POP2(0x58),
    DUP(0x59),
    DUP_X1(0x5A),
    DUP_X2(0x5B),
    DUP2(0x5C),
    DUP2_X1(0x5D),
    DUP2_X2(0x5E),
    SWAP(0x5F),
    IADD(0x60),
    LADD(0x61),
    FADD(0x62),
    DADD(0x63),
    ISUB(0x64),
    LSUB(0x65),
    FSUB(0x66),
    DSUB(0x67),
    IMUL(0x68),
    LMUL(0x69),
    FMUL(0x6A),
    DMUL(0x6B),
    IDIV(0x6C),
    LDIV(0x6D),
    FDIV(0x6E),
    DDIV(0x6F),
    IREM(0x70),
    LREM(0x71),
    FREM(0x72),
    DREM(0x73),
    INEG(0x74),
    LNEG(0x75),
    FNEG(0x76),
    DNEG(0x77),
    ISHL(0x78),
    LSHL(0x79),
    ISHR(0x7A),
    LSHR(0x7B),
    IUSHR(0x7C),
    LUSHR(0x7D),
    IAND(0x7E),
    LAND(0x7F),
    IOR(0x80),
    LOR(0x81),
    IXOR(0x82),
    LXOR(0x83),
    IINC(0x84, "index", "const"),
    I2L(0x85),
    I2F(0x86),
    I2D(0x87),
    L2I(0x88),
    L2F(0x89),
    L2D(0x8A),
    F2I(0x8B),
    F2L(0x8C),
    F2D(0x8D),
    D2I(0x8E),
    D2L(0x8F),
    D2F(0x90),
    I2B(0x91),
    I2C(0x92),
    I2S(0x93),
    LCMP(0x94),
    FCMPL(0x95),
    FCMPG(0x96),
    DCMPL(0x97),
    DCMPG(0x98),
    IFEQ(0x99, "branchbyte1", "branchbyte2"),
    IFNE(0x9A, "branchbyte1", "branchbyte2"),
    IFLT(0x9B, "branchbyte1", "branchbyte2"),
    IFGE(0x9C, "branchbyte1", "branchbyte2"),
    IFGT(0x9D, "branchbyte1", "branchbyte2"),
    IFLE(0x9E, "branchbyte1", "branchbyte2"),
    IF_ICMPEQ(0x9F, "branchbyte1", "branchbyte2"),
    IF_ICMPNE(0xA0, "branchbyte1", "branchbyte2"),
    IF_ICMPLT(0xA1, "branchbyte1", "branchbyte2"),
    IF_ICMPGE(0xA2, "branchbyte1", "branchbyte2"),
    IF_ICMPGT(0xA3, "branchbyte1", "branchbyte2"),
    IF_ICMPLE(0xA4, "branchbyte1", "branchbyte2"),
    IF_ACMPEQ(0xA5, "branchbyte1", "branchbyte2"),
    IF_ACMPNE(0xA6, "branchbyte1", "branchbyte2"),
    GOTO(0xA7, "branchbyte1", "branchbyte2"),
    JSR(0xA8, "branchbyte1", "branchbyte2"),
    RET(0xA9, "index"),
    TABLESWITCH(0xAA)
            {
                /** Tableswitch instructions have a variable size, calculated by this
                 method. */
                public int getSize(byte[] code, int offset)
                {
                    int operandOffset = 4 - (offset % 4);
                    int low =  getInt32(code, offset + operandOffset + 4);
                    int high = getInt32(code, offset + operandOffset + 8);
                    return operandOffset + (3 + high - low + 1) * 4;
                }

                public String[] getByteLabels(byte[] code, int offset)
                {
                    final String[] byteLabels = new String[]{"pad", "pad", "pad",
                            "defaultbyte1", "defaultbyte2", "defaultbyte3", "defaultbyte4",
                            "lowbyte1", "lowbyte2", "lowbyte3", "lowbyte4",
                            "highbyte1", "highbyte2", "highbyte3", "highbyte4"};

                    return Arrays.copyOfRange(
                            byteLabels, offset % 4, byteLabels.length - 1);
                }
            },
    LOOKUPSWITCH(0xAB)
            {
                /** Lookupswitch instructions have a variable size, calculated by this
                 method. */
                public int getSize(byte[] code, int offset)
                {
                    int operandOffset = 4 - (offset % 4);
                    int npairs = getInt32(code, offset + operandOffset + 4);
                    return operandOffset + (2 + npairs * 2) * 4;
                }

                public String[] getByteLabels(byte[] code, int offset)
                {
                    final String[] byteLabels = new String[]{"pad", "pad", "pad",
                            "defaultbyte1", "defaultbyte2", "defaultbyte3", "defaultbyte4",
                            "npairs1", "npairs2", "npairs3", "npairs4"};

                    return Arrays.copyOfRange(
                            byteLabels, offset % 4, byteLabels.length - 1);
                }
            },
    IRETURN(0xAC),
    LRETURN(0xAD),
    FRETURN(0xAE),
    DRETURN(0xAF),
    ARETURN(0xB0),
    RETURN(0xB1),
    GETSTATIC(0xB2, "indexbyte1", "indexbyte2"),
    PUTSTATIC(0xB3, "indexbyte1", "indexbyte2"),
    GETFIELD(0xB4, "indexbyte1", "indexbyte2"),
    PUTFIELD(0xB5, "indexbyte1", "indexbyte2"),
    INVOKEVIRTUAL(0xB6, "indexbyte1", "indexbyte2"),
    INVOKESPECIAL(0xB7, "indexbyte1", "indexbyte2"),
    INVOKESTATIC(0xB8, "indexbyte1", "indexbyte2"),
    INVOKEINTERFACE(0xB9, "indexbyte1", "indexbyte2", "count", "0"),
    INVOKEDYNAMIC(0xBA, "indexbyte1", "indexbyte2", "0", "0"),
    NEW(0xBB, "indexbyte1", "indexbyte2"),
    NEWARRAY(0xBC, "atype"),
    ANEWARRAY(0xBD, "indexbyte1", "indexbyte2"),
    ARRAYLENGTH(0xBE),
    ATHROW(0xBF),
    CHECKCAST(0xC0, "indexbyte1", "indexbyte2"),
    INSTANCEOF(0xC1, "indexbyte1", "indexbyte2"),
    MONITORENTER(0xC2),
    MONITOREXIT(0xC3),
    WIDE(0xC4, "opcode", "indexbyte1", "indexbyte2", "constbyte1", "constbyte2")
            {
                /** Wide instructions have one of two forms, with different sizes. */
                public int getSize(byte[] code, int offset)
                {
                    Opcode opcode2 = Opcode.getOpcode(code[offset + 1]);
                    return (opcode2 == IINC) ? 6 : 4;
                }
            },
    MULTIANEWARRAY(0xC5, "indexbyte1", "indexbyte2", "dimensions"),
    IFNULL(0xC6, "branchbyte1", "branchbyte2"),
    IFNONNULL(0xC7, "branchbyte1", "branchbyte2"),
    GOTO_W(0xC8, "branchbyte1", "branchbyte2", "branchbyte3", "branchbyte4"),
    JSR_W(0xC9, "branchbyte1", "branchbyte2", "branchbyte3", "branchbyte4"),
    BREAKPOINT(0xCA),
    IMPDEP1(0xFE),
    IMPDEP2(0xFF);


    private static Opcode[] table = null;
    private final byte code;
    private final String[] byteLabels;

    private Opcode(int code, String... byteLabels)
    {
        this.code = (byte)code;
        this.byteLabels = byteLabels;
    }

    /**
     * Given a particular byte code value, returns the corresponding Opcode
     * object, or null if no such opcode exists.
     */
    public static Opcode getOpcode(byte code)
    {
        if(table == null)
        {
            // Initialise the lookup table. (This can't be done in the
            // constructor, because the construction of enum objects occurs
            // *before* static fields like 'table' are initialised.)

            table = new Opcode[256];
            Arrays.fill(table, null);

            for(Opcode o : Opcode.values())
            {
                table[(int)o.getCode() & 0xff] = o;
            }
        }

        // Note: the construct '(int)code & 0xff' is required because 'code'
        // is a signed 8-bit integer, which we want to treat as an *unsigned*
        // value.
        return table[(int)code & 0xff];
    }

    /**
     * Utility method used by TABLESWITCH and LOOKUPSWITCH. Reads a 32-bit
     * signed int value from a particular offset into a byte array.
     */
    private static int getInt32(byte[] code, int offset)
    {
        return ((int)code[offset]     << 24) |
                ((int)code[offset + 1] << 16) |
                ((int)code[offset + 2] << 8) |
                (int)code[offset + 3];
    }

    /** Returns the mnemonic (name) for this opcode. */
    public String getMnemonic()
    {
        // The name() method (automatically provided for enums) returns one of
        // the constant identifiers listed above (e.g. IF_ACMPNE). However,
        // mnemonics are traditionally given in lower case.
        return name().toLowerCase();
    }

    /** Returns the byte code value for this opcode. */
    public byte getCode()
    {
        return code;
    }

    /**
     * Determines the number of bytes taken up by an instruction beginning
     * with this opcode. For most opcodes, this is a fixed number, as returned
     * by this method. (TABLESWITCH, LOOKUPSWITCH and WIDE override this
     * method, because determining the length of those instructions is more
     * complicated.)
     */
    public int getSize(byte[] code, int offset)
    {
        return 1 + byteLabels.length;
    }

    /**
     * Returns an array of labels for the extra bytes (if any) that form part
     * of the instuction.
     *
     * For variable-length instructions where this method is overridden, the
     * array returned is *not* guaranteed to be the correct size. If it is too
     * short, the last few bytes will be unlabelled. If it is too long, the
     * last few labels will be unused.
     */
    public String[] getByteLabels(byte[] code, int offset)
    {
        return Arrays.copyOf(byteLabels, byteLabels.length);
    }
}
