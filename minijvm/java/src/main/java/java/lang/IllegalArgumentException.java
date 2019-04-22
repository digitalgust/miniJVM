/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang;

/**
 * Thrown to indicate that a method has been passed an illegal or 
 * inappropriate argument.
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see     java.lang.Thread#setPriority(int)
 * @since   JDK1.0, CLDC 1.0
 */
public
class IllegalArgumentException extends RuntimeException {
    /**
     * Constructs an <code>IllegalArgumentException</code> with no 
     * detail message. 
     */
    public IllegalArgumentException() {
        super();
    }

    /**
     * Constructs an <code>IllegalArgumentException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public IllegalArgumentException(String s) {
        super(s);
    }
    
    public IllegalArgumentException(Throwable cause) {
        super(cause);
    }


    public IllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}

