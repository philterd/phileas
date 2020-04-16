package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.SurnameAnonymizationService;
import com.mtnfog.phileas.services.cache.anonymization.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
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
    public void filter1() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", "Lived in Wshington");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filter2() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, anonymizationService, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", "Lived in Wshington");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

    @Test
    public void filter3() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, Collections.emptySet(), new Crypto());

        final List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", "Lived in Wasinton");
        showSpans(spans);
        Assert.assertEquals(0, spans.size());

    }

}
