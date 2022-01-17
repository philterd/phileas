package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.PersonsFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.services.anonymization.PersonsAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.ai.PersonsFilter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class PersonsFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PersonsFilterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);
    private MetricsService metricsService = Mockito.mock(MetricsService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PersonsFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());
        final Map<String, DescriptiveStatistics> stats = new LinkedHashMap<>();
        final Map<String, Double> thresholds = new LinkedHashMap<>();

        final PersonsFilter filter = new PersonsFilter(
                filterConfiguration,
                model.getAbsolutePath(),
                vocab.getAbsolutePath(),
                stats,
                metricsService,
                thresholds);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "George Washington was president.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.PERSON));
        Assertions.assertEquals("George Washington", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PersonsFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());
        final Map<String, DescriptiveStatistics> stats = new LinkedHashMap<>();
        final Map<String, Double> thresholds = new LinkedHashMap<>();

        final PersonsFilter filter = new PersonsFilter(
                filterConfiguration,
                model.getAbsolutePath(),
                vocab.getAbsolutePath(),
                stats,
                metricsService,
                thresholds);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. The name 456 should be filtered. Jeff Smith should be ignored.");

        Assertions.assertEquals(2, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.PERSON));
        Assertions.assertEquals("George Washington", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(0).getReplacement());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 116, 126, FilterType.PERSON));
        Assertions.assertEquals("Jeff Smith", filterResult.getSpans().get(1).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(1).getReplacement());

    }

}
