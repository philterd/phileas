package com.mtnfog.test.phileas.services.filters.dictionary.lucene;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.SurnameAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Ignore("This isn't finding anything")
public class SurnameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(SurnameFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("surnames");

    @Before
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filter1() throws IOException {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, LuceneDictionaryFilter.SURNAME_DISTANCES, anonymizationService);

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", "Lived in Wshington");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filter2() throws IOException {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, LuceneDictionaryFilter.SURNAME_DISTANCES, anonymizationService);

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", "Lived in Wshington");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filter3() throws IOException {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, LuceneDictionaryFilter.SURNAME_DISTANCES, anonymizationService);

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", "Lived in Wasinton");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

}
