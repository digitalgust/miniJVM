/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.io;

import com.sun.cldc.i18n.*;

/**
 * An OutputStreamWriter is a bridge from character streams to byte streams:
 * Characters written to it are translated into bytes.
 * The encoding that it uses may be specified by name, or the platform's default
 * encoding may be accepted.
 *
 * <p> Each invocation of a write() method causes the encoding converter to be
 * invoked on the given character(s).  The resulting bytes are accumulated in a
 * buffer before being written to the underlying output stream.  The size of
 * this buffer may be specified, but by default it is large enough for most
 * purposes.  Note that the characters passed to the write() methods are not
 * buffered.
 *
 * @version 12/17/01 (CLDC 1.1)
 * @author  Nik Shaylor
 * @see     java.io.Writer
 * @see     java.io.UnsupportedEncodingException
 * @since   CLDC 1.0
 */

public class OutputStreamWriter extends Writer {

    /**
     * The underlying character-output stream.
     */
    private Writer out;

    /**
     * Create an OutputStreamWriter that uses the default character encoding.
     *
     * @param  os  An OutputStream
     */
    public OutputStreamWriter(OutputStream os) {
        out = Helper.getStreamWriter(os);
    }

    /**
     * Create an OutputStreamWriter that uses the named character encoding.
     *
     * @param  os  An OutputStream
     * @param  enc  The name of a supported
     *
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     */
    public OutputStreamWriter(OutputStream os, String enc)
        throws UnsupportedEncodingException
    {
        out = Helper.getStreamWriter(os, enc);
    }

    /** Check to make sure that the stream has not been closed */
    private void ensureOpen() throws IOException {
        if (out == null) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Write a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(int c) throws IOException {
        ensureOpen();
        out.write(c);
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param  cbuf  Buffer of characters to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws IOException {
        ensureOpen();
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
            ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        out.write(cbuf, off, len);
    }

    /**
     * Write a portion of a string.
     *
     * @param  str  String to be written
     * @param  off  Offset from which to start reading characters
     * @param  len  Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(String str, int off, int len) throws IOException {
        ensureOpen();
        if ((off < 0) || (off > str.length()) || (len < 0) ||
            ((off + len) > str.length()) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        out.write(str, off, len);
    }

    /**
     * Flush the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void flush() throws IOException {
        ensureOpen();
        out.flush();
    }

    /**
     * Close the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        if (out != null) {
            out.close();
            out = null;
        }
    }

}

