package io.philterd.test.phileas.services.filters;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.objects.FilterResult;
import io.philterd.phileas.model.services.AlertService;
import io.philterd.phileas.services.anonymization.AgeAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import io.philterd.phileas.services.filters.regex.AgeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class FilterTest extends AbstractFilterTest {

    protected static final Logger LOGGER = LogManager.getLogger(FilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void window0() throws Exception {

        // This tests span window creation.
        int windowSize = 3;


        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.");

        showSpans(filterResult.getSpans());

        final String[] window = new String[]{"the", "patient", "is", "35", "years", "old", "and", "hes", "cool"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(filterResult.getSpans().get(0).getWindow()));

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertArrayEquals(window, filterResult.getSpans().get(0).getWindow());
        Assertions.assertEquals("3.5 years old", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void window1() throws Exception {

        // This tests span window creation.
        int windowSize = 5;

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.");

        showSpans(filterResult.getSpans());

        final String[] window = new String[]{"first", "sentence", "the", "patient", "is", "35", "years", "old", "and", "hes", "cool", "this", "is"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(filterResult.getSpans().get(0).getWindow()));

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertArrayEquals(window, filterResult.getSpans().get(0).getWindow());
        Assertions.assertEquals("3.5 years old", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void window2() throws Exception {

        // Getting window of size 5 for start -1 and end 12

        int windowSize = 5;

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.");

        showSpans(filterResult.getSpans());

        final String[] window = new String[]{"first", "sentence", "the", "patient", "is", "35", "years", "old", "and", "hes", "cool", "this", "is"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(filterResult.getSpans().get(0).getWindow()));

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertArrayEquals(window, filterResult.getSpans().get(0).getWindow());
        Assertions.assertEquals("3.5 years old", filterResult.getSpans().get(0).getText());

    }

}
