/*
 * @(#)ConcurrentMap.java	1.6 04/07/12
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util.concurrent;
import java.util.Map;

/**
 * A {@link Map} providing additional atomic
 * <tt>putIfAbsent</tt>, <tt>remove</tt>, and <tt>replace</tt> methods.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *  
 * @since 1.5
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values 
 */
public interface ConcurrentMap<K, V> extends Map<K, V> {
    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * This is equivalent to
     * <pre>
     *   if (!map.containsKey(key)) 
     *      return map.put(key, value);
     *   else
     *      return map.get(key);
     * </pre>
     * Except that the action is performed atomically.
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     *
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is
     *            not supported by this map.
     * @throws ClassCastException if the class of the specified key or value
     *            prevents it from being stored in this map.
     * @throws IllegalArgumentException if some aspect of this key or value
     *            prevents it from being stored in this map.
     * @throws NullPointerException if this map does not permit <tt>null</tt>
     *            keys or values, and the specified key or value is
     *            <tt>null</tt>.
     *
     */
    V putIfAbsent(K key, V value);

    /**
     * Remove entry for key only if currently mapped to given value.
     * Acts as
     * <pre> 
     *  if ((map.containsKey(key) && map.get(key).equals(value)) {
     *     map.remove(key);
     *     return true;
     * } else return false;
     * </pre>
     * except that the action is performed atomically.
     * @param key key with which the specified value is associated.
     * @param value value associated with the specified key.
     * @return true if the value was removed, false otherwise
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is
     *            not supported by this map.
     * @throws NullPointerException if this map does not permit <tt>null</tt>
     *            keys or values, and the specified key or value is
     *            <tt>null</tt>.
     */
    boolean remove(Object key, Object value);


    /**
     * Replace entry for key only if currently mapped to given value.
     * Acts as
     * <pre> 
     *  if ((map.containsKey(key) && map.get(key).equals(oldValue)) {
     *     map.put(key, newValue);
     *     return true;
     * } else return false;
     * </pre>
     * except that the action is performed atomically.
     * @param key key with which the specified value is associated.
     * @param oldValue value expected to be associated with the specified key.
     * @param newValue value to be associated with the specified key.
     * @return true if the value was replaced
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is
     *            not supported by this map.
     * @throws NullPointerException if this map does not permit <tt>null</tt>
     *            keys or values, and the specified key or value is
     *            <tt>null</tt>.
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Replace entry for key only if currently mapped to some value.
     * Acts as
     * <pre> 
     *  if ((map.containsKey(key)) {
     *     return map.put(key, value);
     * } else return null;
     * </pre>
     * except that the action is performed atomically.
     * @param key key with which the specified value is associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is
     *            not supported by this map.
     * @throws NullPointerException if this map does not permit <tt>null</tt>
     *            keys or values, and the specified key or value is
     *            <tt>null</tt>.
     */
    V replace(K key, V value);

}
