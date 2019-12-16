package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.FirstNameFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.PersonsAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FirstNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(FirstNameFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("names");

    @Before
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterLow() throws IOException {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, Collections.emptySet());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid","John lived in Washington");
        showSpans(spans);
        Assert.assertEquals(2, spans.size());

    }

    @Test
    public void filterMedium() throws IOException {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, anonymizationService, Collections.emptySet());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid","Michel had eye cancer");
        showSpans(spans);
        Assert.assertEquals(3, spans.size());

    }

    @Test
    public void filterHigh() throws IOException {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, Collections.emptySet());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid","Sandra in Washington");
        showSpans(spans);
        Assert.assertEquals(3, spans.size());

    }

}
