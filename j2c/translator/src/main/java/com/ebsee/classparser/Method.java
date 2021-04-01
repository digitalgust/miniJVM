package com.ebsee.classparser;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Parses and stores the fields from Java .class file
 *
 * @author Deshan Dissanayake
 */

public class Method {

    /* Method info variables */
    private int accessFlags;
    private int nameIndex;
    private int descriptorIndex;
    private int attributesCount;
    private Attribute attributes[];
    CodeAttribute codeAttribute = null;
    SignatureAttribute signatureAttribute = null;


    /* Other local variables */
    private MethodFormatter mf;
    private String accessModifier;
    private String methodName;
    private String descriptor;
    private String returnType;
    private List<String> parameters;
    private CPEntry cpEntry;
    private ConstantUtf8 constantUtf8;
    //
    ClassFile clazz;

    public Method(DataInputStream dis, ConstantPool cp, ClassFile clazz) throws IOException, InvalidConstantPoolIndex {
        this.clazz = clazz;
        mf = new MethodFormatter();

        /* Parsing access and property modifier */
        accessFlags = dis.readUnsignedShort();
        accessModifier = mf.setModifier(accessFlags);

        /* Parsing the method name */
        nameIndex = dis.readUnsignedShort();
        cpEntry = cp.getEntry(nameIndex);
        if (cpEntry instanceof ConstantUtf8) {
            constantUtf8 = (ConstantUtf8) cpEntry;
            methodName = new String(constantUtf8.getBytes());
            //if (!methodName.equals("<init>")) methodName += "()";    // enable () front of method except <init>. uncomment if want
        }

        /* Parsing the method descriptor */
        descriptorIndex = dis.readUnsignedShort();
        cpEntry = cp.getEntry(descriptorIndex);
        if (cpEntry instanceof ConstantUtf8) {
            constantUtf8 = (ConstantUtf8) cpEntry;
            descriptor = constantUtf8.getBytes();

            /* Parse return type & parameters */
            returnType = mf.parseReturnType(descriptor);
            parameters = mf.parseParameters(descriptor);
        }

        /* Parsing the method attributes */
        attributesCount = dis.readUnsignedShort();
        attributes = new Attribute[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            attributes[i] = new Attribute(dis, cp);
            Attribute a = attributes[i];
            if (a.getSignatureAttribute() != null) {
                signatureAttribute = a.getSignatureAttribute();
            } else if (a.getCodeAttribute() != null) {
                codeAttribute = a.getCodeAttribute();
            }
        }

    }


    /**
     * =======================================================================
     * Getters and Setters
     * =======================================================================
     */

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public Attribute[] getAttributes() {
        return attributes;
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public ClassFile getClassFile() {
        return clazz;
    }

    public boolean isNative() {
        return (accessFlags & Modifier.NATIVE) != 0;
    }

    public boolean isBridge() {
        return (accessFlags & 0x1000) != 0;
    }

    public boolean isStatic() {
        return (accessFlags & Modifier.STATIC) != 0;
    }

    public boolean isAbstract() {
        return (accessFlags & Modifier.ABSTRACT) != 0;
    }

    public boolean isFinal() {
        return (accessFlags & Modifier.FINAL) != 0;
    }

    public boolean isSync() {
        return (accessFlags & Modifier.SYNCHRONIZED) != 0;
    }


    public CodeAttribute getCodeAttribute() {
        return codeAttribute;
    }

    public SignatureAttribute getSignatureAttribute() {
        return signatureAttribute;
    }

    public String getSignature() {
        if (signatureAttribute != null) {
            return signatureAttribute.getSignature();
        } else {
            return null;
        }
    }

    public String toString() {
        return clazz.getThisClassName() + "." + methodName + descriptor;
    }
}
