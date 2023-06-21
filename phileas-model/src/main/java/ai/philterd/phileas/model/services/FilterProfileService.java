/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
     * @return The filter profile.
     * @throws IOException
     */
    String get(String filterProfileName) throws IOException;

    /**
     * Get the names and content of all filter profiles.
     * @return A map of filter profile names to filter profile content.
     * @throws IOException
     */
    Map<String, String> getAll() throws IOException;

    /**
     * Saves a filter profile.
     * @param filterProfileJson The content of the filter profile as JSON.
     * @throws IOException
     */
    void save(String filterProfileJson) throws IOException;

    /**
     * Deletes a filter profile.
     * @param filterProfileName The name of the filter profile to delete.
     * @throws IOException
     */
    void delete(String filterProfileName) throws IOException;

}
