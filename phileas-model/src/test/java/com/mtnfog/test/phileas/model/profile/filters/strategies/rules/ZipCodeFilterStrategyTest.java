package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

public class ZipCodeFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(ZipCodeFilterStrategyTest.class);

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
    public void staticReplacement1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setRedactionFormat(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("whoa");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "docid", "90210", anonymizationService);

        Assert.assertEquals("whoa", replacement);

    }

    @Test
    public void truncateTo2() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(2);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("90***", replacement);

    }

    @Test
    public void truncateTo3() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(3);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("902**", replacement);

    }

    @Test
    public void truncateTo1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(1);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("9****", replacement);

    }

    @Test
    public void zeroLeading1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setRedactionFormat(ZipCodeFilterStrategy.ZERO_LEADING);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final String replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", anonymizationService);

        LOGGER.info(replacement);

        Assert.assertEquals("00010", replacement);

    }

}
