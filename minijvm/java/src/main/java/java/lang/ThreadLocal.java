/*
 * @(#)ThreadLocal.java	1.33 04/02/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;
import java.lang.ref.*;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * <tt>get</tt> or <tt>set</tt> method) has its own, independently initialized
 * copy of the variable.  <tt>ThreadLocal</tt> instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *
 * <p>For example, in the class below, the private static <tt>ThreadLocal</tt>
 * instance (<tt>serialNum</tt>) maintains a "serial number" for each thread
 * that invokes the class's static <tt>SerialNum.get()</tt> method, which
 * returns the current thread's serial number.  (A thread's serial number is
 * assigned the first time it invokes <tt>SerialNum.get()</tt>, and remains
 * unchanged on subsequent calls.)
 * <pre>
 * public class SerialNum {
 *     // The next serial number to be assigned
 *     private static int nextSerialNum = 0;
 *
 *     private static ThreadLocal serialNum = new ThreadLocal() {
 *         protected synchronized Object initialValue() {
 *             return new Integer(nextSerialNum++);
 *         }
 *     };
 *
 *     public static int get() {
 *         return ((Integer) (serialNum.get())).intValue();
 *     }
 * }
 * </pre>
 *
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the <tt>ThreadLocal</tt>
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * @author  Josh Bloch and Doug Lea
 * @version 1.33, 02/19/04
 * @since   1.2
 */
public class ThreadLocal<T> {
    /**
     * ThreadLocals rely on per-thread hash maps attached to each thread
     * (Thread.threadLocals and inheritableThreadLocals).  The ThreadLocal
     * objects act as keys, searched via threadLocalHashCode.  This is a
     * custom hash code (useful only within ThreadLocalMaps) that eliminates
     * collisions in the common case where consecutively constructed
     * ThreadLocals are used by the same threads, while remaining well-behaved
     * in less common cases.
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out. Accessed only by like-named method.
     */
    private static int nextHashCode = 0;

    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Compute the next hash code. The static synchronization used here
     * should not be a performance bottleneck. When ThreadLocals are
     * generated in different threads at a fast enough rate to regularly
     * contend on this lock, memory contention is by far a more serious
     * problem than lock contention.
     */
    private static synchronized int nextHashCode() {
        int h = nextHashCode;
        nextHashCode = h + HASH_INCREMENT;
        return h;
    }

    /**
     * Returns the current thread's initial value for this thread-local
     * variable.  This method will be invoked at most once per accessing
     * thread for each thread-local, the first time the thread accesses the
     * variable with the {@link #get} method.  The <tt>initialValue</tt>
     * method will not be invoked in a thread if the thread invokes the {@link
     * #set} method prior to the <tt>get</tt> method.
     *
     * <p>This implementation simply returns <tt>null</tt>; if the programmer
     * desires thread-local variables to be initialized to some value other
     * than <tt>null</tt>, <tt>ThreadLocal</tt> must be subclassed, and this
     * method overridden.  Typically, an anonymous inner class will be used.
     * Typical implementations of <tt>initialValue</tt> will invoke an
     * appropriate constructor and return the newly constructed object.
     *
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable.
     */
    public ThreadLocal() {
    }

    /**
     * Returns the value in the current thread's copy of this thread-local
     * variable.  Creates and initializes the copy if this is the first time
     * the thread has called this method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            return (T)map.get(this);

        // Maps are constructed lazily.  if the map for this thread
        // doesn't exist, create it, with this ThreadLocal and its
        // initial value as its only entry.
        T value = initialValue();
        createMap(t, value);
        return value;
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Many applications will have no need for
     * this functionality, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current threads' copy of
     *        this thread-local.
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    /**
     * Removes the value for this ThreadLocal.  This may help reduce
     * the storage requirements of ThreadLocals.  If this ThreadLocal
     * is accessed again, it will by default have its
     * <tt>initialValue</tt>.
     * @since 1.5
     **/

     public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     * @param map the map to store.
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         */
        private static class Entry extends WeakReference {
            /** The value associated with this ThreadLocal. */
            private Object value;

            private Entry(ThreadLocal k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two.
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
         */
        private Entry[] table;

        /**
         * The number of entries in the table.
         */
        private int size = 0;

        /**
         * The next size value at which to resize.
         */
        private int threshold; // Default to 0

        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * Increment i modulo len.
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         */
        ThreadLocalMap(ThreadLocal firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    ThreadLocal k = (ThreadLocal)e.get();
                    if (k != null) {
                        ThreadLocal key = k;
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the value associated with key with code h.  This method itself
         * handles only the fast path: a direct hit of existing key. It
         * otherwise relays to getAfterMiss.  This is designed to maximize
         * performance for direct hits, in part by making this method readily
         * inlinable.
         *
         * @param  key the thread local object
         * @param  h key's  hash code
         * @return the value associated with key
         */
        private Object get(ThreadLocal key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e.value;

            return getAfterMiss(key, i, e);
        }

        /**
         * Version of get method for use when key is not found in its
         * direct hash slot.
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i];
         * @return the value associated with key
         */
        private Object getAfterMiss(ThreadLocal key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal k = (ThreadLocal)e.get();
                if (k == key)
                    return e.value;
                if (k == null)
                    return replaceStaleEntry(key, null, i, true);

                i = nextIndex(i, len);
                e = tab[i];
            }

            Object value = key.initialValue();
            tab[i] = new Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();

            return value;
        }


        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
		 e != null;
		 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal k = (ThreadLocal)e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i, false);
                    return;
                }
            }

            tab[i] = new Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        private void remove(ThreadLocal key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
		 e != null;
		 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a get or set operation
         * with an entry for the specified key.  If actAsGet is true and an
         * entry for the key already exists, the value in the entry is
         * unchanged; if no entry exists for the key, the value in the new
         * entry is obtained by calling key.initialValue.  If actAsGet is
         * false, the value passed in the value parameter is stored in the
         * entry, whether or not an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key; meaningful only
         *         if actAsGet is false.
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         * @param  actAsGet true if this method should act as a get; false
         *         it should act as a set.
         * @return the value associated with key after the operation completes.
         */
        private Object replaceStaleEntry(ThreadLocal key, Object value,
                                    int staleSlot, boolean actAsGet) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
		 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            for (int i = nextIndex(staleSlot, len);
		 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal k = (ThreadLocal)e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                if (k == key) {
                    if (actAsGet)
                        value = e.value;
                    else
                        e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return value;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            if (actAsGet)
                value = key.initialValue();
            tab[staleSlot].value = null;   // Help the GC
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);

            return value;
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            tab[staleSlot].value = null;   // Help the GC
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter null
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
		 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal k = (ThreadLocal)e.get();
                if (k == null) {
                    e.value = null;   // Help the GC
                    tab[i] = null;
                    size--;
                } else {
                    ThreadLocal key = k;
                    int h = key.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: <tt>log2(n)</tt> cells are scanned,
         * unless a stale entry one is found, in which case
         * <tt>log2(table.length)-1</tt> additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        private void rehash() {
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * Double the capacity of the table.
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                oldTab[j] = null; // Help the GC
                if (e != null) {
                    ThreadLocal k = (ThreadLocal)e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        ThreadLocal key = k;
                        int h = key.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
