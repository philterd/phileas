package com.mtnfog.phileas.services.cache.profiles;

import com.mtnfog.phileas.model.services.FilterProfileCacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

public class FilterProfileCacheServiceFactory {

    public static FilterProfileCacheService getInstance(final Properties properties) {

        final boolean isRedisCache = StringUtils.equalsIgnoreCase(properties.getProperty("cache.redis.enabled", "false"), "true");

        if(isRedisCache) {

            return new RedisFilterProfileCacheService(properties);

        } else {

            return new InMemoryFilterProfileCacheService();

        }

    }

}
