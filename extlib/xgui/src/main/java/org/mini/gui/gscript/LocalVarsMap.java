package org.mini.gui.gscript;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * implementation of bin map
 * why do it
 * the map get or put do not create any new instance
 *
 * @param <K>
 * @param <V>
 */
public class LocalVarsMap<K, V> {
    private List<K> keylist = new ArrayList();
    private List<V> valuelist = new ArrayList();

    public LocalVarsMap() {
    }


    public V get(K k) {
        int index = binsearch(k);
        if (index >= 0) {
            V v = valuelist.get(index);
            return v;
        }
        return null;
    }

    public V put(K k, V v) {
        if (keylist.size() == 0) {
            keylist.add(0, k);
            valuelist.add(0, v);
            return null;
        }

        int newhash = k.hashCode();
        int L = 1;
        int R = keylist.size();
        int mid = 0;
        while (L <= R) {
            mid = (L + R) / 2;
            int oldhash = keylist.get(mid - 1).hashCode();
            if (oldhash == newhash) {
                break;
            } else if (newhash < oldhash) {
                R = mid - 1;
            } else {
                L = mid + 1;
            }
        }
        int oldhash = keylist.get(mid - 1).hashCode();
        if (oldhash == newhash) {
            V oldv = valuelist.get(mid - 1);
            valuelist.set(mid - 1, v);
            return oldv;
        } else if (newhash < oldhash) {
            keylist.add(mid - 1, k);
            valuelist.add(mid - 1, v);

        } else {
            keylist.add(mid, k);
            valuelist.add(mid, v);
        }
        return null;
    }

    public V remove(K k) {
        int index = binsearch(k);
        if (index >= 0) {
            V oldv = valuelist.get(index);
            keylist.remove(index);
            valuelist.remove(index);
            return oldv;
        }
        return null;
    }


    public int binsearch(K k) {
        if (keylist.size() == 0) {
            return -1;
        }


        int newhash = k.hashCode();
        int L = 1;
        int R = keylist.size();
        int mid = 0;
        while (L <= R) {
            mid = (L + R) / 2;
            int oldhash = keylist.get(mid - 1).hashCode();
            if (oldhash == newhash) {
                break;
            } else if (newhash < oldhash) {
                R = mid - 1;
            } else {
                L = mid + 1;
            }
        }
        int oldhash = keylist.get(mid - 1).hashCode();
        if (oldhash == newhash) {
            return mid - 1;
        } else {
            return -1;
        }
    }

    public void clear() {
        keylist.clear();
        valuelist.clear();
    }

    public List<K> getKeylist() {
        return keylist;
    }

    public void putAll(Map<K, V> map) {
        for (Iterator<K> it = map.keySet().iterator(); it.hasNext(); ) {
            K k = it.next();
            V v = map.get(k);
            this.put(k, v);
        }
    }

//    public static void main(String[] args) {
//        LocalVarsMap<Integer, String> map = new LocalVarsMap<>();
//        map.put(1, "A");
//        map.put(2, "B");
//        map.put(1, "C");
//        map.put(2, "D");
//        map.put(3, "E");
//        map.put(0, "F");
//
//        String s;
//        s = map.get(0);
//        s = map.get(1);
//        s = map.get(2);
//        s = map.get(3);
//        s = map.get(4);
//    }
}
