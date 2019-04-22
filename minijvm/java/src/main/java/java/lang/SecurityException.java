/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package java.lang;

/**
 * Thrown by the system to indicate a security violation.
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
public class SecurityException extends RuntimeException {
    /**
     * Constructs a <code>SecurityException</code> with no detail  message.
     */
    public SecurityException() {
        super();
    }

    /**
     * Constructs a <code>SecurityException</code> with the specified
     * detail message.
     *
     * @param   s   the detail message.
     */
    public SecurityException(String s) {
        super(s);
    }
}

