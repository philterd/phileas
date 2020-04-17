package com.mtnfog.phileas.model.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FilterProfileService {

    /**
     * Gets the names of all filter profiles from
     * the backend store directly by-passing the cache.
     * @return A list of filter profile names.
     * @throws IOException
     */
    List<String> get() throws IOException;

    /**
     * Gets the content of a filter profile.
     * @param filterProfileName
     * @param ignoreCache Whether or not to reference the cache.
     * @return The content of the filter profile.
     * @throws IOException
     */
    String get(String filterProfileName, boolean ignoreCache) throws IOException;

    /**
     * Get the names and content of all filter profiles.
     * @param ignoreCache Whether or not to reference the cache.
     * @return A map of filter profile names to filter profile content.
     * @throws IOException
     */
    Map<String, String> getAll(boolean ignoreCache) throws IOException;

    /**
     * Saves a filter profile.
     * @param filterProfileJson The content of the filter profile as JSON.
     * @throws IOException
     */
    void save(String filterProfileJson) throws IOException;

    /**
     * Deletes a filter profile.
     * @param name The name of the filter profile to delete.
     * @throws IOException
     */
    void delete(String name) throws IOException;

}
