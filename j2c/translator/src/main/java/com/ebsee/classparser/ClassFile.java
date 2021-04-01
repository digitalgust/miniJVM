package com.ebsee.classparser;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Parses and stores a Java .class file.
 *
 * @author David Cooper
 */

public class ClassFile {


    private String filename;

    private long magic;
    private int minorVersion;
    private int majorVersion;

    private ConstantPool constantPool;
    private int accessFlags;

    private int thisClass;
    private int superClass;

    private int interfacesCount;
    private int interfaces[];
    private String interfaceClasses[];

    private int fieldsCount;
    private Field fields[];

    private int methodsCount;
    private Method methods[];
    private Method virtualMethods[];

    private int attributesCount;
    private Attribute attributes[];

    SignatureAttribute signatureAttribute = null;

    private CPEntry cpEntry;
    private ConstantClass constantClass;
    private String thisClassName;
    private String superClassName;
    private String option;


    /**
     * Parses a class file an constructs a ClassFile object.
     */
    public ClassFile(String filename, String option) throws ClassFileParserException, IOException {

        DataInputStream dis = new DataInputStream(new FileInputStream(filename));

        /* File name & option */
        this.filename = filename;
        this.option = option;

        /* Header */
        magic = (long) dis.readUnsignedShort() << 16 | dis.readUnsignedShort();
        minorVersion = dis.readUnsignedShort();
        majorVersion = dis.readUnsignedShort();

        /* Constant pool */
        constantPool = new ConstantPool(dis);
        accessFlags = dis.readUnsignedShort();

        /* Parse class name */
        thisClass = dis.readUnsignedShort();
        cpEntry = constantPool.getEntry(thisClass);
        if (cpEntry instanceof ConstantClass) {
            constantClass = (ConstantClass) cpEntry;
            thisClassName = new String(constantClass.getName());
        }

        /* Parse super class name */
        superClass = dis.readUnsignedShort();
        if (thisClassName.equals("java/lang/Object")) {
            superClass = -1;
            superClassName = null;
        } else {
            cpEntry = constantPool.getEntry(superClass);
            if (cpEntry instanceof ConstantClass) {
                constantClass = (ConstantClass) cpEntry;
                superClassName = new String(constantClass.getName());
            }
        }

        /* Interfaces */
        interfacesCount = dis.readUnsignedShort();
        interfaceClasses = new String[interfacesCount];
        interfaces = new int[interfacesCount];
        for (int i = 0; i < interfacesCount; i++) {
            interfaces[i] = dis.readUnsignedShort();
            interfaceClasses[i] = ((ConstantClass) constantPool.getEntry(interfaces[i])).getName();
        }

        /* Fields */
        fieldsCount = dis.readUnsignedShort();
        fields = new Field[fieldsCount];
        for (int i = 0; i < fieldsCount; i++) {
            fields[i] = new Field(dis, constantPool, this);
            //System.out.println(fields[i]);
        }

        /* Methods */
        methodsCount = dis.readUnsignedShort();
        methods = new Method[methodsCount];
        for (int i = 0; i < methodsCount; i++) {
            methods[i] = new Method(dis, constantPool, this);
            //System.out.println(methods[i]);
        }

        /* Attributes */
        attributesCount = dis.readUnsignedShort();
        attributes = new Attribute[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            attributes[i] = new Attribute(dis, constantPool);
            //System.out.println(attributes[i]);
            Attribute a = attributes[i];
            if (a.getSignatureAttribute() != null) {
                signatureAttribute = a.getSignatureAttribute();
            }
        }

        //gust
        virtualMethods = Arrays.asList(methods).stream().filter(m -> {
            int acc = m.getAccessFlags();
            return (acc & Modifier.STATIC) == 0
                    //&& (acc & Modifier.FINAL) == 0 //final can't override
                    && (acc & Modifier.PRIVATE) == 0
                    //&& (acc & Modifier.ABSTRACT) == 0
                    && m.getMethodName().indexOf("<") < 0//construct
                    ;
        }).collect(Collectors.toList()).toArray(new Method[0]);
    }


    /**
     * =======================================================================
     * Getters and Setters
     * =======================================================================
     */

    public String getFilename() {
        return filename;
    }

    public Method[] getMethods() {
        return methods;
    }

    public int getMethodsCount() {
        return methodsCount;
    }

    public Field[] getFields() {
        return fields;
    }

    public int getFieldsCount() {
        return fieldsCount;
    }

    public String getThisClassName() {
        return thisClassName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public int getInterfacesCount() {
        return interfacesCount;
    }

    public int[] getInterfaces() {
        return interfaces;
    }

    public Method[] getVirtualMethods() {
        return virtualMethods;
    }

    public String[] getInterfaceClasses() {
        return interfaceClasses;
    }


    public String getSourceFile() {
        for (Attribute a : attributes) {
            if (a.getSourceFileAttribute() != null) {
                return a.getSourceFileAttribute().getSourceFileName();
            }
        }
        return "";
    }


    public int getAccessFlags() {
        return accessFlags;
    }


    public boolean isInterface() {
        return (accessFlags & Modifier.INTERFACE) != 0;
    }

    public String toString() {
        return thisClassName + " extends " + superClassName;
    }

    public String getSignature() {
        if (signatureAttribute != null) {
            return signatureAttribute.getSignature();
        } else {
            return null;
        }
    }

}

