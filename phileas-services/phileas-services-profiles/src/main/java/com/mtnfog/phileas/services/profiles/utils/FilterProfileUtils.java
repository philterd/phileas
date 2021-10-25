package com.mtnfog.phileas.services.profiles.utils;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.FilterProfileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class FilterProfileUtils {

    private static final Logger LOGGER = LogManager.getLogger(FilterProfileUtils.class);

    private FilterProfileService filterProfileService;
    private Gson gson;

    public FilterProfileUtils(FilterProfileService filterProfileService, Gson gson) {

        this.filterProfileService = filterProfileService;
        this.gson = gson;

    }

    public FilterProfile getCombinedFilterProfiles(List<String> filterProfileNames) throws IOException, IllegalStateException {

        // In some chases there may be only one filter profile. We need to make sure
        // the combined filter profile and that one filter profile are identical.

        if(filterProfileNames.size() == 1) {

            final String filterProfileName = filterProfileNames.get(0);

            // This will ALWAYS return a filter profile because if it is not in the cache it will be retrieved from the cache.
            // TODO: How to trigger a reload if the profile had to be retrieved from disk?
            final String filterProfileJson = filterProfileService.get(filterProfileName);

            LOGGER.debug("Deserializing filter profile [{}]", filterProfileName);
            return gson.fromJson(filterProfileJson, FilterProfile.class);

        } else {

            final FilterProfile combinedFilterProfile = new FilterProfile();

            for (final String filterProfileName : filterProfileNames) {

                // This will ALWAYS return a filter profile because if it is not in the cache it will be retrieved from the cache.
                // TODO: How to trigger a reload if the profile had to be retrieved from disk?
                final String filterProfileJson = filterProfileService.get(filterProfileName);

                LOGGER.debug("Deserializing filter profile [{}]", filterProfileName);
                final FilterProfile filterProfile = gson.fromJson(filterProfileJson, FilterProfile.class);

                // For each of the filter types, copy the filter (if it exists) from the source filter profile
                // to the destination (combined) filter profile. If a filter already exists in the destination (combined)
                // filter profile then throw an error.

                for(FilterType filterType : FilterType.values()) {
                    if (filterProfile.getIdentifiers().hasFilter(filterType)) {
                        if (!combinedFilterProfile.getIdentifiers().hasFilter(filterType)) {
                            combinedFilterProfile.getIdentifiers().setFilter(filterType, filterProfile.getIdentifiers().getFilter(filterType));
                        } else {
                            throw new IllegalStateException("Filter profile has duplicate filter: " + filterType.toString());
                        }
                    }
                }

                // TODO: Set the name of the combined filter profile.

            }

            return combinedFilterProfile;

        }

    }

}
