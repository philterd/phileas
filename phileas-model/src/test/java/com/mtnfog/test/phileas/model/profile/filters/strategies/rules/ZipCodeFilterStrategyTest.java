package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FPE;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

public class ZipCodeFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() throws IOException {
        return new ZipCodeFilterStrategy();
    }

    @Test
    public void invalidLength0() throws IOException {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
            strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
            strategy.setTruncateDigits(0);
        });

    }

    @Test
    public void invalidLength5() throws IOException {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
            strategy.setRedactionFormat(ZipCodeFilterStrategy.TRUNCATE);
            strategy.setTruncateDigits(5);
        });

    }

    @Test
    public void evaluateCondition1() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW,"population < 10000", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW,"population > 10000", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW,"population == 21741", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid","90210", WINDOW,"population > 20000 and population < 25000", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW,"population > 20000 and population < 20010", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void staticReplacement1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("whoa");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "docid", "90210", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("whoa", replacement.getReplacement());

    }

    @Test
    public void truncateTo2() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(2);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentid", "90210", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("90***", replacement.getReplacement());

    }

    @Test
    public void truncateTo3() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(3);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentid", "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("902**", replacement.getReplacement());

    }

    @Test
    public void truncateTo1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(1);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentid", "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("9****", replacement.getReplacement());

    }

    @Test
    public void zeroLeading1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.ZERO_LEADING);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentid", "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("00010", replacement.getReplacement());

    }

}
