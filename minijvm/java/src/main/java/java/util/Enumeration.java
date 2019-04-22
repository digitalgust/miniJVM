/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.util;

/**
 * An object that implements the Enumeration interface generates a
 * series of elements, one at a time. Successive calls to the
 * <code>nextElement</code> method return successive elements of the
 * series.
 * <p>
 * For example, to print all elements of a vector <i>v</i>:
 * <blockquote><pre>
 *     for (Enumeration e = v.elements() ; e.hasMoreElements() ;) {
 *         System.out.println(e.nextElement());<br>
 *     }
 * </pre></blockquote>
 * <p>
 * Methods are provided to enumerate through the elements of a
 * vector, the keys of a hashtable, and the values in a hashtable.
 *
 * @author  Lee Boynton
 * @version 12/17/01 (CLDC 1.1)
 * @see     java.util.Enumeration#nextElement()
 * @see     java.util.Hashtable
 * @see     java.util.Hashtable#elements()
 * @see     java.util.Hashtable#keys()
 * @see     java.util.Vector
 * @see     java.util.Vector#elements()
 * @since   JDK1.0, CLDC 1.0
 */
public interface Enumeration<E> {

    /**
     * Tests if this enumeration contains more elements.
     *
     * @return  <code>true</code> if and only if this enumeration object
     *           contains at least one more element to provide;
     *          <code>false</code> otherwise.
     */
    boolean hasMoreElements();

    /**
     * Returns the next element of this enumeration if this enumeration
     * object has at least one more element to provide.
     *
     * @return     the next element of this enumeration.
     * @exception  NoSuchElementException  if no more elements exist.
     */
    E nextElement();
}

