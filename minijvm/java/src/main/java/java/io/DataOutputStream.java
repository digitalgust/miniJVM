/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package java.io;

/**
 * A data output stream lets an application write primitive Java data types to
 * an output stream in a portable way. An application can then use a data input
 * stream to read the data back in.
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see java.io.DataInputStream
 * @since JDK1.0, CLDC 1.0
 */
public class DataOutputStream extends OutputStream implements DataOutput {

    /**
     * The output stream.
     */
    protected OutputStream out;

    /**
     * Creates a new data output stream to write data to the specified
     * underlying output stream.
     *
     * @param out the underlying output stream, to be saved for later use.
     */
    public DataOutputStream(OutputStream out) {
        this.out = out;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument
     * <code>b</code>) to the underlying output stream.
     * <p>
     * Implements the <code>write</code> method of <code>OutputStream</code>.
     *
     * @param b the <code>byte</code> to be written.
     * @exception IOException if an I/O error occurs.
     */
    public void write(int b) throws IOException {
        out.write(b);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to the underlying output stream.
     *
     * @param b the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @exception IOException if an I/O error occurs.
     */
    public void write(byte b[], int off, int len) throws IOException {
        out.write(b, off, len);
    }

    /**
     * Flushes this data output stream. This forces any buffered output bytes to
     * be written out to the stream.
     * <p>
     * The <code>flush</code> method of <code>DataOutputStream</code> calls the
     * <code>flush</code> method of its underlying output stream.
     *
     * @exception IOException if an I/O error occurs.
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Closes this output stream and releases any system resources associated
     * with the stream.
     * <p>
     * The <code>close</code> method calls its <code>flush</code> method, and
     * then calls the <code>close</code> method of its underlying output stream.
     *
     * @exception IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        out.close();
    }

    /**
     * Writes a <code>boolean</code> to the underlying output stream as a 1-byte
     * value. The value <code>true</code> is written out as the value
     * <code>(byte)1</code>; the value <code>false</code> is written out as the
     * value <code>(byte)0</code>.
     *
     * @param v a <code>boolean</code> value to be written.
     * @exception IOException if an I/O error occurs.
     */
    public final void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    /**
     * Writes out a <code>byte</code> to the underlying output stream as a
     * 1-byte value.
     *
     * @param v a <code>byte</code> value to be written.
     * @exception IOException if an I/O error occurs.
     */
    public final void writeByte(int v) throws IOException {
        write(v);
    }

    /**
     * Writes a <code>short</code> to the underlying output stream as two bytes,
     * high byte first.
     *
     * @param v a <code>short</code> to be written.
     * @exception IOException if an I/O error occurs.
     */
    public final void writeShort(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    /**
     * Writes a <code>char</code> to the underlying output stream as a 2-byte
     * value, high byte first.
     *
     * @param v a <code>char</code> value to be written.
     * @exception IOException if an I/O error occurs.
     */
    public final void writeChar(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    /**
     * Writes an <code>int</code> to the underlying output stream as four bytes,
     * high byte first.
     *
     * @param v an <code>int</code> to be written.
     * @exception IOException if an I/O error occurs.
     */
    public final void writeInt(int v) throws IOException {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    /**
     * Writes a <code>long</code> to the underlying output stream as eight
     * bytes, high byte first.
     *
     * @param v a <code>long</code> to be written.
     * @exception IOException if an I/O error occurs.
     */
    public final void writeLong(long v) throws IOException {
        write((int) (v >>> 56) & 0xFF);
        write((int) (v >>> 48) & 0xFF);
        write((int) (v >>> 40) & 0xFF);
        write((int) (v >>> 32) & 0xFF);
        write((int) (v >>> 24) & 0xFF);
        write((int) (v >>> 16) & 0xFF);
        write((int) (v >>> 8) & 0xFF);
        write((int) (v >>> 0) & 0xFF);
    }

    /**
     * Converts the float argument to an <code>int</code> using the
     * <code>floatToIntBits</code> method in class <code>Float</code>, and then
     * writes that <code>int</code> value to the underlying output stream as a
     * 4-byte quantity, high byte first.
     *
     * @param v a <code>float</code> value to be written.
     * @exception IOException if an I/O error occurs.
     * @see java.lang.Float#floatToIntBits(float)
     * @since CLDC 1.1
     */
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /**
     * Converts the double argument to a <code>long</code> using the
     * <code>doubleToLongBits</code> method in class <code>Double</code>, and
     * then writes that <code>long</code> value to the underlying output stream
     * as an 8-byte quantity, high byte first.
     *
     * @param v a <code>double</code> value to be written.
     * @exception IOException if an I/O error occurs.
     * @see java.lang.Double#doubleToLongBits(double)
     * @since CLDC 1.1
     */
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /**
     * Writes a string to the underlying output stream as a sequence of
     * characters. Each character is written to the data output stream as if by
     * the <code>writeChar</code> method.
     *
     * @param s a <code>String</code> value to be written.
     * @exception IOException if an I/O error occurs.
     * @see java.io.DataOutputStream#writeChar(int)
     */
    public final void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int v = s.charAt(i);
            write((v >>> 8) & 0xFF);
            write((v >>> 0) & 0xFF);
        }
    }

    /**
     * Writes a string to the underlying output stream using UTF-8 encoding in a
     * machine-independent manner.
     * <p>
     * First, two bytes are written to the output stream as if by the
     * <code>writeShort</code> method giving the number of bytes to follow. This
     * value is the number of bytes actually written out, not the length of the
     * string. Following the length, each character of the string is output, in
     * sequence, using the UTF-8 encoding for the character.
     *
     * @param str a string to be written.
     * @exception IOException if an I/O error occurs.
     */
    public final void writeUTF(String str) throws IOException {
        writeUTF(str, this);
    }

    /**
     * Writes a string to the specified DataOutput using UTF-8 encoding in a
     * machine-independent manner.
     * <p>
     * First, two bytes are written to out as if by the <code>writeShort</code>
     * method giving the number of bytes to follow. This value is the number of
     * bytes actually written out, not the length of the string. Following the
     * length, each character of the string is output, in sequence, using the
     * UTF-8 encoding for the character.
     *
     * @param str a string to be written.
     * @param out destination to write to
     * @return The number of bytes written out.
     * @exception IOException if an I/O error occurs.
     */
    static final int writeUTF(String str, DataOutput out) throws IOException {
        int strlen = str.length();
        int utflen = 0;
        char[] charr = new char[strlen];
        int c, count = 0;

        str.getChars(0, strlen, charr, 0);

        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535) {
            throw new UTFDataFormatException();
        }

        byte[] bytearr = new byte[utflen + 2];
        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytearr[count++] = (byte) c;
            } else if (c > 0x07FF) {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            } else {
                bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            }
        }
        out.write(bytearr);

        return utflen + 2;
    }

    public final void writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            out.write((byte) s.charAt(i));
        }
    }

}
