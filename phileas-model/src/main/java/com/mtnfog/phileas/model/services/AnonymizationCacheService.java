package com.mtnfog.phileas.model.services;

import java.io.IOException;
import java.io.Serializable;

/**
 * An anonymization cache service used to store
 * anonymized values in memory.
 */
public interface AnonymizationCacheService extends Serializable {

    /**
     * Generates the key for the map.
     * @param context The context.
     * @param key The key
     * @return A key for the item's entry.
     */
    String generateKey(String context, String key);

    /**
     * Puts a value into the cache.
     * @param context The context.
     * @param key The key.
     * @param value The value.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    void put(String context, String key, String value) throws IOException;

    /**
     * Gets a value from the cache.
     * @param context The context.
     * @param key The key.
     * @return The cached value, or <code>null</code> if a value with the given key does not exist in the cache.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    String get(String context, String key) throws IOException;

    /**
     * Removes an item from the cache.
     * @param context The context.
     * @param key The key.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    void remove(String context, String key) throws IOException;

    /**
     * Determines if an item exists in the cache.
     * @param context The context.
     * @param key The key.
     * @return <code>true</code> if the cache contains the item; otherwise <code>false</code>.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    boolean contains(String context, String key) throws IOException;

    /**
     * Determines if an item with the given value exists in the cache.
     * @param value The value.
     * @return <code>true</code> if the cache contains am item with the given value; otherwise <code>false</code>.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    boolean containsValue(String value) throws IOException;

}
