package io.xdag.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> {
    private Map<K, V> map;
    private final int cacheSize;

    public LRUCache(int initialCapacity) {
        map = new LinkedHashMap<K, V>(initialCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > cacheSize;
            }
        };
        this.cacheSize = initialCapacity;
    }

    public V get(K key) {
        return map.get(key);
    }

    public V put(K key,V val) {
        return map.put(key,val);
    }

    public V remove(K key) {
        return map.remove(key);
    }
}