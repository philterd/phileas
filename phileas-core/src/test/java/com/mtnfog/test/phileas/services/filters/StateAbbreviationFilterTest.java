package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.StateAbbreviationFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.StateAbbreviationAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.StateAbbreviationFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class StateAbbreviationFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(StateAbbreviationFilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StateAbbreviationFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StateAbbreviationFilter filter = new StateAbbreviationFilter(filterConfiguration);

        final String input = "The patient is from WV.";
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "docid", input);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(20, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(22, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filterResult.getSpans().get(0).getFilterType());
        Assertions.assertEquals("WV", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StateAbbreviationFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StateAbbreviationFilter filter = new StateAbbreviationFilter(filterConfiguration);

        final String input = "The patient is from wv.";
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "docid", input);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(20, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(22, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filterResult.getSpans().get(0).getFilterType());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new StateAbbreviationFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final StateAbbreviationFilter filter = new StateAbbreviationFilter(filterConfiguration);

        final String input = "Patients from WV and MD.";
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "docid", input);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());

        Assertions.assertEquals(21, filterResult.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(23, filterResult.getSpans().get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filterResult.getSpans().get(0).getFilterType());
        Assertions.assertEquals("MD", filterResult.getSpans().get(0).getText());

        Assertions.assertEquals(14, filterResult.getSpans().get(1).getCharacterStart());
        Assertions.assertEquals(16, filterResult.getSpans().get(1).getCharacterEnd());
        Assertions.assertEquals(FilterType.STATE_ABBREVIATION, filterResult.getSpans().get(1).getFilterType());
        Assertions.assertEquals("WV", filterResult.getSpans().get(1).getText());

    }

}
