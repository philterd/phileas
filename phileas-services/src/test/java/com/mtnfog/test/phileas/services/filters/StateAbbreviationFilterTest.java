package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.StateAbbreviationFilterStrategy;
import com.mtnfog.phileas.services.anonymization.StateAbbreviationAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.StateAbbreviationFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StateAbbreviationFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(StateAbbreviationFilterTest.class);

    @Test
    public void filter1() throws Exception {

        final List<StateAbbreviationFilterStrategy> strategies = Arrays.asList(new StateAbbreviationFilterStrategy());
        final StateAbbreviationFilter filter = new StateAbbreviationFilter(strategies, new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());

        final String input = "The patient is from WV.";
        final List<Span> spans = filter.filter(getFilterProfile(), "context", "docid", input);

        Assert.assertEquals(1, spans.size());
        Assert.assertEquals(20, spans.get(0).getCharacterStart());
        Assert.assertEquals(22, spans.get(0).getCharacterEnd());
        Assert.assertEquals(FilterType.STATE_ABBREVIATION, spans.get(0).getFilterType());

    }

    @Test
    public void filter2() throws Exception {

        final List<StateAbbreviationFilterStrategy> strategies = Arrays.asList(new StateAbbreviationFilterStrategy());
        final StateAbbreviationFilter filter = new StateAbbreviationFilter(strategies, new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());

        final String input = "The patient is from wv.";
        final List<Span> spans = filter.filter(getFilterProfile(), "context", "docid", input);

        Assert.assertEquals(1, spans.size());
        Assert.assertEquals(20, spans.get(0).getCharacterStart());
        Assert.assertEquals(22, spans.get(0).getCharacterEnd());
        Assert.assertEquals(FilterType.STATE_ABBREVIATION, spans.get(0).getFilterType());

    }

    @Test
    public void filter3() throws Exception {

        final List<StateAbbreviationFilterStrategy> strategies = Arrays.asList(new StateAbbreviationFilterStrategy());
        final StateAbbreviationFilter filter = new StateAbbreviationFilter(strategies, new StateAbbreviationAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());

        final String input = "Patients from WV and MD.";
        final List<Span> spans = filter.filter(getFilterProfile(), "context", "docid", input);

        showSpans(spans);

        Assert.assertEquals(2, spans.size());

        Assert.assertEquals(21, spans.get(0).getCharacterStart());
        Assert.assertEquals(23, spans.get(0).getCharacterEnd());
        Assert.assertEquals(FilterType.STATE_ABBREVIATION, spans.get(0).getFilterType());
        Assert.assertEquals("MD", spans.get(0).getText());

        Assert.assertEquals(14, spans.get(1).getCharacterStart());
        Assert.assertEquals(16, spans.get(1).getCharacterEnd());
        Assert.assertEquals(FilterType.STATE_ABBREVIATION, spans.get(1).getFilterType());
        Assert.assertEquals("WV", spans.get(1).getText());

    }

}
