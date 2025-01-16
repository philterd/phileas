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
package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.anonymization.StreetAddressAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StreetAddressAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(StreetAddressAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new StreetAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "100 Main St";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Street Address: " + replacement);
        Assertions.assertNotEquals(token, replacement);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new StreetAddressAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "1000 Main St";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Street Address: " + replacement);
        Assertions.assertNotEquals(token, replacement);

    }

}
