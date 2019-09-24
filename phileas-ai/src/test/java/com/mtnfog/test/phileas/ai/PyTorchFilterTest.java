package com.mtnfog.test.phileas.ai;

import com.google.gson.Gson;
import com.mtnfog.phileas.ai.PhileasSpan;
import com.mtnfog.phileas.ai.PyTorchFilter;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.filters.Ner;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.services.MetricsService;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

public class PyTorchFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PyTorchFilterTest.class);

    private MockWebServer mockServer;

    @Before
    public void before() throws IOException {

        mockServer = new  MockWebServer();
        mockServer.start();

    }

    @After
    public void after() throws IOException {

        mockServer.shutdown();

    }

    @Test
    public void getJson() {

        List<PhileasSpan> spans = new LinkedList<>();
        spans.add(new PhileasSpan("test", "PER", 0.5, 1, 2));

        Gson gson = new Gson();

        LOGGER.info(gson.toJson(spans));

    }

    @Test
    public void personsTest() throws IOException {

        final Map<String, DescriptiveStatistics> stats = new HashMap<>();
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final AnonymizationService anonymizationService = null;
        final String baseUrl = this.mockServer.url("/").toString();

        this.mockServer.enqueue(new MockResponse().setResponseCode(200).setBody("[{\"text\":\"test\",\"tag\":\"PER\",\"score\":0.5,\"start\":1,\"end\":2}]"));

        final PyTorchFilter t = new PyTorchFilter(Arrays.asList(baseUrl), FilterType.NER_ENTITY, "PER", stats, metricsService, anonymizationService);

        final List<Span> spans = t.filter(getFilterProfile(), "context", "doc", "John Smith lives in New York");

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

        Assert.assertEquals(1, spans.size());

    }

    @Test
    public void locationsTest() throws IOException {

        final Map<String, DescriptiveStatistics> stats = new HashMap<>();
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final AnonymizationService anonymizationService = null;
        final String baseUrl = this.mockServer.url("/").toString();

        LOGGER.info("Mock REST server baseUrl = " + baseUrl);

        this.mockServer.enqueue(new MockResponse().setResponseCode(200).setBody("[{\"text\":\"test\",\"tag\":\"LOC\",\"score\":0.5,\"start\":1,\"end\":2}]"));

        final PyTorchFilter t = new PyTorchFilter(Arrays.asList(baseUrl), FilterType.NER_ENTITY, "LOC",
                stats, metricsService, anonymizationService);

        final List<Span> spans = t.filter(getFilterProfile(), "context", "doc", "John Smith lives in New York");

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

        Assert.assertEquals(1, spans.size());

    }

    private FilterProfile getFilterProfile() throws IOException {

        NerFilterStrategy nerFilterStrategy = new NerFilterStrategy();

        Ner ner = new Ner();
        ner.setNerStrategies(Arrays.asList(nerFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

       // ZipCode zipCode = new ZipCode();
       // zipCode.setZipCodeFilterStrategy(zipCodeFilterStrategy);

        Identifiers identifiers = new Identifiers();
        identifiers.setNer(ner);
       // identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("default");
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

}
