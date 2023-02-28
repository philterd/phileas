package io.philterd.test.phileas.services.filters;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.objects.FilterResult;
import io.philterd.phileas.model.profile.filters.strategies.rules.SectionFilterStrategy;
import io.philterd.phileas.model.services.AlertService;
import io.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import io.philterd.phileas.services.filters.regex.SectionFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class SectionFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterSection1() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SectionFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SectionFilter filter = new SectionFilter(filterConfiguration, startPattern, endPattern);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "This is some test. BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 19, 72, FilterType.SECTION));
        Assertions.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterSection2() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SectionFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SectionFilter filter = new SectionFilter(filterConfiguration, startPattern, endPattern);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "This is some test. BEGIN-REDACT This text should be redacted. This is outside the text.");

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterSection3() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SectionFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SectionFilter filter = new SectionFilter(filterConfiguration, startPattern, endPattern);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 53, FilterType.SECTION));
        Assertions.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", filterResult.getSpans().get(0).getText());

    }

}
