package com.mtnfog.phileas.services.cache.profiles;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.services.FilterProfileCacheService;

public class FilterProfileCacheServiceFactory {

    public static FilterProfileCacheService getInstance(PhileasConfiguration phileasConfiguration) {

        if(phileasConfiguration.cacheRedisEnabled()) {

            return new RedisFilterProfileCacheService(phileasConfiguration);

        } else {

            return new InMemoryFilterProfileCacheService();

        }

    }

}
