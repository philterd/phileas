package com.mtnfog.phileas.services.profiles;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.exceptions.api.BadRequestException;
import com.mtnfog.phileas.model.services.FilterProfileCacheService;
import com.mtnfog.phileas.model.services.FilterProfileService;
import com.mtnfog.phileas.services.profiles.cache.InMemoryFilterProfileCacheService;
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

public class LocalFilterProfileService implements FilterProfileService {

    private static final Logger LOGGER = LogManager.getLogger(LocalFilterProfileService.class);

    private static final String JSON_EXTENSION = ".json";

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
        final Collection<File> files = FileUtils.listFiles(new File(filterProfilesDirectory), new String[]{"json"}, false);

        for(final File file : files) {

            final String json = FileUtils.readFileToString(file, Charset.defaultCharset());

            final JSONObject object = new JSONObject(json);
            final String name = object.getString("name");

            names.add(name);

        }

        return names;

    }

    @Override
    public String get(String filterProfileName) throws IOException {

        String filterProfileJson = filterProfileCacheService.get(filterProfileName);

        if(filterProfileJson == null) {

            // The filter profile wasn't found in the cache so look on the file system.

            final File file = new File(filterProfilesDirectory, filterProfileName + JSON_EXTENSION);

            if (file.exists()) {

                filterProfileJson = FileUtils.readFileToString(file, Charset.defaultCharset());

                // Put it in the cache.
                filterProfileCacheService.insert(filterProfileName, filterProfileJson);

            } else {
                throw new FileNotFoundException("Filter profile [" + filterProfileName + "] does not exist.");
            }

        }

        return filterProfileJson;

    }

    @Override
    public Map<String, String> getAll() throws IOException {

        final Map<String, String> filterProfiles = new HashMap<>();

        // Read the filter profiles from the file system.
        final Collection<File> files = FileUtils.listFiles(new File(filterProfilesDirectory), new String[]{"json"}, false);
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
    public void save(String filterProfileJson) throws IOException {

        try {

            final JSONObject object = new JSONObject(filterProfileJson);
            final String filterProfileName = object.getString("name");

            final File file = new File(filterProfilesDirectory, filterProfileName + JSON_EXTENSION);

            FileUtils.writeStringToFile(file, filterProfileJson, Charset.defaultCharset());

            // Put this filter profile into the cache.
            filterProfileCacheService.insert(filterProfileName, filterProfileJson);

        } catch (JSONException ex) {

            LOGGER.error("The provided filter profile is not valid.", ex);
            throw new BadRequestException("The provided filter profile is not valid.");

        }

    }

    @Override
    public void delete(String filterProfileName) throws IOException {

        final File file = new File(filterProfilesDirectory, filterProfileName + JSON_EXTENSION);

        if(file.exists()) {

            if(!file.delete()) {
                throw new IOException("Unable to delete filter profile " + filterProfileName + JSON_EXTENSION);
            }

            // Remove it from the cache.
            filterProfileCacheService.remove(filterProfileName);

        } else {
            throw new FileNotFoundException("Filter profile with name " + filterProfileName + " does not exist.");
        }

    }

}


