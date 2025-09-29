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
package ai.philterd.test.phileas.model.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.anonymization.CreditCardAnonymizationService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CreditCardAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(CreditCardAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new CreditCardAnonymizationService();

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Credit Card: " + replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(replacement.length() == 16);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new CreditCardAnonymizationService();

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Credit Card: " + replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(replacement.length() == 16);

    }

}
