package com.mtnfog.phileas.metrics;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.services.MetricsService;
import io.micrometer.cloudwatch.CloudWatchConfig;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.datadog.DatadogConfig;
import io.micrometer.datadog.DatadogMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class PhileasMetricsService implements MetricsService {

    private static final Logger LOGGER = LogManager.getLogger(PhileasMetricsService.class);

    private static final String TOTAL_DOCUMENTS_PROCESSED = "total.documents.processed";
    private static final String DOCUMENTS_PROCESSED = "documents.processed";

    private transient CompositeMeterRegistry compositeMeterRegistry;

    private transient Counter processed;
    private transient Counter documents;
    private transient Map<FilterType, Counter> filterTypes;

    /**
     * Creates a new metrics service.
     * @param phileasConfiguration The {@link PhileasConfiguration phileasConfiguration} used to initialize the application.
     */
    public PhileasMetricsService(PhileasConfiguration phileasConfiguration) {

        compositeMeterRegistry = new CompositeMeterRegistry();

        if(phileasConfiguration.metricsJmxEnabled()) {
            LOGGER.info("Initializing JMX metric reporting.");

            final JmxConfig jmxConfig = new JmxConfig() {
                @Override
                public String get(String s) {
                    return null;
                }

                @Override
                public Duration step() {
                    return Duration.ofSeconds(60);
                }

                @Override
                public String prefix() {
                    return phileasConfiguration.metricsPrefix();
                }
            };

            compositeMeterRegistry.add(new JmxMeterRegistry(jmxConfig, Clock.SYSTEM));
        }

        if(phileasConfiguration.metricsDataDogEnabled()) {

            LOGGER.info("Initializing Datadog metric reporting.");

            final String datadogApiKey = phileasConfiguration.metricsDataDogApiKey();

            if (StringUtils.isEmpty(datadogApiKey)) {

                LOGGER.warn("Datadog metric reporting enabled but no Datadog API key provided. Reporting will not be enabled.");

            } else {

                final DatadogConfig datadogConfig = new DatadogConfig() {
                    @Override
                    public String apiKey() {
                        return phileasConfiguration.metricsDataDogApiKey();
                    }

                    @Override
                    public Duration step() {
                        return Duration.ofSeconds(60);
                    }

                    @Override
                    public String get(String k) {
                        return null;
                    }

                    @Override
                    public String prefix() {
                        return phileasConfiguration.metricsPrefix();
                    }

                };

                compositeMeterRegistry.add(new DatadogMeterRegistry(datadogConfig, Clock.SYSTEM));

            }

        }

        if(phileasConfiguration.metricsCloudWatchEnabled()) {

            LOGGER.info("Initializing AWS CloudWatch metric reporting.");

            final CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {

                @Override
                public String get(String s) {
                    return null;
                }

                @Override
                public Duration step() {
                    return Duration.ofSeconds(60);
                }

                @Override
                public String namespace() {
                    return phileasConfiguration.metricsCloudWatchNamespace();
                }

                @Override
                public String prefix() {
                    return phileasConfiguration.metricsPrefix();
                }

                @Override
                public int batchSize() {
                    // 20 is the maximum batch size.
                    return CloudWatchConfig.MAX_BATCH_SIZE;
                }

            };

            final AmazonCloudWatchAsync amazonCloudWatchAsync = AmazonCloudWatchAsyncClientBuilder
                    .standard()
                    .withRegion(Regions.fromName(phileasConfiguration.metricsCloudWatchRegion()))
                    .build();

            compositeMeterRegistry.add(new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, amazonCloudWatchAsync));

        }

        this.processed = compositeMeterRegistry.counter(phileasConfiguration.metricsPrefix() + "." + TOTAL_DOCUMENTS_PROCESSED, phileasConfiguration.metricsTag());
        this.documents = compositeMeterRegistry.counter(phileasConfiguration.metricsPrefix() + "." + DOCUMENTS_PROCESSED, phileasConfiguration.metricsTag());

        // Add a counter for each filter type.
        this.filterTypes = new HashMap<>();
        for(FilterType filterType : FilterType.values()) {
            filterTypes.put(filterType, compositeMeterRegistry.counter(phileasConfiguration.metricsPrefix() + "." + filterType.name().toLowerCase().replace("-", "."), phileasConfiguration.metricsTag()));
        }

    }

    @Override
    public void incrementFilterType(FilterType filterType) {

        filterTypes.get(filterType).increment();

    }

    @Override
    public void incrementProcessed() {

        incrementProcessed(1);

    }

    @Override
    public void incrementProcessed(long count) {

        processed.increment(count);
        documents.increment(count);

    }

}
