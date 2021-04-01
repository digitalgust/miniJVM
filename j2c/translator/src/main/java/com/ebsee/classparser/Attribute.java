package com.ebsee.classparser;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Parses and stores the attributes from Java .class file'
 *
 * @author Deshan Dissanayake
 */

public class Attribute {

    /* Attribute info variables */
    private int attributeNameIndex;
    private int attributeLength;
    private byte[] info;

    /* Other local variables */
    private CPEntry cpEntry;
    private ConstantUtf8 constantUtf8;
    private String attributeName;
    private CodeAttribute codeAttribute;
    private SourceFileAttribute sourceFileAttribute;
    private SignatureAttribute signatureAttribute;

    public Attribute() {
    }

    public Attribute(DataInputStream dis, ConstantPool cp) throws IOException, InvalidConstantPoolIndex {

        /* Parsing  the attribute name */
        attributeNameIndex = dis.readUnsignedShort();
        cpEntry = cp.getEntry(attributeNameIndex);
        if (cpEntry instanceof ConstantUtf8) {
            constantUtf8 = (ConstantUtf8) cpEntry;
            attributeName = new String(constantUtf8.getBytes());
        }

        /* Parsing the attribute length */
        attributeLength = dis.readInt();

        /* Read only code attributes and discard others */
        if (attributeName.equals("Code")) {
            codeAttribute = new CodeAttribute(dis, cp);
        } else if (attributeName.equals("SourceFile")) {
            sourceFileAttribute = new SourceFileAttribute(dis, cp);
        } else if (attributeName.equals("Signature")) {
            signatureAttribute = new SignatureAttribute(dis, cp);
        } else {
            dis.skip(attributeLength);
        }

    }


    /**
     * =======================================================================
     * Getters and Setters
     * =======================================================================
     */

    public long getAttributeLength() {
        return attributeLength;
    }

    public CodeAttribute getCodeAttribute() {
        return codeAttribute;
    }

    public SourceFileAttribute getSourceFileAttribute() {
        return sourceFileAttribute;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public SignatureAttribute getSignatureAttribute() {
        return signatureAttribute;
    }


}
