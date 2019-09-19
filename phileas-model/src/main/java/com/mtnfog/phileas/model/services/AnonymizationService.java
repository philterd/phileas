package com.mtnfog.phileas.model.services;

import java.io.Serializable;

/**
 * An anonymization service.
 */
public interface AnonymizationService extends Serializable {

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
