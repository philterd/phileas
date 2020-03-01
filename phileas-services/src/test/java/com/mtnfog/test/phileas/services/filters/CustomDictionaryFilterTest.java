package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomDictionaryFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CustomDictionaryFilterTest.class);

    @Test
    public void filterDictionaryExactMatch() throws Exception {

        final List<CustomDictionaryFilterStrategy> strategies = Arrays.asList(new CustomDictionaryFilterStrategy());
        final List<String> names = Arrays.asList("george", "ted", "bill", "john");
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, strategies, SensitivityLevel.LOW, null, "names", names, 0, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid","He lived with Bill in California.");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assert.assertEquals("bill", spans.get(0).getText());

    }

    @Test
    public void filterDictionaryNoMatch() throws Exception {

        final List<CustomDictionaryFilterStrategy> strategies = Arrays.asList(new CustomDictionaryFilterStrategy());
        final List<String> names = Arrays.asList("george", "ted", "bill", "john");
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.CUSTOM_DICTIONARY, strategies, SensitivityLevel.LOW, null, "names", names, 0, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid","He lived with Sam in California.");

        showSpans(spans);

        Assert.assertEquals(0, spans.size());

    }

}
