package com.mtnfog.phileas.services.registry;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.FilterProfileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
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

    /**
     * Gets the filter profile. Note that the <code>filterProfileName</code> argument
     * is ignored since there is only a single filter profile.
     * @param filterProfileName The name of the filter profile. This value is ignored.
     * @return The filter profile.
     */
    @Override
    public String getFilterProfile(String filterProfileName) {

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
    public Map<String, FilterProfile> getAll() {

        final Map<String, FilterProfile> filterProfiles = new HashMap<>();

        filterProfiles.put(filterProfile.getName(), filterProfile);

        return filterProfiles;

    }

}
