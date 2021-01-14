package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.filter.rules.dictionary.LuceneDictionaryFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.FirstNameFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.services.anonymization.PersonsAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FirstNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(FirstNameFilterTest.class);

    private String INDEX_DIRECTORY = getIndexDirectory("names");

    private AlertService alertService = Mockito.mock(AlertService.class);

    @BeforeEach
    public void before() {
        INDEX_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEX_DIRECTORY.substring(1) : INDEX_DIRECTORY;
        LOGGER.info("Using index directory {}", INDEX_DIRECTORY);
    }

    @Test
    public void filterLow() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0,"John");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterMedium1() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.MEDIUM, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", 0, "Michel had eye cancer");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(3, filterResult.getSpans().size());

    }

    @Test
    public void filterMedium2() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.MEDIUM), "context", "documentid", 0,"Jennifer had eye cancer");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterHigh() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.HIGH, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.HIGH), "context", "documentid", 0,"Sandra in Washington");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(2, filterResult.getSpans().size());

    }

    @Test
    public void filter1() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0,"alae");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter2() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0,"que");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter3() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0,"dat");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter4() throws Exception {

        AnonymizationService anonymizationService = new PersonsAnonymizationService(new LocalAnonymizationCacheService());

        final List<FirstNameFilterStrategy> strategies = Arrays.asList(new FirstNameFilterStrategy());
        final LuceneDictionaryFilter filter = new LuceneDictionaryFilter(FilterType.FIRST_NAME, strategies, INDEX_DIRECTORY, SensitivityLevel.LOW, anonymizationService, alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(SensitivityLevel.LOW), "context", "documentid", 0,"joie");
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

}
