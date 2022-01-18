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

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 116, 126, FilterType.PERSON));
        Assertions.assertEquals("Jeff Smith", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(0).getReplacement());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 0, 17, FilterType.PERSON));
        Assertions.assertEquals("George Washington", filterResult.getSpans().get(1).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(1).getReplacement());

    }

    @Test
    public void filter3() throws Exception {

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

        final String input = "In recent days, healthcare facilities across the nation have again begun to buckle under spiking infection rates. Last week, some local hospitals temporarily postponed scheduled surgeries that require an inpatient stay following an operation, and the trauma center at Harbor-UCLA Medical Center closed for hours because of a blood shortage - a step it hadn't taken in over three decades. A staff shortage at some local ambulance companies further complicated the situation.The virus has spread so fast since the arrival of the Omicron variant that it could take just about a week for California to tally a million new cases. It was only on Jan. 10 that California surpassed 6 million total reported coronavirus cases in the nearly two years since the start of the pandemic, according to data released by state health officials. Even during last winter's surge, it took three weeks to accumulate a million new cases, with the state peaking at 46,000 new infections a day. \"On this national holiday where we celebrate the life and legacy of Dr. Martin Luther King, we remember his deep commitment to health equity,\" said L.A. County Public Health Director Barbara Ferrer Ferrer. \"As Reverend King memorably said, \"Of all the forms of inequality, injustice in health is the most shocking and the most inhuman because it often results in physical death.\"";

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, input);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(3, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 1043, 1061, FilterType.PERSON));
        Assertions.assertEquals("Martin Luther King", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(0).getReplacement());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 1154, 1175, FilterType.PERSON));
        Assertions.assertEquals("Barbara Ferrer Ferrer", filterResult.getSpans().get(1).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(1).getReplacement());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(2), 1181, 1194, FilterType.PERSON));
        Assertions.assertEquals("Reverend King", filterResult.getSpans().get(2).getText());
        Assertions.assertEquals("{{{REDACTED-person}}}", filterResult.getSpans().get(2).getReplacement());

    }

}
