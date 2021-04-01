package com.ebsee.classparser;

import java.io.DataInputStream;
import java.io.IOException;

public class SignatureAttribute extends Attribute {
    String signature;

    public SignatureAttribute(DataInputStream dis, ConstantPool cp) throws IOException, InvalidConstantPoolIndex {
        int sfutfIndex = dis.readUnsignedShort();
        CPEntry entry = cp.getEntry(sfutfIndex);
        ConstantUtf8 utfe = (ConstantUtf8) entry;
        signature = utfe.getBytes();
    }

    public String getSignature() {
        return signature;
    }

}