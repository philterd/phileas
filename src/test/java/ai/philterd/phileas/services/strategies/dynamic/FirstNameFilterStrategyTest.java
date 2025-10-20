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
package ai.philterd.phileas.services.strategies.dynamic;

import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.anonymization.FirstNameAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FirstNameFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new FirstNameFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new FirstNameAnonymizationService(new DefaultContextService());
    }

    @Test
    public void replacementWithContext() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        anonymizationService.getContextService().putReplacement("jeff", "john");

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        if(anonymizationService instanceof FirstNameAnonymizationService) {

            final Replacement replacement1 = strategy.getReplacement("name", "context",  "jeff", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
            Assertions.assertEquals("john", replacement1.getReplacement());

            final Replacement replacement2 = strategy.getReplacement("name", "context",  "thomas", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
            Assertions.assertNotEquals("john", replacement2.getReplacement());
            Assertions.assertNotEquals("jeff", replacement2.getReplacement());
            Assertions.assertNotEquals("thomas", replacement2.getReplacement());

        }

    }

    @Test
    public void replacementWithContextDocumentScope() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        anonymizationService.getContextService().putReplacement("jeff", "john");

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_DOCUMENT);

        if(anonymizationService instanceof FirstNameAnonymizationService) {

            final Replacement replacement1 = strategy.getReplacement("name", "context",  "jeff", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
            Assertions.assertNotEquals("john", replacement1.getReplacement());

            final Replacement replacement2 = strategy.getReplacement("name", "context",  "thomas", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
            Assertions.assertNotEquals("john", replacement2.getReplacement());
            Assertions.assertNotEquals("jeff", replacement2.getReplacement());
            Assertions.assertNotEquals("thomas", replacement2.getReplacement());

        }

    }

}
