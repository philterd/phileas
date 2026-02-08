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

public class CityAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(CityAnonymizationServiceTest.class);

    @Test
    public void constructor() {

        AnonymizationService anonymizationService = new CityAnonymizationService(new DefaultContextService(), new SecureRandom(), AnonymizationMethod.REALISTIC);

        final String token = "New York";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("City: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertFalse(replacement.isEmpty());

    }

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new CityAnonymizationService(new DefaultContextService());

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("City: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertFalse(replacement.isEmpty());

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new CityAnonymizationService(new DefaultContextService());

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("City: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertFalse(replacement.isEmpty());

    }

    @Test
    public void anonymizeUUID() {

        AnonymizationService anonymizationService = new CityAnonymizationService(new DefaultContextService(), new SecureRandom(), AnonymizationMethod.UUID);

        final String token = "New York";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("City: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertTrue(replacement.length() >= 32);

    }

}
