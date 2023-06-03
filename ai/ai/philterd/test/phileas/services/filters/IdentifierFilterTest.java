package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.Crypto;
import ai.philterd.phileas.model.profile.filters.Identifier;
import ai.philterd.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.MacAddressAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.filters.regex.IdentifierFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IdentifierFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new AlphanumericAnonymizationService(new LocalAnonymizationCacheService());

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterId1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is AB4736021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,19, FilterType.IDENTIFIER));
        Assertions.assertEquals("AB4736021", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterId2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is 123-45-6789 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,21, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. Patient id 00076A and 93821A. He is on biotin. Diagnosed with A01000.");

        Assertions.assertEquals(4, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 48, 59, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 94, 100, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(2), 105, 111, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(3), 145, 151, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is 000-00-00-00 ABC123 in california.");

        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 22, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 23, 29, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is AZ12 ABC1234/123ABC4 in california.");

        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 22, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 23, 30, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is H3SNPUHYEE7JD3H in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,25, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is 86637729 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId12() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is 33778376 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId13() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "\\b[A-Z]{4,}\\b", true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is ABCD.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,14, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId14() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the id is 123456.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,16, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId15() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new IdentifierFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "John Smith, patient ID A203493, was seen on February 18.");

        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23,30, FilterType.IDENTIFIER));

    }

}
