package com.mtnfog.test.phileas.services.ai;

import com.google.gson.Gson;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.filters.Ner;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.service.ai.PhileasSpan;
import com.mtnfog.phileas.service.ai.PyTorchFilter;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

public class PyTorchFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PyTorchFilterTest.class);

    private static MockWebServer mockServer;

    private int windowSize = 5;

    @BeforeAll
    public static void before() throws IOException {

        mockServer = new  MockWebServer();
        mockServer.start();

    }

    @AfterAll
    public static void after() throws IOException {

        mockServer.shutdown();

    }

    @Test
    public void removePunctuation() {

        String input = "My name is John St.. John.";
        String output = input.replaceAll("\\p{Punct}", " ");

        // This test is here to make sure that punctuation is replaced by a space.
        Assertions.assertEquals("My name is John St   John ", output);

    }

    @Test
    public void getJson() {

        List<PhileasSpan> spans = new LinkedList<>();
        spans.add(new PhileasSpan("test", "PER", 0.5, 1, 2));

        Gson gson = new Gson();

        LOGGER.info(gson.toJson(spans));

    }

    @Test
    public void personsTest() throws Exception {

        final Map<String, DescriptiveStatistics> stats = new HashMap<>();
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final AnonymizationService anonymizationService = null;
        final AlertService alertService = Mockito.mock(AlertService.class);
        final String baseUrl = this.mockServer.url("/").toString();

        final PhileasConfiguration phileasConfiguration = getConfiguration();

        this.mockServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"c\": \"context\", \"d\": \"docid\", \"p\": \"0\", \"spans\": [{\"text\":\"test\",\"tag\":\"PER\",\"score\":0.5,\"start\":1,\"end\":2}]}"));

        final PyTorchFilter t = new PyTorchFilter(baseUrl, FilterType.NER_ENTITY, getStrategies(), phileasConfiguration, "PER", stats, metricsService, anonymizationService, alertService, Collections.emptySet(), Collections.emptyList(),false, new Crypto(), windowSize);

        final FilterResult filterResult = t.filter(getFilterProfile(), "context", "doc", 0, "John Smith lives in New York");

        for(Span span : filterResult.getSpans()) {
            LOGGER.info(span.toString());
        }

        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void locationsTest() throws Exception {

        final Map<String, DescriptiveStatistics> stats = new HashMap<>();
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final AnonymizationService anonymizationService = null;
        final AlertService alertService = Mockito.mock(AlertService.class);
        final String baseUrl = this.mockServer.url("/").toString();

        final PhileasConfiguration phileasConfiguration = getConfiguration();

        LOGGER.info("Mock REST server baseUrl = " + baseUrl);

        this.mockServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"c\": \"context\", \"d\": \"docid\", \"p\": \"0\", \"spans\": [{\"text\":\"test\",\"tag\":\"LOC\",\"score\":0.5,\"start\":1,\"end\":2}]}"));

        final PyTorchFilter t = new PyTorchFilter(baseUrl, FilterType.NER_ENTITY, getStrategies(), phileasConfiguration, "LOC",
                stats, metricsService, anonymizationService, alertService, Collections.emptySet(), Collections.emptyList(), false, new Crypto(), windowSize);

        final FilterResult filterResult = t.filter(getFilterProfile(), "context", "docid", 0,"John Smith lives in New York");

        for(Span span : filterResult.getSpans()) {
            LOGGER.info(span.toString());
        }

        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    private List<AbstractFilterStrategy> getStrategies() throws IOException {

        final List<AbstractFilterStrategy> strategies = new LinkedList<>();

        NerFilterStrategy nerFilterStrategy = new NerFilterStrategy();
        //nerFilterStrategy.setConditions("type == 'PER'");
        //nerFilterStrategy.setConditions("confidence > 0.9");

        strategies.add(nerFilterStrategy);

        return strategies;

    }

    private FilterProfile getFilterProfile() throws IOException {

        NerFilterStrategy nerFilterStrategy = new NerFilterStrategy();

        Ner ner = new Ner();
        ner.setNerStrategies(Arrays.asList(nerFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setNer(ner);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("default");
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private PhileasConfiguration getConfiguration() {

        final Properties properties = new Properties();

        return ConfigFactory.create(PhileasConfiguration.class, properties);

    }

}
