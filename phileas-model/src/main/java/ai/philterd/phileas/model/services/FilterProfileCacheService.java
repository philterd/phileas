package ai.philterd.phileas.model.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FilterProfileCacheService {

    /**
     * Gets the names of all filter profiles.
     * @return
     * @throws IOException
     */
    List<String> get() throws IOException;

    /**
     * Gets the content of a filter profile.
     * @param filterProfileName
     * @return
     * @throws IOException
     */
    String get(String filterProfileName) throws IOException;

    /**
     * Get the names and content of all filter profiles.
     * @return
     * @throws IOException
     */
    Map<String, String> getAll() throws IOException;

    /**
     * Inserts a new filter profile into the cache.
     * @param filterProfileName The name of the filter profile.
     * @param filterProfile The content of the filter profile.
     */
    void insert(String filterProfileName, String filterProfile);

    /**
     * Removes a filter profile from the cache.
     * @param filterProfileName The name of the filter profile.
     */
    void remove(String filterProfileName);

    /**
     * Clears all filter profiles from the cache.
     * @throws IOException
     */
    void clear() throws IOException;

}
