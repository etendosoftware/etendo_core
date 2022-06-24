package com.etendoerp.redis.interfaces;

import com.etendoerp.redis.RedisClient;
import org.redisson.api.LocalCachedMapOptions;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A cached {@link ConcurrentMap} that uses Redis or in memory cache depending on the server configuration.
 * Uses a {@link org.redisson.api.RLocalCachedMap} when a Redis server is configured,
 * and a {@link ConcurrentHashMap} otherwise.
 * @param <K> the type of keys maintained by this map.
 * @param <V> the type of mapped values. They must be Serializable to support Redis usage (refer to the Redisson documentation).
 */
public class CachedConcurrentMap<K, V> implements ConcurrentMap<K, V> {
    private final Map<K, V> instance;

    /**
     * Creates a Map using a Redis cache when possible.
     * @param name key to use in Redis. (This is not used when Redis is not configured.)
     */
    public CachedConcurrentMap(String name) {
        this(name, true, LocalCachedMapOptions.defaults());
    }

    /**
     * Creates a Map using a Redis cache when possible.
     * When localCache is true, uses the default {@link LocalCachedMapOptions}
     * @param name key to use in Redis. (This is not used when Redis is not configured.)
     * @param localCache whether to use {@link org.redisson.api.RLocalCachedMap} or a {@link org.redisson.api.RMap}
     */
    public CachedConcurrentMap(String name, boolean localCache) {
        this(name, localCache, localCache ? LocalCachedMapOptions.defaults() : null);
    }

    /**
     * Creates a Map using a Redis cache when possible.
     * @param name key to use in Redis. (This is not used when Redis is not configured.)
     * @param localCache whether to use {@link org.redisson.api.RLocalCachedMap} or a {@link org.redisson.api.RMap}
     * @param options {@link LocalCachedMapOptions} cache options. Only used when localCache is true
     */
    public CachedConcurrentMap(String name, boolean localCache, LocalCachedMapOptions<K, V> options) {
        this(name, localCache, options, null);
    }

    /**
     * Creates a Map using a Redis cache when possible.
     * @param name key to use in Redis. (This is not used when Redis is not configured.)
     * @param localCache whether to use {@link org.redisson.api.RLocalCachedMap} or a {@link org.redisson.api.RMap}
     * @param options {@link LocalCachedMapOptions} cache options. Only used when localCache is true
     * @param initialCapacity map initial capacity. Only used when Redis is not available
     */
    public CachedConcurrentMap(String name, boolean localCache, LocalCachedMapOptions<K, V> options, Integer initialCapacity) {
        if (RedisClient.getInstance().isAvailable()) {
            if (localCache) {
                instance = RedisClient.getInstance().getClient().getLocalCachedMap(name, options);
            } else {
                instance = RedisClient.getInstance().getClient().getMap(name);
            }
        } else {
            if (initialCapacity != null) {
                instance = new ConcurrentHashMap<>(initialCapacity);
            } else {
                instance = new ConcurrentHashMap<>();
            }
        }
    }

    /**
     * @see ConcurrentMap#size()
     */
    @Override
    public int size() {
        return instance.size();
    }

    /**
     * @see ConcurrentMap#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return instance.isEmpty();
    }

    /**
     * @see ConcurrentMap#containsKey(Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return instance.containsKey(key);
    }

    /**
     * @see ConcurrentMap#containsValue(Object)
     */
    @Override
    public boolean containsValue(Object value) {
        return instance.containsValue(value);
    }

    /**
     * @see ConcurrentMap#get(Object)
     */
    @Override
    public V get(Object key) {
        return instance.get(key);
    }

    /**
     * @see ConcurrentMap#put(Object, Object)
     */
    @Override
    public V put(K key, V value) {
        return instance.put(key, value);
    }

    /**
     * @see ConcurrentMap#remove(Object)
     */
    @Override
    public V remove(Object key) {
        return instance.remove(key);
    }

    /**
     * @see ConcurrentMap#putAll(Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        instance.putAll(m);
    }

    /**
     * @see ConcurrentMap#clear()
     */
    @Override
    public void clear() {
        instance.clear();
    }

    /**
     * @see ConcurrentMap#keySet()
     */
    @Override
    public Set<K> keySet() {
        return instance.keySet();
    }

    /**
     * @see ConcurrentMap#values()
     */
    @Override
    public Collection<V> values() {
        return instance.values();
    }

    /**
     * @see ConcurrentMap#entrySet()
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return instance.entrySet();
    }

    /**
     * @see ConcurrentMap#getOrDefault(Object, Object)
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return instance.getOrDefault(key, defaultValue);
    }

    /**
     * @see ConcurrentMap#forEach(BiConsumer)
     */
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        instance.forEach(action);
    }

    /**
     * @see ConcurrentMap#putIfAbsent(Object, Object)
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return instance.putIfAbsent(key, value);
    }

    /**
     * @see ConcurrentMap#remove(Object, Object)
     */
    @Override
    public boolean remove(Object key, Object value) {
        return instance.remove(key, value);
    }

    /**
     * @see ConcurrentMap#replace(Object, Object, Object)
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return instance.replace(key, oldValue, newValue);
    }

    /**
     * @see ConcurrentMap#replace(Object, Object)
     */
    @Override
    public V replace(K key, V value) {
        return instance.replace(key, value);
    }

    /**
     * @see ConcurrentMap#replaceAll(BiFunction)
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        instance.replaceAll(function);
    }

    /**
     * @see ConcurrentMap#computeIfAbsent(Object, Function)
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return instance.computeIfAbsent(key, mappingFunction);
    }

    /**
     * @see ConcurrentMap#computeIfPresent(Object, BiFunction)
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return instance.computeIfPresent(key, remappingFunction);
    }

    /**
     * @see ConcurrentMap#compute(Object, BiFunction)
     */
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return instance.compute(key, remappingFunction);
    }

    /**
     * @see ConcurrentMap#merge(Object, Object, BiFunction)
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return instance.merge(key, value, remappingFunction);
    }
}
