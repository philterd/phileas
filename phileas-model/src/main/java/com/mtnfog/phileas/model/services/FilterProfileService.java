package com.mtnfog.phileas.model.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FilterProfileService {

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
     * Saves a filter profile.
     * @param filterProfileJson
     * @throws IOException
     */
    void save(String filterProfileJson) throws IOException;

    /**
     * Deletes a filter profile.
     * @param name
     * @throws IOException
     */
    void delete(String name) throws IOException;

}
