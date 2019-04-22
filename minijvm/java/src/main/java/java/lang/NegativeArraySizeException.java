/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang;

/**
 * Thrown if an application tries to create an array with negative size.
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
public
class NegativeArraySizeException extends RuntimeException {
    /**
     * Constructs a <code>NegativeArraySizeException</code> with no 
     * detail message. 
     */
    public NegativeArraySizeException() {
        super();
    }

    /**
     * Constructs a <code>NegativeArraySizeException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public NegativeArraySizeException(String s) {
        super(s);
    }
}

