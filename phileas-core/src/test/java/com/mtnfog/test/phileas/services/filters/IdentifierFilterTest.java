package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.Identifier;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.IdentifierFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IdentifierFilterTest extends AbstractFilterTest {

    private final AnonymizationService anonymizationService = new AlphanumericAnonymizationService(new LocalAnonymizationCacheService());

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterId1() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is AB4736021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,19, FilterType.IDENTIFIER));
        Assertions.assertEquals("AB4736021", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterId2() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId3() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId4() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId5() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId6() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is 123-45-6789 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,21, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId7() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. Patient id 00076A and 93821A. He is on biotin. Diagnosed with A0100.");

        Assertions.assertEquals(5, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 48, 59, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 76, 81, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(2), 94, 100, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(3), 105, 111, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(4), 145, 150, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId8() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is 000-00-00-00 ABC123 in california.");

        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 22, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 23, 29, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId9() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is AZ12 ABC123/123ABC in california.");

        Assertions.assertEquals(3, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,14, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 15, 21, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(2), 22, 28, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId10() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is H3SNPUHYEE7JD3H in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,25, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId11() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is 86637729 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId12() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is 33778376 in california.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId13() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", "\\b[A-Z]{4,}\\b", true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is ABCD.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,14, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId14() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "the id is 1234.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10,14, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId15() throws Exception {

        final List<IdentifierFilterStrategy> strategies = Arrays.asList(new IdentifierFilterStrategy());
        IdentifierFilter filter = new IdentifierFilter("name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, strategies, anonymizationService, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "John Smith, patient ID A203493, was seen on February 18.");

        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23,30, FilterType.IDENTIFIER));

    }

}
