/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package java.io;

/**
 * Signals that an I/O exception of some sort has occurred. This class is the
 * general class of exceptions produced by failed or interrupted I/O operations.
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see java.io.InputStream
 * @see java.io.OutputStream
 * @since JDK1.0, CLDC 1.0
 */
public class IOException extends Exception {

    /**
     * Constructs an <code>IOException</code> with <code>null</code> as its
     * error detail message.
     */
    public IOException() {
        super();
    }

    /**
     * Constructs an <code>IOException</code> with the specified detail message.
     * The error message string <code>s</code> can later be retrieved by the
     * <code>{@link java.lang.Throwable#getMessage}</code> method of class
     * <code>java.lang.Throwable</code>.
     *
     * @param s the detail message.
     */
    public IOException(String s) {
        super(s);
    }

    public IOException(String message, Throwable cause) {
        super(message, cause);
    }

    public IOException(Throwable cause) {
        this(null, cause);
    }
}
