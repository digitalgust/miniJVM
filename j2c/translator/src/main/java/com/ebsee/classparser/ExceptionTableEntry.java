package com.ebsee.classparser;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Deshan Dissanayake
 */

public class ExceptionTableEntry extends Exception{
    private int startPC;
    private int endPC;
    private int handlerPC;
    private int catchType;

    public ExceptionTableEntry(DataInputStream dis) throws IOException {
        startPC = dis.readUnsignedShort();
        endPC = dis.readUnsignedShort();
        handlerPC = dis.readUnsignedShort();
        catchType = dis.readUnsignedShort();
    }


}
