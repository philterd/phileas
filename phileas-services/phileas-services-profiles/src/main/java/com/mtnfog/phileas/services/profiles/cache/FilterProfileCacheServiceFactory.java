package com.mtnfog.phileas.services.profiles.cache;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.services.FilterProfileCacheService;

import java.io.IOException;

public class FilterProfileCacheServiceFactory {

    public static FilterProfileCacheService getInstance(PhileasConfiguration phileasConfiguration) throws IOException {

        if(phileasConfiguration.cacheRedisEnabled()) {

            return new RedisFilterProfileCacheService(phileasConfiguration);

        } else {

            return new InMemoryFilterProfileCacheService();

        }

    }

}
