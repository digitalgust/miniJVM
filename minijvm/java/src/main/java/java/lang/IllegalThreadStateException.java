/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.lang;

/**
 * Thrown to indicate that a thread is not in an appropriate state
 * for the requested operation. See, for example, the
 * <code>suspend</code> and <code>resume</code> methods in class
 * <code>Thread</code>.
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
public class IllegalThreadStateException extends IllegalArgumentException {
    /**
     * Constructs an <code>IllegalThreadStateException</code> with no
     * detail message.
     */
    public IllegalThreadStateException() {
        super();
    }

    /**
     * Constructs an <code>IllegalThreadStateException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public IllegalThreadStateException(String s) {
        super(s);
    }
}

