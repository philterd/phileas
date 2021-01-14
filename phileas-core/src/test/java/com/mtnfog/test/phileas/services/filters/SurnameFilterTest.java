package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.SurnameAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SurnameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(SurnameFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("surnames");

    private AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filter1() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, false, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0, "Lived in Wshington");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter2() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, false, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", 0, "Lived in Wshington");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter3() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, false, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", 0, "Lived in Wasinton");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter4() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, false, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0, "date");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter5() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, false, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0, "DATE");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter6() throws Exception {

        AnonymizationService anonymizationService = new SurnameAnonymizationService(new LocalAnonymizationCacheService());

        final List<SurnameFilterStrategy> strategies = Arrays.asList(new SurnameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.SURNAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, false, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0, "from");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
