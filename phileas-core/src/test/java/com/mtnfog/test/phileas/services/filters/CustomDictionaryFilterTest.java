package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
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

public class CustomDictionaryFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CustomDictionaryFilterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterDictionaryExactMatch() throws Exception {

        final List<CustomDictionaryFilterStrategy> strategies = Arrays.asList(new CustomDictionaryFilterStrategy());
        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, strategies, SensitivityLevel.LOW, null, alertService, "names", names, 0, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0,"He lived with Bill in California.");

        showSpans(spans);

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("bill", spans.get(0).getText());

    }

    @Test
    public void filterDictionaryNoMatch() throws Exception {

        final List<CustomDictionaryFilterStrategy> strategies = Arrays.asList(new CustomDictionaryFilterStrategy());
        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, strategies, SensitivityLevel.LOW, null, alertService, "names", names, 0, Collections.emptySet(), new Crypto(), windowSize);

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0,"He lived with Sam in California.");

        showSpans(spans);

        Assertions.assertEquals(0, spans.size());

    }

}
