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

import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.services.defaults.DefaultContextService;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AgeFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public AbstractFilterStrategy getFilterStrategy() {
        return new AgeFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new AgeAnonymizationService(new DefaultContextService());
    }

    @Test
    public void replacement1() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context",  "docId", "52 years old", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertTrue(replacement.getReplacement().endsWith(" years old"));
        Assertions.assertNotEquals("52 years old", replacement.getReplacement());

    }

}
