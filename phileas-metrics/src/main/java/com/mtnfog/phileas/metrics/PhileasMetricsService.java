package com.mtnfog.phileas.metrics;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder;
import com.codahale.metrics.*;
import com.codahale.metrics.jmx.JmxReporter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.MetricsService;
import io.github.azagniotov.metrics.reporter.cloudwatch.CloudWatchReporter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class PhileasMetricsService implements MetricsService {

    private static final Logger LOGGER = LogManager.getLogger(PhileasMetricsService.class);

    private static final String TOTAL_DOCUMENTS_PROCESSED = "total.documents.processed";
    private static final String DOCUMENTS_PROCESSED = "documents.processed";
    private static final String ENTITY_CONFIDENCE = "entity.confidence";

    private transient MetricRegistry registry;

    private transient Counter processed;
    private transient Meter documents;
    private transient Histogram entityConfidenceValues;

    private transient ConsoleReporter consoleReporter;
    private transient JmxReporter jmxReporter;

    public PhileasMetricsService(Properties properties) {

        registry = new MetricRegistry();

        processed = registry.counter(TOTAL_DOCUMENTS_PROCESSED);
        documents = registry.meter(DOCUMENTS_PROCESSED);
        entityConfidenceValues = registry.histogram(ENTITY_CONFIDENCE);

        // Console reporter is always enabled
        consoleReporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        consoleReporter.start(300, TimeUnit.SECONDS);

        if(StringUtils.equalsIgnoreCase(properties.getProperty("metrics.jmx.enabled", "true"), "true")) {

            LOGGER.info("Enabling JMX metrics.");

            jmxReporter = JmxReporter.forRegistry(registry).build();
            jmxReporter.start();

        }

        if(StringUtils.equalsIgnoreCase(properties.getProperty("metrics.cloudwatch.enabled", "false"), "true")) {

            LOGGER.info("Enabling AWS CloudWatch metrics.");

            final String region = properties.getProperty("metrics.cloudwatch.region", "us-east-1");
            final String accessKey = properties.getProperty("metrics.cloudwatch.access.key", "");
            final String secretKey = properties.getProperty("metrics.cloudwatch.secret.key", "");
            final String namespace = properties.getProperty("metrics.cloudwatch.namespace", "Philter");

            AmazonCloudWatchAsync amazonCloudWatchAsync;

            if(StringUtils.isEmpty(accessKey) || StringUtils.isEmpty(secretKey)) {

                amazonCloudWatchAsync = AmazonCloudWatchAsyncClientBuilder
                        .standard()
                        .withRegion(Regions.fromName(region))
                        .build();

            } else {

                amazonCloudWatchAsync = AmazonCloudWatchAsyncClientBuilder
                        .standard()
                        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                        .withRegion(Regions.fromName(region))
                        .build();

            }

            final CloudWatchReporter cloudWatchReporter =
                    CloudWatchReporter.forRegistry(registry, amazonCloudWatchAsync, namespace)
                            .convertRatesTo(TimeUnit.SECONDS)
                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                            .filter(MetricFilter.ALL)
                            .withPercentiles(CloudWatchReporter.Percentile.P75, CloudWatchReporter.Percentile.P99)
                            .withOneMinuteMeanRate()
                            .withFiveMinuteMeanRate()
                            .withFifteenMinuteMeanRate()
                            .withMeanRate()
                            .withArithmeticMean()
                            .withStdDev()
                            .withStatisticSet()
                            .withZeroValuesSubmission()
                            .withReportRawCountValue()
                            .build();

            cloudWatchReporter.start(60, TimeUnit.SECONDS);

        }

    }

    @Override
    public void incrementProcessed() {

        incrementProcessed(1);

    }

    @Override
    public void incrementProcessed(long count) {

        processed.inc(count);
        documents.mark(count);

    }

    @Override
    public void reportEntitySpan(Span span) {

        entityConfidenceValues.update((int) (span.getConfidence() * 100));

    }

}
