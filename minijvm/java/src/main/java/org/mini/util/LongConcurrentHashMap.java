/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.mini.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public final class LongConcurrentHashMap<V> implements LongMap<V> {
	/**
	 * The largest possible table capacity.  This value must be
	 * exactly 1<<30 to stay within Java array allocation and indexing
	 * bounds for power of two table sizes, and is further required
	 * because the top two bits of 32bit hash fields are used for
	 * control purposes.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The default initial table capacity.  Must be a power of 2
	 * (i.e., at least 1) and at most MAXIMUM_CAPACITY.
	 */
	private static final int DEFAULT_CAPACITY = 16;

	/**
	 * Minimum number of rebinnings per transfer step. Ranges are
	 * subdivided to allow multiple resizer threads.  This value
	 * serves as a lower bound to avoid resizers encountering
	 * excessive memory contention.  The value should be at least
	 * DEFAULT_CAPACITY.
	 */
	private static final int MIN_TRANSFER_STRIDE = 16;

	/**
	 * The number of bits used for generation stamp in sizeCtl.
	 * Must be at least 6 for 32bit arrays.
	 */
	private static final int RESIZE_STAMP_BITS = 16;

	/**
	 * The maximum number of threads that can help resize.
	 * Must fit in 32 - RESIZE_STAMP_BITS bits.
	 */
	private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

	/** The bit shift for recording size stamp in sizeCtl. */
	private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

	/** Number of CPUS, to place bounds on some sizings */
	private static final int NCPU = 4;

	/**
	 * The array of bins. Lazily initialized upon first insertion.
	 * Size is always a power of two. Accessed directly by iterators.
	 */
	private volatile Node<V>[] table;

	/** The next table to use; non-null only while resizing. */
	private volatile Node<V>[] nextTable;

	/**
	 * Base counter value, used mainly when there is no contention,
	 * but also as a fallback during table initialization
	 * races. Updated via CAS.
	 */
	@SuppressWarnings("unused")
	private volatile long baseCount;

	/**
	 * Table initialization and resizing control.  When negative, the
	 * table is being initialized or resized: -1 for initialization,
	 * else -(1 + the number of active resizing threads).  Otherwise,
	 * when table is null, holds the initial table size to use upon
	 * creation, or 0 for default. After initialization, holds the
	 * next element count value upon which to resize the table.
	 */
	private volatile int sizeCtl;

	/** The next table index (plus one) to split while resizing. */
	private volatile int transferIndex;

	/** Spinlock (locked via CAS) used when resizing and/or creating CounterCells. */
	private volatile int cellsBusy;

	/** Table of counter cells. When non-null, size is a power of 2. */
	private volatile CounterCell[] counterCells;

	/**
	 * Key-value entry.  This class is never exported out as a
	 * user-mutable Map.Entry (i.e., one supporting setValue; see
	 * MapEntry below), but can be used for read-only traversals used
	 * in bulk tasks.  Subclasses of Node with a negative hash field
	 * are special, and contain null keys and values (but are never
	 * exported).  Otherwise, keys and vals are never null.
	 */
	private static class Node<V> {
		private final long key;
		private volatile V val;
		private volatile Node<V> next;

		private Node(long key, V val, Node<V> next) {
			this.key = key;
			this.val = val;
			this.next = next;
		}
	}

	/** A node inserted at head of bins during transfer operations. */
	private static final class ForwardingNode<V> extends Node<V> {
		final Node<V>[] nextTable;

		ForwardingNode(Node<V>[] tab) {
			super(0, null, null);
			nextTable = tab;
		}

		Node<V> find(int h, long k) {
			// loop to avoid arbitrarily deep recursion on forwarding nodes
			for (Node<V>[] tab = nextTable; ; ) {
				Node<V> e;
				int n;
				if (tab == null || (n = tab.length) == 0 || (e = tabAt(tab, h & (n - 1))) == null)
					return null;
				for (; ; ) {
					if (e.val == null) { // MOVED
						tab = ((ForwardingNode<V>)e).nextTable;
						break;
					}
					if (e.key == k)
						return e;
					if ((e = e.next) == null)
						return null;
				}
			}
		}
	}

	/**
	 * Spreads (XORs) higher bits of hash to lower and also forces top
	 * bit to 0. Because the table uses power-of-two masking, sets of
	 * hashes that vary only in bits above the current mask will
	 * always collide. (Among known examples are sets of Float keys
	 * holding consecutive whole numbers in small tables.)  So we
	 * apply a transform that spreads the impact of higher bits
	 * downward. There is a tradeoff between speed, utility, and
	 * quality of bit-spreading. Because many common sets of hashes
	 * are already reasonably distributed (so don't benefit from
	 * spreading), and because we use trees to handle large sets of
	 * collisions in bins, we just XOR some shifted bits in the
	 * cheapest possible way to reduce systematic lossage, as well as
	 * to incorporate impact of the highest bits that would otherwise
	 * never be used in index calculations because of table bounds.
	 */
	private static int spread(long key) {
		//noinspection UnnecessaryLocalVariable
		int h = (int)key; // for faster inner using (key is multiple of prime number)
		return h; // (h ^ (h >>> 16)) & HASH_BITS;
	}

	/**
	 * Returns a power of two table size for the given desired capacity.
	 * See Hackers Delight, sec 3.2
	 */
	private static int tableSizeFor(int n) {
		n--;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return n < 0 ? 1 : (n >= MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : n + 1);
	}

	/*
	 * Volatile access methods are used for table elements as well as
	 * elements of in-progress next table while resizing.  All uses of
	 * the tab arguments must be null checked by callers.  All callers
	 * also paranoically precheck that tab's length is not zero (or an
	 * equivalent check), thus ensuring that any index argument taking
	 * the form of a hash value anded with (length - 1) is a valid
	 * index.  Note that, to be correct wrt arbitrary concurrency
	 * errors by users, these checks must operate on local variables,
	 * which accounts for some odd-looking inline assignments below.
	 * Note that calls to setTabAt always occur within locked regions,
	 * and so in principle require only release ordering, not
	 * full volatile semantics, but are currently coded as volatile
	 * writes to be conservative.
	 */

	@SuppressWarnings("unchecked")
	private static <V> Node<V> tabAt(Node<V>[] tab, int i) {
		return (Node<V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
	}

	private static <V> boolean casTabAt(Node<V>[] tab, int i, Node<V> v) {
		return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, null, v);
	}

	private static <V> void setTabAt(Node<V>[] tab, int i, Node<V> v) {
		U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
	}

	/**
	 * Returns the stamp bits for resizing a table of size n.
	 * Must be negative when shifted left by RESIZE_STAMP_SHIFT.
	 */
	private static int resizeStamp(int n) {
		return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
	}

	/** Creates a new, empty map with the default initial table size (16). */
	public LongConcurrentHashMap() {
	}

	/**
	 * Creates a new, empty map with an initial table size
	 * accommodating the specified number of elements without the need
	 * to dynamically resize.
	 *
	 * @param initialCapacity The implementation performs internal
	 *                        sizing to accommodate this many elements.
	 * @throws IllegalArgumentException if the initial capacity of
	 *                                  elements is negative
	 */
	public LongConcurrentHashMap(int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException();
		sizeCtl = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
	}

	/**
	 * Creates a new, empty map with an initial table size based on
	 * the given number of elements ({@code initialCapacity}) and
	 * initial table density ({@code loadFactor}).
	 *
	 * @param initialCapacity the initial capacity. The implementation
	 *                        performs internal sizing to accommodate this many elements,
	 *                        given the specified load factor.
	 * @param loadFactor      the load factor (table density) for
	 *                        establishing the initial table size
	 * @throws IllegalArgumentException if the initial capacity of
	 *                                  elements is negative or the load factor is nonpositive
	 */
	public LongConcurrentHashMap(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, 1);
	}

	/**
	 * Creates a new, empty map with an initial table size based on
	 * the given number of elements ({@code initialCapacity}), table
	 * density ({@code loadFactor}), and number of concurrently
	 * updating threads ({@code concurrencyLevel}).
	 *
	 * @param initialCapacity  the initial capacity. The implementation
	 *                         performs internal sizing to accommodate this many elements,
	 *                         given the specified load factor.
	 * @param loadFactor       the load factor (table density) for
	 *                         establishing the initial table size
	 * @param concurrencyLevel the estimated number of concurrently
	 *                         updating threads. The implementation may use this value as
	 *                         a sizing hint.
	 * @throws IllegalArgumentException if the initial capacity is
	 *                                  negative or the load factor or concurrencyLevel are
	 *                                  nonpositive
	 */
	public LongConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
		if (loadFactor <= 0.0f || initialCapacity < 0 || concurrencyLevel <= 0)
			throw new IllegalArgumentException();
		if (initialCapacity < concurrencyLevel) // Use at least as many bins
			initialCapacity = concurrencyLevel; // as estimated threads
		long size = (long)(1.0 + initialCapacity / loadFactor);
		sizeCtl = (size >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int)size);
	}

	@Override
	public int size() {
		long n = sumCount();
		return ((n < 0L) ? 0 : (n > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)n);
	}

	@Override
	public boolean isEmpty() {
		return sumCount() <= 0L; // ignore transient negative values
	}

	/**
	 * Returns the value to which the specified key is mapped,
	 * or {@code null} if this map contains no mapping for the key.
	 *
	 * <p>More formally, if this map contains a mapping from a key
	 * {@code k} to a value {@code v} such that {@code key.equals(k)},
	 * then this method returns {@code v}; otherwise it returns
	 * {@code null}.  (There can be at most one such mapping.)
	 *
	 * @throws NullPointerException if the specified key is null
	 */
	@Override
	public V get(long key) {
		Node<V>[] tab;
		Node<V> e, p;
		int n;
		int h = spread(key);
		if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, h & (n - 1))) != null) {
			if (e.val != null) {
				do {
					if (e.key == key)
						return e.val;
				}
				while ((e = e.next) != null);
			} else // MOVED
				return (p = ((ForwardingNode<V>)e).find(h, key)) != null ? p.val : null;
		}
		return null;
	}

	/**
	 * Tests if the specified object is a key in this table.
	 *
	 * @param key possible key
	 * @return {@code true} if and only if the specified object
	 * 		is a key in this table, as determined by the
	 *        {@code equals} method; {@code false} otherwise
	 * @throws NullPointerException if the specified key is null
	 */
	public boolean containsKey(long key) {
		return get(key) != null;
	}

	/**
	 * Returns {@code true} if this map maps one or more keys to the
	 * specified value. Note: This method may require a full traversal
	 * of the map, and is much slower than method {@code containsKey}.
	 *
	 * @param value value whose presence in this map is to be tested
	 * @return {@code true} if this map maps one or more keys to the
	 * 		specified value
	 * @throws NullPointerException if the specified value is null
	 */
	public boolean containsValue(V value) {
		if (value == null)
			throw new NullPointerException();
		Node<V>[] t;
		if ((t = table) != null) {
			Traverser<V> it = new Traverser<>(t, t.length);
			for (Node<V> p; (p = it.advance()) != null; ) {
				V v = p.val;
				//noinspection PointlessNullCheck
				if (v == value || (v != null && value.equals(v)))
					return true;
			}
		}
		return false;
	}

	/**
	 * Maps the specified key to the specified value in this table.
	 * Neither the key nor the value can be null.
	 *
	 * <p>The value can be retrieved by calling the {@code get} method
	 * with a key that is equal to the original key.
	 *
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with {@code key}, or
	 *        {@code null} if there was no mapping for {@code key}
	 * @throws NullPointerException if the specified key or value is null
	 */
	@Override
	public V put(long key, V value) {
		return putVal(key, value, false);
	}

	/** Implementation for put and putIfAbsent */
	private V putVal(long key, V value, boolean onlyIfAbsent) {
		if (value == null)
			throw new NullPointerException();
		int hash = spread(key);
		int binCount = 0;
		for (Node<V>[] tab = table; ; ) {
			Node<V> f;
			int n, i;
			if (tab == null || (n = tab.length) == 0)
				tab = initTable();
			else if ((f = tabAt(tab, i = hash & (n - 1))) == null) {
				if (casTabAt(tab, i, new Node<>(key, value, null)))
					break; // no lock when adding to empty bin
			} else if (f.val == null) // MOVED
				tab = helpTransfer(tab, f);
			else {
				V oldVal = null;
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						binCount = 1;
						for (Node<V> e = f; ; ++binCount) {
							if (e.key == key) {
								oldVal = e.val;
								if (!onlyIfAbsent)
									e.val = value;
								break;
							}
							Node<V> pred = e;
							if ((e = e.next) == null) {
								pred.next = new Node<>(key, value, null);
								break;
							}
						}
					}
				}
				if (binCount != 0) {
					if (oldVal != null)
						return oldVal;
					break;
				}
			}
		}
		addCount(1L, binCount);
		return null;
	}

	/**
	 * Removes the key (and its corresponding value) from this map.
	 * This method does nothing if the key is not in the map.
	 *
	 * @param key the key that needs to be removed
	 * @return the previous value associated with {@code key}, or
	 *        {@code null} if there was no mapping for {@code key}
	 * @throws NullPointerException if the specified key is null
	 */
	@Override
	public V remove(long key) {
		return replaceNode(key, null, null);
	}

	/**
	 * Implementation for the four public remove/replace methods:
	 * Replaces node value with v, conditional upon match of cv if
	 * non-null.  If resulting value is null, delete.
	 */
	private V replaceNode(long key, V value, Object cv) {
		int hash = spread(key);
		for (Node<V>[] tab = table; ; ) {
			Node<V> f;
			int n, i;
			if (tab == null || (n = tab.length) == 0 || (f = tabAt(tab, i = hash & (n - 1))) == null)
				break;
			else if (f.val == null) // MOVED
				tab = helpTransfer(tab, f);
			else {
				V oldVal = null;
				boolean validated = false;
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						validated = true;
						for (Node<V> e = f, pred = null; ; ) {
							if (e.key == key) {
								V ev = e.val;
								//noinspection PointlessNullCheck
								if (cv == null || cv == ev || (ev != null && cv.equals(ev))) {
									oldVal = ev;
									if (value != null)
										e.val = value;
									else if (pred != null)
										pred.next = e.next;
									else
										setTabAt(tab, i, e.next);
								}
								break;
							}
							pred = e;
							if ((e = e.next) == null)
								break;
						}
					}
				}
				if (validated) {
					if (oldVal != null) {
						if (value == null)
							addCount(-1L, -1);
						return oldVal;
					}
					break;
				}
			}
		}
		return null;
	}

	/** Removes all of the mappings from this map. */
	@Override
	public void clear() {
		long delta = 0L; // negative number of deletions
		int i = 0;
		Node<V>[] tab = table;
		while (tab != null && i < tab.length) {
			Node<V> f = tabAt(tab, i);
			if (f == null)
				++i;
			else if (f.val == null) { // MOVED
				tab = helpTransfer(tab, f);
				i = 0; // restart
			} else {
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						do {
							--delta;
							f = f.next;
						}
						while (f != null);
						setTabAt(tab, i++, null);
					}
				}
			}
		}
		if (delta != 0L)
			addCount(delta, -1);
	}

	/**
	 * Returns a string representation of this map.  The string
	 * representation consists of a list of key-value mappings (in no
	 * particular order) enclosed in braces ("{@code {}}").  Adjacent
	 * mappings are separated by the characters {@code ", "} (comma
	 * and space).  Each key-value mapping is rendered as the key
	 * followed by an equals sign ("{@code =}") followed by the
	 * associated value.
	 *
	 * @return a string representation of this map
	 */
	@Override
	public String toString() {
		Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		Traverser<V> it = new Traverser<>(t, f);
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		Node<V> p;
		if ((p = it.advance()) != null) {
			for (; ; ) {
				long k = p.key;
				V v = p.val;
				sb.append(k);
				sb.append('=');
				sb.append(v == this ? "(this Map)" : v);
				if ((p = it.advance()) == null)
					break;
				sb.append(',').append(' ');
			}
		}
		return sb.append('}').toString();
	}

	// ConcurrentMap methods

	/**
	 * @return the previous value associated with the specified key,
	 * 		or {@code null} if there was no mapping for the key
	 * @throws NullPointerException if the specified key or value is null
	 */
	public V putIfAbsent(long key, V value) {
		return putVal(key, value, true);
	}

	/** @throws NullPointerException if the specified key is null */
	@Override
	public boolean remove(long key, Object value) {
		return value != null && replaceNode(key, null, value) != null;
	}

	/** @throws NullPointerException if any of the arguments are null */
	public boolean replace(long key, V oldValue, V newValue) {
		if (oldValue == null || newValue == null)
			throw new NullPointerException();
		return replaceNode(key, newValue, oldValue) != null;
	}

	/**
	 * @return the previous value associated with the specified key,
	 * 		or {@code null} if there was no mapping for the key
	 * @throws NullPointerException if the specified key or value is null
	 */
	public V replace(long key, V value) {
		if (value == null)
			throw new NullPointerException();
		return replaceNode(key, value, null);
	}

	/**
	 * Returns the value to which the specified key is mapped, or the
	 * given default value if this map contains no mapping for the
	 * key.
	 *
	 * @param key          the key whose associated value is to be returned
	 * @param defaultValue the value to return if this map contains
	 *                     no mapping for the given key
	 * @return the mapping for the key, if present; else the default value
	 * @throws NullPointerException if the specified key is null
	 */
	public V getOrDefault(long key, V defaultValue) {
		V v;
		return (v = get(key)) == null ? defaultValue : v;
	}

	/**
	 * Returns the number of mappings. This method should be used
	 * instead of {@link #size} because a LongConcurrentHashMap may
	 * contain more mappings than can be represented as an int. The
	 * value returned is an estimate; the actual count may differ if
	 * there are concurrent insertions or removals.
	 *
	 * @return the number of mappings
	 */
	public long mappingCount() {
		long n = sumCount();
		return (n < 0L) ? 0L : n; // ignore transient negative values
	}

	/** Initializes table, using the size recorded in sizeCtl. */
	private Node<V>[] initTable() {
		Node<V>[] tab;
		int sc;
		while ((tab = table) == null || tab.length == 0) {
			if ((sc = sizeCtl) < 0)
				Thread.yield(); // lost initialization race; just spin
			else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
				try {
					if ((tab = table) == null || tab.length == 0) {
						int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
						@SuppressWarnings("unchecked")
						Node<V>[] nt = (Node<V>[])new Node<?>[n];
						table = tab = nt;
						sc = n - (n >>> 2);
					}
				} finally {
					sizeCtl = sc;
				}
				break;
			}
		}
		return tab;
	}

	private static final class ThreadLocalRandom {
		private static final class Probe {
			int probe;
		}

		private static final AtomicInteger probeGenerator = new AtomicInteger();
		private static final ThreadLocal<Probe> tlProb = new ThreadLocal<>();

		static void localInit() {
			int p = probeGenerator.addAndGet(0x9e3779b9);
			Probe probe = new Probe();
			probe.probe = (p == 0 ? 1 : p); // skip 0
			tlProb.set(probe);
		}

		static int getProbe() {
			Probe probe = tlProb.get();
			return probe != null ? tlProb.get().probe : 0;
		}

		static int advanceProbe(int probe) {
			probe ^= probe << 13; // xorshift
			probe ^= probe >>> 17;
			probe ^= probe << 5;
			tlProb.get().probe = probe;
			return probe;
		}
	}

	/**
	 * Adds to count, and if table is too small and not already
	 * resizing, initiates transfer. If already resizing, helps
	 * perform transfer if work is available.  Rechecks occupancy
	 * after a transfer to see if another resize is already needed
	 * because resizings are lagging additions.
	 *
	 * @param x     the count to add
	 * @param check if <0, don't check resize, if <= 1 only check if uncontended
	 */
	private void addCount(long x, int check) {
		CounterCell[] as;
		long b, s;
		if ((as = counterCells) != null || !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
			CounterCell a;
			long v;
			int m;
			boolean uncontended = true;
			if (as == null || (m = as.length - 1) < 0 || (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
					!(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
				fullAddCount(x, uncontended);
				return;
			}
			if (check <= 1)
				return;
			s = sumCount();
		}
		if (check >= 0) {
			Node<V>[] tab, nt;
			int n, sc;
			while (s >= (sc = sizeCtl) && (tab = table) != null && (n = tab.length) < MAXIMUM_CAPACITY) {
				int rs = resizeStamp(n);
				if (sc < 0) {
					//noinspection ConstantConditions
					if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || (nt = nextTable) == null || transferIndex <= 0)
						break;
					if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
						transfer(tab, nt);
				} else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2))
					transfer(tab, null);
				s = sumCount();
			}
		}
	}

	/** Helps transfer if a resize is in progress. */
	private Node<V>[] helpTransfer(Node<V>[] tab, Node<V> f) {
		Node<V>[] nextTab;
		int sc;
		if (tab != null && f.val == null && (nextTab = ((ForwardingNode<V>)f).nextTable) != null) { // MOVED
			int rs = resizeStamp(tab.length);
			while (nextTab == nextTable && table == tab && (sc = sizeCtl) < 0) {
				//noinspection ConstantConditions
				if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || transferIndex <= 0)
					break;
				if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
					transfer(tab, nextTab);
					break;
				}
			}
			return nextTab;
		}
		return table;
	}

	/**
	 * Moves and/or copies the nodes in each bin to new table. See
	 * above for explanation.
	 */
	private void transfer(Node<V>[] tab, Node<V>[] nextTab) {
		int n = tab.length, stride;
		if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
			stride = MIN_TRANSFER_STRIDE; // subdivide range
		if (nextTab == null) { // initiating
			try {
				@SuppressWarnings("unchecked")
				Node<V>[] nt = (Node<V>[])new Node<?>[n << 1];
				nextTab = nt;
			} catch (Throwable ex) { // try to cope with OOME
				sizeCtl = Integer.MAX_VALUE;
				return;
			}
			nextTable = nextTab;
			transferIndex = n;
		}
		int nextn = nextTab.length;
		ForwardingNode<V> fwd = new ForwardingNode<>(nextTab);
		boolean advance = true;
		boolean finishing = false; // to ensure sweep before committing nextTab
		for (int i = 0, bound = 0; ; ) {
			Node<V> f;
			while (advance) {
				int nextIndex, nextBound;
				if (--i >= bound || finishing)
					advance = false;
				else if ((nextIndex = transferIndex) <= 0) {
					i = -1;
					advance = false;
				} else if (U.compareAndSwapInt(this, TRANSFERINDEX, nextIndex, nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {
					bound = nextBound;
					i = nextIndex - 1;
					advance = false;
				}
			}
			if (i < 0 || i >= n || i + n >= nextn) {
				int sc;
				if (finishing) {
					nextTable = null;
					table = nextTab;
					sizeCtl = (n << 1) - (n >>> 1);
					return;
				}
				if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
					if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
						return;
					finishing = advance = true;
					i = n; // recheck before commit
				}
			} else if ((f = tabAt(tab, i)) == null)
				advance = casTabAt(tab, i, fwd);
			else if (f.val == null) // MOVED
				advance = true; // already processed
			else {
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						Node<V> ln, hn;
						int runBit = spread(f.key) & n;
						Node<V> lastRun = f;
						for (Node<V> p = f.next; p != null; p = p.next) {
							int b = spread(p.key) & n;
							if (b != runBit) {
								runBit = b;
								lastRun = p;
							}
						}
						if (runBit == 0) {
							ln = lastRun;
							hn = null;
						} else {
							hn = lastRun;
							ln = null;
						}
						for (Node<V> p = f; p != lastRun; p = p.next) {
							long pk = p.key;
							V pv = p.val;
							if ((spread(p.key) & n) == 0)
								ln = new Node<>(pk, pv, ln);
							else
								hn = new Node<>(pk, pv, hn);
						}
						setTabAt(nextTab, i, ln);
						setTabAt(nextTab, i + n, hn);
						setTabAt(tab, i, fwd);
						advance = true;
					}
				}
			}
		}
	}

	/**
	 * A padded cell for distributing counts.  Adapted from LongAdder
	 * and Striped64.  See their internal docs for explanation.
	 */
	// @sun.misc.Contended
	private static final class CounterCell {
		@SuppressWarnings("unused")
		private volatile long p0, p1, p2, p3, p4, p5, p6;
		@SuppressWarnings("FieldMayBeFinal")
		private volatile long value;
		@SuppressWarnings("unused")
		private volatile long q0, q1, q2, q3, q4, q5, q6;

		private CounterCell(long x) {
			value = x;
		}
	}

	private long sumCount() {
		CounterCell[] as = counterCells;
		long sum = baseCount;
		if (as != null) {
			for (CounterCell a : as)
				if (a != null)
					sum += a.value;
		}
		return sum;
	}

	// See LongAdder version for explanation
	private void fullAddCount(long x, boolean wasUncontended) {
		int h;
		if ((h = ThreadLocalRandom.getProbe()) == 0) {
			ThreadLocalRandom.localInit(); // force initialization
			h = ThreadLocalRandom.getProbe();
			wasUncontended = true;
		}
		boolean collide = false; // True if last slot nonempty
		for (; ; ) {
			CounterCell[] as;
			CounterCell a;
			int n;
			long v;
			if ((as = counterCells) != null && (n = as.length) > 0) {
				if ((a = as[h & (n - 1)]) == null) {
					if (cellsBusy == 0) { // Try to attach new Cell
						CounterCell r = new CounterCell(x); // Optimistic create
						if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
							boolean created = false;
							try { // Recheck under lock
								CounterCell[] rs;
								int m, j;
								if ((rs = counterCells) != null && (m = rs.length) > 0 && rs[j = h & (m - 1)] == null) {
									rs[j] = r;
									created = true;
								}
							} finally {
								cellsBusy = 0;
							}
							if (created)
								break;
							continue; // Slot is now non-empty
						}
					}
					collide = false;
				} else if (!wasUncontended) // CAS already known to fail
					wasUncontended = true; // Continue after rehash
				else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
					break;
				else if (counterCells != as || n >= NCPU)
					collide = false; // At max size or stale
				else if (!collide)
					collide = true;
				else if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
					try {
						if (counterCells == as) // Expand table unless stale
							counterCells = Arrays.copyOf(as, n << 1, CounterCell[].class);
					} finally {
						cellsBusy = 0;
					}
					collide = false;
					continue; // Retry with expanded table
				}
				h = ThreadLocalRandom.advanceProbe(h);
			} else if (cellsBusy == 0 && counterCells == as && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
				boolean init = false;
				try { // Initialize table
					if (counterCells == as) {
						CounterCell[] rs = new CounterCell[2];
						rs[h & 1] = new CounterCell(x);
						counterCells = rs;
						init = true;
					}
				} finally {
					cellsBusy = 0;
				}
				if (init)
					break;
			} else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
				break; // Fall back on using base
		}
	}

	@Override
	public LongIterator keyIterator() {
		Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		return new KeyIterator<>(t, f);
	}

	@Override
	public Iterator<V> iterator() {
		Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		return new ValueIterator<>(t, f);
	}

	@Override
	public MapIterator<V> entryIterator() {
		Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		return new EntryIterator<>(t, f);
	}

	/**
	 * Records the table, its length, and current traversal index for a
	 * traverser that must process a region of a forwarded table before
	 * proceeding with current table.
	 */
	private static final class TableStack<V> {
		private int length;
		private int index;
		private Node<V>[] tab;
		private TableStack<V> next;
	}

	/**
	 * Encapsulates traversal for methods such as containsValue; also
	 * serves as a base class for other iterators and spliterators.
	 * <p>
	 * Method advance visits once each still-valid node that was
	 * reachable upon iterator construction. It might miss some that
	 * were added to a bin after the bin was visited, which is OK wrt
	 * consistency guarantees. Maintaining this property in the face
	 * of possible ongoing resizes requires a fair amount of
	 * bookkeeping state that is difficult to optimize away amidst
	 * volatile accesses.  Even so, traversal maintains reasonable
	 * throughput.
	 * <p>
	 * Normally, iteration proceeds bin-by-bin traversing lists.
	 * However, if the table has been resized, then all future steps
	 * must traverse both the bin at the current index as well as at
	 * (index + baseSize); and so on for further resizings. To
	 * paranoically cope with potential sharing by users of iterators
	 * across threads, iteration terminates if a bounds checks fails
	 * for a table read.
	 */
	private static class Traverser<V> {
		private Node<V>[] tab; // current table; updated if resized
		protected Node<V> next; // the next entry to use
		private TableStack<V> stack, spare; // to save/restore on ForwardingNodes
		private int index; // index of bin to use next
		private int baseIndex; // current index of initial table
		private final int baseSize; // initial table size

		private Traverser(Node<V>[] tab, int size) {
			this.tab = tab;
			baseSize = size;
		}

		/** Advances if possible, returning next valid node, or null if none. */
		protected final Node<V> advance() {
			Node<V> e = next;
			if (e != null)
				e = e.next;
			for (; ; ) {
				Node<V>[] t;
				int i, n; // must use locals in checks
				if (e != null)
					return next = e;
				if (baseIndex >= baseSize || (t = tab) == null || (n = t.length) <= (i = index) || i < 0)
					return next = null;
				if ((e = tabAt(t, i)) != null && e.val == null) { // MOVED
					tab = ((ForwardingNode<V>)e).nextTable;
					e = null;
					pushState(t, i, n);
					continue;
				}
				if (stack != null)
					recoverState(n);
				else if ((index = i + baseSize) >= n)
					index = ++baseIndex; // visit upper slots if present
			}
		}

		/** Saves traversal state upon encountering a forwarding node. */
		private void pushState(Node<V>[] t, int i, int n) {
			TableStack<V> s = spare; // reuse if possible
			if (s != null)
				spare = s.next;
			else
				s = new TableStack<>();
			s.tab = t;
			s.length = n;
			s.index = i;
			s.next = stack;
			stack = s;
		}

		/**
		 * Possibly pops traversal state.
		 *
		 * @param n length of current table
		 */
		private void recoverState(int n) {
			TableStack<V> s;
			int len;
			while ((s = stack) != null && (index += (len = s.length)) >= n) {
				n = len;
				index = s.index;
				tab = s.tab;
				s.tab = null;
				TableStack<V> next2 = s.next;
				s.next = spare; // save for reuse
				stack = next2;
				spare = s;
			}
			if (s == null && (index += baseSize) >= n)
				index = ++baseIndex;
		}
	}

	/**
	 * Base of key, value, and entry Iterators. Adds fields to
	 * Traverser to support iterator.remove.
	 */
	private abstract static class BaseIterator<V> extends Traverser<V> {
		private BaseIterator(Node<V>[] tab, int size) {
			super(tab, size);
			advance();
		}

		public final boolean hasNext() {
			return next != null;
		}

		@SuppressWarnings("static-method")
		@Deprecated
		public final void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class KeyIterator<V> extends BaseIterator<V> implements LongIterator {
		private KeyIterator(Node<V>[] tab, int size) {
			super(tab, size);
		}

		@Override
		public long next() {
			Node<V> p;
			if ((p = next) == null)
				throw new NoSuchElementException();
			long k = p.key;
			advance();
			return k;
		}
	}

	private static final class ValueIterator<V> extends BaseIterator<V> implements Iterator<V> {
		private ValueIterator(Node<V>[] tab, int size) {
			super(tab, size);
		}

		@Override
		public V next() {
			Node<V> p;
			if ((p = next) == null)
				throw new NoSuchElementException();
			V v = p.val;
			advance();
			return v;
		}
	}

	private static final class EntryIterator<V> extends BaseIterator<V> implements MapIterator<V> {
		private Node<V> node;

		EntryIterator(Node<V>[] tab, int size) {
			super(tab, size);
		}

		@Override
		public boolean moveToNext() {
			if ((node = next) == null)
				return false;
			advance();
			return true;
		}

		@Override
		public long key() {
			return node.key;
		}

		@Override
		public V value() {
			return node.val;
		}
	}

	// Unsafe mechanics
	private static final Unsafe U;
	private static final long SIZECTL;
	private static final long TRANSFERINDEX;
	private static final long BASECOUNT;
	private static final long CELLSBUSY;
	private static final long CELLVALUE;
	private static final long ABASE;
	private static final int ASHIFT;

	/**
	 * Returns a sun.misc.Unsafe.  Suitable for use in a 3rd party package.
	 * Replace with a simple call to Unsafe.getUnsafe when integrating into a jdk.
	 *
	 * @return a sun.misc.Unsafe
	 */
	private static Unsafe getUnsafe() {
		try {
			return Unsafe.getUnsafe();
		} catch (SecurityException ignored) {
		}
		try {
			return AccessController.doPrivileged((PrivilegedExceptionAction<Unsafe>)() -> {
				Class<Unsafe> k = Unsafe.class;
				for (Field f : k.getDeclaredFields()) {
					f.setAccessible(true);
					Object x = f.get(null);
					if (k.isInstance(x))
						return k.cast(x);
				}
				throw new NoSuchFieldException("the Unsafe");
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("Could not initialize intrinsics", e.getCause());
		}
	}

	static {
		try {
			U = getUnsafe();
			Class<?> k = LongConcurrentHashMap.class;
			SIZECTL = U.objectFieldOffset(k.getDeclaredField("sizeCtl"));
			TRANSFERINDEX = U.objectFieldOffset(k.getDeclaredField("transferIndex"));
			BASECOUNT = U.objectFieldOffset(k.getDeclaredField("baseCount"));
			CELLSBUSY = U.objectFieldOffset(k.getDeclaredField("cellsBusy"));
			Class<?> ck = CounterCell.class;
			CELLVALUE = U.objectFieldOffset(ck.getDeclaredField("value"));
			Class<?> ak = Node[].class;
			ABASE = U.arrayBaseOffset(ak);
			int scale = U.arrayIndexScale(ak);
			if ((scale & (scale - 1)) != 0)
				throw new Error("data type scale not a power of two");
			ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
		} catch (Exception e) {
			throw new Error(e);
		}
	}
}
