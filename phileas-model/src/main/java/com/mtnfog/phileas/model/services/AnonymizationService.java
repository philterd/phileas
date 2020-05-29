package com.mtnfog.phileas.model.services;

/**
 * An anonymization service.
 */
public interface AnonymizationService {

    /**
     * Anonymize the given token.
     * @param token The token.
     * @return The anonymized token.
     */
    String anonymize(String token);

    /**
     * Gets the anonymization cache service used for this anonymization service.
     * @return A {@link AnonymizationCacheService}.
     */
    AnonymizationCacheService getAnonymizationCacheService();

}
