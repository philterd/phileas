package com.mtnfog.test.phileas.model.profile.filters.strategies.dyamic;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.SsnFilterStrategy;
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

public class HospitalFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalFilterStrategyTest.class);

    @Test
    public void replacement1() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final HospitalFilterStrategy strategy = new HospitalFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("static-value");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("static-value", replacement);

    }

    @Test
    public void replacement2() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final HospitalFilterStrategy strategy = new HospitalFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("REDACTION-%t");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("REDACTION-hospital", replacement);

    }

    @Test
    public void replacement3() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final HospitalFilterStrategy strategy = new HospitalFilterStrategy();
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

        final HospitalFilterStrategy strategy = new HospitalFilterStrategy();
        strategy.setStrategy("something-wrong");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("{{{REDACTED-hospital}}}", replacement);

    }

    @Test
    public void replacement5() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new HospitalFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("<ENTITY:hospital>token</ENTITY>", replacement);

    }

    @Test
    public void evaluateCondition1() throws IOException {

        HospitalFilterStrategy strategy = new HospitalFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token startswith \"902\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        HospitalFilterStrategy strategy = new HospitalFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token == \"90210\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        HospitalFilterStrategy strategy = new HospitalFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "12345", "token == \"90210\"", Collections.emptyMap());

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final HospitalFilterStrategy strategy = new HospitalFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "context == \"c1\"", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final HospitalFilterStrategy strategy = new HospitalFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "context == \"ctx\"", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

}
