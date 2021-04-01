package com.ebsee.classparser;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses and stores the attributes from Java .class file'
 *
 * @author Deshan Dissanayake
 */

public class CodeAttribute extends Attribute {

    /* Method info variables */
    private int attributeNameIndex;
    private int attributeLength;
    private int maxStack;
    private int maxLocals;
    private int codeLength;
    private byte[] code;
    private int exceptionTableLength;
    private ExceptionTableEntry[] exceptionTables;
    private int attributesCount;
    private Attribute attributes[];

    /* Other local variables */
    private String[] opcode;
    private String returnStr;
    private boolean invoked;
    private int invokeCountPerMethod;
    private CPEntry beingCalledMethod;                  // todo fix grammar being -> been
    private int beingCalledMethodNameIndex;
    private String beingCalledMethodName;
    private String beingCalledMethodType;
    private List<String> beingCalledMethodParameters;
    private List<String> beingCalledMethodNames;
    private List<String> beingCalledMethodSignatures;
    private String beingCalledMethodSignature;
    private MethodFormatter mf;
    private ConstantRef constantRef;
    private boolean overridden;
    private boolean returnValue;


    public CodeAttribute(DataInputStream dis, ConstantPool cp) throws IOException, InvalidConstantPoolIndex {

        invokeCountPerMethod = 0;
        beingCalledMethodNames = new ArrayList<>();
        beingCalledMethodSignatures = new ArrayList<>();
        beingCalledMethodParameters = new ArrayList<>();
        mf = new MethodFormatter();
        returnValue = false;

        maxStack = dis.readUnsignedShort();
        maxLocals = dis.readUnsignedShort();

        codeLength = dis.readInt();
        code = new byte[codeLength];
        opcode = new String[codeLength];
        for (int i = 0; i < codeLength; i++) {
            code[i] = dis.readByte();
            opcode[i] = String.format("0x%02x", code[i]);
        }

        getInvokes(cp);

        /* Parsing the exception table */
        exceptionTableLength = dis.readUnsignedShort();
        exceptionTables = new ExceptionTableEntry[exceptionTableLength];
        for (int i = 0; i < exceptionTableLength; i++) {
            exceptionTables[i] = new ExceptionTableEntry(dis);
        }

        /* Discard unwanted bytes */
        attributesCount = dis.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            dis.skipBytes(2);
            dis.skipBytes(dis.readInt());
        }
    }


    private void getInvokes(ConstantPool cp) throws InvalidConstantPoolIndex {
        for (int i = 0; i < codeLength; i++) {
            invoked = isInvoke(opcode[i]);
            overridden = isOverridden(opcode[i]);
            if (invoked) {
                invokeCountPerMethod++;
                //getInvokedMethod(cp, i);//bug here ,cant itertor bytecode per byte
            }
        }
    }


    /**
     * Count as a method invoke if bytecode is
     * either one of following
     * INVOKEVIRTUAL  = 0xb6
     * INVOKESPECIAL  = 0xb7
     * INVOKESTATIC   = 0xb8
     * INVOKEINTERFAC = 0xb9
     */
    private boolean isInvoke(String opcodeValue) {
        boolean returnValue = false;
        switch (opcodeValue) {
            case "0xb6":
            case "0xb7":
            case "0xb8":
            case "0xb9":
                returnValue = true;
                break;
            default:
                returnValue = false;
        }

        return returnValue;
    }


    /**
     * If bytecode value is INVOKESPECIAL  = 0xb7
     * then that method has overridden
     */
    private boolean isOverridden(String opcodeValue) {
        if (!returnValue) {
            switch (opcodeValue) {
                case "0xb7":
                    returnValue = true;
                    break;
                default:
                    returnValue = false;
            }
        }

        return returnValue;
    }


    /**
     * Parse details of the invoked method
     */
    private void getInvokedMethod(ConstantPool cp, int loc) throws InvalidConstantPoolIndex {

        beingCalledMethodNameIndex = Integer.parseInt(String.format("%02x", code[loc + 2]), 16);

        if (beingCalledMethodNameIndex > 0) {
            beingCalledMethod = cp.getEntry(beingCalledMethodNameIndex);
            constantRef = (ConstantRef) beingCalledMethod;

            beingCalledMethodName = constantRef.getName();
            beingCalledMethodType = constantRef.getType();

            beingCalledMethodParameters = mf.parseParameters(beingCalledMethodType);

            beingCalledMethodSignature = mf.getMethodSignature(beingCalledMethodName, beingCalledMethodParameters);

            beingCalledMethodNames.add(beingCalledMethodName);
            beingCalledMethodSignatures.add(beingCalledMethodSignature);
        }
    }


    /**
     * =======================================================================
     * Getters and Setters
     * =======================================================================
     */

    public int getInvokeCountPerMethod() {
        return invokeCountPerMethod;
    }

    public List<String> getBeingCalledMethodNames() {
        return beingCalledMethodNames;
    }

    public List<String> getBeingCalledMethodSignatures() {
        return beingCalledMethodSignatures;
    }

    public boolean isOverridden() {
        return overridden;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public byte[] getCode() {
        return code;
    }
}