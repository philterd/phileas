package com.mtnfog.phileas.model.cache;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SslProvider;

import java.io.IOException;
import java.net.URL;

/**
 * Base class for classes that use a Redis cache.
 */
public abstract class AbstractRedisCacheService {

    private static final Logger LOGGER = LogManager.getLogger(AbstractRedisCacheService.class);

    protected final RedissonClient redisson;

    public AbstractRedisCacheService(PhileasConfiguration phileasConfiguration) throws IOException {

        final boolean cluster = phileasConfiguration.cacheRedisCluster();
        final String redisEndpoint = phileasConfiguration.cacheRedisHost();
        final int redisPort = phileasConfiguration.cacheRedisPort();
        final String authToken = phileasConfiguration.cacheRedisAuthToken();
        final boolean ssl = phileasConfiguration.cacheRedisSsl();

        final Config config = new Config();

        if (cluster) {

            final String protocol;

            if (ssl) {
                protocol = "rediss://";
            } else {
                protocol = "redis://";
            }

            final String redisAddress = protocol + redisEndpoint + ":" + redisPort;
            LOGGER.info("Using clustered redis connection: {}", redisAddress);

            config.useClusterServers()
                    .setScanInterval(2000)
                    .addNodeAddress(redisAddress)
                    .setPassword(authToken);

            if(StringUtils.isNotEmpty(phileasConfiguration.cacheRedisKeyStore())) {
                config.useClusterServers().setSslKeystore(new URL(phileasConfiguration.cacheRedisKeyStore()));
                config.useClusterServers().setSslKeystorePassword(phileasConfiguration.cacheRedisKeyStorePassword());
                config.useClusterServers().setSslTruststore(new URL(phileasConfiguration.cacheRedisTrustStore()));
                config.useClusterServers().setSslTruststorePassword(phileasConfiguration.cacheRedisTrustStorePassword());
                config.useClusterServers().setSslProvider(SslProvider.JDK);
            }

        } else {

            final String protocol;

            if (ssl) {
                protocol = "rediss://";
            } else {
                protocol = "redis://";
            }

            final String redisAddress = protocol + redisEndpoint + ":" + redisPort;
            LOGGER.info("Using single server redis connection {}", redisAddress);
            config.useSingleServer().setAddress(redisAddress);

            if(StringUtils.isNotEmpty(authToken)) {
                config.useSingleServer().setAddress(redisAddress).setPassword(authToken);
            } else {
                config.useSingleServer().setAddress(redisAddress);
            }

            if(StringUtils.isNotEmpty(phileasConfiguration.cacheRedisKeyStore())) {
                config.useSingleServer().setSslKeystore(new URL(phileasConfiguration.cacheRedisKeyStore()));
                config.useSingleServer().setSslKeystorePassword(phileasConfiguration.cacheRedisKeyStorePassword());
                config.useSingleServer().setSslTruststore(new URL(phileasConfiguration.cacheRedisTrustStore()));
                config.useSingleServer().setSslTruststorePassword(phileasConfiguration.cacheRedisTrustStorePassword());
                config.useSingleServer().setSslProvider(SslProvider.JDK);
            }

        }

        redisson = Redisson.create(config);

    }

}
