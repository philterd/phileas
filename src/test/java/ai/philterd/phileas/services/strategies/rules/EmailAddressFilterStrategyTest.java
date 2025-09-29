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

import ai.philterd.phileas.model.services.defaults.DefaultContextService;
import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.EmailAddressAnonymizationService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EmailAddressFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new EmailAddressFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new EmailAddressAnonymizationService(new DefaultContextService());
    }

    @Test
    public void evaluateCondition1() {

        final AbstractFilterStrategy strategy = new EmailAddressFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context",  "documentid",  "test@test.com", WINDOW, "token == \"test@test.com\"", 1.0, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

}
