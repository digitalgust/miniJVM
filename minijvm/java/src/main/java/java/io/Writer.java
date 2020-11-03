/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package java.io;

/**
 * Abstract class for writing to character streams. The only methods that a
 * subclass must implement are write(char[], int, int), flush(), and close().
 * Most subclasses, however, will override some of the methods defined here in
 * order to provide higher efficiency, additional functionality, or both.
 *
 * @version 12/17/01 (CLDC 1.1)
 * @author Mark Reinhold
 * @since JDK1.1, CLDC 1.0
 * @see java.io.OutputStreamWriter
 * @see java.io.Reader
 */
public abstract class Writer implements Appendable {

    /**
     * Temporary buffer used to hold writes of strings and single characters
     */
    private char[] writeBuffer;

    /**
     * Size of writeBuffer, must be >= 1
     */
    private final int writeBufferSize = 1024;

    /**
     * The object used to synchronize operations on this stream. For efficiency,
     * a character-stream object may use an object other than itself to protect
     * critical sections. A subclass should therefore use the object in this
     * field rather than <tt>this</tt> or a synchronized method.
     */
    protected Object lock;

    /**
     * Create a new character-stream writer whose critical sections will
     * synchronize on the writer itself.
     */
    protected Writer() {
        this.lock = this;
    }

    /**
     * Create a new character-stream writer whose critical sections will
     * synchronize on the given object.
     *
     * @param lock Object to synchronize on.
     */
    protected Writer(Object lock) {
        if (lock == null) {
            throw new NullPointerException();
        }
        this.lock = lock;
    }

    /**
     * Write a single character. The character to be written is contained in the
     * 16 low-order bits of the given integer value; the 16 high-order bits are
     * ignored.
     *
     * <p>
     * Subclasses that intend to support efficient single-character output
     * should override this method.
     *
     * @param c int specifying a character to be written.
     * @exception IOException If an I/O error occurs
     */
    public void write(int c) throws IOException {
        synchronized (lock) {
            if (writeBuffer == null) {
                writeBuffer = new char[writeBufferSize];
            }
            writeBuffer[0] = (char) c;
            write(writeBuffer, 0, 1);
        }
    }

    /**
     * Write an array of characters.
     *
     * @param cbuf Array of characters to be written
     *
     * @exception IOException If an I/O error occurs
     */
    public void write(char cbuf[]) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param cbuf Array of characters
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     *
     * @exception IOException If an I/O error occurs
     */
    abstract public void write(char cbuf[], int off, int len) throws IOException;

    /**
     * Write a string.
     *
     * @param str String to be written
     *
     * @exception IOException If an I/O error occurs
     */
    public void write(String str) throws IOException {
        write(str, 0, str.length());
    }

    /**
     * Write a portion of a string.
     *
     * @param str A String
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     *
     * @exception IOException If an I/O error occurs
     */
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            char cbuf[];
            if (len <= writeBufferSize) {
                if (writeBuffer == null) {
                    writeBuffer = new char[writeBufferSize];
                }
                cbuf = writeBuffer;
            } else {    // Don't permanently allocate very large buffers.
                cbuf = new char[len];
            }
            str.getChars(off, (off + len), cbuf, 0);
            write(cbuf, 0, len);
        }
    }

    /**
     * Flush the stream. If the stream has saved any characters from the various
     * write() methods in a buffer, write them immediately to their intended
     * destination. Then, if that destination is another character or byte
     * stream, flush it. Thus one flush() invocation will flush all the buffers
     * in a chain of Writers and OutputStreams.
     *
     * @exception IOException If an I/O error occurs
     */
    abstract public void flush() throws IOException;

    /**
     * Close the stream, flushing it first. Once a stream has been closed,
     * further write() or flush() invocations will cause an IOException to be
     * thrown. Closing a previously-closed stream, however, has no effect.
     *
     * @exception IOException If an I/O error occurs
     */
    abstract public void close() throws IOException;

    public Writer append(final char c) throws IOException {
        write((int) c);
        return this;
    }

    public Writer append(final CharSequence sequence) throws IOException {
        return append(sequence, 0, sequence.length());
    }

    public Writer append(CharSequence sequence, int start, int end)
            throws IOException {
        final int length = end - start;
        if (sequence instanceof String) {
            write((String) sequence, start, length);
        } else {
            final char[] charArray = new char[length];
            for (int i = start; i < end; i++) {
                charArray[i] = sequence.charAt(i);
            }
            write(charArray, 0, length);
        }
        return this;
    }

}
