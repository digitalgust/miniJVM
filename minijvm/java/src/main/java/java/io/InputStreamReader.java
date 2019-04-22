/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.io;

import com.sun.cldc.i18n.*;

/**
 * An InputStreamReader is a bridge from byte streams to character streams: 
 * It reads bytes and translates them into characters.
 * The encoding that it uses may be specified by name, or the platform's
 * default encoding may be accepted.
 *
 * <p> Each invocation of one of an InputStreamReader's read() methods may
 * cause one or more bytes to be read from the underlying byte input stream.
 * To enable the efficient conversion of bytes to characters, more bytes may
 * be read ahead from the underlying stream than are necessary to satisfy the
 * current read operation.
 *
 * @version 12/17/01 (CLDC 1.1)
 * @author  Nik Shaylor, Antero Taivalsaari
 * @see     java.io.Reader
 * @see     java.io.UnsupportedEncodingException
 * @since   CLDC 1.0
 */

public class InputStreamReader extends Reader {

    /**
     * The underlying character input stream.
     */
    private Reader in;

    /**
     * Create an InputStreamReader that uses the default character encoding.
     *
     * @param  is   An InputStream
     */
    public InputStreamReader(InputStream is) {
        in = Helper.getStreamReader(is);
    }

    /**
     * Create an InputStreamReader that uses the named character encoding.
     *
     * @param  is   An InputStream
     * @param  enc  The name of a supported character encoding
     *
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     */
    public InputStreamReader(InputStream is, String enc)
        throws UnsupportedEncodingException
    {
        in = Helper.getStreamReader(is, enc);
    }

    /** 
     * Check to make sure that the stream has not been closed
     */
    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Read a single character.
     *
     * @return     The character read, or -1 if the end of the stream has
     *             been reached
     * 
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {
        ensureOpen();
        return in.read();
    }

    /**
     * Read characters into a portion of an array.
     *
     * @param      cbuf  Destination buffer
     * @param      off   Offset at which to start storing characters
     * @param      len   Maximum number of characters to read
     *
     * @return     The number of characters read, or -1 if the end of
     *             the stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        ensureOpen();
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
            ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        return in.read(cbuf, off, len);
    }

    /**
     * Skip characters.
     *
     * @param  n   The number of characters to skip
     *
     * @return     The number of characters actually skipped
     *
     * @exception  IllegalArgumentException  If <code>n</code> is negative.
     * @exception  IOException  If an I/O error occurs
     */
    public long skip(long n) throws IOException {
        ensureOpen();
        return in.skip(n);
    }

    /**
     * Tell whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input,
     * false otherwise.  Note that returning false does not guarantee that the
     * next read will block.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        ensureOpen();
        return in.ready();
    }

    /**
     * Tell whether this stream supports the mark() operation.
     *
     * @return true if and only if this stream supports the mark operation.
     */
    public boolean markSupported() {
        if (in == null) {
            return false;
        }
        return in.markSupported();
    }

    /**
     * Mark the present position in the stream.
     *
     * @param  readAheadLimit  Limit on the number of characters that may be
     *                         read while still preserving the mark.  After
     *                         reading this many characters, attempting to
     *                         reset the stream may fail.
     *
     * @exception  IOException  If the stream does not support mark(),
     *                          or if some other I/O error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        ensureOpen();
        if (in.markSupported()) {
            in.mark(readAheadLimit);
        } else {
            throw new IOException("mark() not supported");
        }
    }

    /**
     * Reset the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void reset() throws IOException {
        ensureOpen();
        in.reset();
    }

    /**
     * Close the stream.  Closing a previously closed stream
     * has no effect.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
    }

}

