/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang;

/**
 * Thrown to indicate that an index of some sort (such as to an array,
 * to a string, or to a vector) is out of range. 
 * <p>
 * Applications can subclass this class to indicate similar exceptions. 
 *
 * @author  Frank Yellin
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
public
class IndexOutOfBoundsException extends RuntimeException {
    /**
     * Constructs an <code>IndexOutOfBoundsException</code> with no 
     * detail message. 
     */
    public IndexOutOfBoundsException() {
        super();
    }

    /**
     * Constructs an <code>IndexOutOfBoundsException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public IndexOutOfBoundsException(String s) {
        super(s);
    }
}

