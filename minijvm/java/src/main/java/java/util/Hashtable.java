/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package java.util;

public class Hashtable<K, V> implements Map<K, V> {

    private final HashMap<K, V> map;

    public Hashtable(int capacity) {
        map = new HashMap(capacity);
    }

    public Hashtable(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    public Hashtable() {
        this(0);
    }

    public Hashtable(Map<? extends K, ? extends V> m) {
        this(m.size());
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public synchronized String toString() {
        return map.toString();
    }

    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    public synchronized int size() {
        return map.size();
    }
    
    public synchronized boolean contains(Object value) {
        return map.containsValue(value);
    }

    public synchronized boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public synchronized boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public synchronized V get(Object key) {
        return map.get(key);
    }

    public synchronized V put(K key, V value) {
        return map.put(key, value);
    }

    public synchronized void putAll(Map<? extends K, ? extends V> elts) {
        map.putAll(elts);
    }

    public synchronized V remove(Object key) {
        return map.remove(key);
    }

    public synchronized void clear() {
        map.clear();
    }

    public Enumeration<K> keys() {
        // 标准 J2SE Hashtable 的 keys()/elements() 返回的 Enumeration 不依赖
        // HashMap 的 fail-fast iterator，因此在遍历过程中对本表做 remove 不会抛
        // ConcurrentModificationException。这里先前委托给 Collections.enumeration
        // (keySet())，而 keySet() 底层是 HashMap iterator，遍历中 remove 会抛 CME，
        // 导致依赖"边遍历边删除"的 J2ME 游戏（如 truckracer 的 bc.a(String) 清缓存）
        // 直接崩溃。改为返回一份 key 快照的 Enumeration，对齐 RI 语义。
        final Object[] snapshot;
        synchronized (this) {
            snapshot = map.keySet().toArray();
        }
        return new Enumeration<K>() {
            private int i = 0;
            public boolean hasMoreElements() { return i < snapshot.length; }
            @SuppressWarnings("unchecked")
            public K nextElement() {
                if (i >= snapshot.length) {
                    throw new java.util.NoSuchElementException();
                }
                return (K) snapshot[i++];
            }
        };
    }

    public Enumeration<V> elements() {
        // 同 keys()：返回 value 快照，避免遍历中 remove 抛 CME。
        final Object[] snapshot;
        synchronized (this) {
            snapshot = map.values().toArray();
        }
        return new Enumeration<V>() {
            private int i = 0;
            public boolean hasMoreElements() { return i < snapshot.length; }
            @SuppressWarnings("unchecked")
            public V nextElement() {
                if (i >= snapshot.length) {
                    throw new java.util.NoSuchElementException();
                }
                return (V) snapshot[i++];
            }
        };
    }

    public Set<Entry<K, V>> entrySet() {
        return new Collections.SynchronizedSet(map.entrySet());
    }

    public Set<K> keySet() {
        return new Collections.SynchronizedSet(map.keySet());
    }

    public Collection<V> values() {
        return new Collections.SynchronizedCollection(map.values());
    }

}
