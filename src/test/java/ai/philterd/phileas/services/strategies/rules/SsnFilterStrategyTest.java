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
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.NumericAnonymizationService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SsnFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new SsnFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new NumericAnonymizationService(new DefaultContextService());
    }

    @Test
    public void formatPreservingEncryption1() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = getAnonymizationService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context",  "123-45-6789", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(11, replacement.getReplacement().length());
        Assertions.assertEquals("609-59-7486", replacement.getReplacement());

    }

    @Test
    public void formatPreservingEncryption2() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = getAnonymizationService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context",  "987 65 4321", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(11, replacement.getReplacement().length());
        Assertions.assertEquals("195 55 5147", replacement.getReplacement());

    }

    @Test
    public void formatPreservingEncryption3() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = getAnonymizationService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context",  "987654321", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(9, replacement.getReplacement().length());
        Assertions.assertEquals("195555147", replacement.getReplacement());

    }

    @Test
    public void lastFour1() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();

        final AbstractFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context",  "4111111111111111", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("1111", replacement.getReplacement());

    }

    @Test
    public void lastFour2() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();

        final AbstractFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context",  "123-456-7890", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("7890", replacement.getReplacement());

    }

}
