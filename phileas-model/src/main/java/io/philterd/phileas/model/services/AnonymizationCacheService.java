package io.philterd.phileas.model.services;

import java.io.IOException;

/**
 * An anonymization cache service used to store
 * anonymized values in memory.
 */
public interface AnonymizationCacheService {

    /**
     * Generates the key for the map.
     * @param context The context.
     * @param token The token
     * @return A key for the item's entry.
     */
    String generateKey(String context, String token);

    /**
     * Puts a value into the cache.
     * @param context The context.
     * @param token The token.
     * @param replacement The replacement value.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    void put(String context, String token, String replacement) throws IOException;

    /**
     * Gets a value from the cache.
     * @param context The context.
     * @param token The token.
     * @return The cached value, or <code>null</code> if a value with the given key does not exist in the cache.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    String get(String context, String token) throws IOException;

    /**
     * Removes an item from the cache.
     * @param context The context.
     * @param token The token.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    void remove(String context, String token) throws IOException;

    /**
     * Determines if an item exists in the cache.
     * @param context The context.
     * @param token The key.
     * @return <code>true</code> if the cache contains the item; otherwise <code>false</code>.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    boolean contains(String context, String token) throws IOException;

    /**
     * Determines if an item with the given replacement value exists in the cache.
     * @param context The context.
     * @param replacement The replacement value.
     * @return <code>true</code> if the cache contains am item with the given value; otherwise <code>false</code>.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    boolean containsValue(String context, String replacement) throws IOException;

}
