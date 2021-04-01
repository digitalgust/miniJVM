package com.ebsee.classparser;
import java.io.*;

/**
 * Parses and stores the constant pool from a Java .class file.
 *
 * @author David Cooper
 */
public class ConstantPool
{
    private CPEntry[] entries;

    /**
     * Parses the constant pool, including the length, constructing a
     * ConstantPool object in the process.
     */
    public ConstantPool(DataInputStream dis) throws InvalidTagException,
            InvalidConstantPoolIndex,
            IOException
    {
        int len = dis.readUnsignedShort();
        entries = new CPEntry[len];
        int i;

        // Initialise entries to null.
        for(i = 0; i < len; i++)
        {
            entries[i] = null;
        }

        i = 1;
        while(i < len)
        {
            entries[i] = CPEntry.parse(dis);

            // We can't just have i++, because certain entries (Long and
            // Double) count for two entries.
            i += entries[i].getEntryCount();
        }

        // Once the constant pool has been parsed, resolve the various
        // internal references.
        for(i = 0; i < len; i++)
        {
            if(entries[i] != null)
            {
                entries[i].resolveReferences(this);
            }
        }
    }

    /** Retrieves a given constant pool entry. */
    public CPEntry getEntry(int index) throws InvalidConstantPoolIndex
    {
        if(index < 0 || index > entries.length)
        {
            throw new InvalidConstantPoolIndex(String.format(
                    "Invalid constant pool index: %d (not in range [0, %d])",
                    index, entries.length));
        }
        else if(entries[index] == null)
        {
            throw new InvalidConstantPoolIndex(String.format(
                    "Invalid constant pool index: %d (entry undefined)", index));
        }
        return entries[index];
    }

    /** Returns a formatted String representation of the constant pool. */
    public String toString()
    {
        String s = "Index  Entry type          Entry values\n" +
                "---------------------------------------\n";
        for(int i = 0; i < entries.length; i++)
        {
            if(entries[i] != null)
            {
                s += String.format("0x%02x   %-18s  %s\n",
                        i, entries[i].getTagString(), entries[i].getValues());
            }
        }
        return s;
    }
}

/**
 * Thrown when an invalid index into the constant pool is given. That is,
 * index is zero (or negative), greater than the index of the last entry, or
 * represents the (unused) entry following a Long or Double.
 */
class InvalidConstantPoolIndex extends ClassFileParserException
{
    public InvalidConstantPoolIndex(String msg) { super(msg); }
}
