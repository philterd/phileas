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
package ai.philterd.test.phileas.model.policy.filters.strategies.ai;

import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.ai.PhEyeFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.services.CacheService;
import ai.philterd.test.phileas.model.policy.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class PhEyeFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new PhEyeFilterStrategy();
    }

    @Test
    public void replacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("static-value");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("static-value", replacement.getReplacement());

    }

    @Test
    public void replacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("REDACTION-%t");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("REDACTION-person", replacement.getReplacement());

    }

    @Test
    public void replacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final CacheService anonymizationCacheService = Mockito.mock(CacheService.class);

        when(anonymizationCacheService.getAnonymizedToken("context", "token")).thenReturn("random");
        when(anonymizationService.getCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertNotEquals("random", replacement.getReplacement());

    }

    @Test
    public void replacement4() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final CacheService anonymizationCacheService = Mockito.mock(CacheService.class);

        when(anonymizationCacheService.getAnonymizedToken("context", "token")).thenReturn("random");
        when(anonymizationService.getCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy("something-wrong");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("{{{REDACTED-person}}}", replacement.getReplacement());

    }

    @Test
    public void replacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final CacheService anonymizationCacheService = Mockito.mock(CacheService.class);

        when(anonymizationService.getCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new PhEyeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("<ENTITY:person>token</ENTITY>", replacement.getReplacement());

    }

    @Test
    public void replacement6() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final CacheService anonymizationCacheService = Mockito.mock(CacheService.class);

        when(anonymizationService.getCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new PhEyeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.ABBREVIATE);

        Replacement replacement;

        replacement = strategy.getReplacement("PER", "context", "docId", "John Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P. Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P.", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JP", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("J", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "J Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "J. Peter Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

    }

    @Test
    public void evaluateCondition1() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence > 0.25",  0.5, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        attributes.put("classification", "PER");
        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"type == PER",  1.0, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        attributes.put("classification", "LOC");
        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"type == PER",  1.0, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence == 0.5",  0.5, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence != 0.5",  0.6, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence != 0.5",  0.5, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        attributes.put("classification", "LOC");
        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence != 0.5 and type == PER",  0.5, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition8() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        attributes.put("classification", "PER");
        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence != 0.5 and type != LOC",  0.5, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition9() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        attributes.put("classification", "PER");
        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence > 0.4 and type == PER",  0.5, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition10() {

        final AbstractFilterStrategy strategy = getFilterStrategy();


        attributes.put("classification", "PER");
        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"confidence < 0.4 and type == PER",  0.5, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition11() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "documentid", "John Smith", WINDOW,"context == \"c1\"",  1.0, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition12() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "ctx", "documentId", "John Smith", WINDOW,"context == \"ctx\"",  1.0, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition13() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "ctx", "documentId", "John Smith", WINDOW,"token == \"John Smith\"",  1.0, attributes);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition14() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "ctx", "documentId", "John Smith", WINDOW,"token != \"John Smith\"",  1.0, attributes);

        Assertions.assertFalse(conditionSatisfied);

    }

}
