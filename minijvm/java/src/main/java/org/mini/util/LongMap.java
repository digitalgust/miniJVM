package org.mini.util;

/** Same as 'java.util.Map' but uses primitive 'long' keys to minimise boxing (and GC) overhead. */
public interface LongMap<V> extends Iterable<V> {
	/** Returns the number of elements in this map. */
	int size();

	/**
	 * Returns whether this map is empty.
	 *
	 * @return {@code true} if this map has no elements, {@code false} otherwise.
	 */
	boolean isEmpty();

	/**
	 * Returns the value of the mapping with the specified key.
	 *
	 * @param key the key.
	 * @return the value of the mapping with the specified key, or {@code null}
	 * 		if no mapping for the specified key is found.
	 */
	V get(long key);

	/**
	 * Maps the specified key to the specified value.
	 *
	 * @param key   the key.
	 * @param value the value.
	 * @return the value of any previous mapping with the specified key or
	 *        {@code null} if there was no such mapping.
	 */
	V put(long key, V value);

	/**
	 * Removes the mapping from this map
	 *
	 * @param key to remove
	 * @return value contained under this key, or null if value did not exist
	 */
	V remove(long key);

	boolean remove(long key, V value);

	/** Removes all mappings from this hash map, leaving it empty. */
	void clear();

	LongIterator keyIterator();

	MapIterator<V> entryIterator();

	interface LongIterator {
		boolean hasNext();

		long next();

		void remove();
	}

	interface MapIterator<V> {
		boolean moveToNext();

		long key();

		V value();

		void remove();
	}
}
