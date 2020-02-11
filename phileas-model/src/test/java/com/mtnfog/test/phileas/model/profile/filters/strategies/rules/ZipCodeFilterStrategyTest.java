package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
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

public class ZipCodeFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(ZipCodeFilterStrategyTest.class);

    @Test
    public void replacement1() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("static-value");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("static-value", replacement);

    }

    @Test
    public void replacement2() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("REDACTION-%t");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("REDACTION-zip-code", replacement);

    }

    @Test
    public void replacement3() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
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

        final ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy("something-wrong");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("{{{REDACTED-zip-code}}}", replacement);

    }

    @Test
    public void replacement5() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", anonymizationService);

        Assert.assertEquals("<ENTITY:zip-code>token</ENTITY>", replacement);

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidLength0() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(new Integer(0));

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidLength5() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(new Integer(5));

    }

    @Test
    public void evaluateCondition1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population < 10000", Collections.emptyMap());

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population > 10000", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population == 21741", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population > 20000 and population < 25000", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population > 20000 and population < 20010", Collections.emptyMap());

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "20500", "token startswith \"20\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "31590", "token startswith \"20\"", Collections.emptyMap());

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition8() throws IOException {

        final ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "context == \"c1\"", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition9() throws IOException {

        final ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "context == \"ctx\"", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void staticReplacement1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("whoa");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "docid", "90210", anonymizationService);

        Assert.assertEquals("whoa", replacement);

    }

    @Test
    public void truncateTo2() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(2);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("90***", replacement);

    }

    @Test
    public void truncateTo3() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(3);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("902**", replacement);

    }

    @Test
    public void truncateTo1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(1);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("9****", replacement);

    }

    @Test
    public void zeroLeading1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.ZERO_LEADING);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("00010", replacement);

    }

}
