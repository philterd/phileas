package io.philterd.test.phileas.services.filters;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.Filter;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.objects.FilterResult;
import io.philterd.phileas.model.profile.filters.strategies.rules.IbanCodeFilterStrategy;
import io.philterd.phileas.model.services.AlertService;
import io.philterd.phileas.services.anonymization.IbanCodeAnonymizationService;
import io.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import io.philterd.phileas.services.filters.regex.IbanCodeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class IbanCodeFilterTest extends AbstractFilterTest {

    private final AlertService alertService = Mockito.mock(AlertService.class);

    private Filter getFilter(boolean validate, boolean allowSpaces) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IbanCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new IbanCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IbanCodeFilter filter = new IbanCodeFilter(filterConfiguration, validate, allowSpaces);

        return filter;

    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter(true, false);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "bank code of GB33BUKB20201555555555 ok?");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 13, 35, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB33BUKB20201555555555", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final Filter filter = getFilter(false, false);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "bank code of GB15MIDL40051512345678 ok?");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 13, 35, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB15MIDL40051512345678", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter3() throws Exception {

        final Filter filter = getFilter(true, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "bank code of GB15 MIDL 4005 1512 3456 78 ok?");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 13, 40, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB15 MIDL 4005 1512 3456 78", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter4() throws Exception {

        final Filter filter = getFilter(true, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "bank code of GB15 MIDL 4005 1512 3456 zz ok?");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

}