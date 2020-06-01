package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

public class ZipCodeFilterStrategyTest extends AbstractFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(ZipCodeFilterStrategyTest.class);

    public AbstractFilterStrategy getFilterStrategy() throws IOException {
        return new ZipCodeFilterStrategy();
    }

    @Test
    public void invalidLength0() throws IOException {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
            strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
            strategy.setTruncateDigits(new Integer(0));
        });

    }

    @Test
    public void invalidLength5() throws IOException {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
            strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
            strategy.setTruncateDigits(new Integer(5));
        });

    }

    @Test
    public void evaluateCondition1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population < 10000", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population > 10000", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population == 21741", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population > 20000 and population < 25000", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "population > 20000 and population < 20010", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "20500", "token startswith \"20\"", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "31590", "token startswith \"20\"", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition8() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "context == \"c1\"",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition9() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "context == \"ctx\"",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition10() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "confidence > 0.5",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition11() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "confidence < 0.5",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void staticReplacement1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("whoa");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "docid", "90210", new Crypto(), anonymizationService);

        Assertions.assertEquals("whoa", replacement.getReplacement());

    }

    @Test
    public void truncateTo2() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(2);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentId", "90210", new Crypto(), anonymizationService);

        LOGGER.info(replacement);

        Assertions.assertEquals("90***", replacement.getReplacement());

    }

    @Test
    public void truncateTo3() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(3);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", new Crypto(), anonymizationService);

        LOGGER.info(replacement);

        Assertions.assertEquals("902**", replacement.getReplacement());

    }

    @Test
    public void truncateTo1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(1);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", new Crypto(), anonymizationService);

        LOGGER.info(replacement);

        Assertions.assertEquals("9****", replacement.getReplacement());

    }

    @Test
    public void zeroLeading1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.ZERO_LEADING);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentId", "90210-0110", new Crypto(), anonymizationService);

        LOGGER.info(replacement);

        Assertions.assertEquals("00010", replacement.getReplacement());

    }

}
