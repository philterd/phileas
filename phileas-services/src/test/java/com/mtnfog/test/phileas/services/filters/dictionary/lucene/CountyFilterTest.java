package com.mtnfog.test.phileas.services.filters.dictionary.lucene;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CountyFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.CountyAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.test.phileas.services.filters.AbstractFilterTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CountyFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CountyFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("counties");

    @Before
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterCountiesLow() throws IOException {

        AnonymizationService anonymizationService = new CountyAnonymizationService(new LocalAnonymizationCacheService());

        final List<CountyFilterStrategy> strategies = Arrays.asList(new CountyFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, strategies, INDEX_DIRECTORY, LuceneDictionaryFilter.getCountiesDistances(), anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid","Lived in Fyette");

        showSpans(spans);

        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filterCountiesMedium() throws IOException {

        AnonymizationService anonymizationService = new CountyAnonymizationService(new LocalAnonymizationCacheService());

        final List<CountyFilterStrategy> strategies = Arrays.asList(new CountyFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, strategies, INDEX_DIRECTORY, LuceneDictionaryFilter.getCountiesDistances(), anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid","Lived in Fyette");

        showSpans(spans);

        Assert.assertEquals(1, spans.size());

    }

    @Test
    public void filterCountiesHigh() throws IOException {

        AnonymizationService anonymizationService = new CountyAnonymizationService(new LocalAnonymizationCacheService());

        final List<CountyFilterStrategy> strategies = Arrays.asList(new CountyFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_COUNTY, strategies, INDEX_DIRECTORY, LuceneDictionaryFilter.getCountiesDistances(), anonymizationService);

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid","Lived in Fyette");

        showSpans(spans);

        Assert.assertEquals(3, spans.size());

    }

}
