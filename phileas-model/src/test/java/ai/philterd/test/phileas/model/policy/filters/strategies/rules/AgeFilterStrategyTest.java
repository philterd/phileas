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
package ai.philterd.test.phileas.model.policy.filters.strategies.rules;

import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.model.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.model.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.test.phileas.model.policy.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class AgeFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public AbstractFilterStrategy getFilterStrategy() {
        return new AgeFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new AgeAnonymizationService();
    }

    @Test
    public void replacement1() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", Collections.emptyMap(), "docId", "52 years old", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertTrue(replacement.getReplacement().endsWith(" years old"));
        Assertions.assertNotEquals("52 years old", replacement.getReplacement());

    }

}
