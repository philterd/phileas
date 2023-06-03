package ai.philterd.phileas.services.profiles.cache;

import ai.philterd.phileas.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.services.FilterProfileCacheService;

import java.io.IOException;

public class FilterProfileCacheServiceFactory {

    private FilterProfileCacheServiceFactory() {
        // Use the static methods.
    }

    public static FilterProfileCacheService getInstance(PhileasConfiguration phileasConfiguration) throws IOException {

        if(phileasConfiguration.cacheRedisEnabled()) {

            return new RedisFilterProfileCacheService(phileasConfiguration);

        } else {

            return new InMemoryFilterProfileCacheService();

        }

    }

}
