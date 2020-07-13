package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FilterTest extends AbstractFilterTest {

    protected static final Logger LOGGER = LogManager.getLogger(FilterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void window0() throws Exception {

        // This tests span window creation.
        int windowSize = 3;

        final AgeFilter filter = new AgeFilter(null, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.");

        showSpans(spans);

        final String[] window = new String[]{"the", "patient", "is", "35", "years", "old", "and", "hes", "cool"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(spans.get(0).getWindow()));

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", spans.get(0).getReplacement());
        Assertions.assertArrayEquals(window, spans.get(0).getWindow());
        Assertions.assertEquals("3.5 years old", spans.get(0).getText());

    }

    @Test
    public void window1() throws Exception {

        // This tests span window creation.
        int windowSize = 5;

        final AgeFilter filter = new AgeFilter(null, new AgeAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.");

        showSpans(spans);

        final String[] window = new String[]{"first", "sentence", "the", "patient", "is", "35", "years", "old", "and", "hes", "cool", "this", "is"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(spans.get(0).getWindow()));

        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", spans.get(0).getReplacement());
        Assertions.assertArrayEquals(window, spans.get(0).getWindow());
        Assertions.assertEquals("3.5 years old", spans.get(0).getText());

    }

}
