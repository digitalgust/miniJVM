/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package java.lang;

/**
 * The class <code>Exception</code> and its subclasses are a form of
 * <code>Throwable</code> that indicates conditions that a reasonable
 * application might want to catch.
 *
 * @author Frank Yellin
 * @version 12/17/01 (CLDC 1.1)
 * @see java.lang.Error
 * @since JDK1.0, CLDC 1.0
 */
public class Exception extends Throwable {

    /**
     * Constructs an <code>Exception</code> with no specified detail message.
     */
    public Exception() {
        super();
    }

    /**
     * Constructs an <code>Exception</code> with the specified detail message.
     *
     * @param s the detail message.
     */
    public Exception(String s) {
        super(s);
    }

    public Exception(Throwable cause) {
        this.cause = cause;
    }

    public Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
