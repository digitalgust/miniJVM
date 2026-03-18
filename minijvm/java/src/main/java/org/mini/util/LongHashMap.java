package org.mini.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class LongHashMap<V> implements Cloneable {
	private int size;
	private long[] keyTable;
	private V[] valueTable;
	private V zeroValue;
	private boolean hasZeroValue;
	private final float loadFactor;
	private int threshold;
	private int mask;
	private int shift;

	public LongHashMap() {
		this(2, 0.8f);
	}

	public LongHashMap(int cap) {
		this(cap, 0.8f);
	}

	public LongHashMap(int cap, float loadFactor) {
		if (loadFactor <= 0 || loadFactor >= 1)
			throw new IllegalArgumentException("invalid loadFactor: " + loadFactor);
		this.loadFactor = loadFactor;
		final int tableSize = tableSize(Math.max(cap, 0));
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		keyTable = new long[tableSize];
		//noinspection unchecked
		valueTable = (V[])new Object[tableSize];
	}

	public LongHashMap(LongHashMap<? extends V> map) {
		size = map.size;
		keyTable = map.keyTable.clone();
		valueTable = map.valueTable.clone();
		zeroValue = map.zeroValue;
		hasZeroValue = map.hasZeroValue;
		loadFactor = map.loadFactor;
		threshold = map.threshold;
		mask = map.mask;
		shift = map.shift;
	}

	private int tableSize(int cap) {
		cap = Math.min(Math.max((int)Math.ceil(cap / loadFactor), 2), 1 << 30);
		return 1 << (32 - Integer.numberOfLeadingZeros(cap - 1)); // [0,1<<30] => [0,1,2,4,8,...,1<<30]
	}

	private int hash(long key) {
		return (int)((key * 0x9E3779B97F4A7C15L) >>> shift);
	}

	public long[] getKeyTable() {
		return keyTable;
	}

	public V[] getValueTable() {
		return valueTable;
	}

	public boolean hasZeroValue() {
		return hasZeroValue;
	}

	public V getZeroValue() {
		return zeroValue;
	}

	public float getLoadFactor() {
		return loadFactor;
	}

	public int capacity() {
		return mask + 1;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean containsKey(long key) {
		if (key == 0)
			return hasZeroValue;
		final long[] kt = keyTable;
		final int m = mask;
		for (int i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == key)
				return true;
			if (k == 0)
				return false;
		}
	}

	public boolean containsValue(V value) {
		if (value == null) {
			if (hasZeroValue && zeroValue == null)
				return true;
			final long[] kt = keyTable;
			final V[] vt = valueTable;
			for (int i = 0, n = kt.length; i < n; i++)
				if (kt[i] != 0 && vt[i] == null)
					return true;
		} else {
			if (hasZeroValue && value.equals(zeroValue))
				return true;
			final long[] kt = keyTable;
			final V[] vt = valueTable;
			for (int i = 0, n = kt.length; i < n; i++)
				if (kt[i] != 0 && value.equals(vt[i]))
					return true;
		}
		return false;
	}

	public V get(long key) {
		if (key == 0)
			return hasZeroValue ? zeroValue : null;
		final long[] kt = keyTable;
		final int m = mask;
		for (int i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == key)
				return valueTable[i];
			if (k == 0)
				return null;
		}
	}

	public V getOrDefault(long key, V defaultValue) {
		if (key == 0)
			return hasZeroValue ? zeroValue : defaultValue;
		final long[] kt = keyTable;
		final int m = mask;
		for (int i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == key)
				return valueTable[i];
			if (k == 0)
				return defaultValue;
		}
	}

	public V put(long key, V value) {
		if (key == 0) {
			final V oldV = zeroValue;
			zeroValue = value;
			if (!hasZeroValue) {
				hasZeroValue = true;
				size++;
			}
			return oldV;
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		final int m = mask;
		for (int i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == 0) {
				kt[i] = key;
				vt[i] = value;
				if (++size >= threshold)
					resize(kt.length << 1);
				return null;
			}
			if (k == key) {
				final V oldV = vt[i];
				vt[i] = value;
				return oldV;
			}
		}
	}

	public void putAll(LongHashMap<? extends V> map) {
		if (map.hasZeroValue) {
			hasZeroValue = true;
			zeroValue = map.zeroValue;
		}
		final long[] mapKt = map.keyTable;
		final V[] mapVt = map.valueTable;
		for (int i = 0, n = mapKt.length; i < n; i++) {
			final long k = mapKt[i];
			if (k != 0)
				put(k, mapVt[i]);
		}
	}

	public V putIfAbsent(long key, V value) {
		if (key == 0) {
			final V oldV = zeroValue;
			if (!hasZeroValue) {
				hasZeroValue = true;
				zeroValue = value;
				size++;
			}
			return oldV;
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		final int m = mask;
		for (int i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == 0) {
				kt[i] = key;
				vt[i] = value;
				if (++size >= threshold)
					resize(kt.length << 1);
				return null;
			}
			if (k == key)
				return vt[i];
		}
	}

	public V replace(long key, V value) {
		if (key == 0) {
			final V oldV = zeroValue;
			if (hasZeroValue)
				zeroValue = value;
			return oldV;
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		final int m = mask;
		for (int i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == 0)
				return null;
			if (k == key) {
				final V oldV = vt[i];
				vt[i] = value;
				return oldV;
			}
		}
	}

	public boolean replace(long key, V oldValue, V newValue) {
		if (key == 0) {
			if (!hasZeroValue || !Objects.equals(oldValue, zeroValue))
				return false;
			zeroValue = newValue;
			return true;
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		for (int i = hash(key), m = mask; ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == key) {
				if (!Objects.equals(oldValue, vt[i]))
					return false;
				vt[i] = newValue;
				return true;
			}
			if (k == 0)
				return false;
		}
	}

	public interface LongObjectFunction<V> {
		V apply(long key, V value);
	}

	public V compute(long key, LongObjectFunction<V> op) {
		if (key == 0) {
			final V oldV = zeroValue;
			final V v = op.apply(key, oldV);
			if (v != oldV) {
				zeroValue = v;
				if (v == null) {
					if (hasZeroValue) {
						hasZeroValue = false;
						size--;
					}
				} else if (!hasZeroValue) {
					hasZeroValue = true;
					size++;
				}
			}
			return v;
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		for (int i = hash(key), m = mask; ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == key) {
				final V oldV = vt[i];
				final V v = op.apply(key, oldV);
				if (v != oldV) {
					vt[i] = v;
					if (v == null) {
						for (int j = (i + 1) & m; (key = kt[j]) != 0; j = (j + 1) & m) {
							final int h = hash(key);
							if (((j - h) & m) > ((i - h) & m)) {
								kt[i] = key;
								vt[i] = vt[j];
								i = j;
							}
						}
						kt[i] = 0;
						vt[i] = null;
						size--;
					}
				}
				return v;
			}
			if (k == 0) {
				final V v = op.apply(key, null);
				if (v != null) {
					kt[i] = key;
					vt[i] = v;
					if (++size >= threshold)
						resize(kt.length << 1);
				}
				return v;
			}
		}
	}

	public V remove(long key) {
		if (key == 0) {
			if (!hasZeroValue)
				return null;
			hasZeroValue = false;
			final V oldV = zeroValue;
			zeroValue = null;
			size--;
			return oldV;
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		final int m = mask;
		int i;
		for (i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == key)
				break;
			if (k == 0)
				return null;
		}
		final V oldV = vt[i];
		for (int j = (i + 1) & m; (key = kt[j]) != 0; j = (j + 1) & m) {
			final int h = hash(key);
			if (((j - h) & m) > ((i - h) & m)) {
				kt[i] = key;
				vt[i] = vt[j];
				i = j;
			}
		}
		kt[i] = 0;
		vt[i] = null;
		size--;
		return oldV;
	}

	public boolean remove(long key, V value) {
		if (key == 0) {
			if (!hasZeroValue || !Objects.equals(value, zeroValue))
				return false;
			hasZeroValue = false;
			zeroValue = null;
			size--;
			return true;
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		final int m = mask;
		int i;
		for (i = hash(key); ; i = (i + 1) & m) {
			final long k = kt[i];
			if (k == key) {
				if (!Objects.equals(value, vt[i]))
					return false;
				break;
			}
			if (k == 0)
				return false;
		}
		for (int j = (i + 1) & m; (key = kt[j]) != 0; j = (j + 1) & m) {
			final int h = hash(key);
			if (((j - h) & m) > ((i - h) & m)) {
				kt[i] = key;
				vt[i] = vt[j];
				i = j;
			}
		}
		kt[i] = 0;
		vt[i] = null;
		size--;
		return true;
	}

	public void clear() {
		if (size == 0)
			return;
		size = 0;
		hasZeroValue = false;
		zeroValue = null;
		Arrays.fill(keyTable, 0);
		Arrays.fill(valueTable, null);
	}

	public void clear(int maxCap) {
		final int tableSize = tableSize(Math.max(maxCap, 0));
		if (tableSize >= keyTable.length) {
			clear();
			return;
		}
		size = 0;
		hasZeroValue = false;
		zeroValue = null;
		resize(tableSize);
	}

	public void shrink(int maxCap) {
		final int tableSize = tableSize(Math.max(maxCap, size));
		if (tableSize < keyTable.length)
			resize(tableSize);
	}

	public void ensureCapacity(int cap) {
		final int tableSize = tableSize(Math.max(cap, 0));
		if (tableSize > keyTable.length)
			resize(tableSize);
	}

	private void resize(int newSize) { // [1,2,4,8,...,0x4000_0000]
		threshold = (int)(newSize * loadFactor);
		final int m = newSize - 1;
		mask = m;
		shift = Long.numberOfLeadingZeros(m);
		final long[] kt = new long[newSize];
		//noinspection unchecked
		final V[] vt = (V[])new Object[newSize];
		if (size != 0) {
			final long[] oldKt = keyTable;
			final V[] oldVt = valueTable;
			for (int j = 0, n = oldKt.length; j < n; j++) {
				final long k = oldKt[j];
				if (k != 0) {
					for (int i = hash(k); ; i = (i + 1) & m) {
						if (kt[i] == 0) {
							kt[i] = k;
							vt[i] = oldVt[j];
							break;
						}
					}
				}
			}
		}
		keyTable = kt;
		valueTable = vt;
	}

	public void foreachKey(LongConsumer consumer) {
		if (hasZeroValue)
			consumer.accept(0);
		for (final long k : keyTable)
			if (k != 0)
				consumer.accept(k);
	}

	public void foreachValue(Consumer<V> consumer) {
		if (hasZeroValue)
			consumer.accept(zeroValue);
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		for (int i = 0, n = kt.length; i < n; i++)
			if (kt[i] != 0)
				consumer.accept(vt[i]);
	}

	public interface LongObjectConsumer<V> {
		void accept(long key, V value);
	}

	public void foreach(LongObjectConsumer<V> consumer) {
		if (hasZeroValue)
			consumer.accept(0, zeroValue);
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		for (int i = 0, n = kt.length; i < n; i++) {
			final long k = kt[i];
			if (k != 0)
				consumer.accept(k, vt[i]);
		}
	}

	public interface LongObjectMapPredicate<V> {
		boolean test(LongHashMap<V> map, long key, V value);
	}

	public boolean foreachTest(LongObjectMapPredicate<V> tester) {
		if (hasZeroValue && !tester.test(this, 0, zeroValue))
			return false;
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		for (int i = 0, n = kt.length; i < n; i++) {
			final long k = kt[i];
			if (k != 0 && !tester.test(this, k, vt[i]))
				return false;
		}
		return true;
	}

	public void foreachUpdate(LongObjectFunction<V> func) {
		if (hasZeroValue) {
			final V oldV = zeroValue;
			final V v = func.apply(0, oldV);
			if (v != oldV) {
				zeroValue = v;
				if (v == null) {
					hasZeroValue = false;
					size--;
				}
			}
		}
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		for (int i = 0, n = kt.length; i < n; i++) {
			long k = kt[i];
			if (k != 0) {
				final V oldV = vt[i];
				final V v = func.apply(k, oldV);
				if (v != oldV) {
					vt[i] = v;
					if (v == null) {
						final int m = mask;
						for (int j = (i + 1) & m; (k = kt[j]) != 0; j = (j + 1) & m) {
							final int h = hash(k);
							if (((j - h) & m) > ((i - h) & m)) {
								kt[i] = k;
								vt[i] = vt[j];
								i = j;
							}
						}
						kt[i] = 0;
						vt[i] = null;
						size--;
					}
				}
			}
		}
	}

	@Override
	public LongHashMap<V> clone() throws CloneNotSupportedException {
		//noinspection unchecked
		final LongHashMap<V> map = (LongHashMap<V>)super.clone();
		map.keyTable = keyTable.clone();
		map.valueTable = valueTable.clone();
		return map;
	}

	@Override
	public String toString() {
		if (size == 0)
			return "{}";
		final StringBuilder sb = new StringBuilder(32).append('{');
		final long[] kt = keyTable;
		final V[] vt = valueTable;
		final int n = Math.min(kt.length, 20);
		int i = 0;
		if (hasZeroValue)
			sb.append('0').append('=').append(zeroValue);
		else {
			for (; i < n; i++) {
				final long k = kt[i];
				if (k != 0) {
					sb.append(k).append('=').append(vt[i++]);
					break;
				}
			}
		}
		for (; i < n; i++) {
			final long k = kt[i];
			if (k != 0)
				sb.append(',').append(k).append('=').append(vt[i]);
		}
		if (n != kt.length)
			sb.append(",...");
		return sb.append('}').toString();
	}
}
