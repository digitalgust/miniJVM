/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang;

/**
 * Thrown to indicate that an attempt has been made to store the 
 * wrong type of object into an array of objects. For example, the 
 * following code generates an <code>ArrayStoreException</code>: 
 * <p><blockquote><pre>
 *     Object x[] = new String[3];
 *     x[0] = new Integer(0);
 * </pre></blockquote>
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
public
class ArrayStoreException extends RuntimeException {
    /**
     * Constructs an <code>ArrayStoreException</code> with no detail message. 
     */
    public ArrayStoreException() {
        super();
    }

    /**
     * Constructs an <code>ArrayStoreException</code> with the specified 
     * detail message. 
     *
     * @param   s   the detail message.
     */
    public ArrayStoreException(String s) {
        super(s);
    }
}

