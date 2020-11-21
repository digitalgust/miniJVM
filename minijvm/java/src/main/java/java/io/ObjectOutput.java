/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.io;

public interface ObjectOutput {
  public void close();
  public void flush();
  public void write(byte[] b);
  public void write(byte[] b, int off, int len);
  public void write(int b);
  public void writeObject(Object obj);
  public void writeInt(int f);
  public void writeLong(long f);
  public void writeByte(byte f);
  public void writeUnsignedByte(float f);
  public void writeChar(char f);
  public void writeShort(short f);
  public void writeUnsignedShort(float f);
  public void writeFloat(float f);
  public void writeDouble(double f);
  public void writeUTF(String f);
}
