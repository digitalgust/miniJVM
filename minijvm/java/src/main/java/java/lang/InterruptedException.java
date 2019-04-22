/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.lang;

/**
 * Thrown when a thread is waiting, sleeping, or otherwise paused for
 * a long time and another thread interrupts it.
 *
 * @author  Frank Yellin
 * @version 12/17/01 (CLDC 1.1)
 * @see     java.lang.Object#wait()
 * @see     java.lang.Object#wait(long)
 * @see     java.lang.Object#wait(long, int)
 * @see     java.lang.Thread#sleep(long)
 * @since   JDK1.0, CLDC 1.0
 */
public
class InterruptedException extends Exception {
    /**
     * Constructs an <code>InterruptedException</code> with no detail message.
     */
    public InterruptedException() {
        super();
    }

    /**
     * Constructs an <code>InterruptedException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public InterruptedException(String s) {
        super(s);
    }
}

