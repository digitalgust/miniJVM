/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.io;

/**
 * A <code>PrintStream</code> adds functionality to another output stream,
 * namely the ability to print representations of various data values
 * conveniently.  Two other features are provided as well.  Unlike other output
 * streams, a <code>PrintStream</code> never throws an
 * <code>IOException</code>; instead, exceptional situations merely set an
 * internal flag that can be tested via the <code>checkError</code> method.
 *
 * <p> All characters printed by a <code>PrintStream</code> are converted into
 * bytes using the platform's default character encoding.
 *
 * @version    12/17/01 (CLDC 1.1)
 * @author     Frank Yellin
 * @author     Mark Reinhold
 * @since      JDK1.0, CLDC 1.0
 */

public class PrintStream extends OutputStream {

    private boolean trouble = false;

    /**
     * Track both the text- and character-output streams, so that their buffers
     * can be flushed without flushing the entire stream.
     */
    private OutputStreamWriter charOut;
    private OutputStream       byteOut;

    /**
     * Create a new print stream.  This stream will not flush automatically.
     *
     * @param  out  The output stream to which values and objects will be
     *              printed
     */
    public PrintStream(OutputStream out) {
        if (out == null) {
            throw new NullPointerException("Null output stream");
        }
        byteOut = out;
        this.charOut = new OutputStreamWriter(out);
    }

    /**
     * Check to make sure that the stream has not been closed
     */
    private void ensureOpen() throws IOException {
        if (charOut == null)
            throw new IOException("Stream closed");
    }

    /**
     * Flush the stream.  This is done by writing any buffered output bytes to
     * the underlying output stream and then flushing that stream.
     *
     * @see        java.io.OutputStream#flush()
     */
    public void flush() {
        synchronized (this) {
            try {
                ensureOpen();
                charOut.flush();
            }
            catch (IOException x) {
                trouble = true;
            }
        }
    }

    private boolean closing = false; /* To avoid recursive closing */

    /**
     * Close the stream.  This is done by flushing the stream and then closing
     * the underlying output stream.
     *
     * @see        java.io.OutputStream#close()
     */
    public void close() {
        synchronized (this) {
            if (! closing) {
                closing = true;
                try {
                      charOut.close();
                }
                catch (IOException x) {
                    trouble = true;
                }
                charOut = null;
                byteOut = null;
            }
        }
    }

    /**
     * Flush the stream and check its error state.  The internal error state
     * is set to <code>true</code> when the underlying output stream throws an
     * <code>IOException</code>,
     * and when the <code>setError</code> method is invoked.
     *
     * @return True if and only if this stream has encountered an
     *         <code>IOException</code>, or the
     *         <code>setError</code> method has been invoked
     */
    public boolean checkError() {
        if (charOut != null)
            flush();
        return trouble;
    }

    /**
     * Set the error state of the stream to <code>true</code>.
     *
     * @since JDK1.1
     */
    protected void setError() {
        trouble = true;
    }


    /*
     * Exception-catching, synchronized output operations,
     * which also implement the write() methods of OutputStream
     */

    /**
     * Write the specified byte to this stream.
     *
     * <p> Note that the byte is written as given; to write a character that
     * will be translated according to the platform's default character
     * encoding, use the <code>print(char)</code> or <code>println(char)</code>
     * methods.
     *
     * @param  b  The byte to be written
     * @see #print(char)
     * @see #println(char)
     */
    public void write(int b) {
        try {
            synchronized (this) {
                ensureOpen();
                byteOut.write(b);
            }
        } catch (IOException x) {
            trouble = true;
        }
    }

    /**
     * Write <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to this stream.
     *
     * <p> Note that the bytes will be written as given; to write characters
     * that will be translated according to the platform's default character
     * encoding, use the <code>print(char)</code> or <code>println(char)</code>
     * methods.
     *
     * @param  buf   A byte array
     * @param  off   Offset from which to start taking bytes
     * @param  len   Number of bytes to write
     */
    public void write(byte buf[], int off, int len) {
        try {
            synchronized (this) {
                ensureOpen();
                byteOut.write(buf, off, len);
            }
        } catch (IOException x) {
            trouble = true;
        }
    }

    /*
     * The following private methods on the text- and character-output streams
     * always flush the stream buffers, so that writes to the underlying byte
     * stream occur as promptly as with the original PrintStream.
     */

    private void write(char buf[]) {
        try {
            synchronized (this) {
                ensureOpen();
                charOut.write(buf);
            }
        } catch (IOException x) {
            trouble = true;
        }
    }

    private void write(String s) {
        try {
            synchronized (this) {
                ensureOpen();
                charOut.write(s);
            }
        } catch (IOException x) {
            trouble = true;
        }
    }

    private void newLine() {
        try {
            synchronized (this) {
                ensureOpen();
                charOut.write('\n');
            }
        } catch (IOException x) {
            trouble = true;
        }
    }


    /* Methods that do not terminate lines */

    /**
     * Print a boolean value.  The string produced by <code>{@link
     * java.lang.String#valueOf(boolean)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      b   The <code>boolean</code> to be printed
     */
    public void print(boolean b) {
        write(b ? "true" : "false");
    }

    /**
     * Print a character.  The character is translated into one or more bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      c   The <code>char</code> to be printed
     */
    public void print(char c) {
        write(String.valueOf(c));
    }

    /**
     * Print an integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(int)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      i   The <code>int</code> to be printed
     * @see        java.lang.Integer#toString(int)
     */
    public void print(int i) {
        write(String.valueOf(i));
    }

    /**
     * Print a long integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(long)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      l   The <code>long</code> to be printed
     * @see        java.lang.Long#toString(long)
     */
    public void print(long l) {
        write(String.valueOf(l));
    }

    /**
     * Print a floating point number.  The string produced by 
     * <code>{@link java.lang.String#valueOf(float)}</code> is translated 
     * into bytes according to the platform's default character encoding, 
     * and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      f   The <code>float</code> to be printed
     * @see        java.lang.Float#toString(float)
     * @since      CLDC 1.1
     */
    public void print(float f) {
        write(String.valueOf(f));
    }

    /**
     * Print a double-precision floating point number.  The string produced by 
     * <code>{@link java.lang.String#valueOf(double)}</code> is translated 
     * into bytes according to the platform's default character encoding, 
     * and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      d   The <code>double</code> to be printed
     * @see        java.lang.Double#toString(double)
     * @since      CLDC 1.1
     */
    public void print(double d) {
        write(String.valueOf(d));
    }

    /**
     * Print an array of characters.  The characters are converted into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      s   The array of chars to be printed
     *
     * @throws  NullPointerException  If <code>s</code> is <code>null</code>
     */
    public void print(char s[]) {
        write(s);
    }

    /**
     * Print a string.  If the argument is <code>null</code> then the string
     * <code>"null"</code> is printed.  Otherwise, the string's characters are
     * converted into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      s   The <code>String</code> to be printed
     */
    public void print(String s) {
        if (s == null) {
            s = "null";
        }
        write(s);
    }

    /**
     * Print an object.  The string produced by the <code>{@link
     * java.lang.String#valueOf(Object)}</code> method is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      obj   The <code>Object</code> to be printed
     * @see        java.lang.Object#toString()
     */
    public void print(Object obj) {
        write(String.valueOf(obj));
    }


    /* Methods that do terminate lines */

    /**
     * Terminate the current line by writing the line separator string.  The
     * line separator string is defined by the system property
     * <code>line.separator</code>, and is not necessarily a single newline
     * character (<code>'\n'</code>).
     */
    public void println() {
        newLine();
    }

    /**
     * Print a boolean and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(boolean)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>boolean</code> to be printed
     */
    public void println(boolean x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a character and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(char)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>char</code> to be printed.
     */
    public void println(char x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print an integer and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(int)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>int</code> to be printed.
     */
    public void println(int x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a long and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(long)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>long</code> to be printed.
     */
    public void println(long x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a float and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(float)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>float</code> to be printed.
     * @since CLDC 1.1
     */
    public void println(float x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a double and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(double)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>double</code> to be printed.
     * @since CLDC 1.1
     */
    public void println(double x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print an array of characters and then terminate the line.  This method
     * behaves as though it invokes <code>{@link #print(char[])}</code> and
     * then <code>{@link #println()}</code>.
     *
     * @param x  an array of chars to print.
     */
    public void println(char x[]) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a String and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(String)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>String</code> to be printed.
     */
    public void println(String x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print an Object and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(Object)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>Object</code> to be printed.
     */
    public void println(Object x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

}

