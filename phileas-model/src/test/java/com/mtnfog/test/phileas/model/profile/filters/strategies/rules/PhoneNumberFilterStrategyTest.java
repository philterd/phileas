package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhoneNumberExtensionFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public class PhoneNumberFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(PhoneNumberFilterStrategyTest.class);

    @Test
    public void replacement1() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("static-value");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("static-value", replacement);

    }

    @Test
    public void replacement2() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("REDACTION-%t");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("REDACTION-phone-number", replacement);

    }

    @Test
    public void replacement3() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertNotEquals("random", replacement);

    }

    @Test
    public void replacement4() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();
        strategy.setStrategy("something-wrong");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("{{{REDACTED-phone-number}}}", replacement);

    }

    @Test
    public void evaluateCondition1() throws IOException {

        PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token startswith \"902\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token == \"90210\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "12345", "token == \"90210\"", Collections.emptyMap());

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "context == \"c1\"", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "context == \"ctx\"", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

}
