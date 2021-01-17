package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhysicianNameFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.PhysicianNameFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PhysicianNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhysicianNameFilterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void physicianNameTestPreNominal1() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Doctor Smith was the attending physician.");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 30, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Doctor Smith was the attending", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPreNominal2() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Doctor James Smith");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Doctor James Smith", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal1() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "John Smith, MD");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 14, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal2() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "attending physician was John Smith, MD");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 38, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("physician was John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal3() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "was John J. van Smith, MD");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 4, 25, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("John J. van Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal4() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Smith,James D,MD -General Surgery");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Smith,James D,MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal5() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Smith,James )D,MD -General Surgery");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Smith,James )D,MD", filterResult.getSpans().get(0).getText());

    }

}
