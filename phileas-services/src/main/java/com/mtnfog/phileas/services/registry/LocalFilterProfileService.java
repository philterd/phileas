package com.mtnfog.phileas.services.registry;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.FilterProfileService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LocalFilterProfileService implements FilterProfileService {

    private static final Logger LOGGER = LogManager.getLogger(LocalFilterProfileService.class);

    private String filterProfilesDirectory;
    private Gson gson;

    public LocalFilterProfileService(Properties applicationProperties) {

        this.gson = new Gson();
        this.filterProfilesDirectory = applicationProperties.getProperty("filter.profiles.directory", System.getProperty("user.dir") + "/profiles/");
        LOGGER.info("Looking for filter profiles in {}", filterProfilesDirectory);

    }

    @Override
    public String getFilterProfile(String filterProfileName) throws IOException {

        final File file = new File(filterProfilesDirectory, filterProfileName + ".json");

        if(file.exists()) {
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } else {
            throw new IOException("Filter profile with name " + filterProfileName + " does not exist.");
        }

    }

    @Override
    public Map<String, FilterProfile> getAll() throws IOException {

        final Map<String, FilterProfile> filterProfiles = new HashMap<>();

        // Read the filter profiles from the file system.
        final Collection<File> files = FileUtils.listFiles(new File(filterProfilesDirectory), new String[]{"json"}, false);
        LOGGER.info("Found {} filter profiles", files.size());

        for(final File file : files) {

            LOGGER.info("Loading filter profile {}", file.getAbsolutePath());
            final String json = FileUtils.readFileToString(file, Charset.defaultCharset());
            final FilterProfile filterProfile = gson.fromJson(json, FilterProfile.class);
            filterProfiles.put(filterProfile.getName(), filterProfile);
            LOGGER.info("Added filter profile named {}", filterProfile.getName());

        }

        return filterProfiles;

    }

}


