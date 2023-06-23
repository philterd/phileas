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
package ai.philterd.phileas.services.profiles;

import ai.philterd.phileas.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.exceptions.api.BadRequestException;
import ai.philterd.phileas.model.objects.FilterProfileType;
import ai.philterd.phileas.model.services.AbstractFilterProfileService;
import ai.philterd.phileas.model.services.FilterProfileCacheService;
import ai.philterd.phileas.model.services.FilterProfileService;
import ai.philterd.phileas.services.profiles.cache.InMemoryFilterProfileCacheService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class LocalFilterProfileService extends AbstractFilterProfileService implements FilterProfileService {

    private static final Logger LOGGER = LogManager.getLogger(LocalFilterProfileService.class);

    private String filterProfilesDirectory;
    private FilterProfileCacheService filterProfileCacheService;

    public LocalFilterProfileService(PhileasConfiguration phileasConfiguration) {
        
        this.filterProfilesDirectory = phileasConfiguration.filterProfilesDirectory();
        LOGGER.info("Looking for filter profiles in {}", filterProfilesDirectory);

        // Always use an in-memory cache when using a local filter profile service.
        this.filterProfileCacheService = new InMemoryFilterProfileCacheService();

    }

    @Override
    public List<String> get() throws IOException {

        // This function never uses a cache.
        final List<String> names = new LinkedList<>();

        // Read the filter profiles from the file system.
        final Collection<File> files = FileUtils.listFiles(new File(filterProfilesDirectory), new String[]{"json", "yml"}, false);

        for(final File file : files) {
            names.add(file.getName());
        }

        return names;

    }

    @Override
    public String get(String filterProfileName) throws IOException {

        String filterProfile = filterProfileCacheService.get(filterProfileName);

        if(filterProfile == null) {

            // The filter profile wasn't found in the cache so look on the file system.
            final File jsonFile = new File(filterProfilesDirectory, filterProfileName + FilterProfileType.JSON.getFileExtension());

            if (jsonFile.exists()) {

                filterProfile = FileUtils.readFileToString(jsonFile, Charset.defaultCharset());

                // Put it in the cache.
                filterProfileCacheService.insert(filterProfileName, filterProfile);

            } else {

                // A JSON file was not found so look for a yml file instead.
                final File yamlFile = new File(filterProfilesDirectory, filterProfileName + FilterProfileType.YML.getFileExtension());

                if (yamlFile.exists()) {

                    filterProfile = FileUtils.readFileToString(yamlFile, Charset.defaultCharset());

                    // Put it in the cache.
                    filterProfileCacheService.insert(filterProfileName, filterProfile);

                } else {
                    throw new FileNotFoundException("Filter profile [" + filterProfileName + "] does not exist.");
                }

            }

        }

        return filterProfile;

    }

    @Override
    public Map<String, String> getAll() throws IOException {

        final Map<String, String> filterProfiles = new HashMap<>();

        // Read the filter profiles from the file system.
        final Collection<File> files = FileUtils.listFiles(new File(filterProfilesDirectory), new String[]{"json", "yml"}, false);
        LOGGER.info("Found {} filter profiles", files.size());

        for (final File file : files) {

            LOGGER.info("Loading filter profile {}", file.getAbsolutePath());
            final String json = FileUtils.readFileToString(file, Charset.defaultCharset());

            final JSONObject object = new JSONObject(json);
            final String name = object.getString("name");

            filterProfiles.put(name, json);
            LOGGER.info("Added filter profile named [{}]", name);

        }

        return filterProfiles;

    }

    @Override
    public void save(String filterProfile, FilterProfileType filterProfileType) {

        if(filterProfileType == FilterProfileType.JSON) {

            try {

                final JSONObject object = new JSONObject(filterProfile);
                final String filterProfileName = object.getString("name");

                final File file = new File(filterProfilesDirectory, filterProfileName + filterProfileType.getFileExtension());

                FileUtils.writeStringToFile(file, filterProfile, Charset.defaultCharset());

                // Put this filter profile into the cache.
                filterProfileCacheService.insert(filterProfileName, filterProfile);

            } catch (JSONException | IOException ex) {

                LOGGER.error("The provided filter profile is not valid or could not be saved.", ex);
                throw new BadRequestException("The provided filter profile is not valid or could not be saved.");

            }

        } else if(filterProfileType == FilterProfileType.YML) {

            try {

                // TODO: Get the name from the yaml.
                final String filterProfileName = "filterprofile";

                final File file = new File(filterProfilesDirectory, filterProfileName + filterProfileType.getFileExtension());

                FileUtils.writeStringToFile(file, filterProfile, Charset.defaultCharset());

                // Put this filter profile into the cache.
                filterProfileCacheService.insert(filterProfileName, filterProfile);

            } catch (JSONException | IOException ex) {

                LOGGER.error("The provided filter profile is not valid or could not be saved.", ex);
                throw new BadRequestException("The provided filter profile is not valid or could not be saved.");

            }

        } else {

        }

    }

    @Override
    public void delete(String filterProfileName, FilterProfileType filterProfileType) throws IOException {

        File file = null;

        if(filterProfileType == FilterProfileType.JSON) {
            file = new File(filterProfilesDirectory, filterProfileName + filterProfileType.getFileExtension());
        } else {
            file = new File(filterProfilesDirectory, filterProfileName + filterProfileType.getFileExtension());
        }

        if(file.exists()) {

            if(!file.delete()) {
                throw new IOException("Unable to delete filter profile " + filterProfileName + filterProfileType.getFileExtension());
            }

            // Remove it from the cache.
            filterProfileCacheService.remove(filterProfileName);

        } else {
            throw new FileNotFoundException("Filter profile with name " + filterProfileName + " does not exist.");
        }

    }

}


