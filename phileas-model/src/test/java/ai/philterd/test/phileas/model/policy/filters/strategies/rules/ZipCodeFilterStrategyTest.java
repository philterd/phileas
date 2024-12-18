/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.test.phileas.model.policy.filters.strategies.rules;

import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.ZipCodeFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.test.phileas.model.policy.filters.strategies.AbstractFilterStrategyTest;
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

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "90210", WINDOW,"population < 10000", 1.0, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "90210", WINDOW,"population > 10000", 1.0, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "90210", WINDOW,"population == 21741", 1.0, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid","90210", WINDOW,"population > 20000 and population < 25000", 1.0, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "90210", WINDOW,"population > 20000 and population < 20010", 1.0, attributes);

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
    public void truncateTo1Trailing() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(1);
        strategy.setTruncateDirection(AbstractFilterStrategy.TRAILING);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final Replacement replacement = strategy.getReplacement("name", "context", "documentid", "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("****0", replacement.getReplacement());

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

    // Override the standard truncate tests since zip has a different truncate behavior
    @Test
    public void truncate1() throws Exception {}

    @Test
    public void truncate2() throws Exception {}

    @Test
    public void truncate3() throws Exception {}

}
