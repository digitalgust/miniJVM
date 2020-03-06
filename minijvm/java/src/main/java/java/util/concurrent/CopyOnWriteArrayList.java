/*
 * @(#)CopyOnWriteArrayList.java	1.8 04/04/14
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util.concurrent;
import java.util.*;

/**
 * A thread-safe variant of {@link ArrayList} in which all mutative
 * operations (add, set, and so on) are implemented by making a fresh
 * copy of the underlying array.
 *
 * <p> This is ordinarily too costly, but may be <em>more</em> efficient
 * than alternatives when traversal operations vastly outnumber
 * mutations, and is useful when you cannot or don't want to
 * synchronize traversals, yet need to preclude interference among
 * concurrent threads.  The "snapshot" style iterator method uses a
 * reference to the state of the array at the point that the iterator
 * was created. This array never changes during the lifetime of the
 * iterator, so interference is impossible and the iterator is
 * guaranteed not to throw <tt>ConcurrentModificationException</tt>.
 * The iterator will not reflect additions, removals, or changes to
 * the list since the iterator was created.  Element-changing
 * operations on iterators themselves (remove, set, and add) are not
 * supported. These methods throw
 * <tt>UnsupportedOperationException</tt>.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class CopyOnWriteArrayList<E>
        implements List<E>, Cloneable {
    private static final long serialVersionUID = 8673264195747942595L;

    /**
     * The held array. Directly accessed only within synchronized
     *  methods
     */
    private volatile transient E[] array;

    /**
     * Accessor to the array intended to be called from
     * within unsynchronized read-only methods
     **/
    private E[] array() { return array; }

    /**
     * Creates an empty list.
     */
    public CopyOnWriteArrayList() {
        array = (E[]) new Object[0];
    }

    /**
     * Creates a list containing the elements of the specified
     * Collection, in the order they are returned by the Collection's
     * iterator.
     * @param c the collection of initially held elements
     */
    public CopyOnWriteArrayList(Collection<? extends E> c) {
        array = (E[]) new Object[c.size()];
        Iterator<? extends E> i = c.iterator();
        int size = 0;
        while (i.hasNext())
            array[size++] = i.next();
    }

    /**
     * Create a new CopyOnWriteArrayList holding a copy of given array.
     *
     * @param toCopyIn the array (a copy of this array is used as the
     *        internal array)
     **/
    public CopyOnWriteArrayList(E[] toCopyIn) {
        copyIn(toCopyIn, 0, toCopyIn.length);
    }

    /**
     * Replace the held array with a copy of the <tt>n</tt> elements
     * of the provided array, starting at position <tt>first</tt>.  To
     * copy an entire array, call with arguments (array, 0,
     * array.length).
     * @param toCopyIn the array. A copy of the indicated elements of
     * this array is used as the internal array.
     * @param first The index of first position of the array to
     * start copying from.
     * @param n the number of elements to copy. This will be the new size of
     * the list.
     **/
    private synchronized void copyIn(E[] toCopyIn, int first, int n) {
        array  = (E[]) new Object[n];
        System.arraycopy(toCopyIn, first, array, 0, n);
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return  the number of elements in this list.
     */
    public int size() {
        return array().length;
    }

    /**
     * Tests if this list has no elements.
     *
     * @return  <tt>true</tt> if this list has no elements;
     *          <tt>false</tt> otherwise.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element.
     *
     * @param elem element whose presence in this List is to be tested.
     * @return  <code>true</code> if the specified element is present;
     *          <code>false</code> otherwise.
     */
    public boolean contains(Object elem) {
        E[] elementData = array();
        int len = elementData.length;
        return indexOf(elem, elementData, len) >= 0;
    }

    /**
     * Searches for the first occurrence of the given argument, testing
     * for equality using the <tt>equals</tt> method.
     *
     * @param   elem   an object.
     * @return  the index of the first occurrence of the argument in this
     *          list; returns <tt>-1</tt> if the object is not found.
     * @see     Object#equals(Object)
     */
    public int indexOf(Object elem) {
        E[] elementData = array();
        int len = elementData.length;
        return indexOf(elem, elementData, len);
    }

    /**
     * static version allows repeated call without needed
     * to grab lock for array each time
     **/
    private static int indexOf(Object elem, Object[] elementData, int len) {
        if (elem == null) {
            for (int i = 0; i < len; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = 0; i < len; i++)
                if (elem.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Searches for the first occurrence of the given argument, beginning
     * the search at <tt>index</tt>, and testing for equality using
     * the <tt>equals</tt> method.
     *
     * @param   elem    an object.
     * @param   index   the index to start searching from.
     * @return  the index of the first occurrence of the object argument in
     *          this List at position <tt>index</tt> or later in the
     *          List; returns <tt>-1</tt> if the object is not found.
     * @see     Object#equals(Object)
     */
    public int indexOf(E elem, int index) {
        E[] elementData = array();
        int elementCount = elementData.length;

        if (elem == null) {
            for (int i = index ; i < elementCount ; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index ; i < elementCount ; i++)
                if (elem.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified object in
     * this list.
     *
     * @param   elem   the desired element.
     * @return  the index of the last occurrence of the specified object in
     *          this list; returns -1 if the object is not found.
     */
    public int lastIndexOf(Object elem) {
        E[] elementData = array();
        int len = elementData.length;
        return lastIndexOf(elem, elementData, len);
    }

    private static int lastIndexOf(Object elem, Object[] elementData, int len) {
        if (elem == null) {
            for (int i = len-1; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = len-1; i >= 0; i--)
                if (elem.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Searches backwards for the specified object, starting from the
     * specified index, and returns an index to it.
     *
     * @param  elem    the desired element.
     * @param  index   the index to start searching from.
     * @return the index of the last occurrence of the specified object in this
     *          List at position less than index in the List;
     *          -1 if the object is not found.
     */
    public int lastIndexOf(E elem, int index) {
        // needed in order to compile on 1.2b3
        E[] elementData = array();
        if (elem == null) {
            for (int i = index; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (elem.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns a shallow copy of this list.  (The elements themselves
     * are not copied.)
     *
     * @return  a clone of this list.
     */
//    public Object clone() {
//        try {
//            E[] elementData = array();
//            CopyOnWriteArrayList<E> v = (CopyOnWriteArrayList<E>)super.clone();
//            v.array = (E[]) new Object[elementData.length];
//            System.arraycopy(elementData, 0, v.array, 0, elementData.length);
//            return v;
//        } catch (CloneNotSupportedException e) {
//            // this shouldn't happen, since we are Cloneable
//            throw new InternalError();
//        }
//    }

    /**
     * Returns an array containing all of the elements in this list
     * in the correct order.
     * @return an array containing all of the elements in this list
     *         in the correct order.
     */
    public Object[] toArray() {
        Object[] elementData = array();
        Object[] result = new Object[elementData.length];
        System.arraycopy(elementData, 0, result, 0, elementData.length);
        return result;
    }

    /**
     * Returns an array containing all of the elements in this list in the
     * correct order.  The runtime type of the returned array is that of the
     * specified array.  If the list fits in the specified array, it is
     * returned therein.  Otherwise, a new array is allocated with the runtime
     * type of the specified array and the size of this list.
     * <p>
     * If the list fits in the specified array with room to spare
     * (i.e., the array has more elements than the list),
     * the element in the array immediately following the end of the
     * collection is set to null.  This is useful in determining the length
     * of the list <em>only</em> if the caller knows that the list
     * does not contain any null elements.
     *
     * @param a the array into which the elements of the list are to
     *            be stored, if it is big enough; otherwise, a new array of the
     *            same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list.
     * @throws ArrayStoreException the runtime type of a is not a supertype
     * of the runtime type of every element in this list.
     */
    public <T> T[] toArray(T a[]) {
        E[] elementData = array();

        if (a.length < elementData.length)
            a = (T[])
            java.lang.reflect.Array.newInstance(a.getClass().getComponentType(),
            elementData.length);

        System.arraycopy(elementData, 0, a, 0, elementData.length);

        if (a.length > elementData.length)
            a[elementData.length] = null;

        return a;
    }

    // Positional Access Operations

    /**
     * Returns the element at the specified position in this list.
     *
     * @param  index index of element to return.
     * @return the element at the specified position in this list.
     * @throws    IndexOutOfBoundsException if index is out of range <tt>(index
     *            &lt; 0 || index &gt;= size())</tt>.
     */
    public E get(int index) {
        E[] elementData = array();
        rangeCheck(index, elementData.length);
        return elementData[index];
    }

    /**
     * Replaces the element at the specified position in this list with
     * the specified element.
     *
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws    IndexOutOfBoundsException if index out of range
     *            <tt>(index &lt; 0 || index &gt;= size())</tt>.
     */
    public synchronized E set(int index, E element) {
        int len = array.length;
        rangeCheck(index, len);
        E oldValue = array[index];

        boolean same = (oldValue == element ||
        (element != null && element.equals(oldValue)));
        if (!same) {
            E[] newArray = (E[]) new Object[len];
            System.arraycopy(array, 0, newArray, 0, len);
            newArray[index] = element;
            array = newArray;
        }
        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param element element to be appended to this list.
     * @return true (as per the general contract of Collection.add).
     */
    public synchronized boolean add(E element) {
        int len = array.length;
        E[] newArray = (E[]) new Object[len+1];
        System.arraycopy(array, 0, newArray, 0, len);
        newArray[len] = element;
        array = newArray;
        return true;
    }

    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * @throws    IndexOutOfBoundsException if index is out of range
     *            <tt>(index &lt; 0 || index &gt; size())</tt>.
     */
    public synchronized void add(int index, E element) {
        int len = array.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+len);

        E[] newArray = (E[]) new Object[len+1];
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = element;
        System.arraycopy(array, index, newArray, index+1, len - index);
        array = newArray;
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).
     *
     * @param index the index of the element to removed.
     * @return the element that was removed from the list.
     * @throws    IndexOutOfBoundsException if index out of range <tt>(index
     *            &lt; 0 || index &gt;= size())</tt>.
     */
    public synchronized E remove(int index) {
        int len = array.length;
        rangeCheck(index, len);
        E oldValue = array[index];
        E[] newArray = (E[]) new Object[len-1];
        System.arraycopy(array, 0, newArray, 0, index);
        int numMoved = len - index - 1;
        if (numMoved > 0)
            System.arraycopy(array, index+1, newArray, index, numMoved);
        array = newArray;
        return oldValue;
    }

    /**
     * Removes a single instance of the specified element from this
     * list, if it is present (optional operation).  More formally,
     * removes an element <tt>e</tt> such that <tt>(o==null ? e==null :
     * o.equals(e))</tt>, if the list contains one or more such
     * elements.  Returns <tt>true</tt> if the list contained the
     * specified element (or equivalently, if the list changed as a
     * result of the call).<p>
     *
     * @param o element to be removed from this list, if present.
     * @return <tt>true</tt> if the list contained the specified element.
     */
    public synchronized boolean remove(Object o) {
        int len = array.length;
        if (len == 0) return false;

        // Copy while searching for element to remove
        // This wins in the normal case of element being present

        int newlen = len-1;
        E[] newArray = (E[]) new Object[newlen];

        for (int i = 0; i < newlen; ++i) {
            if (o == array[i] ||
            (o != null && o.equals(array[i]))) {
                // found one;  copy remaining and exit
                for (int k = i + 1; k < len; ++k) newArray[k-1] = array[k];
                array = newArray;
                return true;
            } else
                newArray[i] = array[i];
        }
        // special handling for last cell

        if (o == array[newlen] ||
        (o != null && o.equals(array[newlen]))) {
            array = newArray;
            return true;
        } else
            return false; // throw away copy
    }


    /**
     * Removes from this List all of the elements whose index is between
     * fromIndex, inclusive and toIndex, exclusive.  Shifts any succeeding
     * elements to the left (reduces their index).
     * This call shortens the list by <tt>(toIndex - fromIndex)</tt> elements.
     * (If <tt>toIndex==fromIndex</tt>, this operation has no effect.)
     *
     * @param fromIndex index of first element to be removed.
     * @param toIndex index after last element to be removed.
     * @throws IndexOutOfBoundsException fromIndex or toIndex out of
     *              range (fromIndex &lt; 0 || fromIndex &gt;= size() || toIndex
     *              &gt; size() || toIndex &lt; fromIndex).
     */
    private synchronized void removeRange(int fromIndex, int toIndex) {
        int len = array.length;

        if (fromIndex < 0 || fromIndex >= len ||
        toIndex > len || toIndex < fromIndex)
            throw new IndexOutOfBoundsException();

        int numMoved = len - toIndex;
        int newlen = len - (toIndex-fromIndex);
        E[] newArray = (E[]) new Object[newlen];
        System.arraycopy(array, 0, newArray, 0, fromIndex);
        System.arraycopy(array, toIndex, newArray, fromIndex, numMoved);
        array = newArray;
    }


    /**
     * Append the element if not present.
     * @param element element to be added to this Collection, if absent.
     * @return true if added
     **/
    public synchronized boolean addIfAbsent(E element) {
        // Copy while checking if already present.
        // This wins in the most common case where it is not present
        int len = array.length;
        E[] newArray = (E[]) new Object[len + 1];
        for (int i = 0; i < len; ++i) {
            if (element == array[i] ||
            (element != null && element.equals(array[i])))
                return false; // exit, throwing away copy
            else
                newArray[i] = array[i];
        }
        newArray[len] = element;
        array = newArray;
        return true;
    }

    /**
     * Returns true if this Collection contains all of the elements in the
     * specified Collection.
     * <p>
     * This implementation iterates over the specified Collection, checking
     * each element returned by the Iterator in turn to see if it's
     * contained in this Collection.  If all elements are so contained
     * true is returned, otherwise false.
     * @param c the collection
     * @return true if all elements are contained
     */
    public boolean containsAll(Collection<?> c) {
        E[] elementData = array();
        int len = elementData.length;
        Iterator e = c.iterator();
        while (e.hasNext())
            if (indexOf(e.next(), elementData, len) < 0)
                return false;

        return true;
    }


    /**
     * Removes from this Collection all of its elements that are contained in
     * the specified Collection. This is a particularly expensive operation
     * in this class because of the need for an internal temporary array.
     * <p>
     *
     * @param c the collection
     * @return true if this Collection changed as a result of the call.
     */
    public synchronized boolean removeAll(Collection<?> c) {
        E[] elementData = array;
        int len = elementData.length;
        if (len == 0) return false;

        // temp array holds those elements we know we want to keep
        E[] temp = (E[]) new Object[len];
        int newlen = 0;
        for (int i = 0; i < len; ++i) {
            E element = elementData[i];
            if (!c.contains(element)) {
                temp[newlen++] = element;
            }
        }

        if (newlen == len) return false;

        //  copy temp as new array
        E[] newArray = (E[]) new Object[newlen];
        System.arraycopy(temp, 0, newArray, 0, newlen);
        array = newArray;
        return true;
    }

    /**
     * Retains only the elements in this Collection that are contained in the
     * specified Collection (optional operation).  In other words, removes from
     * this Collection all of its elements that are not contained in the
     * specified Collection.
     * @param c the collection
     * @return true if this Collection changed as a result of the call.
     */
    public synchronized boolean retainAll(Collection<?> c) {
        E[] elementData = array;
        int len = elementData.length;
        if (len == 0) return false;

        E[] temp = (E[]) new Object[len];
        int newlen = 0;
        for (int i = 0; i < len; ++i) {
            E element = elementData[i];
            if (c.contains(element)) {
                temp[newlen++] = element;
            }
        }

        if (newlen == len) return false;

        E[] newArray = (E[]) new Object[newlen];
        System.arraycopy(temp, 0, newArray, 0, newlen);
        array = newArray;
        return true;
    }

    /**
     * Appends all of the elements in the specified Collection that
     * are not already contained in this list, to the end of
     * this list, in the order that they are returned by the
     * specified Collection's Iterator.
     *
     * @param c elements to be added into this list.
     * @return the number of elements added
     */
    public synchronized int addAllAbsent(Collection<? extends E> c) {
        int numNew = c.size();
        if (numNew == 0) return 0;

        E[] elementData = array;
        int len = elementData.length;

        E[] temp = (E[]) new Object[numNew];
        int added = 0;
        Iterator<? extends E> e = c.iterator();
        while (e.hasNext()) {
            E element = e.next();
            if (indexOf(element, elementData, len) < 0) {
                if (indexOf(element, temp, added) < 0) {
                    temp[added++] = element;
                }
            }
        }

        if (added == 0) return 0;

        E[] newArray = (E[]) new Object[len+added];
        System.arraycopy(elementData, 0, newArray, 0, len);
        System.arraycopy(temp, 0, newArray, len, added);
        array = newArray;
        return added;
    }

    /**
     * Removes all of the elements from this list.
     *
     */
    public synchronized void clear() {
        array = (E[]) new Object[0];
    }

    /**
     * Appends all of the elements in the specified Collection to the end of
     * this list, in the order that they are returned by the
     * specified Collection's Iterator.
     *
     * @param c elements to be inserted into this list.
     * @return true if any elements are added
     */
    public synchronized boolean addAll(Collection<? extends E> c) {
        int numNew = c.size();
        if (numNew == 0) return false;

        int len = array.length;
        E[] newArray = (E[]) new Object[len+numNew];
        System.arraycopy(array, 0, newArray, 0, len);
        Iterator<? extends E> e = c.iterator();
        for (int i=0; i<numNew; i++)
            newArray[len++] = e.next();
        array = newArray;

        return true;
    }

    /**
     * Inserts all of the elements in the specified Collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified Collection's iterator.
     *
     * @param index index at which to insert first element
     *                from the specified collection.
     * @param c elements to be inserted into this list.
     * @throws IndexOutOfBoundsException index out of range (index
     *              &lt; 0 || index &gt; size()).
     * @return true if any elements are added
     */
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        int len = array.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+len);

        int numNew = c.size();
        if (numNew == 0) return false;

        E[] newArray = (E[]) new Object[len+numNew];
        System.arraycopy(array, 0, newArray, 0, len);
        int numMoved = len - index;
        if (numMoved > 0)
            System.arraycopy(array, index, newArray, index + numNew, numMoved);
        Iterator<? extends E> e = c.iterator();
        for (int i=0; i<numNew; i++)
            newArray[index++] = e.next();
        array = newArray;

        return true;
    }

    /**
     * Check if the given index is in range.  If not, throw an appropriate
     * runtime exception.
     */
    private void rangeCheck(int index, int length) {
        if (index >= length || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+ length);
    }

    /**
     * Save the state of the list to a stream (i.e., serialize it).
     *
     * @serialData The length of the array backing the list is emitted
     *               (int), followed by all of its elements (each an Object)
     *               in the proper order.
     * @param s the stream
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{

        // Write out element count, and any hidden stuff
        s.defaultWriteObject();

        E[] elementData = array();
        // Write out array length
        s.writeInt(elementData.length);

        // Write out all elements in the proper order.
        for (int i=0; i<elementData.length; i++)
            s.writeObject(elementData[i]);
    }

    /**
     * Reconstitute the list from a stream (i.e., deserialize it).
     * @param s the stream
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {

        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in array length and allocate array
        int arrayLength = s.readInt();
        E[] elementData = (E[]) new Object[arrayLength];

        // Read in all elements in the proper order.
        for (int i=0; i<elementData.length; i++)
            elementData[i] = (E) s.readObject();
        array = elementData;
    }

    /**
     * Returns a string representation of this Collection, containing
     * the String representation of each element.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        Iterator e = iterator();
        buf.append("[");
        int maxIndex = size() - 1;
        for (int i = 0; i <= maxIndex; i++) {
            buf.append(String.valueOf(e.next()));
            if (i < maxIndex)
                buf.append(", ");
        }
        buf.append("]");
        return buf.toString();
    }


    /**
     * Compares the specified Object with this List for equality.  Returns true
     * if and only if the specified Object is also a List, both Lists have the
     * same size, and all corresponding pairs of elements in the two Lists are
     * <em>equal</em>.  (Two elements <tt>e1</tt> and <tt>e2</tt> are
     * <em>equal</em> if <tt>(e1==null ? e2==null : e1.equals(e2))</tt>.)
     * In other words, two Lists are defined to be equal if they contain the
     * same elements in the same order.
     * <p>
     * This implementation first checks if the specified object is this
     * List. If so, it returns true; if not, it checks if the specified
     * object is a List. If not, it returns false; if so, it iterates over
     * both lists, comparing corresponding pairs of elements.  If any
     * comparison returns false, this method returns false.  If either
     * Iterator runs out of elements before the other it returns false
     * (as the Lists are of unequal length); otherwise it returns true when
     * the iterations complete.
     *
     * @param o the Object to be compared for equality with this List.
     * @return true if the specified Object is equal to this List.
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        List<E> l2 = (List<E>)(o);
        if (size() != l2.size())
            return false;

        ListIterator<E> e1 = listIterator();
        ListIterator<E> e2 = l2.listIterator();
        while(e1.hasNext()) {
            E o1 = e1.next();
            E o2 = e2.next();
            if (!(o1==null ? o2==null : o1.equals(o2)))
                return false;
        }
        return true;
    }

    /**
     * Returns the hash code value for this List.
     *
     * <p> This implementation uses the definition in {@link
     * List#hashCode}.
     * @return the hash code
     */
    public int hashCode() {
        int hashCode = 1;
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            E obj = i.next();
            hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    /**
     * Returns an Iterator over the elements contained in this collection.
     * The iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is
     * needed while traversing the iterator. The iterator does
     * <em>NOT</em> support the <tt>remove</tt> method.
     * @return the iterator
     */
    public Iterator<E> iterator() {
        return new COWIterator<E>(array(), 0);
    }

    /**
     * Returns an Iterator of the elements in this List (in proper sequence).
     * The iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is
     * needed while traversing the iterator. The iterator does
     * <em>NOT</em> support the <tt>remove</tt>, <tt>set</tt>,
     * or <tt>add</tt> methods.
     * @return the iterator
     *
     */
    public ListIterator<E> listIterator() {
        return new COWIterator<E>(array(), 0);
    }

    /**
     * Returns a ListIterator of the elements in this List (in proper
     * sequence), starting at the specified position in the List.  The
     * specified index indicates the first element that would be returned by
     * an initial call to nextElement.  An initial call to previousElement
     * would return the element with the specified index minus one.
     * The ListIterator returned by this implementation will throw
     * an UnsupportedOperationException in its remove, set and
     * add methods.
     *
     * @param index index of first element to be returned from the
     *                ListIterator (by a call to getNext).
     * @return the iterator
     * @throws IndexOutOfBoundsException index is out of range
     *              (index &lt; 0 || index &gt; size()).
     */
    public ListIterator<E> listIterator(final int index) {
        E[] elementData = array();
        int len = elementData.length;
        if (index<0 || index>len)
            throw new IndexOutOfBoundsException("Index: "+index);

        return new COWIterator<E>(array(), index);
    }

    private static class COWIterator<E> implements ListIterator<E> {

        /** Snapshot of the array **/
        private final E[] array;

        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor;

        private COWIterator(E[] elementArray, int initialCursor) {
            array = elementArray;
            cursor = initialCursor;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public E next() {
            try {
                return array[cursor++];
            } catch (IndexOutOfBoundsException ex) {
                throw new NoSuchElementException();
            }
        }

        public E previous() {
            try {
                return array[--cursor];
            } catch(IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException remove is not supported
         *            by this Iterator.
         */

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException set is not supported
         *            by this Iterator.
         */
        public void set(E o) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException add is not supported
         *            by this Iterator.
         */
        public void add(E o) {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * Returns a view of the portion of this List between fromIndex,
     * inclusive, and toIndex, exclusive.  The returned List is backed by this
     * List, so changes in the returned List are reflected in this List, and
     * vice-versa.  While mutative operations are supported, they are
     * probably not very useful for CopyOnWriteArrayLists.
     * <p>
     * The semantics of the List returned by this method become undefined if
     * the backing list (i.e., this List) is <i>structurally modified</i> in
     * any way other than via the returned List.  (Structural modifications are
     * those that change the size of the List, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *
     * @param fromIndex low endpoint (inclusive) of the subList.
     * @param toIndex high endpoint (exclusive) of the subList.
     * @return a view of the specified range within this List.
     * @throws IndexOutOfBoundsException Illegal endpoint index value
     *     (fromIndex &lt; 0 || toIndex &gt; size || fromIndex &gt; toIndex).
     */
    public synchronized List<E> subList(int fromIndex, int toIndex) {
        // synchronized since sublist constructor depends on it.
        int len = array.length;
        if (fromIndex<0 || toIndex>len  || fromIndex>toIndex)
            throw new IndexOutOfBoundsException();
        return new COWSubList<E>(this, fromIndex, toIndex);
    }

    private static class COWSubList<E> extends AbstractList<E> {

        /*
          This class extends AbstractList merely for convenience, to
          avoid having to define addAll, etc. This doesn't hurt, but
          is wasteful.  This class does not need or use modCount
          mechanics in AbstractList, but does need to check for
          concurrent modification using similar mechanics.  On each
          operation, the array that we expect the backing list to use
          is checked and updated.  Since we do this for all of the
          base operations invoked by those defined in AbstractList,
          all is well.  While inefficient, this is not worth
          improving.  The kinds of list operations inherited from
          AbstractList are already so slow on COW sublists that
          adding a bit more space/time doesn't seem even noticeable.
         */

        private final CopyOnWriteArrayList<E> l;
        private final int offset;
        private int size;
        private E[] expectedArray;

        private COWSubList(CopyOnWriteArrayList<E> list,
        int fromIndex, int toIndex) {
            l = list;
            expectedArray = l.array();
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        // only call this holding l's lock
        private void checkForComodification() {
            if (l.array != expectedArray)
                throw new ConcurrentModificationException();
        }

        // only call this holding l's lock
        private void rangeCheck(int index) {
            if (index<0 || index>=size)
                throw new IndexOutOfBoundsException("Index: "+index+ ",Size: "+size);
        }


        public E set(int index, E element) {
            synchronized(l) {
                rangeCheck(index);
                checkForComodification();
                E x = l.set(index+offset, element);
                expectedArray = l.array;
                return x;
            }
        }

        public E get(int index) {
            synchronized(l) {
                rangeCheck(index);
                checkForComodification();
                return l.get(index+offset);
            }
        }

        public int size() {
            synchronized(l) {
                checkForComodification();
                return size;
            }
        }

        public void add(int index, E element) {
            synchronized(l) {
                checkForComodification();
                if (index<0 || index>size)
                    throw new IndexOutOfBoundsException();
                l.add(index+offset, element);
                expectedArray = l.array;
                size++;
            }
        }

        public void clear() {
            synchronized(l) {
                checkForComodification();
                l.removeRange(offset, offset+size);
                expectedArray = l.array;
                size = 0;
            }
        }

        public E remove(int index) {
            synchronized(l) {
                rangeCheck(index);
                checkForComodification();
                E result = l.remove(index+offset);
                expectedArray = l.array;
                size--;
                return result;
            }
        }

        public Iterator<E> iterator() {
            synchronized(l) {
                checkForComodification();
                return new COWSubListIterator<E>(l, 0, offset, size);
            }
        }

        public ListIterator<E> listIterator(final int index) {
            synchronized(l) {
                checkForComodification();
                if (index<0 || index>size)
                    throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
                return new COWSubListIterator<E>(l, index, offset, size);
            }
        }

        public List<E> subList(int fromIndex, int toIndex) {
            synchronized(l) {
                checkForComodification();
                if (fromIndex<0 || toIndex>size)
                    throw new IndexOutOfBoundsException();
                return new COWSubList<E>(l, fromIndex+offset, toIndex+offset);
            }
        }

    }


    private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> i;
        private final int index;
        private final int offset;
        private final int size;
        private COWSubListIterator(List<E> l, int index, int offset, int size) {
            this.index = index;
            this.offset = offset;
            this.size = size;
            i = l.listIterator(index+offset);
        }

        public boolean hasNext() {
            return nextIndex() < size;
        }

        public E next() {
            if (hasNext())
                return i.next();
            else
                throw new NoSuchElementException();
        }

        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        public E previous() {
            if (hasPrevious())
                return i.previous();
            else
                throw new NoSuchElementException();
        }

        public int nextIndex() {
            return i.nextIndex() - offset;
        }

        public int previousIndex() {
            return i.previousIndex() - offset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E o) {
            throw new UnsupportedOperationException();
        }

        public void add(E o) {
            throw new UnsupportedOperationException();
        }
    }

}
