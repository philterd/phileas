/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.model.services;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;
import ai.philterd.phileas.model.objects.Span;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CacheService {

    // ---- For alerts

    /**
     * Generate an alert.
     *
     * @param policy The name of the policy.
     * @param strategyId The ID of the filter strategy that caused the alert.
     * @param context The context.
     * @param documentId The document ID.
     * @param filterType The {@link FilterType}.
     */
    void generateAlert(String policy, String strategyId, String context, String documentId, FilterType filterType);

    /**
     * Gets all alerts.
     * @return A list of all the alerts.
     */
    List<Alert> getAlerts();

    /**
     * Delete an alert.
     * @param alertId The ID of the alert.
     */
    void deleteAlert(String alertId);

    /**
     * Remove all alerts.
     */
    void clearAlerts();

    // ---- For anonymization

    /**
     * Generates the key for the map.
     * @param context The context.
     * @param token The token
     * @return A key for the item's entry.
     */
    String generateAnonymizationCacheKey(String context, String token);

    /**
     * Puts a value into the cache.
     * @param context The context.
     * @param token The token.
     * @param replacement The replacement value.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    void putAnonymizedToken(String context, String token, String replacement) throws IOException;

    /**
     * Gets a value from the cache.
     * @param context The context.
     * @param token The token.
     * @return The cached value, or <code>null</code> if a value with the given key does not exist in the cache.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    String getAnonymizedToken(String context, String token) throws IOException;

    /**
     * Removes an item from the cache.
     * @param context The context.
     * @param token The token.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    void removeAnonymizedToken(String context, String token) throws IOException;

    /**
     * Determines if an item exists in the cache.
     * @param context The context.
     * @param token The key.
     * @return <code>true</code> if the cache contains the item; otherwise <code>false</code>.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    boolean containsAnonymizedToken(String context, String token) throws IOException;

    /**
     * Determines if an item with the given replacement value exists in the cache.
     * @param context The context.
     * @param replacement The replacement value.
     * @return <code>true</code> if the cache contains am item with the given value; otherwise <code>false</code>.
     * @throws IOException Thrown if the cache cannot be accessed.
     */
    boolean containsAnonymizedTokenValue(String context, String replacement) throws IOException;

    // ---- For policies

    List<String> getPolicies() throws IOException;

    String getPolicy(String policyName) throws IOException;

    Map<String, String> getAllPolicies() throws IOException;

    void insertPolicy(String policyName, String policy);

    void removePolicy(String policyName);

    void clearPolicyCache() throws IOException;

    // ---- For span disambiguation

    /**
     * Hashes and inserts the span into the cache.
     * @param context The context.
     * @param span The {@link Span} containing the window.
     * @param vectorSize The size of the vector.
     */
    void hashAndInsert(String context, double[] hashes, Span span, int vectorSize);

    /**
     * Gets a vector representation for a {@link Span} given a context.
     * @param context The context.
     * @param filterType The {@link FilterType} whose vector representation to get.
     * @return A map of integers representing the vector for the span.
     */
    Map<Double, Double> getVectorRepresentation(String context, FilterType filterType);


}
