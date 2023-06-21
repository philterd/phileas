/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.model.profile.filters.strategies.rules;

import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.PhoneNumberExtensionFilterStrategy;
import ai.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class PhoneNumberExtensionFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new PhoneNumberExtensionFilterStrategy();
    }

    @Test
    public void evaluateCondition1() throws IOException {

        PhoneNumberExtensionFilterStrategy strategy = new PhoneNumberExtensionFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW,"token startswith \"902\"", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        PhoneNumberExtensionFilterStrategy strategy = new PhoneNumberExtensionFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW,"token == \"90210\"", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        PhoneNumberExtensionFilterStrategy strategy = new PhoneNumberExtensionFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "12345", WINDOW, "token == \"90210\"", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW, "context == \"c1\"",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW, "context == \"ctx\"",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW, "confidence > 0.5",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW,"confidence < 0.5",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }


}
