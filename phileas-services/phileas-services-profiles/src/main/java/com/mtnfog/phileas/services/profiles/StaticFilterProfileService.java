package com.mtnfog.phileas.services.profiles;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.FilterProfileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link FilterProfileService} that uses a static
 * filter profile
 */
public class StaticFilterProfileService implements FilterProfileService {

    private static final Logger LOGGER = LogManager.getLogger(StaticFilterProfileService.class);

    private FilterProfile filterProfile;
    private Gson gson;

    /**
     * Creates a static filter.
     * @param filterProfileJson The filter profile JSON string.
     */
    public StaticFilterProfileService(String filterProfileJson) {

        this.gson = new Gson();
        this.filterProfile = gson.fromJson(filterProfileJson, FilterProfile.class);

    }

    /**
     * Creates a static filter.
     * @param filterProfile A {@link FilterProfile}.
     */
    public StaticFilterProfileService(FilterProfile filterProfile) {

        this.filterProfile = filterProfile;

    }

    @Override
    public List<String> get() throws IOException {
        return Collections.emptyList();
    }

    /**
     * Gets the filter profile. Note that the <code>filterProfileName</code> argument
     * is ignored since there is only a single filter profile.
     * @param filterProfileName The name of the filter profile. This value is ignored.
     * @return The filter profile.
     */
    @Override
    public String get(String filterProfileName) {

        // The filterProfileName does not matter.
        // There is only one filter profile and it is returned.

        return gson.toJson(filterProfile);

    }

    /**
     * Gets a map of filter profiles. Note that the map will always only contain
     * a single filter profile.
     * @return A map of filter profiles.
     */
    @Override
    public Map<String, String> getAll() {

        final Map<String, String> filterProfiles = new HashMap<>();

        filterProfiles.put(filterProfile.getName(), gson.toJson(filterProfile));

        return filterProfiles;

    }

    @Override
    public void save(String filterProfileJson) throws IOException {
        // Will not be implemented.
    }

    @Override
    public void delete(String filterProfileName) throws IOException {
        // Will not be implemented.
    }

}
