package com.mtnfog.phileas.services.cache.profiles;

import com.mtnfog.phileas.model.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.services.FilterProfileCacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

public class FilterProfileCacheServiceFactory {

    public static FilterProfileCacheService getInstance(PhileasConfiguration phileasConfiguration) {

        if(phileasConfiguration.cacheRedisEnabled()) {

            return new RedisFilterProfileCacheService(phileasConfiguration);

        } else {

            return new InMemoryFilterProfileCacheService();

        }

    }

}
