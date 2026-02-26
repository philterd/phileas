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
package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.services.context.DefaultContextService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

public class CurrencyAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(CurrencyAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new CurrencyAnonymizationService(new DefaultContextService());

        final String token = "$1,000.00";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Currency: {}", replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertFalse(replacement.isEmpty());
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertTrue(replacement.startsWith("$"));

    }

    @Test
    public void anonymizeUUID() {

        AnonymizationService anonymizationService = new CurrencyAnonymizationService(new DefaultContextService(), new SecureRandom(), AnonymizationMethod.UUID);

        final String token = "$1,000.00";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Currency: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertTrue(replacement.length() >= 32);

    }

}
