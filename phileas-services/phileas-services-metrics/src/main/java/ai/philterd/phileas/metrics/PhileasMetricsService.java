package ai.philterd.phileas.metrics;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder;
import ai.philterd.phileas.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.services.MetricsService;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.cloudwatch.CloudWatchConfig;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.datadog.DatadogConfig;
import io.micrometer.datadog.DatadogMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhileasMetricsService implements MetricsService {

    private static final Logger LOGGER = LogManager.getLogger(PhileasMetricsService.class);

    private static final String TOTAL_DOCUMENTS_PROCESSED = "total.documents.processed";
    private static final String DOCUMENTS_PROCESSED = "documents.processed";

    private transient CompositeMeterRegistry compositeMeterRegistry;

    private transient Counter processed;
    private transient Counter documents;
    private transient Map<FilterType, Counter> filterTypes;
    private transient Map<FilterType, Timer> filterTimers;

    /**
     * Creates a new metrics service.
     * @param phileasConfiguration The {@link PhileasConfiguration phileasConfiguration} used to initialize the application.
     */
    public PhileasMetricsService(PhileasConfiguration phileasConfiguration) {

        compositeMeterRegistry = new CompositeMeterRegistry();

        compositeMeterRegistry.config().commonTags("application", "philter");

        if(StringUtils.isNotEmpty(phileasConfiguration.metricsHostname())) {
            compositeMeterRegistry.config().commonTags("hostname", phileasConfiguration.metricsHostname());
        }

        final int step = phileasConfiguration.metricsStep();

        if(phileasConfiguration.metricsJmxEnabled()) {

            LOGGER.info("Initializing JMX metric reporting.");

            final JmxConfig jmxConfig = new JmxConfig() {
                @Override
                public String get(String s) {
                    return null;
                }

                @Override
                public Duration step() {
                    return Duration.ofSeconds(step);
                }

                @Override
                public String prefix() {
                    return phileasConfiguration.metricsPrefix();
                }
            };

            compositeMeterRegistry.add(new JmxMeterRegistry(jmxConfig, Clock.SYSTEM));

        }

        if(phileasConfiguration.metricsPrometheusEnabled()) {
            
            LOGGER.info("Initializing Prometheus metric reporting.");

            final PrometheusConfig prometheusConfig = new PrometheusConfig() {

                @Override
                public Duration step() {
                    return Duration.ofSeconds(step);
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

            final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(prometheusConfig);
            final int port = phileasConfiguration.metricsPrometheusPort();
            final String context = phileasConfiguration.metricsPrometheusContext();

            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/" + context, httpExchange -> {
                    final String response = prometheusRegistry.scrape();
                    httpExchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = httpExchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                });

                new Thread(server::start).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            compositeMeterRegistry.add(prometheusRegistry);

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
                        return Duration.ofSeconds(step);
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
                    return Duration.ofSeconds(step);
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

        this.processed = compositeMeterRegistry.counter(phileasConfiguration.metricsPrefix() + "." + TOTAL_DOCUMENTS_PROCESSED);
        this.documents = compositeMeterRegistry.counter(phileasConfiguration.metricsPrefix() + "." + DOCUMENTS_PROCESSED);

        // Add a counter for each filter type.
        this.filterTypes = new HashMap<>();
        for(final FilterType filterType : FilterType.values()) {
            filterTypes.put(filterType, compositeMeterRegistry.counter(phileasConfiguration.metricsPrefix() + "." + filterType.name().toLowerCase().replace("-", ".")));
        }

        // Add a timer for each filter type.
        this.filterTimers = new HashMap<>();
        for(final FilterType filterType : FilterType.values()) {
            final String name = phileasConfiguration.metricsPrefix() + "." + filterType.name().toLowerCase().replace("-", ".") + ".time.ms";
            filterTimers.put(filterType, compositeMeterRegistry.timer(name));
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

    @Override
    public void logFilterTime(FilterType filterType, long timeMs) {

        filterTimers.get(filterType).record(timeMs, TimeUnit.MILLISECONDS);

    }

}
