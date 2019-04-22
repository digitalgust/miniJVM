/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang.ref;

/**
 * This class provides support for weak references. Weak references
 * are most often used to implement canonicalizing mappings.
 *
 * Suppose that the garbage collector determines at a certain
 * point in time that an object is weakly reachable.  At that
 * time it will atomically clear all the weak references to
 * that object and all weak references to any other weakly-
 * reachable objects from which that object is reachable
 * through a chain of strong and weak references.
 *
 * @version  12/19/01 (CLDC 1.1)
 * @author   Mark Reinhold (original JDK implementation)
 * @since    JDK1.2, CLDC 1.1
 */

/*
 * Implementation note: It is generally recommended that
 * you don't subclass the WeakReference class.  The garbage
 * collector treats weak references specially, and the
 * subclasses of class WeakReference might not be handled
 * correctly by the garbage collector.
 */

public class WeakReference extends Reference {

    /**
     * Creates a new weak reference that refers to the given object.
     */
    public WeakReference(Object ref) {
        super(ref);
        initializeWeakReference();
    }

    private void initializeWeakReference(){}
}

