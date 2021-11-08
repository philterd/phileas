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

    public FilterProfile getCombinedFilterProfiles(final List<String> filterProfileNames) throws IOException, IllegalStateException {

        // Get the deserialized filter profile of the first profile in the list.
        // By starting off with a full profile we don't have to worry about adding
        // Config and Crypto (and other sections) since those will always be
        // taken from the first profile.
        final FilterProfile combinedFilterProfile = getFilterProfile(filterProfileNames.get(0));

        // In some chases there may be only one filter profile.
        if(filterProfileNames.size() > 1) {

            // The name has no bearing on the results. We just want to give it a name.
            combinedFilterProfile.setName("combined");

            // Loop over the filter profile names and skip the first one since we have already
            // deserialized it to a filter profile a few lines above.
            for (final String filterProfileName : filterProfileNames.subList(1, filterProfileNames.size())) {

                // Get the filter profile.
                final FilterProfile filterProfile = getFilterProfile(filterProfileName);

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

                // Aggregate the Ignored and IgnoredPatterns into the combined profile.
                combinedFilterProfile.getIgnored().addAll(filterProfile.getIgnored());
                combinedFilterProfile.getIgnoredPatterns().addAll(filterProfile.getIgnoredPatterns());

            }

            // The Config and Crypto sections are expected to be in the first filter profile.
            // The Config and Crypto sections in the other filter profiles are ignored.

            // As other sections are added to the filter profile they will need added here, too.

        }

        return combinedFilterProfile;

    }

    private FilterProfile getFilterProfile(String filterProfileName) throws IOException {

        // This will ALWAYS return a filter profile because if it is not in the cache it will be retrieved from the cache.
        // TODO: How to trigger a reload if the profile had to be retrieved from disk?
        final String filterProfileJson = filterProfileService.get(filterProfileName);

        LOGGER.debug("Deserializing filter profile [{}]", filterProfileName);
        return gson.fromJson(filterProfileJson, FilterProfile.class);

    }

}
