/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang;

/**
 * Thrown by the <code>charAt</code> method in class 
 * <code>String</code> and by other <code>String</code> 
 * methods to indicate that an index is either negative or greater 
 * than or equal to the size of the string. 
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see     java.lang.String#charAt(int)
 * @since   JDK1.0, CLDC 1.0
 */
public
class StringIndexOutOfBoundsException extends IndexOutOfBoundsException {
    /**
     * Constructs a <code>StringIndexOutOfBoundsException</code> with no 
     * detail message. 
     *
     * @since   JDK1.0.
     */
    public StringIndexOutOfBoundsException() {
        super();
    }

    /**
     * Constructs a <code>StringIndexOutOfBoundsException</code> with 
     * the specified detail message. 
     *
     * @param   s   the detail message.
     */
    public StringIndexOutOfBoundsException(String s) {
        super(s);
    }

    /**
     * Constructs a new <code>StringIndexOutOfBoundsException</code> 
     * class with an argument indicating the illegal index. 
     *
     * @param   index   the illegal index.
     */
    public StringIndexOutOfBoundsException(int index) {
        super("String index out of range: " + index);
    }
}

