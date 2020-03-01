package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.StateFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.StateAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StateFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(StateFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("states");

    @Before
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterStatesLow() throws Exception {

        AnonymizationService anonymizationService = new StateAnonymizationService(new LocalAnonymizationCacheService());

        final List<StateFilterStrategy> strategies = Arrays.asList(new StateFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_STATE, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, Collections.emptySet(), new Crypto());

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid","Lived in Washington");
        Assert.assertEquals(1, spans.size());
        Assert.assertEquals("washington", spans.get(0).getText());

    }

    @Test
    public void filterStatesMedium() throws Exception {

        AnonymizationService anonymizationService = new StateAnonymizationService(new LocalAnonymizationCacheService());

        final List<StateFilterStrategy> strategies = Arrays.asList(new StateFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_STATE, strategies, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, anonymizationService, Collections.emptySet(), new Crypto());

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid","Lived in Wshington");
        Assert.assertEquals(1, spans.size());

    }

    @Test
    public void filterStatesHigh() throws Exception {

        AnonymizationService anonymizationService = new StateAnonymizationService(new LocalAnonymizationCacheService());

        final List<StateFilterStrategy> strategies = Arrays.asList(new StateFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.LOCATION_STATE, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, Collections.emptySet(), new Crypto());

        List<Span> spans = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid","Lived in Wasinton");
        Assert.assertEquals(1, spans.size());

    }

}
