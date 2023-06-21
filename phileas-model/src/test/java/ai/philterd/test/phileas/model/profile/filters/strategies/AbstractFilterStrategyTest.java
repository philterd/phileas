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
package ai.philterd.test.phileas.model.profile.filters.strategies;

import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.profile.Crypto;
import ai.philterd.phileas.model.profile.FPE;
import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationCacheService;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.test.phileas.model.profile.filters.strategies.rules.DriversLicenseFilterStrategyTest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.when;

public abstract class AbstractFilterStrategyTest {

    protected static final Logger LOGGER = LogManager.getLogger(AbstractFilterStrategyTest.class);

    public abstract AbstractFilterStrategy getFilterStrategy() throws IOException;

    public static final String[] WINDOW = new String[3];

    @Test
    public void replacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("static-value");

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("static-value", replacement.getReplacement());

    }

    @Test
    public void replacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("REDACTION-%t");

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("REDACTION-" + strategy.getFilterType().getType(), replacement.getReplacement());

    }

    @Test
    public void replacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertNotEquals("random", replacement.getReplacement());

    }

    @Test
    public void replacement4() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy("something-wrong");

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("{{{REDACTED-" + strategy.getFilterType().getType() + "}}}", replacement.getReplacement());

    }

    @Test
    public void replacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("<ENTITY:" + strategy.getFilterType().getType() + ">token</ENTITY>", replacement.getReplacement());

    }

    @Test
    public void replacement6() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.CRYPTO_REPLACE);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final Crypto crypto = new Crypto("9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1", "B674D3B8F1C025AEFF8F6D5FA1AEAD3A");

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, crypto, new FPE(), anonymizationService, null);

        Assertions.assertEquals("{{j6HcaY8m7hPACVVyQtj4PQ==}}", replacement.getReplacement());

    }

    @Test
    public void replacement7() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        when(anonymizationService.anonymize("token")).thenReturn("randomtoken");

        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("randomtoken", replacement.getReplacement());

    }

    @Test
    public void replacement8() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("staticreplacement");

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("staticreplacement", replacement.getReplacement());

    }

    @Test
    public void replacement9() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.CRYPTO_REPLACE);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final Crypto crypto = new Crypto();

        Assertions.assertThrows(Exception.class, () -> {

            // Throws an exception because we tried to use CRYPTO_REPLACE without any keys.
            strategy.getReplacement("name", "context", "docId", "token", WINDOW, crypto, new FPE(), anonymizationService, null);

        });

    }

    @Test
    public void replacement10() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.HASH_SHA256_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "token", WINDOW, null, new FPE(), anonymizationService, null);

        Assertions.assertNotNull(replacement.getSalt());
        final String expected = DigestUtils.sha256Hex("token" + replacement.getSalt());

        // This is the hash of "token"
        Assertions.assertEquals(expected, replacement.getReplacement());

    }

    @Test
    public void evaluateCondition1() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW, "token startswith \"902\"", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW, "token == \"90210\"", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid",  "12345", WINDOW, "token == \"90210\"", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW, "context == \"c1\"",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW, "context == \"ctx\"",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW, "confidence > 0.5",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW, "confidence < 0.5",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

}
