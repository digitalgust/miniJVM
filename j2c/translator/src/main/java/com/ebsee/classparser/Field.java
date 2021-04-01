package com.ebsee.classparser;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Parses and stores the fields from Java .class file
 *
 * @author Deshan Dissanayake
 */

public class Field {

    /* Field info variables */
    private int accessFlags;
    private int nameIndex;
    private int descriptorIndex;
    private int attributesCount;
    private Attribute attributes[];
    SignatureAttribute signatureAttribute = null;

    /* Other local variables */
    private String description;
    private String fieldName;
    private CPEntry cpEntry;
    private ConstantUtf8 constantUtf8;
    //
    ClassFile clazz;

    public Field(DataInputStream dis, ConstantPool cp, ClassFile clazz) throws IOException, InvalidConstantPoolIndex {
        this.clazz = clazz;
        /* Parsing the field info */
        accessFlags = dis.readUnsignedShort();
        nameIndex = dis.readUnsignedShort();
        descriptorIndex = dis.readUnsignedShort();
        description = ((ConstantUtf8) cp.getEntry(descriptorIndex)).getBytes();


        /* Parse the field name */
        cpEntry = cp.getEntry(nameIndex);
        if (cpEntry instanceof ConstantUtf8) {
            constantUtf8 = (ConstantUtf8) cpEntry;
            fieldName = new String(constantUtf8.getBytes());
        }

        attributesCount = dis.readUnsignedShort();
        attributes = new Attribute[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            attributes[i] = new Attribute(dis, cp);
            if (attributes[i].getSignatureAttribute() != null) {
                signatureAttribute = attributes[i].getSignatureAttribute();
            }
        }

    }

    public String getDescriptor() {
        return description;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ClassFile getClassFile() {
        return clazz;
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public String getSignature() {
        if (signatureAttribute != null) {
            return signatureAttribute.getSignature();
        } else {
            return null;
        }
    }

}
