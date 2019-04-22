/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang.ref;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Abstract base class for reference objects.  This class defines the
 * operations common to all reference objects. Because reference objects are
 * implemented in close cooperation with the garbage collector, this class may
 * not be subclassed directly.
 *
 * @version  12/19/01 (CLDC 1.1)
 * @author   Mark Reinhold
 * @since    JDK1.2, CLDC 1.1
 */

public abstract class Reference<T> {
    ReferenceQueue<T> queue=new ReferenceQueue();
    

    private T referent;   /* Treated specially by GC */
    private int    gcReserved; /* Treated specially by GC */

    /* -- Referent accessor and setters -- */

    /**
     * Returns this reference object's referent.  If this reference object has
     * been cleared, either by the program or by the garbage collector, then
     * this method returns <code>null</code>.
     *
     * @return   The object to which this reference refers, or
     *           <code>null</code> if this reference object has been cleared
     */
    public T get() {
        return this.referent;
    }

    /**
     * Clears this reference object.
     */
    public void clear() {
        this.referent = null;
    }

    /* -- Constructors -- */

    Reference(T referent) {
        this.referent = referent;
    }
    
    public Reference(T referent, ReferenceQueue<? super T> q) {
	
    }

}

