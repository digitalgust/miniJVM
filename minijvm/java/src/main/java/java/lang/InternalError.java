/*
 * @(#)InternalError.java	1.21 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

/**
 * Thrown to indicate some unexpected internal error has occurred in 
 * the Java Virtual Machine. 
 *
 * @author  unascribed
 * @version 1.21, 12/19/03
 * @since   JDK1.0
 */
public
class InternalError extends VirtualMachineError {
    /**
     * Constructs an <code>InternalError</code> with no detail message. 
     */
    public InternalError() {
	super();
    }

    /**
     * Constructs an <code>InternalError</code> with the specified 
     * detail message. 
     *
     * @param   s   the detail message.
     */
    public InternalError(String s) {
	super(s);
    }
}
