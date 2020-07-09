package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.dictionary.BloomFilterDictionaryFilter;
import com.mtnfog.phileas.model.filter.rules.dictionary.DictionaryFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

public class BloomFilterDictionaryFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(BloomFilterDictionaryFilterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterDictionaryExactMatch() throws Exception {

        final List<CustomDictionaryFilterStrategy> strategies = Arrays.asList(new CustomDictionaryFilterStrategy());
        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "Bill", "john"));
        final DictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, strategies, names, "none", 0.05, null, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "He lived with Bill in California.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryCaseInsensitiveMatch() throws Exception {

        final List<CustomDictionaryFilterStrategy> strategies = Arrays.asList(new CustomDictionaryFilterStrategy());
        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final DictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, strategies, names, "none", 0.05, null, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "He lived with Bill in California.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryNoMatch() throws Exception {

        final List<CustomDictionaryFilterStrategy> strategies = Arrays.asList(new CustomDictionaryFilterStrategy());
        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final DictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, strategies, Collections.emptySet(), "none", 0.05, null, alertService, Collections.emptySet(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "He lived with Sam in California.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
