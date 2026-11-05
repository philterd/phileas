/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.strategies.rules;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.anonymization.ZipCodeAnonymizationService;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategyTest;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Properties;

public class ZipCodeFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() throws IOException {
        return new ZipCodeFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new ZipCodeAnonymizationService();
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

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "90210", WINDOW,"population < 10000", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "90210", WINDOW,"population > 10000", 1.0);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "90210", WINDOW,"population == 21134", 1.0);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context","90210", WINDOW,"population > 20000 and population < 25000", 1.0);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "90210", WINDOW,"population > 20000 and population < 20010", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateConditionInvalidZipCode() throws IOException {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "12345", WINDOW,"population > 1", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void staticReplacement1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("whoa");

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "90210", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("whoa", replacement.getReplacement());

    }

    @Test
    public void truncateTo2() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(2);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "90210", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("90***", replacement.getReplacement());

    }

    @Test
    public void truncateTo3() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(3);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("902**", replacement.getReplacement());

    }

    @Test
    public void truncateTo1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateDigits(1);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

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
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("****0", replacement.getReplacement());

    }

    @Test
    public void truncateTo1Leave() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.TRUNCATE);
        strategy.setTruncateLeaveCharacters(1);
        strategy.setTruncateDirection(AbstractFilterStrategy.TRAILING);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("****0", replacement.getReplacement());

    }

    @Test
    public void zeroLeading1() throws Exception {

        ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.ZERO_LEADING);

        AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "90210-0110", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        LOGGER.info(replacement);

        Assertions.assertEquals("00010", replacement.getReplacement());

    }

    @ParameterizedTest
    @CsvSource({
            "03602, 000",
            "03602-1234, 000",
            "036021234, 000",
            "05901, 000",
            "82190, 000",
            "20250, 000",
            "00001, 000",
            "09865-1234, 000",
            "57601, 576",
            "90210, 902",
            "90210-0110, 902",
            "902100110, 902"
    })
    public void safeHarborReplacement(final String token, final String expected) throws Exception {

        final ZipCodeFilterStrategy strategy = new Gson().fromJson("{ \"strategy\": \"SAFE_HARBOR\" }", ZipCodeFilterStrategy.class);
        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final ContextService contextService = new DefaultContextService();

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context", token, WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals(expected, replacement.getReplacement());

    }

    @Test
    public void safeHarborShortTokenFailsClosed() throws Exception {

        final ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
        strategy.setStrategy(ZipCodeFilterStrategy.SAFE_HARBOR);

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final ContextService contextService = new DefaultContextService();

        Assertions.assertEquals("000", strategy.getReplacement(contextService, "name", "context", "90", WINDOW, new Crypto(), new FPE(), anonymizationService, null).getReplacement());
        Assertions.assertEquals("000", strategy.getReplacement(contextService, "name", "context", "", WINDOW, new Crypto(), new FPE(), anonymizationService, null).getReplacement());

    }

    @Test
    public void safeHarborEndToEndPolicy() throws Exception {

        final String json = """
                {
                   "identifiers": {
                      "zipCode": {
                         "requireDelimiter": true,
                         "zipCodeFilterStrategies": [
                            {
                               "strategy": "SAFE_HARBOR"
                            }
                         ]
                      }
                   }
                }
                """;

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()),
                new DefaultContextService(),
                new InMemoryVectorService(),
                null);

        final Policy policy = new Gson().fromJson(json, Policy.class);
        final String input = "Patients in 03602, 03602-1234, 90210, 90210-0110, and 09865 were seen.";
        final String filtered = service.filter(policy, "context", input).getFilteredText();

        Assertions.assertEquals("Patients in 000, 000, 902, 902, and 000 were seen.", filtered);

    }

    // Override the standard truncate tests since zip has a different truncate behavior
    @Test
    public void truncate1() throws Exception {}

    @Test
    public void truncate2() throws Exception {}

    @Test
    public void truncate3() throws Exception {}

    @Test
    public void truncate4() throws Exception {}

}
