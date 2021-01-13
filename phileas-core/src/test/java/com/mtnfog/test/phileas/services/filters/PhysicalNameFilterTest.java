package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhysicianNameFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.anonymization.AlphanumericAnonymizationService;
import com.mtnfog.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.custom.PhoneNumberRulesFilter;
import com.mtnfog.phileas.services.filters.regex.PhysicianNameFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PhysicalNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhysicalNameFilterTest.class);

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void physicianNameTestPreNominal1() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "Doctor Smith");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals("Doctor Smith", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTest1() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "John Smith, MD");
        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals("John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTest2() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "was John Smith, MD");
        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals("John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTest3() throws Exception {

        final List<PhysicianNameFilterStrategy> strategies = Arrays.asList(new PhysicianNameFilterStrategy());
        final PhysicianNameFilter filter = new PhysicianNameFilter(strategies, new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()), alertService, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), new Crypto(), windowSize);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", 0, "was John J. van Smith, MD");
        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals("John Smith, MD", filterResult.getSpans().get(0).getText());

    }

}
