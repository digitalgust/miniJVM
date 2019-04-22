/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang;

/**
 * <code>RuntimeException</code> is the superclass of those 
 * exceptions that can be thrown during the normal operation of the 
 * Java Virtual Machine. 
 * <p>
 * A method is not required to declare in its <code>throws</code> 
 * clause any subclasses of <code>RuntimeException</code> that might 
 * be thrown during the execution of the method but not caught. 
 *
 *
 * @author  Frank Yellin
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
public
class RuntimeException extends Exception {
    /**
     * Constructs a <code>RuntimeException</code> with no detail  message.
     */
    public RuntimeException() {
        super();
    }

    /**
     * Constructs a <code>RuntimeException</code> with the specified 
     * detail message. 
     *
     * @param   s   the detail message.
     */
    public RuntimeException(String s) {
        super(s);
    }
    
    public RuntimeException(Throwable cause) {
        this.cause = cause;
    }
    
    public RuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}

