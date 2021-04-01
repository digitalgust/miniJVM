package com.ebsee.classparser;

import java.io.DataInputStream;
import java.io.IOException;

public class SourceFileAttribute extends Attribute {
    String sourceFileName;

    public SourceFileAttribute(DataInputStream dis, ConstantPool cp) throws IOException, InvalidConstantPoolIndex {
        int sfutfIndex = dis.readUnsignedShort();
        CPEntry entry = cp.getEntry(sfutfIndex);
        ConstantUtf8 utfe = (ConstantUtf8) entry;
        sourceFileName = utfe.getBytes();
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

}