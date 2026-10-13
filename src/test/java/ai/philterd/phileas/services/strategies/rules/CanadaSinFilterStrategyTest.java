/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.anonymization.CanadaSinAnonymizationService;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.CanadaSinFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategyTest;
import ai.philterd.phileas.services.validators.LuhnValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CanadaSinFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new CanadaSinFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new CanadaSinAnonymizationService();
    }

    @Test
    public void lastFour() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement(contextService, "name", "context", "046454286", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("4286", replacement.getReplacement());

    }

    @Test
    public void randomReplaceIsLuhnValidSin() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        for (final String sin : new String[]{"046-454-286", "046 454 286", "046454286", "046-454-\n286"}) {
            for (int i = 0; i < 25; i++) {

                final String replacement = strategy.getReplacement(contextService, "name", "context" + i, sin, WINDOW, new Crypto(), new FPE(), anonymizationService, null).getReplacement();

                Assertions.assertNotEquals(sin, replacement);
                Assertions.assertEquals(sin.replaceAll("\\d", "0"), replacement.replaceAll("\\d", "0"), replacement);
                Assertions.assertTrue(LuhnValidator.isValid(replacement), replacement);
                Assertions.assertTrue(CanadaSinFilter.isValidPrefix(replacement), replacement);

            }
        }

    }

}
